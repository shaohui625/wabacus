package com.wabacus.extra.expr.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.jooq.SQLDialect;

import com.wabacus.system.JdbcConnection;

public class ExprJdbcConnection extends JdbcConnection {

    private SQLDialect sqlDialect;

    public ExprJdbcConnection(Connection conn, SQLDialect sqlDialect) {
        super(conn);
        this.sqlDialect = sqlDialect;
    }

//    private DSLContext factory;
//
//    public DSLContext getJooqFactory() {
//        factory = DSL.using(this.getNativeConnection(), sqlDialect);
//        return factory;
//    }

    @Override
    public void close() throws SQLException {
        super.close();
//        factory = null;
    }

}
