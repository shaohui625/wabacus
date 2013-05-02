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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.commoninterface.IListReportRoworderPersistence;
import com.wabacus.util.Consts;

public class AbsListReportBean extends AbsExtendConfigBean
{
    public final static int SCROLLTYPE_NONE=0;
    
    public final static int SCROLLTYPE_FIXED=1;
    
    public final static int SCROLLTYPE_VERTICAL=2;
    
    public final static int SCROLLTYPE_HORIZONTAL=3;
    
    public final static int SCROLLTYPE_ALL=4;//不固定行列的纵横滚动条
    
    private String rowSelectType;

    private List<String> lstRowSelectCallBackFuncs;

    private List<String> lstRoworderTypes;
    
    private IListReportRoworderPersistence loadStoreRoworderObject;
    
    private int fixedrows;//被冻结的行数，如果值为Integer.MAX_VALUE，则表示fixedrows配置为title。即表示冻结整个标题行，如果指定为大于0的数，则表示指定要冻结的行数
    
    private int fixedcols;
    
    public AbsListReportBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getRowSelectType()
    {
        return rowSelectType;
    }

    public void setRowSelectType(String rowSelectType)
    {
        this.rowSelectType=rowSelectType;
    }

    public List<String> getLstRowSelectCallBackFuncs()
    {
        return lstRowSelectCallBackFuncs;
    }

    public void setLstRowSelectCallBackFuncs(List<String> lstRowSelectCallBackFuncs)
    {
        this.lstRowSelectCallBackFuncs=lstRowSelectCallBackFuncs;
    }

    public List<String> getLstRoworderTypes()
    {
        return lstRoworderTypes;
    }

    public void setLstRoworderTypes(List<String> lstRoworderTypes)
    {
        this.lstRoworderTypes=lstRoworderTypes;
    }

    public IListReportRoworderPersistence getLoadStoreRoworderObject()
    {
        return loadStoreRoworderObject;
    }

    public void setLoadStoreRoworderObject(IListReportRoworderPersistence loadStoreRoworderObject)
    {
        this.loadStoreRoworderObject=loadStoreRoworderObject;
    }

    public void addRowSelectCallBackFunc(String rowselectMethod)
    {
        if(rowselectMethod==null||rowselectMethod.trim().equals("")||rowselectMethod.trim().equals("''")) return;
        rowselectMethod=rowselectMethod.trim();
        if(lstRowSelectCallBackFuncs==null)
        {
            lstRowSelectCallBackFuncs=new ArrayList<String>();
        }else if(lstRowSelectCallBackFuncs.contains(rowselectMethod))
        {
            return;
        }
        lstRowSelectCallBackFuncs.add(rowselectMethod);
    }

    public int getFixedrows()
    {
        return fixedrows;
    }

    public void setFixedrows(int fixedrows)
    {
        this.fixedrows=fixedrows;
    }

    public int getFixedcols()
    {
        return fixedcols;
    }

    public void setFixedcols(int fixedcols)
    {
        this.fixedcols=fixedcols;
    }

    public int getScrollType()
    {
        if(this.fixedcols>0||this.fixedrows>0) return SCROLLTYPE_FIXED;
        ReportBean rbean=(ReportBean)this.getOwner();
        if(rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("")&&rbean.getScrollwidth()!=null
                &&!rbean.getScrollwidth().trim().equals("")) return SCROLLTYPE_ALL;
        if(rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("")) return SCROLLTYPE_VERTICAL;
        if(rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("")) return SCROLLTYPE_HORIZONTAL;
        return SCROLLTYPE_NONE;
    }
    
    public boolean hasControllCol()
    {
        if(Consts.ROWSELECT_CHECKBOX.equalsIgnoreCase(this.rowSelectType)||Consts.ROWSELECT_RADIOBOX.equalsIgnoreCase(this.rowSelectType))
            return true;
        if(this.lstRoworderTypes!=null&&(this.lstRoworderTypes.contains(Consts.ROWORDER_ARROW)
                ||this.lstRoworderTypes.contains(Consts.ROWORDER_INPUTBOX)||this.lstRoworderTypes.contains(Consts.ROWORDER_TOP))) return true;
        return false;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportBean mynewrbean=(AbsListReportBean)super.clone(owner);
        if(lstRowSelectCallBackFuncs!=null)
        {
            mynewrbean
                    .setLstRowSelectCallBackFuncs((List<String>)((ArrayList<String>)lstRowSelectCallBackFuncs)
                            .clone());
        }
        
        return mynewrbean;
    }
}