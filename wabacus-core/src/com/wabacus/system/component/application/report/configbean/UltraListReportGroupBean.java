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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.UltraListReportAssistant;
import com.wabacus.system.component.application.report.UltraListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.util.Tools;

public class UltraListReportGroupBean extends AbsExtendConfigBean
{
    private static Log log=LogFactory.getLog(UltraListReportGroupBean.class);
    
    private String groupid;
    
    private String parentGroupid;
    
    private String childids;
    
    private String label;

    private String labelstyleproperty;
    
    private int rowspan=1;

    private List lstChildren;//这里存放displaytype不为never的所有普通子列和分组子列

    public UltraListReportGroupBean(AbsConfigBean owner)
    {
        super(owner);
        this.groupid="group_"+((DisplayBean)owner).generate_childid();
    }

    public UltraListReportGroupBean(AbsConfigBean owner,int groupid)
    {
        super(owner);
        this.groupid="group_"+groupid;
    }
    
    public String getGroupid()
    {
        return groupid;
    }

    public void setGroupid(String groupid)
    {
        this.groupid=groupid;
    }

    public String getParentGroupid()
    {
        return parentGroupid;
    }

    public void setParentGroupid(String parentGroupid)
    {
        this.parentGroupid=parentGroupid;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label=label;
    }

    public String getLabelstyleproperty()
    {
        return labelstyleproperty;
    }

