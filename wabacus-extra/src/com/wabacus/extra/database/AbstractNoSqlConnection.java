package com.wabacus.extra.database;

import java.sql.SQLException;

import com.wabacus.system.IConnection;

public abstract class AbstractNoSqlConnection implements IConnection {

	public boolean getAutoCommit() throws SQLException {
		return false;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setTransactionIsolation(int level) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void commit() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void close() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void rollback() throws SQLException {
		// TODO Auto-generated method stub

	}
}
