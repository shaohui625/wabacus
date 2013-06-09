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

public class InsertSqlActionBean extends AbsEditSqlActionBean
{
    public InsertSqlActionBean(EditableReportUpdateDataBean _owner)
    {
        super(_owner);
    }

    public void parseSql(SqlBean sqlbean,String reportTypeKey,String configSql)
    {
        AbsDatabaseType dbtype = sqlbean.getDbType();
        dbtype.constructInsertSql(configSql,sqlbean.getReportBean(),reportTypeKey,this);
    }

    public void constructInsertSql(String configInsertSql,ReportBean rbean,String reportTypeKey)
    {
        configInsertSql=this.parseAndRemoveReturnParamname(configInsertSql);
        if(this.isNormalInsertSql(configInsertSql))
        {
            owner.addInsertSqlActionBean(configInsertSql,null,this.returnValueParamname);
            return;
        }

        List lstParams=new ArrayList();
        StringBuffer sqlBuffer=new StringBuffer();
        sqlBuffer.append("insert into ");
        configInsertSql=configInsertSql.substring("insert".length()).trim();
        if(configInsertSql.toLowerCase().indexOf("into ")==0)
        {
            configInsertSql=configInsertSql.substring("into".length()).trim();
        }
        int idxleft=configInsertSql.indexOf("(");
        if(idxleft<0)
        {//没有指定要更新的字段，则将所有符合要求的从数据库取数据的<col/>全部更新到表中
            sqlBuffer.append(configInsertSql).append("(");
            List<ColBean> lstColBeans=rbean.getDbean().getLstCols();
            for(ColBean cbean:lstColBeans)
            {
                EditableReportParamBean paramBean=createEditParamBeanByColbean(cbean,reportTypeKey,false,false);
                if(paramBean!=null)
                {
                    sqlBuffer.append(cbean.getColumn()+",");
                    lstParams.add(paramBean);
                }
            }
        }else
        {
            sqlBuffer.append(configInsertSql.substring(0,idxleft)).append("(");
            int idxright=configInsertSql.lastIndexOf(")");
            if(idxright!=configInsertSql.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configInsertSql+"不合法");
            }
            String cols=configInsertSql.substring(idxleft+1,idxright);
            List<String> lstInsertCols=Tools.parseStringToList(cols,',','\'');
            String columnname=null;
            String columnvalue=null;
            ColBean cb;
            for(String updatecol:lstInsertCols)
            {
                if(updatecol==null||updatecol.trim().equals("")) continue;
                int idxequals=updatecol.indexOf("=");
                if(idxequals>0)
                {
                    columnname=updatecol.substring(0,idxequals).trim();
                    columnvalue=updatecol.substring(idxequals+1).trim();
                    sqlBuffer.append(columnname+",");
                    if(Tools.isDefineKey("sequence",columnvalue))
                    {
                        lstParams.add(Tools.getRealKeyByDefine("sequence",columnvalue)+".nextval");
                    }else if(columnvalue.equals("uuid{}"))
                    {
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname("uuid{}");
                        lstParams.add(paramBean);
                    }else if(Tools.isDefineKey("!",columnvalue))
                    {
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(columnvalue);
                        lstParams.add(paramBean);
                    }else if(Tools.isDefineKey("#",columnvalue))
                    {//是从<external-values/>中定义的变量中取值
                        columnvalue=Tools.getRealKeyByDefine("#",columnvalue);
                        EditableReportExternalValueBean valuebean=owner.getValueBeanByName(columnvalue);
                        if(valuebean==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有定义"+columnvalue+"对应的变量值");
                        }
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(columnvalue);
                        paramBean.setOwner(valuebean);
                        lstParams.add(paramBean);
                    }else if(Tools.isDefineKey("@",columnvalue))
                    {
                        cb=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",columnvalue));
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+columnvalue+"不合法，没有取到其值对应的<col/>");
                        }
                        lstParams.add(createEditParamBeanByColbean(cb,reportTypeKey,true,true));
                    }else
                    {
                        lstParams.add(columnvalue);
                    }
                }else
                {
                    if(!Tools.isDefineKey("@",updatecol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的添加数据SQL语句"+configInsertSql+"不合法");
                    }
                    cb=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",updatecol));
                    if(cb==null)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+updatecol+"不合法，没有取到其值对应的<col/>");
                    }
                    sqlBuffer.append(cb.getColumn()+",");
                    lstParams.add(createEditParamBeanByColbean(cb,reportTypeKey,true,true));
                }
            }
        }
        if(lstParams.size()==0)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的sql语句："+configInsertSql+"失败，SQL语句格式不对");
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
        {
            sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        }
        sqlBuffer.append(") values(");
        List<EditableReportParamBean> lstParamsBeanTmp=new ArrayList<EditableReportParamBean>();
        for(int j=0;j<lstParams.size();j++)
        {
            if(lstParams.get(j) instanceof EditableReportParamBean)
            {
                sqlBuffer.append("?,");
                lstParamsBeanTmp.add((EditableReportParamBean)lstParams.get(j));
            }else
            {//常量或直接从数据库取数据的数据库函数
                sqlBuffer.append(lstParams.get(j)).append(",");
            }
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
        {
            sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        }
        sqlBuffer.append(")");
        owner.addInsertSqlActionBean(sqlBuffer.toString(),lstParamsBeanTmp,this.returnValueParamname);
    }
    
    private boolean isNormalInsertSql(String insertsql)
    {
        insertsql=insertsql==null?"":insertsql.toLowerCase().trim();
        if(!insertsql.startsWith("insert")) return false;
        if(insertsql.indexOf(" into ")<0) return false;
        StringBuffer tmpBuf=new StringBuffer();
        for(int i=0,len=insertsql.length();i<len;i++)
        {
            if(insertsql.charAt(i)==' ') continue;
            tmpBuf.append(insertsql.charAt(i));
        }
        insertsql=tmpBuf.toString();
        int idxValues=insertsql.indexOf(")values(");
        if(idxValues<=0||insertsql.substring(0,idxValues).indexOf("(")<0||insertsql.lastIndexOf(")")<idxValues+")values(".length()) return false;
        return true;
    }
}
