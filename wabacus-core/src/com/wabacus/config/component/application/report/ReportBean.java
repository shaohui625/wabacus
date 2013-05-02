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
package com.wabacus.config.component.application.report;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.report.extendconfig.IAfterConfigLoadForReportType;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.other.ButtonsBean;
import com.wabacus.config.dataexport.DataExportsConfigBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.DataImportButton;
import com.wabacus.system.buttons.SearchButton;
import com.wabacus.system.commoninterface.IReportPersonalizePersistence;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsDetailReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IReportType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.FileBox;
import com.wabacus.system.inputbox.PopUpBox;
import com.wabacus.system.inputbox.SelectBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.UniqueArrayList;

public class ReportBean extends AbsConfigBean implements IApplicationConfigBean
{
    private String id;

    private String refreshid;
    
    protected String refreshGuid;
    
    private String top;

    private String bottom;

    private String left;

    private String right;

    private String width="100%";

    private String printwidth;
    
    private String height;

    private String align;

    private String valign="top";

    private String title;

    private String titlealign="left";//报表标题对齐方式，可配置值为：left,center,right
    
    private Map<String,String> mDynTitleParts;
    
    private String subtitle;

    private Map<String,String> mDynSubtitleParts;
    
    private String parenttitle;

    private Map<String,String> mDynParenttitleParts;//副标题parenttitle中的动态部分，形式与mDynTitleParts一致
    
    protected String parentSubtitle;
    
    protected Map<String,String> mDynParentSubtitleParts;
    
    private String navigate_reportid;//用哪个报表的翻页导航栏，默认是本报表有一个独立的翻页导航栏，此时此值为<report/>的id值，也可以配置成其它报表的<report/>的id，则与别的报表共用一个翻页导航栏。
    
    private Set<String> sRelateConditionReportids;

    private XmlElementBean eleReportBean;

    private String bordercolor;//报表中各单元格之间边框的颜色

    private String strclass;

    private boolean showContextMenu=true;
    
    private DisplayBean dbean;

    private SqlBean sbean;

    private List<AbsDataImportConfigBean> lstDataImportItems;//本报表配置的所有数据导入项

    private List<String> lstDataImportFileNames;

    private int dataimportwidth;

    private int dataimportheight;
    
    private String dataimportinitsize;//上传数据文件窗口的初始大小，可配置值包括min/max/normal，分别表示最大化、最小化、正常窗口大小（即上面pagewidth/pageheight配置的大小）
    
    private String dataimportmaxbtn;
    
    private String dataimportminbtn;//上传数据文件窗口是否显示最小化窗口按钮

    private String scrollheight;

    private String scrollwidth;

    private String scrollstyle;
    
    private String border=Consts_Private.REPORT_BORDER_ALL;

    private String dynTplPath;//如果当前报表是用动态类型的模板（比如jsp、servlet），则在这里指定其访问URI，如果值为Consts_Private.REPORT_TEMPLATE_NONE，则没有显示模板，也不会用框架内置的全局模板
    
    private TemplateBean tplBean;

    private DataExportsConfigBean dataExportsBean;
    
    private String type;

    private String classcache="";

    private boolean blClasscache=false;

    private Class pojoclass;//存放从数据库中获取的数据的POJO类，只有当采用框架自动生成pojo类（即strclass=commondatabean）且blClasscache为true时，才会在此属性中存放pojo类的Class对象

    private ButtonsBean buttonsBean=null;

    private IInterceptor interceptor=null;

    private int cellresize=0;

    private int celldrag=0;

    private int jsvalidatetype;//客户端校验类型。0：点击搜索或保存按钮时进行校验；1：焦点离开输入框时马上校验

    private TemplateBean headerTplBean;//<header/>配置的静态模板对象
    
    private TemplateBean footerTplBean;//<footer/>配置的静态模板对象
    
    private AbsPrintProviderConfigBean printBean;
    
    private PDFExportBean pdfPrintBean;
    
    private FormatBean fbean;

    private List<String> ulstMyJavascript;

    private List<String> ulstMyCss;

    private List<Class> lstFormatClasses;//本报表所需用到的校验类。里面定义的校验方法可以在每个<col/>的format属性中使用。配置方式与系统配置文件的全局配置项format-class一样，也可以配置多个。

    private List<TextBox> lstTextBoxesWithTypePrompt;//加载时暂时存放当前报表所有配置有输入提示功能的文本框，加载完后将全部转移到下面的mTextBoxesWithTypePrompt变量中

    private Map<String,TextBox> mTextBoxesWithTypePrompt;

    private Map<String,SelectBox> mSelectBoxesInConditionWithRelate;

    private Map<String,SelectBox> mSelectBoxesInColWithRelate;
    
    private List<FileBox> lstUploadFileBoxes;

    private Map<String,FileBox> mUploadFileBoxes;//上传文件输入框，在dopostload()方法中由lstUploadFileBoxes生成

    private List<PopUpBox> lstPopUpBoxes;

    private Map<String,PopUpBox> mPopUpBoxes;

    private List<AbsInputBox> lstInputboxesWithAutoComplete;
    
    private Map<String,AbsInputBox> mInputboxesWithAutoComplete;
    
    private String dependParentId;//依赖的主报表ID

//    private ReportBean rootReportBean;//如果当前报表是从报表，这里存放其顶层主报表（这里考虑到多级主从关系的情况），此变量是在运行时判断并赋值的
    
    private String dependparams;

