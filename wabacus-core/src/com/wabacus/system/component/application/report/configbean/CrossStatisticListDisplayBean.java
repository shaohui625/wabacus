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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.CrossStatisticListReportType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class CrossStatisticListDisplayBean extends AbsExtendConfigBean
{
    private final static int STATICOL_WHOLEREPORT_STARTID=10000;

    private List lstChildren;

    private Map<String,ColAndGroupTitlePositionBean> mChildrenDefaultPositions;

    private List<ColBean> lstDefaultVerticalStatisColBeans;

    private List<CrossStatisticListStatisDisplayBean> lstDisplayStatisBeans;//针对整个报表进行横向统计的统计项对象，即statitems配置为report/>的<statistic/>标签对象，在doPostLoad()方法中构造

    public CrossStatisticListDisplayBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public List getLstChildren()
    {
        return lstChildren;
    }

    public void setLstChildren(List lstChildren)
    {
        this.lstChildren=lstChildren;
    }

    public List<ColBean> getLstDefaultVerticalStatisColBeans()
    {
        return lstDefaultVerticalStatisColBeans;
    }

    public void setLstDefaultVerticalStatisColBeans(List<ColBean> lstDefaultVerticalStatisColBeans)
    {
        this.lstDefaultVerticalStatisColBeans=lstDefaultVerticalStatisColBeans;
    }

    public List<CrossStatisticListStatisDisplayBean> getLstDisplayStatisBeans()
    {
        return lstDisplayStatisBeans;
    }

    public void setLstDisplayStatisBeans(List<CrossStatisticListStatisDisplayBean> lstDisplayStatisBeans)
    {
        this.lstDisplayStatisBeans=lstDisplayStatisBeans;
    }

    public Map<String,ColAndGroupTitlePositionBean> getMChildrenDefaultPositions()
    {
        return mChildrenDefaultPositions;
    }

    public void setMChildrenDefaultPositions(Map<String,ColAndGroupTitlePositionBean> childrenDefaultPositions)
    {
        mChildrenDefaultPositions=childrenDefaultPositions;
    }

    public boolean isAllDisplayStatisticItemsHidden(Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(mColAndGroupTitlePostions==null) return false;
        if(this.lstDisplayStatisBeans==null||this.lstDisplayStatisBeans.size()==0) return true;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(CrossStatisticListStatisDisplayBean cslsdbeanTmp:this.lstDisplayStatisBeans)
        {
            positionBeanTmp=mColAndGroupTitlePostions.get(cslsdbeanTmp.getStatiBean().getId());
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()>0) return false;
        }
        return true;
    }

    public int createStatisForWholeRow(ReportRequest rrequest,StringBuffer dynselectedColsBuf,List lstChildren,int colidx,
            Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions,String allColConditions)
    {
        if(this.lstDisplayStatisBeans==null||this.lstDisplayStatisBeans.size()==0) return colidx;
        DisplayBean disbean=(DisplayBean)this.getOwner();
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        if(colidx<=0) colidx+=STATICOL_WHOLEREPORT_STARTID;
        for(CrossStatisticListStatisDisplayBean statisdBeanTmp:this.lstDisplayStatisBeans)
        {
            cgpositionBeanTmp=mStaticColAndGroupTitlePostions.get(statisdBeanTmp.getStatiBean().getId());
            if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;//当前统计项不参与本次显示
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
            cbTmp.setProperty("[DYN_STATISTIC_DATA]");
            cbTmp.setColumn("column_"+colidx);
            dynselectedColsBuf.append(statisdBeanTmp.getStatiBean().getType()+"(");
            if(!allColConditions.trim().equals(""))
            {
                dynselectedColsBuf.append("case when ").append(allColConditions).append(" then ").append(statisdBeanTmp.getStatiBean().getColumn())
                        .append("  end ");
            }else
            {
                dynselectedColsBuf.append(statisdBeanTmp.getStatiBean().getColumn());
            }
            dynselectedColsBuf.append(") as ").append("column_"+colidx).append(",");
            lstChildren.add(cbTmp);
            colidx++;
        }
        if(dynselectedColsBuf.length()>0&&dynselectedColsBuf.charAt(dynselectedColsBuf.length()-1)==',')
            dynselectedColsBuf.deleteCharAt(dynselectedColsBuf.length()-1);
        return colidx;
    }

    public void initStatisDisplayBean(ICrossStatisticColAndGroup childDynObj)
    {
        CrossStatisticListColBean cslcbean=childDynObj.getStatisColBean();
        List<String> lstStatitemsTmp;
        for(CrossStatisticListStatisBean statisBeanTmp:cslcbean.getLstStatisBeans())
        {//分析每个统计<statistic/>，看一下它通过statitems属性配置了要对哪些列或分组进行统计
            lstStatitemsTmp=statisBeanTmp.getLstStatitems();
            if(lstStatitemsTmp!=null) lstStatitemsTmp=(List<String>)((ArrayList<String>)lstStatitemsTmp).clone();
            if(lstStatitemsTmp!=null&&lstStatitemsTmp.size()>0&&lstStatitemsTmp.contains(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT))
            {//此<statistic/>配置有针对整个报表数据的统计
                CrossStatisticListStatisDisplayBean cslsdbean=new CrossStatisticListStatisDisplayBean();
                cslsdbean.setStatiBean(statisBeanTmp);
                cslsdbean.setLabel(statisBeanTmp.getLabel(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT));
                cslsdbean.setLabelstyleproperty(statisBeanTmp.getLabelstyleproperty(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT));
                cslsdbean.setValuestyleproperty(statisBeanTmp.getValuestyleproperty(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT));
                if(this.lstDisplayStatisBeans==null) this.lstDisplayStatisBeans=new ArrayList<CrossStatisticListStatisDisplayBean>();
                this.lstDisplayStatisBeans.add(cslsdbean);
                while(lstStatitemsTmp.contains(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT))
                {
                    lstStatitemsTmp.remove(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT);
                }
            }
            childDynObj.initStatisDisplayBean(statisBeanTmp,lstStatitemsTmp);
            if(lstStatitemsTmp!=null&&lstStatitemsTmp.size()>0)
            {//此<statistic/>的statitems属性中还存在没有找到对应分组或列的统计项
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"失败，id为"+statisBeanTmp.getId()
                        +"的<statistic/>在statitems属性中配置的"+lstStatitemsTmp+"没有对应column的<col/>或<group/>");
            }
            statisBeanTmp.setLstLabels(null);
            statisBeanTmp.setLstLabelstyleproperties(null);
            statisBeanTmp.setLstStatitems(null);
            statisBeanTmp.setLstValuestyleproperties(null);
        }
        
        List<CrossStatisticListStatisDisplayBean> lstDisplayStatisBeansInDynCol=cslcbean.getLstDisplayStatisBeans();//取到所有针对最里层<col/>进行统计的统计项对象
        if(lstDisplayStatisBeansInDynCol!=null
                &&(lstDisplayStatisBeansInDynCol.size()>1||(lstDisplayStatisBeansInDynCol.size()==1&&(lstDisplayStatisBeansInDynCol.get(0).getLabel()!=null&&!lstDisplayStatisBeansInDynCol
                        .get(0).getLabel().trim().equals("")))))
        {//如果在最里层的<col/>中要显示多个统计，或只显示一个统计，但配置了label，则当前列要显示两行，一行显示<col/>的label，一行显示统计的label
            ColBean cbOwner=(ColBean)cslcbean.getOwner();
            cslcbean.setShouldShowStaticLabel(true);
            String labelstyleproperty=cbOwner.getLabelstyleproperty();
            labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"align","center");
            labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"valign","middle");
            cbOwner.setLabelstyleproperty(labelstyleproperty);
            String rowspan=Tools.getPropertyValueByName("rowspan",labelstyleproperty,true);
            if(rowspan!=null&&!rowspan.trim().equals(""))
            {
                try
                {
                    cslcbean.setRowspan(Integer.parseInt(rowspan));
                }catch(NumberFormatException e)
                {
                    cslcbean.setRowspan(1);
                }
            }
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        DisplayBean disbean=(DisplayBean)owner;
        CrossStatisticListDisplayBean newBean=(CrossStatisticListDisplayBean)super.clone(owner);
        if(lstChildren!=null)
        {
            List lstChildrenNew=new ArrayList();
            Object objNew;
            ColBean cbTmp;
            for(Object childObj:lstChildren)
            {
                objNew=null;
                if(childObj instanceof ColBean)
                {
                    cbTmp=(ColBean)childObj;
                    cbTmp=disbean.getColBeanByColId(cbTmp.getColid());
                    if(cbTmp==null)
                    {
                        objNew=((ColBean)childObj).clone(owner);
                    }else
                    {
                        objNew=cbTmp;
                    }
                }else if(childObj instanceof CrossStatisticListGroupBean)
                {
                    objNew=((CrossStatisticListGroupBean)childObj).clone(owner);
                }else if(childObj instanceof UltraListReportGroupBean)
                {
                    objNew=((UltraListReportGroupBean)childObj).clone(owner);
                }
                if(objNew!=null) lstChildrenNew.add(objNew);
            }
            newBean.setLstChildren(lstChildrenNew);
        }
        try
        {
            if(mChildrenDefaultPositions!=null)
            {
                Map<String,ColAndGroupTitlePositionBean> mChildrenPositionsNew=new HashMap<String,ColAndGroupTitlePositionBean>();
                for(Entry<String,ColAndGroupTitlePositionBean> entry:mChildrenDefaultPositions.entrySet())
                {
                    mChildrenPositionsNew.put(entry.getKey(),(ColAndGroupTitlePositionBean)entry.getValue().clone());
                }
                newBean.setMChildrenDefaultPositions(mChildrenPositionsNew);
            }
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        if(lstDefaultVerticalStatisColBeans!=null)
        {
            List<ColBean> lstVerticalStatisColBeansNew=new ArrayList<ColBean>();
            for(ColBean cbTmp:lstDefaultVerticalStatisColBeans)
            {
                lstVerticalStatisColBeansNew.add((ColBean)cbTmp.clone(owner));
            }
            newBean.setLstDefaultVerticalStatisColBeans(lstVerticalStatisColBeansNew);
        }
        return newBean;
    }

    public boolean removeChildColBeanByColumn(String column,boolean inherit)
    {
        if(column==null||column.trim().equals("")) return false;
        if(lstChildren==null||lstChildren.size()==0) return false;
        boolean result=false;
        Object obj=null;
        for(int i=lstChildren.size()-1;i>=0;i--)
        {
            obj=lstChildren.get(i);
            if(obj==null) continue;
            if(obj instanceof ColBean)
            {
                if(((ColBean)obj).getColumn().equals(column))
                {
                    lstChildren.remove(i);
                    result=true;
                }
            }else if(obj instanceof UltraListReportGroupBean)
            {
                if(inherit)
                {
                    boolean flag=((UltraListReportGroupBean)obj).removeChildColBeanByColumn(column,true);
                    if(flag)
                    {//成功删除了column对应的列
                        if(((UltraListReportGroupBean)obj).getLstChildren()==null||((UltraListReportGroupBean)obj).getLstChildren().size()==0)
                        {
                            lstChildren.remove(i);
                        }
                        result=true;
                    }
                }
            }
        }
        return result;
    }
}
