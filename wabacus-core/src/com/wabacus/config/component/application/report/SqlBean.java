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
package com.wabacus.config.component.application.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.component.application.report.condition.ConditionInSqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class SqlBean extends AbsConfigBean
{
    private final static Log log=LogFactory.getLog(SqlBean.class);
    
    public final static int STMTYPE_STATEMENT=1;
    
    public final static int STMTYPE_PREPAREDSTATEMENT=2;
    
    private String value;

    private List<String> lstStoreProcedureParams;
    
    private int stmttype=STMTYPE_STATEMENT;//执行本报表sql语句的方式,1:statement方式执行；2：preparedstatement方式执行，默认为1

    private String datasource;//此报表所使用的数据源，默认为wabacus.cfg.xml中<datasources/>标签中的default属性配置的值

//    private Object searchTemplateObj;//搜索栏的显示模板，可能是字符串或TemplateBean
    
    private List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();

    private Map<String,ConditionBean> mConditions;//运行时由lstConditions生成，方便根据<condition/>的name属性取到对应的ConditionBean对象
    
    private List<String> lstConditionFromRequestNames;
    
    private List<ConditionInSqlBean> lstConditionInSqlBeans;

    private String sqlWithoutOrderby;
    
    private String orderby="";//order by部分
    
    private String splitpage_sql;
    
    private String sqlCount;
    
    private String sql_kernel;
    
    private String beforeSearchMethod;
    
    public SqlBean(AbsConfigBean parent)
    {
        super(parent);
    }

    public void setValue(String _value)
    {
        this.value=_value==null?"":_value.trim();
    }

    public String getValue()
    {
        return this.value;
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

    public void setStatementType(String statementtype)
    {
        if(statementtype==null) return;
        statementtype=statementtype.toLowerCase().trim();
        if(statementtype.equals("statement"))
        {
            this.stmttype=STMTYPE_STATEMENT;
        }else if(statementtype.equals("preparedstatement"))
        {
            this.stmttype=STMTYPE_PREPAREDSTATEMENT;
        }
    }

    public boolean isStoreProcedure()
    {
        if(this.value==null||this.value.trim().equals("")) return false;
        if(this.value.toLowerCase().indexOf("call ")==0||this.value.toLowerCase().indexOf("{call ")==0) return true;
        return false;
    }
    
    public boolean isPreparedstatementSql()
    {
        if(this.isStoreProcedure()) return false;
        return this.stmttype==STMTYPE_PREPAREDSTATEMENT;
    }
    
    public boolean isStatementSql()
    {
        if(this.isStoreProcedure()) return false;
        return this.stmttype==STMTYPE_STATEMENT;
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
    
    public void doPostLoad()
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return;
        List<String> lstTmp=new ArrayList<String>();
        for(ConditionBean cbTmp:lstConditions)
        {
            if(cbTmp==null||cbTmp.getName()==null) continue;
            if(lstTmp.contains(cbTmp.getName()))
            {
                throw new WabacusConfigLoadingException("报表 "+this.getPageBean().getId()+"/"+this.getReportBean().getId()+"配置的查询条件name:"
                        +cbTmp.getName()+"存在重复，必须确保唯一");
            }
            lstTmp.add(cbTmp.getName());
            cbTmp.doPostLoad();
        }
        if(this.isStoreProcedure()) return;
        parseConditionInSql();
        validateConditionsConfig();
    }
    
    private void parseConditionInSql()
    {
        if(this.value==null||this.value.trim().equals("")) return;
        List<ConditionInSqlBean> lstConditionsInSqlBeans=new ArrayList<ConditionInSqlBean>();
        ConditionInSqlBean csbeanTmp;
        int placeholderIndex=0;
        String sql=this.value;
        StringBuffer sqlBuf=new StringBuffer();
        int idxBracketStart;//存放sql语句中第一个有效{号的下标
        int idxBracketEnd;
        int idxJingStart;
        int idxJingEnd;
        while(true)
        {
            idxBracketStart=getValidIndex(sql,'{',0);
            idxBracketEnd=getValidIndex(sql,'}',0);
            idxJingStart=getValidIndex(sql,'#',0);
            if(idxJingStart<0)
            {
                idxJingEnd=-1;
            }else
            {
                idxJingEnd=getValidIndex(sql,'#',idxJingStart+1);
            }
            if(idxBracketStart<0&&idxBracketEnd<0&&idxJingStart<0&&idxJingEnd<0) break;//所有动态条件处理完毕



            validateCondition(sql,idxBracketStart,idxBracketEnd,idxJingStart,idxJingEnd);
            if(idxJingEnd>=0&&(idxJingEnd<idxBracketStart||idxBracketStart<0))
            {
                String prex=sql.substring(0,idxJingStart);
                String expression=sql.substring(idxJingStart,idxJingEnd+1);//要包括左右的#号，所以后面用idxJingEnd+1
                String suffix=sql.substring(idxJingEnd+1);
                String conditionname=expression;
                expression="#data#";//这里用#data#占位符即可，方便解析pattern，不用标识<condition/>的name，因为会在对应的ConditionInSqlBean的中标识
                if(prex.endsWith("%"))
                {
                    prex=prex.substring(0,prex.length()-1);
                    expression="%"+expression;
                }
                if(prex.endsWith("'"))
                {
                    prex=prex.substring(0,prex.length()-1);
                    expression="'"+expression;
                }
                if(suffix.startsWith("%"))
                {
                    suffix=suffix.substring(1);
                    expression=expression+"%";
                }
                if(suffix.startsWith("'"))
                {
                    suffix=suffix.substring(1);
                    expression=expression+"'";
                }
                sql=suffix;
                csbeanTmp=new ConditionInSqlBean(this);
                lstConditionsInSqlBeans.add(csbeanTmp);
                csbeanTmp.setConditionname(conditionname);
                csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
                ConditionExpressionBean expressionBean=new ConditionExpressionBean();
                csbeanTmp.setConditionExpression(expressionBean);
                expressionBean.setValue(expression);
                if(this.stmttype==STMTYPE_PREPAREDSTATEMENT) expressionBean.parseConditionExpression();
                sqlBuf.append(prex).append(csbeanTmp.getPlaceholder());
                placeholderIndex++;
            }else if(idxBracketStart<idxJingStart&&idxBracketEnd>idxJingEnd&&idxBracketStart>=0&&idxJingEnd>=0)
            {
                
                sqlBuf.append(sql.substring(0,idxBracketStart));
                String expression=sql.substring(idxBracketStart,idxBracketEnd+1);
                if(expression.equals("{#condition#}"))
                {
                    csbeanTmp=new ConditionInSqlBean(this);
                    csbeanTmp.setConditionname("{#condition#}");
                    lstConditionsInSqlBeans.add(csbeanTmp);
                    sqlBuf.append(" {#condition#} ");
                }else
                {
                    csbeanTmp=new ConditionInSqlBean(this);
                    csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
                    sqlBuf.append(csbeanTmp.getPlaceholder());
                    if(idxBracketStart==0&&idxJingStart==1&&idxBracketEnd==expression.length()-1&&idxJingEnd==expression.length()-2)
                    {//{#name#}形式，则某个查询条件本身就是一个完整的条件表达式
                        csbeanTmp.setConditionname(expression);
                    }else
                    {
                        expression=expression.substring(1,expression.length()-1);
                        String conditionname=sql.substring(idxJingStart+1,idxJingEnd);//放在一个{}中的一定是从同一个<condition/>（即name属性相同）中取值做为条件，因此在这里可以取到此name属性（在##之间的值），
                        if(conditionname.equals("data"))
                        {
                            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean().getPath()+"的查询SQL语句"+this.value
                                    +"失败，不能在其中直接使用占位符#data#，这是一个框架做为保留字的字符串，请使用#conditionname#格式");
                        }
                        expression=Tools.replaceAll(expression,"#"+conditionname+"#","#data#");
                        csbeanTmp.setConditionname(conditionname);
                        ConditionExpressionBean expressionBean=new ConditionExpressionBean();
                        csbeanTmp.setConditionExpression(expressionBean);
                        expressionBean.setValue(expression);
                        if(this.stmttype==STMTYPE_PREPAREDSTATEMENT) expressionBean.parseConditionExpression();
                    }
                    lstConditionsInSqlBeans.add(csbeanTmp);
                    placeholderIndex++;
                }
                sql=sql.substring(idxBracketEnd+1);
            }else
            {
                throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，无法解析其中用{}和##括住的动态条件");
            }
        }
        if(!sql.equals("")) sqlBuf.append(sql);
        if(lstConditionsInSqlBeans==null||lstConditionsInSqlBeans.size()==0
                ||(lstConditionsInSqlBeans.size()==1&&lstConditionsInSqlBeans.get(0).getConditionname().equals("{#condition#}")))
        {
            this.lstConditionInSqlBeans=null;
        }else
        {
            this.value=sqlBuf.toString();
            this.lstConditionInSqlBeans=lstConditionsInSqlBeans;
        }
        this.value=Tools.replaceAllOnetime(this.value,"\\{","{");
        this.value=Tools.replaceAllOnetime(this.value,"\\}","}");
        this.value=Tools.replaceAllOnetime(this.value,"\\#","#");
    }

    private void validateCondition(String sql,int idxBracketStart,int idxBracketEnd,int idxJingStart,int idxJingEnd)
    {
        if(idxBracketStart>=0&&idxBracketEnd<0||idxBracketStart<0&&idxBracketEnd>=0||idxBracketStart>=idxBracketEnd&&idxBracketEnd>=0)
        {//只有一个{或只有一个}或者{在}的后面
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
        {//##之间只有空格
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，#和#之间不是有效的<condition/>的name属性值");
        }
        if(idxBracketStart<idxJingStart&&idxBracketEnd<idxJingEnd&&idxBracketStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{、}、#、#之间的关系混乱");
        }
        if(idxBracketEnd<idxJingStart&&idxBracketStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{、}之间的条件表达式没有指定动态条件的name属性");
        }
        if(idxBracketStart>idxJingStart&&idxBracketStart<idxJingEnd&&idxJingStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+this.getReportBean()+"的SQL语句："+this.value+"中的动态条件失败，{、}、#、#之间的关系混乱");
        }
    }
    
    private int getValidIndex(String sql,char sign,int startindex)
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
                {//上一个字符是转义字符\，则说明此sign是一个普通字符，所以这里跳过
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
        {
            ConditionBean cbReferedTmp;
            String conditionNameTmp;
            for(ConditionInSqlBean csbeanTmp:this.lstConditionInSqlBeans)
            {
                conditionNameTmp=csbeanTmp.getRealConditionname();
                if(conditionNameTmp==null||conditionNameTmp.trim().equals("")) continue;
                if(!lstConditionNamesInSql.contains(conditionNameTmp)) lstConditionNamesInSql.add(conditionNameTmp);
                cbReferedTmp=this.getConditionBeanByName(conditionNameTmp);
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
        for(ConditionBean cbeanTmp:this.lstConditions)
        {
            if(lstConditionNamesInSql.contains(cbeanTmp.getName())) continue;//在<sql/>中通过#name#形式指定了条件的<condition/>可以不在<condition/>中配置条件表达式
            if(!cbeanTmp.isExistConditionExpression(true))
            {
                
                
                       // +"的查询条件即没有以#name#的形式出现在sql语句中，也没有在<condition/>中配置条件表达式");
            }
        }
    }
    
    public final static String sqlprex="select * from (";//有的地方要根据这个去除掉这个前缀和下面的后缀

    public final static String sqlsuffix=") wabacus_temp_tbl";
    
    public void doPostLoadSql(boolean isListReportType)
    {
        if(value==null||value.trim().equals("")||this.isStoreProcedure()) return;
        this.sqlWithoutOrderby=sqlprex+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+sqlsuffix;
        if(isListReportType)
        {
            this.sqlWithoutOrderby=this.sqlWithoutOrderby+" "+Consts_Private.PLACEHODER_FILTERCONDITION;
        }
        this.sqlWithoutOrderby=this.sqlWithoutOrderby+" %orderby%";
        String sqlKernel=value;
        String sqlTemp=Tools.removeBracketAndContentInside(value);
        if(sqlTemp.indexOf("(")>=0||sqlTemp.indexOf(")")>=0)
        {
            throw new WabacusConfigLoadingException("配置的sql语句："+value+"不合法，左右括号没有配对");
        }
        sqlTemp=Tools.replaceAll(sqlTemp,"  "," ");
        if(sqlTemp.toLowerCase().indexOf("order by")>0)
        {
            int idx_orderby=value.toLowerCase().lastIndexOf("order by");
            sqlKernel=value.substring(0,idx_orderby);
            String orderbyTmp=value.substring(idx_orderby+"order by ".length());
            List<String> lstOrderByColumns=Tools.parseStringToList(orderbyTmp,",",false);
            StringBuffer orderbuf=new StringBuffer();
            for(String orderby_tmp:lstOrderByColumns)
            {
                if(orderby_tmp==null||orderby_tmp.trim().equals("")) continue;
                orderby_tmp=orderby_tmp.trim();
                int idx=orderby_tmp.indexOf(".");
                if(idx>0)
                {
                    orderbuf.append(orderby_tmp.substring(idx+1));
                }else
                {
                    orderbuf.append(orderby_tmp);
                }
                orderbuf.append(",");
            }
            orderbyTmp=orderbuf.toString();
            if(orderbyTmp.charAt(orderbyTmp.length()-1)==',') orderbyTmp=orderbyTmp.substring(0,orderbyTmp.length()-1);
            this.orderby=orderbyTmp;
        }else
        {//没有配置order by
            String column=null;
            for(ColBean cbTmp:this.getReportBean().getDbean().getLstCols())
            {
                if(!cbTmp.isControlCol()&&!cbTmp.isNonFromDbCol()&&!cbTmp.isNonValueCol())
                {
                    column=cbTmp.getColumn();
                    if(column!=null&&!column.trim().equals("")) break;
                }
            }

            this.orderby=column;
        }
        this.sql_kernel=sqlKernel;
        this.sqlCount="select count(*) from ("+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+")  tabletemp ";
        if(isListReportType)
        {
            this.sqlCount=this.sqlCount+Consts_Private.PLACEHODER_FILTERCONDITION;
        }
    }
    
    public void buildPageSplitSql()
    {
        AbsDatabaseType dbtype=Config.getInstance().getDataSource(this.getDatasource()).getDbType();
        if(dbtype==null)
        {
            throw new WabacusConfigLoadingException("没有实现数据源"+this.getDatasource()+"对应数据库类型的相应实现类");
        }
        this.setSplitpage_sql(dbtype.constructSplitPageSql(this));
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        SqlBean sbeanNew=(SqlBean)super.clone(parent);
        ((ReportBean)parent).setSbean(sbeanNew);
        if(this.lstConditions!=null&&this.lstConditions.size()>0)
        {
            ConditionBean cb=null;
            ArrayList<ConditionBean> lstConditionsNew=new ArrayList<ConditionBean>();
            for(int i=0;i<lstConditions.size();i++)
            {
                cb=(ConditionBean)(lstConditions.get(i)).clone(sbeanNew);
                lstConditionsNew.add(cb);
            }
            sbeanNew.setLstConditions(lstConditionsNew);
        }
        if(lstConditionFromRequestNames!=null)
        {
            sbeanNew
                    .setLstConditionFromRequestNames((List<String>)((ArrayList<String>)lstConditionFromRequestNames)
                            .clone());
        }
        if(lstConditionInSqlBeans!=null)
        {
            List<ConditionInSqlBean> lstConditionInSqlBeansNew=new ArrayList<ConditionInSqlBean>();
            for(ConditionInSqlBean csbeanTmp:lstConditionInSqlBeans)
            {
                lstConditionInSqlBeansNew.add(csbeanTmp.clone());
            }
            sbeanNew.setLstConditionInSqlBeans(lstConditionInSqlBeansNew);
        }
        if(lstStoreProcedureParams!=null)
        {
            sbeanNew.setLstStoreProcedureParams((List<String>)((ArrayList<String>)lstStoreProcedureParams).clone());
        }
        cloneExtendConfig(sbeanNew);
        return sbeanNew;
    }
}
