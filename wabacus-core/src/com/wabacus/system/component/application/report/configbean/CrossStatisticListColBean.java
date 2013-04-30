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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.CrossStatisticListReportType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class CrossStatisticListColBean extends AbsExtendConfigBean implements ICrossStatisticColAndGroup
{
    private boolean shouldShowStaticLabel=false;

    private String condition;

    private int rowspan=1;//当本统计<col/>下面的统计的标题需要单独显示一行时，此<col/>即相当于一个<group/>，此时可以通过labelstyleproperty指定它的标题占据的行数

    private String realvalue;

    private String tablename;//如果当前<col/>是配置的顶层交叉列，即不属于任意<group/>的，且当前交叉动态列是从一个表中获取，则可以通过其tablename属性配置此表名，这样可以提高查询效率。

    private List<ConditionBean> lstTablenameConditions;

    private boolean verticalstatistic;//是否进行纵向统计

    private String verticallabel;

    private String verticallabelstyleproperty;

    private List<CrossStatisticListStatisBean> lstStatisBeans;//配置的<statistic/>对象集合

    private List<CrossStatisticListStatisDisplayBean> lstDisplayStatisBeans;//本<col/>下要显示的统计项对象，在doPostLoad()方法中构造

    private CrossStatisticListGroupBean parentCrossStatiGroupBean;

    public CrossStatisticListColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public boolean isShouldShowStaticLabel()
    {
        return shouldShowStaticLabel;
    }

    public void setShouldShowStaticLabel(boolean shouldShowStaticLabel)
    {
        this.shouldShowStaticLabel=shouldShowStaticLabel;
    }

    public void setCondition(String condition)
    {
        this.condition=condition;
    }

    public String getRealvalue()
    {
        return realvalue;
    }

    public void setRealvalue(String realvalue)
    {
        this.realvalue=realvalue;
    }

    public int getRowspan()
    {
        return rowspan;
    }

    public void setRowspan(int rowspan)
    {
        if(rowspan<=0) rowspan=1;
        this.rowspan=rowspan;
    }

    public List<CrossStatisticListStatisBean> getLstStatisBeans()
    {
        return lstStatisBeans;
    }

    public String getTablename()
    {
        return tablename;
    }

    public void setTablename(String tablename)
    {
        this.tablename=tablename;
    }

    public List<ConditionBean> getLstTablenameConditions()
    {
        return lstTablenameConditions;
    }

    public void setLstTablenameConditions(List<ConditionBean> lstTablenameConditions)
    {
        this.lstTablenameConditions=lstTablenameConditions;
    }

    public boolean isVerticalstatistic()
    {
        return verticalstatistic;
    }

    public void setVerticalstatistic(boolean verticalstatistic)
    {
        this.verticalstatistic=verticalstatistic;
    }

    public String getVerticallabel()
    {
        return verticallabel;
    }

    public void setVerticallabel(String verticallabel)
    {
        this.verticallabel=verticallabel;
    }

    public String getVerticallabelstyleproperty()
    {
        return verticallabelstyleproperty;
    }

    public void setVerticallabelstyleproperty(String verticallabelstyleproperty)
    {
        this.verticallabelstyleproperty=verticallabelstyleproperty;
    }

    public void setLstStatisBeans(List<CrossStatisticListStatisBean> lstStatisBeans)
    {
        this.lstStatisBeans=lstStatisBeans;
    }

    public List<CrossStatisticListStatisDisplayBean> getLstDisplayStatisBeans()
    {
        return lstDisplayStatisBeans;
    }

    public void setLstDisplayStatisBeans(List<CrossStatisticListStatisDisplayBean> lstDisplayStatisBeans)
    {
        this.lstDisplayStatisBeans=lstDisplayStatisBeans;
    }

    public CrossStatisticListGroupBean getParentCrossStatiGroupBean()
    {
        return parentCrossStatiGroupBean;
    }

    public void setParentCrossStatiGroupBean(CrossStatisticListGroupBean parentCrossStatiGroupBean)
    {
        this.parentCrossStatiGroupBean=parentCrossStatiGroupBean;
    }

    public String getColumn()
    {
        return ((ColBean)this.getOwner()).getColumn();
    }

    public int createDisplayBean(ReportRequest rrequest,ResultSet rs,StringBuffer dynselectedColsBuf,StringBuffer conditionBuf,
            StringBuffer allColConditionsBuf,DisplayBean disbean,List lstChildren,Object headDataObj,int colidx,
            Map<String,String> mAllColConditionsInGroup,Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions) throws SQLException
    {
        String column=((ColBean)this.getOwner()).getColumn();
        String colRealvalue=this.realvalue;
        if(colRealvalue==null||colRealvalue.trim().equals("")) colRealvalue=column;
        String conditionTmp=condition;
        if(!colRealvalue.equals(column)) conditionTmp=Tools.replaceAll(conditionTmp,column,colRealvalue);
        String colvalue=rs.getString(column);
        colvalue=colvalue==null?"":colvalue.trim();
        conditionTmp=Tools.replaceAll(conditionTmp,"#data#",colvalue);
        conditionBuf.append(conditionTmp);
        if(allColConditionsBuf!=null)
        {//收集所有生成<col/>对应的条件，稍后针对整个报表的横向统计会用上
            allColConditionsBuf.append("(").append(conditionBuf.toString()).append(") or ");
        }
        if(mAllColConditionsInGroup!=null)
        {//需要收集各分组包括的所有<col/>对应的条件
            CrossStatisticListGroupBean parentGroupBean=this.parentCrossStatiGroupBean;
            while(parentGroupBean!=null)
            {//给所有包含此列的分组加上此列的条件，这样统计相应分组时，就可以精确包含要统计的条件
                String colConditions=mAllColConditionsInGroup.get(parentGroupBean.getColumn());
                if(colConditions==null) colConditions="";
                colConditions=colConditions+"("+conditionBuf.toString()+") or ";
                mAllColConditionsInGroup.put(parentGroupBean.getColumn(),colConditions);
                parentGroupBean=parentGroupBean.getParentCrossStatiGroupBean();
            }
        }
        if(mStaticColAndGroupTitlePostions.get(column).getDisplaymode()<=0) return colidx;//没有显示权限，则不用构造相应的col/group
        if(this.isShouldShowStaticLabel())
        {
            UltraListReportGroupBean groupBean=new UltraListReportGroupBean(disbean,CrossStatisticListReportType.COL_GROUP_STARTID+colidx);
            if(headDataObj!=null)
            {
                groupBean.setLabel("_"+column+"_"+colidx);
                ReportAssistant.getInstance().setCrossStatisDataToPOJO(disbean.getReportBean(),headDataObj,"_"+column+"_"+colidx,colvalue);
            }else
            {
                groupBean.setLabel(colvalue);
            }
            groupBean.setLabelstyleproperty(((ColBean)this.getOwner()).getLabelstyleproperty());
            groupBean.setRowspan(this.rowspan);
            colidx++;
            List lstColChildren=new ArrayList();
            groupBean.setLstChildren(lstColChildren);
            ColAndGroupTitlePositionBean cgpositionBeanTmp;

            for(CrossStatisticListStatisDisplayBean statisdBeanTmp:this.lstDisplayStatisBeans)
            {//为每一个统计显示一个标题列，并且为它拼凑上查询的字段
                cgpositionBeanTmp=mStaticColAndGroupTitlePostions.get(statisdBeanTmp.getStatiBean().getId());
                if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;
                ColBean cbTmp=new ColBean(disbean,CrossStatisticListReportType.COL_GROUP_STARTID+colidx);
                
               // cbTmp.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcbeanTmp);//必须为它生成此对象
                
                cbTmp.setLabel(rrequest.getI18NStringValue(statisdBeanTmp.getLabel()));
                cbTmp.setLabelstyleproperty(statisdBeanTmp.getLabelstyleproperty());
                cbTmp.setValuestyleproperty(statisdBeanTmp.getValuestyleproperty());
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL)
                {
                    cbTmp.setDatatypeObj(Config.getInstance().getDataTypeByClass(DoubleType.class));
                }else
                {
                    cbTmp.setDatatypeObj(Config.getInstance().getDataTypeByClass(VarcharType.class));
                }
                lstColChildren.add(cbTmp);
                cbTmp.setProperty("[DYN_STATISTIC_DATA]");
                cbTmp.setColumn("column_"+colidx);
                dynselectedColsBuf.append(statisdBeanTmp.getStatiBean().getType()+"(");
                dynselectedColsBuf.append("case when ").append(conditionBuf.toString()).append(" then ").append(
                        statisdBeanTmp.getStatiBean().getColumn()).append("  end ");
                dynselectedColsBuf.append(") as ").append("column_"+colidx).append(",");
                colidx++;
            }
            lstChildren.add(groupBean);
        }else
        {//不用为统计数据显示标题
            CrossStatisticListStatisDisplayBean statisdBeanTmp=this.lstDisplayStatisBeans.get(0);
            ColBean cbTmp=new ColBean(disbean,CrossStatisticListReportType.COL_GROUP_STARTID+colidx);
            if(headDataObj!=null)
            {
                cbTmp.setLabel("_"+column+"_"+colidx);
                ReportAssistant.getInstance().setCrossStatisDataToPOJO(disbean.getReportBean(),headDataObj,"_"+column+"_"+colidx,colvalue);
            }else
            {
                cbTmp.setLabel(colvalue);
            }
            
            
            
            cbTmp.setLabelstyleproperty(statisdBeanTmp.getLabelstyleproperty());
            cbTmp.setValuestyleproperty(statisdBeanTmp.getValuestyleproperty());
            lstChildren.add(cbTmp);
            cbTmp.setProperty("[DYN_STATISTIC_DATA]");
            cbTmp.setColumn("column_"+colidx);
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL)
            {
                cbTmp.setDatatypeObj(Config.getInstance().getDataTypeByClass(DoubleType.class));
            }else
            {
                cbTmp.setDatatypeObj(Config.getInstance().getDataTypeByClass(VarcharType.class));
            }
            dynselectedColsBuf.append(statisdBeanTmp.getStatiBean().getType()+"(");
            dynselectedColsBuf.append("case when ").append(conditionBuf.toString()).append(" then ")
                    .append(statisdBeanTmp.getStatiBean().getColumn()).append("  end ");
            dynselectedColsBuf.append(") as ").append("column_"+colidx).append(",");
            colidx++;
        }
        return colidx;
    }

    public void initStatisDisplayBean(CrossStatisticListStatisBean statisBean,List<String> lstStatitems)
    {
        String column=((ColBean)this.getOwner()).getColumn();
        if(statisBean.getLstStatitems()==null||statisBean.getLstStatitems().size()==0//没有配置statitems，则默认就统计最里层的<col/>
                ||statisBean.getLstStatitems().contains(column))
        {//此统计需要针对此分组进行横向统计
            CrossStatisticListStatisDisplayBean cslsdbean=new CrossStatisticListStatisDisplayBean();
            cslsdbean.setStatiBean(statisBean);
            if(statisBean.getLstStatitems()==null||statisBean.getLstStatitems().size()==0)
            {//没有配置statitems，则只统计最里层的<col/>，所以取label、labelstyleproperty、valuestyleproperty等时只取第一个，即使配置了多个
                cslsdbean.setLabel(statisBean.getLabel(null));
                cslsdbean.setLabelstyleproperty(statisBean.getLabelstyleproperty(null));
                cslsdbean.setValuestyleproperty(statisBean.getValuestyleproperty(null));
            }else
            {
                cslsdbean.setLabel(statisBean.getLabel(column));
                cslsdbean.setLabelstyleproperty(statisBean.getLabelstyleproperty(column));
                cslsdbean.setValuestyleproperty(statisBean.getValuestyleproperty(column));
            }
            if(this.lstDisplayStatisBeans==null) this.lstDisplayStatisBeans=new ArrayList<CrossStatisticListStatisDisplayBean>();
            this.lstDisplayStatisBeans.add(cslsdbean);
            if(lstStatitems!=null)
            {
                while(lstStatitems.contains(column))
                {
                    lstStatitems.remove(column);
                }
            }
        }
    }

    public CrossStatisticListColBean getStatisColBean()
    {
        return this;
    }

    public void calPositionStart(ReportRequest rrequest,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(this.lstStatisBeans==null||this.lstStatisBeans.size()==0) return;
        ColBean cbeanOwner=(ColBean)this.getOwner();
        boolean isAllStatisticItemsHidden=true;
        for(CrossStatisticListStatisBean cslsbeanTmp:this.lstStatisBeans)
        {
            ColAndGroupTitlePositionBean positionBeanTmp=mColAndGroupTitlePostions.get(cslsbeanTmp.getId());
            if(positionBeanTmp==null)
            {
                positionBeanTmp=new ColAndGroupTitlePositionBean();
                mColAndGroupTitlePostions.put(cslsbeanTmp.getId(),positionBeanTmp);
            }
            if(rrequest!=null
                    &&!rrequest.checkPermission(cbeanOwner.getReportBean().getId(),Consts.DATA_PART,cslsbeanTmp.getId(),
                            Consts.PERMISSION_TYPE_DISPLAY))
            {
                positionBeanTmp.setDisplaymode(-1);
            }else
            {
                positionBeanTmp.setDisplaymode(1);
                isAllStatisticItemsHidden=false;
            }
        }
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(cbeanOwner.getColumn());
        if(positionBean==null)
        {
            positionBean=new ColAndGroupTitlePositionBean();
            mColAndGroupTitlePostions.put(cbeanOwner.getColumn(),positionBean);
        }
        positionBean.setDisplaymode(-1);
        if(isAllStatisticItemsHidden) return;//没有一个统计项有显示权限
        if(this.lstDisplayStatisBeans==null||this.lstDisplayStatisBeans.size()==0) return;//没有针对本<col/>的统计
        if(rrequest!=null
                &&!rrequest
                        .checkPermission(cbeanOwner.getReportBean().getId(),Consts.DATA_PART,cbeanOwner.getColumn(),Consts.PERMISSION_TYPE_DISPLAY))
            return; 
        for(CrossStatisticListStatisDisplayBean cslsdBeanTmp:this.lstDisplayStatisBeans)
        {
            if(mColAndGroupTitlePostions.get(cslsdBeanTmp.getStatiBean().getId()).getDisplaymode()>0)
            {
                positionBean.setDisplaymode(1);
                break;
            }
        }
    }

    public void calPositionEnd(Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int maxrowspan)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.getColumn());
        if(positionBean==null||positionBean.getDisplaymode()<=0) return;
        if(!this.isShouldShowStaticLabel())
        {
            positionBean.setRowspan(maxrowspan);
            return;
        }
        positionBean.setRowspan(rowspan);
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(CrossStatisticListStatisDisplayBean cslsBeanTmp:this.lstDisplayStatisBeans)
        {
            positionBeanTmp=mColAndGroupTitlePostions.get(cslsBeanTmp.getStatiBean().getId());
            if(positionBeanTmp.getDisplaymode()>0)
            {
                positionBeanTmp.setRowspan(maxrowspan-rowspan);
            }
        }
    }

    public boolean isAllStatisticItemsHidden(Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(mColAndGroupTitlePostions==null) return false;
        if(this.lstStatisBeans==null||this.lstStatisBeans.size()==0) return true;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(CrossStatisticListStatisBean cslsbeanTmp:this.lstStatisBeans)
        {
            positionBeanTmp=mColAndGroupTitlePostions.get(cslsbeanTmp.getId());
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()>0) return false;
        }
        return true;
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        CrossStatisticListColBean newBean=(CrossStatisticListColBean)super.clone(owner);
        if(lstStatisBeans!=null)
        {
            List<CrossStatisticListStatisBean> lstStatisBeansNew=new ArrayList<CrossStatisticListStatisBean>();
            for(CrossStatisticListStatisBean beanTmp:lstStatisBeans)
            {
                lstStatisBeansNew.add((CrossStatisticListStatisBean)beanTmp.clone(owner));
            }
            newBean.setLstStatisBeans(lstStatisBeansNew);
        }
        if(lstTablenameConditions!=null)
        {//因为在doPostLoad()方法要修改这里的condition，成其是配置了ref属性时要修改，因此为了防止与父报表共用一份ConditionBean导致修改时相互覆盖，这里要为子报表重新生成一份
            List<ConditionBean> lstTablenameConditionsNew=new ArrayList<ConditionBean>();
            for(ConditionBean cbTmp:lstTablenameConditions)
            {
                lstTablenameConditionsNew.add((ConditionBean)cbTmp.clone(null));
            }
            newBean.setLstTablenameConditions(lstTablenameConditionsNew);
        }
        return newBean;
    }
}
