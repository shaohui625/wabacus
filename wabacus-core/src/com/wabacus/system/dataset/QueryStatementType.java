/* 
 * Copyright (C) 2010---2012 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.system.dataset;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.wabacus.config.ResourceUtils;

//$ByQXO NEW  数据库兼容性修改     //ByQXO$
/**
 *  查询语句的类型，也是就report下面的sql元素的type,
 *  以表过查询语句的实现方式：是PreparedstatementSql和存储过程或自定义的实现
 * @author qxo(qxodream@gmail.com)
 *
 */
public enum QueryStatementType {

    PREPARED_STATEMENT("PreparedStatement", ISqlDataSetCreator.PREPARED_STATEMENT_CREATOR, "preparedstatement"), STATEMENT("Statement",
            ISqlDataSetCreator.STATEMENT_CREATOR, "statement"), STORED_PROCEDURE("StoredProcedure", ISqlDataSetCreator.STORED_PROCEDURE_CREATOR), CUSTOM(
            "Custom", null);

    private String label;

    public String getLabel()
    {
        return label;
    }

    private QueryStatementType(String name)
    {
        this.label=name;
    }

    private static final Map<String,QueryStatementType> maps=new HashMap<String,QueryStatementType>();

    private static final Map<String,ISqlDataSetCreator> creatorMap=new HashMap<String,ISqlDataSetCreator>();
    static
    {
        final QueryStatementType[] values=QueryStatementType.values();

        for(int i=0;i<values.length;i++)
        {
            final QueryStatementType type=values[i];
            maps.put(type.name(),type);
            if(!type.name().equals(type.label))
            {
                if(maps.containsKey(type.label))
                {
                    throw new IllegalStateException("QueryStatementType定义重复值！");
                }
                for(Iterator<String> iterator=type.aliasList.iterator();iterator.hasNext();)
                {
                    maps.put(iterator.next(),type);
                }

            }
            if(type.sqlTypeCreator!=null)
            {
                creatorMap.put(type.name(),type.sqlTypeCreator);
            }
        }

    }

    public static void registerISQLTypeCreator(String alias,ISqlDataSetCreator sqlTypeCreator)
    {
        if(!creatorMap.containsKey(alias))
        {
            //TODO
        }
        creatorMap.put(alias,sqlTypeCreator);
    }

    private ISqlDataSetCreator sqlTypeCreator;

    private QueryStatementType(String label,ISqlDataSetCreator sqlTypeCreator)
    {
        this(label,sqlTypeCreator,null);
    }

    private Set<String> aliasList;

    private QueryStatementType(String label,ISqlDataSetCreator sqlTypeCreator,String aliases)
    {
        this.label=label;
        this.sqlTypeCreator=sqlTypeCreator;
        aliasList=new HashSet<String>();
        if(aliases!=null&&aliases.length()>0)
        {
            aliases=aliases.trim();
            if(aliases.length()>0)
            {
                aliasList.addAll(Arrays.asList(aliases.trim().split("[,;]+")));
            }
        }
        aliasList.add(label);

    }

    public static <T> T newInstance(final Class<T> cls)
    {
        try
        {
            return cls.newInstance();
        }catch(InstantiationException e)
        {

            throw new IllegalStateException(e.getMessage(),e);

        }catch(IllegalAccessException e)
        {
            throw new IllegalStateException(e.getMessage(),e);
        }
    }

    public static ISqlDataSetCreator getISQLTypeCreator(ISqlDataSetCreator sqlTypeCreator,String typeValue)
    {

        ISqlDataSetCreator isqlTypeCreator=null;

        if(null==isqlTypeCreator&&typeValue!=null&&typeValue.length()>0)
        {
            isqlTypeCreator=creatorMap.get(typeValue);
            if(null==isqlTypeCreator)
            {
                try
                {
                    final Class<ISqlDataSetCreator> cls=ResourceUtils.loadClass(typeValue);
                    isqlTypeCreator=newInstance(cls);
                }catch(ClassNotFoundException e)
                {
                    throw new IllegalStateException(e.getMessage(),e);
                }
            }
        }
        return isqlTypeCreator==null?ISqlDataSetCreator.STATEMENT_CREATOR:isqlTypeCreator;
    }

    /**
     * @return 此QueryStatmentType对应的ISqlDataSetCreator
     */
    public ISqlDataSetCreator getDateSetCreator()
    {
        return sqlTypeCreator;
    }

    public static QueryStatementType getType(String value)
    {
        final QueryStatementType type=maps.get(value);
        return type==null?CUSTOM:type;
        //Enum.valueOf(QueryStatmentType.class,,name)
    }

}