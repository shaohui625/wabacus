package com.wabacus.extra.expr;

import com.wabacus.system.dataset.ISqlDataSet;
import com.wabacus.system.dataset.ISqlDataSetCreator;

/**
 * 
 *
 * @version $Id: ISQLTypeCreatorMongo.java 3638 2013-05-12 15:10:22Z qxo $
 * @author qxo(qxodream@gmail.com) 
 * @since  2013-5-11
 */
public class ExprISQLTypeCreator implements ISqlDataSetCreator
{

    public ISqlDataSet createAllDataSet()
    {
        return new ExprGetAllResultSet();
    }

    public ISqlDataSet createPartDataSet()
    {
        return new ExprGetPartResultSet();
    }
}