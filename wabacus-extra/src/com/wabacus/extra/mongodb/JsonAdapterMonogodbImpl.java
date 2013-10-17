package com.wabacus.extra.mongodb;

import com.mongodb.util.JSON;
import com.wabacus.extra.JsonAdapter;

/**
 * 
 * 
 * @version $Id$
 * @author qxo
 * @since 2013-10-11
 */
public final class JsonAdapterMonogodbImpl implements JsonAdapter {

    public String toJson(Object query) {
        final String serialize = JSON.serialize(query);
        return serialize;
    }

}
