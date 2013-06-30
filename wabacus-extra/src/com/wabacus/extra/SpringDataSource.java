/*
 * Copyright (C) 2010-2012 星星<349446658@qq.com>
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
package com.wabacus.extra;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.dom4j.Element;

import com.wabacus.config.database.datasource.AbstractJdbcDataSource;

/**
 * 通过spring注入方式获取数据源
 * 
 * @version $Id: SpringDataSource.java 3658 2013-06-30 06:18:13Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-11-28
 */
public final class SpringDataSource extends AbstractJdbcDataSource {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wabacus.config.database.datasource.AbstractJdbcDataSource#getNativeConnection()
	 */
	@Override
	public Connection getConnection() {
		DataSource ds =  dataSourceName == null ? null : (DataSource)WabacusBeanFactory.getInstance().getBean(dataSourceName);
		if(ds == null){
			ds = dsHolder.getDataSource();
		}
		if (null == ds) {
			throw new IllegalArgumentException("初始化存在问题，需要在调用静态化初始化原始的数据源");
		}
		try {
			return ds.getConnection();
		} catch (SQLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private static DataSourceHolder dsHolder;

	public static DataSourceHolder setDataSource(DataSource datasource) {
		dsHolder = new DataSourceHolder(datasource);
		return dsHolder;
	}

	private String dataSourceName;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wabacus.config.database.datasource.AbsDataSource#loadConfig(org.dom4j.Element)
	 */
	@Override
	public void loadConfig(Element eleDataSource) {
		super.loadConfig(eleDataSource);
		
		dataSourceName = eleDataSource.attributeValue("dataSourceName");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wabacus.config.database.datasource.AbsDataSource#closePool()
	 */
	@Override
	public void closePool() {
		super.closePool();
		if (null == dsHolder) {
			// dsHolder.close();
			// dsHolder = null;
		}
	}

}
