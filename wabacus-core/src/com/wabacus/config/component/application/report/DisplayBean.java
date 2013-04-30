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
package com.wabacus.config.component.application.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayBean extends AbsConfigBean
{
    private Boolean colselect=null;
    
    private String colselectwidth;
    
    private String dataheader;

    private List<ColBean> lstCols=new ArrayList<ColBean>();
    private Map<String,ColBean> mPropsAndColBeans;
    
    private Map<String,ColBean> mColumnsAndColBeans;
    
    private Map<String,ColBean> mColIdsAndColBeans;
    
    private int generate_childid=0;//此属性纯粹用于加载<group/>和<col/>时，产生各个<group/>和<col/>对应ColBean和GroupBean的id属性值。
        
    public DisplayBean(AbsConfigBean parent)
    {
        super(parent);
        generate_childid=0;
    }

    public String getDataheader()
    {
        return dataheader;
    }

    public void setDataheader(String dataheader)
    {
        this.dataheader=dataheader;
    }

    public Boolean getColselect()
    {
        return colselect;
    }

    public boolean isColselect()
    {
        if(colselect==null) return false;
        return colselect.booleanValue();
    }
    
    public void setColselect(Boolean colselect)
    {
        this.colselect=colselect;
    }

    public String getColselectwidth()
    {
        return colselectwidth;
    }

    public void setColselectwidth(String colselectwidth)
    {
        this.colselectwidth=colselectwidth;
    }

    public void clearChildrenInfo()
    {
        if(lstCols!=null) lstCols.clear(); 
        generate_childid=0;
    }
    
    public int generate_childid()
    {
        return generate_childid++;
    }
    
    public List<ColBean> getLstCols()
    {
        return lstCols;
    }

    public void setLstCols(List<ColBean> lstCols)
    {
        this.lstCols=lstCols;
    }

    public ColBean getColBeanByColProperty(String property)
    {
        if(property==null||property.trim().equals("")||lstCols==null) return null;
        property=property.trim();
        if(mPropsAndColBeans==null||mPropsAndColBeans.get(property)==null)
        {
            Map<String,ColBean> mPropsAndColBeansTmp=new HashMap<String,ColBean>();
            for(ColBean cbTmp:lstCols)
            {
                if(cbTmp.getProperty()==null) continue;
                mPropsAndColBeansTmp.put(cbTmp.getProperty(),cbTmp);
            }
            mPropsAndColBeans=mPropsAndColBeansTmp;
        }
        return mPropsAndColBeans.get(property);
    }

    public ColBean getColBeanByColColumn(String column)
    {
        if(column==null||column.trim().equals("")||lstCols==null) return null;
        column=column.trim();
        if(mColumnsAndColBeans==null||mColumnsAndColBeans.get(column)==null)
        {
            Map<String,ColBean> mColumnsAndColBeansTmp=new HashMap<String,ColBean>();
            for(ColBean cbTmp:this.lstCols)
            {
                if(cbTmp.getColumn()==null) continue;
                mColumnsAndColBeansTmp.put(cbTmp.getColumn(),cbTmp);
            }
            mColumnsAndColBeans=mColumnsAndColBeansTmp;
        }
        return mColumnsAndColBeans.get(column);
    }
    
    public ColBean getColBeanByColId(String colid)
    {
        if(colid==null||colid.trim().equals("")||lstCols==null) return null;
        colid=colid.trim();
        if(mColIdsAndColBeans==null||mColIdsAndColBeans.size()!=this.lstCols.size())
        {
            Map<String,ColBean> mColidsAndColBeansTmp=new HashMap<String,ColBean>();
            for(ColBean cbTmp:this.lstCols)
            {
                mColidsAndColBeansTmp.put(cbTmp.getColid(),cbTmp);
            }
            mColIdsAndColBeans=mColidsAndColBeansTmp;
        }
        return mColIdsAndColBeans.get(colid);
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        DisplayBean dbeanNew=(DisplayBean)super.clone(parent);
        ((ReportBean)parent).setDbean(dbeanNew);
        if(lstCols!=null)
        {
            
            List<ColBean> lstColsNew=new ArrayList<ColBean>();
            for(int i=0;i<lstCols.size();i++)
            {
                lstColsNew.add((ColBean)lstCols.get(i).clone(dbeanNew));
            }
            dbeanNew.setLstCols(lstColsNew);
        }
        dbeanNew.mPropsAndColBeans=null;
        dbeanNew.mColIdsAndColBeans=null;
        dbeanNew.mColumnsAndColBeans=null;
        cloneExtendConfig(dbeanNew);
        return dbeanNew;
    }
}
