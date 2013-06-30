package com.wabacus.extra;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户登录信息查找器
 * 
 * @version $Id: LoginInfoFinder.java 3405 2012-12-21 02:59:31Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-21
 */
public interface LoginInfoFinder {

	public String getClientAddress(HttpServletRequest request);

	public String getLoginedUid(HttpServletRequest request);
}
