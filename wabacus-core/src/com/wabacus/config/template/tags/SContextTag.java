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
package com.wabacus.config.template.tags;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class SContextTag extends AbsTagInTemplate
{
    public SContextTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }

    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        String value=this.mTagAttributes.get("value");
        if(value==null) return "";
        if(Tools.isDefineKey("request",value)||Tools.isDefineKey("session",value))
        {//如果是定义从request/session中取动态值显示
            return WabacusAssistant.getInstance().getRequestSessionValue(rrequest,value,"");
        }
        return value;
    }

    public String getTagname()
    {
        return Consts_Private.TAGNAME_CONTEXT;
    }

}

