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
package com.wabacus.config.component;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.ResourceUtils;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.jsphtml.HtmlComponentBean;
import com.wabacus.config.component.application.jsphtml.JspComponentBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.component.application.report.condition.ConditionSelectItemBean;
import com.wabacus.config.component.application.report.condition.ConditionSelectorBean;
import com.wabacus.config.component.application.report.condition.ConditionValueSelectItemBean;
import com.wabacus.config.component.application.report.extendconfig.LoadExtendConfigManager;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.component.other.ButtonsBean;
import com.wabacus.config.database.type.Oracle;
import com.wabacus.config.dataexport.DataExportsConfigBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.print.DefaultPrintProviderConfigBean;
import com.wabacus.config.print.LodopPrintProviderConfigBean;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.template.TemplateParser;
import com.wabacus.config.template.tags.AbsTagInTemplate;
import com.wabacus.config.xml.XmlAssistant;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.CancelButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.ResetButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.buttons.UpdateButton;
import com.wabacus.system.buttons.WabacusButton;
import com.wabacus.system.commoninterface.IPagePersonalizePersistence;
import com.wabacus.system.commoninterface.IReportPersonalizePersistence;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.system.inputbox.OptionBean;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class ComponentConfigLoadManager
{
    private static Log log=LogFactory.getLog(ComponentConfigLoadManager.class);

    private static List<Integer> lstDefaultPageSize;
    
    public static int loadApplicationsConfigFiles(BufferedInputStream bisReportFile,String jsFileUrl,String jsFilePath,Map<String,Map> mLocalResourcesTemp) throws Exception
    {
        Document doc=XmlAssistant.getInstance().loadXmlDocument(bisReportFile);
        Element root=doc.getRootElement();
        if(root==null) return 0;
        Element eleLocalResources=XmlAssistant.getInstance().getSingleElementByName(root,"local-resources");
        if(eleLocalResources!=null)
        {
            Map mLocalDefineResources=new HashMap();
            Map mLocalResources=new HashMap();
            List lstLocalResources=eleLocalResources.elements("resource-file");
            List<String> lstLocalResourceFiles=ConfigLoadManager.getListConfigFilePaths(lstLocalResources);
            if(lstLocalResourceFiles!=null&&lstLocalResourceFiles.size()>0)
            {
                Map mResultsTmp;
                for(String file:lstLocalResourceFiles)
                {
                    if(file==null||file.trim().equals("")) continue;
                    String tempKey=file.trim().toLowerCase();
                    mResultsTmp=mLocalResourcesTemp.get(tempKey);
                    if(mResultsTmp==null)
                    {
                        mResultsTmp=ConfigLoadManager.loadResourceFile(file);
                        if(mResultsTmp==null) continue;
                        mLocalResourcesTemp.put(tempKey,mResultsTmp);
                    }
                    String key=Tools.copyMapData(mResultsTmp,mLocalResources,true);
                    if(key!=null)
                    {
                        throw new WabacusConfigLoadingException("在报表配置文件的local_resource文件中，name属性为"+key+"的资源存在重复，加载配置文件失败");
                    }
                }
                if(mLocalResources.size()>0)
                {
                    mLocalResources=Config.getInstance().getResources().replace(mLocalResources);
                    Config.getInstance().getResources().getMLocalResources().put(jsFileUrl,mLocalResources);
                }
            }
            Element eleLocalDefineResources=eleLocalResources.element("resources");
            if(eleLocalDefineResources!=null)
            {
                mLocalDefineResources=ConfigLoadManager.loadXmlResources(eleLocalDefineResources);
                if(mLocalDefineResources!=null&&mLocalDefineResources.size()>0)
                {
                    mLocalDefineResources=Config.getInstance().getResources().replace(mLocalDefineResources);
                    Config.getInstance().getResources().getMLocalDefineResources().put(jsFileUrl,mLocalDefineResources);
                }
            }
        }
        Config.getInstance().getMLocalCss().put(jsFileUrl,ConfigLoadManager.loadCssfiles(root.element("local-cssfiles")));
        Config.getInstance().getMLocalJavascript().put(jsFileUrl,ConfigLoadManager.loadJsfiles(root.element("local-jsfiles")));

        List lstPagesElement=XmlAssistant.getInstance().getElementsByName(root,"page");
        if(lstPagesElement==null||lstPagesElement.size()==0)
        {
            log.warn("报表配置文件没有配置报表!!!");
            return 0;
        }
        for(int i=0;i<lstPagesElement.size();i++)
        {
            Element elePage=(Element)lstPagesElement.get(i);
            if(elePage!=null)
            {
                XmlElementBean elePageBean=XmlAssistant.getInstance().parseXmlValueToXmlBean(elePage);
                loadPageConfig(jsFileUrl,jsFilePath,elePageBean);
            }
        }
        return 1;
    }

    private static void loadPageConfig(String jsFileUrl,String jsFilePath,XmlElementBean elePageBean)
    {
        PageBean pbean=new PageBean(null,"page");
        pbean.setJsFilePath(jsFilePath);
        pbean.setJsFileUrl(jsFileUrl);
        pbean.setReportfile_key(jsFileUrl);
        loadComponentCommonConfig(elePageBean,pbean);
        ConfigLoadManager.mAllPagesConfig.put(pbean.getId(),pbean);
        pbean.setAttrs(elePageBean.getMPropertiesClone());
        try
        {
            loadContainerCommonConfig(elePageBean,pbean);
            String css=elePageBean.attributeValue("css");
            if(css!=null)
            {
                String[] cssArray=Tools.parseStringToArray(css,",");
                if(cssArray.length>0)
                {
                    for(int k=0;k<cssArray.length;k++)
                    {
                        if(cssArray[k]==null||cssArray[k].trim().equals("")) continue;
                        cssArray[k]=Config.webroot+cssArray[k];
                        cssArray[k]=Tools.replaceAll(cssArray[k],"//","/");
                        pbean.addMyCss(cssArray[k]);
                    }
                }
            }
            String js=elePageBean.attributeValue("js");
            if(js!=null)
            {
                String[] jsArray=Tools.parseStringToArray(js,",");
                if(jsArray.length>0)
                {
                    for(int k=0;k<jsArray.length;k++)
                    {
                        if(jsArray[k]==null||jsArray[k].trim().equals("")) continue;
                        jsArray[k]=Config.webroot+jsArray[k];
                        jsArray[k]=Tools.replaceAll(jsArray[k],"//","/");
                        pbean.addMyJavascript(jsArray[k]);
                    }
                }
            }
            String personalizeclass=elePageBean.attributeValue("personalizeclass");
            if(personalizeclass!=null&&!personalizeclass.trim().equals(""))
            {
                Object obj=null;
                try
                {
                    obj=ResourceUtils.loadClass(personalizeclass).newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("页面"+pbean.getId()+"配置的personalizeclass："+personalizeclass+"类对象实例化失败",e);
                }
                if(!(obj instanceof IPagePersonalizePersistence))
                {
                    throw new WabacusConfigLoadingException("页面"+pbean.getId()+"配置的personalizeclass："+personalizeclass+"没有实现"
                            +IPagePersonalizePersistence.class.getName()+"接口");
                }
                pbean.setPersonalizeObj((IPagePersonalizePersistence)obj);
            }
            
            String checkpermission=elePageBean.attributeValue("checkpermission");
            if(checkpermission==null||(!checkpermission.trim().toLowerCase().equals("true")&&!checkpermission.trim().toLowerCase().equals("false")))
            {//如果没有配置checkpermission属性，或者配置的不合法，则用默认全局配置值
                pbean.setCheckPermission(Config.getInstance().getSystemConfigValue("default-checkpermission",true));
            }else
            {
                pbean.setCheckPermission(Boolean.parseBoolean(checkpermission.toLowerCase().trim()));
            }
            loadPageInterceptor(elePageBean,pbean);
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载页面"+pbean.getId()+"失败",e);
        }
    }

    private static void loadPageInterceptor(XmlElementBean elePageBean,PageBean pbean)
    {
        String interceptor=elePageBean.attributeValue("interceptor");
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            List<String> lstInterceptors=Tools.parseStringToList(interceptor,";",false);
            Class clsTmp;
            Object objTmp;
            for(String interceptorTmp:lstInterceptors)
            {
                if(interceptorTmp.equals("")) continue;
                try
                {
                    clsTmp=ResourceUtils.loadClass(interceptorTmp);
                }catch(ClassNotFoundException e)
                {
                    throw new WabacusConfigLoadingException("初始化页面"+pbean.getId()+"的拦截器"+interceptorTmp+"失败",e);
                }
                try
                {
                    objTmp=clsTmp.newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("实例化页面"+pbean.getId()+"的拦截器"+interceptorTmp+"失败",e);
                }
                if(!(objTmp instanceof AbsPageInterceptor))
                {
                    throw new WabacusConfigLoadingException("页面"+pbean.getId()+"的拦截器"+interceptorTmp+"没有继承"+AbsPageInterceptor.class.getName()+"类");
                }
                pbean.addInterceptor((AbsPageInterceptor)objTmp);
            }
        }
        XmlElementBean eleInterceptorBean=elePageBean.getChildElementByName("interceptor");
        if(eleInterceptorBean!=null)
        {
            List<String> lstImportPackages=ConfigLoadAssistant.getInstance().loadImportsConfig(eleInterceptorBean);
            XmlElementBean elePreAction=eleInterceptorBean.getChildElementByName("preaction");
            String preaction=null;
            if(elePreAction!=null)
            {
                preaction=elePreAction.getContent();
            }
            preaction=preaction==null?"":preaction.trim();
            XmlElementBean elePostAction=eleInterceptorBean.getChildElementByName("postaction");
            String postaction=null;
            if(elePostAction!=null)
            {
                postaction=elePostAction.getContent();
            }
            postaction=postaction==null?"":postaction.trim();
            if(!preaction.equals("")||!postaction.equals(""))
            {
                Class c=ComponentAssistant.getInstance().buildPageInterceptorClass(pbean,lstImportPackages,preaction,postaction);
                try
                {
                    pbean.addInterceptor((AbsPageInterceptor)c.newInstance());
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("为页面"+pbean.getId()+"生成拦截器类失败",e);
                }
            }

        }
    }
    
    public static void loadComponentCommonConfig(XmlElementBean eleComponent,IComponentConfigBean acbean)
    {
        String id=eleComponent.attributeValue("id");
        String title=eleComponent.attributeValue("title");
        String subtitle=eleComponent.attributeValue("subtitle");
        String titlealign=eleComponent.attributeValue("titlealign");
        String parenttitle=eleComponent.attributeValue("parenttitle");
        String parentsubtitle=eleComponent.attributeValue("parentsubtitle");
        String width=eleComponent.attributeValue("width");
        String height=eleComponent.attributeValue("height");
        String align=eleComponent.attributeValue("align");
        String valign=eleComponent.attributeValue("valign");
        String top=eleComponent.attributeValue("top");
        String bottom=eleComponent.attributeValue("bottom");
        String left=eleComponent.attributeValue("left");
        String right=eleComponent.attributeValue("right");
        String scrollstyle=eleComponent.attributeValue("scrollstyle");
        String dataexport=eleComponent.attributeValue("dataexport");
        String contextmenu=eleComponent.attributeValue("contextmenu");
        String onload=eleComponent.attributeValue("onload");
        if(!ConfigLoadAssistant.isValidId(id))
        {
            throw new WabacusConfigLoadingException("页面"+acbean.getPageBean().getId()+"中子元素的id属性"+id+"为空或不合法");
        }
        id=id.trim();
        if(acbean.getParentContainer()==null)
        {//如果当前是加载顶层<page/>配置信息
            if(ConfigLoadManager.mAllPagesConfig.containsKey(id))
            {
                throw new WabacusConfigLoadingException("配置的<page/>的id属性："+id+"存在重复");
            }
        }else
        {
            if(id.equals(acbean.getPageBean().getId()))
            {
                throw new WabacusConfigLoadingException("id为"+acbean.getPageBean().getId()+"的页面中，存在本同ID的子组件");
            }
            List<String> lstAllChildIds=ConfigLoadManager.mAllPageChildIds.get(acbean.getPageBean().getId());
            if(lstAllChildIds==null)
            {
                lstAllChildIds=new ArrayList<String>();
                ConfigLoadManager.mAllPageChildIds.put(acbean.getPageBean().getId(),lstAllChildIds);
            }else if(lstAllChildIds.contains(id))
            {
                throw new WabacusConfigLoadingException("id为"+acbean.getPageBean().getId()+"的页面中，子组件ID:"+id+"存在重复");
            }
            lstAllChildIds.add(id);
        }
        acbean.setId(id.trim());
        if(title!=null)
        {
            acbean.setTitle(Config.getInstance().getResourceString(null,acbean.getPageBean(),title,true));
        }
        if(subtitle!=null)
        {
            acbean.setSubtitle(Config.getInstance().getResourceString(null,acbean.getPageBean(),subtitle,true));
        }
        if(titlealign!=null)
        {
            titlealign=titlealign.toLowerCase().trim();
            acbean.setTitlealign(titlealign.trim());
        }
        if(parenttitle!=null)
        {
            acbean.setParenttitle(Config.getInstance().getResourceString(null,acbean.getPageBean(),parenttitle,true));
        }
        if(parentsubtitle!=null)
        {
            acbean.setParentSubtitle(parentsubtitle.trim());
        }
        if(top!=null)
        {
            acbean.setTop(getRealHtmlSizeValueByConfig(top.trim()));
        }
        if(bottom!=null)
        {
            acbean.setBottom(getRealHtmlSizeValueByConfig(bottom.trim()));
        }
        if(left!=null)
        {
            acbean.setLeft(getRealHtmlSizeValueByConfig(left.trim()));
        }
        if(right!=null)
        {
            acbean.setRight(getRealHtmlSizeValueByConfig(right.trim()));
        }
        if(width!=null)
        {
            acbean.setWidth(getRealHtmlSizeValueByConfig(width.trim()));
        }
        if(height!=null)
        {
            acbean.setHeight(getRealHtmlSizeValueByConfig(height.trim()));
        }
        if(align!=null)
        {
            acbean.setAlign(align.trim());
        }
        if(valign!=null)
        {
            acbean.setValign(valign.trim());
        }
        DataExportsConfigBean debean=acbean.getDataExportsBean();
        if(debean==null) debean=new DataExportsConfigBean(acbean);
        if(dataexport!=null)
        {
            acbean.setDataExportsBean(debean);
            dataexport=dataexport.trim();
            if(dataexport.equals(""))
            {
                debean.setLstAutoDataExportTypes(null);
            }else
            {
                debean.setLstAutoDataExportTypes(Tools.parseStringToList(dataexport,"|"));
            }
        }
        XmlElementBean eleDataExportsBean=eleComponent.getChildElementByName("dataexports");
        if(eleDataExportsBean!=null)
        {
            acbean.setDataExportsBean(debean);
            debean.loadConfig(eleDataExportsBean);
        }
        if(scrollstyle!=null)
        {
            scrollstyle=scrollstyle.toLowerCase().trim();
            if(scrollstyle.equals(""))
            {
                acbean.setScrollstyle(null);
            }else if(!Consts_Private.lstAllScrollStyles.contains(scrollstyle))
            {
                throw new WabacusConfigLoadingException("为组件"+acbean.getPath()+"配置的scrollstyle属性值："+scrollstyle+"不支持");
            }else
            {
                acbean.setScrollstyle(scrollstyle);
            }
        }
        
        if(contextmenu!=null)
        {
            contextmenu=contextmenu.toLowerCase().trim();
            if(contextmenu.equals("false"))
            {
                acbean.setShowContextMenu(false);
            }else
            {
                acbean.setShowContextMenu(true);
            }
        }
        XmlElementBean elePrintBean=eleComponent.getChildElementByName("print");
        if(elePrintBean!=null)
        {
            String type=elePrintBean.attributeValue("type");
            type=type==null?"":type.toLowerCase().trim();
            if(type.equals("")||type.equals("default")||type.equals("lodop"))
            {
                AbsPrintProviderConfigBean printConfigBean=null;
                if(type.equals("lodop"))
                {
                    printConfigBean=new LodopPrintProviderConfigBean(acbean);
                }else
                {
                    printConfigBean=new DefaultPrintProviderConfigBean(acbean);
                }
                printConfigBean.loadConfig(elePrintBean);
                acbean.setPrintBean(printConfigBean);
                acbean.setPdfPrintBean(null);
            }else if(type.equals("pdf"))
            {
                acbean.setPrintBean(null);//将可能从父报表继承过来的其它类型的打印清空
                PDFExportBean pdfprintbean=new PDFExportBean(acbean,Consts.DATAEXPORT_PDF);
                pdfprintbean.setPrint(true);
                pdfprintbean.loadConfig(elePrintBean);
                acbean.setPdfPrintBean(pdfprintbean);
            }else if(type.equals("none"))
            {
                acbean.setPrintBean(null);
                acbean.setPdfPrintBean(null);
            }else
            {
                throw new WabacusConfigLoadingException("加载组件"+acbean.getPath()+"的打印功能失败，为其<print/>配置的type属性"+type+"不支持");
            }
        }
        XmlElementBean eleButtonBean=eleComponent.getChildElementByName("buttons");
        if(eleButtonBean!=null)
        {
            loadButtonsInfo(acbean,eleButtonBean);
        }
        //加载组件的header/footer
        loadHeaderFooterConfig(acbean,eleComponent,"header");
        loadHeaderFooterConfig(acbean,eleComponent,"footer");
        if(onload!=null)
        {
            onload=onload.trim();
            if(onload.equals(""))
            {
                acbean.removeOnloadMethodByType(Consts_Private.ONLOAD_CONFIG);
            }else
            {
                List<String> lstOnloadMethods=Tools.parseStringToList(onload,";");
                for(String onloadTmp:lstOnloadMethods)
                {
                    if(onloadTmp.trim().equals("")) continue;
                    acbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONLOAD_CONFIG,onloadTmp));
                }
            }
        }
    }

    private static String getRealHtmlSizeValueByConfig(String htmlsize)
    {
        if(htmlsize==null||htmlsize.trim().equals("")) return "";
        htmlsize=htmlsize.trim();
        String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(htmlsize);
        if(htmlsizeArr==null) return "";
        if(htmlsizeArr[0].equals("0")) return "";//如果配置为0，则相当于没有配置
        return htmlsizeArr[0]+htmlsizeArr[1];
    }
    
    public static void loadApplicationCommonConfig(XmlElementBean eleApplication,IApplicationConfigBean acbean)
    {
        String refreshid=eleApplication.attributeValue("refreshid");
        if(refreshid!=null)
        {
            acbean.setRefreshid(refreshid.trim());
        }
        String printwidth=eleApplication.attributeValue("printwidth");
        if(printwidth!=null)
        {
            acbean.setPrintwidth(printwidth.trim());
        }
    }

    private final static List<String> LstNonChildComponentNames=new ArrayList<String>();
    static
    {
        LstNonChildComponentNames.add("dataexports");
        LstNonChildComponentNames.add("print");
        LstNonChildComponentNames.add("buttons");
        LstNonChildComponentNames.add("interceptor");
        LstNonChildComponentNames.add("header");
        LstNonChildComponentNames.add("footer");
    }
    
    public static void loadContainerCommonConfig(XmlElementBean eleContainer,AbsContainerConfigBean ccbean)
    {
        String border=eleContainer.attributeValue("border");
        String bordercolor=eleContainer.attributeValue("bordercolor");
        String margin=eleContainer.attributeValue("margin");
        String margin_left=eleContainer.attributeValue("margin_left");
        String margin_right=eleContainer.attributeValue("margin_right");
        String margin_top=eleContainer.attributeValue("margin_top");
        String margin_bottom=eleContainer.attributeValue("margin_bottom");
        String titleposition=eleContainer.attributeValue("titleposition");
        String scrollX=eleContainer.attributeValue("scrollX");
        String scrollY=eleContainer.attributeValue("scrollY");
        
        if(scrollX!=null&&scrollX.trim().equalsIgnoreCase("true"))
        {
            ccbean.setScrollX(true);
            if(ccbean.getWidth()==null||ccbean.getWidth().trim().equals("")||ccbean.getWidth().indexOf("%")>=0)
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"配置了横向滚动条，所以必须为其配置width属性，且不能配置为百分比");
            }
        }
        if(scrollY!=null&&scrollY.trim().equalsIgnoreCase("true"))
        {
            ccbean.setScrollY(true);
            if(ccbean.getHeight()==null||ccbean.getHeight().trim().equals("")||ccbean.getHeight().indexOf("%")>=0)
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"配置了垂直滚动条，所以必须为其配置height属性，且不能配置为百分比");
            }
        }
        
        ComponentAssistant.getInstance().doPostLoadForComponentScroll(ccbean,ccbean.isScrollX(),ccbean.isScrollY(),ccbean.getWidth(),
                ccbean.getHeight(),ccbean.getScrollstyle());
        if(border!=null)
        {
            try
            {
                ccbean.setBorder(Integer.parseInt(border.trim()));
            }catch(NumberFormatException e)
            {
                log.warn("页面"+ccbean.getPageBean().getId()+"中id为"+ccbean.getId()+"的子元素border属性不是合法数字",e);
            }
        }
        if(bordercolor!=null)
        {
            ccbean.setBordercolor(bordercolor.trim());
        }
        if(margin!=null)
        {//这个配置对left/right/top/bottom都有效
            ccbean.setMargin_left(margin.trim());
            ccbean.setMargin_right(margin.trim());
            ccbean.setMargin_top(margin.trim());
            ccbean.setMargin_bottom(margin.trim());
        }
        if(margin_left!=null)
        {
            ccbean.setMargin_left(margin_left.trim());
        }
        if(margin_right!=null)
        {
            ccbean.setMargin_right(margin_right.trim());
        }
        if(margin_top!=null)
        {
            ccbean.setMargin_top(margin_top.trim());
        }
        if(margin_bottom!=null)
        {
            ccbean.setMargin_bottom(margin_bottom.trim());
        }
        if(titleposition!=null) ccbean.setTitleposition(titleposition.trim());
        if(ccbean.getMargin_left()!=null&&!ccbean.getMargin_left().trim().equals("")&&ccbean.getMargin_right()!=null&&!ccbean.getMargin_right().trim().equals(""))
        {
            ccbean.setColspan_total(3);
        }else if((ccbean.getMargin_left()!=null&&!ccbean.getMargin_left().trim().equals(""))||(ccbean.getMargin_right()!=null&&!ccbean.getMargin_right().trim().equals("")))
        {
            ccbean.setColspan_total(2);
        }else
        {
            ccbean.setColspan_total(1);
        }
        
        Map<String,IComponentConfigBean> mChildren=new HashMap<String,IComponentConfigBean>();
        List<String> lstChildrenIDs=new ArrayList<String>();
        ccbean.setMChildren(mChildren);
        ccbean.setLstChildrenIDs(lstChildrenIDs);
        List<XmlElementBean> lstChildElements=eleContainer.getLstChildElements();
        if(lstChildElements==null&&lstChildElements.size()==0)
        {
            throw new WabacusConfigLoadingException("加载页面/容器"+ccbean.getPath()+"失败，内容为空");
        }
        for(XmlElementBean eleChildTmp:lstChildElements)
        {
            if(eleChildTmp==null) continue;
            if(LstNonChildComponentNames.contains(eleChildTmp.getName())) continue;
            String childid=eleChildTmp.attributeValue("id");
            if(!ConfigLoadAssistant.isValidId(childid))
            {
                throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"中配置的子元素对象的id"+childid+"不合法");
            }
            lstChildrenIDs.add(childid);
            String tagname=eleChildTmp.getName();
            tagname=tagname==null?"":tagname.trim();
            if(tagname.equals("report"))
            {
                ComponentConfigLoadManager.loadReportConfig(eleChildTmp,ccbean);
            }else if(tagname.equals("html"))
            {
                HtmlComponentBean hcbean=new HtmlComponentBean(ccbean);
                loadComponentCommonConfig(eleChildTmp,hcbean);
                loadApplicationCommonConfig(eleChildTmp,hcbean);
                ccbean.getMChildren().put(hcbean.getId(),hcbean);
                hcbean.loadExtendConfig(eleChildTmp,ccbean);
            }else if(tagname.equals("jsp"))
            {
                JspComponentBean jspcbean=new JspComponentBean(ccbean);
                loadComponentCommonConfig(eleChildTmp,jspcbean);
                loadApplicationCommonConfig(eleChildTmp,jspcbean);
                ccbean.getMChildren().put(jspcbean.getId(),jspcbean);
                jspcbean.loadExtendConfig(eleChildTmp,ccbean);
            }else
            {
                AbsContainerType childContainer=Config.getInstance().getContainerType(tagname);
                if(childContainer==null)
                {
                    throw new WabacusConfigLoadingException("容器"+ccbean.getPath()+"配置的id属性："+childid+"的子元素对应的容器"+tagname+"不存在");
                }
                ccbean.getMChildren().put(childid,childContainer.loadConfig(eleChildTmp,ccbean,tagname));
            }
        }
    }


    public static void loadReportConfig(XmlElementBean eleReportBean,AbsContainerConfigBean parentContainerBean)
    {
        String reportid=eleReportBean.attributeValue("id");
        reportid=reportid.trim();
        String reportextends=eleReportBean.attributeValue("extends");
        ReportBean rbean=null;
        ReportBean rbeanParent=null;
        try
        {
            if(reportextends!=null&&!reportextends.trim().equals(""))
            {
                rbeanParent=getReportBeanByPath(reportextends);
                if(rbeanParent==null||rbeanParent.getEleReportBean()!=null)
                {
                    rbean=new ReportBean(parentContainerBean);
                    rbean.setId(reportid);
                    rbean.setEleReportBean(eleReportBean);
                    parentContainerBean.getMChildren().put(reportid,rbean);
                    ConfigLoadManager.lstExtendReports.add(rbean);
                    return;
                }else
                {
                    rbean=(ReportBean)rbeanParent.clone(reportid,parentContainerBean);
//                    rbean.setId(reportid);

                }
            }else
            {
                rbean=new ReportBean(parentContainerBean);
                rbean.setId(reportid);
            }
            rbean.setAttrs(eleReportBean.getMPropertiesClone());
            parentContainerBean.getMChildren().put(reportid,rbean);
            loadReportInfo(rbean,eleReportBean,rbeanParent);
        }catch(Exception e)
        {
            String reportid2="";
            if(rbean!=null)
            {
                reportid2=rbean.getId();
            }
            throw new WabacusConfigLoadingException("加载报表"+parentContainerBean.getPath()+Consts_Private.PATH_SEPERATOR+reportid2+"时出错",e);
        }
    }

    public static ReportBean getReportBeanByPath(String path)
    {
        if(path==null||path.trim().equals("")||path.trim().indexOf(Consts_Private.PATH_SEPERATOR)<=0) return null;
        int idx=path.lastIndexOf(Consts_Private.PATH_SEPERATOR);
        String pageid=path.substring(0,idx).trim();
        String reportid=path.substring(idx+1).trim();
        PageBean pbean=ConfigLoadManager.mAllPagesConfig.get(pageid);
        if(pbean==null) return null;
        return pbean.getReportChild(reportid,true);
    }

    public static int loadReportInfo(ReportBean rb,XmlElementBean eleReportBean,ReportBean rbParent) throws Exception
    {
        loadComponentCommonConfig(eleReportBean,rb);
        loadApplicationCommonConfig(eleReportBean,rb);
        List<XmlElementBean> lstEleReportBeans=new ArrayList<XmlElementBean>();
        lstEleReportBeans.add(eleReportBean);
        
        String type=eleReportBean.attributeValue("type");
        if(type!=null) rb.setType(type.trim());
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(rb,lstEleReportBeans);
        String strclass=eleReportBean.attributeValue("class");
        String formatclass=eleReportBean.attributeValue("formatclass");
        String dataimport=eleReportBean.attributeValue("dataimport");
        String dataimportwidth=eleReportBean.attributeValue("dataimportwidth");
        String dataimportheight=eleReportBean.attributeValue("dataimportheight");
        String dataimportinitsize=eleReportBean.attributeValue("dataimportinitsize");
        String dataimportmaxbtn=eleReportBean.attributeValue("dataimportmaxbtn");
        String dataimportminbtn=eleReportBean.attributeValue("dataimportminbtn");
        String border=eleReportBean.attributeValue("border");
        String bordercolor=eleReportBean.attributeValue("bordercolor");
        String jsvalidatetype=eleReportBean.attributeValue("jsvalidatetype");
        String template=eleReportBean.attributeValue("template");
        String cellresize=eleReportBean.attributeValue("cellresize");
        String celldrag=eleReportBean.attributeValue("celldrag");
        String depends=eleReportBean.attributeValue("depends");
        String refreshparentonsave=eleReportBean.attributeValue("refreshparentonsave");

        String dependstype=eleReportBean.attributeValue("dependstype");
        String dependsParams=eleReportBean.attributeValue("dependsparams");
        String scrollheight=eleReportBean.attributeValue("scrollheight");
        String scrollwidth=eleReportBean.attributeValue("scrollwidth");
        String pagesize=eleReportBean.attributeValue("pagesize");
        String navigate_reportid=eleReportBean.attributeValue("navigate_reportid");
        String navigate=eleReportBean.attributeValue("navigate");
        String personalizeclass=eleReportBean.attributeValue("personalizeclass");
        loadInterceptorInfo(eleReportBean,rb);

        if(pagesize!=null)
        {
            pagesize=pagesize.trim();
            if(pagesize.equals(""))
            {
                rb.setLstPagesize(null);
            }else
            {
                rb.setLstPagesize(parsePagesize(rb,pagesize));
            }
        }
        if(rb.getLstPagesize()==null||rb.getLstPagesize().size()==0)
        {//当前报表没有配置pagesize，且没有继承其它报表，则用全局默认配置
            if(rb.isDetailReportType())
            {
                List<Integer> lstPageSize=new ArrayList<Integer>();
                lstPageSize.add(0);
                rb.setLstPagesize(lstPageSize);
            }else
            {
                if(lstDefaultPageSize==null)
                {
                    lstDefaultPageSize=parsePagesize(null,Config.getInstance().getSystemConfigValue("default-pagesize","10"));
                    if(lstDefaultPageSize==null||lstDefaultPageSize.size()==0)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，没有为其配置pagesize值，且没有在wabacus.cfg.xml中指定全局默认页大小");
                    }
                }
                rb.setLstPagesize(lstDefaultPageSize);
            }
        }
        if(navigate_reportid!=null) rb.setNavigate_reportid(navigate_reportid.trim());
        if(navigate!=null)
        {
            navigate=navigate.trim();
            if(navigate.equals(""))
            {
                rb.setNavigateObj(null);
            }else
            {
                Object obj=navigate;
                if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(navigate))
                {
                    if(Tools.isDefineKey("$",navigate))
                    {
                        obj=Config.getInstance().getResourceObject(null,rb.getPageBean(),navigate,true);
                    }else
                    {//是从静态模板文件中获取
                        obj=TemplateParser.parseTemplateByPath(navigate);
                    }
                }
                rb.setNavigateObj(obj);
            }
        }
        if(personalizeclass!=null)
        {
            personalizeclass=personalizeclass.trim();
            if(personalizeclass.equals(""))
            {
                rb.setPersonalizeObj(null);
            }else if(personalizeclass.toLowerCase().equals("default"))
            {
                rb.setPersonalizeObj(Config.default_reportpersonalize_object);
            }else
            {
                Object obj=null;
                try
                {
                    obj=ResourceUtils.loadClass(personalizeclass).newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("报表"+rb.getPath()+"配置的personalizeclass："+personalizeclass+"类对象无例化失败",e);
                }
                if(!(obj instanceof IReportPersonalizePersistence))
                {
                    throw new WabacusConfigLoadingException("报表"+rb.getPath()+"配置的personalizeclass："+personalizeclass+"没有实现"
                            +IReportPersonalizePersistence.class.getName()+"接口");
                }
                rb.setPersonalizeObj((IReportPersonalizePersistence)obj);
            }
        }
        if(jsvalidatetype==null)
        {
            if(rbParent==null)
            {
                jsvalidatetype=Config.getInstance().getSystemConfigValue("default-jsvalidatetype","");
            }else
            {
                jsvalidatetype=String.valueOf(rbParent.getJsvalidatetype());
            }
        }else
        {
            jsvalidatetype=jsvalidatetype.trim();
            if(jsvalidatetype.trim().equals(""))
            {
                jsvalidatetype=Config.getInstance().getSystemConfigValue("default-jsvalidatetype","");
            }
        }
        if(jsvalidatetype.equals("")) jsvalidatetype="0";
        rb.setJsvalidatetype(Integer.parseInt(jsvalidatetype));
        if(depends!=null)
        {
            depends=depends.trim();
            if(depends.equals(rb.getId()))
            {
                throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，不能自己依赖自己");
            }
            if(rb.getRefreshid()!=null&&!rb.getRefreshid().trim().equals("")&&!rb.getRefreshid().trim().equals(rb.getId()))
            {
                throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，此报表是从报表，不能配置refreshid，因为从报表永远只能刷新自己");
            }
            rb.setDependParentId(depends);
            if(dependstype!=null)
            {
                dependstype=dependstype.toLowerCase().trim();
                if(dependstype.equals(""))
                {
                    rb.setDisplayOnParentNoData(true);
                }else if(!dependstype.equals("hidden")&&!dependstype.equals("display"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，此报表是从报表，其dependstype属性只能配置为display或hidden");
                }else
                {
                    rb.setDisplayOnParentNoData(dependstype.equals("display"));
                }
            }
            if(refreshparentonsave!=null)
            {
                refreshparentonsave=refreshparentonsave.trim();
                if(refreshparentonsave.trim().equals(""))
                {
                    rb.setRefreshParentOnSave(null);
                }else
                {
                    int idx=refreshparentonsave.indexOf("|");
                    if(idx==0)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，配置的refreshparentonsave不合法");
                    }else if(idx<0)
                    {
                        rb.setRefreshParentOnSave(new String[]{refreshparentonsave,"false"});
                    }else
                    {
                        rb.setRefreshParentOnSave(new String[]{refreshparentonsave.substring(0,idx).trim(),refreshparentonsave.substring(idx+1).trim()});
                    }
                }
            }


//                refreshparentondelete=refreshparentondelete.trim();




//                {




//                    }else if(idx<0)

//                        rb.setRefreshParentOnDelete(new String[]{refreshparentondelete,"false"});//默认刷新主报表时不更新它的页码



//                    }


            if(dependsParams!=null&&!dependsParams.trim().equals(""))
            {
                rb.setDependparams(dependsParams.trim());
            }
            rb.getPageBean().addRelateReports(rb);
        }
        
        if(dataimport!=null)
        {
            if(dataimport.trim().equals(""))
            {
                rb.setLstDataImportItems(null);
            }else
            {
                List<AbsDataImportConfigBean> lstDataImports=new ArrayList<AbsDataImportConfigBean>();
                List<String> lst=Tools.parseStringToList(dataimport,"|");
                for(String strTmp:lst)
                {
                    if(strTmp.equals("")) continue;
                    if(!Tools.isDefineKey("$",strTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，配置的数据导出项"+strTmp+"不是从资源文件中获取");
                    }
                    Object obj=Config.getInstance().getResourceObject(null,rb.getPageBean(),strTmp,true);
                    if(!(obj instanceof AbsDataImportConfigBean))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败，配置的数据导出项"+strTmp+"对应的资源项不是数据导出项资源类型");
                    }
                    lstDataImports.add((AbsDataImportConfigBean)obj);
                }
                rb.setLstDataImportItems(lstDataImports);
            }
        }
        if(dataimportwidth!=null) rb.setDataimportwidth(Tools.getWidthHeightIntValue(dataimportwidth));
        if(dataimportheight!=null) rb.setDataimportheight(Tools.getWidthHeightIntValue(dataimportheight));
        if(dataimportinitsize!=null) rb.setDataimportinitsize(dataimportinitsize.toLowerCase().trim());
        if(dataimportmaxbtn!=null) rb.setDataimportmaxbtn(dataimportmaxbtn.toLowerCase().trim());
        if(dataimportminbtn!=null) rb.setDataimportminbtn(dataimportminbtn.toLowerCase().trim());
        if(border!=null)
        {
            border=border.toLowerCase().trim();
            if(border.equals("")) border=Consts_Private.REPORT_BORDER_ALL;
            if(!Consts_Private.lstAllReportBorderTypes.contains(border))
            {
                log.warn("报表"+rb.getPath()+"配置的border属性"+border+"无效，将采用默认边框");
                border=Consts_Private.REPORT_BORDER_ALL;
            }
            rb.setBorder(border);
        }
        if(bordercolor!=null)
        {
            rb.setBordercolor(bordercolor.trim());
        }
        if(scrollheight!=null)
        {
            scrollheight=scrollheight.trim();
            rb.setScrollheight(scrollheight.trim());
        }
        if(rb.getScrollheight()!=null&&!rb.getScrollheight().trim().equals(""))
        {
            String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(rb.getScrollheight().trim());
            if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0")) 
            {
                rb.setScrollheight(null);
            }else
            {
                if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败,配置的scrollheight不能是百分比,而必须配置为像素或其它单位");
                }
                rb.setScrollheight(htmlsizeArr[0]+htmlsizeArr[1]);
            }            
        }
        if(scrollwidth!=null)
        {
            scrollwidth=scrollwidth.trim();
            rb.setScrollwidth(scrollwidth.trim());
        }
        if(rb.getScrollwidth()!=null&&!rb.getScrollwidth().trim().equals(""))
        {
            String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(rb.getScrollwidth().trim());
            if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0")) 
            {//配置的html大小无效或配置为0，则相当于没有配置
                rb.setScrollwidth(null);
            }else
            {
                if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rb.getPath()+"失败,配置的scrollwidth不能是百分比,而必须配置为像素或其它单位");
                }
                rb.setScrollwidth(htmlsizeArr[0]+htmlsizeArr[1]);
            }       
        }
        if(cellresize==null)
        {
            if(rbParent==null)
            {
                rb.setCellresize(Config.getInstance().getSystemConfigValue("default-cellresize",0));
            }
        }else
        {
            int icellresize=0;
            if(cellresize.trim().equals(""))
            {
                icellresize=Config.getInstance().getSystemConfigValue("default-cellresize",0);
            }else
            {
                try
                {
                    icellresize=Integer.parseInt(cellresize.trim());
                }catch(NumberFormatException e)
                {
                    icellresize=0;
                }
                if(icellresize>2||icellresize<0) icellresize=0;
            }
            rb.setCellresize(icellresize);
        }
        if(celldrag==null)
        {
            if(rbParent==null)
            {
                rb.setCelldrag(Config.getInstance().getSystemConfigValue("default-celldrag",0));
            }
        }else
        {
            int icelldrag=0;
            if(celldrag.trim().equals(""))
            {
                icelldrag=Config.getInstance().getSystemConfigValue("default-celldrag",0);
            }else
            {
                try
                {
                    icelldrag=Integer.parseInt(celldrag.trim());
                }catch(NumberFormatException e)
                {
                    icelldrag=0;
                }
                if(icelldrag>2||icelldrag<0) icelldrag=0;
            }
            rb.setCelldrag(icelldrag);
        }
        if(formatclass!=null)
        {
            if(formatclass.trim().equals(""))
            {
                rb.setLstFormatClasses(null);
            }else
            {
                rb.setLstFormatClasses(ConfigLoadAssistant.getInstance().convertStringToClassList(formatclass));
            }
        }
        if(strclass!=null)
        {
            rb.setStrclass(strclass.trim());
        }
        if(template!=null)
        {
            template=template.trim();
            if(template.equals(""))
            {
                rb.setTplBean(null);
                rb.setDynTplPath(null);
            }else
            {
                if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(template))
                {
                    rb.setTplBean(ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(rb.getPageBean(),template));
                }else
                {
//                   if(!template.toLowerCase().startsWith("http://")&&!template.toLowerCase().startsWith(Config.webroot))

//                       template=Config.webroot+"/"+template;
//                   }
//                   template=Tools.replaceAll(template,"//","/");
                   rb.setDynTplPath(template);
                }
                
            }
        }
        if(rb.getTplBean()==null&&(rb.getDynTplPath()==null||rb.getDynTplPath().trim().equals("")))
        {
            rb.setTplBean(Config.getInstance().getDefaultReportTplBean());
        }
        
        
        XmlElementBean eleSqlBean=eleReportBean.getChildElementByName("sql");
        if(eleSqlBean!=null)
        {
        	SqlBean sbean=new SqlBean(rb);
            rb.setSbean(sbean);
           
        	rb.setMSelectBoxesInConditionWithRelate(null);//配置了自己的<sql/>，则把从父报表中继承过来的查询条件关联下拉框对象从rbean中移除掉
            loadSqlInfo(sbean,eleSqlBean);
          
        }
        
        
        XmlElementBean eleDisplayBean=eleReportBean.getChildElementByName("display");
        if(eleDisplayBean!=null)
        {
            rb.setMSelectBoxesInColWithRelate(null);//配置了自己的<display/>，则把从父报表中继承过来的查询条件关联下拉框对象从rbean中移除掉
            DisplayBean dbean=new DisplayBean(rb);
            int flag=loadDisplayInfo(dbean,eleDisplayBean);
            if(flag==-1) return -1;
            rb.setDbean(dbean);
        }
        

       

        
        String format=null;
        List<String> lstImports=null;
        XmlElementBean eleFormatBean=eleReportBean.getChildElementByName("format");
        if(eleFormatBean!=null)
        {
            List<XmlElementBean> lstEleFormatBeans=new ArrayList<XmlElementBean>();
            lstEleFormatBeans.add(eleFormatBean);
            lstEleFormatBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleFormatBean.attributeValue("ref"),"format",null,rb));//取到所有被此<format ref=""/>引用的<format/>配置
            lstImports=getListImportPackages(lstEleFormatBeans);
            XmlElementBean eleFormatValueBean=null;
            for(XmlElementBean eleFormatBeanTmp:lstEleFormatBeans)
            {
                eleFormatValueBean=eleFormatBeanTmp.getChildElementByName("value");
                if(eleFormatValueBean!=null) break;
            }
            if(eleFormatValueBean!=null)
            {
                format=eleFormatValueBean.getContent();
                if(format!=null)
                {
                    format=format.trim();
                    if(format.equals(""))
                    {//如果配置了<format/>的<value/>，但内容配置为空字符串，则显式将它的FormatBean对象置空，这在从父报表继承了format方法但本报表不想用的情况下有用
                        rb.setFbean(null);
                    }else
                    {
                        FormatBean fbean=new FormatBean(rb);
                        fbean.setFormatContent(format);
                        fbean.setLstImports(lstImports);
                        rb.setFbean(fbean);
                    }
                }
            }
        }
        rb.setBlClasscache(true);
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(rb,lstEleReportBeans);
        //        LoadExtendConfigManager.loadAfterExtendConfigForPagetype(rb,lstEleReportBeans);
        return 1;
    }

    public static boolean isValidNavigateObj(ReportBean rbean,Object navigateObj)
    {
        if(!(navigateObj instanceof String)&&!(navigateObj instanceof TemplateBean))
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的navigate对象类型："+navigateObj.getClass().getName()
                    +"不合法，必须为String或TemplateBean类型之一");
        }
        if(navigateObj instanceof TemplateBean)
        {
            TemplateBean tplbean=(TemplateBean)navigateObj;
            if(tplbean.getLstTagChildren()!=null)
            {
                for(AbsTagInTemplate tagbeanTmp:tplbean.getLstTagChildren())
                {
                    if(Consts_Private.TAGNAME_NAVIGATE.equals(tagbeanTmp.getTagname()))
                    {//如果当前是<wx:navigate/>标签，则必须要指定其type属性
                        if(tagbeanTmp.getMTagAttributes()==null||tagbeanTmp.getMTagAttributes().get("type")==null
                                ||tagbeanTmp.getMTagAttributes().get("type").trim().equals(""))
                        {
                            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的翻页导航栏所使用的静态模板内容包括<wx:navigate/>标签，但没有为它指定type属性，会造成死循环");
                        }
                    }
                }
            }
        }
        return true;
    }
    
    public static List<Integer> parsePagesize(ReportBean rbean,String pagesize)
    {
        List<String> lstTemp=Tools.parseStringToList(pagesize,"|");
        List<Integer> lstPageSize=new UniqueArrayList<Integer>();
        try
        {
            for(String strSizeTmp:lstTemp)
            {
                if(strSizeTmp==null||strSizeTmp.trim().equals("")) continue;
                int isize=Integer.parseInt(strSizeTmp.trim());
                if(isize==0&&(rbean==null||rbean.isListReportType()))
                {
                    isize=10;
                }else if(isize<-1)
                {
                    isize=-1;
                }
                lstPageSize.add(isize);
            }
        }catch(NumberFormatException e1)
        {
            if(rbean==null)
            {
                throw new WabacusConfigLoadingException("配置的default-pagesize："+pagesize+"包含非法数字",e1);
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的pagesize："+pagesize+"包含非法数字",e1);
            }
        }
        if(lstPageSize.contains(0)&&lstPageSize.size()>1&&rbean!=null&&rbean.isDetailReportType())
        {
            if(lstPageSize.contains(1))
            {
                lstPageSize.remove(new Integer(0));
            }else
            {
                int idx=lstPageSize.indexOf(new Integer(0));
                lstPageSize.remove(new Integer(0));
                lstPageSize.add(idx,1);
            }
        }
        return lstPageSize;
    }
    
    public static List<String> getListImportPackages(List<XmlElementBean> lstEleFormatBeans)
    {
        if(lstEleFormatBeans==null||lstEleFormatBeans.size()==0) return null;
        List<String> lstResults=new UniqueArrayList<String>();
        List<String> lstImportTmp;
        for(XmlElementBean eleFormatBeanTmp:lstEleFormatBeans)
        {
            if(eleFormatBeanTmp==null) continue;
            lstImportTmp=ConfigLoadAssistant.getInstance().loadImportsConfig(eleFormatBeanTmp);
            if(lstImportTmp!=null) lstResults.addAll(lstImportTmp);
        }
        return lstResults;
    }

    private static void loadInterceptorInfo(XmlElementBean eleReportBean,ReportBean rbean) throws ClassNotFoundException,InstantiationException,
            IllegalAccessException
    {
        String interceptor=eleReportBean.attributeValue("interceptor");
        Class c=null;
        if(interceptor!=null)
        {
            interceptor=interceptor.trim();
            if(interceptor.equals(""))
            {//如果明确配置此属性为空，则显示地将前后置动作类设置为NULL。这样如果有继承过来的前后置动作就不存在了
                rbean.setInterceptor(null);
            }else
            {
                if(Tools.isDefineKey("$",interceptor))
                {
                    IInterceptor resource=(IInterceptor)Config.getInstance().getResourceObject(null,rbean.getPageBean(),interceptor,true);
                    rbean.setInterceptor(resource);
                }else
                {
                    c=ResourceUtils.loadClass(interceptor);
                }
            }
        }else
        {
            XmlElementBean eleInterceptorBean=eleReportBean.getChildElementByName("interceptor");
            if(eleInterceptorBean!=null)
            {
                List<String> lstImportPackages=ConfigLoadAssistant.getInstance().loadImportsConfig(eleInterceptorBean);
                XmlElementBean elePreAction=eleInterceptorBean.getChildElementByName("preaction");
                String preaction=null;
                if(elePreAction!=null)
                {
                    preaction=elePreAction.getContent();
                }
                preaction=preaction==null?"":preaction.trim();
                XmlElementBean elePostAction=eleInterceptorBean.getChildElementByName("postaction");
                String postaction=null;
                if(elePostAction!=null)
                {
                    postaction=elePostAction.getContent();
                }
                postaction=postaction==null?"":postaction.trim();

                XmlElementBean eleBeforeSave=eleInterceptorBean.getChildElementByName("beforesave");
                String beforesave=null;
                if(eleBeforeSave!=null)
                {
                    beforesave=eleBeforeSave.getContent();
                }
                beforesave=beforesave==null?"":beforesave.trim();
                
                XmlElementBean eleBeforeSavePerRow=eleInterceptorBean.getChildElementByName("beforesave-perrow");
                String beforesavePerrow=null;
                if(eleBeforeSavePerRow!=null)
                {
                    beforesavePerrow=eleBeforeSavePerRow.getContent();
                }
                beforesavePerrow=beforesavePerrow==null?"":beforesavePerrow.trim();
                
                XmlElementBean eleBeforeSavePerSql=eleInterceptorBean.getChildElementByName("beforesave-persql");
                String beforesavePersql=null;
                if(eleBeforeSavePerSql!=null)
                {
                    beforesavePersql=eleBeforeSavePerSql.getContent();
                }
                beforesavePersql=beforesavePersql==null?"":beforesavePersql.trim();
                
                XmlElementBean eleAfterSavePerSql=eleInterceptorBean.getChildElementByName("aftersave-persql");
                String aftersavePersql=null;
                if(eleAfterSavePerSql!=null)
                {
                    aftersavePersql=eleAfterSavePerSql.getContent();
                }
                aftersavePersql=aftersavePersql==null?"":aftersavePersql.trim();
                
                XmlElementBean eleAfterSavePerRow=eleInterceptorBean.getChildElementByName("aftersave-perrow");
                String aftersavePerrow=null;
                if(eleAfterSavePerRow!=null)
                {
                    aftersavePerrow=eleAfterSavePerRow.getContent();
                }
                aftersavePerrow=aftersavePerrow==null?"":aftersavePerrow.trim();
                
                XmlElementBean eleAfterSave=eleInterceptorBean.getChildElementByName("aftersave");
                String aftersave=null;
                if(eleAfterSave!=null)
                {
                    aftersave=eleAfterSave.getContent();
                }
                aftersave=aftersave==null?"":aftersave.trim();

                XmlElementBean eleBeforeLoadData=eleInterceptorBean.getChildElementByName("beforeloaddata");
                String beforeloaddata=null;
                if(eleBeforeLoadData!=null)
                {
                    beforeloaddata=eleBeforeLoadData.getContent();
                }
                beforeloaddata=beforeloaddata==null?"":beforeloaddata.trim();
                
                XmlElementBean eleAfterLoadData=eleInterceptorBean.getChildElementByName("afterloaddata");
                String afterloaddata=null;
                if(eleAfterLoadData!=null)
                {
                    afterloaddata=eleAfterLoadData.getContent();
                }
                afterloaddata=afterloaddata==null?"":afterloaddata.trim();

                XmlElementBean eleDisplayPerRow=eleInterceptorBean.getChildElementByName("beforedisplay-perrow");
                String displayperrow=null;
                if(eleDisplayPerRow!=null)
                {
                    displayperrow=eleDisplayPerRow.getContent();
                }
                displayperrow=displayperrow==null?"":displayperrow.trim();

                XmlElementBean eleDisplayPerCol=eleInterceptorBean.getChildElementByName("beforedisplay-percol");
                String displaypercol=null;
                if(eleDisplayPerCol!=null)
                {
                    displaypercol=eleDisplayPerCol.getContent();
                }
                displaypercol=displaypercol==null?"":displaypercol.trim();

                if(preaction.equals("")&&postaction.equals("")&&beforesave.equals("")&&beforesavePerrow.equals("")&&beforesavePersql.equals("")
                        &&aftersavePersql.equals("")&&aftersavePerrow.equals("")&&aftersave.equals("")&&beforeloaddata.equals("")
                        &&displayperrow.equals("")&&displaypercol.equals(""))
                {
                    rbean.setInterceptor(null);
                }else
                {
                    c=ReportAssistant.getInstance().buildInterceptorClass(rbean.getPageBean().getId()+rbean.getId(),lstImportPackages,preaction,
                            postaction,beforesave,beforesavePerrow,beforesavePersql,aftersavePersql,aftersavePerrow,aftersave,beforeloaddata,
                            afterloaddata,displayperrow,displaypercol);
                }
            }
        }
        if(c!=null)
        {
            rbean.setInterceptor((IInterceptor)c.newInstance());
        }
    }

    private static void loadButtonsInfo(IComponentConfigBean ccbean,XmlElementBean eleButtonsBean)
    {
        if(eleButtonsBean==null) return;
        List<XmlElementBean> lstEleButtonsBeans=new ArrayList<XmlElementBean>();
        lstEleButtonsBeans.add(eleButtonsBean);
        lstEleButtonsBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleButtonsBean.attributeValue("ref"),"buttons",null,ccbean));
        ButtonsBean buttonsBean=new ButtonsBean(ccbean);
        ccbean.setButtonsBean(buttonsBean);
        Map<String,String> mButtonsProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleButtonsBeans,new String[] { "buttonspacing","align" });//组装所有<buttons/>配置的这些属性
        String buttonspacing=mButtonsProperties.get("buttonspacing");
        if(buttonspacing!=null&&!buttonspacing.trim().equals(""))
        {
            try
            {
                buttonsBean.setButtonspacing(Integer.parseInt(buttonspacing.trim()));
            }catch(NumberFormatException nfe)
            {
                log.warn("为组件"+ccbean.getPath()+"的<buttons/>配置的buttonspacing不是合法数字",nfe);
            }
        }
        String align=mButtonsProperties.get("align");
        if(align!=null)
        {//<buttons/>中的align属性只对容器显示在top和bottom的按钮和对报表显示在title上的按钮有效
            buttonsBean.setAlign(align.toLowerCase().trim());
        }
        List<XmlElementBean> lstEleButtons=new ArrayList<XmlElementBean>();
        getEleButtonBeans(lstEleButtonsBeans,lstEleButtons,null,ccbean);
        if(lstEleButtons!=null&&lstEleButtons.size()>0)
        {
            AbsButtonType buttonObj=null;
            for(XmlElementBean eleButtonBeanTmp:lstEleButtons)
            {
                if(eleButtonBeanTmp==null) continue;
                buttonObj=loadButtonConfig(ccbean,eleButtonBeanTmp);
                addButtonToPositions(ccbean,buttonObj);
            }
        }
    }

    private static void loadHeaderFooterConfig(IComponentConfigBean ccbean,XmlElementBean eleComponentBean,String headerfooter)
    {
        XmlElementBean eleHeaderFooter=eleComponentBean.getChildElementByName(headerfooter);
        if(eleHeaderFooter==null) return;
        String content=eleHeaderFooter.getContent().trim();
        TemplateBean tplBean=null;
        if(!content.equals(""))
        {
            if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(content))
            {
                if(Tools.isDefineKey("$",content))
                {
                    Object obj=Config.getInstance().getResourceObject(null,ccbean.getPageBean(),content,true);
                    if(obj==null) obj="";
                    if(obj instanceof TemplateBean)
                    {
                        tplBean=(TemplateBean)obj;
                    }else
                    {//如果是普通字符串
                        tplBean=new TemplateBean();
                        tplBean.setContent(obj.toString());
                    }
                }else
                {//取html/htm文件中的模板
                    tplBean=TemplateParser.parseTemplateByPath(content);
                }
            }else
            {


                tplBean=TemplateParser.parseTemplateByContent(content.trim());
            }
        }
        if(headerfooter.equals("footer"))
        {
            ccbean.setFooterTplBean(tplBean);
        }else
        {
            ccbean.setHeaderTplBean(tplBean);
        }
    }
    
    private static void getEleButtonBeans(List<XmlElementBean> lstEleButtonsBeans,List<XmlElementBean> lstResults,List<String> lstButtonsName,
            IComponentConfigBean ccbean)
    {
        if(lstEleButtonsBeans==null||lstEleButtonsBeans.size()==0) return;
        List<XmlElementBean> lstEleBeanTmp;
        if(lstButtonsName==null) lstButtonsName=new ArrayList<String>();
        for(XmlElementBean eleButtonsBeanTmp:lstEleButtonsBeans)
        {
            lstEleBeanTmp=eleButtonsBeanTmp.getLstChildElements();
            if(lstEleBeanTmp==null||lstEleBeanTmp.size()==0) continue;
            List<String> lstNameTmp=new ArrayList<String>();//存放当前<buttons/>的所有name属性，用于判断是否存在重复name属性
            String buttonNameTmp;
            for(XmlElementBean eleChildBeanTmp:lstEleBeanTmp)
            {
                if("button".equals(eleChildBeanTmp.getName()))
                {//是<button/>配置
                    buttonNameTmp=eleChildBeanTmp.attributeValue("name");
                    if(lstNameTmp.contains(buttonNameTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"的按钮失败，存在重复name属性的配置");
                    }
                    lstNameTmp.add(buttonNameTmp);
                    if(lstButtonsName.contains(buttonNameTmp))
                    {
                        continue;
                    }
                    lstButtonsName.add(buttonNameTmp);
                    lstResults.add(eleChildBeanTmp);
                }else if("ref".equals(eleChildBeanTmp.getName()))
                {//当前是<ref/>标签
                    getEleButtonBeans(ConfigLoadAssistant.getInstance().getRefElements(eleChildBeanTmp.attributeValue("key"),"buttons",null,ccbean),
                            lstResults,lstButtonsName,ccbean);
                }
            }
        }
    }

    public static void addButtonToPositions(IComponentConfigBean ccbean,AbsButtonType buttonObj)
    {
        ButtonsBean buttonsBean=ccbean.getButtonsBean();
        if(buttonsBean==null)
        {
            buttonsBean=new ButtonsBean(ccbean);
            ccbean.setButtonsBean(buttonsBean);
        }
        String position=buttonObj.getPosition();
        if(position==null||position.trim().equals(""))
        {
            position=Consts.OTHER_PART;
        }
        List<String> lstPosis=Tools.parseStringToList(position,"|");
        for(String positionTemp:lstPosis)
        {
            if(positionTemp==null||positionTemp.trim().equals(""))
            {
                positionTemp=Consts.OTHER_PART;
            }
            buttonsBean.addButton(buttonObj,positionTemp.trim());
        }
    }

    public static AbsButtonType loadButtonConfig(IComponentConfigBean ccbean,XmlElementBean eleButtonBean)
    {
        mergeAllParentsButtonConfig(ccbean,eleButtonBean,null);
        AbsButtonType buttonObj=null;
        String buttonclass=eleButtonBean.attributeValue("class");
        try
        {
            Class c=null;
            if(buttonclass==null||buttonclass.trim().equals(""))
            {
                c=WabacusButton.class;
            }else
            {
                c=ResourceUtils.loadClass(buttonclass);
            }
            Object o=c.getConstructor(new Class[] { IComponentConfigBean.class }).newInstance(new Object[] { ccbean });
            if(!(o instanceof AbsButtonType))
            {
                throw new WabacusConfigLoadingException("配置的按钮插件类"+c.getName()+"没有继承"+AbsButtonType.class.getName()+"类");
            }
            buttonObj=(AbsButtonType)o;
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("配置的按钮类："+buttonclass+"无法加载或实例化对象",e);
        }
        String buttonname=eleButtonBean.attributeValue("name");
        if(buttonname!=null) buttonObj.setName(buttonname.trim());
        String label=eleButtonBean.attributeValue("label");
        String menulabel=eleButtonBean.attributeValue("menulabel");
        String menugroup=eleButtonBean.attributeValue("menugroup");
        String position=eleButtonBean.attributeValue("position");
        String positionorder=eleButtonBean.attributeValue("positionorder");
        String styleproperty=eleButtonBean.attributeValue("styleproperty");
        String disabledstyleproperty=eleButtonBean.attributeValue("disabledstyleproperty");
        if(position!=null) buttonObj.setPosition(position.trim());
        if(positionorder!=null)
        {
            positionorder=positionorder.trim();
            if(!positionorder.equals(""))
            {
                try
                {
                    buttonObj.setPositionorder(Integer.parseInt(positionorder));
                }catch(NumberFormatException e)
                {
                    log.warn("组件"+ccbean.getPath()+"上的按钮"+buttonname+"配置的positionorder属性"+positionorder+"不是有效数字");
                }
            }else
            {
                buttonObj.setPositionorder(0);
            }
        }
        String refer=eleButtonBean.attributeValue("refer");
        if(refer!=null&&!refer.trim().equals(""))
        {
            if(!(ccbean instanceof AbsContainerConfigBean))
            {//如果按钮所在的组件不是容器
                throw new WabacusConfigLoadingException("组件"+ccbean.getPath()+"不是容器，不能将其按钮配置为通过refer属性引用其它按钮");
            }
            buttonObj.setRefer(refer.trim());
            String referedbutton=eleButtonBean.attributeValue("referedbutton");
            if(referedbutton!=null)
            {
                if(referedbutton.toLowerCase().trim().equals("display"))
                {
                    buttonObj.setReferedbutton("display");
                }else
                {
                    buttonObj.setReferedbutton("hidden");
                }
            }
        }else
        {
            if(label!=null)
            {
                buttonObj.setLabel(Config.getInstance().getResourceString(null,ccbean.getPageBean(),label,true).trim());
            }
            if(menulabel!=null)
            {
                buttonObj.setMenulabel(Config.getInstance().getResourceString(null,ccbean.getPageBean(),menulabel.trim(),true));
            }
            if(buttonObj.getMenulabel()==null||buttonObj.getMenulabel().trim().equals(""))
            {
                buttonObj.setMenulabel(buttonObj.getLabel());
            }
            if(menugroup!=null) buttonObj.setMenugroup(menugroup.trim());
            if(styleproperty!=null) buttonObj.setStyleproperty(styleproperty.trim());
            if(disabledstyleproperty!=null) buttonObj.setDisabledstyleproperty(disabledstyleproperty.trim());
            String clickevent=eleButtonBean.getContent();
            if(clickevent!=null&&!clickevent.trim().equals(""))
            {
                if(clickevent.indexOf('\"')>=0)
                {
                    throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"的按钮"+buttonname+"失败，按钮事件中不能用双引号，只能用单引用，如果有多级，可以加上转义字符\\");
                }
                buttonObj.setClickEvent(Tools.formatStringBlank(clickevent.trim()));
            }else
            {
                List<String> lstImports=ConfigLoadAssistant.getInstance().loadImportsConfig(eleButtonBean);
                XmlElementBean eleDynEventBean=eleButtonBean.getChildElementByName("dynevent");
                if(eleDynEventBean!=null)
                {
                    String dynevent=eleDynEventBean.getContent();
                    if(dynevent!=null&&!dynevent.trim().equals(""))
                    {
                        buttonObj.setClickEvent(ReportAssistant.getInstance().createButtonEventGeneratorObject(
                                ccbean.getPageBean().getId()+"_"+ccbean.getId()+buttonname,dynevent,lstImports));
                    }
                }
            }
        }
        buttonObj.loadExtendConfig(eleButtonBean);
        return buttonObj;
    }

    private static void mergeAllParentsButtonConfig(IComponentConfigBean ccbean,XmlElementBean eleButtonBean,List<String> lstExtendedParentKeys)
    {
        String extendsParent=eleButtonBean.attributeValue("extends");
        if(extendsParent==null||extendsParent.trim().equals("")) return;
        if(lstExtendedParentKeys==null) lstExtendedParentKeys=new ArrayList<String>();
        extendsParent=extendsParent.trim();
        if(lstExtendedParentKeys.contains(extendsParent))
        {
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"下配置的按钮失败，被直接或间接继承的父按钮对应的KEY："+extendsParent+"存在循环继承");
        }
        lstExtendedParentKeys.add(extendsParent);
        if(!Tools.isDefineKey("$",extendsParent))
        {// 父按钮对象不是定义在资源文件中，则报错
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"下配置的按钮失败，被直接或间接继承的父按钮对应的KEY："+extendsParent+"不是合法的资源项KEY");
        }
        XmlElementBean parentButtonConfig=(XmlElementBean)Config.getInstance().getResourceObject(null,ccbean.getPageBean(),extendsParent,true);
        if(parentButtonConfig==null)
        {
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"下配置的按钮失败，根据KEY"+extendsParent+"没有从资源文件中找到被直接或间接继承的父按钮对象");
        }
        extendsParent=parentButtonConfig.attributeValue("extends");
        if(extendsParent!=null&&!extendsParent.trim().equals(""))
        {
            mergeAllParentsButtonConfig(ccbean,parentButtonConfig,lstExtendedParentKeys);
        }
        XmlAssistant.getInstance().mergeXmlElementBeans(eleButtonBean,parentButtonConfig);
    }
    
    private static int loadSqlInfo(SqlBean sbean,XmlElementBean eleSqlBean)
    {
        List<XmlElementBean> lstEleSqlBeans=new ArrayList<XmlElementBean>();
        lstEleSqlBeans.add(eleSqlBean);
        lstEleSqlBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleSqlBean.attributeValue("ref"),"sql",null,sbean.getReportBean()));
        
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(sbean,lstEleSqlBeans);

        Map<String,String> mSqlProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleSqlBeans,
                new String[] { "type", "datasource","beforesearch"});//组装所有<sql/>配置的这些属性

        String type=mSqlProperties.get("type");
        if(type!=null)
        {
            sbean.setStatementType(type.trim());
        }else
        {
            sbean.setStatementType(Config.getInstance().getSystemConfigValue("default-sqltype","statement"));
        }
        String datasource=mSqlProperties.get("datasource");
        if(datasource==null||datasource.trim().equals(""))
        {
            datasource=Consts.DEFAULT_KEY;
        }
        if(datasource==null||datasource.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("没有在报表"+sbean.getReportBean().getPath()+"的<sql/>标签中配置数据源");
        }
        sbean.setDatasource(datasource.trim());
        String beforesearch=mSqlProperties.get("beforesearch");
        if(beforesearch!=null)
        {
            sbean.setBeforeSearchMethod(beforesearch.trim());
        }

