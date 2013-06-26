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

public class SFileUploadTag extends AbsTagInTemplate
{
    public SFileUploadTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }

    public String getTagname()
    {
        return Consts_Private.TAGNAME_FILEUPLOAD;
    }

    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        String maxsize=null;
        String allowtypes=null;
        String uploadcount="1";
        String newfilename=null;
        String savepath=null;//上传文件保存路径
        String rooturl=null;
        String width=null;
        String height=null;
        String initsize=null;
        String maxbtn=null;
        String minbtn=null;
        String interceptor=null;
        //String includejscss=null;//是否要包含ymPrompt.js和ymPrompt.css文件
        if(this.mTagAttributes!=null)
        {
            maxsize=this.mTagAttributes.get("maxsize");
            allowtypes=this.mTagAttributes.get("allowtypes");
            uploadcount=this.mTagAttributes.get("uploadcount");
            newfilename=this.mTagAttributes.get("newfilename");
            savepath=this.mTagAttributes.get("savepath");
            rooturl=this.mTagAttributes.get("rooturl");
            width=this.mTagAttributes.get("width");
            height=this.mTagAttributes.get("height");
            interceptor=this.mTagAttributes.get("interceptor");
            //includejscss=this.mTagAttributes.get("includejscss");
            initsize=this.mTagAttributes.get("initsize");
            maxbtn=this.mTagAttributes.get("maxbtn");
            minbtn=this.mTagAttributes.get("minbtn");
        }
        return TagAssistant.getInstance().getFileUploadDisplayValue(maxsize,allowtypes,uploadcount,
                newfilename,savepath,rooturl,width,height,initsize,maxbtn,minbtn,interceptor,this.tagContent,rrequest.getRequest());
    }
}
