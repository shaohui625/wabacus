/* 
 * Copyright (C) 2010 星星<349446658@qq.com>
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

import java.util.List;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.intercept.AbsInterceptorDefaultAdapter;
import com.wabacus.system.intercept.RowDataByInterceptor;

public class Interceptor_commonrowbgcolorpage1Report2 extends AbsInterceptorDefaultAdapter
{

    public RowDataByInterceptor beforeDisplayReportDataPerRow(AbsReportType reportTypeObj,ReportRequest rrequest,int rowindex,int colspans,
            List lstColBeans)
    {
        if(rowindex<0)
        {//当前是在显示标题行
            return null;
        }
        RowDataByInterceptor rdata=new RowDataByInterceptor();
        if(reportTypeObj.getLstReportData()!=null&&rowindex<reportTypeObj.getLstReportData().size())
        {
            Object dataObj=reportTypeObj.getLstReportData().get(rowindex);//取出存放当前行数据的POJO对象
            String sex=String.valueOf(ReportAssistant.getInstance().getPropertyValue(dataObj,"sex"));//从中取出sex列的数据
            if(sex.equals("女"))
            {
                rdata.setDynTrStyleproperty("bgcolor='#CCCAFF'");
            }else
            {//男
                rdata.setDynTrStyleproperty("bgcolor='#FFFFFF'");
            }
        }
        return rdata;
    }

    public String beforeDisplayReportDataPerRow(AbsListReportType reportTypeObj,ReportRequest rrequest,int rowindex)
    {
        if(rowindex<0)
        {//当前是在显示标题行
            return null;
        }
        if(reportTypeObj.getLstReportData()!=null&&rowindex<reportTypeObj.getLstReportData().size())
        {
            Object dataObj=reportTypeObj.getLstReportData().get(rowindex);//取出存放当前行数据的POJO对象
            String sex=String.valueOf(ReportAssistant.getInstance().getPropertyValue(dataObj,"sex"));//从中取出sex列的数据
            if(sex.equals("女"))
            {
                return "bgcolor='#CCCAFF'";
            }else
            {//男
                return "bgcolor='#FFFFFF'";
            }
        }
        return null;
    }

}
