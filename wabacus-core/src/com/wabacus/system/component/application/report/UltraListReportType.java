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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.itextpdf.text.Element;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.assistant.UltraListReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupDisplayBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupTitlePositionBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.RowDataByInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class UltraListReportType extends ListReportType
{
    public UltraListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    private static Log log=LogFactory.getLog(UltraListReportType.class);

    public final static String KEY=UltraListReportType.class.getName();

    protected final static String MAX_TITLE_ROWSPANS="MAX_TITLE_ROWSPANS";

    protected Map<String,String> mDisplayRealColAndGroupLabels;

    protected String showDataHeaderPart()
    {
        DisplayBean dbean=rbean.getDbean();
        StringBuffer resultBuf=new StringBuffer();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)dbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null||!ulrdbean.isHasGroupConfig())
        {//如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
            return super.showDataHeaderPart();
        }
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions=getRuntimeColAndGroupPosition(ulrdbean,this.cacheDataBean);
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(MAX_TITLE_ROWSPANS);
        int dataheader_rowcount=positionBean.getRowspan();
        List<String> lstDynColids=this.cacheDataBean.getLstDynOrderColids();
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&dbean.isColselect()) mDisplayRealColAndGroupLabels=new HashMap<String,String>();
        String thstyleproperty=null;
        if(this.rbean.getInterceptor()!=null)
        {
            RowDataByInterceptor rowdataObj=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,-1,
                    this.cacheDataBean.getTotalColCount(),this.getLstDisplayColBeans());
            if(rowdataObj!=null)
            {
                if(rowdataObj.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObj.getInsertDisplayRowHtml());
                thstyleproperty=rowdataObj.getDynTrStyleproperty();
                if(!rowdataObj.isShouldDisplayThisRow()) return resultBuf.toString();
            }
        }
        if(thstyleproperty==null) thstyleproperty="";
        resultBuf.append("<tr  class='"+getDataHeaderTrClassName()+"' ").append(thstyleproperty).append(">");
        List lstChildren=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(ulrdbean.getLstChildren(),lstDynColids,mColAndGroupTitlePostions);
        String lastDisplayColIdInFirstTitleRow=getLastDisplayColIdInFirstTitleRow(ulrdbean,this.cacheDataBean);
        resultBuf.append(showLabel(lstChildren,mColAndGroupTitlePostions,lastDisplayColIdInFirstTitleRow));
        resultBuf.append("</tr>");
        UltraListReportGroupBean groupBean;
        Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();
        ColAndGroupTitlePositionBean positionBeanTmp;
        String groupidTmp;
        for(int i=1;i<dataheader_rowcount;i++)
        {
            resultBuf.append("<tr  class='"+getDataHeaderTrClassName()+"' ").append(thstyleproperty).append(">");
            for(Object obj:lstChildren)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                groupidTmp=groupBean.getGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(groupidTmp);
                if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;//不参与本次显示或没有显示权限
                Integer layer=mGroupLayers.get(groupidTmp);
                if(layer==null) layer=1;
                List lstChildrenLocal=groupBean.getDisplayChildrenByLayer(groupBean.getLstChildren(),mColAndGroupTitlePostions,layer,i+1,groupBean
                        .getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                mGroupLayers.put(groupidTmp,layer+1);
                lstChildrenLocal=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(lstChildrenLocal,lstDynColids,mColAndGroupTitlePostions);
                resultBuf.append(showLabel(lstChildrenLocal,mColAndGroupTitlePostions,null));
            }
            resultBuf.append("</tr>");
        }
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&dbean.isColselect())
        {
            
            rrequest.addColAndGroupDisplayBean(rbean.getId(),
                    createColAndGroupDisplayBeans(ulrdbean,lstDynColids,mColAndGroupTitlePostions));
        }
        if(this.fixedDataBean!=null&&this.fixedDataBean.getFixedrowscount()==Integer.MAX_VALUE)
        {
            this.fixedDataBean.setFixedrowscount(dataheader_rowcount);//显示了标题行，则设置冻结行数为dataheader_rowcount
        }
        return resultBuf.toString();
    }

    private String getLastDisplayColIdInFirstTitleRow(UltraListReportDisplayBean urldbean,CacheDataBean cdb)
    {
        String lastColId=cdb.getLastColId();
        ColBean cb=rbean.getDbean().getColBeanByColId(lastColId);
        UltraListReportColBean urlcbean=(UltraListReportColBean)cb.getExtendConfigDataForReportType(KEY);
        String parentGroupid=urlcbean.getParentGroupid();
        UltraListReportGroupBean urlgroupbean;
        while(parentGroupid!=null&&!parentGroupid.trim().equals(""))
        {
            urlgroupbean=urldbean.getGroupBeanById(parentGroupid);
            lastColId=urlgroupbean.getGroupid();
            parentGroupid=urlgroupbean.getParentGroupid();
        }
        return lastColId;
    }

    protected String showLabel(List lstChildren,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,String lastDisplayColIdInFirstTitleRow)
    {
        StringBuffer resultBuf=new StringBuffer();
        String labelstyleproperty=null;
        String label=null;
        String parentGroupid=null;
        AbsListReportColBean alrcbean;
        UltraListReportColBean ulrcbean;
        ColBean colbean;
        UltraListReportGroupBean groupBean;
        ColDataByInterceptor coldataByInterceptor;//从拦截器中得到的用户为当前列生成的数据；
        ColAndGroupTitlePositionBean positionBeanTmp;
        String id;
        for(Object obj:lstChildren)
        {
            colbean=null;
            alrcbean=null;
            groupBean=null;
            positionBeanTmp=null;
            id=null;
            if(obj instanceof ColBean)
            {
                colbean=(ColBean)obj;
                if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
                {
                    if(alrdbean.getLstRowgroupColsColumn().contains(colbean.getColumn())
                            &&!colbean.getColumn().equals(alrdbean.getLstRowgroupColsColumn().get(0)))
                    {//如果当前cbean是树形行分组的列，但不是第一列，则不显示为一独立列，所以这里就不为它显示一个<td/>
                        continue;
                    }
                }
                alrcbean=(AbsListReportColBean)colbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                ulrcbean=(UltraListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(ulrcbean!=null) parentGroupid=ulrcbean.getParentGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(colbean.getColid());
                labelstyleproperty=colbean.getLabelstyleproperty(rrequest);
                label=colbean.getLabel();
                id=colbean.getColid();
            }else if(obj instanceof UltraListReportGroupBean)
            {
                groupBean=((UltraListReportGroupBean)obj);
                labelstyleproperty=groupBean.getLabelstyleproperty();
                label=groupBean.getLabel();
                parentGroupid=groupBean.getParentGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBean.getGroupid());
                id=groupBean.getGroupid();
            }
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            if(positionBeanTmp.getColspan()>1)
            {
                labelstyleproperty=" colspan='"+positionBeanTmp.getColspan()+"' "+labelstyleproperty;
            }
            if(positionBeanTmp.getRowspan()>1)
            {
                labelstyleproperty=" rowspan='"+positionBeanTmp.getRowspan()+"' "+labelstyleproperty;
            }
            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,obj,-1,label);
            label=ReportAssistant.getInstance().getColGroupLabel(rrequest,label,coldataByInterceptor);
            if(mDisplayRealColAndGroupLabels!=null) mDisplayRealColAndGroupLabels.put(id,label);
            resultBuf.append("<td class='"+getDataHeaderThClassName()+"' ");
            resultBuf.append(getColGroupStyleproperty(labelstyleproperty,coldataByInterceptor));
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                if(rbean.getCelldrag()>0)
                {
                    String dragcolid=null;
                    if(colbean!=null)
                    {
                        if(alrcbean==null||alrcbean.isDragable(alrdbean)) dragcolid=colbean.getColid();
                    }else if(groupBean.isDragable(alrdbean))
                    {
                        dragcolid=groupBean.getGroupid();
                    }
                    if(dragcolid!=null)
                    {
                        resultBuf.append(" onmousedown=\"try{handleCellDragMouseDown(this,'"+rbean.getPageBean().getId()+"','"+rbean.getId()+"');}catch(e){logErrorsAsJsFileLoad(e);}\"");
                        resultBuf.append(" dragcolid=\"").append(dragcolid).append("\"");
                        if(parentGroupid!=null&&!parentGroupid.trim().equals(""))
                        {
                            resultBuf.append(" parentGroupid=\"").append(parentGroupid).append("\"");
                        }
                    }
                }
                if(this.fixedDataBean!=null&&obj instanceof ColBean)
                {
                    resultBuf.append(this.fixedDataBean.showFirstNonFixedColFlag((ColBean)obj));
                }
                resultBuf.append(">");
                if(rbean.getCellresize()>0&&lastDisplayColIdInFirstTitleRow!=null)
                {//配置了调整单元格宽度功能，且当前是显示第一行
                    if(rbean.getCellresize()==1&&!id.equals(lastDisplayColIdInFirstTitleRow))
                    {
                        resultBuf.append(ListReportAssistant.getInstance().appendCellResizeFunction(true));
                    }else
                    {
                        resultBuf.append(ListReportAssistant.getInstance().appendCellResizeFunction(false));
                    }
                }
                if(lastDisplayColIdInFirstTitleRow!=null&&id.equals(lastDisplayColIdInFirstTitleRow)&&rbean.getDbean().isColselect())
                {
                    resultBuf.append(ReportAssistant.getInstance().getColSelectedLabelAndEvent(rrequest,rbean,true));
                }
                if(colbean!=null&&alrcbean!=null)
                {
                    
                    if(this.getLstReportData()!=null&&this.getLstReportData().size()>0)
                    {
                        if(alrcbean.isRequireClickOrderby())
                        {
                            label=ListReportAssistant.getInstance().getColLabelWithOrderBy(colbean,rrequest);
                        }
                        //filterenabled=true;
                    }
                    if(alrcbean.getFilterBean()!=null)
                    {
                        resultBuf.append(ListReportAssistant.getInstance().createColumnFilter(rrequest,alrcbean));
                    }
                }
            }else
            {
                String dataheaderbgcolor=Config.getInstance().getSkinConfigValue(rrequest.getPageskin(),"table.dataheader.bgcolor");
                if(dataheaderbgcolor==null) dataheaderbgcolor="";
                resultBuf.append(" bgcolor='"+dataheaderbgcolor+"'>");
            }
            resultBuf.append(label+"</td>");
        }
        return resultBuf.toString();
    }

    private List<ColAndGroupDisplayBean> createColAndGroupDisplayBeans(UltraListReportDisplayBean urldbean,
            List<String> lstDynColids,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        List<ColAndGroupDisplayBean> lstColAndGroupDisplayBeans=new ArrayList<ColAndGroupDisplayBean>();
        ColAndGroupDisplayBean cgDisplayBeanTmp;
        ColBean cbTmp;
        AbsListReportColBean alrcbean=null;
        UltraListReportGroupBean ulgroupbeanTmp;
        List lstChildren=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(urldbean.getLstChildren(),lstDynColids,mColAndGroupTitlePostions);
        
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        for(Object objTmp:lstChildren)
        {
            cgDisplayBeanTmp=new ColAndGroupDisplayBean();
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
                {
                    if(alrdbean.getLstRowgroupColsColumn().contains(cbTmp.getColumn())
                            &&!cbTmp.getColumn().equals(alrdbean.getLstRowgroupColsColumn().get(0)))
                    {
                        continue;
                    }
                }
                //                ulcbTmp=(UltraReportListColBean)cbTmp.getExtendConfigDataForReportType(UltraListReportType.KEY);
                cgpositionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(cgpositionBeanTmp.getDisplaymode()<0) continue;
                alrcbean=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                cgDisplayBeanTmp.setNonFixedCol(alrcbean==null||!alrcbean.isFixedCol());
                cgDisplayBeanTmp.setId(cbTmp.getColid());
                cgDisplayBeanTmp.setControlCol(cbTmp.isControlCol());
                cgDisplayBeanTmp.setAlways(cgpositionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(cgpositionBeanTmp.getDisplaymode()>0);
                if(cgDisplayBeanTmp.isChecked())
                {
                    cgDisplayBeanTmp.setTitle(this.mDisplayRealColAndGroupLabels.get(cbTmp.getColid()));
                }else
                {
                    cgDisplayBeanTmp.setTitle(rrequest.getI18NStringValue(cbTmp.getLabel()));
                }
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ulgroupbeanTmp=(UltraListReportGroupBean)objTmp;
                cgpositionBeanTmp=mColAndGroupTitlePostions.get(ulgroupbeanTmp.getGroupid());
                if(cgpositionBeanTmp.getDisplaymode()<0) continue;
                cgDisplayBeanTmp.setId(ulgroupbeanTmp.getGroupid());
                cgDisplayBeanTmp.setChildIds(ulgroupbeanTmp.getChildids());
                cgDisplayBeanTmp.setAlways(cgpositionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(cgpositionBeanTmp.getDisplaymode()>0);
                if(cgDisplayBeanTmp.isChecked())
                {
                    cgDisplayBeanTmp.setTitle(this.mDisplayRealColAndGroupLabels.get(ulgroupbeanTmp.getGroupid()));
                }else
                {
                    cgDisplayBeanTmp.setTitle(rrequest.getI18NStringValue(ulgroupbeanTmp.getLabel()));
                }
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
                ulgroupbeanTmp.createColAndGroupDisplayBeans(mDisplayRealColAndGroupLabels,rrequest,lstDynColids,mColAndGroupTitlePostions,
                        lstColAndGroupDisplayBeans);
            }
        }
        return lstColAndGroupDisplayBeans;
    }

    protected void showReportTitleOnPlainExcel(Workbook workbook)
    {
        String plainexceltitle=null;
        if(this.pedebean!=null) plainexceltitle=this.pedebean.getPlainexceltitle();
        if("none".equals(plainexceltitle)) return;
        if("column".equals(plainexceltitle))
        {
            super.showReportTitleOnPlainExcel(workbook);
            return;
        }
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null||!ulrdbean.isHasGroupConfig())
        {//如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
            super.showReportTitleOnPlainExcel(workbook);
            return;
        }
        List<String> lstDynOrderColids=cacheDataBean.getLstDynOrderColids();
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions=getRuntimeColAndGroupPosition(ulrdbean,cacheDataBean);
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(MAX_TITLE_ROWSPANS);
        int dataheader_rowcount=positionBean.getRowspan();//取到标题部分所占的行数
        List lstChildren=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(ulrdbean.getLstChildren(),lstDynOrderColids,mColAndGroupTitlePostions);
        showLabelInPlainExcel(workbook,excelSheet,lstChildren,mColAndGroupTitlePostions);
        UltraListReportGroupBean groupBean;
        Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();
        ColAndGroupTitlePositionBean positionBeanTmp;
        String groupidTmp;
        for(int i=1;i<dataheader_rowcount;i++)
        {
            for(Object obj:lstChildren)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                groupidTmp=groupBean.getGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(groupidTmp);
                if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
                Integer layer=mGroupLayers.get(groupidTmp);
                if(layer==null) layer=1;
                List lstChildrenLocal=groupBean.getDisplayChildrenByLayer(groupBean.getLstChildren(),mColAndGroupTitlePostions,layer,i+1,groupBean
                        .getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                mGroupLayers.put(groupidTmp,layer+1);
                lstChildrenLocal=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(lstChildrenLocal,lstDynOrderColids,mColAndGroupTitlePostions);
                showLabelInPlainExcel(workbook,excelSheet,lstChildrenLocal,mColAndGroupTitlePostions);
            }
        }
        excelRowIdx+=dataheader_rowcount;
    }

    private Map<String,ColAndGroupTitlePositionBean> getRuntimeColAndGroupPosition(UltraListReportDisplayBean ulrdbean,CacheDataBean cdb)
    {
        List<String> lstDynOrderColids=cdb.getLstDynOrderColids();
        AbsListReportBean alrbean=(AbsListReportBean)rbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions;
        if((rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL&&lstDynOrderColids!=null&&lstDynOrderColids.size()>0)
                ||(cdb.getLstDynDisplayColids()!=null&&cdb.getLstDynDisplayColids().size()>0)//客户端进行了列选择操作
                ||"false".equals(String.valueOf(cdb.getAttributes().get("authroize_col_display")).trim())
                ||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&alrbean.hasControllCol()))
        {
            mColAndGroupTitlePostions=calPosition(rbean,ulrdbean.getLstChildren(),cdb.getLstDynDisplayColids());
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL)
            {
                calPositionForStandardExcel(ulrdbean.getLstChildren(),lstDynOrderColids,mColAndGroupTitlePostions);
            }
        }else
        {
            mColAndGroupTitlePostions=ulrdbean.getMChildrenDefaultPositions();
        }
        return mColAndGroupTitlePostions;
    }
    
    protected void showLabelInPlainExcel(Workbook workbook,Sheet sheet,List lstChildren,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        CellStyle titleCellStyle=StandardExcelAssistant.getInstance().getTitleCellStyleForStandardExcel(workbook);//获取标题行的样式对象
        ColBean colbean;
        UltraListReportGroupBean groupBean;
        ColDataByInterceptor coldataByInterceptor;
        ColAndGroupTitlePositionBean positionBeanTmp;
        String id;
        String label=null;
        String align=null;
        CellRangeAddress region;
        for(Object obj:lstChildren)
        {
            colbean=null;
            groupBean=null;
            positionBeanTmp=null;
            id=null;
            align=null;
            if(obj instanceof ColBean)
            {
                colbean=(ColBean)obj;
                id=colbean.getColid();
                label=colbean.getLabel();
                align=colbean.getLabelalign();
            }else if(obj instanceof UltraListReportGroupBean)
            {
                groupBean=((UltraListReportGroupBean)obj);
                label=groupBean.getLabel();
                id=groupBean.getGroupid();
            }
            positionBeanTmp=mColAndGroupTitlePostions.get(id);
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,obj,-1,label);
            label=ReportAssistant.getInstance().getColGroupLabel(rrequest,label,coldataByInterceptor);
            region=new CellRangeAddress(positionBeanTmp.getStartrowindex(),positionBeanTmp.getStartrowindex()+positionBeanTmp.getRowspan()-1,
                    positionBeanTmp.getStartcolindex(),positionBeanTmp.getStartcolindex()+positionBeanTmp.getColspan()-1);
            StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,sheet,region,StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,align),label);
        }
    }

    protected void showDataHeaderOnPdf()
    {
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null||!ulrdbean.isHasGroupConfig())
        {//如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
            super.showDataHeaderOnPdf();
            return;
        }
        List<String> lstDynOrderColids=cacheDataBean.getLstDynOrderColids();
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions=getRuntimeColAndGroupPosition(ulrdbean,cacheDataBean);
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(MAX_TITLE_ROWSPANS);
        int dataheader_rowcount=positionBean.getRowspan();
        List lstChildren=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(ulrdbean.getLstChildren(),lstDynOrderColids,mColAndGroupTitlePostions);
        showLabelInPdf(lstChildren,mColAndGroupTitlePostions);
        UltraListReportGroupBean groupBean;
        Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();
        ColAndGroupTitlePositionBean positionBeanTmp;
        String groupidTmp;
        for(int i=1;i<dataheader_rowcount;i++)
        {
            for(Object obj:lstChildren)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                groupidTmp=groupBean.getGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(groupidTmp);
                if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
                Integer layer=mGroupLayers.get(groupidTmp);//取到当前分组当前要显示的层级数
                if(layer==null) layer=1;
                List lstChildrenLocal=groupBean.getDisplayChildrenByLayer(groupBean.getLstChildren(),mColAndGroupTitlePostions,layer,i+1,groupBean
                        .getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                mGroupLayers.put(groupidTmp,layer+1);
                lstChildrenLocal=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(lstChildrenLocal,lstDynOrderColids,mColAndGroupTitlePostions);
                showLabelInPdf(lstChildrenLocal,mColAndGroupTitlePostions);
            }
        }
    }

    protected void showLabelInPdf(List lstChildren,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        ColBean colbean;
        UltraListReportGroupBean groupBean;
        ColDataByInterceptor coldataByInterceptor;
        ColAndGroupTitlePositionBean positionBeanTmp;
        String id;
        String label=null;
        String align=null;
        for(Object obj:lstChildren)
        {
            colbean=null;
            groupBean=null;
            positionBeanTmp=null;
            align=null;
            id=null;
            if(obj instanceof ColBean)
            {
                colbean=(ColBean)obj;
                id=colbean.getColid();
                label=colbean.getLabel();
                align=colbean.getLabelalign();
            }else if(obj instanceof UltraListReportGroupBean)
            {
                groupBean=((UltraListReportGroupBean)obj);
                label=groupBean.getLabel();
                id=groupBean.getGroupid();
            }
            positionBeanTmp=mColAndGroupTitlePostions.get(id);
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,obj,-1,label);
            label=ReportAssistant.getInstance().getColGroupLabel(rrequest,label,coldataByInterceptor);
            this.addDataHeaderCell(obj,label,positionBeanTmp.getRowspan(),positionBeanTmp.getColspan(),this.getPdfCellAlign(align,Element.ALIGN_CENTER));
        }
    }
    
    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        if(lstEleDisplayBeans==null) return 0;
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null)
        {
            ulrdbean=new UltraListReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,ulrdbean);
        }
        List<XmlElementBean> lstColAndGroups=new ArrayList<XmlElementBean>();
        boolean hasGroupConfig=joinedAllColAndGroupElement(lstEleDisplayBeans,lstColAndGroups,disbean.getReportBean());//取到所有要显示的直接子<col/>和<group/>标签对象
        if(!hasGroupConfig)
        {//没有<group/>配置
            ulrdbean.setHasGroupConfig(false);
            return super.afterDisplayLoading(disbean,lstEleDisplayBeans);
        }
        ulrdbean.setHasGroupConfig(true);
        disbean.clearChildrenInfo();
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrdbean!=null) alrdbean.clearChildrenInfo();
        if(lstColAndGroups==null||lstColAndGroups.size()==0) return 0;
        List lstChildren=new ArrayList();
        ulrdbean.setLstChildren(lstChildren);
        for(XmlElementBean eleChildBeanTmp:lstColAndGroups)
        {
            if(eleChildBeanTmp.getName().equalsIgnoreCase("col"))
            {
                ColBean colbean=ComponentConfigLoadManager.loadColConfig(eleChildBeanTmp,disbean);
                UltraListReportColBean ulrcbean=(UltraListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(ulrcbean==null)
                {
                    ulrcbean=new UltraListReportColBean(colbean);
                    colbean.setExtendConfigDataForReportType(KEY,ulrcbean);
                }
                disbean.getLstCols().add(colbean);
                if(!colbean.getDisplaytype().equals(Consts.COL_DISPLAYTYPE_HIDDEN))
                {
                    lstChildren.add(colbean);
                }
            }else if(eleChildBeanTmp.getName().equalsIgnoreCase("group"))
            {
                UltraListReportGroupBean groupBean=new UltraListReportGroupBean(disbean);
                lstChildren.add(groupBean);
                loadGroupConfig(groupBean,eleChildBeanTmp,disbean,null);
            }
        }
        if(lstChildren.size()==0) throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，没有配置要显示的列");
        int flag=super.afterDisplayLoading(disbean,lstEleDisplayBeans);
        if(flag<0) return -1;
        return 1;
    }

    protected boolean joinedAllColAndGroupElement(List<XmlElementBean> lstEleDisplayBeans,List<XmlElementBean> lstResults,ReportBean rbean)
    {
        boolean hasGroupConfig=false;
        List<XmlElementBean> lstChildrenTmp;
        for(XmlElementBean eleBeanTmp:lstEleDisplayBeans)
        {
            lstChildrenTmp=eleBeanTmp.getLstChildElements();
            if(lstChildrenTmp==null||lstChildrenTmp.size()==0) continue;
            for(XmlElementBean childTmp:lstChildrenTmp)
            {
                if("group".equals(childTmp.getName())||"col".equals(childTmp.getName()))
                {
                    lstResults.add(childTmp);
                    if(childTmp.getName().equals("group")) hasGroupConfig=true;
                }else if("ref".equals(childTmp.getName()))
                {
                    hasGroupConfig=joinedAllColAndGroupElement(ConfigLoadAssistant.getInstance().getRefElements(childTmp.attributeValue("key"),
                            "display",null,rbean),lstResults,rbean);
                }
            }
        }
        return hasGroupConfig;
    }

    protected void loadGroupConfig(UltraListReportGroupBean groupBean,XmlElementBean eleGroupBean,DisplayBean disbean,
            UltraListReportGroupBean parentGroupBean)
    {
        String label=eleGroupBean.attributeValue("label");
        label=label==null?"":label.trim();
        String labelstyleproperty=eleGroupBean.attributeValue("labelstyleproperty");
        labelstyleproperty=labelstyleproperty==null?"":labelstyleproperty.trim();
        labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"align","center");
        labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"valign","middle");
        groupBean.setLabelstyleproperty(labelstyleproperty);
        if(label!=null)
        {
            label=label.trim();
            label=Config.getInstance().getResourceString(null,disbean.getPageBean(),label,true);
        }
        groupBean.setLabel(label);
        String rowspan=Tools.getPropertyValueByName("rowspan",labelstyleproperty,true);
        if(rowspan!=null&&!rowspan.trim().equals(""))
        {//显示在labelstyleproperty中指定了本分组列的rowspan数
            try
            {
                groupBean.setRowspan(Integer.parseInt(rowspan));
            }catch(NumberFormatException e)
            {
                log.warn("报表"+disbean.getReportBean().getPath()+"配置的<group/>的labelstyleproperty中的rowspan不是合法数字",e);
                groupBean.setRowspan(1);
            }
        }
        List lstGroupChildren=new ArrayList();
        groupBean.setLstChildren(lstGroupChildren);
        StringBuffer childIdsBuf=new StringBuffer();
        List<XmlElementBean> lstChildrenElements=eleGroupBean.getLstChildElements();
        if(lstChildrenElements==null||lstChildrenElements.size()==0)
        {
            throw new WabacusConfigLoadingException("报表"+disbean.getReportBean().getPath()+"配置的group"+label+"没有配置子标签");
        }
        for(XmlElementBean eleChildBeanTmp:lstChildrenElements)
        {
            if(eleChildBeanTmp.getName().equalsIgnoreCase("col"))
            {
                ColBean colbean=ComponentConfigLoadManager.loadColConfig(eleChildBeanTmp,disbean);

//                {//如果当前普通列是永远显示，则其所有层级的父分组也必须设置为永远显示


                AbsListReportColBean alrcbean=(AbsListReportColBean)colbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                AbsListReportDisplayBean alrdbeanTmp=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrdbeanTmp!=null&&alrdbeanTmp.getRowgrouptype()==2&&alrcbean!=null&&alrcbean.isRowgroup())
                {
                    throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，<group/>下的<col/>不能参与树形分组");
                }
                UltraListReportColBean ulrcbeanTmp=(UltraListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(ulrcbeanTmp==null)
                {
                    ulrcbeanTmp=new UltraListReportColBean(colbean);
                    colbean.setExtendConfigDataForReportType(KEY,ulrcbeanTmp);
                }
                ulrcbeanTmp.setParentGroupid(groupBean.getGroupid());
                disbean.getLstCols().add(colbean);
                if(!colbean.getDisplaytype().equals(Consts.COL_DISPLAYTYPE_HIDDEN))
                {
                    lstGroupChildren.add(colbean);
                    childIdsBuf.append(colbean.getColid()).append(",");
                }
            }else if(eleChildBeanTmp.getName().equalsIgnoreCase("group"))
            {
                UltraListReportGroupBean groupBeanChild=new UltraListReportGroupBean(disbean);
                groupBeanChild.setParentGroupid(groupBean.getGroupid());
                lstGroupChildren.add(groupBeanChild);
                loadGroupConfig(groupBeanChild,eleChildBeanTmp,disbean,groupBean);
                childIdsBuf.append(groupBeanChild.getGroupid()).append(",");
            }
        }
        if(lstGroupChildren.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"的分组"+label+"失败，没有为它配置有效的显示子标签");
        }
        if(childIdsBuf.charAt(childIdsBuf.length()-1)==',')
        {
            childIdsBuf.deleteCharAt(childIdsBuf.length()-1);
        }
        groupBean.setChildids(childIdsBuf.toString());
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        DisplayBean dbean=reportbean.getDbean();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)dbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean!=null)
        {
            List lstChildren=ulrdbean.getLstChildren();
            if(lstChildren==null||lstChildren.size()==0) return 0;
            for(Object childObj:lstChildren)
            {
                if(childObj==null) continue;
                if(childObj instanceof UltraListReportGroupBean)
                {
                    ((UltraListReportGroupBean)childObj).doPostLoad();
                }
            }
            Map<String,ColAndGroupTitlePositionBean> mPositions=calPosition(reportbean,ulrdbean.getLstChildren(),null);//计算每一普通列和分组的的显示位置
            ulrdbean.setMChildrenDefaultPositions(mPositions);
            calPositionForStandardExcel(ulrdbean.getLstChildren(),null,mPositions);
        }
        return 1;
    }

    protected ColBean[] processRowSelectCol(DisplayBean disbean)
    {
        ColBean[] cbResults=super.processRowSelectCol(disbean);
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null) return cbResults;
        if(alrbean.getRowSelectType()==null
                ||(!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_CHECKBOX)&&!alrbean.getRowSelectType().trim().equals(
                        Consts.ROWSELECT_RADIOBOX)))
        {//当前报表要么没有提供行选中功能，要么提供的不是复选框/单选框的行选择功能
            ulrdbean.removeChildColBeanByColumn(Consts_Private.COL_ROWSELECT,true);
        }else
        {
            if(cbResults!=null&&ulrdbean.getLstChildren()!=null&&ulrdbean.getLstChildren().size()>0)
            {//当前报表的行选择类型为Consts.ROWSELECT_CHECKBOX或Consts.ROWSELECT_RADIOBOX，且没有配置行选择列，而是使用自动生成的列对象，并且此报表配置了<group/>，而不是单行标题的报表
                UltraListReportColBean ulrcbean=new UltraListReportColBean(cbResults[0]);
                cbResults[0].setExtendConfigDataForReportType(KEY,ulrcbean);
                insertNewRowSelectCol(ulrdbean.getLstChildren(),cbResults[0],cbResults[1]);
            }
        }
        return cbResults;
    }

    protected void insertNewRowSelectCol(List lstChildren,ColBean cbNewRowSelect,ColBean cbNext)
    {
        if(cbNext==null)
        {
            lstChildren.add(cbNewRowSelect);
            return;
        }
        Object objTmp;
        ColBean cbTmp;
        for(int i=0;i<lstChildren.size();i++)
        {
            objTmp=lstChildren.get(i);
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                if(cbTmp.getColid().equals(cbNext.getColid()))
                {//找到了行选择列的后面一列，将行选择列放到其前面
                    lstChildren.add(i,cbNewRowSelect);
                    break;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                if(((UltraListReportGroupBean)objTmp).containsChild(cbNext.getColid(),true))
                {
                    lstChildren.add(i,cbNewRowSelect);
                    break;
                }
            }
        }
    }

    protected List<ColBean> processRoworderCol(DisplayBean disbean)
    {
        List<ColBean> lstCreatedColBeans=super.processRoworderCol(disbean);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null) return lstCreatedColBeans;
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        for(String rowordertypeTmp:Consts.lstAllRoworderTypes)
        {
            if(rowordertypeTmp.equals(Consts.ROWORDER_DRAG)) continue;
            if(alrbean.getLstRoworderTypes()==null||!alrbean.getLstRoworderTypes().contains(rowordertypeTmp))
            {
                ulrdbean.removeChildColBeanByColumn(getRoworderColColumnByRoworderType(rowordertypeTmp),true);
            }
        }
        if(lstCreatedColBeans!=null&&ulrdbean.getLstChildren()!=null&&ulrdbean.getLstChildren().size()>0)
        {
            for(ColBean cbCreatedTmp:lstCreatedColBeans)
            {
                UltraListReportColBean ulrcbean=new UltraListReportColBean(cbCreatedTmp);
                cbCreatedTmp.setExtendConfigDataForReportType(KEY,ulrcbean);
                ulrdbean.getLstChildren().add(cbCreatedTmp);
            }
        }
        return lstCreatedColBeans;
    }

    protected Map<String,ColAndGroupTitlePositionBean> calPosition(ReportBean reportbean,List lstChildren,List<String> lstDisplayColIds)
    {
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions=new HashMap<String,ColAndGroupTitlePositionBean>();
        int maxrowspan=calPositionStart(lstChildren,lstDisplayColIds,mColAndGroupTitlePostions);
        if(maxrowspan==0) return new HashMap<String,ColAndGroupTitlePositionBean>();
        calPositionEnd(lstChildren,mColAndGroupTitlePostions,maxrowspan);
        
        ColAndGroupTitlePositionBean positionTmp=new ColAndGroupTitlePositionBean();
        positionTmp.setRowspan(maxrowspan);
        mColAndGroupTitlePostions.put(MAX_TITLE_ROWSPANS,positionTmp);
        return mColAndGroupTitlePostions;
    }

    private int calPositionStart(List lstChildren,List<String> lstDisplayColIds,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        int maxrowspan=0;//本次显示的标题行总行数
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp==null)
                {
                    positionBeanTmp=new ColAndGroupTitlePositionBean();
                    mColAndGroupTitlePostions.put(cbTmp.getColid(),positionBeanTmp);
                }
                positionBeanTmp.setDisplaymode(cbTmp.getDisplaymode(rrequest,lstDisplayColIds));
                if(maxrowspan==0&&positionBeanTmp.getDisplaymode()>0)
                {
                    maxrowspan=1;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                int[] spans=groupBeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions,lstDisplayColIds);
                if(mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid()).getDisplaymode()>0&&spans[1]>maxrowspan)
                {
                    maxrowspan=spans[1];
                }
            }
        }
        return maxrowspan;
    }

    private void calPositionEnd(List lstChildren,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int maxrowspan)
    {
        ColBean cbTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                positionBeanTmp.setLayer(0);
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setRowspan(maxrowspan);
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ((UltraListReportGroupBean)objTmp).calPositionEnd(mColAndGroupTitlePostions,new int[] { maxrowspan, 0 });
            }
        }
    }

    protected void calPositionForStandardExcel(List lstChildren,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(lstChildren==null||lstChildren.size()==0) return;
        lstChildren=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(lstChildren,lstDynColids,mColAndGroupTitlePostions);
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        int startcolidx=0;//起始列号
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setStartcolindex(startcolidx);
                    positionBeanTmp.setStartrowindex(0);
                    startcolidx++;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    groupBeanTmp.calPositionForStandardExcel(mColAndGroupTitlePostions,lstDynColids,new int[] { 0, startcolidx });
                    startcolidx+=positionBeanTmp.getColspan();
                }
            }
        }
    }
}
