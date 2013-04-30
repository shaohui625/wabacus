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
package com.wabacus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.dataexport.WordRichExcelExportBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeWarningException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.WabacusResponse;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.PdfAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.EditableDetailReportType2;
import com.wabacus.system.component.application.report.EditableListFormReportType;
import com.wabacus.system.component.application.report.EditableListReportType2;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.fileupload.AbsFileUpload;
import com.wabacus.system.fileupload.DataImportReportUpload;
import com.wabacus.system.fileupload.DataImportTagUpload;
import com.wabacus.system.fileupload.FileInputBoxUpload;
import com.wabacus.system.fileupload.FileTagUpload;
import com.wabacus.system.inputbox.AutoCompleteConfigBean;
import com.wabacus.system.inputbox.SelectBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.intercept.AbsFileUploadInterceptor;
import com.wabacus.system.print.AbsPrintProvider;
import com.wabacus.system.resultset.GetAllResultSetByPreparedSQL;
import com.wabacus.system.resultset.GetAllResultSetBySQL;
import com.wabacus.system.resultset.GetResultSetByStoreProcedure;
import com.wabacus.system.resultset.ISQLType;
import com.wabacus.system.serveraction.IServerAction;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class WabacusFacade
{
    private static Log log=LogFactory.getLog(WabacusFacade.class);

    public static void displayReport(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PAGE);
        WabacusResponse wresponse=new WabacusResponse(response);
        displayReport(rrequest,wresponse,rrequest.getStringAttribute("PAGEID",""));
    }

    public static String displayReport(String pageid,Map<String,String> mParams,Locale locale)
    {
        ReportRequest rrequest=new ReportRequest(pageid,Consts.DISPLAY_ON_PAGE,locale);
        if(mParams!=null)
        {
            for(Entry<String,String> entryTmp:mParams.entrySet())
            {
                rrequest.setAttribute(entryTmp.getKey(),entryTmp.getValue());
            }
        }
        WabacusResponse wresponse=new WabacusResponse(null);
        displayReport(rrequest,wresponse,pageid);
        StringBuffer resultBuf=wresponse.getOutBuf();
        if(resultBuf==null) return "";
        return resultBuf.toString();
    }

    private static void displayReport(ReportRequest rrequest,WabacusResponse wresponse,String pageid)
    {
        boolean success=true;
        String errorinfo=null;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            if(rrequest.getSlaveReportTypeObj()!=null)
            {
                rrequest.getSlaveReportTypeObj().displayOnPage(null);
            }else
            {
                rrequest.getRefreshComponentTypeObj().displayOnPage(null);
            }
            log.debug(rrequest.getCurrentStatus());
        }catch(WabacusRuntimeWarningException wrwe)
        {
            String logwarnmess=wresponse.getMessageCollector().getLogWarnsMessages();
            if(logwarnmess!=null&&!logwarnmess.trim().equals(""))
            {
                log.warn("显示页面"+pageid+"时，"+logwarnmess);
            }
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
                errorinfo=wresponse.assembleResultsInfo(wrwe);
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            String errormess=wresponse.getMessageCollector().getLogErrorsMessages();
            if(errormess!=null&&!errormess.trim().equals(""))
            {
                log.error("显示页面"+pageid+"失败，"+errormess,wre);
            }else
            {
                log.error("显示页面"+pageid+"失败",wre);
            }
            success=false;
            errorinfo=wresponse.assembleResultsInfo(wre);
        }finally
        {
            rrequest.destroy(success);
        }
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {
            wresponse.println(errorinfo,true);
        }else
        {
            wresponse.println(rrequest.getWResponse().assembleResultsInfo(null));
        }
    }

    public static void exportReportDataOnWordRichexcel(HttpServletRequest request,HttpServletResponse response,int exporttype)
    {
        ReportRequest rrequest=new ReportRequest(request,exporttype);
        WabacusResponse wresponse=new WabacusResponse(response);
        exportReportDataOnWordRichexcel(rrequest.getStringAttribute("PAGEID",""),rrequest,wresponse,exporttype);
    }

    private static void exportReportDataOnWordRichexcel(String pageid,ReportRequest rrequest,WabacusResponse wresponse,int exporttype)
    {
        boolean success=true;
        String errorinfo=null;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            wresponse.initOutput(getDataExportFilename(rrequest));//这里初始化一下输出，以便传入下载的文件名
            IComponentType ccTypeObjTmp;
            Object dataExportTplObjTmp;
            WordRichExcelExportBean debeanTmp;
            for(IComponentConfigBean ccbeanTmp:rrequest.getLstComponentBeans())
            {
                ccTypeObjTmp=rrequest.getComponentTypeObj(ccbeanTmp,null,true);
                dataExportTplObjTmp=null;
                if(ccbeanTmp.getDataExportsBean()!=null)
                {//如果此组件配置了<dataexports/>
                    debeanTmp=(WordRichExcelExportBean)ccbeanTmp.getDataExportsBean().getDataExportBean(exporttype);//有在<dataexports/>中配置了type为此导出类型的<dataexport/>
                    if(debeanTmp!=null) dataExportTplObjTmp=debeanTmp.getDataExportTplObj();
                }
                ccTypeObjTmp.displayOnExportDataFile(dataExportTplObjTmp,true);
            }
        }catch(WabacusRuntimeWarningException wrwe)
        {
            String logwarnmess=rrequest.getWResponse().getMessageCollector().getLogWarnsMessages();
            if(logwarnmess!=null&&!logwarnmess.trim().equals(""))
            {
                log.warn("导出页面"+rrequest.getPagebean().getId()+"下的报表时，"+logwarnmess);
            }
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
                errorinfo=wresponse.assembleResultsInfo(wrwe);
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            String errormess=rrequest.getWResponse().getMessageCollector().getLogErrorsMessages();
            if(errormess!=null&&!errormess.trim().equals(""))
            {
                log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败，"+errormess,wre);
            }else
            {
                log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败，",wre);
            }
            success=false;
            errorinfo=rrequest.getWResponse().assembleResultsInfo(wre);
        }finally
        {
            rrequest.destroy(success);
        }
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {
            try
            {
                wresponse.println(errorinfo,true);
            }catch(Exception e)
            {
                log.error("导出页面"+pageid+"下的应用"+rrequest.getStringAttribute("INCLUDE_APPLICATIONIDS","")+"数据失败",e);
            }
        }
    }

    public static void exportReportDataOnPlainExcel(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PLAINEXCEL);
        WabacusResponse wresponse=new WabacusResponse(response);
        exportReportDataOnPlainExcel(rrequest.getStringAttribute("PAGEID",""),rrequest,wresponse);
    }

    public static void exportReportDataOnPlainExcel(String pageid,Locale locale)
    {
        ReportRequest rrequest=new ReportRequest(pageid,Consts.DISPLAY_ON_PLAINEXCEL,locale);
        WabacusResponse wresponse=new WabacusResponse(null);
        exportReportDataOnPlainExcel(pageid,rrequest,wresponse);
    }

    private static void exportReportDataOnPlainExcel(String pageid,ReportRequest rrequest,WabacusResponse wresponse)
    {
        boolean success=true;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            if(rrequest.getLstAllReportBeans()==null||rrequest.getLstAllReportBeans().size()==0)
            {
                throw new WabacusRuntimeException("导出页面"+pageid+"上的数据失败，plainexcel导出方式只能导出报表，不能导出其它应用");
            }
            Workbook workbook=new HSSFWorkbook();
            AbsReportType reportTypeObjTmp;
            for(ReportBean rbTmp:rrequest.getLstAllReportBeans())
            {
                reportTypeObjTmp=(AbsReportType)rrequest.getComponentTypeObj(rbTmp,null,false);
                reportTypeObjTmp.displayOnPlainExcel(workbook);
            }
            String title=WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),getDataExportFilename(rrequest));
            wresponse.getResponse().setHeader("Content-disposition","attachment;filename="+title+".xls");
            BufferedOutputStream bos=new BufferedOutputStream(wresponse.getResponse().getOutputStream());
            workbook.write(bos);
            bos.close();
        }catch(WabacusRuntimeWarningException wrwe)
        {
            String logwarnmess=rrequest.getWResponse().getMessageCollector().getLogWarnsMessages();
            if(logwarnmess!=null&&!logwarnmess.trim().equals(""))
            {
                log.warn("导出页面"+rrequest.getPagebean().getId()+"下的报表时，"+logwarnmess);
            }
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            String errormess=rrequest.getWResponse().getMessageCollector().getLogErrorsMessages();
            if(errormess!=null&&!errormess.trim().equals(""))
            {//有出错信息
                log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败，"+errormess,wre);
            }else
            {
                log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败",wre);
            }
            success=false;
        }finally
        {
            rrequest.destroy(success);
        }
    }

    public static void exportReportDataOnPDF(HttpServletRequest request,HttpServletResponse response,int showtype)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PDF);
        WabacusResponse wresponse=new WabacusResponse(response);
        exportReportDataOnPDF(rrequest.getStringAttribute("PAGEID",""),rrequest,wresponse);
    }

    private static void exportReportDataOnPDF(String pageid,ReportRequest rrequest,WabacusResponse wresponse)
    {
        boolean success=true;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            if(rrequest.getLstAllReportBeans()==null||rrequest.getLstAllReportBeans().size()==0)
            {
                throw new WabacusRuntimeException("导出页面"+pageid+"上的数据失败，plainexcel导出方式只能导出报表，不能导出其它应用");
            }
            Document document=new Document();
            ByteArrayOutputStream baosResult=new ByteArrayOutputStream();
            PdfCopy pdfCopy=new PdfCopy(document,baosResult);
            document.open();
            boolean ispdfprint=rrequest.isPdfPrintAction();
            for(IComponentConfigBean ccbeanTmp:rrequest.getLstComponentBeans())
            {
                PDFExportBean pdfbeanTmp=null;
                if(ispdfprint)
                {
                    pdfbeanTmp=ccbeanTmp.getPdfPrintBean();
                }else if(ccbeanTmp.getDataExportsBean()!=null)
                {
                    pdfbeanTmp=(PDFExportBean)ccbeanTmp.getDataExportsBean().getDataExportBean(Consts.DATAEXPORT_PDF);
                }
                if(pdfbeanTmp!=null&&pdfbeanTmp.getPdftemplate()!=null&&!pdfbeanTmp.getPdftemplate().trim().equals(""))
                {//如果此组件配置了pdf模板
                    PdfAssistant.getInstance()
                            .addPdfPageToDocument(pdfCopy,PdfAssistant.getInstance().showReportDataOnPdfWithTpl(rrequest,ccbeanTmp));
                }
            }
            AbsReportType reportTypeObjTmp;
            for(ReportBean rbTmp:rrequest.getLstAllReportBeans())
            {
                reportTypeObjTmp=(AbsReportType)rrequest.getComponentTypeObj(rbTmp,null,false);
                if(rrequest.isReportInPdfTemplate(rbTmp.getId())) continue;
                PdfAssistant.getInstance().addPdfPageToDocument(pdfCopy,reportTypeObjTmp.displayOnPdf());
            }
            document.close();
            if(!ispdfprint)
            {
                String title=WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),getDataExportFilename(rrequest));
                wresponse.getResponse().setHeader("Content-disposition","attachment;filename="+title+".pdf");
            }
            wresponse.getResponse().setContentLength(baosResult.size());
            ServletOutputStream out=wresponse.getResponse().getOutputStream();
            baosResult.writeTo(out);
            out.flush();
            out.close();
            baosResult.close();
        }catch(WabacusRuntimeWarningException wrwe)
        {
            String logwarnmess=rrequest.getWResponse().getMessageCollector().getLogWarnsMessages();
            if(logwarnmess!=null&&!logwarnmess.trim().equals(""))
            {
                log.warn("导出页面"+rrequest.getPagebean().getId()+"下的报表时，"+logwarnmess);
            }
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            String errormess=rrequest.getWResponse().getMessageCollector().getLogErrorsMessages();
            if(errormess!=null&&!errormess.trim().equals(""))
            {//有出错信息
                log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败，"+errormess,wre);
            }else
            {
                log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败",wre);
            }
            success=false;
        }finally
        {
            rrequest.destroy(success);
        }
    }

    private static String getDataExportFilename(ReportRequest rrequest)
    {
        StringBuffer filenameBuf=new StringBuffer();
        if(rrequest.getLstComponentBeans()==null||rrequest.getLstComponentBeans().size()==0) return "NoData";
        String filenameTmp;
        for(IComponentConfigBean ccbeanTmp:rrequest.getLstComponentBeans())
        {
            filenameTmp=null;
            if(ccbeanTmp.getDataExportsBean()!=null)
            {
                filenameTmp=ccbeanTmp.getDataExportsBean().getFilename(rrequest);
            }
            if(filenameTmp==null||filenameTmp.trim().equals(""))
            {//如果此组件没有在<dataexports/>中配置filename，则用标题做为文件名
                filenameTmp=ccbeanTmp.getTitle(rrequest);
            }
            if(filenameTmp==null||filenameTmp.trim().equals("")) continue;
            filenameBuf.append(filenameTmp).append(",");
        }
        if(filenameBuf.length()==0) return "DataExport";
        if(filenameBuf.charAt(filenameBuf.length()-1)==',') filenameBuf.deleteCharAt(filenameBuf.length()-1);
        return filenameBuf.toString();
    }

    public static void printComponents(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PRINT);
        WabacusResponse wresponse=new WabacusResponse(response);
        rrequest.setWResponse(wresponse);
        wresponse.setRRequest(rrequest);
        String pageid=rrequest.getStringAttribute("PAGEID","");
        String printComid=rrequest.getStringAttribute("COMPONENTIDS","");
        boolean success=true;
        String errorinfo=null;
        try
        {
            rrequest.init(pageid);
            if(printComid.equals(""))
            {
                throw new WabacusRuntimeException("没有传入打印的组件ID");
            }
            if(rrequest.getLstComponentBeans()==null||rrequest.getLstComponentBeans().size()==0)
            {
                throw new WabacusRuntimeException("页面"+pageid+"不存在ID为"+printComid+"的组件");
            }
            if(rrequest.getLstComponentBeans().size()>1)
            {
                throw new WabacusRuntimeException("打印页面"+pageid+"上的组件"+printComid+"失败，一次只能打印一个组件");
            }
            AbsPrintProviderConfigBean printBean=rrequest.getLstComponentBeans().get(0).getPrintBean();
            if(printBean==null)
            {
                throw new WabacusRuntimeException("页面"+pageid+"ID为"+printComid+"的组件没有配置<print/>");
            }
            AbsPrintProvider printProvider=printBean.createPrintProvider(rrequest);
            printProvider.doPrint();
            wresponse.addOnloadMethod(rrequest.getLstComponentBeans().get(0).getPrintBean().getPrintJsMethodName(),"",true);
            wresponse.println(rrequest.getWResponse().assembleResultsInfo(null));
        }catch(WabacusRuntimeWarningException wrwe)
        {
            String logwarnmess=rrequest.getWResponse().getMessageCollector().getLogWarnsMessages();
            if(logwarnmess!=null&&!logwarnmess.trim().equals(""))
            {
                log.warn("打印页面"+pageid+"下的应用时，"+logwarnmess);
            }
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
                errorinfo=wresponse.assembleResultsInfo(wrwe);
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            String errormess=rrequest.getWResponse().getMessageCollector().getLogErrorsMessages();
            if(errormess!=null&&!errormess.trim().equals(""))
            {
                log.error("打印页面"+pageid+"下的应用失败，"+errormess,wre);
            }else
            {
                log.error("打印页面"+pageid+"下的应用失败，",wre);
            }
            success=false;
            errorinfo=rrequest.getWResponse().assembleResultsInfo(wre);
        }finally
        {
            rrequest.destroy(success);
        }
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {
            try
            {
                wresponse.println(errorinfo,true);
            }catch(Exception e)
            {
                log.error("打印页面"+pageid+"下的组件"+printComid+"数据失败",e);
            }
        }
    }

    public static String getFilterDataList(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        StringBuffer resultBuf=new StringBuffer();
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            wresponse.setRRequest(rrequest);
            rrequest.setWResponse(wresponse);
            rrequest.initGetFilterDataList(rrequest.getStringAttribute("PAGEID",""),rrequest.getStringAttribute("REPORTID",""));
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            
            
            String colproperty=rrequest.getStringAttribute("FILTER_COLPROP","");
            ColBean cbean=rbean.getDbean().getColBeanByColProperty(colproperty);
            if(cbean==null)
            {
                throw new WabacusRuntimeException("取过滤数据时，根据"+colproperty+"没有取到指定的<col/>配置信息");
            }
            /*columnsBuf.append(cbean.getColumn());

            List<ColBean> lstParamCols=null;
            String paramColProperties=rrequest.getStringAttribute("FILTER_PARAMCOLPROPS","");
            if(!paramColProperties.trim().equals(""))
            {
                lstParamCols=new ArrayList<ColBean>();
                List<String> lstColpropertys=Tools.parseStringToList(paramColProperties,",");
                ColBean cbTemp;
                for(int i=0;i<lstColpropertys.size();i++)
                {
                    cbTemp=rbean.getDbean().getColBeanByColProperty(lstColpropertys.get(i));
                    if(cbTemp==null)
                    {
                        throw new WabacusRuntimeException("取过滤数据时，根据"+lstColpropertys.get(i)
                                +"没有取到指定的<col/>配置信息");
                    }
                    columnsBuf.append(",").append(cbTemp.getColumn());
                    lstParamCols.add(cbTemp);
                }
            }*/
            rrequest.setAttribute("FILTER_COLUMNNAME",cbean.getColumn());
            SqlBean sbean=rbean.getSbean();
            ISQLType sqlTypeObj=null;
            if(sbean.isStoreProcedure())
            {//如果是Store Procedure
                sqlTypeObj=new GetResultSetByStoreProcedure();
            }else if(sbean.getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
            {
                sqlTypeObj=new GetAllResultSetByPreparedSQL();
            }else
            {
                sqlTypeObj=new GetAllResultSetBySQL();
            }
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            AbsListReportFilterBean alfbean=alrcbean.getFilterBean();
            if(rbean.getInterceptor()!=null)
            {
                rrequest.setAttribute(rbean.getId()+"_WABACUS_FILTERBEAN",alfbean);
            }
            int maxOptionsCount=Config.getInstance().getSystemConfigValue("colfilter-maxnum-options",-1);
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            List<String> lstSelectedData=new ArrayList<String>();
            if(!alfbean.isConditionRelate())
            {
                rrequest.setAttribute(rbean.getId()+"_WABACUS_GETDATA","selected");//当前是取当前列被选中的选项数据（即当前列目前显示的数据）
                Object objTmp=sqlTypeObj.getResultSet(rrequest,(AbsReportType)rrequest.getComponentTypeObj(rbean,null,true));
                if(objTmp==null||rrequest.getWResponse().getMessageCollector().hasErrors()
                        ||rrequest.getWResponse().getMessageCollector().hasWarnings())
                {
                    return "<item><value>[error]</value><label><![CDATA[获取列过滤选项数据失败]]></label></item>";
                }
                if(!(objTmp instanceof ResultSet))
                {
                    throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"数据失败，在加载过滤选项数据的前置动作中，只能返回SQL语句或ResultSet，不能返回加载好的List对象");
                }
                ResultSet rs=(ResultSet)objTmp;
                Object valObj;
                String strvalue;
                int optioncnt=0;
                while(rs.next())
                {
                    valObj=cbean.getDatatypeObj().getColumnValue(rs,cbean.getColumn(),dbtype);
                    strvalue=cbean.getDatatypeObj().value2label(valObj);
                    if(strvalue==null||strvalue.trim().equals("")) continue;
                    if(!lstSelectedData.contains(strvalue)) lstSelectedData.add(strvalue);
                    if(maxOptionsCount>0&&++optioncnt==maxOptionsCount)
                    {
                        break;
                    }
                }
                rs.close();
            }else
            {
                String filterval=rrequest.getStringAttribute(alfbean.getConditionname(),"");
                if(!filterval.equals(""))
                {
                    resultBuf.append("<item><value><![CDATA[(ALL_DATA)]]></value><label>(全部)</label></item>");
                }
            }
            log.debug(lstSelectedData);
            rrequest.setAttribute(rbean.getId()+"_WABACUS_GETDATA","all");
            Object objTmp=sqlTypeObj.getResultSet(rrequest,(AbsReportType)rrequest.getComponentTypeObj(rbean,null,true));
            if(objTmp==null||rrequest.getWResponse().getMessageCollector().hasErrors()||rrequest.getWResponse().getMessageCollector().hasWarnings())
            {
                return "<item><value>[error]</value><label><![CDATA[获取列过滤选项数据失败]]></label></item>";
            }
            if(!(objTmp instanceof ResultSet))
            {
                throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"数据失败，在加载过滤选项数据的前置动作中，只能返回SQL语句或ResultSet，不能返回加载好的List对象");
            }
            ResultSet rs=(ResultSet)objTmp;
            Object valObj;
            String strvalue;
            List<String> lstValues=new ArrayList<String>();
            int optioncnt=0;
            while(rs.next())
            {
                valObj=cbean.getDatatypeObj().getColumnValue(rs,cbean.getColumn(),dbtype);
                strvalue=cbean.getDatatypeObj().value2label(valObj);
                if(strvalue==null||strvalue.trim().equals("")) continue;
                if(!lstValues.contains(strvalue)) lstValues.add(strvalue);
                if(maxOptionsCount>0&&++optioncnt==maxOptionsCount)
                {//如果指定了最大记录数，且当前记录数达到这个值，则不再取后面的选项数据
                    break;
                }
            }
            rs.close();
            if(rbean.getInterceptor()!=null)
            {
                lstValues=(List)rbean.getInterceptor().afterLoadData(rrequest,rbean,alfbean,lstValues);
            }
            if(lstValues.size()==0)
            {
                resultBuf.append("<item><value>[nodata]</value><label>无选项数据</label></item>");
                return resultBuf.toString();
            }
            String[] strValueArr=lstValues.toArray(new String[lstValues.size()]);
            String[] strLabelArr=null;
            if(alfbean.getFormatMethod()!=null&&alfbean.getFormatClass()!=null)
            {
                strLabelArr=(String[])alfbean.getFormatMethod().invoke(alfbean.getFormatClass(),new Object[] { rbean, strValueArr });
                if(lstSelectedData!=null&&lstSelectedData.size()>0)
                {
                    String[] selectedDataArr=lstSelectedData.toArray(new String[lstSelectedData.size()]);
                    alfbean.getFormatMethod().invoke(alfbean.getFormatClass(),new Object[] { rbean, selectedDataArr });
                    lstSelectedData.clear();
                    for(int i=0;i<selectedDataArr.length;i++)
                    {
                        lstSelectedData.add(selectedDataArr[i]);
                    }
                }
            }
            if(strLabelArr==null||strLabelArr.length!=strValueArr.length)
            {
                strLabelArr=null;
            }
            for(int i=0;i<strValueArr.length;i++)
            {
                resultBuf.append("<item>");
                resultBuf.append("<value");
                if(lstSelectedData.contains(strValueArr[i]))
                {
                    resultBuf.append(" isChecked=\"true\"");
                }
                resultBuf.append("><![CDATA["+strValueArr[i]+"]]></value>");
                if(strLabelArr!=null)
                {
                    resultBuf.append("<label><![CDATA["+strLabelArr[i]+"]]></label>");
                }
                resultBuf.append("</item>");

            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载过滤数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        //        log.debug("获取的过滤数据列表："+resultBuf.toString());
        return resultBuf.toString();
    }

    public static String getTypePromptDataList(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        StringBuffer sbuffer=new StringBuffer();
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initGetTypePromptDataList();
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            String inputboxid=rrequest.getStringAttribute("INPUTBOXID","");
            if(inputboxid.equals(""))
            {
                throw new WabacusRuntimeException("没有取到输入框ID，无法获取输入提示数据");
            }
            int idx=inputboxid.lastIndexOf("__");
            if(idx>0)
            {//自动列表/列表表单的输入框
                inputboxid=inputboxid.substring(0,idx);
            }
            TextBox boxObj=rbean.getTextBoxWithingTypePrompt(inputboxid);
            if(boxObj==null)
            {
                throw new WabacusRuntimeException("没有取到相应输入框对象，无法获取提示数据");
            }
            TypePromptBean promptBean=boxObj.getTypePromptBean();
            if(promptBean==null)
            {
                throw new WabacusRuntimeException("输入框没有配置输入提示功能");
            }
            String inputvalue=rrequest.getStringAttribute("TYPE_PROMPT_TXTVALUE","");
            if(inputvalue.equals(""))
            {
                return "";
            }else
            {
                List<Map<String,String>> lstResults=promptBean.getDatasource().getResultDataList(rrequest,rbean,inputvalue);
                if(lstResults==null||lstResults.size()==0) return "";
                int cnt=promptBean.getResultcount();
                if(cnt>lstResults.size()) cnt=lstResults.size();
                for(int i=0;i<cnt;i++)
                {
                    sbuffer.append("<item ");
                    for(Entry<String,String> entryTmp:lstResults.get(i).entrySet())
                    {
                        sbuffer.append(entryTmp.getKey()).append("=\"").append(entryTmp.getValue()).append("\" ");
                    }
                    sbuffer.append("/>");
                }
            }
            
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载输入提示数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        return sbuffer.toString();
    }

    public static String getSelectBoxDataList(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        StringBuffer resultBuf=new StringBuffer();
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initGetSelectBoxDataList();
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            String selectboxParams=rrequest.getStringAttribute("SELECTBOXIDS_AND_PARENTVALUES","");
            if(selectboxParams.equals("")) return "";
            if(selectboxParams.startsWith("condition:"))
            {
                selectboxParams=selectboxParams.substring("condition:".length());
                List<String> lstSelectboxIds=Tools.parseStringToList(selectboxParams,";",false);
                SelectBox childSelectBoxTmp;
                for(String selectboxidTmp:lstSelectboxIds)
                {
                    childSelectBoxTmp=rbean.getChildSelectBoxInConditionById(selectboxidTmp);
                    if(childSelectBoxTmp==null)
                    {
                        throw new WabacusRuntimeException("报表"+rbean.getPath()+"不存在id为"+selectboxidTmp+"的子下拉框");
                    }
                    List<String[]> lstOptionsResult=childSelectBoxTmp.getOptionsList(rrequest,childSelectBoxTmp.getAllParentValues(rrequest,selectboxidTmp));
                    if(lstOptionsResult==null||lstOptionsResult.size()==0) continue;
                    resultBuf.append(assembleOptionsResult(selectboxidTmp,lstOptionsResult));
                }
            }else
            {
                List<Map<String,String>> lstParams=EditableReportAssistant.getInstance().parseSaveDataStringToList(selectboxParams);
                String realInputboxidTmp,inputboxidTmp;
                SelectBox childSelectBoxTmp;
                for(Map<String,String> mSelectBoxParamsTmp:lstParams)
                {
                    realInputboxidTmp=mSelectBoxParamsTmp.get("wx_inputboxid");//取到下拉框ID
                    if(realInputboxidTmp==null||realInputboxidTmp.trim().equals("")) continue;
                    inputboxidTmp=realInputboxidTmp;
                    int idx=inputboxidTmp.lastIndexOf("__");
                    if(idx>0) inputboxidTmp=inputboxidTmp.substring(0,idx);
                    childSelectBoxTmp=rbean.getChildSelectBoxInColById(inputboxidTmp);
                    if(childSelectBoxTmp==null)
                    {
                        throw new WabacusRuntimeException("报表"+rbean.getPath()+"不存在id为"+inputboxidTmp+"的子下拉框");
                    }
                    mSelectBoxParamsTmp.remove("wx_inputboxid");
                    List<String[]> lstOptionsResult=childSelectBoxTmp.getOptionsList(rrequest,mSelectBoxParamsTmp);
                    if(lstOptionsResult==null||lstOptionsResult.size()==0) continue;
                    resultBuf.append(assembleOptionsResult(realInputboxidTmp,lstOptionsResult));
                }
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载下拉框数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        if(resultBuf.length()>0) return "{"+resultBuf.toString()+"}";
        
        return resultBuf.toString();
    }

    private static String assembleOptionsResult(String realSelectboxid,List<String[]> lstOptionsResult)
    {
        if(lstOptionsResult==null||lstOptionsResult.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(realSelectboxid).append(":[");
        String[] items;
        String name_temp;
        String value_temp;
        for(int j=0;j<lstOptionsResult.size();j++)
        {
            items=lstOptionsResult.get(j);
            name_temp=items[0];
            value_temp=items[1];
            name_temp=name_temp==null?"":name_temp.trim();
            value_temp=value_temp==null?"":value_temp.trim();
            resultBuf.append("{name:\"").append(name_temp).append("\",");
            resultBuf.append("value:\"").append(value_temp).append("\"},");
        }
        if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        resultBuf.append("],");
        return resultBuf.toString();
    }

    public static String getAutoCompleteColValues(HttpServletRequest request,HttpServletResponse response)
    {
        StringBuffer resultBuf=new StringBuffer();
        ReportRequest rrequest=null;
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initGetAutoCompleteColValues();
            String conditionparams=request.getParameter("AUTOCOMPLETE_COLCONDITION_VALUES");
            List<Map<String,String>> lstConditionParamsValue=EditableReportAssistant.getInstance().parseSaveDataStringToList(conditionparams);
            if(lstConditionParamsValue==null||lstConditionParamsValue.size()==0) return "";
            AutoCompleteConfigBean accbean=rrequest.getAutoCompleteSourceInputBoxObj().getAutocompleteBean();
            String colConditionExpression=accbean.getRealColConditionExpression(lstConditionParamsValue.get(0));
            if(colConditionExpression==null||colConditionExpression.trim().equals("")) return "";
            rrequest.setAttribute("COL_CONDITION_EXPRESSION",colConditionExpression);//存入rrequest中以便查询数据时能使用
            rrequest.setAttribute("COL_CONDITION_VALUES",lstConditionParamsValue.get(0));
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            String sql=accbean.getAutocompletesql();
            Object objTmp=null;
            if(sql!=null&&!sql.trim().equals(""))
            {
                sql="select "+accbean.getAutoCompleteColumns()+" from ("+sql+") wx_tbl_autocompletedata where "+colConditionExpression;
                objTmp=new GetAllResultSetBySQL().getResultSet(rrequest,rbean,accbean,sql,null);
            }else
            {
                SqlBean sbean=rbean.getSbean();
                ISQLType sqlTypeObj=null;
                if(sbean.isStoreProcedure())
                {
                    sqlTypeObj=new GetResultSetByStoreProcedure();
                }else if(sbean.getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
                {
                    sqlTypeObj=new GetAllResultSetByPreparedSQL();
                }else
                {
                    sqlTypeObj=new GetAllResultSetBySQL();
                }
                objTmp=sqlTypeObj.getResultSet(rrequest,(AbsReportType)rrequest.getComponentTypeObj(rbean,null,true));
            }
            if(objTmp==null||rrequest.getWResponse().getMessageCollector().hasErrors()||rrequest.getWResponse().getMessageCollector().hasWarnings())
            {
                return "";
            }
            List<ColBean> lstAutoCompleteCols=accbean.getLstCompleteColBeans();
            Map<String,String> mColData=null;
            if(objTmp instanceof Map)
            {//用户直接在拦截器加载数据前置动作中返回了自己构造的填充列的值
                mColData=(Map<String,String>)objTmp;
            }else
            {
                mColData=new HashMap<String,String>();
                String colValTmp;
                ResultSet rs=(ResultSet)objTmp;
                if(rs.next())
                {
                    for(ColBean cbTmp:lstAutoCompleteCols)
                    {
                        colValTmp=rs.getString(cbTmp.getColumn());
                        if(colValTmp==null) colValTmp="";
                        mColData.put(cbTmp.getProperty(),colValTmp);
                    }
                    if(rs.next()&&!accbean.isEnableMultipleRecords())
                    {
                        rs.close();
                        return "";
                    }
                }
                rs.close();
            }
            if(rbean.getInterceptor()!=null)
            {
                mColData=(Map<String,String>)rbean.getInterceptor().afterLoadData(rrequest,rbean,
                        rrequest.getAutoCompleteSourceInputBoxObj().getAutocompleteBean(),mColData);
            }
            if(mColData.size()==0) return "";
            resultBuf.append("{");
            AbsReportType reportTypeObj=Config.getInstance().getReportType(rbean.getType());
            boolean isEditable2ReportType=(reportTypeObj instanceof EditableDetailReportType2)
                    ||(reportTypeObj instanceof EditableListReportType2&&!(reportTypeObj instanceof EditableListFormReportType));
            EditableReportColBean ercbTmp;
            String propTmp, valueTmp;
            for(ColBean cbTmp:lstAutoCompleteCols)
            {
                ercbTmp=(EditableReportColBean)cbTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
                propTmp=cbTmp.getProperty();
                if(ercbTmp==null||ercbTmp.getUpdatecol()==null||ercbTmp.getUpdatecol().trim().equals(""))
                {
                    valueTmp=mColData.get(propTmp);
                }else
                {
                    valueTmp=mColData.get(ercbTmp.getUpdatecol());
                    if(isEditable2ReportType) resultBuf.append(propTmp+"$label").append(":\"").append(mColData.get(propTmp)).append("\",");//如果是editablelist2/editabledetail2，则当前列的值要显示成label
                }
                if(valueTmp==null) valueTmp="";
                resultBuf.append(propTmp).append(":\"").append(valueTmp).append("\",");
                mColData.remove(propTmp);//移除掉，以便后面可以知道mCols中还有哪些没有处理
            }
            for(Entry<String,String> entryTmp:mColData.entrySet())
            {
                resultBuf.append(entryTmp.getKey()).append(":\"").append(entryTmp.getValue()).append("\",");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("}");
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载自动填充的输入框数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        return resultBuf.toString();
    }

    public static void showUploadFilePage(HttpServletRequest request,PrintWriter out)
    {
        String contentType=request.getHeader("Content-type");
        String fileuploadtype=null;
        if(contentType!=null&&contentType.startsWith("multipart/"))
        {
            fileuploadtype=(String)request.getAttribute("FILEUPLOADTYPE");
            fileuploadtype=fileuploadtype==null?"":fileuploadtype.trim();
        }else
        {
            fileuploadtype=Tools.getRequestValue(request,"FILEUPLOADTYPE","");
        }
        AbsFileUpload fileUpload=null;
        if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_FILEINPUTBOX))
        {
            fileUpload=new FileInputBoxUpload(request);
        }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_FILETAG))
        {
            fileUpload=new FileTagUpload(request);
        }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_DATAIMPORTREPORT))
        {
            fileUpload=new DataImportReportUpload(request);
        }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_DATAIMPORTTAG))
        {
            fileUpload=new DataImportTagUpload(request);
        }else
        {
            out.println("显示文件上传界面失败，未知的文件上传类型");
            return;
        }
        out.println("<form  action=\""+Config.showreport_url
                +"\" style=\"margin:0px\" method=\"post\" enctype=\"multipart/form-data\" name=\"uploadform\">");
        out.println("<input type='hidden' name='FILEUPLOADTYPE' value='"+fileuploadtype+"'/>");
        fileUpload.showUploadForm(out);
        out.println("</form>");
    }

    public static void uploadFile(HttpServletRequest request,HttpServletResponse response)
    {
        PrintWriter out=null;
        try
        {
            out=response.getWriter();
        }catch(IOException e1)
        {
           throw new WabacusRuntimeException("从response中获取PrintWriter对象失败",e1);
        }
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
        /*if(true)
        {
            return "这里是公共演示，不允许上传文件，您可以在本地部署WabacusDemo演示项目，进行完全体验，只需几步即可部署完成\n\rWabacusDemo.war位于下载包的samples/目录中";
        }*/
        DiskFileUpload fileUploadObj=new DiskFileUpload();
        fileUploadObj.setHeaderEncoding(Config.encode);
        List lstFieldItems=null;
        String errorinfo=null;
        try
        {
            lstFieldItems=fileUploadObj.parseRequest(request);
            if(lstFieldItems==null||lstFieldItems.size()==0)
            {
                errorinfo="上传失败，没有取到要上传的文件";
            }
        }catch(FileUploadException e)
        {
           log.error("获取上传文件失败",e);
           errorinfo="获取上传文件失败";
        }
        if(errorinfo==null||errorinfo.trim().equals("")) errorinfo=doUploadFile(request,lstFieldItems,out);
        AbsFileUploadInterceptor interceptorObj=(AbsFileUploadInterceptor)request.getAttribute("WX_FILE_UPLOAD_INTERCEPTOR");
        boolean isPromtAuto=true;
        if(interceptorObj!=null)
        {
            Map<String,String> mFormFieldValues=(Map<String,String>)request.getAttribute("WX_FILE_UPLOAD_FIELDVALUES");
            isPromtAuto=interceptorObj.beforeDisplayFileUploadPrompt(request,lstFieldItems,mFormFieldValues,errorinfo,out);
        }
        if(isPromtAuto)
        {//开发人员没有在拦截器的拦截方法中阻止框架的提示
            if(errorinfo==null||errorinfo.trim().equals(""))
            {
                out.println("<script language='javascript'>");
                out.println("parent.ymPrompt.doHandler('close',null);");
                out.println("parent.wx_success('上传文件成功');");
                out.println("</script>");
            }else
            {
                out.println("<table style=\"margin:0px;\"><tr><td style='font-size:13px;'><font color='#ff0000'>"+errorinfo+"</font></td></tr></table>");
            }
        }

//        {//公共演示环境使用
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {
            showUploadFilePage(request,out);
        }

    }
    
    private static String doUploadFile(HttpServletRequest request,List lstFieldItems,PrintWriter out)
    {
        try
        {
            Map<String,String> mFormFieldValues=new HashMap<String,String>();
            Iterator itFieldItems=lstFieldItems.iterator();
            FileItem item;
            while(itFieldItems.hasNext())
            {
                item=(FileItem)itFieldItems.next();
                if(item.isFormField())
                {
                    mFormFieldValues.put(item.getFieldName(),item.getString(Config.encode));
                    request.setAttribute(item.getFieldName(),item.getString(Config.encode));
                }
            }
            String fileuploadtype=mFormFieldValues.get("FILEUPLOADTYPE");
            fileuploadtype=fileuploadtype==null?"":fileuploadtype.trim();
            AbsFileUpload fileUpload=null;
            if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_FILEINPUTBOX))
            {
                fileUpload=new FileInputBoxUpload(request);
            }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_FILETAG))
            {
                fileUpload=new FileTagUpload(request);
            }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_DATAIMPORTREPORT))
            {
                fileUpload=new DataImportReportUpload(request);
            }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_DATAIMPORTTAG))
            {
                fileUpload=new DataImportTagUpload(request);
            }else
            {
                return "上传文件失败，未知的文件上传类型";
            }
            return fileUpload.doFileUpload(lstFieldItems,mFormFieldValues,out);
        }catch(Exception e)
        {
            log.error("上传文件失败",e);
            return "上传文件失败";
        }
    }

    public static void downloadFile(HttpServletRequest request,HttpServletResponse response)
    {
        response.setContentType("application/x-msdownload;");
        BufferedInputStream bis=null;
        BufferedOutputStream bos=null;
        String realfilepath=null;
        try
        {
            bos=new BufferedOutputStream(response.getOutputStream());
            String serverfilename=request.getParameter("serverfilename");//要下载的文件名
            String serverfilepath=request.getParameter("serverfilepath");
            String newfilename=request.getParameter("newfilename");
            if(serverfilename==null||serverfilename.trim().equals(""))
            {
                bos.write("没有取到要下载的文件名".getBytes());
                return;
            }
            if(serverfilepath==null||serverfilepath.trim().equals(""))
            {
                bos.write("没有取到要下载的文件路径".getBytes());
                return;
            }
            if(newfilename==null||newfilename.trim().equals("")) newfilename=serverfilename;
            newfilename=WabacusAssistant.getInstance().encodeAttachFilename(request,newfilename);
            response.setHeader("Content-disposition","attachment;filename="+newfilename);
            String realserverfilepath=null;
            if(Tools.isDefineKey("$",serverfilepath))
            {
                realserverfilepath=Config.getInstance().getResourceString(null,null,serverfilepath,true);
            }else
            {
                realserverfilepath=Tools.decodeFilePath(serverfilepath);
            }
            if(realserverfilepath==null||realserverfilepath.trim().equals(""))
            {
                bos.write(("根据"+serverfilepath+"没有取到要下载的文件路径").getBytes());
            }
            realserverfilepath=WabacusAssistant.getInstance().parseConfigPathToRealPath(realserverfilepath,Config.webroot_abspath);
            if(Tools.isDefineKey("classpath",realserverfilepath))
            {//如果是从classpath中取
                realserverfilepath=Tools.getRealKeyByDefine("classpath",realserverfilepath);
                realserverfilepath=Tools.replaceAll(realserverfilepath+"/"+serverfilename,"//","/").trim();
                while(realserverfilepath.startsWith("/"))
                    realserverfilepath=realserverfilepath.substring(1);//因为这种配置方式是用ClassLoader进行加载，而不是Class，所以必须不能以/打头
                bis=new BufferedInputStream(ConfigLoadManager.currentDynClassLoader.getResourceAsStream(realserverfilepath));
                response.setContentLength(bis.available());
            }else
            {
                File downloadFileObj=new File(Tools.standardFilePath(realserverfilepath+File.separator+serverfilename));
                if(!downloadFileObj.exists()||downloadFileObj.isDirectory())
                {
                    bos.write(("没有找到要下载的文件"+serverfilename).getBytes());
                    return;
                }
                
                response.setContentLength((int)downloadFileObj.length());
                bis=new BufferedInputStream(new FileInputStream(downloadFileObj));
            }
            byte[] buff=new byte[1024];
            int bytesRead;
            while((bytesRead=bis.read(buff,0,buff.length))!=-1)
            {
                bos.write(buff,0,bytesRead);
            }
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("下载文件"+realfilepath+"失败",e);
        }finally
        {
            try
            {
                if(bis!=null) bis.close();
            }catch(IOException e)
            {
                log.warn("下载文件"+realfilepath+"时，关闭输入流失败",e);
            }
            try
            {
                if(bos!=null) bos.close();
            }catch(IOException e)
            {
                log.warn("下载文件"+realfilepath+"时，关闭输出流失败",e);
            }
        }
    }

    public static String invokeServerAction(HttpServletRequest request,HttpServletResponse response)
    {
        String serverClassName=request.getParameter("WX_SERVERACTION_SERVERCLASS");
        if(serverClassName==null||serverClassName.trim().equals(""))
        {
            throw new WabacusRuntimeException("没有传入要调用的服务器端类");
        }
        String params=request.getParameter("WX_SERVERACTION_PARAMS");
        List<Map<String,String>> lstParamsValue=EditableReportAssistant.getInstance().parseSaveDataStringToList(params);
        try
        {
            Object obj=Class.forName(serverClassName.trim()).newInstance();
            if(!(obj instanceof IServerAction))
            {
                throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"没有实现"+IServerAction.class.getName()+"接口");
            }
            return ((IServerAction)obj).executeServerAction(request,response,lstParamsValue);
        }catch(InstantiationException e)
        {
            throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"无法实例化",e);
        }catch(IllegalAccessException e)
        {
            throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"无法访问",e);
        }catch(ClassNotFoundException e)
        {
            throw new WabacusRuntimeException("没有找到要调用的服务器端类"+serverClassName,e);
        }
    }
}
