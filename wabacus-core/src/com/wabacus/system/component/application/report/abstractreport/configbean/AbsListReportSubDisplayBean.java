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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javassist.ClassPool;
import javassist.CtClass;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ClassPoolAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemAndDataSetBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemBean;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class AbsListReportSubDisplayBean extends AbsExtendConfigBean
{
    public final static int SUBROW_DISPLAYTYPE_REPORT=1;
    
    public final static int SUBROW_DISPLAYTYPE_PAGE=2;
    
    public final static int SUBROW_DISPLAYTYPE_PAGEREPORT=3;
    
    public final static int SUBROW_POSITION_TOP=1;
    
    public final static int SUBROW_POSITION_BOTTOM=2;
    
    public final static int SUBROW_POSITION_BOTH=3;
    
    private Class pojoclass;

    private FormatBean fbean;

    private List<StatisticItemBean> lstStatitemBeans;

    private List<StatisticItemAndDataSetBean> lstStatitemAndDatasetBeans;//存放本报表查询所有统计数据时所用到的数据集对象，并且在每个数据集对象中存放其所统计的所有统计项

    private List<AbsListReportSubDisplayRowBean> lstSubDisplayRowBeans;

    private Map<String,AbsListReportRowGroupSubDisplayRowBean> mRowGroupSubDisplayRowBeans;

    public AbsListReportSubDisplayBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public Class getPojoclass()
    {
        return pojoclass;
    }

    public Object getPojoObject()
    {
        try
        {
            return this.pojoclass.newInstance();
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("实例化报表"+getOwner().getReportBean().getPath()+"存放统计数据的POJO对象失败",e);
        }
    }

    public Map<String,AbsListReportRowGroupSubDisplayRowBean> getMRowGroupSubDisplayRowBeans()
    {
        return mRowGroupSubDisplayRowBeans;
    }

    public void setMRowGroupSubDisplayRowBeans(Map<String,AbsListReportRowGroupSubDisplayRowBean> rowGroupSubDisplayRowBeans)
    {
        mRowGroupSubDisplayRowBeans=rowGroupSubDisplayRowBeans;
    }

    public void setPojoclass(Class pojoclass)
    {
        this.pojoclass=pojoclass;
    }

    public List<StatisticItemBean> getLstStatitemBeans()
    {
        return lstStatitemBeans;
    }

    public void setLstStatitemBeans(List<StatisticItemBean> lstStatitemBeans)
    {
        this.lstStatitemBeans=lstStatitemBeans;
    }

    public List<StatisticItemAndDataSetBean> getLstStatitemAndDatasetBeans()
    {
        return lstStatitemAndDatasetBeans;
    }

    public void setLstStatitemAndDatasetBeans(List<StatisticItemAndDataSetBean> lstStatitemAndDatasetBeans)
    {
        this.lstStatitemAndDatasetBeans=lstStatitemAndDatasetBeans;
    }

    public List<AbsListReportSubDisplayRowBean> getLstStatiDisplayRowBeans()
    {
        return lstSubDisplayRowBeans;
    }

    public void setLstStatiDisplayRowBeans(List<AbsListReportSubDisplayRowBean> lstStatiDisplayRowBeans)
    {
        this.lstSubDisplayRowBeans=lstStatiDisplayRowBeans;
    }

    public boolean isAllDisplayPerpageDataRows()
    {
        if(lstSubDisplayRowBeans==null||lstSubDisplayRowBeans.size()==0) return false;
        for(AbsListReportSubDisplayRowBean rowbeanTmp:lstSubDisplayRowBeans)
        {
            if(rowbeanTmp.getDisplaytype()!=SUBROW_DISPLAYTYPE_PAGE) return false;
        }
        return true;
    }

    public boolean isAllDisplayWholeReportDataRows()
    {
        if(lstSubDisplayRowBeans==null||lstSubDisplayRowBeans.size()==0) return false;
        for(AbsListReportSubDisplayRowBean rowbeanTmp:lstSubDisplayRowBeans)
        {
            if(rowbeanTmp.getDisplaytype()!=SUBROW_DISPLAYTYPE_REPORT) return false;
        }
        return true;
    }
    
    public FormatBean getFbean()
    {
        return fbean;
    }

    public void setFbean(FormatBean fbean)
    {
        this.fbean=fbean;
    }

    public void addRowGroupSubDisplayRowBean(AbsListReportRowGroupSubDisplayRowBean srgbean)
    {
        if(srgbean==null) return;
        if(srgbean.getRowgroupcolumn()==null||srgbean.getRowgroupcolumn().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+getOwner().getReportBean().getPath()
                    +"失败，<rowgroup-subrow>或<treerowgroup-subrow>的column属性值为空");
        }
        if(mRowGroupSubDisplayRowBeans==null)
        {
            mRowGroupSubDisplayRowBeans=new HashMap<String,AbsListReportRowGroupSubDisplayRowBean>();
        }
        mRowGroupSubDisplayRowBeans.put(srgbean.getRowgroupcolumn(),srgbean);
    }

    public void doPostLoad()
    {
        ReportBean rbean=this.getOwner().getReportBean();
        buildSubDisplayPojoClass(rbean);
        initGetSetMethod(rbean);//初始化pojo的get/set方法，以便运行时可以直接使用
        if(rbean.getDbean().isColselect())
        {
            rbean.getDbean().setColselect(!hasManyScolsInRow());
        }
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(this.lstSubDisplayRowBeans!=null)
        {
            for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
            {
                validateSubDisplayColsConfig(sRowBeanTmp.getLstSubColBeans(),alrdbean);
            }
        }
        if(mRowGroupSubDisplayRowBeans!=null&&mRowGroupSubDisplayRowBeans.size()>0)
        {
            if(alrdbean.getRowgrouptype()!=1&&alrdbean.getRowgrouptype()!=2)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，当前报表不是行分组显示报表，不能配置行分组统计功能");
            }
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entry:mRowGroupSubDisplayRowBeans.entrySet())
            {
                if(entry==null) continue;
                entry.getValue().validateRowGroupSubDisplayColsConfig(this,(DisplayBean)alrdbean.getOwner());
            }
        }
        parseStatitemBeans();
        buildStatisticSql();
    }

    private void buildSubDisplayPojoClass(ReportBean reportbean)
    {
        List<String> lstProperties=new ArrayList<String>();
        List<IDataType> lstDataTypes=new ArrayList<IDataType>();
        if(this.lstStatitemBeans!=null&&this.lstStatitemBeans.size()>0)
        {
            for(StatisticItemBean statItemBeanTmp:this.lstStatitemBeans)
            {
                if(lstProperties.contains(statItemBeanTmp.getProperty()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，property属性"+statItemBeanTmp.getProperty()+"存在重复");
                }
                if(statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                        ||statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_REPORT)
                {
                    lstProperties.add(statItemBeanTmp.getProperty());
                    lstDataTypes.add(statItemBeanTmp.getDatatypeObj());
                }
                if(statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                        ||statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_PAGE)
                {//如果针对每页数据进行统计
                    lstProperties.add("page_"+statItemBeanTmp.getProperty());
                    lstDataTypes.add(statItemBeanTmp.getDatatypeObj());
                }
            }
        }
        if(lstSubDisplayRowBeans!=null&&lstSubDisplayRowBeans.size()>0)
        {
            for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
            {
                getSColPropertiesAndTypes(reportbean,lstProperties,lstDataTypes,sRowBeanTmp.getLstSubColBeans());
            }
        }
        if(this.mRowGroupSubDisplayRowBeans!=null&&this.mRowGroupSubDisplayRowBeans.size()>0)
        {
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entryTmp:this.mRowGroupSubDisplayRowBeans.entrySet())
            {
                AbsListReportRowGroupSubDisplayRowBean srgbean=entryTmp.getValue();
                getSColPropertiesAndTypes(reportbean,lstProperties,lstDataTypes,srgbean.getLstSubColBeans());
            }
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrdbean.getLstRowgroupColsColumn()!=null)
            {
                for(String groupcol:alrdbean.getLstRowgroupColsColumn())
                {
                    if(groupcol==null||groupcol.trim().equals("")) continue;
                    if(lstProperties.contains(groupcol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，不能配置<subcol/>的property属性为"+groupcol
                                +"，它已经是分组列的column值");
                    }
                    lstProperties.add(groupcol);
                    lstDataTypes.add(Config.getInstance().getDataTypeByClass(VarcharType.class));
                }
            }
        }
        ClassPool pool=ClassPoolAssistant.getInstance().createClassPool();
        String classname=Consts.BASE_PACKAGE_NAME+".Pojo_"+reportbean.getPageBean().getId()+reportbean.getId()+"_subdisplay";
        CtClass cclass=pool.makeClass(classname);
        ClassPoolAssistant.getInstance().addImportPackages(pool,fbean.getLstImports());
        String propertyTmp;
        IDataType dataTypeTmp;
        try
        {
            for(int i=0;i<lstProperties.size();i++)
            {
                propertyTmp=lstProperties.get(i);
                dataTypeTmp=lstDataTypes.get(i);
                if(dataTypeTmp==null) dataTypeTmp=Config.getInstance().getDataTypeByClass(VarcharType.class);
                ClassPoolAssistant.getInstance().addFieldAndGetSetMethod(cclass,propertyTmp,dataTypeTmp.getCreatedClass(pool));
            }
            ClassPoolAssistant.getInstance().addMethod(
                    cclass,
                    "public void format("+ReportRequest.class.getName()+"  rrequest,"+ReportBean.class.getName()+" rbean,"+String.class.getName()
                            +" type){"+fbean.getFormatContent()+" \n}");//type参数用于表示当前是在统计整个报表，还是在统计某个分组，统计整个报表时，type为空，统计某个分组时，这里传入相应<rowgroup/>的column属性
            this.pojoclass=ConfigLoadManager.currentDynClassLoader.loadClass(classname,cclass.toBytecode());
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("生成报表"+reportbean.getPath()+"的存放辅助显示信息的POJO类失败",e);
        }
        cclass.detach();
        pool.clearImportedPackages();
        pool=null;
    }

    private void getSColPropertiesAndTypes(ReportBean reportbean,List<String> lstProperties,List<IDataType> lstDataTypes,
            List<AbsListReportSubDisplayColBean> lstSColBeans)
    {
        if(lstSColBeans!=null&&lstSColBeans.size()>0)
        {
            for(AbsListReportSubDisplayColBean sColBeanTmp:lstSColBeans)
            {
                if(lstProperties.contains(sColBeanTmp.getProperty()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，property属性"+sColBeanTmp.getProperty()+"存在重复");
                }
                lstProperties.add(sColBeanTmp.getProperty());
                lstDataTypes.add(Config.getInstance().getDataTypeByClass(VarcharType.class));
            }
        }
    }

    private void initGetSetMethod(ReportBean reportbean)
    {
        try
        {
            if(this.lstStatitemBeans!=null&&this.lstStatitemBeans.size()>0)
            {
                for(StatisticItemBean statItemBeanTmp:this.lstStatitemBeans)
                {
                    String property=statItemBeanTmp.getProperty();
                    int statiscopeType=statItemBeanTmp.getStatiscope();
                    if(statiscopeType==StatisticItemBean.STATSTIC_SCOPE_ALL||statiscopeType==StatisticItemBean.STATSTIC_SCOPE_REPORT)
                    {
                        String setMethodName="set"+property.substring(0,1).toUpperCase()+property.substring(1);
                        statItemBeanTmp.setSetMethod(this.pojoclass.getMethod(setMethodName,new Class[] { statItemBeanTmp.getDatatypeObj()
                                .getJavaTypeClass() }));
                    }
                    if(statiscopeType==StatisticItemBean.STATSTIC_SCOPE_ALL||statiscopeType==StatisticItemBean.STATSTIC_SCOPE_PAGE)
                    {
                        String setMethodName="setPage_"+property;
                        statItemBeanTmp.setPageStatiSetMethod(this.pojoclass.getMethod(setMethodName,new Class[] { statItemBeanTmp.getDatatypeObj()
                                .getJavaTypeClass() }));
                    }
                }
            }
            if(this.lstSubDisplayRowBeans!=null&&this.lstSubDisplayRowBeans.size()>0)
            {
                for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
                {
                    initSColGetMethod(reportbean,sRowBeanTmp.getLstSubColBeans());
                }
            }
            if(this.mRowGroupSubDisplayRowBeans!=null&&this.mRowGroupSubDisplayRowBeans.size()>0)
            {
                for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entryTmp:this.mRowGroupSubDisplayRowBeans.entrySet())
                {
                    AbsListReportRowGroupSubDisplayRowBean srgbean=entryTmp.getValue();
                    initSColGetMethod(reportbean,srgbean.getLstSubColBeans());
                }
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("初始化报表"+reportbean.getPath()+"统计项的get/set方法失败",e);
        }
    }

    private void initSColGetMethod(ReportBean reportbean,List<AbsListReportSubDisplayColBean> lstSColBeans) throws Exception
    {
        if(lstSColBeans!=null&&lstSColBeans.size()>0)
        {
            
            for(AbsListReportSubDisplayColBean sColBeanTmp:lstSColBeans)
            {
                String property=sColBeanTmp.getProperty();
                String getMethodName="get"+property.substring(0,1).toUpperCase()+property.substring(1);
                sColBeanTmp.setGetMethod(pojoclass.getMethod(getMethodName,new Class[] {}));
            }
        }
    }

    
    private boolean hasManyScolsInRow()
    {
        //        if(bolHasManyScolsInRow!=null) return bolHasManyScolsInRow.booleanValue();
        
        for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
        {
            if(sRowBeanTmp.getLstSubColBeans()!=null&&sRowBeanTmp.getLstSubColBeans().size()>1)
            {
                
                return true;
            }
        }
        if(mRowGroupSubDisplayRowBeans!=null&&mRowGroupSubDisplayRowBeans.size()>0)
        {
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entry:mRowGroupSubDisplayRowBeans.entrySet())
            {
                if(entry==null) continue;
                if(entry.getValue().getLstSubColBeans()!=null&&entry.getValue().getLstSubColBeans().size()>1)
                {
                    
                    return true;
                }
            }
        }
        return false;
        
    }

    private void parseStatitemBeans()
    {
        if(this.lstStatitemBeans==null||this.lstStatitemBeans.size()==0) return;
        ReportBean reportbean=this.getOwner().getReportBean();
        this.lstStatitemAndDatasetBeans=new ArrayList<StatisticItemAndDataSetBean>();
        Map<String,StatisticItemAndDataSetBean> mDatasetItemBeans=new HashMap<String,StatisticItemAndDataSetBean>();
        String datasetidTmp;
        StatisticItemAndDataSetBean itemAndDatasetBeanTmp;
        ReportDataSetBean datasetBeanTmp;
        for(StatisticItemBean statitemBeanTmp:this.lstStatitemBeans)
        {
            datasetidTmp=statitemBeanTmp.getDatasetid();
            datasetBeanTmp=reportbean.getSbean().getDatasetBeanById(datasetidTmp);
            if(datasetBeanTmp==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+statitemBeanTmp.getProperty()+"失败，其配置的dataset数据集"
                        +datasetidTmp+"不存在");
            }
            datasetidTmp=datasetidTmp==null||datasetidTmp.trim().equals("")?"empty_empty_empty":datasetidTmp.trim();
            itemAndDatasetBeanTmp=mDatasetItemBeans.get(datasetidTmp);
            if(itemAndDatasetBeanTmp==null)
            {
                itemAndDatasetBeanTmp=new StatisticItemAndDataSetBean();
                itemAndDatasetBeanTmp.setDatasetbean(datasetBeanTmp);
                mDatasetItemBeans.put(datasetidTmp,itemAndDatasetBeanTmp);
                this.lstStatitemAndDatasetBeans.add(itemAndDatasetBeanTmp);
            }
            itemAndDatasetBeanTmp.addStaticItemBean(statitemBeanTmp);
        }
        /*
         * 将this.lstStatitemAndDatasetBeans中的对象按它们在<sql/>中排好的执行次序进行排序
         * 以便执行时所需的父数据集数据都存在
         */
        if(this.lstStatitemAndDatasetBeans.size()>1)
        {
            Collections.sort(this.lstStatitemAndDatasetBeans);
        }
    }

    private void buildStatisticSql()
    {
        ReportBean reportbean=this.getOwner().getReportBean();
        if(this.lstStatitemAndDatasetBeans==null||this.lstStatitemAndDatasetBeans.size()==0) return;
        for(StatisticItemAndDataSetBean beanTmp:this.lstStatitemAndDatasetBeans)
        {
            beanTmp.buildStatisticSql();
        }
        if(this.mRowGroupSubDisplayRowBeans!=null)
        {
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entryTmp:this.mRowGroupSubDisplayRowBeans.entrySet())
            {
                entryTmp.getValue().buildStatisticSqlGroupby(reportbean.getDbean());
            }
        }
    }

    public void validateSubDisplayColsConfig(List<AbsListReportSubDisplayColBean> lstSubDisplayColBeans,AbsListReportDisplayBean alrdbean)
    {
        if(lstSubDisplayColBeans==null||lstSubDisplayColBeans.size()==0) return;
        if(lstSubDisplayColBeans.size()==1)
        {
            String valuestyleproperty=lstSubDisplayColBeans.get(0).getValuestyleproperty();
            String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(colspan!=null&&!colspan.trim().equals(""))
            {
                lstSubDisplayColBeans.get(0).setValuestyleproperty(Tools.removePropertyValueByName("colspan",valuestyleproperty));
            }
        }else
        {//配置了多个统计列，则校验它们的colspan数是否合法，并计算各列统计信息在导出的plainexcel中占据的起止位置
            calColSpanAndStartColIdx(lstSubDisplayColBeans,alrdbean,alrdbean.getDefaultColumnCount());
        }
    }

    public void calColSpanAndStartColIdx(List<AbsListReportSubDisplayColBean> lstSubDisplayColBeans,AbsListReportDisplayBean alrdbean,int colcount)
    {
        int deltaCount=0;
        if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
        {
            deltaCount=alrdbean.getRowGroupColsNum()-1;
        }
        if(lstSubDisplayColBeans.size()==1)
        {
            lstSubDisplayColBeans.get(0).setPlainexcel_startcolidx(0);
            String valuestyleproperty=lstSubDisplayColBeans.get(0).getValuestyleproperty();
            String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(colspan==null||colspan.trim().equals(""))
            {
                valuestyleproperty=valuestyleproperty+" colspan=\""+colcount+"\"";
            }else
            {
                int icolspan=Integer.parseInt(colspan);
                if(icolspan!=colcount)
                {
                    throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()+"失败，配置的<scol/>的的colspan总数"+icolspan
                            +"与要显示的总列数"+colcount+"不相等");
                }
            }
            lstSubDisplayColBeans.get(0).setPlainexcel_colspan(colcount+deltaCount);
        }else
        {
            int total_colspan=0;
            boolean isFirstCol=true;
            for(AbsListReportSubDisplayColBean scbeanTmp:lstSubDisplayColBeans)
            {
                scbeanTmp.setPlainexcel_startcolidx(total_colspan);
                String valuestyleproperty=scbeanTmp.getValuestyleproperty();
                String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
                int icolspan=1;
                if(colspan!=null&&!colspan.trim().equals(""))
                {
                    icolspan=Integer.parseInt(colspan);
                    if(icolspan<=0) icolspan=1;
                }
                total_colspan+=icolspan;
                if(isFirstCol)
                {
                    icolspan=icolspan+deltaCount;
                    isFirstCol=false;
                }
                scbeanTmp.setPlainexcel_colspan(icolspan);
            }
            if(total_colspan!=colcount)
            {
                throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()+"失败，配置的所有<scol/>的的colspan总数"
                        +total_colspan+"与要显示的总列数"+colcount+"不相等");
            }
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportSubDisplayBean newBean=(AbsListReportSubDisplayBean)super.clone(owner);
        if(this.lstSubDisplayRowBeans!=null)
        {
            List<AbsListReportSubDisplayRowBean> lstNew=new ArrayList<AbsListReportSubDisplayRowBean>();
            for(AbsListReportSubDisplayRowBean rowbeanTmp:this.lstSubDisplayRowBeans)
            {
                lstNew.add(rowbeanTmp.clone());
            }
            newBean.setLstStatiDisplayRowBeans(lstNew);
        }
        if(mRowGroupSubDisplayRowBeans!=null)
        {
            Map<String,AbsListReportRowGroupSubDisplayRowBean> mStatiRowGroupBeansTemp=new HashMap<String,AbsListReportRowGroupSubDisplayRowBean>();
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entry:mRowGroupSubDisplayRowBeans.entrySet())
            {
                if(entry==null) continue;
                mStatiRowGroupBeansTemp.put(entry.getKey(),(AbsListReportRowGroupSubDisplayRowBean)entry.getValue().clone());
            }
            newBean.setMRowGroupSubDisplayRowBeans(mStatiRowGroupBeansTemp);
        }
        if(fbean!=null)
        {
            newBean.setFbean((FormatBean)fbean.clone(null));
        }
        if(this.lstStatitemBeans!=null)
        {
            List<StatisticItemBean> lstItemBeansNew=new ArrayList<StatisticItemBean>();
            for(StatisticItemBean itemBeanTmp:this.lstStatitemBeans)
            {
                lstItemBeansNew.add((StatisticItemBean)itemBeanTmp.clone());
            }
            newBean.setLstStatitemBeans(lstItemBeansNew);
        }
        return newBean;
    }
}
