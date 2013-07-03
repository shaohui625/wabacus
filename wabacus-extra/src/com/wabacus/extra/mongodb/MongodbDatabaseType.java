package com.wabacus.extra.mongodb;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.expr.AbstractExprDatabaseType;
import com.wabacus.system.ReportRequest;

public class MongodbDatabaseType extends AbstractExprDatabaseType {

    @Override
    protected AbstractWabacusScriptExprContext createDefaultExprContext(ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean) {
        return new MongoExprContext(rrequest, rbean, datasetbean);
    }

}