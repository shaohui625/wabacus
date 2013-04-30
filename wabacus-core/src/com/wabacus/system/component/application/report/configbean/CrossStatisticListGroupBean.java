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
import java.util.HashMap;
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

public class CrossStatisticListGroupBean extends UltraListReportGroupBean implements ICrossStatisticColAndGroup
{
    private String column;

    private String realvalue;

    private String condition;

    private String tablename;//如果当前<group/>是配置的顶层交叉分组，即不属于任意<group/>的，且当前交叉动态列是从一个表中获取，则可以通过其tablename属性配置此表名，这样可以提高查询效率。

    private List<ConditionBean> lstTablenameConditions;

    private Object childObj;//包含的子标签对象，是ColBean、CrossStatisticListGroupBean两种类型的一种

    private List<CrossStatisticListStatisDisplayBean> lstDisplayStatisBeans;//针对本<group/>进行统计的统计项对象，在doPostLoad()方法中构造

    private CrossStatisticListGroupBean parentCrossStatiGroupBean;

    public CrossStatisticListGroupBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public CrossStatisticListGroupBean(AbsConfigBean owner,int groupid)
    {
        super(owner,groupid);
    }

    public String getColumn()
    {
        return column;
    }

    public void setColumn(String column)
    {
        this.column=column;
    }

    public String getRealvalue()
    {
        return realvalue;
    }

    public void setRealvalue(String realvalue)
    {
        this.realvalue=realvalue;
    }

    public void setCondition(String condition)
    {
        this.condition=condition;
    }

    public String getTablename()
    {
        return tablename;
    }

    public void setTablename(String tablename)
    {
        this.tablename=tablename;
    }

    public Object getChildObj()
    {
        return childObj;
    }

    public void setChildObj(Object childObj)
    {
        this.childObj=childObj;
    }

    public List<ConditionBean> getLstTablenameConditions()
    {
        return lstTablenameConditions;
    }

    public void setLstTablenameConditions(List<ConditionBean> lstTablenameConditions)
    {
        this.lstTablenameConditions=lstTablenameConditions;
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
        if(this.childObj instanceof ColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            cslrcbean.setParentCrossStatiGroupBean(this);
        }else
        {
            ((CrossStatisticListGroupBean)childObj).setParentCrossStatiGroupBean(this);
        }
    }