    public void setLabelstyleproperty(String labelstyleproperty)
    {
        this.labelstyleproperty=labelstyleproperty;
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

    public String getChildids()
    {
        return childids;
    }

    public void setChildids(String childids)
    {
        this.childids=childids;
    }

    public List getLstChildren()
    {
        return lstChildren;
    }

    public void setLstChildren(List lstChildren)
    {
        this.lstChildren=lstChildren;
    }
    
    public void createColAndGroupDisplayBeans(Map<String,String> mDisplayRealColAndGroupLabels,ReportRequest rrequest,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,List<ColAndGroupDisplayBean> lstColAndGroupDisplayBeans)
    {
        ColAndGroupDisplayBean cgDisplayBeanTmp;
        ColBean cbTmp;
        UltraListReportGroupBean ulgroupbeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        UltraListReportColBean ulcbTmp;
        List lstChildrenTmp=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(lstChildren,
                lstDynColids,mColAndGroupTitlePostions);
        AbsListReportColBean alrcbean=null;
        for(Object objTmp:lstChildrenTmp)
        {
            cgDisplayBeanTmp=new ColAndGroupDisplayBean();
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp.getDisplaymode()<0) continue;
                cgDisplayBeanTmp.setId(cbTmp.getColid());
                cgDisplayBeanTmp.setControlCol(cbTmp.isControlCol());
                alrcbean=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                cgDisplayBeanTmp.setNonFixedCol(alrcbean==null||!alrcbean.isFixedCol());
                cgDisplayBeanTmp.setAlways(positionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(positionBeanTmp.getDisplaymode()>0);
                ulcbTmp=(UltraListReportColBean)cbTmp.getExtendConfigDataForReportType(UltraListReportType.KEY);
                cgDisplayBeanTmp.setParentGroupId(ulcbTmp.getParentGroupid());
                cgDisplayBeanTmp.setLayer(positionBeanTmp.getLayer());
                if(cgDisplayBeanTmp.isChecked())
                {//如果当前列参与本次显示，则从mDisplayRealColAndGroupLabels中取label，因为包含了用户在拦截器中可能的修改
                    cgDisplayBeanTmp.setTitle(mDisplayRealColAndGroupLabels.get(cbTmp.getColid()));
                }else
                {
                    cgDisplayBeanTmp.setTitle(rrequest.getI18NStringValue(cbTmp.getLabel()));
                }
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ulgroupbeanTmp=(UltraListReportGroupBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(ulgroupbeanTmp.getGroupid());
                cgDisplayBeanTmp.setId(ulgroupbeanTmp.getGroupid());
                cgDisplayBeanTmp.setChildIds(ulgroupbeanTmp.getChildids());
                cgDisplayBeanTmp.setAlways(positionBeanTmp.getDisplaymode()==2);
                if(positionBeanTmp.getDisplaymode()<0) continue;
                cgDisplayBeanTmp.setChecked(positionBeanTmp.getDisplaymode()>0);
                cgDisplayBeanTmp.setChildIds(ulgroupbeanTmp.getChildids());
                cgDisplayBeanTmp.setParentGroupId(ulgroupbeanTmp.getParentGroupid());
                cgDisplayBeanTmp.setLayer(positionBeanTmp.getLayer());
                if(cgDisplayBeanTmp.isChecked())
                {
                    cgDisplayBeanTmp.setTitle(mDisplayRealColAndGroupLabels.get(ulgroupbeanTmp.getGroupid()));
                }else
                {
                    cgDisplayBeanTmp.setTitle(rrequest.getI18NStringValue(ulgroupbeanTmp.getLabel()));
                }
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
                ulgroupbeanTmp.createColAndGroupDisplayBeans(mDisplayRealColAndGroupLabels,rrequest,lstDynColids,mColAndGroupTitlePostions,
                        lstColAndGroupDisplayBeans);
            }
        }
    }
    
    public boolean hasRowgroupChildCol()
    {
        Object childObj=lstChildren.get(0);
        boolean hasRowGroupCol=false;
        if(childObj instanceof ColBean)
        {
            ColBean cbeanTmp=((ColBean)childObj);
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbeanTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
            hasRowGroupCol=alrcbean.isRowgroup();
        }else if(childObj instanceof UltraListReportGroupBean)
        {
            hasRowGroupCol=((UltraListReportGroupBean)childObj).hasRowgroupChildCol();
        }
        return hasRowGroupCol;
    }
    
    public boolean hasFixedChildCol()
    {
        Object childObj=lstChildren.get(0);
        boolean result=false;
        if(childObj instanceof ColBean)
        {
            ColBean cbeanTmp=((ColBean)childObj);
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbeanTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
            result=alrcbean!=null&&alrcbean.isFixedCol();
        }else if(childObj instanceof UltraListReportGroupBean)
        {
            result=((UltraListReportGroupBean)childObj).hasFixedChildCol();
        }
        return result;
    }
    
    public boolean isDragable(AbsListReportDisplayBean alrdbean)
    {
        if(this.hasFixedChildCol()) return false;
        if(alrdbean==null||alrdbean.getRowgrouptype()<=0||alrdbean.getRowGroupColsNum()<=0) return true;
        if(this.hasRowgroupChildCol()) return false;//有参与了行分组或树形分组的子列
        return true;
    }
    
    public List getDisplayChildrenByLayer(List lstChildrens,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int layer,
            int currentrowcount,int parentrowspan)
    {
        if(lstChildrens==null||lstChildrens.size()==0||layer<=0) return null;
        if(currentrowcount-parentrowspan<=0) return null;//如果父分组标题所占的rowspan还没有显示完，则在当前<tr/>中先不显示其子分组或子列
        List lstDisplayChildren=new ArrayList();

        if(layer==1)
        {
            String idTmp;
            ColAndGroupTitlePositionBean positionBeanTmp;
            for(Object objTmp:lstChildrens)
            {
                idTmp=null;
                if(objTmp instanceof ColBean)
                {
                    idTmp=((ColBean)objTmp).getColid();

                }else if(objTmp instanceof UltraListReportGroupBean)
                {
                    idTmp=((UltraListReportGroupBean)objTmp).getGroupid();

                }
                if(idTmp!=null)
                {
                    positionBeanTmp=mColAndGroupTitlePostions.get(idTmp);
                    if(positionBeanTmp!=null&&positionBeanTmp.getDisplaymode()>0)
                    {
                        lstDisplayChildren.add(objTmp);
                    }
                }
            }
        }else
        {
            UltraListReportGroupBean groupBeanTmp;
            for(Object obj:lstChildrens)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBeanTmp=(UltraListReportGroupBean)obj;
                //System.out.println("groupBean.getLabel"+groupBean.getLabel()+" size:::"+getChildrenByLayer(groupBean.getLstChildren(),layer - 1));
                List lstDisplayChildrenTmp=getDisplayChildrenByLayer(groupBeanTmp.getLstChildren(),
                        mColAndGroupTitlePostions,layer-1,currentrowcount,parentrowspan+groupBeanTmp.getRowspan());
                if(lstDisplayChildrenTmp!=null) lstDisplayChildren.addAll(lstDisplayChildrenTmp);
            }
        }
        return lstDisplayChildren;
    }
    public void getAllChildColIdsInerit(List<String> lstColIds)
    {
        Object obj;
        for(int i=0;i<lstChildren.size();i++)
        {
            obj=lstChildren.get(i);
            if(obj==null) continue;
            if(obj instanceof ColBean)
            {
                lstColIds.add(((ColBean)obj).getColid());
            }else
            {
                ((UltraListReportGroupBean)obj).getAllChildColIdsInerit(lstColIds);
            }
        }
    }
    
