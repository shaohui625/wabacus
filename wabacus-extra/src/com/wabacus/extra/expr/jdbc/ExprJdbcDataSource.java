package com.wabacus.extra.expr.jdbc;

import java.sql.Connection;
import java.util.List;

import org.dom4j.Element;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wabacus.config.Config;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.config.database.datasource.AbstractJdbcDataSource;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.IConnection;

public class ExprJdbcDataSource extends AbsDataSource {
    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(ExprJdbcDataSource.class);

    private AbstractJdbcDataSource target;

    private String databaseType = "MYSQL";

    @Override
    public void loadConfig(Element eleDataSource) {
        super.loadConfig(eleDataSource);

        List lstEleProperties = eleDataSource.elements("property");

        if (lstEleProperties == null || lstEleProperties.size() == 0) {
            throw new WabacusConfigLoadingException("没有为数据源：" + this.getName() + "配置alias、configfile等参数");
        }

        for (int i = 0; i < lstEleProperties.size(); i++) {
            final Element eleChild = (Element) lstEleProperties.get(i);
            String name = eleChild.attributeValue("name");
            name = name == null ? "" : name.trim();

            String value = this.getOverridePropertyValue(name, eleChild.getText());

            value = value == null ? "" : value.trim();
            if (value.equals("")) {
                continue;
            }
            if (name.equals("target")) {
                target = (AbstractJdbcDataSource) Config.getInstance().getDataSource(value);
            } else if (name.equals("databaseType")) {
                databaseType = value.toUpperCase();
            }
        }

        ExprJdbcDatabaseType dbType = (ExprJdbcDatabaseType) this.getDbType();
        dbType.setTargetDbType(this.target.getDbType());
    }

    @Override
    public IConnection getIConnection() {
        return this.target.getIConnection();
        // return new ExprJdbcConnection(this.getConnection(), this.getSqlDialect());
        // return new JdbcConnection(this.getConnection());
    }

    @Override
    public Connection getConnection() {
        return this.target.getNativeConnection();
    }

    private SQLDialect sqlDialect;

    public SQLDialect getSqlDialect() {
        if (null == sqlDialect) {
            sqlDialect = SQLDialect.valueOf(this.databaseType);
        }
        return sqlDialect;
    }
}
