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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class AutoCompleteConfigBean implements Cloneable
{
    private AbsInputBox owner;
    
    private UniqueArrayList<String> ulstCompleteColumns;
    
    private String autoCompleteColumns;
    
    private List<ColBean> lstCompleteColBeans;
    
    private boolean isEnableMultipleRecords;//当用户输入的数据匹配多条记录时，是否要自动获取第一条记录进行填充
    
    private String colConditionExpression;//用<col/>的值做为条件表达式
    
    private List<String> lstColPropertiesInCondition;
    
    private UniqueArrayList<String> ulstRefConditionNames;
    
    private String autocompletesql;

    public boolean isEnableMultipleRecords()
    {
        return isEnableMultipleRecords;
    }

    public void setEnableMultipleRecords(boolean isEnableMultipleRecords)
    {
        this.isEnableMultipleRecords=isEnableMultipleRecords;
    }

    public String getAutoCompleteColumns()
    {
        return autoCompleteColumns;
    }

    public UniqueArrayList<String> getUlstCompleteColumns()
    {
        return ulstCompleteColumns;
    }

    public void setUlstCompleteColumns(List<String> lstCompleteColumns)
    {
        if(lstCompleteColumns==null)
        {
            this.ulstCompleteColumns=null;
        }else
        {
            this.ulstCompleteColumns=new UniqueArrayList<String>();
            this.ulstCompleteColumns.addAll(lstCompleteColumns);
        }
    }

    public String getColConditionExpression()
    {
        return colConditionExpression;
    }

    public void setColConditionExpression(String colConditionExpression)
    {
        this.colConditionExpression=colConditionExpression;
    }

    public List<ColBean> getLstCompleteColBeans()
    {
        return lstCompleteColBeans;
    }

    public void setLstCompleteColBeans(List<ColBean> lstCompleteColBeans)
    {
        this.lstCompleteColBeans=lstCompleteColBeans;
    }

    public List<String> getLstColPropertiesInCondition()
    {
        return lstColPropertiesInCondition;
    }

    public void setLstColPropertiesInCondition(List<String> lstColPropertiesInCondition)
    {
        this.lstColPropertiesInCondition=lstColPropertiesInCondition;
    }

    public UniqueArrayList<String> getUlstRefConditionNames()
    {
        return ulstRefConditionNames;
    }

    public void setUlstRefConditionNames(Collection<String> cRefConditionNames)
    {
        if(cRefConditionNames==null)
        {
            this.ulstRefConditionNames=null;
        }else
        {
            this.ulstRefConditionNames=new UniqueArrayList<String>();
            this.ulstRefConditionNames.addAll(cRefConditionNames);
        }
    }

    public String getAutocompletesql()
    {
        return autocompletesql;
    }

    public void setAutocompletesql(String autocompletesql)
    {
        this.autocompletesql=autocompletesql;
    }

    public AbsInputBox getOwner()
    {
        return owner;
    }

    public boolean isConstainsCondition(String conditioname)
    {
        if(ulstRefConditionNames==null) return false;
        return ulstRefConditionNames.contains(conditioname);
    }
    
    public boolean hasRefCondition()
    {
        return ulstRefConditionNames!=null&&ulstRefConditionNames.size()>0;
    }
    
    public String getRealColConditionExpression(Map<String,String> mColConditionValues)
    {
        if(mColConditionValues==null||mColConditionValues.size()==0) return "";
        String realColConditionExpression=this.colConditionExpression;
        String colValTmp;
        for(String colpropertyTmp:this.lstColPropertiesInCondition)
        {
            colValTmp=mColConditionValues.get(colpropertyTmp);
            if(colValTmp==null) colValTmp="";
            realColConditionExpression=Tools.replaceAll(realColConditionExpression,"#"+colpropertyTmp+"#",colValTmp);
        }
        return realColConditionExpression;
    }
    
    protected AutoCompleteConfigBean clone() throws CloneNotSupportedException
    {
        AutoCompleteConfigBean newBean=(AutoCompleteConfigBean)super.clone();
        if(ulstCompleteColumns!=null)
        {
            newBean.setUlstCompleteColumns(ulstCompleteColumns.clone());
        }
        
        return newBean;
    }
    
    public void doPostLoad(AbsInputBox owner)
    {
        this.owner=owner;
        if(ulstCompleteColumns==null||ulstCompleteColumns.size()==0||!(owner.getOwner() instanceof EditableReportColBean)
                ||!(Config.getInstance().getReportType(owner.getOwner().getReportBean().getType()) instanceof IEditableReportType))
        {//没有配置自动填充其它列数据，或者不是编辑列的输入框，或者不是可编辑报表类型
            owner.setAutocompleteBean(null);
            return;
        }
        processAutoCompleteCols();
        processColCondition();
        processRefConditions();
    }

    private void processAutoCompleteCols()
    {
        DisplayBean dbean=owner.getOwner().getReportBean().getDbean();
        this.lstCompleteColBeans=new ArrayList<ColBean>();
        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
        ColBean cbTmp;
        EditableReportColBean ercbeanTmp;
        for(String columnTmp:this.ulstCompleteColumns)
        {
            cbTmp=dbean.getColBeanByColColumn(columnTmp);
            if(cbTmp==null||cbTmp.isControlCol()||cbTmp.getProperty()==null||cbTmp.getProperty().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"的列"+cbOwner.getColumn()+"失败，为它配置的自动填充列"+columnTmp
                        +"不存在或不是有效填充列");
            }
            this.lstCompleteColBeans.add(cbTmp);
            ercbeanTmp=(EditableReportColBean)cbTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
            if(ercbeanTmp==null) continue;
            if(ercbeanTmp.getUpdatecol()!=null&&!ercbeanTmp.getUpdatecol().trim().equals(""))
            {
                String updatecolumn=dbean.getColBeanByColProperty(ercbeanTmp.getUpdatecol()).getColumn();
                if(!this.ulstCompleteColumns.contains(updatecolumn))
                {
                    this.lstCompleteColBeans.add(dbean.getColBeanByColProperty(ercbeanTmp.getUpdatecol()));
                }
            }
            if(ercbeanTmp.getUpdatedcol()!=null&&!ercbeanTmp.getUpdatedcol().trim().equals(""))
            {//在自动填充列中指定的是被updatecol更新列，则把源列加进来，因为客户端是填充源列输入框的数据
                String updatedcolumn=dbean.getColBeanByColProperty(ercbeanTmp.getUpdatedcol()).getColumn();
                if(!this.ulstCompleteColumns.contains(updatedcolumn))
                {
                    this.lstCompleteColBeans.add(dbean.getColBeanByColProperty(ercbeanTmp.getUpdatedcol()));
                }
            }
        }
        if(!ulstCompleteColumns.contains(cbOwner.getColumn()))
        {
            this.lstCompleteColBeans.add(cbOwner);
            ulstCompleteColumns.add(cbOwner.getColumn());
        }
        String updatecolTmp=((EditableReportColBean)owner.getOwner()).getUpdatecol();
        if(updatecolTmp!=null&&!updatecolTmp.trim().equals(""))
        {
            ColBean cbUpdated=dbean.getColBeanByColProperty(updatecolTmp);
            if(!this.ulstCompleteColumns.contains(updatecolTmp))
            {
                this.lstCompleteColBeans.add(cbUpdated);
                this.ulstCompleteColumns.add(cbUpdated.getColumn());
            }
        }
        autoCompleteColumns="";
        for(String colTmp:ulstCompleteColumns)
        {
            if(colTmp==null||colTmp.trim().equals("")) continue;
            autoCompleteColumns+=colTmp+",";
        }
        if(autoCompleteColumns.endsWith(",")) autoCompleteColumns=autoCompleteColumns.substring(0,autoCompleteColumns.length()-1);
        ulstCompleteColumns=null;
    }

    private void processColCondition()
    {
        lstColPropertiesInCondition=new ArrayList<String>();
        DisplayBean dbean=owner.getOwner().getReportBean().getDbean();
        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
        ColBean cbTmp;
        EditableReportColBean ercbeanTmp=null;
        if(colConditionExpression==null||colConditionExpression.trim().equals(""))
        {
            ercbeanTmp=(EditableReportColBean)cbOwner.getExtendConfigDataForReportType(EditableReportColBean.class);
            String column=null;
            boolean isVarcharType;
            if(ercbeanTmp!=null&&ercbeanTmp.getUpdatecol()!=null&&!ercbeanTmp.getUpdatecol().trim().equals(""))
            {//如果当前列是通过updatecol更新其它列，则指定比较字段时是指定updatecol引用的列，因为稍后从页面上取到的值也是它的值
                ColBean cbUpdated=dbean.getColBeanByColProperty(ercbeanTmp.getUpdatecol());
                column=cbUpdated.getColumn();
                isVarcharType=cbUpdated.getDatatypeObj() instanceof VarcharType;
            }else
            {
                column=cbOwner.getColumn();
                isVarcharType=cbOwner.getDatatypeObj() instanceof VarcharType;
            }
            colConditionExpression=column+"=";
            if(isVarcharType) colConditionExpression+="'";
            colConditionExpression=colConditionExpression+"#"+cbOwner.getProperty()+"#";
            if(isVarcharType) colConditionExpression+="'";
            if(!lstColPropertiesInCondition.contains(cbOwner.getProperty())) lstColPropertiesInCondition.add(cbOwner.getProperty());
        }else
        {
            String expressTmp=colConditionExpression;
            String columnTmp,propTmp;
            Map<String,String> mColumnProperty=new HashMap<String,String>();
            while(true)
            {
                int idx=expressTmp.indexOf("#");
                if(idx<0) break;
                expressTmp=expressTmp.substring(idx+1);
                idx=expressTmp.indexOf("#");
                if(idx<=0) break;
                columnTmp=expressTmp.substring(0,idx);
                cbTmp=dbean.getColBeanByColColumn(columnTmp.trim());
                if(cbTmp==null||cbTmp.isControlCol()||cbTmp.getProperty()==null||cbTmp.getProperty().trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"的列"+cbOwner.getColumn()
                            +"失败，为它配置的自动填充列所用做为条件的列"+columnTmp+"不存在或不是有效数据列");
                }
                ercbeanTmp=(EditableReportColBean)cbTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
                if(ercbeanTmp!=null&&ercbeanTmp.getUpdatedcol()!=null&&!ercbeanTmp.getUpdatedcol().trim().equals(""))
                {
                    propTmp=ercbeanTmp.getUpdatedcol();
                }else
                {
                    propTmp=cbTmp.getProperty();
                }
                if(!lstColPropertiesInCondition.contains(propTmp)) lstColPropertiesInCondition.add(propTmp);
                mColumnProperty.put(columnTmp,propTmp);
                expressTmp=expressTmp.substring(idx+1);
            }
            for(Entry<String,String> entryTmp:mColumnProperty.entrySet())
            {//将条件表达式中的#column#替换成#property#
                if(entryTmp.getKey().equals(entryTmp.getValue())) continue;
                colConditionExpression=Tools.replaceAll(colConditionExpression,"#"+entryTmp.getKey()+"#","#"+entryTmp.getValue()+"#");
            }
        }
    }
    
    private void processRefConditions()
    {
        SqlBean sbean=owner.getOwner().getReportBean().getSbean();
        if(sbean==null)
        {
            ulstRefConditionNames=null;
        }else
        {
            if(ulstRefConditionNames!=null&&ulstRefConditionNames.size()>0)
            {
                ConditionBean cbTmp=null;
                for(String refNameTmp:ulstRefConditionNames)
                {
                    cbTmp=sbean.getConditionBeanByName(refNameTmp);
                    if(cbTmp==null)
                    {
                        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
                        throw new WabacusConfigLoadingException("加载报表"+owner.getOwner().getReportBean().getPath()+"的列"+cbOwner.getColumn()
                                +"失败，autocompletecondition中引用的条件"+refNameTmp+"不存在");
                    }
                }
            }
        }
    }
}

