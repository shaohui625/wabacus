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
package com.wabacus.system.inputbox;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.util.RegexTools;

public class OptionBean implements Cloneable
{

    private int sourceType;

    private String sql;

    private String label="";

    private String value="";

    private String[] type;

    private List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();//当为SQL语句查询选项数据时，且需要从request/session中获取条件数据，这里存放所有的条件bean

    public void setLabel(String label)
    {
        this.label=label;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public String getLabel()
    {
        return this.label;
    }

    public String getValue()
    {
        return this.value;
    }

    public String[] getType()
    {
        return type;
    }

    public void setType(String[] type)
    {
        this.type=type;
    }

    public int getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(int sourceType)
    {
        this.sourceType=sourceType;
    }

    public String getSql()
    {
        return sql;
    }

    public void setSql(String sql)
    {
        this.sql=sql;
    }

    public List<ConditionBean> getLstConditions()
    {
        return lstConditions;
    }

    public void setLstConditions(List<ConditionBean> lstConditions)
    {
        this.lstConditions=lstConditions;
    }

    public boolean isMatch(String parentValue,boolean isRegex)
    {
        if(this.type==null||this.type.length==0)
        {//没有配置type，则与父下拉框所选中的选项无关，一直显示出来
            return true;
            
            
        }
        if(this.type.length==1&&this.type[0].equals("%true-true%"))
        {
            return false;
        }else if(this.type.length==1&&this.type[0].equals("%false-false%"))
        {
            return false;
        }
        if(parentValue==null) return false;
        for(int i=0;i<type.length;i++)
        {
            if(!isRegex&&parentValue.equals(type[i])) return true;
            if(isRegex&&RegexTools.isMatch(parentValue,type[i])) return true;
        }
        return false;
    }

    protected Object clone()
    {
        OptionBean obNew=null;
        try
        {
            obNew=(OptionBean)super.clone();
            if(this.lstConditions!=null)
            {
                List<ConditionBean> lstConNew=new ArrayList<ConditionBean>();
                for(ConditionBean cbTmp:this.lstConditions)
                {
                    lstConNew.add((ConditionBean)cbTmp.clone(null));
                }
                obNew.setLstConditions(lstConNew);
            }
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return obNew;
    }
}
