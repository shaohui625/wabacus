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

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.configbean.BlockListReportDisplayBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class BlockListReportType extends AbsListReportType
{
    private final static String KEY=BlockListReportType.class.getName();
    
    public BlockListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public String showReportData()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showReportScrollStartTag());
        resultBuf.append("<ul id=\""+rbean.getGuid()+"_data\" class=\"cls-blocklist\"");
        resultBuf.append(" style=\"");

//        {//配置了横向滚动条





        resultBuf.append(" width:"+getReportDataWidthOnPage()+";");
        if(rbean.getHeight()!=null&&!rbean.getHeight().trim().equals(""))
        {
            resultBuf.append("height:").append(rbean.getHeight()).append(";");
        }
        resultBuf.append("\"");
        if(rbean.shouldShowContextMenu())
        {
            resultBuf.append(" oncontextmenu=\"try{showcontextmenu('contextmenu_"+rbean.getGuid()+"',event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
        }
        resultBuf.append(">");
        if(this.lstReportData==null||this.lstReportData.size()==0)
        {
            resultBuf.append("<li class=\"cls-blocklist-block\"><div class=\"cls-blocklist-item\">");
            if(this.isLazyDataLoad()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                resultBuf.append(rrequest.getStringAttribute(rbean.getId()+"_lazyloadmessage",""));
            }else
            {
                resultBuf.append(rrequest.getI18NStringValue((Config.getInstance().getResources().getString(rrequest,rbean.getPageBean(),
                        Consts.NODATA_PROMPT_KEY,true))));
            }
            resultBuf.append("</div></li></ul>");
            return resultBuf.toString();
        }
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            resultBuf.append("<table border='1'");
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_WORD)
            {
                resultBuf.append(" style=\"");
                if(!rbean.getWidth().trim().equals(""))
                {
                    resultBuf.append(" width:"+rbean.getWidth()+";");
                }else
                {
                    resultBuf.append("width:100%;");
                }
                resultBuf
                        .append("border-collapse:collapse;border:none;mso-border-alt:solid windowtext .25pt;mso-border-insideh:.5pt solid windowtext;mso-border-insidev:.5pt solid windowtext");
                resultBuf.append("\"");
            }else
            {
                resultBuf.append(" width=\"100%\"");
            }
            resultBuf.append(">");
        }
        resultBuf.append(showDataPart());
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            resultBuf.append("</table>"); 
        }
        resultBuf.append("</ul>");
        resultBuf.append(showReportScrollEndTag());
        return resultBuf.toString();
    }
    
    private String showDataPart()
    {
        StringBuffer resultBuf=new StringBuffer();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        int startNum=0;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            startNum=(cdb.getFinalPageno()-1)*cdb.getPagesize();
        }else
        {
            resultBuf.append("<tr>");
        }
        List<ColBean> lstColBeans=rbean.getDbean().getLstCols();
        BlockListReportDisplayBean blrdbean=(BlockListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        String blockstyleproperty=blrdbean.getBlockstyleproperty()==null?"":blrdbean.getBlockstyleproperty().trim();
        ColDataByInterceptor coldataByInterceptor;//从拦截器中得到的用户为当前列生成的数据
        String col_displayvalue;
        Object rowDataObjTmp;
        int colsinexportfile=blrdbean.getColsinexportfile();
        int n=1;
        for(int i=0,size=lstReportData.size();i<size;i++)
        {
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                resultBuf.append("<li class=\"cls-blocklist-block\" "+blockstyleproperty+">");
            }else
            {
                resultBuf.append("<td "+blockstyleproperty+">");
            }
            rowDataObjTmp=this.lstReportData.get(i);
            for(ColBean cbean:lstColBeans)
            {
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())||cdb.getColDisplayModeAfterAuthorize(cbean)<=0)
                {
                    continue;
                }
                
                col_displayvalue=getColDisplayValue(cbean,rowDataObjTmp,startNum);
                resultBuf.append("<div class=\"cls-blocklist-item\"");
                coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,i,col_displayvalue);
                if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
                {
                    col_displayvalue=coldataByInterceptor.getDynvalue();
                }
                resultBuf.append(getColGroupStyleproperty(cbean.getValuestyleproperty(rrequest),coldataByInterceptor));
                resultBuf.append(">").append(col_displayvalue);
                resultBuf.append("</div>");
            }
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                resultBuf.append("</li>");
            }else
            {
                resultBuf.append("</td>");
                if(colsinexportfile>0&&n++%colsinexportfile==0) resultBuf.append("</tr><tr>");
            }
        }
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            resultBuf.toString().endsWith("</tr>"); 
        }
        return resultBuf.toString();
    }
    
    private String getColDisplayValue(ColBean cbean,Object dataObj,int startNum)
    {
        String col_displayvalue;
        if(cbean.isControlCol())
        {
            throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，此报表不能配置行选中列或行排序列");
        }else if(cbean.isSequenceCol())
        {
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            col_displayvalue=String.valueOf(startNum+alrcbean.getSequenceStartNum());
        }else
        {
            col_displayvalue=cbean.getDisplayValue(dataObj,rrequest);
            if(col_displayvalue==null) col_displayvalue="";
        }
        return col_displayvalue;
    }
    
    private String showReportScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollStartPart(rbean,isShowScrollX,isShowScrollY,rbean.getScrollwidth(),
                rbean.getScrollheight(),rbean.getScrollstyle());
    }
    
    private String showReportScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollEndPart(isShowScrollX,isShowScrollY);
    }
    
    public String showReportData(boolean showtype)
    {
        if(showtype) return showReportData();
        return "";
    }

    public String getColSelectedMetadata()
    {
        return "";
    }

    public int beforeDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.beforeDisplayLoading(disbean,lstEleDisplayBeans);
        BlockListReportDisplayBean blrdbean=new BlockListReportDisplayBean(disbean);
        disbean.setExtendConfigDataForReportType(KEY,blrdbean);
        XmlElementBean eleDisplayBean=lstEleDisplayBeans.get(0);
        String blockwidth=eleDisplayBean.attributeValue("blockwidth");
        String blockheight=eleDisplayBean.attributeValue("blockheight");
        String blockstyleproperty=eleDisplayBean.attributeValue("blockstyleproperty");
        String colsinexportfile=eleDisplayBean.attributeValue("colsinexportfile");
        if(blockstyleproperty==null) blockstyleproperty="";
        String style=Tools.getPropertyValueByName("style",blockstyleproperty,false);
        style=style==null?"":style.trim();
        String widthinstyle=Tools.getPropertyValueFromStyle("width",style);
        if((widthinstyle==null||widthinstyle.trim().equals(""))&&blockwidth!=null&&!blockwidth.trim().equals(""))
        {//没有在blockstyleproperty的style中指定width，但在<display/>中配置了blockwidth
            if(!style.equals("")&&!style.endsWith(";")) style=style+";";
            style=style+"width:"+blockwidth+";";
        }
        String heightinstyle=Tools.getPropertyValueFromStyle("height",style);
        if((heightinstyle==null||heightinstyle.trim().equals(""))&&blockheight!=null&&!blockheight.trim().equals(""))
        {//没有在blockstyleproperty的style中指定height，但在<display/>中配置了blockheight
            if(!style.equals("")&&!style.endsWith(";")) style=style+";";
            style=style+"height:"+blockheight+";";
        }
        blockstyleproperty=Tools.removePropertyValueByName("style",blockstyleproperty);
        if(style!=null&&!style.trim().equals(""))
        {
            blockstyleproperty=blockstyleproperty+" style=\""+style+"\"";
        }
        blrdbean.setBlockstyleproperty(blockstyleproperty.trim());
        if(colsinexportfile==null||colsinexportfile.trim().equals("")) colsinexportfile="5";
        try
        {
            blrdbean.setColsinexportfile(Integer.parseInt(colsinexportfile.trim()));
        }catch(NumberFormatException e)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，为<display/>配置的colsinexportfile属性值"+colsinexportfile
                    +"不是有效数字",e);
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        DisplayBean disbean=reportbean.getDbean();
        disbean.setColselect(false);
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        alrbean.setRowSelectType(Consts.ROWSELECT_NONE);
        return super.doPostLoad(reportbean);
    }
    
    protected void processReportScrollConfig(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        
        alrbean.setFixedcols(null,0);
        alrbean.setFixedrows(0);
        boolean isShowScrollX=reportbean.getScrollwidth()!=null&&!reportbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=reportbean.getScrollheight()!=null&&!reportbean.getScrollheight().trim().equals("");
        if(Consts_Private.SCROLLSTYLE_IMAGE.equals(reportbean.getScrollstyle())&&isShowScrollY)
        {//要显示图片垂直滚动条
            String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(reportbean.getHeight());
            if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0")) 
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,blocklist报表类型要显示图片垂直滚动条时，必须配置height属性，否则在firefox/chrome等浏览器上显示不出垂直滚动条");
            }else
            {
                if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,blocklist报表类型要显示图片垂直滚动条时，必须配置height属性，且不能配置为百分比，否则在firefox/chrome等浏览器上显示不出垂直滚动条");
                }
            }       
        }
        ComponentAssistant.getInstance().doPostLoadForComponentScroll(reportbean,isShowScrollX,isShowScrollY,reportbean.getScrollwidth(),
                reportbean.getScrollheight(),reportbean.getScrollstyle());
    }
}

