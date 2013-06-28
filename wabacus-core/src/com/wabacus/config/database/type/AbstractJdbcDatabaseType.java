/*
 * Copyright (C) 2010-2012 星星<349446658@qq.com>
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
package com.wabacus.config.database.type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.component.application.report.condition.ConditionInSqlBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.IConnection;
import com.wabacus.system.JdbcConnection;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsJavaEditActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.DeleteSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditActionGroupBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.component.application.report.configbean.editablereport.InsertSqlActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.StoreProcedureActionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.UpdateSqlActionBean;
import com.wabacus.system.dataset.ISqlDataSetBuilder;
import com.wabacus.system.dataset.QueryStatementType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

//$ByQXO NEW   数据存储可扩展性修改     //ByQXO$
public abstract class AbstractJdbcDatabaseType extends AbsDatabaseType
{

    private static  final Log log=LogFactory.getLog(AbstractJdbcDatabaseType.class);


    @Override
    public String getSequnceValue(IConnection conn,String seqname) throws SQLException
    {
        return getSequnceValue(((JdbcConnection)conn).getNativeConnection(),seqname);
    }
    public String getSequnceValue(Connection conn,String seqname) throws SQLException
    {
        //不同数据库获取sequence方式是不同的,此处应用改为AbsDatabaseType中获取
        String sql="select "+Tools.getRealKeyByDefine("sequence",seqname)+".nextval from dual";
        Statement stmt=conn.createStatement();
        ResultSet rs=stmt.executeQuery(sql);
        rs.next();
        final String seqVal=String.valueOf(rs.getInt(1));
        rs.close();
        stmt.close();
        return seqVal;
    }

    

    public String parseDeleteSql(DeleteSqlActionBean actionBean,String reportTypeKey,String actionscript)
    {
        actionscript=actionBean.parseAndRemoveReturnParamname(actionscript);
        final List lstParamBeans=new ArrayList<EditableReportParamBean>();
        final EditActionGroupBean ownerGroupBean=actionBean.getOwnerGroupBean();
        final String sql= ownerGroupBean.getOwnerUpdateBean().parseStandardEditSql(actionscript,lstParamBeans,reportTypeKey);
        actionBean.setLstParamBeans(lstParamBeans);
        return sql;
    }
   
    
    @Override
    public void updateData(Map<String,String> mRowData,
            Map<String,String> mParamValues,ReportBean rbean,
            ReportRequest rrequest,AbsEditSqlActionBean actionBean) throws SQLException
    {

        final EditActionGroupBean ownerGroupBean=actionBean.getOwnerGroupBean();
       final AbsDatabaseType dbtype= this;//rrequest.getDbType(ownerGroupBean.getDatasource());
        Connection conn= rrequest.getConnection(ownerGroupBean.getDatasource());
        Oracle oracleType=null;
        PreparedStatement pstmt=null;
        String sql = actionBean.getSql();
        List<EditableReportParamBean> lstParamBeans=actionBean.getLstParamBeans();
        try
        {
            if(Config.show_sql) log.info("Execute sql:"+sql);
            pstmt=conn.prepareStatement(sql);
            if(sql.trim().toLowerCase().startsWith("select ")&&(dbtype instanceof Oracle))
            {
                oracleType=(Oracle)dbtype;
                if(lstParamBeans!=null&&lstParamBeans.size()>0)
                {
                    int colidx=1;
                    for(EditableReportParamBean paramBean:lstParamBeans)
                    {
                        if((paramBean.getDataTypeObj() instanceof ClobType)||(paramBean.getDataTypeObj() instanceof BlobType)) continue;
                        paramBean.getDataTypeObj().setPreparedStatementValue(colidx++,
                                actionBean.getParamValue(mRowData,mParamValues,rbean,rrequest,paramBean),pstmt,dbtype);
                    }
                }
                ResultSet rs=pstmt.executeQuery();
                while(rs.next())
                {
                    if(lstParamBeans!=null&&lstParamBeans.size()>0)
                    {
                        int colidx=1;
                        for(EditableReportParamBean paramBean:lstParamBeans)
                        {
                            if(!(paramBean.getDataTypeObj() instanceof ClobType)&&!(paramBean.getDataTypeObj() instanceof BlobType)) continue;
                            String paramvalue=actionBean.getParamValue(mRowData,mParamValues,rbean,rrequest,paramBean);
                            if(paramBean.getDataTypeObj() instanceof ClobType)
                            {
                                oracleType.setClobValueInSelectMode(paramvalue,(oracle.sql.CLOB)rs.getClob(colidx++));
                            }else
                            {
                                oracleType.setBlobValueInSelectMode(paramBean.getDataTypeObj().label2value(paramvalue),(oracle.sql.BLOB)rs
                                        .getBlob(colidx++));
                            }
                        }
                    }
                }
                rs.close();
            }else
            {
                if(lstParamBeans!=null&&lstParamBeans.size()>0)
                {
                    int idx=1;
                    for(EditableReportParamBean paramBean:lstParamBeans)
                    {
                        paramBean.getDataTypeObj().setPreparedStatementValue(idx++,
                                actionBean.getParamValue(mRowData,mParamValues,rbean,rrequest,paramBean),pstmt,dbtype);
                    }
                }
                int rtnVal=pstmt.executeUpdate();
                actionBean.storeReturnValue(rrequest,mParamValues,String.valueOf(rtnVal));
            }
        }finally
        {
            WabacusAssistant.getInstance().release(null,pstmt);
        }
    }
//
//
//
    public ISqlDataSetBuilder getISQLTypeBuilder(ReportDataSetValueBean bean,String statementtype){
        QueryStatementType qtype = null;
        if(bean.isStoreProcedure())
        {
            qtype = QueryStatementType.STORED_PROCEDURE;
        }else
        {
            qtype = QueryStatementType.getType(statementtype);
        }
       return new ISqlDataSetBuilder(statementtype,qtype);
    }
//
//
//
//    public void parseConditionInSql(SqlBean bean,String value)
//    {
//        if(bean.isStoreProcedure()) return;
//      
//        
//         if(value==null||value.trim().equals("")) return;
//        List<ConditionInSqlBean> lstConditionsInSqlBeans=new ArrayList<ConditionInSqlBean>();
//        ConditionInSqlBean csbeanTmp;
//        int placeholderIndex=0;
//        String sql=value;
//        StringBuffer sqlBuf=new StringBuffer();
//        int idxBracketStart;//存放sql语句中第一个有效{号的下标
//        int idxBracketEnd;
//        int idxJingStart;
//        int idxJingEnd;
//        final int stmttype= bean.getStatementType();
//        while(true)
//        {
//            idxBracketStart=SqlBean.getValidIndex(sql,'{',0);
//            idxBracketEnd=SqlBean.getValidIndex(sql,'}',0);
//            idxJingStart=SqlBean.getValidIndex(sql,'#',0);
//            if(idxJingStart<0)
//            {
//                idxJingEnd=-1;
//            }else
//            {
//                idxJingEnd= SqlBean.getValidIndex(sql,'#',idxJingStart+1);
//            }
//            if(idxBracketStart<0&&idxBracketEnd<0&&idxJingStart<0&&idxJingEnd<0) break;//所有动态条件处理完毕
//
//            bean.validateCondition(sql,idxBracketStart,idxBracketEnd,idxJingStart,idxJingEnd);
//            if(idxJingEnd>=0&&(idxJingEnd<idxBracketStart||idxBracketStart<0))
//            {
//                String prex=sql.substring(0,idxJingStart);
//                String expression=sql.substring(idxJingStart,idxJingEnd+1);//要包括左右的#号，所以后面用idxJingEnd+1
//                String suffix=sql.substring(idxJingEnd+1);
//                String conditionname=expression;
//                expression="#data#";//这里用#data#占位符即可，方便解析pattern，不用标识<condition/>的name，因为会在对应的ConditionInSqlBean的中标识
//                if(prex.endsWith("%"))
//                {
//                    prex=prex.substring(0,prex.length()-1);
//                    expression="%"+expression;
//                }
//                if(prex.endsWith("'"))
//                {
//                    prex=prex.substring(0,prex.length()-1);
//                    expression="'"+expression;
//                }
//                if(suffix.startsWith("%"))
//                {
//                    suffix=suffix.substring(1);
//                    expression=expression+"%";
//                }
//                if(suffix.startsWith("'"))
//                {
//                    suffix=suffix.substring(1);
//                    expression=expression+"'";
//                }
//                sql=suffix;
//                csbeanTmp=new ConditionInSqlBean(bean);
//                lstConditionsInSqlBeans.add(csbeanTmp);
//                csbeanTmp.setConditionname(conditionname);
//                csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
//                ConditionExpressionBean expressionBean=new ConditionExpressionBean();
//                csbeanTmp.setConditionExpression(expressionBean);
//                expressionBean.setValue(expression);
//                if(stmttype==SqlBean.STMTYPE_PREPAREDSTATEMENT){
//                    expressionBean.parseConditionExpression(csbeanTmp);
//                }
//                sqlBuf.append(prex).append(csbeanTmp.getPlaceholder());
//                placeholderIndex++;
//            }else if(idxBracketStart<idxJingStart&&idxBracketEnd>idxJingEnd&&idxBracketStart>=0&&idxJingEnd>=0)
//            {
//
//                sqlBuf.append(sql.substring(0,idxBracketStart));
//                String expression=sql.substring(idxBracketStart,idxBracketEnd+1);
//                if(expression.equals("{#condition#}"))
//                {
//                    csbeanTmp=new ConditionInSqlBean(bean);
//                    csbeanTmp.setConditionname("{#condition#}");
//                    lstConditionsInSqlBeans.add(csbeanTmp);
//                    sqlBuf.append(" {#condition#} ");
//                }else
//                {
//                    csbeanTmp=new ConditionInSqlBean(bean);
//                    csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
//                    sqlBuf.append(csbeanTmp.getPlaceholder());
//                    if(idxBracketStart==0&&idxJingStart==1&&idxBracketEnd==expression.length()-1&&idxJingEnd==expression.length()-2)
//                    {//{#name#}形式，则某个查询条件本身就是一个完整的条件表达式
//                        csbeanTmp.setConditionname(expression);
//                    }else
//                    {
//                        expression=expression.substring(1,expression.length()-1);
//                        String conditionname=sql.substring(idxJingStart+1,idxJingEnd);//放在一个{}中的一定是从同一个<condition/>（即name属性相同）中取值做为条件，因此在这里可以取到此name属性（在##之间的值），
//                        if(conditionname.equals("data"))
//                        {
//                            throw new WabacusConfigLoadingException("解析报表"+bean.getReportBean().getPath()+"的查询SQL语句"+value
//                                    +"失败，不能在其中直接使用占位符#data#，这是一个框架做为保留字的字符串，请使用#conditionname#格式");
//                        }
//                        expression=Tools.replaceAll(expression,"#"+conditionname+"#","#data#");
//                        csbeanTmp.setConditionname(conditionname);
//                        ConditionExpressionBean expressionBean=new ConditionExpressionBean();
//                        csbeanTmp.setConditionExpression(expressionBean);
//                        expressionBean.setValue(expression);
//                        if(stmttype==SqlBean.STMTYPE_PREPAREDSTATEMENT){
//                            expressionBean.parseConditionExpression(csbeanTmp);
//                        }
//                    }
//                    lstConditionsInSqlBeans.add(csbeanTmp);
//                    placeholderIndex++;
//                }
//                sql=sql.substring(idxBracketEnd+1);
//            }else
//            {
//                throw new WabacusConfigLoadingException("解析报表"+bean.getReportBean()+"的SQL语句："+value+"中的动态条件失败，无法解析其中用{}和##括住的动态条件");
//            }
//        }
//        if(!sql.equals("")) sqlBuf.append(sql);
//        if(lstConditionsInSqlBeans==null||lstConditionsInSqlBeans.size()==0
//                ||(lstConditionsInSqlBeans.size()==1&&lstConditionsInSqlBeans.get(0).getConditionname().equals("{#condition#}")))
//        {
//            lstConditionsInSqlBeans = null;
//           // this.lstConditionInSqlBeans=null;
//        }else
//        {
//            value=sqlBuf.toString();
//            //this.lstConditionInSqlBeans=lstConditionsInSqlBeans;
//        }
//        bean.setLstConditionInSqlBeans(lstConditionsInSqlBeans);
//        value=Tools.replaceAllOnetime(value,"\\{","{");
//        value=Tools.replaceAllOnetime(value,"\\}","}");
//        value=Tools.replaceAllOnetime(value,"\\#","#");
//        bean.setValue(value);
//    }
//

    
  /**
   * sqlWithoutOrderby,orderby,sqlKernel,sqlCount filterdata_sql
   */
    public void doPostLoadSql(ReportDataSetValueBean rdsbean,boolean isListReportType)
    {
        //ByQXO
        final String value = rdsbean.getValue();
        if(value==null||value.trim().equals("")||rdsbean.isStoreProcedure()||rdsbean.getCustomizeDatasetObj()!=null) return;

        String sqlWithoutOrderby=ReportDataSetValueBean.sqlprex+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+ReportDataSetValueBean.sqlsuffix;
        if(isListReportType)
        {
            sqlWithoutOrderby=sqlWithoutOrderby+" "+Consts_Private.PLACEHODER_FILTERCONDITION;
        }
        sqlWithoutOrderby=sqlWithoutOrderby+" %orderby%";
        rdsbean.setSqlWithoutOrderby(sqlWithoutOrderby);
        
        String sqlKernel=value;
        String sqlTemp=Tools.removeBracketAndContentInside(value,true);
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
            rdsbean.setOrderby(orderbyTmp);
        }else
        {
            String column=null;
            for(ColBean cbTmp:rdsbean.getReportBean().getDbean().getLstCols())
            {
                if(!cbTmp.isControlCol()&&!cbTmp.isNonFromDbCol()&&!cbTmp.isNonValueCol()&&!cbTmp.isSequenceCol()&&cbTmp.isMatchDataSet(rdsbean))
                {
                    column=cbTmp.getColumn();
                    if(column!=null&&!column.trim().equals("")) break;
                }
            }
            //            log.debug("没有为查询报表"+this.getReportBean().getPath()+"的数据配置order by子句，将会按"+column+"字段进行排序");
            rdsbean.setOrderby(column);
        }
        final String sql_kernel=sqlKernel;
        if(!rdsbean.isDependentDataSet())
        {
            String sqlCount="select count(*) from ("+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+")  wx_tabletemp ";
            if(isListReportType)
            {
                sqlCount= sqlCount+Consts_Private.PLACEHODER_FILTERCONDITION;
                String filterdata_sql="select distinct %FILTERCOLUMN%  from ("+sql_kernel+") wx_tblfilter "
                        +Consts_Private.PLACEHODER_FILTERCONDITION+" order by  %FILTERCOLUMN%";
                rdsbean.setFilterdata_sql(filterdata_sql);
            }
            rdsbean.setSqlCount(sqlCount);
        }        
        rdsbean.setSql_kernel(sql_kernel);
    }
    

    //from .../report/abstractreport/AbsListReportType
    public void constructSqlForListType(SqlBean sqlbean){
        if(sqlbean==null||sqlbean.getLstDatasetBeans()==null) return;
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)sqlbean.getReportBean().getDbean().getExtendConfigDataForReportType(
                AbsListReportType.KEY);
        boolean isMatchedRowGroupColAndDataset=false;
        for(ReportDataSetBean dsbeanTmp:sqlbean.getLstDatasetBeans())
        {
            for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
            {
                String value=dsvbeanTmp.getValue();
                if(value==null||value.trim().equals("")||dsvbeanTmp.isStoreProcedure()||dsvbeanTmp.getCustomizeDatasetObj()!=null) continue;
                dsvbeanTmp.doPostLoadSql(true);
                if(alrdbean!=null&&alrdbean.getRowGroupColsNum()>0&&dsvbeanTmp.isMatchDatasetid(alrdbean.getRowgroupDatasetId()))
                {
                    if(isMatchedRowGroupColAndDataset)
                    {
                        throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置了多个独立数据集，必须为其行分组列指定来自哪个数据集");
                    }
                    isMatchedRowGroupColAndDataset=true;
                    List<String> lstColsColumnInRowGroup=alrdbean.getLstRowgroupColsColumn();
                    if(lstColsColumnInRowGroup!=null&&lstColsColumnInRowGroup.size()>0)
                    {
                        List<Map<String,String>> lstRowgroupColsAndOrders=new ArrayList<Map<String,String>>();
                        alrdbean.setLstRowgroupColsAndOrders(lstRowgroupColsAndOrders);
                        if(dsvbeanTmp.getSqlWithoutOrderby().indexOf("%orderby%")<0)
                        {
                            dsvbeanTmp.setSqlWithoutOrderby(dsvbeanTmp.getSqlWithoutOrderby()+" %orderby%");
                            StringBuffer orderbybuf=new StringBuffer();
                            for(String column:lstColsColumnInRowGroup)
                            {
                                if(column==null) continue;
                                orderbybuf.append(column).append(",");
                                Map<String,String> mColAndOrders=new HashMap<String,String>();
                                mColAndOrders.put(column,"asc");//默认都为升序排序
                                lstRowgroupColsAndOrders.add(mColAndOrders);
                            }
                            if(orderbybuf.charAt(orderbybuf.length()-1)==',')
                            {
                                orderbybuf.deleteCharAt(orderbybuf.length()-1);
                            }
                            dsvbeanTmp.setOrderby(orderbybuf.toString());
                        }else
                        {
                            addRowGroupColumnToOrderByClause(dsvbeanTmp,lstColsColumnInRowGroup,lstRowgroupColsAndOrders);
                        }
                    }
                }
                if(!dsvbeanTmp.isDependentDataSet()) dsvbeanTmp.buildPageSplitSql();
            }
        }
        }
    
    
    private void addRowGroupColumnToOrderByClause(ReportDataSetValueBean svbean,List<String> lstColsColumnInRowGroup,
            List<Map<String,String>> lstRowgroupColsAndOrders)
    {
        String oldorderby=svbean.getOrderby();
        List<String> lstOrderByColumns=Tools.parseStringToList(oldorderby,",");
        List<Map<String,String>> lstOldOrderByColumns=new ArrayList<Map<String,String>>();
        for(String orderby_tmp:lstOrderByColumns)
        {
            if(orderby_tmp==null||orderby_tmp.trim().equals("")) continue;
            orderby_tmp=orderby_tmp.trim();
            List<String> lstTemp=Tools.parseStringToList(orderby_tmp," ");
            Map<String,String> mOldOrderBy=new HashMap<String,String>();
            lstOldOrderByColumns.add(mOldOrderBy);
            if(lstTemp.size()==1)
            {
                mOldOrderBy.put(lstTemp.get(0),"asc");
            }else if(lstTemp.size()==2)
            {
                mOldOrderBy.put(lstTemp.get(0),lstTemp.get(1).trim().toLowerCase());
            }else
            {
                throw new WabacusConfigLoadingException("报表"+svbean.getReportBean().getPath()+"配置的SQL语句中order by子句"+svbean.getOrderby()+"不合法");
            }
        }
        StringBuffer orderBuf=new StringBuffer();
        for(String rowgroupCol:lstColsColumnInRowGroup)
        {
            if(rowgroupCol==null) continue;
            Map<String,String> mColAndOrders=new HashMap<String,String>();
            lstRowgroupColsAndOrders.add(mColAndOrders);
            Map<String,String> mTemp=null;
            for(Map<String,String> mTemp2:lstOldOrderByColumns)
            {
                if(mTemp2.containsKey(rowgroupCol))
                {
                    mTemp=mTemp2;
                    lstOldOrderByColumns.remove(mTemp2);
                    break;
                }
            }
            if(mTemp!=null)
            {
                orderBuf.append(rowgroupCol).append(" ").append(mTemp.get(rowgroupCol)).append(",");
                mColAndOrders.put(rowgroupCol,mTemp.get(rowgroupCol));
            }else
            {
                orderBuf.append(rowgroupCol).append(",");
                mColAndOrders.put(rowgroupCol,"asc");
            }
        }
        
        for(Map<String,String> mTemp:lstOldOrderByColumns)
        {
            Entry<String,String> entry=mTemp.entrySet().iterator().next();
            orderBuf.append(entry.getKey()).append(" ").append(entry.getValue()).append(",");
        }
        if(orderBuf.charAt(orderBuf.length()-1)==',')
        {
            orderBuf.deleteCharAt(orderBuf.length()-1);
        }
        svbean.setOrderby(orderBuf.toString());
    }    
    public void parseActionscripts(EditActionGroupBean eagbean,String reportTypeKey)
    {
        ReportBean rbean=eagbean.getOwnerUpdateBean().getOwner().getReportBean();
        List<String> lstActionscripts=Tools.parseStringToList(eagbean.getActionscripts(),';','\"');
        String realSqlTmp;
        for(String scriptTmp:lstActionscripts)
        {
            if(scriptTmp==null||scriptTmp.trim().equals("")) continue;
            scriptTmp=scriptTmp.trim();
            if(Tools.isDefineKey("class",scriptTmp))
            {
                scriptTmp=Tools.getRealKeyByDefine("class",scriptTmp).trim();
                String javaname=scriptTmp;
                String params=null;
                int idx1=scriptTmp.indexOf("(");
                int idx2=scriptTmp.indexOf(")");
                if(idx1>0&&idx2==scriptTmp.length()-1)
                {
                    javaname=scriptTmp.substring(0,idx1).trim();
                    params=scriptTmp.substring(idx1+1,idx2).trim();
                }else if(idx1>=0||idx2>=0)
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据JAVA类"+scriptTmp+"不合法");
                }
                Object javaActionBean;
                try
                {
                    javaActionBean=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(javaname).newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据JAVA类"+scriptTmp+"无法实例化",e);
                }
                if(!(javaActionBean instanceof AbsJavaEditActionBean))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据JAVA类"+scriptTmp+"没有继承"
                            +AbsJavaEditActionBean.class.getName());
                }
                ((AbsEditActionBean)javaActionBean).setOwnerGroupBean(eagbean);
                ((AbsEditActionBean)javaActionBean).parseActionscript(reportTypeKey,params);
                eagbean.addActionBean((AbsEditActionBean)javaActionBean);
            }else
            {
                realSqlTmp=new UpdateSqlActionBean(eagbean).parseAndRemoveReturnParamname(scriptTmp).toLowerCase().trim();
                if(realSqlTmp.startsWith("{")&&realSqlTmp.endsWith("}"))
                {
                    realSqlTmp=realSqlTmp.substring(1,realSqlTmp.length()-1).trim();
                }
                if(realSqlTmp.indexOf("insert ")==0)
                {
                    new InsertSqlActionBean(eagbean).parseActionscript(reportTypeKey,scriptTmp);
                }else if(realSqlTmp.indexOf("update ")==0)
                {
                    new UpdateSqlActionBean(eagbean).parseActionscript(reportTypeKey,scriptTmp);
                }else if(realSqlTmp.indexOf("delete ")==0)
                {
                    new DeleteSqlActionBean(eagbean).parseActionscript(reportTypeKey,scriptTmp);
                }else if(realSqlTmp.indexOf("call ")==0)
                {
                    new StoreProcedureActionBean(eagbean).parseActionscript(reportTypeKey,scriptTmp);
                }else
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据的SQL语句"+scriptTmp+"不合法");
                }
            }
        }
    }
    
    @Override
    public void parseConditionInSql(ReportDataSetValueBean bean,String value)
    {
        if(value==null||value.trim().equals("")) return;
        List<ConditionInSqlBean> lstConditionsInSqlBeans=new ArrayList<ConditionInSqlBean>();
        ConditionInSqlBean csbeanTmp;
        int placeholderIndex=0;
        String sql=value;
        StringBuffer sqlBuf=new StringBuffer();
        int idxBracketStart;
        int idxBracketEnd;
        int idxJingStart;//存放sql语句中第一个有效#号的下标
        int idxJingEnd;
        while(true)
        {
            idxBracketStart=ReportDataSetValueBean.getValidIndex(sql,'{',0);
            idxBracketEnd=ReportDataSetValueBean.getValidIndex(sql,'}',0);
            idxJingStart=ReportDataSetValueBean.getValidIndex(sql,'#',0);
            if(idxJingStart<0)
            {
                idxJingEnd=-1;
            }else
            {
                idxJingEnd=ReportDataSetValueBean.getValidIndex(sql,'#',idxJingStart+1);
            }
            if(idxBracketStart<0&&idxBracketEnd<0&&idxJingStart<0&&idxJingEnd<0) break;
                
                
                
            bean.validateCondition(sql,idxBracketStart,idxBracketEnd,idxJingStart,idxJingEnd);
            if(idxJingEnd>=0&&(idxJingEnd<idxBracketStart||idxBracketStart<0))
            {
                String prex=sql.substring(0,idxJingStart);
                String expression=sql.substring(idxJingStart,idxJingEnd+1);
                String suffix=sql.substring(idxJingEnd+1);
                if(expression.equals("#dynamic-columns#"))
                {
                    prex=prex.trim();
                    suffix=suffix.trim();
                    if(prex.endsWith("[")&&suffix.startsWith("]")||prex.endsWith("(")&&suffix.startsWith(")"))
                    {
                        sqlBuf.append(prex).append("#dynamic-columns#").append(suffix.substring(0,1));
                        sql=suffix.substring(1);
                        continue;
                    }
                }
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
                csbeanTmp=new ConditionInSqlBean(bean);
                lstConditionsInSqlBeans.add(csbeanTmp);
                csbeanTmp.setConditionname(conditionname);
                csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
                ConditionExpressionBean expressionBean=new ConditionExpressionBean();
                csbeanTmp.setConditionExpression(expressionBean);
                expressionBean.setValue(expression);
                if(bean.getReportBean().getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT) expressionBean.parseConditionExpression();
                sqlBuf.append(prex).append(csbeanTmp.getPlaceholder());
                placeholderIndex++;
            }else if(idxBracketStart<idxJingStart&&idxBracketEnd>idxJingEnd&&idxBracketStart>=0&&idxJingEnd>=0)
            {
                
                sqlBuf.append(sql.substring(0,idxBracketStart));
                String expression=sql.substring(idxBracketStart,idxBracketEnd+1);
                if(expression.equals("{#condition#}"))
                {
                    csbeanTmp=new ConditionInSqlBean(bean);
                    csbeanTmp.setConditionname("{#condition#}");
                    lstConditionsInSqlBeans.add(csbeanTmp);
                    sqlBuf.append(" {#condition#} ");
                }else if(expression.equals(ReportDataSetValueBean.dependsConditionPlaceHolder))
                {
                    sqlBuf.append(" ").append(ReportDataSetValueBean.dependsConditionPlaceHolder).append(" ");
                }else
                {
                    csbeanTmp=new ConditionInSqlBean(bean);
                    csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
                    sqlBuf.append(csbeanTmp.getPlaceholder());
                    if(idxBracketStart==0&&idxJingStart==1&&idxBracketEnd==expression.length()-1&&idxJingEnd==expression.length()-2)
                    {
                        csbeanTmp.setConditionname(expression);
                    }else
                    {
                        expression=expression.substring(1,expression.length()-1);
                        String conditionname=sql.substring(idxJingStart+1,idxJingEnd);//放在一个{}中的一定是从同一个<condition/>（即name属性相同）中取值做为条件，因此在这里可以取到此name属性（在##之间的值），
                        if(conditionname.equals("data"))
                        {//在sql语句中配置为#data#形式
                            throw new WabacusConfigLoadingException("解析报表"+bean.getReportBean().getPath()+"的查询SQL语句"+value
                                    +"失败，不能在其中直接使用占位符#data#，这是一个框架做为保留字的字符串，请使用#conditionname#格式");
                        }
                        expression=Tools.replaceAll(expression,"#"+conditionname+"#","#data#");
                        csbeanTmp.setConditionname(conditionname);
                        ConditionExpressionBean expressionBean=new ConditionExpressionBean();
                        csbeanTmp.setConditionExpression(expressionBean);
                        expressionBean.setValue(expression);
                        if(bean.getReportBean().getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT) expressionBean.parseConditionExpression();
                    }
                    lstConditionsInSqlBeans.add(csbeanTmp);
                    placeholderIndex++;
                }
                sql=sql.substring(idxBracketEnd+1);
            }else
            {
                throw new WabacusConfigLoadingException("解析报表"+bean.getReportBean()+"的SQL语句："+value+"中的动态条件失败，无法解析其中用{}和##括住的动态条件");
            }
        }
        if(!sql.equals("")) sqlBuf.append(sql);
        if(lstConditionsInSqlBeans==null||lstConditionsInSqlBeans.size()==0
                ||(lstConditionsInSqlBeans.size()==1&&lstConditionsInSqlBeans.get(0).getConditionname().equals("{#condition#}")))
        {
            bean.setLstConditionInSqlBeans(null);
        }else
        {
            value=sqlBuf.toString();
            bean.setLstConditionInSqlBeans(lstConditionsInSqlBeans);
        }
        value=Tools.replaceAllOnetime(value,"\\{","{");
        value=Tools.replaceAllOnetime(value,"\\}","}");
        value=Tools.replaceAllOnetime(value,"\\#","#");
        bean.setValue(value);
      }

