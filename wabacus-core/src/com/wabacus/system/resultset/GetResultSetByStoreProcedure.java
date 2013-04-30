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
package com.wabacus.system.resultset;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import oracle.jdbc.driver.OracleTypes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.Oracle;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.system.inputbox.AutoCompleteConfigBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class GetResultSetByStoreProcedure implements ISQLType
{
    private static Log log=LogFactory.getLog(GetResultSetByStoreProcedure.class);

    private int rtnValueIndex;
    
    private CallableStatement callablestmt;
    
    public Object getResultSet(ReportRequest rrequest,ReportBean reportbean,Object typeObj,String sql,List<ConditionBean> lstConditionBeans)
    {
        throw new WabacusRuntimeException("如果查询数据的数据库脚步本为存储过程，则不支持此方法");
    }

    public Object getResultSet(ReportRequest rrequest,AbsReportType reportObj,Object typeObj,String sql)
    {
        StringBuffer systemParamsBuf=new StringBuffer();
        ReportBean rbean=reportObj.getReportBean();
        if(sql==null||sql.trim().equals(""))
        {
            throw new WabacusRuntimeException("调用报表"+rbean.getPath()+"的存储过程失败，没有传入在存储过程中要执行的SQL语句");
        }
        systemParamsBuf.append("{[(<exec_sql:"+sql+">)]}");
        String filterwhere=ListReportAssistant.getInstance().getFilterConditionExpression(rrequest,rbean);
        if(filterwhere!=null&&!filterwhere.trim().equals(""))
        {
            systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
        }
        Object[] resultObj=doGetResultSet(rrequest,rbean,reportObj,systemParamsBuf);
        if(resultObj==null||resultObj.length==0) return null;
        return resultObj[0];
    }

    public Object getResultSet(ReportRequest rrequest,AbsReportType reportObj)
    {
        ReportBean rbean=reportObj.getReportBean();
        StringBuffer systemParamsBuf=new StringBuffer();
        Object[] resultObj=null;
        if(rrequest.getActiontype().equalsIgnoreCase(Consts.GETFILTERDATALIST_ACTION))
        {
            String columnname=rrequest.getStringAttribute("FILTER_COLUMNNAME","");//过滤列的column
            if(columnname.equals("")) throw new WabacusRuntimeException("没有取到要获取过滤数据的字段名");
            systemParamsBuf.append("{[(<filter_column:"+columnname+">)]}");
            String selectdatatype=rrequest.getStringAttribute(rbean.getId()+"_WABACUS_GETDATA","all");
            if(selectdatatype.equals("selected"))
            {
                String filterwhere=ListReportAssistant.getInstance().getFilterConditionExpression(rrequest,rbean);
                if(filterwhere!=null&&!filterwhere.trim().equals(""))
                {
                    systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
                }
            }
            resultObj=doGetResultSet(rrequest,rbean,rrequest.getAttribute(rbean.getId()+"_WABACUS_FILTERBEAN"),systemParamsBuf);
        }else if(rrequest.getActiontype().equalsIgnoreCase(Consts.GETAUTOCOMPLETEDATA_ACTION))
        {
            AutoCompleteConfigBean accbean=rrequest.getAutoCompleteSourceInputBoxObj().getAutocompleteBean();
            systemParamsBuf.append("{[(<autocomplete_columns:"+accbean.getAutoCompleteColumns()+">)]}");
            systemParamsBuf.append("{[(<autocomplete_conditionexpression:"+rrequest.getStringAttribute("COL_CONDITION_EXPRESSION","")+">)]}");
            resultObj=doGetResultSet(rrequest,rbean,accbean,systemParamsBuf);
        }else
        {
           if(reportObj instanceof AbsListReportType)
           {
                AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrdbean!=null&&alrdbean.getLstRowgroupColsColumn()!=null)
                {
                    StringBuffer rowGroupColsBuf=new StringBuffer();
                    for(String rowgroupColTmp:alrdbean.getLstRowgroupColsColumn())
                    {
                        if(rowgroupColTmp!=null&&!rowgroupColTmp.trim().equals("")) rowGroupColsBuf.append(rowgroupColTmp).append(",");
                    }
                    if(rowGroupColsBuf.length()>0)
                    {
                        if(rowGroupColsBuf.charAt(rowGroupColsBuf.length()-1)==',') rowGroupColsBuf.deleteCharAt(rowGroupColsBuf.length()-1);
                        systemParamsBuf.append("{[(<rowgroup_cols:"+rowGroupColsBuf.toString()+">)]}");
                    }
                }
            }
            String[] orderbys=(String[])rrequest.getAttribute(rbean.getId(),"ORDERBYARRAY");
            if(orderbys!=null&&orderbys.length==2)
            {//用户点击了列排序功能
                systemParamsBuf.append("{[(<dynamic_orderby:"+orderbys[0]+" "+orderbys[1]+">)]}");
            }
            String filterwhere=ListReportAssistant.getInstance().getFilterConditionExpression(rrequest,rbean);
            if(filterwhere!=null&&!filterwhere.trim().equals(""))
            {
                systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
            }
            CacheDataBean cdb=rrequest.getCdb(rbean.getId());
            int pagesize=cdb.isLoadAllReportData()?-1:cdb.getPagesize();
            systemParamsBuf.append("{[(<pagesize:"+pagesize+">)]}");
            if(pagesize>0)
            {
                systemParamsBuf.append("{[(<refreshNavigateType:"+(cdb.getRefreshNavigateInfoType()<0)+">)]}");
                int pageno=cdb.getFinalPageno();
                systemParamsBuf.append("{[(<startrownum:"+((pageno-1)*pagesize)+">)]}");
                systemParamsBuf.append("{[(<endrownum:"+(pageno*pagesize)+">)]}");//本次要查询的结束记录号
                systemParamsBuf.append("{[(<pagesize:"+pagesize+">)]}");
            }
            resultObj=doGetResultSet(rrequest,rbean,reportObj,systemParamsBuf);
            if(pagesize>0&&cdb.getRefreshNavigateInfoType()<=0)
            {
                this.callablestmt=(CallableStatement)resultObj[1];
                this.rtnValueIndex=(Integer)resultObj[2];
            }
        }
        if(resultObj==null||resultObj.length==0) return null;
        return resultObj[0];
    }

    private Object[] doGetResultSet(ReportRequest rrequest,ReportBean rbean,Object typeObj,StringBuffer systemParamsBuf)
    {
        log.debug(systemParamsBuf.toString());
        String procedure=rbean.getSbean().getValue();
        if(rbean.getInterceptor()!=null)
        {
            Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,typeObj,procedure);
            if(obj==null) return null;
            if(obj instanceof List||obj instanceof ResultSet)
            {
                throw new WabacusRuntimeException("执行报表"+rbean.getPath()+"的加载数据拦截器失败，当前报表采用存储过程加载数据，不能在拦截方法中返回ResultSet或List对象");
            }
            if(!(obj instanceof String))
            {
                throw new WabacusRuntimeException("执行报表"+rbean.getPath()+"的加载数据拦截器失败，返回的数据类型"+obj.getClass().getName()+"不合法");
            }
            procedure=(String)obj;
        }
        if(Config.show_sql)
        {
            log.info("Execute sql: "+procedure);
        }
        CallableStatement cstmt=null;
        try
        {
            cstmt=rrequest.getConnection(rbean.getSbean().getDatasource()).prepareCall(procedure);
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            VarcharType varcharObj=(VarcharType)Config.getInstance().getDataTypeByClass(VarcharType.class);
            int idx=1;
            if(rbean.getSbean().getLstStoreProcedureParams()!=null&&rbean.getSbean().getLstStoreProcedureParams().size()>0)
            {
                ConditionBean cbeanTmp;
                for(String paramTmp:rbean.getSbean().getLstStoreProcedureParams())
                {
                    if(Tools.isDefineKey("request",paramTmp)||Tools.isDefineKey("session",paramTmp))
                    {//从request/session中取值
                        varcharObj.setPreparedStatementValue(idx,WabacusAssistant.getInstance().getRequestSessionValue(rrequest,paramTmp,""),cstmt,
                                dbtype);
                    }else if(Tools.isDefineKey("condition",paramTmp))
                    {
                        cbeanTmp=rbean.getSbean().getConditionBeanByName(Tools.getRealKeyByDefine("condition",paramTmp));
                        if(cbeanTmp.getIterator()>1||cbeanTmp.getCcolumnsbean()!=null||cbeanTmp.getCvaluesbean()!=null)
                        {//如果有多套输入框、多个比较列、多个条件表达式，则参数一定是字符串类型
                            varcharObj.setPreparedStatementValue(idx,cbeanTmp.getConditionValueForSP(rrequest),cstmt,dbtype);
                        }else
                        {
                            cbeanTmp.getDatatypeObj().setPreparedStatementValue(idx,cbeanTmp.getConditionValueForSP(rrequest),cstmt,dbtype);
                        }
                    }else
                    {
                        varcharObj.setPreparedStatementValue(idx,paramTmp,cstmt,dbtype);
                    }
                    idx++;
                }
            }
            cstmt.setString(idx++,systemParamsBuf.toString());

            cstmt.registerOutParameter(idx++,java.sql.Types.VARCHAR);
            if(dbtype instanceof Oracle)
            {
                cstmt.registerOutParameter(idx,OracleTypes.CURSOR);
            }
            rrequest.addUsedStatement(cstmt);
            cstmt.executeQuery();
            ResultSet rs=null;
            if(dbtype instanceof Oracle)
            {//如果是oracle，则要返回记录集
                rs=(ResultSet)cstmt.getObject(idx);
            }else
            {
                rs=cstmt.getResultSet();
            }
            return new Object[] { rs, cstmt, idx-1 };
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取报表"+rbean.getPath()+"数据时执行SQL："+procedure+"失败",e);
        }
    }
    
    public void readAndCalNavigateData(ReportBean rbean,CacheDataBean cdb)
    {
        if(cdb.isLoadAllReportData()) return;
        if(this.callablestmt==null||this.rtnValueIndex<=0)
        {
            this.callablestmt=null;
            return;
        }
        String rtnVal=null;
        try
        {
            rtnVal=this.callablestmt.getString(this.rtnValueIndex);
        }catch(SQLException e1)
        {
           throw new WabacusRuntimeException("从存储过程中读取报表"+rbean.getPath()+"的返回记录数失败",e1);
        }finally
        {
            this.callablestmt=null;
        }
        if(rtnVal==null||rtnVal.trim().equals("")) rtnVal="0";
        int recordcount=0;
        try
        {
            recordcount=Integer.parseInt(rtnVal);
        }catch(NumberFormatException e)
        {
            recordcount=0;
            log.warn("从加载报表"+rbean.getPath()+"数据的存储过程中取出的记录数"+rtnVal+"不是有效数字",e);
        }
        cdb.setRecordcount(recordcount);
        cdb.setPagecount(ReportAssistant.getInstance().calPageCount(cdb.getPagesize(),cdb.getRecordcount()));
    }
}
