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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SubmitFunctionParamBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.EditableDetailReportType2;
import com.wabacus.system.component.application.report.EditableListFormReportType;
import com.wabacus.system.component.application.report.EditableListReportType2;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.util.Tools;

public abstract class AbsInputBox implements Cloneable
{
    private static Log log=LogFactory.getLog(AbsInputBox.class);

    protected String defaultvalue;//输入框的默认显示值，如果当前输入框没有数据进行显示时，将显示这里配置的默认值，只有此输入框属于编辑列，即配置在<col/>下时，此默认值才有效，当为查询条件下的输入框时，此属性无效。

    protected String defaultstyleproperty;//在wabacus.cfg.xml中配置的默认样式字符串，在editablelist2/editabledetail2两种报表类型的编辑列输入框中不会用到这里的样式，在其它任意场合的输入框中都会用到这里的样式，且不会被覆盖

    protected String inputboxparams;

    protected String styleproperty;

    protected String styleproperty2;

    private String description;

    protected Map<String,String> mStyleProperties2;//存放从styleproperty转变为styleproperty2后去掉的js事件属性名和属性值，以便在客户端能加上

    protected String language;

    private String jsvalidate=null;

    protected String[][] servervalidate=null;

    protected String typename;

    protected IInputBoxOwnerBean owner;

    protected List<SubmitFunctionParamBean> lstSubmitFunctionParams;

    private AutoCompleteConfigBean autocompleteBean;//自动填充其它列数据的配置

    private List<String> lstChildids;

    protected int fillmode=1;

    protected int displaymode=1;

    private String displayon;//如果当前输入框是配置给可编辑报表，且当此列没有出现在<insert/>和<update/>中时，如果想显示输入框，则通过此属性指定在什么条件下需要显示输入框，可配置值为insert、update、insert|update，此属性对查询条件中的输入框无效

    public AbsInputBox(String typename)
    {
        this.typename=typename;
    }

    public IInputBoxOwnerBean getOwner()
    {
        return owner;
    }

    public void setOwner(IInputBoxOwnerBean owner)
    {
        this.owner=owner;
    }

    public List<String> getLstChildids()
    {
        return lstChildids;
    }

    public void addChildInputboxId(String inputboxid)
    {
        if(inputboxid==null||inputboxid.trim().equals("")) return;
        if(this.lstChildids==null) this.lstChildids=new ArrayList<String>();
        if(!this.lstChildids.contains(inputboxid)) this.lstChildids.add(inputboxid);
    }

    protected String getInputBoxValue(ReportRequest rrequest,String value)
    {
        if(value==null||value.trim().equals("")&&defaultvalue!=null)
        {
            value=ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
        }
        if(value==null) value="";
        return Tools.htmlEncode(value);
    }

    public String getLanguage()
    {
        return language;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public String getDefaultvalue(ReportRequest rrequest)
    {
        if(defaultvalue==null) return null;
        return ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
    }

    public AutoCompleteConfigBean getAutocompleteBean()
    {
        return autocompleteBean;
    }

    public void setAutocompleteBean(AutoCompleteConfigBean autocompleteBean)
    {
        this.autocompleteBean=autocompleteBean;
    }

    public String getDefaultlabel(ReportRequest rrequest)
    {
        if(defaultvalue!=null) return ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
        return "";
    }

    public String getDisplayStringValue(ReportRequest rrequest,String value,String dynstyleproperty,boolean isReadonly)
    {
        if(this.isJsValidateOnBlur())
        {
            dynstyleproperty=Tools.mergeHtmlTagPropertyString(dynstyleproperty,"onblur=\"try{"+this.getBlurValidateEvent(rrequest)
                    +"}catch(e){logErrorsAsJsFileLoad(e);}\"",1);
        }
        return doGetDisplayStringValue(rrequest,value,Tools.mergeHtmlTagPropertyString(this.styleproperty,dynstyleproperty,1),isReadonly);
    }

    public void setDefaultstyleproperty(String defaultstyleproperty)
    {
        this.defaultstyleproperty=defaultstyleproperty;
    }

    public abstract String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,
            boolean isReadonly);

