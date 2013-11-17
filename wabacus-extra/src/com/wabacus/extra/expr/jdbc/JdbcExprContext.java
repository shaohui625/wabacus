package com.wabacus.extra.expr.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteWhereStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.SelectJoinStep;
import org.jooq.SelectSelectStep;
import org.jooq.SelectWhereStep;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.expr.AbstractQueryBuilder;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.JdbcConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.option.SQLOptionDatasource;
import com.wabacus.system.inputbox.option.SelectboxOptionBean;

/**
 * 
 * Mongo Executor (Shell ) Context;
 * 
 * @author qxo(qxodream@gmail.com)
 * 
 */
public final class JdbcExprContext extends AbstractWabacusScriptExprContext {

    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(JdbcExprContext.class);

    public JdbcExprContext(ReportRequest rrequest, ReportBean rbean, ReportDataSetValueBean datasetbean) {
        super(rrequest, rbean, datasetbean);
    }

    public Object getSql() {
        return new groovy.sql.Sql(this.getConnection());
    }

    @Override
    public String toQueryStr() {
        return toSelectJoinStep(this.getQueryConditionMap(),this.getTabname(),false,false).getSQL(ParamType.INLINED);
    }

    protected SelectJoinStep toSelectJoinStep(Map query, String tab) {
        return toSelectJoinStep(query,tab,true,true);
    }
    protected SelectJoinStep toSelectJoinStep(Map query, String tab,final boolean withOrderby ,final boolean withLimit) {
        // if (mongoQuery.isEmpty()) {
        // return "select * from " + tab;
        // }
        try {

            final DSLContext factory = getJooqFactory();
            // Field[] fields = toFields(this.getDbCols());

            final SelectJoinStep builder = Boolean.TRUE.equals(this.getVars().get("anyField")) ? factory
                    .select(DSL.field("*")).from(tab) : factory.select(toFields(this.getDbCols()))
                    .from(tab);
            if (!query.isEmpty()) {
                addWhereCause(builder, query);
            }
            if(withLimit){
                skipAndLimit(builder);
            }
            if(withOrderby){
                builder.orderBy(getSortFields());
            }
            return builder;

        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public List<Map<String, Object>> bySql(String sql) {
        // final List asList = doQuery(sql, new MapListHandler());
        // return asList;
        return bySql(sql, this.getTypePromptLimit());
    }

    public List<Map<String, Object>> bySql(final String sql, int maxSize) {
        return bySql(sql, maxSize, null);
    }

    public List<Map<String, Object>> byQuery(final String sql, Object... parameters) {
           return bySql(sql,-1,parameters);
    }
    
    public List<Map<String, Object>> bySql(final String sql, int maxSize, Object... parameters) {

        Object object = this.getVars().get("typeObj");

        final SelectboxOptionBean opBean = object instanceof SelectboxOptionBean ? (SelectboxOptionBean) object
                : null;
        final String valueTag = opBean == null ? null : opBean.getValue().toUpperCase();
        final String labelTag =  opBean == null ? null : opBean.getLabel().toUpperCase();

        RecordMapper recordMapper1 = opBean == null ? new RecordMapper() {
            public Object map(Record record) {
                Map<String, Object> ret = Maps.newHashMap();
                for (Field fd : record.fields()) {
                    final Object val = record.getValue(fd);
                    ret.put(fd.getName().toLowerCase(), val);
                }
                return ret;
            }
        } : new RecordMapper() {
            public Object map(Record record) {
                Map<String, Object> ret = Maps.newHashMap();
                for (Field fd : record.fields()) {
                    final Object val = record.getValue(fd);
                    ret.put(fd.getName(), val);
                }

                ret.put("value", ret.get(valueTag));
                ret.put("label", ret.get(labelTag));
                return ret;
            }
        };
        ResultQuery<Record> resultQuery =  parameters == null ? this.getJooqFactory().resultQuery(sql) : this.getJooqFactory().resultQuery(sql,parameters);
        if (maxSize > 0) {
            resultQuery = resultQuery.maxRows(maxSize);
        }
        List<Map<String, Object>> fetchMaps  =   resultQuery.fetch(recordMapper1);
        return fetchMaps;
    }

    // private static final RecordMapper recordMapper1 = new RecordMapper() {
    // public Object map(Record record) {
    // Map<String, Object> ret = Maps.newHashMap();
    // for (Field fd : record.fields()) {
    // final Object val = record.getValue(fd);
    // ret.put(fd.getName().toLowerCase(), val);
    // }
    // return ret;
    // }
    // };

    protected Field[] toFields(List<String> fields) {
        final Field[] fields1 = new Field[fields.size()];
        for (int i = 0; i < fields1.length; i++) {
            fields1[i] = field(fields.get(i));
        }
        return fields1;
    }

    //
    //
    // public List<Map<String, Object>> select(final List<String> fields,
    // Map<String, Object> query,
    // int maxRows) {
    // final Field[] fields1 = new Field[fields.size()];
    // for (int i = 0; i < fields1.length; i++) {
    // fields1[i] = field(fields.get(i));
    // }
    // DSLContextFactory jooqFactory = this.getJooqFactory();
    // SelectSelectStep<Record> select = jooqDSL.select(fields1);
    // addWhereCause(select, query);
    // List<Map<String, Object>> fetchMaps = select.maxRows(maxRows).fetch();
    // return fetchMaps;
    // }

    private DSLContext factory;

    protected DSLContext getJooqFactory() {
        Connection nativeConnection = getConnection();
        factory = DSL.using(nativeConnection, getSQLDialect());
        return factory;
    }

    protected SQLDialect getSQLDialect() {
        AbsDataSource ds = getDataSource();
        if (!(ds instanceof ExprJdbcDataSource)) {
            ds = (ExprJdbcDataSource) Config.getInstance().getDataSource(null);
        }
        return ((ExprJdbcDataSource) ds).getSqlDialect();
    }

    private Connection nativeConnection;

    protected Connection getConnection() {
        if (null == nativeConnection) {
            String datasource = getDataSourceName();
            JdbcConnection iConnection = (JdbcConnection) this.rrequest.getIConnection(datasource);
            nativeConnection = iConnection.getNativeConnection();
        }
        return nativeConnection;
    }

    /**
     * 设置开始记录行及返回的最大数据行数
     * 
     * @param find
     * @return
     */
    protected SelectJoinStep skipAndLimit(SelectJoinStep select) {
        if ("true".equals(this.getVars().get("nolimit"))) {
            return select;
        }
        final CacheDataBean cdb = getCacheDataBean();
        int pagesize = cdb.getPagesize();
        if (pagesize == -1) {
            return select;
        }
        int pageno = cdb.getFinalPageno();
        int start = 0;
        if (pageno > 1) {
            start = (pageno - 1) * pagesize;
        }
        int numberOfRows = pagesize;
        if (pagesize > 0) {
            numberOfRows = pagesize;
        } else {
            final int maxrecordcount = cdb.getMaxrecordcount();
            if (maxrecordcount > 0) {
                // select.limit(maxrecordcount);
                numberOfRows = maxrecordcount;
            }
        }

        select.limit(start, numberOfRows);
        return select;
    }

    protected final <T extends SelectWhereStep> T addWhereCause(final T builder, final Map query) {
        if (null == query || query.isEmpty()) {
            return builder;
        }
        for (Iterator<Map.Entry<String, Object>> iterator = query.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<String, Object> entry = iterator.next();
            Condition condition = createCondition(entry);
            if (null != condition) {
                builder.where(condition);
            }
        }
        return builder;
    }

    protected final Condition createCondition(final Map.Entry<String, Object> entry) {
        return createCondition(entry.getKey(), entry.getValue());
    }

    protected final Condition condition(String key, Object... value) {
        return DSL.condition(key, value);
    }

    protected final Condition condition(String key) {
        return DSL.condition(key);
    }

    private static final Pattern P1 = Pattern.compile("[(=?]");

    protected final Condition createCondition(final String key, Object value) {
        Condition condition = null;
        if(FLAG_4_ALL_VALUES.equals(value)){
            return null;
        }
        if (value instanceof Pattern) {
            condition = condition(key + " like ? ", '%' + value.toString() + '%');
        } else if (value instanceof Number) {
            condition = condition(key + " = ?  ", value);
        } else if (null == value) {
            condition = condition(key + " is null");
        } else if (P1.matcher(key).find()) {

            // final Map<String, Object> vars1 = Maps.newHashMap();
            // vars1.put("value", value);
            // vars1.put("context", this);
            // final Object ret =
            // ScriptEngineFactory.getScriptEngine().eval(key,this,vars1);
            // if (ret instanceof Condition) {
            // condition = (Condition) ret;
            // }
            if (value instanceof Collection) {
                value = ((Collection) value).toArray();
            }
            return value instanceof Object[] ? DSL.condition(key, (Object[]) value) : DSL.condition(key,
                    value);
        } else if (value instanceof String[]) {
            final String[] arr = (String[]) value;
            if(arr.length == 1 &&( arr[0] == null || "null".equals(arr[0]))){
                condition = condition(key + " is null");
            }else{
                condition = DSL.field(key, String.class).in(arr);
            }
        } else {

            condition = condition(key + " = ? ", value.toString());
        }
        return condition;
    }

    private final DbUtilsBeanProcessor beanProcessor = new DbUtilsBeanProcessor() {
        protected <T> T newInstance(Class<T> c) throws SQLException {
            return (T) createPojoClassInstance(c);
        }
    };

    @Override
    public List findAsList(Map query, String c) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("query:{}  on {}", new Object[] { query, c });
        }
        final Class pojoCls1 = getTheReportPojoClass();
        final SelectJoinStep sqlbuilder = toSelectJoinStep(query, c);
        final ResultSetHandler<List> resultSetHandler = new ResultSetHandler<List>() {
            public List handle(ResultSet rs) throws SQLException {
                return beanProcessor.toBeanList(rs, pojoCls1);
            }
        };
        final List retList = doQuery(sqlbuilder.toString(), resultSetHandler);
        return retList;
    }

