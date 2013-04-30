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
import com.wabacus.util.Consts;

public interface IInterceptor
{
    public final int WX_TERMINATE=Consts.RETURNVALUE_TERMINAGE;

    public final int WX_IGNORE=Consts.RETURNVALUE_IGNORE;

    public final int WX_CONTINUE=Consts.RETURNVALUE_CONTINUE;

    public final int WX_INSERT=Consts.UPDATETYPE_INSERT;

    public final int WX_UPDATE=Consts.UPDATETYPE_UPDATE;//修改记录的操作

    public final int WX_DELETE=Consts.UPDATETYPE_DELETE;

    public void doStart(ReportRequest rrequest,ReportBean rbean);

    public int beforeSave(ReportRequest rrequest,ReportBean rbean);

    public int beforeSavePerRow(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,int updatetype);

    public int beforeSavePerSql(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,String sql);

    public int afterSavePerSql(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,String sql);

    public int afterSavePerRow(ReportRequest rrequest,ReportBean rbean,Map mRowData,Map mExternalValues,int updatetype);

    public void afterSave(ReportRequest rrequest,ReportBean rbean);

    public Object beforeLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,String sql);

    public Object afterLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,Object dataObj);

    public RowDataByInterceptor beforeDisplayReportDataPerRow(AbsReportType reportTypeObj,ReportRequest rrequest,int rowindex,int colspans,List lstColBeans);

    public ColDataByInterceptor beforeDisplayReportDataPerCol(AbsReportType reportTypeObj,ReportRequest rrequest,Object displayColBean,int rowindex,
            String value);

    public void doEnd(ReportRequest rrequest,ReportBean rbean);
}
