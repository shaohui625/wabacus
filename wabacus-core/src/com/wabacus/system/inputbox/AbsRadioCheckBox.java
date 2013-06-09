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

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Tools;

public abstract class AbsRadioCheckBox extends AbsInputBox implements Cloneable
{
    protected int inline_count;
    
    protected List<OptionBean> lstOptions=null;

    public AbsRadioCheckBox(String typename)
    {
        super(typename);
    }

    public void setLstOptions(List<OptionBean> lstOptions)
    {
        this.lstOptions=lstOptions;
    }

    protected abstract String getBoxType();
    
    protected abstract boolean isSelectedValue(String selectedvalues,String optionvalue);
    
    protected abstract String checkSelectedValueInClient();
    
    
    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(isReadonly) style_property=addReadonlyToStyleProperty2(style_property);
        ReportBean rbean=owner.getReportBean();
        String inputboxid=getInputBoxId(rrequest);
        value=getInputBoxValue(rrequest,value);
        String label_temp="";
        String value_temp="";
        String selected="";
        List<String[]> lstOptionsList=getOptionNameAndValueList(rrequest,rbean);
        int count=0;
        for(String[] strArrTmp:lstOptionsList)
        {
            label_temp=strArrTmp[0];
            value_temp=strArrTmp[1];
            if(this.inline_count>0&&count>0&&count%this.inline_count==0)
            {
                resultBuf.append("<br>");
            }
            value_temp=value_temp==null?"":value_temp.trim();
            if(isSelectedValue(value,value_temp)) selected=" checked ";
            resultBuf.append("<input type=\""+getBoxType()+"\" typename='"+typename+"' name=\""+inputboxid+"\" id=\""+inputboxid+"\"");
            resultBuf.append(" label=\"").append(label_temp).append("\" value=\""+value_temp+"\" ").append(selected);
            if(style_property!=null) resultBuf.append(" ").append(style_property);
            //            resultBuf.append(" onclick=\"this.focus()\"");//加上onclick='this.focus()'是为了兼容google浏览器
            resultBuf.append(">").append(label_temp).append("</input> ");
            selected="";
            count++;
        }
        resultBuf.append(this.getDescription(rrequest));//显示描述信息
        return resultBuf.toString().trim();
    }
    
    protected List<String[]> getOptionNameAndValueList(ReportRequest rrequest,ReportBean rbean)
    {
        List<String[]> lstResults=(List<String[]>)rrequest.getAttribute("LISTOPTIONS_"+rbean.getId()+owner.getInputBoxId());
        if(lstResults==null)
        {
            lstResults=new ArrayList<String[]>();
            String label_temp="";
            String value_temp="";
            for(OptionBean obean:lstOptions)
            {
                if(obean.getSourceType()==1&&obean.getSql()!=null)
                {
                    lstResults.addAll(ReportAssistant.getInstance().getOptionListFromDB(rrequest,rbean,obean.getSql(),obean));
                }else
                {
                    label_temp=rrequest.getI18NStringValue(obean.getLabel());
                    value_temp=obean.getValue();
                    String[] items=new String[2];
                    items[0]=label_temp;
                    items[1]=value_temp;
                    lstResults.add(items);
                }
            }
            rrequest.setAttribute("LISTOPTIONS_"+rbean.getId()+owner.getInputBoxId(),lstResults);
        }
        return lstResults;
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        if(this.inline_count<=0) return super.initDisplaySpanStart(rrequest);
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        resultBuf.append(" inline_count=\"").append(this.inline_count).append("\"");
        return resultBuf.toString();
    }

    protected String initDisplaySpanContent(ReportRequest rrequest)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.initDisplaySpanContent(rrequest));
        List<String[]> lstOptionsResult=getOptionNameAndValueList(rrequest,owner.getReportBean());
        String name_temp;
        String value_temp;
        for(String[] items:lstOptionsResult)
        {
            name_temp=items[0];
            value_temp=items[1];
            value_temp=value_temp==null?"":value_temp.trim();
            resultBuf.append("<span value=\""+value_temp+"\" label=\""+name_temp+"\"></span>");
        }
        return resultBuf.toString();
    }

    public String filledInContainer(String onblur)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("if(inputboxSpanObj!=null){");
        resultBuf.append(" var inline_count=inputboxSpanObj.getAttribute('inline_count');");
        resultBuf.append(" var iinlinecount=0;if(inline_count!=null&&inline_count!='') iinlinecount=parseInt(inline_count,10);");
        resultBuf.append("  var childs=inputboxSpanObj.getElementsByTagName(\"span\");");
        resultBuf.append("  if(childs!=null&&childs.length>0){");
        resultBuf.append("      var optionlabel=null;var optionvalue=null;");
        resultBuf.append("      for(var i=0,len=childs.length;i<len;i++){ ");
        resultBuf.append("          if(iinlinecount>0&&i>0&&i%iinlinecount==0) boxstr=boxstr+\"<br>\";");
        resultBuf.append("          optionlabel=childs[i].getAttribute('label'); optionvalue=childs[i].getAttribute('value');");
        resultBuf.append("          boxstr=boxstr+\"<input type='"+this.getBoxType()+"'  value=\\\"\"+optionvalue+\"\\\" label='\"+optionlabel+\"'\";");
        resultBuf.append(getInputBoxCommonFilledProperties());
        resultBuf.append("          "+this.checkSelectedValueInClient()+" boxstr=boxstr+\" checked\";");
        resultBuf.append("          boxstr=boxstr+\" onblur=\\\"try{\"+onblurmethod+\";fillGroupBoxValue(this,'"+this.typename
                +"','\"+name+\"','\"+reportguid+\"','\"+reportfamily+\"',\"+fillmode+\");}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";");
        resultBuf.append("          boxstr=boxstr+\" onfocus=\\\"try{\"+onfocusmethod+\";setGroupBoxStopFlag('\"+name+\"');}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";");
        resultBuf.append("          boxstr=boxstr+\">\"+optionlabel+\"</input>\";");
        resultBuf.append("      }");
        resultBuf.append("  }");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        List<String[]> lstOptionsResult=(List<String[]>)specificDataObj;
        StringBuffer resultBuf=new StringBuffer();
        dynstyleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,dynstyleproperty,1);
        if(isReadonly) dynstyleproperty=addReadonlyToStyleProperty1(dynstyleproperty);//如果是只读，则将只读属性添加到styleproperty中
        if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
        {
            String name_temp="";
            String value_temp="";
            String selected="";
            for(String[] items:lstOptionsResult)
            {
                name_temp=items[0];
                value_temp=items[1];
                value_temp=value_temp==null?"":value_temp.trim();
                if(isSelectedValue(value,value_temp)) selected=" checked ";
                resultBuf.append("<input type=\""+this.getBoxType()+"\" value=\""+value_temp+"\" ").append(selected);
                if(dynstyleproperty!=null) resultBuf.append(" ").append(dynstyleproperty);
                resultBuf.append(">").append(name_temp).append("</input> ");
                selected="";
            }
        }
        return resultBuf.toString();
    }

    protected String getInputBoxCommonFilledProperties()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("boxstr=boxstr+\" id= '\"+name+\"'").append(" name='\"+name+\"'");
        resultBuf.append(" typename='"+this.typename+"' \"+styleproperty;");
        resultBuf.append("boxstr=boxstr+\" style=\\\"\"+style_propertyvalue+\"\\\"\";");
        return resultBuf.toString();
    }

    public void loadInputBoxConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(ownerbean,eleInputboxBean);
        if(eleInputboxBean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+ownerbean.getReportBean().getPath()+"的单选框类型输入框失败，没有配置单选项");
        }
        List<OptionBean> lstObs=new ArrayList<OptionBean>();
        List<XmlElementBean> lstOptionElements=eleInputboxBean.getLstChildElementsByName("option");
        if(lstOptionElements!=null&&lstOptionElements.size()>0)
        {
            lstObs=ComponentConfigLoadManager.loadOptionInfo(lstOptionElements,ownerbean);
        }
        if(lstObs==null||lstObs.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+ownerbean.getReportBean().getPath()+"配置的单选框类型的输入框失败，没有配置单选项");
        }
        this.setLstOptions(lstObs);
        String inlinecount=eleInputboxBean.attributeValue("inlinecount");
        if(inlinecount!=null&&!inlinecount.trim().equals(""))
        {
            this.inline_count=Integer.parseInt(inlinecount.trim());
        }
        
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "onkeypress='return onKeyEvent(event);'";
    }

    public void doPostLoad(IInputBoxOwnerBean ownerbean)
    {
        super.doPostLoad(ownerbean);
        boolean isPreparedStmt=false;
        if(ownerbean.getReportBean().getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
        {
            isPreparedStmt=true;
        }
        List<ConditionBean> lstCons;
        for(OptionBean obTmp:this.lstOptions)
        {
            if(obTmp.getSourceType()!=1) continue;
            lstCons=obTmp.getLstConditions();
            if(lstCons==null||lstCons.size()==0) continue;
            for(ConditionBean cbTmp:lstCons)
            {
                if(isPreparedStmt) cbTmp.getConditionExpression().parseConditionExpression(cbTmp);
                if(cbTmp.isConditionValueFromUrl())
                {
                    ownerbean.getReportBean().addParamNameFromURL(cbTmp.getName());
                }
            }
        }
    }

    protected void processStylePropertyAfterMerged(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        super.processStylePropertyAfterMerged(reportTypeObj,ownerbean);
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onclick=\"this.focus();\"",1);
    }

    public Object clone(IInputBoxOwnerBean owner)
    {
        AbsRadioCheckBox boxObjNew=(AbsRadioCheckBox)super.clone(owner);
        if(lstOptions!=null)
        {
            List<OptionBean> lstOptionsNew=new ArrayList<OptionBean>();
            for(OptionBean obTmp:lstOptions)
            {
                lstOptionsNew.add((OptionBean)obTmp.clone());
            }
            boxObjNew.setLstOptions(lstOptionsNew);
        }
        return boxObjNew;
    }
}
