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

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.util.Tools;

public class UpdateSqlActionBean extends AbsEditSqlActionBean
{

    public UpdateSqlActionBean(EditableReportUpdateDataBean _owner)
    {
        super(_owner);
    }

    public void parseSql(SqlBean sqlbean,String reportTypeKey,String configSql)
    {
        configSql=this.parseAndRemoveReturnParamname(configSql);
        if(isNormalUpdateSql(configSql))
        {
            UpdateSqlActionBean updateSqlBean=new UpdateSqlActionBean(owner);
            updateSqlBean.setSql(configSql);
            updateSqlBean.setLstParamBeans(null);
            updateSqlBean.setReturnValueParamname(this.returnValueParamname);
            owner.getLstSqlActionBeans().add(updateSqlBean);
            return;
        }
        int idxwhere=configSql.toLowerCase().indexOf(" where ");
        String whereclause=null;
        if(idxwhere>0)
        {
            whereclause=configSql.substring(idxwhere).trim();
            configSql=configSql.substring(0,idxwhere).trim();
        }
        AbsDatabaseType dbtype = sqlbean.getDbType();
        List<UpdateSqlActionBean> lstRealUpdateSqls=dbtype.constructUpdateSql(configSql,sqlbean.getReportBean(),reportTypeKey,this);
        List<EditableReportParamBean> lstParamsBean;
        String updatesql;
        for(UpdateSqlActionBean updateSqlBeanTmp:lstRealUpdateSqls)
        {
            updatesql=updateSqlBeanTmp.getSql();
            lstParamsBean=updateSqlBeanTmp.getLstParamBeans();
            if(whereclause!=null&&!whereclause.trim().equals(""))
            {
                String realwhereclause=parseUpdateWhereClause(sqlbean,reportTypeKey,lstParamsBean,whereclause);
                if(updatesql.indexOf("%where%")>0)
                {
                    updatesql=Tools.replaceAll(updatesql,"%where%",realwhereclause);
                }else
                {
                    updatesql=updatesql+"  "+realwhereclause;
                }
            }
            updateSqlBeanTmp.setSql(updatesql);
            updateSqlBeanTmp.setLstParamBeans(lstParamsBean);
            updateSqlBeanTmp.setReturnValueParamname(this.returnValueParamname);
            owner.getLstSqlActionBeans().add(updateSqlBeanTmp);
        }
    }

    public List<UpdateSqlActionBean> constructUpdateSql(String configUpdateSql,ReportBean rbean,String reportTypeKey)
    {
        StringBuffer sqlBuffer=new StringBuffer();
        List<EditableReportParamBean> lstParamsBean=new ArrayList<EditableReportParamBean>();
        int idxleft=configUpdateSql.indexOf("(");
        if(idxleft<0)
        {//没有指定要更新的字段，则将所有从数据库取数据的<col/>（不包括hidden="1"和="2"的<col/>）全部更新到表中
            sqlBuffer.append(configUpdateSql).append(" set ");
            List<ColBean> lstColBeans=rbean.getDbean().getLstCols();
            for(ColBean cbean:lstColBeans)
            {
                EditableReportParamBean paramBean=createEditParamBeanByColbean(cbean,reportTypeKey,false,false);
                if(paramBean!=null)
                {
                    sqlBuffer.append(cbean.getColumn()+"=?,");
                    lstParamsBean.add(paramBean);
                }
            }
        }else
        {
            sqlBuffer.append(configUpdateSql.substring(0,idxleft)).append(" set ");
            int idxright=configUpdateSql.lastIndexOf(")");
            if(idxright!=configUpdateSql.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法");
            }
            String cols=configUpdateSql.substring(idxleft+1,idxright);
            List<String> lstUpdateCols=Tools.parseStringToList(cols,',','\'');
            String columnname=null;
            String columnvalue=null;
            ColBean cb;
            for(String updatecol:lstUpdateCols)
            {
                if(updatecol==null||updatecol.trim().equals("")) continue;
                int idxequals=updatecol.indexOf("=");
                if(idxequals>0)
                {
                    columnname=updatecol.substring(0,idxequals).trim();
                    columnvalue=updatecol.substring(idxequals+1).trim();
                    if(Tools.isDefineKey("sequence",columnvalue))
                    {
                        sqlBuffer.append(columnname+"=").append(Tools.getRealKeyByDefine("sequence",columnvalue)).append(".nextval,");
                    }else if(columnvalue.equals("uuid{}"))
                    {
                        sqlBuffer.append(columnname+"=?,");
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname("uuid{}");
                        lstParamsBean.add(paramBean);
                    }else if(Tools.isDefineKey("!",columnvalue))
                    {//当前列是获取自定义保存数据进行更新
                        sqlBuffer.append(columnname+"=?,");
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(columnvalue);
                        lstParamsBean.add(paramBean);
                    }else if(Tools.isDefineKey("#",columnvalue))
                    {
                        sqlBuffer.append(columnname+"=?,");
                        columnvalue=Tools.getRealKeyByDefine("#",columnvalue);
                        EditableReportExternalValueBean valuebean=owner.getValueBeanByName(columnvalue);
                        if(valuebean==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有定义"+columnvalue+"对应的变量值");
                        }
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(columnvalue);
                        paramBean.setOwner(valuebean);
                        lstParamsBean.add(paramBean);
                    }else if(Tools.isDefineKey("@",columnvalue))
                    {
                        cb=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",columnvalue));
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+columnvalue+"不合法，没有取到其值对应的<col/>");
                        }
                        sqlBuffer.append(columnname+"=?,");
                        lstParamsBean.add(createEditParamBeanByColbean(cb,reportTypeKey,true,true));
                    }else
                    {
                        sqlBuffer.append(columnname+"="+columnvalue).append(",");
                    }
                }else
                {
                    if(!Tools.isDefineKey("@",updatecol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法，更新的字段值必须采用@{}括住");
                    }
                    cb=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",updatecol));
                    if(cb==null)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+updatecol+"不合法，没有取到其值对应的<col/>");
                    }
                    sqlBuffer.append(cb.getColumn()+"=?,");
                    lstParamsBean.add(createEditParamBeanByColbean(cb,reportTypeKey,true,true));
                }
            }
        }


//            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<update/>中没有取到要更新的字段");
//        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
        {
            sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        }
        List<UpdateSqlActionBean> lstUpdateSqls=new ArrayList<UpdateSqlActionBean>();
        UpdateSqlActionBean updateSqlBean=new UpdateSqlActionBean(owner);
        updateSqlBean.setSql(sqlBuffer.toString());
        updateSqlBean.setLstParamBeans(lstParamsBean);
        lstUpdateSqls.add(updateSqlBean);
        return lstUpdateSqls;
    }
    
    private boolean isNormalUpdateSql(String updatesql)
    {
        updatesql=updatesql==null?"":updatesql.toLowerCase().trim();
        if(!updatesql.startsWith("update ")) return false;
        updatesql=updatesql.substring("update ".length()).trim();
        int idxBlank=updatesql.indexOf(" ");
        if(idxBlank<0||updatesql.substring(0,idxBlank).indexOf("(")>0) return false;
        updatesql=updatesql.substring(idxBlank+1).trim();
        if(updatesql.startsWith("set ")) return true;
        return false;
    }
}
