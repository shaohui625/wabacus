/* 
 * Copyright (C) 2010-2012 星星<349446658@qq.com>
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
package com.wabacusdemo;

import java.util.List;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportDataAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.intercept.AbsInterceptorDefaultAdapter;
import com.wabacus.system.intercept.RowDataByInterceptor;
import com.wabacus.util.Consts;

public class Interceptor_controlpagedisplaypage1Report2 extends AbsInterceptorDefaultAdapter
{
    public RowDataByInterceptor beforeDisplayReportDataPerRow(AbsReportType reportTypeObj,ReportRequest rrequest,int rowindex,int colspans,
            List lstColBeans)
    {
        RowDataByInterceptor rowdata=new RowDataByInterceptor();
        if(rowindex==Integer.MAX_VALUE)
        {//当前是显示完最后一行记录后的调用
            String insertedHtml="<tr><td align='center' bgcolor='#dddddd' colspan='"+colspans+"' ><font size='3'><b>";
            insertedHtml+="显示结束";
            insertedHtml+="</b></font></td></tr>";
            rowdata.setInsertDisplayRowHtml(insertedHtml);
        }else
        {//显示数据行
            List lstData=reportTypeObj.getLstReportData();//所有报表数据的容器
            if(lstData!=null&&lstData.size()>0&&lstColBeans!=null&&lstColBeans.size()>0)
            {//报表有数据
                StringBuffer collabelBuf=new StringBuffer();
                ColBean cbTmp;
                for(int i=0;i<lstColBeans.size();i++)
                {
                    cbTmp=(ColBean)lstColBeans.get(i);
                    if(cbTmp.getDisplaytype().equals(Consts.COL_DISPLAYTYPE_HIDDEN)) continue;
                    if(cbTmp.getLabel()==null||cbTmp.getLabel().trim().equals("")) continue;
                    collabelBuf.append(cbTmp.getLabel()).append(";");
                }
                if(collabelBuf.length()>0&&collabelBuf.charAt(collabelBuf.length()-1)==';') collabelBuf.deleteCharAt(collabelBuf.length()-1);
                if(collabelBuf.length()>0)
                {
                    StringBuffer insertedHtmlBuf=new StringBuffer();
                    insertedHtmlBuf.append("<tr><td colspan='"+colspans+"' align='left' bgcolor='#eeeeee'>");
                    insertedHtmlBuf.append("<font size='2'><b>"+collabelBuf.toString()+"列的数据</b></font>");
                    insertedHtmlBuf.append("</td></tr>");
                    rowdata.setInsertDisplayRowHtml(insertedHtmlBuf.toString());//动态插入要显示的内容
                }
            }
        }
        return rowdata;
    }
}

