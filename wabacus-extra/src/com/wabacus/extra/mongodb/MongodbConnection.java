package com.wabacus.extra.mongodb;

import org.jongo.Jongo;

import com.wabacus.extra.database.AbstractNoSqlConnection;

public class MongodbConnection extends AbstractNoSqlConnection {

    private Jongo jongo;

    public Jongo getJongo() {
        return jongo;
    }

    protected MongodbConnection(Jongo jongo) {
        super();
        this.jongo = jongo;
    }
}