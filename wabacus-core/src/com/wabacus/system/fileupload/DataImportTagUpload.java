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
package com.wabacus.system.fileupload;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.wabacus.config.Config;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.util.Tools;

public class DataImportTagUpload extends AbsFileUpload
{

    public DataImportTagUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(PrintWriter out)
    {
        String ref=getRequestString("DATAIMPORT_REF","");
        if(ref==null||ref.trim().equals(""))
        {
            throw new WabacusRuntimeException("显示数据导入标签<wx:dataimport/>的文件上传页面时，没有取到DATAIMPORT_REF参数值");
        }
        List<String> lstDataImportFileNames=new ArrayList<String>();
        List<String> lst=Tools.parseStringToList(ref,"|");
        String fileuploadpath=null;
        for(String strTmp:lst)
        {
            if(strTmp.equals("")) continue;
            Object obj=Config.getInstance().getResources().get(null,strTmp,true);
            if(!(obj instanceof AbsDataImportConfigBean))
            {
                throw new WabacusConfigLoadingException("<dataimport/>中通过ref属性引用的数据导出项"+strTmp+"对应的资源项不是数据导出项资源类型");
            }
            String filepath=((AbsDataImportConfigBean)obj).getFilepath();
            String filename=((AbsDataImportConfigBean)obj).getFilename();
            filepath=filepath==null?"":filepath.trim();
            filename=filename==null?"":filename.trim();
            if(!lstDataImportFileNames.contains(filename)) lstDataImportFileNames.add(filename);
            if(fileuploadpath==null)
            {
                fileuploadpath=filepath;
            }else if(!fileuploadpath.equals(filepath))
            {
                throw new WabacusConfigLoadingException("显示<wx:dataimport/>失败，被它引用的所有数据导入项的filepath必须相同");
            }
        }
        if(lstDataImportFileNames.size()==0)
        {
            throw new WabacusRuntimeException("显示数据导入标签<wx:dataimport/>的文件上传页面时，没有取到要导入的filename");
        }
        out.print("<input type='hidden' name='DATAIMPORT_REF' value='"+ref+"'/>");
        out.print(showDataImportFileUpload(lstDataImportFileNames));
    }

    public String doFileUpload(List lstFieldItems,Map<String,String> mFormFieldValues,PrintWriter out)
    {
        String ref=mFormFieldValues.get("DATAIMPORT_REF");
        if(ref==null||ref.trim().equals(""))
        {
            throw new WabacusRuntimeException("显示数据导入标签<wx:dataimport/>的文件上传页面时，没有取到DATAIMPORT_REF参数值");
        }
        List<AbsDataImportConfigBean> lstDataImports=new ArrayList<AbsDataImportConfigBean>();
        List<String> lst=Tools.parseStringToList(ref,"|");
        for(String strTmp:lst)
        {
            if(strTmp.equals("")) continue;
            Object obj=Config.getInstance().getResources().get(null,strTmp,true);
            if(!(obj instanceof AbsDataImportConfigBean))
            {
                throw new WabacusConfigLoadingException("<dataimport/>中通过ref属性引用的数据导出项"+strTmp+"对应的资源项不是数据导出项资源类型");
            }
            lstDataImports.add((AbsDataImportConfigBean)obj);
        }
        return uploadDataImportFiles(lstFieldItems,lstDataImports);
    }
}
