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
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.util.Consts_Private;

public class SDataImportTag extends AbsTagInTemplate
{

    public SDataImportTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }

    public String getTagname()
    {
        return Consts_Private.TAGNAME_DATAIMPORT;
    }

    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        String ref=null;
        String dataimportwidth=null;
        String dataimportheight=null;
        String dataimportmaxbtn=null;
        String dataimportminbtn=null;
        String dataimportinitsize=null;
        //        String includejscss="false";//是否由自己提供必须的js和css文件
        if(this.mTagAttributes!=null)
        {
            ref=this.mTagAttributes.get("ref");
            dataimportwidth=this.mTagAttributes.get("dataimportwidth");
            dataimportheight=this.mTagAttributes.get("dataimportheight");
            
            dataimportinitsize=this.mTagAttributes.get("dataimportinitsize");
            dataimportmaxbtn=this.mTagAttributes.get("dataimportmaxbtn");
            dataimportminbtn=this.mTagAttributes.get("dataimportminbtn");
        }
        return TagAssistant.getInstance().getDataImportDisplayValue(ref,dataimportwidth,dataimportheight,dataimportinitsize,dataimportmaxbtn,
                dataimportminbtn,this.tagContent,rrequest.getRequest());
    }
}
