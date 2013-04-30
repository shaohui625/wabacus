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
package com.wabacus.system.buttons;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public class DataImportButton extends WabacusButton
{
    public DataImportButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getButtonType()
    {
        return Consts.IMPORT_DATA;
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        return super.showButton(rrequest,getDataImportEvent());
    }
    
    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        return super.showButton(rrequest,getDataImportEvent(),button);
    }

    private String getDataImportEvent()
    {
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        String serverurl=Config.showreport_url+token+"PAGEID="+ccbean.getPageBean().getId()+"&REPORTID="+ccbean.getId()
                +"&ACTIONTYPE=ShowUploadFilePage&FILEUPLOADTYPE="+Consts_Private.FILEUPLOADTYPE_DATAIMPORTREPORT;
        ReportBean rbean=(ReportBean)ccbean;
        int winwidth=rbean.getDataimportwidth();
        if(winwidth<=0) winwidth=300;
        int winheight=rbean.getDataimportheight();
        winheight=160;
        String clickevent="wx_winpage('"+serverurl+"','上传数据文件',"+winwidth+","+winheight;
        clickevent+=","+(rbean.getDataimportmaxbtn()!=null&&rbean.getDataimportmaxbtn().toLowerCase().trim().equals("true"));
        clickevent+=","+(rbean.getDataimportminbtn()!=null&&rbean.getDataimportminbtn().toLowerCase().trim().equals("true"));
        clickevent+=",null);";
        if(rbean.getDataimportinitsize()!=null&&rbean.getDataimportinitsize().toLowerCase().trim().equals("max"))
        {
            clickevent+="ymPrompt.max();";
        }else if(rbean.getDataimportinitsize()!=null&&rbean.getDataimportinitsize().toLowerCase().trim().equals("min"))
        {
            clickevent+="ymPrompt.min();";
        }
        return clickevent;
    }

}