//
//    
//    /**
//     * 获取列表数据某行的可选过滤数据
//     */
//    public List<String> getFilterDataList(ReportRequest rrequest,ColBean cbean,ISQLType sqlTypeObj,int maxOptionsCount)
//            throws SQLException
//    {
//        final ReportBean rbean = cbean.getReportBean();
//        Object objTmp=sqlTypeObj.getResultSet(rrequest,(AbsReportType)rrequest.getComponentTypeObj(rbean,null,true));
//        if(objTmp==null||rrequest.getWResponse().getMessageCollector().hasErrors()||rrequest.getWResponse().getMessageCollector().hasWarnings())
//        {
//            return null;
//        }
//       
//        if(!(objTmp instanceof ResultSet))
//        {
//            throw new WabacusRuntimeException("加载报表"+rbean.getPath()+"数据失败，在加载过滤选项数据的前置动作中，只能返回SQL语句或ResultSet，不能返回加载好的List对象");
//        }
//        ResultSet rs=(ResultSet)objTmp;
//        Object valObj;
//        String strvalue;
//        List<String> lstValues=new ArrayList<String>();
//        int optioncnt=0;
//        while(rs.next())
//        {
//            valObj=cbean.getDatatypeObj().getColumnValue(rs,cbean.getColumn(),this);
//            strvalue=cbean.getDatatypeObj().value2label(valObj);
//            if(strvalue==null||strvalue.trim().equals("")) continue;
//            if(!lstValues.contains(strvalue)) lstValues.add(strvalue);
//            if(maxOptionsCount>0&&++optioncnt==maxOptionsCount)
//            {//如果指定了最大记录数，且当前记录数达到这个值，则不再取后面的选项数据
//                break;
//            }
//        }
//        rs.close();
//        return lstValues;
//    }
    
    public   void updateDataForDeleteSqlAction(Map<String,String> mRowData,
            Map<String,String> mParamValues,ReportBean rbean,
            ReportRequest rrequest,AbsEditSqlActionBean actionBean) throws SQLException{
        PreparedStatement pstmt=null;
        final EditActionGroupBean ownerGroupBean=actionBean.getOwnerGroupBean();
        final  Connection conn=rrequest.getConnection(ownerGroupBean.getDatasource());
        
        final String sql=actionBean.getSql();
        try
        {
            if(Config.show_sql)
            {
                log.info("Execute sql:"+sql);
            }
            pstmt=conn.prepareStatement(sql);
            
            List<EditableReportParamBean> lstParamBeans=actionBean.getLstParamBeans();
            if(lstParamBeans!=null&&lstParamBeans.size()>0)
            {
                EditableReportParamBean paramBean;
                for(int j=0;j<lstParamBeans.size();j++)
                {
                    paramBean=lstParamBeans.get(j);
                    paramBean.getDataTypeObj().setPreparedStatementValue(j+1,
                            actionBean.getParamValue(mRowData,mParamValues,rbean,rrequest,paramBean),pstmt,this);
                }
            }
            int rtnVal=pstmt.executeUpdate();
            actionBean.storeReturnValue(rrequest,mParamValues,String.valueOf(rtnVal));
        }finally
        {
            WabacusAssistant.getInstance().release(null,pstmt);
        }
    }
}
