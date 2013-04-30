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
package com.wabacus.system.fileupload;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.intercept.AbsFileUploadInterceptor;
import com.wabacus.util.Tools;

public class FileTagUpload extends AbsFileUpload
{
    public FileTagUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(PrintWriter out)
    {
        String savepath=urlDeocde(getRequestString("SAVEPATH",""));
        String uploadcount=getRequestString("UPLOADCOUNT","");
        String newfilename=urlDeocde(getRequestString("NEWFILENAME",""));
        String maxsize=getRequestString("MAXSIZE","");
        String allowtypes=urlDeocde(getRequestString("ALLOWTYPES",""));
        String rooturl=urlDeocde(getRequestString("ROOTURL",""));
        String interceptor=urlDeocde(getRequestString("INTERCEPTOR",""));
        out.print("<input type='hidden' name='SAVEPATH' value='"+savepath+"'/>");
        out.print("<input type='hidden' name='UPLOADCOUNT' value='"+uploadcount+"'/>");
        out.print("<input type='hidden' name='NEWFILENAME' value='"+newfilename+"'/>");
        out.print("<input type='hidden' name='MAXSIZE' value='"+maxsize+"'/>");
        out.print("<input type='hidden' name='ALLOWTYPES' value='"+allowtypes+"'/>");
        out.print("<input type='hidden' name='ROOTURL' value='"+rooturl+"'/>");
        out.print("<input type='hidden' name='INTERCEPTOR' value='"+interceptor+"'/>");
        boolean flag=true;
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            AbsFileUploadInterceptor interceptorObj=AbsFileUploadInterceptor.createInterceptorObj(interceptor.trim());
            Map<String,String> mFormFieldValues=(Map<String,String>)request.getAttribute("WX_FILE_UPLOAD_FIELDVALUES");
            flag=interceptorObj.beforeDisplayFileUploadInterface(request,mFormFieldValues,out);
        }
        if(flag)
        {
            int iuploadcount=Integer.parseInt(uploadcount.trim());
            if(iuploadcount<=0) iuploadcount=1;
            out.print("<table border=0 cellspacing=1 cellpadding=2  style=\"margin:0px\" width=\"98%\" ID=\"Table1\" align=\"center\">");
            out.print("<tr class=filetitle><td style='font-size:13px;'>文件上传</td></tr>");
            for(int i=0;i<iuploadcount;i++)
            {
                out.print("<tr><td style='font-size:13px;'><input type=\"file\" contentEditable=\"false\" name=\"uploadfile"+i+"\"></td></tr>");
            }
            if(allowtypes!=null&&!allowtypes.trim().equals(""))
            {
                out.print("<tr class=filetitle><td style='font-size:13px;'>["+stardardFileSuffixString(allowtypes)+"]</td></tr>");
            }
            out.print("<tr><td style='font-size:13px;'><input type=\"submit\" class=\"cls-button\" name=\"submit\" value=\"上传\">");
            out.print("</td></tr></table>");
        }
    }

    private String urlDeocde(String urlparam)
    {
        if(urlparam==null||urlparam.trim().equals("")) return urlparam;
        try
        {
            return URLDecoder.decode(urlparam,"utf-8");
        }catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return urlparam;
        }
    }

    public String doFileUpload(List lstFieldItems,Map<String,String> mFormFieldValues,PrintWriter out)
    {
        String savepath=mFormFieldValues.get("SAVEPATH");
        
        String newfilename=mFormFieldValues.get("NEWFILENAME");
        String maxsize=mFormFieldValues.get("MAXSIZE");
        String configAllowTypes=mFormFieldValues.get("ALLOWTYPES");
        String rooturl=mFormFieldValues.get("ROOTURL");
        String interceptor=mFormFieldValues.get("INTERCEPTOR");
        long imaxsize=-1L;
        if(maxsize!=null&&!maxsize.trim().equals(""))
        {
            imaxsize=Long.parseLong(maxsize.trim())*1024;
        }
        if(Tools.isDefineKey("classpath",savepath))
        {
            throw new WabacusConfigLoadingException("显示文件上传标签失败，不能将文件上传标签的savepath属性指定为classpath{}格式");
        }
        if(!Tools.isDefineKey("absolute",savepath)&&!Tools.isDefineKey("relative",savepath))
        {
            throw new WabacusConfigLoadingException("显示文件上传标签失败，必须将上传文件的保存路径配置为absolute{}或relative{}格式");
        }
        savepath=WabacusAssistant.getInstance().parseConfigPathToRealPath(savepath,Config.webroot_abspath);
        request.setAttribute("WX_FILE_UPLOAD_FIELDVALUES",mFormFieldValues);
        AbsFileUploadInterceptor interceptorObj=null;
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            interceptorObj=AbsFileUploadInterceptor.createInterceptorObj(interceptor.trim());
            request.setAttribute("WX_FILE_UPLOAD_INTERCEPTOR",interceptorObj);//保存拦截器存取来，以便在WabacusFacade中可以直接使用
        }
        List<String> lstConfigAllowTypes=getFileSuffixList(configAllowTypes);
        FileItem item;
        List<String> lstDestFileNames=new ArrayList<String>();
        for(Object itemObj:lstFieldItems)
        {
            item=(FileItem)itemObj;
            if(item.isFormField()) continue;
            String originalFilename=item.getName();
            if((originalFilename==null||originalFilename.equals(""))) continue;
            originalFilename=getFileNameFromAbsolutePath(originalFilename);
            if((originalFilename==null||originalFilename.equals(""))) continue;
            String destfilename=getSaveFileName(originalFilename,newfilename);
            mFormFieldValues.put(AbsFileUploadInterceptor.ALLOWTYPES_KEY,configAllowTypes);
            mFormFieldValues.put(AbsFileUploadInterceptor.MAXSIZE_KEY,String.valueOf(String.valueOf(imaxsize)));
            mFormFieldValues.put(AbsFileUploadInterceptor.FILENAME_KEY,destfilename);
            mFormFieldValues.put(AbsFileUploadInterceptor.SAVEPATH_KEY,savepath);
            boolean shouldUpload=true;
            if(interceptorObj!=null)
            {
                shouldUpload=interceptorObj.beforeFileUpload(request,item,mFormFieldValues,out);
            }
            if(!shouldUpload) continue;
            String errorMessage=doUploadFileAction(item,mFormFieldValues,originalFilename,configAllowTypes,lstConfigAllowTypes);
            if(errorMessage!=null&&!errorMessage.trim().equals("")) return errorMessage;
            destfilename=mFormFieldValues.get(AbsFileUploadInterceptor.FILENAME_KEY);
            if(!lstDestFileNames.contains(destfilename)) lstDestFileNames.add(destfilename);
        }
        if(rooturl!=null&&!rooturl.trim().equals(""))
        {
            rooturl=rooturl.trim();
            if(!rooturl.startsWith(Config.webroot)&&!rooturl.toLowerCase().startsWith("http://"))
            {
                rooturl=Config.webroot+"/"+rooturl;
                rooturl=Tools.replaceAll(rooturl,"//","/").trim();
            }
            if(!rooturl.endsWith("/")) rooturl=rooturl+"/";
            out.print("<table style=\"margin:0px;\">");
            out.print("<tr><td style='font-size:13px;'>上传文件URL：</td></tr>");
            for(String destfilenameTmp:lstDestFileNames)
            {
                if(destfilenameTmp==null||destfilenameTmp.trim().equals("")) continue;
                out.print("<tr><td style='font-size:13px;'>&nbsp;&nbsp;"+rooturl+destfilenameTmp+"</td></tr>");
            }
            out.print("</table>");
        }
        return null;
    }    
}
