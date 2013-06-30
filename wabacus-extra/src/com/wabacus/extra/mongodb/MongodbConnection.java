package com.wabacus.extra.mongodb;

import java.sql.SQLException;

import org.jongo.Jongo;

import com.wabacus.extra.database.AbstractNoSqlConnection;

public class MongodbConnection extends AbstractNoSqlConnection
{

    private Jongo jongo;

    public Jongo getJongo()
    {
        return jongo;
    }

    public MongodbConnection(Jongo jongo)
    {
        super();
        this.jongo=jongo;
    }


}
