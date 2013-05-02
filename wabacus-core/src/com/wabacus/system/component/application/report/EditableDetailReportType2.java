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
package com.wabacus.system.component.application.report;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.SubmitFunctionParamBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditActionGroupBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableDetailReportType2 extends DetailReportType implements IEditableReportType
{
    protected EditableReportSqlBean ersqlbean=null;
    
    protected String realAccessMode;
    
    public EditableDetailReportType2(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            this.ersqlbean=(EditableReportSqlBean)((ReportBean)comCfgBean).getSbean().getExtendConfigDataForReportType(KEY);
        }
    }

    public final static String KEY=EditableDetailReportType2.class.getName();

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        super.initUrl(applicationConfigBean,rrequest);
        String accessmode=rrequest.getStringAttribute(applicationConfigBean.getId()+"_ACCESSMODE",getDefaultAccessMode()).toLowerCase();
        if(accessmode.equals(Consts.READONLY_MODE))
        {
            rrequest.addParamToUrl(applicationConfigBean.getId()+"_ACCESSMODE",Consts.READONLY_MODE,true);
        }
    }
    
    public void init()
    {
        super.init();
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            this.realAccessMode=Consts.READONLY_MODE;
        }else
        {
            this.realAccessMode="";
        }
    }

    public void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId()+"_ACCESSMODE","").toLowerCase();
            if(accessmode.equals(Consts.READONLY_MODE)) setNewAccessMode(Consts.READONLY_MODE);
        }else
        {
            EditableReportAssistant.getInstance().doAllReportsSaveAction(rrequest);
        }
    }

    public void collectEditActionGroupBeans(List<EditActionGroupBean> lstAllEditActionGroupBeans)
    {
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null||(!sidbean.hasSavingData()&&!sidbean.hasDeleteData())) return;
        if(sidbean.hasSavingData())
        {
            lstAllEditActionGroupBeans.addAll(ersqlbean.getUpdatebean().getLstEditActionGroupBeans());
        }else if(sidbean.hasDeleteData())
        {
            lstAllEditActionGroupBeans.addAll(ersqlbean.getDeletebean().getLstEditActionGroupBeans());
        }
    }
    
    public int[] doSaveAction() throws SQLException
    {
        int[] result=new int[] { IInterceptor.WX_RETURNVAL_SKIP, 0 };
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null) return result;
        AbsEditableReportEditDataBean editbean;
        if(sidbean.hasSavingData())
        {
            editbean=ersqlbean.getUpdatebean();
        }else if(sidbean.hasDeleteData())
        {
            editbean=ersqlbean.getDeletebean();
        }else
        {
            return result;
        }
        if(rbean.getInterceptor()!=null)
        {
            result[0]=rbean.getInterceptor().doSave(rrequest,rbean,editbean);
        }else
        {
            result[0]=EditableReportAssistant.getInstance().doSaveReport(rbean,rrequest,editbean);
        }
        if(result[0]==IInterceptor.WX_RETURNVAL_TERMINATE||result[0]==IInterceptor.WX_RETURNVAL_SKIP) return result;
        if(sidbean.hasSavingData())
        {
            result[1]=IEditableReportType.IS_UPDATE_DATA;
        }else
        {
            result[1]=IEditableReportType.IS_DELETE_DATA;
        }
        result[0]=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,sidbean.hasDeleteData()?"delete":"update",result[0]);
        return result;
    }

    public String getDefaultAccessMode()
    {
        return "";
    }

    public String getRealAccessMode()
    {
        return this.realAccessMode;
    }
    
    public void setNewAccessMode(String newaccessmode)
    {
        rrequest.setAttribute(rbean.getId()+"_ACCESSMODE",newaccessmode);
        rrequest.setAttribute(rbean.getId(),"CURRENT_ACCESSMODE",newaccessmode);
        rrequest.addParamToUrl(rbean.getId()+"_ACCESSMODE",newaccessmode,true);
        if(Consts.READONLY_MODE.equals(newaccessmode))
        {
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode","true");
        }else
        {
            rrequest.getAttributes().remove(rbean.getId()+"_isReadonlyAccessmode");
        }
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        if(!Consts.READONLY_MODE.equals(this.realAccessMode))
        {
            resultBuf.append(ersqlbean.getValidateSaveMethodAndParams(rrequest,false));
        }
        return resultBuf.toString();
    }

    protected String showMetaDataContentDisplayString()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataContentDisplayString());
        resultBuf.append(initInputBox());
        return resultBuf.toString();
    }
    
    private String initInputBox()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(ersqlbean.getUpdatebean()==null||Consts.READONLY_MODE.equals(this.realAccessMode)) return "";
        StringBuffer resultBuf=new StringBuffer();
        EditableReportColBean ercbeanTmp=null;
        for(ColBean cbean:rbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0) continue;
            if(cbean.isNonValueCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercbeanTmp==null||!ercbeanTmp.isEditableForUpdate()) continue;
            
            resultBuf.append(ercbeanTmp.getInputbox().initDisplay(rrequest));
        }
        return resultBuf.toString();
    }
    
    protected String getDataTdClassName()
    {
        return "cls-data-td-editdetail";
    }
    
    protected Object initDisplayCol(ColBean cbean,Object dataObj)
    {
        if(cbean.isNonValueCol()) return null;

//        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype()))






//            {//没有通过updatecol属性引用别的列进行更新



//                col_editvalue=getColOriginalValue(dataObj,ercbean.getUpdateCbean());


        String col_editvalue=getColOriginalValue(dataObj,cbean);
        return EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,ersqlbean.getUpdatebean(),cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String showHiddenCol(ColBean cbean,Object colDataObj)
    {
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.showHiddenCol(cbean,colDataObj);
        return "<td style=\"display:none\" "+getTdPropsForColNameAndValue(cbean,(EditableReportColDataBean)colDataObj)+"></td>";//不能用<td/>，而必须是<td></td>格式，否则在IE上取子节点会有问题
    }
    
    protected String getColValueTdPropertiesAndContent(ColBean cbean,Object dataObj,Object colDataObj,StringBuffer tdPropsBuf)
    {
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        tdPropsBuf.append(" ").append(getTdPropsForColNameAndValue(cbean,ercdatabean));
        if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {
            tdPropsBuf.append(" style=\"display:none;\"");
            return "";
        }
        String col_displayvalue=getColDisplayValue(ercdatabean,cbean,tdPropsBuf,dataObj);
        ColDataByInterceptor coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,0,col_displayvalue);
        if(coldataByInterceptor!=null)
        {
            if(coldataByInterceptor.isReadonly())
            {
                tdPropsBuf.delete(0,tdPropsBuf.length());
                col_displayvalue=cbean.getDisplayValue(dataObj,rrequest);
            }else if(coldataByInterceptor.getDynvalue()!=null)
            {
                col_displayvalue=coldataByInterceptor.getDynvalue();
            }
        }
        if(col_displayvalue==null||col_displayvalue.trim().equals("null")||col_displayvalue.trim().equals("")) col_displayvalue="&nbsp;";
        tdPropsBuf.append(getDetailTdValuestyleproperty(cbean,coldataByInterceptor));
        return col_displayvalue;
    }
    
    private String getTdPropsForColNameAndValue(ColBean cbean,EditableReportColDataBean ercdatabean)
    {
        if(cbean.isNonValueCol()) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td\" ");//有输入框的为此<td/>设置一id，以便客户端校验时用上
        resultBuf.append(" oldvalue=\""+Tools.htmlEncode(ercdatabean.getOldvalue())+"\" ");
        resultBuf.append(" oldvalue_name=\""+ercdatabean.getValuename());
        if(!ercdatabean.isEditable())
        {
            resultBuf.append("\"");
        }else
        {
            resultBuf.append("__old\" value_name=\""+ercdatabean.getValuename()+"\" value=\"");
            resultBuf.append(Tools.htmlEncode(ercdatabean.getValue())).append("\" ");
        }
        if(cbean.getUpdateColBeanSrc(false)!=null)
        {
            resultBuf.append(" updatecolSrc=\"").append(cbean.getUpdateColBeanSrc(false).getProperty()).append("\"");
        }else if(cbean.getUpdateColBeanDest(false)!=null)
        {
            resultBuf.append(" updatecolDest=\"").append(cbean.getUpdateColBeanDest(false).getProperty()).append("\"");
        }
        return resultBuf.toString();
    }

    public String getColOriginalValue(Object object,ColBean cbean)
    {
        String colproperty=cbean.getProperty();
        if(!cbean.isNonFromDbCol()) colproperty=colproperty+"_old";
        String oldvalue=ReportAssistant.getInstance().getPropertyValueAsString(object,colproperty,cbean.getDatatypeObj());
        if(oldvalue==null||oldvalue.equals("null"))
        {
            oldvalue="";
        }
        return oldvalue;
    }

    private String getColDisplayValue(EditableReportColDataBean ercdatabean,ColBean cbean,StringBuffer tdPropsBuf,Object object)
    {
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        String col_displayvalue;
        if(!ercdatabean.isEditable()||this.realAccessMode.equals(Consts.READONLY_MODE)) return cbean.getDisplayValue(object,rrequest);
        boolean isReadonlyPermission=cbean.checkReadonlyPermission(rrequest);
        if(ercbean.getInputbox().getFillmode()==2)
        {
            //这种显示方式不考虑前台编码显示的情况（目前主要是密码框会在前台编码显示，而它是显示时就直接填充的输入框）
            if(ercdatabean.isNeedDefaultValue())
            {
                col_displayvalue=ercdatabean.getDefaultvalue();
            }else
            {
                col_displayvalue=cbean.getDisplayValue(object,rrequest);
                if(col_displayvalue==null||col_displayvalue.equals("null")) col_displayvalue="";
            }
            if(!isReadonlyPermission)
            {
                String beforedesc=ercbean.getInputbox().getBeforedescription(rrequest);
                String afterdesc=ercbean.getInputbox().getAfterdescription(rrequest);
                if(beforedesc!=null&&!beforedesc.trim().equals("")||afterdesc!=null&&!afterdesc.trim().equals(""))
                {
                    String preContent="", postContent="";
                    if(beforedesc!=null&&!beforedesc.trim().equals("")) preContent=beforedesc;
                    if(afterdesc!=null&&!afterdesc.trim().equals("")) postContent=afterdesc;
                    col_displayvalue=preContent+"<span tagtype='COL_CONTENT_WRAP'>"+col_displayvalue+"</span>"+postContent;

                }
                tdPropsBuf.append(" onclick=\"try{fillInputBoxOnClick(event,'"+rbean.getPageBean().getId()+"','");
                tdPropsBuf.append(rbean.getGuid()).append("','"+this.getReportFamily()+"','");
                tdPropsBuf.append(ercbean.getInputbox().getTypename()).append("','").append(ercbean.getInputBoxId()).append(
                        "');}catch(e){logErrorsAsJsFileLoad(e);}\"");
                tdPropsBuf.append(" attachinfo=\""+ercbean.getInputbox().attachInfoForDisplay()+"\" ");
            }
        }else
        {
            col_displayvalue=ercbean.getInputbox().getDisplayStringValue(
                    rrequest,ercdatabean.getValue(),
                    "style=\"text-align:"+ercbean.getTextalign()+";\" onblur=\"try{fillInputBoxValueToParentTd(this,'"
                            +ercbean.getInputbox().getTypename()+"','"+rbean.getGuid()+"','"+this.getReportFamily()
                            +"',1);}catch(e){logErrorsAsJsFileLoad(e);}\"",isReadonlyPermission);
        }
        return col_displayvalue;
    }

    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(Consts.READONLY_MODE.equals(this.realAccessMode)) return false;
        if(buttonType instanceof SaveButton&&ersqlbean.getUpdatebean()!=null) return true;
        if(buttonType instanceof DeleteButton&&ersqlbean.getDeletebean()!=null&&this.lstReportData!=null&&this.lstReportData.size()>0) return true;
        return false;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        ComponentConfigLoadManager.loadEditableColConfig(colbean,lstEleColBeans.get(0),KEY);
        return 1;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        super.afterSqlLoading(sqlbean,lstEleSqlBeans);
        ComponentConfigLoadManager.loadEditableSqlConfig(sqlbean,lstEleSqlBeans,KEY);
        if(sqlbean!=null)
        {
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
            if(ersqlbean!=null)
            {
                if(ersqlbean.getInsertbean()!=null)
                {
                    throw new WabacusConfigLoadingException("不能在editabledetail2类型报表中配置添加功能，如果需要添加功能，可以在editabledetail中配置");
                }
            }
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        ComponentConfigLoadManager.doEditableReportTypePostLoad(reportbean,KEY);
        SqlBean sqlbean=reportbean.getSbean();
        if(sqlbean==null) return 1;
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null) return 1;
        processEditableButtons(reportbean,ersqlbean);

        EditableReportColBean ercolbeanTmp;
        StringBuffer validateScriptBuffer=new StringBuffer();
        List<SubmitFunctionParamBean> lstValidateParams=new ArrayList<SubmitFunctionParamBean>();
        for(ColBean cbean:reportbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            ercolbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            String align=Tools.getPropertyValueByName("align",cbean.getValuestyleproperty(),true);
            if(align==null||align.trim().equals("")) align="left";
            ercolbeanTmp.setTextalign(align);
            if(ercolbeanTmp.isEditableForUpdate())
            {
                JavaScriptAssistant.getInstance().writeEditableReportColValidateJs(ercolbeanTmp,validateScriptBuffer,lstValidateParams);
            }
        }

        if(validateScriptBuffer.length()>0)
        {
            writeValidateJsToFile(reportbean,validateScriptBuffer.toString());
            ersqlbean.setValidateSaveUpdateMethod(reportbean.getGuid()+"_validateSave");
            ersqlbean.setLstValidateSavingUpdateDynParams(lstValidateParams);
        }
        return 1;
    }

    private void processEditableButtons(ReportBean reportbean,EditableReportSqlBean ersqlbean)
    {
        if(ersqlbean.getDeletebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
        if(ersqlbean.getUpdatebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,SaveButton.class,Consts.SAVE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(SaveButton.class);
        }
    }

    private void writeValidateJsToFile(ReportBean reportbean,String script)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+reportbean.getGuid()+"_validateSave(metadataObj){");
        resultBuf.append("  if(WX_UPDATE_ALLDATA==null){ return true;}");
        resultBuf.append(   "var updatedataForSaving=WX_UPDATE_ALLDATA['"+reportbean.getGuid()+"'];");
        resultBuf.append("  if(updatedataForSaving==null){return true}");
        resultBuf.append("  var tableObj=document.getElementById('"+reportbean.getGuid()+"_data');if(tableObj==null){return true;}");
        resultBuf.append("  var tdChilds=tableObj.getElementsByTagName('TD');if(tdChilds==null||tdChilds.length==0){return true;}");
        resultBuf.append("  var paramsObj=getObjectByJsonString(metadataObj.metaDataSpanObj.getAttribute('validateSaveMethodDynParams'));");
        resultBuf.append("  var value_name;var boxValue;var boxObj;");
        resultBuf.append("  for(var j=0;j<tdChilds.length;j=j+1){");
        resultBuf.append("      if(tdChilds[j]==null) continue;");
        resultBuf.append("      boxObj=tdChilds[j];");//在这种报表类型中，传入客户端校验函数的输入框对象不是真正的输入框对象，而是其所在的<td/>对象
        resultBuf.append("      value_name=boxObj.getAttribute('value_name');if(value_name==null||value_name=='') continue;");
        resultBuf.append("      var updateDestTdObj=getUpdateColDestObj(boxObj,metadataObj.reportguid,'"+this.getReportFamily()+"',boxObj);");
        resultBuf.append("      boxValue=getColConditionValueByParentElementObj(updateDestTdObj);if(boxValue==null) boxValue='';");
        //        resultBuf.append("var idxTemp=id.lastIndexOf('__td');if(idxTemp>0) id=id.substring(0,idxTemp);");
        resultBuf.append(script);
        resultBuf.append(" }  return true;} ");
        JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(reportbean.getPageBean(),resultBuf.toString());
    }
    
    public int doPostLoadFinally(ReportBean reportbean)
    {
        ComponentConfigLoadManager.doEditableReportTypePostLoadFinally(reportbean,KEY);
        return super.doPostLoadFinally(reportbean);
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_EDITABLEDETAIL2;
    }
}
