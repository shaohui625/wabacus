package com.wabacus.extra;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.system.ReportRequest;

/**
 * 脚本上下文Factory
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public interface WabacusScriptExprContextFactory {

    public WabacusScriptExprContext createScriptExprContext(Class<? extends AbsDatabaseType> dbTypeClass, ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean);
}
