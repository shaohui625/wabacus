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
package com.wabacus.system.dataimport.interceptor;

import java.io.File;
import java.sql.Connection;
import java.util.List;

import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;

public abstract class AbsDataImportInterceptorDefault implements IDataImportInterceptor
{

    public void afterImportRow(Connection conn,AbsDataImportConfigBean configBean,
            List lstColTitles,List lstColValues)
    {

    }

    public boolean beforeImportRow(Connection conn,AbsDataImportConfigBean configBean,
            List lstColTitles,List lstColValues)
    {
        return true;
    }

    public void doImportEnd(int statuscode,AbsDataImportConfigBean dibean,File dataFileObj,
            Exception e)
    {

    }

    public boolean doImportStart(AbsDataImportConfigBean dibean,File dataFileObj)
    {
        return true;
    }

}
