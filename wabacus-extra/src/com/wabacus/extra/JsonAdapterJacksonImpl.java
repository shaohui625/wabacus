package com.wabacus.extra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * 
 * @version $Id$
 * @author qxo
 * @since 2013-10-11
 */
public final class JsonAdapterJacksonImpl implements JsonAdapter {

    private ObjectMapper mapper = new ObjectMapper();

    public String toJson(Object query) {
        try {
            return query == null ? null : mapper.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
