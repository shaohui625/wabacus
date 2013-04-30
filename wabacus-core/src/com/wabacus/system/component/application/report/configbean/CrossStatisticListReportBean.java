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
package com.wabacus.system.component.application.report.configbean;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public class CrossStatisticListReportBean extends AbsExtendConfigBean
{
    private String dataheaderformatContent;
    
    private List<String> lstDataHeaderFormatImports;
    
    private Class dataHeaderPojoClass;

    public CrossStatisticListReportBean(AbsConfigBean owner)
    {
        super(owner);
    }
    
    public String getDataheaderformatContent()
    {
        return dataheaderformatContent;
    }

    public void setDataheaderformatContent(String dataheaderformatContent)
    {
        this.dataheaderformatContent=dataheaderformatContent;
    }

    public List<String> getLstDataHeaderFormatImports()
    {
        return lstDataHeaderFormatImports;
    }

    public void setLstDataHeaderFormatImports(List<String> lstDataHeaderFormatImports)
    {
        this.lstDataHeaderFormatImports=lstDataHeaderFormatImports;
    }

    public Class getDataHeaderPojoClass()
    {
        return dataHeaderPojoClass;
    }

    public void setDataHeaderPojoClass(Class dataHeaderPojoClass)
    {
        this.dataHeaderPojoClass=dataHeaderPojoClass;
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        CrossStatisticListReportBean cslrbeanNew=(CrossStatisticListReportBean)super.clone(owner);
        if(lstDataHeaderFormatImports!=null)
        {
            cslrbeanNew.setLstDataHeaderFormatImports((List<String>)((ArrayList<String>)lstDataHeaderFormatImports).clone());
        }
        return cslrbeanNew;
    }
}
