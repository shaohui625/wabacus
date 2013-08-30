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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.condition.ConditionInSqlBean;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.Oracle;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.dataset.IReportDataSet;
import com.wabacus.system.dataset.ISqlDataSetBuilder;
import com.wabacus.system.dataset.sqldataset.GetAllDataSetByPreparedSQL;
import com.wabacus.system.dataset.sqldataset.GetAllDataSetBySQL;
import com.wabacus.system.dataset.sqldataset.GetDataSetByStoreProcedure;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class ReportDataSetValueBean extends AbsConfigBean
{
    private static Log log=LogFactory.getLog(ReportDataSetValueBean.class);

    public final static String dependsConditionPlaceHolder="{#dependscondition#}";

    private String id;//<value/>的id，如果只有一个<value/>，则可以不配置id

    private Map<String,DependingColumnBean> mDependParents;

    private List<String[]> lstDependRelateColumns;

    private String dependsConditionExpression;

    private String dependstype="single";

    private String seperator=";";

    private String value;

    private List<String> lstStoreProcedureParams;

    private String sqlWithoutOrderby;

    private String orderby="";

    private String splitpage_sql;

    private String sqlCount;

    private String filterdata_sql;

    private String sql_kernel;

    private List<ConditionInSqlBean> lstConditionInSqlBeans;

    private String datasource;//此报表所使用的数据源，默认为<sql/>中的datasource配置，如果这里也没配置，则取wabacus.cfg.xml中<datasources/>标签中的default属性配置的值

    private IReportDataSet customizeDatasetObj;//开发人员通过<value>class{xxx}</value>指定的数据集对象

    public ReportDataSetValueBean(AbsConfigBean parent)
    {
        super(parent);
        this.datasource=((ReportDataSetBean)parent).getDatasource();
    }

    public String getId()
    {
        return id;
    }

    public String getGuid()
    {
        return this.getReportBean().getId()+"__"+((ReportDataSetBean)this.getParent()).getId()+"__"+this.id;
    }
    
    public void setId(String id)
    {
        this.id=id;
    }

    public void setValue(String _value)
    {
        this.value=_value==null?"":_value.trim();
    }

    public String getValue()
    {
        return this.value;
    }

    public Map<String,DependingColumnBean> getMDependParents()
    {
        return mDependParents;
    }

    public void setFilterdata_sql(String filterdata_sql)
    {
        this.filterdata_sql=filterdata_sql;
    }

    public String getFilterdata_sql()
    {
        return this.filterdata_sql;
    }

    private List<String> lstDependParentColumns;

    private List<String> lstDependMyColumns;

    public List<String> getLstDependParentColumns()
    {
        if(lstDependParentColumns!=null&&lstDependParentColumns.size()>0) return lstDependParentColumns;
        if(lstDependRelateColumns==null||lstDependRelateColumns.size()==0) return null;
        List<String> lstDependParentColumnsTmp=new ArrayList<String>();
        for(String[] arrTmp:this.lstDependRelateColumns)
        {
            lstDependParentColumnsTmp.add(arrTmp[1]);
        }
        this.lstDependParentColumns=lstDependParentColumnsTmp;
        return lstDependParentColumns;
    }

    public IReportDataSet getCustomizeDatasetObj()
    {
        return customizeDatasetObj;
    }

    public void setCustomizeDatasetObj(IReportDataSet customizeDatasetObj)
    {
        this.customizeDatasetObj=customizeDatasetObj;
    }

    public List<String> getLstDependMyColumns()
    {
        if(lstDependMyColumns!=null&&lstDependMyColumns.size()>0) return lstDependMyColumns;
        if(lstDependRelateColumns==null||lstDependRelateColumns.size()==0) return null;
        List<String> lstDependMyColumnsTmp=new ArrayList<String>();
        for(String[] arrTmp:this.lstDependRelateColumns)
        {
            lstDependMyColumnsTmp.add(arrTmp[0]);
        }
        this.lstDependMyColumns=lstDependMyColumnsTmp;
        return lstDependMyColumns;
    }

    public void setDependParents(String depends)
    {
        if(depends==null) return;
        depends=depends.trim();
        if(depends.equals(""))
        {
            this.mDependParents=null;
            return;
        }
        this.mDependParents=new HashMap<String,DependingColumnBean>();
        List<String> lstDepends=Tools.parseStringToList(depends,";",false);
        String columnTmp;
        DependingColumnBean dcbeanTmp;
        for(String strTmp:lstDepends)
        {
            int idx=strTmp.indexOf("=");
            if(idx<=0)
            {
                log.warn("报表"+this.getReportBean().getPath()+"的<value/>的depends属性配置的依赖父数据集"+strTmp+"格式无效");
                continue;
            }
            dcbeanTmp=new DependingColumnBean();
            columnTmp=strTmp.substring(0,idx).trim();
            strTmp=strTmp.substring(idx+1).trim();
            idx=columnTmp.indexOf("(");
            if(columnTmp.endsWith(")")&&idx>0)
            {//配置了格式化方法
                dcbeanTmp.setFormatMethodName(columnTmp.substring(0,idx).trim());
                columnTmp=columnTmp.substring(idx+1,columnTmp.length()-1).trim();
            }
            dcbeanTmp.setColumn(columnTmp);
            if(strTmp.length()>1&&strTmp.startsWith("'")&&strTmp.endsWith("'"))
            {
                dcbeanTmp.setVarcharType(true);
                strTmp=strTmp.substring(1,strTmp.length()-1).trim();
            }
            idx=strTmp.indexOf(".");
            if(idx<=0)
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的<value/>的depends属性配置的依赖父数据集"+strTmp+"不合法");
            }
            dcbeanTmp.setParentValueid(strTmp.substring(0,idx).trim());
            if(dcbeanTmp.getParentValueid().equals(this.id))
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"中id为"+this.id+"的<value/>自己依赖自己");
            }
            dcbeanTmp.setParentColumn(strTmp.substring(idx+1).trim());
            this.mDependParents.put(columnTmp,dcbeanTmp);
        }
        if(this.mDependParents.size()==0) this.mDependParents=null;
    }

    public String getDependsConditionExpression()
    {
        return dependsConditionExpression;
    }

    public void setDependsConditionExpression(String dependsConditionExpression)
    {
        if(this.mDependParents==null||this.mDependParents.size()==0)
        {
            this.dependsConditionExpression=null;
        }else if(dependsConditionExpression!=null&&!dependsConditionExpression.trim().equals(""))
        {
            this.dependsConditionExpression=dependsConditionExpression.trim();
        }else
        {
            DependingColumnBean dcbeanTmp;
            StringBuffer expressionBuf=new StringBuffer();
            if(this.isStoreProcedure())
            {
                for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
                {
                    dcbeanTmp=entryTmp.getValue();
                    expressionBuf.append(dcbeanTmp.getColumn()).append("=");
                    expressionBuf.append("#").append(dcbeanTmp.getParentValueid()).append(".").append(dcbeanTmp.getParentColumn()).append("#;");
                }
                if(expressionBuf.charAt(expressionBuf.length()-1)==';') expressionBuf.deleteCharAt(expressionBuf.length()-1);
                this.dependsConditionExpression=expressionBuf.toString().trim();
            }else
            {
                for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
                {
                    dcbeanTmp=entryTmp.getValue();
                    expressionBuf.append(dcbeanTmp.getColumn()).append(" in (");
                    expressionBuf.append("#").append(dcbeanTmp.getParentValueid()).append(".").append(dcbeanTmp.getParentColumn()).append("#) and ");
                }
                this.dependsConditionExpression=expressionBuf.toString().trim();
                if(this.dependsConditionExpression.endsWith(" and"))
                {
                    this.dependsConditionExpression=this.dependsConditionExpression.substring(0,this.dependsConditionExpression.length()-4);
                }
            }
        }
    }

    public String getDependstype()
    {
        return dependstype;
    }

    public void setDependstype(String dependstype)
    {
        if(dependstype==null) return;
        dependstype=dependstype.toLowerCase().trim();
        if(dependstype.equals(""))
        {
            dependstype="single";
        }else if(!dependstype.equals("single")&&!dependstype.equals("multiple"))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的数据集"+this.id+"失败，不能将数据集依赖关系配置为"+dependstype);
        }
        this.dependstype=dependstype;
    }

    public String getSeperator()
    {
        return seperator;
    }

    public void setSeperator(String seperator)
    {
        this.seperator=seperator;
    }

    public boolean isStoreProcedure()
    {
        if(this.value==null||this.value.trim().equals("")||this.customizeDatasetObj!=null) return false;
        if(this.value.toLowerCase().indexOf("call ")==0||this.value.toLowerCase().indexOf("{call ")==0) return true;
        return false;
    }

    public boolean isSql()
    {
        if(this.isStoreProcedure()||this.customizeDatasetObj!=null) return false;
        if(this.value==null||this.value.trim().equals("")) return false;
        return true;
    }
    
    public List<ConditionInSqlBean> getLstConditionInSqlBeans()
    {
        return lstConditionInSqlBeans;
    }

    public void setLstConditionInSqlBeans(List<ConditionInSqlBean> lstConditionInSqlBeans)
    {
        this.lstConditionInSqlBeans=lstConditionInSqlBeans;
    }

    public String getSqlCount()
    {
        return sqlCount;
    }

    public void setSqlCount(String sqlCount)
    {
        this.sqlCount=sqlCount;
    }

    public String getSqlWithoutOrderby()
    {
        return sqlWithoutOrderby;
    }

    public void setSqlWithoutOrderby(String sqlWithoutOrderby)
    {
        this.sqlWithoutOrderby=sqlWithoutOrderby;
    }

    public String getOrderby()
    {
        return orderby;
    }

    public void setOrderby(String orderby)
    {
        this.orderby=orderby;
    }

    public String getSplitpage_sql()
    {
        return splitpage_sql;
    }

    public void setSplitpage_sql(String splitpage_sql)
    {
        this.splitpage_sql=splitpage_sql;
    }

    public String getSql_kernel()
    {
        return sql_kernel;
    }

    public void setSql_kernel(String sql_kernel)
    {
        this.sql_kernel=sql_kernel;
    }

    public List<String> getLstStoreProcedureParams()
    {
        return lstStoreProcedureParams;
    }

    public void setLstStoreProcedureParams(List<String> lstStoreProcedureParams)
    {
        this.lstStoreProcedureParams=lstStoreProcedureParams;
    }

    public String getDatasource()
    {
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        if(datasource!=null&&!datasource.trim().equals(""))
        {
            this.datasource=datasource;
        }else
        {
            this.datasource=((ReportDataSetBean)this.getParent()).getDatasource();
        }
    }

    public boolean isMatchDatasetid(String datasetid)
    {
        if(datasetid==null||datasetid.trim().equals(""))
        {
            return this.id==null||this.id.trim().equals("")||this.id.equals(Consts.DEFAULT_KEY);
        }else
        {
            return datasetid.equals(id);
        }
    }

    public boolean isDependentDataSet()
    {
        return this.mDependParents!=null&&this.mDependParents.size()>0;
    }

    public List<String> getAllParentValueIds()
    {
        if(this.mDependParents==null||this.mDependParents.size()==0) return null;
        List<String> lstResults=new ArrayList<String>();
        DependingColumnBean dcbeanTmp;
        for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
        {
            dcbeanTmp=entryTmp.getValue();
            if(dcbeanTmp.getParentValueid()!=null&&!dcbeanTmp.getParentValueid().trim().equals(""))
            {
                lstResults.add(dcbeanTmp.getParentValueid());
            }
        }
        return lstResults;
    }

    public String format(String columnname,String value)
    {
        if(this.mDependParents==null||this.mDependParents.size()==0) return value;
        DependingColumnBean dcbean=this.mDependParents.get(columnname);
        if(dcbean==null) return value;
        return dcbean.format(value);
    }

    public IReportDataSet createDataSetObj(ReportRequest rrequest,AbsReportType reportTypeObj)
    {
        
        ReportBean rbean=reportTypeObj.getReportBean();
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        IReportDataSet datSetObj=null;
        if(this.customizeDatasetObj!=null)
        {
            return this.customizeDatasetObj;
        }else
        {
            //$ByQXO
            final ISqlDataSetBuilder isqlTypeBuilder=this.getDbType().getISQLTypeBuilder(this,rbean.getSbean().getStatementTypeName());
            if(this.isDependentDataSet()||cdb.isLoadAllReportData())
            {
                datSetObj  = isqlTypeBuilder.createAllResultSetISQLType();
            }else
            {
                datSetObj  = isqlTypeBuilder.createPartResultSetISQLType();
            }
            //ByQXO$
        }
        return datSetObj;
    }

    public IReportDataSet createLoadAllDataSetObj(ReportRequest rrequest,AbsReportType reportTypeObj)
    {
        
        IReportDataSet datSetObj=null;
        if(this.customizeDatasetObj!=null)
        {
            return this.customizeDatasetObj;
        }else
        {
            
            datSetObj =  this.getISQLTypeBuilder(this.getReportBean().getSbean()).createAllResultSetISQLType();
            /*
            if(this.isStoreProcedure())
            {
                datSetObj=new GetDataSetByStoreProcedure();
                ((GetDataSetByStoreProcedure)datSetObj).setLoadAllData(true);
            }else if(this.getReportBean().getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
            {
                datSetObj=new GetAllDataSetByPreparedSQL();
            }else
            {
                datSetObj=new GetAllDataSetBySQL();
            }
            */
        }
        return datSetObj;
    }

    public String getRealDependsConditionExpression(List lstReportData)
    {
        if(!this.isDependentDataSet()) return "";
        if(lstReportData==null||lstReportData.size()==0||this.dependsConditionExpression==null||this.dependsConditionExpression.trim().equals(""))
            return "";
        String realConExpress=this.dependsConditionExpression;
        DependingColumnBean dcbeanTmp;
        StringBuffer parentValuesBuf=new StringBuffer();
        for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
        {
            dcbeanTmp=entryTmp.getValue();
            for(Object dataObjTmp:lstReportData)
            {
                Object parentColVal=ReportAssistant.getInstance().getPropertyValue(dataObjTmp,dcbeanTmp.getParentColumn());
                if(parentColVal==null) parentColVal="";
                if(dcbeanTmp.isVarcharType()) parentValuesBuf.append("'");
                parentValuesBuf.append(String.valueOf(parentColVal));
                if(dcbeanTmp.isVarcharType()) parentValuesBuf.append("'");
                parentValuesBuf.append(",");
            }
            if(parentValuesBuf.length()>0&&parentValuesBuf.charAt(parentValuesBuf.length()-1)==',')
                parentValuesBuf.deleteCharAt(parentValuesBuf.length()-1);
            if(parentValuesBuf.length()==0)
            {
                if(dcbeanTmp.isVarcharType()) parentValuesBuf.append("''");
            }
            realConExpress=Tools.replaceAll(realConExpress,"#"+dcbeanTmp.getParentValueid()+"."+dcbeanTmp.getParentColumn()+"#",parentValuesBuf
                    .toString());
        }
        return realConExpress;
    }

    public void afterSqlLoad()
    {
        if(this.isStoreProcedure())
        {
            parseSPAndParams(this.getReportBean().getSbean());
        }else if(this.customizeDatasetObj==null)
        {
            if(this.mDependParents!=null&&this.mDependParents.size()>0)
            {
                if(this.value.indexOf(dependsConditionPlaceHolder)<0)
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的id为"+this.id
                            +"的<value/>失败，此数据集依赖其它数据集，但没有在SQL语句中指定{#dependscondition#}占位符");
                }
            }
        }

    }

    private void parseSPAndParams(SqlBean sbean)
    {
        String proc=this.getValue().trim();
        int idxLeft=proc.indexOf("(");
        int idxRight=proc.lastIndexOf(")");
        if(idxLeft>0&&idxRight==proc.length()-1)
        {
            StringBuffer spBuf=new StringBuffer(proc.substring(0,idxLeft+1));//取到call spname(部分
            String procParams=proc.substring(idxLeft+1,idxRight);
            if(!procParams.trim().equals(""))
            {
                List<String> lstParams=Tools.parseStringToList(procParams,',','\'');
                List<String> lstSPParams=new ArrayList<String>();
                for(String paramTmp:lstParams)
                {
                    if(paramTmp.trim().equals("")||WabacusAssistant.getInstance().isGetRequestContextValue(paramTmp))
                    {
                        lstSPParams.add(paramTmp);
                    }else if(paramTmp.startsWith("'")&&paramTmp.endsWith("'"))
                    {
                        lstSPParams.add(paramTmp.substring(1,paramTmp.length()-1));
                    }else
                    {
                        if(sbean.getConditionBeanByName(paramTmp)==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"失败，存储过程引用的name为"+paramTmp+"的动态条件不存在");
                        }
                        lstSPParams.add("condition{"+paramTmp+"}");
                    }
                    spBuf.append("?,");
                }
                this.setLstStoreProcedureParams(lstSPParams);
            }
            proc=spBuf.toString();
        }else
        {
            proc=proc+"(";
        }
        
        proc=proc+"?";
        if(Config.getInstance().getDataSource(datasource).getDbType() instanceof Oracle)
        {
            proc=proc+",?";
        }
        proc="{"+proc+")}";
        this.setValue(proc);
    }

    public void doPostLoad()
    {
        if(this.isStoreProcedure()||this.customizeDatasetObj!=null) return;
        
        //$ByQXO
        this.getDbType().parseConditionInSql(this,this.value);
        //ByQXO$
        
        validateConditionsConfig();
        if(this.mDependParents!=null&&this.mDependParents.size()>0)
        {
            this.lstDependRelateColumns=new ArrayList<String[]>();
            for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
            {
                this.lstDependRelateColumns.add(new String[] { entryTmp.getValue().getColumn(), entryTmp.getValue().getParentColumn() });
            }
        }
    }
    
    //$ByQXO
    //private void parseConditionInSql()
    public ISqlDataSetBuilder getISQLTypeBuilder(SqlBean sbean){
        final AbsDatabaseType dbType = this.getDbType();
        return dbType.getISQLTypeBuilder(this,sbean.getStatementTypeName());
     }
     public AbsDatabaseType getDbType()
     {
         final AbsDatabaseType dbtype=Config.getInstance().getDbType(this.getDatasource());
         return dbtype;
     }     
     
     public AbsDataSource getDatasourceObj()
     {
         final AbsDataSource dbtype=Config.getInstance().getDataSource(this.getDatasource());
         return dbtype;
     }   
     //ByQXO$

    public void validateCondition(String sql,int idxBracketStart,int idxBracketEnd,int idxJingStart,int idxJingEnd)
    {
        if(idxBracketStart>=0&&idxBracketEnd<0||idxBracketStart<0&&idxBracketEnd>=0||idxBracketStart>=idxBracketEnd&&idxBracketEnd>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{和}没有成对");
        }
        if(idxJingStart>=0&&idxJingEnd<0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，#号没有成对");
        }
        int nextValidBracketStartIdx=getValidIndex(sql,'{',idxBracketStart+1);
        if(nextValidBracketStartIdx>0&&idxBracketEnd>nextValidBracketStartIdx)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{和}没有成对");
        }
        if(idxBracketStart>=0&&sql.substring(idxBracketStart+1,idxBracketEnd).trim().equals(""))
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败,，{和}之间不是有效的条件表达式");
        }
        if(idxJingStart>=0&&sql.substring(idxJingStart+1,idxJingEnd).trim().equals(""))
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，#和#之间不是有效的<condition/>的name属性值");
        }
        if(false && idxBracketStart<idxJingStart&&idxBracketEnd<idxJingEnd&&idxBracketStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{、}、#、#之间的关系混乱");
        }
        if(false && idxBracketEnd<idxJingStart&&idxBracketStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{、}之间的条件表达式没有指定动态条件的name属性");
        }
        if(idxBracketStart>idxJingStart&&idxBracketStart<idxJingEnd&&idxJingStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{、}、#、#之间的关系混乱");
        }
    }

    public static int getValidIndex(String sql,char sign,int startindex)
    {
        if(sql==null||sql.equals("")) return -1;
        char c;
        for(int i=startindex,len=sql.length();i<len;i++)
        {
            c=sql.charAt(i);
            if(c==sign)
            {
                if(i==startindex) return i;
                if(sql.charAt(i-1)=='\\')
                {
                    i++;
                }else
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private void validateConditionsConfig()
    {
        List<String> lstConditionNamesInSql=new ArrayList<String>();//在sql语句中通过#name#指定了条件的<condition/>的name集合
        if(this.lstConditionInSqlBeans!=null&&this.lstConditionInSqlBeans.size()>0)
        {//在sql语句中通过#name#指定了某些条件做为条件表达式
            ConditionBean cbReferedTmp;
            String conditionNameTmp;
            for(ConditionInSqlBean csbeanTmp:this.lstConditionInSqlBeans)
            {
                conditionNameTmp=csbeanTmp.getRealConditionname();
                if(conditionNameTmp==null||conditionNameTmp.trim().equals("")) continue;
                if(!lstConditionNamesInSql.contains(conditionNameTmp)) lstConditionNamesInSql.add(conditionNameTmp);
                cbReferedTmp=this.getReportBean().getSbean().getConditionBeanByName(conditionNameTmp);
                if(cbReferedTmp==null)
                {
                    throw new WabacusConfigLoadingException("报表"+this.getReportBean()+"的SQL语句："+this.value+"中引用的name属性为"+conditionNameTmp
                            +"的<condition/>不存在");
                }
                if(cbReferedTmp.getIterator()>1)
                {
                    throw new WabacusConfigLoadingException("报表"+this.getReportBean()+"的SQL语句："+this.value+"中引用的name属性为"+conditionNameTmp
                            +"的<condition/>的iterator值大于1");
                }
            }
        }
        
        
        //            if(lstConditionNamesInSql.contains(cbeanTmp.getName())) continue;//在<sql/>中通过#name#形式指定了条件的<condition/>可以不在<condition/>中配置条件表达式
        //            /**
        
        //             */
        
        //            {//此条件没有配置条件表达式
        //                /**
        //                 * 这里不抛出异常，因为这个条件可能用在了其它地方，比如交叉统计报表的<tablenameconditions/>中定义的查询条件如果需要输入框，则需在<sql/>中定义条件，
        //                 * 然后在<tablenameconditions/>中定义<condition/>中引用它，并在这里指定条件表达式，这个时候就不用在<sql/>的<condition/>中定义条件表达式了
        //                 */
        
        //                //throw new WabacusConfigLoadingException("报表"+this.getReportBean()+"的name属性为"+cbeanTmp.getName()
        //                       // +"的查询条件即没有以#name#的形式出现在sql语句中，也没有在<condition/>中配置条件表达式");
        
        
    }

    public final static String sqlprex="select * from (";

    public final static String sqlsuffix=") wabacus_temp_tbl";

    public void doPostLoadSql(boolean isListReportType)
    {
      //$ByQXO
       this.getDbType().doPostLoadSql(this,isListReportType);
       //ByQXO$
    }

    public void buildPageSplitSql()
    {
        //$ByQXO
        AbsDatabaseType dbtype=getDbType();
       //ByQXO$
        this.setSplitpage_sql(dbtype.constructSplitPageSql(this));
    }

    public ReportDataSetValueBean clone(AbsConfigBean parent)
    {
        ReportDataSetValueBean svbeanNew=(ReportDataSetValueBean)super.clone(parent);
        if(lstConditionInSqlBeans!=null)
        {
            List<ConditionInSqlBean> lstConditionInSqlBeansNew=new ArrayList<ConditionInSqlBean>();
            for(ConditionInSqlBean csbeanTmp:lstConditionInSqlBeans)
            {
                lstConditionInSqlBeansNew.add(csbeanTmp.clone());
            }
            svbeanNew.setLstConditionInSqlBeans(lstConditionInSqlBeansNew);
        }
        if(lstStoreProcedureParams!=null)
        {
            svbeanNew.setLstStoreProcedureParams((List<String>)((ArrayList<String>)lstStoreProcedureParams).clone());
        }
        cloneExtendConfig(svbeanNew);
        return svbeanNew;
    }

    private class DependingColumnBean
    {
        private String column;

        private Class formatClass;

        private Method formatMethod;

        private String parentValueid;//父数据集<value/>的ID

        private String parentColumn;

        private boolean isVarcharType;

        public String getColumn()
        {
            return column;
        }

        public void setColumn(String column)
        {
            this.column=column;
        }

        public String getParentValueid()
        {
            return parentValueid;
        }

        public void setParentValueid(String parentValueid)
        {
            this.parentValueid=parentValueid;
        }

        public String getParentColumn()
        {
            return parentColumn;
        }

        public void setParentColumn(String parentColumn)
        {
            this.parentColumn=parentColumn;
        }

        public boolean isVarcharType()
        {
            return isVarcharType;
        }

        public void setVarcharType(boolean isVarcharType)
        {
            this.isVarcharType=isVarcharType;
        }

        public void setFormatMethodName(String formatmethodname)
        {
            if(formatmethodname==null||formatmethodname.trim().equals(""))
            {
                this.formatClass=null;
                this.formatMethod=null;
            }else
            {
                formatmethodname=formatmethodname.trim();
                this.formatClass=getReportBean().getFormatMethodClass(formatmethodname,new Class[] { String.class });
                try
                {
                    this.formatMethod=this.formatClass.getMethod(formatmethodname,new Class[] { String.class });
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()+"的<value/>子标签时，获取格式化方法"+formatmethodname+"对象失败",e);
                }
            }
        }

        public String format(String value)
        {
            if(formatClass==null||formatMethod==null) return value;
            try
            {
                return (String)formatMethod.invoke(formatClass,new Object[] { value });
            }catch(Exception e)
            {
                log.warn("在查询报表"+getReportBean().getPath()+"的ID为"+id+"的记录集时，格式化字段"+column+"上的数据"+value+"失败",e);
                return value;
            }
        }
    }
}
