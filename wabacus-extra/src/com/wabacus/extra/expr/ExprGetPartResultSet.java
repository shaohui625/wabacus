package com.wabacus.extra.expr;

import java.sql.ResultSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class ExprGetPartResultSet extends ExprGetAllResultSet {
    private static final Log LOG = LogFactory.getLog(ExprGetPartResultSet.class);

    @Override
    public int getRecordcount(ReportRequest rrequest, AbsReportType reportTypeObj,
            ReportDataSetValueBean datasetbean) {
        if (false) {
            return 10;
        }
        ReportBean rbean = reportTypeObj.getReportBean();
        String datasetid = datasetbean.getId();
        // datasetid=datasetid==null?"":datasetid.trim();
        // String sqlKernel=rrequest.getStringAttribute(datasetbean.getId(),datasetid+"_DYN_SQL","");
        // if("[NONE]".equals(sqlKernel)) return 0;
        // if(sqlKernel.equals("")) sqlKernel=datasetbean.getSql_kernel();
        // sqlKernel=ReportAssistant.getInstance().parseRuntimeSqlAndCondition(rrequest,rbean,datasetbean,sqlKernel,lstConditions,lstConditionsTypes);
        String sqlCount = datasetbean.getSqlCount();// Tools.replaceAll(datasetbean.getSqlCount(),Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlKernel);
        // sqlCount=ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest,rbean,datasetbean,sqlCount);

        Number recordcount = 0;
        ResultSet rsCount = null;
        if (rbean.getInterceptor() != null) {
            Object obj = rbean.getInterceptor().beforeLoadData(rrequest, rbean, reportTypeObj, sqlCount);
            if (obj == null)
                return -1;
            if (obj instanceof List) {
                List lst = (List) obj;
                if (lst.size() == 0) {
                    recordcount = 0;
                } else {
                    if (!(lst.get(0) instanceof Integer)) {
                        throw new WabacusRuntimeException("拦截器返回的记录数不是合法数字");
                    }
                    recordcount = (Integer) lst.get(0);
                }
                return recordcount.intValue();
            } else if (obj instanceof String) {
                sqlCount = (String) obj;
            } else {
                throw new WabacusRuntimeException("执行报表" + rbean.getPath() + "的加载数据拦截器失败，返回的数据类型"
                        + obj.getClass().getName() + "不合法");
            }

        }
        Number count = (Number) AbstractWabacusScriptExprContext.initExtraVarsIf(rrequest).get(
                "resultCount");
        if (count != null) {
            return count.intValue();
        }
        try {
            if (StringUtils.isNotBlank(sqlCount)) {
                AbstractWabacusScriptExprContext ctx = eval(rrequest,null, rbean, datasetbean, sqlCount);
                final Object result = ctx.getResult();
                if (result instanceof Number) {
                    recordcount = (Number) result;
                }
            }

        } catch (Exception e) {
            throw new WabacusRuntimeException("从数据库取数据时执行SQL：" + sqlCount + "失败", e);
        } finally {

        }
        return recordcount.intValue();
    }
}
