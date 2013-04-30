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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class StoreProcedureActionBean extends AbsEditSqlActionBean
{
    private static Log log=LogFactory.getLog(StoreProcedureActionBean.class);
    
    private List lstParams;
    
    public StoreProcedureActionBean(EditableReportUpdateDataBean _owner)
    {
        super(_owner);
    }

    public void parseSql(SqlBean sqlbean,String reportTypeKey,String configSql)
    {
        ReportBean rbean=sqlbean.getReportBean();
        configSql=this.parseAndRemoveReturnParamname(configSql);
        String procedure=configSql;
        procedure=procedure.substring("call ".length()).trim();
        if(procedure.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的更新语句"+configSql+"失败，没有指定要调用的存储过程名");
        }
        int idxLeft=procedure.indexOf("(");
        if(idxLeft<0)
        {
            this.sql="{call "+procedure+"(";
            if(this.returnValueParamname!=null&&!this.returnValueParamname.trim().equals(""))
            {
                this.sql+="?";
            }
            this.sql+=")}";
        }else
        {
            int idxRight=procedure.lastIndexOf(")");
            if(idxLeft==0||idxRight!=procedure.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的更新语句"+configSql+"失败，配置的要调用的存储过程格式不对");
            }
            String procname=procedure.substring(0,idxLeft).trim();
            String params=procedure.substring(idxLeft+1,idxRight).trim();
            if(params.equals(""))
            {//没有参数
                this.sql="{call "+procname+"(";
                if(this.returnValueParamname!=null&&!this.returnValueParamname.trim().equals(""))
                {
                    this.sql+="?";
                }
                this.sql+=")}";
            }else
            {
                List<String> lstParamsTmp=Tools.parseStringToList(params,',','\'');
                List lstProcedureParams=new ArrayList();
                for(String paramTmp:lstParamsTmp)
                {
                    if(Tools.isDefineKey("sequence",paramTmp))
                    {
                        lstProcedureParams.add(Tools.getRealKeyByDefine("sequence",paramTmp)+".nextval");
                    }else if(paramTmp.equals("uuid{}"))
                    {
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname("uuid{}");
                        lstProcedureParams.add(paramBean);
                    }else if(Tools.isDefineKey("!",paramTmp))
                    {
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(paramTmp);
                        lstProcedureParams.add(paramBean);
                    }else if(Tools.isDefineKey("#",paramTmp))
                    {//是从<external-values/>中定义的变量中取值
                        paramTmp=Tools.getRealKeyByDefine("#",paramTmp);
                        EditableReportExternalValueBean valuebean=owner.getValueBeanByName(paramTmp);
                        if(valuebean==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有定义"+paramTmp+"对应的变量值");
                        }
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(paramTmp);
                        paramBean.setOwner(valuebean);
                        lstProcedureParams.add(paramBean);
                    }else if(Tools.isDefineKey("@",paramTmp))
                    {
                        String realParamValue=Tools.getRealKeyByDefine("@",paramTmp).trim();
                        boolean isOldValue=false;
                        if(realParamValue.endsWith("_old"))
                        {
                            isOldValue=true;
                            realParamValue=realParamValue.substring(0,realParamValue.length()-4).trim();
                        }
                        ColBean cbTmp=rbean.getDbean().getColBeanByColProperty(realParamValue);
                        if(cbTmp==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+realParamValue+"不合法，没有取到其值对应的<col/>");
                        }
                        EditableReportParamBean paramBean=owner.createParamBeanByColbean(cbTmp,reportTypeKey,true,true);
                        if(isOldValue)
                        {//如果是用此列的旧数据，则相当于用在where条件中
                            this.addColParamBeanInWhereClause(paramBean,reportTypeKey);
                        }else
                        {
                            this.addColParamBeanInUpdateClause(paramBean,reportTypeKey);
                        }
                        lstProcedureParams.add(paramBean);
                    }else
                    {
                        if(paramTmp.startsWith("'")&&paramTmp.endsWith("'")) paramTmp=paramTmp.substring(1,paramTmp.length()-1);
                        if(paramTmp.startsWith("\"")&&paramTmp.endsWith("\"")) paramTmp=paramTmp.substring(1,paramTmp.length()-1);
                        lstProcedureParams.add(paramTmp);
                    }
                }
                StringBuffer tmpBuf=new StringBuffer("{call "+procname+"(");
                for(int i=0,len=lstProcedureParams.size();i<len;i++)
                {
                    tmpBuf.append("?,");
                }
                if(tmpBuf.charAt(tmpBuf.length()-1)==',')
                {
                    tmpBuf.deleteCharAt(tmpBuf.length()-1);
                }
                if(this.returnValueParamname!=null&&!this.returnValueParamname.trim().equals(""))
                {
                    tmpBuf.append(",?");
                }
                tmpBuf.append(")}");
                this.sql=tmpBuf.toString();
                this.lstParams=lstProcedureParams;
            }
        }
        owner.getLstSqlActionBeans().add(this);
    }

    public void updateDBData(Map<String,String> mParamsValue,Map<String,String> mExternalParamsValue,Connection conn,ReportBean rbean,
            ReportRequest rrequest) throws SQLException
    {
        AbsDatabaseType dbtype=rrequest.getDbType(rbean.getSbean().getDatasource());
        CallableStatement cstmt=null;
        try
        {
            if(Config.show_sql) log.info("Execute sql:"+sql);
            cstmt=conn.prepareCall(sql);
            if(lstParams!=null&&lstParams.size()>0)
            {
                int idx=1;
                IDataType varcharTypeObj=Config.getInstance().getDataTypeByClass(VarcharType.class);
                EditableReportParamBean paramBeanTmp;
                for(Object paramObjTmp:this.lstParams)
                {
                    if(paramObjTmp instanceof EditableReportParamBean)
                    {
                        paramBeanTmp=(EditableReportParamBean)paramObjTmp;
                        paramBeanTmp.getDataTypeObj().setPreparedStatementValue(idx++,
                                getParamValue(mParamsValue,mExternalParamsValue,rbean,rrequest,paramBeanTmp),cstmt,dbtype);
                    }else
                    {//常量参数
                        varcharTypeObj.setPreparedStatementValue(idx++,paramObjTmp==null?"":String.valueOf(paramObjTmp),cstmt,dbtype);
                    }
                }
            }
            int outputindex=-1;
            if(this.returnValueParamname!=null&&!this.returnValueParamname.trim().equals(""))
            {
                outputindex=this.lstParams==null?1:this.lstParams.size()+1;
                cstmt.registerOutParameter(outputindex,java.sql.Types.VARCHAR);
            }
            cstmt.execute();
            if(outputindex>0)
            {
                String rtnVal=cstmt.getString(outputindex);
                storeReturnValue(rrequest,mExternalParamsValue,rtnVal);
            }
        }finally
        {
            WabacusAssistant.getInstance().release(null,cstmt);
        }
    }    
}
