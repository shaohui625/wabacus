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
package com.wabacus.config.database.datasource;

import java.sql.Connection;

import com.wabacus.system.IConnection;
import com.wabacus.system.JdbcConnection;

public abstract class AbstractJdbcDataSource extends AbsDataSource
{

    @Override
    public IConnection getIConnection()
    {
        return new JdbcConnection(this.getNativeConnection());
    }


    @Override
    public Connection getConnection()
    {
        return this.getNativeConnection();
    }

    /**
     *
     * @return native jdbc connnection
     */
    public abstract Connection getNativeConnection();

}
