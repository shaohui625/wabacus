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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.wabacus.config.Config;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.component.other.ButtonsBean;
import com.wabacus.config.dataexport.DataExportsConfigBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public abstract class AbsComponentConfigBean implements IComponentConfigBean,Cloneable
{
    protected String id;

    protected String refreshid;
    
    protected String refreshGuid;
    
    protected String top;
    
    protected String bottom;

    protected String left;
    
    protected String right;

    protected String width;
    
    protected String height;
    
    protected String align;
    
    protected String valign="top";
    
    protected String scrollstyle;
    
    protected String title;
    
    protected Map<String,String> mDynTitleParts;//标题title中的动态部分，key为此动态值的在title中的占位符，值为request{xxx}、session{key}、url{key}等等形式，用于运行时得到真正值

    protected String titlealign="left";
    
    protected String subtitle;
    
    protected Map<String,String> mDynSubtitleParts;
    
    protected String parenttitle;
    
    protected Map<String,String> mDynParenttitleParts;//副标题parenttitle中的动态部分，形式与mDynTitleParts一致
    
    protected String parentSubtitle;
    
    protected Map<String,String> mDynParentSubtitleParts;
    
    private ButtonsBean buttonsBean=null;
    
    private boolean showContextMenu=true;
    
    private TemplateBean headerTplBean;//<header/>配置的静态模板对象
    
    private TemplateBean footerTplBean;//<footer/>配置的静态模板对象
    
    private DataExportsConfigBean dataExportsBean;
    
    protected AbsPrintProviderConfigBean printBean;
    
    private PDFExportBean pdfPrintBean;//PDF打印配置，与pdf导出配置完全一样，但与其它打印方式不同，所以单独做为一个成员变量存放
    
    protected List<OnloadMethodBean> lstOnloadMethods;
    
    protected AbsContainerConfigBean parentContainer;

    public AbsComponentConfigBean(AbsContainerConfigBean parentContainer)
    {
        this.parentContainer=parentContainer;
    }
    
    public String getId()
    {
        return id;
    }

    public String getGuid()
    {
        if(this.parentContainer==null) return this.id;//没有父容器，即是顶层<page/>
        return this.getPageBean().getId()+Consts_Private.GUID_SEPERATOR+this.id;
    }
    
    public String getPath()
    {
        if(this.getParentContainer()==null)
        {
            return this.id;
        }else
        {
            return this.getParentContainer().getPath()+Consts_Private.PATH_SEPERATOR+this.id;
        }
    }
    
    public void setId(String id)
    {
        this.id=id;
    }

    public String getRefreshid()
    {
        return refreshid;
    }

    public void setRefreshid(String refreshid)
    {
        this.refreshid=refreshid;
    }

    public String getRefreshGuid()
    {
        if(this.id.equals(this.getPageBean().getId())) return this.id;
        if(this.refreshGuid==null)
        {
            this.refreshGuid=ComponentConfigLoadAssistant.getInstance().createComponentRefreshGuidByRefreshId(this.getPageBean(),this.id,this.refreshid);
        }
        return refreshGuid;
    }

    public String getTop()
    {
        return top;
    }

    public void setTop(String top)
    {
        this.top=top;
    }

    public String getBottom()
    {
        return bottom;
    }

    public void setBottom(String bottom)
    {
        this.bottom=bottom;
    }

    public String getRight()
    {
        return right;
    }

    public void setRight(String right)
    {
        this.right=right;
    }

    public String getLeft()
    {
        return left;
    }

    public void setLeft(String left)
    {
        this.left=left;
    }

    public String getAlign()
    {
        return align;
    }

    public void setAlign(String align)
    {
        this.align=align;
    }

    public String getValign()
    {
        return valign;
    }

    public void setValign(String valign)
    {
        this.valign=valign;
    }

    public DataExportsConfigBean getDataExportsBean()
    {
        return dataExportsBean;
    }

    public void setDataExportsBean(DataExportsConfigBean dataExportsBean)
    {
        this.dataExportsBean=dataExportsBean;
    }

    public String getScrollstyle()
    {
        if(scrollstyle==null||scrollstyle.trim().equals(""))
        {
            String scrollstyleTmp=Config.getInstance().getSystemConfigValue("default-scrollstyle",Consts_Private.SCROLLSTYLE_NORMAL).toLowerCase();
            if(!Consts_Private.lstAllScrollStyles.contains(scrollstyleTmp))
            {
                throw new WabacusRuntimeException("在wabacus.cfg.xml的default-scrollstyle属性中配置的值"+scrollstyleTmp+"不支持");
            }
            return scrollstyleTmp;
        }
        return scrollstyle;
    }

    public void setScrollstyle(String scrollstyle)
    {
        this.scrollstyle=scrollstyle;
    }

    public String getTitle(ReportRequest rrequest)
    {
        String realtitle=null;
        if(this.mDynTitleParts==null||this.mDynTitleParts.size()==0)
        {
            realtitle=this.title;
        }else
        {
            realtitle=WabacusAssistant.getInstance().getRuntimeStringValueWithDynPart(rrequest,this.title,this.mDynTitleParts);
        }
        return realtitle==null?"":rrequest.getI18NStringValue(realtitle.trim());
    }

    public void setTitle(String title)
    {
        this.mDynTitleParts=new HashMap<String,String>();
        this.title=WabacusAssistant.getInstance().parseStringWithDynPart(title,this.mDynTitleParts);
        if(this.mDynTitleParts.size()==0) this.mDynTitleParts=null;
    }

    public String getSubtitle(ReportRequest rrequest)
    {
        String realsubtitle=null;
        if(this.mDynSubtitleParts==null||this.mDynSubtitleParts.size()==0)
        {
            realsubtitle=this.subtitle;
        }else
        {
            realsubtitle=WabacusAssistant.getInstance().getRuntimeStringValueWithDynPart(rrequest,this.subtitle,this.mDynSubtitleParts);
        }
        return realsubtitle==null?"":rrequest.getI18NStringValue(realsubtitle.trim());
    }

    public void setSubtitle(String subtitle)
    {
        this.mDynSubtitleParts=new HashMap<String,String>();
        this.subtitle=WabacusAssistant.getInstance().parseStringWithDynPart(subtitle,this.mDynSubtitleParts);
        if(this.mDynSubtitleParts.size()==0) this.mDynSubtitleParts=null;
    }

    public String getParenttitle(ReportRequest rrequest)
    {
        String realparenttitle=null;
        if(this.mDynParenttitleParts==null||this.mDynParenttitleParts.size()==0)
        {
            realparenttitle=this.parenttitle;
        }else
        {
            realparenttitle=WabacusAssistant.getInstance().getRuntimeStringValueWithDynPart(rrequest,this.parenttitle,this.mDynParenttitleParts);
        }
        return realparenttitle==null?"":rrequest.getI18NStringValue(realparenttitle.trim());
    }

    public void setParenttitle(String parenttitle)
    {
        this.mDynParenttitleParts=new HashMap<String,String>();
        this.parenttitle=WabacusAssistant.getInstance().parseStringWithDynPart(parenttitle,this.mDynParenttitleParts);
        if(this.mDynParenttitleParts.size()==0) this.mDynParenttitleParts=null;
    }

    public String getParentSubtitle(ReportRequest rrequest)
    {
        String realparentSubtitle=null;
        if(this.mDynParentSubtitleParts==null||this.mDynParentSubtitleParts.size()==0)
        {
            realparentSubtitle=this.parentSubtitle;
        }else
        {
            realparentSubtitle=WabacusAssistant.getInstance().getRuntimeStringValueWithDynPart(rrequest,this.parentSubtitle,this.mDynParentSubtitleParts);
        }
        return realparentSubtitle==null?"":rrequest.getI18NStringValue(realparentSubtitle.trim());
    }

    public void setParentSubtitle(String parentSubtitle)
    {
        this.mDynParentSubtitleParts=new HashMap<String,String>();
        this.parentSubtitle=WabacusAssistant.getInstance().parseStringWithDynPart(parentSubtitle,this.mDynParentSubtitleParts);
        if(this.mDynParentSubtitleParts.size()==0) this.mDynParentSubtitleParts=null;
    }

    public void setMDynTitleParts(Map<String,String> dynTitleParts)
    {
        this.mDynTitleParts=dynTitleParts;
    }

    public void setMDynSubtitleParts(Map<String,String> dynSubtitleParts)
    {
        this.mDynSubtitleParts=dynSubtitleParts;
    }

    public void setMDynParenttitleParts(Map<String,String> dynParenttitleParts)
    {
        this.mDynParenttitleParts=dynParenttitleParts;
    }

    public void setMDynParentSubtitleParts(Map<String,String> dynParentSubtitleParts)
    {
        this.mDynParentSubtitleParts=dynParentSubtitleParts;
    }

    public String getTitlealign()
    {
        return titlealign;
    }

    public void setTitlealign(String titlealign)
    {
        this.titlealign=titlealign;
    }

    public String getWidth()
    {
        return width;
    }

    public void setWidth(String width)
    {
        this.width=width;
    }

    public String getHeight()
    {
        return height;
    }

    public void setHeight(String height)
    {
        this.height=height;
    }
    
    public boolean shouldShowContextMenu()
    {
        if(!showContextMenu) return false;
        if(this.buttonsBean==null) return false;
        if(this.buttonsBean.getButtonsByPosition(Consts.CONTEXTMENU_PART)==null
                ||this.buttonsBean.getButtonsByPosition(Consts.CONTEXTMENU_PART).size()==0) return false;
        return true;
    }
    
    public void setShowContextMenu(boolean showContextMenu)
    {
        this.showContextMenu=showContextMenu;
    }
    
    public TemplateBean getHeaderTplBean()
    {
        return headerTplBean;
    }

    public void setHeaderTplBean(TemplateBean headerTplBean)
    {
        ComponentAssistant.getInstance().validComponentHeaderTpl(this,headerTplBean);
        this.headerTplBean=headerTplBean;
    }

    public TemplateBean getFooterTplBean()
    {
        return footerTplBean;
    }

    public void setFooterTplBean(TemplateBean footerTplBean)
    {
        ComponentAssistant.getInstance().validComponentFooterTpl(this,footerTplBean);
        this.footerTplBean=footerTplBean;
    }
    
    public ButtonsBean getButtonsBean()
    {
        return buttonsBean;
    }

    public void setButtonsBean(ButtonsBean buttonsBean)
    {
        this.buttonsBean=buttonsBean;
    }
    
    public void setPrintBean(AbsPrintProviderConfigBean printBean)
    {
        this.printBean=printBean;
    }
    
    public AbsPrintProviderConfigBean getPrintBean()
    {
        return this.printBean;
    }
    
    public PDFExportBean getPdfPrintBean()
    {
        return pdfPrintBean;
    }

    public void setPdfPrintBean(PDFExportBean pdfPrintBean)
    {
        this.pdfPrintBean=pdfPrintBean;
    }
    
    public void addOnloadMethod(OnloadMethodBean onLoadMethodBean)
    {
        if(this.lstOnloadMethods==null) this.lstOnloadMethods=new ArrayList<OnloadMethodBean>();
        lstOnloadMethods.add(onLoadMethodBean);
    }
    
    public void removeOnloadMethodByType(String type)
    {
        if(lstOnloadMethods==null||lstOnloadMethods.size()==0) return;
        for(int i=lstOnloadMethods.size()-1;i>=0;i--)
        {
            if(type.equalsIgnoreCase(lstOnloadMethods.get(i).getType()))
            {
                lstOnloadMethods.remove(i);
            }
        }
    }
    
    public List<OnloadMethodBean> getLstOnloadMethods()
    {
        return lstOnloadMethods;
    }

    public String getOnloadMethodName()
    {
        if(lstOnloadMethods==null||lstOnloadMethods.size()==0) return "''";
        return this.getGuid()+"_onload";
    }
    
    public AbsContainerConfigBean getParentContainer()
    {
        return parentContainer;
    }

    public void setParentContainer(AbsContainerConfigBean parentContainer)
    {
        this.parentContainer=parentContainer;
    }

    public PageBean getPageBean()
    {
        if(this instanceof PageBean)
        {
            return (PageBean)this;
        }
        AbsContainerConfigBean bean=this.getParentContainer();
        if(bean==null) return null;
        while(!(bean instanceof PageBean))
        {
            bean=bean.getParentContainer();
            if(bean==null) return null;
            continue;
        }
        return (PageBean)bean;
    }

    public void doPostLoad()
    {
        if(this.dataExportsBean!=null) this.dataExportsBean.doPostLoad();
        if(this.printBean!=null) this.printBean.doPostLoad();
        if(pdfPrintBean!=null) pdfPrintBean.doPostLoad();
    }

    public IComponentConfigBean clone(AbsContainerConfigBean parentContainer)
    {
        try
        {
            AbsComponentConfigBean configBeanNew=(AbsComponentConfigBean)super.clone();
            configBeanNew.setParentContainer(parentContainer);
            configBeanNew.setRefreshid(null);
            if(this.dataExportsBean!=null)
            {
                configBeanNew.setDataExportsBean(dataExportsBean.clone(configBeanNew));
            }
            if(this.printBean!=null)
            {
                AbsPrintProviderConfigBean newPrintBean=(AbsPrintProviderConfigBean)this.printBean.clone();
                newPrintBean.setOwner(configBeanNew);
                configBeanNew.setPrintBean(newPrintBean);
            }
            if(pdfPrintBean!=null)
            {
                configBeanNew.setPdfPrintBean((PDFExportBean)pdfPrintBean.clone(configBeanNew)); 
            }
            if(this.mDynTitleParts!=null)
            {
                configBeanNew.setMDynTitleParts((Map<String,String>)((HashMap<String,String>)this.mDynTitleParts).clone());
            }
            if(this.mDynSubtitleParts!=null)
            {
                configBeanNew.setMDynSubtitleParts((Map<String,String>)((HashMap<String,String>)this.mDynSubtitleParts).clone());
            }
            if(this.mDynParenttitleParts!=null)
            {
                configBeanNew.setMDynParenttitleParts((Map<String,String>)((HashMap<String,String>)this.mDynParenttitleParts).clone());
            }
            if(this.mDynParentSubtitleParts!=null)
            {
                configBeanNew.setMDynParentSubtitleParts((Map<String,String>)((HashMap<String,String>)this.mDynParentSubtitleParts).clone());
            }
            return configBeanNew;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    
    
    private Map<String,String> attrs;

    public Map<String,String> getAttrs()
    {
        return attrs == null ? MapUtils.EMPTY_MAP : attrs;
    }

    public void setAttrs(Map<String,String> attrs)
    {
        this.attrs=attrs;
    }
}
