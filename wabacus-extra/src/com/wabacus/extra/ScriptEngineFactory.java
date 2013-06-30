package com.wabacus.extra;

/**
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-1-29
 */
public final class ScriptEngineFactory {

    private static transient ScriptEngine scriptEngine = null;

    /**
     * @return
     */
    public static ScriptEngine getScriptEngine() {
        if (null == scriptEngine) {
            scriptEngine = SystemHelper.loadServiceIf(ScriptEngine.class, null);
            if (null == scriptEngine) {
                scriptEngine = new ScriptEngineMvel();
            }
        }
        return scriptEngine;
    }
}