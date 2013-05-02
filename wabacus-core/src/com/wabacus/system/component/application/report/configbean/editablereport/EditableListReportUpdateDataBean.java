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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.EditableListReportType;
import com.wabacus.util.Tools;

public class EditableListReportUpdateDataBean extends EditableReportUpdateDataBean
{
    private EditableListReportInsertUpdateBean realUpdateBean;

    public EditableListReportUpdateDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        super(owner);
    }

    public EditableListReportInsertUpdateBean getRealUpdateBean()
    {
        return realUpdateBean;
    }

    public void setRealUpdateBean(EditableListReportInsertUpdateBean realUpdateBean)
    {
        this.realUpdateBean=realUpdateBean;
    }

    public int parseActionscripts(String reportTypeKey)
    {
        if(this.realUpdateBean.getLstUrlParams()!=null)
        {
            for(Map<String,String> mParamTmp:this.realUpdateBean.getLstUrlParams())
            {
                if(mParamTmp==null||mParamTmp.size()==0) continue;
                if(Tools.isDefineKey("url",mParamTmp.entrySet().iterator().next().getValue()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()
                            +"失败，editablelist报表类型的<update/>中urlparams参数不能配置为url{name}，可以配置为request/session{key}和@{column}格式");
                }
            }
        }
        return 1;
    }

    public String assembleAccessPageUrl(ReportRequest rrequest,EditableListReportType reportTypeObj,Object dataObj)
    {
        return this.realUpdateBean.assembleAccessPageUrl(rrequest,reportTypeObj,dataObj);
    }

    public void doPostLoad()
    {
       this.realUpdateBean.doPostLoad();
    }

    public void setRealParamnamesInDoPostLoadFinally()
    {
        
    }
    
    public Object clone(IEditableReportEditGroupOwnerBean newowner)
    {
        EditableListReportUpdateDataBean newbean=(EditableListReportUpdateDataBean)super.clone(newowner);
        if(this.realUpdateBean!=null)
        {
            newbean.setRealUpdateBean((EditableListReportInsertUpdateBean)realUpdateBean.clone(newbean));
        }
        return newbean;
    }
}
