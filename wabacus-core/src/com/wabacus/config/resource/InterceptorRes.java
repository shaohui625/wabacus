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
package com.wabacus.config.resource;

import java.util.List;

import org.dom4j.Element;

import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.intercept.IInterceptor;

public class InterceptorRes extends AbsResource
{
    public Object getValue(Element itemElement)
    {
        if(itemElement==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中没有配置拦截器资源项");
        }
        String name=itemElement.attributeValue("key");
        Element eleInterceptor=itemElement.element("interceptor");
        if(eleInterceptor==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中配置的资源项"+itemElement.attributeValue("key")+"不是有效的拦截器资源项，必须以<interceptor/>做为其顶层标签");
        }
        List<String> lstImportPackages=ConfigLoadAssistant.getInstance().loadImportsConfig(eleInterceptor);
        Element elePreAction=eleInterceptor.element("preaction");
        String preaction=null;
        if(elePreAction!=null)
        {
            preaction=elePreAction.getText();
        }
        preaction=preaction==null?"":preaction.trim();
        Element elePostAction=eleInterceptor.element("postaction");
        String postaction=null;
        if(elePostAction!=null)
        {
            postaction=elePostAction.getText();
        }
        postaction=postaction==null?"":postaction.trim();

        Element eleBeforeSave=eleInterceptor.element("beforesave");
        String beforesave=null;
        if(eleBeforeSave!=null)
        {
            beforesave=eleBeforeSave.getText();
        }
        beforesave=beforesave==null?"":beforesave.trim();

        Element eleBeforeSavePerRow=eleInterceptor.element("beforesave-perrow");
        String beforesavePerrow=null;
        if(eleBeforeSavePerRow!=null)
        {
            beforesavePerrow=eleBeforeSavePerRow.getText();
        }
        beforesavePerrow=beforesavePerrow==null?"":beforesavePerrow.trim();

        Element eleBeforeSavePerSql=eleInterceptor.element("beforesave-persql");
        String beforesavePersql=null;
        if(eleBeforeSavePerSql!=null)
        {
            beforesavePersql=eleBeforeSavePerSql.getText();
        }
        beforesavePersql=beforesavePersql==null?"":beforesavePersql.trim();

        Element eleAfterSavePerSql=eleInterceptor.element("aftersave-persql");
        String aftersavePersql=null;
        if(eleAfterSavePerSql!=null)
        {
            aftersavePersql=eleAfterSavePerSql.getText();
        }
        aftersavePersql=aftersavePersql==null?"":aftersavePersql.trim();

        Element eleAfterSavePerRow=eleInterceptor.element("aftersave-perrow");
        String aftersavePerrow=null;
        if(eleAfterSavePerRow!=null)
        {
            aftersavePerrow=eleAfterSavePerRow.getText();
        }
        aftersavePerrow=aftersavePerrow==null?"":aftersavePerrow.trim();

        Element eleAfterSave=eleInterceptor.element("aftersave");
        String aftersave=null;
        if(eleAfterSave!=null)
        {
            aftersave=eleAfterSave.getText();
        }
        aftersave=aftersave==null?"":aftersave.trim();

        Element eleBeforeLoadData=eleInterceptor.element("beforeloaddata");
        String beforeloaddata=null;
        if(eleBeforeLoadData!=null)
        {
            beforeloaddata=eleBeforeLoadData.getText();
        }
        beforeloaddata=beforeloaddata==null?"":beforeloaddata.trim();

        Element eleAfterLoadData=eleInterceptor.element("afterloaddata");
        String afterloaddata=null;
        if(eleAfterLoadData!=null)
        {
            afterloaddata=eleAfterLoadData.getText();
        }
        afterloaddata=afterloaddata==null?"":afterloaddata.trim();

        Element eleDisplayPerRow=eleInterceptor.element("beforedisplay-perrow");
        String displayperrow=null;
        if(eleDisplayPerRow!=null)
        {
            displayperrow=eleDisplayPerRow.getText();
        }
        displayperrow=displayperrow==null?"":displayperrow.trim();

        Element eleDisplayPerCol=eleInterceptor.element("beforedisplay-percol");
        String displaypercol=null;
        if(eleDisplayPerCol!=null)
        {
            displaypercol=eleDisplayPerCol.getText();
        }
        displaypercol=displaypercol==null?"":displaypercol.trim();

        if(preaction.equals("")&&postaction.equals("")&&beforesave.equals("")&&beforesavePerrow.equals("")&&beforesavePersql.equals("")
                &&aftersavePersql.equals("")&&aftersavePerrow.equals("")&&aftersave.equals("")&&beforeloaddata.equals("")&&displayperrow.equals("")
                &&displaypercol.equals(""))
        {
            return null;
        }
        Class c=ReportAssistant.getInstance().buildInterceptorClass("resource_"+name,lstImportPackages,preaction,postaction,beforesave,
                beforesavePerrow,beforesavePersql,aftersavePersql,aftersavePerrow,aftersave,beforeloaddata,afterloaddata,displayperrow,displaypercol);
        if(c!=null)
        {
            try
            {
                return (IInterceptor)c.newInstance();

            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("在资源文件中定义的拦截器类"+name+"无法实例化对象",e);
            }
        }
        return null;
    }
}