//        if(pagesplit_side!=null)




//            sbean.setPagesplit_side(Config.getInstance().getSystemConfigValue("pagesplit_side","app").trim());

        XmlElementBean eleValueBean=getEleSqlValueBean(lstEleSqlBeans);//从所有<sql/>中得到第一个<value/>对象
        List<XmlElementBean> lstCondition=getLstEleSqlConditionBeans(lstEleSqlBeans);//从所有<sql/>中得到name属性不重复的<condition/>对象集合
        if(eleValueBean!=null)
        {
            String sqlValue=eleValueBean.getContent();
            sqlValue=Tools.formatStringBlank(sqlValue);
            if(sqlValue!=null)
            {
                sqlValue=sqlValue.trim();
                while(sqlValue.endsWith(";")) sqlValue=sqlValue.substring(0,sqlValue.length()-1).trim();
                if(sqlValue.startsWith("{")&&sqlValue.endsWith("}")) sqlValue=sqlValue.substring(1,sqlValue.length()-1).trim();
            }
            sbean.setValue(sqlValue);
        }
        if(lstCondition!=null&&lstCondition.size()>0)
        {
            loadReportConditionConfig(lstCondition,sbean);
        }
        if(sbean.isStoreProcedure())
        {
            String proc=sbean.getValue().trim();
            int idxLeft=proc.indexOf("(");
            int idxRight=proc.lastIndexOf(")");
            if(idxLeft>0&&idxRight==proc.length()-1)
            {
                StringBuffer spBuf=new StringBuffer(proc.substring(0,idxLeft+1));
                String procParams=proc.substring(idxLeft+1,idxRight);//取到存储过程中的参数
                if(!procParams.trim().equals(""))
                {
                    List<String> lstParams=Tools.parseStringToList(procParams,',','\'');
                    List<String> lstSPParams=new ArrayList<String>();
                    for(String paramTmp:lstParams)
                    {
                        if(paramTmp.trim().equals("")||Tools.isDefineKey("request",paramTmp)||Tools.isDefineKey("session",paramTmp))
                        {
                            lstSPParams.add(paramTmp);
                        }else if(paramTmp.startsWith("'")&&paramTmp.endsWith("'"))
                        {
                            lstSPParams.add(paramTmp.substring(1,paramTmp.length()-1));
                        }else
                        {
                            if(sbean.getConditionBeanByName(paramTmp)==null)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+sbean.getReportBean().getPath()+"失败，存储过程引用的name为"+paramTmp+"的动态条件不存在");
                            }
                            lstSPParams.add("condition{"+paramTmp+"}");
                        }
                        spBuf.append("?,");
                    }
                    sbean.setLstStoreProcedureParams(lstSPParams);
                }
                proc=spBuf.toString();
            }else
            {//没有配置存储过程的参数
                proc=proc+"(";
            }
            proc=proc+"?,?";
            if(Config.getInstance().getDataSource(datasource).getDbType() instanceof Oracle)
            {
                proc=proc+",?";
            }
            proc="{"+proc+")}";
            sbean.setValue(proc);
        }
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(sbean,lstEleSqlBeans);
        
        return 1;
    }

    private static XmlElementBean getEleSqlValueBean(List<XmlElementBean> lstEleSqlBeans)
    {
        if(lstEleSqlBeans==null||lstEleSqlBeans.size()==0) return null;
        XmlElementBean eleSelectBeanTmp;
        XmlElementBean eleSqlValueBeanTmp;
        for(XmlElementBean eleSqlBeanTmp:lstEleSqlBeans)
        {
            eleSelectBeanTmp=eleSqlBeanTmp.getChildElementByName("select");
            if(eleSelectBeanTmp!=null)
            {
                eleSqlValueBeanTmp=eleSelectBeanTmp.getChildElementByName("value");
                if(eleSqlValueBeanTmp!=null) return eleSqlValueBeanTmp;//在<select/>中配置了<value/>，则返回
            }else
            {
                eleSqlValueBeanTmp=eleSqlBeanTmp.getChildElementByName("value");
                if(eleSqlValueBeanTmp!=null) return eleSqlValueBeanTmp;
            }
        }
        return null;
    }

    private static List<XmlElementBean> getLstEleSqlConditionBeans(List<XmlElementBean> lstEleSqlBeans)
    {
        List<XmlElementBean> lstResults=new ArrayList<XmlElementBean>();
        if(lstEleSqlBeans==null||lstEleSqlBeans.size()==0) return lstResults;
        XmlElementBean eleSelectBeanTmp;
        List<XmlElementBean> lstConTemps;
        List<String> lstConNames=new ArrayList<String>();//存放已经处理过的<condition/>的name属性，以便收集所有name属性不同的<condition/>对象
        for(XmlElementBean eleSqlBeanTmp:lstEleSqlBeans)
        {
            eleSelectBeanTmp=eleSqlBeanTmp.getChildElementByName("select");
            lstConTemps=null;
            if(eleSelectBeanTmp!=null)
            {
                lstConTemps=eleSelectBeanTmp.getLstChildElementsByName("condition");

            }else
            {
                lstConTemps=eleSqlBeanTmp.getLstChildElementsByName("condition");
            }
            if(lstConTemps!=null&&lstConTemps.size()>0)
            {
                for(XmlElementBean eleConBeanTmp:lstConTemps)
                {
                    if(eleConBeanTmp==null) continue;
                    if(lstConNames.contains(eleConBeanTmp.attributeValue("name"))) continue;
                    lstResults.add(eleConBeanTmp);
                    lstConNames.add(eleConBeanTmp.attributeValue("name"));
                }
            }
        }
        return lstResults;
    }

    private static void loadReportConditionConfig(List<XmlElementBean> lstConditionElements,SqlBean sbean)
    {
        List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();
        sbean.setLstConditions(lstConditions);

        List<String> lstConNamesTmp=new ArrayList<String>();
        for(XmlElementBean eleConditionBeanTmp:lstConditionElements)
        {
            if(eleConditionBeanTmp==null) continue;
            ConditionBean cb=new ConditionBean(sbean);
            String name=eleConditionBeanTmp.attributeValue("name");
            if(name==null||name.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+sbean.getReportBean().getPath()+"配置的查询条件没有配置name属性");
            }
            name=name.trim();
            if(lstConNamesTmp.contains(name))
            {
                throw new WabacusConfigLoadingException("报表"+sbean.getReportBean().getPath()+"存在多个name为"+name+"的查询条件");
            }
            lstConNamesTmp.add(name);
            lstConditions.add(cb);
            cb.setName(name.trim()); 
            List<XmlElementBean> lstEleConditionBeans=new ArrayList<XmlElementBean>();
            lstEleConditionBeans.add(eleConditionBeanTmp);
            //            LoadExtendConfigManager.loadBeforeExtendConfigForPagetype(cb,lstEleConditionBeans);
            LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(cb,lstEleConditionBeans);
            String label=eleConditionBeanTmp.attributeValue("label");
            if(label!=null)
            {
                cb.setLabel(Config.getInstance().getResourceString(null,sbean.getPageBean(),label,true));
            }
            String labelstyle=eleConditionBeanTmp.attributeValue("labelstyle");
            
            String hidden=eleConditionBeanTmp.attributeValue("hidden");
            String constant=eleConditionBeanTmp.attributeValue("constant");
            String br=eleConditionBeanTmp.attributeValue("br");
            String splitlike=eleConditionBeanTmp.attributeValue("splitlike");
            String defaultvalue=eleConditionBeanTmp.attributeValue("defaultvalue");
            String keepkeywords=eleConditionBeanTmp.attributeValue("keepkeywords");
            String source=eleConditionBeanTmp.attributeValue("source");
            String left=eleConditionBeanTmp.attributeValue("left");
            String right=eleConditionBeanTmp.attributeValue("right");
            if(defaultvalue!=null)
            {
                cb.setDefaultvalue(defaultvalue.trim());
            }
            if(splitlike!=null)
            {
                cb.setSplitlike(splitlike.trim());
            }
            IDataType typeObj=ConfigLoadAssistant.loadDataType(eleConditionBeanTmp);
            if((typeObj instanceof BlobType)||(typeObj instanceof ClobType))
            {
                throw new WabacusConfigLoadingException("报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"配置不合法，不允许指定其type为clob或blob");
            }
            if(((SqlBean)cb.getParent()).isStatementSql())
            {
                String datatype=eleConditionBeanTmp.attributeValue("datatype");
                if(datatype!=null&&!datatype.trim().equals("")) log.info("报表"+cb.getReportBean().getPath()+"采用Statement方式执行报表，可以不用配置其查询条件的type属性");
            }
            cb.setDatatypeObj(typeObj);
            if(labelstyle!=null&&!labelstyle.trim().equals(""))
            {
                try
                {
                    cb.setLabelstyle(Integer.parseInt(labelstyle.trim()));
                }catch(NumberFormatException e)
                {
                    throw new WabacusConfigLoadingException("报表"+cb.getReportBean().getPath()+"配置的查询条件"+cb.getName()+"中labelstyle属性不是合法的数字");
                }
            }else
            {
                cb.setLabelstyle(Config.getInstance().getSystemConfigValue("default-labelstyle",2));
            }
            if(keepkeywords!=null&&keepkeywords.trim().equalsIgnoreCase("true"))
            {
                cb.setKeepkeywords(true);
            }
            if(hidden!=null)
            {
                hidden=hidden.toLowerCase().trim();
                if(hidden.equals(""))
                {
                    cb.setHidden(false);
                }else if(!hidden.equals("true")&&!hidden.equals("false"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"失败，其配置的hidden属性："+hidden
                            +"值不合法，只能是true或false");
                }else
                {
                    cb.setHidden(Boolean.parseBoolean(hidden));
                }
            }
            if(constant!=null)
            {
                constant=constant.toLowerCase().trim();
                if(constant.equals(""))
                {
                    cb.setConstant(false);
                }else if(!constant.equals("true")&&!constant.equals("false"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"失败，其配置的constant属性："+constant
                            +"值不合法，只能是true或false");
                }else
                {
                    cb.setConstant(Boolean.parseBoolean(constant));
                }
            }
            if(cb.isConstant())
            {

                XmlElementBean eleValueBeanTmp=eleConditionBeanTmp.getChildElementByName("value");
                if(eleValueBeanTmp==null) throw new WabacusConfigLoadingException("报表"+cb.getReportBean().getPath()+"的查询条件"+cb.getName()+"是常量查询条件，必须配置<value/>并在其中指定常量值");
                String conValueTmp=eleValueBeanTmp.getContent();
                ConditionExpressionBean cebean=new ConditionExpressionBean();
                cebean.setValue(conValueTmp==null?"":conValueTmp.trim());
                cb.setConditionExpression(cebean);
                cb.setHidden(true);
                continue;
            }
            if(source!=null) cb.setSource(source);
            if(br!=null&&br.trim().equalsIgnoreCase("true"))
            {
                cb.setBr(true);
            }
            if(left!=null)
            {
                left=left.trim();
                if(left.equals("")) left="0";
                cb.setLeft(Integer.parseInt(left));
                if(cb.getLeft()<0) cb.setLeft(0-cb.getLeft());
            }
            if(right!=null)
            {
                right=right.trim();
                if(right.equals("")) right="0";
                cb.setRight(Integer.parseInt(right));
                if(cb.getRight()<0) cb.setRight(0-cb.getRight());
            }
            if(cb.isConditionWithInputbox())
            {//需要显示输入框
                String iterator=eleConditionBeanTmp.attributeValue("iterator");
                if(iterator!=null)
                {
                    iterator=iterator.trim();
                    if(iterator.equals("")) iterator="0";
                    cb.setIterator(Integer.parseInt(iterator));
                }
                String innerlogic=eleConditionBeanTmp.attributeValue("innerlogic");
                if(innerlogic!=null)
                {
                    cb.setInnerlogic(innerlogic.trim());
                }
                List<String> lstChildOrders=new ArrayList<String>();//存放各子元素的配置顺序，以便决定它们的显示顺序（主要是<inputbox/>、<columns/>、<values/>三个子标签的顺序）
                cb.setLstChildDisplayOrder(lstChildOrders);
                List<XmlElementBean> lstChildrenElements=eleConditionBeanTmp.getLstChildElements();
                if(lstChildrenElements!=null&&lstChildrenElements.size()>0)
                {
                    for(XmlElementBean xebeanTmp:lstChildrenElements)
                    {
                        if(xebeanTmp.getName().equals("innerlogic"))
                        {
                            if(lstChildOrders.contains("innerlogic"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<innerlogic/>子标签");
                            }
                            if(cb.getInnerlogic()!=null&&!cb.getInnerlogic().trim().equals(""))
                            {//已经在<condition/>中配置了innerlogic属性
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能在<condition/>中即配置innerlogic属性，又配置<innerlogic/>子标签");
                            }
                            lstChildOrders.add("innerlogic");
                            ConditionSelectorBean cilbean=new ConditionSelectorBean(cb,ConditionBean.SELECTTYPE_INNERLOGIC);
                            cilbean.loadConfig(xebeanTmp,"logic");
                            if(cilbean.isEmpty()) continue;
                            cb.setCinnerlogicbean(cilbean);
                        }else if(xebeanTmp.getName().equals("inputbox"))
                        {
                            if(lstChildOrders.contains("inputbox"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<inputbox/>子标签");
                            }
                            lstChildOrders.add("inputbox");
                            String inputbox=xebeanTmp.attributeValue("type");
                            AbsInputBox box=Config.getInstance().getInputBoxTypeByName(inputbox);
                            box=(AbsInputBox)box.clone(cb);
                            box.loadInputBoxConfig(cb,xebeanTmp);
                            cb.setInputbox(box);
                        }else if(xebeanTmp.getName().equals("columns"))
                        {
                            if(lstChildOrders.contains("columns"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<columns/>子标签");
                            }
                            lstChildOrders.add("columns");
                            ConditionSelectorBean csbean=new ConditionSelectorBean(cb,ConditionBean.SELECTORTYPE_COLUMNS);
                            csbean.loadConfig(xebeanTmp,"column");
                            if(csbean.isEmpty()) continue;
                            cb.setCcolumnsbean(csbean);
                        }else if(xebeanTmp.getName().equals("values"))
                        {
                            if(lstChildOrders.contains("values"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<values/>子标签");
                            }
                            if(cb.getConditionExpression()!=null)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它同时配置<values/>和<value/>子标签");
                            }
                            lstChildOrders.add("values");
                            ConditionSelectorBean csbean=new ConditionSelectorBean(cb,ConditionBean.SELECTORTYPE_VALUES);
                            csbean.loadConfig(xebeanTmp,"value");
                            if(csbean.isEmpty()) continue;
                            cb.setCvaluesbean(csbean);
                        }else if(xebeanTmp.getName().equals("value"))
                        {
                            if(sbean.isStoreProcedure())
                            {
                                log.warn("报表"+cb.getReportBean().getPath()+"采用存储过程加载数据，可以不为其动态查询条件配置<value/>子标签");
                                continue;
                            }
                            if(cb.getConditionExpression()!=null)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它配置多个<value/>子标签");
                            }
                            if(lstChildOrders.contains("values"))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的name属性为"+cb.getName()
                                        +"的查询条件失败，不能为它同时配置<values/>和<value/>子标签");
                            }
                            Object valueObj=loadConditionValueConfig(cb,xebeanTmp);
                            if(valueObj instanceof ConditionExpressionBean)
                            {
                                cb.setConditionExpression((ConditionExpressionBean)valueObj);
                            }else
                            {//在<value/>中配置了多个<column/>，可能是要提供多个列的选择搜索
                                ConditionSelectorBean cvaluesbean=new ConditionSelectorBean(cb,ConditionBean.SELECTORTYPE_VALUES);
                                cb.setCvaluesbean(cvaluesbean);
                                List<ConditionSelectItemBean> lstValuesBean=new ArrayList<ConditionSelectItemBean>();
                                cvaluesbean.setLstSelectItemBeans(lstValuesBean);
                                ConditionValueSelectItemBean cvbean=new ConditionValueSelectItemBean(cb);
                                cvbean.setLstColumnsBean((List<ConditionSelectItemBean>)valueObj);
                                lstValuesBean.add(cvbean);
                                lstChildOrders.add("values");
                            }
                        }
                    }
                }
                if(!lstChildOrders.contains("inputbox"))
                {//没有配置<inputbox/>，则取默认
                    lstChildOrders.add("inputbox");//如果没有配置<inputbox/>，则将它放在最后显示
                    AbsInputBox box=Config.getInstance().getInputBoxTypeByName(null);
                    box=(AbsInputBox)box.clone(cb);
                    box.loadInputBoxConfig(cb,null);
                    cb.setInputbox(box);
                }
            }else if(!sbean.isStoreProcedure())
            {//隐藏查询条件，且不是用存储过程查询数据，加载它们的<value/>子标签（因为存储过程的查询条件是不用配置<value/>子标签的）
                XmlElementBean eleValueBean=eleConditionBeanTmp.getChildElementByName("value");
                if(eleValueBean==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的<condition/>失败，此查询条件为隐藏查询条件，必须配置<value/>子标签");
                }
                String expression=eleValueBean.getContent();
                if(expression==null||expression.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cb.getReportBean().getPath()+"的<condition/>失败，此查询条件不需显示输入框，必须配置<value/>子标签内容");
                }
                ConditionExpressionBean cexpressionBean=new ConditionExpressionBean();
                cexpressionBean.setValue(expression.trim());
                cb.setConditionExpression(cexpressionBean);
            }
            LoadExtendConfigManager.loadAfterExtendConfigForReporttype(cb,lstEleConditionBeans);
            
        }
    }

    public static Object loadConditionValueConfig(ConditionBean cbean,XmlElementBean eleValuebean)
    {
        List<XmlElementBean> lstEleColumnsElement=eleValuebean.getLstChildElementsByName("column");
        String conditionexpression=eleValuebean.getContent();
        if(conditionexpression!=null&&!conditionexpression.trim().equals("")&&lstEleColumnsElement!=null&&lstEleColumnsElement.size()>0)
        {
            throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                    +"的<condition/>下<values/>的子标签<value/>失败，不能同时为它配置标签内容和<column/>子标签");
        }
        if(conditionexpression!=null&&!conditionexpression.trim().equals(""))
        {
            ConditionExpressionBean cebean=new ConditionExpressionBean();
            cebean.setValue(conditionexpression.trim());
            return cebean;
        }else
        {//在<value/>下配置了多个<column/>
            List<ConditionSelectItemBean> lstColumnBeans=new ArrayList<ConditionSelectItemBean>();
            List<String> lstColIdsTmp=new ArrayList<String>();
            for(XmlElementBean xebeanTmp:lstEleColumnsElement)
            {
                String refid=xebeanTmp.attributeValue("refid");
                String expression=xebeanTmp.getContent();
                if(refid==null||refid.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                            +"的<condition/>下<values/>的子标签<value/>失败，其下的<column/>子标签配置的refid："+refid+"不能为空");
                }
                refid=refid.trim();
                if(lstColIdsTmp.contains(refid))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                            +"的<condition/>下<values/>的子标签<value/>失败，其下的<column/>子标签配置的refid："+refid+"存在重复");
                }
                lstColIdsTmp.add(refid);
                ConditionSelectItemBean ccbean=new ConditionSelectItemBean(cbean);
                ccbean.setId(refid);
                if(expression==null||expression.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                            +"的<condition/>下<values/>的子标签<value/>失败，其下refid为"+refid+"的<column/>子标签没有配置条件表达式");
                }
                ConditionExpressionBean cexpressionBean=new ConditionExpressionBean();
                cexpressionBean.setValue(expression.trim());
                ccbean.setConditionExpression(cexpressionBean);
                lstColumnBeans.add(ccbean);
            }
            return lstColumnBeans;
        }
    }
    
    public static List<OptionBean> loadOptionInfo(List<XmlElementBean> lstOptionBeans,IInputBoxOwnerBean ownerbean)
    {
        List<OptionBean> lstOptions=new ArrayList<OptionBean>();
        String labeltemp;
        String valuetemp;
        for(XmlElementBean eleOptionBeanTmp:lstOptionBeans)
        {
            if(eleOptionBeanTmp==null) continue;
            OptionBean ob=new OptionBean();
            String source=eleOptionBeanTmp.attributeValue("source");
            labeltemp=eleOptionBeanTmp.attributeValue("label");
            valuetemp=eleOptionBeanTmp.attributeValue("value");
            labeltemp=labeltemp==null?"":labeltemp.trim();
            valuetemp=valuetemp==null?"":valuetemp.trim();
            ob.setLabel(Config.getInstance().getResourceString(null,ownerbean.getReportBean().getPageBean(),labeltemp,true));
            ob.setValue(valuetemp);
            if(source==null||source.trim().equals(""))
            {
                String type=eleOptionBeanTmp.attributeValue("type");
                if(type!=null)
                {
                    type=type.trim();
                    String[] typearray=null;
                    if(type.equalsIgnoreCase("true"))
                    {//当前选项只有在当前下拉框有数据时才显示出来，如果没选项数据，则不显示出来
                        typearray=new String[1];
                        typearray[0]="%true-true%";
                    }else if(type.equalsIgnoreCase("false"))
                    {
                        typearray=new String[1];
                        typearray[0]="%false-false%";
                    }else if(!type.equals(""))
                    {
                        if(!type.startsWith("[")&&!type.endsWith("]"))
                        {
                            throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"配置的下拉框的type属性值没有用[]括住");
                        }
                        typearray=Tools.parseStringToArray(type,'[',']');
                        if(typearray==null||typearray.length==0)
                        {
                            throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"配置的下拉框的下拉选项的type属性不合法");
                        }
                    }
                    ob.setType(typearray);
                }
                lstOptions.add(ob);
            }else
            {
                source=source.trim();
                if(Tools.isDefineKey("$",source))
                {
                    List<OptionBean> lstOptions2=(List<OptionBean>)Config.getInstance().getResourceObject(null,ownerbean.getReportBean().getPageBean(),source,true);
                    if(lstOptions2!=null&&lstOptions2.size()>0)
                    {
                        OptionBean obTemp;
                        for(int j=0;j<lstOptions2.size();j++)
                        {
                            obTemp=(OptionBean)((OptionBean)lstOptions2.get(j));
                            lstOptions.add(obTemp);
                        }
                    }

                }else if(Tools.isDefineKey("@",source))
                {
                    source=Tools.getRealKeyByDefine("@",source);
                    ob.setSourceType(1);
                    ob.setSql(source);
                    ob.setLstConditions(loadConditionsInOtherPlace(eleOptionBeanTmp,ownerbean.getReportBean()));
                    lstOptions.add(ob);
                }else
                {
                    throw new WabacusConfigLoadingException("报表"+ownerbean.getReportBean().getPath()+"配置的下拉选项的source："
                            +eleOptionBeanTmp.attributeValue("source")+"不合法");
                }
            }
        }
        return lstOptions;
    }

    public static List<ConditionBean> loadConditionsInOtherPlace(XmlElementBean eleParentBean,ReportBean rbean)
    {
        List<XmlElementBean> lstConditionEles=eleParentBean.getLstChildElementsByName("condition");
        if(lstConditionEles==null||lstConditionEles.size()==0) return null;
        List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();
        List<String> lstConNames=new ArrayList<String>();
        for(XmlElementBean eleConBeanTmp:lstConditionEles)
        {
            if(eleConBeanTmp==null) continue;
            ConditionBean cbTmp=loadHiddenConditionConfig(eleConBeanTmp,rbean);
            if(lstConNames.contains(cbTmp.getName()))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的查询条件失败，<condition/>的name属性不能重复");
            }
            lstConNames.add(cbTmp.getName());
            if(cbTmp.getConditionExpression()==null||cbTmp.getConditionExpression().getValue()==null
                    ||cbTmp.getConditionExpression().getValue().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，必须为<value/>配置条件表达式");
            }
            lstConditions.add(cbTmp);
        }
        return lstConditions;
    }

    public static ConditionBean loadHiddenConditionConfig(XmlElementBean eleConditionBean,ReportBean rbean)
    {
        if(eleConditionBean==null) return null;
        ConditionBean conbean=new ConditionBean(null);
        String name=eleConditionBean.attributeValue("name");
        String source=eleConditionBean.attributeValue("source");
        String keepkeywords=eleConditionBean.attributeValue("keepkeywords");
        String constant=eleConditionBean.attributeValue("constant");
        name=name==null?"":name.trim();
        if(name.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的<condition/>失败，name属性不能为空");
        }
        conbean.setName(name);
        IDataType typeObj=ConfigLoadAssistant.loadDataType(eleConditionBean);
        if((typeObj instanceof BlobType)||(typeObj instanceof ClobType))
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的name为"+name+"的<condition/>配置不合法，不允许指定其type为clob或blob");
        }
        conbean.setDatatypeObj(typeObj);
        if(source!=null)
        {
            conbean.setSource(source.trim());
        }
        conbean.setHidden(true);
        if(constant!=null)
        {
            conbean.setConstant(constant.trim().equalsIgnoreCase("true"));
        }
        if(keepkeywords!=null)
        {
            conbean.setKeepkeywords(keepkeywords.trim().equalsIgnoreCase("true"));
        }
        XmlElementBean eleConValue=eleConditionBean.getChildElementByName("value");
        if(eleConValue!=null)
        {
            String value=eleConValue.getContent();
            if(value!=null&&!value.trim().equals(""))
            {
                ConditionExpressionBean cebeanTmp=new ConditionExpressionBean();
                cebeanTmp.setValue(value.trim());
                conbean.setConditionExpression(cebeanTmp);
            }
        }
        return conbean;
    }
    
    private static int loadDisplayInfo(DisplayBean dbean,XmlElementBean eleDisplayBean)
    {
        List<XmlElementBean> lstEleDisplayBeans=new ArrayList<XmlElementBean>();
        lstEleDisplayBeans.add(eleDisplayBean);
        lstEleDisplayBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleDisplayBean.attributeValue("ref"),"display",null,
                dbean.getReportBean()));
        //        LoadExtendConfigManager.loadBeforeExtendConfigForPagetype(dbean,lstEleDisplayBeans);
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(dbean,lstEleDisplayBeans);
        Map<String,String> mDisplayProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] {"dataheader","colselect","colselectwidth" });
        String dataheader=mDisplayProperties.get("dataheader");
        String colselect=mDisplayProperties.get("colselect");
        String colselectwidth=mDisplayProperties.get("colselectwidth");
        if(dataheader!=null)
        {
            dbean.setDataheader(Config.getInstance().getResourceString(null,dbean.getPageBean(),dataheader,true));
        }
        if(colselect!=null)
        {
            colselect=colselect.toLowerCase().trim();
            if(colselect.equals(""))
            {
                dbean.setColselect(null);
            }else if(colselect.equals("true"))
            {
                dbean.setColselect(true);
            }else if(colselect.equals("false"))
            {
                dbean.setColselect(false);
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"的<display/>标签失败，配置的colselect属性："+colselect+"无效");
            }
        }
        if(colselectwidth!=null) dbean.setColselectwidth(colselectwidth.trim());
        loadColInfo(lstEleDisplayBeans,dbean);
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(dbean,lstEleDisplayBeans);
        
        return 1;
    }

    private static int loadColInfo(List<XmlElementBean> lstEleDisplayBeans,DisplayBean dbean)
    {
        List<XmlElementBean> lstAllEleCols=new ArrayList<XmlElementBean>();
        getAllEleCols(lstEleDisplayBeans,lstAllEleCols,dbean.getReportBean());//获取到所有要显示的<col/>配置
        List<ColBean> lstColBeans=new ArrayList<ColBean>();
        for(XmlElementBean eleColBeanTmp:lstAllEleCols)
        {
            if(eleColBeanTmp!=null) lstColBeans.add(loadColConfig(eleColBeanTmp,dbean));
        }
        dbean.setLstCols(lstColBeans);
        return 1;
    }

    private static void getAllEleCols(List<XmlElementBean> lstEleDisplayBeans,List<XmlElementBean> lstAllEleCols,ReportBean rbean)
    {
        if(lstEleDisplayBeans==null||lstEleDisplayBeans.size()==0) return;
        List<XmlElementBean> lstTmps;
        for(XmlElementBean eleDisplayBeanTmp:lstEleDisplayBeans)
        {//将所有被引用的<display/>标签中的<col/>子标签放入进来加载
            lstTmps=eleDisplayBeanTmp.getLstChildElements();
            if(lstTmps==null||lstTmps.size()==0) continue;
            for(XmlElementBean eleChilds:lstTmps)
            {
                if("col".equals(eleChilds.getName()))
                {
                    lstAllEleCols.add(eleChilds);
                }else if("ref".equals(eleChilds.getName()))
                {//又是引用其它资源项中的key
                    getAllEleCols(ConfigLoadAssistant.getInstance().getRefElements(eleChilds.attributeValue("key"),"display",null,rbean),
                            lstAllEleCols,rbean);
                }
            }
        }
    }

    public static ColBean loadColConfig(XmlElementBean eleColBean,DisplayBean dbean)
    {
        if(eleColBean==null) return null;
        ColBean cb=new ColBean(dbean);
        String column=eleColBean.attributeValue("column");
        if(column==null||column.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"失败，在<col/>中必须配置column属性");
        }
        cb.setColumn(column.trim());
        column=cb.getColumn();
        String property=eleColBean.attributeValue("property");
        if(property==null||property.trim().equals(""))
        {
            if(cb.isNonFromDbCol())
            {
                throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"失败，对于column配置为"+Consts_Private.NON_FROMDB
                        +"的列，必须配置其property属性");
            }
            property=column;
        }
        cb.setProperty(property.trim());
        
        List<XmlElementBean> lstEleColBeans=new ArrayList<XmlElementBean>();
        lstEleColBeans.add(eleColBean);
        
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(cb,lstEleColBeans);
        
        String label=eleColBean.attributeValue("label");
        if(label!=null)
        {
            cb.setLabel(Config.getInstance().getResourceString(null,dbean.getPageBean(),label,true));
        }
        cb.setDatatypeObj(ConfigLoadAssistant.loadDataType(eleColBean));
        String displaytype=eleColBean.attributeValue("displaytype");
        String labelstyleproperty=eleColBean.attributeValue("labelstyleproperty");
        String valuestyleproperty=eleColBean.attributeValue("valuestyleproperty");
        if(valuestyleproperty==null) valuestyleproperty="";
        if(labelstyleproperty==null) labelstyleproperty="";
        cb.setValuestyleproperty(valuestyleproperty);
        cb.setLabelstyleproperty(labelstyleproperty);

