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
package com.wabacus.system.dataset;

import com.wabacus.system.dataset.sqldataset.GetAllDataSetByPreparedSQL;
import com.wabacus.system.dataset.sqldataset.GetAllDataSetBySQL;
import com.wabacus.system.dataset.sqldataset.GetDataSetByStoreProcedure;
import com.wabacus.system.dataset.sqldataset.GetPartDataSetByPreparedSQL;
import com.wabacus.system.dataset.sqldataset.GetPartDataSetBySQL;

/**
 * 
 * 数据集生成器的创建器,用于屏蔽不同类型的数据集获取方式(PreparedStatement,Statement,Procedue,...)实现上的差异
 * @author qxo(qxodream@gmail.com)
 *
 */
public interface ISqlDataSetCreator
{

    /**
     * 面向PreparedStatement的DataSetCreator
     */
    ISqlDataSetCreator PREPARED_STATEMENT_CREATOR=new ISqlDataSetCreator()
    {

        public ISqlDataSet createAllDataSet()
        {
            return new GetAllDataSetByPreparedSQL();
        }

        public ISqlDataSet createPartDataSet()
        {
            return new GetPartDataSetByPreparedSQL();
        }

    };

    /**
     * 面向Statement的DataSetCreator
     */
    ISqlDataSetCreator STATEMENT_CREATOR=new ISqlDataSetCreator()
    {

        public ISqlDataSet createAllDataSet()
        {
            return new GetAllDataSetBySQL();
        }

        public ISqlDataSet createPartDataSet()
        {
            return new GetPartDataSetBySQL();
        }

    };

    /**
     * 面向存储过程的DataSetCreator
     */
    ISqlDataSetCreator STORED_PROCEDURE_CREATOR=new ISqlDataSetCreator()
    {

        public ISqlDataSet createAllDataSet()
        {
            GetDataSetByStoreProcedure ret=new GetDataSetByStoreProcedure();
            ret.setLoadAllData(true);
            return ret;
        }

        public ISqlDataSet createPartDataSet()
        {
            return new GetDataSetByStoreProcedure();
        }

    };

    /**
     * @return 返回获取所有结果集的ISqlDataSet
     */
    public ISqlDataSet createAllDataSet();

    /**
     * @return 返回获取所有结果集的ISqlDataSet
     */
    public ISqlDataSet createPartDataSet();

}