    private String[] refreshParentOnSave;
    
//    private String[] refreshParentOnDelete;//如果当前报表是可编辑从报表，则删除数据时需要刷新它的哪个主报表（因为可能有多层主报表），以及刷新方式。第一个元素为要刷新的主报表ID，第二个元素为刷新方式，true表示重新计算主报表页码，false为不重新计算主报表页码，默认为false。
    
    private boolean displayOnParentNoData=true;//当<report/>的dependstype配置为display时，此值为true，表示当父报表没有数据时，此从报表也显示出来；当配置为hidden时，此值为false，表示当父报表没有数据时，此从报表隐藏起来
    
    private Map<String,Map<String,String>> mDependChilds;

    private Map<String,String> mDependsDetailReportParams;//如果当前报表依赖细览报表，这里存放依赖所用到的所有参数，以便加载数据时能根据这里的参数取到条件数据，在此Map中，键为此参数在URL中的参数名，值为参数值对应的<col/>的property
    
    private List<OnloadMethodBean> lstOnloadMethods;

    private Set<String> sParamNamesFromURL;

    private AbsContainerConfigBean parentContainer;

    private String validateSearchMethod;//当前报表的搜索需要客户端校验时，这里存放校验的方法名，否则为null
    
    private List<SubmitFunctionParamBean> lstSearchFunctionDynParams;
    
    private List<Integer> lstPagesize;
    
    private Object navigateObj;
    
    private IReportPersonalizePersistence personalizeObj;
    
    public ReportBean(AbsConfigBean parent)
    {
        super(null);
        throw new WabacusConfigLoadingException("不能对"+this.getClass().getName()+"调用此构造函数");
    }

    public ReportBean(AbsContainerConfigBean parentContainer)
    {
        super(null);
        this.parentContainer=parentContainer;
    }

    public AbsContainerConfigBean getParentContainer()
    {
        return parentContainer;
    }

    public String getGuid()
    {
        return this.getPageBean().getId()+Consts_Private.GUID_SEPERATOR+this.id;
    }

    public String getPath()
    {
        return this.parentContainer.getPath()+Consts_Private.PATH_SEPERATOR+this.id;
    }

