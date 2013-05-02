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
package com.wabacus.system.component.container.page;

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.buttons.BackButton;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class PageType extends AbsContainerType
{
    private List<String> lstCsses=null;

    private List<String> lstDynCsses=null;
    
    private List<String> lstJavascripts=null;
    
    private PageBean pagebean;

    public PageType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        this.pagebean=(PageBean)comCfgBean;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        rrequest.addParamToUrl("CSS","rrequest{CSS}",false);
        rrequest.addParamToUrl("JS","rrequest{JS}",false);
        super.initUrl(applicationConfigBean,rrequest);
    }
    
    public List<ReportBean> initDisplayOnPage()
    {
        this.lstCsses=ComponentAssistant.getInstance().initDisplayCss(rrequest);
        this.lstJavascripts=new UniqueArrayList<String>();
        String js=rrequest.getStringAttribute("JS","");
        if(!js.equals(""))
        {
            js=Tools.htmlEncode(js);
            String[] jsArray=Tools.parseStringToArray(js,",");
            for(int k=0;k<jsArray.length;k++)
            {
                if(jsArray[k]==null||jsArray[k].trim().equals("")) continue;
                jsArray[k]=Config.webroot+jsArray[k];
                jsArray[k]=Tools.replaceAll(jsArray[k],"//","/");
                lstJavascripts.add(jsArray[k]);
            }
        }
        if(lstJavascripts.size()==0&&pagebean.getUlstMyJavascript()!=null)
        {
            lstJavascripts.addAll(pagebean.getUlstMyJavascript());
        }
        List<String> lstSystemJs=pagebean.getUlstSystemJavascript();
        if(lstSystemJs!=null)
        {
            lstJavascripts.addAll(lstSystemJs);
        }
        return super.initDisplayOnPage();
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!rrequest.checkPermission(pagebean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        wresponse.println(showStartWebResources());
        wresponse.println("<span id=\"WX_CONTENT_"+pagebean.getGuid()+"\">");//顶层<page/>的内容必须用<span/>完整括住，这样更新页面时才能更新整个页面内容
        wresponse.println(showContainerStartPart());
        wresponse.println(showContainerTableTag());//显示容器最里层的<table>标签
        if(rrequest.checkPermission(pagebean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            IComponentType childObjTmp;
            for(String childIdTmp:lstChildrenIds)
            {
                wresponse.println("<tr>");
                childObjTmp=this.mChildren.get(childIdTmp);
                showChildObj(childObjTmp,null);
                wresponse.println("</tr>");
            }
        }
        String backbutton=showBackButtonInPage();
        if(!backbutton.trim().equals(""))
        {
            wresponse.println("<tr><td align=\"center\">");
            wresponse.println(backbutton);
            wresponse.println("</td></tr>");
        }
        wresponse.println("</table>");
        wresponse.println(showContainerEndPart());
        wresponse.println("<div id=\"wx_titletree_container\" style=\"display:none;\" class=\"titletree_container\">");
        wresponse.println("<div id=\"titletree_container_inner\" class=\"titletree_container_inner\">");
        wresponse
                .println("<div id=\"tree\" class=\"bbit-tree\"><div class=\"bbit-tree-bwrap\"><div class=\"bbit-tree-body\" id=\"wx_titletree_content\">");
        wresponse.println("</div></div></div></div>");
        wresponse.println("<div id=\"wx_titletree_buttoncontainer\" style=\"padding-top: 3px;padding-bottom:5px;text-align:center\"></div>");
        wresponse.println("</div>");
        wresponse.println("<div id=\"LOADING_IMG_ID\" class=\"cls-loading-img\"></div>");
        String pageurlspan="<span id=\""+pagebean.getId()+"_url_id\" style=\"display:none;\" value=\""
                +Tools.htmlEncode(Tools.jsParamEncode(rrequest.getUrl()))+"\"";
        if(pagebean.isShouldProvideEncodePageUrl())
        {
            pageurlspan=pageurlspan+" encodevalue=\""+Tools.convertBetweenStringAndAscii(rrequest.getUrl(),true)+"\"";
        }
        String ancestorUrls=rrequest.getStringAttribute("ancestorPageUrls","");
        if(!ancestorUrls.equals(""))
        {
            pageurlspan=pageurlspan+" ancestorPageUrls=\""+ancestorUrls+"\"";
        }
        wresponse.println(pageurlspan+"></span>");
        if(pagebean.getLstPrintBeans()!=null)
        {//此页面上有组件需要打印，初始化所有打印对象
            for(AbsPrintProviderConfigBean ppcbeanTmp:pagebean.getLstPrintBeans())
            {
                ppcbeanTmp.initPrint(rrequest);
            }
        }
        wresponse.println("</span>");
        wresponse.println(showEndWebResources());
    }
    
    private String showBackButtonInPage()
    {
        StringBuffer resultBuf=new StringBuffer();
        String clickevent=rrequest.getStringAttribute("BACK_ACTION_EVENT","");
        if(rrequest.getLstAncestorUrls()!=null&&rrequest.getLstAncestorUrls().size()>0&&clickevent.equals(""))
        {
            if(this.pagebean.getButtonsBean()!=null&&this.pagebean.getButtonsBean().getcertainTypeButton(BackButton.class)!=null)
            {
                return "";
            }
            BackButton buttonObj=(BackButton)Config.getInstance().getResourceButton(rrequest,rrequest.getPagebean(),Consts.BACK_BUTTON_DEFAULT,
                    BackButton.class);
            resultBuf.append("<table height='3'><tr><td>&nbsp;</td></tr></table>");
            resultBuf.append("<table width='100%' align='center'><tr><td align=\"center\">").append(buttonObj.showButton(rrequest,null)).append(
                    "</td></tr></table>");
        }
        return resultBuf.toString();
    }

    private static String systemheadjs="";
    static
    {
        if(Config.encode.toLowerCase().trim().equals("utf-8"))
        {
            systemheadjs="/webresources/script/wabacus_systemhead.js";
        }else
        {
            String encode=Config.encode;
            if(encode.trim().equalsIgnoreCase("gb2312"))
            {
                encode="gbk";
            }
            systemheadjs="/webresources/script/"+encode.toLowerCase()+"/wabacus_systemhead.js";
        }
        systemheadjs=Config.webroot+"/"+systemheadjs;
        systemheadjs=Tools.replaceAll(systemheadjs,"//","/");
    }
    
    private String showStartWebResources()
    {
        if(rrequest.isLoadedByAjax()||!rrequest.isDisplayOnPage()) return "";
        StringBuffer resultBuf=new StringBuffer();
        if(this.lstCsses!=null)
        {
            for(String cssTmp:this.lstCsses)
            {
                resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssTmp).append("\"/>");
            }
        }
        resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(Config.webroot).append(
                "webresources/skin/colselected_tree.css\"/>");
        resultBuf.append("<script language=\"javascript\" src=\"");
        resultBuf.append(systemheadjs).append("\"></script>");
        return resultBuf.toString();
    }
    
    public String showEndWebResources()
    {
//        if(!rrequest.isAccessFirstTime()) return "";//ajax加载的话，不显示静态资源，因为显示了也无效
        if(rrequest.isLoadedByAjax()||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        if(this.lstJavascripts!=null)
        {
            for(String jsTmp:this.lstJavascripts)
            {
                resultBuf.append("<script type=\"text/javascript\"  src=\"").append(jsTmp).append("\"></script>");
            }
        }
        if(lstDynCsses!=null)
        {//将运行时动态决定需要包含的css文件包含进来
            for(String cssTmp:lstDynCsses)
            {
                resultBuf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssTmp).append("\"/>");
            }
        }
        if(!rrequest.isLoadedByAjax())
        {
            String onloadMethods=rrequest.getWResponse().invokeOnloadMethodsFirstTime();
            if(onloadMethods!=null&&!onloadMethods.trim().equals(""))
            {
                resultBuf.append("<script type=\"text/javascript\">").append(onloadMethods).append("</script>");
            }
        }
        return resultBuf.toString();
    }
    
    public String getRealParenttitle()
    {
        return null;//因为<page/>是顶层容器，没有父容器
    }
    
    public void addDynCss(String css)
    {
        if(css==null||css.trim().equals("")) return;
        if(this.lstCsses!=null&&this.lstCsses.contains(css)) return;
        if(this.lstDynCsses==null) this.lstDynCsses=new UniqueArrayList<String>();
        this.lstDynCsses.add(css);
    }
    
    public void addDynJs(String js)
    {
        if(js==null||js.trim().equals("")) return;
        if(this.lstJavascripts==null) this.lstJavascripts=new UniqueArrayList<String>();
        this.lstJavascripts.add(js);
    }
    
    public AbsContainerConfigBean loadConfig(XmlElementBean eleContainer,AbsContainerConfigBean parent,String tagname)
    {
        return null;
    }
    
    protected String getComponentTypeName()
    {
        return "container.page";
    }
}