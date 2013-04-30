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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public class AbsListReportSqlBean extends AbsExtendConfigBean
{
    private String filterdata_sql;//获取某个字段过滤数据的SQL语句，只有某个报表一个或多个<col/>配置了filter属性时才会自动生成此SQL语句

    public AbsListReportSqlBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getFilterdata_sql()
    {
        return filterdata_sql;
    }

    public void setFilterdata_sql(String filterdata_sql)
    {
        this.filterdata_sql=filterdata_sql;
    }
}
