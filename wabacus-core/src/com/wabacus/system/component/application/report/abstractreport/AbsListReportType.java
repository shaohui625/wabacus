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
package com.wabacus.system.component.application.report.abstractreport;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.ResourceUtils;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.commoninterface.IListReportRoworderPersistence;
import com.wabacus.system.component.application.report.UltraListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSqlBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportStatItemBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportStatiBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportStatiColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportStatiRowGroupBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public abstract class AbsListReportType extends AbsReportType
{
    public final static String KEY=AbsListReportType.class.getName();

    private final static Log log=LogFactory.getLog(AbsListReportType.class);

    protected AbsListReportBean alrbean;
    
    protected AbsListReportDisplayBean alrdbean;
    
    public AbsListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            alrbean=(AbsListReportBean)((ReportBean)comCfgBean).getExtendConfigDataForReportType(KEY);
            alrdbean=(AbsListReportDisplayBean)((ReportBean)comCfgBean).getDbean().getExtendConfigDataForReportType(KEY);
        }
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        ReportBean reportbean=(ReportBean)applicationConfigBean;
        super.initUrl(reportbean,rrequest);
        String colFilterId=rrequest.getStringAttribute(reportbean.getId()+"_COL_FILTERID","");
        if(!colFilterId.equals(""))
        {
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
            AbsListReportFilterBean filterbean=alrdbean.getFilterBeanById(colFilterId);
            if(filterbean!=null&&!filterbean.isConditionRelate())
            {
                String filterVal=rrequest.getStringAttribute(colFilterId,"");
                if(!filterVal.trim().equals(""))
                {
                    rrequest.addParamToUrl(colFilterId,filterVal,true);
                    rrequest.addParamToUrl(reportbean.getId()+"_COL_FILTERID",colFilterId,true);
                }
            }
        }
        String reportid=reportbean.getId();
        rrequest.addParamToUrl(reportid+"ORDERBY","rrequest{"+reportid+"ORDERBY}",true);

        rrequest.addParamToUrl(reportid+"_DYNCOLUMNORDER","rrequest{"+reportid+"_DYNCOLUMNORDER}",true);
    }

    public void initReportBeforeDoStart()
    {
        super.initReportBeforeDoStart();
        rrequest.setFilterCondition(rbean);
        String orderby=null;
        String orderbyAction=rrequest.getStringAttribute(rbean.getId()+"ORDERBY_ACTION","");
        if(orderbyAction.equals("true"))
        {
            orderby=rrequest.getStringAttribute(rbean.getId()+"ORDERBY","");
            if(rbean.getPersonalizeObj()!=null)
            {
                rbean.getPersonalizeObj().storeOrderByCol(rrequest,rbean,orderby);
            }
        }else
        {
            if(rbean.getPersonalizeObj()!=null)
            {
                orderby=rbean.getPersonalizeObj().loadOrderByCol(rrequest,rbean);
            } 
            if(orderby==null||orderby.trim().equals(""))
            {
                orderby=rrequest.getStringAttribute(rbean.getId()+"ORDERBY","");
            }
        }
        if(!orderby.equals(""))
        {
            List<String> lstTemp=Tools.parseStringToList(orderby,"||");
            if(lstTemp==null||lstTemp.size()!=2)
            {
                log.error("URL中传入的排序字段"+orderby+"不合法，必须为字段名+||+(asc|desc)格式");
            }else
            {
                String[] str=new String[2];
                str[0]=lstTemp.get(0);
                str[1]=lstTemp.get(1);
                if(str[0]==null) str[0]="";
                if(str[1]==null||str[1].trim().equals("")||(!str[1].equalsIgnoreCase("desc")&&!str[1].equalsIgnoreCase("asc")))
                {
                    str[1]="asc";
                }
                rrequest.setAttribute(rbean.getId(),"ORDERBYARRAY",str);
            }
        }
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<String> lstColIds=null;
        String dyncolumnorder=null;
        if(rbean.getPersonalizeObj()!=null)
        {//配置了个性化持久化对象
            dyncolumnorder=rbean.getPersonalizeObj().loadColOrderData(rrequest,rbean);
        }
        if(dyncolumnorder==null||dyncolumnorder.trim().equals(""))
        {
            dyncolumnorder=rrequest.getStringAttribute(rbean.getId()+"_DYNCOLUMNORDER","");
        }
        if(!dyncolumnorder.equals(""))
        {
            lstColIds=Tools.parseStringToList(dyncolumnorder,";");
        }
        String dragcols=rrequest.getStringAttribute(rbean.getId()+"_DRAGCOLS","");
        if(!dragcols.equals(""))
        {
            if(lstColIds==null)
            {
                lstColIds=new ArrayList<String>();
                for(ColBean cbTmp:rbean.getDbean().getLstCols())
                {
                    lstColIds.add(cbTmp.getColid());
                }
            }
            lstColIds=processDragCols(dragcols,lstColIds);
        }
        if(lstColIds!=null&&lstColIds.size()>0)
        {
            List<ColBean> lstColBeansDyn=new ArrayList<ColBean>();
            ColBean cbTmp;
            StringBuffer dynColOrderBuf=new StringBuffer();
            for(String colidTmp:lstColIds)
            {
                cbTmp=rbean.getDbean().getColBeanByColId(colidTmp);
                if(cbTmp==null)
                {
                    throw new WabacusRuntimeException("在报表"+rbean.getPath()+"中没有取到colid为"+colidTmp+"的ColBean对象");
                }
                dynColOrderBuf.append(colidTmp).append(";");
                lstColBeansDyn.add(cbTmp);
            }
            cdb.setLstDynOrderColBeans(lstColBeansDyn);
            if(!dragcols.equals(""))
            {
                rrequest.addParamToUrl(rbean.getId()+"_DYNCOLUMNORDER",dynColOrderBuf.toString(),true);//这里可能做了列拖动，因此需要将拖动后的顺序存入URL中。
                if(rbean.getPersonalizeObj()!=null) rbean.getPersonalizeObj().storeColOrderData(rrequest,rbean,dynColOrderBuf.toString());
            }
        }
    }

    private List<String> processDragCols(String dragcols,List<String> lstColIds)
    {
        String dragdirect=rrequest.getStringAttribute(rbean.getId()+"_DRAGDIRECT","1");
        if(!dragdirect.equals("1")&&!dragdirect.equals("-1")) dragdirect="1";
        String[] dragcolsArr=dragcols.split(";");
        if(dragcolsArr==null||dragcolsArr.length!=2)
        {
            log.warn("传入的移动列数据不合法，移动报表列失败");
            return lstColIds;
        }
        List<String> lstFromColids=new ArrayList<String>();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(UltraListReportType.KEY);
        if(dragcolsArr[0].indexOf("group_")==0)
        {
            if(ulrdbean==null)
            {
                log.warn("当前报表没有配置列分组，但传入的移动列ID为分组ID，移动报表列失败");
                return lstColIds;
            }
            UltraListReportGroupBean groupBean=ulrdbean.getGroupBeanById(dragcolsArr[0]);
            if(groupBean==null)
            {
                log.warn("没有取到id为"+dragcolsArr[0]+"的列分组，移动报表列失败");
                return lstColIds;
            }
            groupBean.getAllChildColIdsInerit(lstFromColids);
        }else
        {
            lstFromColids.add(dragcolsArr[0]);
        }
        String targetColid=dragcolsArr[1];//移动到目标列id
        if(targetColid.indexOf("group_")==0)
        {
            UltraListReportGroupBean groupBean=ulrdbean.getGroupBeanById(targetColid);
            if(groupBean==null)
            {
                log.warn("没有取到id为"+targetColid+"的列分组，移动报表列失败");
                return lstColIds;
            }
            if(dragdirect.equals("1"))
            {
                targetColid=groupBean.getLastColId(lstColIds);
            }else
            {
                targetColid=groupBean.getFirstColId(lstColIds);
            }
        }
        List<String> lstColIdsNew=new ArrayList<String>();
        for(String colidTmp:lstColIds)
        {
            if(lstFromColids.contains(colidTmp)) continue;
            if(targetColid.equals(colidTmp))
            {
                if(dragdirect.equals("1"))
                {
                    lstColIdsNew.add(colidTmp);
                    lstColIdsNew.addAll(lstFromColids);
                }else
                {//向左移，则将被拖动单元格加到目标单元格前面
                    lstColIdsNew.addAll(lstFromColids);
                    lstColIdsNew.add(colidTmp);
                }
            }else
            {
                lstColIdsNew.add(colidTmp);
            }
        }
        return lstColIdsNew;
    }

    protected void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        String rootReportId=rrequest.getStringAttribute("SAVEDSLAVEREPORT_ROOTREPORT_ID","");
        if(rootReportId.equals(rbean.getId()))
        {
            EditableReportAssistant.getInstance().doAllReportsSaveAction(rrequest);
        }
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        String rowselecttype=rrequest.getCdb(rbean.getId()).getRowSelectType();
        if(rowselecttype!=null&&!rowselecttype.trim().equals(""))
        {
            resultBuf.append(" rowselecttype=\"").append(rowselecttype).append("\"");
            if(!rowselecttype.trim().equalsIgnoreCase(Consts.ROWSELECT_NONE))
            {
                List<String> lstRowSelectCallBackFuns=rrequest.getCdb(rbean.getId()).getLstRowSelectCallBackFuncs();
                if(lstRowSelectCallBackFuns!=null&&lstRowSelectCallBackFuns.size()>0)
                {//如果用户配置了行选中的javascript回调函数
                    StringBuffer rowSelectMethodBuf=new StringBuffer();
                    rowSelectMethodBuf.append("{rowSelectMethods:[");
                    for(String callbackFunc:lstRowSelectCallBackFuns)
                    {
                        if(callbackFunc!=null&&!callbackFunc.trim().equals(""))
                        {
                            rowSelectMethodBuf.append("{value:").append(callbackFunc).append("},");
                        }
                    }
                    if(rowSelectMethodBuf.charAt(rowSelectMethodBuf.length()-1)==',') rowSelectMethodBuf.deleteCharAt(rowSelectMethodBuf.length()-1);
                    rowSelectMethodBuf.append("]}");
                    resultBuf.append(" rowSelectMethods=\"").append(rowSelectMethodBuf.toString()).append("\"");
                }
            }
        }
        return resultBuf.toString();
    }

    protected String showReportDataWithVerticalScroll()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showReportData(false));
        resultBuf.append("<div style=\"width:").append(rbean.getWidth()).append(";");
        if(Consts_Private.SCROLLSTYLE_NORMAL.equals(rbean.getScrollstyle()))
        {
            resultBuf.append("max-height:"+rbean.getScrollheight()+";overflow-x:hidden;overflow-y:auto;");
            resultBuf.append("height:expression(this.scrollHeight>parseInt('").append(rbean.getScrollheight()).append("')?'").append(
                    rbean.getScrollheight()).append("':'auto');\"");
        }else if(Consts_Private.SCROLLSTYLE_IMAGE.equals(rbean.getScrollstyle()))
        {
            resultBuf.append("overflow-x:hidden;overflow-y:hidden;\"");
            resultBuf.append("id=\"vscroll_"+rbean.getGuid()+"\"");
        }
        resultBuf.append(">");
        resultBuf.append(showReportData(true));
        resultBuf.append("</div>");
        return resultBuf.toString();
    }

    protected String showScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        int scrolltype=this.alrbean.getScrollType();
        StringBuffer resultBuf=new StringBuffer();
        if(scrolltype==AbsListReportBean.SCROLLTYPE_ALL)
        {
            return ComponentAssistant.getInstance().showComponentScrollStartPart(rbean,true,true,rbean.getScrollwidth(),rbean.getScrollheight(),
                    rbean.getScrollstyle());
        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_FIXED)
        {
            resultBuf.append("<div style=\"overflow:hidden;");
            if(rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals(""))
            {
                resultBuf.append("width:").append(rbean.getScrollwidth()).append(";");
            }
            if(rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals(""))
            {
                resultBuf.append("height:").append(rbean.getScrollheight()).append(";");
            }
            resultBuf.append("\">");
            
        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_HORIZONTAL)
        {//只显示横向滚动条
            resultBuf.append("<div style=\"width:").append(rbean.getScrollwidth()).append(";");
            if(Consts_Private.SCROLLSTYLE_NORMAL.equals(rbean.getScrollstyle()))
            {
                resultBuf.append("overflow-x:auto;overflow-y:hidden;height:expression(this.scrollHeight+15);\"");
            }else if(Consts_Private.SCROLLSTYLE_IMAGE.equals(rbean.getScrollstyle()))
            {
                resultBuf.append("overflow-x:hidden;overflow-y:hidden;\"");
                resultBuf.append(" id=\"hscroll_"+rbean.getGuid()+"\"");
            }
            resultBuf.append(">");
        }
        
        return resultBuf.toString();
    }

    protected String showScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        int scrolltype=this.alrbean.getScrollType();
        if(scrolltype==AbsListReportBean.SCROLLTYPE_ALL)
        {
            return ComponentAssistant.getInstance().showComponentScrollEndPart(true,true);
        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_FIXED||scrolltype==AbsListReportBean.SCROLLTYPE_HORIZONTAL)
        {//显示冻结行列标题的滚动条或只显示横向滚动条
            return "</div>";
        }
        return "";
    }

    public abstract String showReportData(boolean showtype);

    protected  String getDefaultNavigateKey()
    {
        return Consts.LISTREPORT_NAVIGATE_DEFAULT;
    }
    
    
    
    
    
    //        int pagenum=cdb.getPageno(rrequest);
    
    
    
    
    //        {//不分页或依赖其它报表的翻页导航栏
    //            return "";
    
    
    //        {//从数据库中没有取到数据，注意不能用recordcount==0判断，因为在可编辑列表报表中，recordcount为0时可能还要显示一页或多页的添加的行。
    
    
    //        String navigate_count_info=WabacusUtil.getI18nValue(Config.getInstance().getResources()
    
    
    
    
    //                .getString(rbean.getPageBean(),Consts.NAVIGATE_NEXTPAGE_LABEL,true),rrequest);
    
    
    
    
    //                .valueOf(pagenum));
    
    
    
    
    //
    
    
    
    
    //            rtnStr.append(" width=\""+tablewidth+"\" ");
    
    
    
    //        rtnStr.append("</td>");
    
    //        {
    
    
    
    
    //                        "</td>");
    //            }
    
    //        rtnStr.append("<td>&nbsp;</td>");
    
    
    
    //        {
    
    
    //            rtnStr.append("  <select name=\"SELEPAGENUM\"><option>1</option></select>");
    
    
    //            if(pagenum==1)
    //            {//当前为第一页
    
    
    
    
    //            }else if(pagenum==pagecount)
    //            {//当前为最后一页
    
    
    
    
    //            {//中间页
    //                rtnStr.append(WabacusUtil.getNavigateInfo(pagenum-1,navigate_prevpage_label,rbean,
    
    
    
    
    //            }
    
    
    
    //        rtnStr.append("</td></tr></table>");
    
    //    }

    protected List<ColBean> getLstDisplayColBeans()
    {
        List<ColBean> lstColBeans=this.cacheDataBean.getLstDynOrderColBeans();
        if(lstColBeans==null||lstColBeans.size()==0) lstColBeans=rbean.getDbean().getLstCols();
        return lstColBeans;
    }
    
