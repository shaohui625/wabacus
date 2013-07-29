/* 
 * Copyright (C) 2010-2012 吴卫华(wuweihua)<349446658@qq.com>
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
package com.wabacus.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

public class AbstractConfigExtendable
{

    
    
    protected Map<String,String> attrs;

    public Map<String,String> getAttrs()
    {
        return attrs == null ? MapUtils.EMPTY_MAP : attrs;
    }

    public void setAttrs(Map<String,String> attrs)
    {
        this.attrs=attrs;
    }
    
    public void mergeAttrs(Map<String,String> overrideAttrs){
        if(overrideAttrs == null || overrideAttrs.isEmpty()){
            return;
        }
        if(attrs == null){
            attrs = new HashMap<String,String>();
        }
        attrs.putAll(overrideAttrs);
    }    
}

