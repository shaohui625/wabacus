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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.exception.WabacusRuntimeException;

public class AbsListReportStatiColBean implements Cloneable
{
    private String property;
    private int plainexcel_startcolidx;
    
    private int plainexcel_colspan;

    private String valuestyleproperty="";

    private Method getMethod;
    
    public int getPlainexcel_startcolidx()
    {
        return plainexcel_startcolidx;
    }

    public void setPlainexcel_startcolidx(int plainexcel_startcolidx)
    {
        this.plainexcel_startcolidx=plainexcel_startcolidx;
    }

    public int getPlainexcel_colspan()
    {
        return plainexcel_colspan;
    }

    public void setPlainexcel_colspan(int plainexcel_colspan)
    {
        this.plainexcel_colspan=plainexcel_colspan;
    }

    public String getValuestyleproperty()
    {
        return valuestyleproperty;
    }

    public void setValuestyleproperty(String valuestyleproperty)
    {
        this.valuestyleproperty=valuestyleproperty;
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(String property)
    {
        this.property=property;
    }
   
    public Method getGetMethod()
    {
        return getMethod;
    }

    public void setGetMethod(Method getMethod)
    {
        this.getMethod=getMethod;
    }

    public Object clone()
    {
        try
        {
            AbsListReportStatiColBean newBean=(AbsListReportStatiColBean)super.clone();
            return newBean;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
