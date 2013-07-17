package com.wabacus.extra;

import java.util.Collection;

import com.wabacus.config.Config;

/**
 * 
 * 
 * @version $Id: WabacusHelper.java 3430 2013-01-25 02:20:29Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-7
 */
public final class WabacusHelper {

    private WabacusHelper() {
        super();
    }

    public static Collection<String> getPageIds() {
        return Config.getInstance().getPageIds();
    }
}
