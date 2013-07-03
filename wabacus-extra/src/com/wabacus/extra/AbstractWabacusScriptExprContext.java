package com.wabacus.extra;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.jongo.ResultHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JsonUtils;
import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.exception.MessageCollector;
import com.wabacus.extra.database.PojoMapper;
import com.wabacus.extra.expr.AbstractQueryBuilder;
import com.wabacus.extra.mongodb.JongoResultHandlerFactory;
import com.wabacus.extra.mongodb.JsonMapperFactory;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.system.inputbox.option.SQLOptionDatasource;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

/**
 * 抽象的上下文对象
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public abstract class AbstractWabacusScriptExprContext implements WabacusScriptExprContext {

    protected ReportRequest rrequest;

    protected ReportBean rbean;

    protected ReportDataSetValueBean datasetbean;

    public AbstractWabacusScriptExprContext(ReportRequest rrequest, ReportBean rbean,
            ReportDataSetValueBean datasetbean) {
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

    public Map getReportAttrs() {
        return this.rbean.getAttrs();
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

    public List asList(Object find) {
        final List ret = new ArrayList(1);
        ret.add(find);
        return ret;
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
     *      <bean class="com.branchitech.wabacus.WabacusBeanFactory" factory-method="setWabacusBeanFactory">
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

    public CacheDataBean getCacheDataBean() {

        return rrequest.getCdb(rbean.getId());
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

            String serialize = JSON.serialize(query);
            return serialize;// getJsonMapper().writeValueAsString(query);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
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

    /**
     * report的POJO对象类
     */
    private Class pojoClass;

    public Class getReportPojoClass() {
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

    public Object createPojoClassInstance(Class pojoClassObj) {
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

    protected Class loadClass(final String cls) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (null == classLoader) {
                classLoader = this.getClass().getClassLoader();
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
            newMapper = JongoResultHandlerFactory.newMapper(reportPojoClass, this);
        }
        return newMapper;
    }

    public final List distinct(String colKey) {
        return distinct(colKey, MapUtils.EMPTY_MAP, this.getTabname());
    }

    public final List distinct(String colKey, Map query) {
        return distinct(colKey, query, this.getTabname());
    }

    public abstract List distinct(String colKey, Map query, String c);

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

    private String pk;

    /**
     * @return 实体主键，一般采用 “_id"
     */
    public String getPk() {
        if (null == pk) {
            pk = this.rbean.getAttrs().get("pk");
            if (StringUtils.isBlank(pk)) {
                final List<ColBean> lstCols = this.rbean.getDbean().getLstCols();
                for (Iterator iterator = lstCols.iterator(); iterator.hasNext();) {
                    ColBean colBean = (ColBean) iterator.next();
                    if ("true".equals(colBean.getAttrs().get("pk"))) {
                        pk = colBean.getColumn();
                        break;
                    }
                }
            }
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

    public PojoMapper getPojoMapper() {
        final String pojoMapperCls = this.getReportAttr("pojoMapper");
        if (StringUtils.isBlank(pojoMapperCls)) {
            return null;
            // throw new IllegalArgumentException("报表参数pojoMapper未指定");
        }
        try {
            PojoMapper pojoMapper = (PojoMapper) loadClass(pojoMapperCls).getConstructor(
                    AbstractWabacusScriptExprContext.class).newInstance(this);
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

    protected String[] getOrderByArray() {
        String[] orderbys = (String[]) rrequest.getAttribute(rbean.getId(), "ORDERBYARRAY");
        return orderbys;
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

    public ColBean getColBeanByColumn(String p) {
        final List<ColBean> lstCols = this.rbean.getDbean().getLstCols();
        for (ColBean colBean : lstCols) {
            final String column = colBean.getColumn();
            if (StringUtils.isNotBlank(column) && column.equals(p)) {
                return colBean;
            }
        }
        return null;
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

    public final int delete() {
        return delete(this.getPk());
    }

    public final Object deleteByQuery(Map query, String c) {
        return this.delete(query, c);
    }

    public final Object deleteByQuery(Map query) {
        return this.deleteByQuery(query, this.getTabname());
    }

    public final int delete(String idProp) {
        final Map attrs = attrs(idProp);
        if (attrs.isEmpty()) {
            throw new IllegalArgumentException("指定ID的的值为空!id=" + idProp);
        }
        return delete(attrs);
    }

    public final int delete(Map query) {
        return delete(query, this.getTabname());
    }

    public abstract int delete(Map data, String c);

    public final int update() {
        return update(this.getPk());
    }

    public final int update(String idProp) {
        final AttrsBuilder newAttrsBuilder = this.newAttrsBuilder(true);
        Map atts = newAttrsBuilder.getAtts();
        final Object idVal = atts.remove(idProp);

        if (null == idVal || StringUtils.EMPTY.equals(idVal)) {
            throw new IllegalArgumentException("指定ID的的值为空!id=" + idProp);
        }
        return update(toMap(idProp, idVal), atts);
    }

    public final Map<String, Object> toMap(String idProp, Object val) {
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

    /**
     * 根据查询条件获取符合条件的数据
     * 
     * @param query
     * @return
     */
    public final List findAsList(Map query) {
        return findAsList(query, this.getTabname());
    }

    public abstract List findAsList(Map mongoQuery, String c);

    /**
     * 返回符合条的记录数
     * 
     * @param query
     * @return
     */
    public final Number count(Map query) {
        return count(query, this.getTabname());
    }

    public abstract Number count(Map query, String c);

    public final int update(Map query, Map data) {
        return update(query, data, this.getTabname());
    }

    public final Object insert() {
        return insert(this.getPk());
    }

    /**
     * 
     * @param idProp
     *            - 主键属性名
     * @return 调用此方法等同于 insert( newAttrsBuilder(true).remove(idProp))
     */
    public final Object insert(String idProp) {
        final AttrsBuilder attrsBuilder = newAttrsBuilder(true);
        // .remove(idProp);
        Map atts = attrsBuilder.getAtts();
        Object idValue = atts.get(idProp);
        if (null == idValue) {
            ColBean colBean = this.getColBeanByColumn(idProp);
            String idGenerator = colBean.getAttrs().get("idGenerator");
            if ("UUID".equals(idGenerator)) {
                atts.put(idProp, UUID.randomUUID().toString());
            }
        }
        return insert(atts);
    }

    public final Object insert(AttrsBuilder aBuilder) {
        return insert(aBuilder.getAtts());
    }

    public final Object insert(Map data) {
        return insert(data, this.getTabname());
    }

    public abstract Object insert(Map data, String tablename);

    /**
     * 
     * @param query
     *            - 查询条件
     * @param data
     *            - 要更新的数据
     * @param tabname
     *            - 表名
     * @return
     */
    public abstract int update(Map query, Map data, String tabname);

    public abstract AbstractQueryBuilder newQuery();

    public final List<Map<String, String>> findTypePrompts(AbstractQueryBuilder qbuilder, String c,
            SQLOptionDatasource typeObj) {
        return findTypePrompts(qbuilder.toMap(), c, typeObj);
    }

    public abstract List<Map<String, String>> findTypePrompts(Map query, String c,
            SQLOptionDatasource typeObj);

    public final int getTypePromptLimit() {
        int typePromptLimit = Config.getInstance().getSystemConfigValue("typePromptsLimit", 15);
        return typePromptLimit;
    }
}