package com.wabacus.extra.database;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jongo.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wabacus.extra.AbstractWabacusScriptExprContext;
import com.wabacus.extra.AlterException;

/**
 * 此转换器负责转换报表Pojo和mongo对象(json)的相互转换 用于直接转换mongo对象到pojo的一个属性中
 * 
 * @version $Id: PojoJsonContentMapper.java 3658 2013-06-30 06:18:13Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-18
 */
public class PojoJsonContentMapper implements PojoMapper {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PojoJsonContentMapper.class);

	private Class pojoClass;

	private String jsonContentProp;

	private String idProp;

	private AbstractWabacusScriptExprContext context;

	public PojoJsonContentMapper(AbstractWabacusScriptExprContext context) {
		super();
		this.context = context;
		pojoClass = context.getReportPojoClass();
		jsonContentProp = context.getReportAttr("jsonContentProp");

		idProp = context.getReportAttr("pk");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.branchitech.wabacus.mongodb.PojoMapper#getResultHandler()
	 */
	public ResultHandler getResultHandler() {
		return context.createJsonResultHandler(jsonContentProp, idProp, pojoClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.branchitech.wabacus.mongodb.PojoMapper#convertToDbColsMap(java.util.Map)
	 */
	public Map convertToDbColsMap(Map attrs) {
		// attrs.containsKey(this.idProp) &&
		if (attrs.containsKey(this.jsonContentProp)) {
			final String json = (String) attrs.get(this.jsonContentProp);
			if(StringUtils.isBlank(json)){
				throw new AlterException("参数:" + jsonContentProp + " 不有效的json数据!");
			}
			try {
				Map map = this.context.jsonToMap(json);
				if (attrs.containsKey(this.idProp)) {
					map.put(this.idProp, attrs.get(this.idProp));
				}
				return map;
			} catch (IllegalArgumentException ex) {
				LOG.error("参数:{} 不有效的json数据!",jsonContentProp);
				throw new AlterException("参数:" + jsonContentProp + " 不有效的json数据!");
				//context.terminateOnAlert("参数:" + jsonContentProp + " 不有效的json数据!", ex);
			}

		}
		return attrs;
	}
}