    protected abstract String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly);

    public abstract String filledInContainer(String onblur);

    protected abstract String getDefaultStylePropertyForDisplayMode2();

    public void setDefaultFillmode(AbsReportType reportTypeObj)
    {
        if(reportTypeObj instanceof EditableListFormReportType)
        {
            this.fillmode=1;
        }else if(reportTypeObj instanceof EditableListReportType2||reportTypeObj instanceof EditableDetailReportType2)
        {
            this.fillmode=2;
        }else
        {
            this.fillmode=1;
        }
    }


    public String initDisplay(ReportRequest rrequest)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(initDisplaySpanStart(rrequest)).append(">");
        resultBuf.append(initDisplaySpanContent(rrequest));
        resultBuf.append("</span>");
        return resultBuf.toString();
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<span id=\"span_"+this.owner.getInputBoxId()+"_span\" style=\"display:none;\"");
        if(this.styleproperty2!=null&&!this.styleproperty2.trim().equals(""))
        {
            resultBuf.append(" styleproperty=\""+Tools.jsParamEncode(this.styleproperty2)+"\"");
        }
        if(this.mStyleProperties2!=null)
        {
            for(Entry<String,String> entryTmp:this.mStyleProperties2.entrySet())
            {
                if(entryTmp.getValue()==null||entryTmp.getValue().trim().equals("")) continue;
                resultBuf.append(" ").append(entryTmp.getKey()).append("_propertyvalue=\"").append(entryTmp.getValue()).append("\"");
            }
        }
        if(this.isJsValidateOnBlur())
        {//如果需要失去焦点时的客户端校验
            resultBuf.append(" jsvalidate_onblur_method=\"").append(this.getBlurValidateEvent(rrequest)).append("\"");
        }
        if(this.inputboxparams!=null&&!this.inputboxparams.trim().equals(""))
        {
            resultBuf.append(" inputboxparams=\""+this.inputboxparams.trim()+"\"");
        }
        if(this.lstChildids!=null&&this.lstChildids.size()>0)
        {
            resultBuf.append(" childboxids=\"");
            for(String childidTmp:this.lstChildids)
            {
                resultBuf.append(childidTmp).append(";");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==';') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("\"");
        }
        return resultBuf.toString();
    }

    protected String initDisplaySpanContent(ReportRequest rrequest)
    {
        return "";
    }

    protected String getInputBoxCommonFilledProperties()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("boxstr=boxstr+\" id= '\"+name+\"'").append(" name='\"+name+\"'");
        resultBuf.append(" typename='"+this.typename+"' \"+styleproperty;");
        resultBuf.append("boxstr=boxstr+\" style=\\\"text-align:\"+textalign+\";\";");
        resultBuf.append("if(wid!=null&&parseInt(wid)>0){boxstr=boxstr+\"width:\"+wid+\";\";}");
        resultBuf.append("boxstr=boxstr+style_propertyvalue+\"\\\"\";");
        return resultBuf.toString();
    }

    public String attachInfoForDisplay()
    {
        return "";
    }

    public String createSetInputBoxValueByIdJs()
    {
        StringBuffer buf=new StringBuffer();
        buf.append("var boxObj=document.getElementById(id);");
        buf.append("if(boxObj){boxObj.value=newvalue;}");
        return buf.toString();
    }

    public String createGetValueByIdJs()
    {
        StringBuffer buf=new StringBuffer();
        buf.append("var boxObj=document.getElementById(id);");
        buf.append("if(boxObj!=null){ return boxObj.value;}");
        buf.append("return null;");
        return buf.toString();
    }

    public String createGetValueByInputBoxObjJs()
    {
        return "value=boxObj.value; label=boxObj.value;";
    }

    protected boolean isJsValidateOnBlur()
    {
        if(this.getOwner()==null) return false;
        if(this.getOwner().getReportBean().getJsvalidatetype()==0||this.getJsvalidate()==null||this.getJsvalidate().trim().equals("")) return false;
        return true;
    }

    protected String getBlurValidateEvent(ReportRequest rrequest)
    {
        if(!isJsValidateOnBlur()) return "";
        return "validate_"+this.owner.getInputBoxId()+"(this,'"
                +JavaScriptAssistant.getInstance().getRuntimeParamsValueJsonString(rrequest,lstSubmitFunctionParams)+"');";
    }

    protected String getInputBoxId(ReportRequest rrequest)
    {
        String inputboxid=rrequest.getStringAttribute("DYN_INPUTBOX_ID");
        if(inputboxid==null||inputboxid.trim().equals("")) inputboxid=owner.getInputBoxId();
        return inputboxid;
    }

    //    protected String getOnKeypressEventProp(String style_property,boolean type)
    
    
    
    
    //            String onkeypress=Tools.getPropertyValueByName("onkeypress",style_property.toLowerCase(),false);
    
    //            {//没有在styleproperty中指定onkeypress事件，则加上默认的事件
    
    //                {//如果要显示输入框边框，由按回车键时，跳到下一个输入框（一般是输入框显示在表单中的情况）
    
    
    //                {//如果不显示输入框边框，说明输入框只是做为其它可编辑元素的一部分，此时按回车键时，失去焦点即可（一般是editablelist2/editabledetail2的情况）
    //                    resultStr=" onkeypress=\"return onKeyEvent(event);\" ";
    
    
    
    
    //            resultStr="if(!styleproperty||styleproperty.toLowerCase().indexOf('onkeypress')<0){";
    
    
    
    
    //        return resultStr;
    
    
    
    
    //        if(type)
    //        {//如果是被getDisplayStringValue()调用
    
    
    
    
    //            {
    
    
    
    //        {//如果是被filledInContainer()调用
    
    //        }
    
    
    protected String addReadonlyToStyleProperty1(String style_property)
    {
        if(style_property==null)
        {
            style_property="";
        }else if(style_property.toLowerCase().indexOf(" readonly ")>=0)
        {
            return style_property;
        }
        return style_property+" readonly ";
    }

    protected String addReadonlyToStyleProperty2(String style_property)
    {
        if(style_property==null)
        {
            style_property="";
        }else if(style_property.toLowerCase().indexOf(" disabled ")>=0)
        {
            return style_property;
        }
        return style_property+" disabled ";
    }

    public String getTypename()
    {
        return typename;
    }

    public String getStyleproperty()
    {
        return styleproperty;
    }

    public void setStyleproperty(String styleproperty)
    {
        this.styleproperty=styleproperty;
    }

    public String getJsvalidate()
    {
        return jsvalidate;
    }

    protected void setJsvalidate(String jsvalidate)
    {
        this.jsvalidate=jsvalidate;
    }

    public String getDescription(ReportRequest rrequest)
    {
        if(this.description!=null&&!this.description.trim().equals(""))
        {
            return rrequest.getI18NStringValue(this.description);
        }
        return "";
    }

    public List<SubmitFunctionParamBean> getLstSubmitFunctionParams()
    {
        return lstSubmitFunctionParams;
    }

    public void setLstSubmitFunctionParams(List<SubmitFunctionParamBean> lstSubmitFunctionParams)
    {
        this.lstSubmitFunctionParams=lstSubmitFunctionParams;
    }

    public String[][] getServervalidate()
    {
        return servervalidate;
    }

    protected void setServervalidate(String[][] servervalidate)
    {
        this.servervalidate=servervalidate;
    }

    public int getFillmode()
    {
        return fillmode;
    }

    public void setFillmode(int fillmode)
    {
        this.fillmode=fillmode;
    }

    public int getDisplaymode()
    {
        return displaymode;
    }

    public void setDisplaymode(int displaymode)
    {
        this.displaymode=displaymode;
    }

    public String getDisplayon()
    {
        return displayon;
    }

    public Object clone(IInputBoxOwnerBean owner)
    {
        try
        {
            AbsInputBox inputBoxNew=(AbsInputBox)super.clone();
            inputBoxNew.setOwner(owner);
            if(this.autocompleteBean!=null)
            {
                inputBoxNew.setAutocompleteBean(autocompleteBean.clone());
                owner.getReportBean().addInputboxWithAutoComplete(inputBoxNew);
            }
            return inputBoxNew;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone输入框对象失败",e);
        }
    }

    public void loadInputBoxConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleInputboxBean)
    {
        this.owner=ownerbean;
        if(eleInputboxBean==null) return;
        if(ownerbean instanceof EditableReportColBean)
        {
            String autocomplete=eleInputboxBean.attributeValue("autocomplete");
            if(autocomplete!=null&&!autocomplete.trim().equals(""))
            {//为此输入框配置了输入数据时自动填充其它列数据的功能
                List<String> lstTmp=Tools.parseStringToList(autocomplete,";",false);
                if(lstTmp.size()>0)
                {
                    this.autocompleteBean=new AutoCompleteConfigBean();
                    this.autocompleteBean.setUlstCompleteColumns(lstTmp);
                    String autocompletecondition=eleInputboxBean.attributeValue("autocompletecondition");
                    if(autocompletecondition!=null&&!autocompletecondition.trim().equals(""))
                    {
                        this.autocompleteBean.setColConditionExpression(autocompletecondition.trim());
                    }
                    String autocompleteconditionref=eleInputboxBean.attributeValue("autocompleteconditionref");
                    if(autocompleteconditionref!=null&&!autocompleteconditionref.trim().equals(""))
                    {
                        this.autocompleteBean.setUlstRefConditionNames(Tools.parseStringToList(autocompleteconditionref.trim(),";",false));
                    }
                    String autocompletemultiple=eleInputboxBean.attributeValue("autocompletemultiple");
                    this.autocompleteBean.setEnableMultipleRecords(autocompletemultiple==null
                            ||!autocompletemultiple.toLowerCase().trim().equals("false"));
                    String autocompletesql=eleInputboxBean.attributeValue("autocompletesql");
                    if(autocompletesql!=null) this.autocompleteBean.setAutocompletesql(autocompletesql.trim());
                }
            }
        }
        String description=eleInputboxBean.attributeValue("description");
        if(description!=null)
        {
            this.description=Config.getInstance().getResourceString(null,ownerbean.getReportBean().getPageBean(),description,true);
        }
        styleproperty=eleInputboxBean.attributeValue("styleproperty");
        styleproperty=styleproperty==null?"":Tools.formatStringBlank(styleproperty.trim());
        String jsvalidate=eleInputboxBean.attributeValue("jsvalidate");
        if(jsvalidate!=null)
        {
            this.setJsvalidate(jsvalidate.trim());
        }
        String servervalidate=eleInputboxBean.attributeValue("servervalidate");
        if(servervalidate!=null&&!servervalidate.trim().equals(""))
        {
            List<String> lstMethods=Tools.parseStringToList(servervalidate.trim(),',','\"');
            String[][] tempArray=new String[lstMethods.size()][2];
            int k=0;
            for(String methodname:lstMethods)
            {
                methodname=methodname==null?"":methodname.trim();
                if(methodname.trim().equals("")) continue;
                String finalmethodname=methodname;
                String errormsg=null;
                int lidx=methodname.indexOf("(");
                int ridx=methodname.indexOf(")");
                if(lidx>0&&lidx<ridx)
                {
                    finalmethodname=methodname.substring(0,lidx);
                    errormsg=methodname.substring(lidx+1,ridx).trim();
                    //System.out.println(errormsg);
                    errormsg=Config.getInstance().getResourceString(null,ownerbean.getReportBean().getPageBean(),errormsg,true);
                }
                if(finalmethodname==null||finalmethodname.trim().equals("")) continue;
                if(errormsg==null||errormsg.trim().equals(""))
                {
                    if(finalmethodname.trim().equalsIgnoreCase("isnotempty"))
                    {
                        errormsg="查询条件{0}不能为空";
                    }else
                    {
                        errormsg="输入的查询条件{0}不合要求";
                    }
                }
                tempArray[k][0]=finalmethodname;
                tempArray[k++][1]=errormsg;
            }
            this.setServervalidate(tempArray);
        }
        String inputboxparams=eleInputboxBean.attributeValue("inputboxparams");
        if(inputboxparams!=null) this.inputboxparams=inputboxparams.trim();
        String _language=eleInputboxBean.attributeValue("language");
        if(_language==null||_language.trim().equals(""))
        {
            this.language=null;
        }else
        {
            this.language=_language;
        }
        String displayon=eleInputboxBean.attributeValue("displayon");
        if(displayon!=null)
        {
            displayon=displayon.toLowerCase().trim();
            if(!displayon.equals(""))
            {
                this.displayon="";
                List<String> lstTmp=Tools.parseStringToList(displayon,"|");
                for(String tmp:lstTmp)
                {
                    if(!tmp.trim().equals("insert")&&!tmp.trim().equals("update")) continue;
                    this.displayon=this.displayon+tmp+"|";
                }
            }else
            {
                this.displayon=null;
            }
        }
    }

    public void doPostLoad(IInputBoxOwnerBean ownerbean)
    {
        AbsReportType reportTypeObj=Config.getInstance().getReportType(ownerbean.getReportBean().getType());
        setDisplaymode(reportTypeObj,ownerbean);
        if(this.displaymode==1)
        {
            this.styleproperty=this.styleproperty==null?"":this.styleproperty.trim();
            if(this.styleproperty.toLowerCase().startsWith("(overwrite)"))
            {
                this.styleproperty=this.styleproperty.substring("(overwrite)".length());
            }else if(this.styleproperty.toLowerCase().startsWith("[overwrite]"))
            {
                this.styleproperty=this.styleproperty.substring("[overwrite]".length());
                this.styleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,this.styleproperty,0);
            }else
            {
                this.styleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,this.styleproperty,1);
            }
        }else
        {
            if(this.styleproperty!=null
                    &&(this.styleproperty.toLowerCase().trim().startsWith("(overwrite)")||this.styleproperty.trim().toLowerCase().startsWith(
                            "[overwrite]")))
            {
                log.warn("报表"+ownerbean.getReportBean().getPath()
                        +"为editablelist2/editabledetail2之一的报表类型，不能在其编辑列的输入框上的styleproperty配置(overwrite)或[overwrite]方式");
                this.styleproperty=this.styleproperty.trim().substring("(overwrite)".length());
            }
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.getDefaultStylePropertyForDisplayMode2(),this.styleproperty,1);
        }

        processRelativeInputBoxes();

        if(this.autocompleteBean!=null) this.autocompleteBean.doPostLoad(this);
        if(this.autocompleteBean!=null)
        {
            owner.getReportBean().addInputboxWithAutoComplete(this);
            ColBean cbOwner=(ColBean)((EditableReportColBean)owner).getOwner();
            styleproperty=Tools.mergeHtmlTagPropertyString(styleproperty,
                    "onfocus=\"autoComplete_oldData=getInputBoxValue(this.getAttribute('id'),this.getAttribute('typename'));\"",1);
            StringBuffer blurEventBuf=new StringBuffer();
            blurEventBuf.append("loadAutoCompleteInputboxData(");
            blurEventBuf.append("'").append(owner.getReportBean().getPageBean().getId()).append("','").append(owner.getReportBean().getId()).append(
                    "',");
            blurEventBuf.append("this.getAttribute('id'),'").append(cbOwner.getProperty()).append("','");
            for(String colPropTmp:this.autocompleteBean.getLstColPropertiesInCondition())
            {
                if(colPropTmp==null||colPropTmp.trim().equals("")) continue;
                blurEventBuf.append(colPropTmp).append(";");
            }
            blurEventBuf.append("');");
            styleproperty=Tools.mergeHtmlTagPropertyString(styleproperty,"onblur=\"try{"+blurEventBuf.toString()
                    +"}catch(e){logErrorsAsJsFileLoad(e);}\"",1);
        }
        processStylePropertyAfterMerged(reportTypeObj,ownerbean);
        if((ownerbean instanceof EditableReportColBean)
                &&(reportTypeObj instanceof EditableDetailReportType2||reportTypeObj instanceof EditableListReportType2))
        {
            processStylePropertyForFillInContainer();
        }

    }

    protected void processRelativeInputBoxes()
    {
        if(this.lstChildids==null||this.lstChildids.size()==0) return;
        ReportBean rbean=this.owner.getReportBean();
        if(this.displaymode==1)
        {//主输入框数据有变时，立即刷新子下拉框选项数据
            StringBuffer childidAndParamsBuf=new StringBuffer("{");
            SelectBox childBoxObjTmp;
            boolean isConditionBox=this.owner instanceof ConditionBean;
            for(String childidTmp:this.lstChildids)
            {
                childidAndParamsBuf.append(childidTmp).append(":'");
                if(isConditionBox)
                {
                    childBoxObjTmp=rbean.getChildSelectBoxInConditionById(childidTmp);
                }else
                {
                    childBoxObjTmp=rbean.getChildSelectBoxInColById(childidTmp);
                }
                childidAndParamsBuf.append(childBoxObjTmp.getAllParentIdsAsString()).append("',");
            }
            if(childidAndParamsBuf.charAt(childidAndParamsBuf.length()-1)==',') childidAndParamsBuf.deleteCharAt(childidAndParamsBuf.length()-1);
            childidAndParamsBuf.append("}");
            String event=getRefreshChildboxDataEventName()+"=\"reloadSelectBoxData('"+rbean.getPageBean().getId()+"','"+rbean.getId()+"',this,"
                    +childidAndParamsBuf.toString()+","+isConditionBox+")\"";
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,event,1);
        }else
        {
            String event=getRefreshChildboxDataEventName()+"=\"resetChildSelectBoxData(this)\"";
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,event,1);
        }
    }

    protected String getRefreshChildboxDataEventName()
    {
        return "onblur";
    }
    
    private void setDisplaymode(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        if(ownerbean instanceof EditableReportColBean
                &&((reportTypeObj instanceof EditableListReportType2&&!(reportTypeObj instanceof EditableListFormReportType))||reportTypeObj instanceof EditableDetailReportType2))
        {
            this.displaymode=2;
        }else
        {
            this.displaymode=1;
        }
    }

    protected void processStylePropertyAfterMerged(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        if(isJsValidateOnBlur())
        {
            this.styleproperty=Tools
                    .mergeHtmlTagPropertyString(
                            this.styleproperty,
                            "onfocus=\"try{if(this.errorPromptObj==null){this.errorPromptObj=createJsValidateTipObj(this);}}catch(e){logErrorsAsJsFileLoad(e);}\"",
                            1);
        }
    }

    protected void processStylePropertyForFillInContainer()
    {
        this.mStyleProperties2=new HashMap<String,String>();
        this.styleproperty2=this.styleproperty;
        String onfocus=Tools.getPropertyValueByName("onfocus",this.styleproperty,false);
        if(onfocus!=null&&!onfocus.trim().equals("")) this.mStyleProperties2.put("onfocus",onfocus);
        this.styleproperty2=Tools.removePropertyValueByName("onfocus",this.styleproperty2);
        String onblur=Tools.getPropertyValueByName("onblur",this.styleproperty,false);
        if(onblur!=null&&!onblur.trim().equals("")) this.mStyleProperties2.put("onblur",onblur);
        this.styleproperty2=Tools.removePropertyValueByName("onblur",this.styleproperty2);
        String style=Tools.getPropertyValueByName("style",this.styleproperty,false);
        if(style!=null&&!style.trim().equals("")) this.mStyleProperties2.put("style",style);
        this.styleproperty2=Tools.removePropertyValueByName("style",this.styleproperty2);
    }
}
