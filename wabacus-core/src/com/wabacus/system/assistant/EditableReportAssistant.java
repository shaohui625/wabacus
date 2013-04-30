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
package com.wabacus.system.assistant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeWarningException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSecretColValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UUIDGenerator;

public class EditableReportAssistant
{
    private static Log log=LogFactory.getLog(EditableReportAssistant.class);

    private final static EditableReportAssistant instance=new EditableReportAssistant();

    protected EditableReportAssistant()
    {}

    public static EditableReportAssistant getInstance()
    {
        return instance;
    }

    public String getInputBoxId(ColBean cbean)
    {
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return "";
        return cbean.getReportBean().getGuid()+"_"+cbean.getProperty();
    }

    public String getInputBoxId(ReportBean rbean,String property)
    {
        if(property==null||property.trim().equals("")) return "";
        return rbean.getGuid()+"_"+property;
    }
    
    public String getColParamName(ColBean cbean)
    {
        return cbean.getProperty();
    }
    
    public String getColParamValue(ReportRequest rrequest,Map<String,String> mColParamsValue,ColBean cbean)
    {
        return getColParamValue(rrequest,cbean.getReportBean(),mColParamsValue,getColParamName(cbean));
    }
    
    public String getColParamValue(ReportRequest rrequest,ReportBean rbean,Map<String,String> mColParamsValue,String paramname)
    {
        if(mColParamsValue==null) return "";
        String paramvalue=null;
        if(mColParamsValue.containsKey(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX+paramname))
        {
            EditableReportSecretColValueBean secretColvalueBean=EditableReportSecretColValueBean.loadFromSession(rrequest,rbean);
            if(secretColvalueBean==null)
            {
                rrequest.getWResponse().getMessageCollector().warn("session过期，没有取到保存数据，请刷新后重试",null,true,Consts.STATECODE_FAILED);
            }
            String colkey=mColParamsValue.get(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX+paramname);
            if(colkey==null||colkey.trim().equals("")) return "";
            if(!secretColvalueBean.containsColkey(colkey))
            {
                paramvalue=colkey;
            }else
            {
                paramvalue=secretColvalueBean.getParamValue(colkey);
            }
        }else
        {//直接从客户端
            paramvalue=mColParamsValue.get(paramname);
        }
        if(paramvalue==null) paramvalue="";
        return paramvalue;
    }
    
    public String getColParamRealValue(ReportRequest rrequest,ReportBean rbean,String paramname,String paramvalue)
    {
        if(paramname==null||!paramname.startsWith(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX)) return paramvalue;
        EditableReportSecretColValueBean secretColvalueBean=EditableReportSecretColValueBean.loadFromSession(rrequest,rbean);
        if(secretColvalueBean==null)
        {
            throw new WabacusRuntimeException("session过期，无法获取到"+paramname+"参数的真正参数值");
        }
        if(secretColvalueBean.containsColkey(paramvalue))
        {
            paramvalue=secretColvalueBean.getParamValue(paramvalue);
        }
        return paramvalue;
    }
    
