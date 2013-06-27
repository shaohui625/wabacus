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

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.intercept.AbsInterceptorDefaultAdapter;
import com.wabacus.system.intercept.ColDataByInterceptor;

public class Interceptor_rowgroupbgcolorpage1Report2 extends AbsInterceptorDefaultAdapter
{
    public ColDataByInterceptor beforeDisplayReportDataPerCol(AbsReportType reportTypeObj,
            ReportRequest rrequest,Object displayColBean,int rowindex,String value)
    {
        if(rowindex<0)
        {//当前是在显示标题行
            return null;
        }
        if(!(displayColBean instanceof ColBean))
        {//不是显示列的数据
            return null;
        }
        ColBean cb=(ColBean)displayColBean;
        if("province".equals(cb.getColumn())||"city".equals(cb.getColumn())
                ||"county".equals(cb.getColumn()))
        {//如果是分组列，则不改变它的背景色
            return null;
        }

        Object dataObj=reportTypeObj.getLstReportData().get(rowindex);//取出存放当前行数据的POJO对象
        String sex=String.valueOf(ReportAssistant.getInstance().getPropertyValue(dataObj,"sex"));//从中取出sex列的数据
        if(sex.equals("女"))
        {
            ColDataByInterceptor cbi=new ColDataByInterceptor();
            cbi.setDynstyleproperty("bgcolor='#CFDFF8'");
            return cbi;
        }else
        {//男
            return null;
        }
    }

}
