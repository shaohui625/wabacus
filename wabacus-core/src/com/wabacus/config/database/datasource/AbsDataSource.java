/* 
 * Copyright (C) 2010---2013 星星(wuweixing)<349446658@qq.com>
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
package com.wabacus.config.database.datasource;

import java.sql.Connection;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.IConnection;
import com.wabacus.system.dataset.RuntimeQueryBuilder;

public abstract class AbsDataSource
{
    private String name;

    private AbsDatabaseType dbType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name=name;
    }

    public AbsDatabaseType getDbType()
    {
        return dbType;
    }

    public void setDbType(AbsDatabaseType dbType)
    {
        this.dbType=dbType;
    }
    
    public void closePool()
    {
    }
   
    //$ByQXO 数据存储可扩展性修改
    
    public  void loadConfig(Element eleDataSource)
    {
        
        String dbtype=getOverridePropertyValue("dbtype",eleDataSource.attributeValue("dbtype"));
        if(dbtype==null||dbtype.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("必须配置数据源的dbtype属性");
        }
        Class c;
        try
        {
            c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(dbtype.trim());
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("配置的dbtype："+dbtype+"，无法加载此类",e);
        }
        Object o;
        try
        {
            o=c.newInstance();
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("配置的dbtype："+dbtype+"，无法实例化此类的对象",e);
        }
        if(!(o instanceof AbsDatabaseType))
        {
            throw new WabacusConfigLoadingException("配置的dbtype："+dbtype+"对应的类没有继承"
                    +AbsDatabaseType.class.getName()+"超类");
        }
        this.dbType=(AbsDatabaseType)o;
        
        
        String runtimeQueryBuilderStr =getOverridePropertyValue("runtimeQueryBuilder",eleDataSource.attributeValue("runtimeQueryBuilder"));
        if(StringUtils.isNotBlank(runtimeQueryBuilderStr)){
            try
            {
                runtimeQueryBuilder =(RuntimeQueryBuilder)ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(runtimeQueryBuilderStr.trim()).newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("配置的runtimeQueryBuilder："+runtimeQueryBuilderStr+"，无法加载此类",e);
            }
        }        
        
    }
 
    // public abstract DataSource getDataSource();
    
    /**
     * @deprecated 改用getIConnection
     */
    public abstract Connection getConnection();
    
    public abstract IConnection getIConnection();

    
    private String prefix ;
    
    protected String getPrefix(){
        if( null == prefix){
            prefix = this.getName() +".";
        }
        return prefix;
    }

    /**
     *   获取覆盖的系统属性
     * @param name
     * @param defaultValue
     * @return
     */
    protected String getOverridePropertyValue(String name,String currentVal){
        return Config.getInstance().getPropertyOverrideLoader().getOverridePropertyValue(this.getPrefix(),name,currentVal) ;
    }
    
    private RuntimeQueryBuilder runtimeQueryBuilder;
    public final RuntimeQueryBuilder getRuntimeQueryBuilder(){
        return runtimeQueryBuilder == null ? RuntimeQueryBuilder.DUMMY : runtimeQueryBuilder;
    }
    //ByQXO$
}
