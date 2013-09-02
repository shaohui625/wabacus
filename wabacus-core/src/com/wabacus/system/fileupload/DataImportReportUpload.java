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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.buttons.DataImportButton;

public class DataImportReportUpload extends AbsFileUpload
{
    private DataImportButton dataImportButtonObj;
    
    public DataImportReportUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(Appendable out) throws IOException
    {
        String pageid=getRequestString("PAGEID","");
        String comid=getRequestString("COMPONENTID","");
        String buttonname=getRequestString("DATAIMPORT_BUTTONNAME","");
        DataImportButton buttonObj=getDataImportButtonObj(pageid,comid,buttonname);
        out.append("<input type='hidden' name='PAGEID' value='"+pageid+"'/>");
        out.append("<input type='hidden' name='COMPONENTID' value='"+comid+"'/>");
        out.append("<input type='hidden' name='DATAIMPORT_BUTTONNAME' value='"+buttonname+"'/>");
        boolean flag=true;
        if(buttonObj.getDataimportInterceptorObj()!=null)
        {
            Map<String,String> mFormFieldValues=(Map<String,String>)request.getAttribute("WX_FILE_UPLOAD_FIELDVALUES");
            request.setAttribute("LST_DATAIMPORT_CONFIGBEANS",buttonObj.getLstDataImportItems());
            flag=buttonObj.getDataimportInterceptorObj().beforeDisplayFileUploadInterface(request,mFormFieldValues,out);
        }
        if(flag)
        {
            out.append(showDataImportFileUpload(buttonObj.getLstDataImportFileNames()));
        }
    }

    public String doFileUpload(List lstFieldItems,Appendable out) throws IOException
    {
        String pageid=mFormFieldValues.get("PAGEID");
        String comid=mFormFieldValues.get("COMPONENTID");
        String buttonname=mFormFieldValues.get("DATAIMPORT_BUTTONNAME");
        this.dataImportButtonObj=getDataImportButtonObj(pageid,comid,buttonname);
        this.interceptorObj=this.dataImportButtonObj.getDataimportInterceptorObj();
        return uploadDataImportFiles(lstFieldItems,this.dataImportButtonObj.getLstDataImportItems(),this.dataImportButtonObj.isDataImportAsyn(),out);
    }
    
    private DataImportButton getDataImportButtonObj(String pageid,String comid,String buttonname)
    {
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("页面ID："+pageid+"不存在");
        }
        IComponentConfigBean ccbeanTmp=null;
        if(comid.equals(pageid))
        {
            ccbeanTmp=pbean;
        }else
        {
            ccbeanTmp=pbean.getChildComponentBean(comid,true);
            if(ccbeanTmp==null)
            {
                throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+comid+"的子组件");
            }
        }
        DataImportButton buttonObj=(DataImportButton)ccbeanTmp.getButtonsBean().getButtonByName(buttonname);
        if(buttonObj==null)
        {
            throw new WabacusRuntimeException("组件"+ccbeanTmp.getPath()+"下不存在name为"+buttonname+"的数据导入按钮");
        }
        return buttonObj;
    }
    
    public void promptSuccess(Appendable out,boolean isArtDialog)  throws IOException
    {
        String message="";
        if(this.dataImportButtonObj.isDataImportAsyn())
        {
            message="数据文件上传成功";
        }else
        {
            message="数据文件导入成功!";
        }
        String parentRef=null;
        if(isArtDialog)
        {
            out.append("artDialog.open.origin.wx_success('"+message+"');\n");
            out.append("art.dialog.close();\n");
            parentRef="artDialog.open.origin";
        }else
        {
            out.append("parent.wx_success('"+message+"');\n");
            out.append("parent.closePopupWin();");
            parentRef="parent";
        }
        IComponentConfigBean ccbean=this.dataImportButtonObj.getCcbean();
        if(!this.dataImportButtonObj.isDataImportAsyn())
        {
            out.append(parentRef+".refreshComponentDisplay('"+ccbean.getPageBean().getId()+"','"+ccbean.getId()+"',true);\n");
        }
    }
}
