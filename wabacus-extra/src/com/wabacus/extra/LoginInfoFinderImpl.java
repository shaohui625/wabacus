package com.wabacus.extra;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.wabacus.WabacusBeanFactory;
import com.wabacus.config.Config;

/**
 * 
 * 获取用户登录信息的默认实现
 * 
 * @version $Id: LoginInfoFinderImpl.java 3405 2012-12-21 02:59:31Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-7-10
 */
public final class LoginInfoFinderImpl implements LoginInfoFinder {

	private static final String NONE = "";
	private String headName = null;

	public String getHeadName() {
		if (null == headName) {
			// 从配置
			headName = Config.getInstance().getPropertyOverrideLoader().getOverridePropertyValue(null, "clientIpHeader", NONE);
		}
		return headName;
	}

	public static LoginInfoFinder INSTANCE;

	/**
	 * @return 获取LoginInfoFinder
	 */
	public static LoginInfoFinder getInstance() {
		if (null == INSTANCE) {
			INSTANCE = WabacusBeanFactory.getInstance().getBean("loginInfoFinder");
			if (null == INSTANCE) {
				INSTANCE = new LoginInfoFinderImpl();
			}
		}
		return INSTANCE;
	}

	public void setHeadName(String headName) {
		if (StringUtils.isNotBlank(headName)) {
			this.headName = headName;
		}
	}

	/**
	 * 获取IP地址,如果有array等设备的话请在全局配置中设置正确的属性"clientIpHeader",如果没设置则取request.getRemoteAddr为当前的客户端地址
	 */
	public String getClientAddress(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		final String hn = getHeadName();
		String addr = NONE.equals(hn) ? null : request.getHeader(hn);
		return addr == null ? request.getRemoteAddr() : addr;
	}

	public String getLoginedUid(HttpServletRequest request) {
		return request.getRemoteUser();
	}
}
