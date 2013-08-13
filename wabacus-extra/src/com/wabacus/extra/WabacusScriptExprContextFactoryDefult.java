package com.wabacus.extra;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.extra.expr.AbstractExprDatabaseType;
import com.wabacus.extra.expr.jdbc.JdbcExprContext;
import com.wabacus.system.ReportRequest;

/**
 * 
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public final class WabacusScriptExprContextFactoryDefult implements WabacusScriptExprContextFactory {

    public WabacusScriptExprContext createScriptExprContext(final AbsDatabaseType dbType,
            final ReportRequest rrequest, final ReportBean rbean, final ReportDataSetValueBean datasetbean) {
        if (dbType instanceof AbstractExprDatabaseType) {
            return ((AbstractExprDatabaseType) dbType).createDefaultExprContext(rrequest, rbean,
                    datasetbean);
        }
        return new JdbcExprContext(rrequest, rbean, datasetbean);
    }
}
