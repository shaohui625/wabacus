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
package com.wabacus.system.intercept;
public class ColDataByInterceptor
{
    private String dynvalue;
    
    private String dynstyleproperty;
    
    private boolean styleOverwrite;
    
    private boolean readonly;
    
    public String getDynvalue()
    {
        return dynvalue;
    }

    public void setDynvalue(String dynvalue)
    {
        this.dynvalue=dynvalue;
    }

    public String getDynstyleproperty()
    {
        return dynstyleproperty;
    }

    public void setDynstyleproperty(String dynstyleproperty)
    {
        this.dynstyleproperty=dynstyleproperty;
    }

    public boolean isStyleOverwrite()
    {
        return styleOverwrite;
    }

    public void setStyleOverwrite(boolean styleOverwrite)
    {
        this.styleOverwrite=styleOverwrite;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly=readonly;
    }
}

