/* 
 * Copyright (C) 2010-2012 吴卫华(wuweihua)<349446658@qq.com>
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
package com.wabacus.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局属性配置加载器:用于把具体环境相关的配置(如数据库IP,帐号,密码等)放入共享类库中外置,以使同一发布包能在多个环境共用
 * @author qxo(qxodream@gmail.com)
 *
 */
public interface PropertyOverrideLoader
{

    /**
     * 默认实现取原来的值,以保留兼容
     */
    PropertyOverrideLoader NOP=new PropertyOverrideLoader()
    {

        /* (non-Javadoc)
         * @see com.wabacus.config.database.datasource.PropertyOverrideLoader#getOverridePropertyValue(java.lang.String, java.lang.String, java.lang.String)
         */
        public String getOverridePropertyValue(String prefix,String name,String currentVal)
        {
            return currentVal;
        }
    };

    /**
     * 如果系统属性中存指定的prefix+name存在,则此采用系统属性的值,否则取原来配置设置的值
     */
    PropertyOverrideLoader SYSTEM_PROPERTY_FIRST=new PropertyOverrideLoader()
    {

        /* (non-Javadoc)
         * @see com.wabacus.config.database.datasource.PropertyOverrideLoader#getOverridePropertyValue(java.lang.String, java.lang.String, java.lang.String)
         */
        public String getOverridePropertyValue(String prefix,String name,String currentVal)
        {

            return System.getProperty(PropertyOverrideLoaderDefault.getName(prefix,name),currentVal);
        }
    };

    /**
     * 如果系统属性中存指定的prefix+name存在,则此采用系统属性的值,否则取原来配置设置的值
     */
    PropertyOverrideLoader ENV_VAR_FIRST=new PropertyOverrideLoader()
    {

        /* (non-Javadoc)
         * @see com.wabacus.config.database.datasource.PropertyOverrideLoader#getOverridePropertyValue(java.lang.String, java.lang.String, java.lang.String)
         */
        public String getOverridePropertyValue(String prefix,String name,String currentVal)
        {
            final String env=System.getenv(PropertyOverrideLoaderDefault.getName(prefix,name));
            return env==null?currentVal:env;
        }
    };

    enum DEFINED_TYPE {
        NOP(PropertyOverrideLoader.NOP), SYSTEM_PROPERTY_FIRST(PropertyOverrideLoader.SYSTEM_PROPERTY_FIRST), ENV_VAR_FIRST(
                PropertyOverrideLoader.ENV_VAR_FIRST);

        private PropertyOverrideLoader ploader;

        private static final Map<String,PropertyOverrideLoader> map;

        static
        {
            final DEFINED_TYPE[] values=DEFINED_TYPE.values();
            map=new HashMap<String,PropertyOverrideLoader>(values.length);
            for(DEFINED_TYPE type:values)
            {
                map.put(type.name(),type.ploader);

            }
        }

        private DEFINED_TYPE(PropertyOverrideLoader ploader)
        {
            this.ploader=ploader;
        }

        public PropertyOverrideLoader getPropertyOverrideLoader()
        {
            return ploader;
        }

        public static PropertyOverrideLoader getPropertyOverrideLoader(String enumName)
        {
            return enumName==null?null:map.get(enumName);
        }
    }

    /**
     * 
     * @param prefix
     * @param name
     * @param currentVal
     * @return
     */
    String getOverridePropertyValue(String prefix,String name,String currentVal);
}
