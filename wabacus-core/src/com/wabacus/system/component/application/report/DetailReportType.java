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
package com.wabacus.system.component.application.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.itextpdf.text.Element;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.other.ButtonsBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsDetailReportType;
import com.wabacus.system.component.application.report.configbean.DetailReportColBean;
import com.wabacus.system.component.application.report.configbean.DetailReportColPositionBean;
import com.wabacus.system.component.application.report.configbean.DetailReportDisplayBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.RowDataByInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class DetailReportType extends AbsDetailReportType
{
    public final static String KEY=DetailReportType.class.getName();

    private static Log log=LogFactory.getLog(DetailReportType.class);

    protected Map<String,DetailReportColPositionBean> mColPositions;

    protected DetailReportDisplayBean drdbean;
    
    public DetailReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            drdbean=(DetailReportDisplayBean)((ReportBean)comCfgBean).getDbean().getExtendConfigDataForReportType(KEY);
        }
    }
    
    protected void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        if((cacheDataBean.getLstDynDisplayColids()!=null&&cacheDataBean.getLstDynDisplayColids().size()>0)
                ||(cacheDataBean.getAttributes().get("authroize_col_display")!=null&&String.valueOf(cacheDataBean.getAttributes().get("authroize_col_display")).trim()
                        .equals("false")))
        {
            mColPositions=calPosition(rbean.getDbean(),rbean.getDbean().getLstCols(),cacheDataBean.getLstDynDisplayColids());
            if(mColPositions==null||mColPositions.size()<=0) return;
        }else
        {
            mColPositions=drdbean.getMColDefaultPositions();
        }
    }

    protected String showButtonsOnTitleBar()
    {
        if(!rbean.getDbean().isColselect()) return super.showButtonsOnTitleBar();
        String colselectedBtn=ReportAssistant.getInstance().getColSelectedLabelAndEvent(rrequest,rbean,false);
        if(colselectedBtn==null||colselectedBtn.trim().equals("")) return super.showButtonsOnTitleBar();
        String superbtns=super.showButtonsOnTitleBar();
        if(superbtns==null||superbtns.trim().equals("")) return colselectedBtn;
        ButtonsBean bbeans=rbean.getButtonsBean();
        return superbtns+WabacusAssistant.getInstance().getSpacingDisplayString(bbeans.getButtonspacing())+colselectedBtn;
    }

    public String showReportData()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        if(mColPositions==null) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showReportScrollStartTag());
        if(this.lstReportData==null||this.lstReportData.size()==0)
        {
            resultBuf.append(showReportTablePropsForCommon()).append(">");
            String trstylepropertyTmp=null;
            if(this.rbean.getInterceptor()!=null)
            {
                RowDataByInterceptor rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,0,1,rbean.getDbean().getLstCols());
                if(rowdataObjTmp!=null)
                {
                    if(rowdataObjTmp.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                    trstylepropertyTmp=rowdataObjTmp.getDynTrStyleproperty();
                    if(!rowdataObjTmp.isShouldDisplayThisRow()) return resultBuf.toString();
                }
            }
            if(trstylepropertyTmp==null) trstylepropertyTmp="";
            resultBuf.append("<tr "+trstylepropertyTmp+">");
            resultBuf.append("<td bgcolor='#ffffff'>");
            if(this.isLazyDataLoad()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                resultBuf.append(rrequest.getStringAttribute(rbean.getId()+"_lazyloadmessage",""));
            }else
            {
                resultBuf.append(rrequest.getI18NStringValue((Config.getInstance().getResources().getString(rrequest,rbean.getPageBean(),
                        Consts.NODATA_PROMPT_KEY,true))));
            }
            resultBuf.append("</td></tr>");
            if(this.rbean.getInterceptor()!=null)
            {
                RowDataByInterceptor rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,Integer.MAX_VALUE,1,
                        rbean.getDbean().getLstCols());
                if(rowdataObjTmp!=null&&rowdataObjTmp.getInsertDisplayRowHtml()!=null)
                {
                    resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                }
            }
            resultBuf.append("</table>");
        }else
        {
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT
                    ||!this.cacheDataBean.shouldBatchDataExport())
            {
                showReportDataPart(resultBuf);
            }else
            {
                for(int i=0;i<this.cacheDataBean.getPagecount();i++)
                {
                    if(i!=0)
                    {
                        this.cacheDataBean.setPageno(i+1);
                        this.cacheDataBean.setRefreshNavigateInfoType(1);//不需要再计算页码之类
                        this.setHasLoadedDataFlag(false);
                        loadReportData(true);
                    }
                    showReportDataPart(resultBuf);
                }
            }
        }
        resultBuf.append(showReportScrollEndTag());
        return resultBuf.toString();
    }
    
    private void showReportDataPart(StringBuffer resultBuf)
    {
        int[] displayrowinfo=this.getDisplayRowInfo();
        if(displayrowinfo[1]<=0) return;
        int totalcolcnt=getTotalColCount();
        if(totalcolcnt<=0) return;
        int rowidx=0;
        Object dataObj,colDataObj;
        for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
        {
            resultBuf.append(showReportTablePropsForCommon()).append(">");
            dataObj=this.lstReportData.get(i);
            DetailReportColBean drcbeanTmp=null;
            String valueTdContent;
            StringBuffer tdPropsBuf;
            DetailReportColPositionBean colPositionBeanTmp;
//            boolean hasDisplayColInThisRow=false;//当前行中是否有显示列（还是所有列都不参与本次显示）
            StringBuffer trBuf=new StringBuffer();
            List<ColBean> lstColsInThisTr=new ArrayList<ColBean>();//临时存放当前行显示的所有<col/>的配置信息
            for(ColBean cbean:rbean.getDbean().getLstCols())
            {
                colDataObj=initDisplayCol(cbean,dataObj);
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype()))
                {
                    if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE) trBuf.append(showHiddenCol(cbean,colDataObj));
                    continue;
                }
                colPositionBeanTmp=mColPositions.get(cbean.getColid());
                if(colPositionBeanTmp.getDisplaymode()>0)
                {

                    trBuf.append(showColLabel(cbean));
                    lstColsInThisTr.add(cbean);
                }
                if((rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE||colPositionBeanTmp.getDisplaymode()>0)&&!cbean.isNonValueCol())
                {
                    trBuf.append("<td class=\""+getDataTdClassName()+"\"");
                    tdPropsBuf=new StringBuffer();
                    valueTdContent=getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);//获取此列<td/>的属性及显示内容
                    trBuf.append(" ").append(tdPropsBuf.toString()).append(">");
                    trBuf.append(valueTdContent);
                    trBuf.append("</td>");
                }
                drcbeanTmp=(DetailReportColBean)cbean.getExtendConfigDataForReportType(DetailReportType.KEY);

                if(drcbeanTmp.isBr()&&trBuf.length()>0)
                {
//                    hasDisplayColInThisRow=false;//新启一行，所以重置新行的此变量值
                    String trstyleproperty=null;
                    if(this.rbean.getInterceptor()!=null)
                    {
                        RowDataByInterceptor rowdataObj=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,rowidx,totalcolcnt,lstColsInThisTr);
                        if(rowdataObj!=null)
                        {
                            if(rowdataObj.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObj.getInsertDisplayRowHtml());
                            if(!rowdataObj.isShouldDisplayThisRow())
                            {
                                trBuf=new StringBuffer();
                                lstColsInThisTr=new ArrayList<ColBean>();
                                continue;
                            }
                            trstyleproperty=rowdataObj.getDynTrStyleproperty();
                        }
                    }
                    if(trstyleproperty==null) trstyleproperty="";
                    resultBuf.append("<tr ").append(trstyleproperty).append(">");
                    resultBuf.append(trBuf.toString()).append("</tr>");
                    trBuf=new StringBuffer();
                    lstColsInThisTr=new ArrayList<ColBean>();
                    rowidx++;
                }
            }

            if(trBuf.length()>0)
            {//最后一行还没有显示
                String trstyleproperty=null;
                if(this.rbean.getInterceptor()!=null)
                {
                    RowDataByInterceptor rowdataObj=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,rowidx,totalcolcnt,
                            lstColsInThisTr);
                    if(rowdataObj!=null)
                    {
                        if(rowdataObj.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObj.getInsertDisplayRowHtml());
                        if(!rowdataObj.isShouldDisplayThisRow())
                        {
                            trBuf=null;
                        }else
                        {
                            trstyleproperty=rowdataObj.getDynTrStyleproperty();
                        }
                    }
                }
                if(trBuf!=null&&trBuf.length()>0)
                {
                    if(trstyleproperty==null) trstyleproperty="";
                    resultBuf.append("<tr ").append(trstyleproperty).append(">");
                    resultBuf.append(trBuf.toString()).append("</tr>");
                }
            }
            if(this.rbean.getInterceptor()!=null)
            {
                RowDataByInterceptor rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,Integer.MAX_VALUE,
                        totalcolcnt,rbean.getDbean().getLstCols());
                if(rowdataObjTmp!=null&&rowdataObjTmp.getInsertDisplayRowHtml()!=null)
                {
                    resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                }
            }
            resultBuf.append("</table>");
            if(i<displayrowinfo[1]-1)
            {
                resultBuf.append("&nbsp;");
            }
        }
    }
    
    protected String getDataTdClassName()
    {
        return "cls-data-td-detail";
    }
    
    protected Object initDisplayCol(ColBean cbean,Object dataObj)
    {
        if(cbean.isNonValueCol()) return null;
        return cbean.getDisplayValue(dataObj,rrequest);
    }
    
    protected String showHiddenCol(ColBean cbean,Object colDataObj)
    {
        return "";
    }

    protected String getColValueTdPropertiesAndContent(ColBean cbean,Object dataObj,Object colDataObj,StringBuffer tdPropsBuf)
    {
        if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {
            tdPropsBuf.append(" style=\"display:none;\"");
            return "";
        }
        String col_displayvalue=(String)colDataObj;
        if(col_displayvalue==null) col_displayvalue="";
        ColDataByInterceptor coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,0,col_displayvalue);
        if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
        {
            col_displayvalue=coldataByInterceptor.getDynvalue();
        }
        tdPropsBuf.append(getDetailTdValuestyleproperty(cbean,coldataByInterceptor));
        return col_displayvalue;
    }
    
    protected String getDetailTdValuestyleproperty(ColBean cbean,ColDataByInterceptor coldataByInterceptor)
    {
        String valuestyleproperty=cbean.getValuestyleproperty(rrequest);
        if(valuestyleproperty==null) valuestyleproperty="";
        DetailReportColPositionBean colPositionBean=mColPositions.get(cbean.getColid());
        valuestyleproperty=valuestyleproperty+" colspan='"+colPositionBean.getColspan()+"'";
        if(colPositionBean.getColspan()<=1)
        {
            String widthTmp=null;
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
            {
                widthTmp=drdbean.getPrintvaluewidth();
            }else
            {
                widthTmp=drdbean.getValuetdwidth();
            }
            if(widthTmp!=null&&!widthTmp.trim().equals("")) valuestyleproperty=valuestyleproperty+" width='"+widthTmp+"'";
        }
        return getColGroupStyleproperty(valuestyleproperty,coldataByInterceptor);
    }
    
    protected String showReportTablePropsForCommon()
    {
        if(!rrequest.isDisplayOnPage()) return super.showReportTablePropsForNonOnPage();
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table class='cls-data-table-detail' cellspacing='0' cellpadding='0'  id=\""+rbean.getGuid()+"_data\"");
        resultBuf.append(" style=\"table-layout:fixed;");
        if(Consts_Private.REPORT_BORDER_NONE0.equals(rbean.getBorder()))
        {//如果不用显示外围表格的边框
            resultBuf.append("border:none;");
        }else if(Consts_Private.REPORT_BORDER_HORIZONTAL0.equals(rbean.getBorder()))
        {
            resultBuf.append("border-left:none;border-right:none;");
        }
        resultBuf.append("\"");
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {

//            {//配置了横向滚动条





            resultBuf.append(" width=\""+getReportDataWidthOnPage()+"\"");
            if(rbean.shouldShowContextMenu())
            {
                resultBuf.append(" oncontextmenu=\"try{showcontextmenu('contextmenu_"+rbean.getGuid()
                        +"',event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }
        }else
        {
            String printwidth=rbean.getPrintwidth();
            if(printwidth==null||printwidth.trim().equals("")) printwidth="100%";
            resultBuf.append(" width=\"").append(printwidth).append("\"");
        }
        return resultBuf.toString();
    }

    public String showColData(ColBean cbean,boolean showpart,boolean showcontent,String dynstyleproperty)
    {
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) return "";
        if(mColPositions==null) return "";
        DetailReportColPositionBean colPositionBeanTmp=mColPositions.get(cbean.getColid());
        if(colPositionBeanTmp.getDisplaymode()<=0) return "";
        if(showpart)
        {
            if(!showcontent) return "";
            if(cbean.isNonValueCol()) return "";
            int[] displayrowinfo=this.getDisplayRowInfo();
            if(displayrowinfo[1]<=0) return "";
            String value=cbean.getDisplayValue(this.lstReportData.get(displayrowinfo[0]),rrequest);
            if(value==null) value="";
            ColDataByInterceptor coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,0,value);
            if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
            {//如果拦截器对象返回此列的动态数据
                value=coldataByInterceptor.getDynvalue();
            }
            return value;//+showMetaData();//加上本报表要用的元数据，（因为showMetaData()方法自己会判断不重复显示，所以这里不用判断，直接调用即可）
        }else
        {
            String label=cbean.getLabel();
            if(label==null) return "";
            label=ReportAssistant.getInstance().getColGroupLabel(rrequest,label,ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,label));
            if(label.equals("")) label="&nbsp;";
            return label;
        }
    }
    
    public void showReportOnPlainExcel(Workbook workbook)
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(mColPositions==null) return;
        createNewSheet(workbook,20);
        if(!this.cacheDataBean.shouldBatchDataExport())
        {
            showReportDataOnPlainExcel(workbook);
        }else
        {
            for(int i=0;i<this.cacheDataBean.getPagecount();i++)
            {
                if(i!=0)
                {
                    this.cacheDataBean.setPageno(i+1);
                    this.cacheDataBean.setRefreshNavigateInfoType(1);
                    this.setHasLoadedDataFlag(false);
                    loadReportData(true);
                }
                showReportDataOnPlainExcel(workbook);
            }
        }
    }
    
    private void showReportDataOnPlainExcel(Workbook workbook)
    {
        DisplayBean dbean=rbean.getDbean();
        if(lstReportData==null||lstReportData.size()==0)
        {
            lstReportData=new ArrayList();
            lstReportData.add(ReportAssistant.getInstance().getReportDataPojoInstance(rbean));
        }
        for(Object object:this.lstReportData)
        {
            DetailReportColBean drcolbean=null;
            ColDataByInterceptor coldataByInterceptor;
            int startcolidx=0;
            int endcolidx=-1;
            if(sheetsize>0&&excelRowIdx>=sheetsize)
            {
                createNewSheet(workbook,20);
            }
            CellStyle titleCellStyle=StandardExcelAssistant.getInstance().getTitleCellStyleForStandardExcel(workbook);
            CellStyle dataCellStyle=StandardExcelAssistant.getInstance().getDataCellStyleForStandardExcel(workbook);
            CellStyle dataCellStyleWithFormat=StandardExcelAssistant.getInstance().getDataCellStyleForStandardExcel(workbook);//获取带格式数据行的样式对象（比如日期类型都带格式）
            Row dataRow=excelSheet.createRow(this.excelRowIdx);
            DetailReportColPositionBean colPositionBeanTmp;
            boolean hasDisplayColInThisRow=false;
            Cell cell;
            CellRangeAddress region;
            int colspan;
            String labelTmp;
            for(ColBean cbean:dbean.getLstCols())
            {
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
                drcolbean=(DetailReportColBean)cbean.getExtendConfigDataForReportType(KEY);
                colPositionBeanTmp=mColPositions.get(cbean.getColid());
                if(colPositionBeanTmp.getDisplaymode()>0)
                {
                    hasDisplayColInThisRow=true;
                    if(cbean.getLabel()!=null)
                    {//<col/>的label没有配置时，不为它显示标题列
                        String plainexceltitle=null;
                        if(this.pedebean!=null) plainexceltitle=this.pedebean.getPlainexceltitle();
                        if("none".equals(plainexceltitle))
                        {
                            labelTmp="";
                        }else if("column".equals(plainexceltitle))
                        {
                            labelTmp=cbean.getColumn();
                        }else
                        {
                            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,cbean.getLabel());
                            labelTmp=ReportAssistant.getInstance().getColGroupLabel(rrequest,cbean.getLabel(),coldataByInterceptor);
                        }
                        colspan=drcolbean.getLabelcolspan();
                        if(colspan<=0) colspan=1;
                        startcolidx=endcolidx+1;
                        endcolidx=startcolidx+colspan-1;
                        if(colspan==1)
                        {
                            cell=dataRow.createCell(endcolidx);
                            cell.setCellType(Cell.CELL_TYPE_STRING);
                            
                            cell.setCellValue(labelTmp);
                            cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,cbean.getLabelalign()));
                        }else
                        {
                            region=new CellRangeAddress(excelRowIdx,excelRowIdx,startcolidx,endcolidx);
                            StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,excelSheet,region,
                                    StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,cbean.getLabelalign()),labelTmp);
                        }
                    }
                    if(!cbean.isNonValueCol())
                    {
                        Object objvalueTmp=cbean.getRealTypeValue(object,rrequest);
                        colspan=colPositionBeanTmp.getColspan();
                        if(colspan<=0) colspan=1;
                        startcolidx=endcolidx+1;
                        endcolidx=startcolidx+colspan-1;
                        if(colspan==1)
                        {
                            cell=dataRow.createCell(endcolidx);
                            boolean flag=StandardExcelAssistant.getInstance().setCellValue(workbook,cbean.getValuealign(),cell,objvalueTmp,cbean.getDatatypeObj(),dataCellStyleWithFormat);
                            if(!flag) cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(dataCellStyle,cbean.getValuealign()));
                        }else
                        {
                            region=new CellRangeAddress(excelRowIdx,excelRowIdx,startcolidx,endcolidx);
                            StandardExcelAssistant.getInstance().setRegionCellRealTypeValue(workbook,excelSheet,region,
                                    StandardExcelAssistant.getInstance().setCellAlign(dataCellStyle,cbean.getValuealign()),dataCellStyleWithFormat,cbean.getValuealign(),objvalueTmp,
                                    cbean.getDatatypeObj());
                        }
                    }
                }
                if(drcolbean.isBr()&&hasDisplayColInThisRow)
                {//当前行显示了列数据并且显示完此列后要换新行
                    hasDisplayColInThisRow=false;
                    dataRow=excelSheet.createRow(++excelRowIdx);
                    startcolidx=0;
                    endcolidx=-1;
                }
            }
            excelRowIdx=excelRowIdx+2;
        }
    }

    protected void showReportOnPdfWithoutTpl()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(mColPositions==null) return;
        this.createNewPdfPage();
        if(!this.cacheDataBean.shouldBatchDataExport())
        {
            showReportDataOnPdf();
        }else
        {
            for(int i=0;i<this.cacheDataBean.getPagecount();i++)
            {
                if(i!=0)
                {
                    this.cacheDataBean.setPageno(i+1);
                    this.cacheDataBean.setRefreshNavigateInfoType(1);
                    this.setHasLoadedDataFlag(false);
                    loadReportData(true);
                }
                showReportDataOnPdf();
            }
        }
    }
    
    protected int getTotalColCount()
    {
        int totalcolcount=0, colspan;
        DetailReportColBean drcolbean=null;
        DetailReportColPositionBean colPositionBeanTmp;
        for(ColBean cbean:rbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            drcolbean=(DetailReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            colPositionBeanTmp=mColPositions.get(cbean.getColid());
            if(colPositionBeanTmp.getDisplaymode()>0)
            {
                if(cbean.getLabel()!=null)
                {//<col/>的label没有配置时，不为它显示标题列
                    colspan=drcolbean.getLabelcolspan();
                    if(colspan<=0) colspan=1;
                    totalcolcount+=colspan;
                }
                if(!cbean.isNonValueCol())
                {
                    colspan=colPositionBeanTmp.getColspan();
                    if(colspan<=0) colspan=1;
                    totalcolcount+=colspan;
                }
            }
            if(drcolbean.isBr()&&totalcolcount>0) break;
        }
        return totalcolcount;
    }
    
    private void showReportDataOnPdf()
    {
        DisplayBean dbean=rbean.getDbean();
        if(lstReportData==null||lstReportData.size()==0)
        {
            lstReportData=new ArrayList();
            lstReportData.add(ReportAssistant.getInstance().getReportDataPojoInstance(rbean));
        }
        for(Object object:this.lstReportData)
        {
            if(this.pdfpagesize>0&&this.pdfrowindex!=0&&this.pdfrowindex%this.pdfpagesize==0)
            {
                this.createNewPdfPage();
            }
            DetailReportColBean drcolbean=null;
            ColDataByInterceptor coldataByInterceptor;
            DetailReportColPositionBean colPositionBeanTmp;
            int colspan;
            String labelTmp;
            for(ColBean cbean:dbean.getLstCols())
            {
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
                drcolbean=(DetailReportColBean)cbean.getExtendConfigDataForReportType(KEY);
                colPositionBeanTmp=mColPositions.get(cbean.getColid());
                if(colPositionBeanTmp.getDisplaymode()>0)
                {
                    if(cbean.getLabel()!=null)
                    {//<col/>的label没有配置时，不为它显示标题列
                        coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,cbean.getLabel());
                        labelTmp=ReportAssistant.getInstance().getColGroupLabel(rrequest,cbean.getLabel(),coldataByInterceptor);
                        colspan=drcolbean.getLabelcolspan();
                        if(colspan<=0) colspan=1;
                        addDataHeaderCell(cbean,labelTmp,1,colspan,this.getPdfCellAlign(cbean.getLabelalign(),Element.ALIGN_LEFT));
                    }
                    if(!cbean.isNonValueCol())
                    {//此列有数据部分
                        String valueTmp=cbean.getDisplayValue(object,rrequest);
                        coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,this.pdfrowindex,valueTmp);
                        if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                        {
                            valueTmp=coldataByInterceptor.getDynvalue();
                        }
                        colspan=colPositionBeanTmp.getColspan();
                        if(colspan<=0) colspan=1;
                        addDataCell(cbean,valueTmp,1,colspan,this.getPdfCellAlign(cbean.getValuealign(),Element.ALIGN_LEFT));
                    }
                }
            }
            this.pdfrowindex++;
        }
    }
    
    public String getColSelectedMetadata()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(this.mColPositions==null||this.mColPositions.size()==0) return "";
        DisplayBean dbean=rbean.getDbean();
        if(!dbean.isColselect()) return "";
        DetailReportColPositionBean positionBeanTmp;
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<span id=\"").append(rbean.getGuid()).append("_col_titlelist\" style=\"display:none\">");
        String title;
        for(ColBean cbeanTmp:rbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbeanTmp.getDisplaytype())) continue;
            if(cbeanTmp.isNonValueCol()) continue;
            positionBeanTmp=mColPositions.get(cbeanTmp.getColid());
            if(positionBeanTmp.getDisplaymode()<0) continue;
            title=cbeanTmp.getLabel();
            title=title==null?"":title.replaceAll("<.*?\\>","").trim();
            resultBuf.append("<item nodeid=\"").append(cbeanTmp.getColid()).append("\"");
            resultBuf.append(" parentgroupid=\"\"");
            resultBuf.append(" childids=\"\"");
            resultBuf.append(" layer=\"0\"");
            resultBuf.append(" title=\"").append(title).append("\"");
            resultBuf.append(" checked=\"").append(positionBeanTmp.getDisplaymode()>0).append("\"");
            resultBuf.append(" isControlCol=\"false\"");
            resultBuf.append(" isNonFixedCol=\"true\"");
            resultBuf.append(" always=\"").append(positionBeanTmp.getDisplaymode()==2).append("\"");
            resultBuf.append("></item>");
        }
        resultBuf.append("</span>");
        return resultBuf.toString();
    }
    
    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        XmlElementBean eleColBean=lstEleColBeans.get(0);
        String br=eleColBean.attributeValue("br");
        DetailReportColBean drcolbean=(DetailReportColBean)colbean.getExtendConfigDataForReportType(KEY);
        if(drcolbean==null)
        {
            drcolbean=new DetailReportColBean(colbean);
            colbean.setExtendConfigDataForReportType(KEY,drcolbean);
        }
        if(br!=null&&br.trim().equalsIgnoreCase("true"))
        {
            drcolbean.setBr(true);
        }
        DetailReportDisplayBean drdbean=(DetailReportDisplayBean)colbean.getParent().getExtendConfigDataForReportType(KEY);
        if(colbean.getLabel()==null)
        {
            drcolbean.setLabelcolspan(0);
        }else
        {
            int ilabelcolspan=1;
            String labelcolspan=Tools.getPropertyValueByName("colspan",colbean.getLabelstyleproperty(),true);
            if(labelcolspan!=null&&!labelcolspan.trim().equals(""))
            {
                try
                {
                    ilabelcolspan=Integer.parseInt(labelcolspan);
                }catch(NumberFormatException e)
                {
                    ilabelcolspan=1;
                    log.warn("为报表"+colbean.getReportBean().getPath()+"的列"+colbean.getProperty()+"配置的labelstyleproperty中的colbean"+labelcolspan+"不是有效数字",e);
                }
            }
            drcolbean.setLabelcolspan(ilabelcolspan);
            if(ilabelcolspan<2&&Tools.getPropertyValueByName("width",colbean.getLabelstyleproperty(),true)==null)
            {//只有ilabelcolspan值为1且没有在<col/>中通过labeltdproperty设置width时，才用<display/>中的labeltdwidth做为此标题列宽
                String labeltdwidth=drdbean.getLabeltdwidth();
                if(labeltdwidth!=null&&!labeltdwidth.trim().equals(""))
                {
                    colbean.setLabelstyleproperty(colbean.getLabelstyleproperty()+" width='"+labeltdwidth+"' ");
                }
            }
            String labelbgcolor=drdbean.getLabelbgcolor();
            if(labelbgcolor!=null&&!labelbgcolor.trim().equals("")&&Tools.getPropertyValueByName("bgcolor",colbean.getLabelstyleproperty(),true)==null)
            {
                colbean.setLabelstyleproperty(colbean.getLabelstyleproperty()+" bgcolor='"+labelbgcolor+"' ");
            }
            String labelalign=drdbean.getLabelalign();
            if(labelalign!=null&&!labelalign.trim().equals("")&&(colbean.getLabelalign()==null||colbean.getLabelalign().trim().equals("")))
            {//如果没有在<col/>的labelstyleproperty中配置align，但在<display/>中配置了labelalign，则使用这里配置的labelalign
                colbean.setLabelstyleproperty(colbean.getLabelstyleproperty()+" align='"+labelalign+"' ");
                colbean.setLabelalign(labelalign.trim());
            }
            String printlabelstyleproperty=colbean.getPrintlabelstyleproperty();
            if(printlabelstyleproperty==null||printlabelstyleproperty.trim().equals(""))
            {
                printlabelstyleproperty=colbean.getLabelstyleproperty();
            }
            if(printlabelstyleproperty==null) printlabelstyleproperty="";
            String printlabelwidth=drdbean.getPrintlabelwidth();
            if(printlabelwidth!=null&&!printlabelwidth.trim().equals("")&&drcolbean.getLabelcolspan()<2)
            {//在<display/>的printlabelwidth配置值优先级更高
                printlabelstyleproperty=Tools.removePropertyValueByName("width",printlabelstyleproperty);
                printlabelstyleproperty=printlabelstyleproperty+" width=\""+printlabelwidth+"\"";
            }
            colbean.setPrintlabelstyleproperty(printlabelstyleproperty);
        }
        
        if(colbean.isNonValueCol())
        {
            drcolbean.setValuecolspan(0);
        }else
        {
            int ivaluecolspan=1;
            String valuecolspan=Tools.getPropertyValueByName("colspan",colbean.getValuestyleproperty(),true);
            if(valuecolspan!=null&&!valuecolspan.trim().equals(""))
            {
                try
                {
                    ivaluecolspan=Integer.parseInt(valuecolspan.trim());
                }catch(NumberFormatException e)
                {
                    ivaluecolspan=1;
                    log.warn("为报表"+colbean.getReportBean().getPath()+"的列"+colbean.getProperty()+"配置的valuestyleproperty中的colbean"+valuecolspan+"不是有效数字",e);
                }
                colbean.setValuestyleproperty(Tools.removePropertyValueByName("colspan",colbean.getValuestyleproperty()));
            }
            drcolbean.setValuecolspan(ivaluecolspan);
            String valuebgcolor=drdbean.getValuebgcolor();
            if(Tools.getPropertyValueByName("bgcolor",colbean.getValuestyleproperty(),true)==null)
            {
                if(valuebgcolor!=null&&!valuebgcolor.trim().equals(""))
                {
                    colbean.setValuestyleproperty(colbean.getValuestyleproperty()+" bgcolor='"+valuebgcolor+"' ");
                }else
                {
                    colbean.setValuestyleproperty(colbean.getValuestyleproperty()+" bgcolor='#ffffff' ");
                }
            }
            String valuealign=drdbean.getValuealign();
            if(valuealign!=null&&!valuealign.trim().equals("")&&(colbean.getValuealign()==null||colbean.getValuealign().trim().equals("")))
            {//如果没有在<col/>的valuestyleproperty中配置algin，但在<display/>中配置了valuealign，则使用这里配置的valuealign
                colbean.setValuestyleproperty(colbean.getValuestyleproperty()+"align='"+valuealign+"' ");
                colbean.setValuealign(valuealign.trim());
            }
            String printvaluestyleproperty=colbean.getPrintvaluestyleproperty();
            if(printvaluestyleproperty==null||printvaluestyleproperty.trim().equals(""))
            {
                printvaluestyleproperty=colbean.getValuestyleproperty();
            }else
            {
                printvaluestyleproperty=Tools.removePropertyValueByName("colspan",printvaluestyleproperty);
            }

//            String printvaluewidth=drdbean.getPrintvaluewidth();

//            {//在<display/>的printvaluewidth配置值优先级更高



            colbean.setPrintvaluestyleproperty(printvaluestyleproperty);
        }
        return 1;
    }

    public int beforeDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.beforeDisplayLoading(disbean,lstEleDisplayBeans);
        Map<String,String> mJoinedAttributes=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "labeltdwidth", "valuetdwidth", "labelbgcolor", "valuebgcolor", "labelalign", "valuealign","printlabelwidth","printvaluewidth" });
        String labeltdwidth=mJoinedAttributes.get("labeltdwidth");
        String valuetdwidth=mJoinedAttributes.get("valuetdwidth");
        String labelbgcolor=mJoinedAttributes.get("labelbgcolor");
        String valuebgcolor=mJoinedAttributes.get("valuebgcolor");
        String labelalign=mJoinedAttributes.get("labelalign");
        String valuealign=mJoinedAttributes.get("valuealign");
        String printlabelwidth=mJoinedAttributes.get("printlabelwidth");
        String printvaluewidth=mJoinedAttributes.get("printvaluewidth");
        DetailReportDisplayBean drdbean=(DetailReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(drdbean==null)
        {
            drdbean=new DetailReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,drdbean);
        }
        if(labeltdwidth!=null&&!labeltdwidth.trim().equals(""))
        {
            drdbean.setLabeltdwidth(labeltdwidth.trim());
        }
        if(valuetdwidth!=null&&!valuetdwidth.trim().equals(""))
        {
            drdbean.setValuetdwidth(valuetdwidth.trim());
        }
        if(labelbgcolor!=null&&!labelbgcolor.trim().equals(""))
        {
            drdbean.setLabelbgcolor(labelbgcolor.trim());
        }
        if(valuebgcolor!=null&&!valuebgcolor.trim().equals(""))
        {
            drdbean.setValuebgcolor(valuebgcolor.trim());
        }
        if(labelalign!=null&&!labelalign.trim().equals(""))
        {
            drdbean.setLabelalign(labelalign.trim());
        }
        if(valuealign!=null&&!valuealign.trim().equals(""))
        {
            drdbean.setValuealign(valuealign.trim());
        }
        if(printlabelwidth!=null&&!printlabelwidth.trim().equals(""))
        {
            drdbean.setPrintlabelwidth(printlabelwidth);
        }
        if(printvaluewidth!=null&&!printvaluewidth.trim().equals(""))
        {
            drdbean.setPrintvaluewidth(printvaluewidth.trim());
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        DisplayBean disbean=reportbean.getDbean();
        Map<String,DetailReportColPositionBean> mPositions=calPosition(disbean,disbean.getLstCols(),null);
        if(mPositions!=null)
        {
            DetailReportDisplayBean drdbean=(DetailReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
            if(drdbean==null)
            {
                drdbean=new DetailReportDisplayBean(disbean);
                disbean.setExtendConfigDataForReportType(KEY,drdbean);
            }
            drdbean.setMColDefaultPositions(mPositions);
        }
        return 1;
    }

    public Map<String,DetailReportColPositionBean> calPosition(DisplayBean disbean,List<ColBean> lstColBeans,List<String> lstDisplayColIds)
    {
        Map<String,DetailReportColPositionBean> mPositions=new HashMap<String,DetailReportColPositionBean>();
        List<List<DetailReportColPositionBean>> lstDisplayColPositions=new ArrayList<List<DetailReportColPositionBean>>();
        if(lstColBeans==null||lstColBeans.size()==0) return null;
        int maxcolspan=calPositionStart(disbean,lstColBeans,lstDisplayColIds,mPositions,lstDisplayColPositions);
        if(maxcolspan==0) return null;
        calPositionEnd(disbean,lstDisplayColPositions,maxcolspan);
        return mPositions;
    }

    private int calPositionStart(DisplayBean disbean,List<ColBean> lstColBeans,List<String> lstDisplayColIds,Map<String,DetailReportColPositionBean> mPositions,
            List<List<DetailReportColPositionBean>> lstDisplayColsPositions)
    {
        int maxcolspan=0;
        int currentRowColspan=0;
        DetailReportColBean drcolbeanTmp;
        DetailReportColPositionBean positionBeanTmp;
        List<DetailReportColPositionBean> lstPositionBeans=new ArrayList<DetailReportColPositionBean>();
        for(ColBean cbeanTmp:lstColBeans)
        {
            drcolbeanTmp=(DetailReportColBean)cbeanTmp.getExtendConfigDataForReportType(KEY);
            positionBeanTmp=new DetailReportColPositionBean(cbeanTmp);
            mPositions.put(cbeanTmp.getColid(),positionBeanTmp);
            positionBeanTmp.setDisplaymode(cbeanTmp.getDisplaymode(rrequest,lstDisplayColIds));
            if(positionBeanTmp.getDisplaymode()>0)
            {
                currentRowColspan+=drcolbeanTmp.getLabelcolspan();
                currentRowColspan+=drcolbeanTmp.getValuecolspan();
                lstPositionBeans.add(positionBeanTmp);
            }
            if(drcolbeanTmp!=null&&drcolbeanTmp.isBr())
            {//显示完当前列后进行分行显示
                if(currentRowColspan>maxcolspan) maxcolspan=currentRowColspan;
                currentRowColspan=0;
                if(lstPositionBeans.size()>0)
                {
                    lstDisplayColsPositions.add(lstPositionBeans);
                    lstPositionBeans=new ArrayList<DetailReportColPositionBean>();
                }
            }
        }
        if(currentRowColspan>maxcolspan) maxcolspan=currentRowColspan;
        if(lstPositionBeans.size()>0)
        {
            lstDisplayColsPositions.add(lstPositionBeans);
        }
        return maxcolspan;
    }

    private void calPositionEnd(DisplayBean disbean,List<List<DetailReportColPositionBean>> lstDisplayColPositions,int maxcolspan)
    {
        int currentRowColspan=0;
        DetailReportColPositionBean positionBeanTmp;
        DetailReportColBean drcolbeanTmp;
        for(List<DetailReportColPositionBean> lstRowsTmp:lstDisplayColPositions)
        {
            for(int i=0,len=lstRowsTmp.size()-1;i<=len;i++)
            {
                positionBeanTmp=lstRowsTmp.get(i);
                drcolbeanTmp=(DetailReportColBean)positionBeanTmp.getColbean().getExtendConfigDataForReportType(KEY);
                if(i!=len)
                {
                    positionBeanTmp.setColspan(drcolbeanTmp.getValuecolspan());
                    currentRowColspan+=drcolbeanTmp.getLabelcolspan();
                    currentRowColspan+=drcolbeanTmp.getValuecolspan();
                }else
                {
                    currentRowColspan+=drcolbeanTmp.getLabelcolspan();
                    if(rrequest!=null||disbean.isColselect())
                    {
                        positionBeanTmp.setColspan(maxcolspan-currentRowColspan);
                    }else
                    {//如果不提供列选择功能，则不强行让它们保持一样的colspan数，比如某列占据多行的情况，则它们的colspan数不一致。
                        positionBeanTmp.setColspan(drcolbeanTmp.getValuecolspan());
                    }
                    currentRowColspan=0;
                }
            }
        }
    }
}