    /**
     * @return 获取排序字符串
     */
    protected SortField[] getSortFields() {
        final String[] orderbys = getOrderByArray();

        // StringBuffer s = null;
        if (orderbys != null) {
            if (orderbys.length == 2) {
                SortField[] orders = new SortField[1];
                Field<Object> field = field(orderbys[0]);
                SortField sfield = field.sort("asc".equals(orderbys[1]) ? SortOrder.ASC : SortOrder.DESC);
                orders[0] = sfield;
                return orders;
            } else if (orderbys.length == 1) {

            }
        } else {
            String orderby = (String) this.getVars().get("orderBy");
            if (null == orderby) {
                orderby = this.getReportAttr("defaultOrderby"); // 默认排序
            }
            if (StringUtils.isNotBlank(orderby)) {
                String[] orders = orderby.split("[;]+");
                List<SortField> sorts = Lists.newArrayList();
                for (String order : orders) {
                    if (StringUtils.isNotBlank(order)) {
                        String[] keyOrder = order.indexOf('[') != -1 ? order.split("[#]+") : order
                                .split("[ #]+");
                        Field<Object> field = field(keyOrder[0].trim());
                        SortField sfield = null;
                        if (keyOrder.length > 0 && StringUtils.isNotBlank(keyOrder[1])
                                && keyOrder[1].indexOf(':') != -1) {
                            // map
                            Object map = MVEL.eval(keyOrder[1]);
                            if (map instanceof Map) {
                                sfield = field.sort((Map) map);
                            }
                        } else {
                            sfield = field
                                    .sort(keyOrder.length == 1 || "asc".equals(keyOrder[1]) ? SortOrder.ASC
                                            : SortOrder.DESC);
                        }
                        if (null != sfield) {
                            sorts.add(sfield);
                        }
                    }
                }
                return sorts.toArray(EMPTY_SORTFIELD_ARRAY);
            }
        }
        return EMPTY_SORTFIELD_ARRAY;
    }

