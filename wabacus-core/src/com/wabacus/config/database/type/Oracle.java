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
package com.wabacus.config.database.type;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oracle.sql.BLOB;
import oracle.sql.CLOB;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.InsertSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.UpdateSqlActionBean;
import com.wabacus.system.datatype.BigdecimalType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.FloatType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.TimestampType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class Oracle extends AbsDatabaseType
{
    private final static Log log=LogFactory.getLog(Oracle.class);

    public String constructSplitPageSql(SqlBean sbean)
    {
        String sql=sbean.getSqlWithoutOrderby();
        if(sql.indexOf("%orderby%")>0)
        {
            sql=Tools.replaceAll(sql,"%orderby%"," order by "+sbean.getOrderby());
        }
        StringBuffer sqlBuffer=new StringBuffer("SELECT * FROM(SELECT wx_temp_tbl1.*, ROWNUM row_num FROM ");
        sqlBuffer.append("("+sql+")  wx_temp_tbl1 WHERE ROWNUM<=%END%)  wx_temp_tbl2");
        sqlBuffer.append(" WHERE row_num>%START%");
        return sqlBuffer.toString();
    }

    public String constructSplitPageSql(SqlBean sbean,String dynorderby)
    {
        dynorderby=ReportAssistant.getInstance().mixDynorderbyAndRowgroupCols(sbean.getReportBean(),dynorderby);
        dynorderby=" ORDER BY "+dynorderby;
        String sql=sbean.getSqlWithoutOrderby();
        if(sql.indexOf("%orderby%")<0)
        {
            sql=sql+dynorderby;
        }else
        {
            sql=Tools.replaceAll(sql,"%orderby%",dynorderby);
        }
        StringBuffer sqlBuffer=new StringBuffer("SELECT * FROM(SELECT wx_temp_tbl1.*, ROWNUM row_num FROM ");
        sqlBuffer.append("("+sql+")  wx_temp_tbl1 WHERE ROWNUM<=%END%)  wx_temp_tbl2");
        sqlBuffer.append(" WHERE row_num>%START%");
        return sqlBuffer.toString();
    }

    public void constructInsertSql(String configInsertSql,ReportBean rbean,String reportTypeKey,InsertSqlActionBean insertSqlBean)
    {
        int idxwhere=configInsertSql.toLowerCase().indexOf(" where ");
        if(idxwhere<0)
        {
            super.constructInsertSql(configInsertSql,rbean,reportTypeKey,insertSqlBean);
            return;
        }
        String whereclause=configInsertSql.substring(idxwhere).trim();
        if(whereclause.equals(""))
        {
            super.constructInsertSql(configInsertSql,rbean,reportTypeKey,insertSqlBean);
            return;
        }
        configInsertSql=configInsertSql.substring(0,idxwhere).trim();
        List lstParams=new ArrayList();
        StringBuffer sqlBuffer=new StringBuffer();
        sqlBuffer.append("insert into ");
        configInsertSql=configInsertSql.substring("insert".length()).trim();
        if(configInsertSql.toLowerCase().indexOf("into ")==0)
        {
            configInsertSql=configInsertSql.substring("into".length()).trim();
        }
        Map<String,EditableReportParamBean> mLobParamsBean=new HashMap<String,EditableReportParamBean>();
        String tablename=null;
        int idxleft=configInsertSql.indexOf("(");
        if(idxleft<0)
        {//没有指定要更新的字段，则将所有符合要求的从数据库取数据的<col/>全部更新到表中
            tablename=configInsertSql;
            sqlBuffer.append(tablename).append("(");
            List<ColBean> lstCols=rbean.getDbean().getLstCols();
            for(ColBean cbean:lstCols)
            {
                EditableReportParamBean paramBean=insertSqlBean.createEditParamBeanByColbean(cbean,reportTypeKey,false,false);
                if(paramBean==null) continue;
                sqlBuffer.append(cbean.getColumn()+",");
                if(paramBean.getColbeanOwner().getDatatypeObj() instanceof ClobType)
                {
                    lstParams.add("EMPTY_CLOB()");
                    mLobParamsBean.put(cbean.getColumn(),paramBean);
                }else if(paramBean.getColbeanOwner().getDatatypeObj() instanceof BlobType)
                {
                    lstParams.add("EMPTY_BLOB()");
                    mLobParamsBean.put(cbean.getColumn(),paramBean);
                }else
                {
                    lstParams.add(paramBean);
                }
            }
        }else
        {
            tablename=configInsertSql.substring(0,idxleft);
            sqlBuffer.append(tablename).append("(");
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
                        EditableReportExternalValueBean valuebean=insertSqlBean.getOwner().getValueBeanByName(columnvalue);
                        if(valuebean==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有定义"+columnvalue+"对应的变量值");
                        }
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(columnvalue);
                        paramBean.setOwner(valuebean);
                        if(valuebean.getTypeObj() instanceof ClobType)
                        {
                            lstParams.add("EMPTY_CLOB()");
                            mLobParamsBean.put(columnname,paramBean);
                        }else if(valuebean.getTypeObj() instanceof BlobType)
                        {
                            lstParams.add("EMPTY_BLOB()");
                            mLobParamsBean.put(columnname,paramBean);
                        }else
                        {
                            lstParams.add(paramBean);
                        }
                    }else if(Tools.isDefineKey("@",columnvalue))
                    {
                        cb=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",columnvalue));
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+columnvalue+"不合法，没有取到其值对应的<col/>");
                        }
                        EditableReportParamBean paramBean=insertSqlBean.createEditParamBeanByColbean(cb,reportTypeKey,true,true);
                        if(paramBean.getColbeanOwner().getDatatypeObj() instanceof ClobType)
                        {
                            lstParams.add("EMPTY_CLOB()");
                            mLobParamsBean.put(columnname,paramBean);
                        }else if(paramBean.getColbeanOwner().getDatatypeObj() instanceof BlobType)
                        {
                            lstParams.add("EMPTY_BLOB()");
                            mLobParamsBean.put(columnname,paramBean);
                        }else
                        {
                            lstParams.add(paramBean);
                        }
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
                    EditableReportParamBean paramBean=insertSqlBean.createEditParamBeanByColbean(cb,reportTypeKey,true,true);
                    sqlBuffer.append(cb.getColumn()+",");
                    if(paramBean.getColbeanOwner().getDatatypeObj() instanceof ClobType)
                    {
                        lstParams.add("EMPTY_CLOB()");
                        mLobParamsBean.put(cb.getColumn(),paramBean);
                    }else if(paramBean.getColbeanOwner().getDatatypeObj() instanceof BlobType)
                    {
                        lstParams.add("EMPTY_BLOB()");
                        mLobParamsBean.put(cb.getColumn(),paramBean);
                    }else
                    {
                        lstParams.add(paramBean);
                    }
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
        List<EditableReportParamBean> lstCommonTypeParamsBean=new ArrayList<EditableReportParamBean>();
        for(int j=0;j<lstParams.size();j++)
        {
            if(lstParams.get(j) instanceof EditableReportParamBean)
            {
                sqlBuffer.append("?,");
                lstCommonTypeParamsBean.add((EditableReportParamBean)lstParams.get(j));
            }else
            {
                sqlBuffer.append(lstParams.get(j)).append(",");
            }
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
        {
            sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        }
        sqlBuffer.append(")");
        insertSqlBean.getOwner().addInsertSqlActionBean(sqlBuffer.toString(),lstCommonTypeParamsBean,insertSqlBean.getReturnValueParamname());
        if(mLobParamsBean.size()>0)
        {
            List<EditableReportParamBean> lstParamsBean2=new ArrayList<EditableReportParamBean>();
            sqlBuffer=new StringBuffer("select ");
            for(Entry<String,EditableReportParamBean> entry:mLobParamsBean.entrySet())
            {
                sqlBuffer.append(entry.getKey()).append(",");
                lstParamsBean2.add(entry.getValue());
            }
            if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
            {
                sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
            }
            sqlBuffer.append(" from ").append(tablename);
            List<EditableReportParamBean> lstDynParamsInWhereClause=new ArrayList<EditableReportParamBean>();
            whereclause=insertSqlBean.getOwner().parseUpdateWhereClause(rbean,reportTypeKey,lstDynParamsInWhereClause,whereclause);
            ColBean cb;
            EditableReportColBean ercbeanTmp;
            for(EditableReportParamBean paramBean:lstDynParamsInWhereClause)
            {
                cb=paramBean.getColbeanOwner();
                if(cb!=null)
                {//说明是从<col/>中获取值的参数
                    ColBean cbSrc=cb;
                    if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cb.getDisplaytype()))
                    {
                        ercbeanTmp=(EditableReportColBean)cb.getExtendConfigDataForReportType(reportTypeKey);
                        if(ercbeanTmp!=null&&ercbeanTmp.getUpdatedcol()!=null&&!ercbeanTmp.getUpdatedcol().trim().equals(""))
                        {//被别的<col/>通过updatecol属性引用到
                            cbSrc=cb.getReportBean().getDbean().getColBeanByColProperty(ercbeanTmp.getUpdatedcol());//取到引用此<col/>的<col/>对象
                        }
                    }
                    if(insertSqlBean.getOwner().getEdittype()==EditableReportUpdateDataBean.EDITTYPE_INSERT)
                    {//如果当前插入大字段的SQL语句是配置在<insert/>中
                        ColBean cbTmp;
                        EditableReportParamBean paramBeanTmp=null;
                        for(int k=0;k<lstCommonTypeParamsBean.size();k++)
                        {
                            cbTmp=lstCommonTypeParamsBean.get(k).getColbeanOwner();
                            if(cbTmp==null) continue;
                            if(cbTmp.getColumn().equals(cb.getColumn()))
                            {
                                paramBeanTmp=lstCommonTypeParamsBean.get(k);
                                break;
                            }
                        }
                        if(paramBeanTmp==null)
                        {//大字段、添加时不能录入数据的列的字段均不能做为这里的where中的条件
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，字段"+cb.getColumn()
                                    +"或者是大字段类型，或者不能被添加，因此不能做为insert子句的where条件");
                        }
                        insertSqlBean.getOwner().addParamBeanInUpdateClause(cbSrc,paramBean);
                    }else if(insertSqlBean.getOwner().getEdittype()==EditableReportUpdateDataBean.EDITTYPE_DELETE)
                    {//如果当前插入大字段的SQL语句是配置在<delete/>中
                        insertSqlBean.getOwner().addParamBeanInWhereClause(cbSrc,paramBean);
                    }else
                    {//如果当前插入大字段的SQL语句是配置在<update/>中，即EditableReportUpdateDataBean.EDITTYPE_UPDATE
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，对于Oracle数据库，不能在<update/>中"+"配置带where条件的insert语句");
                        
                    }
                }
                lstParamsBean2.add(paramBean);
            }
            sqlBuffer.append(" ").append(whereclause);
            /** ******************************************************** */
            sqlBuffer.append(" for update");
            insertSqlBean.getOwner().addInsertSqlActionBean(sqlBuffer.toString(),lstParamsBean2,null);
        }
    }

    public List<UpdateSqlActionBean> constructUpdateSql(String configUpdateSql,ReportBean rbean,String reportTypeKey,UpdateSqlActionBean updateSqlBean)
    {
        StringBuffer sqlBuffer=new StringBuffer();
        List<EditableReportParamBean> lstParamsBean=new ArrayList<EditableReportParamBean>();
        Map<String,EditableReportParamBean> mLobParamsBean=new HashMap<String,EditableReportParamBean>();
        int idxleft=configUpdateSql.indexOf("(");
        if(idxleft<0)
        {//没有指定要更新的字段，则将所有从数据库取数据的<col/>（不包括displaytype为hidden的<col/>）全部更新到表中
            sqlBuffer.append(configUpdateSql).append(" set ");
            List<ColBean> lstColBeans=rbean.getDbean().getLstCols();
            for(ColBean cbean:lstColBeans)
            {
                EditableReportParamBean paramBean=updateSqlBean.createEditParamBeanByColbean(cbean,reportTypeKey,false,false);
                if(paramBean==null) continue;
                if(paramBean.getColbeanOwner().getDatatypeObj() instanceof ClobType||paramBean.getColbeanOwner().getDatatypeObj() instanceof BlobType)
                {
                    mLobParamsBean.put(cbean.getColumn(),paramBean);
                }else
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
                        sqlBuffer.append(columnname+"="+Tools.getRealKeyByDefine("sequence",columnvalue)+".nextval,");
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
                    {//是从<external-values/>中定义的变量中取值
                        columnvalue=Tools.getRealKeyByDefine("#",columnvalue);
                        EditableReportExternalValueBean valuebean=updateSqlBean.getOwner().getValueBeanByName(columnvalue);
                        if(valuebean==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有定义"+columnvalue+"对应的变量值");
                        }
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(columnvalue);
                        paramBean.setOwner(valuebean);
                        if(valuebean.getTypeObj() instanceof ClobType||valuebean.getTypeObj() instanceof BlobType)
                        {
                            mLobParamsBean.put(columnname,paramBean);
                        }else
                        {
                            sqlBuffer.append(columnname+"=?,");
                            lstParamsBean.add(paramBean);
                        }
                    }else if(Tools.isDefineKey("@",columnvalue))
                    {
                        cb=rbean.getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",columnvalue));
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+columnvalue+"不合法，没有取到其值对应的<col/>");
                        }
                        EditableReportParamBean paramBean=updateSqlBean.createEditParamBeanByColbean(cb,reportTypeKey,true,true);
                        if(paramBean.getColbeanOwner().getDatatypeObj() instanceof ClobType
                                ||paramBean.getColbeanOwner().getDatatypeObj() instanceof BlobType)
                        {
                            mLobParamsBean.put(columnname,paramBean);
                        }else
                        {
                            sqlBuffer.append(columnname+"=?,");
                            lstParamsBean.add(paramBean);
                        }
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
                    EditableReportParamBean paramBean=updateSqlBean.createEditParamBeanByColbean(cb,reportTypeKey,true,true);
                    if(paramBean.getColbeanOwner().getDatatypeObj() instanceof ClobType
                            ||paramBean.getColbeanOwner().getDatatypeObj() instanceof BlobType)
                    {
                        mLobParamsBean.put(cb.getColumn(),paramBean);
                    }else
                    {
                        sqlBuffer.append(cb.getColumn()+"=?,");
                        lstParamsBean.add(paramBean);
                    }
                }
            }
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',')
        {
            sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        }
        List<UpdateSqlActionBean> lstUpdateSqls=new ArrayList<UpdateSqlActionBean>();
        if(mLobParamsBean!=null&&mLobParamsBean.size()>0)
        {
            String tablename=configUpdateSql;
            if(configUpdateSql.indexOf("(")>0)
            {
                tablename=configUpdateSql.substring(0,configUpdateSql.indexOf("("));
            }
            tablename=tablename.substring("update".length()+1).trim();
            List<EditableReportParamBean> lstParamsBean2=new ArrayList<EditableReportParamBean>();
            StringBuffer sqlBuffer2=new StringBuffer("select ");
            StringBuffer sqlBuf=new StringBuffer("update ");
            sqlBuf.append(tablename).append(" set ");

            for(Entry<String,EditableReportParamBean> entry:mLobParamsBean.entrySet())
            {
                sqlBuffer2.append(entry.getKey()).append(",");
                lstParamsBean2.add(entry.getValue());
                sqlBuf.append(entry.getKey()).append("=");
                if(entry.getValue().getDataTypeObj() instanceof ClobType)
                {
                    sqlBuf.append("EMPTY_CLOB(),");
                }else
                {
                    sqlBuf.append("EMPTY_BLOB(),");
                }
            }
            if(sqlBuf.charAt(sqlBuf.length()-1)==',')
            {
                sqlBuf.deleteCharAt(sqlBuf.length()-1);
            }
            if(sqlBuffer2.charAt(sqlBuffer2.length()-1)==',')
            {
                sqlBuffer2.deleteCharAt(sqlBuffer2.length()-1);
            }
            sqlBuffer2.append(" from ");
            sqlBuffer2.append(tablename).append(" %where% for update");
            //先执行update 大字段=EMPTY_C/BLOB()
            UpdateSqlActionBean updateSqlTmpBean=new UpdateSqlActionBean(updateSqlBean.getOwner());
            updateSqlTmpBean.setSql(sqlBuf.toString());
            updateSqlTmpBean.setLstParamBeans(new ArrayList<EditableReportParamBean>());
            lstUpdateSqls.add(updateSqlTmpBean);
            
            updateSqlTmpBean=new UpdateSqlActionBean(updateSqlBean.getOwner());
            updateSqlTmpBean.setSql(sqlBuffer2.toString());
            updateSqlTmpBean.setLstParamBeans(lstParamsBean2);
            lstUpdateSqls.add(updateSqlTmpBean);
        }
        
