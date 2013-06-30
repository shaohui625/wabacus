package com.wabacus.extra;

import java.util.Map;

import org.mvel2.MVEL;

/**
 * 
 * 采用MVEL来实现
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public class ScriptEngineMvel implements ScriptEngine {

    public Object eval(String expression, Object ctx, Map<String, Object> vars) {
        return MVEL.eval(expression, ctx, vars);
    }
}
