package com.wabacus.extra.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.WabacusScriptEngineHelper;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemAndDataSetBean;
import com.wabacus.system.dataset.ISqlDataSet;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class ExprGetAllResultSet implements ISqlDataSet {

    private static Logger LOG = LoggerFactory.getLogger(ExprGetAllResultSet.class);

    // @Override
    public int getRecordcount(ReportRequest rrequest, AbsReportType reportTypeObj,
            ReportDataSetValueBean datasetbean) {
        return 0;
    }

    // @Override
    public Object getDataSet(ReportRequest rrequest, AbsReportType reportTypeObj,
            ReportDataSetValueBean datasetbean, List lstReportData) {
        ReportBean rbean = reportTypeObj.getReportBean();
        String sql = datasetbean.getValue();
        // String datasetid=datasetbean.getId();

        // FIXME!!!
        return getDataSet(rrequest, rbean, datasetbean, reportTypeObj, sql);
    }

    // @Override
    public Object getDataSet(ReportRequest rrequest, ReportBean rbean, Object typeObj, String sql,
            List<ConditionBean> lstConditionBeans, String datasource) {

        return getDataSet(rrequest, rbean, null, typeObj, sql);

        // throw new NotImplementedException();
    }

    // @Override
    public Object getColFilterDataSet(ReportRequest rrequest, ColBean filterColBean,
            ReportDataSetValueBean datasetbean, Map<String, List<String>> mSelectedFilterValues) {
        String sql = datasetbean.getFilterdata_sql();
        ReportBean rbean = filterColBean.getReportBean();
        if (sql == null || sql.trim().equals("")) {
            throw new WabacusRuntimeException("没有取到报表" + rbean.getPath() + "要获取过滤数据的SQL语句");
        }
        if (filterColBean.getColumn() == null || filterColBean.getColumn().trim().equals(""))
            return null;
        // sql=Tools.replaceAll(sql,"%FILTERCOLUMN%",filterColBean.getColumn());

        final String colunmname = filterColBean.getColumn();
        boolean filterCondition = false;
        if (mSelectedFilterValues != null && mSelectedFilterValues.size() > 0) {
            filterCondition = false;
            // sql=ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest,rbean,datasetbean,sql);
        } else {
            filterCondition = true;
            // sql=Tools.replaceAll(sql,Consts_Private.PLACEHODER_FILTERCONDITION,"");
        }

        final Map extraVars = AbstractWabacusScriptExprContext.initExtraVarsIf(rrequest);
        extraVars.put("filterColumn", colunmname);
        extraVars.put("filterCondition", filterCondition);

        final Object dataSet = getDataSet(rrequest, rbean, datasetbean,
                rrequest.getAttribute(rbean.getId() + "_WABACUS_FILTERBEAN"), sql);
        return dataSet;
    }

    // @Override
    public Object getStatisticDataSet(ReportRequest rrequest, AbsReportType reportObj,
            ReportDataSetValueBean svbean, Object typeObj, String sql, boolean isStatisticForOnePage) {

        List<String> lstConditions = new ArrayList<String>();
        List<IDataType> lstConditionsTypes = new ArrayList<IDataType>();
        ReportBean rbean = reportObj.getReportBean();
        
        
        AbstractExprDatabaseType dbType = getDbType(rbean, svbean);
        
      AbstractWabacusScriptExprContext ctx = (AbstractWabacusScriptExprContext) WabacusScriptEngineHelper
              .getScriptExprContextFactory().createScriptExprContext(dbType, rrequest, rbean, svbean);
      String sqlTmp = ctx.toQueryStr();
        
//        String sqlTmp = svbean.getSqlWithoutOrderby();
//        sqlTmp = Tools.replaceAll(sqlTmp, "%orderby%", "");
//        sqlTmp = Tools.replaceAll(sqlTmp, Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,
//                svbean.getSql_kernel());
//        sqlTmp = ReportAssistant.getInstance().parseRuntimeSqlAndCondition(rrequest, rbean, svbean, sqlTmp,
//                lstConditions, lstConditionsTypes);
//        sqlTmp = ListReportAssistant.getInstance().addColFilterConditionToSql(rrequest, rbean, svbean,
//                sqlTmp);
        
        //sql = ReportAssistant.getInstance().parseRuntimeSqlAndCondition(rrequest, rbean, svbean, sql, lstConditions, lstConditionsTypes);
        CacheDataBean cdb = rrequest.getCdb(rbean.getId());

        if (svbean.isDependentDataSet()) {
            List lstReportData = null;
            if (!cdb.isLoadAllReportData() && !isStatisticForOnePage) {
                lstReportData = (List) rrequest.getAttribute(rbean.getId() + "wx_all_data_tempory");
                if (lstReportData == null) {
                    lstReportData = ReportAssistant.getInstance().loadReportDataSet(rrequest, reportObj,
                            true);
                    rrequest.setAttribute(rbean.getId() + "wx_all_data_tempory", lstReportData);
                }
            } else {
                lstReportData = reportObj.getLstReportData();
            }
            String realConExpress = svbean.getRealDependsConditionExpression(lstReportData);
            if (realConExpress == null || realConExpress.trim().equals("")) {
                sqlTmp = ReportAssistant.getInstance().removeConditionPlaceHolderFromSql(rbean, sqlTmp,
                        ReportDataSetValueBean.dependsConditionPlaceHolder);
            } else {
                sqlTmp = Tools.replaceAll(sqlTmp, ReportDataSetValueBean.dependsConditionPlaceHolder,
                        realConExpress);
            }
        }
        sql = Tools.replaceAll(sql, StatisticItemAndDataSetBean.STATISQL_PLACEHOLDER, sqlTmp);
        if (rbean.getInterceptor() != null && typeObj != null) {
            Object obj = rbean.getInterceptor().beforeLoadData(rrequest, rbean, typeObj, sql);
            if (!(obj instanceof String))
                return obj;
            sql = (String) obj;
        }
        
        return ctx.byQuery(sql);
     //   Map<String,Object> vars = Maps.newHashMap();
     //   vars.put("queryStr", sql);
        
        //return asList(ctx,dbType.eval( "byQuery(queryStr)", vars, ctx));
       // return getDataSet(rrequest, rbean, svbean, null, sql);
       // return this.evalAsList(rrequest, rbean, svbean, "byQuery(queryStr)",vars);
    }

    public Object getDataSet(ReportRequest rrequest, ReportBean rbean, ReportDataSetValueBean datasetbean,
            Object typeObj, String sql) {
        // Statement stmt = null;

        final ReportAssistant rptAst = ReportAssistant.getInstance();
        // final Object reportDataPojoInstance=rptAst.getReportDataPojoInstance(reportbean);

        // sql = rptAst.addDynamicConditionExpressionsToSql(rrequest, reportbean, sql, lstConditionBeans,
        // null, null);
        if (rbean.getInterceptor() != null && typeObj != null) {
            Object obj = rbean.getInterceptor().beforeLoadData(rrequest, rbean, typeObj, sql);
            if (!(obj instanceof String))
                return obj;
            sql = (String) obj;
        }
        if (Config.show_sql) {
            LOG.info("Execute expr:{} ", sql);
        }
        Map vars = Maps.newHashMap();
        vars.put("typeObj", typeObj);
        AbstractWabacusScriptExprContext ctx = eval(rrequest, vars, rbean, datasetbean, sql);
        final Object result = ctx.getResult();
        return result;

    }

    public List evalAsList(ReportRequest rrequest, ReportBean rbean, ReportDataSetValueBean datasetbean,
            String sql) {
        AbstractWabacusScriptExprContext ctx = eval(rrequest, null, rbean, datasetbean, sql);
        final Object result = ctx.getResult();
        return asList(ctx, result);
    }
    
    public List evalAsList(ReportRequest rrequest, ReportBean rbean, ReportDataSetValueBean datasetbean,
            String sql,Map vars) {
        AbstractWabacusScriptExprContext ctx = eval(rrequest, vars, rbean, datasetbean, sql);
        final Object result = ctx.getResult();
        return asList(ctx, result);
    }

    public List asList(AbstractWabacusScriptExprContext ctx, final Object result) {
        List retList = null;
        if (result instanceof List) {
            retList = (List) result;
        } else {
            retList = ctx.asList(retList);
        }
        return retList;
    }

    public AbstractWabacusScriptExprContext eval(ReportRequest rrequest, Map vars, ReportBean rbean,
            ReportDataSetValueBean datasetbean, String sql) {
        AbstractExprDatabaseType dbType = getDbType(rbean, datasetbean);
        
//        AbstractWabacusScriptExprContext ctx = (AbstractWabacusScriptExprContext) WabacusScriptEngineHelper
//                .getScriptExprContextFactory().createScriptExprContext(dbType, rrequest, rbean, datasetbean);

        
        return dbType.eval(sql, vars,rrequest, rbean, datasetbean);
    }

    protected AbstractExprDatabaseType getDbType(ReportBean rbean, ReportDataSetValueBean datasetbean) {
        AbstractExprDatabaseType dbType = (AbstractExprDatabaseType) (datasetbean == null ? Config
                .getInstance().getDbType(rbean.getSbean().getDatasource()) : datasetbean.getDbType());
        return dbType;
    }
}