    public boolean isReadonlyAccessMode(IEditableReportType reportTypeObj)
    {
        ReportBean rbean=((AbsReportType)reportTypeObj).getReportBean();
        ReportRequest rrequest=((AbsReportType)reportTypeObj).getReportRequest();
        String isReadonlyAccessmode=rrequest.getStringAttribute(rbean.getId()+"_isReadonlyAccessmode","");
        if(isReadonlyAccessmode.equals(""))
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId()+"_ACCESSMODE",reportTypeObj.getDefaultAccessMode()).toLowerCase();
            if(accessmode.equals(Consts.READONLY_MODE)||rrequest.checkPermission(rbean.getId(),null,null,"readonly")||rbean.getSbean()==null)
            {
                isReadonlyAccessmode="true";
            }else
            {
                EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
                if(ersqlbean==null||(ersqlbean.getDeletebean()==null&&ersqlbean.getInsertbean()==null&&ersqlbean.getUpdatebean()==null))
                {//没有配置任何编辑功能
                    isReadonlyAccessmode="true";
                }else
                {
                    isReadonlyAccessmode="false";
                }
            }
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode",isReadonlyAccessmode);
        }
        return Boolean.valueOf(isReadonlyAccessmode);
    }

    public void doAllReportsSaveAction(ReportRequest rrequest)
    {
        String flag=rrequest.getStringAttribute("WX_HAS_SAVING_DATA_"+rrequest.getPagebean().getId(),"");
        if(flag.equals("true")) return;
        rrequest.setAttribute("WX_HAS_SAVING_DATA_"+rrequest.getPagebean().getId(),"true");
        Map<Object,Object> mAtts=new HashMap<Object,Object>();
        mAtts.putAll(rrequest.getAttributes());
        Object objKeyTmp;
        Object objValueTmp;
        String keyTmp;
        String valueTmp;
        Map<String,ReportBean> mSavingReportBeans=new HashMap<String,ReportBean>();
        Map<String,IEditableReportType> mSavingReportObjs=new HashMap<String,IEditableReportType>();
        String reportidTmp;
        for(Entry<Object,Object> entryTmp:mAtts.entrySet())
        {
            objKeyTmp=entryTmp.getKey();
            objValueTmp=entryTmp.getValue();
            if(!(objKeyTmp instanceof String)||!(objValueTmp instanceof String)) continue;
            keyTmp=(String)objKeyTmp;
            if(keyTmp.endsWith("_INSERTDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_INSERTDATAS".length());
            }else if(keyTmp.endsWith("_UPDATEDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_UPDATEDATAS".length());
            }else if(keyTmp.endsWith("_DELETEDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_DELETEDATAS".length());
            }else if(keyTmp.endsWith("_CUSTOMIZEDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_CUSTOMIZEDATAS".length());
            }else
            {
                continue;
            }
            if(reportidTmp.trim().equals("")) continue;
            ReportBean rbeanTmp=rrequest.getPagebean().getReportChild(reportidTmp,true);
            if(rbeanTmp==null) continue;
            valueTmp=(String)objValueTmp;
            if(valueTmp.trim().equals("")) continue;
            Object objTmp=rrequest.getComponentTypeObj(rbeanTmp,null,true);
            if(!(objTmp instanceof IEditableReportType)) continue;//当前报表不是可编辑报表
            if(isReadonlyAccessMode((IEditableReportType)objTmp)) continue;
            mSavingReportBeans.put(reportidTmp,rbeanTmp);
            mSavingReportObjs.put(reportidTmp,(IEditableReportType)objTmp);
            initEditedParams(rrequest,rbeanTmp);
        }
        doSaveAction(rrequest,mSavingReportObjs);
    }

    private void initEditedParams(ReportRequest rrequest,ReportBean reportbean)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)reportbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        CacheDataBean cdb=rrequest.getCdb(reportbean.getId());
        boolean[] shouldDoSave=new boolean[4];
        SaveInfoDataBean sidbean=new SaveInfoDataBean();
        sidbean.setShouldDoSave(shouldDoSave);
        List<Map<String,String>> lstParamsValue=parseSaveDataStringToList(rrequest.getStringAttribute(reportbean.getId()+"_CUSTOMIZEDATAS",""));
        if(lstParamsValue!=null&&lstParamsValue.size()>0)
        {
            Map<String,String> mCustomizeData=lstParamsValue.get(0);
            cdb.getAttributes().put("WX_UPDATE_CUSTOMIZEDATAS",mCustomizeData);//对于用户自定义的数据，都会存放在一个Map中，键为参数名；值为参数值
            shouldDoSave[3]=true;
            if(mCustomizeData!=null&&mCustomizeData.containsKey("WX_UPDATETYPE"))
            {
                sidbean.setUpdatetype(mCustomizeData.get("WX_UPDATETYPE"));
            }else
            {
                sidbean.setUpdatetype("");
            }
        }else
        {
            shouldDoSave[3]=false;
        }
        shouldDoSave[0]=initEditedParams(rrequest.getStringAttribute(reportbean.getId()+"_INSERTDATAS",""),rrequest,reportbean,ersqlbean
                .getInsertbean());
        shouldDoSave[1]=initEditedParams(rrequest.getStringAttribute(reportbean.getId()+"_UPDATEDATAS",""),rrequest,reportbean,ersqlbean
                .getUpdatebean());
        shouldDoSave[2]=initEditedParams(rrequest.getStringAttribute(reportbean.getId()+"_DELETEDATAS",""),rrequest,reportbean,ersqlbean
                .getDeletebean());
        rrequest.setAttribute(reportbean.getId(),"SAVEINFO_DATABEAN",sidbean);
    }

    private boolean initEditedParams(String strParams,ReportRequest rrequest,ReportBean reportbean,EditableReportUpdateDataBean updatebean)
    {
        if(strParams.equals("")||updatebean==null) return false;
        log.debug(strParams);
        List<Map<String,String>> lstParamsValue=parseSaveDataStringToList(strParams);
        if(lstParamsValue==null||lstParamsValue.size()==0) return false;
        CacheDataBean cdb=rrequest.getCdb(reportbean.getId());
        cdb.setLstEditedData(updatebean,lstParamsValue);
        cdb.setLstEditedExternalValues(updatebean,getExternalValues(updatebean,lstParamsValue,reportbean,rrequest));
        return true;
    }

    public List<Map<String,String>> parseSaveDataStringToList(String strSavedata)
    {
        if(strSavedata==null||strSavedata.trim().equals("")) return null;
        List<String> lstRowDatas=Tools.parseStringToList(strSavedata.trim(),Consts_Private.SAVE_ROWDATA_SEPERATOR);
        List<Map<String,String>> lstResults=new ArrayList<Map<String,String>>();
        for(String rowdataTmp:lstRowDatas)
        {//解析每条记录
            if(rowdataTmp==null||rowdataTmp.trim().equals("")) continue;
            Map<String,String> mRowData=new HashMap<String,String>();
            List<String> lstColsData=Tools.parseStringToList(rowdataTmp,Consts_Private.SAVE_COLDATA_SEPERATOR);
            String colnameTmp;
            String colvalueTmp;
            for(String coldataTmp:lstColsData)
            {
                if(coldataTmp==null||coldataTmp.trim().equals("")) continue;
                int idx=coldataTmp.indexOf(Consts_Private.SAVE_NAMEVALUE_SEPERATOR);
                if(idx<=0) continue;
                colnameTmp=coldataTmp.substring(0,idx).trim();
                colvalueTmp=coldataTmp.substring(idx+Consts_Private.SAVE_NAMEVALUE_SEPERATOR.length());
                if(colnameTmp.equals("")||colvalueTmp.equals("")) continue;
                mRowData.put(colnameTmp,colvalueTmp);
            }
            if(mRowData.size()>0) lstResults.add(mRowData);
        }
        return lstResults;
    }

    private void doSaveAction(ReportRequest rrequest,Map<String,IEditableReportType> mSavingReportObjs)
    {
        if(mSavingReportObjs==null||mSavingReportObjs.size()==0) return;
        Map<String,Connection> mConnections=new HashMap<String,Connection>();
        IEditableReportType reportTypeObjTmp;
        ReportBean rbeanTmp;
        Connection connTmp;
        boolean hasSaveReport=false;
        boolean shouldStopRefreshDisplay=true;
        String transactionLevel=rrequest.getStringAttribute(rrequest.getPagebean().getId()+"_TRANSACTION_LEVEL","").toLowerCase();
        if(!transactionLevel.equals("")&&!Consts_Private.M_ALL_TRANSACTION_LEVELS.containsKey(transactionLevel))
        {
            log.warn("保存页面"+rrequest.getPagebean().getId()+"上的报表时，传入的事务属性"+transactionLevel+"无效，将启用默认事务");
            transactionLevel="";
        }
        boolean hasInsertData=false;//本次是否有添加数据
        boolean hasUpdateData=false;
        boolean hasDeleteData=false;
        int[] resultTmp=null;
        boolean isFailed=false;
        try
        {
            for(Entry<String,IEditableReportType> entryTmp:mSavingReportObjs.entrySet())
            {
                reportTypeObjTmp=entryTmp.getValue();
                rbeanTmp=((AbsReportType)reportTypeObjTmp).getReportBean();
                String datasource=rbeanTmp.getSbean().getDatasource();
                if(datasource==null||datasource.trim().equals("")) datasource=Consts.DEFAULT_KEY;
                connTmp=mConnections.get(datasource);
                if(connTmp==null)
                {
                    connTmp=rrequest.getConnection(datasource);
                    if(!transactionLevel.equals(Consts.TRANS_NONE))
                    {//本次保存是放在事务中的
                        connTmp.setAutoCommit(false);
                        if(!transactionLevel.equals(""))
                            connTmp.setTransactionIsolation(Consts_Private.M_ALL_TRANSACTION_LEVELS.get(transactionLevel));
                    }
                    mConnections.put(datasource,connTmp);
                }
                resultTmp=reportTypeObjTmp.doSaveAction(connTmp);
                if(resultTmp==null||resultTmp.length!=2||resultTmp[0]==0) continue;
                rrequest.getWResponse().addUpdateReportGuid(rbeanTmp.getGuid());
                if(resultTmp[0]==-1) shouldStopRefreshDisplay=false;
                if(resultTmp[1]==1)
                {
                    hasInsertData=true;
                }else if(resultTmp[1]==2)
                {
                    hasUpdateData=true;
                }else if(resultTmp[1]==3)
                {//对此报表同时有添加和修改操作
                    hasInsertData=true;
                    hasUpdateData=true;
                }else if(resultTmp[1]==4)
                {
                    hasDeleteData=true;
                }
                hasSaveReport=true;
            }
            commitRollBackAllTranses(mConnections,transactionLevel,true);
        }catch(WabacusRuntimeWarningException wrwe)
        {
            commitRollBackAllTranses(mConnections,transactionLevel,rrequest.getWResponse().getStatecode()!=Consts.STATECODE_FAILED);
            throw new WabacusRuntimeWarningException();
        }catch(Exception e)
        {
            isFailed=true;
            commitRollBackAllTranses(mConnections,transactionLevel,false);
            log.error("保存页面"+rrequest.getPagebean().getId()+"上的报表数据失败",e);
            if(resultTmp!=null&&resultTmp.length==2)
            {
                if(resultTmp[1]==1)
                {
                    hasInsertData=true;
                }else if(resultTmp[1]==2)
                {
                    hasUpdateData=true;
                }else if(resultTmp[1]==3)
                {
                    hasInsertData=true;
                    hasUpdateData=true;
                }else if(resultTmp[1]==4)
                {
                    hasDeleteData=true;
                }
            }
            if(!rrequest.isDisableAutoFailedPrompt()) promptFailedMessage(rrequest,hasInsertData,hasUpdateData,hasDeleteData);
        }
        if(hasSaveReport&&!isFailed)
        {
            if(!rrequest.isDisableAutoSuccessPrompt())
            {
                promptSuccessMessage(rrequest,hasInsertData,hasUpdateData,hasDeleteData);
            }
            if(shouldStopRefreshDisplay)
            {
                rrequest.getWResponse().terminateResponse(Consts.STATECODE_NONREFRESHPAGE);
            }
        }
    }

    private void commitRollBackAllTranses(Map<String,Connection> mConnections,String transactionLevel,boolean isSuccess)
    {
        Connection connTmp;
        for(Entry<String,Connection> entryTmp:mConnections.entrySet())
        {
            try
            {
                connTmp=entryTmp.getValue();
                if(!transactionLevel.equals(Consts.TRANS_NONE))
                {
                    if(isSuccess)
                    {
                        connTmp.commit();
                    }else
                    {
                        connTmp.rollback();
                    }
                }
                connTmp.setAutoCommit(true);
            }catch(SQLException e1)
            {
                log.error("保存数据时事务"+(isSuccess?"提交":"回滚")+"失败",e1);
            }
        }
    }
    
    private void promptFailedMessage(ReportRequest rrequest,boolean hasInsertData,boolean hasUpdateData,boolean hasDeleteData)
    {
        String errorprompt=null;
        if((hasInsertData&&hasDeleteData)||(hasUpdateData&&hasDeleteData))
        {//如果同时有增、删或同时有改、删操作
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${operate.failed.prompt}",false));
        }else if(hasInsertData&&hasUpdateData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${save.failed.prompt}",false));
        }else if(hasInsertData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${insert.failed.prompt}",false));
        }else if(hasUpdateData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${update.failed.prompt}",false));
        }else if(hasDeleteData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${delete.failed.prompt}",false));
        }else
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${operate.failed.prompt}",false));
        }
        rrequest.getWResponse().getMessageCollector().error(errorprompt,true);
    }

    private void promptSuccessMessage(ReportRequest rrequest,boolean hasInsertData,boolean hasUpdateData,boolean hasDeleteData)
    {
        String successprompt=null;
        if((hasInsertData&&hasDeleteData)||(hasUpdateData&&hasDeleteData))
        {//如果同时有增、删或同时有改、删操作
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${operate.success.prompt}",false));
        }else if(hasInsertData&&hasUpdateData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${save.success.prompt}",false));
        }else if(hasInsertData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${insert.success.prompt}",false));
        }else if(hasUpdateData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${update.success.prompt}",false));
        }else if(hasDeleteData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${delete.success.prompt}",false));
        }else
        {
            return;
        }
        rrequest.getWResponse().getMessageCollector().success(successprompt,false);
    }
    
    public int processAfterSaveAction(ReportRequest rrequest,ReportBean rbean,String updatetype)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean.getAfterSaveAction()!=null&&ersqlbean.getAfterSaveAction().length>0)
        {
            String afterSaveActionMethod=ersqlbean.getAfterSaveActionMethod();
            if(!afterSaveActionMethod.equals(""))
            {//如果配置了保存后回调函数，则将它们加入本次的onload函数中执行。
                StringBuffer paramsBuf=new StringBuffer();
                paramsBuf.append("{pageid:\""+rbean.getPageBean().getId()+"\"");
                paramsBuf.append(",reportid:\""+rbean.getId()+"\"");
                paramsBuf.append(",updatetype:\""+updatetype+"\"}");
                rrequest.getWResponse().addOnloadMethod(afterSaveActionMethod,paramsBuf.toString(),true);
            }
            if(ersqlbean.getAfterSaveAction().length==2&&"true".equals(ersqlbean.getAfterSaveAction()[1])) return 1;
        }
        return -1;
    }
    
    public String getEditableMetaData(IEditableReportType editableReportTypeObj)
    {
        ReportBean rbean=((AbsReportType)editableReportTypeObj).getReportBean();
        StringBuffer resultBuf=new StringBuffer();
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(editableReportTypeObj))
        {
            resultBuf.append(" current_accessmode=\"").append(Consts.READONLY_MODE).append("\"");
        }else
        {
            resultBuf.append(" current_accessmode=\"").append(editableReportTypeObj.getRealAccessMode()).append("\"");
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
            if(ersqlbean.getBeforeSaveAction()!=null&&!ersqlbean.getBeforeSaveAction().trim().equals(""))
            {
                resultBuf.append(" beforeSaveAction=\"{method:").append(ersqlbean.getBeforeSaveAction()).append("}\"");
            }
            if(ersqlbean.getDeletebean()!=null)
            {
                ReportRequest rrequest=((AbsReportType)editableReportTypeObj).getReportRequest();
                String deleteconfirmmess=ersqlbean.getDeletebean().getDeleteConfirmMessage();
                if(deleteconfirmmess==null||deleteconfirmmess.trim().equals(""))
                {
                    deleteconfirmmess=Config.getInstance().getResourceString(null,null,"${delete.confirm.prompt}",true);
                }
                if(deleteconfirmmess!=null&&!deleteconfirmmess.trim().equals(""))
                {
                    resultBuf.append(" deleteconfirmmessage=\"").append(rrequest.getI18NStringValue(deleteconfirmmess)).append("\"");
                }
            }
        }
        return resultBuf.toString();
    }

    public int updateDBData(ReportBean rbean,ReportRequest rrequest,Connection conn,EditableReportUpdateDataBean updatebean) throws SQLException
    {
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<Map<String,String>> lstParamsValue=cdb.getLstEditedData(updatebean);
        List<Map<String,String>> lstExternalValue=cdb.getLstEditedExternalValues(updatebean);
        Map<String,String> mParamsValue;
        Map<String,String> mExternalValues;//存放要插入的记录对应的<external-values/>配置的所有参数值
        int rtnVal=Consts.RETURNVALUE_CONTINUE;
        outer:
        for(int i=0;i<lstParamsValue.size();i++)
        {
            mParamsValue=lstParamsValue.get(i);
            mExternalValues=null;
            if(lstExternalValue!=null) mExternalValues=lstExternalValue.get(i);
            if(rbean.getInterceptor()!=null)
            {
                rtnVal=rbean.getInterceptor().beforeSavePerRow(rrequest,rbean,mParamsValue,mExternalValues,updatebean.getEdittype());
                if(rtnVal==Consts.RETURNVALUE_TERMINAGE) break;
                if(rtnVal==Consts.RETURNVALUE_IGNORE) continue;
            }
            List<AbsEditSqlActionBean> lstSqlActions=updatebean.getLstSqlActionBeans();
            for(AbsEditSqlActionBean sqlActionBean:lstSqlActions)
            {
                if(rbean.getInterceptor()!=null)
                {
                    rtnVal=rbean.getInterceptor().beforeSavePerSql(rrequest,rbean,mParamsValue,mExternalValues,sqlActionBean.getSql());
                    if(rtnVal==Consts.RETURNVALUE_TERMINAGE) break outer;
                    if(rtnVal==Consts.RETURNVALUE_IGNORE) continue;
                }
                sqlActionBean.updateDBData(mParamsValue,mExternalValues,conn,rbean,rrequest);
                if(rbean.getInterceptor()!=null)
                {
                    rtnVal=rbean.getInterceptor().afterSavePerSql(rrequest,rbean,mParamsValue,mExternalValues,sqlActionBean.getSql());
                    if(rtnVal==Consts.RETURNVALUE_TERMINAGE) break outer;
                }
            }
            if(rbean.getInterceptor()!=null)
            {
                rtnVal=rbean.getInterceptor().afterSavePerRow(rrequest,rbean,mParamsValue,mExternalValues,updatebean.getEdittype());
                if(rtnVal==Consts.RETURNVALUE_TERMINAGE) break;
            }
        }
        return rtnVal;
    }

    public List<Map<String,String>> getExternalValues(EditableReportUpdateDataBean updatebean,List<Map<String,String>> lstColParamsValue,
            ReportBean rbean,ReportRequest rrequest)
    {
        if(lstColParamsValue==null||lstColParamsValue.size()==0||updatebean.getLstExternalValues()==null) return null;
        List<Map<String,String>> lstExternalValues=new ArrayList<Map<String,String>>();
        try
        {
            Map<String,String> mCustomizedValues=rrequest.getMCustomizeEditData(rbean);//用户传过来的自定义保存数据
            Map<String,String> mExternalValue;
            Connection conn=rrequest.getConnection(rbean.getSbean().getDatasource());
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            for(Map<String,String> mColParamsValue:lstColParamsValue)
            {//保存的每一条记录都要计算一次与它相应的所有在<external-values/>中定义的参数值。
                mExternalValue=new HashMap<String,String>();
                lstExternalValues.add(mExternalValue);
                for(EditableReportExternalValueBean valuebean:updatebean.getLstExternalValues())
                {
                    if(valuebean.getValue().equals("uuid{}"))
                    {
                        mExternalValue.put(valuebean.getName(),UUIDGenerator.generateID());
                    }else if(Tools.isDefineKey("url",valuebean.getValue())||Tools.isDefineKey("request",valuebean.getValue())
                            ||Tools.isDefineKey("session",valuebean.getValue()))
                    {
                        mExternalValue.put(valuebean.getName(),WabacusAssistant.getInstance()
                                .getRequestSessionValue(rrequest,valuebean.getValue(),""));
                    }else if(valuebean.getValue().equals("now{}"))
                    {
                        SimpleDateFormat sdf=new SimpleDateFormat(((AbsDateTimeType)valuebean.getTypeObj()).getDateformat());
                        mExternalValue.put(valuebean.getName(),sdf.format(new Date()));
                    }else if(Tools.isDefineKey("!",valuebean.getValue()))
                    {
                        String customizeParamName=Tools.getRealKeyByDefine("!",valuebean.getValue());
                        if(mCustomizedValues==null||!mCustomizedValues.containsKey(customizeParamName))
                        {
                            mExternalValue.put(valuebean.getName(),"");
                        }else
                        {
                            mExternalValue.put(valuebean.getName(),mCustomizedValues.get(customizeParamName));
                        }
                    }else if(Tools.isDefineKey("sequence",valuebean.getValue()))
                    {
                        String sql="select "+Tools.getRealKeyByDefine("sequence",valuebean.getValue())+".nextval from dual";
                        Statement stmt=conn.createStatement();
                        ResultSet rs=stmt.executeQuery(sql);
                        rs.next();
                        mExternalValue.put(valuebean.getName(),String.valueOf(rs.getInt(1)));
                        rs.close();
                        stmt.close();
                    }else if(valuebean.getValue().toLowerCase().trim().startsWith("select ")&&valuebean.getLstParamsBean()!=null)
                    {
                        PreparedStatement pstmtTemp=conn.prepareStatement(valuebean.getValue());
                        if(valuebean.getLstParamsBean().size()>0)
                        {
                            int j=1;
                            for(EditableReportParamBean paramBean:valuebean.getLstParamsBean())
                            {
                                String paramvalue=null;
                                if(paramBean.getOwner() instanceof EditableReportExternalValueBean)
                                {
                                    paramvalue=paramBean.getParamValue(mExternalValue.get(paramBean.getParamname()),rrequest,rbean);
                                }else
                                {
                                    if(mColParamsValue!=null)
                                    {
                                        paramvalue=paramBean.getParamValue(getColParamValue(rrequest,rbean,mColParamsValue,paramBean.getParamname()),
                                                rrequest,rbean);
                                    }
                                }
                                paramBean.getDataTypeObj().setPreparedStatementValue(j++,paramvalue,pstmtTemp,dbtype);
                            }
                        }
                        ResultSet rs=pstmtTemp.executeQuery();
                        if(rs.next())
                        {
                            mExternalValue.put(valuebean.getName(),valuebean.getTypeObj().value2label(
                                    valuebean.getTypeObj().getColumnValue(rs,1,dbtype)));
                        }else
                        {
                            mExternalValue.put(valuebean.getName(),"");
                        }
                        rs.close();
                        pstmtTemp.close();
                    }else
                    {//常量或#{reportid.insert.othervaluename}（即从别的报表中<external-value/>取出定义的数据）或@{reportid.insert.col}（即从别的报表中取某列数据）
                        mExternalValue.put(valuebean.getName(),valuebean.getValue());
                    }
                }
            }
        }catch(SQLException sqle)
        {
            throw new WabacusRuntimeException("获取报表"+rbean.getPath()+"配置的<external-values/>值失败",sqle);
        }
        return lstExternalValues;
    }
}
