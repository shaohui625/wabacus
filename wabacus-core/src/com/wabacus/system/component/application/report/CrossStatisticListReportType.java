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
package com.wabacus.system.component.application.report;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.itextpdf.text.Element;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ClassPoolAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSqlBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupTitlePositionBean;
import com.wabacus.system.component.application.report.configbean.CrossStatisticListColBean;
import com.wabacus.system.component.application.report.configbean.CrossStatisticListDisplayBean;
import com.wabacus.system.component.application.report.configbean.CrossStatisticListGroupBean;
import com.wabacus.system.component.application.report.configbean.CrossStatisticListReportBean;
import com.wabacus.system.component.application.report.configbean.CrossStatisticListSqlBean;
import com.wabacus.system.component.application.report.configbean.CrossStatisticListStatisBean;
import com.wabacus.system.component.application.report.configbean.ICrossStatisticColAndGroup;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.format.IFormat;
import com.wabacus.system.intercept.RowDataByInterceptor;
import com.wabacus.system.resultset.GetAllResultSetByPreparedSQL;
import com.wabacus.system.resultset.GetAllResultSetBySQL;
import com.wabacus.system.resultset.ISQLType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class CrossStatisticListReportType extends UltraListReportType
{
    public final static String KEY=CrossStatisticListReportType.class.getName();

    public final static int COL_GROUP_STARTID=1000;

    private static Log log=LogFactory.getLog(CrossStatisticListReportType.class);

    private Map<String,ColAndGroupTitlePositionBean> mDynColGroupPositionBeans;

    private List lstAllDisplayColAndGroups;

    private List<ColBean> lstColBeansForVerticalStatis;

    private String verticalStatiSql;

    public CrossStatisticListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    private boolean hasLoadedData=false;//此报表是否已经加载过数据，这里一定要定义成private，而不能定义成protected，因为有可能被子类的super.loadReportData()调用，因此不能父子报表共用hasLoadedData变量

    public boolean isLoadedReportData()
    {
        return this.hasLoadedData;
    }
    
    protected void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        super.setHasLoadedDataFlag(hasLoadedDataFlag);
        this.hasLoadedData=hasLoadedDataFlag;
    }

    public void loadReportData()
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        super.initLoadReportData();
        List<ColBean> lstCbeans=new ArrayList<ColBean>();
        List lstAllChildren=new ArrayList();
        for(ColBean cbTmp:rbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTmp.getDisplaytype())) lstCbeans.add(cbTmp);
        }
        CrossStatisticListDisplayBean cslrdbean=(CrossStatisticListDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions=null;//不包含新生成动态列的各列位置信息
        if((cdb.getAttributes().get("authroize_col_display")!=null
                &&String.valueOf(cdb.getAttributes().get("authroize_col_display")).trim().equals("false"))||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&alrbean.hasControllCol()))
        {
            mStaticColAndGroupTitlePostions=calPosition(rbean,cslrdbean.getLstChildren());
        }else
        {
            mStaticColAndGroupTitlePostions=cslrdbean.getMChildrenDefaultPositions();
        }
        ICrossStatisticColAndGroup topCrossStatiColAndGroupBean=null;
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        ColBean cbTmp;
        CrossStatisticListGroupBean cslgbeanTmp;
        UltraListReportGroupBean ulrgbeanTmp;
        CrossStatisticListColBean cslrcbeanTmp;
        for(Object childObj:cslrdbean.getLstChildren())
        {
            if(childObj instanceof ColBean)
            {
                cbTmp=(ColBean)childObj;
                cslrcbeanTmp=(CrossStatisticListColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(cslrcbeanTmp==null)
                {
                    cgpositionBeanTmp=mStaticColAndGroupTitlePostions.get(cbTmp.getColid());
                    if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;
                    lstAllChildren.add(cbTmp);
                    lstCbeans.add(cbTmp);
                }else
                {
                    if(cslrcbeanTmp.isAllStatisticItemsHidden(mStaticColAndGroupTitlePostions)) continue;
                    lstCbeans.add(null);//用null做为占位符，以便稍后取出动态列后放在这个位置
                    lstAllChildren.add(null);
                    topCrossStatiColAndGroupBean=cslrcbeanTmp;
                }
            }else if(childObj instanceof CrossStatisticListGroupBean)
            {//配置的交叉列是一个<group/>
                cslgbeanTmp=(CrossStatisticListGroupBean)childObj;
                if(cslgbeanTmp.getStatisColBean().isAllStatisticItemsHidden(mStaticColAndGroupTitlePostions)) continue;
                lstCbeans.add(null);
                lstAllChildren.add(null);
                topCrossStatiColAndGroupBean=(ICrossStatisticColAndGroup)childObj;
            }else if(childObj instanceof UltraListReportGroupBean)
            {
                ulrgbeanTmp=(UltraListReportGroupBean)childObj;
                cgpositionBeanTmp=mStaticColAndGroupTitlePostions.get(ulrgbeanTmp.getGroupid());
                if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;
                lstAllChildren.add(ulrgbeanTmp);
                ulrgbeanTmp.getAllColBeans(lstCbeans,mStaticColAndGroupTitlePostions);
            }
        }

        SqlBean sqlbean=rbean.getSbean();
        CrossStatisticListSqlBean cslrsbean=(CrossStatisticListSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        ResultSet rs=null;
        try
        {
            String selectedCols="";
            List lstDynChildren=new ArrayList();
            StringBuffer allDynColConditonsBuf=new StringBuffer();
            
            if(topCrossStatiColAndGroupBean!=null//存在要显示的统计项
                    &&(mStaticColAndGroupTitlePostions.get(topCrossStatiColAndGroupBean.getColumn()).getDisplaymode()>0||(topCrossStatiColAndGroupBean
                            .getLstTablenameConditions()!=null&&topCrossStatiColAndGroupBean.getLstTablenameConditions().size()>0)))
            {
                log.debug("Execute Sql:::"+cslrsbean.getSql_getcols());
                ISQLType ImpISQLType=null;
                if(rbean.getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
                {
                    ImpISQLType=new GetAllResultSetByPreparedSQL();
                }else
                {
                    ImpISQLType=new GetAllResultSetBySQL();
                }
                Object objTmp=ImpISQLType.getResultSet(rrequest,rbean,topCrossStatiColAndGroupBean,cslrsbean.getSql_getcols(),
                        topCrossStatiColAndGroupBean.getLstTablenameConditions());
                if(objTmp==null) return;
                if(!(objTmp instanceof ResultSet))
                {
                    throw new WabacusRuntimeException("获取报表："+rbean.getPath()+"的动态标题列失败，不能在查询交叉统计报表的标题时在拦截器中返回非ResultSet类型的数据");
                }
                rs=(ResultSet)objTmp;
                if(topCrossStatiColAndGroupBean instanceof CrossStatisticListColBean)
                {
                    selectedCols=getListDynamicGroupAndCols(rs,(CrossStatisticListColBean)topCrossStatiColAndGroupBean,lstDynChildren,
                            mStaticColAndGroupTitlePostions,allDynColConditonsBuf);
                }else if(topCrossStatiColAndGroupBean instanceof CrossStatisticListGroupBean)
                {
                    selectedCols=getListDynamicGroupAndCols(rs,(CrossStatisticListGroupBean)topCrossStatiColAndGroupBean,lstDynChildren,
                            mStaticColAndGroupTitlePostions,allDynColConditonsBuf);
                }
            }
            if(!cslrdbean.isAllDisplayStatisticItemsHidden(mStaticColAndGroupTitlePostions))
            {
                StringBuffer tmpBuf=new StringBuffer();
                String allColConditions=allDynColConditonsBuf.toString().trim();
                if(allColConditions.endsWith("or")) allColConditions=allColConditions.substring(0,allColConditions.length()-2);
                cslrdbean.createStatisForWholeRow(rrequest,tmpBuf,lstDynChildren,0,mStaticColAndGroupTitlePostions,allColConditions);//创建针对整个报表统计的列
                if(!tmpBuf.toString().trim().equals(""))
                {
                    if(selectedCols.trim().equals(""))
                    {
                        selectedCols=tmpBuf.toString();
                    }else
                    {
                        selectedCols=selectedCols+","+tmpBuf.toString();
                    }
                }
            }
            /**现在不用执行，因为父类的doPostLoad()方法没有做什么处理。*/
            
            createAllDisplayChildren(lstCbeans,lstAllChildren,lstDynChildren);
            calColGroupPositionDynamically(cslrdbean,lstDynChildren,mStaticColAndGroupTitlePostions);
            createColBeansForVerticalStati(cslrdbean,cslrsbean,lstDynChildren,selectedCols,cdb,mStaticColAndGroupTitlePostions,
                    topCrossStatiColAndGroupBean==null?null:topCrossStatiColAndGroupBean.getStatisColBean());

            String sqlKernel=sqlbean.getSql_kernel();
            sqlKernel=replaceDynColPlaceHolder(sqlKernel,selectedCols);
            rrequest.setAttribute(rbean.getId(),"DYN_SQL",sqlKernel);
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("获取报表："+rbean.getPath()+"的动态标题列失败",e);
        }finally
        {
            try
            {
                if(rs!=null) rs.close();
            }catch(SQLException e)
            {
                e.printStackTrace();
            }
        }
        super.loadReportData();
    }

    private String replaceDynColPlaceHolder(String sql,String realSelectCols)
    {
        if(realSelectCols==null||realSelectCols.trim().equals(""))
        {
            int idx=sql.indexOf("%dyncols%");
            while(idx>0)
            {
                String sql1=sql.substring(0,idx).trim();
                String sql2=sql.substring(idx+"%dyncols%".length());
                while(sql1.endsWith(","))
                    sql1=sql1.substring(0,sql1.length()-1).trim();
                sql=sql1+" "+sql2;
                idx=sql.indexOf("%dyncols%");
            }
        }else
        {
            sql=Tools.replaceAll(sql,"%dyncols%",realSelectCols);
        }
        return sql;
    }

    private void createColBeansForVerticalStati(CrossStatisticListDisplayBean mydisbean,CrossStatisticListSqlBean cslrsbean,List lstDynChildren,
            String selectedCols,CacheDataBean cdb,Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions,
            CrossStatisticListColBean cslrcbean)
    {
        if(lstDynChildren==null||lstDynChildren.size()==0||selectedCols==null||selectedCols.trim().equals("")) return;//没有动态显示列
        List<ColBean> lstCBeansForVertiStatis=null;
        if((cdb.getAttributes().get("authroize_col_display")!=null&&String.valueOf(cdb.getAttributes().get("authroize_col_display")).trim().equals(
                "false"))
                ||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&alrbean.hasControllCol()))
        {
            lstCBeansForVertiStatis=createColBeansForVerticalStatisticRow((DisplayBean)mydisbean.getOwner(),mydisbean.getLstChildren(),cslrcbean,
                    mStaticColAndGroupTitlePostions);
        }else
        {
            lstCBeansForVertiStatis=mydisbean.getLstDefaultVerticalStatisColBeans();
        }
        if(lstCBeansForVertiStatis!=null&&lstCBeansForVertiStatis.size()>0)
        {
            lstColBeansForVerticalStatis=new ArrayList<ColBean>();
            for(ColBean cbTmp:lstCBeansForVertiStatis)
            {
                if(cbTmp!=null)
                {
                    lstColBeansForVerticalStatis.add(cbTmp);
                }else
                {
                    for(Object objBeanTmp:lstDynChildren)
                    {
                        if(objBeanTmp instanceof ColBean)
                        {
                            lstColBeansForVerticalStatis.add((ColBean)objBeanTmp);
                        }else
                        {//UltraReportListGroupBean
                            ((UltraListReportGroupBean)objBeanTmp).getAllColBeans(lstColBeansForVerticalStatis,null);
                        }
                    }
                }
            }
            verticalStatiSql=cslrsbean.getSql_getVerticalStatisData();
            verticalStatiSql=Tools.replaceAll(verticalStatiSql,"%dyncols%",selectedCols);
        }
    }

    private void createAllDisplayChildren(List<ColBean> lstCbeans,List lstAllChildren,List lstDynChildren)
    {
        lstAllDisplayColAndGroups=new ArrayList();
        if(lstAllChildren.size()>0)
        {
            for(Object objTmp:lstAllChildren)
            {
                if(objTmp==null)
                {
                    lstAllDisplayColAndGroups.addAll(lstDynChildren);
                }else
                {
                    lstAllDisplayColAndGroups.add(objTmp);
                }
            }
        }else if(lstDynChildren!=null)
        {
            lstAllDisplayColAndGroups.addAll(lstDynChildren);
        }
        List<ColBean> lstCbeansNew=new ArrayList<ColBean>();
        if(lstCbeans.size()>0)
        {
            for(ColBean cbTmp:lstCbeans)
            {
                if(cbTmp==null)
                {
                    for(Object objBeanTmp:lstDynChildren)
                    {
                        if(objBeanTmp instanceof ColBean)
                        {
                            lstCbeansNew.add((ColBean)objBeanTmp);
                        }else
                        {
                            ((UltraListReportGroupBean)objBeanTmp).getAllColBeans(lstCbeansNew,null);//将<group/>下的所有ColBean取出放在lstCbeansNew中
                        }
                    }
                }else
                {
                    lstCbeansNew.add(cbTmp);
                }
            }
        }else
        {//没有普通列，则将所有动态生成的交叉统计列加入进行显示
            for(Object objBeanTmp:lstDynChildren)
            {
                if(objBeanTmp instanceof ColBean)
                {
                    lstCbeansNew.add((ColBean)objBeanTmp);
                }else
                {
                    ((UltraListReportGroupBean)objBeanTmp).getAllColBeans(lstCbeansNew,null);//将<group/>下的所有ColBean取出放在lstCbeansNew中
                }
            }
        }
        rrequest.getCdb(rbean.getId()).setLstDynOrderColBeans(lstCbeansNew);
    }

    private String getListDynamicGroupAndCols(ResultSet rs,CrossStatisticListColBean cslcbean,List lstDynChildren,
            Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions,StringBuffer allColConditionsBuf) throws SQLException
    {
        int colidx=0;
        StringBuffer dynSelectedColsBuf=new StringBuffer();
        StringBuffer conditionBuf;//存放每个动态列值构成的查询条件
        Object headDataObj=getDataHeadPojoInstance();
        while(rs.next())
        {
            conditionBuf=new StringBuffer();
            colidx=cslcbean.createDisplayBean(rrequest,rs,dynSelectedColsBuf,conditionBuf,allColConditionsBuf,rbean.getDbean(),lstDynChildren,
                    headDataObj,colidx,null,mStaticColAndGroupTitlePostions);
        }
        if(headDataObj!=null)
        {
            if(headDataObj instanceof IFormat)
            {
                ((IFormat)headDataObj).format(rrequest,rbean);
            }
            getRealDisplayLabel(headDataObj,lstDynChildren);
        }
        if(dynSelectedColsBuf.length()>0&&dynSelectedColsBuf.charAt(dynSelectedColsBuf.length()-1)==',')
            dynSelectedColsBuf.deleteCharAt(dynSelectedColsBuf.length()-1);
        return dynSelectedColsBuf.toString();
    }

    private String getListDynamicGroupAndCols(ResultSet rs,CrossStatisticListGroupBean cslgroupBean,List lstDynChildren,
            Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions,StringBuffer allColConditionsBuf) throws SQLException
    {
        StringBuffer dynSelectedColsBuf=new StringBuffer();
        StringBuffer conditionBuf;
        int colidx=0;
        Map<String,String> mCurrentGroupValues=new HashMap<String,String>();//用于存放每个<group/>上一个行的显示值，以便它们决定是否要新开一个GroupBean
        Map<String,String> mAllColConditionsInGroup=new HashMap<String,String>();//存放当前正在处理的每个分组包括的所有<col/>对应的条件,并用or拼凑在一起,以便对整个分组进行统计时能取到此分组的条件
        Object headDataObj=getDataHeadPojoInstance();
        while(rs.next())
        {
            conditionBuf=new StringBuffer();
            colidx=cslgroupBean.createDisplayBean(rrequest,rs,mCurrentGroupValues,mAllColConditionsInGroup,dynSelectedColsBuf,conditionBuf,allColConditionsBuf,
                    rbean.getDbean(),lstDynChildren,headDataObj,colidx,mStaticColAndGroupTitlePostions);
        }
        if(lstDynChildren.size()>0)
        {
            colidx=cslgroupBean.createStatisticForLastWholeGroup(rrequest,dynSelectedColsBuf,rbean.getDbean(),colidx,
                    (UltraListReportGroupBean)lstDynChildren.get(lstDynChildren.size()-1),mAllColConditionsInGroup,mStaticColAndGroupTitlePostions);
        }
        if(headDataObj!=null)
        {//如果有存放表头数据的POJO对象，则从这里取出每一动态列的真正显示数据
            if(headDataObj instanceof IFormat)
            {
                ((IFormat)headDataObj).format(rrequest,rbean);
            }
            getRealDisplayLabel(headDataObj,lstDynChildren);
        }
        if(dynSelectedColsBuf.length()>0&&dynSelectedColsBuf.charAt(dynSelectedColsBuf.length()-1)==',')
            dynSelectedColsBuf.deleteCharAt(dynSelectedColsBuf.length()-1);
        return dynSelectedColsBuf.toString();
    }

    private void getRealDisplayLabel(Object headDataObj,List lstDynChildren)
    {
        if(lstDynChildren==null||lstDynChildren.size()==0) return;
        for(Object objBeanTmp:lstDynChildren)
        {
            String label;
            if(objBeanTmp instanceof ColBean)
            {
                label=((ColBean)objBeanTmp).getLabel();
                if(label!=null&&label.indexOf("_")==0&&label.substring(1).indexOf("_")>0)
                {
                    ((ColBean)objBeanTmp).setLabel((String)ReportAssistant.getInstance().getCrossStatisDataFromPOJO(rbean,headDataObj,label));
                }
            }else
            {
                getRealDisplayLabel((UltraListReportGroupBean)objBeanTmp,headDataObj);
            }
        }
    }

    private void getRealDisplayLabel(UltraListReportGroupBean objBeanTmp,Object headDataObj)
    {
        String label=objBeanTmp.getLabel();
        if(label!=null&&label.indexOf("_")==0&&label.substring(1).indexOf("_")>0)
        {
            objBeanTmp.setLabel((String)ReportAssistant.getInstance().getCrossStatisDataFromPOJO(rbean,headDataObj,label));
        }
        for(Object objTmp:objBeanTmp.getLstChildren())
        {
            if(objTmp instanceof ColBean)
            {
                label=((ColBean)objTmp).getLabel();
                if(label!=null&&label.indexOf("_")==0&&label.substring(1).indexOf("_")>0)
                {
                    ((ColBean)objTmp).setLabel((String)ReportAssistant.getInstance().getCrossStatisDataFromPOJO(rbean,headDataObj,label));
                }
            }else
            {//UltraReportListGroupBean
                getRealDisplayLabel((UltraListReportGroupBean)objTmp,headDataObj);
            }
        }
    }

    private Object getDataHeadPojoInstance()
    {
        Object headDataObj=null;
        try
        {
            CrossStatisticListReportBean cslrbean=(CrossStatisticListReportBean)rbean.getExtendConfigDataForReportType(KEY);
            if(cslrbean!=null&&cslrbean.getDataHeaderPojoClass()!=null)
            {
                headDataObj=cslrbean.getDataHeaderPojoClass().newInstance();
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("创建存放报表"+rbean.getPath()+"的动态表头数据的对象失败",e);
        }
        return headDataObj;
    }

    private void calColGroupPositionDynamically(CrossStatisticListDisplayBean mydisbean,List lstDynChildren,
            Map<String,ColAndGroupTitlePositionBean> mStaticColAndGroupTitlePostions)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL||(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT&&this.alrbean.hasControllCol()))
        {
            
            mDynColGroupPositionBeans=super.calPosition(rbean,this.lstAllDisplayColAndGroups,null);
            calPositionForStandardExcel(lstAllDisplayColAndGroups,null,mDynColGroupPositionBeans);
        }else
        {
            mDynColGroupPositionBeans=(Map<String,ColAndGroupTitlePositionBean>)((HashMap<String,ColAndGroupTitlePositionBean>)mStaticColAndGroupTitlePostions)
                    .clone();
            ColAndGroupTitlePositionBean positionBeanTmp=mDynColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
            int maxrowcount=positionBeanTmp.getRowspan();//取到总行数
            for(Object objBeanTmp:lstDynChildren)
            {
                if(objBeanTmp instanceof UltraListReportGroupBean)
                {
                    ((UltraListReportGroupBean)objBeanTmp).calPositionStart(rrequest,mDynColGroupPositionBeans,null);
                    ((UltraListReportGroupBean)objBeanTmp).calPositionEnd(mDynColGroupPositionBeans,new int[] { maxrowcount, 0 });
                }else if(objBeanTmp instanceof ColBean)
                {
                    positionBeanTmp=new ColAndGroupTitlePositionBean();
                    positionBeanTmp.setRowspan(maxrowcount);
                    positionBeanTmp.setDisplaymode(1);
                    mDynColGroupPositionBeans.put(((ColBean)objBeanTmp).getColid(),positionBeanTmp);
                }
            }
        }
    }

    protected String showDataHeaderPart()
    {
        StringBuffer resultBuf=new StringBuffer();
        ColAndGroupTitlePositionBean positionBeanTmp=this.mDynColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
        int maxrowcount=positionBeanTmp.getRowspan();
        String thstyleproperty=null;
        if(this.rbean.getInterceptor()!=null)
        {
            RowDataByInterceptor rowdataObj=this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this,rrequest,-1,this.cacheDataBean.getTotalColCount(),this.getLstDisplayColBeans());
            if(rowdataObj!=null)
            {
                if(rowdataObj.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObj.getInsertDisplayRowHtml());
                thstyleproperty=rowdataObj.getDynTrStyleproperty();
                if(!rowdataObj.isShouldDisplayThisRow()) return resultBuf.toString();
            }
        }
        if(thstyleproperty==null) thstyleproperty="";
        resultBuf.append("<tr  class='"+getDataHeaderTrClassName()+"' ").append(thstyleproperty).append(">");
        //        lstChildren=sortChildrenByDynColOrders(lstChildren);
        resultBuf.append(showLabel(lstAllDisplayColAndGroups,this.mDynColGroupPositionBeans,null));
        resultBuf.append("</tr>");
        UltraListReportGroupBean groupBean;
        Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();
        for(int i=1;i<maxrowcount;i++)
        {
            resultBuf.append("<tr  class='"+getDataHeaderTrClassName()+"' ").append(thstyleproperty).append(">");
            for(Object obj:lstAllDisplayColAndGroups)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                Integer layer=mGroupLayers.get(groupBean.getGroupid());
                if(layer==null) layer=1;
                List lstChildrenLocal=groupBean.getDisplayChildrenByLayer(groupBean.getLstChildren(),this.mDynColGroupPositionBeans,layer,i+1,
                        groupBean.getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                mGroupLayers.put(groupBean.getGroupid(),layer+1);
                
                resultBuf.append(showLabel(lstChildrenLocal,this.mDynColGroupPositionBeans,null));
            }
            resultBuf.append("</tr>");
        }
        if(this.fixedDataBean!=null&&this.fixedDataBean.getFixedrowscount()==Integer.MAX_VALUE)
        {//如果指定了固定标题行(这里不能从alrbean中取，因为要考虑在拦截器中的动态设置冻结记录数)
            this.fixedDataBean.setFixedrowscount(maxrowcount);
        }
        return resultBuf.toString();
    }

    protected String showStatisticDataForWholeReport()
    {
        StringBuffer resultBuf=new StringBuffer();
        if(cacheDataBean.getPagesize()>0&&cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount())
        {
            return "";
        }
        if(this.lstColBeansForVerticalStatis==null||lstColBeansForVerticalStatis.size()==0||this.verticalStatiSql==null
                ||verticalStatiSql.trim().equals("")) return "";
        
        Object dataObj=getStatisticDataForWholeReport();
        if(dataObj!=null&&dataObj instanceof IFormat)
        {
            ((IFormat)dataObj).format(rrequest,rbean);
        }
        resultBuf.append("<tr  class='cls-data-tr'>");
        for(ColBean cbeanTmp:lstColBeansForVerticalStatis)
        {
            resultBuf.append("<td class='cls-data-td-list' ");
            if(cbeanTmp.getProperty().equals("[VERTICAL-STATIS-CONSTANTCOL]"))
            {
                resultBuf.append(cbeanTmp.getValuestyleproperty()).append(">");
                resultBuf.append(cbeanTmp.getLabel()).append("</td>");
                continue;
            }
            resultBuf.append(cbeanTmp.getValuestyleproperty()).append(">");
            resultBuf.append(cbeanTmp.getDisplayValue(dataObj,rrequest));
            resultBuf.append("</td>");
        }
        resultBuf.append("</tr>");
        return resultBuf.toString();
    }

    private Object getStatisticDataForWholeReport()
    {
        ResultSet rs=null;
        try
        {
            AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
            Object objTmp=impISQLType.getResultSet(rrequest,this,this,verticalStatiSql);
            if(!(objTmp instanceof ResultSet))
            {
                throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"数据失败，在加载数据的前置动作中，如果是统计数据的SQL语句，则只能返回SQL语句或ResultSet，不能返回加载好的List对象");
            }
            rs=(ResultSet)objTmp;
            Object dataObj=null;
            if(rs.next())
            {
                dataObj=rbean.getPojoclass().newInstance();
                Object objVal;
                for(ColBean cbeanTmp:lstColBeansForVerticalStatis)
                {
                    if(cbeanTmp.getProperty().equals("[VERTICAL-STATIS-CONSTANTCOL]")) continue;
                    objVal=cbeanTmp.getDatatypeObj().getColumnValue(rs,cbeanTmp.getColumn(),dbtype);
                    Method setMDataMethodForJava=rbean.getPojoclass().getMethod("setStatisticData",new Class[] { String.class, Object.class });
                    setMDataMethodForJava.invoke(dataObj,new Object[] { cbeanTmp.getColumn(), objVal });
                }
            }
            return dataObj;
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("查询交叉统计报表"+rbean.getPath()+"的统计数据失败",e);
        }finally
        {
            try
            {
                if(rs!=null) rs.close();
            }catch(SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    protected void showReportTitleOnPlainExcel(Workbook workbook)
    {
        String plainexceltitle=null;
        if(this.pedebean!=null) plainexceltitle=this.pedebean.getPlainexceltitle();
        if("none".equals(plainexceltitle)) return;
        if("column".equals(plainexceltitle))
        {//标题部分显示字段名
            super.showReportTitleOnPlainExcel(workbook);
            return;
        }
        ColAndGroupTitlePositionBean positionBeanTmp=this.mDynColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
        int maxrowcount=positionBeanTmp.getRowspan();
        showLabelInPlainExcel(workbook,excelSheet,lstAllDisplayColAndGroups,mDynColGroupPositionBeans);
        UltraListReportGroupBean groupBean;
        Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();
        for(int i=1;i<maxrowcount;i++)
        {
            for(Object obj:lstAllDisplayColAndGroups)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                Integer layer=mGroupLayers.get(groupBean.getGroupid());
                if(layer==null) layer=1;
                List lstChildrenLocal=groupBean.getDisplayChildrenByLayer(groupBean.getLstChildren(),this.mDynColGroupPositionBeans,layer,i+1,
                        groupBean.getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                mGroupLayers.put(groupBean.getGroupid(),layer+1);
                showLabelInPlainExcel(workbook,excelSheet,lstChildrenLocal,mDynColGroupPositionBeans);
            }
        }
        excelRowIdx+=maxrowcount;
    }

    protected void showStatisticDataInPlainExcelForWholeReport(Workbook workbook,CellStyle dataCellStyle)
    {
        if(this.lstColBeansForVerticalStatis==null||lstColBeansForVerticalStatis.size()==0||this.verticalStatiSql==null
                ||verticalStatiSql.trim().equals("")) return;
        if(this.cacheDataBean.getPagesize()>0&&this.cacheDataBean.getFinalPageno()!=this.cacheDataBean.getPagecount()) return;//如果是分批导出，且现在还没导到最后一批
        Object dataObj=getStatisticDataForWholeReport();
        int startcolidx=0;
        int endcolidx=-1;
        CellRangeAddress region;
        Object objvalueTmp;
        String labelTmp;
        IDataType dataTypeObj=new DoubleType();
        Row dataRow=excelSheet.createRow(excelRowIdx);
        for(ColBean cbeanTmp:lstColBeansForVerticalStatis)
        {
            startcolidx=endcolidx+1;
            if(cbeanTmp.getProperty().equals("[VERTICAL-STATIS-CONSTANTCOL]"))
            {
                String colspan=Tools.getPropertyValueByName("colspan",cbeanTmp.getValuestyleproperty(),true);
                if(colspan==null||colspan.trim().equals("")) colspan="1";
                int icolspan=Integer.parseInt(colspan);
                endcolidx=startcolidx+icolspan-1;
                labelTmp=cbeanTmp.getLabel();
                labelTmp=Tools.replaceAll(labelTmp,"&nbsp;"," ");
                labelTmp=labelTmp.replaceAll("<.*?\\>","");
                region=new CellRangeAddress(excelRowIdx,excelRowIdx,startcolidx,endcolidx);
                StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,excelSheet,region,dataCellStyle,labelTmp);
            }else
            {
                endcolidx=startcolidx;
                Cell cell=dataRow.createCell(endcolidx);
                cell.setCellStyle(dataCellStyle);
                objvalueTmp=cbeanTmp.getRealTypeValue(dataObj,rrequest);
                StandardExcelAssistant.getInstance().setCellValue(workbook,null,cell,objvalueTmp,dataTypeObj);
            }
        }
        excelRowIdx++;
    }

    protected void showDataHeaderOnPdf()
    {
        ColAndGroupTitlePositionBean positionBeanTmp=this.mDynColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
        int maxrowcount=positionBeanTmp.getRowspan();
        showLabelInPdf(lstAllDisplayColAndGroups,mDynColGroupPositionBeans);
        UltraListReportGroupBean groupBean;
        Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();//存放每个顶层分组当前显示的层级数
        for(int i=1;i<maxrowcount;i++)
        {
            for(Object obj:lstAllDisplayColAndGroups)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                Integer layer=mGroupLayers.get(groupBean.getGroupid());
                if(layer==null) layer=1;
                List lstChildrenLocal=groupBean.getDisplayChildrenByLayer(groupBean.getLstChildren(),this.mDynColGroupPositionBeans,layer,i+1,
                        groupBean.getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                mGroupLayers.put(groupBean.getGroupid(),layer+1);
                showLabelInPdf(lstChildrenLocal,mDynColGroupPositionBeans);
            }
        }
    }
    
    protected void showStatisticDataOnPdfForWholeReport()
    {
        if(this.lstColBeansForVerticalStatis==null||lstColBeansForVerticalStatis.size()==0||this.verticalStatiSql==null
                ||verticalStatiSql.trim().equals("")) return;
        if(this.cacheDataBean.getPagesize()>0&&this.cacheDataBean.getFinalPageno()!=this.cacheDataBean.getPagecount()) return;
        Object dataObj=getStatisticDataForWholeReport();
        int icolspan;
        String valueTmp;
        for(ColBean cbeanTmp:lstColBeansForVerticalStatis)
        {
            valueTmp="";
            if(cbeanTmp.getProperty().equals("[VERTICAL-STATIS-CONSTANTCOL]"))
            {
                String colspan=Tools.getPropertyValueByName("colspan",cbeanTmp.getValuestyleproperty(),true);
                if(colspan==null||colspan.trim().equals("")) colspan="1";
                icolspan=Integer.parseInt(colspan);
                valueTmp=cbeanTmp.getLabel();
                valueTmp=Tools.replaceAll(valueTmp,"&nbsp;"," ");
                valueTmp=valueTmp.replaceAll("<.*?\\>","");//去掉html标签
            }else
            {
                icolspan=1;
                valueTmp=cbeanTmp.getDisplayValue(dataObj,rrequest);
            }
            addDataCell(cbeanTmp,valueTmp,1,icolspan,Element.ALIGN_LEFT);
        }
    }
    
    public String getColSelectedMetadata()
    {
        return "";
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        XmlElementBean eleColBean=lstEleColBeans.get(0);
        String condition=eleColBean.attributeValue("condition");
        if(condition!=null&&!condition.trim().equals(""))
        {//当前<col/>是交叉统计列
            if(colbean.getColumn().equals(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，为交叉<col/>配置column属性不能为"
                        +CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT);
            }
            String realvalue=eleColBean.attributeValue("realvalue");
            String tablename=eleColBean.attributeValue("tablename");
            String verticalstatistic=eleColBean.attributeValue("verticalstatistic");
            String verticallabel=eleColBean.attributeValue("verticallabel");
            String verticallabelstyleproperty=eleColBean.attributeValue("verticallabelstyleproperty");
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)colbean.getExtendConfigDataForReportType(KEY);
            if(cslrcbean==null)
            {
                cslrcbean=new CrossStatisticListColBean(colbean);
                colbean.setExtendConfigDataForReportType(KEY,cslrcbean);
            }
            cslrcbean.setCondition(condition.trim());
            if(realvalue!=null) cslrcbean.setRealvalue(realvalue.trim());
            cslrcbean.setVerticalstatistic(verticalstatistic!=null&&verticalstatistic.trim().equalsIgnoreCase("true"));
            cslrcbean.setVerticallabel(verticallabel==null?"":verticallabel.trim());
            cslrcbean.setVerticallabelstyleproperty(verticallabelstyleproperty==null?"":verticallabelstyleproperty.trim());
            if(tablename!=null) cslrcbean.setTablename(tablename.trim());
            List<XmlElementBean> lstEleChildren=eleColBean.getLstChildElements();
            if(lstEleChildren==null||lstEleChildren.size()==0)
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，没有为此报表的交叉<col/>配置<statistic/>");
            }
            List<CrossStatisticListStatisBean> lstStatisBeans=new ArrayList<CrossStatisticListStatisBean>();
            cslrcbean.setLstStatisBeans(lstStatisBeans);
            CrossStatisticListStatisBean statisBean;
            List<String> lstExistIds=new ArrayList<String>();
            for(XmlElementBean eleStaticBeanTmp:lstEleChildren)
            {
                if(!eleStaticBeanTmp.getName().equals("statistic")) continue;
                statisBean=new CrossStatisticListStatisBean(colbean);
                String id=eleStaticBeanTmp.attributeValue("id");
                if(id==null||id.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载交叉统计报表"+colbean.getReportBean().getPath()+"失败，没有为<statistic/>配置id");
                }
                id=id.trim();
                if(lstExistIds.contains(id))
                {
                    throw new WabacusConfigLoadingException("加载交叉统计报表"+colbean.getReportBean().getPath()+"失败，<statistic/>的id值"+id+"存在重复");
                }
                lstExistIds.add(id);
                statisBean.setId(colbean.getColumn()+"."+id);//加上column前缀，方便保持唯一性，尤其是授权的时候，不会与其它<col/>的column有冲突
                String type=eleStaticBeanTmp.attributeValue("type");
                String column=eleStaticBeanTmp.attributeValue("column");
                String label=eleStaticBeanTmp.attributeValue("label");
                String labelstyleproperty=eleStaticBeanTmp.attributeValue("labelstyleproperty");
                String valuestyleproperty=eleStaticBeanTmp.attributeValue("valuestyleproperty");
                String statitems=eleStaticBeanTmp.attributeValue("statitems");
                statisBean.setType(type==null?"":type.toLowerCase().trim());
                statisBean.setColumn(column==null?"":column.trim());
                statisBean.setLstLabels(Tools
                        .parseAllStringToList(Config.getInstance().getResourceString(null,colbean.getPageBean(),label,false),"|"));
                statisBean.setLstLabelstyleproperties(Tools.parseAllStringToList(labelstyleproperty,"|"));
                statisBean.setLstValuestyleproperties(Tools.parseAllStringToList(valuestyleproperty,"|"));
                if(statitems!=null)
                {
                    statitems=statitems.trim();
                    if(statitems.equals(""))
                    {
                        statisBean.setLstStatitems(null);
                    }else
                    {
                        List<String> lstTemp=new UniqueArrayList<String>();
                        lstTemp.addAll(Tools.parseAllStringToList(statitems,"|"));
                        statisBean.setLstStatitems(lstTemp);
                    }
                }
                statisBean.validateConfig();
                lstStatisBeans.add(statisBean);
            }
            if(lstStatisBeans.size()==0)
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，没有为此报表的交叉<col/>配置<statistic/>");
            }
        }else
        {
            super.afterColLoading(colbean,lstEleColBeans);
        }
        return 1;
    }

    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        disbean.clearChildrenInfo();
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrdbean!=null) alrdbean.clearChildrenInfo();
        List<XmlElementBean> lstEleChildren=new ArrayList<XmlElementBean>();
        joinedAllColAndGroupElement(lstEleDisplayBeans,lstEleChildren,disbean.getReportBean());//取到所有要显示的直接子<col/>和<group/>标签对象
        if(lstEleChildren==null||lstEleChildren.size()==0) return 0;
        disbean.setColselect(false);
        CrossStatisticListDisplayBean cslrdbean=(CrossStatisticListDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(cslrdbean==null)
        {
            cslrdbean=new CrossStatisticListDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,cslrdbean);
        }
        List lstChildren=new ArrayList();
        cslrdbean.setLstChildren(lstChildren);
        for(XmlElementBean eleChildBeanTmp:lstEleChildren)
        {
            if(eleChildBeanTmp.getName().equalsIgnoreCase("col"))
            {
                ColBean colbean=ComponentConfigLoadManager.loadColConfig(eleChildBeanTmp,disbean);
                CrossStatisticListColBean cslrcbeanTmp=(CrossStatisticListColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(cslrcbeanTmp==null)
                {
                    disbean.getLstCols().add(colbean);
                }else
                {//交叉统计列
                    if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(colbean.getDisplaytype()))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
                                +"失败，交叉统计报表的动态统计列对应的<col/>的displaytype不能配置为hidden");
                    }
                    colbean.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
                    loadCrossStatisticColAndGroupTableconditios(disbean.getReportBean(),cslrcbeanTmp,eleChildBeanTmp);
                }
                if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(colbean.getDisplaytype())) lstChildren.add(colbean);
            }else if(eleChildBeanTmp.getName().equalsIgnoreCase("group"))
            {
                String condition=eleChildBeanTmp.attributeValue("condition");
                if(condition!=null&&!condition.trim().equals(""))
                {
                    CrossStatisticListGroupBean crossGroupBean=new CrossStatisticListGroupBean(disbean);
                    lstChildren.add(crossGroupBean);
                    loadCrossGroupConfig(crossGroupBean,eleChildBeanTmp,disbean);
                    loadCrossStatisticColAndGroupTableconditios(disbean.getReportBean(),crossGroupBean,eleChildBeanTmp);//加载动态条件配置
                }else
                {
                    UltraListReportGroupBean groupBean=new UltraListReportGroupBean(disbean);
                    lstChildren.add(groupBean);
                    loadGroupConfig(groupBean,eleChildBeanTmp,disbean,null);
                }
            }
        }
        doAfterSuperDisplayLoading(disbean,lstEleDisplayBeans);
        return 1;
    }

    private void loadCrossStatisticColAndGroupTableconditios(ReportBean reportbean,ICrossStatisticColAndGroup topStatiColAndGroupBean,
            XmlElementBean eleTopStatisticColAndGroup)
    {
        if(topStatiColAndGroupBean.getTablename()==null||topStatiColAndGroupBean.getTablename().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败必须为顶层交叉统计列配置tablename属性，在此指定获取动态列数据的表名或SQL语句");
        }
        List<ConditionBean> lstTablenameConditions=new ArrayList<ConditionBean>();
        XmlElementBean eleTableconditions=eleTopStatisticColAndGroup.getChildElementByName("tableconditions");
        if(eleTableconditions!=null)
        {
            List<XmlElementBean> lstConditionEles=eleTableconditions.getLstChildElementsByName("condition");
            if(lstConditionEles!=null&&lstConditionEles.size()>0)
            {//在<tablecondtions/>中配置了条件
                ConditionBean cbTmp;
                for(XmlElementBean eleConBeanTmp:lstConditionEles)
                {
                    if(eleConBeanTmp==null) continue;
                    cbTmp=ComponentConfigLoadManager.loadHiddenConditionConfig(eleConBeanTmp,reportbean);
                    if(cbTmp==null) continue;
                    if(!Tools.isDefineKey("ref",cbTmp.getName())
                            &&(cbTmp.getConditionExpression()==null||cbTmp.getConditionExpression().getValue()==null||cbTmp.getConditionExpression()
                                    .getValue().trim().equals("")))
                    {//如果没有引用在<sql/>中配置的条件，则必须在<condition/>中指定条件表达式
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的name为"+cbTmp.getName()
                                +"的<condition/>失败，没有引用<sql/>中的条件时必须为<value/>配置条件表达式");
                    }
                    lstTablenameConditions.add(cbTmp);
                }
            }
        }
        if(lstTablenameConditions.size()>0)
        {
            topStatiColAndGroupBean.setLstTablenameConditions(lstTablenameConditions);
            if(topStatiColAndGroupBean.getTablename().toLowerCase().trim().indexOf("select ")!=0)
            {
                topStatiColAndGroupBean.setTablename("select * from "+topStatiColAndGroupBean.getTablename()+" where {#condition#}");
            }else if(topStatiColAndGroupBean.getTablename().toLowerCase().trim().indexOf("{#condition#}")<0)
            {
                throw new WabacusConfigLoadingException("加载交叉统计报表"+rbean.getPath()
                        +"失败，为其配置了<tableconditions/>，但没有在tablename属性配置的SQL语句中指定{#condition#}动态条件占位符");
            }
        }else
        {
            topStatiColAndGroupBean.setLstTablenameConditions(null);
        }
    }

    private void loadCrossGroupConfig(CrossStatisticListGroupBean crossGroupBean,XmlElementBean eleGroupBean,DisplayBean disbean)
    {
        String column=eleGroupBean.attributeValue("column");
        String condition=eleGroupBean.attributeValue("condition");
        String realvalue=eleGroupBean.attributeValue("realvalue");
        String tablename=eleGroupBean.attributeValue("tablename");
        String labelstyleproperty=eleGroupBean.attributeValue("labelstyleproperty");
        labelstyleproperty=labelstyleproperty==null?"":labelstyleproperty.trim();
        labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"align","center");
        labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"valign","middle");
        crossGroupBean.setLabelstyleproperty(labelstyleproperty);
        String rowspan=Tools.getPropertyValueByName("rowspan",labelstyleproperty,true);
        if(rowspan!=null&&!rowspan.trim().equals(""))
        {
            try
            {
                crossGroupBean.setRowspan(Integer.parseInt(rowspan));
            }catch(NumberFormatException e)
            {
                log.warn("报表"+disbean.getReportBean().getPath()+"配置的<group/>的labelstyleproperty中的rowspan不是合法数字",e);
                crossGroupBean.setRowspan(1);
            }
        }
        if(column==null||column.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，没有为此报表的交叉<group/>配置column属性");
        }
        if(column.equals(CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT))
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，为交叉<group/>配置column属性不能为"
                    +CrossStatisticListStatisBean.STATICS_FOR_WHOLEREPORT);
        }
        crossGroupBean.setColumn(column.trim());
        if(condition==null||condition.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
                    +"失败，配置在交叉<group/>下的所有<col/>或<group/>也必须是交叉，因此必须配置condition属性");
        }
        crossGroupBean.setCondition(condition.trim());
        crossGroupBean.setRealvalue(realvalue==null?null:realvalue.trim());
        crossGroupBean.setTablename(tablename==null?null:tablename.trim());

        List<XmlElementBean> lstEleChildren=eleGroupBean.getLstChildElements();
        if(lstEleChildren==null||lstEleChildren.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，交叉<group/>下必须配置子<group/>或子<col/>");
        }
        XmlElementBean eleChildBean=null;
        for(XmlElementBean eleTmp:lstEleChildren)
        {
            if(eleTmp.getName().equalsIgnoreCase("col")||eleTmp.getName().equalsIgnoreCase("group"))
            {//配置的交叉<group/>下只能配置<group/>或<col/>，且只能配置一个直接这种子标签。
                eleChildBean=eleTmp;
                break;
            }
        }
        if(eleChildBean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，交叉<group/>下只能配置子<group/>或子<col/>");
        }
        if(eleChildBean.getName().equalsIgnoreCase("col"))
        {
            ColBean colbean=ComponentConfigLoadManager.loadColConfig(eleChildBean,disbean);
            CrossStatisticListColBean cslrcbean=(CrossStatisticListColBean)colbean.getExtendConfigDataForReportType(KEY);
            if(cslrcbean==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
                        +"失败，配置在交叉<group/>下的所有<col/>或<group/>也必须是交叉，即必须配置condition属性");
            }
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(colbean.getDisplaytype()))
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，交叉统计报表的动态统计列对应的<col/>的displaytype不能配置为never");
            }
            colbean.setDisplaytype(Consts.COL_DISPLAYTYPE_ALWAYS);
            crossGroupBean.setChildObj(colbean);
        }else
        {//是子<group/>
            CrossStatisticListGroupBean crossGroupBeanChild=new CrossStatisticListGroupBean(disbean);
            crossGroupBean.setChildObj(crossGroupBeanChild);
            loadCrossGroupConfig(crossGroupBeanChild,eleChildBean,disbean);
        }
    }

    private void doAfterSuperDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        //return super.afterDisplayLoading(disbean,eleDisplay);
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        Map<String,String> mJoinedAttributes=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "mouseoverbgcolor" });
        String mouseoverbgcolor=mJoinedAttributes.get("mouseoverbgcolor");
        if(mouseoverbgcolor==null)
        {
            if(alrdbean.getMouseoverbgcolor()==null)
            {
                alrdbean.setMouseoverbgcolor(Config.getInstance().getSystemConfigValue("default-mouseoverbgcolor",""));
            }
        }else
        {
            alrdbean.setMouseoverbgcolor(mouseoverbgcolor.trim());
        }
        //        boolean isAllAlwayOrNeverCol=true;//是否全部是hidden为1或3的<col/>
        List<ColBean> lstColBeans=disbean.getLstCols();
        boolean bolContainsClickOrderby=false;
        if(lstColBeans!=null&&lstColBeans.size()>0)
        {
            ColBean cbean;
            AbsListReportColBean beanTemp;
            for(int i=0;i<lstColBeans.size();i++)
            {
                cbean=(ColBean)lstColBeans.get(i);
                if(cbean==null) continue;
                //                if(cbean.getHidden()==0||cbean.getHidden()==2) isAllAlwayOrNeverCol=false;//先判断普通列，即非交叉统计列
                beanTemp=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(beanTemp==null) continue;
                if(beanTemp.isRequireClickOrderby())
                {
                    bolContainsClickOrderby=true;
                    break;
                }
            }
        }
        alrdbean.setContainsClickOrderBy(bolContainsClickOrderby);
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        CrossStatisticListReportBean cslrbean=(CrossStatisticListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(cslrbean==null)
        {
            cslrbean=new CrossStatisticListReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(KEY,cslrbean);
        }
        reportbean.setCelldrag(0);
        XmlElementBean eleHeaderFormatBean=eleReportBean.getChildElementByName("dataheader-format");
        if(eleHeaderFormatBean!=null)
        {
            List<XmlElementBean> lstEleFormatBeans=new ArrayList<XmlElementBean>();
            lstEleFormatBeans.add(eleHeaderFormatBean);
            lstEleFormatBeans.addAll(ConfigLoadAssistant.getInstance().getRefElements(eleHeaderFormatBean.attributeValue("ref"),"dataheader-format",
                    null,reportbean));//取到所有被此<format ref=""/>引用的<format/>配置
            XmlElementBean eleFormatValueBean=null;
            for(XmlElementBean eleFormatBeanTmp:lstEleFormatBeans)
            {
                eleFormatValueBean=eleFormatBeanTmp.getChildElementByName("value");
                if(eleFormatValueBean!=null) break;//取到一个就跳出，优先级低的不再取
            }
            if(eleFormatValueBean!=null)
            {
                String formatmethod=eleFormatValueBean.getContent();
                if(formatmethod==null||formatmethod.trim().equals(""))
                {//如果将<value/>配置为空
                    cslrbean.setDataheaderformatContent(null);
                }else
                {
                    cslrbean.setDataheaderformatContent(formatmethod);
                    List<String> lstImports=ComponentConfigLoadManager.getListImportPackages(lstEleFormatBeans);
                    cslrbean.setLstDataHeaderFormatImports(lstImports);
                }
            }
        }
        return super.afterReportLoading(reportbean,lstEleReportBeans);
    }

    public int doPostLoad(ReportBean reportbean)
    {
        CrossStatisticListDisplayBean cslrdbean=(CrossStatisticListDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        List lstChildren=cslrdbean.getLstChildren();
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof UltraListReportGroupBean)
            {
                ((UltraListReportGroupBean)objTmp).doPostLoad();
            }
        }
        SqlBean sqlbean=reportbean.getSbean();
        CrossStatisticListSqlBean cslrsbean=(CrossStatisticListSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(cslrsbean==null)
        {
            cslrsbean=new CrossStatisticListSqlBean(sqlbean);
            sqlbean.setExtendConfigDataForReportType(KEY,cslrsbean);
        }
        String tablename=null;
        List<ConditionBean> lstTablenameConditions=null;
        ICrossStatisticColAndGroup childDynObj=null;
        for(Object childObj:lstChildren)
        {
            if(childObj instanceof ColBean)
            {
                CrossStatisticListColBean cslrcbeanTmp=(CrossStatisticListColBean)((ColBean)childObj).getExtendConfigDataForReportType(KEY);
                if(cslrcbeanTmp==null) continue;
                if(childDynObj!=null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，一个交叉统计数据展示只能配置一个动态列或动态分组");
                }
                cslrsbean.initDynCols(cslrcbeanTmp);
                tablename=cslrcbeanTmp.getTablename();
                if(tablename==null||tablename.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，没有为顶层动态统计列配置tablname属性，无法获取其动态列数据");
                }
                lstTablenameConditions=cslrcbeanTmp.getLstTablenameConditions();
                childDynObj=cslrcbeanTmp;
            }else if(childObj instanceof CrossStatisticListGroupBean)
            {//配置的交叉列是一个<group/>
                if(childDynObj!=null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，一个交叉统计数据展示只能配置一个动态列或动态分组");
                }
                cslrsbean.initDynCols(childObj);
                tablename=((CrossStatisticListGroupBean)childObj).getTablename();
                if(tablename==null||tablename.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，没有为顶层动态统计列配置tablname属性，无法获取其动态列数据");
                }
                lstTablenameConditions=((CrossStatisticListGroupBean)childObj).getLstTablenameConditions();
                childDynObj=(CrossStatisticListGroupBean)childObj;
                childDynObj.setParentCrossStatiGroupBean(null);
            }
        }
        cslrdbean.initStatisDisplayBean(childDynObj);
        if(lstTablenameConditions!=null&&lstTablenameConditions.size()>0)
        {
            for(ConditionBean cbTmp:lstTablenameConditions)
            {
                if(Tools.isDefineKey("ref",cbTmp.getName()))
                {//此条件引用了其它<sql/>中配置的条件
                    ConditionBean cbRefered=reportbean.getSbean().getConditionBeanByName(Tools.getRealKeyByDefine("ref",cbTmp.getName()));
                    if(cbRefered==null)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，其<tableconditions/>中配置的name为"
                                +cbTmp.getName()+"的查询条件引用的查询条件在<sql/>中不存在");
                    }
                    cbTmp.setName(cbRefered.getName());
                    cbTmp.setConstant(cbRefered.isConstant());
                    cbTmp.setDefaultvalue(cbRefered.getDefaultvalue());
                    cbTmp.setKeepkeywords(cbRefered.isKeepkeywords());
                    cbTmp.setSource(cbRefered.getSource());
                    if(cbTmp.getConditionExpression()==null||cbTmp.getConditionExpression().getValue()==null
                            ||cbTmp.getConditionExpression().getValue().trim().equals(""))
                    {//如果没有配置自己的条件表达式，则用源条件的条件表达式
                        cbTmp.setConditionExpression(cbRefered.getConditionExpression());
                        
                    }else if(reportbean.getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
                    {
                        cbTmp.getConditionExpression().parseConditionExpression();
                    }
                    if(cbTmp.getConditionExpression()==null||cbTmp.getConditionExpression().getValue()==null
                            ||cbTmp.getConditionExpression().getValue().trim().equals(""))
                    {
                        throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"在<tableconditions/>中配置的name为"+cbTmp.getName()
                                +"的查询条件引用的查询条件没有配置条件表达式");
                    }
                }else if(reportbean.getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
                {
                    cbTmp.getConditionExpression().parseConditionExpression();
                }
            }
        }
        cslrsbean.initFetchDynColSql(tablename);
        reportbean.getDbean().setColselect(false);
        String sql=sqlbean.getValue().trim();
        int idxselect=sql.toLowerCase().indexOf("select");
        int idxfrom=sql.toLowerCase().indexOf("from");
        if(idxselect!=0||idxfrom<=0||sql.toLowerCase().indexOf(" group ")<0)
        {
            throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，配置的sql语句不合法，必须配置group by");
        }
        StringBuffer sqlBuf=new StringBuffer("select ");
        sqlBuf.append(sql.substring(idxselect+"select".length(),idxfrom)).append(",%dyncols% ").append(sql.substring(idxfrom));
        sqlbean.setValue(sqlBuf.toString());
        CrossStatisticListColBean cslrcbean=childDynObj.getStatisColBean();
        if(cslrcbean.isVerticalstatistic())
        {
            sql=sql.substring(idxfrom,sql.toLowerCase().lastIndexOf(" group "));
            
            //            sqlBuf.append("select %dyncols% ").append(sql);
            //            sqlBuf.append(") tableVerticalStaticTmp ").append(Consts_Private.PLACEHODER_FILTERCONDITION);//加上列过滤占位符
            cslrsbean.setSql_getVerticalStatisData("select %dyncols% "+sql);
            
        }
        super.doPostLoad(reportbean);

        AbsListReportSqlBean alrsqlbean=(AbsListReportSqlBean)sqlbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        alrsqlbean.setFilterdata_sql(replaceDynColPlaceHolder(alrsqlbean.getFilterdata_sql(),""));

        Map<String,ColAndGroupTitlePositionBean> mPositions=calPosition(reportbean,lstChildren);
        cslrdbean.setMChildrenDefaultPositions(mPositions);
        if(cslrcbean.isVerticalstatistic())
        {
            cslrdbean.setLstDefaultVerticalStatisColBeans(createColBeansForVerticalStatisticRow(reportbean.getDbean(),lstChildren,cslrcbean,
                    mPositions));
        }
        rebuildReportPojoClass(reportbean);
        CrossStatisticListReportBean cslrbean=(CrossStatisticListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(cslrbean.getDataheaderformatContent()==null||cslrbean.getDataheaderformatContent().trim().equals(""))
        {//如果没有格式化表头的配置，即没有配置<dataheader-format/>
            cslrbean.setDataHeaderPojoClass(null);
        }else
        {
            createDataHeaderPojoClass(reportbean,cslrbean.getDataheaderformatContent(),cslrbean.getLstDataHeaderFormatImports());
        }
        return 1;
    }

    protected void processFixedColsAndRows(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrbean.getFixedcols()<=0&&alrbean.getFixedrows()<=0) return;
        if(alrbean.getFixedcols()>0)
        {
            boolean isChkRadioRowselectReport=Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType())
                    ||Consts.ROWSELECT_RADIOBOX.equals(alrbean.getRowSelectType());
            CrossStatisticListDisplayBean cslrdbean=(CrossStatisticListDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
            List lstChildren=cslrdbean.getLstChildren();
            ColBean cbTmp;
            AbsListReportColBean alrcbeanTmp;
            int childColCntBeforeDynCol=0;
            for(Object objTmp:lstChildren)
            {
                if(objTmp instanceof ColBean)
                {
                    cbTmp=(ColBean)objTmp;
                    CrossStatisticListColBean cslrcbeanTmp=(CrossStatisticListColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                    if(cslrcbeanTmp!=null)
                    {
                        log.warn("不能指定交叉统计报表"+reportbean.getPath()+"的交叉统计列为冻结列");
                        alrbean.setFixedcols(childColCntBeforeDynCol);
                        break;
                    }else
                    {//普通列
                        if(cbTmp.isRowSelectCol())
                        {
                            if(!isChkRadioRowselectReport) continue;
                            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,在<report/>的fixedcols中配置的冻结列数包括了行选中列，这样不能正常选中行");
                        }
                        alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                        alrcbeanTmp.setFixedCol(true);
                        if(++childColCntBeforeDynCol==alrbean.getFixedcols()) break;
                    }
                }else if(objTmp instanceof CrossStatisticListGroupBean)
                {
                    log.warn("不能指定交叉统计报表"+reportbean.getPath()+"的交叉统计列为冻结列");
                    alrbean.setFixedcols(childColCntBeforeDynCol);
                    break;
                }else if(objTmp instanceof UltraListReportGroupBean)
                {
                    List<ColBean> lstTemp=new ArrayList<ColBean>();
                    ((UltraListReportGroupBean)objTmp).getAllColBeans(lstTemp,null);
                    for(ColBean cb:lstTemp)
                    {
                        if(cb.isRowSelectCol())
                        {
                            if(!isChkRadioRowselectReport) continue;
                            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,在<report/>的fixedcols中配置的冻结列数包括了行选中列，这样不能正常选中行");
                        }
                        alrcbeanTmp=(AbsListReportColBean)cb.getExtendConfigDataForReportType(AbsListReportType.KEY);
                        alrcbeanTmp.setFixedCol(true);
                        if(++childColCntBeforeDynCol==alrbean.getFixedcols()) break;
                    }
                }
            }
        }
    }

    protected ColBean[] processRowSelectCol(DisplayBean disbean)
    {
        ColBean[] cbResults=super.processRowSelectCol(disbean);
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        CrossStatisticListDisplayBean cslrdbean=(CrossStatisticListDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(alrbean.getRowSelectType()==null
                ||(!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_CHECKBOX)&&!alrbean.getRowSelectType().trim().equals(
                        Consts.ROWSELECT_RADIOBOX)))
        {//当前报表要么没有提供行选中功能，要么提供的不是复选框/单选框的行选择功能
            cslrdbean.removeChildColBeanByColumn(Consts_Private.COL_ROWSELECT,true);//将分组中配置的或继承过来的行选中列删除
        }else
        {
            if(cbResults!=null&&cslrdbean.getLstChildren()!=null&&cslrdbean.getLstChildren().size()>0)
            {//当前报表的行选择类型为Consts.ROWSELECT_CHECKBOX或Consts.ROWSELECT_RADIOBOX，且没有配置行选择列，而是使用自动生成的列对象，并且此报表配置了<group/>，而不是单行标题的报表
                
                
                insertNewRowSelectCol(cslrdbean.getLstChildren(),cbResults[0],cbResults[1]);
            }
        }
        return cbResults;
    }

    protected List<ColBean> processRoworderCol(DisplayBean disbean)
    {
        List<ColBean> lstCreatedColBeans=super.processRoworderCol(disbean);
        CrossStatisticListDisplayBean cslrdbean=(CrossStatisticListDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        for(String rowordertypeTmp:Consts.lstAllRoworderTypes)
        {
            if(rowordertypeTmp.equals(Consts.ROWORDER_DRAG)) continue;
            if(alrbean.getLstRoworderTypes()==null||!alrbean.getLstRoworderTypes().contains(rowordertypeTmp))
            {//当前报表没有提供此列的行选中功能，则删除掉用户配置的或从父报表继承过来的这种行排序列
                cslrdbean.removeChildColBeanByColumn(getRoworderColColumnByRoworderType(rowordertypeTmp),true);
            }
        }
        if(lstCreatedColBeans!=null)
        {
            for(ColBean cbCreatedTmp:lstCreatedColBeans)
            {
                cslrdbean.getLstChildren().add(cbCreatedTmp);
            }
        }
        return lstCreatedColBeans;
    }

    private Map<String,ColAndGroupTitlePositionBean> calPosition(ReportBean reportbean,List lstChildren)
    {
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions=new HashMap<String,ColAndGroupTitlePositionBean>();
        int maxrowspan=calPositionStart(lstChildren,mColAndGroupTitlePostions);
        if(maxrowspan==0)
        {
            throw new WabacusConfigLoadingException("没有为报表"+reportbean.getPath()+"指定要显示的列");
        }
        calPositionEnd(lstChildren,mColAndGroupTitlePostions,maxrowspan);
        
        ColAndGroupTitlePositionBean positionTmp=new ColAndGroupTitlePositionBean();
        positionTmp.setRowspan(maxrowspan);
        mColAndGroupTitlePostions.put(MAX_TITLE_ROWSPANS,positionTmp);
        return mColAndGroupTitlePostions;
    }

    private int calPositionStart(List lstChildren,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        ColBean cbTmp;
        CrossStatisticListGroupBean cslGroupBeanTmp;
        UltraListReportGroupBean groupBeanTmp;
        int maxrowspan=0;
        CrossStatisticListColBean cslrcbeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                cslrcbeanTmp=(CrossStatisticListColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(cslrcbeanTmp==null)
                {
                    positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                    if(positionBeanTmp==null)
                    {
                        positionBeanTmp=new ColAndGroupTitlePositionBean();
                        mColAndGroupTitlePostions.put(cbTmp.getColid(),positionBeanTmp);
                    }
                    positionBeanTmp.setDisplaymode(cbTmp.getDisplaymode(rrequest,null));
                    if(maxrowspan==0&&positionBeanTmp.getDisplaymode()>0)
                    {//如果当前列是第一个要显示的列，则将maxrowspan置为1
                        maxrowspan=1;
                    }
                }else
                {
                    cslrcbeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions);
                    if(mColAndGroupTitlePostions.get(cbTmp.getColumn()).getDisplaymode()>0)
                    {
                        if(cslrcbeanTmp.isShouldShowStaticLabel())
                        {//如果要为动态统计显示一行标题，则此统计<col/>相当于一个<group/>，自己显示一行（可能占据多个rowspan），<statistic/>显示一行
                            if(maxrowspan<cslrcbeanTmp.getRowspan()+1)
                            {
                                maxrowspan=cslrcbeanTmp.getRowspan()+1;
                            }
                        }else if(maxrowspan==0)
                        {
                            maxrowspan=1;
                        }
                    }
                }
            }else if(objTmp instanceof CrossStatisticListGroupBean)
            {
                cslGroupBeanTmp=(CrossStatisticListGroupBean)objTmp;
                int rowspan=cslGroupBeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions);
                if(rowspan>maxrowspan&&mColAndGroupTitlePostions.get(cslGroupBeanTmp.getColumn()).getDisplaymode()>0)
                {
                    maxrowspan=rowspan;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                int[] spans=groupBeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions,null);
                if(spans[1]>maxrowspan&&mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid()).getDisplaymode()>0)
                {
                    maxrowspan=spans[1];
                }
            }
        }
        return maxrowspan;
    }

    private void calPositionEnd(List lstChildren,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int maxrowspan)
    {
        ColBean cbTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        CrossStatisticListColBean cslrcbeanTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                cslrcbeanTmp=(CrossStatisticListColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(cslrcbeanTmp==null)
                {//是普通列
                    positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                    if(positionBeanTmp.getDisplaymode()>0)
                    {
                        positionBeanTmp.setRowspan(maxrowspan);
                    }
                }else
                {
                    cslrcbeanTmp.calPositionEnd(mColAndGroupTitlePostions,maxrowspan);
                }
            }else if(objTmp instanceof CrossStatisticListGroupBean)
            {
                ((CrossStatisticListGroupBean)objTmp).calPositionEnd(mColAndGroupTitlePostions,maxrowspan);
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ((UltraListReportGroupBean)objTmp).calPositionEnd(mColAndGroupTitlePostions,new int[] { maxrowspan, 0 });
            }
        }
    }

    private List<ColBean> createColBeansForVerticalStatisticRow(DisplayBean disbean,List lstChildren,CrossStatisticListColBean cslrcbean,
            Map<String,ColAndGroupTitlePositionBean> mPositions)
    {
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        List<ColBean> lstColBeansForVerticalStatis=new ArrayList<ColBean>();
        int colspan=0;
        boolean showedVerticalTitle=false;
        ColAndGroupTitlePositionBean positionBeanTmp;
        AbsListReportColBean alrcbeanTmp;
        Boolean isPrevFixedCol=null;//前一列是否是冻结列
        for(Object childObj:lstChildren)
        {
            if(childObj instanceof ColBean)
            {
                ColBean cbTmp=(ColBean)childObj;
                CrossStatisticListColBean cslrcbeanTmp=(CrossStatisticListColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(cslrcbeanTmp==null)
                {
                    positionBeanTmp=mPositions.get(cbTmp.getColid());
                    if(positionBeanTmp.getDisplaymode()>0)
                    {
                        alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                        if(isPrevFixedCol==null)
                        {
                            colspan++;
                            isPrevFixedCol=alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol();
                        }else if(isPrevFixedCol)
                        {
                            if(alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol())
                            {//当前列也是冻结列
                                colspan++;
                            }else
                            {
                                lstColBeansForVerticalStatis.add(createVerticalStatiLabelColBean(disbean,cslrcbean,colspan,showedVerticalTitle));
                                showedVerticalTitle=true;
                                colspan=1;
                                isPrevFixedCol=false;
                            }
                        }else
                        {
                            colspan++;
                            //当前列不是冻结列，则后面所有列都视为非冻结列，所以不再为isPrevFixedCol变量赋值
                        }
                    }
                }else
                {
                    if(colspan>0)
                    {
                        lstColBeansForVerticalStatis.add(createVerticalStatiLabelColBean(disbean,cslrcbean,colspan,showedVerticalTitle));
                        showedVerticalTitle=true;
                        colspan=0;
                    }
                    lstColBeansForVerticalStatis.add(null);
                }
            }else if(childObj instanceof CrossStatisticListGroupBean)
            {
                if(colspan>0)
                {
                    lstColBeansForVerticalStatis.add(createVerticalStatiLabelColBean(disbean,cslrcbean,colspan,showedVerticalTitle));
                    showedVerticalTitle=true;
                    colspan=0;
                }
                lstColBeansForVerticalStatis.add(null);
            }else if(childObj instanceof UltraListReportGroupBean)
            {
                List<ColBean> lstColBeansTmp=new ArrayList<ColBean>();
                ((UltraListReportGroupBean)childObj).getAllColBeans(lstColBeansTmp,mPositions);
                if(alrbean.getFixedcols()>0)
                {
                    for(ColBean cbTmp:lstColBeansTmp)
                    {
                        alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                        if(isPrevFixedCol==null)
                        {//当前是显示的第一列
                            colspan++;
                            isPrevFixedCol=alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol();
                        }else if(isPrevFixedCol)
                        {
                            if(alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol())
                            {
                                colspan++;
                            }else
                            {
                                lstColBeansForVerticalStatis.add(createVerticalStatiLabelColBean(disbean,cslrcbean,colspan,showedVerticalTitle));
                                showedVerticalTitle=true;
                                colspan=1;//当前普通列单独起一个ColBean
                                isPrevFixedCol=false;
                            }
                        }else
                        {
                            colspan++;
                            
                        }
                    }
                }else
                {
                    colspan+=lstColBeansTmp.size();//将列数加上当前<group/>下的所有<col/>的数目
                }
            }
        }
        if(colspan>0)
        {
            lstColBeansForVerticalStatis.add(createVerticalStatiLabelColBean(disbean,cslrcbean,colspan,showedVerticalTitle));
        }
        return lstColBeansForVerticalStatis;
    }

    private ColBean createVerticalStatiLabelColBean(DisplayBean disbean,CrossStatisticListColBean mystatiscolbean,int colspan,
            boolean showedVerticalTitle)
    {
        ColBean cbTmp=new ColBean(disbean);
        if(!showedVerticalTitle)
        {//还没有显示垂直统计行的标题
            cbTmp.setLabel(mystatiscolbean.getVerticallabel());
            String valuestyleproperty=mystatiscolbean.getVerticallabelstyleproperty();
            valuestyleproperty=valuestyleproperty==null?"":valuestyleproperty.trim();
            String strcolspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(strcolspan==null||strcolspan.trim().equals(""))
            {
                valuestyleproperty=valuestyleproperty+" colspan=\""+colspan+"\"";
                cbTmp.setValuestyleproperty(valuestyleproperty);
            }else if(Integer.parseInt(strcolspan.trim())!=colspan)
            {
                throw new WabacusConfigLoadingException("加载交叉统计报表"+disbean.getReportBean().getPath()+"失败，配置的"+valuestyleproperty+"中的colspan值不对，应该为"
                        +colspan);
            }
        }else
        {
            cbTmp.setValuestyleproperty(" colspan=\""+colspan+"\"");
            cbTmp.setLabel("");
        }
        cbTmp.setProperty("[VERTICAL-STATIS-CONSTANTCOL]");
        return cbTmp;
    }

    private void rebuildReportPojoClass(ReportBean reportbean)
    {
        try
        {
            List<String> lstImports=null;
            String format=null;
            if(reportbean.getFbean()!=null)
            {
                format=reportbean.getFbean().getFormatContent();
                lstImports=reportbean.getFbean().getLstImports();
            }
            format=format==null?"":format.trim();
            ClassPool pool=ClassPoolAssistant.getInstance().createClassPool();
            CtClass cclass=pool.makeClass(Consts.BASE_PACKAGE_NAME+".Pojo_"+reportbean.getPageBean().getId()+reportbean.getId());
            if(lstImports==null) lstImports=new UniqueArrayList<String>();
            lstImports.add("com.wabacus.system.format");
            lstImports.add("java.util");
            ClassPoolAssistant.getInstance().addImportPackages(pool,lstImports);
            if(!format.equals(""))
            {
                cclass.setInterfaces(new CtClass[] { pool.get(IFormat.class.getName()) });
            }
            if(reportbean.getDbean().getLstCols()!=null)
            {
                for(ColBean cbTmp:reportbean.getDbean().getLstCols())
                {
                    if(cbTmp==null) continue;
                    String property=cbTmp.getProperty();
                    if(property==null||property.trim().equals("")) continue;
                    if(cbTmp.isNonValueCol()|| 
                            cbTmp.isSequenceCol()||
                            cbTmp.isControlCol())
                    {
                        continue;
                    }
                    ClassPoolAssistant.getInstance().addFieldAndGetSetMethod(cclass,property,cbTmp.getDatatypeObj().getCreatedClass(pool));
                }
            }
            addMDataFieldToClass(pool,cclass);

            if(!format.equals(""))
            {
                CtMethod formatMethod=CtNewMethod.make("public void format("+ReportRequest.class.getName()+"  rrequest,"+ReportBean.class.getName()
                        +" rbean){"+format+" \n}",cclass);
                cclass.addMethod(formatMethod);
            }
            reportbean.setPojoclass(ConfigLoadManager.currentDynClassLoader.loadClass(Consts.BASE_PACKAGE_NAME+".Pojo_"
                    +reportbean.getPageBean().getId()+reportbean.getId(),cclass.toBytecode()));
            cclass.detach();
            pool.clearImportedPackages();
            pool=null;
            ReportAssistant.getInstance().setMethodInfoToColBean(reportbean.getDbean(),reportbean.getPojoclass());
        }catch(NotFoundException e)
        {
            throw new WabacusConfigLoadingException("为报表"+reportbean.getPath()+"生成字节码时，执行pool.get()失败",e);
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("生成类"+reportbean.getPath()+"时无法编译",e);
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("生成类"+reportbean.getPath()+"的字节码失败",e);
        }
    }

    private void createDataHeaderPojoClass(ReportBean reportbean,String formatmethod,List<String> lstImports)
    {
        try
        {
            ClassPool pool=ClassPoolAssistant.getInstance().createClassPool();
            CtClass cclass=pool.makeClass(Consts.BASE_PACKAGE_NAME+".DataHeaderPojo_"+reportbean.getPageBean().getId()+reportbean.getId());
            if(lstImports==null) lstImports=new UniqueArrayList<String>();
            lstImports.add("com.wabacus.system.format");
            lstImports.add("java.util");
            ClassPoolAssistant.getInstance().addImportPackages(pool,lstImports);
            if(!formatmethod.equals(""))
            {
                cclass.setInterfaces(new CtClass[] { pool.get(IFormat.class.getName()) });
            }
            addMDataFieldToClass(pool,cclass);
            if(!formatmethod.equals(""))
            {
                CtMethod formatMethod=CtNewMethod.make("public void format("+ReportRequest.class.getName()+"  rrequest,"+ReportBean.class.getName()
                        +" rbean){"+formatmethod+" }",cclass);
                cclass.addMethod(formatMethod);
            }
            CrossStatisticListReportBean cslrbean=(CrossStatisticListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
            cslrbean.setDataHeaderPojoClass(ConfigLoadManager.currentDynClassLoader.loadClass(Consts.BASE_PACKAGE_NAME+".DataHeaderPojo_"
                    +reportbean.getPageBean().getId()+reportbean.getId(),cclass.toBytecode()));
            cclass.detach();
            pool.clearImportedPackages();
            pool=null;
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("为报表"+reportbean.getPath()+"生成数据表头字节码失败",e);
        }
    }

    private void addMDataFieldToClass(ClassPool pool,CtClass cclass) throws CannotCompileException,NotFoundException
    {
        CtField cfield=new CtField(pool.get("java.util.Map"),"mData",cclass);
        cfield.setModifiers(Modifier.PRIVATE);
        cclass.addField(cfield);
        //            CtMethod setMethod=CtNewMethod.setter("setMData",cfield);
        
        
        

        StringBuffer methodBuf=new StringBuffer();
        methodBuf.append("public Object getStatisticData(String colname)");
        methodBuf.append("{if(mData==null) return null;");
        methodBuf.append("return mData.get(colname);}");
        CtMethod getMDataMethod=CtNewMethod.make(methodBuf.toString(),cclass);
        cclass.addMethod(getMDataMethod);

        methodBuf=new StringBuffer();
        methodBuf.append("public void setStatisticData(String colname,Object value)");
        methodBuf.append("{if(mData==null) mData=new HashMap();");
        methodBuf.append("mData.put(colname,value);}");
        CtMethod setMDataMethod=CtNewMethod.make(methodBuf.toString(),cclass);
        cclass.addMethod(setMDataMethod);
    }
}
