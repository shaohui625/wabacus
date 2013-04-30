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

import java.util.ArrayList;
import java.util.List;

public class TypePromptBean implements Cloneable
{
    private int resultcount=10;

    private int resultspanwidth=-1;//显示结果的<span/>的宽度，如果为-1，则与相应输入框保持同一宽度。

    private boolean timeout=true;//显示结果的<span/>是否自动消失，true：不自动消失，false不自动消失

    private boolean showtitle;
    
    private String callbackmethod;
    
    private List<TypePromptColBean> lstPColBeans;

    private AbsTypePromptDataSource datasource;

    public int getResultcount()
    {
        return resultcount;
    }

    public void setResultcount(int resultcount)
    {
        this.resultcount=resultcount;
    }

    public int getResultspanwidth()
    {
        return resultspanwidth;
    }

    public void setResultspanwidth(int resultspanwidth)
    {
        this.resultspanwidth=resultspanwidth;
    }

    public boolean isTimeout()
    {
        return timeout;
    }

    public void setTimeout(boolean timeout)
    {
        this.timeout=timeout;
    }

    public boolean isShowtitle()
    {
        return showtitle;
    }

    public void setShowtitle(boolean showtitle)
    {
        this.showtitle=showtitle;
    }

    public String getCallbackmethod()
    {
        return callbackmethod;
    }

    public void setCallbackmethod(String callbackmethod)
    {
        this.callbackmethod=callbackmethod;
    }

    public List<TypePromptColBean> getLstPColBeans()
    {
        return lstPColBeans;
    }

    public void setLstPColBeans(List<TypePromptColBean> lstPColBeans)
    {
        this.lstPColBeans=lstPColBeans;
    }

    public AbsTypePromptDataSource getDatasource()
    {
        return datasource;
    }

    public void setDatasource(AbsTypePromptDataSource datasource)
    {
        this.datasource=datasource;
    }

    public Object clone()
    {
        try
        {
            TypePromptBean tpbNew=(TypePromptBean)super.clone();
            if(datasource!=null)
            {
                AbsTypePromptDataSource dsNew=(AbsTypePromptDataSource)datasource.clone();
                dsNew.setPromptConfigBean(this);
                tpbNew.setDatasource(dsNew);
            }
            if(lstPColBeans!=null&&lstPColBeans.size()>0)
            {
                List<TypePromptColBean> lstPColBeansNew=new ArrayList<TypePromptColBean>();
                for(int i=0;i<lstPColBeans.size();i++)
                {
                    lstPColBeansNew.add((TypePromptColBean)lstPColBeans.get(i).clone());
                }
                tpbNew.setLstPColBeans(lstPColBeansNew);
            }
            return tpbNew;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
