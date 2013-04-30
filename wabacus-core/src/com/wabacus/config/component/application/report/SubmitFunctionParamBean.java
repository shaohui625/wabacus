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
package com.wabacus.config.component.application.report;

import java.util.ArrayList;
import java.util.List;

public class SubmitFunctionParamBean implements Cloneable
{
    private String name;
    
    private String value;

    private List<String> lstParamValues;

    public SubmitFunctionParamBean(String name)
    {
        this.name=name;
    }
    
    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public List<String> getLstParamValues()
    {
        return lstParamValues;
    }

    public void setLstParamValues(List<String> lstParamValues)
    {
        this.lstParamValues=lstParamValues;
    }

    public Object clone()
    {
        try
        {
            SubmitFunctionParamBean paramBeanNew=(SubmitFunctionParamBean)super.clone();
            if(this.lstParamValues!=null)
            {
                paramBeanNew.setLstParamValues((List)((ArrayList)lstParamValues).clone());
            }
            return paramBeanNew;
        }catch(Exception e)
        {
            e.printStackTrace();
            return this;
        }
    }
}
