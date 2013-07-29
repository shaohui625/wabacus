package com.wabacus.extra.expr;

import java.util.Map;

public abstract class AbstractQueryBuilder {

    public abstract Map toMap();

    public  abstract AbstractQueryBuilder like(String key, String value);
}
