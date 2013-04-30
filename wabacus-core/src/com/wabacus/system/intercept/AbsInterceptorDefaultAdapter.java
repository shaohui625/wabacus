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
package com.wabacus.system.intercept;

import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public abstract class AbsInterceptorDefaultAdapter implements IInterceptor
{
    public void doStart(ReportRequest rrequest,ReportBean rbean)
    {}

    public int beforeSave(ReportRequest rrequest,ReportBean rbean)
    {
        return WX_CONTINUE;
    }

    public int beforeSavePerRow(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,int updatetype)
    {
        return WX_CONTINUE;
    }

    public int beforeSavePerSql(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,String sql)
    {
        return WX_CONTINUE;
    }

    public int afterSavePerSql(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,String sql)
    {
        return WX_CONTINUE;
    }

    public int afterSavePerRow(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,int updatetype)
    {
        return WX_CONTINUE;
    }

    public void afterSave(ReportRequest rrequest,ReportBean rbean)
    {}

    public void doEnd(ReportRequest rrequest,ReportBean rbean)
    {}

    public Object beforeLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,String sql)
    {
        return sql;
    }

    public Object afterLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,Object dataObj)
    {
        return dataObj;
    }

    public RowDataByInterceptor beforeDisplayReportDataPerRow(AbsReportType reportTypeObj,ReportRequest rrequest,int rowindex,int colspans,
            List lstColBeans)
    {
        return null;
    }

    public ColDataByInterceptor beforeDisplayReportDataPerCol(AbsReportType reportTypeObj,ReportRequest rrequest,Object displayColBean,int rowindex,
            String value)
    {
        return null;
    }
}
