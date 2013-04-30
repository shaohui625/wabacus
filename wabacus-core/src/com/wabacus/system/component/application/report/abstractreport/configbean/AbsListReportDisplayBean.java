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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class AbsListReportDisplayBean extends AbsExtendConfigBean
{
//    private boolean containsNonConditionFilter;//当前报表是否配置了与查询条件无关的列过滤功能，此属性不用配置，会自动根据配置文件赋值
    
    private boolean containsClickOrderBy;
    
    private String defaultFirstColid;
    
    private String defaultLastColId;
    
    private int defaultColumnCount;
    
    private int rowgrouptype;//行分组的类型，1：普通行分组；2：树形行分组

    private int treeborder=2;

    private boolean treecloseable=true;

    private int treexpandlayer=-1;
    
    private boolean isTreeAsynLoad;
    
    private List<String> lstTreeclosedimgs;//各层树枝节点收缩时显示的前缀图片
    
    private List<String> lstTreexpandimgs;
    
    private String treeleafimg;
    
    private String mouseoverbgcolor;
    private List<String> lstRowgroupColsColumn;

    private List<Map<String,String>> lstRowgroupColsAndOrders;

    private List<ColBean> lstRoworderValueCols;
    
    private AbsListReportStatiBean statibean;//行统计的配置    

    private Map<String,AbsListReportFilterBean> mAllFilterBeans;
    
    private String treenodeid;
    
    private String treenodename;
    
    private String treenodeparentid;
    
//    /**
//     * 存放当前报表中，所有参与折线标题的列的集合。每个折线标题列对应一个List,存放当前参与当前折线标题的列的集合。

//     * 如果是存放普通报表的折线标题包括的列的集合，则Map的key为0，如果是存放某个分组的折线列标题，则key为相应分组对应的<group/>的id。
//     */

    
    public AbsListReportDisplayBean(AbsConfigBean owner)
    {
        super(owner);
    }



//        return containsNonConditionFilter;




//        this.containsNonConditionFilter=containsNonConditionFilter;


    public boolean isContainsClickOrderBy()
    {
        return containsClickOrderBy;
    }

    public void setContainsClickOrderBy(boolean containsClickOrderBy)
    {
        this.containsClickOrderBy=containsClickOrderBy;
    }

    public String getDefaultFirstColid()
    {
        return defaultFirstColid;
    }

    public void setDefaultFirstColid(String defaultFirstColid)
    {
        this.defaultFirstColid=defaultFirstColid;
    }

    public String getDefaultLastColId()
    {
        return defaultLastColId;
    }

    public void setDefaultLastColId(String defaultLastColId)
    {
        this.defaultLastColId=defaultLastColId;
    }

    public int getDefaultColumnCount()
    {
        return defaultColumnCount;
    }

    public void setDefaultColumnCount(int defaultColumnCount)
    {
        this.defaultColumnCount=defaultColumnCount;
    }

    public int getRowgrouptype()
    {
        return rowgrouptype;
    }

    public void setRowgrouptype(int rowgrouptype)
    {
        this.rowgrouptype=rowgrouptype;
    }

    public List<String> getLstRowgroupColsColumn()
    {
        return lstRowgroupColsColumn;
    }

    public void setLstRowgroupColsColumn(List<String> lstRowgroupColsColumn)
    {
        this.lstRowgroupColsColumn=lstRowgroupColsColumn;
    }

    public List<Map<String,String>> getLstRowgroupColsAndOrders()
    {
        return lstRowgroupColsAndOrders;
    }

    public void setLstRowgroupColsAndOrders(List<Map<String,String>> lstRowgroupColsAndOrders)
    {
        this.lstRowgroupColsAndOrders=lstRowgroupColsAndOrders;
    }

    public List<ColBean> getLstRoworderValueCols()
    {
        return lstRoworderValueCols;
    }

    public void setLstRoworderValueCols(List<ColBean> lstRoworderValueCols)
    {
        this.lstRoworderValueCols=lstRoworderValueCols;
    }

    public AbsListReportFilterBean getFilterBeanById(String filterid)
    {
        if(mAllFilterBeans==null) return null;
        return mAllFilterBeans.get(filterid);
    }

    public void addFilterBean(AbsListReportFilterBean filterBean)
    {
        if(filterBean==null) return;
        if(mAllFilterBeans==null)
        {
            mAllFilterBeans=new HashMap<String,AbsListReportFilterBean>();
        }
        mAllFilterBeans.put(filterBean.getId(),filterBean);
    }

    public int getTreeborder()
    {
        return treeborder;
    }

    public void setTreeborder(int treeborder)
    {
        this.treeborder=treeborder;
    }

    public boolean isTreecloseable()
    {
        return treecloseable;
    }

    public void setTreecloseable(boolean treecloseable)
    {
        this.treecloseable=treecloseable;
    }

    public int getTreexpandlayer()
    {
        if(!this.treecloseable) return -1;
        return treexpandlayer;
    }

    public void setTreexpandlayer(int treexpandlayer)
    {
        this.treexpandlayer=treexpandlayer;
    }

    public boolean isTreeAsynLoad()
    {
        return isTreeAsynLoad;
    }

    public void setTreeAsynLoad(boolean isTreeAsynLoad)
    {
        this.isTreeAsynLoad=isTreeAsynLoad;
    }

    public List<String> getLstTreeclosedimgs(int layer)
    {
        return lstTreeclosedimgs;
    }

    public void setLstTreeclosedimgs(List<String> lstTreeclosedimgs)
    {
        this.lstTreeclosedimgs=lstTreeclosedimgs;
    }

    public List<String> getLstTreexpandimgs(int layer)
    {
        return lstTreexpandimgs;
    }

    public void setLstTreexpandimgs(List<String> lstTreexpandimgs)
    {
        this.lstTreexpandimgs=lstTreexpandimgs;
    }

    public String getTreeleafimg()
    {
        return treeleafimg;
    }

    public void setTreeleafimg(String treeleafimg)
    {
        this.treeleafimg=treeleafimg;
    }

    public String getTreenodeid()
    {
        return treenodeid;
    }

    public void setTreenodeid(String treenodeid)
    {
        this.treenodeid=treenodeid;
    }

    public String getTreenodename()
    {
        return treenodename;
    }

    public void setTreenodename(String treenodename)
    {
        this.treenodename=treenodename;
    }

    public String getTreenodeparentid()
    {
        return treenodeparentid;
    }

    public void setTreenodeparentid(String treenodeparentid)
    {
        this.treenodeparentid=treenodeparentid;
    }

    public AbsListReportStatiBean getStatibean()
    {
        return statibean;
    }

    public void setStatibean(AbsListReportStatiBean statibean)
    {
        this.statibean=statibean;
    }
    
    public String getMouseoverbgcolor()
    {
        return mouseoverbgcolor;
    }

    public void setMouseoverbgcolor(String mouseoverbgcolor)
    {
        this.mouseoverbgcolor=mouseoverbgcolor;
    }



//        return mCurveColids;




//        mCurveColids=curveColids;


    public void addRowgroupCol(ColBean cbean)
    {
        if(this.lstRowgroupColsColumn==null) this.lstRowgroupColsColumn=new ArrayList<String>();
        if(this.lstRowgroupColsColumn.size()>0
                &&this.lstRowgroupColsColumn.get(this.lstRowgroupColsColumn.size()-1)==null)
        {//已经加载了不参与行分组的<col/>，则后面的<col/>都不能参与行分组
            if(cbean!=null)
            {
                throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                        +"失败，参与行分组的<col/>必须配置在最前面");
            }
        }
        if(cbean==null)
        {
            if(lstRowgroupColsColumn.size()==0
                    ||lstRowgroupColsColumn.get(lstRowgroupColsColumn.size()-1)!=null)
            {
                lstRowgroupColsColumn.add(null);
            }
        }else if(!this.lstRowgroupColsColumn.contains(cbean.getColumn()))
        {
            this.lstRowgroupColsColumn.add(cbean.getColumn());
        }
    }
    public int getRowGroupColsNum()
    {
        if(lstRowgroupColsColumn==null||lstRowgroupColsColumn.size()==0) return 0;
        int cnt=lstRowgroupColsColumn.size();
        while(lstRowgroupColsColumn.get(cnt-1)==null)
        {
            cnt--;
        }
        return cnt;
    }
    
    public void clearChildrenInfo()
    {
        this.lstRowgroupColsColumn=null;
        this.lstRowgroupColsAndOrders=null;
        this.rowgrouptype=0;
        this.statibean=null;

    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportDisplayBean newBean=(AbsListReportDisplayBean)super.clone(owner);
        if(lstRowgroupColsColumn!=null)
        {
            newBean
                    .setLstRowgroupColsColumn((List<String>)((ArrayList<String>)lstRowgroupColsColumn)
                            .clone());
        }
        if(lstRowgroupColsAndOrders!=null)
        {
            newBean
                    .setLstRowgroupColsAndOrders((List<Map<String,String>>)((ArrayList<Map<String,String>>)lstRowgroupColsAndOrders)
                            .clone());
        }
        if(this.statibean!=null)
        {
            newBean.setStatibean((AbsListReportStatiBean)statibean.clone(owner));
        }
        return newBean;
    }

    public void doPostLoad()
    {
        DisplayBean dbean=(DisplayBean)this.getOwner();
        String[] strs=ListReportAssistant.getInstance().calColPosition(null,this,dbean.getLstCols(),null);
        this.defaultFirstColid=strs[0];
        this.defaultLastColId=strs[1];
        this.setDefaultColumnCount(Integer.parseInt(strs[2]));
        AbsListReportColBean alrcbean;
        AbsListReportBean alrbean=(AbsListReportBean)dbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        boolean isFirstCol=true;
        for(ColBean cbean:dbean.getLstCols())
        {
            if(cbean.getDisplaytype().equals(Consts.COL_DISPLAYTYPE_HIDDEN)) continue;
            String borderstyle=cbean.getBorderStylePropertyOnColBean();
            if(borderstyle!=null&&!borderstyle.trim().equals(""))
            {
                cbean.setValuestyleproperty(Tools.mergeHtmlTagPropertyString(cbean.getValuestyleproperty(),"style=\""+borderstyle+"\"",1));
            }
            if(!isFirstCol&&this.getRowGroupColsNum()>0&&this.getRowgrouptype()==2&&alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL)
            {//对于配置了垂直滚动条的树形分组的报表，只能在第一列配置width，其它列均不能配置width，否则可能导致表头数据对不齐
                if(Tools.getPropertyValueByName("width",cbean.getValuestyleproperty(),true)!=null
                        ||Tools.getPropertyValueByName("width",cbean.getLabelstyleproperty(),true)!=null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()
                            +"失败，此报表是配置了scrollheight的树形报表，因此除第一列之外，其它列均不能配置width属性");
                }
            }
            isFirstCol=false;
            alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrcbean==null||alrcbean.getFilterBean()==null) continue;
            alrcbean.getFilterBean().doPostLoad();
        }
        if(this.statibean!=null) this.statibean.doPostLoad();
    }
}
