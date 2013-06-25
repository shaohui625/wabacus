/* 
 * Copyright (C) 2010---2013 星星(wuweixing)<349446658@qq.com>
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

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;

public class SqlBean extends AbsConfigBean
{
    public final static int STMTYPE_STATEMENT=1;
    
    public final static int STMTYPE_PREPAREDSTATEMENT=2;
    
    private int stmttype=STMTYPE_STATEMENT;

    private String datasource;//此报表所使用的数据源，默认为wabacus.cfg.xml中<datasources/>标签中的default属性配置的值

//    private Object searchTemplateObj;//搜索栏的显示模板，可能是字符串或TemplateBean
    
    private List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();

    private Map<String,ConditionBean> mConditions;//运行时由lstConditions生成，方便根据<condition/>的name属性取到对应的ConditionBean对象
    
    private List<String> lstConditionFromRequestNames;
    
    private String beforeSearchMethod;
    
    private List<ReportDataSetBean> lstDatasetBeans;//存放所有<value/>子标签，存放顺序为它们的执行顺序
    
    private Map<String,ReportDataSetBean> mDatasetBeans;
    
    public SqlBean(AbsConfigBean parent)
    {
        super(parent);
    }

    public int getStatementType()
    {
        return this.stmttype;
    }
    
    public String getBeforeSearchMethod()
    {
        return beforeSearchMethod;
    }

    public void setBeforeSearchMethod(String beforeSearchMethod)
    {
        this.beforeSearchMethod=beforeSearchMethod;
    }
    
    private String statementTypeName;
    
    public String getStatementTypeName()
    {
        return statementTypeName;
    }


    public void setStatementType(String statementtype)
    {
        if(statementtype==null) return;
        statementtype=statementtype.toLowerCase().trim();
        statementTypeName = statementtype;
        if(statementtype.equals("statement"))
        {
            this.stmttype=STMTYPE_STATEMENT;
        }else if(statementtype.equals("preparedstatement"))
        {
            this.stmttype=STMTYPE_PREPAREDSTATEMENT;
        }
    }

    public List<ConditionBean> getLstConditions()
    {
        return lstConditions;
    }

    public void setLstConditions(List<ConditionBean> lstConditions)
    {
        this.lstConditions=lstConditions;
    }

    public String getDatasource()
    {
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }
    
    public List<ReportDataSetBean> getLstDatasetBeans()
    {
        return lstDatasetBeans;
    }

    public void setLstDatasetBeans(List<ReportDataSetBean> lstDatasetBeans)
    {
        this.lstDatasetBeans=lstDatasetBeans;
    }

    public ReportDataSetBean getDatasetBeanById(String datasetid)
    {
        if(this.mDatasetBeans==null||this.mDatasetBeans.size()==0)
        {
            this.mDatasetBeans=constructMSqlValueBeans();
        }
        if((datasetid==null||datasetid.trim().equals(""))&&this.lstDatasetBeans!=null&&this.lstDatasetBeans.size()==1)
        {
            return this.lstDatasetBeans.get(0);
        }
        if(this.mDatasetBeans==null) return null;
        return this.mDatasetBeans.get(datasetid);
    }
    
    public boolean isIndependentDataset(String datasetid)
    {
        if(this.mDatasetBeans==null||this.mDatasetBeans.size()==0)
        {
            this.mDatasetBeans=constructMSqlValueBeans();
        }
        if(datasetid==null||datasetid.trim().equals("")) return true;
        if(this.mDatasetBeans==null||this.mDatasetBeans.get(datasetid)==null) return true;
        return this.mDatasetBeans.get(datasetid).isIndependentDataSet();
    }
    
    public List<String> getLstConditionFromUrlNames()
    {
        if(lstConditionFromRequestNames==null&&lstConditions!=null&&lstConditions.size()>0)
        {
            List<String> lstConditionFromRequestNamesTmp=new ArrayList<String>();
            for(ConditionBean cbeanTmp:lstConditions)
            {
                if(cbeanTmp==null||cbeanTmp.isConstant()) continue;
                if(cbeanTmp.isConditionValueFromUrl()) lstConditionFromRequestNamesTmp.add(cbeanTmp.getName());
            }
            this.lstConditionFromRequestNames=lstConditionFromRequestNamesTmp;
        }
        return lstConditionFromRequestNames;
    }

    public void setLstConditionFromRequestNames(List<String> lstConditionFromRequestNames)
    {
        this.lstConditionFromRequestNames=lstConditionFromRequestNames;
    }

    public ConditionBean getConditionBeanByName(String name)
    {
        if(name==null||name.trim().equals("")) return null;
        if(this.lstConditions==null||this.lstConditions.size()==0) return null;
        if(this.mConditions==null)
        {
            Map<String,ConditionBean> mConditionsTmp=new HashMap<String,ConditionBean>();
            for(ConditionBean cbTmp:lstConditions)
            {
                mConditionsTmp.put(cbTmp.getName(),cbTmp);
            }
            this.mConditions=mConditionsTmp;
        }
        return this.mConditions.get(name);
    }

    public void initConditionValues(ReportRequest rrequest)
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return;
        for(ConditionBean cbean:lstConditions)
        {
            cbean.initConditionValueByInitMethod(rrequest);
        }
    }
    
    public boolean isExistConditionWithInputbox(ReportRequest rrequest)
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return false;
        for(ConditionBean cbeanTmp:this.lstConditions)
        {
            if(!cbeanTmp.isConditionWithInputbox()) continue;
            if(rrequest!=null//如果是在运行时判断，且当前查询条件被授权为不显示
                    &&!rrequest.checkPermission(this.getReportBean().getId(),Consts.SEARCH_PART,cbeanTmp.getName(),Consts.PERMISSION_TYPE_DISPLAY))
                continue;
            return true;
        }
        return false;
    }
    
    public List<ConditionBean> getLstDisplayConditions(ReportRequest rrequest)
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return null;
        List<ConditionBean> lstConditionsResult=new ArrayList<ConditionBean>();
        for(ConditionBean cbeanTmp:this.lstConditions)
        {
            if(!cbeanTmp.isConditionWithInputbox()) continue;
            if(rrequest!=null
                    &&!rrequest.checkPermission(this.getReportBean().getId(),Consts.SEARCH_PART,cbeanTmp.getName(),Consts.PERMISSION_TYPE_DISPLAY))
                continue;
            lstConditionsResult.add(cbeanTmp);
        }
        return lstConditions;
    }
    
    public void afterLoad()
    {
        for(ReportDataSetBean svbTmp:lstDatasetBeans)
        {
            svbTmp.afterLoad();
        }
        List<ReportDataSetBean> lstResults=new ArrayList<ReportDataSetBean>();
        List<String> lstProcessedValueIds=new ArrayList<String>();
        ReportDataSetBean svbeanTmp;
        List<ReportDataSetBean> lstTmp=new ArrayList<ReportDataSetBean>();
        while(this.lstDatasetBeans.size()>0)
        {
            for(int i=0;i<this.lstDatasetBeans.size();i++)
            {
                svbeanTmp=this.lstDatasetBeans.get(i);
                if(svbeanTmp.getMDependParents()==null||svbeanTmp.getMDependParents().size()==0
                        ||hasProcessedAllParentValues(svbeanTmp,lstProcessedValueIds))
                {
                    if(svbeanTmp.getId()!=null&&!svbeanTmp.getId().trim().equals("")) lstProcessedValueIds.add(svbeanTmp.getId());
                    lstResults.add(svbeanTmp);
                }else
                {
                    lstTmp.add(svbeanTmp);
                }
            }
            if(lstTmp.size()==this.lstDatasetBeans.size())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的<sql/>中的<value/>配置失败，存在循环依赖或者依赖的父数据集ID不存在的配置");
            }
            this.lstDatasetBeans=lstTmp;
            lstTmp.clear();
        }
        this.lstDatasetBeans=lstResults;
    }
    
    private boolean hasProcessedAllParentValues(ReportDataSetBean svbeanTmp,List<String> lstProcessedValueIds)
    {
        if(svbeanTmp.getMDependParents()==null||svbeanTmp.getMDependParents().size()==0) return true;
        if(lstProcessedValueIds==null||lstProcessedValueIds.size()==0) return false;
        List<String> lstParentValueids=svbeanTmp.getAllParentValueIds();
        for(String parentValueidTmp:lstParentValueids)
        {
            if(!lstProcessedValueIds.contains(parentValueidTmp)) return false;
        }
        return true;
    }

    public void doPostLoad()
    {
        this.mDatasetBeans=constructMSqlValueBeans();
        if(this.lstConditions==null||this.lstConditions.size()==0) return;
        List<String> lstTmp=new ArrayList<String>();
        for(ConditionBean cbTmp:lstConditions)
        {
            if(cbTmp==null||cbTmp.getName()==null) continue;
            if(lstTmp.contains(cbTmp.getName()))
            {
                throw new WabacusConfigLoadingException("报表 "+this.getReportBean().getPath()+"配置的查询条件name:"+cbTmp.getName()+"存在重复，必须确保唯一");
            }
            lstTmp.add(cbTmp.getName());
            cbTmp.doPostLoad();
        }
        for(ReportDataSetBean svbeanTmp:this.lstDatasetBeans)
        {
            svbeanTmp.doPostLoad();
        }        
    }

    private Map<String,ReportDataSetBean> constructMSqlValueBeans()
    {
        Map<String,ReportDataSetBean> mSqlValueBeans=null;
        if(this.lstDatasetBeans!=null&&this.lstDatasetBeans.size()>0)
        {
            mSqlValueBeans=new HashMap<String,ReportDataSetBean>();
            for(ReportDataSetBean svbeanTmp:this.lstDatasetBeans)
            {
                if(svbeanTmp.getId()!=null&&!svbeanTmp.getId().trim().equals("")) mSqlValueBeans.put(svbeanTmp.getId(),svbeanTmp);
            }
        }
        return mSqlValueBeans;
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        SqlBean sbeanNew=(SqlBean)super.clone(parent);
        ((ReportBean)parent).setSbean(sbeanNew);
        sbeanNew.setLstConditions(ComponentConfigLoadAssistant.getInstance().cloneLstConditionBeans(sbeanNew,lstConditions));
        if(lstConditionFromRequestNames!=null)
        {
            sbeanNew.setLstConditionFromRequestNames((List<String>)((ArrayList<String>)lstConditionFromRequestNames).clone());
        }
        if(lstDatasetBeans!=null)
        {
            List<ReportDataSetBean> lstSqlValueBeansNew=new ArrayList<ReportDataSetBean>();
            for(ReportDataSetBean svbeanTmp:lstDatasetBeans)
            {
                lstSqlValueBeansNew.add(svbeanTmp.clone(sbeanNew));
            }
            sbeanNew.setLstDatasetBeans(lstSqlValueBeansNew);
        }
        this.mDatasetBeans=null;
        cloneExtendConfig(sbeanNew);
        return sbeanNew;
    }

    
}
