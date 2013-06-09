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
package com.wabacus.system.assistant;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.ResourceUtils;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.condition.ConditionInSqlBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.SQLSERVER2K;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.buttons.IButtonClickeventGenerate;
import com.wabacus.system.component.application.report.CrossStatisticListReportType;
import com.wabacus.system.component.application.report.EditableDetailReportType;
import com.wabacus.system.component.application.report.EditableListFormReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.format.IFormat;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.OptionBean;
import com.wabacus.system.inputbox.SelectBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.system.resultset.GetResultSetByStoreProcedure;
import com.wabacus.system.resultset.ISQLType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class ReportAssistant
{
    private static Log log=LogFactory.getLog(ReportAssistant.class);

    private final static ReportAssistant instance=new ReportAssistant();

    protected ReportAssistant()
    {}

    public static ReportAssistant getInstance()
    {
        return instance;
    }

    public String mixDynorderbyAndRowgroupCols(ReportBean rbean,String dynorderby)
    {
        List<String> lstTemp=Tools.parseStringToList(dynorderby," ");
        Map<String,String> mOldDynOrderBy=new HashMap<String,String>();
        if(lstTemp.size()!=2)
        {
            throw new WabacusRuntimeException("查询报表"+rbean.getPath()+"数据失败，传入的动态排序子句"+dynorderby+"不合法");
        }else
        {
            mOldDynOrderBy.put(lstTemp.get(0).trim(),lstTemp.get(1).trim());
        }
        StringBuffer orderbybuf=new StringBuffer();
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrdbean!=null&&alrdbean.getRowGroupColsNum()>0)
        {
            List<Map<String,String>> lstRowgroupColsAndOrders=alrdbean.getLstRowgroupColsAndOrders();
            if(lstRowgroupColsAndOrders!=null&&lstRowgroupColsAndOrders.size()>0)
            {
                String ordercol;
                for(Map<String,String> mOrderCols:lstRowgroupColsAndOrders)
                {
                    if(mOrderCols==null||mOrderCols.size()==0) continue;
                    ordercol=mOrderCols.keySet().iterator().next();
                    if(mOldDynOrderBy!=null&&mOldDynOrderBy.containsKey(ordercol))
                    {
                        orderbybuf.append(ordercol).append(" ").append(mOldDynOrderBy.get(ordercol)).append(",");
                        mOldDynOrderBy=null;
                    }else
                    {
                        orderbybuf.append(ordercol).append(" ").append(mOrderCols.get(ordercol)).append(",");
                    }
                }
            }
        }
        if(mOldDynOrderBy!=null)
        {
            orderbybuf.append(dynorderby);
        }
        if(orderbybuf.charAt(orderbybuf.length()-1)==',')
        {
            orderbybuf.deleteCharAt(orderbybuf.length()-1);
        }
        return orderbybuf.toString();
    }

    public String parseRuntimeSqlAndCondition(ReportRequest rrequest,ReportBean rbean,String sql,List<String> lstConditionValues,
            List<IDataType> lstConditionTypes)
    {
        List<ConditionBean> lstConditionBeans=rbean.getSbean().getLstConditions();
        if(lstConditionBeans==null||lstConditionBeans.size()==0)
        {
            if(sql!=null&&sql.indexOf("{#condition#}")>0)
            {
                sql=removeConditionPlaceHolderFromSql(rbean,sql,"{#condition#}");
            }
            return sql;//没有动态条件
        }
        List<ConditionInSqlBean> lstConditionInSqlBeans=rbean.getSbean().getLstConditionInSqlBeans();
        if(lstConditionInSqlBeans==null||lstConditionInSqlBeans.size()==0)
        {
            sql=addDynamicConditionExpressionsToSql(rrequest,rbean,sql,rbean.getSbean().getLstConditions(),lstConditionValues,lstConditionTypes);
        }else
        {
            for(ConditionInSqlBean conbeanInSqlTmp:lstConditionInSqlBeans)
            {
                sql=conbeanInSqlTmp.parseConditionInSql(rrequest,sql,lstConditionValues,lstConditionTypes);
            }
        }
        return sql;
    }

    public String addDynamicConditionExpressionsToSql(ReportRequest rrequest,ReportBean rbean,String sql,List<ConditionBean> lstConditionBeans,
            List<String> lstConditionValues,List<IDataType> lstConditionTypes)
    {
        if(lstConditionBeans==null||lstConditionBeans.size()==0) return sql;
        StringBuffer dynConditionsBuf=new StringBuffer();
        String conditionExpressionTmp;
        for(ConditionBean conbean:lstConditionBeans)
        {
            conditionExpressionTmp=conbean.getConditionExpressionAndParams(rrequest,lstConditionValues,lstConditionTypes);
            if(conditionExpressionTmp==null||conditionExpressionTmp.trim().equals("")) continue;//没有在<condition/>中配置条件表达式（可能是通过#name#的形式直接在sql语句中指定条件）
            if(dynConditionsBuf.length()==0)
            {
                dynConditionsBuf.append(conditionExpressionTmp);
            }else
            {
                dynConditionsBuf.append(" and ").append(conditionExpressionTmp);
            }
        }
        String dynconditions=dynConditionsBuf.toString();
        if(dynconditions.trim().equals(""))
        {
            
            sql=removeConditionPlaceHolderFromSql(rbean,sql,"{#condition#}");
        }else
        {
            sql=Tools.replaceAll(sql,"{#condition#}"," ("+dynconditions+") ");
        }
        return sql;
    }

    public String removeConditionPlaceHolderFromSql(ReportBean rbean,String sql,String placeholder)
    {
        int idx=sql.indexOf(placeholder);
        if(idx<0) return sql;
        String sql1=sql.substring(0,idx).trim();//sql语句中此条件表达式占位符之前的部分
        String sql2=sql.substring(idx+placeholder.length()).trim();
        while(sql1.endsWith("(")&&sql2.startsWith(")"))
        {
            sql1=sql1.substring(0,sql1.length()-1).trim();
            sql2=sql2.substring(1).trim();
        }
        if(sql1.endsWith("("))
        {//sql为select xxx where 条件1 and/or ({当前条件} and/or 其它条件) ...形式，此时{当前条件}后面只有可能是and或or
            if(!sql2.toLowerCase().startsWith("and ")&&!sql2.toLowerCase().startsWith("or ")||sql2.indexOf(")")<0)
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"中的sql语句"+sql+"格式不对，"+placeholder+"所在位置不合法");
            }
            if(sql2.toLowerCase().startsWith("and "))
            {
                sql2=sql2.substring(3);
            }else if(sql2.toLowerCase().startsWith("or "))
            {
                sql2=sql2.substring(2);
            }
        }else if(sql2.startsWith(")"))
        {//sql为select xxx where 条件1 and/or (其它条件 and/or {当前条件}) ...形式或者select * from table where id in(select id from table2 where {当前条件})形式
            if(!sql1.toLowerCase().endsWith(" and")&&!sql1.toLowerCase().endsWith(" or")&&!sql1.toLowerCase().endsWith(" where")||sql1.indexOf("(")<0)
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"中的sql语句"+sql+"格式不对，"+placeholder+"所在位置不合法");
            }
            if(sql1.toLowerCase().endsWith(" and"))
            {
                sql1=sql1.substring(0,sql1.length()-3);
            }else if(sql1.toLowerCase().endsWith(" or"))
            {
                sql1=sql1.substring(0,sql1.length()-2);
            }else if(sql1.toLowerCase().endsWith(" where"))
            {
                sql1=sql1.substring(0,sql1.length()-5);
            }
        }else
        {//sql为select xxx where/and/or {当前条件} and/or/其它非条件值或空
            if(sql1.toLowerCase().endsWith(" where"))
            {//sql为select xxx where {当前条件} and/or/其它非条件值或空
                if(sql2.toLowerCase().startsWith("or "))
                {
                    sql2=sql2.substring(2);
                }else if(sql2.toLowerCase().startsWith("and "))
                {
                    sql2=sql2.substring(3);
                }else
                {
                    sql1=sql1.substring(0,sql1.length()-5);
                }
            }else if(sql1.toLowerCase().endsWith(" and"))
            {//sql为select xxx where 其它条件 and {当前条件} and/or/其它非条件值或空
                sql1=sql1.substring(0,sql1.length()-3);
            }else if(sql1.toLowerCase().endsWith(" or"))
            {//sql为select xxx where 其它条件 or {当前条件} and/or/其它非条件值或空
                if(sql2.toLowerCase().startsWith("and "))
                {//当前条件与后面条件表达式的关系是and，则去掉and
                    sql2=sql2.substring(3);
                }else
                {
                    sql1=sql1.substring(0,sql1.length()-2);
                }
            }
        }
        return sql1+" "+sql2;
    }

    public List loadAllDataFromDB(ReportRequest rrequest,AbsReportType reportObj,ISQLType sqlTypeObj)
    {
        CacheDataBean cdb=rrequest.getCdb(reportObj.getReportBean().getId());
        int maxrecordcount=cdb.getMaxrecordcount();
        if(maxrecordcount<=0) maxrecordcount=-1;
        List lstData=null;
        try
        {
            Object objResult=sqlTypeObj.getResultSet(rrequest,reportObj);
            if(objResult==null) return null;
            if(objResult instanceof ResultSet)
            {
                ResultSet rs=(ResultSet)objResult;
                lstData=parseDataIntoBean(reportObj,rs,maxrecordcount,rrequest);
                rs.close();
            }else if(objResult instanceof List)
            {
                lstData=(List)objResult;
                if(maxrecordcount>0&&lstData.size()>maxrecordcount)
                {
                    while(lstData.size()>maxrecordcount)
                    {
                        lstData.remove(lstData.size()-1);
                    }
                }
            }else
            {
                throw new WabacusRuntimeException("获取数据时，返回的结果集："+objResult.getClass().getName()+"为非法结果类型");
            }
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("加载报表数据失败",e);
        }
        if(lstData!=null)
        {
            cdb.setRecordcount(lstData.size());
            if(cdb.getRecordcount()>0) cdb.setPagecount(1);
        }
        return lstData;
    }

    public List loadOneRowDataFromDB(ReportRequest rrequest,AbsReportType reportObj,ISQLType sqlTypeObj,int rownum)
    {
        List lstData=null;
        try
        {
            Object objResult=sqlTypeObj.getResultSet(rrequest,reportObj);
            if(objResult==null) return null;
            if(objResult instanceof ResultSet)
            {
                ResultSet rs=(ResultSet)objResult;
                int i=0;
                while(i<rownum&&rs.next())
                    i++;
                lstData=parseDataIntoBean(reportObj,rs,1,rrequest);
                rs.close();
            }else if(objResult instanceof List)
            {//直接返回数据集
                lstData=(List)objResult;
            }else
            {
                throw new WabacusRuntimeException("获取数据时，返回的结果集："+objResult.getClass().getName()+"为非法结果类型");
            }
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("加载报表数据失败",e);
        }
        return lstData;
    }

    public List loadOnePageDataFromDB(ReportRequest rrequest,AbsReportType reportObj,ISQLType sqlTypeObj)
    {
        List lstData=null;
        try
        {
            ReportBean rbean=reportObj.getReportBean();
            Object objResult=sqlTypeObj.getResultSet(rrequest,reportObj);
            if(objResult==null) return null;
            CacheDataBean cdb=rrequest.getCdb(rbean.getId());
            int pagesize=cdb.getPagesize();
            if(objResult instanceof ResultSet)
            {
                ResultSet rs=(ResultSet)objResult;
                lstData=parseDataIntoBean(reportObj,rs,pagesize,rrequest);
                rs.close();
                if(sqlTypeObj instanceof GetResultSetByStoreProcedure) ((GetResultSetByStoreProcedure)sqlTypeObj).readAndCalNavigateData(rbean,cdb);
                if(rrequest.getDbType(rbean.getSbean().getDatasource()) instanceof SQLSERVER2K)
                {
                    int pageno=cdb.getFinalPageno();
                    int pagecount=cdb.getPagecount();
                    int recordcount=cdb.getRecordcount();
                    if(recordcount<=(pageno-1)*pagesize)
                    {
                        lstData=null;
                    }else if(pagecount>1&&pageno==pagecount&&recordcount%pagesize>0)
                    {
                        int cnt=pagesize-recordcount%pagesize;
                        while(cnt-->0)
                        {
                            lstData.remove(0);
                        }
                    }
                }
            }else if(objResult instanceof List)
            {//直接返回数据集
                lstData=(List)objResult;
            }else
            {
                throw new WabacusRuntimeException("获取数据时，返回的结果集："+objResult.getClass().getName()+"为非法结果类型");
            }
            if(lstData!=null&&cdb.getMaxrecordcount()>0&&(cdb.getFinalPageno()-1)*pagesize+lstData.size()>cdb.getMaxrecordcount())
            {
                int delta=(cdb.getFinalPageno()-1)*pagesize+lstData.size()-cdb.getMaxrecordcount();
                while(delta-->0)
                {
                    lstData.remove(lstData.size()-1);
                }
            }
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("加载报表数据失败",e);
        }
        return lstData;
    }

    public int calPageCount(int ipagesize,int irecordcount)
    {
        int ipagecount=0;
        if(irecordcount%ipagesize==0)
        {
            ipagecount=irecordcount/ipagesize;
        }else
        {
            ipagecount=irecordcount/ipagesize+1;
        }
        return ipagecount;
    }

    private List parseDataIntoBean(AbsReportType reportObj,ResultSet rs,long size,ReportRequest rrequest)
    {
        ReportBean rbean=reportObj.getReportBean();
        try
        {
            if(rs==null) return null;
            List lstResultData=new ArrayList();
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            Object obj=getReportDataPojoInstance(rbean);
            if(obj==null)
            {
                log.error("没有取到报表"+rbean.getPath()+"存放中间数据的POJO类");
                return null;
            }
            Class c=obj.getClass();
            int n=0;
            if(size==-1) size=Long.MAX_VALUE;
            List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
            if(lstColBeans==null||lstColBeans.size()==0) lstColBeans=rbean.getDbean().getLstCols();
            
            
            
            Object dataObj;
            Object objVal;
            while(rs.next()&&n++<size)
            {
                dataObj=c.newInstance();
                for(ColBean cbTmp:lstColBeans)
                {
                    if(!"[DYN_STATISTIC_DATA]".equals(cbTmp.getProperty()))
                    {//如果当前列不是存放交叉统计数据的动态列，则每一列都在POJO中有一个名为property的值的成员变量
                        setColumnValueToDataObj(rs,dataObj,dbtype,cbTmp,rrequest);
                    }else
                    {
                        objVal=cbTmp.getDatatypeObj().getColumnValue(rs,cbTmp.getColumn(),dbtype);
                        setCrossStatisDataToPOJO(rbean,dataObj,cbTmp.getColumn(),objVal);
                    }
                }
                
                
                
                //                }
                
                //{//显示纯数据到Excel文件时是不用格式化的
                if(dataObj instanceof IFormat)
                {
                    ((IFormat)dataObj).format(rrequest,rbean);
                }
                lstResultData.add(dataObj);
            }
            return lstResultData;
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("将报表"+rbean.getPath()+"数据从ResultSet解析到Bean中出错",e);
        }
    }

    private void setColumnValueToDataObj(ResultSet rs,Object dataObj,AbsDatabaseType dbtype,ColBean colbean,ReportRequest rrequest)
    {
        String column=colbean.getColumn();
        column=column==null?"":column.trim();
        if(!column.equals("")&&!colbean.isNonFromDbCol()&&!colbean.isNonValueCol()&&!colbean.isSequenceCol()&&!colbean.isControlCol())
        {
            try
            {
                Object columnvalue=null;
                if(colbean.isI18n()&&rrequest!=null&&!rrequest.getLocallanguage().trim().equals(""))
                {
                    column=column+"_"+rrequest.getLocallanguage();
                    try
                    {
                        columnvalue=colbean.getDatatypeObj().getColumnValue(rs,column,dbtype);
                    }catch(SQLException sqle)
                    {
                        log.warn("根据列名"+column+"获取数据失败，可能是数据表中不支持"+rrequest.getLocallanguage()+"语言",sqle);
                        columnvalue=colbean.getDatatypeObj().getColumnValue(rs,colbean.getColumn(),dbtype);
                    }
                }else
                {
                    columnvalue=colbean.getDatatypeObj().getColumnValue(rs,column,dbtype);
                }
                colbean.getSetMethod().invoke(dataObj,new Object[] { columnvalue });
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("设置报表"+colbean.getReportBean().getPath()+"的列"+colbean.getColumn()+"数据到POJO对象时失败",e);
            }
        }
    }

    public void setCrossStatisDataToPOJO(ReportBean rbean,Object dataObj,String datakey,Object objVal)
    {
        if(dataObj==null) return;
        try
        {
            Method setMDataMethodForJava=dataObj.getClass().getMethod("setStatisticData",new Class[] { String.class, Object.class });
            setMDataMethodForJava.invoke(dataObj,new Object[] { datakey, objVal });
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"的数据失败",e);
        }
    }

    public Object getCrossStatisDataFromPOJO(ReportBean reportbean,Object dataObj,String datakey)
    {
        if(dataObj==null) return null;
        Object objValue=null;
        try
        {
            Method getMethod=dataObj.getClass().getMethod("getStatisticData",new Class[] { String.class });
            objValue=getMethod.invoke(dataObj,new Object[] { datakey });
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("从POJO对象中获取报表"+reportbean.getPath()+"数据失败",e);
        }
        return objValue;
    }

    public Object getPropertyValue(Object dataobj,String property)
    {
        if(dataobj==null||property==null||property.trim().equals(""))
        {
            return null;
        }
        String getMethodName="get"+property.substring(0,1).toUpperCase()+property.substring(1);
        try
        {
            Method getMethod=dataobj.getClass().getMethod(getMethodName,new Class[] {});
            return getMethod.invoke(dataobj,new Object[] {});
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("获取属性："+property+"数据失败",e);
        }

    }

    public String getPropertyValueAsString(Object dataobj,String property,IDataType datatypeObj)
    {
        if(dataobj==null||property==null||property.trim().equals(""))
        {
            return null;
        }
        String getMethodName="get"+property.substring(0,1).toUpperCase()+property.substring(1);
        try
        {
            Method getMethod=dataobj.getClass().getMethod(getMethodName,new Class[] {});
            Object value=getMethod.invoke(dataobj,new Object[] {});
            if(value==null) return null;
            if(datatypeObj==null) return value.toString();
            return datatypeObj.value2label(value);
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("获取属性："+property+"数据失败",e);
        }
    }

    public String formatCondition(String src,String token)
    {
        if(src==null||src.trim().length()<2||token==null||token.trim().equals(""))
        {
            return src;
        }
        src=src.trim();
        String dest="";
        if(token.equals("2"))
        {
            dest=src.substring(0,1);
            for(int i=1;i<src.length()-1;i++)
            {
                if(src.charAt(i)!=' ')
                {
                    dest=dest+"%"+src.charAt(i);
                }
            }
            dest=dest+"%"+src.substring(src.length()-1);
        }else
        {
            if(token.equals("1"))
            {
                token=" ";
            }

            while(src.indexOf(token)==0)
            {
                src=src.substring(1);
                src=src.trim();
            }
            while(src.endsWith(token))
            {
                src=src.substring(0,src.length()-1);
                src=src.trim();
            }
            

            StringTokenizer st=new StringTokenizer(src,token);
            while(st.hasMoreElements())
            {
                dest=dest+((String)st.nextElement()).trim()+"%";
                
            }
            if(dest.endsWith("%"))
            {
                dest=dest.substring(0,dest.length()-1);
            }
        }
        log.debug("条件值："+src+"在经过splitlike转换后，变为"+dest);
        return dest;
    }

    public int doServerValidate(String value_temp,String[][] methods)
    {
        try
        {
            int len=methods.length;
            Method m;
            List<Class> lstTemp=Config.getInstance().getLstServerValidateClasses();
            if(lstTemp==null||lstTemp.size()==0)
            {
                throw new WabacusRuntimeException("没有在系统配置文件中配置服务器端校验类，无法完成服务器端校验");
            }
            for(int j=0;j<len;j++)
            {
                if(methods[j][0].equals("")||methods[j][0].toLowerCase().equals("isnotempty"))
                {
                    continue;
                }
                int k=0;
                for(;k<lstTemp.size();k++)
                {
                    try
                    {
                        m=lstTemp.get(k).getMethod(methods[j][0],new Class[] { String.class });
                    }catch(NoSuchMethodException nse)
                    {
                        continue;
                    }
                    Boolean success=(Boolean)m.invoke(lstTemp.get(k),new Object[] { value_temp });
                    if(!success)
                    {
                        return j;
                    }else
                    {
                        break;
                    }
                }
                if(k==lstTemp.size())
                {
                    throw new WabacusRuntimeException("配置的服务器端校验方法名："+methods[j][0]+"在注册的所有服务器端校验类中都没有声明，无法完成校验");
                }
            }
            return -1;
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("利用配置的"+methods+"校验"+value_temp+"时出错",e);
        }
    }

    public void buildReportPOJOClass(ReportBean rbean,boolean cache)
    {
        DisplayBean dbean=rbean.getDbean();
        if(dbean==null) return;
        if(Config.getInstance().getReportType(rbean.getType()) instanceof CrossStatisticListReportType)
        {
            return;
        }
        try
        {
            List<String> lstImports=null;
            String format=null;
            if(rbean.getFbean()!=null)
            {
                format=rbean.getFbean().getFormatContent();
                lstImports=rbean.getFbean().getLstImports();
            }
            format=format==null?"":format.trim();
            //            ClassPool pool = ClassPool.getDefault();
            ClassPool pool=ClassPoolAssistant.getInstance().createClassPool();
            
            
            CtClass cclass=pool.makeClass(Consts.BASE_PACKAGE_NAME+".Pojo_"+rbean.getPageBean().getId()+rbean.getId());
            if(!format.equals(""))
            {
                if(lstImports==null) lstImports=new UniqueArrayList<String>();
                lstImports.add("com.wabacus.system.format");
                ClassPoolAssistant.getInstance().addImportPackages(pool,lstImports);
                cclass.setInterfaces(new CtClass[] { pool.get(IFormat.class.getName()) });
            }
            if(dbean.getLstCols()!=null)
            {
                for(ColBean cbean:dbean.getLstCols())
                {
                    if(cbean==null) continue;
                    String property=cbean.getProperty();
                    if(property==null||property.trim().equals("")) continue;
                    if(cbean.isNonValueCol()|| 
                            cbean.isSequenceCol()||cbean.isControlCol())
                    {
                        continue;
                    }
                    CtField cfield=ClassPoolAssistant.getInstance().addField(cclass,property,cbean.getDatatypeObj().getCreatedClass(pool),
                            Modifier.PRIVATE);
                    CtMethod setMethod=ClassPoolAssistant.getInstance().addSetMethod(cclass,cfield,property);
                    ClassPoolAssistant.getInstance().addGetMethod(cclass,cfield,property);
                    if(isNeedOriginalColValue(rbean,cbean))
                    {//如果运行时需要当前列的原始值，（即没有被格式化方法修改过的值）
                        String propertyOld=property+"_old";
                        CtField cfieldOld=ClassPoolAssistant.getInstance().addField(cclass,propertyOld,cbean.getDatatypeObj().getCreatedClass(pool),
                                Modifier.PRIVATE);
                        ClassPoolAssistant.getInstance().addGetMethod(cclass,cfieldOld,propertyOld);
                        setMethod.insertBefore("if($0."+propertyOld+"==null) $0."+propertyOld+"=$1;");
                    }
                }
            }
            if(!format.equals(""))
            {
                ClassPoolAssistant.getInstance().addMethod(cclass,
                        "public void format("+ReportRequest.class.getName()+"  rrequest,"+ReportBean.class.getName()+" rbean){"+format+" \n}");
            }
            if(cache)
            {
                rbean.setPojoclass(ConfigLoadManager.currentDynClassLoader.loadClass(Consts.BASE_PACKAGE_NAME+".Pojo_"+rbean.getPageBean().getId()
                        +rbean.getId(),cclass.toBytecode()));
            }else
            {
                cclass.writeFile(Config.homeAbsPath+"WEB-INF/classes");
            }
            cclass.detach();
            pool.clearImportedPackages();
            pool=null;
        }catch(NotFoundException e)
        {
            throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"生成字节码时，执行pool.get()失败",e);
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("生成类"+rbean.getPath()+"时无法编译",e);
        }catch(IOException ioe)
        {
            throw new WabacusConfigLoadingException("生成类"+rbean.getPath()+"时无法将生成的字节码写到本地文件系统",ioe);
        }
    }

    public void setMethodInfoToColBean(DisplayBean dbean,Class pojoclass)
    {
        if(dbean.getLstCols()==null||pojoclass==null) return;
        String propertyTmp=null;
        for(ColBean cbeanTmp:dbean.getLstCols())
        {
            try
            {
                if(cbeanTmp==null) continue;
                propertyTmp=cbeanTmp.getProperty();
                if(propertyTmp==null||propertyTmp.trim().equals("")) continue;
                if(cbeanTmp.isNonValueCol()||cbeanTmp.isSequenceCol()||cbeanTmp.isControlCol()) continue;
                String setMethodName="set"+propertyTmp.substring(0,1).toUpperCase()+propertyTmp.substring(1);
                Method setMethod=pojoclass.getMethod(setMethodName,new Class[] { cbeanTmp.getDatatypeObj().getJavaTypeClass() });
                cbeanTmp.setSetMethod(setMethod);

                String getMethodName="get"+propertyTmp.substring(0,1).toUpperCase()+propertyTmp.substring(1);
                Method getMethod=pojoclass.getMethod(getMethodName,new Class[] {});
                cbeanTmp.setGetMethod(getMethod);
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("从POJO类"+pojoclass.getClass().getName()+"获取报表"+dbean.getReportBean().getPath()+"的列"
                        +propertyTmp+"的get/set方法失败",e);
            }
        }
    }

    private boolean isNeedOriginalColValue(ReportBean rbean,ColBean cbean)
    {
        AbsReportType reportTypeObj=Config.getInstance().getReportType(rbean.getType());
        if((reportTypeObj instanceof IEditableReportType)
                &&!(reportTypeObj instanceof EditableDetailReportType||reportTypeObj instanceof EditableListFormReportType))
        {
            return true;
        }
        
        //        {//如果当前报表需要提供纯数据的Excel下载
        //            return true;
        
        AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrcbean!=null&&alrcbean.isRowgroup())
        {
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrdbean!=null&&alrdbean.getStatibean()!=null&&alrdbean.getStatibean().getMStatiRowGroupBeans()!=null
                    &&alrdbean.getStatibean().getMStatiRowGroupBeans().size()>0)
            {
                return true;
            }
        }
        return false;
    }

    public IButtonClickeventGenerate createButtonEventGeneratorObject(String classname,String clickevent,List<String> lstImports)
    {
        try
        {
            if(clickevent==null||clickevent.trim().equals("")) return null;
            clickevent=clickevent.trim();
            ClassPool pool=new ClassPool();
            pool.appendSystemPath();
            pool.insertClassPath(new ClassClassPath(ReportAssistant.class));
            CtClass cclass=pool.makeClass(Consts.BASE_PACKAGE_NAME+"."+classname+"_Event");

            if(lstImports==null) lstImports=new UniqueArrayList<String>();
            lstImports.add("com.wabacus.system.buttons");
            ClassPoolAssistant.getInstance().addImportPackages(pool,lstImports);

            cclass.setInterfaces(new CtClass[] { pool.get(IButtonClickeventGenerate.class.getName()) });
            CtMethod generateMethod=CtNewMethod.make("public String generateClickEvent("+ReportRequest.class.getName()+"  rrequest){"+clickevent
                    +" \n}",cclass);
            cclass.addMethod(generateMethod);
            Class cls=ConfigLoadManager.currentDynClassLoader.loadClass(Consts.BASE_PACKAGE_NAME+"."+classname+"_Event",cclass.toBytecode());
            return (IButtonClickeventGenerate)cls.newInstance();
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("生成"+classname+"按钮事件类失败",e);
        }
    }

    public Class buildInterceptorClass(String className,List<String> lstImports,String preaction,String postaction,String beforesave,
            String beforesavePerrow,String beforesavePersql,String aftersavePersql,String aftersavePerrow,String aftersave,String beforeloaddata,
            String afterloaddata,String displayperrow,String displaypercol)
    {
        try
        {
            ClassPool pool=ClassPoolAssistant.getInstance().createClassPool();
            CtClass cclass=pool.makeClass(Consts.BASE_PACKAGE_NAME+"."+className+"_Interceptor");
            if(lstImports==null) lstImports=new UniqueArrayList<String>();
            lstImports.add("com.wabacus.system.intercept");
            lstImports.add("com.wabacus.config.component.application.report");
            ClassPoolAssistant.getInstance().addImportPackages(pool,lstImports);
            cclass.setInterfaces(new CtClass[] { pool.get(IInterceptor.class.getName()) });
            preaction=preaction==null?"":preaction.trim();
            postaction=postaction==null?"":postaction.trim();
            beforesave=beforesave==null?"":beforesave.trim();
            beforesavePerrow=beforesavePerrow==null?"":beforesavePerrow.trim();
            beforesavePersql=beforesavePersql==null?"":beforesavePersql.trim();
            aftersavePersql=aftersavePersql==null?"":aftersavePersql.trim();
            aftersavePerrow=aftersavePerrow==null?"":aftersavePerrow.trim();
            aftersave=aftersave==null?"":aftersave.trim();
            beforeloaddata=beforeloaddata==null?"":beforeloaddata.trim();
            afterloaddata=afterloaddata==null?"":afterloaddata.trim();
            displayperrow=displayperrow==null?"":displayperrow.trim();
            displaypercol=displaypercol==null?"":displaypercol.trim();
            
            StringBuffer sbuffer=new StringBuffer();
            sbuffer.append("public void doStart("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean) {")
                    .append(preaction).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            sbuffer=new StringBuffer();
            sbuffer.append("public void doEnd("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean) {").append(postaction)
                    .append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            sbuffer=new StringBuffer();
            if(beforesave.equals("")) beforesave=" return WX_CONTINUE;";
            sbuffer.append("public int beforeSave("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean){").append(
                    beforesave).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            sbuffer=new StringBuffer();
            if(beforesavePerrow.equals("")) beforesavePerrow=" return WX_CONTINUE;";
            sbuffer.append(
                    "public int beforeSavePerRow("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean,"
                            +Map.class.getName()+" mRowData,"+Map.class.getName()+" mExternalValues,int updatetype){").append(beforesavePerrow)
                    .append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());
            
            sbuffer=new StringBuffer();
            if(beforesavePersql.equals("")) beforesavePersql=" return WX_CONTINUE;";
            sbuffer.append(
                    "public int beforeSavePerSql("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean,"
                            +Map.class.getName()+" mRowData,"+Map.class.getName()+" mExternalValues,"+String.class.getName()+" sql){").append(
                    beforesavePersql).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());
            
            sbuffer=new StringBuffer();
            if(aftersavePersql.equals("")) aftersavePersql=" return WX_CONTINUE;";
            sbuffer.append(
                    "public int afterSavePerSql("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean,"+Map.class.getName()
                            +" mRowData,"+Map.class.getName()+" mExternalValues,"+String.class.getName()+" sql){").append(aftersavePersql).append(
                    " \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());
            
            sbuffer=new StringBuffer();
            if(aftersavePerrow.equals("")) aftersavePerrow=" return WX_CONTINUE;";
            sbuffer.append(
                    "public int afterSavePerRow("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean,"+Map.class.getName()
                            +" mRowData,"+Map.class.getName()+" mExternalValues,int updatetype){").append(aftersavePerrow).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());
            
            sbuffer=new StringBuffer();
            sbuffer.append("public void afterSave("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean){").append(
                    aftersave).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            sbuffer=new StringBuffer();
            if(beforeloaddata.equals(""))
            {
                beforeloaddata=" return sql;";
            }
            sbuffer.append("public Object beforeLoadData("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean,").append(
                    Object.class.getName()+" typeObj,").append(String.class.getName()).append(" sql){");
            sbuffer.append(beforeloaddata).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            sbuffer=new StringBuffer();
            if(afterloaddata.equals(""))
            {
                afterloaddata=" return dataObj;";
            }
            sbuffer.append("public Object afterLoadData("+ReportRequest.class.getName()+" rrequest,"+ReportBean.class.getName()+" rbean,").append(
                    Object.class.getName()+" typeObj,").append(Object.class.getName()).append(" dataObj){");
            sbuffer.append(afterloaddata).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());
            
            sbuffer=new StringBuffer();
            if(displayperrow.equals(""))
            {
                displayperrow=" return null;";
            }
            sbuffer.append(
                    "public RowDataByInterceptor beforeDisplayReportDataPerRow("+AbsReportType.class.getName()+" reportTypeObj,"
                            +ReportRequest.class.getName()+" rrequest,").append(" int rowindex,int colspans, "+List.class.getName()+" lstColBeans){");
            sbuffer.append(displayperrow).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            sbuffer=new StringBuffer();
            if(displaypercol.equals(""))
            {
                displaypercol=" return null;";
            }
            sbuffer.append(
                    "public ColDataByInterceptor beforeDisplayReportDataPerCol("+AbsReportType.class.getName()+" reportTypeObj,"
                            +ReportRequest.class.getName()+" rrequest,").append(Object.class.getName()+" displayColBean,int rowindex,").append(
                    String.class.getName()+" value").append("){");
            sbuffer.append(displaypercol).append(" \n}");
            ClassPoolAssistant.getInstance().addMethod(cclass,sbuffer.toString());

            Class c=ConfigLoadManager.currentDynClassLoader.loadClass(Consts.BASE_PACKAGE_NAME+"."+className+"_Interceptor",cclass.toBytecode());
            cclass.detach();
            pool.clearImportedPackages();
            pool=null;
            return c;
        }catch(NotFoundException e)
        {
            throw new WabacusConfigLoadingException("生成"+className+"拦截器字节码时，执行pool.get()失败",e);
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("生成拦截器"+className+"字节码时无法编译",e);
        }catch(IOException ioe)
        {
            throw new WabacusConfigLoadingException("生成拦截器"+className+"字节码时无法将生成的字节码写到本地文件系统",ioe);
        }
    }

    //    /**
    
    //     * 
    
    
    
    
    //     */
    //    public static String parseUpdateWhereClause(SqlBean sqlbean, AbsReportUpdateBean updatebean,
    
    
    
    
    //        if (whereclause.toLowerCase().indexOf("where ") != 0)
    
    
    
    
    //        whereBuffer.append(" where ");
    
    
    
    
    //        String conname;
    
    
    
    
    //            val = lstCons.get(k).trim();
    
    
    
    
    //            {
    
    
    
    
    //                }
    
    
    
    
    //                            + sqlbean.getReportBean().getPath() + "失败，在<update/>中配置的更新语句中条件子句不对");
    //                }
    
    
    
    
    //                {
    
    
    
    
    //                {
    
    
    
    
    //                        lstCbPropertytemp.add(cb.getProperty());
    
    
    
    
    //                {//条件值为常量，则不用解决此条件子句，直接附在后面即可。
    //                    whereBuffer.append(val);
    
    
    
    
    //        return whereBuffer.toString();
    

    
    
    
    //        List<String> lst = Tools.parseStringToList(whereclause," ");
    
    
    
    
    //        {
    
    
    
    
    //                sbuffer.append(" ").append(tmpval).append(" ");
    
    
    
    
    //        }
    
    

    public List<String[]> getOptionListFromDB(ReportRequest rrequest,ReportBean rbean,String sql,OptionBean obean)
    {
        List<String[]> lstResults=new ArrayList<String[]>();
        try
        {
           ISQLType impISQLType=rbean.getSbean().getISQLTypeBuilder().createAllResultSetISQLType();;
            Object objTmp=impISQLType.getResultSet(rrequest,rbean,obean,sql,obean.getLstConditions());
            if(objTmp instanceof List)
            {
                for(Object itemTmp:(List)objTmp)
                {
                    if(itemTmp==null) continue;
                    if(!(itemTmp instanceof String[]))
                    {
                        throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"下拉选项数据的拦截器返回的List对象中元素类型不对，必须为String[]类型");
                    }
                    if(((String[])itemTmp).length!=2)
                    {
                        throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"下拉选项数据的拦截器返回的List对象中String[]类型的元素长度必须为2");
                    }
                    lstResults.add((String[])itemTmp);
                }
            }else if(objTmp instanceof ResultSet)
            {
                ResultSet rs=(ResultSet)objTmp;
                while(rs.next())
                {
                    String name_temp=rs.getString(obean.getLabel());
                    String value_temp=rs.getString(obean.getValue());
                    name_temp=name_temp==null?"":name_temp.trim();
                    value_temp=value_temp==null?"":value_temp.trim();
                    String[] items=new String[2];
                    items[0]=name_temp;
                    items[1]=value_temp;
                    lstResults.add(items);
                }
                rs.close();
            }else if(objTmp!=null)
            {
                throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"的下拉选项数据失败，在加载下拉选项数据的拦截器中返回的对象类型"+objTmp.getClass().getName()+"不合法");
            }
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("执行SQL语句："+obean.getSql()+" 从数据库中获取下拉选项失败",e);
        }
        if(rbean.getInterceptor()!=null)
        {
            lstResults=(List)rbean.getInterceptor().afterLoadData(rrequest,rbean,obean,lstResults);
        }
        return lstResults;
    }

    public boolean shouldShowThisApplication(IComponentConfigBean applicationBean,ReportRequest rrequest)
    {
        if(applicationBean instanceof AbsContainerConfigBean)
        {
            //            rrequest.getWResponse().getErrors().error("显示页面失败");
            throw new WabacusRuntimeException("此方法只能传入应用进行判断，不能传入容器");
        }
        if(applicationBean==null) return false;
        if(rrequest.getSlaveReportBean()!=null)
        {
            if(applicationBean==rrequest.getSlaveReportBean()) return true;
            return false;
        }
        if(applicationBean==rrequest.getRefreshComponentBean()) return true;

        if(rrequest.getRefreshComponentBean() instanceof AbsContainerConfigBean)
        {
            if(((AbsContainerConfigBean)rrequest.getRefreshComponentBean()).isExistChildId(applicationBean.getId(),false,true))
            {
                return true;
            }

        }
        return false;
    }

    public String getColAndConditionDefaultValue(ReportRequest rrequest,String defaultvalue)
    {
        if(defaultvalue==null||defaultvalue.trim().equals("")) return "";
        if(Tools.isDefineKey("url",defaultvalue)||Tools.isDefineKey("request",defaultvalue)||Tools.isDefineKey("session",defaultvalue))
        {
            return WabacusAssistant.getInstance().getRequestSessionValue(rrequest,defaultvalue,"");
        }
        return defaultvalue.trim();
    }

    //    public String getExportDataLinkAndLabel(ReportRequest rrequest,String exportReportids,String label,String exporttype)
    
    
    
    
    ////        CacheDataBean cdb=null;
    //        List<String> lstExportReportids=Tools.parseStringToList(exportReportids,";",false);
    
    //        {//只下载一个报表
    
    
    ////            IComponentType typeObjTmp=rrequest.getComponentTypeObj(rbean,null,false);
    ////            if(typeObjTmp!=null) cdb=rrequest.getCdb(rbean.getId());//如果本报表参与了本次显示
    
    //        /**
    //         * 判断本次导出的所有报表在此导出类型上的权限
    //         */
    //        boolean isAllNonDisplayPermission=true;//本次导出的所有报表是否都没有相应类型的导出链接的显示权限
    
    
    
    
    //            if(!rrequest.checkPermission(reportidTmp,Consts.BUTTON_PART,"type{"+exporttype+"}",Consts.PERMISSION_TYPE_DISPLAY)) continue;//没有显示权限
    //            isAllNonDisplayPermission=false;
    
    //            {//此报表的此权限不是禁用
    
    
    
    //        if(isAllNonDisplayPermission) return "";//如果本次导出的所有报表都没有显示此类型数据导出功能的权限
    //        if(validExportReportids.endsWith(";")) validExportReportids=validExportReportids.substring(0,validExportReportids.length()-1);
    
    
    //        {//不是所有报表都禁用这种类型的导出
    
    
    //            {
    
    
    
    
    //            }else
    //            {//Consts.DATAEXPORT_RICHEXCEL
    
    
    
    //            {//当前只下载一个参与本次显示的报表，且此报表需要提供列动态选择框
    
    //                paramsBuf.append("{reportguid:\"").append(rbean.getGuid()).append("\"");
    
    
    
    
    //                {
    
    
    
    
    //                    paramsBuf.append(",width:\"").append(width).append("\"");
    
    
    
    
    //                clickevent="createTreeObjHtml(this,'"+Tools.jsParamEncode(paramsBuf.toString())+"',event);";//注意这个方法的第二个参数pageid必须为空，因为在此方法中要据此判断当前是在做下载Excel还是刷新页面的操作
    //            }else
    //            {//下载多个报表，或下载的一个报表不参与本次显示，或虽参与本次显示，但不需要提供动态列选择框，直接下载当前页面显示的数据
    //                //clickevent="postlinkurl('"+url+"',true);";
    
    
    
    
    //        if(label!=null&&!label.trim().equals(""))
    //        {//客户端提供了显示的label
    
    
    
    //            resultBuf.append("</a>");
    
    //        {
    
    
    
    
    //            }else if(Consts.DATAEXPORT_RICHEXCEL.equals(exporttype.toLowerCase().trim()))
    
    
    
    
    //            {
    
    
    
    
    //                return "";
    
    
    
    
    //        }
    
    
    
    
    //        if(rbean.getLstDataImportItems()==null||rbean.getLstDataImportItems().size()==0)  return "";
    
    
    
    //        {//如果不是禁用
    
    //            if(Config.showreport_url.indexOf("?")>0) token="&";
    
    
    
    
    //            {
    
    
    
    
    //            {
    
    
    
    
    //        StringBuffer resultBuf=new StringBuffer();
    
    //        {//客户端提供了显示的label
    
    
    //            resultBuf.append("</a>");
    
    //        {
    
    
    
    
    //        }
    
    
    //    /**
    
    
    //     * @return
    //     */
    
    
    
    
    //        StringBuffer resultBuf=new StringBuffer();
    //        /**
    
    //         */
    //        boolean hasCondtionWithInputBox=false;//是否有带输入框的查询条件
    
    
    
    //        {
    
    
    
    
    //            resultBuf.append("inputboxtype:\"").append(cbeanTmp.getInputbox().getTypename()).append("\",");
    //            /**************临时删除*************if(cbeanTmp.getLstConditionValuesBean().size()>1)
    //            {//此条件有多个条件表达式供选择
    
    
    //            }*********************/
    
    
    //                resultBuf.deleteCharAt(resultBuf.length()-1);
    
    
    
    
    //        if(!hasCondtionWithInputBox) return "";
    
    
    
    
    //        resultBuf.append("]}");
    
    
    public Object getReportDataPojoInstance(ReportBean rbean)
    {
        String classType=rbean.getStrclass();
        Object resultObj=null;
        try
        {
            Class c=null;
            if(classType==null||classType.trim().equals("")||classType.trim().equalsIgnoreCase("commondatabean"))
            {

                if(rbean.isClasscache())
                {
                    c=rbean.getPojoclass();
                }else
                {
                    c=ResourceUtils.loadClass(Consts.BASE_PACKAGE_NAME+".Pojo_"+rbean.getPageBean().getId()+"_"+rbean.getId());
                }
            }else
            {
                c=ResourceUtils.loadClass(classType);
            }
            resultObj=c.newInstance();
        }catch(ClassNotFoundException e1)
        {
            throw new WabacusRuntimeException("存放数据的类"+rbean.getStrclass()+"没有找到",e1);
        }catch(Exception e2)
        {
            throw new WabacusRuntimeException("存放报表"+rbean.getPath()+"数据的类无法实例化",e2);
        }
        return resultObj;
    }

    public String getColSelectedLabelAndEvent(ReportRequest rrequest,ReportBean rbean,boolean isListReport)
    {
        StringBuffer paramsBuf=new StringBuffer();
        paramsBuf.append("{reportguid:\"").append(rbean.getGuid()).append("\"");
        paramsBuf.append(",skin:\"").append(rrequest.getPageskin()).append("\"");
        paramsBuf.append(",webroot:\"").append(Config.webroot).append("\"");
        String width=rbean.getDbean().getColselectwidth();
        if(width==null||width.trim().equals(""))
        {
            width=Config.getInstance().getSystemConfigValue("default-colselect-width","");
        }
        if(!width.equals(""))
        {
            paramsBuf.append(",width:\"").append(width).append("\"");
        }
        paramsBuf.append("}");
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<img src=\""+Config.webroot+"webresources/skin/"+rrequest.getPageskin()+"/images/coltitle_selected/selectcols.gif\"");
        if(isListReport)
        {
            resultBuf.append(" class=\"colSelectedLabel_img\"");
        }else
        {
            resultBuf.append(" style=\"cursor:pointer;\"");
        }
        resultBuf.append(" onclick=\"");
        resultBuf.append("try{createTreeObjHtml(this,'"+Tools.jsParamEncode(paramsBuf.toString())+"',event);}catch(e){logErrorsAsJsFileLoad(e);}\">");
        return resultBuf.toString();
    }
    
     public String getNavigatePagenoWithEvent(ReportRequest rrequest,ReportBean rbean,int pageno,String label)
     {
         StringBuffer resultBuf=new StringBuffer();
         resultBuf.append("<span class=\"cls-navigate-label\" onmouseover=\"this.style.cursor='pointer';\" onclick=\"try{navigateReportPage('");
         resultBuf.append(rbean.getPageBean().getId()).append("','");
         resultBuf.append(rbean.getId()).append("','");
         resultBuf.append(pageno).append("');}catch(e){logErrorsAsJsFileLoad(e);}");
         resultBuf.append("\">").append(label).append("</span>");
         return resultBuf.toString();
     }
     
     public String getNavigateTextBox(ReportRequest rrequest,ReportBean rbean)
     {
         CacheDataBean cdb=rrequest.getCdb(rbean.getId());
         int pagecount=cdb.getPagecount();
         int pageno=cdb.getFinalPageno();
         boolean isReadonly=false;
         if(rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PAGENO,Consts.PERMISSION_TYPE_DISABLED))
         {//如果是禁用
             isReadonly=true;
         }
         String strpageno=String.valueOf(pagecount).trim();
         int width=strpageno.length()*10;
         if(width<30) width=30;
         String dynstyleproperty="style=\"width:"+width+"px;\"";
         if(!isReadonly&&pagecount>1)
         {
             StringBuffer blurEventBuf=new StringBuffer();
             blurEventBuf.append("if(isPositiveInteger(this.value)&&parseInt(this.value,10)!=").append(pageno).append("){");
             blurEventBuf.append("if(parseInt(this.value,10)>").append(pagecount).append(") this.value='").append(pagecount).append("';");
             blurEventBuf.append("try{navigateReportPage('");
             blurEventBuf.append(rbean.getPageBean().getId()).append("','");
             blurEventBuf.append(rbean.getId()).append("',this.value);}catch(e){logErrorsAsJsFileLoad(e);}");
             blurEventBuf.append("}");
             dynstyleproperty=dynstyleproperty+" onblur=\""+blurEventBuf.toString()+"\"";
         }
         AbsInputBox box=Config.getInstance().getInputBoxByType(TextBox.class);
         return box.getIndependentDisplayString(rrequest,String.valueOf(pageno),dynstyleproperty,null,isReadonly);
     }
     
     public String getNavigateSelectBox(ReportRequest rrequest,ReportBean rbean)
     {
         CacheDataBean cdb=rrequest.getCdb(rbean.getId());
         int pagecount=cdb.getPagecount();
         int pageno=cdb.getFinalPageno();
         boolean isDisabled=false;
         if(rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PAGENO,Consts.PERMISSION_TYPE_DISABLED))
         {
             isDisabled=true;
         }
         String dynstyleproperty="name=\""+rbean.getGuid()+"_SELEPAGENUM\"";
         if(!isDisabled&&pagecount>1)
         {
             StringBuffer onchangeEventBuf=new StringBuffer();
             onchangeEventBuf.append("try{navigateReportPage('");
             onchangeEventBuf.append(rbean.getPageBean().getId()).append("','");
             onchangeEventBuf.append(rbean.getId()).append("',this.options[this.options.selectedIndex].value);}catch(e){logErrorsAsJsFileLoad(e);}");
             dynstyleproperty=dynstyleproperty+" onchange=\""+onchangeEventBuf.toString()+"\"";
         }
         List<String[]> lstOptionsResult=new ArrayList<String[]>();
         if(pagecount<=0)
         {
             lstOptionsResult.add(new String[] { String.valueOf(0), String.valueOf(0) });
         }else
         {
             for(int j=1;j<=pagecount;j++)
             {
                 lstOptionsResult.add(new String[] { String.valueOf(j), String.valueOf(j) });
             }
         }
         AbsInputBox box=Config.getInstance().getInputBoxByType(SelectBox.class);//取到注册的下拉框对象
         return box.getIndependentDisplayString(rrequest,String.valueOf(pageno),dynstyleproperty,lstOptionsResult,isDisabled);
     }
     
     public String getNavigateSelectBoxForPagesizeConvert(ReportRequest rrequest,
             ReportBean rbean)
     {
         AbsListReportBean alrbean=(AbsListReportBean)rbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
         if(alrbean==null) return "";
         CacheDataBean cdb=rrequest.getCdb(rbean.getId());
         int currentpagesize=cdb.getPagesize();
         boolean isDisabled=false;
         if(rrequest.checkPermission(rbean.getId(),Consts.NAVIGATE_PART,Consts_Private.NAVIGATE_PAGESIZE,Consts.PERMISSION_TYPE_DISABLED))
         {
             isDisabled=true;
         }
         StringBuffer onchangeEventBuf=new StringBuffer();
         onchangeEventBuf.append("var url=getComponentUrl('").append(rbean.getPageBean().getId()).append("','");
         onchangeEventBuf.append(rbean.getRefreshGuid()).append("','");
         if(rbean.isSlaveReportDependsonListReport())
         {
             onchangeEventBuf.append(rbean.getId());
         }
         onchangeEventBuf.append("');");
         onchangeEventBuf.append("url=replaceUrlParamValue(url,'").append(rbean.getId()).append("_PREV_PAGESIZE','"+currentpagesize+"');");
         onchangeEventBuf.append("url=replaceUrlParamValue(url,'").append(rbean.getId()).append(
                 "_PAGESIZE',this.options[this.options.selectedIndex].value);");
         onchangeEventBuf.append("refreshComponent(url);");

         List<String[]> lstOptionsResult=new ArrayList<String[]>();
         List<Integer> lstPagesizeTmp=rbean.getLstPagesize();
         String alldata_label="";
         if(lstPagesizeTmp.contains(-1))
         {
             alldata_label=rrequest.getI18NStringValue((Config.getInstance().getResources().getString(rrequest,rbean.getPageBean(),
                     Consts.NAVIGATE_ALLDATA_LABEL,true)).trim());
         }
         boolean isExistCurrentPagesizeOption=false;
         String labelTmp;
         for(int i=0;i<lstPagesizeTmp.size();i++)
         {
             labelTmp=lstPagesizeTmp.get(i)==-1?alldata_label:String.valueOf(lstPagesizeTmp.get(i));
             lstOptionsResult.add(new String[] { labelTmp, String.valueOf(lstPagesizeTmp.get(i)) });
             if(currentpagesize==lstPagesizeTmp.get(i)) isExistCurrentPagesizeOption=true;
         }
         if(!isExistCurrentPagesizeOption) lstOptionsResult.add(0,new String[] { String.valueOf(currentpagesize), String.valueOf(currentpagesize) });//如果配置的选项中没有此页大小，则也把它加入下拉框显示
         AbsInputBox box=Config.getInstance().getInputBoxByType(SelectBox.class);
         return box.getIndependentDisplayString(rrequest,String.valueOf(currentpagesize),"onchange=\""+onchangeEventBuf.toString()+"\"",
                 lstOptionsResult,isDisabled);
     }
     
     public ColDataByInterceptor getColDataFromInterceptor(ReportRequest rrequest,AbsReportType reportTypeObj,Object colGroupBean,int rowindex,
            String value)
    {
        if(reportTypeObj.getReportBean().getInterceptor()==null) return null;
        if(rowindex<0&&rowindex!=-1) rowindex=-1;
        return reportTypeObj.getReportBean().getInterceptor().beforeDisplayReportDataPerCol(reportTypeObj,rrequest,colGroupBean,rowindex,value);
    }
     
     public String getColGroupLabel(ReportRequest rrequest,String configLabel,ColDataByInterceptor coldataByInterceptor)
    {
        if(configLabel==null) configLabel="";
        if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
        {
            configLabel=coldataByInterceptor.getDynvalue();
        }
        configLabel=rrequest.getI18NStringValue(configLabel);
        configLabel=configLabel==null?"":configLabel.trim();
        return configLabel;
    }
}
