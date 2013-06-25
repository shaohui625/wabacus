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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wabacus.config.PropertyOverrideLoader;
import com.wabacus.config.ResourceUtils;
import com.wabacus.config.PropertyOverrideLoader.DEFINED_TYPE;

/**
 * 默认的全局配置加载器
 * @author qxo(qxodream@gmail.com)
 *
 */
public final class PropertyOverrideLoaderDefault implements PropertyOverrideLoader
{
    /**
    * Logger for this class
    */
    private static final Logger LOG=LoggerFactory.getLogger(PropertyOverrideLoaderDefault.class);

    private Properties props;

    /**
     * 类路径下的属性文件资源,如GlobalConf.properties
     */
    public PropertyOverrideLoaderDefault(String propertyfile)
    {
        this(loadProperties(propertyfile));
    }

    public PropertyOverrideLoaderDefault(final InputStream in)
    {
        this(loadProperties(in));
    }

    protected static Properties loadProperties(String propertyfile)
    {
        final InputStream in=loadResource(propertyfile);
        if(null==in)
        {
            LOG.warn("can not find propertyfile({}) in classpath",propertyfile);
            return new Properties();
        }
        return loadProperties(in);
    }

    protected static Properties loadProperties(final InputStream in)
    {
        final Properties props=new Properties();
        if(null!=in)
        {
            try
            {
                props.load(in);
            }catch(IOException e)
            {
                throw new IllegalArgumentException(e.getMessage(),e);
            }
        }
        return props;
    }

    public PropertyOverrideLoaderDefault(Properties properties)
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug("properties:{}",properties);
        }
        this.props=properties;
    }

    protected static InputStream loadResource(String propertyfile)
    {
        final ClassLoader contextClassLoader=getClassLoader();
        final InputStream as=contextClassLoader.getResourceAsStream(propertyfile);
        return as==null?getInpuStream(propertyfile):as;
    }

    protected static ClassLoader getClassLoader()
    {
        return ResourceUtils.getClassLoader();
    }

    public static PropertyOverrideLoader createPropertyOverrideLoader(String key)
    {
     return    createPropertyOverrideLoader(key,null);
    }
    public static PropertyOverrideLoader createPropertyOverrideLoader(String key,String defaultLoader)
    {
        String loader=System.getProperty(key,System.getenv(key));
        if( null == loader ){
            loader = defaultLoader;
        }
        PropertyOverrideLoader ploader=PropertyOverrideLoader.DEFINED_TYPE.getPropertyOverrideLoader(loader);
        LOG.info("PropertyOverrideLoader:{}",loader);
        if(ploader==null&&loader!=null)
        {
            
            if("globalConf".equalsIgnoreCase(loader))
            { // load from default globalConf
                ploader=new PropertyOverrideLoaderDefault();
            }else if(loader.endsWith(".properties")||ResourceUtils.isUrl(loader))
            {//load from classpath or url
                final InputStream in=getInpuStream(loader);
                ploader=new PropertyOverrideLoaderDefault(in);
            }else
            {
                try
                {
                    ploader=(PropertyOverrideLoader)getClassLoader().loadClass(loader).newInstance();
                }catch(InstantiationException e)
                {
                    throw new IllegalArgumentException(e.getMessage(),e);
                }catch(IllegalAccessException e)
                {
                    throw new IllegalArgumentException(e.getMessage(),e);
                }catch(ClassNotFoundException e)
                {
                    throw new IllegalArgumentException(e.getMessage(),e);
                }
            }
        }

        if(null==ploader)
        {
            ploader=PropertyOverrideLoader.NOP;
        }
        return ploader;
    }

    protected static InputStream getInpuStream(String loader)
    {
        try
        {
            final boolean url=ResourceUtils.isUrl(loader);
            InputStream in=url?null:ResourceUtils.getClassLoader().getResourceAsStream(loader);
            if(in!=null)
            {
                return in;
            }
            final File file=url?ResourceUtils.getFile(loader):new File(loader);
            if(!file.exists())
            {
                LOG.warn("全局配置文件({})不存在",file);
                return null;
            }
            in=new FileInputStream(file);
            return in;
        }catch(FileNotFoundException e)
        {
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }

    public PropertyOverrideLoaderDefault()
    {
        this("GlobalConf.properties");
    }

    public static String getName(String prefix,String name)
    {
        return null==prefix?name:prefix+name;
    }

    public String getOverridePropertyValue(String prefix,String name,String currentVal)
    {
        return props.getProperty(getName(prefix,name),currentVal);
    }
}