    public int calPositionStart(ReportRequest rrequest,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.column);
        if(positionBean==null)
        {
            positionBean=new ColAndGroupTitlePositionBean();
            mColAndGroupTitlePostions.put(this.column,positionBean);
        }
        ColAndGroupTitlePositionBean childPositionBean;
        int rowcount=this.getRowspan();
        if(this.childObj instanceof ColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            cslrcbean.calPositionStart(rrequest,mColAndGroupTitlePostions);
            childPositionBean=mColAndGroupTitlePostions.get(((ColBean)this.childObj).getColumn());
            if(childPositionBean.getDisplaymode()>0)
            {
                if(cslrcbean.isShouldShowStaticLabel())
                {
                    rowcount+=cslrcbean.getRowspan();
                }
                rowcount+=1;
            }
        }else
        {
            rowcount+=((CrossStatisticListGroupBean)childObj).calPositionStart(rrequest,mColAndGroupTitlePostions);
            childPositionBean=mColAndGroupTitlePostions.get(((CrossStatisticListGroupBean)this.childObj).getColumn());
        }
        if(childPositionBean.getDisplaymode()>0)
        {
            positionBean.setDisplaymode(1);
        }else
        {
            boolean hasDisplayStatisChild=false;
            if(this.lstDisplayStatisBeans!=null&&this.lstDisplayStatisBeans.size()>0)
            {//有针对本分组的统计
                for(CrossStatisticListStatisDisplayBean cslsdbeanTmp:this.lstDisplayStatisBeans)
                {
                    if(mColAndGroupTitlePostions.get(cslsdbeanTmp.getStatiBean().getId()).getDisplaymode()>0)
                    {
                        positionBean.setDisplaymode(1);
                        rowcount=rowcount+1;
                        hasDisplayStatisChild=true;
                        break;
                    }
                }
            }
            if(!hasDisplayStatisChild)
            {
                positionBean.setDisplaymode(-1);
                rowcount=0;
            }
        }
        return rowcount;
    }

    public void calPositionEnd(Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int maxrowspan)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.column);
        if(positionBean==null||positionBean.getDisplaymode()<=0) return;
        positionBean.setRowspan(getRowspan());
        if(this.childObj instanceof ColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            cslrcbean.calPositionEnd(mColAndGroupTitlePostions,maxrowspan-getRowspan());
        }else
        {
            ((CrossStatisticListGroupBean)childObj).calPositionEnd(mColAndGroupTitlePostions,maxrowspan-getRowspan());
        }
    }

    public void getSelectCols(StringBuffer[] strcolsBuf)
    {
        if(this.realvalue!=null&&!this.realvalue.trim().equals(""))
        {
            strcolsBuf[0].append(this.realvalue).append(" as ");
        }
        strcolsBuf[0].append(this.column).append(",");
        strcolsBuf[1].append(this.column).append(",");
        if(this.childObj instanceof ColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            if(cslrcbean.getRealvalue()!=null&&!cslrcbean.getRealvalue().trim().equals(""))
            {
                strcolsBuf[0].append(cslrcbean.getRealvalue()).append(" as ");
            }
            strcolsBuf[0].append(((ColBean)childObj).getColumn()).append(",");
            strcolsBuf[1].append(((ColBean)childObj).getColumn()).append(",");
        }else
        {
            getSelectCols(strcolsBuf);
        }
    }

    public void getDynCols(List<Map<String,String>> lstDynCols)
    {
        Map<String,String> mTmp=new HashMap<String,String>();
        lstDynCols.add(mTmp);
        mTmp.put(this.column,this.realvalue);
        if(this.childObj instanceof ColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            mTmp=new HashMap<String,String>();
            mTmp.put(((ColBean)childObj).getColumn(),cslrcbean.getRealvalue());
            lstDynCols.add(mTmp);
        }else
        {
            ((CrossStatisticListGroupBean)this.childObj).getDynCols(lstDynCols);
        }
    }

    public List<CrossStatisticListStatisBean> getLstStatisBeans()
    {
        if(this.childObj instanceof ColBean)
        {
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            return cslrcbean.getLstStatisBeans();
        }else
        {
            return ((CrossStatisticListGroupBean)this.childObj).getLstStatisBeans();
        }
    }

    public int createDisplayBean(ReportRequest rrequest,ResultSet rs,Map<String,String> values,Map<String,String> mAllColConditionsInGroup,
            StringBuffer dynselectedColsBuf,StringBuffer conditionBuf,StringBuffer allColConditionsBuf,DisplayBean disbean,List lstDynChildren,
            Object headDataObj,int colidx,Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions) throws SQLException
    {
        String realvalueTmp=this.realvalue;
        if(realvalueTmp==null||realvalueTmp.trim().equals("")) realvalueTmp=column;
        String conditionTmp=condition;
        if(!realvalueTmp.equals(column)) conditionTmp=Tools.replaceAll(conditionTmp,column,realvalueTmp);
        String colvalue=rs.getString(column);
        colvalue=colvalue==null?"":colvalue.trim();
        List lstGroupChildren=null;
        if(mStaticColAndGroupTitlePostions.get(this.column).getDisplaymode()>0)
        {//此分组参与本次显示
            UltraListReportGroupBean groupBean=null;
            if(colvalue.equals(values.get(column)))
            {
                groupBean=(UltraListReportGroupBean)lstDynChildren.get(lstDynChildren.size()-1);
            }else
            {
                values.put(column,colvalue);
                if(this.hasDisplayStatisBeans(mStaticColAndGroupTitlePostions))
                {
                    if(lstDynChildren.size()>0)
                    {//对前一个分组的整个分组数据进行统计
                        groupBean=(UltraListReportGroupBean)lstDynChildren.get(lstDynChildren.size()-1);
                        colidx=createStatisticForPrevWholeGroup(rrequest,dynselectedColsBuf,disbean,colidx,groupBean,mAllColConditionsInGroup,
                                mStaticColAndGroupTitlePostions);
                    }
                }
                if(mAllColConditionsInGroup!=null) mAllColConditionsInGroup.remove(this.column);
                groupBean=new UltraListReportGroupBean(disbean,CrossStatisticListReportType.COL_GROUP_STARTID+colidx);
                if(headDataObj!=null)
                {
                    groupBean.setLabel("_"+column+"_"+colidx);
                    ReportAssistant.getInstance().setCrossStatisDataToPOJO(disbean.getReportBean(),headDataObj,"_"+column+"_"+colidx,colvalue);
                }else
                {
                    groupBean.setLabel(colvalue);
                }
                groupBean.setLabelstyleproperty(this.getLabelstyleproperty());
                groupBean.setLstChildren(new ArrayList());
                groupBean.setRowspan(this.getRowspan());
                lstDynChildren.add(groupBean);
                colidx++;
            }
            lstGroupChildren=groupBean.getLstChildren();
        }
        conditionTmp=Tools.replaceAll(conditionTmp,"#data#",colvalue);
        conditionBuf.append(conditionTmp).append(" and ");
        if(this.childObj instanceof CrossStatisticListGroupBean)
        {
            return ((CrossStatisticListGroupBean)childObj).createDisplayBean(rrequest,rs,values,mAllColConditionsInGroup,dynselectedColsBuf,
                    conditionBuf,allColConditionsBuf,disbean,lstGroupChildren,headDataObj,colidx,mStaticColAndGroupTitlePostions);
        }else
        {
            CrossStatisticListColBean cslrcolbean=(CrossStatisticListColBean)((ColBean)childObj)
                    .getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
            return cslrcolbean.createDisplayBean(rrequest,rs,dynselectedColsBuf,conditionBuf,allColConditionsBuf,disbean,lstGroupChildren,
                    headDataObj,colidx,mAllColConditionsInGroup,mStaticColAndGroupTitlePostions);
        }
    }

    private int createStatisticForPrevWholeGroup(ReportRequest rrequest,StringBuffer dynselectedColsBuf,DisplayBean disbean,int colidx,
            UltraListReportGroupBean groupBean,Map<String,String> mAllColConditionsInGroup,
            Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions)
    {
        if(this.lstDisplayStatisBeans==null||this.lstDisplayStatisBeans.size()==0) return colidx;
        String groupConditions=mAllColConditionsInGroup.get(column);//此分组包括的所有生成的<col/>组合的条件
        if(groupConditions==null||groupConditions.trim().equals("")) return colidx;
        groupConditions=groupConditions.trim();
        if(groupConditions.endsWith("or")) groupConditions=groupConditions.substring(0,groupConditions.length()-2);
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        for(CrossStatisticListStatisDisplayBean statisdBeanTmp:this.lstDisplayStatisBeans)
        {//为每一个统计显示一个标题列，并且为它拼凑上查询的字段
            cgpositionBeanTmp=mStaticColAndGroupTitlePostions.get(statisdBeanTmp.getStatiBean().getId());
            if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;
            ColBean cbTmp=new ColBean(disbean,CrossStatisticListReportType.COL_GROUP_STARTID+colidx);
            
            //cbTmp.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcbeanTmp);//必须为它生成此对象
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
            groupBean.getLstChildren().add(cbTmp);
            cbTmp.setProperty("[DYN_STATISTIC_DATA]");
            cbTmp.setColumn("column_"+colidx);
            dynselectedColsBuf.append(statisdBeanTmp.getStatiBean().getType()+"(");
            dynselectedColsBuf.append("case when ").append(groupConditions).append(" then ").append(statisdBeanTmp.getStatiBean().getColumn())
                    .append("  end ");
            dynselectedColsBuf.append(") as ").append("column_"+colidx).append(",");
            colidx++;
        }
        return colidx;
    }

    public int createStatisticForLastWholeGroup(ReportRequest rrequest,StringBuffer dynselectedColsBuf,DisplayBean disbean,int colidx,
            UltraListReportGroupBean currentGroupBean,Map<String,String> mLastGroupConditions,
            Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions)
    {
        if(this.childObj instanceof CrossStatisticListGroupBean)
        {//子标签也是一个<group/>，则先处理最后一个子分组，这是一个递归操作，会依次处理所有最后一个子分组的整个分组的统计
            if(currentGroupBean.getLstChildren().size()>0)
            {
                colidx=((CrossStatisticListGroupBean)this.childObj).createStatisticForLastWholeGroup(rrequest,dynselectedColsBuf,disbean,colidx,
                        (UltraListReportGroupBean)currentGroupBean.getLstChildren().get(currentGroupBean.getLstChildren().size()-1),
                        mLastGroupConditions,mStaticColAndGroupTitlePostions);
            }
        }
        colidx=createStatisticForPrevWholeGroup(rrequest,dynselectedColsBuf,disbean,colidx,currentGroupBean,mLastGroupConditions,
                mStaticColAndGroupTitlePostions);
        return colidx;
    }

    private boolean hasDisplayStatisBeans(Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions)
    {
        if(this.lstDisplayStatisBeans==null||this.lstDisplayStatisBeans.size()==0) return false;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(CrossStatisticListStatisDisplayBean cslsdbeanTmp:this.lstDisplayStatisBeans)
        {
            positionBeanTmp=mStaticColAndGroupTitlePostions.get(cslsdbeanTmp.getStatiBean().getId());
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()>0) return true;
        }
        return false;
    }

    public CrossStatisticListColBean getStatisColBean()
    {
        if(this.childObj instanceof ColBean)
        {
            return (CrossStatisticListColBean)((ColBean)childObj).getExtendConfigDataForReportType(CrossStatisticListReportType.KEY);
        }else
        {
            return ((CrossStatisticListGroupBean)childObj).getStatisColBean();
        }
    }

    public void getDynHeaderColumns(List<String> lstDataHeaderColumns)
    {
        lstDataHeaderColumns.add(this.column);
        if(this.childObj instanceof ColBean)
        {
            lstDataHeaderColumns.add(((ColBean)childObj).getColumn());
        }else
        {
            ((CrossStatisticListGroupBean)childObj).getDynHeaderColumns(lstDataHeaderColumns);
        }
    }

    public void initStatisDisplayBean(CrossStatisticListStatisBean statisBean,List<String> lstStatitems)
    {
        if(statisBean.getLstStatitems()!=null&&statisBean.getLstStatitems().contains(this.column))
        {
            CrossStatisticListStatisDisplayBean cslsdbean=new CrossStatisticListStatisDisplayBean();
            cslsdbean.setStatiBean(statisBean);
            cslsdbean.setLabel(statisBean.getLabel(this.column));
            cslsdbean.setLabelstyleproperty(statisBean.getLabelstyleproperty(this.column));
            cslsdbean.setValuestyleproperty(statisBean.getValuestyleproperty(this.column));
            if(this.lstDisplayStatisBeans==null) this.lstDisplayStatisBeans=new ArrayList<CrossStatisticListStatisDisplayBean>();
            this.lstDisplayStatisBeans.add(cslsdbean);
            while(lstStatitems.contains(this.column))
            {//从lstStatitems中删除掉此分组列的column，以便最后校验是否存在没有对应column列的statitem
                lstStatitems.remove(this.column);
            }
        }
        if(this.childObj instanceof ColBean)
        {
            ((CrossStatisticListColBean)((ColBean)childObj).getExtendConfigDataForReportType(CrossStatisticListReportType.KEY))
                    .initStatisDisplayBean(statisBean,lstStatitems);
        }else
        {
            ((CrossStatisticListGroupBean)childObj).initStatisDisplayBean(statisBean,lstStatitems);
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        DisplayBean disbean=(DisplayBean)owner;
        CrossStatisticListGroupBean bean=(CrossStatisticListGroupBean)super.clone(owner);
        if(this.childObj!=null)
        {
            if(childObj instanceof ColBean)
            {
                ColBean cbTmp=(ColBean)childObj;
                cbTmp=disbean.getColBeanByColId(cbTmp.getColid());
                if(cbTmp==null)
                {
                    bean.setChildObj(((ColBean)childObj).clone(owner));
                }else
                {
                    bean.setChildObj(cbTmp);
                }
            }else if(childObj instanceof CrossStatisticListGroupBean)
            {
                bean.setChildObj(((CrossStatisticListGroupBean)childObj).clone(owner));
            }
        }
        if(lstTablenameConditions!=null)
        {
            List<ConditionBean> lstTablenameConditionsNew=new ArrayList<ConditionBean>();
            for(ConditionBean cbTmp:lstTablenameConditions)
            {
                lstTablenameConditionsNew.add((ConditionBean)cbTmp.clone(null));
            }
            bean.setLstTablenameConditions(lstTablenameConditionsNew);
        }
        return bean;
    }
}
