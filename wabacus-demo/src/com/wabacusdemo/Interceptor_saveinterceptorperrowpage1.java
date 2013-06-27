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

public class Interceptor_saveinterceptorperrowpage1 extends AbsInterceptorDefaultAdapter
{
    public int beforeSavePerRow(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,int updatetype)
    {
        String message="";
        if(updatetype==WX_INSERT)
        {//对本条记录做添加操作
            message="正在添加记录：";
        }else   if(updatetype==WX_UPDATE)
        {//对本条记录做修改操作
            message="正在修改记录：";
        }else if(updatetype==WX_DELETE)
        {//对本条记录做删除操作
            message="正在删除记录：";
        }
        if(message!=null)
        {
            System.out.print(message);
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
        }
        return WX_CONTINUE;
    }

    public int afterSavePerRow(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,int updatetype)
    {
        String message="";
        if(updatetype==WX_INSERT)
        {//对本条记录做添加操作
            message="添加完记录：";
        }else   if(updatetype==WX_UPDATE)
        {//对本条记录做修改操作
            message="修改完记录：";
        }else if(updatetype==WX_DELETE)
        {//对本条记录做删除操作
            message="删除完记录：";
        }
        if(message!=null)
        {
            System.out.print(message);
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
        }
        return WX_CONTINUE;
    }

}