    private static final SortField[] EMPTY_SORTFIELD_ARRAY = new SortField[0];

    /**
     * @return 获取所有的表名清单
     */
    public Collection<String> listTableNames(String excludePattern) {

        // final Set<String> collectionNames = getDB().getCollectionNames();
        // if (StringUtils.isBlank(excludePattern)) {
        // return collectionNames;
        // }
        // return exclude(collectionNames, excludePattern);
        //
        throw new NotImplementedException();
    }

    public Collection<String> listTableNames() {
        final String exclude = "true".equals(this.rrequest.getAttribute("listAllTables")) ? null
                : "^system";
        return listTableNames(exclude);
    }

    // public Number count(String query) {
    // return count(query, this.getTabname());
    // }
    @Override
    public Number count(Map query, String c) {
        final DSLContext factory = getJooqFactory();
        final SelectJoinStep builder = factory.selectCount().from(c);
        // factory.selectDistinct(field(sql))
        addWhereCause(builder, query);
        // final String sql = builder.toString();
        LOG.debug("query:{}, on:{}", query, c);

        final Number count = (Number) builder.fetchOne(0);
        return count;
        // return countBySql(sql);
    }

    // private Number countBySql(final String sql) {
    // final ScalarHandler scalarHandler = new ScalarHandler(1);
    // return doQuery(sql, scalarHandler);
    // }
    private QueryRunner queryRunner = new QueryRunner();

