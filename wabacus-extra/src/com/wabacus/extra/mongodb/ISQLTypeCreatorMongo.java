package com.wabacus.extra.mongodb;

import com.wabacus.system.dataset.ISqlDataSet;
import com.wabacus.system.dataset.ISqlDataSetCreator;

/**
 * 
 *
 * @version $Id: ISQLTypeCreatorMongo.java 3638 2013-05-12 15:10:22Z qxo $
 * @author qxo(qxodream@gmail.com) 
 * @since  2013-5-11
 */
public class ISQLTypeCreatorMongo implements ISqlDataSetCreator
{

    public ISqlDataSet createAllDataSet()
    {
        return new MongodbGetAllResultSet();
    }

    public ISqlDataSet createPartDataSet()
    {
        return new MongoGetPartResultSet();
    }

}