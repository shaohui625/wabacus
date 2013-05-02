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

import java.util.List;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SubmitFunctionParamBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;

public final class EditableReportSqlBean extends AbsExtendConfigBean
{
    private EditableReportUpdateDataBean updatebean;

    private EditableReportUpdateDataBean insertbean;

    private EditableReportUpdateDataBean deletebean;

    private String transactionLever;
    
    private String beforeSaveAction;
    
    private String[] afterSaveAction;

    private List<String> lstSaveBindingReportIds;
    
    private List<ReportBean> lstSaveBindingReportBeans;
    
    private List<String> lstDeleteBindingReportIds;
    
    private List<ReportBean> lstDeleteBindingReportBeans;//绑定保存的报表配置对象，在doPostLoad()方法中由lstDeleteBindingReportIds生成，生成完后会清空lstDeleteBindingReportIds
    
    private String validateSaveAddingMethod;//保存添加记录时的JS校验方法名，只对editabledetail/form两种报表类型有效
    
    private String validateSaveUpdateMethod;//保存修改记录时的JS校验方法名，对所有可编辑报表类型都有效。对于editablelist2/listform的添加和修改保存都有效，因为它们合在一起保存
    
    private List<SubmitFunctionParamBean> lstValidateSavingAddDynParams;//校验添加记录时所需的动态参数,只对editabledetail/form两种报表类型有效
    
    private List<SubmitFunctionParamBean> lstValidateSavingUpdateDynParams;//校验修改记录时所需的动态参数。对于editablelist2/listform的添加和修改保存都有效，因为它们合在一起保存
    
    public EditableReportSqlBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public EditableReportUpdateDataBean getUpdatebean()
    {
        return updatebean;
    }

    public void setUpdatebean(EditableReportUpdateDataBean updatebean)
    {
        this.updatebean=updatebean;
    }

    public EditableReportUpdateDataBean getInsertbean()
    {
        return insertbean;
    }

    public void setInsertbean(EditableReportUpdateDataBean insertbean)
    {
        this.insertbean=insertbean;
    }

    public EditableReportUpdateDataBean getDeletebean()
    {
        return deletebean;
    }

    public void setDeletebean(EditableReportUpdateDataBean deletebean)
    {
        this.deletebean=deletebean;
    }

    public String getTransactionLever()
    {
        return transactionLever;
    }

    public void setTransactionLever(String transactionLever)
    {
        this.transactionLever=transactionLever;
    }
    
    public List<String> getLstSaveBindingReportIds()
    {
        return lstSaveBindingReportIds;
    }

    public void setLstSaveBindingReportIds(List<String> lstSaveBindingReportIds)
    {
        this.lstSaveBindingReportIds=lstSaveBindingReportIds;
    }

    public List<ReportBean> getLstSaveBindingReportBeans()
    {
        return lstSaveBindingReportBeans;
    }

    public void setLstSaveBindingReportBeans(List<ReportBean> lstSaveBindingReportBeans)
    {
        if(lstSaveBindingReportBeans!=null&&lstSaveBindingReportBeans.size()==0) lstSaveBindingReportBeans=null;
        this.lstSaveBindingReportBeans=lstSaveBindingReportBeans;
    }

    public List<String> getLstDeleteBindingReportIds()
    {
        return lstDeleteBindingReportIds;
    }

    public void setLstDeleteBindingReportIds(List<String> lstDeleteBindingReportIds)
    {
        this.lstDeleteBindingReportIds=lstDeleteBindingReportIds;
    }

    public List<ReportBean> getLstDeleteBindingReportBeans()
    {
        return lstDeleteBindingReportBeans;
    }

    public void setLstDeleteBindingReportBeans(List<ReportBean> lstDeleteBindingReportBeans)
    {
        if(lstDeleteBindingReportBeans!=null&&lstDeleteBindingReportBeans.size()==0) lstDeleteBindingReportBeans=null;
        this.lstDeleteBindingReportBeans=lstDeleteBindingReportBeans;
    }

    public String getBeforeSaveAction()
    {
        return beforeSaveAction;
    }

    public void setBeforeSaveAction(String beforeSaveAction)
    {
        this.beforeSaveAction=beforeSaveAction;
    }

    public String[] getAfterSaveAction()
    {
        return afterSaveAction;
    }
    
    public String getAfterSaveActionMethod()
    {
        if(afterSaveAction==null||afterSaveAction.length==0) return "";
        return afterSaveAction[0];
    }
    
