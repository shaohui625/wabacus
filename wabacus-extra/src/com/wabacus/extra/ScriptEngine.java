package com.wabacus.extra;

import java.util.Map;

/**
 * 脚本引擎接口
 *
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-1-29
 */
public interface ScriptEngine {
    /**
     * 运行脚本返回结果
     *
     * @param expression
     * @param ctx
     * @param vars
     * @return
     */
    Object eval(String expression, Object ctx, Map<String, Object> vars);
}
