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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.IConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportUpdateDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableListReportType extends UltraListReportType implements IEditableReportType
{
    public final static String KEY=EditableListReportType.class.getName();

    protected EditableListReportDisplayBean elrdbean=null;

    protected EditableReportSqlBean ersqlbean=null;

    protected String realAccessMode;

    public EditableListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
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

    public int[] doSaveAction(IConnection conn) throws SQLException
    {
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        int[] result=new int[]{0,0};
        if(sidbean==null||!sidbean.hasDeleteData()) return result;
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
            rtnVal=EditableReportAssistant.getInstance().updateDBData(rbean,rrequest,conn,ersqlbean.getDeletebean());
            if(rtnVal==IInterceptor.WX_TERMINATE) return new int[]{0,0};
        }
        if(rbean.getInterceptor()!=null)
        {
            rbean.getInterceptor().afterSave(rrequest,rbean);
        }
        result[1]=4;
        result[0]=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,"delete");
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
        return resultBuf.toString();
    }

    public String getColOriginalValue(Object object,ColBean cbean)
    {
        String oldvalue=ReportAssistant.getInstance().getPropertyValueAsString(object,cbean.getProperty()+"_old",cbean.getDatatypeObj());
        if(oldvalue==null||oldvalue.equals("null"))
        {
            oldvalue="";
        }
        return oldvalue;
    }
    
    protected Object initDisplayCol(CacheDataBean cdb,ColBean cbean,Object rowDataObjTmp,int startNum)
    {
        if(cbean.isSequenceCol()||cbean.isControlCol()) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrcbean==null) return super.initDisplayCol(cdb,cbean,rowDataObjTmp,startNum);
        String col_editvalue=getColOriginalValue(rowDataObjTmp,cbean);//得到此列的编辑数据
        return EditableReportColDataBean.createInstance(rrequest,cdb,null,cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String getTdPropertiesForCol(CacheDataBean cdb,ColBean cbean,Object colDataObj,int rowidx,boolean isCommonRowGroupCol)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getTdPropertiesForCol(cdb,cbean,colDataObj,rowidx,isCommonRowGroupCol);
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        StringBuffer resultBuf=new StringBuffer();
        String tdSuperProperties=super.getTdPropertiesForCol(cdb,cbean,ercdatabean.getEditvalue(),rowidx,isCommonRowGroupCol);
        resultBuf.append(tdSuperProperties);
        resultBuf.append(" oldvalue=\""+Tools.htmlEncode(ercdatabean.getOldvalue())+"\" ");
        resultBuf.append(" oldvalue_name=\""+ercdatabean.getValuename()+"\"");
        if(isCommonRowGroupCol)
        {//如果是普通分组的列，则必须显示id属性，以便它的数据行根据parentgroupid属性能找到此<td/>
            resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx+"\" ");
        }
        return resultBuf.toString();
    }
    
    protected String getColDisplayValue(ColBean cbean,Object dataObj,StringBuffer tdPropBuf,Object colDataObj,int startNum,int rowidx)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cbean.isEditableListEditCol())
        {
            String roweditcolvalue=cbean.getTagcontent();
            if(roweditcolvalue==null||roweditcolvalue.trim().equals(""))
            {//如果没有在<col></col>中定义此列的显示内容，则取默认显示内容
                roweditcolvalue=Config.getInstance().getResourceString(rrequest,this.rbean.getPageBean(),"${editablelist.editcol}",true);
            }
            roweditcolvalue=rrequest.getI18NStringValue(roweditcolvalue);
            EditableListReportUpdateDataBean elrudbean=(EditableListReportUpdateDataBean)ersqlbean.getUpdatebean();
            String col_displayvalue=null;
            if(rrequest.checkPermission(this.rbean.getId(),Consts.DATA_PART,Consts_Private.COL_EDITABLELIST_EDIT,Consts.PERMISSION_TYPE_DISABLED))
            {
                col_displayvalue="<span class='cls-editablelist-disabledcol'>"+roweditcolvalue+"</span>";
            }else
            {
                col_displayvalue="<a  onClick=\"wx_winpage('"+elrudbean.assembleAccessPageUrl(rrequest,this,dataObj)+"','',"+elrudbean.getPagewidth()
                        +","+elrudbean.getPageheight()+",";
                col_displayvalue+=elrudbean.isPagemaxbtn()+",";
                col_displayvalue+=elrudbean.isPageminbtn()+",";
                col_displayvalue+="closePopUpPageEvent);";
                if("max".equals(elrudbean.getPageinitsize()))
                {
                    col_displayvalue+="ymPrompt.max();";
                }else if("min".equals(elrudbean.getPageinitsize()))
                {
                    col_displayvalue+="ymPrompt.min();";
                }
                col_displayvalue+="\">";
                col_displayvalue=col_displayvalue+roweditcolvalue+"</a>";
            }
            return col_displayvalue;
        }
        return super.getColDisplayValue(cbean,dataObj,tdPropBuf,
                colDataObj instanceof EditableReportColDataBean?((EditableReportColDataBean)colDataObj).getEditvalue():colDataObj,startNum,rowidx);
    }
    
    public String getAddEvent()
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)||ersqlbean.getInsertbean()==null) return "";
        StringBuffer resultBuf=new StringBuffer();
        EditableListReportUpdateDataBean elrudbean=(EditableListReportUpdateDataBean)ersqlbean.getInsertbean();
        resultBuf.append("wx_winpage('").append(elrudbean.assembleAccessPageUrl(rrequest,this,null)).append("','',");
        resultBuf.append(elrudbean.getPagewidth()).append(",").append(elrudbean.getPageheight()).append(",");
        resultBuf.append(elrudbean.isPagemaxbtn()).append(",").append(elrudbean.isPageminbtn()).append(",");
        resultBuf.append("closePopUpPageEvent);");
        if("max".equals(elrudbean.getPageinitsize()))
        {
            resultBuf.append("ymPrompt.max();");
        }else if("min".equals(elrudbean.getPageinitsize()))
        {
            resultBuf.append("ymPrompt.min();");
        }
        return resultBuf.toString();
    }

    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            return true;
        }else if(buttonType instanceof DeleteButton)
        {
            if(ersqlbean.getDeletebean()==null) return false;
            return true;
        }
        return false;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        super.afterSqlLoading(sqlbean,lstEleSqlBeans);
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null)
        {
            ersqlbean=new EditableReportSqlBean(sqlbean);
            sqlbean.setExtendConfigDataForReportType(KEY,ersqlbean);
        }
        Map<String,String> mSqlProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleSqlBeans,new String[] { "transaction" });//组装所有<sql/>配置的这些属性
        String transactiontype=mSqlProperties.get("transaction");
        if(transactiontype!=null&&!transactiontype.trim().equals(""))
        {
            transactiontype=transactiontype.toLowerCase().trim();
            if(!Consts_Private.M_ALL_TRANSACTION_LEVELS.containsKey(transactiontype))
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，在其<sql/>中配置的transaction属性"+transactiontype+"不合法");
            }
            ersqlbean.setTransactionLever(transactiontype);
        }
        XmlElementBean eleInsertBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"insert");
        if(eleInsertBean!=null)
        {
            EditableListReportUpdateDataBean insertBean=new EditableListReportUpdateDataBean(ersqlbean,EditableReportUpdateDataBean.EDITTYPE_INSERT);
            ersqlbean.setInsertbean(insertBean);
            loadInsertUpdateConfig(sqlbean,eleInsertBean,insertBean);
        }
        XmlElementBean eleUpdateBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"update");
        if(eleUpdateBean!=null)
        {
            EditableListReportUpdateDataBean updateBean=new EditableListReportUpdateDataBean(ersqlbean,EditableReportUpdateDataBean.EDITTYPE_UPDATE);
            ersqlbean.setUpdatebean(updateBean);
            loadInsertUpdateConfig(sqlbean,eleUpdateBean,updateBean);
            if(updateBean.getLstUrlParams()==null||updateBean.getLstUrlParams().size()==0)
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，在<update/>中必须通过urlparams参数配置进入编辑报表的参数");
            }
        }
        ComponentConfigLoadManager.loadDeleteConfig(lstEleSqlBeans,ersqlbean);
        return 1;
    }

    private void loadInsertUpdateConfig(SqlBean sqlbean,XmlElementBean eleInsertUpdateBean,EditableListReportUpdateDataBean insertUpdateBean)
    {
        String pageurl=eleInsertUpdateBean.attributeValue("pageurl");
        if(pageurl==null||pageurl.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，这种报表类型必须为其<insert/>和<update/>指定pageurl属性");
        }
        pageurl=pageurl.trim();
        if(Tools.isDefineKey("report",pageurl))
        {//是指向另一个报表做为本报表的编辑报表
            String realpageurl=Tools.getRealKeyByDefine("report",pageurl);
            int idx=realpageurl.indexOf(".");
            if(idx<=0||idx==realpageurl.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，这种报表类型必须为其<insert/>和<update/>指定pageurl属性："+pageurl+"不合法");
            }
            insertUpdateBean.setPageid(realpageurl.substring(0,idx).trim());
            insertUpdateBean.setReportid(realpageurl.substring(idx+1).trim());
            if(insertUpdateBean.getPageid().equals("")||insertUpdateBean.getReportid().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，这种报表类型必须为其<insert/>和<update/>指定pageurl属性："+pageurl+"不合法");
            }
        }else
        {
            if(pageurl.startsWith(Config.webroot)&&!pageurl.toLowerCase().startsWith("http://"))
            {
                pageurl=Tools.replaceAll(Config.webroot+"/"+pageurl,"//","/");
            }
            insertUpdateBean.setPageurl(pageurl.trim());
        }
        String urlparams=eleInsertUpdateBean.attributeValue("urlparams");
        if(urlparams!=null&&!urlparams.trim().equals(""))
        {
            List<Map<String,String>> lstUrlParams=new ArrayList<Map<String,String>>();
            List<String> lstParams=Tools.parseStringToList(urlparams,";",false);
            for(String paramTmp:lstParams)
            {
                if(paramTmp==null||paramTmp.trim().equals("")) continue;
                paramTmp=paramTmp.trim();
                int idxEquals=paramTmp.indexOf("=");
                if(idxEquals<=0)
                {
                    throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，为其<insert/>和<update/>指定urlparams属性中的："
                            +paramTmp+"参数不合法，没有=号，或者没有参数名");
                }
                Map<String,String> mParamTmp=new HashMap<String,String>();
                mParamTmp.put(paramTmp.substring(0,idxEquals).trim(),paramTmp.substring(idxEquals+1).trim());
                lstUrlParams.add(mParamTmp);
            }
            insertUpdateBean.setLstUrlParams(lstUrlParams);
        }
        String pagewidth=eleInsertUpdateBean.attributeValue("pagewidth");
        if(pagewidth!=null&&!pagewidth.trim().equals(""))
        {
            insertUpdateBean.setPagewidth(Tools.getWidthHeightIntValue(pagewidth));
        }
        String pageheight=eleInsertUpdateBean.attributeValue("pageheight");
        if(pageheight!=null&&!pageheight.trim().equals(""))
        {
            insertUpdateBean.setPageheight(Tools.getWidthHeightIntValue(pageheight));
        }
        String pageinitsize=eleInsertUpdateBean.attributeValue("pageinitsize");
        if(pageinitsize!=null&&!pageinitsize.trim().equals(""))
        {
            insertUpdateBean.setPageinitsize(pageinitsize.toLowerCase().trim());
        }
        String pagemaxbtn=eleInsertUpdateBean.attributeValue("pagemaxbtn");
        if(pagemaxbtn!=null&&!pagemaxbtn.trim().equals(""))
        {
            insertUpdateBean.setPagemaxbtn(pagemaxbtn.toLowerCase().trim().equals("true"));
        }
        String pageminbtn=eleInsertUpdateBean.attributeValue("pageminbtn");
        if(pageminbtn!=null&&!pageminbtn.trim().equals(""))
        {
            insertUpdateBean.setPageminbtn(pageminbtn.toLowerCase().trim().equals("true"));
        }
    }

    public int doPostLoad(ReportBean reportbean)
    {
        SqlBean sqlbean=reportbean.getSbean();
        if(sqlbean==null) return 1;
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null) return 1;
        processRowEditCol(ersqlbean);
        super.doPostLoad(reportbean);
        ComponentConfigLoadManager.doEditableReportTypePostLoad(reportbean,KEY);
        processEditableButtons(ersqlbean);
        if(ersqlbean.getDeletebean()!=null)
        {
            AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrbean==null)
            {
                alrbean=new AbsListReportBean(reportbean);
                reportbean.setExtendConfigDataForReportType(AbsListReportType.KEY,alrbean);
            }
            if(alrbean.getRowSelectType()==null||alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_NONE))
            {
                alrbean.setRowSelectType(Consts.ROWSELECT_MULTIPLY);
            }
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
        if(ersqlbean.getDeletebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
    }
    
    protected void processRowEditCol(EditableReportSqlBean ersqlbean)
    {
        ReportBean reportbean=ersqlbean.getOwner().getReportBean();
        DisplayBean disbean=reportbean.getDbean();
        List<ColBean> lstCols=disbean.getLstCols();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(UltraListReportType.KEY);
        if(ersqlbean.getUpdatebean()==null)
        {//没有编辑功能，则删除掉所有编辑列
            for(int i=lstCols.size()-1;i>=0;i--)
            {
                if(lstCols.get(i).isEditableListEditCol())
                {
                    lstCols.remove(i);
                }
            }
            if(ulrdbean!=null)
            {
                ulrdbean.removeChildColBeanByColumn(Consts_Private.COL_EDITABLELIST_EDIT,true);
            }
        }else
        {
            boolean hasEditCol=false;
            for(ColBean cbTmp:lstCols)
            {
                if(cbTmp!=null&&cbTmp.isEditableListEditCol())
                {
                    hasEditCol=true;
                    break;
                }
            }
            if(!hasEditCol)
            {
                ColBean cbNewRowEditCol=new ColBean(disbean);
                cbNewRowEditCol.setColumn(Consts_Private.COL_EDITABLELIST_EDIT);
                cbNewRowEditCol.setProperty(Consts_Private.COL_EDITABLELIST_EDIT);
                AbsListReportColBean alrcbean=new AbsListReportColBean(cbNewRowEditCol);
                cbNewRowEditCol.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcbean);
                cbNewRowEditCol.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${editablelist.editcol.label}",false));
                cbNewRowEditCol.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
                cbNewRowEditCol.setLabelstyleproperty("style=\"text-align:center;vertical-align:middle;\"");
                cbNewRowEditCol.setValuestyleproperty("style=\"text-align:center;vertical-align:middle;\"");
                lstCols.add(cbNewRowEditCol);
                if(ulrdbean!=null&&ulrdbean.getLstChildren()!=null)
                {
                    UltraListReportColBean ulrcbean=new UltraListReportColBean(cbNewRowEditCol);
                    cbNewRowEditCol.setExtendConfigDataForReportType(UltraListReportType.KEY,ulrcbean);
                    ulrdbean.getLstChildren().add(cbNewRowEditCol);
                }
            }
        }
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_EDITABLELIST;
    }
}
