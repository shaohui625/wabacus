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
package com.wabacus.system;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcConnection implements IConnection
{

    public void rollback() throws SQLException
    {
        conn.rollback();
    }

    private Connection conn;

    public void close() throws SQLException
    {
        conn.close();
    }

    public JdbcConnection(Connection conn)
    {
        super();
        this.conn=conn;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        conn.setAutoCommit(autoCommit);
    }

    public void setTransactionIsolation(int level) throws SQLException
    {
        conn.setTransactionIsolation(level);
    }

    public void commit() throws SQLException
    {
        conn.commit();
    }

    public Connection getNativeConnection(){
        return conn;
    }
}