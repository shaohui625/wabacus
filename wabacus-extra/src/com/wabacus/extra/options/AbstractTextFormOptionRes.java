package com.wabacus.extra.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import com.wabacus.config.resource.OptionRes;

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
 * @version $Id: AbstractTextFormOptionRes.java 3638 2013-05-12 15:10:22Z qxo $
 * @author qxo(qxodream@gmail.com)
 * @since 2012-12-5
 */
public abstract class AbstractTextFormOptionRes extends OptionRes {

    /**
	 * 
	 */
    public final Object getValue(Element itemElement) {
        final String text = itemElement.getText();
        if (StringUtils.isNotBlank(text)) {
            final Object ret = textToObject(text.trim());
            return toList(ret);
        }
        return ListUtils.EMPTY_LIST;
    }

    protected abstract Object textToObject(final String text);

    protected List<OptionBean> toList(Object ret) {
        if (null == ret) {
            return ListUtils.EMPTY_LIST;
        }
        if (ret instanceof String) {
            ret = ((String) ret).split(StringSplitOptionRes.DEFAULT_SEPARATOR);
        }
        final List<OptionBean> lstOptions = new ArrayList<OptionBean>();

        if (ret instanceof Map) {
            final Set<Map.Entry> entrySet = ((Map) ret).entrySet();
            for (Map.Entry entry : entrySet) {
                final OptionBean oBean = new OptionBean(toStr(entry.getKey()));
                // oBean.setLabel(toStr(entry.getKey()));
                oBean.setValue(toStr(entry.getValue()));
                lstOptions.add(oBean);
            }
        } else if (ret instanceof Collection) {
            for (Object entry : (Collection) ret) {
                final String str = toStr(entry);
                final OptionBean oBean = new OptionBean(str);
                // oBean.setLabel(str);
                oBean.setValue(str);
                lstOptions.add(oBean);
            }
        } else if (null != ret && ret.getClass().isArray()) {
            for (Object entry : (Object[]) ret) {
                final String str = toStr(entry);
                final OptionBean oBean = new OptionBean(str);
                // oBean.setLabel(str);
                oBean.setValue(str);
                lstOptions.add(oBean);
            }
        } else {
            throw new NotImplementedException("ret:" + ret + " 此类型对象尚未支持！");
        }
        return lstOptions;
    }

    protected static String toStr(Object val) {
        return val == null ? null : val.toString();
    }
}