    protected <T> T doQuery(String sql, ResultSetHandler<T> resultSetHandler) {
        try {

            T ret = (T) queryRunner.query(getConnection(), sql, resultSetHandler);
            return ret;
        } catch (SQLException e) {
            LOG.error("{}", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected AbsDataSource getDataSource() {
        final AbsDataSource dataSource = (AbsDataSource) Config.getInstance().getDataSource(
                this.getDataSourceName());
        return dataSource;
    }

    protected boolean isHistoryRevFeature() {
        return BooleanUtils.toBoolean(getReportAttr("historyRevFeature"));
    }

    @Override
    public int update(Map query, Map updateModifier, String c) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(" update query:{},modifier:{}, on:{}", new Object[] { query, updateModifier, c });
        }
        if (StringUtils.isBlank(c)) {
            throw new IllegalArgumentException("配置错误,表名未指定!");
        }
        if (updateModifier.isEmpty()) {
            throw new IllegalArgumentException("要修改的数据不能为空!");
        }
        if (query.isEmpty()) {
            throw new IllegalArgumentException("暂时不支持全表更新");
        }

        final StringBuilder sb = new StringBuilder(32);

        sb.append("update ").append(c).append(" set ");
        List<Object> params = new ArrayList();
        for (Iterator<Map.Entry<String, ?>> iterator = updateModifier.entrySet().iterator(); iterator
                .hasNext();) {
            Map.Entry<String, ?> entry = iterator.next();
            // Object value = entry.getValue();
            sb.append(entry.getKey()).append(" = ?,");

        }
        params.addAll(updateModifier.values());

        sb.deleteCharAt(sb.length() - 1).append(" where ");
        for (Iterator<Map.Entry<String, Object>> iterator = query.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<String, Object> entry = iterator.next();
            // set.where(createCondition(entry));
            sb.append(entry.getKey()).append("=?");
            if (iterator.hasNext()) {
                sb.append(" and ");
            }
        }
        params.addAll(query.values());
        return runSql(sb.toString(), params.toArray());

        // final DSLContext factory = getJooqFactory();
        // UpdateSetStep<Record> update = factory.update(table(c));
        // UpdateSetMoreStep<Record> set = null;
        //
        // for (Iterator<Map.Entry<String, ?>> iterator =
        // updateModifier.entrySet().iterator(); iterator
        // .hasNext();) {
        // Map.Entry<String, ?> entry = iterator.next();
        // Object value = entry.getValue();
        // set = set == null ? update.set(field(entry.getKey()), value) :
        // set.set(field(entry.getKey()),
        // value);
        // }
        // for (Iterator<Map.Entry<String, Object>> iterator =
        // query.entrySet().iterator();
        // iterator.hasNext();) {
        // final Map.Entry<String, Object> entry = iterator.next();
        // set.where(createCondition(entry));
        // }
        // final String sql = set.getSQL(ParamType.INDEXED);
        // return doUpdate(sql,set.getBindValues().toArray());

    }

    private Field field(String sql) {
        return DSL.field(sql, String.class);
    }

    private Field<?> field(String sql, Object value) {
        return DSL.field(sql, String.class, value);
    }

    @Override
    public Object insert(Map data, String c) {

        LOG.debug(" insert data:{}, on:{}", data, c);
        if (data.isEmpty()) {
            throw new IllegalArgumentException("insert data is null!");
        }
        // final String pk = this.getPk();

        final StringBuilder sb = new StringBuilder(32);
        sb.append("insert into ").append(c).append("( ");

        // List<Object> params = new ArrayList();
        for (Iterator<Map.Entry<String, ?>> iterator = data.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, ?> entry = iterator.next();
            sb.append(entry.getKey());
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append(")  values(");
        for (Iterator<Map.Entry<String, ?>> iterator = data.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, ?> entry = iterator.next();
            sb.append("?");
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append(") ");

        return runSql(sb.toString(), data.values().toArray());

        // if(!data.containsKey(pk)){
        //
        // this.getColBeanByColumn(pk).get
        // data.put(key, value)
        // }

        // final DSLContext factory = getJooqFactory();
        // InsertSetStep<Record> insertInto = factory.insertInto(table(c));
        // InsertSetMoreStep<Record> set = null;
        // for (Iterator<Map.Entry<String, Object>> iterator =
        // data.entrySet().iterator();
        // iterator.hasNext();) {
        // Map.Entry<String, Object> entry = iterator.next();
        // Object value = entry.getValue();
        //
        // set = set == null ? insertInto.set(field(entry.getKey()), value) :
        // set.set(
        // field(entry.getKey()), value);
        // }
        // // return set.execute();
        // set.getSQL(ParamType.INDEXED);
        // List<Object> bindValues = set.getBindValues();
        // String sql = set.toString();
        // return doUpdate(sql, bindValues);
    }

    public int runSql(String sql, Object[] params) {
        try {
            return queryRunner.update(getConnection(), sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public int runSql(String sql) {
        return runSql(sql, ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    protected Table<Record> table(String c) {
        return DSL.table(c);
    }

    @Override
    public int delete(Map data, String c) {
        LOG.debug(" delete data:{}, on:{}", data, c);
        final DSLContext factory = getJooqFactory();
        final DeleteWhereStep<Record> deleteStep = factory.delete(table(c));
        for (Iterator<Map.Entry<String, Object>> iterator = data.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<String, Object> entry = iterator.next();
            deleteStep.where(createCondition(entry));
        }
        return deleteStep.execute();
    }

    /**
     * @return 产生报表查询条件Map
     */
    public Map<String, Object> getQueryConditionMap(final Collection<String> excludes) {
        final Map<String, Object> ret = new HashMap<String, Object>();

        final SqlBean sbean = rbean.getSbean();
        final List<ConditionBean> lstConditions = sbean.getLstConditions();
        for (Iterator<ConditionBean> iterator = lstConditions.iterator(); iterator.hasNext();) {
            final ConditionBean cdbean = (ConditionBean) iterator.next();
            if (excludes.contains(cdbean.getName())) {
                continue;
            }
            final ConditionExpressionBean conditionExpression = cdbean.getConditionExpression();
            final String expr = conditionExpression == null ? null : conditionExpression.getValue();
            if (StringUtils.isBlank(expr)) {
                continue;
            }

            try {
                final Map vars = new HashMap();
                final Object data = getReqAttr(cdbean);
                vars.putAll(rrequest.getAttributes());
                vars.put("data", data == null ? cdbean.getDefaultvalue() : data);
                Object ev = MVEL.eval(expr, this, vars);
                if (ev instanceof CustomQueryBuilder) {
                    ev = ((CustomQueryBuilder) ev).toMap();
                }
                if (ev instanceof Map) {
                    final Map<String, Object> eMap = (Map<String, Object>) ev;
                    for (Iterator iter2 = eMap.entrySet().iterator(); iter2.hasNext();) {
                        Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iter2.next();
                        final Object cv = filterValue(entry.getValue());
                        if (entry.getKey() == null || cv == null) {
                            continue;
                        }
                        ret.put(entry.getKey(), cv);
                    }
                }

            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }

        final Object[] filterConditionExpression = getFilterConditionExpression(this.rrequest, this.rbean);
        if (filterConditionExpression != null && filterConditionExpression.length == 2) {
            ret.put((String) filterConditionExpression[0], filterConditionExpression[1]);
        }
        return ret;
    }

    public static enum MATCH_MODE {
        START(new MatchCaseCreator() {
            public void create(final CustomQueryBuilder build, final String key, final String value) {
                build.startsWith(key, value);
            }
        }), END(new MatchCaseCreator() {
            public void create(final CustomQueryBuilder build, final String key, final String value) {
                build.endsWith(key, value);
            }
        }), ANYWHERE(new MatchCaseCreator() {
            public void create(final CustomQueryBuilder build, final String key, final String value) {
                build.anywhere(key, value);
            }
        }), START_OR_END(new MatchCaseCreator() {
            public void create(final CustomQueryBuilder build, final String key, final String value) {
                build.startsOrEnds(key, value);
            }
        }), EQ(new MatchCaseCreator() {
            public void create(final CustomQueryBuilder build, final String key, final String value) {
                build.eq(key, value);
            }
        });

        private final MatchCaseCreator matchFunction;

        private MATCH_MODE(MatchCaseCreator matchFunction) {
            this.matchFunction = matchFunction;
        }

        public void build(CustomQueryBuilder build, String key, String value) {
            if (matchFunction != null) {
                matchFunction.create(build, key, value);
            }
        }
    }

    public static MATCH_MODE defaultMatchMode = MATCH_MODE.valueOf(Config.getInstance()
            .getSystemConfigValue("defaultMatchMode", "START_OR_END").toUpperCase());

    /**
     * @return 默认的匹配方式
     */
    public static MATCH_MODE getDefaultMatchMode() {
        return defaultMatchMode;
    }

    private static interface MatchCaseCreator {

        public void create(final CustomQueryBuilder build, final String key, final String value);
    }

    // ["deptno":["$regex":data+".*","$options":'i']]
    public final class CustomQueryBuilder extends AbstractQueryBuilder {

        private Map<String, Object> builder = Maps.newHashMap();

        private transient MATCH_MODE matchMode;

        public CustomQueryBuilder(Map<String, Object> builder) {
            this();
            this.builder = builder;
        }

        public CustomQueryBuilder() {
            super();

        }

        public MATCH_MODE getMatchMode() {
            if (null == matchMode) {
                final String matchmode = (String) getVars().get("matchmode");
                if (matchmode != null) {
                    matchMode = MATCH_MODE.valueOf(matchmode.toUpperCase());
                }
                if (null == matchMode) {
                    matchMode = getDefaultMatchMode();
                }
            }
            return matchMode;
        }

        /**
         * 采用默认的匹配方式
         */
        public CustomQueryBuilder like(String key, String value) {
            // startsOrEnds(key, value);
            getMatchMode().build(this, key, value);
            return this;
        }

        public CustomQueryBuilder anywhere(String key, String value) {
            if (isValidString(value)) {
                builder.put(key + " like ? ", '%' + value + '%');
            }
            return this;
        }

        protected boolean isValidString(String value) {
            return StringUtils.isNotBlank(value);
        }

        public CustomQueryBuilder startsOrEnds(String key, String value) {
            if (isValidString(value)) {
                builder.put(key + " like ? or " + key + " like ?",
                        new String[] { value + '%', '%' + value });
            }
            return this;
        }

        public CustomQueryBuilder startsWith(String key, String value) {
            if (isValidString(value)) {
                builder.put(key + " like ? ", value + '%');
            }
            return this;
        }

        public CustomQueryBuilder endsWith(String key, String value) {
            if (isValidString(value)) {
                builder.put(key + " like ? ", '%' + value);
            }
            return this;
        }

        public CustomQueryBuilder eq(String key, String val) {
            builder.put(key, val);
            return this;
        }

        public Map toMap() {
            return builder;
        }

    }

    @Override
    public CustomQueryBuilder newQuery() {
        return new CustomQueryBuilder();
    }

    @Override
    public AbstractQueryBuilder newQuery(final Map<String, Object> initMap) {
        return new CustomQueryBuilder(initMap);
    }

    public final String distinctSql(String colKey) {
        return distinctSql(colKey, this.getTabname());
    }

    public final String distinctSql(String colKey, String c) {
        final Field<Object> field = field(colKey);
        final DSLContext factory = getJooqFactory();
        final SelectSelectStep selectDistinct = factory.selectDistinct(field);
        selectDistinct.from(c);
        return selectDistinct.getSQL(ParamType.INLINED);
    }

    @Override
    public List distinct(String colKey, boolean filterCondition) {
        Map<String, Object> map = null;
        if (!filterCondition) {
            map = this.getQueryConditionMap();
        } else {
            map = Maps.newHashMap();
            // final Map<String, String> notnull = new HashMap<String,
            // String>();
            // notnull.put("$ne", null);
            // map.put(colKey, notnull);
        }
        return distinct(colKey, map, this.getTabname());
    }

    @Override
    public List distinct(String colKey, Map query, String c) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(" distinct  key:{}, on:{} query:{}", new Object[] { colKey, c, query });
        }

        final Field<Object> field = field(colKey);
        final DSLContext factory = getJooqFactory();
        SelectSelectStep selectDistinct = factory.selectDistinct(field);
        selectDistinct.from(c);

        addWhereCause(selectDistinct, query);
        selectDistinct.limit(this.getTypePromptLimit());
        return selectDistinct.fetch(field, String.class);

    }

    @Override
    public List<Map<String, String>> findTypePrompts(Map query, SQLOptionDatasource typeObj, String c) {

        // final String query = toJson(query);

        final TypePromptBean typePromptBean = ((TextBox) typeObj.getOwnerOptionBean().getOwnerInputboxObj())
                .getTypePromptBean();
        final List<TypePromptColBean> lstPColBeans = typePromptBean.getLstPColBeans();
        final Map<TypePromptColBean, Integer> fields = Maps.newHashMap();
        for (Iterator<TypePromptColBean> iterator = lstPColBeans.iterator(); iterator.hasNext();) {
            TypePromptColBean typePromptColBean = (TypePromptColBean) iterator.next();
            fields.put(typePromptColBean, 1);
        }
        int typePromptLimit = typePromptBean.getResultcount();
        if (typePromptLimit < 1) {
            typePromptLimit = getTypePromptLimit();
        }
        final DSLContext factory = getJooqFactory();
        SelectJoinStep builder = null;
        if (lstPColBeans.size() == 1) {
            final TypePromptColBean typepcol = fields.keySet().iterator().next();
            String key = (String) typepcol.getLabel();
            List<Map<String, String>> retList = new ArrayList<Map<String, String>>();
            builder = factory.selectDistinct(field(key)).from(c);
            addWhereCause(builder, query);

            builder.limit(typePromptLimit);

            final List rList = builder.fetch(0);
            // doQuery(builder.toString(), new ColumnListHandler<String>(1));

            for (Iterator<String> iterator = rList.iterator(); iterator.hasNext();) {
                final Map<String, String> m = new HashMap<String, String>();
                String value = iterator.next();
                m.put(key, value);
                retList.add(m);
            }
            return retList;
        }
        // fields.put("_id", 0);

        final Collection<Field> ofields = new ArrayList();
        for (TypePromptColBean key : fields.keySet()) {
            ofields.add(field((String) key.getLabel()));
        }

        builder = factory.selectDistinct(ofields.toArray(new Field[0])).from(c);
        addWhereCause(builder, query);
        builder.limit(typePromptLimit);
        final List asList = builder.fetchMaps();// doQuery(builder.toString(),
                                                // new MapListHandler());
        return asList;
    }
}