//        if(lstParamsBean.size()==0) lstParamsBean=null;
        UpdateSqlActionBean updateSqlTmpBean=new UpdateSqlActionBean(updateSqlBean.getOwner());
        updateSqlTmpBean.setSql(sqlBuffer.toString());
        updateSqlTmpBean.setLstParamBeans(lstParamsBean);
        lstUpdateSqls.add(updateSqlTmpBean);
        return lstUpdateSqls;
    }

    public byte[] getBlobValue(ResultSet rs,String column) throws SQLException
    {
        BLOB blob=(BLOB)rs.getBlob(column);
        if(blob==null) return null;
        BufferedInputStream bin=null;
        try
        {
            bin=new BufferedInputStream(blob.getBinaryStream());
            return Tools.getBytesArrayFromInputStream(bin);
        }catch(Exception e)
        {
            log.error("读取二进制字段"+column+"失败",e);
            return null;
        }finally
        {
            if(bin!=null)
            {
                try
                {
                    bin.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] getBlobValue(ResultSet rs,int iindex) throws SQLException
    {
        BLOB blob=(BLOB)rs.getBlob(iindex);
        if(blob==null) return null;
        BufferedInputStream bin=null;
        try
        {
            bin=new BufferedInputStream(blob.getBinaryStream());
            return Tools.getBytesArrayFromInputStream(bin);
        }catch(Exception e)
        {
            log.error("读取二进制字段失败",e);
            return null;
        }finally
        {
            if(bin!=null)
            {
                try
                {
                    bin.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getClobValue(ResultSet rs,String column) throws SQLException
    {
        CLOB clob=(CLOB)rs.getClob(column);
        BufferedReader in=null;
        try
        {
            if(clob==null) return "";
            in=new BufferedReader(clob.getCharacterStream());
            StringBuffer sbuffer=new StringBuffer();
            String str=in.readLine();
            while(str!=null)
            {
                sbuffer.append(str).append("\n");
                str=in.readLine();
            }
            return sbuffer.toString();
        }catch(IOException e)
        {
            log.error("读取大字符串字段"+column+"失败",e);
            return null;
        }finally
        {
            if(in!=null)
            {
                try
                {
                    in.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getClobValue(ResultSet rs,int iindex) throws SQLException
    {
        CLOB clob=(CLOB)rs.getClob(iindex);
        BufferedReader in=null;
        try
        {
            if(clob==null) return "";
            in=new BufferedReader(clob.getCharacterStream());
            StringBuffer sbuffer=new StringBuffer();
            String str=in.readLine();
            while(str!=null)
            {
                sbuffer.append(str).append("\n");
                str=in.readLine();
            }
            return sbuffer.toString();
        }catch(IOException e)
        {
            log.error("读取大字符串字段失败",e);
            return null;
        }finally
        {
            if(in!=null)
            {
                try
                {
                    in.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setClobValue(int iindex,String value,PreparedStatement pstmt) throws SQLException
    {
        if(value==null) value="";
        BufferedReader reader=new BufferedReader(new StringReader(value));
        pstmt.setCharacterStream(iindex,reader,value.length());
    }

    public void setClobValueInSelectMode(Object value,oracle.sql.CLOB clob) throws SQLException
    {
        if(clob==null) return;
        String strvalue="";
        if(value!=null) strvalue=String.valueOf(value);
        BufferedWriter out=new BufferedWriter(clob.getCharacterOutputStream());
        BufferedReader in=new BufferedReader(new StringReader(strvalue));
        try
        {
            int c;
            while((c=in.read())!=-1)
            {
                out.write(c);
            }
            in.close();
            out.flush();
            out.close();
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("将"+value+"写入CLOB字段失败",e);
        }
    }

    public void setBlobValueInSelectMode(Object value,oracle.sql.BLOB blob) throws SQLException
    {
        if(blob==null) return;
        InputStream in=null;
        if(value==null)
        {
            blob=null;
            return;
        }else if(value instanceof byte[])
        {
            in=Tools.getInputStreamFromBytesArray((byte[])value);
        }else if(value instanceof InputStream)
        {
            in=(InputStream)value;
        }else
        {
            throw new WabacusRuntimeException("将"+value+"写入BLOB字段失败，不是byte[]类型或InputStream类型");
        }
        BufferedOutputStream out=new BufferedOutputStream(blob.getBinaryOutputStream());
        try
        {
            int c;
            while((c=in.read())!=-1)
            {
                out.write(c);
            }
            in.close();
            out.close();
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("将流"+value+"写入BLOB字段失败",e);
        }
    }

    public IDataType getWabacusDataTypeByColumnType(String columntype)
    {
        if(columntype==null||columntype.trim().equals("")) return null;
        columntype=columntype.toLowerCase().trim();
        IDataType dataTypeObj=null;
        if(columntype.indexOf("varchar")>=0||columntype.equals("char")||columntype.equals("nchar"))
        {
            dataTypeObj=new VarcharType();
        }else if(columntype.equals("integer"))
        {
            dataTypeObj=new IntType();
        }else if(columntype.equals("long raw")||columntype.equals("raw")||columntype.equals("blob"))
        {
            dataTypeObj=new BlobType();
        }else if(columntype.indexOf("date")>=0)
        {
            dataTypeObj=new DateType();
        }else if(columntype.equals("decimal")||columntype.equals("number"))
        {
            dataTypeObj=new BigdecimalType();
        }else if(columntype.equals("float"))
        {
            dataTypeObj=new FloatType();
        }else if(columntype.equals("real"))
        {
            dataTypeObj=new DoubleType();
        }else if(columntype.indexOf("timestamp")>=0)
        {
            dataTypeObj=new TimestampType();
        }else if(columntype.equals("clob")||columntype.equals("long")||columntype.equals("nclob"))
        {
            dataTypeObj=new ClobType();
        }else
        {
            log.warn("数据类型："+columntype+"不支持，将当做varchar类型");
            dataTypeObj=new VarcharType();
        }
        return dataTypeObj;
    }
}
