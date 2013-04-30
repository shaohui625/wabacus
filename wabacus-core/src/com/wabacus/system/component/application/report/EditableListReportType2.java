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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.SubmitFunctionParamBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.PasswordBox;
import com.wabacus.system.inputbox.SelectBox;
import com.wabacus.system.intercept.ColDataByInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableListReportType2 extends UltraListReportType implements IEditableReportType
{
    public final static String KEY=EditableListReportType2.class.getName();

    private final static Log log=LogFactory.getLog(EditableListReportType2.class);
    
    protected EditableListReportDisplayBean elrdbean=null;
    
    protected EditableReportSqlBean ersqlbean=null;
    
    private int display_rowcount;
    
    protected String realAccessMode;
    
    public EditableListReportType2(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            this.elrdbean=(EditableListReportDisplayBean)((ReportBean)comCfgBean).getDbean().getExtendConfigDataForReportType(KEY);
            this.ersqlbean=(EditableReportSqlBean)((ReportBean)comCfgBean).getSbean().getExtendConfigDataForReportType(KEY);
        }
    }

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

    public int[] doSaveAction(Connection conn) throws SQLException
    {
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        int[] result=new int[]{0,0};
        if(sidbean==null||(!sidbean.hasSavingData()&&!sidbean.hasDeleteData())) return result;
        boolean[] shouldDoSave=sidbean.getShouldDoSave();
        if(shouldDoSave.length!=4) return result;
        int rtnVal=Integer.MIN_VALUE;
        if(rbean.getInterceptor()!=null)
        {
            rtnVal=rbean.getInterceptor().beforeSave(rrequest,rbean);
            if(rtnVal==IInterceptor.WX_TERMINATE) return result;
        }
        if(rtnVal!=IInterceptor.WX_IGNORE)
        {//要完成框架的保存操作
            if(sidbean.hasSavingData())
            {
                if(shouldDoSave[0])
                {
                    rtnVal=EditableReportAssistant.getInstance().updateDBData(rbean,rrequest,conn,ersqlbean.getInsertbean());
                }
                if(shouldDoSave[1])
                {
                    rtnVal=EditableReportAssistant.getInstance().updateDBData(rbean,rrequest,conn,ersqlbean.getUpdatebean());
                }
            }else if(sidbean.hasDeleteData())
            {
                rtnVal=EditableReportAssistant.getInstance().updateDBData(rbean,rrequest,conn,ersqlbean.getDeletebean());
            }
            if(rtnVal==IInterceptor.WX_TERMINATE) return new int[]{0,0};
        }
        if(sidbean.hasSavingData())
        {
            if(shouldDoSave[0]&&shouldDoSave[1])
            {
                result[1]=3;
            }else if(shouldDoSave[0])
            {//只有添加数据需要保存
                result[1]=1;
            }else if(shouldDoSave[1])
            {
                result[1]=2;
            }else
            {
                result[1]=3;
            }
        }else if(sidbean.hasDeleteData())
        {
            result[1]=4;
        }
        if(rbean.getInterceptor()!=null)
        {
            rbean.getInterceptor().afterSave(rrequest,rbean);
        }
        result[0]=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,sidbean.hasDeleteData()?"delete":"");
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
        {//新的模式是readonly
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode","true");
        }else
        {
            rrequest.getAttributes().remove(rbean.getId()+"_isReadonlyAccessmode");
        }
    }

    protected boolean isLazyDataLoad()
    {
        boolean isLazyLoad=super.isLazyDataLoad();
        int[] maxminrownums=getMaxMinRownums();
        if(maxminrownums[1]>0)
        {
            if(isLazyLoad)
            {
                log.warn("报表"+this.rbean.getPath()+"配置了最记录数，因此将它指定为延迟加载无效");
            }
            return false;
        }
        return isLazyLoad;
    }
    
    private boolean hasLoadedData=false;
    
    protected void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        super.setHasLoadedDataFlag(hasLoadedDataFlag);
        this.hasLoadedData=hasLoadedDataFlag;
    }
    
    public void loadReportData()
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        super.loadReportData();
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            return;
        }
        if(lstReportData==null) lstReportData=new ArrayList();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        int rowcount=cdb.getRecordcount();
        int[] maxminrownums=getMaxMinRownums();//获取到指定的最大小记录数
        int maxrownum=maxminrownums[0];
        int minrownum=maxminrownums[1];
        if(maxrownum>0&&maxrownum<cdb.getRecordcount())
        {
            rowcount=maxrownum;
            if(cdb.getPagesize()>0)
            {
                if(cdb.getRefreshNavigateInfoType()<=0)
                {
                    cdb.setRecordcount(rowcount);
                    cdb.setPagecount(ReportAssistant.getInstance().calPageCount(cdb.getPagesize(),rowcount));
                }
            }else
            {
                cdb.setPagecount(1);
            }
        }else if(minrownum>0&&minrownum>cdb.getRecordcount())
        {//如果配置了最小记录数，且大于总记录数，则用最小记录数来更新页码信息
            rowcount=minrownum;
            if(cdb.getPagesize()>0)
            {
                if(cdb.getRefreshNavigateInfoType()<=0)
                {
                    cdb.setPagecount(ReportAssistant.getInstance().calPageCount(cdb.getPagesize(),rowcount));
                    
                }
            }else
            {
                cdb.setPagecount(1);
            }
        }

        if(cdb.getPagesize()<0)
        {//不分页
            display_rowcount=rowcount;
        }else
        {
            if(cdb.getFinalPageno()*cdb.getPagesize()>rowcount)
            {
                display_rowcount=rowcount-(cdb.getFinalPageno()-1)*cdb.getPagesize();
            }else
            {
                display_rowcount=cdb.getPagesize();
            }
        }
    }

    private int[] getMaxMinRownums()
    {
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        int[] rownums=new int[2];
        rownums[0]=elrdbean.getMaxrownum();
        rownums[1]=elrdbean.getMinrownum();
        boolean hasDynMaxminrownums=false;
        String maxrownum=(String)cdb.getAttributes().get("maxrownum");
        if(maxrownum!=null&&!maxrownum.trim().equals(""))
        {
            rownums[0]=Integer.parseInt(maxrownum.trim());
            hasDynMaxminrownums=true;
        }
        String minrownum=(String)cdb.getAttributes().get("minrownum");
        if(minrownum!=null&&!minrownum.trim().equals(""))
        {
            rownums[1]=Integer.parseInt(minrownum.trim());
            hasDynMaxminrownums=true;
        }
        if(hasDynMaxminrownums) validateAndCalMaxMinRownum(ersqlbean,rownums);
        return rownums;
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        if(!this.realAccessMode.equals(Consts.READONLY_MODE))
        {//不是只读访问模式
            resultBuf.append(ersqlbean.getValidateSaveMethodAndParams(rrequest,false));
            resultBuf.append(createColInfosForAddRow());
        }
        return resultBuf.toString();
    }

    private String createColInfosForAddRow()
    {
        if(ersqlbean==null||ersqlbean.getInsertbean()==null) return "";
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        EditableReportColBean ercbeanTmp;
        AbsInputBox boxObjTmp;
        StringBuffer colsBuf=new StringBuffer();
        colsBuf.append("{");
        int currentTotalRowcount=cdb.getRecordcount();
        int[] maxminrownums=getMaxMinRownums();
        int maxrownum=maxminrownums[0];
        int minrownum=maxminrownums[1];
        if(maxrownum>0&&minrownum>0&&minrownum>currentTotalRowcount)
        {
            /*
             * 计算本页显示了多少条添加新行的记录，方便当用户在客户端添加新记录行时，判断是否已经超出了最大记录数
             */
            if(cdb.getPagesize()>0)
            {
                /***********************************************************
                 * 计算到本页为止，应该显示的总记录行数，包括已有记录的行和添加新记录的行
                 */
                int tmp=cdb.getFinalPageno()*cdb.getPagesize();
                if(tmp>minrownum) tmp=minrownum;//本页是最后一页，且不用显示pagesize条记录。
                /***********************************************************/

                tmp=tmp-cdb.getRecordcount();
                if(tmp>0)
                {
                    if(tmp>=cdb.getPagesize())
                    {
                        if(cdb.getFinalPageno()<cdb.getPagecount())
                        {
                            tmp=cdb.getPagesize();
                        }else
                        {//最后一页的话，计算出此页实际显示的添加新记录的行数
                            tmp=minrownum-((cdb.getFinalPageno()-1)*cdb.getPagesize());
                        }
                    }
                    
                    currentTotalRowcount=currentTotalRowcount+tmp;
                }
                
            }else
            {//不分页显示的报表
                currentTotalRowcount=minrownum;
            }
        }

        colsBuf.append("currentRecordCount:").append(currentTotalRowcount);
        colsBuf.append(",maxRecordCount:").append(maxrownum);
        colsBuf.append(",cols:[");
        int displaymodeTmp;
        for(ColBean cbean:lstColBeans)
        {
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            colsBuf.append("{");
            displaymodeTmp=cdb.getColDisplayModeAfterAuthorize(cbean);
            if(displaymodeTmp<=0)
            {
                colsBuf.append("hidden:\"true\",");
            }
            String wrapstart=this.getColDisplayValueWrapStart(cbean,true);
            if(!wrapstart.trim().equals(""))
            {
                wrapstart=wrapstart+"&gt;";
                colsBuf.append("colWrapStart:\"").append(wrapstart).append("\",");
                colsBuf.append("colWrapEnd:\"").append(this.getColDisplayValueWrapEnd(cbean,true)).append("\",");
            }
            if(cbean.isRowSelectCol())
            {
                if(Consts.ROWSELECT_RADIOBOX.equalsIgnoreCase(alrbean.getRowSelectType()))
                {
                    colsBuf.append("coltype:\"ROWSELECTED-RADIOBOX\"");
                }else
                {
                    colsBuf.append("coltype:\"ROWSELECTED-CHECKBOX\"");
                }
                colsBuf.append("},");
                continue;
            }else if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")||cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isRoworderCol())
            {
                colsBuf.append("coltype:\"EMPTY\"},");
                continue;
            }
            colsBuf.append("boxid:\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("\"");
            colsBuf.append(",value_name:\"");
            boxObjTmp=null;
            String defaultvalue=null;
            String defaultvaluelabel=null;
            if(ercbeanTmp!=null&&ercbeanTmp.getInputbox()!=null)
            {
                boxObjTmp=ercbeanTmp.getInputbox();
                defaultvalue=boxObjTmp.getDefaultvalue(rrequest);
                defaultvaluelabel=boxObjTmp.getDefaultlabel(rrequest);
            }
            if(displaymodeTmp<0)
            {//当前列没有显示权限，加上前缀标识
                colsBuf.append(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX);
            }else if(boxObjTmp instanceof PasswordBox&&defaultvalue!=null&&!defaultvalue.trim().equals(""))
            {
                if(((PasswordBox)boxObjTmp).getEncodelength()>0)
                {
                    colsBuf.append(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX);
                    String encodevalue=this.currentSecretColValuesBean.getUniqueEncodeString(((PasswordBox)boxObjTmp).getEncodelength());
                    this.currentSecretColValuesBean.addParamValue(encodevalue,defaultvalue);
                    defaultvalue=encodevalue;
                    defaultvaluelabel=encodevalue;
                }
            }
            colsBuf.append(EditableReportAssistant.getInstance().getColParamName(cbean)).append("\"");
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype()))
            {
                colsBuf.append("},");
                continue;
            }
            if(!ercbeanTmp.isEditableForInsert())
            {
                colsBuf.append(",coltype:\"NONE-EDITABLE\"},");
                continue;
            }
            if(defaultvalue!=null)
            {//输入框配置了默认值
                colsBuf.append(",defaultvalue:\"").append(defaultvalue).append("\"");
                colsBuf.append(",defaultvaluelabel:\"").append(defaultvaluelabel).append("\"");
            }
            if(cbean.checkReadonlyPermission(rrequest))
            {
                colsBuf.append(",coltype:\"NONE-EDITABLE\"},");
                continue;
            }
            colsBuf.append(",coltype:\"EDITABLE\"");
            if(displaymodeTmp>0)
            {
                colsBuf.append(",boxtype:\"").append(boxObjTmp.getTypename()).append("\"");
                colsBuf.append(",fillmode:\"").append(boxObjTmp.getFillmode()).append("\"");
                if(boxObjTmp instanceof SelectBox)
                {
                    colsBuf.append(",hasParent:\"").append(
                            ((SelectBox)boxObjTmp).getMParentids()!=null&&((SelectBox)boxObjTmp).getMParentids().size()>0).append("\"");
                }
                colsBuf.append(",hasChild:\"").append(boxObjTmp.getLstChildids()!=null&&boxObjTmp.getLstChildids().size()>0).append("\"");
                colsBuf.append(",attachinfo:\"").append(boxObjTmp.attachInfoForDisplay()).append("\"");
            }
            colsBuf.append("},");
        }
        if(colsBuf.length()>0&&colsBuf.charAt(colsBuf.length()-1)==',')
        {
            colsBuf.deleteCharAt(colsBuf.length()-1);
        }
        colsBuf.append("]}");
        return " newRowCols=\""+Tools.jsParamEncode(colsBuf.toString())+"\"";
    }

    protected boolean isFixedLayoutTable()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return false;
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return super.isFixedLayoutTable();
        return true;
    }

    protected String showMetaDataContentDisplayString()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataContentDisplayString());
        resultBuf.append(initInputBox());
        return resultBuf.toString();
    }

    protected String initInputBox()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(ersqlbean.getInsertbean()==null&&ersqlbean.getUpdatebean()==null) return "";
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return "";//只读访问模式
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        StringBuffer resultBuf=new StringBuffer();
        EditableReportColBean ercbeanTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(cdb.getColDisplayModeAfterAuthorize(cbean)<=0) continue;
            if(cbean.isSequenceCol()||cbean.isControlCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercbeanTmp==null) continue;
            if(ercbeanTmp.isEditableForInsert()||ercbeanTmp.isEditableForUpdate())
            {
                resultBuf.append(ercbeanTmp.getInputbox().initDisplay(rrequest));
            }
        }
        return resultBuf.toString();
    }
    
    protected int[] getDisplayRowInfo()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||this.realAccessMode.equals(Consts.READONLY_MODE))
        {
            return super.getDisplayRowInfo();
        }
        return new int[]{0,this.display_rowcount};
    }

    protected String getDataTdClassName()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder())) 
        {
            return super.getDataTdClassName();
        }
        return "cls-data-td-editlist";
    }

    protected Object initDisplayCol(CacheDataBean cdb,ColBean cbean,Object rowDataObjTmp,int startNum)
    {
        if(cbean.isSequenceCol()||cbean.isControlCol()) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrcbean==null) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbean==null) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        String col_editvalue=null;
        if(ercbean.getUpdateCbean()==null)
        {
            col_editvalue=getColOriginalValue(rowDataObjTmp,cbean);
        }else
        {
            col_editvalue=getColOriginalValue(rowDataObjTmp,ercbean.getUpdateCbean());
        }
        return EditableReportColDataBean.createInstance(rrequest,cdb,ersqlbean.getUpdatebean(),cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String getTdPropertiesForCol(CacheDataBean cdb,ColBean cbean,Object colDataObj,int rowidx,boolean isCommonRowGroupCol)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getTdPropertiesForCol(cdb,cbean,colDataObj,rowidx,isCommonRowGroupCol);
        StringBuffer resultBuf=new StringBuffer();
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        String tdSuperProperties=super.getTdPropertiesForCol(cdb,cbean,ercdatabean.getEditvalue(),rowidx,isCommonRowGroupCol);
        resultBuf.append(tdSuperProperties);
        resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx+"\" ");//有输入框的为此<td/>设置一id，以便客户端校验时用上。
        resultBuf.append(" oldvalue=\""+Tools.htmlEncode(ercdatabean.getOldvalue())+"\" ");
        resultBuf.append(" oldvalue_name=\""+ercdatabean.getValuename());
        if(!ercdatabean.isEditable())
        {//当前列不可编辑，即不需要显示输入框
            resultBuf.append("\"");
        }else
        {
            resultBuf.append("_old\" value_name=\""+ercdatabean.getValuename()+"\"");
            if(tdSuperProperties.indexOf(" value=")>=0&&ercdatabean.isNeedEncode())
            {
                throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"的列"+cbean.getColumn()+"失败，此列不在前台显示明文数据，不能在客户端使用它");
            }
            if(tdSuperProperties.indexOf(" value=")<0)
            {
                resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean.getValue())).append("\"");
            }
        }
        return resultBuf.toString();
    }
   
    protected String showDataRowInAddMode(List<ColBean> lstColBeans,int startNum,int rowidx)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||this.realAccessMode.equals(Consts.READONLY_MODE))
        {
            return super.showDataRowInAddMode(lstColBeans,startNum,rowidx);
        }
        StringBuffer resultBuf=new StringBuffer();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        EditableReportColDataBean ercdatabeanTmp;
        String col_displayvalue;
        ColDataByInterceptor coldataByInterceptor;
        resultBuf.append("<tbody>");//对于新增的行，外面套一层<tbody/>，删除行是需要用上
        resultBuf.append(showDataRowTrStart(rowidx));//显示<tr及其属性
        resultBuf.append(" EDIT_TYPE=\"add\">");
        int colDisplayModeTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue; 
            resultBuf.append("<td class='"+getDataTdClassName()+"'");
            colDisplayModeTmp=cdb.getColDisplayModeAfterAuthorize(cbean);
            if(colDisplayModeTmp<0)
            {
                resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx).append("\"");
                resultBuf.append(" value_name=\"").append(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX).append(
                        EditableReportAssistant.getInstance().getColParamName(cbean)).append("\"");
                resultBuf.append(" style=\"display:none;\"></td>");
                continue;
            }
            ercdatabeanTmp=EditableReportColDataBean.createInstance(rrequest,cdb,ersqlbean.getInsertbean(),cbean,"",this.currentSecretColValuesBean);
            if(!ercdatabeanTmp.isEditable())
            {
                if(colDisplayModeTmp==0)
                {//当前列不参与本次显示
                    resultBuf.append(" style=\"display:none;\"></td>");
                    continue;
                }
                if(cbean.isSequenceCol()||cbean.isRowSelectCol())
                {
                    col_displayvalue=super.getColDisplayValue(cbean,null,null,null,startNum,rowidx);
                }else
                {
                    col_displayvalue="&nbsp;";
                }
            }else
            {
                resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx+"\" ");
                resultBuf.append(" value_name=\"").append(ercdatabeanTmp.getValuename()).append("\"");
                resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabeanTmp.getValue())).append("\"");
                if(colDisplayModeTmp>0)
                {
                    col_displayvalue=showInputBoxForCol(resultBuf,(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY),rowidx,null,
                            cbean,ercdatabeanTmp);
                }else
                {
                    resultBuf.append(" style=\"display:none;\"></td>");
                    continue;
                }
            }
            coldataByInterceptor=ReportAssistant.getInstance().getColDataFromInterceptor(rrequest,this,cbean,rowidx,col_displayvalue);
            if(coldataByInterceptor!=null&&coldataByInterceptor.getDynvalue()!=null)
            {
                col_displayvalue=coldataByInterceptor.getDynvalue();
            }
            resultBuf.append(" ").append(getColGroupStyleproperty(cbean.getValuestyleproperty(rrequest),coldataByInterceptor));
            resultBuf.append(">").append(getColDisplayValueWithWrap(cbean,col_displayvalue));
            resultBuf.append("</td>");
        }
        resultBuf.append("</tr></tbody>");
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
    
    protected String getColDisplayValue(ColBean cbean,Object dataObj,StringBuffer tdPropBuf,Object colDataObj,int startNum,int rowidx)
    {
        if(!(colDataObj instanceof EditableReportColDataBean))
        {
            return super.getColDisplayValue(cbean,dataObj,tdPropBuf,colDataObj,startNum,rowidx);
        }
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||!ercdatabean.isEditable()||this.realAccessMode.equals(Consts.READONLY_MODE))
        {//如果对整个数据部分授权为只读
            return super.getColDisplayValue(cbean,dataObj,tdPropBuf,ercdatabean.getEditvalue(),startNum,rowidx);
        }
        return showInputBoxForCol(tdPropBuf,(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY),rowidx,dataObj,cbean,ercdatabean);
    }

    protected String showInputBoxForCol(StringBuffer tdPropBuf,EditableReportColBean ercbean,int rowindex,Object object,ColBean cbean,
            EditableReportColDataBean ercdatabean)
    {
        String strvalue;
        boolean isReadonlyPermission=cbean.checkReadonlyPermission(rrequest);
        if(ercbean.getInputbox().getFillmode()==2)
        {
            if(!isReadonlyPermission)
            {
                tdPropBuf.append(" onclick=\"try{fillInputBoxOnClick(event,'"+rbean.getPageBean().getId()+"','");
                tdPropBuf.append(rbean.getGuid()).append("','"+this.getReportFamily()+"','");
                tdPropBuf.append(ercbean.getInputbox().getTypename()).append(
                        "','"+ercbean.getInputBoxId()+"__"+rowindex+"');}catch(e){logErrorsAsJsFileLoad(e);}\"");
                tdPropBuf.append(" attachinfo=\""+ercbean.getInputbox().attachInfoForDisplay()+"\" ");
            }
            
            if(ercdatabean.isNeedDefaultValue())
            {
                strvalue=ercdatabean.getDefaultvaluelabel();
            }else
            {
                strvalue=cbean.getDisplayValue(object,rrequest);
                if(strvalue==null||strvalue.equals("null")) strvalue="";
            }
        }else
        {//显示时就直接填充
            rrequest.getAttributes().put("DYN_INPUTBOX_ID",ercbean.getInputBoxId()+"__"+rowindex);
            strvalue=ercbean.getInputbox().getDisplayStringValue(
                    rrequest,ercdatabean.getValue(),
                    "style=\"text-align:"+ercbean.getTextalign()+";\" onblur=\"try{fillInputBoxValueToParentTd(this,'"
                            +ercbean.getInputbox().getTypename()+"','"+rbean.getGuid()+"','"+this.getReportFamily()
                            +"',1);}catch(e){logErrorsAsJsFileLoad(e);}\"",isReadonlyPermission);
            rrequest.getAttributes().remove("DYN_INPUTBOX_ID");
        }
        return strvalue;
    }

    protected String getColDisplayValueWrapStart(ColBean cbean,boolean isInProperty)
    {
        String supervalue=super.getColDisplayValueWrapStart(cbean,isInProperty);
        if(supervalue.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(supervalue).append(" tagtype='COL_CONTENT_WRAP'");
        return resultBuf.toString();
    }

    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            int[] maxminrownums=getMaxMinRownums();
            if(maxminrownums[0]>0&&maxminrownums[0]==maxminrownums[1]) return false;//如果是固定记录行数，则不显示添加按钮
            return true;
        }else if(buttonType instanceof DeleteButton)
        {
            if(ersqlbean.getDeletebean()==null) return false;
            return true;
        }else if(buttonType instanceof SaveButton)
        {
            if(ersqlbean.getUpdatebean()!=null||ersqlbean.getInsertbean()!=null) return true;
        }
        return false;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        ComponentConfigLoadManager.loadEditableColConfig(colbean,lstEleColBeans.get(0),KEY);
        return 1;
    }

    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.afterDisplayLoading(disbean,lstEleDisplayBeans);
        EditableListReportDisplayBean elrdbean=(EditableListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(elrdbean==null)
        {
            elrdbean=new EditableListReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,elrdbean);
        }
        Map<String,String> mJoinedAttributes=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "maxrownum", "minrownum" });
        String maxrownum=mJoinedAttributes.get("maxrownum");
        if(maxrownum!=null)
        {
            if(maxrownum.trim().equals(""))
            {
                elrdbean.setMaxrownum(-1);
            }else
            {
                elrdbean.setMaxrownum(Integer.parseInt(maxrownum.trim()));
            }
        }
        String minrownum=mJoinedAttributes.get("minrownum");
        if(minrownum!=null)
        {
            if(minrownum.trim().equals(""))
            {
                elrdbean.setMinrownum(0);
            }else
            {
                elrdbean.setMinrownum(Integer.parseInt(minrownum.trim()));
            }
        }
        return 1;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        super.afterSqlLoading(sqlbean,lstEleSqlBeans);
        ComponentConfigLoadManager.loadEditableSqlConfig(sqlbean,lstEleSqlBeans,KEY);
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
        processEditableButtons(ersqlbean);
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(ersqlbean.getInsertbean()!=null||ersqlbean.getDeletebean()!=null)
        {
            if(alrbean.getRowSelectType()==null||alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_NONE))
            {
                alrbean.setRowSelectType(Consts.ROWSELECT_MULTIPLY);
            }
        }
        if((ersqlbean.getInsertbean()!=null||ersqlbean.getUpdatebean()!=null)&&(alrbean.getFixedrows()>0||alrbean.getFixedrows()>0))
        {//如果配置了<insert/>或<update/>，且配置为冻结行或列标题
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()
                    +"失败，editablelist2/listform报表类型如果配置了编辑功能时，不允许冻结其行或列标题，可以考虑采用editablelist报表类型进行编辑");
        }
        /*
         * 校验在<display/>中配置的maxrownum/minrownum的合法性
         */
        EditableListReportDisplayBean elrdbean=(EditableListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        int[] rownums=new int[2];
        rownums[0]=elrdbean.getMaxrownum();
        rownums[1]=elrdbean.getMinrownum();
        validateAndCalMaxMinRownum(ersqlbean,rownums);
        elrdbean.setMaxrownum(rownums[0]);
        elrdbean.setMinrownum(rownums[1]);

        List<ColBean> lstColBeans=reportbean.getDbean().getLstCols();
        //        EditableReportParamBean paramBean;
        
        EditableReportColBean ercolbean;
        StringBuffer validateScriptBuffer=new StringBuffer();
        List<SubmitFunctionParamBean> lstValidateParams=new ArrayList<SubmitFunctionParamBean>();
        for(ColBean cbean:lstColBeans)
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue;
            String align=Tools.getPropertyValueByName("align",cbean.getValuestyleproperty(),true);
            if(align==null||align.trim().equals("")) align="left";
            ercolbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercolbean==null)
            {
                ercolbean=new EditableReportColBean(cbean);
                cbean.setExtendConfigDataForReportType(KEY,ercolbean);
            }
            ercolbean.setTextalign(align);
            if(ercolbean.isEditableForUpdate()||ercolbean.isEditableForInsert())
            {
                JavaScriptAssistant.getInstance().writeEditableReportColValidateJs(ercolbean,validateScriptBuffer,lstValidateParams);
            }
        }

        // elrdbean.setDefaultInfoForAddRow(createColInfosForAddRow(sqlbean,lstColBeans,null));//将一行记录中，每列是否要显示输入框，以及显示输入框的类型记录在一容器对象中 以便新增记录时 能根据它们为每个<td/>显示合适的数据框，并填充上合适的数据

        if(validateScriptBuffer.length()>0)
        {
            writeValidateJsToFile(reportbean,validateScriptBuffer.toString());
            ersqlbean.setValidateSaveUpdateMethod(reportbean.getGuid()+"_validateSave");
            ersqlbean.setLstValidateSavingUpdateDynParams(lstValidateParams);
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
        if(ersqlbean.getDeletebean()!=null||ersqlbean.getInsertbean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
        if(ersqlbean.getUpdatebean()!=null||ersqlbean.getInsertbean()!=null)
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
        resultBuf.append("function "+reportbean.getGuid()+"_validateSave(metadataObj){");//参数为此报表取到的元数据对象
        resultBuf.append("if(WX_UPDATE_ALLDATA==null) return true;");
        resultBuf.append("var updatedataForSaving=WX_UPDATE_ALLDATA['"+reportbean.getGuid()+"'];");
        resultBuf.append("if(updatedataForSaving==null||updatedataForSaving.length==0) return true;");
        resultBuf.append("var paramsObj=getObjectByJsonString(metadataObj.metaDataSpanObj.getAttribute('validateSaveMethodDynParams'));");
        resultBuf.append("var trObj;for(var i=0;i<updatedataForSaving.length;i=i+1){");
        resultBuf.append("trObj=updatedataForSaving[i];if(trObj==null||!hasEditDataForSavingRow(trObj)) continue;");
        resultBuf.append("var tdChilds=trObj.getElementsByTagName('TD');if(tdChilds==null||tdChilds.length==0) continue;");
        resultBuf.append("var value_name;var boxValue;");
        resultBuf.append("var boxObj;");//在这种报表类型中，传入客户端校验函数的输入框对象不是真正的输入框对象，而是其所在的<td/>对象
        resultBuf.append("for(var j=0;j<tdChilds.length;j=j+1){if(tdChilds[j]==null) continue;");
        resultBuf.append("boxObj=tdChilds[j];");
        resultBuf.append("value_name=boxObj.getAttribute('value_name');if(value_name==null||value_name=='') continue;");
        resultBuf.append("boxValue=getEditable2ColValueByParentTd(boxObj);if(boxValue==null) boxValue='';");
        //        resultBuf.append("var idxTemp=id.lastIndexOf('__td');if(idxTemp>0) id=id.substring(0,idxTemp);");//<td/>的id结构为输入框id+"__td"+行号
        resultBuf.append(script);
        resultBuf.append(" } } return true;} ");
        JavaScriptAssistant.getInstance().writeJsMethodToJsFiles(reportbean.getPageBean(),resultBuf.toString());
    }

    private void validateAndCalMaxMinRownum(EditableReportSqlBean ersbean,int[] rownums)
    {
        ReportBean reportbean=ersbean.getOwner().getReportBean();
        int maxrownum=rownums[0];
        int minrownum=rownums[1];
        if(maxrownum==0)
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"的maxrownum不能指定为0");
        }
        if(maxrownum<-1)
        {
            log.warn("报表"+reportbean.getPath()+"maxrownum："+maxrownum+"不合法，将用默认值-1");
            maxrownum=-1;
        }
        if(minrownum<0)
        {
            log.warn("报表"+reportbean.getPath()+"的minrownum指定为小于0的数不合法，将用默认值0");
            minrownum=0;
        }
        if(ersbean.getInsertbean()==null&&minrownum!=0)
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"没有配置添加记录功能，因此不能指定minrownum为不为0的数");
        }
        if(maxrownum>0&&maxrownum<minrownum)
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"的maxrownum属性指定值必须大于等于minrownum属性值");
        }
        rownums[0]=maxrownum;
        rownums[1]=minrownum;
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_EDITABLELIST2;
    }
}