    public void setAfterSaveAction(String[] afterSaveAction)
    {
        this.afterSaveAction=afterSaveAction;
    }

    public String getValidateSaveAddingMethod()
    {
        return validateSaveAddingMethod;
    }

    public void setValidateSaveAddingMethod(String validateSaveAddingMethod)
    {
        this.validateSaveAddingMethod=validateSaveAddingMethod;
    }

    public String getValidateSaveUpdateMethod()
    {
        return validateSaveUpdateMethod;
    }

    public void setValidateSaveUpdateMethod(String validateSaveUpdateMethod)
    {
        this.validateSaveUpdateMethod=validateSaveUpdateMethod;
    }

    public List<SubmitFunctionParamBean> getLstValidateSavingAddDynParams()
    {
        return lstValidateSavingAddDynParams;
    }

    public void setLstValidateSavingAddDynParams(List<SubmitFunctionParamBean> lstValidateSavingAddDynParams)
    {
        this.lstValidateSavingAddDynParams=lstValidateSavingAddDynParams;
    }

    public List<SubmitFunctionParamBean> getLstValidateSavingUpdateDynParams()
    {
        return lstValidateSavingUpdateDynParams;
    }

    public void setLstValidateSavingUpdateDynParams(List<SubmitFunctionParamBean> lstValidateSavingUpdateDynParams)
    {
        this.lstValidateSavingUpdateDynParams=lstValidateSavingUpdateDynParams;
    }

    public String getValidateSaveMethodAndParams(ReportRequest rrequest,boolean isSaveAdd)
    {
        String validateSaveMethod=null;
        List<SubmitFunctionParamBean> lstValidateSavingDynParams=null;
        if(isSaveAdd)
        {
            validateSaveMethod=validateSaveAddingMethod;
            lstValidateSavingDynParams=lstValidateSavingAddDynParams;
        }else
        {
            validateSaveMethod=validateSaveUpdateMethod;
            lstValidateSavingDynParams=lstValidateSavingUpdateDynParams;
        }
        if(validateSaveMethod==null||validateSaveMethod.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(" validateSaveMethod=\"{method:").append(validateSaveMethod.trim()).append("}\"");
        if(lstValidateSavingDynParams!=null&&lstValidateSavingDynParams.size()>0)
        {
            resultBuf.append(" validateSaveMethodDynParams=\"").append(
                    JavaScriptAssistant.getInstance().getRuntimeParamsValueJsonString(rrequest,lstValidateSavingDynParams)).append("\"");
        }
        return resultBuf.toString();
    }
    
    public String getBeforeSaveActionString(String actiontype)
    {
        if(this.beforeSaveAction==null||this.beforeSaveAction.trim().equals("")) return "";
        if(actiontype==null) actiontype="";
        StringBuffer buf=new StringBuffer();
        buf.append("if(!").append(this.beforeSaveAction).append("('").append(
                this.getOwner().getReportBean().getPageBean().getId()).append("','")
                .append(this.getOwner().getReportBean().getId()).append("','").append(actiontype)
                .append("'))");
        buf.append("return;");
        return buf.toString();
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        EditableReportSqlBean newsqlbean=(EditableReportSqlBean)super.clone(owner);
        if(this.insertbean!=null)
        {
            newsqlbean.setInsertbean((EditableReportUpdateDataBean)insertbean.clone(newsqlbean));
        }
        if(this.updatebean!=null)
        {
            newsqlbean.setUpdatebean((EditableReportUpdateDataBean)updatebean.clone(newsqlbean));
        }
        if(this.deletebean!=null)
        {
            newsqlbean.setDeletebean((EditableReportUpdateDataBean)deletebean.clone(newsqlbean));
        }
        return newsqlbean;
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((this.getOwner()==null)?0:this.getOwner().getReportBean().hashCode());
        result=prime*result+((transactionLever==null)?0:transactionLever.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final EditableReportSqlBean other=(EditableReportSqlBean)obj;
        if(this.getOwner()==null)
        {
            if(other.getOwner()!=null) return false;
        }else if(!this.getOwner().getReportBean().equals(other.getOwner().getReportBean()))
        {
            return false;
        }
        if(transactionLever==null)
        {
            if(other.transactionLever!=null) return false;
        }else if(!transactionLever.equals(other.transactionLever)) return false;
        return true;
    }
}