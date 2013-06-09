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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.system.IConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;
import com.wabacus.util.UUIDGenerator;

public abstract class AbsEditSqlActionBean
{
    private static Log log=LogFactory.getLog(AbsEditSqlActionBean.class);

    protected String sql;

    protected List<EditableReportParamBean> lstParamBeans;

    protected String returnValueParamname;//用于保存此SQL语句或存储过程返回值的变量名，可以是定义在<external-values/>的变量的name属性或rrequset的key
    
    protected EditableReportUpdateDataBean owner;

    public AbsEditSqlActionBean(EditableReportUpdateDataBean _owner)
    {
        this.owner=_owner;
    }

    public EditableReportUpdateDataBean getOwner()
    {
        return owner;
    }

    public void setOwner(EditableReportUpdateDataBean owner)
    {
        this.owner=owner;
    }

    public String getSql()
    {
        return sql;
    }

    public void setSql(String sql)
    {
        this.sql=sql;
    }

    public List<EditableReportParamBean> getLstParamBeans()
    {
        return lstParamBeans;
    }

    public void setLstParamBeans(List<EditableReportParamBean> lstParamBeans)
    {
        this.lstParamBeans=lstParamBeans;
    }

    public String getReturnValueParamname()
    {
        return returnValueParamname;
    }

    public void setReturnValueParamname(String returnValueParamname)
    {
        this.returnValueParamname=returnValueParamname;
    }

    public String parseAndRemoveReturnParamname(String configsql)
    {
        if(configsql==null||configsql.trim().equals("")) return configsql;
        int idx=configsql.indexOf("=");
        if(idx<0) return configsql;
        String returnValName=configsql.substring(0,idx).trim();
        if(Tools.isDefineKey("#",returnValName)||Tools.isDefineKey("rrequest",returnValName))
        {
            this.returnValueParamname=returnValName;
            configsql=configsql.substring(idx+1);
        }
        return configsql;
    }

    public String parseUpdateWhereClause(SqlBean sqlbean,String reportKey,List<EditableReportParamBean> lstParamsBean,String whereclause)
    {
        List<EditableReportParamBean> lstDynParamsInWhereClause=new ArrayList<EditableReportParamBean>();
        whereclause=owner.parseUpdateWhereClause(sqlbean.getReportBean(),reportKey,lstDynParamsInWhereClause,whereclause);
        for(EditableReportParamBean paramBean:lstDynParamsInWhereClause)
        {
            if(paramBean.getOwner() instanceof ColBean)
            {
                addColParamBeanInWhereClause(paramBean,reportKey);
            }
            if(lstParamsBean!=null)
            {
                lstParamsBean.add(paramBean);
            }
        }
        return whereclause;
    }

