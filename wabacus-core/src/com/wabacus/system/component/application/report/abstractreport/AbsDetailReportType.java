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

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public abstract class AbsDetailReportType extends AbsReportType
{
    public AbsDetailReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    protected String showMetaDataDisplayStringStart()
    {
        if(rbean.getMDependChilds()==null||rbean.getMDependChilds().size()==0) return super.showMetaDataDisplayStringStart();
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        StringBuffer childReportidsBuf=new StringBuffer();
        for(String childReportidTmp:rbean.getMDependChilds().keySet())
        {
            childReportidsBuf.append(childReportidTmp).append(";");
        }
        resultBuf.append(" dependingChildReportIds=\"").append(childReportidsBuf.toString()).append("\"");
        return resultBuf.toString();
    }

    protected String showReportScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollStartPart(rbean,isShowScrollX,isShowScrollY,rbean.getScrollwidth(),
                rbean.getScrollheight(),rbean.getScrollstyle());
    }

    protected String showReportScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollEndPart(isShowScrollX,isShowScrollY);
    }

    public String showColLabel(ColBean cbean)
    {
        if(cbean.getLabel()==null) return "";//<col/>的label没有配置时，不为它显示标题列
        StringBuffer resultBuf=new StringBuffer();
        ColDataByInterceptor coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,-1,cbean.getLabel());
        String label=ReportAssistant.getInstance().getColGroupLabel(rrequest,cbean.getLabel(),coldataByInterceptor);
        resultBuf.append("<td class='cls-data-th-detail' ");
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            String dataheaderbgcolor=Config.getInstance().getSkinConfigValue(rrequest.getPageskin(),"table.dataheader.bgcolor");
            if(dataheaderbgcolor==null) dataheaderbgcolor="";
            resultBuf.append(" bgcolor='"+dataheaderbgcolor+"'");
        }
        resultBuf.append(getColGroupStyleproperty(cbean.getLabelstyleproperty(rrequest),coldataByInterceptor));
        if(label.equals("")) label="&nbsp;";
        resultBuf.append(">"+label+"</td>");
        return resultBuf.toString();
    }
    
    protected  String getDefaultNavigateKey()
    {
        return Consts.DETAILREPORT_NAVIGATE_DEFAULT;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        super.afterReportLoading(reportbean,lstEleReportBeans);
        reportbean.setCellresize(0);
        return 1;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        if(colbean.isSequenceCol())
        {
            throw new WabacusConfigLoadingException("报表"+colbean.getReportBean().getPath()+"为数据细览报表，不允许<col/>标签的column属性配置为自动增长列");
        }
        if(colbean.isRowSelectCol())
        {
            throw new WabacusConfigLoadingException("报表"+colbean.getReportBean().getPath()+"为数据细览报表，不允许<col/>标签的column属性配置为行选中列");
        }
        if(colbean.isRoworderCol())
        {
            throw new WabacusConfigLoadingException("报表"+colbean.getReportBean().getPath()+"为数据细览报表，不允许<col/>标签的column属性配置为行排序列");
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        constructSqlForDetailType(reportbean.getSbean());
        DisplayBean dbean=reportbean.getDbean();
        List<ColBean> lstColBeans=dbean.getLstCols();
        if(Consts_Private.REPORT_BORDER_VERTICAL.equals(reportbean.getBorder())||Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(reportbean.getBorder()))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，细览报表不支持"
                    +Consts_Private.REPORT_BORDER_VERTICAL+"和"+Consts_Private.REPORT_BORDER_HORIZONTAL2+"两种边框类型");
        }
        if(lstColBeans!=null&&lstColBeans.size()>0)
        {
            for(ColBean cbean:lstColBeans)
            {
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
                String borderstyle=cbean.getBorderStylePropertyOnColBean();
                if(borderstyle!=null&&!borderstyle.trim().equals(""))
                {
                    cbean.setValuestyleproperty(Tools.mergeHtmlTagPropertyString(cbean.getValuestyleproperty(),"style=\""+borderstyle+"\"",1));
                    cbean.setLabelstyleproperty(Tools.mergeHtmlTagPropertyString(cbean.getLabelstyleproperty(),"style=\""+borderstyle+"\"",1));
                }
            }
        }
        if(dbean.getColselect()==null) dbean.setColselect(false);//这种报表类型默认不支持列选择功能
        
        boolean isShowScrollX=reportbean.getScrollwidth()!=null&&!reportbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=reportbean.getScrollheight()!=null&&!reportbean.getScrollheight().trim().equals("");
        ComponentAssistant.getInstance().doPostLoadForComponentScroll(reportbean,isShowScrollX,isShowScrollY,reportbean.getScrollwidth(),
                reportbean.getScrollheight(),reportbean.getScrollstyle());
        return 1;
    }

    private void constructSqlForDetailType(SqlBean sqlbean)
    {
        if(sqlbean==null) return;
        sqlbean.doPostLoadSql(false);
        sqlbean.buildPageSplitSql();
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_DETAIL;
    }
    
    public abstract String showColData(ColBean cbean,boolean showpart,boolean showinputbox,String dynstyleproperty);
}
