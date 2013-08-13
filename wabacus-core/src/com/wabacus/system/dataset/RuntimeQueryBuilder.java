/* 
 * Copyright (C) 2010-2012 吴卫华(wuweihua)<349446658@qq.com>
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
package com.wabacus.system.dataset;

import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.datatype.IDataType;

/**
 * 
 *  运行时查询构建器
 * @author qxo(qxodream@gmail.com)
 *
 */
public interface RuntimeQueryBuilder
{

    RuntimeQueryBuilder DUMMY=new RuntimeQueryBuilder()
    {
        public String parseRuntimeSqlAndCondition(final ReportRequest rrequest,final ReportBean rbean,final ReportDataSetValueBean svbean,
                final String sql,final List<String> lstConditionValues,final List<IDataType> lstConditionTypes)
        {
            return sql;
        }
    };

    /**
     * 
     * @return
     */
    public String parseRuntimeSqlAndCondition(final ReportRequest rrequest,final ReportBean rbean,final ReportDataSetValueBean svbean,String sql,
            final List<String> lstConditionValues,final List<IDataType> lstConditionTypes);

}