    protected void addColParamBeanInWhereClause(EditableReportParamBean paramBean,String reportTypeKey)
    {
        if(paramBean==null||!(paramBean.getOwner() instanceof ColBean)) return;
        ColBean cbOwner=(ColBean)paramBean.getOwner();
        ColBean cbSrc=cbOwner;
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbOwner.getDisplaytype()))
        {
            EditableReportColBean ercbeanTmp=(EditableReportColBean)cbOwner.getExtendConfigDataForReportType(reportTypeKey);
            if(ercbeanTmp!=null&&ercbeanTmp.getUpdatedcol()!=null&&!ercbeanTmp.getUpdatedcol().trim().equals(""))
            {
                cbSrc=cbOwner.getReportBean().getDbean().getColBeanByColProperty(ercbeanTmp.getUpdatedcol());//取到引用此<col/>的<col/>对象
            }
        }
        owner.addParamBeanInWhereClause(cbSrc,paramBean);
    }
    
    public EditableReportParamBean createEditParamBeanByColbean(ColBean cbean,String reportKey,boolean enableHiddenCol,boolean isMust)
    {
        EditableReportParamBean paramBean=owner.createParamBeanByColbean(cbean,reportKey,enableHiddenCol,isMust);
        if(paramBean==null) return null;
        addColParamBeanInUpdateClause(paramBean,reportKey);
        return paramBean;
    }

    protected void addColParamBeanInUpdateClause(EditableReportParamBean paramBean,String reportTypeKey)
    {
        if(paramBean==null) return;
        if(!(paramBean.getOwner() instanceof ColBean)) return;
        ColBean cbOwner=(ColBean)paramBean.getOwner();
        ColBean cbSrc=cbOwner;
        EditableReportColBean ercbean=(EditableReportColBean)cbOwner.getExtendConfigDataForReportType(reportTypeKey);
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbOwner.getDisplaytype()))
        {
            if(ercbean!=null&&ercbean.getUpdatedcol()!=null&&!ercbean.getUpdatedcol().trim().equals(""))
            {//被别的<col/>通过updatecol属性引用到
                cbSrc=cbOwner.getReportBean().getDbean().getColBeanByColProperty(ercbean.getUpdatedcol());//取到引用此<col/>的<col/>对象
                EditableReportColBean ercbeanSrc=(EditableReportColBean)cbSrc.getExtendConfigDataForReportType(reportTypeKey);
                if(ercbeanSrc.getInputbox()!=null) paramBean.setServervalidate(ercbeanSrc.getInputbox().getServervalidate());
            }
        }else
        {
            if(ercbean!=null&&ercbean.getInputbox()!=null) paramBean.setServervalidate(ercbean.getInputbox().getServervalidate());
        }
        owner.addParamBeanInUpdateClause(cbSrc,paramBean);
    }
    
    public void updateDBData(Map<String,String> mParamsValue,Map<String,String> mExternalParamsValue,IConnection conn,ReportBean rbean,
            ReportRequest rrequest) throws SQLException

    {
        AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
        dbtype.updateDBData(mParamsValue,mExternalParamsValue,conn,rbean,rrequest,this);
    }

   public String getParamValue(Map<String,String> mParamsValue,Map<String,String> mExternalParamsValue,ReportBean rbean,ReportRequest rrequest,
            EditableReportParamBean paramBean)
    {
        String paramvalue="";
        if(paramBean.getOwner() instanceof EditableReportExternalValueBean)
        {
            /**paramvalue=((EditableReportExternalValueBean)paramBean.getOwner()).getValue();
             if(Tools.isDefineKey("#",paramvalue))
            {//当前变量是引用绑定保存的其它报表的<external-values/>中定义的某个变量值
                paramvalue=getReferedOtherExternalValue(rbean,rrequest,paramBean,paramvalue);
            }else if(Tools.isDefineKey("@",paramvalue))
            {
                paramvalue=getExternalValueOfReferedCol(rbean,rrequest,paramBean,paramvalue);
            }else
            {*/
            paramvalue=paramBean.getParamValue(mExternalParamsValue.get(paramBean.getParamname()),rrequest,rbean);
            //}
        }else if(paramBean.getOwner() instanceof ColBean)
        {
            paramvalue=EditableReportAssistant.getInstance().getColParamValue(rrequest,rbean,mParamsValue,paramBean.getParamname());
            paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
        }else if("uuid{}".equals(paramBean.getParamname()))
        {
            paramvalue=UUIDGenerator.generateID();
        }else if(Tools.isDefineKey("!",paramBean.getParamname()))
        {
            String customizeParamName=Tools.getRealKeyByDefine("!",paramBean.getParamname());
            Map<String,String> mCustomizedValues=rrequest.getMCustomizeEditData(rbean);
            if(mCustomizedValues==null||!mCustomizedValues.containsKey(customizeParamName))
            {
                paramvalue="";
            }else
            {
                paramvalue=mCustomizedValues.get(customizeParamName);
            }
        }
        return paramvalue;
    }

    public void storeReturnValue(ReportRequest rrequest,Map<String,String> mExternalParamsValue,String rtnVal)
    {
        if(this.returnValueParamname==null||this.returnValueParamname.trim().equals("")) return;
        if(Tools.isDefineKey("#",this.returnValueParamname))
        {
            if(mExternalParamsValue!=null)
            {
                mExternalParamsValue.put(Tools.getRealKeyByDefine("#",this.returnValueParamname),rtnVal);
            }
        }else if(Tools.isDefineKey("rrequest",this.returnValueParamname))
        {
            rrequest.setAttribute(Tools.getRealKeyByDefine("rrequest",this.returnValueParamname),rtnVal);
        }
    }
    
    /*private String getExternalValueOfReferedCol(ReportBean rbean,ReportRequest rrequest,EditableReportParamBean paramBean,String paramvalue)
    {
        ColBean referredColBean=(ColBean)((EditableReportExternalValueBean)paramBean.getOwner()).getRefObj();
        String colParamname=referredColBean.getReportBean().getId()+referredColBean.getProperty();
        if(paramvalue.indexOf(".insert.")>0)
        {//引用某列添加时的数据
            List<Map<String,String>> lstInsertedCValues=rrequest.getLstInsertedData(referredColBean.getReportBean());
            if(lstInsertedCValues!=null&&lstInsertedCValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstInsertedCValues.get(0).get(colParamname),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".update.")>0)
        {
            List<Map<String,String>> lstUpdatedCValues=rrequest.getLstUpdatedData(referredColBean.getReportBean());
            if(lstUpdatedCValues!=null&&lstUpdatedCValues.size()>0)
            {
                paramvalue=Tools.getRealKeyByDefine("@",paramvalue).trim();
                if(paramvalue.endsWith(".old"))
                {
                    paramvalue=lstUpdatedCValues.get(0).get(colParamname+"_old");
                    if(paramvalue==null)
                    {//当前列没有_old，可能此列是隐藏列，或不可编辑的显式列，则新旧值的参数名都是colParamname
                        paramvalue=lstUpdatedCValues.get(0).get(colParamname);
                    }
                    paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
                }else
                {
                    paramvalue=paramBean.getParamValue(lstUpdatedCValues.get(0).get(colParamname),rrequest,rbean);
                }
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".delete.")>0)
        {
            List<Map<String,String>> lstDeletedCValues=rrequest.getLstDeletedData(referredColBean.getReportBean());
            if(lstDeletedCValues!=null&&lstDeletedCValues.size()>0)
            {
                paramvalue=lstDeletedCValues.get(0).get(colParamname+"_old");
                if(paramvalue==null)
                {
                    paramvalue=lstDeletedCValues.get(0).get(colParamname);
                }
                paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else
        {//只指定引用此列的数据，不关心是什么时候操作的数据，则依次从insert、update、delete中取当前列的数据，只要取到即可
            List<Map<String,String>> lstInsertedCValues=rrequest.getLstInsertedData(referredColBean.getReportBean());
            if(lstInsertedCValues!=null&&lstInsertedCValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstInsertedCValues.get(0).get(colParamname),rrequest,rbean);
            }else
            {
                List<Map<String,String>> lstUpdatedCValues=rrequest.getLstUpdatedData(referredColBean.getReportBean());
                if(lstUpdatedCValues!=null&&lstUpdatedCValues.size()>0)
                {
                    paramvalue=paramBean.getParamValue(lstUpdatedCValues.get(0).get(colParamname),rrequest,rbean);
                }else
                {
                    List<Map<String,String>> lstDeletedCValues=rrequest.getLstDeletedData(referredColBean.getReportBean());
                    if(lstDeletedCValues!=null&&lstDeletedCValues.size()>0)
                    {
                        paramvalue=lstDeletedCValues.get(0).get(colParamname+"_old");
                        if(paramvalue==null)
                        {
                            paramvalue=lstDeletedCValues.get(0).get(colParamname);
                        }
                        paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
                    }else
                    {
                        paramvalue="";
                    }
                }
            }
        }
        return paramvalue;
    }*/

    /**private String getReferedOtherExternalValue(ReportBean rbean,ReportRequest rrequest,EditableReportParamBean paramBean,String paramvalue)
    {
        EditableReportExternalValueBean referredEValueBean=(EditableReportExternalValueBean)((EditableReportExternalValueBean)paramBean.getOwner())
                .getRefObj();
        ReportBean rbeanRefered=referredEValueBean.getOwner().getOwner().getOwner().getReportBean();
        if(paramvalue.indexOf(".insert.")>0)
        {//是引用其它报表在<insert/>中定义的变量的值
            List<Map<String,String>> lstInsertedEValues=rrequest.getLstInsertedExternalValues(rbeanRefered);
            if(lstInsertedEValues!=null&&lstInsertedEValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstInsertedEValues.get(0).get(referredEValueBean.getName()),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".update.")>0)
        {//是引用其它报表在<update/>中定义的变量的值
            List<Map<String,String>> lstUpdatedEValues=rrequest.getLstUpdatedExternalValues(rbeanRefered);
            if(lstUpdatedEValues!=null&&lstUpdatedEValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstUpdatedEValues.get(0).get(referredEValueBean.getName()),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".delete.")>0)
        {//是引用其它报表在<delete/>中定义的变量的值
            List<Map<String,String>> lstDeletedEValues=rrequest.getLstDeletedExternalValues(rbeanRefered);//取到被引用的报表本次保存时所有变量的数据
            if(lstDeletedEValues!=null&&lstDeletedEValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstDeletedEValues.get(0).get(referredEValueBean.getName()),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }
        return paramvalue;
    }*/

    public abstract void parseSql(SqlBean sqlbean,String reportTypeKey,String configSql);

    /* public AbsEditSqlActionBean clone(EditableReportUpdateDataBean newowner)
     {
         try
         {
             AbsEditSqlActionBean newBean=(AbsEditSqlActionBean)super.clone();
             newBean.setOwner(newowner);
             if(lstParamBeans!=null)
             {
                 newBean
                         .setLstParamBeans((List<EditableReportParamBean>)((ArrayList<EditableReportParamBean>)lstParamBeans)
                                 .clone());
             }
             return newBean;
         }catch(CloneNotSupportedException e)
         {
             e.printStackTrace();
             return null;
         }
     }*/
}
