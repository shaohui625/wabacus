package com.wabacus.extra.mongodb;

import java.util.Map;

import org.jongo.ResultHandler;

/**
 * 
 * @version $Id: PojoMapper.java 3429 2013-01-25 02:17:15Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-18
 */
public interface PojoMapper {

	
	/**
	 * @return mongo 对象到报表pojo转换器
	 */
	ResultHandler getResultHandler();

	/**
	 * @param attrs
	 * @return
	 */
	Map convertToDbColsMap(Map attrs);
}
