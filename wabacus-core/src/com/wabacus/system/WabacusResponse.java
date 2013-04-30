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
package com.wabacus.system;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.exception.MessageCollector;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeWarningException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class WabacusResponse
{
    private static Log log=LogFactory.getLog(WabacusResponse.class);
    
    private PrintWriter out;

    private JspWriter jspout;
    
    private StringBuffer outBuf;

    private ReportRequest rrequest;
    
    private MessageCollector messageCollector;

    private int statecode=Consts.STATECODE_SUCCESS;
    
    private List<Map<String,String>> lstOnloadMethods;
    
    private List<String> lstUpdateReportGuids;
    
    private String dynamicRefreshComponentGuid;
    
    private String dynamicSlaveReportId;//如果当前是动态产生刷新组件的GUID，且此组件为一个从报表，则这里存放此从报表的ID
    
    private HttpServletResponse response;
    
    private boolean hasInitOutput;
    
    public WabacusResponse(HttpServletResponse response)
    {
        this.response=response;
        messageCollector=new MessageCollector(this);
    }

    public MessageCollector getMessageCollector()
    {
        return messageCollector;
    }

    public void addOnloadMethod(String methodname,String methodparams,boolean isInsertFirst)
    {
        if(methodname==null||methodname.trim().equals("")) return;
        if(lstOnloadMethods==null) lstOnloadMethods=new ArrayList<Map<String,String>>();
        Map<String,String> mMethod=new HashMap<String,String>();
        if(methodparams!=null&&!methodparams.trim().equals(""))
        {
            
            methodparams=methodparams.trim();
            if(!methodparams.startsWith("{")||!methodparams.endsWith("}"))
            {
                methodparams="{"+methodparams+"}";
            }
        }
        mMethod.put(methodname,methodparams);
        if(isInsertFirst)
        {
            lstOnloadMethods.add(0,mMethod);
        }else
        {//加在后面调用
            lstOnloadMethods.add(mMethod);
        }
    }

    public HttpServletResponse getResponse()
    {
        return response;
    }

    public void setResponse(HttpServletResponse response)
    {
        this.response=response;
    }

    public StringBuffer getOutBuf()
    {
        return outBuf;
    }

    public int getStatecode()
    {
        return statecode;
    }

    public void setStatecode(int statecode)
    {
        if(statecode!=Consts.STATECODE_NONREFRESHPAGE&&statecode!=Consts.STATECODE_FAILED&&statecode!=Consts.STATECODE_SUCCESS)
        {
            log.warn("设置的响应状态码"+statecode+"不支持，将用默认的成功状态码");
            statecode=Consts.STATECODE_SUCCESS;
        }
        this.statecode=statecode;
    }
    
    public void terminateResponse(int statecode)
    {
        setStatecode(statecode);
        throw new WabacusRuntimeWarningException();
    }
    
    public void addUpdateReportGuid(String reportguid)
    {
        if(reportguid==null||reportguid.trim().equals("")) return;
        if(this.lstUpdateReportGuids==null)
        {
            this.lstUpdateReportGuids=new ArrayList<String>();
        }else if(this.lstUpdateReportGuids.contains(reportguid))
        {
            return;
        }
        this.lstUpdateReportGuids.add(reportguid);
    }

    public ReportRequest getRRequest()
    {
        return rrequest;
    }

    public void setRRequest(ReportRequest rrequest)
    {
        this.rrequest=rrequest;
    }

    public void setJspout(JspWriter jspout)
    {
        this.jspout=jspout;
    }

    public String invokeOnloadMethodsFirstTime()
    {
        if(this.lstOnloadMethods==null||this.lstOnloadMethods.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        String methodName;
        String methodParams;
        for(Map<String,String> mMethodTmp:this.lstOnloadMethods)
        {
            methodName=mMethodTmp.keySet().iterator().next();
            methodParams=mMethodTmp.get(methodName);
            resultBuf.append(methodName).append("(");
            if(methodParams!=null&&!methodParams.trim().equals(""))
            {
               resultBuf.append(methodParams);
            }
            resultBuf.append(");");
        }
        return resultBuf.toString();
    }
    
    public String assembleResultsInfo(Throwable t)
    {
        String load_error_mess=Config.getInstance().getResources().getString(rrequest,rrequest.getPagebean(),Consts.LOADERROR_MESS_KEY,false);
        if(load_error_mess==null||load_error_mess.trim().equals(""))
        {
            load_error_mess="<strong>System is busy,Please try later</strong>";
        }else
        {
            load_error_mess=load_error_mess.trim();
            load_error_mess=rrequest.getI18NStringValue(load_error_mess);
        }
        StringBuffer resultBuf=new StringBuffer();
        if(!rrequest.isLoadedByAjax()||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&rrequest.getShowtype()!=Consts.DISPLAY_ON_PRINT))
        {
            if(t!=null)
            {
                if(t instanceof WabacusRuntimeWarningException)
                {
                   resultBuf.append(addWarnMessage(false,"<br>"));
                   resultBuf.append(addSuccessMessage(false,"<br>"));
                   resultBuf.append(addAlertMessage(false,null));
                }else
                {//当前抛出了异常信息
                    resultBuf.append(addErrorMessage(load_error_mess,false,null));
                }
            }else
            {
                resultBuf.append(addWarnMessage(false,"<br>"));
                resultBuf.append(addAlertMessage(false,"<br>"));
                resultBuf.append(addSuccessMessage(false,"<br>"));
                resultBuf.append(addErrorMessage(null,false,null));
            }
        }else
        {
            String pageid=rrequest.getPagebean().getId();
            resultBuf.append("<RESULTS_INFO-").append(pageid).append(">").append("{");
            resultBuf.append("pageurl:\"").append(rrequest.getUrl()).append("\",");
            
            if(rrequest.getPagebean().isShouldProvideEncodePageUrl())
            {
                resultBuf.append("pageEncodeUrl:\"").append(Tools.convertBetweenStringAndAscii(rrequest.getUrl(),true)).append("\",");
            }
            if(dynamicRefreshComponentGuid!=null&&!dynamicRefreshComponentGuid.trim().equals(""))
            {
                resultBuf.append("dynamicRefreshComponentGuid:\"").append(dynamicRefreshComponentGuid).append("\",");
                if(dynamicSlaveReportId!=null&&!dynamicSlaveReportId.trim().equals(""))
                {//动态刷新的组件为一个从报表
                    resultBuf.append("dynamicSlaveReportId:\"").append(dynamicSlaveReportId).append("\",");
                }
            }
            resultBuf.append("statecode:").append(this.statecode).append(",");
            if(t!=null)
            {
                if(t instanceof WabacusRuntimeWarningException)
                {
                    resultBuf.append(addWarnMessage(true,","));
                    resultBuf.append(addSuccessMessage(true,","));
                    resultBuf.append(addAlertMessage(true,","));
                }else
                {
                    resultBuf.append(addErrorMessage(load_error_mess,true,","));
                }
            }else
            {
                resultBuf.append(addWarnMessage(true,","));
                resultBuf.append(addAlertMessage(true,","));
                resultBuf.append(addSuccessMessage(true,","));
                resultBuf.append(addErrorMessage(null,true,","));
            }
            if(this.lstOnloadMethods!=null&&this.lstOnloadMethods.size()>0)
            {
                resultBuf.append("onloadMethods:[");
                String methodNameTmp;
                String methodParamsTmp;
                for(Map<String,String> mMethodsTmp:this.lstOnloadMethods)
                {//循环每个onload方法
                    methodNameTmp=mMethodsTmp.keySet().iterator().next();
                    methodParamsTmp=mMethodsTmp.get(methodNameTmp);
                    resultBuf.append("{methodname:").append(methodNameTmp);
                    if(methodParamsTmp!=null&&!methodParamsTmp.trim().equals(""))
                    {
                        resultBuf.append(",methodparams:").append(methodParamsTmp);
                    }
                    resultBuf.append("},");
                }
                if(resultBuf.charAt(resultBuf.length()-1)==',')
                {
                    resultBuf.deleteCharAt(resultBuf.length()-1);
                }
                resultBuf.append("],");
            }
            if(lstUpdateReportGuids!=null&&lstUpdateReportGuids.size()>0)
            {
                resultBuf.append("updateReportGuids:[");
                for(String rguidTmp:lstUpdateReportGuids)
                {
                    resultBuf.append("{value:\"").append(rguidTmp).append("\"},");
                }
                if(resultBuf.charAt(resultBuf.length()-1)==',')
                {
                    resultBuf.deleteCharAt(resultBuf.length()-1);
                }
                resultBuf.append("],");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',')
            {
                resultBuf.deleteCharAt(resultBuf.length()-1);
            }
            resultBuf.append("}").append("</RESULTS_INFO-").append(pageid).append(">");
        }
        return resultBuf.toString();
    }
    
    private String addAlertMessage(boolean jsPrompt,String suffixIfExist)
    {
        String resultMess="";
        if(jsPrompt)
        {
            resultMess=this.messageCollector.getJsAlertMessages("  ");
            if(resultMess!=null&&!resultMess.trim().equals(""))
            {
                resultMess="alertmess:\""+resultMess.trim()+"\"";
            }
        }else
        {//直接提示到页面
            resultMess=this.messageCollector.getJsAlertMessages("<br>");
        }
        resultMess=resultMess==null?"":resultMess.trim();
        if(!resultMess.equals("")&&suffixIfExist!=null) resultMess=resultMess+suffixIfExist;
        return resultMess;
    }
    
    private String addSuccessMessage(boolean jsPrompt,String suffixIfExist)
    {
        String resultMess="";
        if(jsPrompt)
        {
            resultMess=this.messageCollector.getJsSuccessMessages("  ");
            if(resultMess!=null&&!resultMess.trim().equals(""))
            {
                resultMess="successmess:\""+resultMess.trim()+"\"";
            }
        }else
        {
            resultMess=this.messageCollector.getJsSuccessMessages("<br>");
        }
        resultMess=resultMess==null?"":resultMess.trim();
        if(!resultMess.equals("")&&suffixIfExist!=null) resultMess=resultMess+suffixIfExist;
        return resultMess;
    }
    
    private String addWarnMessage(boolean jsPrompt,String suffixIfExist)
    {
        String resultMess="";
        if(jsPrompt)
        {
            resultMess=this.messageCollector.getJsWarnMessages("  ");
            if(resultMess!=null&&!resultMess.trim().equals(""))
            {
                resultMess="warnmess:\""+resultMess.trim()+"\"";
            }
        }else
        {
            resultMess=this.messageCollector.getJsWarnMessages("<br>");
        }
        resultMess=resultMess==null?"":resultMess.trim();
        if(!resultMess.equals("")&&suffixIfExist!=null) resultMess=resultMess+suffixIfExist;
        return resultMess;
    }
    
    private String addErrorMessage(String defaultvalue,boolean jsPrompt,String suffixIfExist)
    {
        String resultMess="";
        if(jsPrompt)
        {//通过js提示
            resultMess=this.messageCollector.getJsErrorMessages("  ");
            if(resultMess==null||resultMess.trim().equals("")) resultMess=defaultvalue;
            if(resultMess!=null&&!resultMess.trim().equals(""))
            {
                resultMess="errormess:\""+resultMess.trim()+"\"";
            }
        }else
        {
            resultMess=this.messageCollector.getJsErrorMessages("<br>");
        }
        resultMess=resultMess==null?"":resultMess.trim();
        if(!resultMess.equals("")&&suffixIfExist!=null) resultMess=resultMess+suffixIfExist;
        return resultMess;
    }

    public void setDynamicRefreshComponentGuid(String componentguid,String slaveReportId)
    {
        this.dynamicRefreshComponentGuid=componentguid;
        this.dynamicSlaveReportId=slaveReportId;
    }
    
    public boolean isOutputImmediately()
    {
        if(this.response==null) return false;
        return true;
    }
    
    public void initOutput(String attachFilename)
    {
        hasInitOutput=true;
        if(response!=null&&rrequest.getRequest()!=null)
        {
            rrequest.getRequest().getSession();
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL)
            {
                attachFilename=attachFilename==null?"":attachFilename.trim();
                response.setHeader("Content-disposition","attachment;filename="
                        +WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),attachFilename)+".xls");
            }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_WORD)
            {
                attachFilename=attachFilename==null?"":attachFilename.trim();
                response.setHeader("Content-disposition","attachment;filename="
                        +WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),attachFilename)+".doc");
            }
            try
            {
                out=response.getWriter();
            }catch(IOException e)
            {
                throw new WabacusRuntimeException("初始化页面输出失败",e);
            }
            if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PRINT)
            {
                out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
                out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
            }
        }else
        {
            this.outBuf=new StringBuffer();
            outBuf.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
            outBuf.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
        }
    }
    
    public void println(String content)
    {
        println(content,false);
    }
    
    public void println(String content,boolean overwrite)
    {
        if(!hasInitOutput) initOutput(null);//还没有初始化输出，则先初始化
        if(content==null||content.trim().equals("")) return;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL||rrequest.getShowtype()==Consts.DISPLAY_ON_WORD
                ||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            content=WabacusAssistant.getInstance().replaceAllImgPathInExportDataFile(rrequest.getRequest(),content);
        }
        if(jspout!=null)
        {//当前是通过<wx:report/>显示报表，则从jsp中取out进行输出
            try
            {
                jspout.println(content);
            }catch(IOException e)
            {
                
                e.printStackTrace();
            }
        }else if(out!=null)
        {
            out.println(content);
        }else
        {
            if(overwrite) outBuf=new StringBuffer();
            outBuf.append(content);
        }
        /*if(mReportsWithIncludePage!=null&&mReportsWithIncludePage.size()>0)
        {
            rrequest.getRequest().setAttribute("WX_REPORTREQUEST",rrequest);
            String starttagTmp=ReportAssistant.getInstance().getStartTagOfIncludeFilePlaceholder(rrequest.getPagebean());
            String endtagTmp=ReportAssistant.getInstance().getEndTagOfIncludeFilePlaceholder(rrequest.getPagebean());//取到占位符结束标签
            int idx=content.indexOf(starttagTmp);
            String reportidTmp;
            AbsReportType reportTypeObjTmp;
            *
             * 每个包含外部页面的格式为<tag>reportid</tag>，其中<tag>和</tag>分别是starttagTmp和endtagTmp
             *
            String dynTplPathTmp=null;
            while(true)
            {
                if(idx<0) break;
                out.println(content.substring(0,idx));
                content=content.substring(idx+starttagTmp.length());
                idx=content.indexOf(endtagTmp);
                if(idx<0) break;
                reportidTmp=content.substring(0,idx);
                if(reportidTmp==null||reportidTmp.trim().equals("")||!this.mReportsWithIncludePage.containsKey(reportidTmp))
                {
                    idx=content.indexOf(starttagTmp);
                    continue;
                }
                reportTypeObjTmp=mReportsWithIncludePage.get(reportidTmp);
                rrequest.getRequest().setAttribute("WX_COMPONENT_OBJ",reportTypeObjTmp);
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
                {
                    dynTplPathTmp=reportTypeObjTmp.getReportBean().getDynTplPath();
                }else
                {
                    dynTplPathTmp=reportTypeObjTmp.getReportBean().getDynDataExportTplPath();
                }
                RequestDispatcher rd=rrequest.getRequest().getRequestDispatcher(dynTplPathTmp);
                rd.include(rrequest.getRequest(),response);

                content=content.substring(idx+endtagTmp.length());
                idx=content.indexOf(starttagTmp);
            }
        }*/
    }
    
    public void print(String content)
    {
        print(content,false);
    }
    
    public void print(String content,boolean overwrite)
    {
        if(!hasInitOutput) initOutput(null);
        if(content==null||content.trim().equals("")) return;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL||rrequest.getShowtype()==Consts.DISPLAY_ON_WORD
                ||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {//数据导出
            content=WabacusAssistant.getInstance().replaceAllImgPathInExportDataFile(rrequest.getRequest(),content);
        }
        if(jspout!=null)
        {//当前是通过<wx:report/>显示报表，则从jsp中取out进行输出
            try
            {
                jspout.print(content);
            }catch(IOException e)
            {
                
                e.printStackTrace();
            }
        }else if(out!=null)
        {
            out.print(content);
        }else
        {
            if(overwrite) outBuf=new StringBuffer();
            outBuf.append(content);
        }
    }
    
    public void sendRedirect(String url)
    {
        if(url==null||url.trim().equals("")) return;
        if(!rrequest.isLoadedByAjax())
        {
            try
            {
                this.response.sendRedirect(url);
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }else
        {
            this.addOnloadMethod("wx_sendRedirect","{url:\""+url+"\"}",false);
        }
        throw new WabacusRuntimeWarningException();
    }
}
