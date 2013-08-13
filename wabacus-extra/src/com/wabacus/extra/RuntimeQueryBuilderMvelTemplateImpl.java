package com.wabacus.extra;

import java.util.List;
import java.util.Map;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import com.google.common.collect.Maps;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.RuntimeQueryBuilder;
import com.wabacus.system.datatype.IDataType;

public class RuntimeQueryBuilderMvelTemplateImpl implements RuntimeQueryBuilder {

    public String parseRuntimeSqlAndCondition(final ReportRequest rrequest, final ReportBean rbean,
            ReportDataSetValueBean svbean, String sql, List<String> lstConditionValues,
            List<IDataType> lstConditionTypes) {
        if (sql.indexOf("@{") > 0) {
            Map<String,Object> vars = Maps.newHashMap();
            vars.put("lstConditionValues", lstConditionValues);
            WabacusScriptExprContext exprContext = WabacusScriptEngineHelper.getScriptExprContextFactory()
                    .createScriptExprContext(svbean.getDbType(), rrequest, rbean, svbean);
            // WabacusScriptEngineHelper.getScriptEngine().eval(expression, ctx, vars)
            CompiledTemplate compiled = TemplateCompiler.compileTemplate(sql);
            sql = (String) TemplateRuntime.execute(compiled, exprContext);
        }
        return sql;
    }

}
