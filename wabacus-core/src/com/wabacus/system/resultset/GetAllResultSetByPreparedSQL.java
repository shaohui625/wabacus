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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSqlBean;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.inputbox.AutoCompleteConfigBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class GetAllResultSetByPreparedSQL implements ISQLType
{
    private static Log log=LogFactory.getLog(GetAllResultSetByPreparedSQL.class);

    protected List<String> lstConditions;

    protected List<IDataType> lstConditionsTypes;

    public GetAllResultSetByPreparedSQL()
    {
        lstConditions=new ArrayList<String>();
        lstConditionsTypes=new ArrayList<IDataType>();
    }

    public Object getResultSet(ReportRequest rrequest,ReportBean reportbean,Object typeObj,String sql,List<ConditionBean> lstConditionBeans)
    {
        PreparedStatement pstmt=null;
        try
        {
            lstConditions.clear();
            lstConditionsTypes.clear();
            sql=ReportAssistant.getInstance().addDynamicConditionExpressionsToSql(rrequest,reportbean,sql,lstConditionBeans,lstConditions,lstConditionsTypes);
            if(reportbean.getInterceptor()!=null&&typeObj!=null)
            {
                Object obj=reportbean.getInterceptor().beforeLoadData(rrequest,reportbean,typeObj,sql);
                if(!(obj instanceof String)) return obj;
                sql=(String)obj;
            }
            if(Config.show_sql)
            {
                log.info("Execute sql: "+sql);
            }
            pstmt=rrequest.getConnection(reportbean.getSbean().getDatasource()).prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            if(lstConditions.size()>0)
            {
                AbsDatabaseType dbtype=rrequest.getDbType(reportbean.getSbean().getDatasource());
                for(int j=0;j<lstConditions.size();j++)
                {
                    if(Config.show_sql)
                    {
                        log.info("param"+(j+1)+"="+lstConditions.get(j));
                    }
                    lstConditionsTypes.get(j).setPreparedStatementValue(j+1,lstConditions.get(j),pstmt,dbtype);
                }

            }
            rrequest.addUsedStatement(pstmt);
            return pstmt.executeQuery();
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取数据失败，执行SQL："+sql+"抛出异常",e);
        }
    }

    public Object getResultSet(ReportRequest rrequest,AbsReportType reportObj,Object typeObj,String sql)
    {
        PreparedStatement pstmt=null;
        try
        {
            lstConditions.clear();
            lstConditionsTypes.clear();
            ReportBean rbean=reportObj.getReportBean();
            sql=ReportAssistant.getInstance().parseRuntimeSqlAndCondition(rrequest,rbean,sql,lstConditions,lstConditionsTypes);
            sql=ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest,rbean,sql);
            if(rbean.getInterceptor()!=null&&typeObj!=null)
            {
                Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,typeObj,sql);
                if(!(obj instanceof String)) return obj;
                sql=(String)obj;
            }
            if(Config.show_sql)
            {
                log.info("Execute sql: "+sql);
            }
            pstmt=rrequest.getConnection(rbean.getSbean().getDatasource()).prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            if(lstConditions.size()>0)
            {
                AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
                for(int j=0;j<lstConditions.size();j++)
                {
                    if(Config.show_sql)
                    {
                        log.info("param"+(j+1)+"="+lstConditions.get(j));
                    }
                    lstConditionsTypes.get(j).setPreparedStatementValue(j+1,lstConditions.get(j),pstmt,dbtype);
                }

            }
            rrequest.addUsedStatement(pstmt);
            return pstmt.executeQuery();
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取数据失败，执行SQL："+sql+"抛出异常",e);
        }
    }

    public Object getResultSet(ReportRequest rrequest,AbsReportType reportObj)
    {
        ReportBean rbean=reportObj.getReportBean();
        SqlBean sbean=rbean.getSbean();
        String sql=null;
        Object typeObj=null;
        if(rrequest.getActiontype().equalsIgnoreCase(Consts.GETFILTERDATALIST_ACTION))
        {
            AbsListReportSqlBean alrsbean=(AbsListReportSqlBean)sbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrsbean==null)
            {
                throw new WabacusRuntimeException("没有取到报表"+rbean.getPath()+"要获取过滤数据的SQL语句");
            }
            sql=alrsbean.getFilterdata_sql();
            if(sql==null||sql.trim().equals(""))
            {
                throw new WabacusRuntimeException("没有取到报表"+rbean.getPath()+"要获取过滤数据的SQL语句");
            }
            String colunmname=rrequest.getStringAttribute("FILTER_COLUMNNAME","");
            if(colunmname.equals(""))
            {
                throw new WabacusRuntimeException("没有取到要获取过滤数据的字段名");
            }
            sql=Tools.replaceAll(sql,"%FILTERCOLUMN%",colunmname);
            typeObj=rrequest.getAttribute(rbean.getId()+"_WABACUS_FILTERBEAN");
            String selectdatatype=rrequest.getStringAttribute(rbean.getId()+"_WABACUS_GETDATA","all");
            if(selectdatatype.equals("selected"))
            {
                sql=ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest,rbean,sql);
            }else
            {
                sql=Tools.replaceAll(sql,Consts_Private.PLACEHODER_FILTERCONDITION,"");
            }
        }else if(rrequest.getActiontype().equalsIgnoreCase(Consts.GETAUTOCOMPLETEDATA_ACTION))
        {
            AutoCompleteConfigBean accbean=rrequest.getAutoCompleteSourceInputBoxObj().getAutocompleteBean();
            sql="select "+accbean.getAutoCompleteColumns()+" from ("+sbean.getSql_kernel()+") wx_tbl_autocompletedata where "
                    +rrequest.getStringAttribute("COL_CONDITION_EXPRESSION","");
            typeObj=accbean;
        }else
        {
            sql=sbean.getValue();
            AbsListReportSqlBean alrsbean=(AbsListReportSqlBean)sbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrsbean!=null)
            {
                String sqlKernel=rrequest.getStringAttribute(rbean.getId(),"DYN_SQL","");
                if(sqlKernel.equals("")) sqlKernel=sbean.getSql_kernel();//如果没有动态构造的SQL语句，则用静态配置的
                sql=Tools.replaceAll(sbean.getSqlWithoutOrderby(),Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlKernel);
                String[] orderbys=(String[])rrequest.getAttribute(rbean.getId(),"ORDERBYARRAY");
                if(orderbys!=null&&orderbys.length==2)
                {
                    String dynorderby=ReportAssistant.getInstance().mixDynorderbyAndRowgroupCols(sbean.getReportBean(),orderbys[0]+" "+orderbys[1]);
                    if(sql.indexOf("%orderby%")<0)
                    {
                        sql=sql+" order by "+dynorderby;
                    }else
                    {
                        sql=Tools.replaceAll(sql,"%orderby%"," order by "+dynorderby);
                    }
                }else
                {
                    String ordertmp=sbean.getOrderby();
                    if(ordertmp==null) ordertmp="";
                    if(!ordertmp.trim().equals(""))
                    {
                        ordertmp=" order by "+ordertmp;
                    }
                    sql=Tools.replaceAll(sql,"%orderby%",ordertmp);
                }
            }else
            {
                String sqlTmp=rrequest.getStringAttribute(rbean.getId(),"DYN_SQL","");
                if(!sqlTmp.equals("")) sql=sqlTmp;
            }
            typeObj=reportObj;
        }
        return getResultSet(rrequest,reportObj,typeObj,sql);
    }
}
