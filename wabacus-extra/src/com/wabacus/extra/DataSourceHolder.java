package com.wabacus.extra;

import javax.sql.DataSource;

public final class DataSourceHolder {

	private DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public DataSourceHolder(DataSource dataSource) {
		super();
		this.dataSource = dataSource;
	}

	public void close() {
		dataSource = null;
	}
}
