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
package com.wabacus.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.component.other.JavascriptFileBean;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.config.database.datasource.DriverManagerDataSource;
import com.wabacus.config.resource.AbsResource;
import com.wabacus.config.resource.Resources;
import com.wabacus.config.resource.StringRes;
import com.wabacus.config.xml.XmlAssistant;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.commoninterface.IListReportRoworderPersistence;
import com.wabacus.system.commoninterface.IPagePersonalizePersistence;
import com.wabacus.system.commoninterface.IReportPersonalizePersistence;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IReportType;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.DesEncryptTools;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;
import com.wabacus.util.WabacusClassLoader;

public class ConfigLoadManager
{
    private static Log log=LogFactory.getLog(ConfigLoadManager.class);

    public static WabacusClassLoader currentDynClassLoader;

    public static Map<String,PageBean> mAllPagesConfig;

    public static Map<String,List<String>> mAllPageChildIds;//加载配置文件时，存放每个<page/>中的所有子组件ID，以便校验每个<page/>中是否存在重复ID的组件

    public static List<ReportBean> lstExtendReports;

    public static int loadAllReportSystemConfigs()
    {
        try
        {
            Config.getInstance().initConfigLoad();
            loadSystemItemConfig();//加载wabacus.cfg.default.xml/wabacus.cfg.xml中<system/>所有<item/>的配置
            DesEncryptTools.initEncryptKey();
            encryptDatasourcePassword();
            loadBuildInDefaultSystemConfig();
            Document doc=XmlAssistant.getInstance().loadXmlDocument("wabacus.cfg.xml");
            Element root=doc.getRootElement();
            if(root==null) return 0;
            Element eleDatasources=XmlAssistant.getInstance().getSingleElementByName(root,"datasources");
            if(eleDatasources==null)
            {
                throw new WabacusConfigLoadingException("没有配置报表数据源<datasources/>");
            }
            loadDataSources(eleDatasources);

            loadBuildInDefaultResources();

            loadGlobalPageInterceptors(root.element("global-interceptors"));
            
            Element eleI18nResources=root.element("i18n-resources");
            if(eleI18nResources!=null)
            {
                loadI18nResources(eleI18nResources);
            }

            loadGlobeResources(root);
            Config.getInstance().getResources().replacePlaceHolderInStringRes();

            Config.getInstance().addGlobalCss(loadCssfiles(root.element("global-cssfiles")));
            List<JavascriptFileBean> lstJsFiles=new UniqueArrayList<JavascriptFileBean>();
            lstJsFiles.addAll(loadJsfiles(root.element("global-jsfiles")));
            lstJsFiles.addAll(ConfigLoadAssistant.getInstance().getLstPopupComponentJs());
            lstJsFiles.add(new JavascriptFileBean(Tools.replaceAll(Config.webroot+"/wabacus-generatejs/generate_system.js","//","/"),0));
            Config.getInstance().setLstGlobalJavascriptFiles(lstJsFiles);
            Config.getInstance().addGlobalCss(ConfigLoadAssistant.getInstance().getLstPopupComponentCss());
            Config.getInstance().setMLocalCss(new HashMap<String,List<String>>());
            Config.getInstance().setMLocalJavascriptFiles(new HashMap<String,List<JavascriptFileBean>>());
            
            loadInputBoxTypesConfig(root);
            loadDataTypesConfig(root);
            loadContainerTypesConfig(root);
            loadReportTypesConfig(root);
            createSystemJS();
            mAllPagesConfig=new HashMap<String,PageBean>();
            mAllPageChildIds=new HashMap<String,List<String>>();
            lstExtendReports=new ArrayList<ReportBean>();
            loadReportConfigFiles(root);
            if(lstExtendReports!=null&&lstExtendReports.size()>0)
            {
                while(lstExtendReports.size()>0)
                {
                    List<ReportBean> lstTemp=new ArrayList<ReportBean>();
                    ReportBean rbeanTemp;
                    ReportBean rbeanParent;
                    for(int i=0;i<lstExtendReports.size();i++)
                    {
                        rbeanTemp=lstExtendReports.get(i);
                        if(rbeanTemp==null) continue;
                        XmlElementBean eleReportBean=rbeanTemp.getEleReportBean();
                        if(eleReportBean==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbeanTemp.getPath()+"失败");
                        }
                        String reportextends=eleReportBean.attributeValue("extends");
                        rbeanParent=ComponentConfigLoadManager.getReportBeanByPath(reportextends);
                        if(rbeanParent==null)
                        {
                            throw new WabacusConfigLoadingException("报表"+rbeanTemp.getPath()+"配置"+"继承的报表"+reportextends+"不存在");
                        }
                        if(rbeanParent.getEleReportBean()!=null)
                        {
                            lstTemp.add(rbeanTemp);
                            continue;
                        }

                        ReportBean rbTemp=(ReportBean)rbeanParent.clone(rbeanTemp.getId(),rbeanTemp.getParentContainer());


                        rbTemp.setEleReportBean(null);
                        rbTemp.getParentContainer().getMChildren().put(rbTemp.getId(),rbTemp);
                        ComponentConfigLoadManager.loadReportInfo(rbTemp,eleReportBean,rbeanParent);
                    }
                    if(lstTemp.size()==lstExtendReports.size())
                    {
                        throw new WabacusConfigLoadingException("报表"+lstTemp+"无法加载，可能存在相互继承关系");
                    }
                    lstExtendReports=lstTemp;
                }
            }
            Config.getInstance().setMReportStructureInfo(mAllPagesConfig);
            log.info("成功加载完所有报表配置文件，开始执行所有页面加载后置动作...");
            if(mAllPagesConfig!=null&&mAllPagesConfig.size()>0)
            {
                for(Entry<String,PageBean> entryTmp:mAllPagesConfig.entrySet())
                {
                    if(entryTmp.getValue()==null) continue;
                    try
                    {
                        entryTmp.getValue().doPostLoad();
                    }catch(Exception e)
                    {
                        log.error("执行页面"+entryTmp.getValue().getPath()+"加载后置动作失败",e);
                        return -1;
                    }
                }
                
                for(Entry<String,PageBean> entryTmp:mAllPagesConfig.entrySet())
                {
                    if(entryTmp.getValue()==null) continue;
                    try
                    {
                        entryTmp.getValue().doPostLoadFinally();
                    }catch(Exception e)
                    {
                        log.error("执行页面"+entryTmp.getValue().getPath()+"加载后置动作失败",e);
                        return -1;
                    }
                }
            }
            loadAllSkinConfigProperties();
            mAllPagesConfig=null;
            mAllPageChildIds=null;
            lstExtendReports=null;
            log.info("wabacus应用启动完成!");
            return 1;
        }catch(Exception e)
        {
            log.error("加载报表配置文件失败",e);
            return -1;
        }
    }

    private static void loadReportConfigFiles(Element root)
    {
        Element eleReport=root.element("report-files");
        if(eleReport==null)
        {
            throw new WabacusConfigLoadingException("没有配置报表配置文件");
        }
        List lstReport=eleReport.elements("report-file");
        List<String> lstReportConfigFiles=getListConfigFilePaths(lstReport);//获取到所有配置文件路径
        if(lstReportConfigFiles==null||lstReportConfigFiles.size()<=0)
        {
            throw new WabacusConfigLoadingException("没有配置报表配置文件");
        }
        Map<String,Map> mLocalResourcesTemp=new HashMap<String,Map>();
        boolean isClasspathTmp;
        BufferedInputStream bisTmp;
        for(String fileTmp:lstReportConfigFiles)
        {
            if(fileTmp==null||fileTmp.trim().equals("")) continue;
            isClasspathTmp=Tools.isDefineKey("classpath",fileTmp);
            if(isClasspathTmp) fileTmp=Tools.getRealKeyByDefine("classpath",fileTmp);
            String jsFileName=convertFileNameByPath(fileTmp)+".js";
            String jsFilePath=Tools.standardFilePath(Config.webroot_abspath+"\\wabacus-generatejs\\"+jsFileName);
            String jsFileUrl=Config.webroot+"/wabacus-generatejs/"+jsFileName;
            jsFileUrl=Tools.replaceAll(jsFileUrl,"//","/");
            if(!fileTmp.toLowerCase().endsWith(".xml"))
            {
                log.warn("没有加载报表配置文件"+fileTmp+"，因为不是xml格式");
                continue;
            }
            log.info("正在加载配置文件："+fileTmp+"...");
            bisTmp=null;
            try
            {
                bisTmp=getConfigFileInputStream(fileTmp,isClasspathTmp);
                ComponentConfigLoadManager.loadApplicationsConfigFiles(bisTmp,jsFileUrl,jsFilePath,mLocalResourcesTemp);
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("加载报表配置文件"+fileTmp+"失败",e);
            }finally
            {
                try
                {
                    if(bisTmp!=null) bisTmp.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String convertFileNameByPath(String path)
    {
        if(path==null) return path;
        String path2=path.trim();
        path2=Tools.replaceAll(path2,"\\","");
        path2=Tools.replaceAll(path2,".","");
        path2=Tools.replaceAll(path2,"/","");
        path2=Tools.replaceAll(path2,":","");
        path2=Tools.replaceAll(path2,"-","");
        return path2;
    }
    
    private static void encryptDatasourcePassword()
    {
        if(DesEncryptTools.KEY_OBJ==null)
        {
            return;
        }
        Document doc=XmlAssistant.getInstance().loadXmlDocument("wabacus.cfg.xml");
        String xpath=XmlAssistant.getInstance().addNamespaceToXpath("//datasources/datasource/property[@name='password']");
        List lstPasswords=doc.selectNodes(xpath);
        if(lstPasswords==null||lstPasswords.size()==0) return;
        Element elePasswordTmp;
        String passwordTmp;
        boolean hasEncrypted=false;
        for(int i=0;i<lstPasswords.size();i++)
        {
            elePasswordTmp=(Element)lstPasswords.get(i);
            passwordTmp=elePasswordTmp.getText();
            passwordTmp=passwordTmp==null?"":passwordTmp.trim();
            if(passwordTmp.equals("")) continue;
            if(passwordTmp.startsWith("{3DES}"))
            {
                if(DesEncryptTools.IS_NEWKEY)
                {
                    throw new WabacusConfigLoadingException("密钥文件已经改变，但wabacus.cfg.xml中已有用旧密钥加密好的密码，它们将无法解密，请将它们先置成明文再换密钥文件");
                }
            }else
            {
                hasEncrypted=true;
                passwordTmp=DesEncryptTools.encrypt(passwordTmp);
                elePasswordTmp.setText("{3DES}"+passwordTmp);
            }
        }
        if(hasEncrypted)
        {
            try
            {
                XmlAssistant.getInstance().saveDocumentToXmlFile("wabacus.cfg.xml",doc);
            }catch(IOException e)
            {
                log.warn("wabacus.cfg.xml中的数据源密码加密失败，将存放明文的密码",e);
            }
        }
    }

    private static void loadSystemItemConfig()
    {
        Map<String,String> mFinalSystemConfig=new HashMap<String,String>();
        Map<String,String> mBuiltInSystemConfig=new HashMap<String,String>();
        BufferedInputStream bis=null;
        try
        {
            InputStream is=ConfigLoadManager.currentDynClassLoader.getResourceAsStream("defaultconfig/wabacus.cfg.default.xml");
            if(is!=null)
            {
                bis=new BufferedInputStream(is);
                Document docTemp=XmlAssistant.getInstance().loadXmlDocument(is);
                Element eleRoot=docTemp.getRootElement();
                Element eleSystem=XmlAssistant.getInstance().getSingleElementByName(eleRoot,"system");
                if(eleSystem!=null)
                {
                    List lstItem=eleSystem.elements("item");
                    if(lstItem!=null&&lstItem.size()>0)
                    {
                        Element eleItemTmp;
                        for(int i=0;i<lstItem.size();i++)
                        {
                            eleItemTmp=(org.dom4j.Element)lstItem.get(i);
                            if(eleItemTmp!=null)
                            {
                                String name=eleItemTmp.attributeValue("name");
                                String value=eleItemTmp.attributeValue("value");
                                mBuiltInSystemConfig.put(name,value);
                                mFinalSystemConfig.put(name,value);
                            }
                        }
                    }
                }
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载系统内置配置文件/defaultconfig/wabacus.cfg.default.xml失败",e);
        }finally
        {
            if(bis!=null)
            {
                try
                {
                    bis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        Map<String,String> mSystemConfig=new HashMap<String,String>();
        Document doc=XmlAssistant.getInstance().loadXmlDocument("wabacus.cfg.xml");
        Element root=doc.getRootElement();
        Element eleSystem=XmlAssistant.getInstance().getSingleElementByName(root,"system");
        if(eleSystem!=null)
        {
            List lstItem=eleSystem.elements("item");
            if(lstItem!=null&&lstItem.size()>0)
            {
                for(int i=0;i<lstItem.size();i++)
                {
                    Element eleItem=(Element)lstItem.get(i);
                    if(eleItem!=null)
                    {
                        String name=eleItem.attributeValue("name");
                        String value=eleItem.attributeValue("value");
                        mSystemConfig.put(name,value);
                        mFinalSystemConfig.put(name,value);
                    }
                }
            }
        }
        Config.getInstance().setMSystemConfig(mFinalSystemConfig);
        initSystemConfig(mBuiltInSystemConfig,mSystemConfig);
    }

    private static void createSystemJS()
    {
        if(!Config.should_createjs) return;
        File fRoot=new File(Tools.standardFilePath(Config.webroot_abspath+"\\wabacus-generatejs"));
        if(fRoot.exists()&&fRoot.isDirectory())
        {
            File[] filesArr=fRoot.listFiles();
            if(filesArr!=null)
            {
                for(int i=0;i<filesArr.length;i++)
                {
                    filesArr[i].delete();
                }
            }
        }else
        {
            fRoot.mkdir();
        }
        String jsFilePath=Tools.standardFilePath(Config.webroot_abspath+"\\wabacus-generatejs\\generate_system.js");
        String rowselectbgcolor=Config.getInstance().getSystemConfigValue("selectedrow-bgcolor","");
        StringBuffer scriptBuf=new StringBuffer();
        if(!rowselectbgcolor.trim().equals(""))
        {
            scriptBuf.append("var WX_selectedRowBgcolor='"+rowselectbgcolor+"';");
        }else
        {
            scriptBuf.append("var WX_selectedRowBgcolor='#0000FF';");
        }
        scriptBuf.append("var WXConfig=new Object();");
        scriptBuf.append("WXConfig.showreport_url='"+Config.showreport_url+"';");
        scriptBuf.append("WXConfig.showreport_onpage_url='"+Config.showreport_onpage_url+"';");
        scriptBuf.append("WXConfig.webroot='").append(Config.webroot).append("';");
        scriptBuf.append("WXConfig.prompt_dialog_type='").append(Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog")).append(
                "';");
        String loadErrorMessage=Config.getInstance().getResourceString(null,null,"${load.error.mess}",false);
        scriptBuf.append("WXConfig.load_error_message=").append(
                loadErrorMessage==null||loadErrorMessage.trim().equals("")?"null":"'"+loadErrorMessage+"'").append(";");
        JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(jsFilePath,scriptBuf.toString());
        
        if(Config.getInstance().getMInputBoxTypes()!=null&&Config.getInstance().getMInputBoxTypes().size()>0)
        {
            scriptBuf=new StringBuffer();
            scriptBuf.append("function getInputBoxValue(id,type){");
            scriptBuf.append("if(id==null||id==''||type==null||type=='') return null;");
            Iterator<String> itTypes=Config.getInstance().getMInputBoxTypes().keySet().iterator();
            String name;
            AbsInputBox inputbox;
            while(itTypes.hasNext())
            {
                name=itTypes.next();
                if(name.equals(Consts.DEFAULT_KEY)) continue;
                inputbox=Config.getInstance().getInputBoxTypeByName(name);
                if(inputbox==null) continue;
                scriptBuf.append("  if(type=='"+name+"')  {    ");
                scriptBuf.append(inputbox.createGetValueByIdJs());
                scriptBuf.append("  }");
            }
            scriptBuf.append("return null;}");
            JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(jsFilePath,scriptBuf.toString());

            scriptBuf=new StringBuffer();
            scriptBuf.append("function getInputBoxLabel(id,type){");
            scriptBuf.append("if(id==null||id==''||type==null||type=='') return null;");
            itTypes=Config.getInstance().getMInputBoxTypes().keySet().iterator();
            while(itTypes.hasNext())
            {
                name=itTypes.next();
                if(name.equals(Consts.DEFAULT_KEY)) continue;
                inputbox=Config.getInstance().getInputBoxTypeByName(name);
                if(inputbox==null) continue;
                scriptBuf.append("  if(type=='"+name+"')  {    ");
                scriptBuf.append(inputbox.createGetLabelByIdJs());
                scriptBuf.append("  }");
            }
            scriptBuf.append("return null;}");
            JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(jsFilePath,scriptBuf.toString());
            
            scriptBuf=new StringBuffer();
            scriptBuf.append("function setInputBoxValue(id,type,newvalue){");
            scriptBuf.append("if(id==null||id==''||type==null||type=='') return;if(newvalue==null) newvalue='';");
            itTypes=Config.getInstance().getMInputBoxTypes().keySet().iterator();
            while(itTypes.hasNext())
            {
                name=itTypes.next();
                if(name.equals(Consts.DEFAULT_KEY)) continue;
                inputbox=Config.getInstance().getInputBoxTypeByName(name);
                if(inputbox==null) continue;
                scriptBuf.append("  if(type=='"+name+"'){  ");
                scriptBuf.append(inputbox.createSetInputBoxValueByIdJs());
                scriptBuf.append("  }");
            }
            scriptBuf.append("}");
            JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(jsFilePath,scriptBuf.toString());
            
            scriptBuf=new StringBuffer();
            scriptBuf.append("function fillInputBoxToTd(pageid,reportguid,reportfamily,tdObj,type,name,fillmode,displaymode){");
            scriptBuf.append("   var boxstr='';");
            scriptBuf.append("   var textalign=tdObj.style.textAlign||tdObj.getAttribute('align'); if(textalign==null) textalign='left';");
            scriptBuf.append("   var wid=tdObj.clientWidth-2;");
            scriptBuf.append("   var updateDestTdObj=getUpdateColDestObj(tdObj,reportguid,reportfamily,tdObj);");
            scriptBuf.append("   var boxValue=updateDestTdObj.getAttribute('value');");
            scriptBuf.append("   if(boxValue==null){");
            scriptBuf.append("      boxValue='';");
            //$ByQXO textarea时直接存放json数据时的转义问题
            scriptBuf.append("   }else  if (type != 'textareabox') {");
            //ByQXO$
            scriptBuf.append("      boxValue=boxValue.replace(/</g,'&lt;');boxValue=boxValue.replace(/>/g,'&gt;');boxValue=boxValue.replace(/\\\'/g,'&#039;');boxValue=boxValue.replace(/\\\"/g,'&quot;');");
            scriptBuf.append("   }var boxId=name;if(boxId.lastIndexOf('__')>0) boxId=boxId.substring(0,boxId.lastIndexOf('__'));");
            scriptBuf.append("   var inputboxSpanObj=document.getElementById('span_'+boxId+'_span');");
            scriptBuf.append("   var styleproperty=null;var parentid=null;var childid=null;var onfocusmethod=null;var onblurmethod=null;");
            scriptBuf.append("   var jsvalidateOnblurMethod=null;");
            scriptBuf.append("   var style_propertyvalue=null;");
            scriptBuf.append("   if(inputboxSpanObj!=null){");
            scriptBuf.append("       onfocusmethod=inputboxSpanObj.getAttribute('onfocus_propertyvalue');onblurmethod=inputboxSpanObj.getAttribute('onblur_propertyvalue');");
            scriptBuf.append("       styleproperty=inputboxSpanObj.getAttribute('styleproperty');style_propertyvalue=inputboxSpanObj.getAttribute('style_propertyvalue');");
            scriptBuf.append("       jsvalidateOnblurMethod=inputboxSpanObj.getAttribute('jsvalidate_onblur_method');");
            scriptBuf.append("       parentid=inputboxSpanObj.getAttribute('parentid');");
            scriptBuf.append("       childid=inputboxSpanObj.getAttribute('childid');");
            scriptBuf.append("   }");
            scriptBuf.append("   if(styleproperty==null) styleproperty='';styleproperty=paramdecode(styleproperty);");
            scriptBuf.append("   if(style_propertyvalue==null) style_propertyvalue='';");
            scriptBuf.append("   if(onfocusmethod==null) onfocusmethod='';if(onblurmethod==null) onblurmethod='';");
            scriptBuf.append("   if(jsvalidateOnblurMethod!=null&&jsvalidateOnblurMethod!=''){");
            scriptBuf.append("       if(onblurmethod!=''&&onblurmethod.lastIndexOf(';')!=onblurmethod.length-1) onblurmethod=onblurmethod+';';");//如果已经有onblur事件，且没有以;结尾，则在末尾加上;，以便拼接两个函数
            scriptBuf.append("       onblurmethod=onblurmethod+jsvalidateOnblurMethod;");
            scriptBuf.append("   }");
            scriptBuf.append("   if(onfocusmethod!=''&&onfocusmethod.lastIndexOf(';')!=onfocusmethod.length-1) onfocusmethod+=';';");
            scriptBuf.append("   if(onblurmethod!=''&&onblurmethod.lastIndexOf(';')!=onblurmethod.length-1) onblurmethod+=';';");
            itTypes=Config.getInstance().getMInputBoxTypes().keySet().iterator();
            while(itTypes.hasNext())
            {
                name=itTypes.next();
                if(name.equals(Consts.DEFAULT_KEY)) continue;
                inputbox=Config.getInstance().getInputBoxTypeByName(name);
                if(inputbox==null) continue;
                scriptBuf.append("   if(type=='"+name+"'){");
                scriptBuf.append(        inputbox.filledInContainer("fillInputBoxValueToParentTd(this,'"+name+"','\"+reportguid+\"','\"+reportfamily+\"',\"+fillmode+\",'\"+name+\"');"));
                scriptBuf.append("   }");
            }
            scriptBuf.append("   setColDisplayValueToEditable2Td(tdObj,boxstr);var colInnerWrapEle=getColInnerWrapElement(tdObj);if(colInnerWrapEle==null) colInnerWrapEle=tdObj;");
            scriptBuf.append("   if(colInnerWrapEle.children[0]){");
            scriptBuf.append("       colInnerWrapEle.children[0].focus();");
            scriptBuf.append("          if(fillmode==2){");
            scriptBuf.append("              for(var j=0,len=colInnerWrapEle.children.length;j<len;j++){");
            scriptBuf.append("                  if(colInnerWrapEle.children[j].nodeType!=1) continue;");
            scriptBuf.append("                  colInnerWrapEle.children[j].dataObj=initInputBoxData(colInnerWrapEle.children[j],tdObj);");
            scriptBuf.append("              }");
            scriptBuf.append("          }");
            scriptBuf.append("   }");
            scriptBuf.append("}");
            JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(jsFilePath,scriptBuf.toString());

            scriptBuf=new StringBuffer();
            scriptBuf.append("function fillInputBoxValueToParentTd(boxObj,type,reportguid,reportfamily,fillmode,boxid){");
            scriptBuf.append("   var parentTdObj=null;");
            scriptBuf.append("   if(fillmode==1){parentTdObj=getParentElementObj(boxObj,'TD');}else if(fillmode==2){parentTdObj=boxObj.dataObj.parentTdObj;}");
            scriptBuf.append("   if(parentTdObj==null) {wx_warn('没有取到输入框所属<td/>对象，设置其值失败');return;}");
            /*scriptBuf.append("   var oldValueName=parentTdObj.getAttribute('oldvalue_name');");
            scriptBuf.append("   if(oldValueName!=null&&oldValueName.indexOf(COL_NONDISPLAY_PERMISSION_PREX)==0){");
            
            scriptBuf.append("       if(reportfamily==ReportFamily.EDITABLELIST2||reportfamily==ReportFamily.LISTFORM){");
            scriptBuf.append("           addDataForSaving(reportguid,parentTdObj.parentNode);");
            scriptBuf.append("       }else if(reportfamily==ReportFamily.EDITABLEDETAIL2){");
            scriptBuf.append("           addDataForSaving(reportguid,'true');");
            scriptBuf.append("       }");
            scriptBuf.append("       return;");
            scriptBuf.append("    }");*/
            scriptBuf.append("   var label=' ';var value=' ';");
            itTypes=Config.getInstance().getMInputBoxTypes().keySet().iterator();
            while(itTypes.hasNext())
            {
                name=itTypes.next();
                if(name.equals(Consts.DEFAULT_KEY)) continue;
                inputbox=Config.getInstance().getInputBoxTypeByName(name);
                if(inputbox==null) continue;
                scriptBuf.append("  if(type=='"+name+"'){  ");
                scriptBuf.append(inputbox.createGetValueByInputBoxObjJs());
                scriptBuf.append("  }");
            }
            scriptBuf.append("   if(label==null){label='';}");
            //scriptBuf.append("else{ label=label.replace(/</g,'&lt;');label=label.replace(/>/g,'&gt;');label=label.replace(/\\\'/g,'&#039;');label=label.replace(/\\\"/g,'&quot;');}");
            scriptBuf.append("  if(fillmode==2)");//如果是点击时填充，则赋完值到所属<td/>的value属性后，将它的label也填充到<td/>的innerHTML中。
            scriptBuf.append("  {");
            scriptBuf.append("     var realBoxId=boxid;");
            scriptBuf.append("     if(realBoxId.lastIndexOf('__')>0) realBoxId=realBoxId.substring(0,realBoxId.lastIndexOf('__'));");
            scriptBuf.append("     var inputboxSpanObj=document.getElementById('span_'+realBoxId+'_span');");
            scriptBuf.append("     if(inputboxSpanObj!=null)");
            scriptBuf.append("     {");
            scriptBuf.append("         var formatemplate=inputboxSpanObj.getAttribute('formatemplate');");
            scriptBuf.append("         if(formatemplate!=null&&formatemplate!='')");
            scriptBuf.append("         {");
            scriptBuf.append("               label=getEditable2ColRealValueByFormatemplate(parentTdObj,reportguid,formatemplate,inputboxSpanObj.getAttribute('formatemplate_dyncols'),label);");
            scriptBuf.append("         }");
            scriptBuf.append("     }");
            scriptBuf.append("     setColDisplayValueToEditable2Td(parentTdObj,label);");
            scriptBuf.append("  }");
            scriptBuf.append("   if(value==null){value='';}");
            //scriptBuf.append("  else{ value=value.replace(/</g,'&lt;');value=value.replace(/>/g,'&gt;');value=value.replace(/\\\'/g,'&#039;');value=value.replace(/\\\"/g,'&quot;');}");
            scriptBuf.append("  var updateDestTdObj=getUpdateColDestObj(parentTdObj,reportguid,reportfamily,parentTdObj);");
            scriptBuf.append("  updateDestTdObj.setAttribute('value',value);");
//            /**
//             * 注意下面的oldvalue变量不能从<td/>的oldvalue属性中取，而必须从存放新值的value属性中取，因为假设第一次编辑新值时，与oldvalue不同，将此记录数据放入待保存队列中，并将新值设置到value属性中，

//             */

//            scriptBuf.append("   if(value!=oldvalue){");//编辑后的值与<td/>中原有的值不同，则要更新它



//            scriptBuf.append("           if(childids!=null&&childids!=''){");//是树形分组节点或普通分组节点对应的列，则更新它所包括的所有数据行的相应分组列的值，以便保存时用上








//            scriptBuf.append("       }");

            scriptBuf.append("}");
            JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(jsFilePath,scriptBuf.toString());
        }
    }
    
    private static void loadI18nResources(Element eleI18nResources)
    {
        String file=eleI18nResources.attributeValue("file");
        if(file==null||file.trim().equals(""))
        {
            return;
            //throw new WabacusConfigLoadingException("在系统配置文件<i18n-resources/>中必须配置file属性，且不能配置为空字符串");
        }
        file=file.trim();
        if(file.toLowerCase().endsWith(".xml"))
        {
            file=file.substring(0,file.length()-4);
        }
        String absFilePath=WabacusAssistant.getInstance().getRealFilePath(Config.configpath,file);
        if(Tools.isDefineKey("classpath",Config.configpath))
        {
            validI18nFile(absFilePath,"/");
            Config.i18n_filename=absFilePath;
            Config.getInstance().getResources().setMI18NResources(new HashMap<String,Map<String,Object>>());
            loadI18nResourcesInClassPath(null);
        }else
        {
            validI18nFile(absFilePath,File.separator);
            int idxtmp=absFilePath.lastIndexOf(File.separator);
            Config.i18n_filename=absFilePath.substring(idxtmp+1).toLowerCase();
            String i18n_filepath=absFilePath.substring(0,idxtmp);
            loadI18nResourcesInAbsPath(i18n_filepath);
        }
    }

    private static void validI18nFile(String file,String seperator)
    {
        if(file.endsWith(seperator))
        {
            throw new WabacusConfigLoadingException("在系统配置文件<i18n-resources/>中配置的file属性必须包括I18N资源文件名，不能只指定路径");
        }
        int idxsep=file.lastIndexOf(seperator);
        int idxdot=file.lastIndexOf(".");
        if(idxsep>=0&&idxdot>idxsep||idxdot>0)
        {
            throw new WabacusConfigLoadingException("在系统配置文件<i18n-resources/>中配置的file指定的国际化资源文件类型："+file.substring(idxdot)+"不合法，只能是.xml格式");
        }
    }

    private static void loadI18nResourcesInAbsPath(String i18n_filepath)
    {
        log.info("正在加载"+i18n_filepath+"路径下的国际化资源文件...");
        File f=new File(i18n_filepath);
        if(!f.exists()||!f.isDirectory())
        {
            throw new WabacusConfigLoadingException("在<i18n-resources/>的path属性中配置的："+i18n_filepath+"不存在或不是目录，无法加载其下的资源文件");
        }
        File[] filesArray=f.listFiles();
        if(filesArray==null||filesArray.length==0)
        {
            log.warn("在<i18n-resources/>的path属性中配置的："+i18n_filepath+"下没有资源文件");
        }
        Map<String,Map<String,Object>> mI18nResourceItems=new HashMap<String,Map<String,Object>>();
        String nameTemp;
        for(int i=0;i<filesArray.length;i++)
        {
            nameTemp=filesArray[i].getName().toLowerCase();
            int idx=nameTemp.lastIndexOf(File.separator);
            if(idx>0)
            {
                nameTemp=nameTemp.substring(idx+1);
            }
            idx=nameTemp.lastIndexOf(".");
            if(idx<=0) continue;
            String typetmp=nameTemp.substring(idx).toLowerCase();
            nameTemp=nameTemp.substring(0,idx);
            if(nameTemp.startsWith(Config.i18n_filename)&&typetmp.equals(".xml"))
            {
                Map<String,Object> mResults=null;
                try
                {
                    Map mTemp=loadXmlResources(XmlAssistant.getInstance().loadXmlDocument(filesArray[i]).getRootElement());
                    if(mTemp==null||mTemp.size()==0)
                    {
                        log.warn("没有在I18N资源文件"+filesArray[i].getName()+"中配置资源项");
                    }else
                    {
                        mResults=mTemp;
                    }
                    if(mResults!=null) mI18nResourceItems.put(nameTemp,mResults);
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("加载i18n资源文件"+filesArray[i].getName()+"失败",e);
                }
            }
        }
        if(mI18nResourceItems==null||mI18nResourceItems.size()==0)
        {
            log.warn("在<i18n-resources/>的path属性中配置的："+i18n_filepath+"下没有符合要求的资源文件");
        }
        Config.getInstance().getResources().setMI18NResources(mI18nResourceItems);
    }

    public static void loadI18nResourcesInClassPath(String localetype)
    {
        String myi18nfile=Config.i18n_filename;
        if(localetype!=null&&!localetype.trim().equals("")&&!localetype.trim().equalsIgnoreCase("en"))
        {
            myi18nfile=myi18nfile+"_"+localetype;
        }
        myi18nfile=myi18nfile+".xml";
        Map<String,Object> mResults=null;
        try
        {
            Map mTemp=loadXmlResources(XmlAssistant.getInstance().loadXmlDocument(
                    ConfigLoadManager.currentDynClassLoader.getResourceAsStream(myi18nfile)).getRootElement());
            if(mTemp==null||mTemp.size()==0)
            {
                log.warn("没有在I18N资源文件"+myi18nfile+"中配置资源项");
            }else
            {
                mResults=mTemp;
            }
        }catch(IOException ioe)
        {
            if(localetype==null||localetype.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("配置的国际化资源文件"+Config.i18n_filename+"不存在",ioe);
            }
            log.warn("没有配置语言类型为"+localetype+"的国际化资源文件");
            
        }catch(DocumentException de)
        {
            if(localetype==null||localetype.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("配置的国际化资源文件"+Config.i18n_filename+"不合法",de);
            }
            log.warn("配置语言类型为"+localetype+"的国际化资源文件不合法");
            
        }catch(Exception e)
        {
            if(localetype==null||localetype.trim().equals("")) localetype="EN";
            throw new WabacusConfigLoadingException("加载"+localetype+"对应的国际化资源文件失败",e);
        }
        if(mResults==null)
        {
            mResults=new HashMap<String,Object>();
        }else
        {
            mResults=Config.getInstance().getResources().replace(mResults);
        }
        if(localetype==null||localetype.trim().equals("")) localetype="en";
        Config.getInstance().getResources().getMI18NResources().put(localetype,mResults);
    }

    public static List<String> loadCssfiles(Element root)
    {
        List<String> lstCssFiles=new UniqueArrayList<String>();
        if(root==null) return lstCssFiles;
        List lstCssFileElements=root.elements("css-file");
        if(lstCssFileElements!=null&&lstCssFileElements.size()>0)
        {
            Element eleCssFile;
            for(int i=0;i<lstCssFileElements.size();i++)
            {
                eleCssFile=(Element)lstCssFileElements.get(i);
                if(eleCssFile==null) continue;
                String cssfile=eleCssFile.getTextTrim();
                if(cssfile==null||cssfile.trim().equals("")) continue;
                
                if(!cssfile.toLowerCase().trim().startsWith("http://"))
                {
                    cssfile=Config.webroot+"/"+cssfile.trim();
                    cssfile=Tools.replaceAll(cssfile,"\\","/");
                    cssfile=Tools.replaceAll(cssfile,"//","/");
                }
                lstCssFiles.add(cssfile);
            }
        }
        return lstCssFiles;
    }

    public static List<JavascriptFileBean> loadJsfiles(Element root)
    {
        List<JavascriptFileBean> lstJsFiles=new ArrayList<JavascriptFileBean>();
        if(root==null) return lstJsFiles;
        List lstJsFileElements=root.elements("js-file");
        if(lstJsFileElements!=null&&lstJsFileElements.size()>0)
        {
            String encodetype=Config.encode.toLowerCase().trim();
            if(encodetype.equalsIgnoreCase("gb2312"))
            {
                encodetype="gbk";
            }else if(encodetype.equals("utf-8"))
            {
                encodetype="";
            }
            Element eleJsFile;
            String loadorderTmp;
            for(int i=0;i<lstJsFileElements.size();i++)
            {
                eleJsFile=(Element)lstJsFileElements.get(i);
                if(eleJsFile==null) continue;
                String jsfile=eleJsFile.getTextTrim();
                if(jsfile==null||jsfile.trim().equals("")) continue;
                jsfile=Tools.replaceAll(jsfile,"%ENCODING%",encodetype);
                if(!jsfile.trim().startsWith(Config.webroot)&&!jsfile.toLowerCase().trim().startsWith("http://"))
                {
                    jsfile=Config.webroot+"/"+jsfile.trim();
                    jsfile=Tools.replaceAll(jsfile,"\\","/");
                    jsfile=Tools.replaceAll(jsfile,"//","/");
                }
                loadorderTmp=eleJsFile.attributeValue("loadorder");
                lstJsFiles.add(new JavascriptFileBean(jsfile.trim(),loadorderTmp==null||loadorderTmp.trim().equals("")?0:Integer
                        .parseInt(loadorderTmp.trim())));
            }
        }
        return lstJsFiles;
    }

    private static void initSystemConfig(Map<String,String> mBuiltInSystemConfig,Map<String,String> mSystemConfig)
    {
        Config.webroot_abspath=Config.getInstance().getSystemConfigValue("webroot-abspath",Config.homeAbsPath);
        Config.webroot_abspath=Tools.standardFilePath(Config.webroot_abspath);

        Config.should_createjs=Config.getInstance().getSystemConfigValue("js-create",true);
        if(Config.should_createjs)
        {
            String createjs_path=Tools.standardFilePath(Config.webroot_abspath+"\\wabacus-generatejs\\");
            File f=new File(createjs_path);
            if(f.exists())
            {
                try
                {
                    Tools.delete(f,".js",false);
                }catch(IOException e)
                {
                    throw new WabacusConfigLoadingException("删除js文件创建路径失败",e);
                }
            }
            log.info("报表javascript生成路径："+createjs_path);
            f.mkdir();
        }else
        {
            log.warn("由于js-create配置为false，不生成报表的javascript");
        }
        Config.show_sql=Config.getInstance().getSystemConfigValue("show-sql",false);
        Config.skin=Config.getInstance().getSystemConfigValue("skin","");
        if(Config.webroot==null||Config.webroot.trim().equals(""))
        {
            Config.webroot=Config.getInstance().getSystemConfigValue("webroot","/");
            Config.webroot=Tools.replaceAll(Config.webroot,"\\","/");
            if(!Config.webroot.startsWith("/")) Config.webroot="/"+Config.webroot;
            if(!Config.webroot.endsWith("/")) Config.webroot=Config.webroot+"/";
        }
        Config.encode=Config.getInstance().getSystemConfigValue("encode","utf-8");
        Config.showreport_url=Config.getInstance().getSystemConfigValue("showreport-url","");
        if(Config.showreport_url.equals(""))
        {
            throw new WabacusConfigLoadingException("必须在wabacus.cfg.xml文件中，配置showreport-url");
        }
        if(!Config.showreport_url.startsWith(Config.webroot))
        {
            if(Config.showreport_url.startsWith("/"))
            {
                Config.showreport_url=Config.showreport_url.substring(1);
            }
            Config.showreport_url=Config.webroot+Config.showreport_url;
        }
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        Config.showreport_onpage_url=Config.showreport_url+token+Consts.DISPLAYTYPE_PARAMNAME+"="+Consts.DISPLAY_ON_PAGE;
        Config.showreport_onword_url=Config.showreport_url+token+Consts.DISPLAYTYPE_PARAMNAME+"="+Consts.DISPLAY_ON_WORD;
        Config.showreport_onrichexcel_url=Config.showreport_url+token+Consts.DISPLAYTYPE_PARAMNAME+"="+Consts.DISPLAY_ON_RICHEXCEL;
        Config.showreport_onplainexcel_url=Config.showreport_url+token+Consts.DISPLAYTYPE_PARAMNAME+"="+Consts.DISPLAY_ON_PLAINEXCEL;
        Config.showreport_onpdf_url=Config.showreport_url+token+Consts.DISPLAYTYPE_PARAMNAME+"="+Consts.DISPLAY_ON_PDF;
        List<Class> lstClasses=new ArrayList<Class>();
        String strValidateClass=mSystemConfig.get("server-validate-class");
        List lstTemp=ConfigLoadAssistant.getInstance().convertStringToClassList(strValidateClass);
        if(lstTemp!=null)
        {
            lstClasses.addAll(lstTemp);
        }
        strValidateClass=mBuiltInSystemConfig.get("server-validate-class");
        lstTemp=ConfigLoadAssistant.getInstance().convertStringToClassList(strValidateClass);
        if(lstTemp!=null) lstClasses.addAll(lstTemp);
        Config.getInstance().setLstServerValidateClasses(lstClasses);

        List<Class> lstFormatClasses=new ArrayList<Class>();
        String strFormatClass=mSystemConfig.get("format-class");
        lstTemp=ConfigLoadAssistant.getInstance().convertStringToClassList(strFormatClass);
        if(lstTemp!=null)
        {
            lstFormatClasses.addAll(lstTemp);
        }
        strFormatClass=mBuiltInSystemConfig.get("format-class");
        lstTemp=ConfigLoadAssistant.getInstance().convertStringToClassList(strFormatClass);
        if(lstTemp!=null) lstFormatClasses.addAll(lstTemp);
        Config.getInstance().setLstFormatClasses(lstFormatClasses);
        String default_roworderclass=Config.getInstance().getSystemConfigValue("default-roworderclass","");
        if(!default_roworderclass.equals(""))
        {
            Object obj=null;
            try
            {
                obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(default_roworderclass).newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml通过default-roworderclass配置的"+default_roworderclass+"类对象实例化失败",e);
            }
            if(!(obj instanceof IListReportRoworderPersistence))
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml通过default-roworderclass配置的类"+default_roworderclass+"没有实现"
                        +IListReportRoworderPersistence.class.getName()+"接口");
            }
            Config.default_roworder_object=(IListReportRoworderPersistence)obj;
        }
        String default_pagepersonalizeclass=Config.getInstance().getSystemConfigValue("default-pagepersonalizeclass","");
        if(!default_pagepersonalizeclass.equals(""))
        {
            Object obj=null;
            try
            {
                obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(default_pagepersonalizeclass).newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml通过default-pagepersonalizeclass配置的"+default_pagepersonalizeclass+"类对象实例化失败",e);
            }
            if(!(obj instanceof IPagePersonalizePersistence))
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml通过default-pagepersonalizeclass配置的类"+default_pagepersonalizeclass+"没有实现"
                        +IPagePersonalizePersistence.class.getName()+"接口");
            }
            Config.default_pagepersonalize_object=(IPagePersonalizePersistence)obj;
        }
        String default_reportpersonalizeclass=Config.getInstance().getSystemConfigValue("default-reportpersonalizeclass","");
        if(!default_reportpersonalizeclass.equals(""))
        {
            Object obj=null;
            try
            {
                obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(default_reportpersonalizeclass).newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml通过default-reportpersonalizeclass配置的"+default_reportpersonalizeclass+"类对象无例化失败",e);
            }
            if(!(obj instanceof IReportPersonalizePersistence))
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml通过default-reportpersonalizeclass配置的类"+default_reportpersonalizeclass+"没有实现"
                        +IReportPersonalizePersistence.class.getName()+"接口");
            }
            Config.default_reportpersonalize_object=(IReportPersonalizePersistence)obj;
        }
    }

    private static void loadBuildInDefaultSystemConfig()
    {
        BufferedInputStream bis=null;
        try
        {
            InputStream is=ConfigLoadManager.currentDynClassLoader.getResourceAsStream("defaultconfig/wabacus.cfg.default.xml");
            if(is!=null)
            {
                bis=new BufferedInputStream(is);
                Document docTemp=XmlAssistant.getInstance().loadXmlDocument(is);
                Element eleRoot=docTemp.getRootElement();
                loadContainerTypesConfig(eleRoot);
                loadReportTypesConfig(eleRoot);
                loadInputBoxTypesConfig(eleRoot);
                loadDataTypesConfig(eleRoot);
                Config.getInstance().addGlobalCss(loadCssfiles(eleRoot.element("global-cssfiles")));
                Config.getInstance().setLstDefaultGlobalJavascriptFiles(loadJsfiles(eleRoot.element("global-jsfiles")));
                loadGlobalPageInterceptors(eleRoot.element("global-interceptors"));
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载系统内置配置文件/defaultconfig/wabacus.cfg.default.xml失败",e);
        }finally
        {
            if(bis!=null)
            {
                try
                {
                    bis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void loadGlobalPageInterceptors(Element eleGlobalInterceptors)
    {
        if(eleGlobalInterceptors==null) return;
        List lstEleGlobalInterceptors=eleGlobalInterceptors.elements("interceptor");
        if(lstEleGlobalInterceptors==null||lstEleGlobalInterceptors.size()==0) return;
        Element eleInterceptorTmp;
        Class clsTmp;
        Object objTmp;
        String classTmp,pageidTmp,matchmodeTmp;
        for(int i=0;i<lstEleGlobalInterceptors.size();i++)
        {
            eleInterceptorTmp=(Element)lstEleGlobalInterceptors.get(i);
            classTmp=eleInterceptorTmp.attributeValue("class");
            pageidTmp=eleInterceptorTmp.attributeValue("pageid");
            matchmodeTmp=eleInterceptorTmp.attributeValue("matchmode");
            if(classTmp==null||classTmp.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载全局拦截器失败，存在没有配置class属性的拦截器");
            }
            clsTmp=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(classTmp);
            try
            {
                objTmp=clsTmp.newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("实例化全局拦截器"+classTmp+"失败",e);
            }
            if(!(objTmp instanceof AbsPageInterceptor))
            {
                throw new WabacusConfigLoadingException("全局拦截器"+classTmp+"没有继承"+AbsPageInterceptor.class.getName()+"类");
            }
            if(pageidTmp!=null&&!pageidTmp.trim().equals(""))
            {
                try
                {
                    Method method=clsTmp.getMethod("setMatchpageids",new Class[] { String.class });
                    method.invoke(objTmp,new Object[] { pageidTmp.trim() });
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("全局拦截器"+classTmp+"的类中没有setMatchpageids(String)方法",e);
                }
            }
            if(matchmodeTmp!=null&&!matchmodeTmp.trim().equals(""))
            {
                try
                {
                    Method method=clsTmp.getMethod("setMatchmode",new Class[] { String.class });
                    method.invoke(objTmp,new Object[] { matchmodeTmp.trim() });
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("全局拦截器"+classTmp+"的类中没有setMatchmode(String)方法",e);
                }
            }
            Config.getInstance().addGlobalInterceptor((AbsPageInterceptor)objTmp);
        }
    }

    private static void loadInputBoxTypesConfig(Element eleRoot)
    {
        Element eleTypes=eleRoot.element("inputbox-types");
        Map<String,AbsInputBox> mTypes=new HashMap<String,AbsInputBox>();
        String defaulttype=null;
        if(eleTypes!=null)
        {
            defaulttype=eleTypes.attributeValue("default");
            List lstEleTypes=eleTypes.elements("inputbox-type");
            if(lstEleTypes!=null&&lstEleTypes.size()>0)
            {
                Element eleType;
                for(int i=0;i<lstEleTypes.size();i++)
                {
                    eleType=(Element)lstEleTypes.get(i);
                    if(eleType==null) continue;
                    String name=eleType.attributeValue("name");
                    String strclass=eleType.attributeValue("class");
                    String defaultstyleproperty=eleType.attributeValue("defaultstyleproperty");
                    if(name==null)
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的输入框类型的name属性不能为空");
                    }
                    name=name.trim();
                    if(mTypes.containsKey(name))
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的输入框类型的name属性"+name+"存在重复");
                    }
                    if(strclass==null||strclass.trim().equals(""))
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的name属性为"+name+"的输入框没有配置class属性");
                    }
                    try
                    {
                        Class c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(strclass);
                        Object o=c.getConstructor(new Class[] { String.class }).newInstance(new Object[] { name });
                        if(!(o instanceof AbsInputBox))
                        {
                            throw new WabacusConfigLoadingException("在系统配置文件中注册的name属性为"+name+"的输入框，其class："+strclass+"没有继承类"
                                    +AbsInputBox.class.getName());
                        }
                        mTypes.put(name,(AbsInputBox)o);
                    }catch(Exception e)
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的name属性为"+name+"的输入框，其class："+strclass+"无法加载或无法实例化",e);
                    }
                    if(defaultstyleproperty!=null)
                    {
                        mTypes.get(name).setDefaultstyleproperty(Tools.formatStringBlank(defaultstyleproperty.trim()));
                    }
                }
            }
        }
        if(Config.getInstance().getMInputBoxTypes()==null)
        {
            Config.getInstance().setMInputBoxTypes(mTypes);
        }else
        {
            Tools.copyMapData(mTypes,Config.getInstance().getMInputBoxTypes(),false);
        }
        if(defaulttype!=null&&!defaulttype.trim().equals(""))
        {
            AbsInputBox defaultTypeObj=Config.getInstance().getInputBoxTypeByName(defaulttype);
            if(defaultTypeObj==null)
            {
                throw new WabacusConfigLoadingException("配置的默认输入框"+defaulttype+"不存在");
            }
            Config.getInstance().setInputBoxType(Consts.DEFAULT_KEY,defaultTypeObj);
        }
    }

    private static void loadDataTypesConfig(Element eleRoot)
    {
        Element eleTypes=eleRoot.element("data-types");
        Map<String,IDataType> mTypes=new HashMap<String,IDataType>();
        String defaulttype=null;
        if(eleTypes!=null)
        {
            defaulttype=eleTypes.attributeValue("default");
            List lstEleTypes=eleTypes.elements("data-type");
            if(lstEleTypes!=null&&lstEleTypes.size()>0)
            {
                Element eleType;
                for(int i=0;i<lstEleTypes.size();i++)
                {
                    eleType=(Element)lstEleTypes.get(i);
                    if(eleType==null) continue;
                    String name=eleType.attributeValue("name");
                    String strclass=eleType.attributeValue("class");
                    if(name==null)
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的数据类型的name属性不能为空");
                    }
                    name=name.trim();
                    if(mTypes.containsKey(name))
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的数据类型的name属性"+name+"存在重复");
                    }
                    if(strclass==null||strclass.trim().equals(""))
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的name属性为"+name+"的数据类型没有配置class属性");
                    }
                    try
                    {
                        Class c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(strclass);
                        Object o=c.newInstance();
                        if(!(o instanceof IDataType))
                        {
                            throw new WabacusConfigLoadingException("在系统配置文件中注册的name属性为"+name+"的数据类型，其class："+strclass+"没有实现"
                                    +IDataType.class.getName()+"接口");
                        }
                        ((IDataType)o).loadTypeConfig(eleType);
                        mTypes.put(name,(IDataType)o);
                    }catch(Exception e)
                    {
                        throw new WabacusConfigLoadingException("在系统配置文件中注册的name属性为"+name+"的数据类型时，其class："+strclass+"无法加载或无法实例化",e);
                    }
                }
            }
        }
        if(Config.getInstance().getMDataTypes()==null)
        {
            Config.getInstance().setMDataTypes(mTypes);
        }else
        {
            Tools.copyMapData(mTypes,Config.getInstance().getMDataTypes(),false);
        }
        if(defaulttype!=null&&!defaulttype.trim().equals(""))
        {
            IDataType defaultTypeObj=Config.getInstance().getDataTypeByName(defaulttype);
            if(defaultTypeObj==null)
            {
                throw new WabacusConfigLoadingException("配置的默认数据类型"+defaulttype+"不存在");
            }
            Config.getInstance().getMDataTypes().put(Consts.DEFAULT_KEY,defaultTypeObj);
        }
    }

    private static void loadBuildInDefaultResources()
    {
        BufferedInputStream bis=null;
        try
        {
            Config.getInstance().setResources(new Resources());
            InputStream is=ConfigLoadManager.currentDynClassLoader.getResourceAsStream("defaultconfig/wabacus.resources.default.xml");
            if(is!=null)
            {
                bis=new BufferedInputStream(is);
                Document docTemp=XmlAssistant.getInstance().loadXmlDocument(bis);
                Map mResults=loadXmlResources(docTemp.getRootElement());
                if(mResults!=null&&mResults.size()>0)
                {
                    Tools.copyMapData(mResults,Config.getInstance().getResources().getMBuiltInDefaultResources(),true);
                }
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载系统内置配置文件/defaultconfig/wabacus.resources.default.xml失败",e);
        }finally
        {
            if(bis!=null)
            {
                try
                {
                    bis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void loadGlobeResources(Element root)
    {
        Element eleResources=root.element("global-resources");
        if(eleResources==null) return;
        List lstFiles=eleResources.elements("resource-file");
        if(lstFiles==null||lstFiles.size()==0) return;
        List<String> lstFilePaths=getListConfigFilePaths(lstFiles);
        if(lstFilePaths==null||lstFilePaths.size()==0) return;
        Map mResultsTmp;
        for(String fileTmp:lstFilePaths)
        {
            mResultsTmp=loadResourceFile(fileTmp);
            if(mResultsTmp==null) continue;
            String key=Tools.copyMapData(mResultsTmp,Config.getInstance().getResources().getMGlobalResources(),true);
            if(key!=null)
            {
                throw new WabacusConfigLoadingException("在resource文件中，name属性为"+key+"的存在重复，不能加载配置文件");
            }
        }
    }

    public static Map loadResourceFile(String resourcefile)
    {
        boolean isClasspath=Tools.isDefineKey("classpath",resourcefile);
        if(isClasspath) resourcefile=Tools.getRealKeyByDefine("classpath",resourcefile);
        if(!resourcefile.toLowerCase().endsWith(".properties")&&!resourcefile.toLowerCase().endsWith(".xml"))
        {
            log.warn("没有加载"+resourcefile+"文件，因为不是xml和properties格式");
            return null;
        }
        log.info("正在加载资源文件："+resourcefile+"...");
        Map mResults=null;
        BufferedInputStream bis=null;
        try
        {
            bis=getConfigFileInputStream(resourcefile,isClasspath);
            if(resourcefile.toLowerCase().endsWith(".properties"))
            {
                Properties props=new Properties();
                props.load(bis);
                if(props!=null) mResults=convertPropertiesToResources(props);

            }else
            {
                mResults=loadXmlResources(XmlAssistant.getInstance().loadXmlDocument(bis).getRootElement());
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载配置文件"+resourcefile+"失败，请确认是否存在此配置文件",e);
        }finally
        {
            try
            {
                if(bis!=null) bis.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return mResults;
    }
    
    private static BufferedInputStream getConfigFileInputStream(String filepath,boolean isClasspath) throws IOException
    {
        BufferedInputStream bisResult=null;
        if(isClasspath)
        {
            if(filepath.startsWith("/")) filepath=filepath.substring(1);
            bisResult=new BufferedInputStream(ConfigLoadManager.currentDynClassLoader.getResourceAsStream(filepath));
        }else
        {
            bisResult=new BufferedInputStream(new FileInputStream(filepath));
        }
        return bisResult;
    }

    public static List<String> getListConfigFilePaths(List lstFileElements)
    {
        if(lstFileElements==null||lstFileElements.size()==0) return null;
        List<String> lstResults=new ArrayList<String>();
        Element eleTmp;
        for(int i=0,len=lstFileElements.size();i<len;i++)
        {
            eleTmp=(Element)lstFileElements.get(i);
            String filepathTmp=eleTmp.getTextTrim();
            if(filepathTmp==null||filepathTmp.trim().equals("")) continue;
            String pattern=eleTmp.attributeValue("pattern");
            boolean isPattern=pattern!=null&&pattern.toLowerCase().trim().equals("true");//是否是正则表达式
            if(isPattern)
            {
                boolean isClasspathType=false;
                if(Tools.isDefineKey("classpath",filepathTmp))
                {
                    filepathTmp=Tools.getRealKeyByDefine("classpath",filepathTmp).trim();
                    while(filepathTmp.startsWith("/"))
                    {
                        filepathTmp=filepathTmp.substring(1).trim();
                    }
                    isClasspathType=true;
                }else if(Tools.isDefineKey("absolute",filepathTmp))
                {
                    filepathTmp=Tools.getRealKeyByDefine("absolute",filepathTmp).trim();
                    filepathTmp=Tools.standardFilePath(filepathTmp);
                }else if(Tools.isDefineKey("relative",filepathTmp))
                {
                    filepathTmp=Tools.getRealKeyByDefine("relative",filepathTmp).trim();
                    filepathTmp=WabacusAssistant.getInstance().getRealFilePath(Config.webroot_abspath,filepathTmp);
                }else
                {
                    filepathTmp=WabacusAssistant.getInstance().getRealFilePath(Config.configpath,filepathTmp);
                    if(Tools.isDefineKey("classpath",Config.configpath)) isClasspathType=true;
                }
                String recursive=eleTmp.attributeValue("recursive");
                boolean isRecursive=recursive!=null&&recursive.toLowerCase().trim().equals("true");
                FilePathAssistant.getInstance().getLstFilesByPath(lstResults,filepathTmp,isClasspathType,isRecursive);
            }else
            {
                if(Tools.isDefineKey("classpath",filepathTmp))
                {
                    filepathTmp=Tools.getRealKeyByDefine("classpath",filepathTmp).trim();
                    while(filepathTmp.startsWith("/"))
                    {
                        filepathTmp=filepathTmp.substring(1).trim();
                    }
                    filepathTmp="classpath{"+filepathTmp+"}";
                }else if(Tools.isDefineKey("absolute",filepathTmp))
                {
                    filepathTmp=Tools.getRealKeyByDefine("absolute",filepathTmp).trim();
                    filepathTmp=Tools.standardFilePath(filepathTmp);
                }else if(Tools.isDefineKey("relative",filepathTmp))
                {
                    filepathTmp=Tools.getRealKeyByDefine("relative",filepathTmp).trim();
                    filepathTmp=WabacusAssistant.getInstance().getRealFilePath(Config.webroot_abspath,filepathTmp);
                }else
                {
                    filepathTmp=WabacusAssistant.getInstance().getRealFilePath(Config.configpath,filepathTmp);
                    if(Tools.isDefineKey("classpath",Config.configpath))
                    {
                        filepathTmp="classpath{"+filepathTmp+"}";
                    }
                }
                if(!lstResults.contains(filepathTmp)) lstResults.add(filepathTmp);
            }
        }
        return lstResults;
    }
    
    private static void loadDataSources(Element eleDataSources)
    {
        List lstEleDatasource=eleDataSources.elements("datasource");
        if(lstEleDatasource==null||lstEleDatasource.size()==0)
        {
            throw new WabacusConfigLoadingException("没有在wabacus.cfg.xml文件中配置数据源");
        }
        Map<String,AbsDataSource> mDataSources=new HashMap<String,AbsDataSource>();
        Element eleDataSource=null;
        String name;
        for(int i=0;i<lstEleDatasource.size();i++)
        {
            eleDataSource=(Element)lstEleDatasource.get(i);
            name=eleDataSource.attributeValue("name");
            if(name==null||name.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("必须配置数据源的name属性");
            }
            name=name.trim();
            if(mDataSources.containsKey(name))
            {
                throw new WabacusConfigLoadingException("配置的数据源："+name+"存在重复");
            }
            String provider=eleDataSource.attributeValue("type");
            if(provider==null||provider.trim().equals(""))
            {
                provider=DriverManagerDataSource.class.getName();
            }
            Object providerObj=null;
            try
            {
                Class c_provider=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(provider);
                providerObj=c_provider.newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("数据源类："+provider+"无法实例化",e);
            }
            if(!(providerObj instanceof AbsDataSource))
            {
                throw new WabacusConfigLoadingException("配置的数据源类："+provider+"没有继承超类"+AbsDataSource.class.getName());
            }
            ((AbsDataSource)providerObj).setName(name);
            ((AbsDataSource)providerObj).loadConfig(eleDataSource);

            mDataSources.put(name,((AbsDataSource)providerObj));
        }
        String defaultname=eleDataSources.attributeValue("default");
        if(defaultname!=null&&!defaultname.trim().equals(""))
        {
            String defaultname2=defaultname.trim();
            if(Tools.isDefineKey("i18n",defaultname2))
            {
                defaultname2=Tools.getRealKeyByDefine("i18n",defaultname2);
            }
            if(!mDataSources.containsKey(defaultname2))
            {
                throw new WabacusConfigLoadingException("配置的默认数据源："+eleDataSources.attributeValue("default")+"不存在");
            }
            mDataSources.put(Consts.DEFAULT_KEY,mDataSources.get(defaultname2));
            Config.getInstance().setDefault_datasourcename(defaultname.trim());
        }else
        {
            Config.getInstance().setDefault_datasourcename(null);
        }
        Config.getInstance().setMDataSources(mDataSources);
    }

    public static Map loadXmlResources(Element rootTemp) throws Exception
    {
        Map mResult=new HashMap();
        if(rootTemp!=null)
        {
            List lstResources=XmlAssistant.getInstance().getElementsByName(rootTemp,"resource");
            if(lstResources!=null&&lstResources.size()>0)
            {
                for(int k=0;k<lstResources.size();k++)
                {
                    Element eleResource=(Element)lstResources.get(k);
                    if(eleResource!=null)
                    {
                        String key=eleResource.attributeValue("key");
                        String type=eleResource.attributeValue("type");
                        if(key==null||key.trim().equals(""))
                        {
                            throw new WabacusConfigLoadingException("在资源文件中，为各<resource/>配置的key属性不能为空");
                        }
                        key=key.trim();
                        if(mResult.containsKey(key))
                        {
                            throw new WabacusConfigLoadingException("在resource文件中，key属性为"+key+"的存在重复，不能加载配置文件");
                        }
                        if(type==null||type.trim().equals(""))
                        {
                            type=StringRes.class.getName();
                        }
                        type=type.trim();

                        Object o=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(type).newInstance();
                        if(!(o instanceof AbsResource))
                        {
                            throw new WabacusConfigLoadingException("在resource文件中，为"+key+"资源项配置的type，没有继承AbsResource类");
                        }
                        mResult.put(key,((AbsResource)o).getValue(eleResource));
                    }
                }
            }
        }
        return mResult;
    }

    private static void loadContainerTypesConfig(Element root)
    {
        Element eleContainertypes=XmlAssistant.getInstance().getSingleElementByName(root,"container-types");//root.element("pagetypes");
        if(eleContainertypes==null)  return;
        List lstContainertypes=eleContainertypes.elements("container-type");
        if(lstContainertypes==null||lstContainertypes.size()<=0) return;
        Map<String,AbsContainerType> mContainertypes=new HashMap<String,AbsContainerType>();
        for(int i=0;i<lstContainertypes.size();i++)
        {
            Element eleContainertype=(Element)lstContainertypes.get(i);
            if(eleContainertype==null) continue;
            String name=eleContainertype.attributeValue("name");
            String strclass=eleContainertype.attributeValue("class");
            if(name==null||name.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("配置报表的容器类型的name属性不能为空");
            }
            name=name.trim();
            if(mContainertypes.containsKey(name))
            {
                throw new WabacusConfigLoadingException("系统配置文件中配置的容器类型name属性："+name+"存在重复");
            }
            strclass=strclass==null?"":strclass.trim();
            if(strclass.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("系统配置文件中配置的name属性为"+name+"的容器类型没有配置class属性");
            }
            Object containerObj=AbsComponentType.createComponentTypeObj(ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(strclass),
                    null,null,null);
            if(!(containerObj instanceof AbsContainerType))
            {
                throw new WabacusConfigLoadingException("配置报表的容器类型的class："+strclass+"没有继承"+AbsContainerType.class.getName()+"类");
            }
            mContainertypes.put(name,(AbsContainerType)containerObj);
        }
        if(Config.getInstance().getMContainertypes()==null)
        {
            Config.getInstance().setMContainertypes(mContainertypes);
        }else
        {
            Tools.copyMapData(mContainertypes,Config.getInstance().getMContainertypes(),false);
        }
    }

    private static void loadReportTypesConfig(Element root)
    {
        Element eleReporttypes=XmlAssistant.getInstance().getSingleElementByName(root,"report-types");
        if(eleReporttypes==null) return;
        String defaultreporttype=eleReporttypes.attributeValue("default");
        defaultreporttype=defaultreporttype==null?"":defaultreporttype.trim();
        List lstReporttypes=eleReporttypes.elements("report-type");
        if(lstReporttypes==null||lstReporttypes.size()<=0) return;
        Map<String,IReportType> mReportTypes=new HashMap<String,IReportType>();
        for(int i=0;i<lstReporttypes.size();i++)
        {
            Element eleReporttype=(Element)lstReporttypes.get(i);
            if(eleReporttype==null) continue;
            String name=eleReporttype.attributeValue("name");
            String strclass=eleReporttype.attributeValue("class");
            name=name==null?"":name.trim();
            if(name.equals(""))
            {
                throw new WabacusConfigLoadingException("配置报表类型的name属性不能为空");
            }
            if(mReportTypes.containsKey(name))
            {
                throw new WabacusConfigLoadingException("配置报表类型的name属性："+name+"存在重复");
            }
            strclass=strclass==null?"":strclass.trim();
            if(strclass.equals(""))
            {
                throw new WabacusConfigLoadingException("配置报表类型的class属性不能为空");
            }
            Object reportObj=AbsComponentType.createComponentTypeObj(ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(strclass),null,
                    null,null);
            if(!(reportObj instanceof AbsReportType))
            {
                throw new WabacusConfigLoadingException("配置报表类型的class："+strclass+"没有继承"+AbsReportType.class.getName()+"类");
            }
            mReportTypes.put(name,(IReportType)reportObj);

        }
        if(Config.getInstance().getMReporttypes()==null)
        {
            Config.getInstance().setMReporttypes(mReportTypes);
        }else
        {
            Tools.copyMapData(mReportTypes,Config.getInstance().getMReporttypes(),false);
        }
        if(!defaultreporttype.equals(""))
        {
            Object reportObj=Config.getInstance().getMReporttypes().get(defaultreporttype);
            if(reportObj==null) throw new WabacusConfigLoadingException("配置的默认报表类型key："+defaultreporttype+"不存在");
            mReportTypes.put(Consts.DEFAULT_KEY,(IReportType)reportObj);
        }
    }

    public static Map convertPropertiesToResources(Properties props)
    {
        Map mResults=new HashMap();
        if(props==null||props.isEmpty())
        {
            return null;
        }
        Iterator itKeys=props.keySet().iterator();
        while(itKeys.hasNext())
        {
            String key=(String)itKeys.next();
            String value=(String)props.get(key);
            
            
            
            
            
            
            
            
            
            
            if(mResults.containsKey(key))
            {
                throw new WabacusConfigLoadingException("配置的资源key"+key+"存在重复，加载配置文件失败");
            }
            mResults.put(key,value);
        }
        return mResults;
    }

    private static void loadAllSkinConfigProperties()
    {
        File skinFileObj=new File(Tools.standardFilePath(Config.webroot_abspath+"\\webresources\\skin\\"));
        if(!skinFileObj.exists()||!skinFileObj.isDirectory()) return;
        File[] childFilesArr=skinFileObj.listFiles();
        if(childFilesArr.length==0) return;
        File childFileObjTmp;
        String skinNameTmp=null;
        for(int i=0;i<childFilesArr.length;i++)
        {
            childFileObjTmp=childFilesArr[i];
            if(!childFileObjTmp.isDirectory()) continue;
            skinNameTmp=childFileObjTmp.getName();
            childFileObjTmp=new File(childFileObjTmp.getAbsolutePath()+File.separator+"config.properties");
            if(!childFileObjTmp.exists()) continue;
            loadConfigSkinProperties(skinNameTmp,childFileObjTmp);
        }
    }

    private static void loadConfigSkinProperties(String skinName,File propFileObj)
    {
        BufferedInputStream bis=null;
        try
        {
            bis=new BufferedInputStream(new FileInputStream(propFileObj));
            Properties props=new Properties();
            props.load(bis);
            Config.getInstance().addSkinConfigProperties(skinName,convertPropertiesToResources(props));
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载主题风格"+skinName+"的config.properties配置文件失败",e);
        }finally
        {
            try
            {
                if(bis!=null) bis.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
