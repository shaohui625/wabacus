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
package com.wabacus.system.assistant;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class ReportDataAssistant
{
    private final static ReportDataAssistant instance=new ReportDataAssistant();

    private static Log log=LogFactory.getLog(ReportDataAssistant.class);

    private ReportDataAssistant()
    {}

    public static ReportDataAssistant getInstance()
    {
        return instance;
    }

    public List getReportData(ReportRequest rrequest,String reportid)
    {
        if(rrequest==null) return null;
        AbsReportType reportTypeObj=getReportTypeObj(rrequest,reportid);
        if(reportTypeObj==null) return null;
        return reportTypeObj.getLstReportData();
    }

    public void setColValue(ReportRequest rrequest,String reportid,String property,Object dataObj,Object colvalue)
    {
        ColBean cbean=getColBeanByProperty(rrequest,reportid,property);
        if(cbean==null) return;
        try
        {
            cbean.getSetMethod().invoke(dataObj,new Object[] { colvalue });
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("设置"+colvalue+"到报表"+cbean.getReportBean().getPath()+"的列"+property+"失败",e);
        }
    }
    
    public Object getColValue(ReportRequest rrequest,String reportid,String property,Object dataObj)
    {
        ColBean cbean=getColBeanByProperty(rrequest,reportid,property);
        if(cbean==null) return null;
        return cbean.getRealTypeValue(dataObj,rrequest);
    }

    public String getStringColValue(ReportRequest rrequest,String reportid,String property,Object dataObj)
    {
        ColBean cbean=getColBeanByProperty(rrequest,reportid,property);
        if(cbean==null) return null;
        return cbean.getDisplayValue(dataObj,rrequest);
    }
    
    public List getLstColValues(ReportRequest rrequest,String reportid,String property)
    {
        List lstReportData=getReportData(rrequest,reportid);
        if(lstReportData==null||lstReportData.size()==0) return null;
        ColBean cbean=getColBeanByProperty(rrequest,reportid,property);
        if(cbean==null) return null;
        List lstResults=new ArrayList();
        for(int i=0;i<lstReportData.size();i++)
        {
            lstResults.add(cbean.getRealTypeValue(lstReportData.get(i),rrequest));
        }
        return lstResults;
    }
    
    public List<String> getLstStringColValues(ReportRequest rrequest,String reportid,String property)
    {
        List lstReportData=getReportData(rrequest,reportid);
        if(lstReportData==null||lstReportData.size()==0) return null;
        ColBean cbean=getColBeanByProperty(rrequest,reportid,property);
        if(cbean==null) return null;
        List<String> lstResults=new ArrayList<String>();
        for(int i=0;i<lstReportData.size();i++)
        {
            lstResults.add(cbean.getDisplayValue(lstReportData.get(i),rrequest));
        }
        return lstResults;
    }
    
    private ColBean getColBeanByProperty(ReportRequest rrequest,String reportid,String property)
    {
        AbsReportType reportTypeObj=getReportTypeObj(rrequest,reportid);
        if(reportTypeObj==null) return null;
        ReportBean rbean=reportTypeObj.getReportBean();
        if(rbean.getDbean()==null) return null;
        ColBean cbean=rbean.getDbean().getColBeanByColProperty(property);
        if(cbean==null)
        {
            log.warn("在报表"+rbean.getPath()+"中没有找到property为"+property+"的列");
            return null;
        }
        return cbean;
    }
    
    private AbsReportType getReportTypeObj(ReportRequest rrequest,String reportid)
    {
        IComponentType comTypeObj=rrequest.getComponentTypeObj(reportid,null,false);
        if(comTypeObj==null)
        {
            log.warn("页面"+rrequest.getPagebean().getId()+"中不存在ID为"+reportid+"的报表");
            return null;
        }
        if(!(comTypeObj instanceof AbsReportType))
        {
            log.warn("页面"+rrequest.getPagebean().getId()+"中ID为"+reportid+"的组件不是报表");
            return null;
        }
        return (AbsReportType)comTypeObj;
    }
}
