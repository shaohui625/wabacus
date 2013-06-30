package com.wabacus.extra.options;

import org.mvel2.MVEL;

/**
 * 
 * <h3>MVEL表达工生成options</h3>
 * 
 * <textarea cols="120" rows="8"> <resource key ="pOptions2"
 * type="com.branchitech.wabacus.options.MvelTextOptionRes" > ['优秀':1,'良好':2,'较好':3,'很差':4] </resource>
 * <resource key ="pOptions3" type="com.branchitech.wabacus.options.MvelTextOptionRes" >
 * {'优秀','良好','较好','很差'} </resource> <resource key ="pOptions4"
 * type="com.branchitech.wabacus.options.MvelTextOptionRes" > '优秀,良好,较好,很差'.split(',') </resource>
 * </textarea>
 * 
 * @version $Id: MvelTextOptionRes.java 3638 2013-05-12 15:10:22Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-5
 */
public final class MvelTextOptionRes extends AbstractTextFormOptionRes {

	protected Object textToObject(final String text) {
		final Object ret = MVEL.eval(text);
		return ret;
	}
}
