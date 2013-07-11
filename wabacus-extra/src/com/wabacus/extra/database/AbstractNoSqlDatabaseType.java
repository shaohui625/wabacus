package com.wabacus.extra.database;

import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.system.IConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditSqlActionBean;

public abstract class AbstractNoSqlDatabaseType extends AbsDatabaseType {
    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNoSqlDatabaseType.class);

    @Override
    public String constructSplitPageSql(ReportDataSetValueBean bean) {
        final String sql = bean.getSqlWithoutOrderby();
        return sql;
    }

    @Override
    public String getSequenceValueByName(String sequencename) {
        throw new NotImplementedException();
    }

    @Override
    public Object getSequnceValue(IConnection conn, String seqname) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public String getSequenceValueSql(String sequencename) {
        throw new NotImplementedException();
    }

    @Override
    public void updateData(Map<String, String> mParamsValue, Map<String, String> mExternalParamsValue,
            ReportBean rbean, ReportRequest rrequest, AbsEditSqlActionBean actionBean) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public String constructSplitPageSql(ReportDataSetValueBean svbean, String dynorderby) {
        throw new NotImplementedException();
    }

}
