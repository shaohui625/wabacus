package com.wabacus.extra.expr;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.buttons.AbsButtonType;

/**
 * 
 * @version $Id$
 * @author qxo
 * @since 2013-9-3
 */
public final class ButtonPostPaserContext {

    private final  IComponentConfigBean ccbean;
    private final  AbsButtonType buttonObj;

    public IComponentConfigBean getCcbean() {
        return ccbean;
    }

    public AbsButtonType getButtonObj() {
        return buttonObj;
    }

    public ButtonPostPaserContext(IComponentConfigBean ccbean, AbsButtonType buttonObj) {
        super();
        this.ccbean = ccbean;
        this.buttonObj = buttonObj;
    }

    public String getPageId() {
        return ccbean.getPageBean().getId();
    }

    public String getCmpId() {
        return ccbean.getId();
    }
}
