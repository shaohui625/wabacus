package com.wabacus.extra;

/**
 * Json转换适配器
 * 
 * @version $Id$
 * @author qxo
 * @since 2013-10-11
 */
public interface JsonAdapter {

    /**
     * @param query
     *            -
     * @return 转换java对象为json字符串
     */
    String toJson(Object query);
}
