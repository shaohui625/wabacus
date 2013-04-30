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
package com.wabacus.config.component.application.report.extendconfig;

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;

public class LoadExtendConfigManager
{
    public static void loadBeforeExtendConfigForReporttype(AbsConfigBean configbean,List<XmlElementBean> lstEleBeans)
    {
        loadExtendConfig(Config.getInstance().getReportType(configbean.getReportBean().getType()),
                configbean,lstEleBeans,true);
    }

    public static void loadAfterExtendConfigForReporttype(AbsConfigBean configbean,List<XmlElementBean> lstEleBeans)
    {
        loadExtendConfig(Config.getInstance().getReportType(configbean.getReportBean().getType()),
                configbean,lstEleBeans,false);
    }

    private static void loadExtendConfig(Object type,AbsConfigBean configbean,List<XmlElementBean> lstEleBeans,
            boolean isBefore)
    {
        int flag=0;
        if((type instanceof IReportExtendConfigLoad)&&(configbean instanceof ReportBean))
        {
            if(isBefore)
                flag=((IReportExtendConfigLoad)type).beforeReportLoading((ReportBean)configbean,
                        lstEleBeans);
            else
                flag=((IReportExtendConfigLoad)type).afterReportLoading((ReportBean)configbean,
                        lstEleBeans);
        }else if((type instanceof IColExtendConfigLoad)&&(configbean instanceof ColBean))
        {
            if(isBefore)
                flag=((IColExtendConfigLoad)type).beforeColLoading((ColBean)configbean,lstEleBeans);
            else
                flag=((IColExtendConfigLoad)type).afterColLoading((ColBean)configbean,lstEleBeans);
        }else if((type instanceof IDisplayExtendConfigLoad)&&(configbean instanceof DisplayBean))
        {
            if(isBefore)
                flag=((IDisplayExtendConfigLoad)type).beforeDisplayLoading((DisplayBean)configbean,
                        lstEleBeans);
            else
                flag=((IDisplayExtendConfigLoad)type).afterDisplayLoading((DisplayBean)configbean,
                        lstEleBeans);
        }else if((type instanceof IConditionExtendConfigLoad)
                &&(configbean instanceof ConditionBean))
        {
            if(isBefore)
                flag=((IConditionExtendConfigLoad)type).beforeConditionLoading(
                        (ConditionBean)configbean,lstEleBeans);
            else
                flag=((IConditionExtendConfigLoad)type).afterConditionLoading(
                        (ConditionBean)configbean,lstEleBeans);
        }else if((type instanceof ISqlExtendConfigLoad)&&(configbean instanceof SqlBean))
        {
            if(isBefore)
                flag=((ISqlExtendConfigLoad)type).beforeSqlLoading((SqlBean)configbean,lstEleBeans);
            else
                flag=((ISqlExtendConfigLoad)type).afterSqlLoading((SqlBean)configbean,lstEleBeans);
        }
        if(flag<0)
        {
            String pageid=configbean.getPageBean().getId();
            if(configbean.getReportBean()!=null)
            {
                pageid=pageid+"/"+configbean.getReportBean().getId();
            }
            throw new WabacusConfigLoadingException("加载报表："+pageid+"的特定配置失败");
        }

    }
}
