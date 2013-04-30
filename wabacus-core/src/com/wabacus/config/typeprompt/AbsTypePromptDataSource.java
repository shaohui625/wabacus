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
package com.wabacus.config.typeprompt;

import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;

public abstract class AbsTypePromptDataSource implements Cloneable
{
    protected TypePromptBean promptConfigBean;

    public TypePromptBean getPromptConfigBean()
    {
        return promptConfigBean;
    }

    public void setPromptConfigBean(TypePromptBean promptConfigBean)
    {
        this.promptConfigBean=promptConfigBean;
    }

    public void loadExternalConfig(ReportBean rbean,XmlElementBean eleDataSourceBean)
    {}

    public void doPostLoad(ReportBean rbean)
    {}
    
    public abstract List<Map<String,String>> getResultDataList(ReportRequest rrequest,
            ReportBean rbean,String typedata);

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
