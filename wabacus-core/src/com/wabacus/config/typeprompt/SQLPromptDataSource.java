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
package com.wabacus.config.typeprompt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.resultset.GetAllResultSetByPreparedSQL;
import com.wabacus.system.resultset.GetAllResultSetBySQL;
import com.wabacus.system.resultset.ISQLType;
import com.wabacus.util.Tools;

public class SQLPromptDataSource extends AbsTypePromptDataSource implements Cloneable
{
    private static Log log=LogFactory.getLog(SQLPromptDataSource.class);

    private String sql;

    private List<ConditionBean> lstConditions;
    
    public String getSql()
    {
        return sql;
    }

    public List<ConditionBean> getLstConditions()
    {
        return lstConditions;
    }

    public List<Map<String,String>> getResultDataList(ReportRequest rrequest,ReportBean rbean,
            String typedata)
    {
        typedata=Tools.removeSQLKeyword(typedata);
        List<Map<String,String>> lstResults=new ArrayList<Map<String,String>>();
        try
        {
            List<TypePromptColBean> lstPColsBean=promptConfigBean.getLstPColBeans();
            if(lstPColsBean==null||lstPColsBean.size()==0) return null;
            ISQLType ImpISQLType=null;
            if(rbean.getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
            {
                ImpISQLType=new GetAllResultSetByPreparedSQL();
            }else
            {
                ImpISQLType=new GetAllResultSetBySQL();
            }
            String sqlTemp=Tools.replaceAll(sql,"#data#",typedata);
            log.debug("SQL语句："+sqlTemp);
            Object objTmp=ImpISQLType.getResultSet(rrequest,rbean,this,sqlTemp,this.getLstConditions());
            int cnt=0;
            if(objTmp instanceof List)
            {
                for(Object itemTmp:(List)objTmp)
                {
                    if(itemTmp==null) continue;
                    if(!(itemTmp instanceof Map))
                    {
                        throw new WabacusRuntimeException("加载报表"+rbean.getPath()
                                +"输入联想选项数据的拦截器返回的List对象中元素类型不对，必须为Map<String,String>类型，其中key为<typeprompt/>的value或label属性配置值，value为相应的选项数据");
                    }
                    lstResults.add((Map<String,String>)itemTmp);
                    if(++cnt==this.promptConfigBean.getResultcount()) break;
                }
            }else if(objTmp instanceof ResultSet)
            {
                Map<String,String> mCols;
                String labelTmp;
                String valueTmp;
                ResultSet rs=(ResultSet)objTmp;
                while(rs.next())
                {
                    mCols=new HashMap<String,String>();
                    for(TypePromptColBean tpcbean:lstPColsBean)
                    {
                        labelTmp=rs.getString(tpcbean.getLabel());
                        mCols.put(tpcbean.getLabel(),labelTmp==null?"":labelTmp.trim());
                        if(tpcbean.getValue()!=null&&!tpcbean.getValue().trim().equals("")&&!tpcbean.getValue().equals(tpcbean.getLabel()))
                        {//此选项列需要value，且取value的字段名与取label的字段名不同
                            valueTmp=rs.getString(tpcbean.getValue());
                            mCols.put(tpcbean.getValue(),valueTmp==null?"":valueTmp.trim());
                        }
                    }
                    lstResults.add(mCols);
                    if(++cnt==this.promptConfigBean.getResultcount()) break;
                }
                rs.close();
            }else if(objTmp!=null)
            {
                throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"的输入联想选项数据失败，在加载输入联想选项数据的拦截器中返回的对象类型"+objTmp.getClass().getName()+"不合法");
            }
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库为报表"+rbean.getPath()+"获取输入提示数据失败",e);
        }
        if(rbean.getInterceptor()!=null)
        {
            lstResults=(List)rbean.getInterceptor().afterLoadData(rrequest,rbean,this,lstResults);
        }
        return lstResults;
    }

    public void loadExternalConfig(ReportBean rbean,XmlElementBean eleDataSourceBean)
    {
        super.loadExternalConfig(rbean,eleDataSourceBean);
        this.sql=eleDataSourceBean.getContent();
        if(sql==null||sql.trim().equals(""))
        {
            XmlElementBean eleValueBean=eleDataSourceBean.getChildElementByName("value");
            if(eleValueBean!=null)
            {
                sql=eleValueBean.getContent();
            }
        }
        if(sql==null||sql.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"配置的输入联想配置的SQL语句不能为空");
        }
        sql=Tools.formatStringBlank(sql).trim();
        if(!sql.toLowerCase().startsWith("select")||sql.toLowerCase().indexOf("from")<=0)
        {
            throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"配置的输入联想配置的SQL语句"+sql+"不合法");
        }
        sql=sql.substring("select".length()).trim();
        if(sql.toLowerCase().indexOf("distinct")!=0)
        {
            sql=" distinct "+sql;
        }
        sql="select "+sql;
        this.lstConditions=ComponentConfigLoadManager.loadConditionsInOtherPlace(eleDataSourceBean,rbean);
        if(this.lstConditions!=null&&this.lstConditions.size()>0)
        {
            for(ConditionBean cbeanTmp:this.lstConditions)
            {
                if(!Tools.isDefineKey("session",cbeanTmp.getSource()))
                {
                    throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"配置的输入联想的动态查询条件"+cbeanTmp.getName()
                            +"必须指定<condition/>的source为session");
                }
            }
        }
    }

    public void doPostLoad(ReportBean rbean)
    {
        super.doPostLoad(rbean);
        if(rbean.getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT&&this.lstConditions!=null&&this.lstConditions.size()>0)
        {
            for(ConditionBean cbeanTmp:this.lstConditions)
            {
                cbeanTmp.getConditionExpression().parseConditionExpression();
            }
        }
    }
}
