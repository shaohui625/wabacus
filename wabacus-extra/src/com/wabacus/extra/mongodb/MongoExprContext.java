package com.wabacus.extra.mongodb;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.jongo.Distinct;
import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jongo.MongoCollection;
import org.jongo.ResultHandler;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;
import com.mongodb.util.JsonUtils;
import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.exception.MessageCollector;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.LoginInfoFinder;
import com.wabacus.extra.LoginInfoFinderImpl;
import com.wabacus.extra.WabacusBeanFactory;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

import foodev.jsondiff.JsonDiff;

/**
 * 
 * Mongo Executor (Shell ) Context;
 * 
 * @author qxo(qxodream@gmail.com)
 * 
 */
public final class MongoExprContext extends AbstractWabacusScriptExprContext {

    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(MongoExprContext.class);

    private ReportRequest rrequest;

    private ReportBean rbean;

    private ReportDataSetValueBean datasetbean;

    public MongoExprContext(ReportRequest rrequest, ReportBean rbean, ReportDataSetValueBean datasetbean) {
        super();
        this.rrequest = rrequest;
        this.rbean = rbean;
        this.datasetbean = datasetbean;
    }

    /**
     * @return the rrequest
     */
    public ReportRequest getRrequest() {
        return rrequest;
    }

    /**
     * 脚本执行器返回结果的保存变量
     */
    private Object Result;

    public Object getResult() {
        return Result;
    }

    public void setResult(Object value) {
        this.Result = value;
    }

    public Map getReportAttrs() {
        return this.rbean.getAttrs();
    }

    /**
     * 脚本执行时的附加变量及执行过程产生的变理的存储区
     */
    private Map vars;

    public Map getVars() {
        return vars == null ? MapUtils.EMPTY_MAP : vars;
    }

    public void setVars(Map vars) {
        this.vars = vars;
    }

    /**
     * 通过此对象可获取Jongo对象
     */
    private MongodbConnection iconn;

    private MongodbConnection getMongodbConnection() {
        if (null == iconn) {
            iconn = (MongodbConnection) rrequest.getIConnection(rbean.getSbean().getDatasource());
        }
        return iconn;
    }

    /**
     * 通过此连接来执行数据库操作
     * 
     * @return
     * @see
     */
    public Jongo getJongo() {
        return this.getMongodbConnection().getJongo();
    }

    /**
     * report的POJO对象类
     */
    private Class pojoClass;

    protected Class getReportPojoClass() {
        if (null == pojoClass) {
            final ReportAssistant rptAst = ReportAssistant.getInstance();
            // pojoClass = rptAst.getReportDataPojoInstance(rbean).getClass();
            // ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,rbean.getPojoClassObj());
            pojoClass = this.rbean.getPojoClassObj();// FIXME
        }
        return pojoClass;
    }

    private Class getTheReportPojoClass() {
        final String reportPojoClassStr = (String) this.getVars().get("pojoClass");
        final Class reportPojoClass = StringUtils.isNotBlank(reportPojoClassStr) ? loadClass(reportPojoClassStr)
                : getReportPojoClass();
        return reportPojoClass;
    }

