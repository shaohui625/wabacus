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
package com.wabacus.system.resultset;

import com.wabacus.system.resultset.GetAllResultSetByPreparedSQL;
import com.wabacus.system.resultset.GetAllResultSetBySQL;
import com.wabacus.system.resultset.GetPartResultSetByPreparedSQL;
import com.wabacus.system.resultset.GetPartResultSetBySQL;
import com.wabacus.system.resultset.GetResultSetByStoreProcedure;
import com.wabacus.system.resultset.ISQLType;

public interface ISQLTypeCreator
{

    ISQLTypeCreator PREPARED_STATEMENT_CREATOR=new ISQLTypeCreator()
    {

        public ISQLType createAllResultSetISQLType()
        {
            return new GetAllResultSetByPreparedSQL();
        }

        public ISQLType createPartResultSetISQLType()
        {
            return new GetPartResultSetByPreparedSQL();
        }

    };

    ISQLTypeCreator STATEMENT_CREATOR=new ISQLTypeCreator()
    {

        public ISQLType createAllResultSetISQLType()
        {
            return new GetAllResultSetBySQL();
        }

        public ISQLType createPartResultSetISQLType()
        {
            return new GetPartResultSetBySQL();
        }

    };

    ISQLTypeCreator STORED_PROCEDURE_CREATOR=new ISQLTypeCreator()
    {

        public ISQLType createAllResultSetISQLType()
        {
            return new GetResultSetByStoreProcedure();
        }

        public ISQLType createPartResultSetISQLType()
        {
            return new GetResultSetByStoreProcedure();
        }

    };

    public ISQLType createAllResultSetISQLType();

    public ISQLType createPartResultSetISQLType();

}
