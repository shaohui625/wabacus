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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class GetPartResultSetByPreparedSQL extends GetAllResultSetByPreparedSQL
{
    private static Log log=LogFactory.getLog(GetPartResultSetByPreparedSQL.class);

    public Object getResultSet(ReportRequest rrequest,AbsReportType reportObj)
    {
        ReportBean rbean=reportObj.getReportBean();
        SqlBean sbean=rbean.getSbean();
        String sqlKernel=rrequest.getStringAttribute(rbean.getId(),"DYN_SQL","");
        if(sqlKernel.equals(""))
        {
            sqlKernel=sbean.getSql_kernel();
        }
        sqlKernel=ReportAssistant.getInstance().parseRuntimeSqlAndCondition(rrequest,rbean,sqlKernel,lstConditions,lstConditionsTypes);
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        int pagesize=cdb.getPagesize();
        int refreshNavigateInfoType=cdb.getRefreshNavigateInfoType();
        if(refreshNavigateInfoType<0)
        {
            int recordcount=getRecordcount(rrequest,reportObj,sqlKernel);
            if(recordcount<0) return null;
            cdb.setRecordcount(recordcount);
        }
        if(refreshNavigateInfoType<=0)
        {
            cdb.setPagecount(ReportAssistant.getInstance().calPageCount(pagesize,cdb.getRecordcount()));
        }
        if(cdb.getPagecount()<=0) return null;//没有记录
        int pageno=cdb.getFinalPageno();
        PreparedStatement pstmt=null;
        String sql=null;
        try
        {
            sql=sbean.getSplitpage_sql();
            String[] orderbys=(String[])rrequest.getAttribute(rbean.getId(),"ORDERBYARRAY");
            if(orderbys!=null&&orderbys.length==2)
            {
                sql=rrequest.getDbType(sbean.getDatasource()).constructSplitPageSql(sbean,orderbys[0]+" "+orderbys[1]);
            }
            sql=Tools.replaceAll(sql,Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlKernel);
            sql=ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest,rbean,sql);
            sql=Tools.replaceAll(sql,"%START%",String.valueOf((pageno-1)*pagesize));
            sql=Tools.replaceAll(sql,"%END%",String.valueOf(pageno*pagesize));
            sql=Tools.replaceAll(sql,"%PAGESIZE%",String.valueOf(pagesize));
            if(rbean.getInterceptor()!=null)
            {
                Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,reportObj,sql);
                if(obj==null) return null;
                if(obj instanceof List||obj instanceof ResultSet) return obj;
                if(!(obj instanceof String))
                {
                    throw new WabacusRuntimeException("执行报表"+rbean.getPath()+"的加载数据拦截器失败，返回的数据类型"+obj.getClass().getName()+"不合法");
                }
                sql=(String)obj;
            }
            if(Config.show_sql)
            {
                log.info("Execute sql: "+sql);
            }
            pstmt=rrequest.getConnection(rbean.getSbean().getDatasource()).prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            for(int j=0;j<lstConditions.size();j++)
            {
                if(Config.show_sql)
                {
                    log.info("param"+(j+1)+"="+lstConditions.get(j));
                }
                lstConditionsTypes.get(j).setPreparedStatementValue(j+1,lstConditions.get(j),pstmt,dbtype);
            }
            rrequest.addUsedStatement(pstmt);
            return pstmt.executeQuery();
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取数据失败，执行SQL："+sql+"失败",e);
        }
    }
    
    private int getRecordcount(ReportRequest rrequest,AbsReportType reportObj,String sqlKernel)
    {
        ReportBean rbean=reportObj.getReportBean();
        String sqlCount=Tools.replaceAll(rbean.getSbean().getSqlCount(),Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlKernel);
        sqlCount=ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest,rbean,sqlCount);
        int recordcount=0;
        PreparedStatement pstmt=null;
        ResultSet rsCount=null;
        if(rbean.getInterceptor()!=null)
        {
            Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,reportObj,sqlCount);
            if(obj==null) return -1;
            if(obj instanceof List)
            {
                List lst=(List)obj;
                if(lst.size()==0)
                {
                    recordcount=0;
                }else
                {
                    if(!(lst.get(0) instanceof Integer))
                    {
                        throw new WabacusRuntimeException("拦截器返回的记录数不是合法数字");
                    }
                    recordcount=(Integer)lst.get(0);
                    if(recordcount<0) recordcount=0;
                }
                return recordcount;
            }else if(obj instanceof String)
            {
                sqlCount=(String)obj;
            }else if(obj instanceof ResultSet)
            {
                rsCount=(ResultSet)obj;
            }else
            {
                throw new WabacusRuntimeException("执行报表"+rbean.getPath()+"的加载数据拦截器失败，返回的数据类型"+obj.getClass().getName()+"不合法");
            }
        }
        try
        {
            if(rsCount==null)
            {
                pstmt=rrequest.getConnection(rbean.getSbean().getDatasource()).prepareStatement(sqlCount,ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                if(Config.show_sql)
                {
                    log.info("Execute sqlCount: "+sqlCount);
                }
                AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
                for(int j=0;j<lstConditions.size();j++)
                {
                    if(Config.show_sql)
                    {
                        log.info("param"+(j+1)+"="+lstConditions.get(j));
                    }
                    lstConditionsTypes.get(j).setPreparedStatementValue(j+1,lstConditions.get(j),pstmt,dbtype);
                }
                rsCount=pstmt.executeQuery();
            }
            rsCount.next();
            recordcount=rsCount.getInt(1);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取数据时执行SQL："+sqlCount+"失败",e);
        }finally
        {
            try
            {
                if(rsCount!=null) rsCount.close();
            }catch(SQLException e)
            {
                e.printStackTrace();
            }
            WabacusAssistant.getInstance().release(null,pstmt);
        }
        return recordcount;
    }
}