//    /**



//     */

//    {




//            {//拦截器中返回了<tr/>的样式字符串
//                return trstyleproperty.trim();





    protected String showStatisticDataForWholeReport()
    {
        StringBuffer resultBuf=new StringBuffer();
        if(cacheDataBean.getPagesize()>0&&cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount())
        {//如果当前报表是分页显示且当前不是显示最后一页
            return "";
        }
        if(alrdbean.getStatibean()==null) return "";
        List<AbsListReportStatiColBean> lstStatiColBeans=alrdbean.getStatibean().getLstStatiColBeans();
        if(lstStatiColBeans==null||lstStatiColBeans.size()==0) return "";
        Object statiDataObj=alrdbean.getStatibean().getPojoObject();
        boolean hasStaticData=this.getStatisticData(alrdbean.getStatibean(),statiDataObj,"","");
        if(!hasStaticData) statiDataObj=null;
        resultBuf.append("<tr  class='cls-data-tr'>");
        ColDataByInterceptor coldataByInterceptor;
        String stativalue=null;
        if(rbean.getDbean().isColselect()
                ||lstStatiColBeans.size()==1
                ||(cacheDataBean.getAttributes().get("authroize_col_display")!=null&&String.valueOf(cacheDataBean.getAttributes().get("authroize_col_display")).trim()
                        .equals("false"))||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&alrbean.hasControllCol()))
        {
            resultBuf.append("<td class='cls-data-td-list' ");
            int colspan=cacheDataBean.getTotalColCount();//取到当前参与显示的总列数
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
            if(stativalue.equals("")) return "";
            if(dyntdstyleproperty!=null) resultBuf.append(dyntdstyleproperty);
            resultBuf.append(">").append(stativalue).append("</td>");
        }else
        {//统计项显示在多列中
            for(AbsListReportStatiColBean scbean:lstStatiColBeans)
            {
                //                if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,scbean.getProperty(),Consts.PERMISSION_TYPE_DISPLAY)) continue;//当前统计项没有显示权限
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
        resultBuf.append("</tr>");
        return resultBuf.toString();
    }

    protected String getStatiColDisplayValue(Object statiDataObj,AbsListReportStatiColBean scbean)
    {
        String stativalue;
        try
        {
            Object objTmp=scbean.getGetMethod().invoke(statiDataObj,new Object[] {});
            if(objTmp==null)
            {
                stativalue="";
            }else
            {
                stativalue=String.valueOf(objTmp);
            }
        }catch(Exception e)
        {
            log.error("获取报表"+rbean.getPath()+"统计数据失败",e);
            stativalue="";
        }
        return stativalue;
    }

    public String showColData(ColBean cbean,int rowidx)
    {
        if(this.lstReportData==null||this.lstReportData.size()==0) return "";
        if(rowidx==-1)
        {
            rowidx=this.lstReportData.size()-1;
        }else if(rowidx==-2)
        {
            int[] displayrowinfo=this.getDisplayRowInfo();
            if(displayrowinfo[1]<=0) return "";
            rowidx=displayrowinfo[0];
        }
        if(lstReportData.size()<=rowidx) return "";
        Object dataObj=this.lstReportData.get(rowidx);
        String strvalue=cbean.getDisplayValue(dataObj,rrequest);
        if(strvalue==null) strvalue="";
        return strvalue;
    }
    
    public void showReportOnPlainExcel(Workbook workbook)
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(this.cacheDataBean.getTotalColCount()==0) return;
        createNewSheet(workbook,10);
        if(!this.cacheDataBean.shouldBatchDataExport())
        {
            showReportDataOnPlainExcel(workbook,0);
        }else
        {
            int startNum=0;
            for(int i=0;i<this.cacheDataBean.getPagecount();i++)
            {
                if(i!=0)
                {
                    this.cacheDataBean.setPageno(i+1);
                    this.cacheDataBean.setRefreshNavigateInfoType(1);//不需要再计算页码之类
                    this.setHasLoadedDataFlag(false);
                    loadReportData();
                }
                showReportDataOnPlainExcel(workbook,startNum);
                startNum+=this.cacheDataBean.getPagesize();
            }
        }
    }

    protected void createNewSheet(Workbook workbook,int defaultcolumnwidth)
    {
        super.createNewSheet(workbook,defaultcolumnwidth);
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        int i=0;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
            
            if(cbean.getPlainexcelwidth()>1.0f)
            {
                
                this.excelSheet.setColumnWidth(i,(int)cbean.getPlainexcelwidth());
            }
            i++;
        }
        showReportTitleOnPlainExcel(workbook);
    }
    
    protected void showReportTitleOnPlainExcel(Workbook workbook)
    {
        String plainexceltitle=null;
        if(this.pedebean!=null) plainexceltitle=this.pedebean.getPlainexceltitle();
        if("none".equals(plainexceltitle)) return;//不显示标题部分
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        CellStyle titleCellStyle=StandardExcelAssistant.getInstance().getTitleCellStyleForStandardExcel(workbook);
        Row dataTitleRow=excelSheet.createRow(excelRowIdx++);
        ColDataByInterceptor coldataByInterceptor;
        int cellidx=0;
        Cell cell;
        String labelTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0)
            {
                continue;
            }
            if("column".equals(plainexceltitle))
            {
                labelTmp=cbean.getColumn();
            }else
            {
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,cbean.getLabel());
                labelTmp=ReportAssistant.getInstance().getColGroupLabel(rrequest,cbean.getLabel(),coldataByInterceptor);
            }
            cell=dataTitleRow.createCell(cellidx++);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(labelTmp);
            cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,cbean.getLabelalign()));
        }
        //        Row r=sheet.getRow(0);
        
        
    }

    private void showReportDataOnPlainExcel(Workbook workbook,int startNum)
    {
        boolean hasdata=true;
        if(lstReportData==null|lstReportData.size()==0)
        {
            hasdata=false;
            if(lstReportData==null) lstReportData=new ArrayList();
            lstReportData.add(ReportAssistant.getInstance().getReportDataPojoInstance(rbean));
        }
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        CellStyle dataCellStyle=StandardExcelAssistant.getInstance().getDataCellStyleForStandardExcel(workbook);//获取数据行的样式对象
        Cell cell;
        AbsListReportColBean alrcbeanTmp;
        Object objvalueTmp;
        for(Object dataObjTmp:lstReportData)
        {
            if(sheetsize>0&&excelRowIdx>=sheetsize)
            {
                createNewSheet(workbook,10);
            }
            Row dataRow=excelSheet.createRow(excelRowIdx++);
            int cellidx=0;
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
                alrcbeanTmp=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                cell=dataRow.createCell(cellidx++);
                boolean flag=false;
                if(cbean.isSequenceCol())
                {
                    if(hasdata)
                        cell.setCellValue(startNum+alrcbeanTmp.getSequenceStartNum());
                    else
                        cell.setCellValue("");
                }else if(!cbean.isControlCol())
                {
                    objvalueTmp=cbean.getRealTypeValue(dataObjTmp,rrequest);
                    flag=StandardExcelAssistant.getInstance().setCellValue(workbook,cbean.getValuealign(),cell,objvalueTmp,cbean.getDatatypeObj());
                }
                if(!flag) cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(dataCellStyle,cbean.getValuealign()));
            }
            startNum++;
        }
        if(hasdata) showStatisticDataInPlainExcelForWholeReport(workbook,dataCellStyle);
    }

    protected void showStatisticDataInPlainExcelForWholeReport(Workbook workbook,CellStyle dataCellStyle)
    {
        AbsListReportStatiBean statiBean=alrdbean.getStatibean();
        if(statiBean==null) return;
        if(this.cacheDataBean.getPagesize()>0&&this.cacheDataBean.getFinalPageno()!=this.cacheDataBean.getPagecount()) return;
        List<AbsListReportStatiColBean> lstStatiColBeans=statiBean.getLstStatiColBeans();
        if(lstStatiColBeans==null||lstStatiColBeans.size()==0) return;//没有配置针对整个报表的统计功能
        Object statiDataObj=statiBean.getPojoObject();
        boolean hasStaticData=this.getStatisticData(statiBean,statiDataObj,"","");
        if(!hasStaticData) statiDataObj=null;
        
        String stativalue;
        int startcolidx=0;
        int endcolidx=-1;
        CellRangeAddress region;
        for(AbsListReportStatiColBean scbean:lstStatiColBeans)
        {
            stativalue=getStatiColDisplayValue(statiDataObj,scbean);
            stativalue=Tools.replaceAll(stativalue,"&nbsp;"," ");
            stativalue=stativalue.replaceAll("<.*?\\>","");
            if(rbean.getDbean().isColselect()||lstStatiColBeans.size()==1||alrbean.hasControllCol())
            {
                startcolidx=0;
                endcolidx=cacheDataBean.getTotalColCount()-1;
                int deltaCount=0;
                if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
                {//对于树形分组的报表，所有参与树形分组的列，在显示纯数据到Excel时，每个要显示成一个独立的列
                    deltaCount=alrdbean.getRowGroupColsNum()-1;
                }
                endcolidx=endcolidx+deltaCount;
            }else
            {
                startcolidx=endcolidx+1;
                endcolidx=startcolidx+scbean.getPlainexcel_colspan()-1;
            }
            region=new CellRangeAddress(excelRowIdx,excelRowIdx,startcolidx,endcolidx);
            StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,excelSheet,region,dataCellStyle,stativalue);
        }
        excelRowIdx++;
    }
    
    protected void showReportOnPdfWithoutTpl()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(this.cacheDataBean.getTotalColCount()==0) return;
        createNewPdfPage();
        showDataHeaderOnPdf();
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
                    this.cacheDataBean.setRefreshNavigateInfoType(1);//不需要再计算页码之类
                    this.setHasLoadedDataFlag(false);
                    loadReportData();
                }
                showReportDataOnPdf();
            }
        }
    }
    
    private float[] colwidthArr;
    
    protected void createNewPdfPage()
    {
        super.createNewPdfPage();
        if(this.totalcolcount<=0) return;
        if(colwidthArr==null)
        {
            colwidthArr=new float[this.totalcolcount];
            float totalconfigwidth=0f;
            int nonconfigwidthColcnt=0;//存放没有配置dataexportwidth的列数
            List<ColBean> lstColBeans=this.getLstDisplayColBeans();
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
                if(cbean.getPdfwidth()>0.1f)
                {
                    totalconfigwidth+=cbean.getPdfwidth();
                }else
                {//没有在<col/>中配置dataexportwidth
                    nonconfigwidthColcnt++;
                }
            }
            float nonconfigcolwidth=0f;
            if(nonconfigwidthColcnt==0)
            {
                pdfwidth=totalconfigwidth;
                this.pdfDataTable.setTotalWidth(totalconfigwidth);
            }else
            {//存在没有配置导出宽度的列
                if(pdfwidth<=totalconfigwidth)
                {
                    nonconfigcolwidth=50f;
                    pdfwidth=totalconfigwidth+nonconfigcolwidth*nonconfigwidthColcnt;
                    this.pdfDataTable.setTotalWidth(pdfwidth);
                }else
                {
                    nonconfigcolwidth=(pdfwidth-totalconfigwidth)/nonconfigwidthColcnt;
                }
            }
            int i=0;
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
                if(cbean.getPdfwidth()>0.1f)
                {
                    colwidthArr[i]=cbean.getPdfwidth();
                }else
                {
                    colwidthArr[i]=nonconfigcolwidth;
                }
                i++;
            }
        }
        try
        {
            this.pdfDataTable.setWidths(colwidthArr);
        }catch(DocumentException e)
        {
            throw new WabacusRuntimeException("导出报表"+rbean.getPath()+"的数据到PDF文件失败",e);
        }
    }

    protected void showDataHeaderOnPdf()
    {
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        ColDataByInterceptor coldataByInterceptor;
        String labelTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,cbean.getLabel());
            labelTmp=ReportAssistant.getInstance().getColGroupLabel(rrequest,cbean.getLabel(),coldataByInterceptor);
            addDataHeaderCell(cbean,labelTmp,1,1,getPdfCellAlign(cbean.getLabelalign(),Element.ALIGN_CENTER));
        }
    }
    
    private void showReportDataOnPdf()
    {
        if(lstReportData==null|lstReportData.size()==0)
        {
            if(lstReportData==null) lstReportData=new ArrayList();
            lstReportData.add(ReportAssistant.getInstance().getReportDataPojoInstance(rbean));//放一个空POJO，这样就会显示一行空数据行
        }
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        ColDataByInterceptor coldataByInterceptor;
        AbsListReportColBean alrcbeanTmp;
        String valueTmp;
        for(Object dataObjTmp:lstReportData)
        {
            if(this.pdfpagesize>0&&this.pdfrowindex!=0&&this.pdfrowindex%this.pdfpagesize==0)
            {
                this.createNewPdfPage();
                if(this.isFullpagesplit) showDataHeaderOnPdf();
            }
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
                valueTmp="";
                if(cbean.isSequenceCol())
                {
                    alrcbeanTmp=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                    valueTmp=String.valueOf(this.pdfrowindex+alrcbeanTmp.getSequenceStartNum());
                }else if(!cbean.isControlCol())
                {
                    valueTmp=cbean.getDisplayValue(dataObjTmp,rrequest);
                    coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,this.pdfrowindex,valueTmp);
                    if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                    {
                        valueTmp=coldataByInterceptor.getDynvalue();
                    }
                }
                addDataCell(cbean,valueTmp,1,1,getPdfCellAlign(cbean.getValuealign(),Element.ALIGN_CENTER));
            }
            this.pdfrowindex++;
        }
        if(lstReportData!=null&&lstReportData.size()>0) showStatisticDataOnPdfForWholeReport();
    }
    
    protected int getTotalColCount()
    {
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        int cnt=0;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
            if(cbean.isControlCol()) continue;
            cnt++;
        }
        return cnt;
    }
    
    protected void showStatisticDataOnPdfForWholeReport()
    {
        AbsListReportStatiBean statiBean=alrdbean.getStatibean();
        if(statiBean==null) return;
        if(this.cacheDataBean.getPagesize()>0&&this.cacheDataBean.getFinalPageno()!=this.cacheDataBean.getPagecount()) return;//如果是分批导出，且现在还没导到最后一批
        List<AbsListReportStatiColBean> lstStatiColBeans=statiBean.getLstStatiColBeans();
        if(lstStatiColBeans==null||lstStatiColBeans.size()==0) return;
        Object statiDataObj=statiBean.getPojoObject();
        boolean hasStaticData=this.getStatisticData(statiBean,statiDataObj,"","");
        if(!hasStaticData) statiDataObj=null;
        
        String stativalue;
        int startcolidx=0;
        int endcolidx=-1;
        for(AbsListReportStatiColBean scbean:lstStatiColBeans)
        {
            stativalue=getStatiColDisplayValue(statiDataObj,scbean);
            stativalue=Tools.replaceAll(stativalue,"&nbsp;"," ");
            stativalue=stativalue.replaceAll("<.*?\\>","");
            if(rbean.getDbean().isColselect()||lstStatiColBeans.size()==1||alrbean.hasControllCol())
            {
                startcolidx=0;
                endcolidx=cacheDataBean.getTotalColCount()-1;//取到当前参与显示的总列数
                int deltaCount=0;
                if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
                {
                    deltaCount=alrdbean.getRowGroupColsNum()-1;
                }
                endcolidx=endcolidx+deltaCount;
            }else
            {
                startcolidx=endcolidx+1;
                endcolidx=startcolidx+scbean.getPlainexcel_colspan()-1;
            }
            addDataCell(scbean,stativalue,1,endcolidx-startcolidx,Element.ALIGN_LEFT);
        }
    }
    
    protected Map<ColBean,List<RowGroupDataBean>> parseRowGroupColData(Map<Integer,List<RowGroupDataBean>> mAllParentDataBeansForPerDataRow)
    {
        DisplayBean disbean=rbean.getDbean();
        List<String> lstRowGroupColsColumn=alrdbean.getLstRowgroupColsColumn();
        Map<ColBean,List<RowGroupDataBean>> mResults=new HashMap<ColBean,List<RowGroupDataBean>>();
        Object dataObj;
        List<RowGroupDataBean> lstrgdb;
        RowGroupDataBean rgdbean;
        List<Integer> lstParentDisplayRowIdx=null;
        List<Integer> lstCurrentDisplayRowIdx;

        Map<String,AbsListReportStatiRowGroupBean> mStatiRowGroupBeans=null;
        if(alrdbean.getStatibean()!=null)
        {//配置有针对行分组的统计功能
            mStatiRowGroupBeans=alrdbean.getStatibean().getMStatiRowGroupBeans();
        }
        int[] displayrowinfo=getDisplayRowInfo();
        int layer=0;
        ColBean cbeanTemp;
        for(String colcolumn:lstRowGroupColsColumn)
        {
            if(colcolumn==null) continue;
            cbeanTemp=disbean.getColBeanByColColumn(colcolumn);
            lstrgdb=new ArrayList<RowGroupDataBean>();
            mResults.put(cbeanTemp,lstrgdb);
            lstCurrentDisplayRowIdx=new ArrayList<Integer>();
            for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
            {
                dataObj=lstReportData.get(i);
                String value=cbeanTemp.getDisplayValue(dataObj,rrequest);
                value=value==null?"":value.trim();
                if(lstrgdb.size()==0||(lstParentDisplayRowIdx!=null&&lstParentDisplayRowIdx.contains(i))
                        ||!(value.equals(lstrgdb.get(lstrgdb.size()-1).getDisplayvalue())))
                {
                    lstCurrentDisplayRowIdx.add(i);
                    lstrgdb.add(createRowGroupDataObj(mAllParentDataBeansForPerDataRow,dataObj,alrdbean.getStatibean(),layer,cbeanTemp,i,value));
                }else
                {
                    rgdbean=lstrgdb.get(lstrgdb.size()-1);
                    rgdbean.setRowspan(rgdbean.getRowspan()+1);
                    rgdbean.addChildDataRowIdx("tr_"+i);
                    List<RowGroupDataBean> lstParentDataBeans=mAllParentDataBeansForPerDataRow.get(i);//取出间接包括此行的所有父分组节点
                    if(lstParentDataBeans==null)
                    {
                        lstParentDataBeans=new ArrayList<RowGroupDataBean>();
                        mAllParentDataBeansForPerDataRow.put(i,lstParentDataBeans);
                    }
                    lstParentDataBeans.add(rgdbean);
                    if(alrdbean.getRowgrouptype()==1&&mStatiRowGroupBeans!=null)
                    {
                        if(mStatiRowGroupBeans.containsKey(cbeanTemp.getColumn()))
                        {
                            ((CommonRowGroupDataBean)lstrgdb.get(lstrgdb.size()-1)).setDisplaystatidata_rowidx(i);
                        }
                    }
                }
            }
            lstParentDisplayRowIdx=lstCurrentDisplayRowIdx;
            layer++;
        }
        return mResults;
    }

    private RowGroupDataBean createRowGroupDataObj(Map<Integer,List<RowGroupDataBean>> mAllParentDataBeansForPerDataRow,Object dataObj,
            AbsListReportStatiBean statiBean,int layer,ColBean cbean,int rowidx,String value)
    {
        RowGroupDataBean rgdbean=null;
        if(alrdbean.getRowgrouptype()==1)
        {
            rgdbean=new CommonRowGroupDataBean(cbean,value,rowidx,layer);
        }else if(alrdbean.getRowgrouptype()==2)
        {
            rgdbean=new TreeRowGroupDataBean(cbean,value,rowidx,layer);
        }
        rgdbean.setValue(statiBean,dataObj,mAllParentDataBeansForPerDataRow);
        return rgdbean;
    }

    protected abstract class RowGroupDataBean
    {
        protected ColBean cbean;//分组的列对象

        protected int layer;

        protected int display_rowidx;

        protected String displayvalue;

        protected int rowspan;

        protected String idSuffix;

        protected String parentGroupIdSuffix;

        protected List<String> lstAllChildDataRowIdxs=new ArrayList<String>();//对于普通分组，此属性只在可编辑分组报表中会用上

        private List<String> lstAllChildGroupIdxs=new ArrayList<String>();

        private Object statiDataObj;

        private List<AbsListReportStatiColBean> lstScolbeans;

        public RowGroupDataBean(ColBean cbean,String value,int rowidx,int layer)
        {
            this.cbean=cbean;
            this.displayvalue=value;
            this.display_rowidx=rowidx;
            this.layer=layer;
        }

        public int getLayer()
        {
            return layer;
        }

        public void setLayer(int layer)
        {
            this.layer=layer;
        }

        public int getDisplay_rowidx()
        {
            return display_rowidx;
        }

        public void setDisplay_rowidx(int display_rowidx)
        {
            this.display_rowidx=display_rowidx;
        }

        public ColBean getCbean()
        {
            return cbean;
        }

        public void setCbean(ColBean cbean)
        {
            this.cbean=cbean;
        }

        public String getDisplayvalue()
        {
            return displayvalue;
        }

        public void setDisplayvalue(String displayvalue)
        {
            this.displayvalue=displayvalue;
        }

        public int getRowspan()
        {
            return rowspan;
        }

        public void setRowspan(int rowspan)
        {
            this.rowspan=rowspan;
        }

        public String getIdSuffix()
        {
            return idSuffix;
        }

        public void setIdSuffix(String idSuffix)
        {
            this.idSuffix=idSuffix;
        }

        public String getParentGroupIdSuffix()
        {
            return parentGroupIdSuffix;
        }

        public void setParentGroupIdSuffix(String parentGroupIdSuffix)
        {
            this.parentGroupIdSuffix=parentGroupIdSuffix;
        }

        public List<String> getLstAllChildDataRowIdxs()
        {
            return lstAllChildDataRowIdxs;
        }

        public void addChildDataRowIdx(String idx)
        {
            lstAllChildDataRowIdxs.add(idx);
        }

        public void addChildGroupIdx(String idx)
        {
            lstAllChildGroupIdxs.add(idx);
        }

        public List<String> getLstAllChildGroupIdxs()
        {
            return lstAllChildGroupIdxs;
        }

        public Object getStatiDataObj()
        {
            return statiDataObj;
        }

        public void setStatiDataObj(Object statiDataObj)
        {
            this.statiDataObj=statiDataObj;
        }

        public List<AbsListReportStatiColBean> getLstScolbeans()
        {
            return lstScolbeans;
        }

        public void setLstScolbeans(List<AbsListReportStatiColBean> lstScolbeans)
        {
            this.lstScolbeans=lstScolbeans;
        }

        public String getAllChildDataRowIdxsAsString()
        {
            if(lstAllChildDataRowIdxs==null||lstAllChildDataRowIdxs.size()==0) return "";
            StringBuffer sbuffer=new StringBuffer();
            for(String childdataidx:lstAllChildDataRowIdxs)
            {
                sbuffer.append(childdataidx).append(";");
            }
            while(sbuffer.length()>0&&sbuffer.charAt(sbuffer.length()-1)==';')
            {
                sbuffer.deleteCharAt(sbuffer.length()-1);
            }
            return sbuffer.toString();
        }

        public String getAllChildGroupIdxsAsString()
        {
            if(lstAllChildGroupIdxs==null||lstAllChildGroupIdxs.size()==0) return "";
            StringBuffer sbuffer=new StringBuffer();
            for(String childgroupidx:lstAllChildGroupIdxs)
            {
                sbuffer.append(childgroupidx).append(";");
            }
            while(sbuffer.length()>0&&sbuffer.charAt(sbuffer.length()-1)==';')
            {
                sbuffer.deleteCharAt(sbuffer.length()-1);
            }
            return sbuffer.toString();
        }

        public void setValue(AbsListReportStatiBean statiBean,Object dataObj,Map<Integer,List<RowGroupDataBean>> mParentDataBeansForPerDataRow)
        {
            this.addChildDataRowIdx("tr_"+display_rowidx);
            List<RowGroupDataBean> lstParentDataBeans=mParentDataBeansForPerDataRow.get(display_rowidx);
            if(lstParentDataBeans==null)
            {
                lstParentDataBeans=new ArrayList<RowGroupDataBean>();
                mParentDataBeansForPerDataRow.put(display_rowidx,lstParentDataBeans);
            }else
            {
                this.setParentGroupIdSuffix(lstParentDataBeans.get(lstParentDataBeans.size()-1).getIdSuffix());//此数据行对应的最后一个父分组行对象即为当前分组行的直接父分组行对象，记下它的ID后缀
            }
            for(RowGroupDataBean rgdbean:lstParentDataBeans)
            {
                rgdbean.addChildGroupIdx(this.getIdSuffix());
            }
            lstParentDataBeans.add(this);
            if(statiBean!=null&&statiBean.getMStatiRowGroupBeans()!=null&&statiBean.getMStatiRowGroupBeans().containsKey(cbean.getColumn()))
            {
                getRowGroupStatisticData(mParentDataBeansForPerDataRow.get(display_rowidx),dataObj,statiBean,cbean,this);
            }
        }
    }

    protected class CommonRowGroupDataBean extends RowGroupDataBean
    {
        private int displaystatidata_rowidx;

        public int getDisplaystatidata_rowidx()
        {
            return displaystatidata_rowidx;
        }

        public void setDisplaystatidata_rowidx(int displaystatidata_rowidx)
        {
            this.displaystatidata_rowidx=displaystatidata_rowidx;
        }

        public CommonRowGroupDataBean(ColBean cbean,String value,int rowidx,int layer)
        {
            super(cbean,value,rowidx,layer);
        }

        public void setValue(AbsListReportStatiBean statiBean,Object dataObj,Map<Integer,List<RowGroupDataBean>> mParentDataBeansForPerDataRow)
        {
            this.setRowspan(1);
            this.setIdSuffix(cbean.getReportBean().getGuid()+"_"+cbean.getProperty()+"__td"+display_rowidx);
            super.setValue(statiBean,dataObj,mParentDataBeansForPerDataRow);
            List<RowGroupDataBean> lstParentDataBeans=mParentDataBeansForPerDataRow.get(display_rowidx);
            if(statiBean!=null&&statiBean.getMStatiRowGroupBeans()!=null&&statiBean.getMStatiRowGroupBeans().containsKey(cbean.getColumn()))
            {
                this.setDisplaystatidata_rowidx(display_rowidx);//显示当前分组行的统计信息的记录号先设置为rowidx，如果它包括多行，后面还会不断地修正，一直到它包括的最后一条记录，才是真正显示当前分组统计信息的行
                for(RowGroupDataBean rgdbeanTmp:lstParentDataBeans)
                {
                    rgdbeanTmp.setRowspan(rgdbeanTmp.getRowspan()+1);
                }
            }
        }
    }

    protected class TreeRowGroupDataBean extends RowGroupDataBean
    {

        public TreeRowGroupDataBean(ColBean cbean,String value,int rowidx,int layer)
        {
            super(cbean,value,rowidx,layer);
        }

        public void setValue(AbsListReportStatiBean statiBean,Object dataObj,Map<Integer,List<RowGroupDataBean>> mParentDataBeansForPerDataRow)
        {
            this.setRowspan(2);
            this.setIdSuffix("trgroup_"+layer+"_"+display_rowidx);
            super.setValue(statiBean,dataObj,mParentDataBeansForPerDataRow);
            List<RowGroupDataBean> lstParentDataBeans=mParentDataBeansForPerDataRow.get(display_rowidx);
            for(RowGroupDataBean trgdbean:lstParentDataBeans)
            {
                if(trgdbean==this) continue;//当前节点除外
                trgdbean.setRowspan(trgdbean.getRowspan()+1);
            }
        }
    }

    private void getRowGroupStatisticData(List lstParentDataBeans,Object dataObj,AbsListReportStatiBean statiBean,ColBean cbean,RowGroupDataBean rgdatabean)
    {
        if(statiBean==null||statiBean.getMStatiRowGroupBeans()==null) return;
        AbsListReportStatiRowGroupBean srgbean=statiBean.getMStatiRowGroupBeans().get(cbean.getColumn());
        if(srgbean==null) return;
        String statiSqlGroupBy=srgbean.getStatiSqlGroupby();
        if(statiSqlGroupBy==null||statiSqlGroupBy.trim().equals("")) return;
        rgdatabean.setLstScolbeans(srgbean.getLstStatiColBeans());
        try
        {
            Object statiDataObj=statiBean.getPojoObject();
            ColBean cbeanGroupTmp;
            for(Object beanTmp:lstParentDataBeans)
            {
                cbeanGroupTmp=((RowGroupDataBean)beanTmp).getCbean();
                String convalue=ReportAssistant.getInstance().getPropertyValueAsString(dataObj,cbeanGroupTmp.getColumn()+"_old",
                        cbeanGroupTmp.getDatatypeObj());
                convalue=convalue==null?"":convalue.trim();
                statiSqlGroupBy=Tools.replaceAll(statiSqlGroupBy,"#"+cbeanGroupTmp.getColumn()+"#",convalue);
                String setMethodName="set"+cbeanGroupTmp.getColumn().substring(0,1).toUpperCase()+cbeanGroupTmp.getColumn().substring(1);
                Method setMethod=statiBean.getPojoclass().getMethod(setMethodName,new Class[] { String.class });//存放分组列的数据都是字符串类型
                setMethod.invoke(statiDataObj,new Object[]{convalue});
            }
            boolean hasStatiData=getStatisticData(statiBean,statiDataObj,statiSqlGroupBy,cbean.getColumn());
            if(!hasStatiData) statiDataObj=null;
            rgdatabean.setStatiDataObj(statiDataObj);
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"针对分组列"+cbean.getColumn()+"的统计数据失败",e);
        }
    }

    protected boolean getStatisticData(AbsListReportStatiBean statiBean,Object statiDataObj,String groupbyClause,String rowgroupcolumn)
    {
        try
        {
            boolean hasStatisticData=false;
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            if(statiBean.getLstStatitemBeansWithoutCondition()!=null&&statiBean.getLstStatitemBeansWithoutCondition().size()>0)
            {
                String statisql=statiBean.getStatiSqlWithoutCondition();
                if(groupbyClause!=null&&!groupbyClause.trim().equals(""))
                {
                    statisql=statisql+"  "+groupbyClause;
                }
                Object objTmp=impISQLType.getResultSet(rrequest,this,this,statisql);
                if(!(objTmp instanceof ResultSet))
                {
                    throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"数据失败，在加载数据的前置动作中，如果是统计数据的SQL语句，则只能返回SQL语句或ResultSet，不能返回加载好的List对象");
                }
                ResultSet rs=(ResultSet)objTmp;
                if(rs.next())
                {
                    List<AbsListReportStatItemBean> lstStatitemBeans=statiBean.getLstStatitemBeansWithoutCondition();
                    for(AbsListReportStatItemBean alrsibeanTmp:lstStatitemBeans)
                    {
                        Object colVal=alrsibeanTmp.getDatatypeObj().getColumnValue(rs,alrsibeanTmp.getProperty(),dbtype);
                        alrsibeanTmp.getSetMethod().invoke(statiDataObj,new Object[] { colVal });
                    }
                    hasStatisticData=true;
                }
                rs.close();
            }
            if(statiBean.getLstStatitemBeansWithCondition()!=null&&statiBean.getLstStatitemBeansWithCondition().size()>0)
            {
                List<AbsListReportStatItemBean> lstStatitemBeans=statiBean.getLstStatitemBeansWithCondition();
                String statisql=statiBean.getStatiSqlWithCondition();
                String sqlTmp;
                for(AbsListReportStatItemBean alrsibeanTmp:lstStatitemBeans)
                {//循环每个统计项
                    sqlTmp=Tools.replaceAll(statisql,"%SELECTEDCOLUMNS%",alrsibeanTmp.getValue()+" as "+alrsibeanTmp.getProperty());
                    String realcondition=getRealStatisticItemConditionValues(alrsibeanTmp.getLstConditions());
                    if(realcondition.trim().equals(""))
                    {
                        sqlTmp=Tools.replaceAll(sqlTmp,"%CONDITION%","");
                    }else
                    {
                        sqlTmp=Tools.replaceAll(sqlTmp,"%CONDITION%"," where "+realcondition);
                    }
                    if(groupbyClause!=null&&!groupbyClause.trim().equals(""))
                    {
                        sqlTmp=sqlTmp+"  "+groupbyClause;
                    }
                    Object objTmp=impISQLType.getResultSet(rrequest,this,this,sqlTmp);
                    if(!(objTmp instanceof ResultSet))
                    {
                        throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"数据失败，在加载数据的前置动作中，如果是统计数据的SQL语句，则只能返回SQL语句或ResultSet，不能返回加载好的List对象");
                    }
                    ResultSet rs=(ResultSet)objTmp;
                    if(rs.next())
                    {
                        Object colVal=alrsibeanTmp.getDatatypeObj().getColumnValue(rs,alrsibeanTmp.getProperty(),dbtype);
                        alrsibeanTmp.getSetMethod().invoke(statiDataObj,new Object[] { colVal });
                        hasStatisticData=true;
                    }
                    rs.close();
                }
            }
            if(!hasStatisticData) return false;
            Method formatMethod=statiBean.getPojoclass().getMethod("format",new Class[] { ReportRequest.class, ReportBean.class, String.class });
            formatMethod.invoke(statiDataObj,new Object[] { rrequest, rbean, rowgroupcolumn });
            return true;
        }catch(SQLException sqle)
        {
            throw new WabacusRuntimeException("查询报表"+rbean.getPath()+"统计数据失败",sqle);
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("设置报表"+rbean.getPath()+"统计数据到POJO对象中失败",e);
        }
    }
    
    private String getRealStatisticItemConditionValues(List<ConditionBean> lstConditionBeans)
    {
        if(lstConditionBeans==null||lstConditionBeans.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        String conditionValTmp;
        for(ConditionBean cbeanTmp:lstConditionBeans)
        {
            if(cbeanTmp.isConstant())
            {//是常量查询条件表达式
                conditionValTmp=cbeanTmp.getConditionExpression().getValue();
                
            }else
            {
                conditionValTmp=cbeanTmp.getDynamicConditionvalueForSql(rrequest,-1);
            }
            if(conditionValTmp!=null&&!conditionValTmp.trim().equals("")) resultBuf.append(conditionValTmp).append(" and ");
        }
        conditionValTmp=resultBuf.toString().trim();
        if(conditionValTmp.endsWith(" and"))
        {
            conditionValTmp=conditionValTmp.substring(0,conditionValTmp.length()-4);
        }
        return conditionValTmp;
    }
    
    public int beforeReportLoading(ReportBean rbean,List<XmlElementBean> lstEleReportBeans)
    {
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        AbsListReportBean alrbean=(AbsListReportBean)rbean.getExtendConfigDataForReportType(KEY);
        if(alrbean==null)
        {
            alrbean=new AbsListReportBean(rbean);
            rbean.setExtendConfigDataForReportType(KEY,alrbean);
        }
        String fixedcols=eleReportBean.attributeValue("fixedcols");
        if(fixedcols!=null)
        {
            fixedcols=fixedcols.trim();
            if(fixedcols.equals(""))
            {
                alrbean.setFixedcols(0);
            }else
            {
                try
                {
                    alrbean.setFixedcols(Integer.parseInt(fixedcols));
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+rbean.getPath()+"的<report/>标签上fixedcols属性配置的值"+fixedcols+"为无效数字，"+e.toString());
                    alrbean.setFixedcols(0);
                }
            }
        }
        if(alrbean.getFixedcols()<0) alrbean.setFixedcols(0);
        String fixedrows=eleReportBean.attributeValue("fixedrows");
        if(fixedrows!=null)
        {
            fixedrows=fixedrows.trim();
            if(fixedrows.equals(""))
            {
                alrbean.setFixedrows(0);
            }else if(fixedrows.toLowerCase().equals("title"))
            {
                alrbean.setFixedrows(Integer.MAX_VALUE);
            }else
            {
                try
                {
                    alrbean.setFixedrows(Integer.parseInt(fixedrows));
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+rbean.getPath()+"的<report/>标签上fixedrows属性配置的值"+fixedrows+"为无效数字，"+e.toString());
                    alrbean.setFixedrows(0);
                }
            }
        }
        if(alrbean.getFixedrows()<0) alrbean.setFixedrows(0);
        String rowselect=eleReportBean.attributeValue("rowselect");
        if(rowselect!=null)
        {
            rowselect=rowselect.toLowerCase().trim();
            if(rowselect.equals(""))
            {
                alrbean.setRowSelectType(null);
            }else
            {
                if(!Consts.lstAllRowSelectTypes.contains(rowselect))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的rowselect属性"+rowselect+"不合法");
                }
                alrbean.setRowSelectType(rowselect);
            }
        }
        String rowselectcallback=eleReportBean.attributeValue("selectcallback");
        if(rowselectcallback!=null)
        {
            rowselectcallback=rowselectcallback.trim();
            if(rowselectcallback.equals(""))
            {
                alrbean.setLstRowSelectCallBackFuncs(null);
            }else
            {
                List<String> lstTemp=Tools.parseStringToList(rowselectcallback,";");
                for(String strfun:lstTemp)
                {
                    if(strfun==null||strfun.trim().equals("")) continue;
                    alrbean.addRowSelectCallBackFunc(strfun.trim());
                }
            }
        }
        String rowordertype=eleReportBean.attributeValue("rowordertype");
        if(rowordertype!=null)
        {
            rowordertype=rowordertype.toLowerCase().trim();
            if(rowordertype.equals(""))
            {
                alrbean.setLoadStoreRoworderObject(null);
                alrbean.setLstRoworderTypes(null);
            }else
            {
                List<String> lstRoworderTypes=new ArrayList<String>();
                List<String> lstTmp=Tools.parseStringToList(rowordertype,"|");
                for(String roworderTmp:lstTmp)
                {
                    if(roworderTmp==null||roworderTmp.trim().equals("")) continue;
                    roworderTmp=roworderTmp.trim();
                    if(!Consts.lstAllRoworderTypes.contains(roworderTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的rowordertype属性"+roworderTmp+"不支持");
                    }
                    if(!lstRoworderTypes.contains(roworderTmp)) lstRoworderTypes.add(roworderTmp);
                }
                if(lstRoworderTypes.size()==0)
                {
                    alrbean.setLoadStoreRoworderObject(null);
                    alrbean.setLstRoworderTypes(null);
                }else
                {
                    alrbean.setLstRoworderTypes(lstRoworderTypes);
                    String roworderclass=eleReportBean.attributeValue("roworderclass");
                    if(roworderclass!=null)
                    {
                        roworderclass=roworderclass.trim();
                        if(roworderclass.equals(""))
                        {
                            alrbean.setLoadStoreRoworderObject(null);
                        }else
                        {
                            Object obj=null;
                            try
                            {
                                obj=ResourceUtils.loadClass(roworderclass).newInstance();
                            }catch(Exception e)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，无法实例化"+roworderclass+"类对象",e);
                            }
                            if(!(obj instanceof IListReportRoworderPersistence))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，roworderclass属性配置的类"+roworderclass+"没有实现"
                                        +IListReportRoworderPersistence.class.getName()+"接口");
                            }
                            alrbean.setLoadStoreRoworderObject((IListReportRoworderPersistence)obj);
                        }
                    }
                    if(alrbean.getLoadStoreRoworderObject()==null&&Config.default_roworder_object==null)
                    {//没有配置局默认的处理行排序的对象
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                                +"失败，没有在wabacus.cfg.xml通过default-roworderclass配置项配置全局默认处理行排序的类，因此必须在其<report/>中配置roworderclass属性指定处理本报表行排序的类");

                    }
                }
            }
        }
        return 1;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        XmlElementBean eleColBean=lstEleColBeans.get(0);
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)colbean.getParent().getExtendConfigDataForReportType(KEY);
        if(alrdbean==null)
        {
            alrdbean=new AbsListReportDisplayBean(colbean.getParent());
            colbean.getParent().setExtendConfigDataForReportType(KEY,alrdbean);
        }
        AbsListReportColBean alrcbean=(AbsListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
        if(alrcbean==null)
        {
            alrcbean=new AbsListReportColBean(colbean);
            colbean.setExtendConfigDataForReportType(KEY,alrcbean);
        }
        ReportBean reportbean=colbean.getReportBean();
        String column=colbean.getColumn().trim();
        if(colbean.isNonValueCol())
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"为数据自动列表报表，不允许<col/>标签的column属性配置为"+Consts_Private.NON_VALUE);
        }
        if(colbean.isSequenceCol())
        {
            String sequence=column.substring(1,column.length()-1);
            int start=1;
            int idx=sequence.indexOf(":");
            if(idx>0)
            {
                sequence=sequence.substring(idx+1);
                try
                {
                    if(!sequence.trim().equals("")) start=Integer.parseInt(sequence);
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+reportbean.getPath()+"配置的序号列"+colbean.getColumn()+"中的起始序号不是合法数字",e);
                    start=1;
                }
            }
            alrcbean.setSequenceStartNum(start);
        }
        if(!colbean.isSequenceCol()&&!colbean.isControlCol()&&(colbean.getProperty()==null||colbean.getProperty().trim().equals("")))
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"为数据自动列表报表，不允许<col/>标签的property属性为空");
        }
        String width=eleColBean.attributeValue("width");
        String align=eleColBean.attributeValue("align");
        String clickorderby=eleColBean.attributeValue("clickorderby");
        String filter=eleColBean.attributeValue("filter");

        String rowselectvalue=eleColBean.attributeValue("rowselectvalue");//当前<col/>是否需要在行选中的javascript回调函数中使用，如果设置为true，则在显示当前<col/>时，会在<td/>中显示一个名为value属性，值为当前列的值
        String rowgroup=eleColBean.attributeValue("rowgroup");//是否参与普通行分组
        String treerowgroup=eleColBean.attributeValue("treerowgroup");

        if(filter!=null)
        {
            filter=filter.trim();
            if(filter.equals("")||filter.equalsIgnoreCase("false"))
            {
                alrcbean.setFilterBean(null);
            }else
            {
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，不能在column为非数据的<col/>中配置过滤列，即不能配置filter属性");
                }
                AbsListReportFilterBean filterBean=new AbsListReportFilterBean(colbean);
                if(Tools.isDefineKey("condition",filter))
                {
                    filterBean.setConditionname(Tools.getRealKeyByDefine("condition",filter).trim());
                }else if(!filter.toLowerCase().equals("true"))
                {
                    filterBean.setFilterColumnExpression(filter);
                }
                String filterwidth=eleColBean.attributeValue("filterwidth");
                if(filterwidth!=null)
                {
                    filterBean.setFilterwidth(filterwidth.trim());
                }
                String filterformat=eleColBean.attributeValue("filterformat");
                if(filterformat!=null&&!filterformat.trim().equals(""))
                {
                    filterformat=filterformat.trim();
                    filterBean.setFilterformat(filterformat);
                    filterBean.setFormatClass(reportbean.getFormatMethodClass(filterformat,new Class[] { ReportBean.class, String[].class }));
                    try
                    {
                        filterBean.setFormatMethod(filterBean.getFormatClass().getMethod(filterformat,
                                new Class[] { ReportBean.class, String[].class }));
                    }catch(Exception e)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，无法得到"+filterformat+"格式化方法对象",e);
                    }
                }
                alrcbean.setFilterBean(filterBean);
            }
        }

        if(width!=null&&!width.trim().equals(""))
        {
            if(Tools.getPropertyValueByName("width",colbean.getValuestyleproperty(),true)==null)
                colbean.setValuestyleproperty(colbean.getValuestyleproperty()+" width='"+width+"' ");
            if(Tools.getPropertyValueByName("width",colbean.getLabelstyleproperty(),true)==null)
                colbean.setLabelstyleproperty(colbean.getLabelstyleproperty()+" width='"+width+"' ");
        }
        if(colbean.getValuealign()==null||colbean.getValuealign().trim().equals(""))
        {//如果没有在valuestyleproperty中指定align，则使用<col/>中配置的align属性
            if(align==null)
            {//在<col/>中没有配置align
                if(treerowgroup!=null&&treerowgroup.trim().equals("true"))
                {
                    align="left";
                }else
                {
                    align="center";//默认为center
                }
            }
            colbean.setValuestyleproperty(colbean.getValuestyleproperty()+" align='"+align+"' ");
            colbean.setValuealign(align);
        }
        if(colbean.getPrintlabelstyleproperty()==null||colbean.getPrintlabelstyleproperty().trim().equals(""))
        {
            colbean.setPrintlabelstyleproperty(colbean.getLabelstyleproperty());
        }
        if(colbean.getPrintvaluestyleproperty()==null||colbean.getPrintvaluestyleproperty().trim().equals(""))
        {
            colbean.setPrintvaluestyleproperty(colbean.getValuestyleproperty());
        }
        String printwidth=colbean.getPrintwidth();
        if(printwidth!=null&&!printwidth.trim().equals(""))
        {
            String printlabelstyleproperty=colbean.getPrintlabelstyleproperty();
            if(printlabelstyleproperty==null) printlabelstyleproperty="";
            printlabelstyleproperty=Tools.removePropertyValueByName("width",printlabelstyleproperty);
            printlabelstyleproperty=printlabelstyleproperty+" width=\""+printwidth+"\"";
            colbean.setPrintlabelstyleproperty(printlabelstyleproperty);
            String printvaluestyleproperty=colbean.getPrintvaluestyleproperty();
            if(printvaluestyleproperty==null) printvaluestyleproperty="";
            printvaluestyleproperty=Tools.removePropertyValueByName("width",printvaluestyleproperty);
            printvaluestyleproperty=printvaluestyleproperty+" width=\""+printwidth+"\"";
            colbean.setPrintvaluestyleproperty(printvaluestyleproperty);
        }
        if(clickorderby!=null)
        {
            clickorderby=clickorderby.toLowerCase().trim();
            if(!clickorderby.equals("true")&&!clickorderby.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，column为"+colbean.getColumn()
                        +"的<col/>的clickorderby属性配置不合法，必须配置为true或false");
            }
            if(clickorderby.equals("true"))
            {
                if(colbean.getColumn()==null||colbean.getColumn().trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()
                            +"失败，存在在没有配置column属性的<col/>中配置clickorderby为true的情况");
                }
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能在非数据列的<col/>中配置排序功能");
                }
                alrcbean.setRequireClickOrderby(true);
            }else
            {
                alrcbean.setRequireClickOrderby(false);
            }
        }
        if(rowselectvalue!=null)
        {
            rowselectvalue=rowselectvalue.trim();
            if(rowselectvalue.equalsIgnoreCase("true"))
            {
                alrcbean.setRowSelectValue(true);
            }else
            {
                alrcbean.setRowSelectValue(false);
            }
        }
        if(rowgroup!=null&&treerowgroup!=null)
        {
            throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能在同一个<col/>中同时配置treerowgroup和rowgroup属性");
        }else if(rowgroup!=null)
        {//加载参与普通行分组的<col/>的信息
            rowgroup=rowgroup.toLowerCase().trim();
            if(!rowgroup.equals("true")&&!rowgroup.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，参与分组的<col/>的rowgroup属性配置值不合法，只能配置true或false");
            }
            alrcbean.setRowgroup(Boolean.parseBoolean(rowgroup));
            if(alrcbean.isRowgroup())
            {
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()
                            +"失败，不能将显示sequence或不从数据库某字段获取数据(即column为sequence或non-fromdb)的<col/>配置rowgroup为true");
                }
                if(alrdbean.getRowgrouptype()==2)
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能为同一个报表同时配置普通分组和行分组功能");
                }
                colbean.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
                alrdbean.addRowgroupCol(colbean);
                alrdbean.setRowgrouptype(1);
            }
        }else if(treerowgroup!=null)
        {//加载参与树形行分组的<col/>的信息
            treerowgroup=treerowgroup.toLowerCase().trim();
            if(!treerowgroup.equals("true")&&!treerowgroup.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，参与分组的<col/>的treerowgroup属性配置值不合法，只能配置true或false");
            }
            alrcbean.setRowgroup(Boolean.parseBoolean(treerowgroup));
            if(alrcbean.isRowgroup())
            {
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()
                            +"失败，不能将显示sequence或不从数据库某字段获取数据(即column为sequence或non-fromdb)的<col/>配置treerowgroup为true");
                }
                if(alrdbean.getRowgrouptype()==1)
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能为同一个报表同时配置普通分组和行分组功能");
                }
                colbean.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);//参与树形分组的列永远显示
                alrdbean.addRowgroupCol(colbean);
                alrdbean.setRowgrouptype(2);
            }
        }
        if((alrdbean.getRowgrouptype()==1||alrdbean.getRowgrouptype()==2))
        {
            if(alrdbean.getTreenodeid()!=null&&!alrdbean.getTreenodeid().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，此报表已经配置为不限层级的树形分组报表，不能再配置普通行分组或树形行分组功能");
            }
            if(!alrcbean.isRowgroup()) alrdbean.addRowgroupCol(null);//当前列不参与行/树形分组，则后面的列都不能再参与行分组/树形分组，因为参与行分组/树形分组的列必须配置在最前面
        }
        String rowordervalue=eleColBean.attributeValue("rowordervalue");
        if(rowordervalue!=null)
        {
            alrcbean.setRoworderValue(rowordervalue.trim().toLowerCase().equals("true"));
        }
        if(colbean.isRoworderInputboxCol())
        {
            String inputboxstyleproperty=eleColBean.attributeValue("inputboxstyleproperty");
            if(inputboxstyleproperty!=null)
            {
                alrcbean.setRoworder_inputboxstyleproperty(inputboxstyleproperty.trim());
            }
        }
        
        //        {//只有hidden为0的列才能参与折线标题列
        //            String curvelabelup=eleCol.attributeValue("curvelabelup");//折线标题列的上部标题
        //            String curvelabeldown=eleCol.attributeValue("curvelabeldown");//折线标题列的上部标题
        //            String curvelabel=eleCol.attributeValue("curvelabel");//是否参与了折线标题
        //            String curvecolor=eleCol.attributeValue("curvecolor");//是否参与了折线标题
        
        //            {
        
        
        
        
        //            }
        
        
        return 1;
    }

    
    
    public int beforeDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.beforeDisplayLoading(disbean,lstEleDisplayBeans);
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(alrdbean==null)
        {
            alrdbean=new AbsListReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,alrdbean);
        }
        Map<String,String> mDisplayProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "treenodeid", "treenodename", "treenodeparentid"});
        String treenodeid=mDisplayProperties.get("treenodeid");
        String treenodename=mDisplayProperties.get("treenodename");
        String treenodeparentid=mDisplayProperties.get("treenodeparentid");
        if(treenodeid!=null) alrdbean.setTreenodeid(treenodeid.trim());
        if(treenodename!=null) alrdbean.setTreenodename(treenodename.trim());
        if(treenodeparentid!=null) alrdbean.setTreenodeparentid(treenodeparentid.trim());
        return 1;
    }

    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        Map<String,String> mDisplayProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(
                lstEleDisplayBeans,
                new String[] { "treeborder", "treecloseable", "treexpandlayer", "treeasyn", "treexpandimg", "treeclosedimg", "treeleafimg",
                        "mouseoverbgcolor" });
        String treeborder=mDisplayProperties.get("treeborder");
        String treecloseable=mDisplayProperties.get("treecloseable");
        //        String treecheckbox=eleDisplay.attributeValue("treecheckbox");//是否需要为树形行分组树枝节点显示复选框
        String treexpandlayer=mDisplayProperties.get("treexpandlayer");//树形分组初始展开层数
        String treeasyn=mDisplayProperties.get("treeasyn");
        String treexpandimg=mDisplayProperties.get("treexpandimg");
        String treeclosedimg=mDisplayProperties.get("treeclosedimg");
        String treeleafimg=mDisplayProperties.get("treeleafimg");
        String mouseoverbgcolor=mDisplayProperties.get("mouseoverbgcolor");
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(mouseoverbgcolor==null)
        {
            if(alrdbean.getMouseoverbgcolor()==null)
            {
                alrdbean.setMouseoverbgcolor(Config.getInstance().getSystemConfigValue("default-mouseoverbgcolor",""));
            }
        }else
        {
            alrdbean.setMouseoverbgcolor(mouseoverbgcolor.trim());
        }
        if(treeborder!=null)
        {
            treeborder=treeborder.trim();
            if(treeborder.equals("")) treeborder="2";
            if(!treeborder.equals("0")&&!treeborder.equals("1")&&!treeborder.equals("2")&&!treeborder.equals("3"))
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，<display>的treeborder属性只能配置为0、1、2、3");
            }
            alrdbean.setTreeborder(Integer.parseInt(treeborder));
        }
        if(treecloseable!=null)
        {
            treecloseable=treecloseable.toLowerCase().trim();
            if(treecloseable.equals("")) treecloseable="true";
            if(!treecloseable.equals("true")&&!treecloseable.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，<display>的treecloseable属性只能配置为true或false");
            }
            alrdbean.setTreecloseable(Boolean.parseBoolean(treecloseable));
        }
        if(treexpandlayer!=null)
        {
            treexpandlayer=treexpandlayer.trim();
            if(treexpandlayer.equals("")) treexpandlayer="-1";
            alrdbean.setTreexpandlayer(Integer.parseInt(treexpandlayer));
        }
        if(!alrdbean.isTreecloseable()) alrdbean.setTreexpandlayer(-1);
        if(treeasyn!=null)
        {
            alrdbean.setTreeAsynLoad(treeasyn.toLowerCase().trim().equals("true"));
        }
        if(treexpandimg!=null)
        {
            treexpandimg=treexpandimg.trim();
            if(treexpandimg.equals(""))
            {
                alrdbean.setLstTreexpandimgs(null);
            }else
            {
                alrdbean.setLstTreexpandimgs(Tools.parseStringToList(treexpandimg,';','\''));
            }
        }
        if(treeclosedimg!=null)
        {
            treeclosedimg=treeclosedimg.trim();
            if(treeclosedimg.equals(""))
            {
                alrdbean.setLstTreeclosedimgs(null);
            }else
            {
                alrdbean.setLstTreeclosedimgs(Tools.parseStringToList(treeclosedimg,';','\''));
            }
        }
        if(treeleafimg!=null) alrdbean.setTreeleafimg(treeleafimg.trim());
            
        //        processRowSelectCol(disbean);//处理提供行选中的列，只有报表行选中类型为Consts.ROWSELECT_CHECKBOX或Consts.ROWSELECT_RADIOBOX类型时，才会有行选中的列
        //        if(treecheckbox!=null)
        
        
        
        
        //                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
        
        
        
        
        //        boolean isAllAlwayOrNeverCol=true;//是否全部是hidden为1或3的<col/>（如果<col/>全部是1或3，则<group/>的hidden不可能出现0和2的情况，所以不用判断它）
        List<ColBean> lstColBeans=disbean.getLstCols();
        boolean bolContainsClickOrderby=false;
        //        boolean bolContainsNonConditionFilter=false;
        if(lstColBeans!=null&&lstColBeans.size()>0)
        {
            AbsListReportColBean alcbeanTemp;
            for(ColBean cbeanTmp:lstColBeans)
            {
                if(cbeanTmp==null) continue;
                
                
                alcbeanTemp=(AbsListReportColBean)cbeanTmp.getExtendConfigDataForReportType(KEY);
                if(alcbeanTemp==null) continue;
                if(alcbeanTemp.isRequireClickOrderby())
                {
                    bolContainsClickOrderby=true;
                }
                
                
                //                {
                
                
            }
        }
        //        if(isAllAlwayOrNeverCol) bean.setColselected(false);//全部是hidden为1或3的<col/>，后面显示时将不为它提供列选择框
        alrdbean.setContainsClickOrderBy(bolContainsClickOrderby);
        //        bean.setContainsNonConditionFilter(bolContainsNonConditionFilter);//此报表包括与条件无关的列过滤功能
        XmlElementBean eleStatisticBean=getEleStatisticBean(lstEleDisplayBeans,disbean.getReportBean());
        if(eleStatisticBean!=null) loadStatisticConfig(eleStatisticBean,disbean,alrdbean);
        return 1;
    }

    private XmlElementBean getEleStatisticBean(List<XmlElementBean> lstEleDisplayBeans,ReportBean rbean)
    {
        if(lstEleDisplayBeans==null||lstEleDisplayBeans.size()==0) return null;
        List<XmlElementBean> lstTemp;
        for(XmlElementBean eleDisBeanTmp:lstEleDisplayBeans)
        {
            lstTemp=eleDisBeanTmp.getLstChildElements();
            if(lstTemp==null||lstTemp.size()==0) continue;
            for(XmlElementBean eleChildTmp:lstTemp)
            {
                if("statistic".equals(eleChildTmp.getName()))
                {
                    return eleChildTmp;
                }else if("ref".equals(eleChildTmp.getName()))
                {
                    XmlElementBean eleStatiBean=getEleStatisticBean(ConfigLoadAssistant.getInstance().getRefElements(
                            eleChildTmp.attributeValue("key"),"display",null,rbean),rbean);
                    if(eleStatiBean!=null) return eleStatiBean;
                }
            }
        }
        return null;
    }

    private void loadStatisticConfig(XmlElementBean eleStatiBean,DisplayBean disbean,AbsListReportDisplayBean alrdbean)
    {
        AbsListReportStatiBean statiBean=new AbsListReportStatiBean(disbean);
        alrdbean.setStatibean(statiBean);
        ReportBean reportbean=disbean.getReportBean();
        XmlElementBean eleStatitemsBean=eleStatiBean.getChildElementByName("statitems");
        if(eleStatitemsBean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计失败，没有配置<statitems/>");
        }
        List<XmlElementBean> lstEleStatitemBeans=eleStatitemsBean.getLstChildElementsByName("statitem");
        if(lstEleStatitemBeans==null||lstEleStatitemBeans.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计失败，没有在<statitems/>中配置<statitem/>");
        }
        AbsListReportStatItemBean alrsibeanTmp;
        for(XmlElementBean eleBeanTmp:lstEleStatitemBeans)
        {
            alrsibeanTmp=new AbsListReportStatItemBean();
            String property=eleBeanTmp.attributeValue("property");
            String value=eleBeanTmp.attributeValue("value");
            if(property==null||property.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项<statitems/>失败，property属性不能为空");
            }
            alrsibeanTmp.setProperty(property.trim());
            if(value==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，必须配置<statitem/>的value属性");
            }
            value=value.trim();
            int idxl=value.indexOf("(");
            int idxr=value.lastIndexOf(")");
            if(idxl<=0||idxr!=value.length()-1)
            {//不合要求
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，"+value+"不是有效的统计");
            }
            String statitype=value.substring(0,idxl).trim();
            String column=value.substring(idxl+1,idxr).trim();
            if(!Consts.lstStatisticsType.contains(statitype))
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，"+statitype+"不是有效的统计类型");
            }
            if(column.equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，"+value+"中统计字段为空");
            }
            alrsibeanTmp.setValue(value);
            alrsibeanTmp.setDatatypeObj(ConfigLoadAssistant.loadDataType(eleBeanTmp));
            alrsibeanTmp.setLstConditions(ComponentConfigLoadManager.loadConditionsInOtherPlace(eleBeanTmp,disbean.getReportBean()));
            statiBean.addStaticItemBean(alrsibeanTmp);
        }
        if(statiBean.getLstAllStatitemBeans()==null||statiBean.getLstAllStatitemBeans().size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计失败，没有在<statitems/>中配置<statitem/>");
        }
        FormatBean fbean=ConfigLoadAssistant.getInstance().loadFormatConfig(eleStatiBean.getChildElementByName("format"));
        if(fbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，没有配置格式化显示<format/>标签");
        }
        statiBean.setFbean(fbean);
        List<XmlElementBean> lstEleColBeans=eleStatiBean.getLstChildElementsByName("scol");
        if(lstEleColBeans!=null&&lstEleColBeans.size()>0)
        {
            for(XmlElementBean objTmp:lstEleColBeans)
            {
                statiBean.addStatiColBean(loadStatiColConfig(alrdbean,objTmp));
            }
        }
        List<XmlElementBean> lstRowGroupStatistics=eleStatiBean.getLstChildElementsByName("rowgroup");
        List<XmlElementBean> lstTreeRowGroupStatistics=eleStatiBean.getLstChildElementsByName("treerowgroup");
        if(lstRowGroupStatistics!=null&&lstRowGroupStatistics.size()>0&&lstTreeRowGroupStatistics!=null&&lstTreeRowGroupStatistics.size()>0)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，不能在<statistic/>同时配置<rowgroup/>和<treerowgroup/>");
        }
        if(lstRowGroupStatistics!=null&&lstRowGroupStatistics.size()>0)
        {
            for(XmlElementBean objTmp:lstRowGroupStatistics)
            {
                loadStatiRowGroupConfig(alrdbean,statiBean,objTmp);
            }
        }
        if(lstTreeRowGroupStatistics!=null&&lstTreeRowGroupStatistics.size()>0)
        {
            for(XmlElementBean objTmp:lstTreeRowGroupStatistics)
            {
                loadStatiRowGroupConfig(alrdbean,statiBean,objTmp);
            }
        }
    }

    private void loadStatiRowGroupConfig(AbsListReportDisplayBean alrdbean,AbsListReportStatiBean statiBean,XmlElementBean eleRowGroupBean)
    {
        String column=eleRowGroupBean.attributeValue("column");
        if(column==null||column.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()
                    +"失败，在<statistic/>配置<rowgroup/>时，column属性不能为空");
        }
        String condition=eleRowGroupBean.attributeValue("condition");
        
        
        //            throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()
        //                    +"失败，在<statistic/>配置<rowgroup/>时，因为是配置针对某个行分组的统计，因此condition属性不能为空");
        
        AbsListReportStatiRowGroupBean srgbean=new AbsListReportStatiRowGroupBean();
        srgbean.setRowgroupcolumn(column.trim());
        if(condition!=null) srgbean.setCondition(condition.trim());
        statiBean.addStatiRowGroupBean(srgbean);
        List<XmlElementBean> lstEleCols=eleRowGroupBean.getLstChildElementsByName("scol");
        if(lstEleCols!=null&&lstEleCols.size()>0)
        {
            for(XmlElementBean colobjTmp:lstEleCols)
            {
                srgbean.addStaticColBean(loadStatiColConfig(alrdbean,colobjTmp));
            }
        }
    }

    private AbsListReportStatiColBean loadStatiColConfig(AbsListReportDisplayBean alrdbean,XmlElementBean eleScolBean)
    {
        AbsListReportStatiColBean statiColBean=new AbsListReportStatiColBean();
        String property=eleScolBean.attributeValue("property");
        if(property==null||property.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()+"的统计项<scol/>失败，property属性不能为空");
        }
        statiColBean.setProperty(property.trim());
        String colspan=eleScolBean.attributeValue("colspan");
        String valuestyleproperty=eleScolBean.attributeValue("valuestyleproperty");
        if(valuestyleproperty==null) valuestyleproperty="";
        String colspanInStyleprop=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
        if(colspan!=null&&!colspan.trim().equals("")&&colspanInStyleprop!=null&&!colspanInStyleprop.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()
                    +"失败，不能同时在<scol/>的colspan和valuestyleproperty中同时配置colspan值");
        }
        if(colspan!=null&&!colspan.trim().equals(""))
        {
            valuestyleproperty=valuestyleproperty+" colspan=\""+colspan+"\"";
        }
        statiColBean.setValuestyleproperty(valuestyleproperty.trim());
        return statiColBean;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        AbsListReportSqlBean alrsbean=(AbsListReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(alrsbean==null)
        {
            alrsbean=new AbsListReportSqlBean(sqlbean);
            sqlbean.setExtendConfigDataForReportType(KEY,alrsbean);
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        constructSqlForListType(reportbean.getSbean());
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean==null) return 1;
        processFixedColsAndRows(reportbean);
        processReportScrollConfig(reportbean);
        if(alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL||alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_FIXED)
        {
            reportbean.setCellresize(0);
        }
        DisplayBean dbean=reportbean.getDbean();
        if(dbean.getColselect()==null) dbean.setColselect(true);
        processRowSelectCol(dbean);
        processRoworderCol(dbean);
        ((AbsListReportDisplayBean)dbean.getExtendConfigDataForReportType(KEY)).doPostLoad();
        return 1;
    }

    private void constructSqlForListType(SqlBean sqlbean)
    {
        if(sqlbean==null) return;
        sqlbean.getDbType().constructSqlForListType(sqlbean);
    }

    protected void processFixedColsAndRows(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean.getFixedcols()<=0&&alrbean.getFixedrows()<=0) return;
        if(alrbean.getFixedcols()>0)
        {
            int cnt=0;
            for(ColBean cbTmp:reportbean.getDbean().getLstCols())
            {
                if(cbTmp.getDisplaytype()==Consts.COL_DISPLAYTYPE_HIDDEN) continue;
                cnt++;
            }
            if(cnt<=alrbean.getFixedcols()) alrbean.setFixedcols(0);
        }
        doProcessFixedColsAndRows(reportbean);
    }
    
    private void doProcessFixedColsAndRows(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean.getFixedcols()<=0&&alrbean.getFixedrows()<=0) return;
        if(alrbean.getFixedcols()>0)
        {
            boolean isChkRadioRowselectReport=Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType())
                    ||Consts.ROWSELECT_RADIOBOX.equals(alrbean.getRowSelectType());
            AbsListReportColBean alrcbeanTmp;
            int cnt=0;
            for(ColBean cbTmp:reportbean.getDbean().getLstCols())
            {
                if(cbTmp.getDisplaytype()==Consts.COL_DISPLAYTYPE_HIDDEN) continue;
                if(cbTmp.isRowSelectCol())
                {
                    if(!isChkRadioRowselectReport) continue;
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,在<report/>的fixedcols中配置的冻结列数包括了行选中列，这样不能正常选中行");
                }
                alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(alrcbeanTmp==null)
                {
                    alrcbeanTmp=new AbsListReportColBean(cbTmp);
                    cbTmp.setExtendConfigDataForReportType(KEY,alrcbeanTmp);
                }
                alrcbeanTmp.setFixedCol(true);//表示此列是冻结列
                if(++cnt==alrbean.getFixedcols()) break;
            }
        }
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,树形分组报表不能冻结行列标题");
        }
    }
    
    protected void processReportScrollConfig(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        int scrolltype=alrbean.getScrollType();
        if(scrolltype==AbsListReportBean.SCROLLTYPE_NONE||scrolltype==AbsListReportBean.SCROLLTYPE_FIXED) return;
        if(scrolltype==AbsListReportBean.SCROLLTYPE_ALL)
        {
            ComponentAssistant.getInstance().doPostLoadForComponentScroll(reportbean,true,true,reportbean.getScrollwidth(),
                    reportbean.getScrollheight(),reportbean.getScrollstyle());
        }else
        {
            if(scrolltype==AbsListReportBean.SCROLLTYPE_VERTICAL)
            {
                String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(reportbean.getWidth());
                if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0")) 
                {//配置的html大小无效或配置为0，则相当于没有配置
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,此报表只显示垂直滚动条，因此必须为其配置width属性指定报表宽度");
                }else
                {
                    if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,只显示垂直滚动条时，不能将报表的width属性配置为百分比");
                    }
                    reportbean.setWidth(htmlsizeArr[0]+htmlsizeArr[1]);
                }       
                reportbean.setShowContextMenu(false);
            }
            if(Consts_Private.SCROLLSTYLE_IMAGE.equals(reportbean.getScrollstyle()))
            {
                String scrolljs=null;
                if(Config.encode.toLowerCase().trim().equals("utf-8"))
                {
                    scrolljs="/webresources/script/wabacus_scroll.js";
                }else
                {
                    String encode=Config.encode;
                    if(encode.trim().equalsIgnoreCase("gb2312"))
                    {
                        encode="gbk";
                    }
                    scrolljs="/webresources/script/"+encode.toLowerCase()+"/wabacus_scroll.js";
                }
                scrolljs=Config.webroot+"/"+scrolljs;
                scrolljs=Tools.replaceAll(scrolljs,"//","/");
                reportbean.getPageBean().addMyJavascript(scrolljs);
                String css=Config.webroot+"/webresources/skin/"+Consts_Private.SKIN_PLACEHOLDER+"/wabacus_scroll.css";
                css=Tools.replaceAll(css,"//","/");
                reportbean.getPageBean().addMyCss(css);
                if(scrolltype==AbsListReportBean.SCROLLTYPE_HORIZONTAL)
                {
                    reportbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+reportbean.getGuid()
                            +"','-1',12)"));
                }else if(scrolltype==AbsListReportBean.SCROLLTYPE_VERTICAL)
                {//只显示垂直滚动条
                    reportbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+reportbean.getGuid()+"','"
                            +reportbean.getScrollheight()+"',11)"));
                }
            }
        }
    }
    
    protected ColBean[] processRowSelectCol(DisplayBean disbean)
    {
        List<ColBean> lstCols=disbean.getLstCols();
        ColBean cbRowSelect=null;
        for(ColBean cbTmp:lstCols)
        {
            if(cbTmp.isRowSelectCol())
            {
                if(cbRowSelect!=null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，不能配置多个行选择的单选框或复选框列");
                }
                cbRowSelect=cbTmp;
            }
        }
        ReportBean reportbean=disbean.getReportBean();
        ColBean[] cbResult=null;
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrbean.getRowSelectType()==null
                ||(!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_CHECKBOX)&&!alrbean.getRowSelectType().trim().equals(
                        Consts.ROWSELECT_RADIOBOX)))
        {//当前报表要么没有提供行选中功能，要么提供的不是复选框/单选框的行选择功能
            if(cbRowSelect==null) return null;
            for(int i=0,len=lstCols.size();i<len;i++)
            {
                if(lstCols.get(i).isRowSelectCol())
                {
                    lstCols.remove(i);
                    break;
                }
            }
            return null;
        }
        //提供了复选框/单选框的行选择功能
        if(cbRowSelect==null)
        {
            cbResult=new ColBean[2];
            ColBean cbNewRowSelect=new ColBean(disbean);
            cbNewRowSelect.setColumn(Consts_Private.COL_ROWSELECT);
            cbNewRowSelect.setProperty(Consts_Private.COL_ROWSELECT);
            cbResult[0]=cbNewRowSelect;
            AbsListReportColBean alrcbean=new AbsListReportColBean(cbNewRowSelect);//为这个新增的colbean生成一个alrcbean对象
            cbNewRowSelect.setExtendConfigDataForReportType(KEY,alrcbean);
            if(Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType()))
            {
                cbNewRowSelect
                        .setLabel("<input type=\"checkbox\" onclick=\"try{doSelectedAllDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                                +reportbean.getGuid()+"_rowselectbox\">");
            }else
            {
                cbNewRowSelect.setLabel("");
            }
            cbNewRowSelect.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
            cbNewRowSelect.setLabelstyleproperty("style=\"text-align:center;vertical-align:middle;\"");
            cbNewRowSelect.setValuestyleproperty("style=\"text-align:center;vertical-align:middle;\"");
            UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(UltraListReportType.KEY);
            ColBean cbTmp;
            for(int i=0,len=lstCols.size();i<len;i++)
            {
                cbTmp=lstCols.get(i);
                if(cbTmp.getDisplaytype().equals(Consts.COL_DISPLAYTYPE_HIDDEN))
                {
                    if(i==len-1) lstCols.add(cbNewRowSelect);
                    continue;
                }
                AbsListReportColBean alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(alrcbeanTmp!=null&&(alrcbeanTmp.isRowgroup()||alrcbeanTmp.isFixedCol()))
                {
                    if(i==len-1) lstCols.add(cbNewRowSelect);
                    continue;
                }
                UltraListReportColBean ulrcbeanTmp=(UltraListReportColBean)cbTmp.getExtendConfigDataForReportType(UltraListReportType.KEY);
                if(ulrcbeanTmp!=null&&ulrcbeanTmp.getParentGroupid()!=null&&!ulrcbeanTmp.getParentGroupid().trim().equals(""))
                {//当前列是在<group/>中
                    String parentgroupid=ulrcbeanTmp.getParentGroupid();
                    if(ulrdbean!=null&&(hasRowgroupColSibling(parentgroupid,ulrdbean)||hasFixedColSibling(parentgroupid,ulrdbean)))
                    {//如果此列所在的<group/>或任意层父<group/>中有行分组列或被冻结的列，则新生成的行选择列不能在它的前面
                        if(i==len-1) lstCols.add(cbNewRowSelect);
                        continue;
                    }
                }
                lstCols.add(i,cbNewRowSelect);
                cbResult[1]=cbTmp;
                break;
            }
        }else
        {
            if(Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType()))
            {
                String label=cbRowSelect.getLabel();
                label=label==null?"":label.trim();
                if(label.indexOf("<input")<0||label.indexOf("type")<0||label.indexOf("checkbox")<0)
                {
                    label=label
                            +"<input type=\"checkbox\" onclick=\"try{doSelectedAllDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                            +reportbean.getGuid()+"_rowselectbox\">";
                }
                cbRowSelect.setLabel(label);
            }
            cbRowSelect.setLabelstyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getLabelstyleproperty(),"align","center"));
            cbRowSelect.setLabelstyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getLabelstyleproperty(),"valign","middle"));
            cbRowSelect.setValuestyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getValuestyleproperty(),"align","center"));
            cbRowSelect.setValuestyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getValuestyleproperty(),"valign","middle"));
            cbRowSelect.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
        }
        return cbResult;
    }
    
    protected List<ColBean> processRoworderCol(DisplayBean disbean)
    {
        List<ColBean> lstCols=disbean.getLstCols();
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrbean.getLstRoworderTypes()!=null&&alrbean.getLstRoworderTypes().size()>0)
        {//如果有行排序功能
            List<ColBean> lstRoworderValueCols=new ArrayList<ColBean>();
            AbsListReportColBean alrcbeanTmp=null;
            for(ColBean cbTmp:lstCols)
            {
                alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);   
                if(alrcbeanTmp==null||!alrcbeanTmp.isRoworderValue()) continue;
                lstRoworderValueCols.add(cbTmp);
            }
            if(lstRoworderValueCols.size()==0)
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
                        +"失败，为它配置了行排序功能，但没有一个<col/>的rowordervalue属性配置为true，这样无法完成行排序功能");
            }
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
            alrdbean.setLstRoworderValueCols(lstRoworderValueCols);
        }
        Map<String,ColBean> mCreatedRoworderCols=new HashMap<String,ColBean>();
        for(String rowordertypeTmp:Consts.lstAllRoworderTypes)
        {
            if(rowordertypeTmp.equals(Consts.ROWORDER_DRAG)) continue;
            if(alrbean.getLstRoworderTypes()==null||!alrbean.getLstRoworderTypes().contains(rowordertypeTmp))
            {
                for(int i=lstCols.size()-1;i>=0;i--)
                {
                    if(lstCols.get(i).isRoworderCol(getRoworderColColumnByRoworderType(rowordertypeTmp)))
                    {
                        lstCols.remove(i);
                    }
                }
            }else
            {//有这种行排序功能，则判断到用户没有配置相应的列时，增加一个这样的列
                boolean isExistCol=false;
                for(int i=lstCols.size()-1;i>=0;i--)
                {
                    if(lstCols.get(i).isRoworderCol(getRoworderColColumnByRoworderType(rowordertypeTmp)))
                    {
                        isExistCol=true;
                        break;
                    }
                }
                if(!isExistCol)
                {
                    ColBean cbNewRoworder=new ColBean(disbean);
                    cbNewRoworder.setColumn(getRoworderColColumnByRoworderType(rowordertypeTmp));
                    cbNewRoworder.setProperty(getRoworderColColumnByRoworderType(rowordertypeTmp));
                    AbsListReportColBean alrcbean=new AbsListReportColBean(cbNewRoworder);
                    cbNewRoworder.setExtendConfigDataForReportType(KEY,alrcbean);
                    if(rowordertypeTmp.equals(Consts.ROWORDER_ARROW))
                    {
                        cbNewRoworder.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${roworder.arrow.label}",false));
                    }else if(rowordertypeTmp.equals(Consts.ROWORDER_INPUTBOX))
                    {
                        cbNewRoworder.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${roworder.inputbox.label}",false));
                    }else if(rowordertypeTmp.equals(Consts.ROWORDER_TOP))
                    {
                        cbNewRoworder.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${roworder.top.label}",false));
                    }
                    cbNewRoworder.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
                    cbNewRoworder.setLabelstyleproperty("style=\"text-align:center;vertical-align:middle;\"");
                    cbNewRoworder.setValuestyleproperty("style=\"text-align:center;vertical-align:middle;\"");
                    mCreatedRoworderCols.put(rowordertypeTmp,cbNewRoworder);
                }
            }
        }
        List<ColBean> lstCreatedColBeans=new ArrayList<ColBean>();
        if(mCreatedRoworderCols.size()>0)
        {
            for(String roworderTmp:alrbean.getLstRoworderTypes())
            {//根据配置顺序，依次加入disbean中
                if(!mCreatedRoworderCols.containsKey(roworderTmp)) continue;
                lstCols.add(mCreatedRoworderCols.get(roworderTmp));
                lstCreatedColBeans.add(mCreatedRoworderCols.get(roworderTmp));
            }
        }
        return lstCreatedColBeans;
    }
    
    protected String getRoworderColColumnByRoworderType(String rowordertype)
    {
        if(rowordertype==null) return null;
        if(rowordertype.equals(Consts.ROWORDER_ARROW))
        {
            return Consts_Private.COL_ROWORDER_ARROW;
        }else if(rowordertype.equals(Consts.ROWORDER_INPUTBOX))
        {
           return Consts_Private.COL_ROWORDER_INPUTBOX;
        }else if(rowordertype.equals(Consts.ROWORDER_TOP))
        {
            return Consts_Private.COL_ROWORDER_TOP;
        }
        return "";
    }
    
    private boolean hasRowgroupColSibling(String parentgroupid,UltraListReportDisplayBean ulrdbean)
    {
        if(parentgroupid==null||parentgroupid.trim().equals("")) return false;
        UltraListReportGroupBean ulrgbean=ulrdbean.getGroupBeanById(parentgroupid);
        if(ulrgbean==null) return false;
        if(ulrgbean.hasRowgroupChildCol()) return true;
        return hasRowgroupColSibling(ulrgbean.getParentGroupid(),ulrdbean);
    }
    
    private boolean hasFixedColSibling(String parentgroupid,UltraListReportDisplayBean ulrdbean)
    {
        if(parentgroupid==null||parentgroupid.trim().equals("")) return false;
        UltraListReportGroupBean ulrgbean=ulrdbean.getGroupBeanById(parentgroupid);
        if(ulrgbean==null) return false;
        if(ulrgbean.hasFixedChildCol()) return true;
        return hasFixedColSibling(ulrgbean.getParentGroupid(),ulrdbean);
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_LIST;
    }
}
