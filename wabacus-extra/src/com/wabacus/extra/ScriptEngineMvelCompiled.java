package com.wabacus.extra;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.lang.StringUtils;
import org.mvel2.MVEL;

/**
 * 
 * 采用MVEL来实现
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public final class ScriptEngineMvelCompiled implements ScriptEngine {

    private transient Map<String, SoftReference<Serializable>> cache = new ReferenceMap();

    // HashMap<String, SoftReference<Serializable>>();

    public Object eval(final String expression, final Object ctx, final Map<String, Object> vars) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }
        final String str = expression.trim();
        final SoftReference<Serializable> ref = cache.get(str);
        Serializable compileExpression = ref == null ? null : ref.get();
        if (null == compileExpression) {
            compileExpression = MVEL.compileExpression(str);
            cache.put(str, new SoftReference<Serializable>(compileExpression));
        }
        return MVEL.executeExpression(compileExpression, ctx, vars);
    }
}
