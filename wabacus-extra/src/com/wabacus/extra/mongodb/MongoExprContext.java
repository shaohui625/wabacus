package com.wabacus.extra.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jongo.Distinct;
import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jongo.MongoCollection;
import org.jongo.ResultHandler;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;
import com.mongodb.util.JSONSerializers;
import com.mongodb.util.ObjectSerializer;
import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.LoginInfoFinder;
import com.wabacus.extra.LoginInfoFinderImpl;
import com.wabacus.extra.expr.AbstractQueryBuilder;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.option.SQLOptionDatasource;

import de.undercouch.bson4jackson.types.ObjectId;
import foodev.jsondiff.JsonDiff;

/**
 * 
 * Mongo Executor (Shell ) Context;
 * 
 * @author qxo(qxodream@gmail.com)
 * 
 */
public class MongoExprContext extends AbstractWabacusScriptExprContext {

    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(MongoExprContext.class);

    protected MongoExprContext(ReportRequest rrequest, ReportBean rbean, ReportDataSetValueBean datasetbean) {
        super(rrequest, rbean, datasetbean);
    }

    /**
     * 通过此对象可获取Jongo对象
     */
    private MongodbConnection iconn;

    private MongodbConnection getMongodbConnection() {
        if (null == iconn) {
            iconn = (MongodbConnection) rrequest.getIConnection(rbean.getSbean().getDatasource());// FIXME
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

    protected static Mapper getJongoMapper() {
        return JsonMapperFactory.getJongoMapper();
    }

    @Override
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

    /**
     * 设置开始记录行及返回的最大数据行数
     * 
     * @param find
     * @return
     */
    public Find skipAndLimit(Find find) {

        final CacheDataBean cdb = getCacheDataBean();
        int pagesize = cdb.getPagesize();
        if (pagesize == -1) {
            return find;
        }
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

    public MongoCollection getCollection(String collectionsName) {
        if (StringUtils.isBlank(collectionsName)) {
            throw new NullArgumentException("collectionsName");
        }
        final MongoCollection collection = getJongo().getCollection(collectionsName);
        return collection;
    }

    @Override
    public List<Map<String, Object>> byQuery(final String query, Object... parameters) {
        return asList(getJongo().getCollection(this.getTabname()).find(query, parameters));
    }
    
    

    @Override
    public String toQueryStr() {
         return toJson(this.getQueryConditionMap());
    }

    public List asList(Find find) {
        // final Iterator iterator=find.as(getReportPojoClass()).iterator();
        final ResultHandler newMapper = getCurrentResultHandler();
        final Iterator iterator = find.map(newMapper).iterator();
        return IteratorUtils.toList(iterator);
    }

    @Override
    public Number count(Map query, String c) {
        return count(toJson(query), c);
    }

    public final Number count(String query) {
        return count(query, this.getTabname());
    }

    public Number count(String query, String c) {
        LOG.debug("query:{}, on:{}", query, c);
        final long count = getCollection(c).count(StringUtils.isBlank(query) ? "{}" : query);
        return count;
    }

    @Override
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

    public Map jsonStrToMap(String json) {
        if (StringUtils.isBlank(json)) {
            return MapUtils.EMPTY_MAP;
        }
        return jsonToObject(json, Map.class);
    }

    protected boolean isHistoryRevFeature() {
        return BooleanUtils.toBoolean(getReportAttr("historyRevFeature"));
    }

    @Override
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
                if (id instanceof ObjectId) {
                    ObjectId _id = (ObjectId) id;
                    id = new org.bson.types.ObjectId(_id.getTime(), _id.getMachine(), _id.getInc());
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

    public Object insert(Map data, String c) {
        final String json = toJson(data);
        LOG.debug("mongodb insert data:{}, on:{}", json, c);
        return this.getCollection(c).insert(json);
    }

    /**
     * @return 产生报表查询条件Map
     */
    public Map<String, Object> getQueryConditionMap(final Collection<String> excludes) {
        final SqlBean sbean = rbean.getSbean();
        final List<ConditionBean> lstConditions = sbean.getLstConditions();
        final Map<String, Object> ret = new HashMap<String, Object>();
        for (Iterator<ConditionBean> iterator = lstConditions.iterator(); iterator.hasNext();) {
            final ConditionBean cdbean = (ConditionBean) iterator.next();
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

    // ["deptno":["$regex":data+".*","$options":'i']]

    public class CustomQueryBuilder extends AbstractQueryBuilder {

        // public QueryBuilder regex(Object regex)
        // {
        // return regex((String)regex);
        // }

        private QueryBuilder builder = new QueryBuilder();

        public final AbstractQueryBuilder like(String key, String value) {
            return this.regex(key, value);
        }

        public CustomQueryBuilder regex(String key, String regex) {
            regex(key, regex, Pattern.CASE_INSENSITIVE);
            return this;
        }

        public CustomQueryBuilder put(String key) {
            builder.put(key);
            return this;
        }

        public CustomQueryBuilder putAll(Map<String, Object> map) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                builder.put(entry.getKey()).is(entry.getValue());
            }
            return this;
        }

        public CustomQueryBuilder eq(String key, String val) {
            builder.put(key).is(val);
            return this;
        }

        public Map toMap() {
            final DBObject dbObject = builder.get();
            // return dbObject.toMap();
            final ObjectSerializer jsonSerializer = JSONSerializers.getLegacy();
            final Set<String> keySet = dbObject.keySet();
            final Map<String, Object> vars = new HashMap<String, Object>();
            for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
                final String key = (String) iterator.next();
                final Object value = dbObject.get(key);
                // final String json = jsonSerializer.serialize( value);
                vars.put(key, value);
            }
            return vars;
        }

        public String toJson() {
            return builder.get().toString();
        }

        public CustomQueryBuilder regex(String key, String regex, int flags) {

            if (StringUtils.isNotBlank(regex)) {
                builder.put(key).regex(Pattern.compile(regex, flags));
            }
            return this;
        }
    }

    @Override
    public CustomQueryBuilder newQuery() {
        return new CustomQueryBuilder();
    }

    @Override
    public AbstractQueryBuilder newQuery(Map<String, Object> initMap) {
        final CustomQueryBuilder builder = new CustomQueryBuilder();
        builder.putAll(initMap);
        return builder;
    }

    @Override
    public List distinct(String colKey, boolean filterCondition) {
        Map<String, Object> map = null;
        if (!filterCondition) {
            map = this.getQueryConditionMap();
        } else {
            map = new HashMap<String, Object>();
            final Map<String, String> notnull = new HashMap<String, String>();
            notnull.put("$ne", null);
            map.put(colKey, notnull);
        }
        return distinct(colKey, map, this.getTabname());
    }

    @Override
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

    @Override
    public List<Map<String, String>> findTypePrompts(Map query, SQLOptionDatasource typeObj, String c) {
        final String json = toJson(query);
        return findTypePrompts(json, typeObj, c);
    }

    public List<Map<String, String>> findTypePrompts(String query, SQLOptionDatasource typeObj, String c) {
        TypePromptBean typePromptBean = ((TextBox) typeObj.getOwnerOptionBean().getOwnerInputboxObj())
                .getTypePromptBean();
        List<TypePromptColBean> lstPColBeans = typePromptBean.getLstPColBeans();
        final Map fields = new HashMap();
        for (Iterator<TypePromptColBean> iterator = lstPColBeans.iterator(); iterator.hasNext();) {
            TypePromptColBean typePromptColBean = (TypePromptColBean) iterator.next();
            fields.put(typePromptColBean.getLabel(), 1);
        }

        if (lstPColBeans.size() == 1) {
            String key = (String) fields.keySet().iterator().next();
            List<Map<String, String>> retList = new ArrayList<Map<String, String>>();
            final List<String> rList = getCollection(c).distinct(key).query(query).as(String.class);
            for (Iterator<String> iterator = rList.iterator(); iterator.hasNext();) {
                final Map<String, String> m = new HashMap<String, String>();
                String value = iterator.next();
                m.put(key, value);
                retList.add(m);
            }
            return retList;
        }
        fields.put("_id", 0);
        final Find find = getCollection(c).find(query).projection(toJson(fields));

        int typePromptLimit = typePromptBean.getResultcount();
        if (typePromptLimit < 1) {
            typePromptLimit = getTypePromptLimit();
        }
        find.limit(typePromptLimit);

        final List asList = asList(find.as(HashMap.class).iterator());
        return asList;
    }

    protected DB getDB() {
        return this.getJongo().getDatabase();
    }

}