    public void setParentContainer(AbsContainerConfigBean parentContainer)
    {
        this.parentContainer=parentContainer;
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

    public String getHeight()
    {
        return height;
    }

    public void setHeight(String height)
    {
        this.height=height;
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

    public String getDependParentId()
    {
        return dependParentId;
    }

    public void setDependParentId(String dependParentId)
    {
        this.dependParentId=dependParentId;
    }

    public Map<String,String> getMDependsDetailReportParams()
    {
        return mDependsDetailReportParams;
    }

    public void setMDependsDetailReportParams(Map<String,String> dependsDetailReportParams)
    {
        mDependsDetailReportParams=dependsDetailReportParams;
    }

    public boolean isSlaveReport()
    {
        return this.dependParentId!=null&&!this.dependParentId.trim().equals("");
    }
    
    public boolean isSlaveReportDependsonListReport()
    {
        if(!isSlaveReport()) return false;
        return this.getPageBean().getReportChild(this.dependParentId,true).isListReportType();
    }
    
    public boolean isSlaveReportDependsonDetailReport()
    {
        if(!isSlaveReport()) return false;
        return this.getPageBean().getReportChild(this.dependParentId,true).isDetailReportType();
    }
    
    public boolean shouldDisplaySlaveReportDependsonDetailReport(ReportRequest rrequest)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return true;
        if(!this.isSlaveReportDependsonDetailReport()) return true;//没有依赖细览报表
        AbsReportType reportTypeObj=(AbsReportType)rrequest.getComponentTypeObj(this.dependParentId,null,false);
        if(reportTypeObj==null||reportTypeObj.getParentContainerType()==null) return true;
        boolean shouldShowParent=reportTypeObj.getReportBean().shouldDisplaySlaveReportDependsonDetailReport(rrequest);
        if(!shouldShowParent) return false;
        if(rrequest.getStringAttribute(this.id+"_PARENTREPORT_NODATA","").toLowerCase().equals("true")&&!this.isDisplayOnParentNoData())
            return false;
        return true;
    }
    
    public boolean isMasterReportOfMe(ReportBean rbeanMaster,boolean inherit)
    {
        if(!this.isSlaveReport()) return false;//当前报表不是从报表
        if(this.dependParentId.equals(rbeanMaster.getId())) return true;
        if(!inherit) return false;
        return ((ReportBean)this.getPageBean().getChildComponentBean(this.dependParentId,true)).isMasterReportOfMe(rbeanMaster,inherit);
    }
    




//        {//还没初始化此从报表的此变量
//            ReportBean masterReport=this.getPageBean().getReportChild(this.dependParentId,true);//得到当前从报表的主报表对象
//            if(masterReport.isSlaveReport())
//            {//主报表又是一个从报表




//            }



    
    public String getDependparams()
    {
        return dependparams;
    }

    public void setDependparams(String dependparams)
    {
        this.dependparams=dependparams;
    }

    public String[] getRefreshParentOnSave()
    {
        return refreshParentOnSave;
    }

    public void setRefreshParentOnSave(String[] refreshParentOnSave)
    {
        this.refreshParentOnSave=refreshParentOnSave;
    }

    public boolean isDisplayOnParentNoData()
    {
        return displayOnParentNoData;
    }

    public void setDisplayOnParentNoData(boolean displayOnParentNoData)
    {
        this.displayOnParentNoData=displayOnParentNoData;
    }

    public Map<String,Map<String,String>> getMDependChilds()
    {
        return mDependChilds;
    }

    public void setMDependChilds(Map<String,Map<String,String>> dependChilds)
    {
        mDependChilds=dependChilds;
    }

    public String getScrollheight()
    {
        return scrollheight;
    }

    public void setScrollheight(String scrollheight)
    {
        this.scrollheight=scrollheight;
    }

    public String getScrollwidth()
    {
        return scrollwidth;
    }

    public void setScrollwidth(String scrollwidth)
    {
        this.scrollwidth=scrollwidth;
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

    public IComponentConfigBean getConfigBeanWithValidParentTitle()
    {
        if(this.parenttitle!=null&&!this.parenttitle.trim().equals("")||this.title!=null&&!this.title.trim().equals("")) return this;
        return null;
    }

    public String getRefreshid()
    {
        return refreshid;
    }

    public void setRefreshid(String refreshid)
    {
        this.refreshid=refreshid;
    }

    public Set<String> getSRelateConditionReportids()
    {
        return sRelateConditionReportids;
    }

    public void setSRelateConditionReportids(Set<String> relateConditionReportids)
    {
        sRelateConditionReportids=relateConditionReportids;
    }

    public void addRelateConditionReportid(String reportid)
    {
        if(reportid==null||reportid.trim().equals("")) return;
        if(reportid.equals(id)) return;
        if(this.sRelateConditionReportids==null)
        {
            this.sRelateConditionReportids=new HashSet<String>();
        }
        this.sRelateConditionReportids.add(reportid);
    }

    public List<String> getLstDataImportFileNames()
    {
        return lstDataImportFileNames;
    }

    public void setLstDataImportFileNames(List<String> lstDataImportFileNames)
    {
        this.lstDataImportFileNames=lstDataImportFileNames;
    }
    
    public List<AbsDataImportConfigBean> getLstDataImportItems()
    {
        return lstDataImportItems;
    }

    public void setLstDataImportItems(List<AbsDataImportConfigBean> lstDataImportItems)
    {
        lstDataImportFileNames=new ArrayList<String>();
        if(lstDataImportItems!=null&&lstDataImportItems.size()>0)
        {
            String fileuploadpath=null;
            for(AbsDataImportConfigBean dataImportTmp:lstDataImportItems)
            {
                if(!lstDataImportFileNames.contains(dataImportTmp.getFilename())) lstDataImportFileNames.add(dataImportTmp.getFilename());
                if(fileuploadpath==null)
                {
                    fileuploadpath=dataImportTmp.getFilepath();
                }else if(!fileuploadpath.equals(dataImportTmp.getFilepath()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getPath()+"失败，被同一个报表引用的所有数据导入项"+dataImportTmp.getReskey()+"的导入路径必须一致");
                }
            }
        }
        this.lstDataImportItems=lstDataImportItems;
    }

    public String getBorder()
    {
        return border;
    }

    public void setBorder(String border)
    {
        this.border=border;
    }

    public String getBordercolor()
    {
        return bordercolor;
    }

    public void setBordercolor(String bordercolor)
    {
        this.bordercolor=bordercolor;
    }

    public ButtonsBean getButtonsBean()
    {
        return buttonsBean;
    }

    public void setButtonsBean(ButtonsBean buttonsBean)
    {
        this.buttonsBean=buttonsBean;
    }

    public String getDisplayWidth()
    {
        if(scrollwidth!=null&&!scrollwidth.trim().equals(""))
        {
            return scrollwidth.trim();
        }
        return width;
    }

    public String getWidth()
    {
        return width;
    }

    public void setWidth(String width)
    {
        this.width=width;
    }

    public String getPrintwidth()
    {
        return printwidth;
    }

    public void setPrintwidth(String printwidth)
    {
        this.printwidth=printwidth;
    }

    public String getRefreshGuid()
    {
        if(this.refreshGuid==null)
        {//还没有根据refreshid生成，则生成
            this.refreshGuid=ComponentConfigLoadAssistant.getInstance().createComponentRefreshGuidByRefreshId(this.getPageBean(),this.id,this.refreshid);
        }
        return refreshGuid;
    }

    public void setId(String key)
    {
        this.id=key;
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

    public boolean isClasscache()
    {
        return blClasscache;
    }

    public void setBlClasscache(boolean blClasscache)
    {
        this.blClasscache=blClasscache;
    }

    public String getTitlealign()
    {
        return titlealign;
    }

    public void setTitlealign(String titlealign)
    {
        titlealign=titlealign.trim().toLowerCase();
        if(!titlealign.equals("left")&&!titlealign.equals("center")&&!titlealign.equals("right"))
        {
            titlealign="left";
        }
        this.titlealign=titlealign;
    }

    public String getClasscache()
    {
        return classcache;
    }

    public void setClasscache(String classcache)
    {
        this.classcache=classcache;
    }

    public void setStrclass(String strclass)
    {
        this.strclass=strclass;
    }

    public Class getPojoclass()
    {
        return pojoclass;
    }

    public void setPojoclass(Class pojoclass)
    {
        this.pojoclass=pojoclass;
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

    public void setDbean(DisplayBean dbean)
    {
        this.dbean=dbean;
    }

    public void setSbean(SqlBean sbean)
    {
        this.sbean=sbean;
    }

    public String getId()
    {
        return this.id;
    }

    public int getDataimportwidth()
    {
        return dataimportwidth;
    }

    public void setDataimportwidth(int dataimportwidth)
    {
        this.dataimportwidth=dataimportwidth;
    }

    public int getDataimportheight()
    {
        return dataimportheight;
    }

    public void setDataimportheight(int dataimportheight)
    {
        this.dataimportheight=dataimportheight;
    }

    public String getDataimportinitsize()
    {
        return dataimportinitsize;
    }

    public void setDataimportinitsize(String dataimportinitsize)
    {
        this.dataimportinitsize=dataimportinitsize;
    }

    public String getDataimportmaxbtn()
    {
        return dataimportmaxbtn;
    }

    public void setDataimportmaxbtn(String dataimportmaxbtn)
    {
        this.dataimportmaxbtn=dataimportmaxbtn;
    }

    public String getDataimportminbtn()
    {
        return dataimportminbtn;
    }

    public void setDataimportminbtn(String dataimportminbtn)
    {
        this.dataimportminbtn=dataimportminbtn;
    }

    public String getStrclass()
    {
        return this.strclass;
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
    
    public DisplayBean getDbean()
    {
        return this.dbean;
    }

    public SqlBean getSbean()
    {
        return this.sbean;
    }

    public String getType()
    {
        return type;
    }

    public int getCelldrag()
    {
        return celldrag;
    }

    public void setCelldrag(int celldrag)
    {
        this.celldrag=celldrag;
    }

    public void setType(String type)
    {
        this.type=type;
    }

    public IInterceptor getInterceptor()
    {
        return interceptor;
    }

    public void setInterceptor(IInterceptor interceptor)
    {
        this.interceptor=interceptor;
    }

    public String getDynTplPath()
    {
        return dynTplPath;
    }

    public void setDynTplPath(String dynTplPath)
    {
        this.dynTplPath=dynTplPath;
    }

    public TemplateBean getTplBean()
    {
        return tplBean;
    }

    public void setTplBean(TemplateBean tplBean)
    {
        this.tplBean=tplBean;
    }

    public PDFExportBean getPdfPrintBean()
    {
        return pdfPrintBean;
    }

    public void setPdfPrintBean(PDFExportBean pdfPrintBean)
    {
        this.pdfPrintBean=pdfPrintBean;
    }
    
    public DataExportsConfigBean getDataExportsBean()
    {
        return dataExportsBean;
    }

    public void setDataExportsBean(DataExportsConfigBean dataExportsBean)
    {
        this.dataExportsBean=dataExportsBean;
    }

    public XmlElementBean getEleReportBean()
    {
        return eleReportBean;
    }

    public void setEleReportBean(XmlElementBean eleReportBean)
    {
        this.eleReportBean=eleReportBean;
    }

    public List<Class> getLstFormatClasses()
    {
        return lstFormatClasses;
    }

    public void setLstFormatClasses(List<Class> lstFormatClasses)
    {
        this.lstFormatClasses=lstFormatClasses;
    }

    public int getCellresize()
    {
        return cellresize;
    }

    public void setCellresize(int cellresize)
    {
        this.cellresize=cellresize;
    }

    public int getJsvalidatetype()
    {
        return jsvalidatetype;
    }

    public void setJsvalidatetype(int jsvalidatetype)
    {
        if(jsvalidatetype!=0&&jsvalidatetype!=1)
        {
            jsvalidatetype=1;
        }
        this.jsvalidatetype=jsvalidatetype;
    }

    public String getRefreshSlaveReportsCallBackMethodName()
    {
        if(this.mDependChilds!=null&&this.mDependChilds.size()>0)
        {
            return this.getGuid()+"_loadSlaveReports";
        }
        return "''";
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

    public void setLstOnloadMethods(List<OnloadMethodBean> lstOnloadMethods)
    {
        this.lstOnloadMethods=lstOnloadMethods;
    }

    public String getOnloadMethodName()
    {
        if(lstOnloadMethods==null||lstOnloadMethods.size()==0) return "''";
        return this.getGuid()+"_onload";
    }

    public String getValidateSearchMethod()
    {
        return validateSearchMethod;
    }

    public void setValidateSearchMethod(String validateSearchMethod)
    {
        this.validateSearchMethod=validateSearchMethod;
    }

    public List<SubmitFunctionParamBean> getLstSearchFunctionDynParams()
    {
        return lstSearchFunctionDynParams;
    }

    public void setLstSearchFunctionDynParams(List<SubmitFunctionParamBean> lstSearchFunctionDynParams)
    {
        this.lstSearchFunctionDynParams=lstSearchFunctionDynParams;
    }

    public FormatBean getFbean()
    {
        return fbean;
    }

    public void setFbean(FormatBean fbean)
    {
        this.fbean=fbean;
    }

    public List<String> getUlstMyJavascript()
    {
        return ulstMyJavascript;
    }

    public void setUlstMyJavascript(List<String> ulstMyJavascript)
    {
        this.ulstMyJavascript=ulstMyJavascript;
    }

    public List<String> getUlstMyCss()
    {
        return ulstMyCss;
    }

    public void setUlstMyCss(List<String> ulstMyCss)
    {
        this.ulstMyCss=ulstMyCss;
    }

    public void addMyJavascript(String javascript)
    {
        if(ulstMyJavascript==null) ulstMyJavascript=new UniqueArrayList<String>();
        ulstMyJavascript.add(javascript);
    }

    public void addMyCss(String css)
    {
        if(ulstMyCss==null) ulstMyCss=new UniqueArrayList<String>();
        ulstMyCss.add(css);
    }

    public List<Integer> getLstPagesize()
    {
        return lstPagesize;
    }

    public void setLstPagesize(List<Integer> lstPagesize)
    {
        this.lstPagesize=lstPagesize;
    }

    public void setLstTextBoxesWithTypePrompt(List<TextBox> lstTextBoxesWithTypePrompt)
    {
        this.lstTextBoxesWithTypePrompt=lstTextBoxesWithTypePrompt;
    }

    public Object getNavigateObj()
    {
        return navigateObj;
    }

    public void setNavigateObj(Object navigateObj)
    {
        if(ComponentConfigLoadManager.isValidNavigateObj(this,navigateObj)) this.navigateObj=navigateObj;
    }

    public void setMTextBoxesWithTypePrompt(Map<String,TextBox> textBoxesWithTypePrompt)
    {
        mTextBoxesWithTypePrompt=textBoxesWithTypePrompt;
    }

    public void addTextBoxWithingTypePrompt(TextBox textBoxObj)
    {
        if(lstTextBoxesWithTypePrompt==null)
        {
            lstTextBoxesWithTypePrompt=new ArrayList<TextBox>();
        }
        lstTextBoxesWithTypePrompt.add(textBoxObj);
    }

    public TextBox getTextBoxWithingTypePrompt(String inputboxid)
    {
        if(mTextBoxesWithTypePrompt==null) return null;
        return mTextBoxesWithTypePrompt.get(inputboxid);
    }
    
    public void setMSelectBoxesInConditionWithRelate(Map<String,SelectBox> selectBoxesInConditionWithRelate)
    {
        mSelectBoxesInConditionWithRelate=selectBoxesInConditionWithRelate;
    }

    public void setMSelectBoxesInColWithRelate(Map<String,SelectBox> selectBoxesInColWithRelate)
    {
        mSelectBoxesInColWithRelate=selectBoxesInColWithRelate;
    }

    public void addSelectBoxWithRelate(SelectBox selectBoxObj)
    {
        if(selectBoxObj.getOwner() instanceof ConditionBean)
        {
            if(mSelectBoxesInConditionWithRelate==null) mSelectBoxesInConditionWithRelate=new HashMap<String,SelectBox>();
            mSelectBoxesInConditionWithRelate.put(selectBoxObj.getOwner().getInputBoxId(),selectBoxObj);
        }else
        {
            if(mSelectBoxesInColWithRelate==null) mSelectBoxesInColWithRelate=new HashMap<String,SelectBox>();
            mSelectBoxesInColWithRelate.put(selectBoxObj.getOwner().getInputBoxId(),selectBoxObj);
        }
    }

    public SelectBox getChildSelectBoxInConditionById(String inputboxid)
    {
        if(mSelectBoxesInConditionWithRelate==null) return null;
        return this.mSelectBoxesInConditionWithRelate.get(inputboxid);
    }
    
    public SelectBox getChildSelectBoxInColById(String inputboxid)
    {
        if(mSelectBoxesInColWithRelate==null) return null;
        return this.mSelectBoxesInColWithRelate.get(inputboxid);
    }
    
    public void setLstUploadFileBoxes(List<FileBox> lstUploadFileBoxes)
    {
        this.lstUploadFileBoxes=lstUploadFileBoxes;
    }

    public void setMUploadFileBoxes(Map<String,FileBox> uploadFileBoxes)
    {
        mUploadFileBoxes=uploadFileBoxes;
    }

    public void addUploadFileBox(FileBox fileBoxObj)
    {
        if(this.lstUploadFileBoxes==null) this.lstUploadFileBoxes=new ArrayList<FileBox>();
        this.lstUploadFileBoxes.add(fileBoxObj);
    }

    public FileBox getUploadFileBox(String inputboxid)
    {
        if(this.mUploadFileBoxes==null) return null;
        return this.mUploadFileBoxes.get(inputboxid);
    }

    public void setLstPopUpBoxes(List<PopUpBox> lstPopUpBoxes)
    {
        this.lstPopUpBoxes=lstPopUpBoxes;
    }

    public void setMPopUpBoxes(Map<String,PopUpBox> popUpBoxes)
    {
        mPopUpBoxes=popUpBoxes;
    }

    public void addPopUpBox(PopUpBox popboxObj)
    {
        if(this.lstPopUpBoxes==null) this.lstPopUpBoxes=new ArrayList<PopUpBox>();
        this.lstPopUpBoxes.add(popboxObj);
    }

    public PopUpBox getPopUpBox(String inputboxid)
    {
        if(this.mPopUpBoxes==null) return null;
        return this.mPopUpBoxes.get(inputboxid);
    }

    public Set<String> getSParamNamesFromURL()
    {
        return sParamNamesFromURL;
    }

    public void setSParamNamesFromURL(Set<String> paramNamesFromURL)
    {
        sParamNamesFromURL=paramNamesFromURL;
    }

    public void addParamNameFromURL(String paramname)
    {
        if(sParamNamesFromURL==null)
        {
            sParamNamesFromURL=new HashSet<String>();
        }
        sParamNamesFromURL.add(paramname);
    }

    public void setLstInputboxesWithAutoComplete(List<AbsInputBox> lstInputboxesWithAutoComplete)
    {
        this.lstInputboxesWithAutoComplete=lstInputboxesWithAutoComplete;
    }

    public void setMInputboxesWithAutoComplete(Map<String,AbsInputBox> inputboxesWithAutoComplete)
    {
        mInputboxesWithAutoComplete=inputboxesWithAutoComplete;
    }

    public void addInputboxWithAutoComplete(AbsInputBox boxObj)
    {
        if(this.lstInputboxesWithAutoComplete==null) this.lstInputboxesWithAutoComplete=new ArrayList<AbsInputBox>();
        this.lstInputboxesWithAutoComplete.add(boxObj);
    }
    
    public AbsInputBox getInputboxWithAutoComplete(String inputboxid)
    {
        if(this.mInputboxesWithAutoComplete==null) return null;
        return this.mInputboxesWithAutoComplete.get(inputboxid);
    }
    
    public String getNavigate_reportid()
    {
        return navigate_reportid;
    }

    public void setNavigate_reportid(String navigate_reportid)
    {
        this.navigate_reportid=navigate_reportid;
    }
    
    public IReportPersonalizePersistence getPersonalizeObj()
    {
        return personalizeObj;
    }

    public void setPersonalizeObj(IReportPersonalizePersistence personalizeObj)
    {
        this.personalizeObj=personalizeObj;
    }

    public void setPrintBean(AbsPrintProviderConfigBean printBean)
    {
        this.printBean=printBean;
    }

    public AbsPrintProviderConfigBean getPrintBean()
    {
        return this.printBean;
    }

    public boolean isPageSplitReport()
    {
        if(this.lstPagesize.size()>1||this.lstPagesize.get(0)>0) return true;
        
        //对于列表报表，不存在pagesize为0的情况，因为在加载配置文件时已经将它替换成10，对于细览报表，则pagesize为0时，表示只取满足条件的第一条记录，相当于不分页显示
        return false;
    }
    
    public boolean isListReportType()
    {
        return Config.getInstance().getReportType(this.getType()) instanceof AbsListReportType;
    }
    
    public boolean isDetailReportType()
    {
        return Config.getInstance().getReportType(this.getType()) instanceof AbsDetailReportType;
    }
    
    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer)
    {
        Class c_temp=Config.getInstance().getReportType(this.type).getClass();
        if(c_temp==null)
        {
            throw new WabacusRuntimeException("配置的报表类型"+this.type+"没有在wabacus.cfg.xml文件中声明");
        }
        return (AbsReportType)AbsComponentType.createComponentTypeObj(c_temp,parentContainer,this,rrequest);
    }
    
    public Class getFormatMethodClass(String methodname,Class[] paramsType)
    {
        List<Class> lstFormatClasses=new ArrayList<Class>();
        if(this.lstFormatClasses!=null)
        {
            lstFormatClasses.addAll(this.lstFormatClasses);
        }
        if(Config.getInstance().getLstFormatClasses()!=null)
        {
            lstFormatClasses.addAll(Config.getInstance().getLstFormatClasses());
        }
        if(lstFormatClasses.size()==0) throw new WabacusConfigLoadingException("没有在配置文件中配置格式化类，无法调用格式化方法"+methodname);
        Class myformatClass=null;
        Method methodTmp;
        for(Class cTmp:lstFormatClasses)
        {
            try
            {
                methodTmp=cTmp.getMethod(methodname,paramsType);
            }catch(NoSuchMethodException nse)
            {
                continue;
            }
            if(methodTmp!=null)
            {
                myformatClass=cTmp;
                break;
            }
        }
        if(myformatClass==null)
        {
            throw new WabacusConfigLoadingException("没有找到格式化方法："+methodname+"");
        }
        return myformatClass;
    }
    
    public void doPostLoad()
    {
        if(this.refreshid==null||this.refreshid.trim().equals("")) this.refreshid=this.id;
        processNavigateReportid();
        if(this.mSelectBoxesInConditionWithRelate!=null&&this.mSelectBoxesInConditionWithRelate.size()>0)
        {
            for(Entry<String,SelectBox> entryTmp:this.mSelectBoxesInConditionWithRelate.entrySet())
            {
                entryTmp.getValue().processRelateInputboxByReportBean();
            }
        }
        if(this.sbean!=null) sbean.doPostLoad();
        if(this.dataExportsBean!=null) this.dataExportsBean.doPostLoad();
        if(this.printBean!=null) this.printBean.doPostLoad();
        if(pdfPrintBean!=null) pdfPrintBean.doPostLoad();
        if(lstTextBoxesWithTypePrompt!=null&&lstTextBoxesWithTypePrompt.size()>0)
        {
            mTextBoxesWithTypePrompt=new HashMap<String,TextBox>();
            for(int i=0;i<lstTextBoxesWithTypePrompt.size();i++)
            {
                mTextBoxesWithTypePrompt.put(lstTextBoxesWithTypePrompt.get(i).getOwner().getInputBoxId(),lstTextBoxesWithTypePrompt.get(i));
            }
        }
        lstTextBoxesWithTypePrompt=null;
        if(this.lstUploadFileBoxes!=null&&this.lstUploadFileBoxes.size()>0)
        {
            this.mUploadFileBoxes=new HashMap<String,FileBox>();
            for(FileBox boxObj:this.lstUploadFileBoxes)
            {
                this.mUploadFileBoxes.put(boxObj.getOwner().getInputBoxId(),boxObj);
            }
        }
        this.lstUploadFileBoxes=null;
        if(this.lstPopUpBoxes!=null&&this.lstPopUpBoxes.size()>0)
        {
            this.mPopUpBoxes=new HashMap<String,PopUpBox>();
            for(PopUpBox boxObj:this.lstPopUpBoxes)
            {
                this.mPopUpBoxes.put(boxObj.getOwner().getInputBoxId(),boxObj);
            }
        }
        this.lstPopUpBoxes=null;
        checkAndAddButtons();
        Class pojoclass=null;
        if(this.strclass==null||this.strclass.trim().equals(""))
        {//开发人员没有自己开发存放数据的POJO，则自动生成一个
            ReportAssistant.getInstance().buildReportPOJOClass(this,true);
            pojoclass=this.pojoclass;
        }else
        {
            try
            {
                pojoclass=Class.forName(strclass);
            }catch(ClassNotFoundException e)
            {
               throw new WabacusConfigLoadingException("没有找到报表"+this.getPath()+"配置的类"+strclass,e);
            }
        }
        ReportAssistant.getInstance().setMethodInfoToColBean(dbean,pojoclass);//设置每列colbean对应的POJO类的get/set方法
        IReportType reportObj=Config.getInstance().getReportType(this.getType());
        if(reportObj instanceof IAfterConfigLoadForReportType)
        {
            ((IAfterConfigLoadForReportType)reportObj).doPostLoad(this);
        }
        if(this.mSelectBoxesInColWithRelate!=null&&this.mSelectBoxesInColWithRelate.size()>0)
        {
            for(Entry<String,SelectBox> entryTmp:this.mSelectBoxesInColWithRelate.entrySet())
            {
                entryTmp.getValue().processRelateInputboxByReportBean();
            }
        }
        
        if(this.dbean!=null&&this.dbean.getLstCols()!=null)
        {
            List<ColBean> lstCols=this.dbean.getLstCols();
            for(ColBean cbean:lstCols)
            {
                if(cbean==null) continue;
                cbean.doPostLoad();
            }
        }
        if(this.lstInputboxesWithAutoComplete!=null&&this.lstInputboxesWithAutoComplete.size()>0)
        {
            this.mInputboxesWithAutoComplete=new HashMap<String,AbsInputBox>();
            for(AbsInputBox boxObjTmp:this.lstInputboxesWithAutoComplete)
            {
                this.mInputboxesWithAutoComplete.put(boxObjTmp.getOwner().getInputBoxId(),boxObjTmp);
            }
            this.lstInputboxesWithAutoComplete=null;
        }
        if(this.buttonsBean!=null) this.buttonsBean.sortButtons();
        ComponentConfigLoadAssistant.getInstance().validateApplicationRefreshid(this);
        if(this.refreshid==null||this.refreshid.trim().equals("")) this.refreshid=this.id;
        
        if(this.ulstMyCss!=null)
        {
            getPageBean().addMyCss(ulstMyCss);
            this.ulstMyCss=null;
        }
        if(this.ulstMyJavascript!=null)
        {
            getPageBean().addMyJavascript(ulstMyJavascript);
            this.ulstMyJavascript=null;
        }
        
        JavaScriptAssistant.getInstance().createComponentOnloadScript(this);
        JavaScriptAssistant.getInstance().createSearchValidateEvent(this);//创建搜索事件的校验函数
        this.fbean=null;
    }
    
    private void processNavigateReportid()
    {
        if(this.navigate_reportid==null||this.navigate_reportid.trim().equals("")) this.navigate_reportid=this.id;
        if(!this.isPageSplitReport()) return;
        if(this.id.equals(this.navigate_reportid)) return;
        ReportBean rbNavigate=this.getParentContainer().getReportChild(navigate_reportid,false);
        if(rbNavigate==null)
        {
            throw new WabacusConfigLoadingException("报表"+this.getPath()+"配置的navigate_reportid："+navigate_reportid+"对应的报表与它不在同一个容器的同一层级上，不能进行翻页导航栏关联");
        }
        if(this.getRefreshid()!=null&&!this.getRefreshid().trim().equals(""))
        {
            String refreshid=this.getPageBean().getCommonRefreshIdOfComponents(this.getRefreshid(),this.getParentContainer().getId());
            this.setRefreshid(refreshid);
        }else
        {
            this.setRefreshid(this.getParentContainer().getId());
        }
        if(rbNavigate.getRefreshid()!=null&&!rbNavigate.getRefreshid().trim().equals(""))
        {
            String refreshid=rbNavigate.getPageBean().getCommonRefreshIdOfComponents(rbNavigate.getRefreshid(),
                    rbNavigate.getParentContainer().getId());
            rbNavigate.setRefreshid(refreshid);
        }else
        {
            rbNavigate.setRefreshid(rbNavigate.getParentContainer().getId());
        }
    }
    
    private void checkAndAddButtons()
    {
        if(lstDataImportItems!=null&&lstDataImportItems.size()>0)
        {//配置了数据导入功能，则判断是否配置有数据导入按钮，如果没有，新增一个
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(this,DataImportButton.class,Consts.DATAIMPORT_BUTTON_DEFAULT);
        }
        if(sbean.isExistConditionWithInputbox(null))
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(this,SearchButton.class,Consts.SEARCH_BUTTON_DEFAULT);
        }
    }
    
    public IComponentConfigBean clone(AbsContainerConfigBean parentContainer)
    {
        return clone(this.id,parentContainer);
    }

    public IComponentConfigBean clone(String newid,AbsContainerConfigBean parentContainer)
    {
        ReportBean rbNew=(ReportBean)super.clone(null);
        rbNew.setId(newid);
        if(this.dataExportsBean!=null)
        {
            rbNew.setDataExportsBean(dataExportsBean.clone(rbNew));
        }
        if(this.printBean!=null)
        {
            AbsPrintProviderConfigBean newPrintBean=(AbsPrintProviderConfigBean)this.printBean.clone();
            newPrintBean.setOwner(rbNew);
            rbNew.setPrintBean(newPrintBean);
        }
        if(pdfPrintBean!=null)
        {
            rbNew.setPdfPrintBean((PDFExportBean)pdfPrintBean.clone(rbNew)); 
        }
        if(lstPagesize!=null)
        {
            rbNew.setLstPagesize((List<Integer>)((ArrayList<Integer>)this.lstPagesize).clone());
        }
        if(this.mDynTitleParts!=null)
        {
            rbNew.setMDynTitleParts((Map<String,String>)((HashMap<String,String>)this.mDynTitleParts).clone());
        }
        if(this.mDynSubtitleParts!=null)
        {
            rbNew.setMDynSubtitleParts((Map<String,String>)((HashMap<String,String>)this.mDynSubtitleParts).clone());
        }
        if(this.mDynParenttitleParts!=null)
        {
            rbNew.setMDynParenttitleParts((Map<String,String>)((HashMap<String,String>)this.mDynParenttitleParts).clone());
        }
        if(this.mDynParentSubtitleParts!=null)
        {
            rbNew.setMDynParentSubtitleParts((Map<String,String>)((HashMap<String,String>)this.mDynParentSubtitleParts).clone());
        }
        if(lstDataImportItems!=null)
        {
            rbNew.setLstDataImportItems((List<AbsDataImportConfigBean>)((ArrayList<AbsDataImportConfigBean>)lstDataImportItems).clone());
        }
        if(lstDataImportFileNames!=null)
        {
            rbNew.setLstDataImportFileNames((List<String>)((ArrayList<String>)lstDataImportFileNames).clone());
        }
        if(mTextBoxesWithTypePrompt!=null)
        {
            rbNew.setMTextBoxesWithTypePrompt(new HashMap<String,TextBox>());
        }
        if(lstTextBoxesWithTypePrompt!=null&&lstTextBoxesWithTypePrompt.size()>0)
        {
            rbNew.setLstTextBoxesWithTypePrompt(new ArrayList<TextBox>());
        }
        if(this.mSelectBoxesInColWithRelate!=null) rbNew.setMSelectBoxesInColWithRelate(new HashMap<String,SelectBox>());
        if(this.mSelectBoxesInConditionWithRelate!=null) rbNew.setMSelectBoxesInConditionWithRelate(new HashMap<String,SelectBox>());
        if(this.lstUploadFileBoxes!=null)
        {
            rbNew.setLstUploadFileBoxes(new ArrayList<FileBox>());
        }
        if(this.mUploadFileBoxes!=null)
        {
            rbNew.setMUploadFileBoxes(new HashMap<String,FileBox>());
        }
        if(this.lstPopUpBoxes!=null)
        {
            rbNew.setLstPopUpBoxes(new ArrayList<PopUpBox>());
        }
        if(this.mPopUpBoxes!=null)
        {
            rbNew.setMPopUpBoxes(new HashMap<String,PopUpBox>());
        }
        if(this.lstInputboxesWithAutoComplete!=null)
        {
            rbNew.setLstInputboxesWithAutoComplete(new ArrayList<AbsInputBox>());
        }
        if(this.mInputboxesWithAutoComplete!=null)
        {
            rbNew.setMInputboxesWithAutoComplete(new HashMap<String,AbsInputBox>());
        }
        if(this.sParamNamesFromURL!=null)
        {
            rbNew.setSParamNamesFromURL(new HashSet<String>());
        }        
        if(this.ulstMyJavascript!=null)
        {
            rbNew.setUlstMyJavascript((List<String>)((UniqueArrayList<String>)this.ulstMyJavascript).clone());
        }
        if(this.ulstMyCss!=null)
        {
            rbNew.setUlstMyCss((List<String>)((UniqueArrayList<String>)this.ulstMyCss).clone());
        }
        if(this.lstFormatClasses!=null)
        {
            rbNew.setLstFormatClasses((List)((ArrayList)this.lstFormatClasses).clone());
        }
        if(lstOnloadMethods!=null)
        {
            List<OnloadMethodBean> lstTmp=new ArrayList<OnloadMethodBean>();
            for(OnloadMethodBean beanTmp:lstOnloadMethods)
            {
                lstTmp.add((OnloadMethodBean)beanTmp.clone());
            }
            rbNew.setLstOnloadMethods(lstTmp);
        }
        if(this.dependParentId!=null&&!this.dependParentId.trim().equals(""))
        {
            rbNew.setDependParentId(null);
        }
        if(this.mDependChilds!=null&&this.mDependChilds.size()>0)
        {
            rbNew.setMDependChilds(null);
        }
        if(mDependsDetailReportParams!=null)
        {
            rbNew.setMDependsDetailReportParams(null);
        }
        if(this.refreshParentOnSave!=null)
        {
            rbNew.setRefreshParentOnSave(null);
        }
//        if(this.refreshParentOnDelete!=null)



        
        rbNew.setRefreshid(null);
        cloneExtendConfig(rbNew);//clone扩展配置项
        rbNew.setParentContainer(parentContainer);
        if(this.buttonsBean!=null) rbNew.setButtonsBean((ButtonsBean)this.buttonsBean.clone(rbNew));
        if(this.dbean!=null) rbNew.setDbean((DisplayBean)this.dbean.clone(rbNew));
        if(this.sbean!=null) rbNew.setSbean((SqlBean)this.sbean.clone(rbNew));
        if(this.fbean!=null) rbNew.setFbean((FormatBean)this.fbean.clone(rbNew));
        return rbNew;
    }
    
    public String toString()
    {
        return this.getPath();
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((this.getGuid()==null)?0:this.getGuid().hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final ReportBean other=(ReportBean)obj;
        if(this.getGuid()==null)
        {
            if(other.getGuid()!=null) return false;
        }else if(!this.getGuid().equals(other.getGuid())) return false;
        return true;
    }
}