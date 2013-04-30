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
package com.wabacus.system.component.application.report.configbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;

public class CrossStatisticListSqlBean extends AbsExtendConfigBean
{
    private String sql_getcols;
    
    private String sql_getVerticalStatisData;
    
    private List<Map<String,String>> lstDynCols;
    
    public CrossStatisticListSqlBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getSql_getcols()
    {
        return sql_getcols;
    }

    public String getSql_getVerticalStatisData()
    {
        return sql_getVerticalStatisData;
    }

    public void setSql_getVerticalStatisData(String sql_getVerticalStatisData)
    {
        this.sql_getVerticalStatisData=sql_getVerticalStatisData;
    }

    public void setSql_getcols(String sql_getcols)
    {
        this.sql_getcols=sql_getcols;
    }

    public List<Map<String,String>> getLstDynCols()
    {
        return lstDynCols;
    }

    public void setLstDynCols(List<Map<String,String>> lstDynCols)
    {
        this.lstDynCols=lstDynCols;
    }
    
    public List<String> getLstDynColumns()
    {
        List<String> lstCols=new ArrayList<String>();
        for(Map<String,String> mTmp:this.lstDynCols)
        {
            lstCols.add(mTmp.keySet().iterator().next());
        }
        return lstCols;
    }
    
    public void initDynCols(Object dynBean)
    {
        lstDynCols=new ArrayList<Map<String,String>>();
        if(dynBean instanceof CrossStatisticListColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)dynBean;
            Map<String,String> mTmp=new HashMap<String,String>();
            mTmp.put(((ColBean)cslrcbean.getOwner()).getColumn(),cslrcbean.getRealvalue());
            lstDynCols.add(mTmp);
        }else if(dynBean instanceof CrossStatisticListGroupBean)
        {//配置的交叉列是一个<group/>
            ((CrossStatisticListGroupBean)dynBean).getDynCols(lstDynCols);
        }
        if(lstDynCols.size()==0)
        {
            throw new WabacusConfigLoadingException("没有取到配置的获取动态列的字段");
        }
    }
    
    public void initFetchDynColSql(String tablename)
    {
        if(tablename==null||tablename.trim().equals("")) return;
        StringBuffer getDyncolsSqlBuf=new StringBuffer();
        getDyncolsSqlBuf.append("select distinct ");
        String columnTmp;
        String realvalueTmp;
        for(Map<String,String> mTmp:this.lstDynCols)
        {//构造select 子句
            columnTmp=mTmp.keySet().iterator().next();
            realvalueTmp=mTmp.get(columnTmp);
            if(realvalueTmp!=null&&!realvalueTmp.trim().equals(""))
            {
                getDyncolsSqlBuf.append(realvalueTmp).append(" as ");
            }
            getDyncolsSqlBuf.append(columnTmp).append(",");
        }
        if(getDyncolsSqlBuf.charAt(getDyncolsSqlBuf.length()-1)==',')
            getDyncolsSqlBuf.deleteCharAt(getDyncolsSqlBuf.length()-1);
        getDyncolsSqlBuf.append(" from ");
        if(tablename.toLowerCase().trim().indexOf("select ")>=0
                &&tablename.toLowerCase().indexOf(" from ")>0)
        {
            getDyncolsSqlBuf.append(" (").append(tablename).append(")  tbl_getdyncol");
        }else
        {
            getDyncolsSqlBuf.append(tablename);
        }
        getDyncolsSqlBuf.append(" order by ");
        for(Map<String,String> mTmp:this.lstDynCols)
        {
            getDyncolsSqlBuf.append(mTmp.keySet().iterator().next()).append(",");
        }
        if(getDyncolsSqlBuf.charAt(getDyncolsSqlBuf.length()-1)==',')
            getDyncolsSqlBuf.deleteCharAt(getDyncolsSqlBuf.length()-1);
        this.sql_getcols=getDyncolsSqlBuf.toString();
    }
}

