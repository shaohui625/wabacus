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
package com.wabacus.system.component.application.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.SubmitFunctionParamBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.IConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.CancelButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.ResetButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.buttons.UpdateButton;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.format.IFormat;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableDetailReportType extends DetailReportType implements IEditableReportType
{
    protected EditableReportSqlBean ersqlbean=null;
    
    protected String realAccessMode;
    
    public final static String KEY=EditableDetailReportType.class.getName();

    private static Log log=LogFactory.getLog(EditableDetailReportType.class);

    private final static List<String> LST_ALL_ACCESSMODE=new ArrayList<String>();

    static
    {
        LST_ALL_ACCESSMODE.add(Consts.ADD_MODE);
        LST_ALL_ACCESSMODE.add(Consts.READ_MODE);
        LST_ALL_ACCESSMODE.add(Consts.UPDATE_MODE);
        LST_ALL_ACCESSMODE.add(Consts.READONLY_MODE);
    }

    public EditableDetailReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            this.ersqlbean=(EditableReportSqlBean)((ReportBean)comCfgBean).getSbean().getExtendConfigDataForReportType(KEY);
        }
    }
    
    public List<String> getLstAllAccessModes()
    {
        return LST_ALL_ACCESSMODE;
    }

    protected boolean getDefaultShowContextMenu()
    {
        return true;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        super.initUrl(applicationConfigBean,rrequest);
        String accessmode=rrequest.getStringAttribute(applicationConfigBean.getId()+"_ACCESSMODE",getDefaultAccessMode()).toLowerCase();
        //        /**
        
        //         * 其它访问模式不保留到URL中，因为它们可以通过“添加”，“修改”等按钮切换进来，而readonly访问模式不能与其它模式切换，因此当用户想以readonly方式访问，则后续都是以这个访问。
        
        //         */
        
        
        
        //        }
        if(!accessmode.equals(""))
        {
            rrequest.addParamToUrl(applicationConfigBean.getId()+"_ACCESSMODE",accessmode,true);
        }
        String referedReportIdByEditablelist=rrequest.getStringAttribute("WX_REFEREDREPORTID","");
        if(referedReportIdByEditablelist.equals(applicationConfigBean.getId()))
        {
            rrequest.addParamToUrl("WX_REFEREDREPORTID","rrequest{WX_REFEREDREPORTID}",true);
            rrequest.addParamToUrl("SRCPAGEID","rrequest{SRCPAGEID}",true);
            rrequest.addParamToUrl("SRCREPORTID","rrequest{SRCREPORTID}",true);
            rrequest.addParamToUrl("EDITTYPE","rrequest{EDITTYPE}",true);
        }
    }
    
    protected void initReportBeforeDoStart()
    {
        super.initReportBeforeDoStart();
        String referedReportIdByEditablelist=rrequest.getStringAttribute("WX_REFEREDREPORTID","");
        if(referedReportIdByEditablelist.equals(rbean.getId()))
        {
            rrequest.authorize(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts_Private.CANCEL_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");
            String edittype=rrequest.getStringAttribute("EDITTYPE","");
            if(edittype.equals(Consts.UPDATE_MODE))
            {
                rrequest.authorize(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts_Private.ADD_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");//添加按钮将不显示出来
                rrequest.authorize(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts_Private.DELETE_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");
            }
        }
    }

    public void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        String accessmode=null;
        if(!EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            EditableReportAssistant.getInstance().doAllReportsSaveAction(rrequest);
            SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
            if(sidbean!=null&&(sidbean.hasSavingData()||sidbean.hasDeleteData()))
            {
                accessmode=getDefaultAccessMode();
                if(accessmode==null) accessmode="";
            }
        }
        if(accessmode==null)
        {
            accessmode=rrequest.getStringAttribute(rbean.getId()+"_ACCESSMODE",getDefaultAccessMode()).toLowerCase();
            if(!getLstAllAccessModes().contains(accessmode)) accessmode=getDefaultAccessMode();//如果传入的状态不是此报表类型能接受的，则采用默认状态访问报表
        }
        setNewAccessMode(accessmode);
    }

    public int[] doSaveAction(IConnection conn) throws SQLException
    {
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        int[] result=new int[]{0,0};
        if(sidbean==null) return result;
        if(sidbean.hasDeleteData())
        {
            result[0]=updateDBData(ersqlbean.getDeletebean(),conn);
            result[1]=4;
        }else if(sidbean.hasSavingData())
        {
            boolean[] shouldDoSave=sidbean.getShouldDoSave();
            if(shouldDoSave[0]||(shouldDoSave[3]&&Consts.ADD_MODE.equals(sidbean.getUpdatetype())))
            {
                result[0]=updateDBData(ersqlbean.getInsertbean(),conn);
                result[1]=1;
            }else if(shouldDoSave[1]||(shouldDoSave[3]&&Consts.UPDATE_MODE.equals(sidbean.getUpdatetype())))
            {//如果当前修改了记录，则执行保存修改数据的动作
                result[0]=updateDBData(ersqlbean.getUpdatebean(),conn);
                result[1]=2;
            }
        }
        return result;
        
        
    }

    protected int updateDBData(EditableReportUpdateDataBean updatedatabean,IConnection conn) throws SQLException
    {
        if(updatedatabean==null) return 0;
        int rtnVal=Integer.MIN_VALUE;
        if(rbean.getInterceptor()!=null)
        {
            rtnVal=rbean.getInterceptor().beforeSave(rrequest,rbean);
            if(rtnVal==IInterceptor.WX_TERMINATE) return 0;
        }
        if(rtnVal!=IInterceptor.WX_IGNORE)
        {
            rtnVal=EditableReportAssistant.getInstance().updateDBData(rbean,rrequest,conn,updatedatabean);
            if(rtnVal==IInterceptor.WX_TERMINATE) return 0;//在保存的时候返回了IInterceptor.WX_TERMINATE，则都视为本次没有进行保存操作
        }
        if(rbean.getInterceptor()!=null)
        {
            rbean.getInterceptor().afterSave(rrequest,rbean);
        }
        String referedReportIdByEditablelist=rrequest.getStringAttribute("WX_REFEREDREPORTID","");
        if(referedReportIdByEditablelist.equals(rbean.getId()))
        {
            String srcpageid=rrequest.getStringAttribute("SRCPAGEID","");
            String srcreportid=rrequest.getStringAttribute("SRCREPORTID","");
            String srcedittype=rrequest.getStringAttribute("EDITTYPE","");
            StringBuffer paramsBuf=new StringBuffer();
            paramsBuf.append("{pageid:\""+srcpageid+"\"");
            paramsBuf.append(",reportid:\""+srcreportid+"\"");
            paramsBuf.append(",edittype:\""+srcedittype+"\"}");
            rrequest.getWResponse().addOnloadMethod("closeMeAndRefreshParentReport",paramsBuf.toString(),true);
        }else if(updatedatabean.getEdittype()==EditableReportUpdateDataBean.EDITTYPE_INSERT&&updatedatabean.getMUpdateConditions()!=null
                &&updatedatabean.getMUpdateConditions().size()>0)
        {
            CacheDataBean cdb=rrequest.getCdb(rbean.getId());
            Map<String,String> mParamValues=cdb.getLstEditedData(updatedatabean).get(0);
            Map<String,String> mExternalValues=null;
            if(cdb.getLstEditedExternalValues(updatedatabean)!=null&&cdb.getLstEditedExternalValues(updatedatabean).size()>0)
                mExternalValues=cdb.getLstEditedExternalValues(updatedatabean).get(0);
            String paramvalue;
            ColBean cbeanTmp;
            Set<Entry<String,String>> entrySetTmp=updatedatabean.getMUpdateConditions().entrySet();
            for(Entry<String,String> entry:entrySetTmp)
            {
                paramvalue=entry.getValue();
                if(Tools.isDefineKey("@",paramvalue))
                {
                    paramvalue=Tools.getRealKeyByDefine("@",paramvalue);
                    cbeanTmp=rbean.getDbean().getColBeanByColProperty(paramvalue);
                    if(cbeanTmp==null) throw new WabacusRuntimeException("报表"+rbean.getPath()+"中不存在"+paramvalue+"的<col/>");
                    paramvalue=EditableReportAssistant.getInstance().getColParamValue(rrequest,mParamValues,cbeanTmp);
                    if(paramvalue==null) log.warn("没有从新增的记录中获取到property为"+entry.getValue()+"的<col/>的数据");
                }else if(Tools.isDefineKey("#",paramvalue))
                {//从<external-values/>中取值
                    paramvalue=mExternalValues.get(Tools.getRealKeyByDefine("#",paramvalue));
                }
                if(paramvalue==null) paramvalue="";
                rrequest.setAttribute(entry.getKey(),paramvalue);
            }
        }
        String updatetype=null;
        if(updatedatabean.getEdittype()==1)
        {
            updatetype="add";
        }else if(updatedatabean.getEdittype()==2)
        {//当前是保存“修改”操作
            updatetype="update";
        }else
        {
            updatetype="delete";
        }
        rtnVal=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,updatetype);
        if(referedReportIdByEditablelist.equals(rbean.getId())) return 1;
        return rtnVal;
    }

    public String getDefaultAccessMode()
    {
        return Consts.READ_MODE;
    }
    
    public String getRealAccessMode()
    {
        return this.realAccessMode;
    }
    
    public void setNewAccessMode(String newaccessmode)
    {
        if(newaccessmode==null||newaccessmode.trim().equals("")) return;
        if(!this.getLstAllAccessModes().contains(newaccessmode))
        {
            log.warn("设置报表"+rbean.getPath()+"的新访问模式为"+newaccessmode+"失败，此报表对应的报表类型不支持此访问模式");
            return;
        }
        rrequest.setAttribute(rbean.getId(),"CURRENT_ACCESSMODE",newaccessmode);
        rrequest.addParamToUrl(rbean.getId()+"_ACCESSMODE",newaccessmode,true);
        rrequest.setAttribute(rbean.getId()+"_ACCESSMODE",newaccessmode);
        if(Consts.READONLY_MODE.equals(newaccessmode))
        {
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode","true");
        }else
        {
            rrequest.getAttributes().remove(rbean.getId()+"_isReadonlyAccessmode");
        }
    }

    private boolean hasLoadedData=false;//此报表是否已经加载过数据，这里一定要定义成private，而不能定义成protected，因为有可能被子类的super.loadReportData()调用，因此不能父子报表共用hasLoadedData变量
    
    public  boolean isLoadedReportData()
    {
        return this.hasLoadedData;
    }
    
    protected void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        super.setHasLoadedDataFlag(hasLoadedDataFlag);
        this.hasLoadedData=hasLoadedDataFlag;
    }
    
    protected void initLoadReportData()
    {
        super.initLoadReportData();
        if(this.rbean.isSlaveReportDependsonDetailReport()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            AbsReportType parentReportTypeObj=(AbsReportType)rrequest.getComponentTypeObj(this.rbean.getDependParentId(),null,false);
            if(parentReportTypeObj!=null&&parentReportTypeObj instanceof EditableDetailReportType &&parentReportTypeObj.getParentContainerType()!=null)
            {
                String parentRealAccessMode=((EditableDetailReportType)parentReportTypeObj).getRealAccessMode();
                if(parentRealAccessMode==Consts.ADD_MODE)
                {
                    setNewAccessMode(Consts.ADD_MODE);
                }else if(rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode()).equals(Consts.ADD_MODE))
                {//如果父报表不是添加模式，但本报表是添加模式，则强制变为默认模式
                    setNewAccessMode(getDefaultAccessMode());
                }
            }
        }
    }
    
    public void loadReportData()
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        initLoadReportData();
        SqlBean sqlbean=rbean.getSbean();
        if(sqlbean.getValue()!=null&&!sqlbean.getValue().trim().equals("")
                &&!rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode()).equals(Consts.ADD_MODE))
        {
            super.loadReportData();
        }
        if(!this.isLazyDataLoad())
        {
            if(!EditableReportAssistant.getInstance().isReadonlyAccessMode(this)&&(lstReportData==null||lstReportData.size()==0))
            {
                if(ersqlbean!=null&&ersqlbean.getInsertbean()!=null)
                {//如果配置了添加功能，则用添加模式
                    setNewAccessMode(Consts.ADD_MODE);
                    Object dataObj=ReportAssistant.getInstance().getReportDataPojoInstance(rbean);
                    this.lstReportData=new ArrayList();
                    this.lstReportData.add(dataObj);
                    if(dataObj instanceof IFormat)
                    {
                        ((IFormat)dataObj).format(rrequest,rbean);
                    }
                }else
                {
                    
                }
            }
        }
        initRealAccessMode();
    }

    protected void initRealAccessMode()
    {
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            realAccessMode=Consts.READONLY_MODE;
        }else
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode());
            if(accessmode.equals(Consts.ADD_MODE))
            {
                if(ersqlbean.getInsertbean()==null)
                {
                    throw new WabacusRuntimeException("报表"+rbean.getPath()+"没有配置<insert/>，不能进行添加操作");
                }
                realAccessMode=Consts.ADD_MODE;
            }else if(accessmode.equals(Consts.UPDATE_MODE))
            {
                if(ersqlbean.getUpdatebean()==null)
                {//没有配置<update/>
                    realAccessMode=Consts.READ_MODE;
                }else
                {
                    realAccessMode=Consts.UPDATE_MODE;
                }
            }else
            {
                realAccessMode=Consts.READ_MODE;
            }
        }
    }
    
    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        if(realAccessMode.equals(Consts.UPDATE_MODE))
        {
            resultBuf.append(ersqlbean.getValidateSaveMethodAndParams(rrequest,false));
        }else if(realAccessMode.equals(Consts.ADD_MODE))
        {//当前是添加模式，则加上保存添加数据的客户端校验方法及所用的动态参数
            resultBuf.append(ersqlbean.getValidateSaveMethodAndParams(rrequest,true));
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
        String col_editvalue=null;
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype()))
        {
            col_editvalue=getColOriginalValue(dataObj,cbean);
        }else
        {
            EditableReportColBean ercolbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercolbean==null||ercolbean.getUpdateCbean()==null)
            {
                col_editvalue=getColOriginalValue(dataObj,cbean);
            }else
            {
                col_editvalue=getColOriginalValue(dataObj,ercolbean.getUpdateCbean());
            }
        }
        if(col_editvalue==null) col_editvalue="";
        return EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,ersqlbean.getUpdatebean(),cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String showHiddenCol(ColBean cbeanHidden,Object colDataObj)
    {
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbeanHidden.getDisplaytype())) return "";
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.showHiddenCol(cbeanHidden,colDataObj);
        StringBuffer resultBuf=new StringBuffer();
        String hiddenValuename=null;
        String hiddenValue=null;
        if(this.realAccessMode.equals(Consts.ADD_MODE))
        {
            hiddenValuename="value_name";
            hiddenValue="value";
        }else
        {
            hiddenValuename="oldvalue_name";
            hiddenValue="oldvalue";
        }
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        resultBuf.append("<font name=\"font_").append(rbean.getGuid()).append("\"");
        resultBuf.append(" id=\"font_").append(rbean.getGuid()).append("\" ");
        resultBuf.append(hiddenValuename+"=\"").append(ercdatabean.getValuename());
        resultBuf.append("\" "+hiddenValue+"=\"").append(Tools.htmlEncode(ercdatabean.getOldvalue()));
        resultBuf.append("\" style=\"display:none\"></font>");//不能用<font/>，而必须是<font></font>格式，否则在IE上取子节点会有问题
        return resultBuf.toString();
    }
    
    protected String getColValueTdPropertiesAndContent(ColBean cbean,Object dataObj,Object colDataObj,StringBuffer tdPropsBuf)
    {
        EditableReportColBean ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbeanTmp==null) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        StringBuffer resultBuf=new StringBuffer();
        if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {
            tdPropsBuf.append(" style=\"display:none;\" ");
            resultBuf.append(showTempHiddenCol(cbean,ercdatabean,true));
        }else
        {
            String col_displayvalue=getColDisplayValue(cbean,ercbeanTmp,dataObj,ercdatabean,null);
            ColDataByInterceptor coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,0,col_displayvalue);
            if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
            {
                col_displayvalue=coldataByInterceptor.getDynvalue();
            }
            if(col_displayvalue==null||col_displayvalue.trim().equals("")) col_displayvalue="&nbsp;";
            resultBuf.append(getDisplayedColFontValue(cbean,ercbeanTmp,ercdatabean,true));
            resultBuf.append(col_displayvalue);
            tdPropsBuf.append(getDetailTdValuestyleproperty(cbean,coldataByInterceptor));
        }
        resultBuf.append("</font>");
        return resultBuf.toString();
    }
    
    public String showColData(ColBean cbean,boolean showpart,boolean showinputbox,String dynstyleproperty)
    {
        if(!showpart||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return super.showColData(cbean,showpart,showinputbox,dynstyleproperty);//显示label部分或显示在导出文件中
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) return "";
        
        if(cbean.isNonValueCol()) return "";
        Object dataObj=null;
        if(this.lstReportData!=null&&this.lstReportData.size()>0)
        {
            dataObj=this.lstReportData.get(0);
        }
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbean==null) return super.showColData(cbean,showpart,showinputbox,dynstyleproperty);
        Object colDataObj;
        EditableReportColDataBean ercdatabean;
        StringBuffer resultBuf=new StringBuffer();
        if(rrequest.getStringAttribute(rbean.getGuid()+"_showHiddenCols","").equals(""))
        {//还没有显示删除数据时需要用到的displaytype为hidden的<col/>
            rrequest.setAttribute(rbean.getGuid()+"_showHiddenCols","true");
            for(ColBean cbeanTemp:rbean.getDbean().getLstCols())
            {//显示所有隐藏列的<font/>
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbeanTemp.getDisplaytype()))
                {
                    colDataObj=this.initDisplayCol(cbeanTemp,dataObj);
                    resultBuf.append(showHiddenCol(cbeanTemp,colDataObj));
                }
            }
        }
        colDataObj=this.initDisplayCol(cbean,dataObj);
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.showColData(cbean,showpart,showinputbox,dynstyleproperty);
        ercdatabean=(EditableReportColDataBean)colDataObj;
        if(this.mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {
            return this.showTempHiddenCol(cbean,ercdatabean,true);
        }
        resultBuf.append(getDisplayedColFontValue(cbean,ercbean,ercdatabean,showinputbox));
        if(showinputbox)
        {
            String col_displayvalue=getColDisplayValue(cbean,ercbean,dataObj,ercdatabean,dynstyleproperty);
            if(col_displayvalue==null) col_displayvalue="";
            ColDataByInterceptor coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,0,col_displayvalue);
            if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
            {//如果拦截器对象返回此列的动态数据
                col_displayvalue=coldataByInterceptor.getDynvalue();
            }
            if(col_displayvalue==null||col_displayvalue.trim().equals("")) col_displayvalue="&nbsp;";
            resultBuf.append(col_displayvalue).append("</font>");
        }
        return resultBuf.toString();
    }
    
    private String showTempHiddenCol(ColBean cbean,EditableReportColDataBean ercdatabean,boolean isRealHidden)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<font id=\"font_").append(rbean.getGuid()).append("\" name=\"font_").append(rbean.getGuid()).append("\" ");
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(this.realAccessMode.equals(Consts.ADD_MODE)
                ||(this.realAccessMode.equals(Consts.UPDATE_MODE)&&ercbean!=null&&ercbean.isEditableForUpdate()))
        {
            resultBuf.append(" value_name=\"").append(ercdatabean.getValuename()).append("\"");
            resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
        }
        if(!this.realAccessMode.equals(Consts.ADD_MODE))
        {
            String oldvaluename=ercdatabean.getValuename();
            if(ercbean!=null&&ercbean.isEditableForUpdate())
            {
                oldvaluename=oldvaluename+"_old";
            }
            resultBuf.append(" oldvalue_name=\"").append(oldvaluename).append("\"");
            resultBuf.append(" oldvalue=\"").append(Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
        }
        if(isRealHidden) resultBuf.append(" style=\"display:none\"");
        resultBuf.append(">");
        return resultBuf.toString();
    }
    
    private String getDisplayedColFontValue(ColBean cbean,EditableReportColBean ercbean,EditableReportColDataBean ercdatabean,boolean showinputbox)
    {
        if(!this.realAccessMode.equals(Consts.ADD_MODE)&&!this.realAccessMode.equals(Consts.UPDATE_MODE))
        {
            return showTempHiddenCol(cbean,ercdatabean,false);
        }
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<font id=\"font_").append(rbean.getGuid()).append("\" name=\"font_").append(rbean.getGuid()).append("\" ");
        if(!showinputbox)
        {//当前列是由用户自己显示输入框（同时表示当前列是可编辑的，因为不可编辑的必定由框架自动显示值）
            resultBuf.append(" customized_inputbox=\"true\" value=\"").append(ercdatabean.getValue()).append("\"");
        }
        String valuename=ercdatabean.getValuename();
        if(this.realAccessMode.equals(Consts.UPDATE_MODE))
        {
            String oldvaluename=valuename;
            if(ercbean!=null&&ercbean.isEditableForUpdate())
            {
                resultBuf.append(" value_name=\"").append(valuename).append("\"");
                oldvaluename=oldvaluename+"_old";
            }
            resultBuf.append(" oldvalue=\"").append(Tools.htmlEncode(ercdatabean.getOldvalue())).append("\" ");
            resultBuf.append(" oldvalue_name=\"").append(oldvaluename).append("\"");
        }else
        {
            resultBuf.append(" value_name=\"").append(valuename).append("\"");
            if(ercbean==null||!ercbean.isEditableForInsert())
            {
                resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean==null?"":ercdatabean.getOldvalue())).append("\"");
            }
        }
        resultBuf.append(">");
        return resultBuf.toString();
    }
    
    private String getColDisplayValue(ColBean cbean,EditableReportColBean ercbean,Object dataObj,EditableReportColDataBean ercdatabean,String dynstyleproperty)
    {
        String col_displayvalue=null;
        if(ercbean==null) return cbean.getDisplayValue(dataObj,rrequest);
        if((this.realAccessMode.equals(Consts.ADD_MODE)&&ercbean.isEditableForInsert())
                ||(this.realAccessMode.equals(Consts.UPDATE_MODE)&&ercbean.isEditableForUpdate()))
        {
            col_displayvalue=ercbean.getInputbox().getDisplayStringValue(rrequest,ercdatabean.getValue(),dynstyleproperty,
                    cbean.checkReadonlyPermission(rrequest));
        }else
        {
            col_displayvalue=cbean.getDisplayValue(dataObj,rrequest);
        }
        return col_displayvalue;
    }
    
    public String getColOriginalValue(Object dataObj,ColBean cbean)
    {
        if(cbean==null||dataObj==null) return "";
        return cbean.getDisplayValue(dataObj,rrequest);
    }

    public boolean isReadonlyCol(ColBean cbean)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return true;
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) return true;
        if(cbean.isNonValueCol()) return true;
        if(!this.realAccessMode.equals(Consts.ADD_MODE)&&!this.realAccessMode.equals(Consts.UPDATE_MODE)) return true;
        if(ersqlbean==null) return true;
        if(cbean.checkReadonlyPermission(rrequest)) return true;
        EditableReportColBean ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbeanTmp==null) return true;
        if(this.realAccessMode.equals(Consts.ADD_MODE))
        {
           return !ercbeanTmp.isEditableForInsert();
        }else
        {//updatemode
            return !ercbeanTmp.isEditableForUpdate();
        }
    }
    
    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            if(rbean.getSbean().getValue()!=null&&!rbean.getSbean().getValue().trim().equals("")&&this.realAccessMode.equals(Consts.READ_MODE))
            {
                return true;
            }
        }else if(buttonType instanceof UpdateButton)
        {
            if(ersqlbean.getUpdatebean()==null) return false;
            if(rbean.getSbean().getValue()!=null&&!rbean.getSbean().getValue().trim().equals("")&&this.realAccessMode.equals(Consts.READ_MODE)
                    &&this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                return true;
            }
        }else if(buttonType instanceof DeleteButton)
        {
            if(ersqlbean.getDeletebean()==null) return false;
            if(this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                if(rbean.getSbean().getValue()!=null&&!rbean.getSbean().getValue().trim().equals("")&&this.realAccessMode.equals(Consts.READ_MODE))
                {
                    return true;
                }
                if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE))
                {
                    return true;
                }
            }
        }else if(buttonType instanceof SaveButton)
        {
            if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE)&&this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                return true;
            }
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE))
            {
                return true;
            }
        }else if(buttonType instanceof CancelButton||buttonType instanceof ResetButton)
        {
            if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE))
            {
                return true;
            }
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE))
            {
                return true;
            }
        }
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
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean!=null&&ersqlbean.getInsertbean()!=null)
        {
            XmlElementBean eleInsertBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"insert");
            String condition=eleInsertBean.attributeValue("condition");
            if(condition!=null)
            {//如果在<insert/>中配置了condition，说明添加数据后需要以update/read模式显示当前添加的数据
                condition=condition.trim();
                if(condition.equals(""))
                {
                    ersqlbean.getInsertbean().setMUpdateConditions(null);
                }else
                {
                    Map<String,String> mCondtions=new HashMap<String,String>();
                    List<String> lstConditions=Tools.parseStringToList(condition,";");
                    for(String contemp:lstConditions)
                    {
                        if(contemp==null||contemp.trim().equals("")) continue;
                        contemp=contemp.trim();
                        int idxEqual=contemp.indexOf("=");
                        if(idxEqual<=0)
                        {
                            throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置的<insert/>的condition属性"+condition+"不合法");
                        }
                        String conname=contemp.substring(0,idxEqual).trim();
                        String colprop=contemp.substring(idxEqual+1).trim();
                        if(conname.equals(""))
                        {
                            throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置的<insert/>的condition属性"+condition
                                    +"不合法，没有指定要赋值的查询条件name属性");
                        }
                        mCondtions.put(conname,colprop);
                    }
                    ersqlbean.getInsertbean().setMUpdateConditions(mCondtions);
                }
            }
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        ComponentConfigLoadManager.doEditableReportTypePostLoad(reportbean,KEY);
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)reportbean.getSbean().getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null) return 1;
        processEditableButtons(ersqlbean);
        EditableReportColBean ercolbeanTmp=null;
        List<SubmitFunctionParamBean> lstUpdateParams=new ArrayList<SubmitFunctionParamBean>();
        StringBuffer updateScriptBuffer=new StringBuffer();
        List<SubmitFunctionParamBean> lstInsertParams=new ArrayList<SubmitFunctionParamBean>();
        StringBuffer insertScriptBuffer=new StringBuffer();
        List<ColBean> lstColBeans=reportbean.getDbean().getLstCols();
        for(ColBean cbean:lstColBeans)
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            ercolbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercolbeanTmp==null||ercolbeanTmp.getInputbox()==null) continue;
            if(ercolbeanTmp.isEditableForUpdate())
            {
                JavaScriptAssistant.getInstance().writeEditableReportColValidateJs(ercolbeanTmp,updateScriptBuffer,lstUpdateParams);//如果当前列配置了客户端校验，且出现在更新数据的字段列表中，则为它生成JS校验代码
            }
            if(ercolbeanTmp.isEditableForInsert())
            {
                JavaScriptAssistant.getInstance().writeEditableReportColValidateJs(ercolbeanTmp,insertScriptBuffer,lstInsertParams);
            }
        }
        if(updateScriptBuffer.length()>0)
        {
            writeEditValidateJsToFiles(updateScriptBuffer.toString(),reportbean.getGuid()+"_validateSaveUpdate",reportbean);
            ersqlbean.setValidateSaveUpdateMethod(reportbean.getGuid()+"_validateSaveUpdate");
            ersqlbean.setLstValidateSavingUpdateDynParams(lstUpdateParams);
        }
        if(insertScriptBuffer.length()>0)
        {
            writeEditValidateJsToFiles(insertScriptBuffer.toString(),reportbean.getGuid()+"_validateSaveInsert",reportbean);
            ersqlbean.setValidateSaveAddingMethod(reportbean.getGuid()+"_validateSaveInsert");
            ersqlbean.setLstValidateSavingAddDynParams(lstInsertParams);
        }
        return 1;
    }

    private void processEditableButtons(EditableReportSqlBean ersqlbean)
    {
        ReportBean reportbean=ersqlbean.getOwner().getReportBean();
        if(ersqlbean.getInsertbean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,AddButton.class,Consts.ADD_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(AddButton.class);
        }
        if(ersqlbean.getUpdatebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,UpdateButton.class,Consts.MODIFY_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(UpdateButton.class);
        }
        if(ersqlbean.getDeletebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
        if(ersqlbean.getInsertbean()!=null||ersqlbean.getUpdatebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,SaveButton.class,Consts.SAVE_BUTTON_DEFAULT);
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,CancelButton.class,Consts.CANCEL_BUTTON_DEFAULT);
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,ResetButton.class,Consts.RESET_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(SaveButton.class);
            reportbean.getButtonsBean().removeAllCertainTypeButtons(CancelButton.class);
            reportbean.getButtonsBean().removeAllCertainTypeButtons(ResetButton.class);
        }
    }

    private void writeEditValidateJsToFiles(String script,String validateMethodName,ReportBean reportbean)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+validateMethodName+"(metadataObj){");
        resultBuf.append("var paramsObj=getObjectByJsonString(metadataObj.metaDataSpanObj.getAttribute('validateSaveMethodDynParams'));");//从元数据中取到校验所需的动态参数
        resultBuf.append("var fontChilds=document.getElementsByName('font_"+reportbean.getGuid()+"');");
        resultBuf.append("if(fontChilds==null||fontChilds.length==0) return true;");
        resultBuf.append("var boxObj;var boxValue;var value_name;");
        resultBuf.append("for(var i=0;i<fontChilds.length;i=i+1){if(fontChilds[i]==null) continue;");
        resultBuf.append("boxObj=fontChilds[i];");//在这种报表类型中，传入客户端校验函数的输入框对象不是真正的输入框对象，而是其所在的<font/>对象
        resultBuf.append("value_name=boxObj.getAttribute('value_name');if(value_name==null||value_name=='') continue;");
        resultBuf.append("boxValue=getInputboxValueByParentFont(boxObj);");//从<font/>中获取到此输入框的值
        resultBuf.append("if(boxValue==null) boxValue='';");
        resultBuf.append(script);
        resultBuf.append("} return true;}");
        JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(reportbean.getPageBean(),resultBuf.toString());
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_EDITABLEDETAIL;
    }
}
