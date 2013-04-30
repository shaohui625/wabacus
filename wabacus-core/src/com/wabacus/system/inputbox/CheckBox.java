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
package com.wabacus.system.inputbox;

import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Tools;

public class CheckBox extends AbsRadioCheckBox
{
    private String separator;
    
    public CheckBox(String typename)
    {
        super(typename);
    }

    protected String getBoxType()
    {
        return "checkbox";
    }
    
    protected boolean isSelectedValue(String selectedvalues,String optionvalue)
    {
        return SelectedBoxAssistant.getInstance().isSelectedValueOfMultiSelectBox(selectedvalues,optionvalue,this.separator);
    }

    public String getDefaultlabel(ReportRequest rrequest)
    {
        if(this.defaultvalue==null) return null;
        return SelectedBoxAssistant.getInstance().getSelectedLabelByValuesOfMultiSelectedBox(
                getOptionNameAndValueList(rrequest,owner.getReportBean()),this.getDefaultvalue(rrequest),this.separator);
    }
    
    protected String checkSelectedValueInClient()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("var separator=null;if(inputboxSpanObj!=null) separator=inputboxSpanObj.getAttribute('separator');");
        resultBuf.append("if(separator==null||separator=='') separator=' ';");
        resultBuf.append("if(isSelectedValueForMultiSelectedBox(boxValue,optionvalue,separator))");
        return resultBuf.toString();
    }
    
    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        resultBuf.append(" separator=\"").append(this.separator).append("\"");
        return resultBuf.toString();
    }
    
    public String createGetValueByIdJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("var chkObjs=document.getElementsByName(id);");
        resultBuf.append("if(chkObjs==null||chkObjs.length==0) return '';");
        resultBuf.append("var value=''; var separator=chkObjs[0].getAttribute('separator');if(separator==null||separator=='') separator=' ';");
        resultBuf.append("for(i=0,len=chkObjs.length;i<len;i=i+1){");
        resultBuf.append("    if(chkObjs[i].checked){");
        resultBuf.append("        value=value+chkObjs[i].value+separator;");
        resultBuf.append("    }");
        resultBuf.append("}");
        resultBuf.append("value=wx_rtrim(value,separator);");
        resultBuf.append("return value;");
        return resultBuf.toString();
    }

    public String createGetValueByInputBoxObjJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("var chkboxname=boxObj.getAttribute('name');");
        resultBuf.append("if(chkboxname==null||chkboxname=='') return '';");
        resultBuf.append("var chkObjs=document.getElementsByName(chkboxname);");
        resultBuf.append("if(chkObjs==null||chkObjs.length==0) return '';");
        resultBuf.append("var value='';var label=''; var separator=chkObjs[0].getAttribute('separator');if(separator==null||separator=='') separator=' ';");
        resultBuf.append("for(i=0,len=chkObjs.length;i<len;i=i+1){");
        resultBuf.append("    if(chkObjs[i].checked){");
        resultBuf.append("        label=label+chkObjs[i].getAttribute('label')+separator;value=value+chkObjs[i].value+separator;");
        resultBuf.append("    }");
        resultBuf.append("}");
        resultBuf.append("label=wx_rtrim(label,separator);");//去掉label结尾部分的separator
        resultBuf.append("value=wx_rtrim(value,separator);");
        return resultBuf.toString();
    }
    
    public String createSetInputBoxValueByIdJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("var chkObjs=document.getElementsByName(id);");
        resultBuf.append("if(chkObjs==null||chkObjs.length==0) return;");
        resultBuf.append("var separator=chkObjs[0].getAttribute('separator');if(separator==null||separator=='') separator=' ';");
        resultBuf.append("for(var i=0,len=chkObjs.length;i<len;i=i+1){");
        resultBuf.append("  if(isSelectedValueForMultiSelectedBox(newvalue,chkObjs[i].value,separator)){chkObjs[i].checked=true;}");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    protected void processStylePropertyAfterMerged(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        super.processStylePropertyAfterMerged(reportTypeObj,ownerbean);
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"separator=\""+this.separator+"\"",1);
    }

    public void loadInputBoxConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(ownerbean,eleInputboxBean);
        if(eleInputboxBean==null) return;
        this.separator=eleInputboxBean.attributeValue("separator");
        if(separator==null||separator.equals("")) this.separator=" ";
    }
}
