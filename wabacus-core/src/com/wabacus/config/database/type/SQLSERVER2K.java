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
package com.wabacus.config.database.type;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.datatype.BigdecimalType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.BooleanType;
import com.wabacus.system.datatype.ByteType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.FloatType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.LongType;
import com.wabacus.system.datatype.ShortType;
import com.wabacus.system.datatype.TimestampType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class SQLSERVER2K extends AbsDatabaseType
{
    private static Log log=LogFactory.getLog(SQLSERVER2K.class);

    public String constructSplitPageSql(SqlBean sbean)
    {
        String sql=sbean.getSqlWithoutOrderby();
        String orderby=sbean.getOrderby();
        if(orderby==null||orderby.trim().equals("")||sql==null||sql.indexOf("%orderby%")<=0)
        {
            throw new WabacusConfigLoadingException("报表"+sbean.getReportBean().getPath()+"配置的查询数据脚本："+sbean.getValue()
                    +"没有order by子句，无法在SQLSERVER2000数据库上进行分页");
        }
        String[] orderbyarr=getPageSplitOrderByArray(sbean,orderby);
        sql=Tools.replaceAll(sql,"%orderby%","");
        
        sql="select * from (select top %PAGESIZE% * from (select top %END% * from ("+sql+") as jd_temp_tbl1 "+orderbyarr[0]+") as jd_temp_tbl2 "
                +orderbyarr[1]+") as jd_temp_tbl3 "+orderbyarr[0];
        return sql;
    }

    public String constructSplitPageSql(SqlBean sbean,String dynorderby)
    {
        String sql=sbean.getSqlWithoutOrderby();
        dynorderby=ReportAssistant.getInstance().mixDynorderbyAndRowgroupCols(sbean.getReportBean(),dynorderby);
        String[] orderbyarr=getPageSplitOrderByArray(sbean,dynorderby);
        sql=Tools.replaceAll(sql,"%orderby%","");
        
        sql="select * from (select top %PAGESIZE% * from (select top %END% * from ("+sql+") as jd_temp_tbl1 "+orderbyarr[0]+") as jd_temp_tbl2 "
                +orderbyarr[1]+") as jd_temp_tbl3 "+orderbyarr[0];
        return sql;
    }

    private String[] getPageSplitOrderByArray(SqlBean sbean,String orderby)
    {
        List<String> lstOrderByColumns=Tools.parseStringToList(orderby,",");
        StringBuffer sbufferOrder=new StringBuffer();
        StringBuffer sbufferOrder_reverse=new StringBuffer();
        for(String orderbyTmp:lstOrderByColumns)
        {
            if(orderbyTmp==null||orderbyTmp.trim().equals("")) continue;
            orderbyTmp=orderbyTmp.trim();
            List<String> lstTemp=Tools.parseStringToList(orderbyTmp," ");
            if(sbufferOrder.length()>0&&sbufferOrder_reverse.length()>0)
            {
                sbufferOrder.append(",");
                sbufferOrder_reverse.append(",");
            }
            if(lstTemp.size()==1)
            {
                sbufferOrder.append(lstTemp.get(0)).append(" asc");
                sbufferOrder_reverse.append(lstTemp.get(0)).append(" desc");
            }else if(lstTemp.size()==2)
            {
                String ordertype=lstTemp.get(1).trim().toLowerCase();
                if(ordertype.equals("desc"))
                {
                    sbufferOrder.append(lstTemp.get(0)).append(" desc");
                    sbufferOrder_reverse.append(lstTemp.get(0)).append(" asc");
                }else
                {
                    sbufferOrder.append(lstTemp.get(0)).append(" asc");
                    sbufferOrder_reverse.append(lstTemp.get(0)).append(" desc");
                }
            }else
            {
                throw new WabacusConfigLoadingException("报表"+sbean.getReportBean().getPath()+"配置的SQL语句中order by子句"+orderby+"不合法");
            }
        }
        return new String[] { "order by "+sbufferOrder.toString(), "order by "+sbufferOrder_reverse.toString() };
    }

    public IDataType getWabacusDataTypeByColumnType(String columntype)
    {
        if(columntype==null||columntype.trim().equals("")) return null;
        columntype=columntype.toLowerCase().trim();
        IDataType dataTypeObj=null;
        if(columntype.equals("varchar")||columntype.equals("nvarchar")||columntype.equals("char")||columntype.equals("nchar"))
        {
            dataTypeObj=new VarcharType();
        }else if(columntype.equals("bit"))
        {
            dataTypeObj=new BooleanType();
        }else if(columntype.equals("tinyint"))
        {
            dataTypeObj=new ByteType();
        }else if(columntype.equals("smallint"))
        {
            dataTypeObj=new ShortType();
        }else if(columntype.equals("int"))
        {
            dataTypeObj=new IntType();
        }else if(columntype.equals("bigint"))
        {
            dataTypeObj=new LongType();
        }else if(columntype.equals("binary")||columntype.equals("varbinary")||columntype.equals("image"))
        {
            dataTypeObj=new BlobType();
        }else if(columntype.indexOf("datetime")>=0)
        {
            dataTypeObj=new DateType();
        }else if(columntype.equals("decimal")||columntype.equals("numeric")||columntype.equals("money")||columntype.equals("smallmoney"))
        {
            dataTypeObj=new BigdecimalType();
        }else if(columntype.equals("float"))
        {
            dataTypeObj=new FloatType();
        }else if(columntype.equals("double")||columntype.equals("real"))
        {
            dataTypeObj=new DoubleType();
        }else if(columntype.equals("timestamp"))
        {
            dataTypeObj=new TimestampType();
        }else if(columntype.equals("text")||columntype.equals("ntext"))
        {
            dataTypeObj=new ClobType();
        }else
        {
            log.warn("数据类型："+columntype+"不支持，将当做varchar类型");
            dataTypeObj=new VarcharType();
        }
        return dataTypeObj;
    }
}