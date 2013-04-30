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

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ClassPoolAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class AbsListReportStatiBean extends AbsExtendConfigBean
{
    private Class pojoclass;
    
    private FormatBean fbean;
    
    private String statiSqlWithoutCondition;

    private String statiSqlWithCondition;
    
    private List<AbsListReportStatItemBean> lstStatitemBeansWithCondition;//配置的所有有条件的统计项
    
    private List<AbsListReportStatItemBean> lstStatitemBeansWithoutCondition;
    
    private List<AbsListReportStatiColBean> lstStatiColBeans;

    private Map<String,AbsListReportStatiRowGroupBean> mStatiRowGroupBeans;
    
    public AbsListReportStatiBean(AbsConfigBean owner)
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
    
    public void setPojoclass(Class pojoclass)
    {
        this.pojoclass=pojoclass;
    }

    public List<AbsListReportStatiColBean> getLstStatiColBeans()
    {
        return lstStatiColBeans;
    }

    public void setLstStatiColBeans(List<AbsListReportStatiColBean> lstStatiColBeans)
    {
        this.lstStatiColBeans=lstStatiColBeans;
    }

    public Map<String,AbsListReportStatiRowGroupBean> getMStatiRowGroupBeans()
    {
        return mStatiRowGroupBeans;
    }

    public void setMStatiRowGroupBeans(Map<String,AbsListReportStatiRowGroupBean> statiRowGroupBeans)
    {
        mStatiRowGroupBeans=statiRowGroupBeans;
    }

    public String getStatiSqlWithoutCondition()
    {
        return statiSqlWithoutCondition;
    }

    public void setStatiSqlWithoutCondition(String statiSqlWithoutCondition)
    {
        this.statiSqlWithoutCondition=statiSqlWithoutCondition;
    }

    public String getStatiSqlWithCondition()
    {
        return statiSqlWithCondition;
    }

    public void setStatiSqlWithCondition(String statiSqlWithCondition)
    {
        this.statiSqlWithCondition=statiSqlWithCondition;
    }

    public List<AbsListReportStatItemBean> getLstStatitemBeansWithCondition()
    {
        return lstStatitemBeansWithCondition;
    }

    public List<AbsListReportStatItemBean> getLstStatitemBeansWithoutCondition()
    {
        return lstStatitemBeansWithoutCondition;
    }

    public List<AbsListReportStatItemBean> getLstAllStatitemBeans()
    {
        List<AbsListReportStatItemBean> lstResults=new ArrayList<AbsListReportStatItemBean>();
        if(lstStatitemBeansWithCondition!=null)
        {
            lstResults.addAll(lstStatitemBeansWithCondition);
        }
        if(lstStatitemBeansWithoutCondition!=null)
        {
            lstResults.addAll(lstStatitemBeansWithoutCondition);
        }
        return lstResults;
    }
    
    public void addStaticItemBean(AbsListReportStatItemBean staticitembean)
    {
        if(staticitembean.getLstConditions()==null||staticitembean.getLstConditions().size()==0)
        {
            if(this.lstStatitemBeansWithoutCondition==null) this.lstStatitemBeansWithoutCondition=new ArrayList<AbsListReportStatItemBean>();
            this.lstStatitemBeansWithoutCondition.add(staticitembean);
        }else
        {//此统计项有动态条件
            if(this.lstStatitemBeansWithCondition==null) this.lstStatitemBeansWithCondition=new ArrayList<AbsListReportStatItemBean>();
            this.lstStatitemBeansWithCondition.add(staticitembean);
        }
    }
    
    public FormatBean getFbean()
    {
        return fbean;
    }

    public void setFbean(FormatBean fbean)
    {
        this.fbean=fbean;
    }

    public void addStatiColBean(AbsListReportStatiColBean scbean)
    {
        if(lstStatiColBeans==null)
        {
            lstStatiColBeans=new ArrayList<AbsListReportStatiColBean>();
        }
        lstStatiColBeans.add(scbean);
    }

    public void addStatiRowGroupBean(AbsListReportStatiRowGroupBean srgbean)
    {
        if(srgbean==null||srgbean.getRowgroupcolumn()==null
                ||srgbean.getRowgroupcolumn().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+getOwner().getReportBean().getPath()
                    +"失败，添加AbsListReportStatiRowGroupBean对象时，此对象或其rowgroupcolumn属性值为空");
        }
        if(mStatiRowGroupBeans==null)
        {
            mStatiRowGroupBeans=new HashMap<String,AbsListReportStatiRowGroupBean>();
        }
        mStatiRowGroupBeans.put(srgbean.getRowgroupcolumn(),srgbean);
    }

    public void doPostLoad()
    {
        DisplayBean dbean=(DisplayBean)getOwner();
        ReportBean rbean=dbean.getReportBean();
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)dbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        AbsListReportSqlBean alrsbean=(AbsListReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrsbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，它不是数据自动列表报表，不能配置数据统计功能");
        }
        buildStatisticPojoClass(rbean);
        initGetSetMethod(rbean);//初始化pojo的get/set方法，以便运行时可以直接使用
        validateStatisticColsConfig(lstStatiColBeans,alrdbean);
        if(mStatiRowGroupBeans!=null&&mStatiRowGroupBeans.size()>0)
        {
            if(alrdbean.getRowgrouptype()!=1&&alrdbean.getRowgrouptype()!=2)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，当前报表不是行分组显示报表，不能配置行分组统计功能");
            }
            for(Entry<String,AbsListReportStatiRowGroupBean> entry:mStatiRowGroupBeans.entrySet())
            {
                if(entry==null) continue;
                entry.getValue().validateStatisticColsConfig(this,(DisplayBean)alrdbean.getOwner());
            }
        }
        buildStatisticSql();
    }

   
    private void buildStatisticPojoClass(ReportBean reportbean)
    {
        List<String> lstProperties=new ArrayList<String>();
        List<IDataType> lstDataTypes=new ArrayList<IDataType>();
        List<AbsListReportStatItemBean> lstAllStaticItems=this.getLstAllStatitemBeans();
        for(AbsListReportStatItemBean statItemBeanTmp:lstAllStaticItems)
        {
            if(lstProperties.contains(statItemBeanTmp.getProperty()))
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，property属性"+statItemBeanTmp.getProperty()+"存在重复");
            }
            lstProperties.add(statItemBeanTmp.getProperty());
            lstDataTypes.add(statItemBeanTmp.getDatatypeObj());
        }
        getSColPropertiesAndTypes(reportbean,lstProperties,lstDataTypes,lstStatiColBeans);
        if(this.mStatiRowGroupBeans!=null&&this.mStatiRowGroupBeans.size()>0)
        {
            for(Entry<String,AbsListReportStatiRowGroupBean> entryTmp:this.mStatiRowGroupBeans.entrySet())
            {
                AbsListReportStatiRowGroupBean srgbean=entryTmp.getValue();
                getSColPropertiesAndTypes(reportbean,lstProperties,lstDataTypes,srgbean.getLstStatiColBeans());
            }
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrdbean.getLstRowgroupColsColumn()!=null)
            {
                for(String groupcol:alrdbean.getLstRowgroupColsColumn())
                {
                    if(groupcol==null||groupcol.trim().equals("")) continue;
                    if(lstProperties.contains(groupcol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，不能配置<scol/>的property属性为"+groupcol+"，它已经是分组列的column值");
                    }
                    lstProperties.add(groupcol);
                    lstDataTypes.add(Config.getInstance().getDataTypeByClass(VarcharType.class));//显示的列都是字符串类型
                }
            }
        }
        ClassPool pool= ClassPoolAssistant.getInstance().createClassPool();
        CtClass cclass=pool.makeClass(Consts.BASE_PACKAGE_NAME+".Pojo_"+reportbean.getPageBean().getId()+reportbean.getId()+"_statistic");
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
            this.pojoclass=ConfigLoadManager.currentDynClassLoader.loadClass(Consts.BASE_PACKAGE_NAME+".Pojo_"+reportbean.getPageBean().getId()
                    +reportbean.getId()+"_statistic",cclass.toBytecode());
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("生成报表"+reportbean.getPath()+"的存放统计信息的POJO类失败",e);
        }
        cclass.detach();
        pool.clearImportedPackages();
        pool=null;
    }

    private void getSColPropertiesAndTypes(ReportBean reportbean,List<String> lstProperties,List<IDataType> lstDataTypes,
            List<AbsListReportStatiColBean> lstSColBeans)
    {
        if(lstSColBeans!=null&&lstSColBeans.size()>0)
        {
            for(AbsListReportStatiColBean sColBeanTmp:lstSColBeans)
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
            List<AbsListReportStatItemBean> lstAllStaticItems=this.getLstAllStatitemBeans();
            for(AbsListReportStatItemBean statItemBeanTmp:lstAllStaticItems)
            {
                String property=statItemBeanTmp.getProperty();
                String setMethodName="set"+property.substring(0,1).toUpperCase()+property.substring(1);
                Method setMethod=this.pojoclass.getMethod(setMethodName,new Class[] { statItemBeanTmp.getDatatypeObj().getJavaTypeClass() });
                statItemBeanTmp.setSetMethod(setMethod);
            }
            initSColGetSetMethod(reportbean,this.lstStatiColBeans);
            if(this.mStatiRowGroupBeans!=null&&this.mStatiRowGroupBeans.size()>0)
            {
                for(Entry<String,AbsListReportStatiRowGroupBean> entryTmp:this.mStatiRowGroupBeans.entrySet())
                {
                    AbsListReportStatiRowGroupBean srgbean=entryTmp.getValue();
                    initSColGetSetMethod(reportbean,srgbean.getLstStatiColBeans());
                    //这里不为分组列对应的成员变量生成get/set方法，在调用的时候再生成
                }
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("初始化报表"+reportbean.getPath()+"统计项的get/set方法失败",e);
        }
    }
    
    private void initSColGetSetMethod(ReportBean reportbean,List<AbsListReportStatiColBean> lstSColBeans) throws Exception
    {
        if(lstSColBeans!=null&&lstSColBeans.size()>0)
        {

            for(AbsListReportStatiColBean sColBeanTmp:lstSColBeans)
            {
               String property=sColBeanTmp.getProperty();
               String getMethodName="get"+property.substring(0,1).toUpperCase()+property.substring(1);
               sColBeanTmp.setGetMethod(pojoclass.getMethod(getMethodName,new Class[] {}));
            }
        }
    }
    
    private void buildStatisticSql()
    {
        ReportBean reportbean=this.getOwner().getReportBean();
        String sql=null;
        if(reportbean.getSbean().isStoreProcedure())
        {
            sql="%ORIGINAL_SQL%";//因为存储过程中SQL语句是动态拼凑的，所以这里放上占位符，而不是真正的SQL语句，以便在存储过程中真正替换
        }else
        {
            sql=reportbean.getSbean().getSqlWithoutOrderby();
            sql=Tools.replaceAll(sql,"%orderby%","");
            sql=Tools.replaceAll(sql,Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,reportbean.getSbean().getSql_kernel());
        }
        if(this.lstStatitemBeansWithoutCondition!=null&&this.lstStatitemBeansWithoutCondition.size()>0)
        {
            StringBuffer statisticColumnsBuf=new StringBuffer();
            for(AbsListReportStatItemBean statItemBeanTmp:this.lstStatitemBeansWithoutCondition)
            {
                statisticColumnsBuf.append(statItemBeanTmp.getValue()).append(" as ").append(statItemBeanTmp.getProperty()).append(",");
            }
            if(statisticColumnsBuf.charAt(statisticColumnsBuf.length()-1)==',')
            {
                statisticColumnsBuf.deleteCharAt(statisticColumnsBuf.length()-1);
            }
            this.statiSqlWithoutCondition="select "+statisticColumnsBuf.toString()+" from ("+sql+") tableStati";
        }
        if(this.lstStatitemBeansWithCondition!=null&&this.lstStatitemBeansWithCondition.size()>0)
        {
            this.statiSqlWithCondition="select %SELECTEDCOLUMNS% from (select * from ("+sql+") tableStati1 %CONDITION%) tableStati2";
        }
        if(this.mStatiRowGroupBeans!=null)
        {//配置了分组统计，则创建每个分组的统计数据的SQL语句后面的group by  having短语，以便运行时可以直接使用
            for(Entry<String,AbsListReportStatiRowGroupBean> entryTmp:this.mStatiRowGroupBeans.entrySet())
            {
                AbsListReportStatiRowGroupBean srgbean=entryTmp.getValue();
                srgbean.buildStatisticSqlGroupby(reportbean.getDbean());
            }
        }
    }
    
    void validateStatisticColsConfig(List<AbsListReportStatiColBean> lstStatisticColBeans,
            AbsListReportDisplayBean alrdbean)
    {
        if(lstStatisticColBeans==null||lstStatisticColBeans.size()==0) return;
        ReportBean reportbean=alrdbean.getOwner().getReportBean();
        if(reportbean.getDbean().isColselect())
        {
            if(lstStatisticColBeans.size()>1)
            {
                throw new WabacusConfigLoadingException("加载报表"
                        +alrdbean.getOwner().getReportBean().getPath()
                        +"失败，此报表需要提供客户端列选择功能，因此为它配置统计信息时，不能配置多个<scol/>，只能配置一个，在其中显示所有统计信息，它将占据整行");
            }
            String valuestyleproperty=lstStatisticColBeans.get(0).getValuestyleproperty();
            String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(colspan!=null&&!colspan.trim().equals(""))
            {
                throw new WabacusConfigLoadingException(
                        "加载报表"
                                +reportbean.getPath()
                                +"失败，此报表需要提供客户端列选择功能，因此不能在<scol/>中配置colspan属性，或在valuestyleproperty属性中配置colspan值");
            }
        }else
        {
            calColSpanAndStartColIdx(lstStatisticColBeans,alrdbean,alrdbean.getDefaultColumnCount());
        }
    }

    void calColSpanAndStartColIdx(List<AbsListReportStatiColBean> lstStatisticColBeans,
            AbsListReportDisplayBean alrdbean,int colcount)
    {
        int deltaCount=0;
        if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
        {
            deltaCount=alrdbean.getRowGroupColsNum()-1;
        }
        if(lstStatisticColBeans.size()==1)
        {
            lstStatisticColBeans.get(0).setPlainexcel_startcolidx(0);
            String valuestyleproperty=lstStatisticColBeans.get(0).getValuestyleproperty();
            String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(colspan==null||colspan.trim().equals(""))
            {
                valuestyleproperty=valuestyleproperty+" colspan=\""+colcount+"\"";
            }else
            {
                int icolspan=Integer.parseInt(colspan);
                if(icolspan!=colcount)
                {
                    throw new WabacusConfigLoadingException("加载报表"
                            +alrdbean.getOwner().getReportBean().getPath()
                            +"失败，配置的<scol/>的的colspan总数"+icolspan+"与要显示的总列数"+colcount+"不相等");
                }
            }
            lstStatisticColBeans.get(0).setPlainexcel_colspan(colcount+deltaCount);
        }else
        {
            int total_colspan=0;
            boolean isFirstCol=true;
            for(AbsListReportStatiColBean scbeanTmp:lstStatisticColBeans)
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
                {//如果是第一列，则将树形节点省略的部分加上
                    icolspan=icolspan+deltaCount;
                    isFirstCol=false;
                }
                scbeanTmp.setPlainexcel_colspan(icolspan);
            }
            if(total_colspan!=colcount)
            {
                throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()
                        +"失败，配置的所有<scol/>的的colspan总数"+total_colspan+"与要显示的总列数"+colcount+"不相等");
            }
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportStatiBean newBean=(AbsListReportStatiBean)super.clone(owner);
        if(lstStatiColBeans!=null)
        {
            List<AbsListReportStatiColBean> lstStatiColBeansNew=new ArrayList<AbsListReportStatiColBean>();
            for(AbsListReportStatiColBean cb:lstStatiColBeans)
            {
                if(cb!=null)
                {
                    lstStatiColBeansNew.add((AbsListReportStatiColBean)cb.clone());
                }
            }
            newBean.setLstStatiColBeans(lstStatiColBeansNew);
        }
        if(mStatiRowGroupBeans!=null)
        {
            Map<String,AbsListReportStatiRowGroupBean> mStatiRowGroupBeansTemp=new HashMap<String,AbsListReportStatiRowGroupBean>();
            for(Entry<String,AbsListReportStatiRowGroupBean> entry:mStatiRowGroupBeans.entrySet())
            {
                if(entry==null) continue;
                mStatiRowGroupBeansTemp.put(entry.getKey(),(AbsListReportStatiRowGroupBean)entry
                        .getValue().clone());
            }
            newBean.setMStatiRowGroupBeans(mStatiRowGroupBeansTemp);
        }
        if(fbean!=null)
        {
            newBean.setFbean((FormatBean)fbean.clone(null));
        }
        if(this.lstStatitemBeansWithCondition!=null)
        {
            List<AbsListReportStatItemBean> lstItemBeansNew=new ArrayList<AbsListReportStatItemBean>();
            for(AbsListReportStatItemBean itemBeanTmp:this.lstStatitemBeansWithCondition)
            {
                lstItemBeansNew.add((AbsListReportStatItemBean)itemBeanTmp.clone());
            }
            newBean.lstStatitemBeansWithCondition=lstItemBeansNew;
        }
        if(this.lstStatitemBeansWithoutCondition!=null)
        {
            List<AbsListReportStatItemBean> lstItemBeansNew=new ArrayList<AbsListReportStatItemBean>();
            for(AbsListReportStatItemBean itemBeanTmp:this.lstStatitemBeansWithoutCondition)
            {
                lstItemBeansNew.add((AbsListReportStatItemBean)itemBeanTmp.clone());
            }
            newBean.lstStatitemBeansWithoutCondition=lstItemBeansNew;
        }
        return newBean;
    }
}
