package com.wabacus.extra.options;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import com.wabacus.config.resource.OptionRes;

/**
 * 
 * 字符串自动分隔的options
 * 
 * <textarea cols="120" rows="8"> <resource key ="pOptions1"
 * type="com.branchitech.wabacus.options.StringSplitOptionRes" separator="[,;  　]"> 优秀,良好,较好,很差 </resource>
 * 
 * </textarea>
 * 
 * @version $Id: StringSplitOptionRes.java 3638 2013-05-12 15:10:22Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-5
 */
public final class StringSplitOptionRes extends OptionRes {

	/**
	 * 默认的字符串分隔符
	 */
	public static final String DEFAULT_SEPARATOR = "[,;  　]";

	public Object getValue(Element itemElement) {
		final List<OptionBean> lstOptions = new ArrayList<OptionBean>();
		final String text = itemElement.getText();
		final String sp = itemElement.attributeValue("separator", DEFAULT_SEPARATOR);
		if (null != text) {
			final String[] arr = text.trim().split(sp);
			for (String name : arr) {
				final OptionBean oBean = new OptionBean(name);
				//oBean.setLabel(name);
				oBean.setValue(name);
				lstOptions.add(oBean);
			}
		}
		return lstOptions;
	}
}
