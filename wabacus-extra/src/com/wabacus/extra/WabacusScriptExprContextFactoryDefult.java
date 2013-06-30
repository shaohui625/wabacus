package com.wabacus.extra;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.extra.mongodb.MongoExprContext;
import com.wabacus.extra.mongodb.MongodbDatabaseType;
import com.wabacus.system.ReportRequest;

/**
 * 
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public class WabacusScriptExprContextFactoryDefult implements WabacusScriptExprContextFactory {

    public WabacusScriptExprContext createScriptExprContext(Class<? extends AbsDatabaseType> dbTypeClass, ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean) {
        assert dbTypeClass.isAssignableFrom(MongodbDatabaseType.class);
        return new MongoExprContext(rrequest, rbean, datasetbean);
    }
}
