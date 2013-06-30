package com.wabacus.extra.options;

import org.yaml.snakeyaml.Yaml;

import com.wabacus.extra.WabacusBeanFactory;

/**
 * 
 * <h3>YAML表示的options</h3>
 * 
 * <textarea cols="120" rows="8"> <resource key ="pOptions1" type="com.branchitech.wabacus.options.YamlTextOptionRes" > 优秀,良好,较好,很差 </resource> 
 * <resource key="pOptions2" type="com.branchitech.wabacus.options.YamlTextOptionRes" >[优秀,良好,较好,很差]</resource> 
 * <resource key ="pOptions3" type="com.branchitech.wabacus.options.YamlTextOptionRes" >"{'优秀':1,'良好':2,'较好':3,'很差':4}</resource> 
 * <resource key ="pOptions4" type="com.branchitech.wabacus.options.YamlTextOptionRes" >{优秀: 1,良好: 2, 较好 : 3,很差 : 4}</resource>
 * <resource key ="pOptions5" type="com.branchitech.wabacus.options.YamlTextOptionRes" >['优秀','良好','较好','很差']</resource>
 * </textarea>
 * 
 * @version $Id: YamlTextOptionRes.java 3374 2012-12-10 11:42:04Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-5
 */
public final class YamlTextOptionRes extends AbstractTextFormOptionRes {

	private static Yaml yaml;

	private Yaml getYaml() {
		if (null == yaml) {
			yaml = WabacusBeanFactory.getInstance().getBean("yaml");
			if (null == yaml) {
				yaml = new Yaml();
			}
		}
		return yaml;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.branchitech.wabacus.options.AbstractTextFormOptionRes#textToObject(java.lang.String)
	 */
	@Override
	protected Object textToObject(String text) {
		return getYaml().load(text);
	}
}
