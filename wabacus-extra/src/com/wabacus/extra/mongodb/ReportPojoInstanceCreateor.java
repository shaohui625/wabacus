package com.wabacus.extra.mongodb;

/**
 * 报表POJO类实例化
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-28
 */
public interface ReportPojoInstanceCreateor {

    public Object getPojoClassInstance(Class pojoClass);
}