    public boolean containsChild(String childid,boolean inherit)
    {
        if(childid==null||childid.trim().equals("")) return false;
        for(Object childTmp:lstChildren)
        {
            if(childTmp==null) continue;
            if(childTmp instanceof ColBean)
            {
                if(childid.equals((((ColBean)childTmp).getColid())))
                {
                    return true;
                }
            }else
            {
                if(childid.equals(((UltraListReportGroupBean)childTmp).getGroupid()))
                {
                    return true;
                }
                if(inherit)
                {
                    boolean isExist=((UltraListReportGroupBean)childTmp).containsChild(childid,inherit);
                    if(isExist) return true;
                }
            }
        }
        return false;
    }
    
    public UltraListReportGroupBean getGroupBeanById(String groupid)
    {
        if(groupid==null||groupid.trim().equals("")) return null;
        if(lstChildren!=null&&lstChildren.size()>0)
        {
            Object obj;
            for(int i=0;i<lstChildren.size();i++)
            {
                obj=lstChildren.get(i);
                if(obj==null||obj instanceof ColBean) continue;
                if(groupid.equals(((UltraListReportGroupBean)obj).getGroupid()))
                   return (UltraListReportGroupBean)obj;
                obj=((UltraListReportGroupBean)obj).getGroupBeanById(groupid);
                if(obj!=null) return (UltraListReportGroupBean)obj;
            }
        }
        return null;
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
                    {
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
    
    public String getFirstColId(List<String> lstNewOrderChildCols)
    {
        if(lstNewOrderChildCols==null||lstNewOrderChildCols.size()==0)
        {
            Object childObj=lstChildren.get(0);
            if(childObj instanceof ColBean)
            {
                return ((ColBean)childObj).getColid();
            }else
            {
                return ((UltraListReportGroupBean)childObj).getFirstColId(null);
            }
        }else
        {
            List<String> lstAllChildColIds=new ArrayList<String>();
            this.getAllChildColIdsInerit(lstAllChildColIds);
            for(String colidTmp:lstNewOrderChildCols)
            {
                if(lstAllChildColIds.contains(colidTmp))
                {
                    return colidTmp;
                }
            }
        }
        return null;
    }
    
    public String getLastColId(List<String> lstNewOrderChildCols)
    {
        if(lstNewOrderChildCols==null||lstNewOrderChildCols.size()==0)
        {
            Object childObj=lstChildren.get(lstChildren.size()-1);
            if(childObj instanceof ColBean)
            {
                return ((ColBean)childObj).getColid();
            }else
            {
                return ((UltraListReportGroupBean)childObj).getLastColId(null);
            }
        }else
        {
            List<String> lstAllChildColIds=new ArrayList<String>();
            this.getAllChildColIdsInerit(lstAllChildColIds);
            String colidTmp;
            for(int i=lstNewOrderChildCols.size()-1;i>=0;i--)
            {
                colidTmp=lstNewOrderChildCols.get(i);
                if(lstAllChildColIds.contains(colidTmp))
                {
                    return colidTmp;
                }
            }
        }
        return null;
    }
    
    public void getAllColBeans(List<ColBean> lstColBeans,Map<String,ColAndGroupTitlePositionBean> mPositions)
    {
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        for(Object childObj:lstChildren)
        {
            if(childObj instanceof ColBean)
            {
                if(mPositions!=null)
                {
                    cgpositionBeanTmp=mPositions.get(((ColBean)childObj).getColid());
                    if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;
                }
                lstColBeans.add((ColBean)childObj);
            }else
            {
                ((UltraListReportGroupBean)childObj).getAllColBeans(lstColBeans,mPositions);
            }
        }
    }
    
    public int calColSpans()
    {
        int colspans=0;
        for(Object childObj:lstChildren)
        {
            if(childObj instanceof ColBean)
            {
                colspans++;
            }else
            {
                colspans+=((UltraListReportGroupBean)childObj).calColSpans();
            }
        }
        if(labelstyleproperty!=null&&!labelstyleproperty.trim().equals(""))
        {
            String value=Tools.getPropertyValueByName("colspan",labelstyleproperty,true);
            try
            {
                if(Integer.parseInt(value)>1)
                {
                    log
                            .warn("对于复杂标题的报表，最好不要配置其colspan和rowspan，否则可能会干扰报表的正常显示，可采用width和height来控制宽度和高度");
                }
            }catch(Exception e)
            {
            }
        }
        if(colspans>1)
        {
            if(labelstyleproperty==null) labelstyleproperty="";
            labelstyleproperty="  colspan=\""+colspans+"\" "+labelstyleproperty;
        }
        return colspans;
    }
    
    public int[] calPositionStart(ReportRequest rrequest,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            List<String> lstDisplayColids)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.groupid);
        if(positionBean==null)
        {
            positionBean=new ColAndGroupTitlePositionBean();
            mColAndGroupTitlePostions.put(this.groupid,positionBean);
        }
        ColAndGroupTitlePositionBean positionBeanTmp;
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        boolean hasGroupChild=false;
        boolean isAllChildNonDisplayPermission=true;
        boolean containsAlwaysCol=false;//当前分组是否包含参与本次显示的displaytype为always的列
        int maxrowspan=0;
        int colspan=0;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp==null)
                {
                    positionBeanTmp=new ColAndGroupTitlePositionBean();
                    mColAndGroupTitlePostions.put(cbTmp.getColid(),positionBeanTmp);
                }
                positionBeanTmp.setDisplaymode(cbTmp.getDisplaymode(rrequest,lstDisplayColids));
                if(positionBeanTmp.getDisplaymode()>=0)
                {
                    isAllChildNonDisplayPermission=false;
                    if(positionBeanTmp.getDisplaymode()>0) colspan++;
                    if(positionBeanTmp.getDisplaymode()==2) containsAlwaysCol=true;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                int[] spans=groupBeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions,
                        lstDisplayColids);
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()>=0)
                {
                    isAllChildNonDisplayPermission=false;
                    if(positionBeanTmp.getDisplaymode()>0)
                    {
                        colspan+=spans[0];
                        if(spans[1]>maxrowspan)
                        {
                            maxrowspan=spans[1];
                        }
                        hasGroupChild=true;
                        if(positionBeanTmp.getDisplaymode()==2) containsAlwaysCol=true;//当前分组包含参与本次显示的displaytype为always的列
                    }
                }
            }
        }
        if(isAllChildNonDisplayPermission)
        {
            positionBean.setDisplaymode(-1);
        }else if(colspan==0)
        {
            positionBean.setDisplaymode(0);
        }else if(colspan>0)
        {
            if(containsAlwaysCol)
            {
                positionBean.setDisplaymode(2);
            }else
            {
                positionBean.setDisplaymode(1);
            }
        }
        if(positionBean.getDisplaymode()>0)
        {
            positionBean.setColspan(colspan);
            maxrowspan=maxrowspan+this.rowspan;
            if(!hasGroupChild)
            {//如果没有子分组节点，即子节点全部是普通列<col/>，则本分组节点总行数要再加1即包括其所有子<col/>所占的那一行
                maxrowspan=maxrowspan+1;
            }
        }else
        {
            colspan=0;
            maxrowspan=0;
        }
        return new int[] { colspan, maxrowspan };
    }
    
    public void calPositionEnd(Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            int[] position)
    {
        int totalrowspan=position[0];
        int layer=position[1];
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.groupid);
        if(positionBean.getDisplaymode()<0) return;//如果没有显示权限，则不会在前台提供列选择选项，因此直接返回
        positionBean.setLayer(layer);
        if(positionBean.getDisplaymode()>0)
        {
            positionBean.setRowspan(this.rowspan);
        }
        ColAndGroupTitlePositionBean positionBeanTmp;
        ColBean cbTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                positionBeanTmp.setLayer(layer+1);
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setRowspan(totalrowspan-this.rowspan);
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ((UltraListReportGroupBean)objTmp).calPositionEnd(mColAndGroupTitlePostions,
                        new int[] { totalrowspan-this.rowspan, layer+1 });
            }
        }
    }

    public void calPositionForStandardExcel(
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            List<String> lstDynColids,int[] startcolrowidx)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.groupid);
        if(positionBean.getDisplaymode()<=0||lstChildren==null||lstChildren.size()==0)
        {
            return;
        }
        int startrowidx=startcolrowidx[0];//起始行号
        int startcolidx=startcolrowidx[1];
        positionBean.setStartcolindex(startcolidx);
        positionBean.setStartrowindex(startrowidx);
        List lstChildrenTemp=UltraListReportAssistant.getInstance().sortChildrenByDynColOrders(lstChildren,lstDynColids,mColAndGroupTitlePostions);
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(Object objTmp:lstChildrenTemp)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setStartcolindex(startcolidx);
                    positionBeanTmp.setStartrowindex(startrowidx+this.rowspan);
                    startcolidx++;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    groupBeanTmp.calPositionForStandardExcel(mColAndGroupTitlePostions,lstDynColids,
                            new int[] { startrowidx+this.rowspan, startcolidx });
                    startcolidx+=positionBeanTmp.getColspan();
                }
            }
        }
    }
    

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        DisplayBean disbean=(DisplayBean)owner;
        UltraListReportGroupBean bean=(UltraListReportGroupBean)super.clone(owner);
        if(bean.getLstChildren()!=null&&bean.getLstChildren().size()>0)
        {
            List lstTemp=new ArrayList();
            ColBean cbTmp;
            for(int i=0;i<bean.getLstChildren().size();i++)
            {
                Object obj=bean.getLstChildren().get(i);
                if(obj==null) continue;
                if(obj instanceof ColBean)
                {
                    cbTmp=(ColBean)obj;
                    cbTmp=disbean.getColBeanByColId(cbTmp.getColid());
                    lstTemp.add(cbTmp);
                }else if(obj instanceof AbsExtendConfigBean)
                {
                    lstTemp.add(((AbsExtendConfigBean)obj).clone(owner));
                }
            }
            bean.setLstChildren(lstTemp);
        }
        return bean;
    }

    public void doPostLoad()
    {
        if(this.lstChildren!=null&&this.lstChildren.size()>0)
        {
            AbsListReportBean alrbean=(AbsListReportBean)this.getOwner().getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            Object childObj;
            ColBean cbeanTmp;
            for(int i=0;i<lstChildren.size();i++)
            {
                childObj=lstChildren.get(i);
                if(childObj==null) continue;
                if(childObj instanceof ColBean)
                {
                    cbeanTmp=((ColBean)childObj);
                    if(alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL)
                    {//报表配置了纵向滚动条,则不能在<group/>中的列配置width。
                        if(Tools.getPropertyValueByName("width",cbeanTmp.getValuestyleproperty(),true)!=null
                                ||Tools.getPropertyValueByName("width",cbeanTmp.getLabelstyleproperty(),true)!=null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+cbeanTmp.getReportBean().getPath()
                                    +"失败，此报表配置了scrollheight，因此不能在<group/>中的<col/>配置width属性");
                        }
                    }
                }else if(childObj instanceof UltraListReportGroupBean)
                {
                    ((UltraListReportGroupBean)childObj).doPostLoad();
                }
            }
        }
    }
    
    public int getHidden()
    {
        throw new RuntimeException("被删除");
    }
}