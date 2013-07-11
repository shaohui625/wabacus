package com.wabacus.extra.expr;

import java.util.Map;

public abstract class AbstractQueryBuilder {

    public abstract Map toMap();

    public abstract AbstractQueryBuilder regex(String key, String regex);

    public final AbstractQueryBuilder like(String key, String value) {
        return this.regex(key, value);
    }
}
