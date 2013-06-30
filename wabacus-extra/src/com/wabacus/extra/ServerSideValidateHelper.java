package com.wabacus.extra;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.wabacus.extra.mongodb.JsonMapperFactory;

/**
 * wabacus 输入服务端验证工具类<br/>
 * 
 * 先将此校验类在报表系统配置文件wabacus.cfg.xml 的system元素下server -validate-class 配置项中进行注册，里面的校验方法才能使用
 * eg: 
 * <item name="server-validate-class">com.branchitech.wabacus.ServerSideValidateHelper</item>
 * @version $Id: ServerSideValidateHelper.java 3430 2013-01-25 02:20:29Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-20
 */
public final class ServerSideValidateHelper {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ServerSideValidateHelper.class);

	/**
	 * 此为工具类不需要实例化
	 */
	private ServerSideValidateHelper() {
		super();
	}

	public static boolean isJson(String input) {

		if (StringUtils.isNotBlank(input)) {
			try {
				final Object obj = JsonMapperFactory.getJsonMapper().readValue(input, Map.class);
				return obj != null;
			} catch (JsonParseException e) {
				LOG.warn("{}", e.getMessage(), e);
			} catch (JsonMappingException e) {
				LOG.warn("{}", e.getMessage(), e);
			} catch (IOException e) {
				LOG.warn("{}", e.getMessage(), e);
			}
		}
		return false;

	}
}
