package com.wabacus.extra;

/**
 * <H3>BeanFactory用于处理实例配置可覆盖机制</H3>
 * <UL>
 * <LI>目前主要解决wabacus和spring集成时部分实例初始在spring中，然后可通过此factory来静态获取</LI>
 * <LI>
 * </UL>
 * 
 * @version $Id: WabacusBeanFactory.java 3390 2012-12-18 13:31:09Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-7
 */
public abstract class WabacusBeanFactory {

	public abstract <T> T getBean(String beanId);

	private static WabacusBeanFactory instance;

	private static final WabacusBeanFactory NULL = new WabacusBeanFactory() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.branchitech.wabacus.WabacusBeanFactory#getBean(java.lang.String)
		 */
		@Override
		public <T> T getBean(String beanId) {
			return null;
		}

	};

	/**
	 * 注意方式仅用于静态初始化时调用，暂调用的时机要在wabacus初始化开始之前
	 */
	public static WabacusBeanFactory  setWabacusBeanFactory(WabacusBeanFactory inst) {
		instance = inst;
		return instance;
	}

	public static WabacusBeanFactory getInstance() {
		return instance == null ? NULL : instance;
	}
	
	
	public <T> T getBean(String beanId, T defaultObj){
		final T bean = this.getBean(beanId);
		return bean == null ? defaultObj : bean;
	}

}
