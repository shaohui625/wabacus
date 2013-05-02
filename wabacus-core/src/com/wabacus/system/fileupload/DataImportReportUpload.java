/* 
 * Copyright (C) 2010---2013 星星(wuweixing)<349446658@qq.com>
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
package com.wabacus.system.fileupload;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusRuntimeException;

public class DataImportReportUpload extends AbsFileUpload
{
    public DataImportReportUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(PrintWriter out)
    {
        String pageid=getRequestString("PAGEID","");
        String reportid=getRequestString("REPORTID","");
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("页面ID："+pageid+"不存在");
        }
        ReportBean rbean=pbean.getReportChild(reportid,true);
        if(rbean==null)
        {
            throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+rbean.getId()+"的报表");
        }
        out.print("<input type='hidden' name='PAGEID' value='"+pageid+"'/>");
        out.print("<input type='hidden' name='REPORTID' value='"+reportid+"'/>");
        out.print(showDataImportFileUpload(rbean.getLstDataImportFileNames()));
    }

    public String doFileUpload(List lstFieldItems,Map<String,String> mFormFieldValues,PrintWriter out)
    {
        String pageid=mFormFieldValues.get("PAGEID");
        String reportid=mFormFieldValues.get("REPORTID");
        pageid=pageid==null?"":pageid.trim();
        reportid=reportid==null?"":reportid.trim();

        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("页面ID："+pageid+"不存在");
        }
        ReportBean rbean=pbean.getReportChild(reportid,true);
        if(rbean==null)
        {
            throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+rbean.getId()+"的报表");
        }
        return uploadDataImportFiles(lstFieldItems,rbean.getLstDataImportItems());
    }
}
