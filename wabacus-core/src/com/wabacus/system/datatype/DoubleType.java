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
package com.wabacus.system.datatype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.database.type.AbsDatabaseType;

public class DoubleType extends AbsNumberType
{
    private final static Log log=LogFactory.getLog(DoubleType.class);

    private final static Map<String,AbsNumberType> mDoubleTypeObjects=new HashMap<String,AbsNumberType>();
    
    public Object getColumnValue(ResultSet rs,String column,AbsDatabaseType dbtype)
            throws SQLException
    {
        return Double.valueOf(rs.getDouble(column));
    }

    public Object getColumnValue(ResultSet rs,int iindex,AbsDatabaseType dbtype)
            throws SQLException
    {
        return Double.valueOf(rs.getDouble(iindex));
    }

    public void setPreparedStatementValue(int iindex,String value,PreparedStatement pstmt,
            AbsDatabaseType dbtype) throws SQLException
    {
        log.debug("setDouble("+iindex+","+value+")");
        Object objTmp=label2value(value);
        if(objTmp==null)
        {
            pstmt.setObject(iindex,null,java.sql.Types.DOUBLE);
        }else
        {
            pstmt.setDouble(iindex,(Double)objTmp);
        }
    }

    
    
    
    

    public Class getJavaTypeClass()
    {
        return Double.class;
    }

    public Object label2value(String label)
    {
        if(label==null||label.trim().equals("")) return null;
        if(this.numberformat!=null&&!this.numberformat.trim().equals(""))
        {
            return Double.valueOf(this.getNumber(label.trim()).doubleValue());
        }else
        {
            return Double.valueOf(label.trim());
        }
    }

    public String value2label(Object value)
    {
        if(value==null) return "";
        if(!(value instanceof Double)) return String.valueOf(value);
        if(this.numberformat!=null&&!this.numberformat.trim().equals(""))
        {
            DecimalFormat df=new DecimalFormat(this.numberformat);
            return df.format((Double)value);
        }else
        {
            return String.valueOf(value);
        }
    }

    protected Map<String,AbsNumberType> getAllMNumberTypeObjects()
    {
        return mDoubleTypeObjects;
    }
}
