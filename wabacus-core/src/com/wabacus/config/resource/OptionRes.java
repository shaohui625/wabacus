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
package com.wabacus.config.resource;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.inputbox.OptionBean;
import com.wabacus.util.Tools;

public class OptionRes extends AbsResource
{
    public Object getValue(Element itemElement)
    {
        if(itemElement==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中没有配置选项资源项");
        }
        Element eleOptions=itemElement.element("options");
        if(eleOptions==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中配置的资源项"
                    +itemElement.attributeValue("key")+"不是有效的选项资源项，必须以<options/>做为其顶层标签");
        }
        List lstOptionElements=eleOptions.elements("option");
        if(lstOptionElements==null||lstOptionElements.size()==0)
        {
            return new ArrayList<OptionBean>();
        }
        List<OptionBean> lstOptions=new ArrayList<OptionBean>();
        Element eleOption;
        String label;
        String value;
        for(int i=0;i<lstOptionElements.size();i++)
        {
            eleOption=(Element)lstOptionElements.get(i);
            if(eleOption==null) continue;
            OptionBean oBean=new OptionBean();
            label=eleOption.attributeValue("label");
            label=label==null?"":label.trim();
            oBean.setLabel(label);
            value=eleOption.attributeValue("value");
            value=value==null?"":value.trim();
            oBean.setValue(value);
            String type=eleOption.attributeValue("type");
            if(type!=null)
            {
                String[] typearray=Tools.parseStringToArray(type,'[',']');
                oBean.setType(typearray);
            }
            lstOptions.add(oBean);
        }
        return lstOptions;
    }
}
