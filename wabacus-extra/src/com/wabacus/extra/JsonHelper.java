package com.wabacus.extra;

/**
 * 
 * 
 * @version $Id$
 * @author qxo
 * @since 2013-10-11
 */
public final class JsonHelper {

    private JsonHelper() {
    }

    public static void main(String[] args) {
        
        JsonHelper.toJson("ABC");
    }
    private static final class Holder {
        public static final JsonAdapter ADPATER = SystemHelper.loadService(JsonAdapter.class, null);
    }

    public static final JsonAdapter getJsonAdapter() {
        return Holder.ADPATER;
    }

    public static final String toJson(Object query) {
        return getJsonAdapter().toJson(query);
    }
}
