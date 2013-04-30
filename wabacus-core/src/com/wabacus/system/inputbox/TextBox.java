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

import com.wabacus.config.Config;
import com.wabacus.config.typeprompt.AbsTypePromptDataSource;
import com.wabacus.config.typeprompt.SQLPromptDataSource;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Tools;

public class TextBox extends AbsInputBox implements Cloneable
{
    private TypePromptBean typePromptBean;

    public TextBox(String typename)
    {
        super(typename);
    }

    public TypePromptBean getTypePromptBean()
    {
        return typePromptBean;
    }

    public void setTypePromptBean(TypePromptBean typePromptBean)
    {
        this.typePromptBean=typePromptBean;
    }

    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuffer resultBuf=new StringBuffer();
        String inputboxid=getInputBoxId(rrequest);
        resultBuf.append("<input  type='"+this.getTextBoxType()+"' typename='"+typename+"' id='"+inputboxid+"' name='"+inputboxid+"'");
        resultBuf.append(" value=\""+getInputBoxValue(rrequest,value)+"\"  ");
        style_property=Tools.mergeHtmlTagPropertyString(style_property,getTextBoxExtraStyleProperty(rrequest,isReadonly),1);
        if(isReadonly) style_property=addReadonlyToStyleProperty1(style_property);
        if(style_property!=null) resultBuf.append(" ").append(style_property).append(" ");
        resultBuf.append("/>");
        resultBuf.append(this.getDescription(rrequest));
        return resultBuf.toString();
    }

    protected String getTextBoxType()
    {
        return "text";
    }
    
    protected String getTextBoxExtraStyleProperty(ReportRequest rrequest,boolean isReadonly)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(typePromptBean!=null)
        {
            resultBuf.append(" onfocus=\"try{if(this.obj==null){this.obj=initializeTypePromptProperties(this,'"
                    +getTypePromptJsonString(getInputBoxId(rrequest))+"');}}catch(e){logErrorsAsJsFileLoad(e);}\"");
        }
        return resultBuf.toString();
    }
    
    private String getTypePromptJsonString(String inputboxid)
    {
        if(typePromptBean==null) return "";
        StringBuffer resultBuf=new StringBuffer("{");
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        String serverurl=Config.showreport_url+token+"PAGEID="+owner.getReportBean().getPageBean().getId()+"&REPORTID="+owner.getReportBean().getId()
                +"&INPUTBOXID="+inputboxid+"&ACTIONTYPE=GetTypePromptDataList";
        resultBuf.append("serverUrl:\"").append(serverurl).append("\"");
        resultBuf.append(",spanOutputWidth:").append(typePromptBean.getResultspanwidth());
        resultBuf.append(",resultCount:").append(typePromptBean.getResultcount());
        resultBuf.append(",useTimeout:").append(typePromptBean.isTimeout());
        resultBuf.append(",isShowTitle:").append(typePromptBean.isShowtitle());
        if(typePromptBean.getCallbackmethod()!=null&&!typePromptBean.getCallbackmethod().trim().equals(""))
        {
            resultBuf.append(",callbackmethod:").append(typePromptBean.getCallbackmethod());
        }
        StringBuffer colBuf=new StringBuffer();
        for(TypePromptColBean tpColBean:typePromptBean.getLstPColBeans())
        {
            colBuf.append("{");
            colBuf.append("collabel:\"").append(tpColBean.getLabel()).append("\"");
            colBuf.append(",colvalue:\"").append(tpColBean.getValue()).append("\"");
            colBuf.append(",coltitle:\"").append(tpColBean.getTitle()==null?"":tpColBean.getTitle()).append("\"");
            colBuf.append(",matchmode:").append(tpColBean.getMatchmode());
            colBuf.append("},");
        }
        if(colBuf.charAt(colBuf.length()-1)==',')  colBuf.deleteCharAt(colBuf.length()-1);
        resultBuf.append(",colsArray:[").append(colBuf.toString()).append("]");
        resultBuf.append("}");
        return Tools.jsParamEncode(resultBuf.toString());
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        if(this.typePromptBean==null) return super.initDisplaySpanStart(rrequest);
        //配置了输入联想功能
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        resultBuf.append(" typePrompt=\"").append(getTypePromptJsonString(this.owner.getInputBoxId())).append("\"");
        return resultBuf.toString();
    }

    public String filledInContainer(String onblur)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(" boxstr=\"<input type='"+this.getTextBoxType()+"' value=\\\"\"+boxValue+\"\\\"\";");
        resultBuf.append(getInputBoxCommonFilledProperties());
        resultBuf.append("boxstr=boxstr+\" onblur=\\\"try{\"+onblurmethod+\"").append(onblur).append("}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";");
        resultBuf.append("var typePrompt=null;if(inputboxSpanObj!=null){typePrompt=inputboxSpanObj.getAttribute('typePrompt');}");
        resultBuf.append("if(onfocusmethod!=null&&onfocusmethod!=''||typePrompt!=null&&typePrompt!=''){");
        resultBuf.append("  boxstr=boxstr+\" onfocus=\\\"try{\"+onfocusmethod;");
        
        resultBuf
                .append("   if(typePrompt!=null&&typePrompt!=''){boxstr=boxstr+\"if(this.obj==null) this.obj=initializeTypePromptProperties(this,'\"+typePrompt+\"');\";}");
        resultBuf.append("  boxstr=boxstr+\"}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";");
        resultBuf.append("}");
        resultBuf.append("boxstr=boxstr+\">\";");
        return resultBuf.toString();
    }
    
    
    
    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<input  type='"+this.getTextBoxType()+"'  value=\""+value+"\"");
        dynstyleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,dynstyleproperty,1);
        if(isReadonly) dynstyleproperty=addReadonlyToStyleProperty1(dynstyleproperty);
        if(dynstyleproperty!=null) resultBuf.append(" ").append(dynstyleproperty).append(" ");
        resultBuf.append("/>");
        return resultBuf.toString();
    }

    public void loadInputBoxConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(ownerbean,eleInputboxBean);
        if(eleInputboxBean==null) return;
        
        XmlElementBean eleTypeprompt=eleInputboxBean.getChildElementByName("typeprompt");
        if(eleTypeprompt!=null)
        {
            ownerbean.getReportBean().addTextBoxWithingTypePrompt(this);
            typePromptBean=new TypePromptBean();
            String width=eleTypeprompt.attributeValue("width");
            if(width!=null&&!width.trim().equals(""))
            {
                typePromptBean.setResultspanwidth(Tools.getWidthHeightIntValue(width));
            }
            String count=eleTypeprompt.attributeValue("count");
            if(count!=null&&!count.trim().equals(""))
            {
                typePromptBean.setResultcount(Integer.parseInt(count.trim()));
            }
            String timeout=eleTypeprompt.attributeValue("timeout");
            if(timeout!=null&&!timeout.trim().equals(""))
            {
                typePromptBean.setTimeout(Boolean.parseBoolean(timeout.trim()));
            }
            String callbackmethod=eleTypeprompt.attributeValue("callbackmethod");
            if(callbackmethod!=null&&!callbackmethod.trim().equals(""))
            {
                typePromptBean.setCallbackmethod(callbackmethod.trim());
            }
            List<XmlElementBean> lstPromptcols=eleTypeprompt.getLstChildElementsByName("promptcol");
            loadPromptcolsConfig(ownerbean,lstPromptcols);
            XmlElementBean eleDataSources=eleTypeprompt.getChildElementByName("datasource");
            loadPromptDataSourcesConfig(ownerbean,eleDataSources);
            String typepromptjs=null;
            if(Config.encode.toLowerCase().trim().equals("utf-8"))
            {
                typepromptjs="/webresources/script/wabacus_typeprompt.js";
            }else
            {
                String encode=Config.encode;
                if(encode.trim().equalsIgnoreCase("gb2312"))
                {
                    encode="gbk";
                }
                typepromptjs="/webresources/script/"+encode.toLowerCase()+"/wabacus_typeprompt.js";
            }
            typepromptjs=Config.webroot+"/"+typepromptjs;
            typepromptjs=Tools.replaceAll(typepromptjs,"//","/");
            ownerbean.getReportBean().addMyJavascript(typepromptjs);
        }
    }

    private void loadPromptDataSourcesConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleDataSources)
    {
        if(eleDataSources==null)
        {
            throw new WabacusConfigLoadingException("没有为报表"+ownerbean.getReportBean().getPath()+"的<typeprompt/>配置子标签<datasource/>");
        }
        String type=eleDataSources.attributeValue("type");
        Object type_obj;
        if(type==null||type.trim().equals(""))
        {
            type_obj=new SQLPromptDataSource();
        }else
        {//这里配置的是相应类的全限定类名
            try
            {
                type_obj=Class.forName(type.trim()).newInstance();
            }catch(InstantiationException e)
            {
                throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"为<typeprompt/>的子标签<datasource/>配置的type"+type
                        +"对应的类无法实例化",e);
            }catch(IllegalAccessException e)
            {
                throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"为<typeprompt/>的子标签<datasource/>配置的type"+type
                        +"对应的类不能访问",e);
            }catch(ClassNotFoundException e)
            {
                throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"为<typeprompt/>的子标签<datasource/>配置的type"+type
                        +"对应的类没有找到",e);
            }
        }
        if(!(type_obj instanceof AbsTypePromptDataSource))
        {
            throw new WabacusConfigLoadingException("加载报表"+ownerbean.getReportBean().getPath()+"的<typeprompt/>的<datasource/>失败，其配置的数据源类："
                    +type_obj.getClass().getName()+"没有继承父类："+AbsTypePromptDataSource.class.getName());
        }
        ((AbsTypePromptDataSource)type_obj).setPromptConfigBean(typePromptBean);
        ((AbsTypePromptDataSource)type_obj).loadExternalConfig(ownerbean.getReportBean(),eleDataSources);
        typePromptBean.setDatasource((AbsTypePromptDataSource)type_obj);
    }

    private void loadPromptcolsConfig(IInputBoxOwnerBean ownerbean,List<XmlElementBean> lstPromptcols)
    {
        if(lstPromptcols==null&&lstPromptcols.size()==0)
        {
            throw new WabacusConfigLoadingException("没有为报表"+ownerbean.getReportBean().getPath()+"<typeprompt/>的配置子标签<promptcol/>");
        }
        List<TypePromptColBean> lstPColBeans=new ArrayList<TypePromptColBean>();
        boolean isShowTitle=false;
        boolean isHasMatchCol=false;
        TypePromptColBean tpColbeanTmp;
        for(XmlElementBean elePromptColBeanTmp:lstPromptcols)
        {
            if(elePromptColBeanTmp==null) continue;
            String label=elePromptColBeanTmp.attributeValue("label");
            String value=elePromptColBeanTmp.attributeValue("value");
            String title=elePromptColBeanTmp.attributeValue("title");
            String matchmode=elePromptColBeanTmp.attributeValue("matchmode");
            if(label==null||label.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"<typeprompt/>的子标签<promptcol/>的label属性不能为空");
            }
            tpColbeanTmp=new TypePromptColBean();
            tpColbeanTmp.setLabel(label.trim());
            if(title!=null&&!title.trim().equals(""))
            {
                tpColbeanTmp.setTitle(title.trim());
                isShowTitle=true;
            }
            if(matchmode!=null&&!matchmode.trim().equals(""))
            {
                try
                {
                    tpColbeanTmp.setMatchmode(Integer.parseInt(matchmode.trim()));
                }catch(NumberFormatException e)
                {
                    throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()
                            +"<typeprompt/>的子标签<promptcol/>的matchmode属性只能配置为0,1,2三个数字");
                }
                if(tpColbeanTmp.getMatchmode()<0||tpColbeanTmp.getMatchmode()>2)
                {
                    throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()
                            +"<typeprompt/>的子标签<promptcol/>的matchmode属性只能配置为0,1,2三个数字");
                }
                if(tpColbeanTmp.getMatchmode()>0)
                {
                    isHasMatchCol=true;//存在参与匹配的列
                    if(value==null||value.trim().equals("")) value=label;
                    tpColbeanTmp.setValue(value.trim());
                }
            }
            lstPColBeans.add(tpColbeanTmp);
        }
        if(!isHasMatchCol)
        {
            throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()
                    +"<typeprompt/>的子标签<promptcol/>的matchmode属性不能均配置为0，必须指定一个或以上用于匹配的列");
        }
        typePromptBean.setShowtitle(isShowTitle);
        typePromptBean.setLstPColBeans(lstPColBeans);
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "onfocus='this.select();' onkeypress='return onKeyEvent(event);' class='cls-inputbox2'";
    }
    
    public void doPostLoad(IInputBoxOwnerBean ownerbean)
    {
        super.doPostLoad(ownerbean);
        if(this.typePromptBean!=null)
        {
            this.typePromptBean.getDatasource().doPostLoad(ownerbean.getReportBean());        
        }
    }

    protected void processStylePropertyAfterMerged(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        super.processStylePropertyAfterMerged(reportTypeObj,ownerbean);
        if(this.typePromptBean!=null)
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onblur=\"hideTypePromptOnBlur(this);\"",1);
            this.styleproperty=Tools.removePropertyValueByName("onkeypress",this.styleproperty);
        }
    }

    public Object clone(IInputBoxOwnerBean owner)
    {
        TextBox tbNew=(TextBox)super.clone(owner);
        if(typePromptBean!=null)
        {
            tbNew.setTypePromptBean((TypePromptBean)typePromptBean.clone());
            if(owner!=null&&owner.getReportBean()!=null)
            {
                owner.getReportBean().addTextBoxWithingTypePrompt(tbNew);
            }
        }
        return tbNew;
    }
}
