package com.wabacus.extra.expr;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.exception.MessageCollector;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.AlterException;
import com.wabacus.extra.SystemHelper;
import com.wabacus.extra.WabacusScriptEngineHelper;
import com.wabacus.extra.database.AbstractNoSqlDatabaseType;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.component.application.report.configbean.crosslist.CrossListReportDynDatasetBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsJavaEditActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.DeleteSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditActionGroupBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.component.application.report.configbean.editablereport.InsertSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.UpdateSqlActionBean;
import com.wabacus.system.dataset.ISqlDataSetBuilder;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.option.SQLOptionDatasource;
import com.wabacus.system.inputbox.option.TypepromptOptionBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbstractExprDatabaseType extends AbstractNoSqlDatabaseType {

    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractExprDatabaseType.class);

    /**
     * sqlWithoutOrderby,orderby,sqlKernel,sqlCount
     */
    @Override
    public void doPostLoadSql(ReportDataSetValueBean sbean, boolean isListReportType) {

        String value = sbean.getValue();
        if (value == null) {
            return;
            // final AbsConfigBean parent = sbean.getParent();
            // if( null !=parent){
            // final ReportBean pReport = parent.getReportBean();
            // if( null != pReport){
            // pReport.getSbean().getValue();
            // }
            // }
        }
        Map vars = new HashMap();
        value = (String) TemplateRuntime.eval(value, sbean, vars);
        String sqlKernel = sbean.getSql_kernel();
        if (StringUtils.isBlank(sqlKernel)) {
            // sqlKernel = sbean.getReportBean().getAttrs().get("pojoTabname");
        }
        if (StringUtils.isBlank(sqlKernel)) {
            sqlKernel = (String) vars.get("tabname");
        }

        if (StringUtils.isNotBlank(sqlKernel)) {
            // sbean.setSqlWithoutOrderby("select * from "+sqlKernel+" where {#condition#}");
        }

        if (StringUtils.isNotBlank(value)) {
            String queryExpr = (String) vars.get("queryExpr");
            if (StringUtils.isNotBlank(sqlKernel)) {
                sbean.setSql_kernel(sqlKernel);
                if (StringUtils.isBlank(value) && StringUtils.isBlank(queryExpr)) {
                    queryExpr = "findAsList(sqlKernel,'{}')";
                }
                if (StringUtils.isNotBlank(queryExpr)) {
                    value = queryExpr;
                }
            }
        }

        Object orderBy = vars.get("orderBy");
        if (orderBy instanceof Map) {
            orderBy = AbstractWabacusScriptExprContext.toJson(orderBy);
        }

        if (orderBy != null) {
            sbean.setOrderby(orderBy.toString());
        }

        sbean.setValue(value);

        if (StringUtils.isBlank(sbean.getSqlCount())) {
            String sqlCount = (String) vars.get("sqlCount");
            if (StringUtils.isBlank(sqlCount)) {
                sqlCount = "this.count(getQueryConditionMap())";
            } else {
                LOG.info("custom count:{}", sqlCount);
            }
            sbean.setSqlCount(sqlCount);
        }
        vars.clear();
    }

    // @Override
    public void constructSqlForListType(SqlBean sqlbean) {

        for (ReportDataSetBean dataset : sqlbean.getLstDatasetBeans()) {

            for (ReportDataSetValueBean datasetbeanTmp : dataset.getLstValueBeans()) {
                String value = datasetbeanTmp.getValue();
                if (value == null || value.trim().equals("") || datasetbeanTmp.isStoreProcedure()
                        || datasetbeanTmp.getCustomizeDatasetObj() != null)
                    continue;
                datasetbeanTmp.doPostLoadSql(true);
                String filterdata_sql = sqlbean.getReportBean().getAttrs().get("filterDataExpr");
                if (StringUtils.isBlank(filterdata_sql)) {
                    filterdata_sql = "distinct(filterColumn,filterCondition)";
                }
                datasetbeanTmp.setFilterdata_sql(filterdata_sql);
                if (!datasetbeanTmp.isDependentDataSet())
                    datasetbeanTmp.buildPageSplitSql();
            }
        }
    }

    @Override
    public String constructSplitPageSql(ReportDataSetValueBean sbean, String dynorderby) {
        throw new NotImplementedException();
    }

    @Override
    public IDataType getWabacusDataTypeByColumnType(String columntype) {
        throw new NotImplementedException();
    }

    @Override
    public void updateData(Map<String, String> mParamsValue, Map<String, String> mExternalParamsValue,
            ReportBean rbean, ReportRequest rrequest, AbsEditSqlActionBean actionBean) throws SQLException {

        final AbstractWabacusScriptExprContext ctx = createExprContext(rrequest, rbean, null);
        Map vars = new HashMap();
        if (null != mExternalParamsValue) {
            vars.putAll(mExternalParamsValue);
        }
        if (null != mParamsValue) {
            vars.putAll(mParamsValue);
        }
        vars.put("mExternalParamsValue", mExternalParamsValue);
        vars.put("mParamsValue", mParamsValue);

        // ctx.setVars(vars);

        // final Object eval = MVEL.eval(actionBean.getSql(), ctx, vars);
        final Object eval = eval(actionBean.getSql(), vars, ctx);
        vars.clear();

    }

    @Override
    public String parseDeleteSql(DeleteSqlActionBean actionBean, String reportTypeKey, String actionscript) {

        ReportBean rbean = actionBean.getOwnerGroupBean().getOwnerUpdateBean().getOwner().getReportBean();
        List<EditableReportParamBean> lstParamsBean = getLstParamsBean(rbean, reportTypeKey, actionBean);

        return actionscript;
    }

    public List<UpdateSqlActionBean> constructUpdateSql(String configUpdateSql, ReportBean rbean,
            String reportTypeKey, UpdateSqlActionBean updateSqlBean1) {
        List<EditableReportParamBean> lstParamsBean = new ArrayList<EditableReportParamBean>();
        if (true) {// 没有指定要更新的字段，则将所有从数据库取数据的<col/>（不包括hidden="1"和="2"的<col/>）全部更新到表中

            // EditableReportSqlBean ersqlbean = (EditableReportSqlBean) rbean.getSbean()
            // .getExtendConfigDataForReportType(EditableReportSqlBean.class);
            // final EditableReportUpdateDataBean updatebean = ersqlbean.getUpdatebean();

            List<ColBean> lstColBeans = rbean.getDbean().getLstCols();
            for (ColBean cbean : lstColBeans) {

                EditableReportParamBean paramBean = updateSqlBean1.createParamBeanByColbean(
                        cbean.getProperty(), reportTypeKey, false, false);
                if (paramBean != null) {
                    lstParamsBean.add(paramBean);
                    Map<String, String> attrs = cbean.getAttrs();
                    if (true && ("false".equals(attrs.get("editable")) || "false".equals(attrs
                            .get("updatable")))) {
                        EditableReportColBean ercbeanUpdateSrc = (EditableReportColBean) cbean
                                .getExtendConfigDataForReportType(reportTypeKey);
                        if (ercbeanUpdateSrc != null) {
                            ercbeanUpdateSrc.setEditableWhenUpdate(0);
                        }
                    }
                }

            }
        }

        List<UpdateSqlActionBean> lstUpdateSqls = new ArrayList<UpdateSqlActionBean>();
        UpdateSqlActionBean updateSqlBean = new UpdateSqlActionBean(configUpdateSql, lstParamsBean,
                updateSqlBean1.getOwnerGroupBean(), updateSqlBean1.getReturnValueParamname());

        updateSqlBean.setSql(configUpdateSql);
        // updateSqlBean.setLstParamBeans(lstParamsBean);
        lstUpdateSqls.add(updateSqlBean);

        return lstUpdateSqls;
    }

    public void constructInsertSql(String configInsertSql, ReportBean rbean, String reportTypeKey,
            InsertSqlActionBean insertSqlBean) {
        List<EditableReportParamBean> lstParamsBean = getLstParamsBean(rbean, reportTypeKey, insertSqlBean);
        insertSqlBean.addInsertSqlActionBean(configInsertSql, lstParamsBean,
                insertSqlBean.getReturnValueParamname());
    }

    private List<EditableReportParamBean> getLstParamsBean(ReportBean rbean, String reportTypeKey,
            AbsEditSqlActionBean insertSqlBean) {
        List<EditableReportParamBean> lstParamsBean = new ArrayList<EditableReportParamBean>();
        List<ColBean> lstColBeans = rbean.getDbean().getLstCols();
        for (ColBean cbean : lstColBeans) {
            EditableReportParamBean paramBean = insertSqlBean.createParamBeanByColbean(cbean.getProperty(),
                    reportTypeKey, false, false);
            if (paramBean != null) {
                Map<String, String> attrs = cbean.getAttrs();
                if ("false".equals(attrs.get("editable")) || "false".equals(attrs.get("insertable"))) {
                    EditableReportColBean ercbeanUpdateSrc = (EditableReportColBean) cbean
                            .getExtendConfigDataForReportType(reportTypeKey);
                    if (ercbeanUpdateSrc != null) {
                        ercbeanUpdateSrc.setEditableWhenInsert(0);
                    }
                }

                lstParamsBean.add(paramBean);
            }
        }
        return lstParamsBean;
    }

    private ExprISQLTypeCreator defaultCreator = new ExprISQLTypeCreator();

    // @Override
    public ISqlDataSetBuilder getISQLTypeBuilder(ReportDataSetValueBean bean, String statementtype) {
        return new ISqlDataSetBuilder(statementtype, defaultCreator);
    }

    //
    @Override
    public void parseConditionInSql(ReportDataSetValueBean bean, String value) {
        // TODO
        LOG.warn("parseConditionInSql dothing!");
    }

    @Override
    public void parseActionscripts(EditActionGroupBean eagbean, String reportTypeKey) {
        ReportBean rbean = eagbean.getOwnerUpdateBean().getOwner().getReportBean();
        String realSqlTmp;
        String scriptTmp = eagbean.getActionscripts();
        if (scriptTmp == null || scriptTmp.trim().equals("")) {
            return;
        }
        scriptTmp = scriptTmp.trim();
        if (Tools.isDefineKey("class", scriptTmp)) {
            scriptTmp = Tools.getRealKeyByDefine("class", scriptTmp).trim();
            String javaname = scriptTmp;
            String params = null;
            int idx1 = scriptTmp.indexOf("(");
            int idx2 = scriptTmp.indexOf(")");
            if (idx1 > 0 && idx2 == scriptTmp.length() - 1) {
                javaname = scriptTmp.substring(0, idx1).trim();
                params = scriptTmp.substring(idx1 + 1, idx2).trim();
            } else if (idx1 >= 0 || idx2 >= 0) {
                throw new WabacusConfigLoadingException("加载报表" + rbean.getPath() + "失败，配置的更新数据JAVA类"
                        + scriptTmp + "不合法");
            }
            Object javaActionBean;
            try {
                javaActionBean = ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(javaname)
                        .newInstance();
            } catch (Exception e) {
                throw new WabacusConfigLoadingException("加载报表" + rbean.getPath() + "失败，配置的更新数据JAVA类"
                        + scriptTmp + "无法实例化", e);
            }
            if (!(javaActionBean instanceof AbsJavaEditActionBean)) {
                throw new WabacusConfigLoadingException("加载报表" + rbean.getPath() + "失败，配置的更新数据JAVA类"
                        + scriptTmp + "没有继承" + AbsJavaEditActionBean.class.getName());
            }
            ((AbsEditActionBean) javaActionBean).setOwnerGroupBean(eagbean);
            ((AbsEditActionBean) javaActionBean).parseActionscript(reportTypeKey, params);
            eagbean.addActionBean((AbsEditActionBean) javaActionBean);
        } else {
            realSqlTmp = new UpdateSqlActionBean(eagbean).parseAndRemoveReturnParamname(scriptTmp)
                    .toLowerCase().trim();
            if (realSqlTmp.startsWith("{") && realSqlTmp.endsWith("}")) {
                realSqlTmp = realSqlTmp.substring(1, realSqlTmp.length() - 1).trim();
            }
            if (realSqlTmp.indexOf("insert") == 0) {
                new InsertSqlActionBean(eagbean).parseActionscript(reportTypeKey, scriptTmp);
            } else if (realSqlTmp.indexOf("update") == 0) {
                UpdateSqlActionBean updateBean = new UpdateSqlActionBean(eagbean);
                updateBean.setSql(scriptTmp);

                constructUpdateSql(scriptTmp, rbean, reportTypeKey, updateBean);
                eagbean.addActionBean(updateBean);
                // new UpdateSqlActionBean(eagbean).parseActionscript(
                // reportTypeKey, scriptTmp);
            } else if (realSqlTmp.indexOf("delete") == 0) {
                new DeleteSqlActionBean(eagbean).parseActionscript(reportTypeKey, scriptTmp);
                // } else if (realSqlTmp.indexOf("call") == 0) {
                // new StoreProcedureActionBean(eagbean).parseActionscript(reportTypeKey, scriptTmp);
            } else {
                UpdateSqlActionBean updateBean = new UpdateSqlActionBean(eagbean);
                updateBean.setSql(scriptTmp);

                constructUpdateSql(scriptTmp, rbean, reportTypeKey, updateBean);
                eagbean.addActionBean(updateBean);

                // new InsertSqlActionBean(eagbean).parseActionscript(reportTypeKey, scriptTmp);
                // throw new WabacusConfigLoadingException("加载报表" + rbean.getPath() + "失败，配置的更新数据的SQL语句"
                // + scriptTmp + "不合法");
            }
        }
    }

    // @Override
    // public void doConditionBeanPostLoad(ConditionBean cdbean) {
    // LOG.warn("doConditionBeanPostLoad dothing!");
    // }

    // public Object getPromptDataList(ReportRequest rrequest, ReportBean rbean,
    // AbsTypePromptDataSource typeObj, String typedata) {
    // SQLPromptDataSource pTypeObj = (SQLPromptDataSource) typeObj;
    // final SqlBean sbean = rbean.getSbean();
    // String sqlTemp = pTypeObj.getSql();//
    // Tools.replaceAll(pTypeObj.getSql(),"#data#",typedata);
    // LOG.debug("expr：{} data:{}", sqlTemp, typedata);
    // pTypeObj.getPromptConfigBean().getLstPColBeans();
    // Map vars = new HashMap();
    // vars.put("typeObj", typeObj);
    // vars.put("data", typedata);
    // return eval(sqlTemp, vars, rrequest, rbean).getResult();
    //
    // }

    public AbstractWabacusScriptExprContext eval(String sql, ReportRequest rrequest, ReportBean rbean,
            ReportDataSetValueBean datasetbean) {
        return eval(sql, null, rrequest, rbean, datasetbean);
    }

    public AbstractWabacusScriptExprContext eval(String script, Map vars,
            AbstractWabacusScriptExprContext ctx) {
        
        if(script != null){ //此处代码用于解决：当前两报表拥有相同脚本，由于mvel的缓存机制可能导致第一个报表运行相同脚本时引用的方法名却是第一个报表类的
            script += "\n//"+ctx.getReportPojoClass().getName();
        }
        if (Config.show_sql) {
            LOG.info("Execute query expr:\n {}", script);
        }

        final Map extraVars = ctx.getExtraVars();
        if (!extraVars.isEmpty()) {
            vars.putAll(extraVars);
        }
        ctx.setResult(null);
        ctx.setVars(vars);
        try {
            final Object eval = WabacusScriptEngineHelper.getScriptEngine().eval(script, ctx, vars);
            if (eval != null && ctx.getResult() == null) {
                ctx.setResult(eval);
            }
        } catch (RuntimeException ex) {
            handleEx(ctx, ex);
            throw ex;
        } catch (java.lang.AssertionError ex) {
            handleEx(ctx, ex);
            throw ex;
        }
        return ctx;
    }

    protected void handleEx(AbstractWabacusScriptExprContext ctx, Throwable ex) {
        LOG.error("脚本运行出错:" + ex.getMessage(), ex);

        final Throwable rootCause = SystemHelper.getRootCause(ex);
        final MessageCollector mc = ctx.getRrequest().getWResponse().getMessageCollector();

        if (rootCause instanceof RuntimeException) {
            final String message = SystemHelper.friendlyMessage(rootCause);
            if (rootCause instanceof AlterException) {
                mc.warn(message, message, rootCause, Consts.STATECODE_NONREFRESHPAGE);
            } else {
                mc.error(message, false);
            }
            throw (RuntimeException) rootCause;
        } else if (rootCause instanceof java.lang.AssertionError) {
            final String message = SystemHelper.friendlyMessage(rootCause);
            mc.warn(message, message, rootCause, Consts.STATECODE_NONREFRESHPAGE);
            throw (AssertionError) rootCause;
        }

    }

    public AbstractWabacusScriptExprContext eval(String script, Map vars, ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean) {
        if (null == vars) {
            vars = new HashMap();
        }
        final AbstractWabacusScriptExprContext ctx = createExprContext(rrequest, rbean, datasetbean);
        return eval(script, vars, ctx);
    }

    protected final AbstractWabacusScriptExprContext createExprContext(ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean) {
        AbstractWabacusScriptExprContext ctx = (AbstractWabacusScriptExprContext) WabacusScriptEngineHelper
                .getScriptExprContextFactory().createScriptExprContext(this, rrequest, rbean, datasetbean);
        if (null == ctx) {
            ctx = createDefaultExprContext(rrequest, rbean, datasetbean);
        }
        return ctx;
    }

    public abstract AbstractWabacusScriptExprContext createDefaultExprContext(ReportRequest rrequest,
            ReportBean rbean, ReportDataSetValueBean datasetbean);

    public Object getPromptDataList(ReportRequest rrequest, ReportBean rbean, SQLOptionDatasource typeObj,
            String typedata) {

        if (StringUtils.isBlank(typedata)) {
            return null;
        }
        // typedata = typedata.trim();
        final SqlBean sbean = rbean.getSbean();
        final String sqlTemp = typeObj.getSql();//
        // sqlTemp = Tools.replaceAll(typeObj.getSql(),"#data#",typedata);
        if (LOG.isDebugEnabled()) {
            LOG.debug("expr：{} data:{}", sqlTemp, typedata);
        }

        final Map<String, Object> vars = Maps.newHashMap();
        vars.put("typeObj", typeObj);
        vars.put("data", typedata);

        final TypePromptBean typePromptBean = ((TextBox) typeObj.getOwnerOptionBean().getOwnerInputboxObj())
                .getTypePromptBean();
        if (typePromptBean != null && typePromptBean.getLstPColBeans().size() > 0) {
            TypePromptColBean typePromptColBean = typePromptBean.getLstPColBeans().get(0);
            String matchmode = typePromptColBean.getAttrs().get("matchmode");
            vars.put("matchmode", matchmode);
        }

        List<ReportDataSetValueBean> lstDatasetValueBeans = rbean.getSbean()
                .getLstDatasetValueBeansByValueid(null);// FIXME
        return eval(sqlTemp, vars, rrequest, rbean, lstDatasetValueBeans.get(0)).getResult();
    }

    @Override
    public void doPostLoadSQLOptionDatasource(TypepromptOptionBean tpBean) {

    }

    @Override
    public String parseAndTrimScript(final String content) {
        return formatSqlString(content);
    }

    @Override
    public String parseButtonsClickevent(final IComponentConfigBean ccbean, final AbsButtonType buttonObj,
            String clickevent) {
        if (clickevent.indexOf("@{") > 0) {
            final Map<String, Object> vars = Maps.newHashMap();
            final ButtonPostPaserContext ctx = new ButtonPostPaserContext(ccbean, buttonObj);
            
            CompiledTemplate compiled = TemplateCompiler.compileTemplate(clickevent);
            Object eval = TemplateRuntime.execute(compiled, ctx,vars);
            if (eval instanceof CharSequence) {
                clickevent = eval.toString();
            }
        }
        return clickevent.trim();
    }

    private static final Pattern TRIM_SCRIPT_PATTERN = Pattern
            .compile("(^\\s+)|(\\s+$)", Pattern.MULTILINE);


    private static final Pattern TRIM_SCRIPT_PATTERN1 = Pattern
            .compile("^(\\s+)[^\\s]+", Pattern.MULTILINE);

    
    protected static final String formatSqlString(String sqlValue) {
        
        if( sqlValue == null){
            return StringUtils.EMPTY;
        }
        sqlValue = sqlValue.trim();
        final Matcher m = TRIM_SCRIPT_PATTERN1.matcher(sqlValue);
        int min=0;
        while(m.find()){ 
            int size =  m.group(1).length() ;
            if(min == 0 ||  min > size ){
                min = size;
            };
        }
        
       Pattern  p = Pattern.compile("(^\\s{"+min+"})",Pattern.MULTILINE);
       return  p.matcher(sqlValue).replaceAll(StringUtils.EMPTY);
        
//        sqlValue = sqlValue == null ? StringUtils.EMPTY : TRIM_SCRIPT_PATTERN.matcher(sqlValue).replaceAll(
//                StringUtils.EMPTY);// sqlValue.replaceAll("(^\\s+)|(\\s+$)",StringUtils.EMPTY );
//        return sqlValue;
    }

    // public String parseAndValidPromptSql(ReportBean rbean,
    // SQLPromptDataSource typeObj, String sql) {
    // if (typeObj.getClass().isAssignableFrom(SQLPromptDataSource.class)) {
    // LOG.warn("parseAndValidPromptSql dothing!");
    // return sql;
    // }
    // return typeObj.parseAndValidPromptSql(rbean, sql);
    // }

    public void doPostLoadCrossListReportDynDatasetBean(CrossListReportDynDatasetBean crdBean) {
        LOG.info("doPostLoadCrossListReportDynDatasetBean...");
    }
}
