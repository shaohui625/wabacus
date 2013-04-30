/* 
 * Copyright (C) 2010---2012 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.system.buttons;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.system.serveraction.IServerAction;
import com.wabacus.util.Tools;
import com.wabacus.util.UUIDGenerator;

public class ServerSQLActionButton extends WabacusButton implements IServerAction
{
    private static Log log=LogFactory.getLog(ServerSQLActionButton.class);
    
    private boolean shouldRefreshPage;

    private String beforeCallbackMethod;
    
    private String afterCallbackMethod;
    
    private String conditions;
    
    private String datasource;//执行SQL所使用的数据源，没有配置时取全局默认连接
    
    private String successprompt;
    
    private String failedprompt;
    
    private List<Map<String,List<SqlParamBean>>> lstSqlAndParams;
    
    public ServerSQLActionButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    void setLstSqlAndParams(List<Map<String,List<SqlParamBean>>> lstSqlAndParams)
    {
        this.lstSqlAndParams=lstSqlAndParams;
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        return super.showButton(rrequest,getMyClickEvent(rrequest));
    }
    
    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        return super.showButton(rrequest,getMyClickEvent(rrequest),button);
    }

    public String showMenu(ReportRequest rrequest,String dynclickevent)
    {
        return super.showMenu(rrequest,getMyClickEvent(rrequest));
    }

    private String getMyClickEvent(ReportRequest rrequest)
    {
        //这里在serverClassName参数中传入button{name}，而不是传入此类的类名，因为一个报表可能配置多个这种按钮，只有传入<button/>的name才能在调用时找到相应的按钮
        return "invokeServerActionForReportData('"+this.ccbean.getPageBean().getId()+"','"+this.ccbean.getId()+"','button{"+this.getName()+"}',"
                +this.shouldRefreshPage+","+this.conditions+","+this.beforeCallbackMethod+","+this.afterCallbackMethod+")";
    }
    
    public String executeSeverAction(ReportRequest rrequest,IComponentConfigBean ccbean,List<Map<String,String>> lstData)
    {
        if(lstSqlAndParams==null||lstSqlAndParams.size()==0) return "0";
        AbsDatabaseType dbtype=rrequest.getDbType(this.datasource);
        Connection conn=rrequest.getConnection(this.datasource);
        try
        {
            conn.setAutoCommit(false);
            if(lstData==null||lstData.size()==0)
            {
                executeServerActionForPerDataRow(rrequest,ccbean,null,conn,dbtype);
            }else
            {
                for(Map<String,String> mDatasTmp:lstData)
                {//对每条记录执行一次SQL操作
                    executeServerActionForPerDataRow(rrequest,ccbean,mDatasTmp,conn,dbtype);
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            if(this.successprompt!=null&&!this.successprompt.trim().equals(""))
            {
                rrequest.getWResponse().getMessageCollector().success(rrequest.getI18NStringValue(this.successprompt),false);
            }
        }catch(Exception e)
        {
            try
            {
                conn.rollback();
                conn.setAutoCommit(true);
            }catch(SQLException e1)
            {
                e1.printStackTrace();
            }
            rrequest.getWResponse().getMessageCollector().error(rrequest.getI18NStringValue(this.failedprompt),
                    "执行报表"+ccbean.getPath()+"下的按钮"+this.name+"配置的SQL语句失败",e);
            return "-1";
        }
        return "1";
        
    }

    private void executeServerActionForPerDataRow(ReportRequest rrequest,IComponentConfigBean ccbean,Map<String,String> mDatas,Connection conn,
            AbsDatabaseType dbtype) throws SQLException
    {
        String sqlTmp;
        List<SqlParamBean> lstSqlParamBeansTmp;
        PreparedStatement pstmt;
        for(Map<String,List<SqlParamBean>> mSqlAndParamsTmp:this.lstSqlAndParams)
        {
            sqlTmp=mSqlAndParamsTmp.keySet().iterator().next();
            lstSqlParamBeansTmp=mSqlAndParamsTmp.get(sqlTmp);//动态参数
            pstmt=conn.prepareStatement(sqlTmp);
            log.debug("Execute Sql："+sqlTmp);
            if(lstSqlParamBeansTmp!=null&&lstSqlParamBeansTmp.size()>0)
            {
                int colidx=1;
                for(SqlParamBean paramBeanTmp:lstSqlParamBeansTmp)
                {
                    paramBeanTmp.getParamTypeObj().setPreparedStatementValue(colidx++,paramBeanTmp.getParamValue(rrequest,mDatas),pstmt,dbtype);
                }
            }
            pstmt.executeUpdate();
            pstmt.close();
        }
    }
    
    public String executeServerAction(HttpServletRequest request,HttpServletResponse response,List<Map<String,String>> lstData)
    {
        return "";
    }
    
    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
        super.loadExtendConfig(eleButtonBean);
        String refreshpage=eleButtonBean.attributeValue("refreshpage");
        this.beforeCallbackMethod=eleButtonBean.attributeValue("beforecallbackmethod");
        if(this.beforeCallbackMethod!=null&&this.beforeCallbackMethod.trim().equals("")) this.beforeCallbackMethod=null;
        this.afterCallbackMethod=eleButtonBean.attributeValue("aftercallbackmethod");
        if(this.afterCallbackMethod!=null&&this.afterCallbackMethod.trim().equals("")) this.afterCallbackMethod=null;
        this.conditions=eleButtonBean.attributeValue("conditions");
        refreshpage=refreshpage==null||refreshpage.trim().equals("")?"true":refreshpage.toLowerCase().trim();
        this.shouldRefreshPage=refreshpage.equals("true");
        if((this.ccbean instanceof ReportBean)&&(Config.getInstance().getReportType(((ReportBean)this.ccbean).getType()) instanceof AbsListReportType))
        {
            if(this.conditions==null)
            {
                this.conditions="{name:'SELECTEDROW',value:true}";
            }else if(this.conditions.trim().equals(""))
            {//如果显式配置conditions=""，则说明取所有记录的数据
                this.conditions="null";
            }else
            {
                this.conditions=this.conditions.trim();
                if((!this.conditions.startsWith("{")||!this.conditions.endsWith("}"))
                        &&(!this.conditions.startsWith("[")||!this.conditions.endsWith("]")))
                {
                    this.conditions="{"+this.conditions+"}";
                }
            }
        }else
        {
            this.conditions=null;
        }
        this.datasource=eleButtonBean.attributeValue("datasource");
        String successmess=eleButtonBean.attributeValue("successprompt");
        String failedmess=eleButtonBean.attributeValue("failedprompt");
        if(successmess!=null)
        {
            this.successprompt=Config.getInstance().getResourceString(null,this.ccbean.getPageBean(),successmess,false).trim();
        }else
        {
            this.successprompt="";
        }
        if(failedmess!=null)
        {
            this.failedprompt=Config.getInstance().getResourceString(null,this.ccbean.getPageBean(),failedmess,false).trim();
        }else
        {
            this.failedprompt="";
        }
        if(this.clickhandler==null)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，没有为它配置SQL语句");
        }else if(this.clickhandler instanceof IButtonClickeventGenerate)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，不能为这种类型的按钮配置为动态构造SQL语句");
        }else if(this.clickhandler.toString().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，没有为它配置SQL语句");
        }
        
        List<String> lstSqls=Tools.parseStringToList(this.clickhandler.toString(),';','\'');
        for(String sqlTmp:lstSqls)
        {
            if(sqlTmp==null||sqlTmp.trim().equals("")) continue;
            sqlTmp=sqlTmp.trim();
            if(sqlTmp.toLowerCase().startsWith("insert"))
            {
                parseInsertSql(sqlTmp);
            }else if(sqlTmp.toLowerCase().startsWith("update"))
            {
                parseUpdateSql(sqlTmp);
            }else if(sqlTmp.toLowerCase().startsWith("delete"))
            {
                parseDeleteSql(sqlTmp);
            }else
            {
                throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句"+sqlTmp+"不合法");
            }
        }
        if(lstSqlAndParams==null||lstSqlAndParams.size()==0)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，没有为它配置SQL语句");
        }
    }
    
    private void parseInsertSql(String sql)
    {
        String insertsql=sql.trim();
        int idxValues=insertsql.toLowerCase().indexOf(" values");
        if(idxValues>0)
        {
            String tmp1=insertsql.substring(0,idxValues).trim();
            String tmp2=insertsql.substring(idxValues+" values".length()).trim();
            if(tmp1.indexOf("(")>0&&tmp1.endsWith(")")&&tmp2.startsWith("(")&&tmp2.indexOf(")")>0)
            {
                this.addSqlAndParams(sql,null);
                return;
            }
        }
        insertsql=insertsql.substring("insert".length()).trim();
        if(insertsql.toLowerCase().indexOf("into ")==0)
        {
            insertsql=insertsql.substring("into".length()).trim();
        }
        int idxleft=insertsql.indexOf("(");
        if(idxleft<=0)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句"+sql+"不合法");
        }
        String tablename=insertsql.substring(0,idxleft).trim();
        insertsql=insertsql.substring(idxleft).trim();
        int idxright=insertsql.lastIndexOf(")");
        if(idxright!=insertsql.length()-1)
        {//不是配置为insert into tbl(...)格式
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句"+sql+"不合法");
        }
        
        String insertcols=insertsql.substring(1,insertsql.length()-1).trim();
        if(tablename.equals("")||insertcols.equals(""))
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句"+sql+"不合法");
        }
        List<Map<String,Object>> lstInsertColAndParams=parseSqlUpdateClause(insertcols);
        insertsql="insert into "+tablename+" (";
        for(Map<String,Object> mColAndParamTmp:lstInsertColAndParams)
        {
            insertsql=insertsql+mColAndParamTmp.keySet().iterator().next()+",";
        }
        if(insertsql.endsWith(",")) insertsql=insertsql.substring(0,insertsql.length()-1);
        insertsql=insertsql+")values(";
        List<SqlParamBean> lstDynParams=new ArrayList<SqlParamBean>();
        Object paramObjTmp;
        for(Map<String,Object> mColAndParamTmp:lstInsertColAndParams)
        {
            paramObjTmp=mColAndParamTmp.entrySet().iterator().next().getValue();
            if(paramObjTmp instanceof SqlParamBean)
            {
                insertsql=insertsql+"?,";
                lstDynParams.add((SqlParamBean)paramObjTmp);
            }else
            {//常量参数
                if(paramObjTmp==null) paramObjTmp="''";
                insertsql=insertsql+paramObjTmp+",";
            }
        }
        if(insertsql.endsWith(",")) insertsql=insertsql.substring(0,insertsql.length()-1);
        insertsql=insertsql+")";
        if(lstDynParams.size()==0) lstDynParams=null;
        this.addSqlAndParams(insertsql,lstDynParams);
    }
    
    private void parseUpdateSql(String sql)
    {
        String updatesql=sql.trim();
        int idxleft=updatesql.indexOf("(");
        if(idxleft<0)
        {
            this.addSqlAndParams(sql,null);
            return;
        }
        StringBuffer sqlBuffer=new StringBuffer();
        sqlBuffer.append(updatesql.substring(0,idxleft)).append(" set ");
        String whereclause=null;
        int idxWhere=updatesql.toLowerCase().indexOf(" where ");
        if(idxWhere>0)
        {
            whereclause=updatesql.substring(idxWhere).trim();
            whereclause=whereclause.substring("where".length()).trim();
            updatesql=updatesql.substring(0,idxWhere).trim();
        }
        int idxright=updatesql.lastIndexOf(")");
        if(idxright!=updatesql.length()-1)
        {
            this.addSqlAndParams(sql,null);
            return;
        }
        List<Map<String,Object>> lstUpdateColAndParams=parseSqlUpdateClause(updatesql.substring(idxleft+1,idxright));
        List<SqlParamBean> lstDynParams=new ArrayList<SqlParamBean>();
        Entry<String,Object> entryTmp;
        for(Map<String,Object> mColAndParamTmp:lstUpdateColAndParams)
        {
            entryTmp=mColAndParamTmp.entrySet().iterator().next();
            sqlBuffer.append(entryTmp.getKey()).append("=");
            if(entryTmp.getValue() instanceof SqlParamBean)
            {//动态参数
                sqlBuffer.append("?");
                lstDynParams.add((SqlParamBean)entryTmp.getValue());
            }else
            {
                if(entryTmp.getValue()==null)
                {
                    sqlBuffer.append("''");
                }else
                {
                    sqlBuffer.append(entryTmp.getValue().toString());
                }
            }
            sqlBuffer.append(",");
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
        {
            sqlBuffer=sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        }
        if(whereclause!=null&&!whereclause.trim().equals(""))
        {
            whereclause=parseSqlWhereClause(whereclause,lstDynParams);
            sqlBuffer.append(" where ").append(whereclause);
        }
        if(lstDynParams.size()==0) lstDynParams=null;
        this.addSqlAndParams(sqlBuffer.toString(),lstDynParams);
    }
    
    private void parseDeleteSql(String sql)
    {
        String deletesql=sql.trim();
        int idxwhere=deletesql.toLowerCase().indexOf(" where ");
        if(idxwhere<=0)
        {
            this.addSqlAndParams(sql,null);
        }else
        {
            List<SqlParamBean> lstDynParams=new ArrayList<SqlParamBean>();
            StringBuffer sqlBuffer=new StringBuffer();
            sqlBuffer.append(deletesql.substring(0,idxwhere+" where ".length()));
            sqlBuffer.append(parseSqlWhereClause(deletesql.substring(idxwhere+" where ".length()),lstDynParams));
            if(lstDynParams.size()==0) lstDynParams=null;
            this.addSqlAndParams(sqlBuffer.toString(),lstDynParams);
        }
    }
    
    private List<Map<String,Object>> parseSqlUpdateClause(String updateclause)
    {
        List<Map<String,Object>> lstUpdateColAndParams=new ArrayList<Map<String,Object>>();
        if(updateclause==null||updateclause.trim().equals("")) return lstUpdateColAndParams;
        updateclause=updateclause.trim();
        List<String> lstUpdatCols=Tools.parseStringToList(updateclause,',','\'');
        String colnameTmp;
        String colvalTmp;
        SqlParamBean spbeanTmp;
        for(String colTmp:lstUpdatCols)
        {
            if(colTmp==null) continue;
            colTmp=colTmp.trim();
            int idxEqual=colTmp.indexOf("=");
            if(idxEqual<=0) continue;
            colnameTmp=colTmp.substring(0,idxEqual).trim();//字段名
            colvalTmp=colTmp.substring(idxEqual+1).trim();
            if(colnameTmp.equals("")) continue;
            Map<String,Object> mParamTmp=new HashMap<String,Object>();
            lstUpdateColAndParams.add(mParamTmp);
            spbeanTmp=getCurrentDateParamBean(colvalTmp);
            if(spbeanTmp!=null)
            {
                mParamTmp.put(colnameTmp,spbeanTmp);
                continue;
            }
            spbeanTmp=getDynValueParamBean(colvalTmp,"request");//看一下是不是配置为从request中取数据
            if(spbeanTmp!=null)
            {
                mParamTmp.put(colnameTmp,spbeanTmp);
                continue;
            }
            spbeanTmp=getDynValueParamBean(colvalTmp,"session");
            if(spbeanTmp!=null)
            {
                mParamTmp.put(colnameTmp,spbeanTmp);
                continue;
            }
            spbeanTmp=getDynValueParamBean(colvalTmp,"@");
            if(spbeanTmp!=null)
            {//是从用户传过来的数据列表中取参数数据
                mParamTmp.put(colnameTmp,spbeanTmp);
                continue;
            }
            if(colvalTmp.toLowerCase().equals("uuid{}"))
            {
                spbeanTmp=new SqlParamBean();
                spbeanTmp.setParamname("uuid{}");
                spbeanTmp.setParamTypeObj(Config.getInstance().getDataTypeByClass(VarcharType.class));
                mParamTmp.put(colnameTmp,spbeanTmp);
            }else if(Tools.isDefineKey("sequence",colvalTmp))
            {
                mParamTmp.put(colnameTmp,Tools.getRealKeyByDefine("sequence",colvalTmp)+".nextval");
            }else
            {
                mParamTmp.put(colnameTmp,colvalTmp);
            }
        }
        return lstUpdateColAndParams;
    }
    
    private SqlParamBean getCurrentDateParamBean(String paramvalue)
    {
        if(paramvalue==null||paramvalue.trim().equals("")) return null;
        if(!Tools.isDefineKey("now",paramvalue.trim())) return null;
        String paramdatatype=Tools.getRealKeyByDefine("now",paramvalue.trim()).trim();
        SqlParamBean spbeanTmp=null;
        if(paramdatatype.equals(""))
        {
            spbeanTmp=new SqlParamBean();
            spbeanTmp.setParamname("now{}");
            spbeanTmp.setParamTypeObj(Config.getInstance().getDataTypeByClass(DateType.class));
        }else
        {
            if(paramdatatype.startsWith("[")&&paramdatatype.endsWith("]"))
            {//数据类型用[]括住了，则去掉[]
                paramdatatype=paramdatatype.substring(1,paramdatatype.length()-1).trim();
            }
            IDataType typeObj=getDataTypeObj(paramdatatype);
            if(!(typeObj instanceof AbsDateTimeType))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句的参数"+paramvalue+"必须为日期类型");
            }
            spbeanTmp=new SqlParamBean();
            spbeanTmp.setParamname("now{}");
            spbeanTmp.setParamTypeObj(typeObj);
        }
        return spbeanTmp;
    }
    
    private SqlParamBean getDynValueParamBean(String paramvalue,String type)
    {
        if(paramvalue==null||paramvalue.trim().equals("")) return null;
        paramvalue=paramvalue.trim();
        if(!Tools.isDefineKey(type,paramvalue)) return null;
        String realparamvalue=Tools.getRealKeyByDefine(type,paramvalue).trim();
        if(realparamvalue.equals(""))
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句的参数"+paramvalue+"不合法");
        }
        int idx1=realparamvalue.indexOf("[");
        int idx2=realparamvalue.indexOf("]");
        IDataType dataTypeObj=Config.getInstance().getDataTypeByClass(VarcharType.class);
        if(idx1==0&&idx2>0&&idx2!=realparamvalue.length()-1)
        {
            dataTypeObj=this.getDataTypeObj(realparamvalue.substring(1,idx2).trim());
            if(dataTypeObj==null)
            {
                throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句的参数"+paramvalue+"的数据类型无效");
            }
            realparamvalue=realparamvalue.substring(idx2+1).trim();
        }
        if(realparamvalue.equals(""))
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句的参数"+paramvalue+"不合法");
        }
        SqlParamBean spbeanTmp=new SqlParamBean();
        if(type.equals("@"))
        {//这种是默认的取从客户端传入的数据的标识，所以不用在paramname上加@{}括住
            spbeanTmp.setParamname(realparamvalue);
        }else
        {
            spbeanTmp.setParamname(type+"{"+realparamvalue+"}");
        }
        spbeanTmp.setParamTypeObj(dataTypeObj);
        return spbeanTmp;
    }
    
    private String parseSqlWhereClause(String whereclause,List<SqlParamBean> lstDynParams)
    {
        if(whereclause==null||whereclause.trim().equals("")) return "";
        whereclause=whereclause.trim();
        whereclause=Tools.replaceCharacterInQuote(whereclause,'{',"$_LEFTBRACKET_$",true);
        whereclause=Tools.replaceCharacterInQuote(whereclause,'}',"$_RIGHTBRACKET_$",true);
        Map<String,SqlParamBean> mDynParamsAndPlaceHolder=new HashMap<String,SqlParamBean>();
        whereclause=parseDynParamsInWhereClause(whereclause,mDynParamsAndPlaceHolder,"request");
        whereclause=parseDynParamsInWhereClause(whereclause,mDynParamsAndPlaceHolder,"session");
        whereclause=parseDynParamsInWhereClause(whereclause,mDynParamsAndPlaceHolder,"@");//解析条件子句中的@{...}动态参数
        whereclause=convertPlaceHolderToRealParams(whereclause,mDynParamsAndPlaceHolder,lstDynParams);
        
        whereclause=Tools.replaceAll(whereclause,"$_LEFTBRACKET_$","{");
        whereclause=Tools.replaceAll(whereclause,"$_RIGHTBRACKET_$","}");
        return whereclause;
    }
    
    private String parseDynParamsInWhereClause(String whereclause,Map<String,SqlParamBean> mDynParamsAndPlaceHolder,String paramtype)
    {
        int idx=whereclause.indexOf(paramtype+"{");
        String strStart=null;
        String strDynValue=null;
        String strEnd=null;
        SqlParamBean spbeanTmp;
        String placeHolderTmp;
        int placeholderIdxTmp=0;
        while(idx>=0)
        {
            strStart=whereclause.substring(0,idx).trim();
            strEnd=whereclause.substring(idx);
            idx=strEnd.indexOf("}");
            if(idx<0)
            {
                throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"下的按钮"+this.name+"失败，为它配置的更新数据SQL语句条件子句"+whereclause+"中动态参数没有闭合的}");
            }
            strDynValue=strEnd.substring(0,idx+1);
            strEnd=strEnd.substring(idx+1).trim();//存放type{...}后面的部分
            spbeanTmp=getDynValueParamBean(strDynValue,paramtype);
            if((strStart.endsWith("%")&&strStart.substring(0,strStart.length()-1).trim().toLowerCase().endsWith(" like"))
                    ||strStart.toLowerCase().endsWith(" like"))
            {
                if(strStart.endsWith("%"))
                {
                    strStart=strStart.substring(0,strStart.length()-1);
                    spbeanTmp.setHasLeftPercent(true);
                }
                if(strEnd.startsWith("%"))
                {
                    strEnd=strEnd.substring(1);
                    spbeanTmp.setHasRightPercent(true);
                }
            }
            placeHolderTmp="[PLACE_HOLDER_"+paramtype+"_"+placeholderIdxTmp+"]";
            mDynParamsAndPlaceHolder.put(placeHolderTmp,spbeanTmp);//存放到mDynParamsAndPlaceHolder中，以便后面解析时使用
            whereclause=strStart+placeHolderTmp+strEnd;
            idx=whereclause.indexOf(paramtype+"{");
            placeholderIdxTmp++;
        }
        return whereclause;
    }
    
    private String convertPlaceHolderToRealParams(String whereclause,Map<String,SqlParamBean> mDynParamsAndPlaceHolder,List<SqlParamBean> lstDynParams)
    {
        if(mDynParamsAndPlaceHolder==null||mDynParamsAndPlaceHolder.size()==0) return whereclause;
        int idxPlaceHolderStart=whereclause.indexOf("[PLACE_HOLDER_");
        String strStart=null;
        String strEnd=null;
        String placeHolderTmp;
        while(idxPlaceHolderStart>=0)
        {
            strStart=whereclause.substring(0,idxPlaceHolderStart);
            strEnd=whereclause.substring(idxPlaceHolderStart);
            int idxPlaceHolderEnd=strEnd.indexOf("]");
            placeHolderTmp=strEnd.substring(0,idxPlaceHolderEnd+1);
            strEnd=strEnd.substring(idxPlaceHolderEnd+1);
            lstDynParams.add(mDynParamsAndPlaceHolder.get(placeHolderTmp));
            whereclause=strStart+" ? "+strEnd;
            idxPlaceHolderStart=whereclause.indexOf("[PLACE_HOLDER_");
        }
        return whereclause;
    }
    
    private IDataType getDataTypeObj(String configtype)
    {
        if(configtype==null||configtype.trim().equals("")) return null;
        configtype=configtype.trim();
        int idx1=configtype.indexOf("{");
        int idx2=configtype.lastIndexOf("}");
        String extrainfo=null;
        if(idx1>0&&idx2==configtype.length()-1)
        {//指定了数据类型的参数，比如日期类型指定了日期格式
            extrainfo=configtype.substring(idx1+1,idx2).trim();
            configtype=configtype.substring(0,idx1);
        }
        IDataType dataTypeObj=Config.getInstance().getDataTypeByName(configtype);
        if(dataTypeObj==null) return null;
        if(extrainfo!=null&&!extrainfo.trim().equals(""))
        {
            dataTypeObj=dataTypeObj.setUserConfigString(extrainfo);
        }
        return dataTypeObj;
    }
    
    private void addSqlAndParams(String sql,List<SqlParamBean> lstParams)
    {
        if(this.lstSqlAndParams==null) this.lstSqlAndParams=new ArrayList<Map<String,List<SqlParamBean>>>();
        Map<String,List<SqlParamBean>> mSqls=new HashMap<String,List<SqlParamBean>>();
        this.lstSqlAndParams.add(mSqls);
        mSqls.put(sql,lstParams);
    }
    
    public AbsButtonType clone(ReportBean rbean)
    {
        ServerSQLActionButton buttonNew=(ServerSQLActionButton)super.clone(rbean);
        if(this.lstSqlAndParams!=null)
        {
            List<Map<String,List<SqlParamBean>>> lstParamsNew=new ArrayList<Map<String,List<SqlParamBean>>>();
            buttonNew.setLstSqlAndParams(lstParamsNew);
            for(Map<String,List<SqlParamBean>> mTmp:this.lstSqlAndParams)
            {
                if(mTmp==null) continue;
                Map<String,List<SqlParamBean>> mNew=new HashMap<String,List<SqlParamBean>>();
                lstParamsNew.add(mNew);
                List<SqlParamBean> lstParamBeanTmp;
                for(Entry<String,List<SqlParamBean>> entryTmp:mTmp.entrySet())
                {
                    lstParamBeanTmp=entryTmp.getValue();
                    if(lstParamBeanTmp==null)  
                    {
                        mNew.put(entryTmp.getKey(),null);
                    }else
                    {
                        List<SqlParamBean> lstParamBeanNew=new ArrayList<SqlParamBean>();
                        mNew.put(entryTmp.getKey(),lstParamBeanNew);
                        for(SqlParamBean spbTmp:lstParamBeanTmp)
                        {
                            lstParamBeanNew.add((SqlParamBean)spbTmp.clone());
                        }
                    }
                }
            }
        }
        return buttonNew;
    }

    private class SqlParamBean implements Cloneable
    {
        private String paramname;
        
        private IDataType paramTypeObj;
        
        private boolean hasLeftPercent;

        private boolean hasRightPercent;

        public String getParamname()
        {
            return paramname;
        }

        public void setParamname(String paramname)
        {
            this.paramname=paramname;
        }

        public void setHasLeftPercent(boolean hasLeftPercent)
        {
            this.hasLeftPercent=hasLeftPercent;
        }

        public void setHasRightPercent(boolean hasRightPercent)
        {
            this.hasRightPercent=hasRightPercent;
        }
        
        public void setParamTypeObj(IDataType paramTypeObj)
        {
            this.paramTypeObj=paramTypeObj;
        }

        public IDataType getParamTypeObj()
        {
            return paramTypeObj;
        }

        public String getParamValue(ReportRequest rrequest,Map<String,String> mData)
        {
            String paramvalue=null;
            if(paramname!=null&&!paramname.equals(""))
            {
                if(paramname.equals("uuid{}"))
                {//产生一个guid值
                    paramvalue=UUIDGenerator.generateID();
                }else if(paramname.equals("now{}"))
                {
                    SimpleDateFormat sdf=new SimpleDateFormat(((AbsDateTimeType)paramTypeObj).getDateformat());
                    paramvalue=sdf.format(new Date());
                }else if(Tools.isDefineKey("request",paramname)||Tools.isDefineKey("session",paramname))
                {//从request/session中取值
                    paramvalue=WabacusAssistant.getInstance().getRequestSessionValue(rrequest,paramname,"");
                }else if(mData!=null)
                {
                    paramvalue=mData.get(paramname);
                }
            }
            if(paramvalue==null) paramvalue="";
            if(this.hasLeftPercent) paramvalue="%"+paramvalue;
            if(this.hasRightPercent) paramvalue=paramvalue+"%";
            return paramvalue;
        }

        protected Object clone() 
        {
            try
            {
                return super.clone();
            }catch(CloneNotSupportedException e)
            {
                e.printStackTrace();
                return null;
            }
        }
        
    }
}