    protected Object createPojoClassInstance(Class pojoClassObj) {
        if (AbsReportDataPojo.class.isAssignableFrom(pojoClassObj)) {
            return ReportAssistant.getInstance().getPojoClassInstance(rrequest, rbean,
                    rbean.getPojoClassObj());
        } else {
            try {
                return pojoClassObj.newInstance();
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    /**
     * 根据查询条件获取符合条件的数据
     * 
     * @param mongoQuery
     * @return
     */
    public List findAsList(Map mongoQuery) {
        return findAsList(mongoQuery, this.getTabname());
    }

    public List findAsList(Map mongoQuery, String c) {
        final String json = toJson(mongoQuery);
        return findAsList(json, c);
    }

    public List findAsList(String mongoQuery, String c) {

        final String sortCause = getSortCause();
        if (LOG.isDebugEnabled()) {
            LOG.debug("query:{} on:{} sorts:{}", new Object[] { mongoQuery, c, sortCause });
        }
        final MongoCollection collection = getCollection(c);
        final Find find = collection.find(mongoQuery);
        if (null != sortCause) {
            find.sort(sortCause);
        }
        skipAndLimit(find);
        return asList(find);
    }

    public CacheDataBean getCacheDataBean() {

        return rrequest.getCdb(rbean.getId());
    }

    /**
     * 设置开始记录行及返回的最大数据行数
     * 
     * @param find
     * @return
     */
    public Find skipAndLimit(Find find) {

        CacheDataBean cdb = getCacheDataBean();
        int pagesize = cdb.getPagesize();
        int pageno = cdb.getFinalPageno();
        if (pageno > 1) {
            int start = (pageno - 1) * pagesize;
            find.skip(start);
        }

        if (pagesize > 0) {
            find.limit(pagesize);
        } else {
            final int maxrecordcount = cdb.getMaxrecordcount();
            if (maxrecordcount > 0) {
                find.limit(maxrecordcount);
            }
        }
        return find;
    }

    /**
     * @return 获取排序字符串
     */
    public String getSortCause() {
        final String[] orderbys = getOrderByArray();
        StringBuffer s = null;
        if (orderbys != null && orderbys.length == 2) {
            s = new StringBuffer("{").append(orderbys[0]).append(":")
                    .append("asc".equals(orderbys[1]) ? 1 : -1);
            s.append("}");
        }
        return s != null && s.length() > 0 ? s.toString() : "";// this.rbean.getSbean().getOrderby();
    }

    protected String[] getOrderByArray() {
        String[] orderbys = (String[]) rrequest.getAttribute(rbean.getId(), "ORDERBYARRAY");
        return orderbys;
    }

    public MongoCollection getCollection(String collectionsName) {
        if (StringUtils.isBlank(collectionsName)) {
            throw new NullArgumentException("collectionsName");
        }
        final MongoCollection collection = getJongo().getCollection(collectionsName);
        return collection;
    }

    public List asList(Find find) {
        // final Iterator iterator=find.as(getReportPojoClass()).iterator();
        final ResultHandler newMapper = getCurrentResultHandler();
        final Iterator iterator = find.map(newMapper).iterator();
        return IteratorUtils.toList(iterator);

    }

    public PojoMapper getPojoMapper() {
        final String pojoMapperCls = this.getReportAttr("pojoMapper");
        if (StringUtils.isBlank(pojoMapperCls)) {
            return null;
            // throw new IllegalArgumentException("报表参数pojoMapper未指定");
        }
        try {
            PojoMapper pojoMapper = (PojoMapper) loadClass(pojoMapperCls).getConstructor(
                    MongoExprContext.class).newInstance(this);
            return pojoMapper;
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

    }

    protected Class loadClass(final String cls) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (null == classLoader) {
                classLoader = MongoExprContext.class.getClassLoader();
            }
            return classLoader.loadClass(cls);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected ResultHandler getCurrentResultHandler() {

        ResultHandler newMapper = this.newMapper;

        if (null == newMapper) {

            final PojoMapper pojoMapper = getPojoMapper();
            if (null != pojoMapper) {
                newMapper = pojoMapper.getResultHandler();
            }
            if (null == newMapper) {
                final Object object = this.getVars().get("ResultHandler");
                if (object instanceof String) {
                    try {
                        newMapper = (ResultHandler) loadClass((String) object).newInstance();
                    } catch (InstantiationException e) {
                        throw new IllegalArgumentException(e.getMessage(), e);
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(e.getMessage(), e);
                    }
                } else if (object != null) {
                    newMapper = (ResultHandler) object;
                }
            }
        }
        if (null == newMapper) {
            Class reportPojoClass = this.getReportPojoClass();
            newMapper = JongoResultHandlerFactory.newMapper(reportPojoClass);
        }
        return newMapper;
    }

    private ResultHandler newMapper = null;

    public ResultHandler setJsonResultHandler(final ResultHandler newMapper) {
        return this.newMapper = newMapper;
    }

    public ResultHandler setJsonResultHandler(final String jsonContentProp) {
        return this.newMapper = createJsonResultHandler(jsonContentProp);
    }

    public ResultHandler createJsonResultHandler(final String jsonContentProp) {
        return createJsonResultHandler(jsonContentProp, null, this.getTheReportPojoClass());
    }

    private static ObjectMapper jsonMapper = new ObjectMapper();

    public ResultHandler createJsonResultHandler(final String jsonContentProp, String idProp,
            final Class pojoCls) {
        final String id = idProp == null ? "_id" : idProp;
        final ResultHandler mapper = new ResultHandler() {

            /*
             * (non-Javadoc)
             * 
             * @see org.jongo.ResultHandler#map(com.mongodb.DBObject)
             */
            public Object map(DBObject result) {

                try {
                    // System.out.println("result:"+result.getClass());
                    Object ret = createPojoClassInstance(pojoCls); // pojoCls.newInstance();

                    final String content = JsonUtils.getCustomObjectSerializer().serialize(result);// result.toString();
                                                                                                   // //fixme
                    final Object obj = result.get(id);
                    BeanUtils.setProperty(ret, id, obj);
                    BeanUtils.setProperty(ret, jsonContentProp, content);
                    return ret;

                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);

                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }

            }

        };
        return mapper;
    }

    public List asList(final Iterator iterator) {
        return IteratorUtils.toList(iterator);
    }

    public List asList(Object find) {
        final List ret = new ArrayList(1);
        ret.add(find);
        return ret;
    }

    // public String getSql_kernel() {
    // return this.rbean.getSbean().getSql_kernel();
    // }

    /**
     * 表名(集合名)
     */
    private String tabname;

    /**
     * @param tabname
     *            the tabname to set
     */
    public void setTabname(String tabname) {
        if (StringUtils.isNotBlank(tabname)) {
            this.tabname = tabname.trim();
            this.rrequest.addParamToUrl("_tabname", this.tabname, true);
        }
    }

    public String getTabname() {
        initTabName();
        return tabname;
    }

    protected void initTabName() {
        if (StringUtils.isBlank(tabname)) {
            tabname = (String) this.rrequest.getAttribute("_tabname");
            if (StringUtils.isBlank(tabname)) {
                tabname = this.getReportAttr("pojoTabname");
            }
            if (StringUtils.isBlank(tabname)) {
                tabname = getSqlKernel();
            }

            // if (StringUtils.isBlank(tabname)) {
            // throw new NotImplementedException();
            // // tabname = this.rbean.getSbean().getSql_kernel();
            // }
        }
    }

    public String getSqlKernel() {
        return datasetbean != null ? datasetbean.getSql_kernel() : null;
        // return this.rbean.getSbean().getDatasetBeanById(datasetid).getSql_kernel();
    }

    /**
     * 返回符合条的记录数
     * 
     * @param query
     * @return
     */
    public Number count(Map query) {
        return count(query, this.getTabname());
    }

    public Number count(String query) {
        return count(query, this.getTabname());
    }

    public Number count(Map query, String c) {
        return count(toJson(query), c);
    }

    public Number count(String query, String c) {
        LOG.debug("query:{}, on:{}", query, c);
        final long count = getCollection(c).count(StringUtils.isBlank(query) ? "{}" : query);
        return count;
    }

    public WriteResult deleteByQuery(Map query, String c) {

        final String json = toJson(query);
        return getCollection(c).remove(json);
    }

    public WriteResult deleteByQuery(Map query) {
        return this.deleteByQuery(query, this.getTabname());
    }

    public static Object[] getFilterConditionExpression(ReportRequest rrequest, ReportBean rbean) {
        AbsListReportFilterBean filterBean = rrequest.getCdb(rbean.getId()).getFilteredBean();
        if (filterBean == null)
            return null;
        String filterval = rrequest.getStringAttribute(filterBean.getId(), "");
        if (filterval.equals(""))
            return null;
        ColBean cbTmp = (ColBean) filterBean.getOwner();
        if (cbTmp.getDatatypeObj() == null || cbTmp.getDatatypeObj() instanceof VarcharType
                || cbTmp.getDatatypeObj() instanceof AbsDateTimeType) {// 这两种字段类型需要将条件值用'括住。
            filterval = Tools.replaceAll(filterval, ";;", "','");
            // if(!filterval.startsWith("'")) filterval="'"+filterval;
            // if(filterval.endsWith("','")) filterval=filterval.substring(0,filterval.length()-3);
            // if(!filterval.endsWith("'")) filterval=filterval+"'";
            // if(filterval.equals("'")) filterval="";
        } else {
            filterval = Tools.replaceAll(filterval, ";;", ",");
            if (filterval.endsWith(",")) {
                filterval = filterval.substring(0, filterval.length() - 1);
            }
        }
        String column = null;
        if (filterBean.getFilterColumnExpression() != null
                && !filterBean.getFilterColumnExpression().trim().equals("")) {
            column = filterBean.getFilterColumnExpression();
        } else {
            column = cbTmp.getColumn();
        }
        if (!filterval.trim().equals("")) {
            return new Object[] { column, filterval.split("[,;']+") };
        }
        return null;
    }

    public static String toJson(Object query) {
        try {
            if (query instanceof Map) {
                Object id = ((Map) query).get("_id");

                if (id instanceof String) {
                    ((Map) query).put("_id", new ObjectId((String) id));
                }
            }
            return getJsonMapper().writeValueAsString(query);
        } catch (JsonGenerationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (JsonMappingException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public Map jsonToMap(String jsonStr) {
        return jsonToObject(jsonStr, Map.class);
    }

    public <T> T jsonToObject(String jsonStr, Class<T> cls) {
        try {
            return (T) getJsonMapper().readValue(jsonStr, cls);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (JsonMappingException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void terminateOnAlert(String msg, Exception ex) {
        getMessageCollector().alert(msg, true);
    }

    public void terminateOnError(String msg, Exception ex) {
        getMessageCollector().error(msg, true);
    }

    protected MessageCollector getMessageCollector() {
        return this.rrequest.getWResponse().getMessageCollector();
    }

    public static ObjectMapper getJsonMapper() {
        return JsonMapperFactory.getJsonMapper();
    }

    public static Mapper getJongoMapper() {
        return JsonMapperFactory.getJongoMapper();
    }

    public int delete() {
        return delete(this.getPk());
    }

    public int delete(String idProp) {
        final Map attrs = attrs(idProp);
        if (attrs.isEmpty()) {
            throw new IllegalArgumentException("指定ID的的值为空!id=" + idProp);
        }

        return delete(attrs);
    }

    public int delete(Map query) {
        return delete(query, this.getTabname());
    }

    public int delete(Map query, String c) {
        return delete(toJson(query), c);
    }

    protected int delete(String query, String c) {
        LOG.debug("mongodb delete query:{}, on:{}", query, c);
        // return getCollection(c).remove(query);
        return this.updateOrDelete(query, c, null);
    }

    public int remove(String query, String c) {
        return delete(query, c);
    }

    public int update() {
        return update(this.getPk());
    }

    public int update(String idProp) {
        final AttrsBuilder newAttrsBuilder = this.newAttrsBuilder(true);
        Map atts = newAttrsBuilder.getAtts();
        final Object idVal = atts.remove(idProp);
        if (null == idVal || StringUtils.EMPTY.equals(idVal)) {
            throw new IllegalArgumentException("指定ID的的值为空!id=" + idProp);
        }
        return update(toMap(idProp, idVal), atts);
    }

    public Map<String, Object> toMap(String idProp, Object val) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(idProp, val);
        return map;
    }

    public Map jsonStrToMap(String json) {
        if (StringUtils.isBlank(json)) {
            return MapUtils.EMPTY_MAP;
        }
        return jsonToObject(json, Map.class);
    }

    public int update(Map query, Map data) {
        return update(query, data, this.getTabname());
    }

    protected boolean isHistoryRevFeature() {
        return BooleanUtils.toBoolean(getReportAttr("historyRevFeature"));
    }

    public int update(Map query, Map updateModifier, String c) {
        final String qjson = toJson(query);

        final String modifier = toJson(updateModifier);
        if (LOG.isDebugEnabled()) {
            LOG.debug("mongodb update query:{},modifier:{}, on:{}", new Object[] { qjson, modifier, c });
        }
        if (StringUtils.isBlank(modifier)) {
            throw new IllegalArgumentException("要修改的数据不能为空!");
        }
        return updateOrDelete(qjson, c, modifier);

    }

    /**
     * 进行更新或删除删除
     * 
     * @param qjson
     *            - 查询条件
     * @param c
     *            - 集合名称
     * @param modifier
     *            - 修改的数据,如果为null表示删除
     * @return
     */
    protected int updateOrDelete(final String qjson, String c, final String modifier) {
        if (StringUtils.isBlank(qjson)) {
            throw new IllegalArgumentException("查询条件不能为空!");
        }
        final MongoCollection collection = this.getCollection(c);
        final String historyRevTableName = getHistoryRevTableName();
        final boolean isDelete = modifier == null;
        if (historyRevTableName != null && isHistoryRevFeature()
                && !historyRevTableName.equals(collection.getName())) { // 启动历史版本功能且当前"表"不是历史数表时才记录版本
            final LoginInfoFinder loginInfoFinder = LoginInfoFinderImpl.getInstance();
            final HttpServletRequest request = this.rrequest.getRequest();
            final String clientAddress = loginInfoFinder.getClientAddress(request);
            final String uid = loginInfoFinder.getLoginedUid(request);
            final Find find = collection.find(qjson);
            final Iterator<Map> iterator = find.as(Map.class).iterator();
            int count = 0;
            while (iterator.hasNext()) {
                Map old = (Map) iterator.next();
                Object id = (Object) old.get("_id");
                if (id instanceof Map && ((Map) id).containsKey("$oid")) {
                    id = ((Map) id).get("$oid");
                }
                final WriteResult ret = isDelete ? collection.remove("{_id:#}", id) : collection.update(
                        "{_id:#}", id).with(modifier);
                // TODO更新是否成功

                final MongoCollection history = this.getCollection(historyRevTableName);
                old.remove("_id");
                final Iterator<Map> iter = history.find("{_entityId:#,_entityName:#}", id, c)
                        .sort("{_createDate:-1}").limit(1).as(Map.class).iterator();
                Map newOne = iter.hasNext() ? iter.next() : null;
                if (null == newOne) {
                    newOne = old;
                }
                newOne.remove("_id");
                if (true || (old == newOne || JsonDiff.hasDiff(old, newOne))) { // 记录版本库
                    if (old != newOne) {
                        newOne.putAll(old);
                    }
                    newOne.put("_entityId", id);
                    newOne.put("_entityName", c);
                    newOne.put("_op", isDelete ? "d" : "u");
                    if (null != clientAddress) {
                        newOne.put("_ip", clientAddress);
                    }
                    if (null != uid) {
                        newOne.put("_uid", uid);
                    }
                    Number rev = (Number) newOne.get("_rev");
                    if (null == rev) {
                        rev = Long.valueOf(0);
                    }
                    newOne.put("_rev", Long.valueOf(rev.longValue() + 1));
                    newOne.put("_createDate", new Date());
                    // newOne.put("comment", "");
                    history.insert(toJson(newOne));
                }
                count++;
            }
            return count;
        } else {
            final WriteResult ret = isDelete ? collection.remove(qjson) : collection.update(qjson).with(
                    modifier);
            return ret.getN();

        }
    }

    /**
     * @return 获取历史版本的表名
     */
    protected String getHistoryRevTableName() {
        final String name = this.rbean.getAttrs().get("historyRevRepo");
        return StringUtils.isNotBlank(name) ? name : Config.getInstance().getSystemConfigValue(
                "historyRevRepo", "historyRevRepo");
    }

    public WriteResult insert() {

        return insert(this.getPk());
    }

    /**
     * 
     * @param idProp
     *            - 主键属性名
     * @return 调用此方法等同于 insert( newAttrsBuilder(true).remove(idProp))
     */
    public WriteResult insert(String idProp) {
        final AttrsBuilder attrsBuilder = newAttrsBuilder(true).remove(idProp);
        return insert(attrsBuilder);
    }

    public WriteResult insert(AttrsBuilder aBuilder) {
        return insert(aBuilder.getAtts());
    }

    public WriteResult insert(Map data) {
        return insert(data, this.getTabname());
    }

    public WriteResult insert(Map data, String c) {
        final String json = toJson(data);
        LOG.debug("mongodb insert data:{}, on:{}", json, c);
        return this.getCollection(c).insert(json);
    }

    private static final Set<String> nonDbCols = new HashSet<String>();

    static {

        nonDbCols.add(Consts_Private.NON_FROMDB);
        nonDbCols.add(Consts_Private.NON_VALUE);
        nonDbCols.add(Consts_Private.SEQUENCE);
        nonDbCols.add(Consts_Private.COL_ROWSELECT);

        nonDbCols.add(Consts_Private.COL_ROWSELECT);
    }

    public void assertNotEmpty(Map map) {
        assertNotEmpty(map, null);
    }

    public void assertNotEmpty(Map map, String msg) {
        if (null == map || map.size() < 1) {
            throw new IllegalArgumentException(msg == null ? "对象不能为空!" : msg);
        }
    }

    /**
     * @return 产生报表查询条件Map
     */
    public Map<String, Object> getQueryConditionMap() {
        SqlBean sbean = rbean.getSbean();
        final List<ConditionBean> lstConditions = sbean.getLstConditions();
        Map<String, Object> ret = new HashMap<String, Object>();
        for (Iterator<ConditionBean> iterator = lstConditions.iterator(); iterator.hasNext();) {
            final ConditionBean cdbean = (ConditionBean) iterator.next();
            final ConditionExpressionBean conditionExpression = cdbean.getConditionExpression();
            final String expr = conditionExpression == null ? null : conditionExpression.getValue();
            if (StringUtils.isBlank(expr)) {
                continue;
            }

            try {
                Map vars = new HashMap();
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
                        final Object cv = entry.getValue();
                        if (entry.getKey() == null || cv == null
                                || (cv instanceof String && StringUtils.isBlank((String) cv))) {
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

            Map filter = new HashMap();

            Map filter1 = new HashMap();
            filter1.put("$in", filterConditionExpression[1]);

            filter.put(filterConditionExpression[0], filter1);
            Map nret = new HashMap();

            nret.put("$and", new Object[] { ret, filter });
            return nret;
        }
        return ret;
    }

    public List distinct(String colKey) {
        return distinct(colKey, MapUtils.EMPTY_MAP, this.getTabname());
    }

    public List distinct(String colKey, Map query) {
        return distinct(colKey, query, this.getTabname());
    }

    // ["deptno":["$regex":data+".*","$options":'i']]

    public class CustomQueryBuilder {

        // public QueryBuilder regex(Object regex)
        // {
        // return regex((String)regex);
        // }

        private QueryBuilder builder = new QueryBuilder();

        public CustomQueryBuilder regex(String key, String regex) {
            regex(key, regex, Pattern.CASE_INSENSITIVE);
            return this;
        }

        public CustomQueryBuilder put(String key) {
            builder.put(key);
            return this;
        }

        public CustomQueryBuilder eq(String key, String val) {
            builder.put(key).is(val);
            return this;
        }

        public Map toMap() {
            return builder.get().toMap();
        }

        public CustomQueryBuilder regex(String key, String regex, int flags) {

            if (StringUtils.isNotBlank(regex)) {
                builder.put(key).regex(Pattern.compile(regex, flags));
            }
            return this;
        }
    }

    public CustomQueryBuilder newQuery() {

        return new CustomQueryBuilder();
    }

    public List distinct(String colKey, boolean filterCondition) {
        Map<String, Object> map = null;
        if (filterCondition) {
            map = this.getQueryConditionMap();
        } else {
            map = new HashMap<String, Object>();
            final Map<String, String> notnull = new HashMap<String, String>();
            notnull.put("$ne", null);
            map.put(colKey, notnull);
        }
        return distinct(colKey, map, this.getTabname());
    }

    public List distinct(String colKey, Map query, String c) {
        if (LOG.isDebugEnabled()) {
            LOG.info("mongodb distinct  key:{}, on:{} query:{}", new Object[] { colKey, c, query });
        }
        final Distinct distinct = this.getCollection(c).distinct(colKey);
        if (query != null && !query.isEmpty()) {
            distinct.query(toJson(query));
        }
        return distinct.as(String.class);
    }

    public Map<String, Object> attrs(Collection<String> args) {
        final Map<String, Object> vars = new HashMap<String, Object>();
        for (String name : args) {
            vars.put(name, this.attr(name));
        }
        return vars;
    }

    public Map<String, Object> attrs(String propName) {
        return attrs(new String[] { propName });
    }

    public Map<String, Object> attrs(String[] args) {
        final Map<String, Object> vars = new HashMap<String, Object>();
        for (String name : args) {
            vars.put(name, this.attr(name));
        }
        return vars;
    }

    public Map<String, Object> getAttrsOfDbCols() {
        return attrs(this.getDbCols());
    }

    /**
     * 列属性构建器
     * 
     * @version $Id: MongoExprContext.java 3658 2013-06-30 06:18:13Z qxo $
     * @author qxo(qxodream@gmail.com)
     * @since 2012-12-8
     */
    public class AttrsBuilder {

        private Map attrs;

        private AttrsBuilder(Map attrs) {
            super();
            this.attrs = attrs;
        }

        public AttrsBuilder() {
            this(new HashMap());
        }

        public AttrsBuilder addDbCols() {
            attrs.putAll(getAttrsOfDbCols());
            return this;
        }

        public AttrsBuilder remove(String name) {
            attrs.remove(name);
            return this;
        }

        public AttrsBuilder remove(String[] names) {
            for (String n : names) {
                attrs.remove(n);
            }
            return this;
        }

        public AttrsBuilder remove(Collection<String> names) {
            for (String n : names) {
                attrs.remove(n);
            }
            return this;
        }

        public AttrsBuilder add(String name, String value) {
            attrs.put(name, value);
            return this;
        }

        public AttrsBuilder addAll(Map map) {
            attrs.putAll(map);
            return this;
        }

        public Map getAtts() {
            if (null == attrs) {
                return MapUtils.EMPTY_MAP;
            }
            final PojoMapper pojoMapper = getPojoMapper();
            if (null != pojoMapper) {
                return pojoMapper.convertToDbColsMap(attrs);
            }
            return attrs;
        }
    }

    /**
     * 
     * @param addCols
     *            - 是否添加所在的数据库列，如果true则添加
     * @return 创建一个AttrsBuilder
     */
    public AttrsBuilder newAttrsBuilder(boolean addCols) {
        final AttrsBuilder attrsBuilder = addCols ? new AttrsBuilder(this.getAttrsOfDbCols())
                : new AttrsBuilder();
        return attrsBuilder;
    }

    /**
     * @return 获取当前报表评估定久的所有数据库列名
     */
    public List<String> getDbCols() {
        final List<ColBean> lstCols = this.rbean.getDbean().getLstCols();
        final List<String> cols = new ArrayList<String>();

        for (ColBean colBean : lstCols) {
            final String column = colBean.getColumn();
            if (StringUtils.isNotBlank(column) && !nonDbCols.contains(column)) {
                final String property = colBean.getProperty();
                cols.add(StringUtils.isEmpty(property) ? column : property);
            }
        }
        return cols;
    }

    public Object getReqAttr(final ConditionBean cdbean) {
        return attr(cdbean.getName());
    }

    public Object attr(final String name) {

        Object data = this.getVars().get(name);
        if (data == null) {
            data = rrequest.getAttribute(name);
        }

        if (data instanceof String) {
            final ColBean col = this.rbean.getDbean().getColBeanByColColumn(name);
            if (null != col && col.getDatatypeObj() != null) {
                data = col.getDatatypeObj().label2value((String) data);
            }
        }
        return data;
    }

    // public List<Map<String, String>> findTypePrompts(CustomQueryBuilder qbuilder, String c,
    // AbsTypePromptDataSource typeObj) {
    // return findTypePrompts(qbuilder.toMap(), c, typeObj);
    // }
    //
    // public List<Map<String, String>> findTypePrompts(Map query, String c, AbsTypePromptDataSource
    // typeObj) {
    //
    // final List<TypePromptColBean> lstPColBeans = typeObj.getPromptConfigBean().getLstPColBeans();
    // final Map fields = new HashMap();
    // for (Iterator<TypePromptColBean> iterator = lstPColBeans.iterator(); iterator.hasNext();) {
    // TypePromptColBean typePromptColBean = (TypePromptColBean) iterator.next();
    // fields.put(typePromptColBean.getLabel(), 1);
    // }
    // final String json = toJson(query);
    //
    // if (lstPColBeans.size() == 1) {
    // String key = (String) fields.keySet().iterator().next();
    // List<Map<String, String>> retList = new ArrayList<Map<String, String>>();
    // final List<String> rList = getCollection(c).distinct(key).query(json).as(String.class);
    // for (Iterator<String> iterator = rList.iterator(); iterator.hasNext();) {
    // final Map<String, String> m = new HashMap<String, String>();
    // String value = iterator.next();
    // m.put(key, value);
    // retList.add(m);
    // }
    // return retList;
    // }
    // fields.put("_id", 0);
    // final Find find = getCollection(c).find(json).fields(toJson(fields));
    // final List asList = asList(find.as(HashMap.class).iterator());
    // return asList;
    // }

    private String pk;

    /**
     * @return 实体主键，一般采用 “_id"
     */
    public String getPk() {
        if (null == pk) {
            pk = this.rbean.getAttrs().get("pk");
            // final List<ColBean> lstCols = this.rbean.getDbean().getLstCols();
            if (StringUtils.isBlank(pk)) {
                throw new IllegalAccessError("在请report定义中pk属性中指定主键!");
            }
        }
        return pk;
    }

    public String getReportAttr(String key) {
        String pv = (String) this.getVars().get(key);
        if (null == pv) {
            pv = (String) this.getReportAttrs().get(key);
            // pv = (String) this.getVars().get(key);
        }
        if (null == pv) {
            // throw new IllegalArgumentException("不存在指定的报表属性key:" + key);
        }
        return pv;
    }

    /**
     * @return 获取所有的表名清单
     */
    public Collection<String> listTableNames(String excludePattern) {
        final Set<String> collectionNames = getDB().getCollectionNames();
        if (StringUtils.isBlank(excludePattern)) {
            return collectionNames;
        }
        return exclude(collectionNames, excludePattern);
    }

    public Collection<String> listTableNames() {
        final String exclude = "true".equals(this.rrequest.getAttribute("listAllTables")) ? null
                : "^system";
        return listTableNames(exclude);
    }

    protected DB getDB() {
        return this.getJongo().getDatabase();
    }

    /**
     * 转换集成到List<Map<String,String>>以合适"联想输入"的数据要求
     * 
     * @param list
     * @return
     */
    public static List<Map<String, String>> toPromptListMap(Collection<String> list) {
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        for (String val : list) {
            Map<String, String> item = new HashMap<String, String>();
            item.put("value", val);
            item.put("label", val);
            ret.add(item);
        }
        return ret;
    }

    /**
     * 转换集合为符合<option source=要求的数据列表
     * 
     * @param list
     * @return
     */
    // public static List<String[]> toOptionsList(Collection<String> list) {
    // List<String[]> ret = new ArrayList<String[]>();
    // for (String val : list) {
    // ret.add(new String[] { val, val });
    // }
    // return ret;
    // }
    //
    public static List<Map<String, String>> toOptionsList(Collection<String> list) {
        List<Map<String, String>> ret = Lists.newArrayList();
        for (String val : list) {
            ret.add(createOption(val, val));
        }
        return ret;
    }

    private static Map<String, String> createOption(String value, String label) {
        Map<String, String> option = Maps.newHashMap();
        option.put("value", value);
        option.put("label", label);
        return option;
    }

    public static List exclude(Collection<String> inputList, String pattern) {
        return filter(inputList, pattern, false);
    }

    public static List include(Collection<String> inputList, String pattern) {
        return filter(inputList, pattern, true);
    }

    public static List filter(Collection<String> inputList, String pattern, boolean include) {
        final List retList = new ArrayList(inputList.size());
        if (StringUtils.isBlank(pattern)) {
            retList.addAll(inputList);
        } else {
            final Pattern pa = Pattern.compile(pattern);
            for (final String s : inputList) {
                final boolean find = pa.matcher(s).find();
                if (include ? find : !find) {
                    retList.add(s);
                }
            }
        }
        return retList;
    }

    /**
     * 要想通过此方法获取spring bean同要在spring配置文件中如下配置: <code>
     *  	<bean class="com.branchitech.wabacus.WabacusBeanFactory" factory-method="setWabacusBeanFactory">
			<constructor-arg>
				<bean class="com.branchitech.wabacus.WabacusBeanFactorySpringImpl"/>
			</constructor-arg>
	</bean>
	</code>
     * 
     * @param beanId
     *            - beanId
     * @return 通过WabacusBeanFactory来获取指定beanId的对象
     * 
     * @see WabacusBeanFactory.getBean
     */
    public static <T> T getBean(String beanId) {
        final T bean = WabacusBeanFactory.getInstance().getBean(beanId);
        return bean;
    }

    public List toReportPojoList(Collection srcList) {
        return toReportPojoList(srcList, true);
    }

    public List toReportPojoList(Collection srcList, boolean doPaging) {

        if (srcList == null || srcList.size() < 1) {
            return ListUtils.EMPTY_LIST;
        }

        // filter
        Object[] filterConditionExpression = null;
        if (!"true".equals(this.rrequest.getAttribute("disabledOrderByFeature"))) {
            filterConditionExpression = getFilterConditionExpression(this.rrequest, this.rbean);
        }

        final List retList = new ArrayList();
        final Class reportPojoClass = this.getTheReportPojoClass();
        for (Object src : srcList) {
            Object dist = null;
            try {
                if (filterConditionExpression != null) {
                    final String pv = BeanUtils.getProperty(src, (String) filterConditionExpression[0]);
                    final Object[] filterVals = (Object[]) filterConditionExpression[1];
                    if (!ArrayUtils.contains(filterVals, pv)) {
                        continue;
                    }
                }
                dist = createPojoClassInstance(reportPojoClass);
                BeanUtils.copyProperties(dist, src);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
            retList.add(dist);
        }

        // orderby
        if (!"true".equals(this.rrequest.getAttribute("disabledOrderByFeature"))) {
            final String[] orderByArray = this.getOrderByArray();
            if (orderByArray != null && orderByArray.length == 2) {
                Comparator comparator = ComparableComparator.getInstance();
                if ("desc".equals(orderByArray[1])) {
                    comparator = ComparatorUtils.reversedComparator(comparator);
                }
                Collections.sort(retList, new BeanComparator(orderByArray[0], comparator));
            }
        }
        if (doPaging) {
            CacheDataBean cdb = getCacheDataBean();
            int pagesize = cdb.getPagesize();
            if (pagesize > 0) {
                int pageno = cdb.getFinalPageno();
                final int size = retList.size();
                setResultCount(size);
                if (pageno == 1 && size < pagesize) {
                    return retList;
                }
                int start = (pageno - 1) * pagesize;
                if (start < size) {
                    return retList.subList(start, start + pagesize > size ? size - 1 : start + pagesize);
                } else {
                    return ListUtils.EMPTY_LIST;
                }
            }
        }
        return retList;
    }

    public Map getExtraVars() {
        Map extraMap = (Map) rrequest.getAttribute("extraVars");
        return null == extraMap ? MapUtils.EMPTY_MAP : extraMap;
    }

    public void setResultCount(Number size) {
        initExtraVarsIf().put("resultCount", size);
    }

    public Map initExtraVarsIf() {
        return initExtraVarsIf(this.rrequest);
    }

    public static Map initExtraVarsIf(ReportRequest rrequest) {
        Map extraMap = (Map) rrequest.getAttribute("extraVars");
        if (null == extraMap) {
            extraMap = new HashMap();
            rrequest.setAttribute("extraVars", extraMap);
        }
        return extraMap;
    }
}