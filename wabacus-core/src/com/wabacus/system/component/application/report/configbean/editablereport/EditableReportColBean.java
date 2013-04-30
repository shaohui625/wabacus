/* 
 * Copyright (C) 2010---2012 星星(wuweixing)<349446658@qq.com>
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
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public final class EditableReportColBean extends AbsExtendConfigBean implements IInputBoxOwnerBean
{
    private String updatecol;

    private ColBean updateCbean;
    
    private String updatedcol;//被哪个<col/>通过updatecol属性引用，这里存放相应<col/>的property。只有hidden=1或hidden=2才能被别的<col/>引用。
    
    private String defaultvalue;

    private String textalign;

    private AbsInputBox inputbox;

    private Boolean editableForInsert;
    
    private Boolean editableForUpdate;//修改时本列是否可编辑（即是否需要显示输入框）
    
    public EditableReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getUpdatecol()
    {
        return updatecol;
    }

    public void setUpdatecol(String updatecol)
    {
        if(updatecol.trim().equals(""))
        {
            this.updatecol=null;
        }else
        {
            if(!Tools.isDefineKey("@",updatecol))
            {
                throw new WabacusConfigLoadingException("加载报表"
                        +((ColBean)getOwner()).getReportBean().getPath()+"失败，column为"
                        +((ColBean)getOwner()).getColumn()
                        +"的<col/>的value属性配置不合法，必须配置为@{其它<col/>的property}格式");
            }
            this.updatecol=Tools.getRealKeyByDefine("@",updatecol);
        }
    }

    public ColBean getUpdateCbean()
    {
        return updateCbean;
    }

    public void setUpdateCbean(ColBean updateCbean)
    {
        this.updateCbean=updateCbean;
    }

    public String getUpdatedcol()
    {
        return updatedcol;
    }

    public void setUpdatedcol(String updatedcol)
    {
        this.updatedcol=updatedcol;
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

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        EditableReportColBean ercbeanNew=(EditableReportColBean)super.clone(owner);
        if(inputbox!=null)
        {
            ercbeanNew.setInputbox((AbsInputBox)inputbox.clone(ercbeanNew));
        }
        return ercbeanNew;
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

    public String getInputBoxValue(ReportRequest rrequest,int rowidx)
    {
        ReportBean rbean=getOwner().getReportBean();
        AbsReportType reportTypeObj=rrequest.getDisplayReportTypeObj(rbean.getId());
        if(reportTypeObj.getLstReportData()==null||reportTypeObj.getLstReportData().size()==0) return "";
        if(rowidx>=reportTypeObj.getLstReportData().size()) return "";
        if(rowidx<0) rowidx=0;//说明当前列不是在editablelist2/listform报表类型中，则取第一条记录的此列值
        Object dataObj=reportTypeObj.getLstReportData().get(rowidx);
        if(dataObj==null) return "";
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean==null)
        {
            throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是可编辑报表类型，不能显示输入框");
        }
        String accessmode=rrequest.getCurrentAccessMode(rbean.getId());
        if(accessmode.equals(Consts.ADD_MODE))
        {
            if(ersqlbean.getInsertbean()==null||!this.isEditableForInsert())
            {
                return "";
            }
        }else
        {
            if(ersqlbean.getUpdatebean()==null||!this.isEditableForUpdate())
            {
                return "";
            }
        }
        ColBean cbean=null;
        if(this.updateCbean==null)
        {
            cbean=(ColBean)this.getOwner();
        }else
        {
            cbean=this.updateCbean;
        }
        return ((IEditableReportType)reportTypeObj).getColOriginalValue(dataObj,cbean);
    }

    public String getLabel()
    {
        return ((ColBean)this.getOwner()).getLabel();
    }
    
    public boolean isEditableForInsert()
    {
        if(editableForInsert==null)
        {
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)this.getOwner().getReportBean().getSbean().getExtendConfigDataForReportType(
                    EditableReportSqlBean.class);
            if(ersqlbean==null||ersqlbean.getInsertbean()==null)
            {
                this.editableForInsert=false;
            }else if(ersqlbean.getInsertbean().containsParamBeanInUpdateClause((ColBean)this.getOwner()))
            {//出现在<insert/>的更新子句中
                this.editableForInsert=true;
            }else
            {
                if(this.inputbox!=null)
                {
                    String displayon=this.inputbox.getDisplayon();
                    if(displayon!=null&&displayon.trim().toLowerCase().indexOf("insert")>=0)
                    {//通过<inputbox/>的displayon配置了insert
                        this.editableForInsert=true;
                    }
                }
                if(this.editableForInsert==null) this.editableForInsert=false;
            }
        }
        return editableForInsert.booleanValue();
    }
    
    public boolean isEditableForUpdate()
    {
        if(editableForUpdate==null)
        {
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)this.getOwner().getReportBean().getSbean().getExtendConfigDataForReportType(
                    EditableReportSqlBean.class);
            if(ersqlbean==null||ersqlbean.getUpdatebean()==null)
            {
                this.editableForUpdate=false;
            }else if(ersqlbean.getUpdatebean().containsParamBeanInUpdateClause((ColBean)this.getOwner()))
            {//出现在<update/>的更新子句中
                this.editableForUpdate=true;
            }else
            {
                if(this.inputbox!=null)
                {
                    String displayon=this.inputbox.getDisplayon();
                    if(displayon!=null&&displayon.trim().toLowerCase().indexOf("update")>=0)
                    {//通过<inputbox/>的displayon配置了insert
                        this.editableForUpdate=true;
                    }
                }
                if(this.editableForUpdate==null) this.editableForUpdate=false;
            }
        }
        return editableForUpdate.booleanValue();
    }    
}
