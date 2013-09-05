package com.wabacus.extra;

/**
 * wabacus扩展点接口
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-6-30
 */
public final class WabacusScriptEngineHelper {

    /**
     * @return 当前使用的脚本引擎
     */
    public static ScriptEngine getScriptEngine() {
        return ScriptEngineFactory.getScriptEngine();
    }

    private static transient WabacusScriptExprContextFactory scriptExprContextFactory = null;

    public static WabacusScriptExprContextFactory getScriptExprContextFactory() {
        if (null == scriptExprContextFactory) {
            scriptExprContextFactory = SystemHelper.loadServiceIf(WabacusScriptExprContextFactory.class,
                    null);
            if (null == scriptExprContextFactory) {
                scriptExprContextFactory = new WabacusScriptExprContextFactoryDefult();
            }
        }
        return scriptExprContextFactory;
    }
}
