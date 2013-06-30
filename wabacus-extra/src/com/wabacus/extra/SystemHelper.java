package com.wabacus.extra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Service;

import com.google.common.collect.Lists;

/**
 * 系统级工具类
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-3-12
 */
public final class SystemHelper {
    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(SystemHelper.class);

    // 工具类不需要实例化
    private SystemHelper() {

    }

    /**
     * @param ver
     *            - java版本号如1.5,1.6,1.7
     * @return 如果当前的jvm版本号>=给定版本则返回true;否则false.
     */
    public static boolean isJavaVersionAtLeast(Number ver) {
        return SystemUtils.isJavaVersionAtLeast(ver.floatValue());
    }

    /**
     * 
     * @param serviceClass
     *            - SPI接口类
     * @param classLoader
     *            - 使用的类加载器,可为null(此时先采用Thread getContextClassLoader ,如还为空则采用serviceClass的ClassLoader)
     * @return 加载以指定的SPI实现
     */
    public static <T> T loadService(Class<T> serviceClass, ClassLoader classLoader) {
        Iterator<T> providers = loadServies(serviceClass, classLoader);
        if (!providers.hasNext()) {
            throw new IllegalAccessError("请确保类路径下存在指定文本文件且其只有此服务实现类文件全名一行! META-INF/services/"
                    + serviceClass.getName());
        }

        T serviceImpl = (T) providers.next();
        if (providers.hasNext()) {
            LOG.warn("存在多个服务只取第一个!以下服务实现不会采用:{}", Lists.newArrayList(providers));
        }
        LOG.info("serviceImpl:{}", serviceImpl);
        return serviceImpl;
    }

    public static <T> T loadServiceIf(Class<T> serviceClass, ClassLoader classLoader) {
        Iterator<T> providers = loadServies(serviceClass, classLoader);
        if (providers.hasNext()) {
            T serviceImpl = (T) providers.next();
            if (providers.hasNext()) {
                LOG.warn("存在多个服务只取第一个!以下服务实现不会采用:{}", Lists.newArrayList(providers));
            }
            return serviceImpl;
        } else {
            LOG.info("在请类路径META-INF/services/{}下指定服务实现类", serviceClass.getName());
        }
        return null;
    }

    private static <T> Iterator<T> loadServies(Class<T> serviceClass, ClassLoader classLoader) {
        if (null == classLoader) {
            classLoader = getClassLoader(serviceClass);
        }
        Iterator<T> providers;
        if (isJavaVersionAtLeast(1.6f)) {
            LOG.debug("using java.util.ServiceLoader for java > 1.5");
            providers = ServiceLoader.load(serviceClass, classLoader).iterator();
        } else {
            providers = Service.providers(serviceClass, classLoader);
        }
        return providers;
    }

    public static <T> ClassLoader getClassLoader(Class<T> serviceClass) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (null == classLoader && serviceClass != null) {
            classLoader = serviceClass.getClassLoader();
        }
        return classLoader;
    }

    public static URL getResource(String name, Class refClass) {
        final URL url = refClass != null ? refClass.getResource(name) : null;
        return url != null ? url : getClassLoader(refClass).getResource(name);
    }

    public static InputStream getResourceAsStream(String name, Class refClass) {
        final InputStream in = refClass != null ? refClass.getResourceAsStream(name) : null;
        return in != null ? in : getClassLoader(refClass).getResourceAsStream(name);
    }

    public static URL getResource(String resourceLocation) {
        final ClassLoader classLoader = getClassLoader(SystemHelper.class);
        return classLoader.getResource(resourceLocation);
    }

    public static URL getClassPathResource(String resourceLocation) {
        return getResource(resourceLocation);
    }

    public static String getResourceContent(String resourceLocation) {
        return getResourceContent(resourceLocation, SystemHelper.class);
    }

    /**
     * 从类路径中获资源文件的内容
     * 
     * @param resourceLocation
     * @return 从给定资源在类路径查找资源,如果找不到则返回空null
     */
    public static String getResourceContent(String resourceLocation, Class refClass) {
        final InputStream in = getResourceAsStream(resourceLocation, refClass);
        try {
            return in == null ? null : IOUtils.toString(in, "UTF-8");
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
