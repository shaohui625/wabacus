/* 
 * Copyright (C) 2010---2013 星星(wuweixing)<349446658@qq.com>
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
package com.wabacus.system.component.application.report.configbean.editablereport;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.util.Tools;

public final class EditableReportColBean extends AbsExtendConfigBean implements IInputBoxOwnerBean
{
    private String updatecolDest;

    private String updatecolSrc;//被哪个<col/>通过updatecol属性引用，这里存放相应<col/>的property。只有hidden=1或hidden=2才能被别的<col/>引用。
    
    private String defaultvalue;

    private String textalign;

    private AbsInputBox inputbox;

    private int editableWhenInsert;//本列添加时是否可编辑，如果为0，不可编辑；1：因为显示辅助输入框可编辑；2：因为在<insert/>指定了它可编辑；
    
    private int editableWhenUpdate;//本列修改时是否可编辑如果为0，不可编辑；1：因为显示辅助输入框可编辑；2：因为在<update/>指定了它可编辑；
    
    public EditableReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public void setUpdatecolDest(String updatecol)
    {
        if(updatecol.trim().equals(""))
        {
            this.updatecolDest=null;
        }else
        {
            if(!Tools.isDefineKey("@",updatecol))
            {
                throw new WabacusConfigLoadingException("加载报表"+((ColBean)getOwner()).getReportBean().getPath()+"失败，column为"
                        +((ColBean)getOwner()).getColumn()+"的<col/>的value属性配置不合法，必须配置为@{其它<col/>的property}格式");
            }
            this.updatecolDest=Tools.getRealKeyByDefine("@",updatecol);
        }
    }

    public String getUpdatecolSrc()
    {
        return updatecolSrc;
    }

    public void setUpdatecolSrc(String updatecolSrc)
    {
        this.updatecolSrc=updatecolSrc;
    }

    public String getUpdatecolDest()
    {
        return updatecolDest;
    }

    public String getDefaultvalue()
    {
        return defaultvalue;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public AbsInputBox getInputbox()
    {
        if(this.inputbox==null)
        {
            this.inputbox=Config.getInstance().getInputBoxTypeByName(null);
            this.inputbox=(AbsInputBox)this.inputbox.clone(this);
            this.inputbox.setDefaultFillmode(Config.getInstance().getReportType(this.getOwner().getReportBean().getType()));
        }
        return inputbox;
    }

    public void setInputbox(AbsInputBox inputbox)
    {
        this.inputbox=inputbox;
    }

    public String getTextalign()
    {
        return textalign;
    }

    public void setTextalign(String textalign)
    {
        this.textalign=textalign;
    }

    public int getEditableWhenInsert()
    {
        return editableWhenInsert;
    }

    public void setEditableWhenInsert(int editableWhenInsert)
    {
        if(editableWhenInsert>0)
        {
            ColBean cbean=(ColBean)this.getOwner();
            AbsListReportColBean lcolbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportColBean.class);
            if(lcolbean!=null&&lcolbean.isRowgroup())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"上的列"+cbean.getColumn()
                        +"失败，此列为分组列，不能将其配置为可编辑，可以用editablelist报表类型编辑此列");
            }
        }
        this.editableWhenInsert=editableWhenInsert;
    }

    public int getEditableWhenUpdate()
    {
        return editableWhenUpdate;
    }

    public void setEditableWhenUpdate(int editableWhenUpdate)
    {
        if(editableWhenUpdate>0)
        {
            ColBean cbean=(ColBean)this.getOwner();
            AbsListReportColBean lcolbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportColBean.class);
            if(lcolbean!=null&&lcolbean.isRowgroup())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"上的列"+cbean.getColumn()
                        +"失败，此列为分组列，不能将其配置为可编辑，可以用editablelist报表类型编辑此列");
            }
        }
        this.editableWhenUpdate=editableWhenUpdate;
    }

    public String getInputBoxId()
    {
        return EditableReportAssistant.getInstance().getInputBoxId((ColBean)this.getOwner());
    }

    public ReportBean getReportBean()
    {
        return this.getOwner().getReportBean();
    }

    public AbsInputBox getSiblingInputBoxByOwnerId(String ownerid)
    {
        DisplayBean dbean=(DisplayBean)getOwner().getParent();
        if((ownerid==null)||ownerid.equals(((ColBean)getOwner()).getProperty()))
        {
            return null;
        }
        ColBean cbeanTmp;
        for(int i=0;i<dbean.getLstCols().size();i++)
        {
            cbeanTmp=dbean.getLstCols().get(i);
            if(ownerid.equals(cbeanTmp.getProperty()))
            {
                EditableReportColBean ercbean=(EditableReportColBean)cbeanTmp
                        .getExtendConfigDataForReportType(EditableReportColBean.class);
                if(ercbean==null) return null;
                return ercbean.getInputbox();
            }
        }
        return null;
    }

    public String getLabel()
    {
        return ((ColBean)this.getOwner()).getLabel();
    }
    
    public boolean isEditableForInsert()
    {
        return this.editableWhenInsert>0;
    }
    
    public boolean isEditableForUpdate()
    {
        return this.editableWhenUpdate>0;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        EditableReportColBean ercbeanNew=(EditableReportColBean)super.clone(owner);
        if(inputbox!=null)
        {
            ercbeanNew.setInputbox((AbsInputBox)inputbox.clone(ercbeanNew));
        }
        return ercbeanNew;
    }
}
