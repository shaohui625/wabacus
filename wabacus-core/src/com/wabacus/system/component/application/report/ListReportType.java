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
package com.wabacus.system.component.application.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportStatiColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportStatiRowGroupBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupDisplayBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.RowDataByInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class ListReportType extends AbsListReportType
{
    private final static Log log=LogFactory.getLog(ListReportType.class);

    protected FixedColAndGroupDataBean fixedDataBean;

    public ListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public void initReportBeforeDoStart()
    {
        super.initReportBeforeDoStart();
        ListReportAssistant.getInstance().storeRoworder(rrequest,rbean);
    }

    public String showReportData()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        if(this.cacheDataBean.getTotalColCount()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            if(this.alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL)
            {
                return showReportDataWithVerticalScroll();
            }else if(this.alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_FIXED)
            {//冻结行列标题
                fixedDataBean=new FixedColAndGroupDataBean();
            }
        }
        resultBuf.append(showScrollStartTag());
        resultBuf.append(showReportTableStart());
        resultBuf.append(">");
        DisplayBean dbean=rbean.getDbean();
        if(dbean!=null)
        {
            if(dbean.getDataheader()!=null)
            {
                resultBuf.append(rrequest.getI18NStringValue(dbean.getDataheader()));
            }else
            {
                resultBuf.append(showDataHeaderPart());
            }
        }
        resultBuf.append(showDataPart());
        if(this.rbean.getInterceptor()!=null)
        {
            RowDataByInterceptor rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,Integer.MAX_VALUE,
                    this.cacheDataBean.getTotalColCount(),this.getLstDisplayColBeans());
            if(rowdataObjTmp!=null&&rowdataObjTmp.getInsertDisplayRowHtml()!=null)
            {
                resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
            }
        }
        resultBuf.append("</table>");
        resultBuf.append(showScrollEndTag());
        if(this.fixedDataBean!=null)
        {
            rrequest.getWResponse().addOnloadMethod("fixedRowColTable","{pageid:\""+rbean.getPageBean().getId()+"\",reportid:\""+rbean.getId()+"\"}",
                    false);
        }
        return resultBuf.toString();
    }

    protected boolean isFixedLayoutTable()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return false;
        if(rbean.getCellresize()>0) return true;
        if(alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL) return true;//只提供垂直滚动条
        return false;
    }

    protected String showReportTableStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showReportTablePropsForCommon());
        resultBuf.append(showReportTablePropsForDataPart());
        resultBuf.append(showReportTablePropsForTitlePart(false));
        return resultBuf.toString();
    }

    protected String showReportTablePropsForCommon()
    {
        if(!rrequest.isDisplayOnPage()) return super.showReportTablePropsForNonOnPage();
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table class=\""+getDataTableClassName()+"\"");
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            if(rbean.getWidth()!=null&&!rbean.getWidth().trim().equals(""))
            {
                resultBuf.append(" width=\""+rbean.getWidth()+"\"");
            }else
            {
                resultBuf.append(" width=\"100%\"");
            }
            resultBuf.append(" pageid=\"").append(rbean.getPageBean().getId()).append("\"");
            resultBuf.append(" reportid=\"").append(rbean.getId()).append("\"");
            resultBuf.append(" refreshComponentGuid=\"").append(rbean.getRefreshGuid()).append("\"");
            if(rbean.isSlaveReportDependsonListReport())
            {
                resultBuf.append(" isSlave=\"true\"");
            }
        }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            String printwidth=rbean.getPrintwidth();
            if(printwidth==null||printwidth.trim().equals("")) printwidth="100%";
            resultBuf.append(" width=\"").append(printwidth).append("\"");
        }
        return resultBuf.toString();
    }

    protected String showReportTablePropsForDataPart()
    {
        if(!rrequest.isDisplayOnPage()) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(" id=\"").append(rbean.getGuid()).append("_data\"");
        resultBuf.append(" style=\"");
        if(Consts_Private.REPORT_BORDER_NONE0.equals(rbean.getBorder()))
        {
            resultBuf.append("border:none;");
        }else if(Consts_Private.REPORT_BORDER_HORIZONTAL0.equals(rbean.getBorder()))
        {//如果不用显示外围表格的纵向边框
            resultBuf.append("border-left:none;border-right:none;");
        }
        if(isFixedLayoutTable()) resultBuf.append("table-layout:fixed;");
        resultBuf.append("\"");
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            String rowselecttype=rrequest.getCdb(rbean.getId()).getRowSelectType();
            if(rowselecttype!=null&&(rowselecttype.trim().equals(Consts.ROWSELECT_SINGLE)||rowselecttype.trim().equals(Consts.ROWSELECT_MULTIPLY)))
            {
                resultBuf.append(" onClick=\"try{doSelectDataRowEvent(event,'"+rowselecttype+"');}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }
            if(rbean.shouldShowContextMenu())
            {
                resultBuf.append(" oncontextmenu=\"try{showcontextmenu('contextmenu_"+rbean.getGuid()
                        +"',event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }
        }
        return resultBuf.toString();
    }

    protected String showReportTablePropsForTitlePart(boolean isAbsoluteTable)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(!rrequest.isDisplayOnPage()) return "";
        if(isAbsoluteTable)
        {
            resultBuf.append(" id=\"").append(rbean.getGuid()).append("_dataheader\"");
            if(isFixedLayoutTable()) resultBuf.append(" style=\"table-layout:fixed;\"");
        }
        return resultBuf.toString();
    }

    protected String getDataTableClassName()
    {
        if(Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder()))
        {
            return "cls-articlelist-data-table";
        }
        return "cls-data-table";
    }

    public String showReportData(boolean showtype)
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        if(this.cacheDataBean.getTotalColCount()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showReportTablePropsForCommon());
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            if(showtype)
            {
                resultBuf.append(showReportTablePropsForDataPart());
            }else
            {//显示数据标题部分
                resultBuf.append(showReportTablePropsForTitlePart(true));
            }
        }
        resultBuf.append(">");
        if(showtype)
        {
            resultBuf.append(showDataPart());
        }else
        {
            DisplayBean dbean=rbean.getDbean();
            if(dbean!=null)
            {
                if(dbean.getDataheader()!=null)
                {
                    resultBuf.append(rrequest.getI18NStringValue(dbean.getDataheader()));
                }else
                {
                    resultBuf.append(showDataHeaderPart());
                }
            }
        }
        if(showtype&&this.rbean.getInterceptor()!=null)
        {
            RowDataByInterceptor rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,Integer.MAX_VALUE,
                    this.cacheDataBean.getTotalColCount(),this.getLstDisplayColBeans());
            if(rowdataObjTmp!=null&&rowdataObjTmp.getInsertDisplayRowHtml()!=null)
            {//在显示完最后一行后还在此拦截方法中显示了信息
                resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
            }
        }
        resultBuf.append("</table>");
        return resultBuf.toString();
    }

    protected String showDataHeaderPart()
    {
        DisplayBean dbean=rbean.getDbean();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        StringBuffer resultBuf=new StringBuffer();
        String thstyleproperty=null;
        if(this.rbean.getInterceptor()!=null)
        {
            RowDataByInterceptor rowdataObj=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,-1,cdb.getTotalColCount(),this.getLstDisplayColBeans());
            if(rowdataObj!=null)
            {
                if(rowdataObj.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObj.getInsertDisplayRowHtml());
                thstyleproperty=rowdataObj.getDynTrStyleproperty();
                if(!rowdataObj.isShouldDisplayThisRow()) return resultBuf.toString();
            }
        }
        if(thstyleproperty==null) thstyleproperty="";
        resultBuf.append("<tr  class='"+getDataHeaderTrClassName()+"' ").append(thstyleproperty).append(">");
        AbsListReportColBean alrcbean;
        ColDataByInterceptor coldataByInterceptor;
        List<ColAndGroupDisplayBean> lstColAndGroupDisplayBeans=null;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&dbean.isColselect())
        {
            lstColAndGroupDisplayBeans=new ArrayList<ColAndGroupDisplayBean>();
            rrequest.addColAndGroupDisplayBean(rbean.getId(),lstColAndGroupDisplayBeans);
        }
        ColAndGroupDisplayBean cgDisplayBeanTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
            {
                if(alrdbean.getLstRowgroupColsColumn().contains(cbean.getColumn())
                        &&!cbean.getColumn().equals(alrdbean.getLstRowgroupColsColumn().get(0)))
                {//如果当前cbean是树形行分组的列，但不是第一列，则不显示为一独立列，所以这里就不为它显示一个<td/>
                    continue;
                }
            }
            cgDisplayBeanTmp=null;
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            int displaymodeTmp=cdb.getColDisplayModeAfterAuthorize(cbean);
            if(displaymodeTmp<0||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&displaymodeTmp==0)) continue;//授权为不显示，或者当前是显示在数据导出文件中，且被列选择为不显示
            alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(lstColAndGroupDisplayBeans!=null)
            {
                cgDisplayBeanTmp=new ColAndGroupDisplayBean();
                cgDisplayBeanTmp.setId(cbean.getColid());
                cgDisplayBeanTmp.setControlCol(cbean.isControlCol());
                cgDisplayBeanTmp.setNonFixedCol(alrcbean==null||!alrcbean.isFixedCol());
                if(displaymodeTmp==0)
                {
                    cgDisplayBeanTmp.setChecked(false);
                }else
                {
                    cgDisplayBeanTmp.setChecked(true);
                    cgDisplayBeanTmp.setAlways(Consts.COL_DISPLAYTYPE_ALWAYS.equals(cbean.getDisplaytype()));
                }
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
            }
            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,cbean.getLabel());
            String label=ReportAssistant.getInstance().getColGroupLabel(rrequest,cbean.getLabel(),coldataByInterceptor);
            if(cgDisplayBeanTmp!=null)
            {
                cgDisplayBeanTmp.setTitle(label);
                if(displaymodeTmp==0) continue;
            }
            resultBuf.append("<td class='"+getDataHeaderThClassName()+"' ");
            resultBuf.append(getColGroupStyleproperty(cbean.getLabelstyleproperty(rrequest),coldataByInterceptor));
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                if(rbean.getCelldrag()>0&&(alrcbean==null||alrcbean.isDragable(alrdbean)))
                {//如果当前报表需要列拖动，且当前单元格不参与普通行分组和树形分组，且不是冻结列
                    resultBuf.append(" onmousedown=\"try{handleCellDragMouseDown(this,'"+rbean.getPageBean().getId()+"','"+rbean.getId()
                            +"');}catch(e){logErrorsAsJsFileLoad(e);}\"");
                    resultBuf.append(" dragcolid=\"").append(cbean.getColid()).append("\"");
                }
                if(this.fixedDataBean!=null) resultBuf.append(this.fixedDataBean.showFirstNonFixedColFlag(cbean));
                resultBuf.append(">");
                if(rbean.getCellresize()==1&&!cbean.getColid().equals(cdb.getLastColId()))
                {
                    resultBuf.append(ListReportAssistant.getInstance().appendCellResizeFunction(true));
                }else if(rbean.getCellresize()>0)
                {
                    resultBuf.append(ListReportAssistant.getInstance().appendCellResizeFunction(false));
                }
                if(dbean.isColselect()&&cbean.getColid().equals(cdb.getLastColId()))
                {
                    resultBuf.append(ReportAssistant.getInstance().getColSelectedLabelAndEvent(rrequest,rbean,true));
                }
                if(alrcbean!=null)
                {
                    if(this.getLstReportData()!=null&&this.getLstReportData().size()>0)
                    {
                        if(alrcbean.isRequireClickOrderby())
                        {
                            label=ListReportAssistant.getInstance().getColLabelWithOrderBy(cbean,rrequest);
                        }
                    }
                    if(alrcbean.getFilterBean()!=null) resultBuf.append(ListReportAssistant.getInstance().createColumnFilter(rrequest,alrcbean));
                }
            }else
            {
                String dataheaderbgcolor=Config.getInstance().getSkinConfigValue(rrequest.getPageskin(),"table.dataheader.bgcolor");
                if(dataheaderbgcolor==null) dataheaderbgcolor="";
                resultBuf.append(" bgcolor='"+dataheaderbgcolor+"'>");
            }
            resultBuf.append(label+"</td>");
        }
        resultBuf.append("</tr>");
        if(this.fixedDataBean!=null&&this.fixedDataBean.getFixedrowscount()==Integer.MAX_VALUE)
        {
            this.fixedDataBean.setFixedrowscount(1);//显示了标题行，则设置冻结行数为1
        }
        return resultBuf.toString();
    }

    protected String getDataHeaderTrClassName()
    {
        if(Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder()))
        {
            return "cls-data-tr-head-articlelist";
        }
        return "cls-data-tr-head-list";
    }

    protected String getDataHeaderThClassName()
    {
        if(Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder()))
        {
            return "cls-data-th-articlelist";
        }
        return "cls-data-th-list";
    }

    protected String getDataTdClassName()
    {
        if(Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder()))
        {
            return "cls-data-td-articlelist";
        }
        return "cls-data-td-list";
    }

    protected String showDataPart()
    {
        StringBuffer resultBuf=new StringBuffer();
        List<ColBean> lstColBeans=getLstDisplayColBeans();
        if(getDisplayRowInfo()[1]<=0)
        {
            String trstylepropertyTmp=null;
            if(this.rbean.getInterceptor()!=null)
            {
                RowDataByInterceptor rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,0,this.cacheDataBean.getTotalColCount(),lstColBeans);
                if(rowdataObjTmp!=null)
                {
                    if(rowdataObjTmp.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                    trstylepropertyTmp=rowdataObjTmp.getDynTrStyleproperty();
                    if(!rowdataObjTmp.isShouldDisplayThisRow()) return resultBuf.toString();
                }
            }
            if(trstylepropertyTmp==null) trstylepropertyTmp="";
            resultBuf.append("<tr id=\""+rbean.getGuid()+"_nodata_tr\" "+trstylepropertyTmp+"><td class='"+getDataTdClassName()+"' bgcolor='#ffffff' colspan='").append(
                    this.cacheDataBean.getTotalColCount()).append("'>");
            if(this.isLazyDataLoad()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                resultBuf.append(rrequest.getStringAttribute(rbean.getId()+"_lazyloadmessage",""));
            }else
            {
                resultBuf.append(rrequest.getI18NStringValue((Config.getInstance().getResources().getString(rrequest,rbean.getPageBean(),
                        Consts.NODATA_PROMPT_KEY,true))));
            }
            resultBuf.append("</td></tr>");
            return resultBuf.toString();
        }
        int startNum=0;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            startNum=(this.cacheDataBean.getFinalPageno()-1)*this.cacheDataBean.getPagesize();
        }
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT
                ||!this.cacheDataBean.shouldBatchDataExport())
        {
            showRealDataPart(resultBuf,lstColBeans,startNum);
        }else
        {
            for(int i=0;i<this.cacheDataBean.getPagecount();i++)
            {
                if(i!=0)
                {
                    this.cacheDataBean.setPageno(i+1);
                    this.cacheDataBean.setRefreshNavigateInfoType(1);//不需要再计算页码之类
                    this.setHasLoadedDataFlag(false);
                    loadReportData();
                }
                showRealDataPart(resultBuf,lstColBeans,startNum);
                startNum+=this.cacheDataBean.getPagesize();
            }
        }
        resultBuf.append(showStatisticDataForWholeReport());
        return resultBuf.toString();
    }

    private void showRealDataPart(StringBuffer resultBuf,List<ColBean> lstColBeans,int startNum)
    {
        if(alrdbean!=null&&alrdbean.getRowGroupColsNum()>0)
        {
            if(alrdbean.getRowgrouptype()==1)
            {
                resultBuf.append(showCommonRowGroupDataPart(lstColBeans,startNum));
            }else if(alrdbean.getRowgrouptype()==2)
            {
                resultBuf.append(showTreeRowGroupDataPart(lstColBeans,startNum));
            }else
            {
                throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，无效的分组类型");
            }
        }else
        {
            resultBuf.append(showCommonDataPart(lstColBeans,startNum));
        }
    }

    private String showCommonDataPart(List<ColBean> lstColBeans,int startNum)
    {
        StringBuffer resultBuf=new StringBuffer();
        StringBuffer tdPropsBuf;
        ColDataByInterceptor coldataByInterceptor;//从拦截器中得到的用户为当前列生成的数据
        String col_displayvalue;
        Object rowDataObjTmp;
        int[] displayrowinfo=this.getDisplayRowInfo();
        if(displayrowinfo[0]>0) startNum+=displayrowinfo[0];
        RowDataByInterceptor rowdataObjTmp;
        String trstylepropertyTmp=null;
        for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
        {
            if(i>=this.lstReportData.size())
            {
                resultBuf.append(showDataRowInAddMode(lstColBeans,startNum++,i));
                continue;
            }
            trstylepropertyTmp=null;
            if(this.rbean.getInterceptor()!=null)
            {
                rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,i,this.cacheDataBean.getTotalColCount(),lstColBeans);
                if(rowdataObjTmp!=null)
                {
                    if(rowdataObjTmp.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                    trstylepropertyTmp=rowdataObjTmp.getDynTrStyleproperty();
                    if(!rowdataObjTmp.isShouldDisplayThisRow()) continue;
                }
            }
            if(trstylepropertyTmp==null) trstylepropertyTmp="";
            resultBuf.append(showDataRowTrStart(i)).append(" ").append(trstylepropertyTmp).append(">");
            rowDataObjTmp=this.lstReportData.get(i);
            for(ColBean cbean:lstColBeans)
            {
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())||this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0)
                {
                    resultBuf.append(showHiddenCol(this.cacheDataBean,cbean,rowDataObjTmp,i,startNum));
                    continue;
                }
                
                tdPropsBuf=new StringBuffer();
                Object colDataObj=initDisplayCol(this.cacheDataBean,cbean,rowDataObjTmp,startNum);
                resultBuf.append("<td ").append(getTdPropertiesForCol(this.cacheDataBean,cbean,colDataObj,i,false));
                col_displayvalue=getColDisplayValue(cbean,rowDataObjTmp,tdPropsBuf,colDataObj,startNum,i);//得到此列的显示数据
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,i,col_displayvalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    col_displayvalue=coldataByInterceptor.getDynvalue();
                }
                resultBuf.append(" class='"+getDataTdClassName()+"' ");
                resultBuf.append(tdPropsBuf.toString()).append(" ");
                resultBuf.append(getColGroupStyleproperty(cbean.getValuestyleproperty(rrequest),coldataByInterceptor));
                resultBuf.append(">").append(getColDisplayValueWithWrap(cbean,col_displayvalue));
                resultBuf.append("</td>");
            }
            resultBuf.append("</tr>");
            startNum++;
        }
        return resultBuf.toString();
    }

    private String showCommonRowGroupDataPart(List<ColBean> lstColBeans,int startNum)
    {
        StringBuffer resultBuf=new StringBuffer();
        Map<Integer,List<RowGroupDataBean>> mAllParentDataBeansForPerDataRow=new HashMap<Integer,List<RowGroupDataBean>>();
        Map<ColBean,List<RowGroupDataBean>> mRowGroupCols=parseRowGroupColData(mAllParentDataBeansForPerDataRow);
        List<RowGroupDataBean> lstHasDisplayedRowGroupCols=null;
        Map<String,AbsListReportStatiRowGroupBean> mStatiRowGroupBeans=null;
        if(alrdbean.getStatibean()!=null&&alrdbean.getStatibean().getMStatiRowGroupBeans()!=null
                &&alrdbean.getStatibean().getMStatiRowGroupBeans().size()>0)
        {
            lstHasDisplayedRowGroupCols=new ArrayList<RowGroupDataBean>();
            mStatiRowGroupBeans=alrdbean.getStatibean().getMStatiRowGroupBeans();
        }
        ColDataByInterceptor coldataByInterceptor;
        RowGroupDataBean rgdbean;
        StringBuffer tdPropsBuf;
        Object rowDataObjTmp;
        int[] displayrowinfo=this.getDisplayRowInfo();
        if(displayrowinfo[0]>0) startNum+=displayrowinfo[0];
        if(displayrowinfo[1]<=0) return "";
        RowDataByInterceptor rowdataObjTmp;
        String trstylepropertyTmp=null;
        for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
        {
            if(i>=this.lstReportData.size())
            {
                resultBuf.append(showDataRowInAddMode(lstColBeans,startNum++,i));
                continue;
            }
            trstylepropertyTmp=null;
            if(this.rbean.getInterceptor()!=null)
            {
                rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,i,this.cacheDataBean.getTotalColCount(),lstColBeans);
                if(rowdataObjTmp!=null)
                {
                    if(rowdataObjTmp.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                    trstylepropertyTmp=rowdataObjTmp.getDynTrStyleproperty();
                    if(!rowdataObjTmp.isShouldDisplayThisRow()) continue;
                }
            }
            if(trstylepropertyTmp==null) trstylepropertyTmp="";
            resultBuf.append(showDataRowTrStart(i)).append(" ").append(trstylepropertyTmp);
            resultBuf.append(" parentCommonGroupTdId=\"").append(getDirectParentGroupId(mAllParentDataBeansForPerDataRow.get(i))).append("\"");
            resultBuf.append(" grouprow=\"true\"");//在<tr/>中加上grouprow=true，这样如果有选中功能，则不将整个<tr/>改变背景色，那些分组列的背景色不能变，只变不参与行分组的列
            resultBuf.append(">");
            rowDataObjTmp=lstReportData.get(i);
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                for(RowGroupDataBean parentObjTmp:mAllParentDataBeansForPerDataRow.get(i))
                {//为当前记录行对应的所有父分组节点，根据需要显示一个隐藏<td/>存放它的值，以便能用上
                    if(parentObjTmp.getDisplay_rowidx()!=i)
                    {//对于父分组对象，如果与当前数据行不在同一个<tr/>中，则根据需要为它显示一个隐藏的<td/>存放可能要用到的数据
                        resultBuf.append(showHiddenCol(this.cacheDataBean,parentObjTmp.getCbean(),rowDataObjTmp,i,startNum));
                    }
                }
            }
            for(ColBean cbean:lstColBeans)
            {
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())||this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0)
                {
                    if(mRowGroupCols.containsKey(cbean)&&this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<0)
                    {//当前列参与了行分组，且被授为不显示权限
                        throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，不能将参与行分组的列授权为不显示");
                    }
                    resultBuf.append(showHiddenCol(this.cacheDataBean,cbean,rowDataObjTmp,i,startNum));
                    continue;
                }
                int rowspan=1;
                boolean isRowGroup=false;
                tdPropsBuf=new StringBuffer();
                if(mRowGroupCols.containsKey(cbean))
                {
                    rgdbean=getCommonRowGroupDataBean(mRowGroupCols.get(cbean),mStatiRowGroupBeans,lstHasDisplayedRowGroupCols,cbean,i);
                    if(rgdbean==null) continue;
                    isRowGroup=true;
                    rowspan=rgdbean.getRowspan();
                    String childIdSuffix=rgdbean.getAllChildDataRowIdxsAsString();
                    if(!childIdSuffix.equals(""))
                    {
                        tdPropsBuf.append(" childDataIdSuffixes=\"").append(childIdSuffix).append("\"");
                    }
                    childIdSuffix=rgdbean.getAllChildGroupIdxsAsString();
                    if(!childIdSuffix.equals(""))
                    {//子分组节点集点，在列表表单中如果分组节点中有联动下拉框，更新分组节点上的下拉框数据后要更新子分组或子数据上的从下拉框数据时需用上此属性
                        tdPropsBuf.append(" childGroupIdSuffixes=\"").append(childIdSuffix).append("\"");
                    }
                    if(rgdbean.getParentGroupIdSuffix()!=null&&!rgdbean.getParentGroupIdSuffix().trim().equals(""))
                    {
                        tdPropsBuf.append(" parentCommonGroupTdId=\"").append(rgdbean.getParentGroupIdSuffix()).append("\"");//记下当前分组列的父分组列所在<td/>的id
                    }
                }
                Object colDataObj=initDisplayCol(this.cacheDataBean,cbean,rowDataObjTmp,startNum);
                resultBuf.append("<td ").append(getTdPropertiesForCol(this.cacheDataBean,cbean,colDataObj,i,isRowGroup));//在<td/>中附加上编辑时要用到的属性;
                String col_displayvalue=getColDisplayValue(cbean,rowDataObjTmp,tdPropsBuf,colDataObj,startNum,i);
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,i,col_displayvalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    col_displayvalue=coldataByInterceptor.getDynvalue();
                }
                resultBuf.append(" class='"+getDataTdClassName()+"' rowspan=\"").append(rowspan).append("\" ");
                resultBuf.append(tdPropsBuf.toString());
                resultBuf.append(" ").append(getColGroupStyleproperty(cbean.getValuestyleproperty(rrequest),coldataByInterceptor));
                if(isRowGroup)
                {
                    resultBuf.append(" groupcol=\"true\"");
                    if(alrbean.getLstRoworderTypes()!=null&&alrbean.getLstRoworderTypes().contains(Consts.ROWORDER_DRAG))
                    {
                        resultBuf.append(" onmouseover=\"dragrow_enabled=false;\" onmouseout=\"dragrow_enabled=true;\"");
                    }
                }
                resultBuf.append(">").append(getColDisplayValueWithWrap(cbean,col_displayvalue));
                resultBuf.append("</td>");
            }
            resultBuf.append("</tr>");
            startNum++;
            resultBuf.append(showStatisticForCommonRowGroup(this.cacheDataBean,lstHasDisplayedRowGroupCols,i));
        }
        return resultBuf.toString();
    }

    private RowGroupDataBean getCommonRowGroupDataBean(List<RowGroupDataBean> lstRgdbeans,
            Map<String,AbsListReportStatiRowGroupBean> mStatiRowGroupBeans,List<RowGroupDataBean> lstHasDisplayedRowGroupCols,ColBean cbean,
            int rowindex)
    {
        if(lstRgdbeans==null||lstRgdbeans.size()==0) return null;
        RowGroupDataBean rgdbean=lstRgdbeans.get(0);
        if(rgdbean==null) return null;
        if(rgdbean.getDisplay_rowidx()!=rowindex) return null;
        lstRgdbeans.remove(0);//显示完后，将它删除，这样后面的又变成第一个
        if(lstHasDisplayedRowGroupCols!=null&&mStatiRowGroupBeans.containsKey(cbean.getColumn()))
        {
            lstHasDisplayedRowGroupCols.add(rgdbean);
        }
        return rgdbean;
    }

    private String showStatisticForCommonRowGroup(CacheDataBean cdb,List<RowGroupDataBean> lstHasDisplayedRowGroupCols,int rowindex)
    {
        if(lstHasDisplayedRowGroupCols==null) return "";
        StringBuffer resultBuf=new StringBuffer();
        RowGroupDataBean rgdbTmp;
        for(int j=lstHasDisplayedRowGroupCols.size()-1;j>=0;j--)
        {
            rgdbTmp=lstHasDisplayedRowGroupCols.get(j);
            if(((CommonRowGroupDataBean)rgdbTmp).getDisplaystatidata_rowidx()!=rowindex) continue;
            resultBuf.append("<tr  class='cls-data-tr' >");
            resultBuf.append(showRowGroupStatiData(cdb,rgdbTmp,rgdbTmp.getLayer()+1));
            resultBuf.append("</tr>");
            lstHasDisplayedRowGroupCols.remove(j);
        }
        return resultBuf.toString();
    }

    private String showTreeRowGroupDataPart(List<ColBean> lstColBeans,int startNum)
    {
        StringBuffer resultBuf=new StringBuffer();
        Map<Integer,List<RowGroupDataBean>> mParentDataBeansForPerDataRow=new HashMap<Integer,List<RowGroupDataBean>>();
        Map<ColBean,List<RowGroupDataBean>> mTreeRowGroupCols=parseRowGroupColData(mParentDataBeansForPerDataRow);
        String trgroupid=rbean.getGuid()+"_trgroup_";
        List<RowGroupDataBean> lstTreeGroupDataBeans;
        TreeRowGroupDataBean trgdbean;
        ColDataByInterceptor coldataByInterceptor;
        StringBuffer tdPropsBuf;
        Object rowDataObjTmp=null;
        int[] displayrowinfo=this.getDisplayRowInfo();
        if(displayrowinfo[0]>0) startNum+=displayrowinfo[0];
        if(displayrowinfo[1]<=0) return "";
        for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
        {
            if(i>=this.lstReportData.size())
            {//当前行是显示添加行
                resultBuf.append(showDataRowInAddMode(lstColBeans,startNum++,i));
                continue;
            }
            rowDataObjTmp=lstReportData.get(i);
            ColBean cbeanTemp;
            RowDataByInterceptor rowdataObjTmp;
            String trstylepropertyTmp=null;
            for(String colcolumn:alrdbean.getLstRowgroupColsColumn())
            {
                if(colcolumn==null) continue;
                cbeanTemp=rbean.getDbean().getColBeanByColColumn(colcolumn);
                if(this.cacheDataBean.getColDisplayModeAfterAuthorize(cbeanTemp)<0)
                {
                    throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，不能将参与树形分组的列授权为不显示");
                }
                lstTreeGroupDataBeans=mTreeRowGroupCols.get(cbeanTemp);
                trgdbean=getTreeRowGroupDataBean(lstTreeGroupDataBeans,i);
                if(trgdbean==null) continue;
                resultBuf.append(showTreeRowGroupTrStart(trgroupid+trgdbean.getLayer()+"_"+i,trgdbean));
                resultBuf.append("<td class='cls-data-td-list' ");
                Object colDataObj=initDisplayCol(this.cacheDataBean,cbeanTemp,rowDataObjTmp,startNum);
                tdPropsBuf=new StringBuffer();//对于树形分组节点，tdPropsBuf的属性不是显示在上面外层<td/>中，而是显示在最里层的<td/>中
                tdPropsBuf.append(getTdPropertiesForCol(this.cacheDataBean,cbeanTemp,colDataObj,i,false));//这个不能放在resultBuf，而应该放在tdPropsBuf中，因为稍后要放入显示内容的<td/>中
                String col_displayvalue=getColDisplayValue(cbeanTemp,rowDataObjTmp,tdPropsBuf,colDataObj,startNum,i);
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbeanTemp,i,col_displayvalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    col_displayvalue=coldataByInterceptor.getDynvalue();
                }
                resultBuf.append(" ").append(getTreeNodeTdValueStyleProperty(trgdbean,coldataByInterceptor,i,cbeanTemp)).append(">");
                String childIds=trgdbean.getAllChildDataRowIdxsAsString();//当前分组的所有数据子节点ID
                if(!childIds.equals(""))
                {
                    tdPropsBuf.append(" childDataIdSuffixes=\"").append(childIds).append("\"");//在这里放一份子数据结点id集合，以便更新数据时能判断到当前输入框所在<td/>是否是分组节点的td
                }
                resultBuf.append(showTreeNodeContent(trgroupid+trgdbean.getLayer()+"_"+i,trgdbean,getColDisplayValueWithWrap(cbeanTemp,
                        col_displayvalue),tdPropsBuf.toString()));
                resultBuf.append("</td>");
                resultBuf.append(showOtherTdInTreeGroupRow(trgdbean,i,cbeanTemp));
                resultBuf.append("</tr>");
            }
            
            trstylepropertyTmp=null;
            if(this.rbean.getInterceptor()!=null)
            {
                rowdataObjTmp=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,i,this.cacheDataBean.getTotalColCount(),lstColBeans);
                if(rowdataObjTmp!=null)
                {
                    if(rowdataObjTmp.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObjTmp.getInsertDisplayRowHtml());
                    trstylepropertyTmp=rowdataObjTmp.getDynTrStyleproperty();
                    if(!rowdataObjTmp.isShouldDisplayThisRow()) continue;
                }
            }
            if(trstylepropertyTmp==null) trstylepropertyTmp="";
            resultBuf.append(showTreeDataRowTrStart(mParentDataBeansForPerDataRow,i));
            resultBuf.append(" ").append(trstylepropertyTmp).append(">");
            resultBuf.append(showTreeNodeTdInDataTr(i));//每个具体数据记录行都要在最前面多显示一个<td/>，因为所有树枝节点只占据一个<td/>，所以在记录行也要显示一个内容为空的单元格。
            for(RowGroupDataBean parentObjTmp:mParentDataBeansForPerDataRow.get(i))
            {//为当前记录行对应的所有父分组节点，根据需要显示一个隐藏<td/>存放它的值，以便能用上
                resultBuf.append(showHiddenCol(this.cacheDataBean,parentObjTmp.getCbean(),rowDataObjTmp,i,startNum));
            }
            for(ColBean cbean:lstColBeans)
            {
                if(alrdbean.getLstRowgroupColsColumn().contains(cbean.getColumn())) continue;
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())||this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0)
                {//隐藏列
                    resultBuf.append(showHiddenCol(this.cacheDataBean,cbean,rowDataObjTmp,i,startNum));
                    continue;
                }
                Object colDataObj=initDisplayCol(this.cacheDataBean,cbean,rowDataObjTmp,startNum);
                tdPropsBuf=new StringBuffer();
                resultBuf.append("<td ").append(getTdPropertiesForCol(this.cacheDataBean,cbean,colDataObj,i,false));//在<td/>中附加上编辑时要用到的属性;
                String col_displayvalue=getColDisplayValue(cbean,rowDataObjTmp,tdPropsBuf,colDataObj,startNum,i);
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,i,col_displayvalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    col_displayvalue=coldataByInterceptor.getDynvalue();
                }
                resultBuf.append(" class='"+getDataTdClassName()+"' ");
                resultBuf.append(tdPropsBuf.toString()).append(" ");
                resultBuf.append(getTreeDataTdValueStyleProperty(cbean,coldataByInterceptor,i));
                resultBuf.append(">").append(getColDisplayValueWithWrap(cbean,col_displayvalue));
                resultBuf.append("</td>");
            }
            resultBuf.append("</tr>");
            startNum++;
        }
        return resultBuf.toString();
    }

    private TreeRowGroupDataBean getTreeRowGroupDataBean(List<RowGroupDataBean> lstTreeGroupDataBeans,int rowindex)
    {
        if(lstTreeGroupDataBeans==null||lstTreeGroupDataBeans.size()==0) return null;
        RowGroupDataBean rgdb=lstTreeGroupDataBeans.get(0);
        if(rgdb.getDisplay_rowidx()!=rowindex) return null;
        lstTreeGroupDataBeans.remove(0);
        return (TreeRowGroupDataBean)rgdb;
    }

    protected String showDataRowTrStart(int rowindex)
    {
        StringBuffer trBuf=new StringBuffer();
        trBuf.append("<tr  class='cls-data-tr' id=\""+rbean.getGuid()+"_tr_"+rowindex+"\"");
        AbsListReportBean alrbean=(AbsListReportBean)rbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrbean.getLstRoworderTypes()!=null&&alrbean.getLstRoworderTypes().contains(Consts.ROWORDER_DRAG)
                &&!rrequest.checkPermission(this.rbean.getId(),"roworder",Consts.ROWORDER_DRAG,Consts.PERMISSION_TYPE_DISABLED))
        {
            trBuf.append(" onmousedown=\"try{handleRowDragMouseDown(this,'"+this.rbean.getPageBean().getId()+"','"+this.rbean.getId()
                    +"');}catch(e){logErrorsAsJsFileLoad(e);}\" ");
        }
        
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            if(alrdbean.getMouseoverbgcolor()!=null&&!alrdbean.getMouseoverbgcolor().trim().equals(""))
            {//当前报表需要鼠标滑过时改变行背景色
                trBuf.append(" onmouseover=\"try{changeRowBgcolorOnMouseOver(this,'").append(alrdbean.getMouseoverbgcolor()).append(
                        "');}catch(e){}\"");
                trBuf.append(" onmouseout=\"try{resetRowBgcolorOnMouseOver(this);}catch(e){}\"");
            }
        }
        return trBuf.toString();
    }

    protected String getTdPropertiesForCol(CacheDataBean cdb,ColBean cbean,Object colDataObj,int rowidx,boolean isCommonRowGroup)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(this.fixedDataBean!=null)
        {
            resultBuf.append(this.fixedDataBean.showFirstNonFixedColFlag(cbean));
            if(alrcbean!=null&&alrcbean.isFixedCol())
            {
                resultBuf.append(" isFixedCol=\"true\"");
            }
        }
        if(cbean.isSequenceCol()||cbean.isControlCol()||alrcbean==null) return resultBuf.toString();
        if(alrcbean.shouldShowColNamePropertyInTd())
        {
            resultBuf.append(" name=\"").append(cbean.getProperty()).append("\"");
        }
        if(alrcbean.shouldShowColValuePropertyInTd())
        {
            if(colDataObj==null) colDataObj="";
            if(cdb.getColDisplayModeAfterAuthorize(cbean)<0)
            {
                throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"的列"+cbean.getColumn()+"失败，不能将其授权为不显示");
            }
            resultBuf.append(" value=\"").append(Tools.htmlEncode(colDataObj.toString())).append("\"");
        }
        if(alrcbean.getSlaveReportParamName()!=null&&!alrcbean.getSlaveReportParamName().trim().equals(""))
        {
            resultBuf.append(" slave_paramname=\"").append(alrcbean.getSlaveReportParamName()).append("\"");
        }
        return resultBuf.toString();
    }

    protected String showDataRowInAddMode(List<ColBean> lstColBeans,int startNum,int rowidx)
    {
        return "";
    }

    protected Object initDisplayCol(CacheDataBean cdb,ColBean cbean,Object rowDataObjTmp,int startNum)
    {
        if(cbean.isSequenceCol()||cbean.isControlCol()) return null;
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return null;
        return cbean.getDisplayValue(rowDataObjTmp,rrequest);
        
    }

    private String showHiddenCol(CacheDataBean cdb,ColBean cbean,Object dataObj,int rowidx,int startNum)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        Object colDataObj=initDisplayCol(cdb,cbean,dataObj,startNum);
        resultBuf.append("<td style='display:none' ");
        resultBuf.append(getTdPropertiesForCol(cdb,cbean,colDataObj,rowidx,false));
        resultBuf.append("></td>");
        return resultBuf.toString();
    }

    private String getTreeDataTdValueStyleProperty(ColBean cbean,ColDataByInterceptor coldataByInterceptor,int rowindex)
    {
        String valuestyleproperty=getColGroupStyleproperty(cbean.getValuestyleproperty(rrequest),coldataByInterceptor);
        String styletmp=null;
        if(alrdbean.getTreeborder()==0||alrdbean.getTreeborder()==1)
        {
            styletmp="border-top:none;";
            if(rowindex!=lstReportData.size()-1)
            {
                styletmp+="border-bottom:none;";
            }
        }
        if(styletmp!=null)
        {
            valuestyleproperty=Tools.mergeHtmlTagPropertyString(valuestyleproperty,"style=\""+styletmp+"\"",1);
        }
        return valuestyleproperty;
    }

    private String showTreeNodeTdInDataTr(int rowindex)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(alrdbean.getTreeborder()==3)
        {
            resultBuf.append("<td class='cls-data-td-list' groupcol='true'>&nbsp;</td>");
        }else
        {
            if(rowindex!=lstReportData.size()-1)
            {
                resultBuf.append("<td class='cls-data-td-list' style='border-top:none;border-bottom:none;' groupcol='true'>&nbsp;</td>");
            }else
            {
                resultBuf.append("<td class='cls-data-td-list' style='border-top:none;' groupcol='true'>&nbsp;</td>");
            }
        }
        return resultBuf.toString();
    }

    private String showTreeDataRowTrStart(Map<Integer,List<RowGroupDataBean>> mParentDataBeansForPerDataRow,int i)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showDataRowTrStart(i));
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            List<RowGroupDataBean> listAllParentsObj=mParentDataBeansForPerDataRow.get(i);
            if(this.alrdbean.getTreexpandlayer()>=0&&listAllParentsObj!=null&&listAllParentsObj.size()>0)
            {
                if(this.alrdbean.getTreexpandlayer()<=listAllParentsObj.get(listAllParentsObj.size()-1).getLayer())
                {//如果指定的展开层数小于等于本记录行父节点的层级数，则隐藏数据节点
                    resultBuf.append(" style='display:none;'");
                }
            }
            resultBuf.append(" parentTridSuffix=\"").append(getDirectParentGroupId(listAllParentsObj)).append("\"");
            resultBuf.append(" grouprow=\"true\"");
        }
        return resultBuf.toString();
    }

    private String showOtherTdInTreeGroupRow(TreeRowGroupDataBean trgdbean,int rowindex,ColBean cbeanTemp)
    {
        StringBuffer resultBuf=new StringBuffer();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        int columncount=cdb.getTotalColCount();
        if(trgdbean.getLstScolbeans()!=null&&trgdbean.getLstScolbeans().size()>0&&trgdbean.getStatiDataObj()!=null)
        {
            resultBuf.append(showRowGroupStatiData(cdb,trgdbean,1));
        }else
        {
            if(alrdbean.getTreeborder()==0)
            {
                String style="";
                if(rowindex!=0||trgdbean.getLayer()!=0)
                {
                    style=style+"border-top:none;";
                }
                if(rowindex!=lstReportData.size()-1||trgdbean.getLayer()!=alrdbean.getRowGroupColsNum()-1)
                {
                    style=style+"border-bottom:none;";
                }
                if(!style.equals(""))
                {
                    style=Tools.mergeHtmlTagPropertyString(cbeanTemp.getValuestyleproperty(rrequest),"style=\""+style+"\"",1);
                }
                for(int m=0;m<columncount-1;m++)
                {//因为要确保树枝节点的行也显示竖线边框，因此不能用colspan=columncount-1将td占满剩余的行，否则这些行不会为没个单元格显示竖线边框，必须为它们各自显示一个<td/>
                    resultBuf.append("<td class='cls-data-td-list' ").append(style).append(">&nbsp;</td>");
                }
            }else
            {
                resultBuf.append("<td class='cls-data-td-list' colspan='").append(columncount-1).append("'>&nbsp;</td>");
            }
        }
        return resultBuf.toString();
    }

    private String showTreeNodeContent(String trgroupid,TreeRowGroupDataBean trgdbean,String displayvalue,String displayvalue_tdproperty)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            
            resultBuf.append("<table border='0' cellpadding='0' cellspacing='0' width='100%' style='margin:0pt;padding: 0pt;'><tr><td");
            resultBuf.append(" width='").append(trgdbean.getLayer()*12).append("px'>");
            resultBuf.append("</td>");
        }else
        {//excel中不能用表格
            String nodeblank="&nbsp;&nbsp;&nbsp;&nbsp;";
            for(int k=0;k<trgdbean.getLayer();k++)
            {
                resultBuf.append(nodeblank);
            }
        }
        if(alrdbean.isTreecloseable()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            resultBuf.append("<td align='right' width='12px'>");
            resultBuf.append("<img id=\"").append(trgroupid).append("_img\"");
            resultBuf.append(" src=\"").append(Config.webroot).append("webresources/skin/"+rrequest.getPageskin()+"/images/");
            if(this.alrdbean.getTreexpandlayer()>=0&&this.alrdbean.getTreexpandlayer()<=trgdbean.getLayer())
            {
                resultBuf.append("nodeclosed.gif");
            }else
            {
                resultBuf.append("nodeopen.gif");
            }
            resultBuf.append("\"");
            String tridPrex=trgroupid.substring(0,trgroupid.lastIndexOf("trgroup_"));
            String scrollid="";
            if(alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL&&Consts_Private.SCROLLSTYLE_IMAGE.equals(rbean.getScrollstyle()))
            {
                scrollid="vscroll_"+rbean.getGuid();
            }
            resultBuf.append(" onmouseover=\"this.style.cursor='pointer'\" onclick=\"try{expandOrCloseTreeNode('").append(Config.webroot).append(
                    "','"+rrequest.getPageskin()+"',this,'");
            resultBuf.append(tridPrex).append("','"+scrollid+"');}catch(e){logErrorsAsJsFileLoad(e);}\"");
            resultBuf.append("/>");
            resultBuf.append("</td>");
        }
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            if(Consts.ROWSELECT_CHECKBOX.equalsIgnoreCase(cdb.getRowSelectType()))
            {//如果是复选框选择类型，则在树枝节点上提供复选框
                resultBuf
                        .append("<td width='1px' nowrap><input type=\"checkbox\" onclick=\"try{doSelectedDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                                +rbean.getGuid()+"_rowselectbox_col\" rowgroup=\"true\"></td>");
            }
            resultBuf.append("<td align='left'");
            if(displayvalue_tdproperty!=null&&!displayvalue_tdproperty.trim().equals(""))
            {
                resultBuf.append(" ").append(displayvalue_tdproperty);
            }
            resultBuf.append(">");
            if(displayvalue==null||displayvalue.trim().equals(""))
            {
                displayvalue="&nbsp;";
            }
        }
        resultBuf.append(displayvalue);
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            resultBuf.append("</td></tr></table>");
        }
        return resultBuf.toString();
    }

    private String getTreeNodeTdValueStyleProperty(TreeRowGroupDataBean trgdbean,ColDataByInterceptor coldataByInterceptor,int rowindex,
            ColBean cbeanTemp)
    {
        String valuestyleproperty=getColGroupStyleproperty(cbeanTemp.getValuestyleproperty(rrequest),coldataByInterceptor);
        if(alrdbean.getTreeborder()!=3)
        {
            String styletmp="border-bottom:none;";
            if(rowindex!=0||trgdbean.getLayer()!=0)
            {
                styletmp+="border-top:none;";
            }
            valuestyleproperty=Tools.mergeHtmlTagPropertyString(valuestyleproperty,"style=\""+styletmp+"\"",1);
        }
        return valuestyleproperty;
    }

    private String showTreeRowGroupTrStart(String trid,TreeRowGroupDataBean trgdbean)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<tr  class='cls-data-tr' id=\""+trid+"\"");
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            
            String childIds=trgdbean.getAllChildGroupIdxsAsString();
            if(!childIds.equals(""))
            {
                resultBuf.append(" childGroupIdSuffixes=\"").append(childIds).append("\"");
            }
            childIds=trgdbean.getAllChildDataRowIdxsAsString();
            if(!childIds.equals(""))
            {
                resultBuf.append(" childDataIdSuffixes=\"").append(childIds).append("\"");
            }
            if(trgdbean.getParentGroupIdSuffix()!=null&&!trgdbean.getParentGroupIdSuffix().trim().equals(""))
            {//如果有父分组节点，则记下父分组节点的ID后缀，以便后面可以找到父分组对象
                resultBuf.append(" parentTridSuffix=\"").append(trgdbean.getParentGroupIdSuffix()).append("\"");
            }
            if(this.alrdbean.getTreexpandlayer()>=0&&trgdbean.getLayer()>=this.alrdbean.getTreexpandlayer())
            {
                resultBuf.append(" state=\"close\"");
                if(trgdbean.getLayer()>this.alrdbean.getTreexpandlayer())
                {
                    resultBuf.append(" style=\"display:none;\"");
                }
            }else
            {
                resultBuf.append(" state=\"open\"");
            }
        }
        resultBuf.append(">");
        return resultBuf.toString();
    }

    private String getDirectParentGroupId(List<RowGroupDataBean> listAllParentsObj)
    {
        if(listAllParentsObj==null||listAllParentsObj.size()==0) return "";
        return listAllParentsObj.get(listAllParentsObj.size()-1).getIdSuffix();
    }

    private String showRowGroupStatiData(CacheDataBean cdb,RowGroupDataBean rgdbean,int startcolspan)
    {
        StringBuffer resultBuf=new StringBuffer();
        String stativalue;
        ColDataByInterceptor coldataByInterceptor;
        List<AbsListReportStatiColBean> lstStatiColBeans=rgdbean.getLstScolbeans();
        Object statiDataObj=rgdbean.getStatiDataObj();
        if(statiDataObj==null||lstStatiColBeans==null) return "";
        if(rbean.getDbean().isColselect()
                ||lstStatiColBeans.size()==1
                ||(cdb.getAttributes().get("authroize_col_display")!=null&&String.valueOf(cdb.getAttributes().get("authroize_col_display")).trim()
                        .equals("false"))||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&alrbean.hasControllCol()))
        {
            resultBuf.append("<td class='cls-data-td-list' ");
            int colspan=cdb.getTotalColCount()-startcolspan;//取到当前参与显示的总列数-当前统计信息显示的起始列数
            if(colspan<=0) return "";
            resultBuf.append(" colspan='").append(colspan).append("' ");
            StringBuffer statiContentBuf=new StringBuffer();
            String dyntdstyleproperty=null;
            for(AbsListReportStatiColBean scbean:lstStatiColBeans)
            {
                if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,scbean.getProperty(),Consts.PERMISSION_TYPE_DISPLAY)) continue;
                stativalue=getStatiColDisplayValue(statiDataObj,scbean);
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,scbean,0,stativalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    stativalue=coldataByInterceptor.getDynvalue();
                }
                statiContentBuf.append(stativalue).append("&nbsp;&nbsp;");
                if(dyntdstyleproperty==null)
                    dyntdstyleproperty=Tools.removePropertyValueByName("colspan",getColGroupStyleproperty(scbean.getValuestyleproperty(),
                            coldataByInterceptor));
            }
            stativalue=statiContentBuf.toString().trim();
            if(stativalue.endsWith("&nbsp;&nbsp;")) stativalue=stativalue.substring(0,stativalue.length()-"&nbsp;&nbsp;".length()).trim();
            //            if(stativalue.equals("")) return "";//当前没有要显示的统计项
            if(dyntdstyleproperty!=null) resultBuf.append(dyntdstyleproperty);
            resultBuf.append(">").append(stativalue).append("</td>");
        }else
        {
            for(AbsListReportStatiColBean scbean:lstStatiColBeans)
            {
                stativalue=getStatiColDisplayValue(statiDataObj,scbean);
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,scbean,0,stativalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    stativalue=coldataByInterceptor.getDynvalue();
                }
                resultBuf.append("<td class='cls-data-td-list' ");
                resultBuf.append(getColGroupStyleproperty(scbean.getValuestyleproperty(),coldataByInterceptor));
                resultBuf.append(">").append(stativalue).append("</td>");
            }
        }
        return resultBuf.toString();
    }

    public String getColOriginalValue(Object object,ColBean cbean)
    {
        return cbean.getDisplayValue(object,rrequest);
    }

    protected String getColDisplayValue(ColBean cbean,Object dataObj,StringBuffer tdPropBuf,Object colDataObj,int startNum,int rowidx)
    {
        String col_displayvalue="";
        if(cbean.isRowSelectCol())
        {
            AbsListReportBean alrbean=(AbsListReportBean)rbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(Consts.ROWSELECT_CHECKBOX.equalsIgnoreCase(alrbean.getRowSelectType()))
            {
                col_displayvalue="<input type=\"checkbox\" onclick=\"try{doSelectedDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                        +rbean.getGuid()+"_rowselectbox_col\">";
            }else if(Consts.ROWSELECT_RADIOBOX.equalsIgnoreCase(alrbean.getRowSelectType()))
            {
                col_displayvalue="<input type=\"radio\" onclick=\"try{doSelectedDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                        +rbean.getGuid()+"_rowselectbox_col\">";
            }else
            {
                throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，此报表的行选中类型不是"+Consts.ROWSELECT_CHECKBOX+"或"+Consts.ROWSELECT_RADIOBOX
                        +"类型，不能配置"+Consts_Private.COL_ROWSELECT+"类型的列");
            }
        }else if(cbean.isRoworderCol())
        {//是行排序列
            if(cbean.isRoworderArrowCol())
            {
                String arrowup=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,this.rbean.getPageBean(),
                        "${roworder.arrow.up}",true));
                String arrowdown=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,this.rbean.getPageBean(),
                        "${roworder.arrow.down}",true));
                boolean hasNoPermission=rrequest.checkPermission(this.rbean.getId(),Consts.DATA_PART,Consts_Private.COL_ROWORDER_ARROW,
                        Consts.PERMISSION_TYPE_DISABLED)
                        ||rrequest.checkPermission(this.rbean.getId(),"roworder",Consts.ROWORDER_ARROW,Consts.PERMISSION_TYPE_DISABLED);
                if(hasNoPermission)
                {
                    col_displayvalue="<span class='cls-roworder-disabled'>"+arrowup+"</span>";
                }else
                {
                    col_displayvalue="<a onClick=\"changeListReportRoworderByArrow('"+this.rbean.getPageBean().getId()+"','"+this.rbean.getId()
                            +"',this,true)\">";
                    col_displayvalue=col_displayvalue+arrowup+"</a>";
                }
                col_displayvalue+="&nbsp;";
                if(hasNoPermission)
                {
                    col_displayvalue=col_displayvalue+"<span class='cls-roworder-disabled'>"+arrowdown+"</span>";
                }else
                {
                    col_displayvalue=col_displayvalue+"<a onClick=\"changeListReportRoworderByArrow('"+this.rbean.getPageBean().getId()+"','"
                            +this.rbean.getId()+"',this,false)\">";
                    col_displayvalue=col_displayvalue+arrowdown+"</a>";
                }
            }else if(cbean.isRoworderInputboxCol())
            {
                Map<String,String> mRoworderColValuesInRow=getRoworderColvaluesInRow(dataObj);
                String oldordervalue="";
                if(alrbean.getLoadStoreRoworderObject()!=null)
                {
                    oldordervalue=alrbean.getLoadStoreRoworderObject().loadRoworder(mRoworderColValuesInRow);
                }else
                {
                    oldordervalue=Config.default_roworder_object.loadRoworder(mRoworderColValuesInRow);
                }
                if(Tools.isDefineKey("@",oldordervalue))
                {//是取某列的值
                    ColBean cbTmp=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",oldordervalue));
                    if(cbTmp==null)
                    {
                        throw new WabacusRuntimeException("获取报表"+rbean.getPath()+"的行排序数据失败，不存在property为"+Tools.getRealKeyByDefine("@",oldordervalue)
                                +"的<col/>");
                    }
                    oldordervalue=this.getColOriginalValue(dataObj,cbTmp);
                }
                oldordervalue=oldordervalue==null?"":oldordervalue.trim();
                AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                String inputboxstyleproperty=" onblur=\"changeListReportRoworderByInputbox('"+this.rbean.getPageBean().getId()+"','"
                        +this.rbean.getId()+"',this,'"+oldordervalue+"')\" ";
                if(alrcbean!=null&&alrcbean.getRoworder_inputboxstyleproperty()!=null)
                {
                    inputboxstyleproperty=inputboxstyleproperty+" "+alrcbean.getRoworder_inputboxstyleproperty();
                }
                AbsInputBox box=Config.getInstance().getInputBoxByType(TextBox.class);
                col_displayvalue=box.getIndependentDisplayString(rrequest,oldordervalue,inputboxstyleproperty,null,rrequest.checkPermission(rbean
                        .getId(),Consts.DATA_PART,Consts_Private.COL_ROWORDER_INPUTBOX,Consts.PERMISSION_TYPE_DISABLED)
                        ||rrequest.checkPermission(rbean.getId(),"roworder",Consts.ROWORDER_INPUTBOX,Consts.PERMISSION_TYPE_DISABLED));
            }else if(cbean.isRoworderTopCol())
            {
                String topstr=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,this.rbean.getPageBean(),"${roworder.top}",
                        true));
                if(rrequest.checkPermission(this.rbean.getId(),Consts.DATA_PART,Consts_Private.COL_ROWORDER_TOP,Consts.PERMISSION_TYPE_DISABLED)
                        ||rrequest.checkPermission(this.rbean.getId(),"roworder",Consts.ROWORDER_TOP,Consts.PERMISSION_TYPE_DISABLED))
                {
                    col_displayvalue="<span class='cls-roworder-disabled'>"+topstr+"</span>";
                }else
                {
                    col_displayvalue="<a onMouseOver=\"this.style.cursor='pointer'\" onClick=\"changeListReportRoworderByTop('"
                            +this.rbean.getPageBean().getId()+"','"+this.rbean.getId()+"',this)\">";
                    col_displayvalue=col_displayvalue+topstr+"</a>";
                }
            }
        }else if(cbean.isSequenceCol())
        {
            AbsListReportColBean absListColbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            col_displayvalue=String.valueOf(startNum+absListColbean.getSequenceStartNum());
        }else
        {
            col_displayvalue=cbean.getDisplayValue(dataObj,rrequest);
            if(col_displayvalue==null) col_displayvalue="";
        }
        return col_displayvalue;
    }

    /*private boolean isDisabledRoworderByUpArrowOrTop(int rowidx)
    {
        if(this.lstReportData==null||this.lstReportData.size()<2) return true;
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        if(cdb.getPagesize()<=0)
        {
            if(rowidx==0) return true;//当前是第一条记录
        }else
        {
            if(cdb.getFinalPageno()==1&&rowidx==0) return true;
        }
        return false;
    }
    
    private boolean isDisabledRoworderByDownArrow(int rowidx)
    {
        if(this.lstReportData==null||this.lstReportData.size()<2) return true;
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        if(cdb.getPagesize()<=0)
        {
            if(rowidx==lstReportData.size()-1) return true;
        }else
        {
            if(cdb.getFinalPageno()==cdb.getPagecount()&&rowidx==lstReportData.size()-1) return true;//当前报表是分页显示，且当前是最后一页的最后一条记录
        }
        return false;
    }*/

    protected String getColDisplayValueWithWrap(ColBean cbean,String col_displayvalue)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(getColDisplayValueWrapStart(cbean,false));
        if(resultBuf.toString().trim().equals("")) return col_displayvalue;
        resultBuf.append(">").append(col_displayvalue);
        resultBuf.append(getColDisplayValueWrapEnd(cbean,false));
        return resultBuf.toString();
    }

    protected String getColDisplayValueWrapStart(ColBean cbean,boolean isInProperty)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        String startTag="<";
        String endTag=">";
        if(isInProperty)
        {
            startTag="&lt;";
            endTag="&gt;";
        }
        if(rbean.getCellresize()>0||alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL)
        {
            resultBuf.append(startTag).append("div style='width:100%;' class='cls-data-content-list'");
        }
        if(alrbean.getLstRoworderTypes()!=null&&alrbean.getLstRoworderTypes().contains(Consts.ROWORDER_DRAG))
        {
            if(resultBuf.length()>0) resultBuf.append(endTag);
            resultBuf.append(startTag).append("span onmouseover='dragrow_enabled=false;' onmouseout='dragrow_enabled=true;'");
        }
        
        return resultBuf.toString();
    }

    protected String getColDisplayValueWrapEnd(ColBean cbean,boolean isInProperty)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        String startTag="<";
        String endTag=">";
        if(isInProperty)
        {
            startTag="&lt;";
            endTag="&gt;";
        }
        if(alrbean.getLstRoworderTypes()!=null&&alrbean.getLstRoworderTypes().contains(Consts.ROWORDER_DRAG))
        {//是否要行拖动排序
            resultBuf.append(startTag).append("/span").append(endTag);
        }
        if(rbean.getCellresize()>0||alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL)
        {
            resultBuf.append(startTag).append("/div").append(endTag);
        }
        return resultBuf.toString();
    }

    private Map<String,String> getRoworderColvaluesInRow(Object dataObj)
    {
        Map<String,String> mResults=new HashMap<String,String>();
        for(ColBean cbTmp:alrdbean.getLstRoworderValueCols())
        {
            mResults.put(cbTmp.getProperty(),this.getColOriginalValue(dataObj,cbTmp));
        }
        return mResults;
    }

    public String getColSelectedMetadata()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(!rbean.getDbean().isColselect()) return "";
        if(rrequest.getMColAndGroupDisplayBeans()==null||rrequest.getMColAndGroupDisplayBeans().get(rbean.getId())==null)
        {//如果没有此报表的列选择信息，因为是在showDataHeaderPart()方法中构造本报表的列选择信息，所以可能是由于没有调用此方法（比如配置了<display/>的dataheader=""或者在模板文件中没有调用），则调用一下
            showDataHeaderPart();
        }
        if(rrequest.getMColAndGroupDisplayBeans()==null) return "";
        List<ColAndGroupDisplayBean> lstCgdbeansTmp=rrequest.getMColAndGroupDisplayBeans().get(rbean.getId());
        if(lstCgdbeansTmp==null||lstCgdbeansTmp.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<span id=\"").append(rbean.getGuid()).append("_col_titlelist\" style=\"display:none\">");
        String title;
        for(ColAndGroupDisplayBean cgdbeanTmp:lstCgdbeansTmp)
        {
            title=cgdbeanTmp.getTitle();
            title=title==null?"":title.replaceAll("<.*?\\>","").trim();
            resultBuf.append("<item nodeid=\"").append(cgdbeanTmp.getId()).append("\"");
            resultBuf.append(" parentgroupid=\"").append(cgdbeanTmp.getParentGroupId()).append("\"");
            resultBuf.append(" childids=\"").append(cgdbeanTmp.getChildIds()).append("\"");
            resultBuf.append(" layer=\"").append(cgdbeanTmp.getLayer()).append("\"");
            resultBuf.append(" title=\"").append(title).append("\"");
            resultBuf.append(" checked=\"").append(cgdbeanTmp.isChecked()).append("\"");
            resultBuf.append(" isControlCol=\"").append(cgdbeanTmp.isControlCol()).append("\"");
            resultBuf.append(" isNonFixedCol=\"").append(cgdbeanTmp.isNonFixedCol()).append("\"");
            resultBuf.append(" always=\"").append(cgdbeanTmp.isAlways()).append("\"");
            resultBuf.append("></item>");
        }
        resultBuf.append("</span>");
        return resultBuf.toString();
    }

    protected String showMetaDataDisplayStringStart()
    {
        if(this.fixedDataBean==null||this.fixedDataBean.getTotalcolcount()<=0
                ||(this.fixedDataBean.getFixedcolids().trim().equals("")&&this.fixedDataBean.getFixedrowscount()<=0))
            return super.showMetaDataDisplayStringStart();
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(" ft_fixedRowsCount=\"").append(this.fixedDataBean.getFixedrowscount()).append("\"");
        resultBuf.append(" ft_fixedColids=\"").append(this.fixedDataBean.getFixedcolids()).append("\"");
        resultBuf.append(" ft_totalColCount=\"").append(this.fixedDataBean.getTotalcolcount()).append("\"");
        return resultBuf.toString();
    }

    protected class FixedColAndGroupDataBean
    {
        private int fixedrowscount;

        private String fixedcolids;

        private int totalcolcount;//本次显示的列数（包括隐藏列、显示列、冻结列和普通列）

        private String firstNonFixedColid;//第一个非冻结列的colid（之所以不用冻结列的isFixedCol去寻找第一个非冻结列，是isFixedCol只显示在数据列，不显示在标题列，所以没有数据时找不到isFixedCol进行定位。其次如果将isFixedCol显示到冻结列的标题部分，则当是<group/>时，它的isFixedCol为false，但不能将它视为非冻结列，因为它可能包括冻结列，所以专门用一个标识来标识第一个非冻结列）

        private boolean hasDisplayedFirstNonFixedColFlag;

        public FixedColAndGroupDataBean()
        {
            CacheDataBean cdb=rrequest.getCdb(rbean.getId());
            List<ColBean> lstColBeans=getLstDisplayColBeans();
            if(lstColBeans==null||lstColBeans.size()==0) return;
            String dynfixedRows=rrequest.getStringAttribute(rbean.getId()+"_FIXEDROWS","");
            if(!dynfixedRows.equals(""))
            {
                if(dynfixedRows.toLowerCase().equals("title"))
                {
                    fixedrowscount=Integer.MAX_VALUE;
                }else
                {
                    try
                    {
                        fixedrowscount=Integer.parseInt(dynfixedRows);
                    }catch(NumberFormatException e)
                    {
                        log.warn("动态设置的报表"+rbean.getPath()+"冻结行数"+dynfixedRows+"为无效数字");
                        fixedrowscount=alrbean.getFixedrows();
                    }
                }
            }else
            {
                fixedrowscount=alrbean.getFixedrows();
            }
            if(fixedrowscount<0) fixedrowscount=0;
            totalcolcount=0;
            fixedcolids="";
            boolean encounterNonFixedCol=false;
            AbsListReportColBean alrcbeanTmp;
            for(ColBean cbean:lstColBeans)
            {
                totalcolcount++;
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())||cdb.getColDisplayModeAfterAuthorize(cbean)<=0)
                {//永远隐藏列或者当前列不参与本次显示
                    continue;
                }
                
                if(encounterNonFixedCol) continue;
                alrcbeanTmp=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrcbeanTmp==null||!alrcbeanTmp.isFixedCol())
                {
                    encounterNonFixedCol=true;
                    firstNonFixedColid=cbean.getColid();
                }else
                {
                    fixedcolids=fixedcolids+cbean.getColid()+";";
                }
            }
            if(fixedcolids.endsWith(";")) fixedcolids=fixedcolids.substring(0,fixedcolids.length()-1).trim();
        }

        public void setFixedrowscount(int fixedrowscount)
        {
            this.fixedrowscount=fixedrowscount;
        }

        public int getFixedrowscount()
        {
            return fixedrowscount;
        }

        public String getFixedcolids()
        {
            return fixedcolids;
        }

        public String getFirstNonFixedColid()
        {
            return firstNonFixedColid;
        }

        public int getTotalcolcount()
        {
            return totalcolcount;
        }

        public String showFirstNonFixedColFlag(ColBean cbean)
        {
            if(hasDisplayedFirstNonFixedColFlag||fixedcolids.trim().equals("")) return "";
            if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
            if(cbean.getColid().equals(this.firstNonFixedColid))
            {
                this.hasDisplayedFirstNonFixedColFlag=true;
                return " first_nonfixed_col=\"true\"";
            }
            return "";
        }
    }
}
