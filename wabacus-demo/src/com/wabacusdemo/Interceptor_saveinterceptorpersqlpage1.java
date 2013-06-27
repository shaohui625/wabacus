/* 
 * Copyright (C) 2010-2012 星星<349446658@qq.com>
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

import java.util.Iterator;
import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.intercept.AbsInterceptorDefaultAdapter;

public class Interceptor_saveinterceptorpersqlpage1 extends AbsInterceptorDefaultAdapter
{
    public int beforeSavePerSql(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,String sql)
    {
        System.out.print("正在执行sql语句："+sql+"，保存的记录为：");
        if(mRowData!=null)//mRowData中存放了本条记录各列的值
        {
            Iterator itKeys=mRowData.keySet().iterator();
            while(itKeys.hasNext())
            {
                String key=(String)itKeys.next();
                String value=(String)mRowData.get(key);
                System.out.print("["+key+"="+value+"]");
            }
            System.out.println();
        }
        if(mExternalValues!=null)//存放了相对于本条记录的在<insert/>或<update/>或<delete/>中定义的变量值
        {
            System.out.print("各变量的值为：");
            Iterator itKeys=mExternalValues.keySet().iterator();
            while(itKeys.hasNext())
            {
                String key=(String)itKeys.next();
                String value=(String)mExternalValues.get(key);
                System.out.print("["+key+"="+value+"]");
            }
            System.out.println();
        }
        return WX_CONTINUE;
    }

    public int afterSavePerSql(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,String sql)
    {
        System.out.print("已经执行完sql语句："+sql+"，保存的记录为：");
        if(mRowData!=null)//mRowData中存放了本条记录各列的值
        {
            Iterator itKeys=mRowData.keySet().iterator();
            while(itKeys.hasNext())
            {
                String key=(String)itKeys.next();
                String value=(String)mRowData.get(key);
                System.out.print("["+key+"="+value+"]");
            }
            System.out.println();
        }
        if(mExternalValues!=null)//存放了相对于本条记录的在<insert/>或<update/>或<delete/>中定义的变量值
        {
            System.out.print("各变量的值为：");
            Iterator itKeys=mExternalValues.keySet().iterator();
            while(itKeys.hasNext())
            {
                String key=(String)itKeys.next();
                String value=(String)mExternalValues.get(key);
                System.out.print("["+key+"="+value+"]");
            }
            System.out.println();
        }
        return WX_CONTINUE;
    }

}
