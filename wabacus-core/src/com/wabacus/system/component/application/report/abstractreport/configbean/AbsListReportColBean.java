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

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public class AbsListReportColBean extends AbsExtendConfigBean
{
    private int sequenceStartNum;
    
    private boolean rowgroup=false;

    private boolean requireClickOrderby=false;//是否需要点击标题进行排序功能，配置此属性的<col/>必须配置有column属性

    private boolean isRowSelectValue=false;//当前<col/>是否需要在行选中的javascript回调函数中使用，如果设置为true，则在显示当前<col/>时，会在<td/>中显示一个名为value属性，值为当前列的值

    private String slaveReportParamName;

//    private String curvelabelup;//如果当前列标题是显示成折线，则此处存放当前列的折线上面的标题。

//    private String curvelabeldown;//如果当前列标题是显示成折线，则此处存放当前列的折线上面的标题。
//    
//    private String curvecolor;//折线标题中折线的颜色

//    private boolean isCurveLabel;//当前列是否参与了显示折线标题
    
    private boolean isRoworderValue=false;//当前<col/>是否需要在行排序时传入后台，此时会在其所属的<td/>中显示name和value值
    
    private String roworder_inputboxstyleproperty;
    
    private boolean isFixedCol;
    
    private AbsListReportFilterBean filterBean;

    public AbsListReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public int getSequenceStartNum()
    {
        return sequenceStartNum;
    }

    public void setSequenceStartNum(int sequenceStartNum)
    {
        this.sequenceStartNum=sequenceStartNum;
    }

    public boolean isRequireClickOrderby()
    {
        return requireClickOrderby;
    }

    public void setRequireClickOrderby(boolean requireClickOrderby)
    {
        this.requireClickOrderby=requireClickOrderby;
    }

    public boolean isRowgroup()
    {
        return rowgroup;
    }

    public void setRowgroup(boolean rowgroup)
    {
        this.rowgroup=rowgroup;
    }

    public boolean isRowSelectValue()
    {
        return isRowSelectValue;
    }

    public void setRowSelectValue(boolean isRowSelectValue)
    {
        this.isRowSelectValue=isRowSelectValue;
    }

    public String getSlaveReportParamName()
    {
        return slaveReportParamName;
    }

    public void setSlaveReportParamName(String slaveReportParamName)
    {
        this.slaveReportParamName=slaveReportParamName;
    }


//    {




//    {




//    {




//    {




//    {




//    {




//    {




//    {



    public boolean isFixedCol()
    {
        return isFixedCol;
    }

    public void setFixedCol(boolean isFixedCol)
    {
        this.isFixedCol=isFixedCol;
    }

    public boolean isRoworderValue()
    {
        return isRoworderValue;
    }

    public void setRoworderValue(boolean isRoworderValue)
    {
        this.isRoworderValue=isRoworderValue;
    }

    public String getRoworder_inputboxstyleproperty()
    {
        return roworder_inputboxstyleproperty;
    }

    public void setRoworder_inputboxstyleproperty(String roworder_inputboxstyleproperty)
    {
        this.roworder_inputboxstyleproperty=roworder_inputboxstyleproperty;
    }

    public AbsListReportFilterBean getFilterBean()
    {
        return filterBean;
    }

    public void setFilterBean(AbsListReportFilterBean filterBean)
    {
        this.filterBean=filterBean;
    }

    public boolean isDragable(AbsListReportDisplayBean alrdbean)
    {
        if(this.isFixedCol) return false;
        if(alrdbean==null||alrdbean.getRowgrouptype()<=0||alrdbean.getRowGroupColsNum()<=0) return true;
        if(this.isRowgroup()) return false;//当前列参与了行分组或树形分组
        return true;
    }
    
    public boolean shouldShowColNamePropertyInTd()
    {
        if(this.isRowSelectValue) return true;
        if(this.isRoworderValue) return true;
        return false;
    }
    
    public boolean shouldShowColValuePropertyInTd()
    {
        if(this.slaveReportParamName!=null&&!this.slaveReportParamName.trim().equals("")) return true;
        if(this.isRowSelectValue) return true;
        if(this.isRoworderValue) return true;//当前在需要在行排序时用到
        
        return false;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportColBean bean=(AbsListReportColBean)super.clone(owner);
        if(this.filterBean!=null)
        {
            bean.setFilterBean((AbsListReportFilterBean)this.filterBean.clone(owner));
        }
        return bean;
    }
}