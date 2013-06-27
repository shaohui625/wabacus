/* 
 * Copyright (C) 2010-2011 星星<349446658@qq.com>
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
package com.wabacusdemo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.OptionBean;
import com.wabacus.system.intercept.AbsInterceptorDefaultAdapter;

public class Interceptor_resultsetpage3Report2 extends AbsInterceptorDefaultAdapter
{

    public Object beforeLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,String sql)
    {
        if(typeObj instanceof OptionBean)
        {
            sql=sql.replaceAll("%mycondition%","deptno like '001%'");//只显示部门编号以001开头的部门
            PreparedStatement pstmt=null;
            List lstResults=new ArrayList();
            try
            {
                Connection conn=rrequest.getConnection(rbean.getSbean().getDatasource());
                pstmt=conn.prepareStatement(sql);
                ResultSet rs=pstmt.executeQuery();
                while(rs.next())
                {
                    String[] strTmp=new String[2];
                    strTmp[0]=rs.getString(((OptionBean)typeObj).getLabel());//取到下拉选项的显示label
                    strTmp[1]=rs.getString(((OptionBean)typeObj).getValue());//取到下拉选项的value
                    lstResults.add(strTmp);
                }
                rs.close();
            }catch(Exception e)
            {
                e.printStackTrace();
            }finally
            {
                if(pstmt!=null)
                {
                    try
                    {
                        pstmt.close();
                    }catch(SQLException e)
                    {
                        e.printStackTrace();
                    }

                }
            }
            return lstResults;
        }
        //其它的数据库查询操作，不做修改，直接原样返回sql
        return sql;
    }

}