//        String printvaluestyleproperty=eleColBean.attributeValue("printvaluestyleproperty");




//        if(printvaluestyleproperty!=null)



        String plainexcelwidth=eleColBean.attributeValue("plainexcelwidth");
        if(plainexcelwidth!=null)
        {
            if(plainexcelwidth.trim().equals(""))
            {
                cb.setPlainexcelwidth(0f);
            }else
            {
                cb.setPlainexcelwidth(Float.parseFloat(plainexcelwidth.trim()));
            }
        }
        String pdfwidth=eleColBean.attributeValue("pdfwidth");
        if(pdfwidth!=null)
        {
            if(pdfwidth.trim().equals(""))
            {
                cb.setPdfwidth(0f);
            }else
            {
                cb.setPdfwidth(Float.parseFloat(pdfwidth.trim()));
            }
        }
        String printwidth=eleColBean.attributeValue("printwidth");
        if(printwidth!=null)
        {
           cb.setPrintwidth(printwidth.trim());
        }
        
        //        {
        
        
        if(labelstyleproperty!=null&&!labelstyleproperty.trim().equals(""))
        {
            cb.setLabelalign(Tools.getPropertyValueByName("align",labelstyleproperty,true));
        }
        if(valuestyleproperty!=null&&!valuestyleproperty.trim().equals(""))
        {
            cb.setValuealign(Tools.getPropertyValueByName("align",valuestyleproperty,true));
        }
        if(displaytype!=null) cb.setDisplaytype(displaytype.toLowerCase().trim());
        String tagcontent=eleColBean.getContent();
        if(tagcontent!=null&&!tagcontent.trim().equals(""))
        {
            cb.setTagcontent(Config.getInstance().getResourceString(null,cb.getPageBean(),tagcontent.trim(),false));
        }
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(cb,lstEleColBeans);
        //        LoadExtendConfigManager.loadAfterExtendConfigForPagetype(cb,lstEleColBeans);
        return cb;
    }

    public static void loadEditableColConfig(ColBean colbean,XmlElementBean eleColBean,String reportTypeKey)
    {
        if(eleColBean==null||colbean==null) return;
        EditableReportColBean ercbean=(EditableReportColBean)colbean.getExtendConfigDataForReportType(reportTypeKey);
        if(ercbean==null)
        {
            ercbean=new EditableReportColBean(colbean);
            colbean.setExtendConfigDataForReportType(reportTypeKey,ercbean);
        }
        String defaultvalue=eleColBean.attributeValue("defaultvalue");
        if(defaultvalue!=null)
        {
            ercbean.setDefaultvalue(defaultvalue);
        }
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(colbean.getDisplaytype())&&!colbean.isNonValueCol()&&!colbean.isSequenceCol()
                &&!colbean.isControlCol())
        {
            String updatecol=eleColBean.attributeValue("updatecol");
            if(updatecol!=null) ercbean.setUpdatecol(updatecol.trim());
            XmlElementBean eleInputboxBean=eleColBean.getChildElementByName("inputbox");
            if(eleInputboxBean!=null)
            {
                String inputbox=eleInputboxBean.attributeValue("type");
                String fillmode=eleInputboxBean.attributeValue("fillmode");
                String box_defaultvalue=eleInputboxBean.attributeValue("defaultvalue");
                int ifillmode=-1;
                if(fillmode!=null&&!fillmode.trim().equals(""))
                {
                    try
                    {
                        ifillmode=Integer.parseInt(fillmode.trim());
                    }catch(NumberFormatException e)
                    {
                        log.warn("配置的输入框的fillmode属性"+fillmode+"不是合法数字，将用默认模式填充当前输入框",e);
                        ifillmode=-1;
                    }
                    if(ifillmode!=1&&ifillmode!=2)
                    {
                        log.warn("配置的输入框的fillmode属性"+fillmode+"无效，将用默认模式填充");
                        ifillmode=-1;
                    }
                }
                AbsInputBox box=Config.getInstance().getInputBoxTypeByName(inputbox);
                box=(AbsInputBox)box.clone(ercbean);
                if(ifillmode==-1)
                {
                    box.setDefaultFillmode(Config.getInstance().getReportType(colbean.getReportBean().getType()));
                }else
                {
                    box.setFillmode(ifillmode);
                }
                if(box_defaultvalue!=null)
                {
                    box.setDefaultvalue(box_defaultvalue.trim());
                }
                box.loadInputBoxConfig(ercbean,eleInputboxBean);
                ercbean.setInputbox(box);
            }
        }
    }

    public static void loadEditableSqlConfig(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans,String reportTypeKey)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(reportTypeKey);
        if(ersqlbean==null)
        {
            ersqlbean=new EditableReportSqlBean(sqlbean);
            sqlbean.setExtendConfigDataForReportType(reportTypeKey,ersqlbean);
        }
        Map<String,String> mSqlProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleSqlBeans,
                new String[] { "transaction", "beforesave", "aftersave", "savebinding","deletebinding" });//组装所有<sql/>配置的这些属性  
        String transactiontype=mSqlProperties.get("transaction");
        if(transactiontype!=null&&!transactiontype.trim().equals(""))
        {
            transactiontype=transactiontype.toLowerCase().trim();
            if(!Consts_Private.M_ALL_TRANSACTION_LEVELS.containsKey(transactiontype))
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，在其<sql/>中配置的transaction属性"+transactiontype+"不合法");    
            }
            ersqlbean.setTransactionLever(transactiontype);
        }

        String beforeSaveAction=mSqlProperties.get("beforesave");
        if(beforeSaveAction!=null&&!beforeSaveAction.trim().equals(""))
        {
            ersqlbean.setBeforeSaveAction(beforeSaveAction.trim());
        }
        String afterSaveAction=mSqlProperties.get("aftersave");
        if(afterSaveAction!=null&&!afterSaveAction.trim().equals(""))
        {
            ersqlbean.setAfterSaveAction(loadUpdatePostAction(sqlbean,afterSaveAction.trim()));
        }
        String saveBinding=mSqlProperties.get("savebinding");//绑定保存
        if(saveBinding!=null&&!saveBinding.trim().equals(""))
        {
            ersqlbean.setLstSaveBindingReportIds(Tools.parseStringToList(saveBinding,";"));
        }
        String deletebinding=mSqlProperties.get("deletebinding");
        if(deletebinding!=null&&!deletebinding.trim().equals(""))
        {
            ersqlbean.setLstDeleteBindingReportIds(Tools.parseStringToList(deletebinding,";"));
        }
        
        loadInsertConfig(lstEleSqlBeans,ersqlbean);
        loadUpdateConfig(lstEleSqlBeans,ersqlbean);
        loadDeleteConfig(lstEleSqlBeans,ersqlbean);
    }

    private static void loadInsertConfig(List<XmlElementBean> lstEleSqlBeans,EditableReportSqlBean ersqlbean)
    {
        XmlElementBean eleInsertBean=getEleSqlUpdateBean(lstEleSqlBeans,"insert");
        if(eleInsertBean!=null)
        {
            XmlElementBean eleValueBean=eleInsertBean.getChildElementByName("value");
            String insertsqls=null;
            if(eleValueBean!=null)
            {
                insertsqls=eleValueBean.getContent();
            }else
            {
                insertsqls=eleInsertBean.getContent();
            }
            if(insertsqls!=null&&!insertsqls.trim().equals(""))
            {
                EditableReportUpdateDataBean insertBean=new EditableReportUpdateDataBean(ersqlbean,EditableReportUpdateDataBean.EDITTYPE_INSERT);
                ersqlbean.setInsertbean(insertBean);
                insertsqls=Tools.formatStringBlank(insertsqls);
                insertBean.setSqls(insertsqls);
                loadExternalValues(eleInsertBean.getChildElementByName("external-values"),insertBean);
            }
        }
    }

    private static void loadUpdateConfig(List<XmlElementBean> lstEleSqlBeans,EditableReportSqlBean ersqlbean)
    {
        XmlElementBean eleUpdateBean=getEleSqlUpdateBean(lstEleSqlBeans,"update");
        if(eleUpdateBean!=null)
        {
            XmlElementBean eleValueBean=eleUpdateBean.getChildElementByName("value");
            String updatesqls=null;
            if(eleValueBean!=null)
            {
                updatesqls=eleValueBean.getContent();
            }else
            {
                updatesqls=eleUpdateBean.getContent();
            }
            if(updatesqls!=null&&!updatesqls.trim().equals(""))
            {
                EditableReportUpdateDataBean updateBean=new EditableReportUpdateDataBean(ersqlbean,EditableReportUpdateDataBean.EDITTYPE_UPDATE);
                ersqlbean.setUpdatebean(updateBean);
                updatesqls=Tools.formatStringBlank(updatesqls);
                updateBean.setSqls(updatesqls);
                loadExternalValues(eleUpdateBean.getChildElementByName("external-values"),updateBean);
            }
        }
    }

    public static void loadDeleteConfig(List<XmlElementBean> lstEleSqlBeans,EditableReportSqlBean ersqlbean)
    {
        XmlElementBean eleDeleteBean=getEleSqlUpdateBean(lstEleSqlBeans,"delete");
        if(eleDeleteBean!=null)
        {
            XmlElementBean eleValueBean=eleDeleteBean.getChildElementByName("value");
            String deletesqls=null;
            if(eleValueBean!=null)
            {
                deletesqls=eleValueBean.getContent();
            }else
            {
                deletesqls=eleDeleteBean.getContent();
            }
            if(deletesqls!=null&&!deletesqls.trim().equals(""))
            {
                deletesqls=Tools.formatStringBlank(deletesqls);
                EditableReportUpdateDataBean deleteBean=new EditableReportUpdateDataBean(ersqlbean,EditableReportUpdateDataBean.EDITTYPE_DELETE);
                deleteBean.setSqls(deletesqls);
                ersqlbean.setDeletebean(deleteBean);
                loadExternalValues(eleDeleteBean.getChildElementByName("external-values"),deleteBean);
                String confirmmessage=eleDeleteBean.attributeValue("confirmessage");
                if(confirmmessage!=null&&!confirmmessage.trim().equals(""))
                {
                    deleteBean.setDeleteConfirmMessage(Config.getInstance().getResourceString(null,ersqlbean.getOwner().getPageBean(),confirmmessage,
                            true));
                }
            }
        }
    }

    public static XmlElementBean getEleSqlUpdateBean(List<XmlElementBean> lstEleSqlBeans,String updatetype)
    {
        if(lstEleSqlBeans==null||lstEleSqlBeans.size()==0) return null;
        XmlElementBean eleUpdateBeanTmp;
        for(XmlElementBean eleSqlBeanTmp:lstEleSqlBeans)
        {
            eleUpdateBeanTmp=eleSqlBeanTmp.getChildElementByName(updatetype);
            if(eleUpdateBeanTmp!=null) return eleUpdateBeanTmp;
        }
        return null;
    }

    public static String[] loadUpdatePostAction(SqlBean sqlbean,String postaction)
    {
        if(postaction==null||postaction.trim().equals("")) return null;
        postaction=postaction.trim();
        String[] action=new String[2];
        int idx=postaction.indexOf("|");
        if(idx>0)
        {
            action[0]=postaction.substring(0,idx).trim();
            String flag=postaction.substring(idx+1).toLowerCase().trim();
            if(!flag.equals("true")&&!flag.equals("false"))
            {
                throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置的属性"+postaction+"配置不合法");
            }
            action[1]=flag;
        }else
        {
            action[0]=postaction;
            action[1]="false";
        }
        return action;
    }

    private static void loadExternalValues(XmlElementBean eleExternalValuesBean,EditableReportUpdateDataBean updateBean)
    {
        if(eleExternalValuesBean==null) return;
        List<XmlElementBean> lstExternalValuesEles=eleExternalValuesBean.getLstChildElementsByName("value");
        if(lstExternalValuesEles==null||lstExternalValuesEles.size()==0) return;
        List<EditableReportExternalValueBean> lstExternalValues=new ArrayList<EditableReportExternalValueBean>();
        for(XmlElementBean eleExternalValueBeanTmp:lstExternalValuesEles)
        {
            String name=eleExternalValueBeanTmp.attributeValue("name");
            String value=eleExternalValueBeanTmp.attributeValue("value");
            if(name==null||name.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载<external-values/>的<value/>失败，name属性不能为空");
            }
            value=value==null?"":value.trim();

            EditableReportExternalValueBean valueBean=new EditableReportExternalValueBean(updateBean);
            valueBean.setName(name.trim());
            valueBean.setValue(value);
            if(Tools.isDefineKey("@",value)||Tools.isDefineKey("#",value))
            {
                String datatype=eleExternalValueBeanTmp.attributeValue("datatype");
                if(datatype==null)
                {
                    valueBean.setTypeObj(null);
                }else
                {
                    valueBean.setTypeObj(ConfigLoadAssistant.loadDataType(eleExternalValueBeanTmp));
                }
            }else
            {
                valueBean.setTypeObj(ConfigLoadAssistant.loadDataType(eleExternalValueBeanTmp));
            }
            if(value.trim().equals("now{}"))
            {//是取当前时间做为参数值
                if(!(valueBean.getTypeObj() instanceof AbsDateTimeType))
                {
                    throw new WabacusConfigLoadingException("配置为取当前时间（now()）的参数值的数据类型必须配置为日期类型");
                }
            }
            lstExternalValues.add(valueBean);
        }
        if(lstExternalValues.size()>0)
        {
            updateBean.setLstExternalValues(lstExternalValues);
        }
    }

    public static void doEditableReportTypePostLoad(ReportBean reportbean,String reportTypeKey)
    {
        DisplayBean dbean=reportbean.getDbean();
        if(dbean!=null) processAllUpdateCol(dbean,reportTypeKey);
        SqlBean sqlbean=reportbean.getSbean();
        if(sqlbean==null) return;
        ButtonsBean bbeans=reportbean.getButtonsBean();
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean==null)
        {
            if(bbeans!=null)
            {
                bbeans.removeAllCertainTypeButtons(UpdateButton.class);
                bbeans.removeAllCertainTypeButtons(AddButton.class);
                bbeans.removeAllCertainTypeButtons(DeleteButton.class);
                bbeans.removeAllCertainTypeButtons(SaveButton.class);
                bbeans.removeAllCertainTypeButtons(CancelButton.class);
                bbeans.removeAllCertainTypeButtons(ResetButton.class);
            }
            return;
        }
        String systemjsfile=null;
        if(Config.encode.toLowerCase().trim().equals("utf-8"))
        {
            systemjsfile="/webresources/script/wabacus_editsystem.js";
        }else
        {
            String encode=Config.encode;
            if(encode.trim().equalsIgnoreCase("gb2312"))  encode="gbk";
            systemjsfile="/webresources/script/"+encode.toLowerCase()+"/wabacus_editsystem.js";
        }
        systemjsfile=Config.webroot+"/"+systemjsfile;
        systemjsfile=Tools.replaceAll(systemjsfile,"//","/");
        reportbean.getPageBean().addMyJavascript(systemjsfile);
        List<ReportBean> lstBindingReportBeans=getLstBindedReportBeans(reportbean,ersqlbean.getLstSaveBindingReportIds(),"savebinding");
        if(lstBindingReportBeans!=null&&lstBindingReportBeans.size()>0)
        {
            ersqlbean.setLstSaveBindingReportBeans(lstBindingReportBeans);
        }
        ersqlbean.setLstSaveBindingReportIds(null);
        lstBindingReportBeans=getLstBindedReportBeans(reportbean,ersqlbean.getLstDeleteBindingReportIds(),"deletebinding");
        if(lstBindingReportBeans!=null&&lstBindingReportBeans.size()>0)
        {
            ersqlbean.setLstDeleteBindingReportBeans(lstBindingReportBeans);
        }
        ersqlbean.setLstDeleteBindingReportIds(null);
        
        EditableReportUpdateDataBean insertBean=ersqlbean.getInsertbean();
        if(insertBean!=null)
        {
            int result=insertBean.parseSqls(sqlbean,reportTypeKey);
            if(result<=0) ersqlbean.setInsertbean(null);
        }
        EditableReportUpdateDataBean updateBean=ersqlbean.getUpdatebean();
        if(updateBean!=null)
        {
            int result=updateBean.parseSqls(sqlbean,reportTypeKey);
            if(result<=0) ersqlbean.setUpdatebean(null);
        }
        EditableReportUpdateDataBean deleteBean=ersqlbean.getDeletebean();
        if(deleteBean!=null)
        {
            int result=deleteBean.parseSqls(sqlbean,reportTypeKey);
            if(result<=0) ersqlbean.setDeletebean(null);
        }
        if(ersqlbean.getInsertbean()!=null) ersqlbean.getInsertbean().doPostLoad();
        if(ersqlbean.getUpdatebean()!=null) ersqlbean.getUpdatebean().doPostLoad();
        if(ersqlbean.getDeletebean()!=null) ersqlbean.getDeletebean().doPostLoad();
    }

    private static List<ReportBean> getLstBindedReportBeans(ReportBean reportbean,List<String> lstBindedReportIds,String bindtype)
    {
        if(lstBindedReportIds==null||lstBindedReportIds.size()==0) return null;
        List<ReportBean> lstBindedReportBeans=new ArrayList<ReportBean>();
        List<String> lstReportIdsTmp=new ArrayList<String>();
        ReportBean rbBindedTmp;
        for(String bindedReportidTmp:lstBindedReportIds)
        {
            if(bindedReportidTmp==null||bindedReportidTmp.trim().equals("")||bindedReportidTmp.equals(reportbean.getId()))
            {
                continue;
            }
            if(lstReportIdsTmp.contains(bindedReportidTmp)) continue;
            lstReportIdsTmp.add(bindedReportidTmp);
            rbBindedTmp=reportbean.getPageBean().getReportChild(bindedReportidTmp,true);
            if(rbBindedTmp==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，通过<sql/>的"+bindtype+"属性绑定操作的报表"+bindedReportidTmp+"不存在");
            }
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbBindedTmp.getSbean().getExtendConfigDataForReportType(
                    EditableReportSqlBean.class);
            if(ersqlbean==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，通过<sql/>的"+bindtype+"属性绑定操作的报表"+bindedReportidTmp
                        +"不是可编辑报表类型");
            }
            if(bindtype.equals("savebinding"))
            {
                Config.getInstance().authorize(rbBindedTmp.getGuid(),Consts.BUTTON_PART,"type{"+Consts_Private.SAVE_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");
            }else if(bindtype.equals("deletebinding"))
            {
                Config.getInstance().authorize(rbBindedTmp.getGuid(),Consts.BUTTON_PART,"type{"+Consts_Private.DELETE_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");
            }
            lstBindedReportBeans.add(rbBindedTmp);
        }
        if(lstBindedReportBeans.size()==0) return null;
        return lstBindedReportBeans;
    }
    
    private static void processAllUpdateCol(DisplayBean dbean,String reportTypeKey)
    {
        List<ColBean> lstCols=dbean.getLstCols();
        if(lstCols==null||lstCols.size()==0) return;
        EditableReportColBean ercolbeanTmp;
        for(ColBean cbean:lstCols)
        {
            if(cbean==null||Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) continue;
            if(cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isControlCol()) continue;
            ercolbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(reportTypeKey);
            if(ercolbeanTmp==null||ercolbeanTmp.getUpdatecol()==null||ercolbeanTmp.getUpdatecol().trim().equals("")) continue;
            ColBean cbTemp=getReferedColBean(cbean);//取到被引用的col
            ercolbeanTmp.setUpdateCbean(cbTemp);
            EditableReportColBean ercbeanTmp=(EditableReportColBean)cbTemp.getExtendConfigDataForReportType(reportTypeKey);
            if(ercbeanTmp==null)
            {
                ercbeanTmp=new EditableReportColBean(cbTemp);
                cbTemp.setExtendConfigDataForReportType(reportTypeKey,ercbeanTmp);
            }else if(ercbeanTmp.getUpdatedcol()!=null&&!ercbeanTmp.getUpdatedcol().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+dbean.getReportBean().getPath()+"的column属性为"+cbTemp.getColumn()
                        +"的<col/>被多个<col/>通过updatecol属性引用");
            }
            ercbeanTmp.setUpdatedcol(cbean.getProperty());//将property设置到被引用的扩展配置对象中，以便下次能通过它取到被哪个<col/>引用到
        }
    }

    private static ColBean getReferedColBean(ColBean cbean)
    {
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(EditableReportColBean.class);
        ColBean cbTemp=cbean.getReportBean().getDbean().getColBeanByColProperty(ercbean.getUpdatecol());
        if(cbTemp==null)
        {
            throw new WabacusConfigLoadingException("报表"+cbean.getReportBean().getPath()+"的column属性为"+cbean.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecol()+"引用的列不存在");
        }
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTemp.getDisplaytype()))
        {
            throw new WabacusConfigLoadingException("报表"+cbean.getReportBean().getPath()+"的column属性为"+cbean.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecol()+"引用的列不是displaytype为hidden的列");
        }
        if(cbTemp.getProperty()==null||cbTemp.getProperty().trim().equals("")||cbTemp.isNonValueCol()
                ||cbTemp.isSequenceCol()||cbTemp.isControlCol())
        {
            throw new WabacusConfigLoadingException("报表"+cbean.getReportBean().getPath()+"的column属性为"+cbean.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecol()+"引用的列不是从数据库中获取数据，不能被引用");
        }
        return cbTemp;
    }
}
