package com.wabacus.extra.expr.jdbc;

import java.sql.SQLException;

import org.apache.commons.lang.NotImplementedException;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.expr.AbstractExprDatabaseType;
import com.wabacus.system.IConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.datatype.IDataType;

public class ExprJdbcDatabaseType extends AbstractExprDatabaseType {

    private AbsDatabaseType targetDbType;

    protected AbsDatabaseType getTargetDbType() {
        return targetDbType;
    }

    void setTargetDbType(AbsDatabaseType targetDbType) {
        this.targetDbType = targetDbType;
    }
    
    
    @Override
    public IDataType getWabacusDataTypeByColumnType(String columntype) {
        if (targetDbType == null) {
            throw new NotImplementedException();
        }
        return targetDbType.getWabacusDataTypeByColumnType(columntype);
    }
    

    @Override
    public  AbstractWabacusScriptExprContext createDefaultExprContext(ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean) {
        return new JdbcExprContext(rrequest, rbean, datasetbean);
    }

    @Override
    public Object getSequnceValue(IConnection conn, String seqname) throws SQLException {
        if (targetDbType == null) {
            throw new NotImplementedException();
        }
        return targetDbType.getSequnceValue(conn, seqname);
    }

    @Override
    public String getSequenceValueSql(String sequencename) {
        if (targetDbType == null) {
            throw new NotImplementedException();
        }
        return targetDbType.getSequenceValueSql(sequencename);
    }
}