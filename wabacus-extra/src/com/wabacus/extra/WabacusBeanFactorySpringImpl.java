package com.wabacus.extra;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 
 * 此WabacusBeanFactory实现用于从spring配置中获取实例
 * 
 * @version $Id: WabacusBeanFactorySpringImpl.java 3430 2013-01-25 02:20:29Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-7
 */
public final class WabacusBeanFactorySpringImpl extends WabacusBeanFactory implements ApplicationContextAware,
		DisposableBean {

	@Override
	public <T> T getBean(String beanId) {
		if (null == applicationContext) {
			throw new IllegalArgumentException("初始化不正确：此类要在spring配置中始化");
		}
		return applicationContext.containsBean(beanId) ? (T) applicationContext.getBean(beanId) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		applicationContext = null;
		WabacusBeanFactory.setWabacusBeanFactory(null);
	}

	private ApplicationContext applicationContext;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context
	 * .ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
