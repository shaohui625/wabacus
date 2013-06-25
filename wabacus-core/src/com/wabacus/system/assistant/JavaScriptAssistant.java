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
package com.wabacus.system.assistant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SubmitFunctionParamBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class JavaScriptAssistant
{
    private final static JavaScriptAssistant instance=new JavaScriptAssistant();

    protected JavaScriptAssistant()
    {}

    public static JavaScriptAssistant getInstance()
    {
        return instance;
    }

    public void createRefreshSlaveReportsDataScript(ReportBean rbean)
    {
        if(rbean.getMDependChilds()==null||rbean.getMDependChilds().size()==0)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"没有依赖它的从报表，不需生成刷新从报表的javascript函数");
        }
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+rbean.getRefreshSlaveReportsCallBackMethodName()+"(pageid,reportid,selectedTrObjArr,deselectedTrObjArr){");
        resultBuf.append(getRefreshSlaveReportsScriptString(rbean));
        resultBuf.append("}");
        writeJsMethodToJsFiles(rbean.getPageBean(),resultBuf.toString());
    }

    private String getRefreshSlaveReportsScriptString(ReportBean rbean)
    {
        StringBuffer scriptBuf=new StringBuffer();
        scriptBuf.append("var reportguid=getComponentGuidById(pageid,reportid);");
        scriptBuf.append("var trObj=null;");
        scriptBuf.append("if(selectedTrObjArr==null||selectedTrObjArr.length<=0){ ");
        scriptBuf.append("  selectedTrObjArr=getAllSelectedTrObjs(pageid,reportid);");
        scriptBuf.append("} ");
        scriptBuf.append("if((selectedTrObjArr==null||selectedTrObjArr.length<=0)&&(deselectedTrObjArr==null||deselectedTrObjArr.length==0)){");
        scriptBuf.append("  var tableObj=document.getElementById(reportguid+'_data');");
        scriptBuf.append("  if(tableObj!=null){");
        scriptBuf.append("      var trChilds=tableObj.getElementsByTagName('TR');");
        scriptBuf.append("      if(trChilds!=null){");
        scriptBuf.append("          var trObjTmp;");
        scriptBuf.append("          for(var k=0,len=trChilds.length;k<len;k++){");//从上到下循环所有<tr/>对象，找出第一条记录的<tr/>，将它选中，并刷新从报表(之所以要循环，是因为要去掉报表头部的<tr/>)
        scriptBuf.append("              trObjTmp=trChilds[k];");
        scriptBuf.append("              if(!isListReportDataTrObj(trObjTmp)){continue;}");
        scriptBuf.append("              var trtype=trObjTmp.getAttribute('EDIT_TYPE');if(trtype==null||trtype!='add'){trObj=trObjTmp;break;}");
        scriptBuf.append("          }");
        scriptBuf.append("          if(trObj!=null){");
        AbsListReportBean alrbean=(AbsListReportBean)rbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType())||Consts.ROWSELECT_RADIOBOX.equals(alrbean.getRowSelectType()))
        {//是单选框/复选框两种类型的选中
            if(Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType()))
            {
                scriptBuf.append("var selectBoxObjTmp=getSelectCheckBoxObj(trObj);");
            }else
            {
                scriptBuf.append("var selectBoxObjTmp=getSelectRadioBoxObj(trObj);");
            }
            scriptBuf.append("selectBoxObjTmp.checked=true;doSelectedDataRowChkRadio(selectBoxObjTmp,'false');");
        }else
        {
            scriptBuf.append("selectReportDataRow(trObj,false);");
        }
        scriptBuf.append("          }");
        scriptBuf.append("      }");
        scriptBuf.append("  }");
        scriptBuf.append("}else{");
        scriptBuf.append("  var trObjTmp;");
        scriptBuf.append("  for(var k=selectedTrObjArr.length-1;k>=0;k--){");
        scriptBuf.append("      trObjTmp=selectedTrObjArr[k];");
        scriptBuf.append("      var trtype=trObjTmp.getAttribute('EDIT_TYPE');if(trtype==null||trtype!='add'){trObj=trObjTmp;break;}");//不是新增的行
        scriptBuf.append("  }");
        scriptBuf.append("}");
        scriptBuf.append("if(trObj==null){");
        scriptBuf.append("  clearCurrentSlaveTrObjForReport('").append(rbean.getPageBean().getId()).append("','").append(rbean.getId()).append("');");
        scriptBuf.append("  var slaveReportSpanObjTmp=null;var serverurl='';var staticlinkparams='';");
        scriptBuf.append(   hideAllSlaveReports(rbean,null));
        scriptBuf.append("}else{");
        scriptBuf.append(   refreshAllSlaveReports(rbean));
        scriptBuf.append("}");
        return scriptBuf.toString();
    }

    private String refreshAllSlaveReports(ReportBean rbean)
    {
        StringBuffer scriptBuf=new StringBuffer();
        scriptBuf.append("if(!shouldRefreshSlaveReportsForThisRow(trObj)) return false;");
        scriptBuf.append("var linkparams=getRefreshSlaveReportsHrefParams(trObj);");
        scriptBuf.append("if(linkparams==null||linkparams==''){wx_warn('没有取到刷新从报表数据的动态参数，刷新失败');return false;}");
        scriptBuf.append("var serverurl='"+Config.showreport_onpage_url+"&'+linkparams+'&PAGEID='+pageid;");
        scriptBuf.append("var staticlinkparams;");
        for(Entry<String,Map<String,String>> reportEntries:rbean.getMDependChilds().entrySet())
        {
            String slaveid=reportEntries.getKey();
            Map<String,String> mParamsTmp=reportEntries.getValue();
            scriptBuf.append("staticlinkparams='';");
            for(Entry<String,String> paramEntry:mParamsTmp.entrySet())
            {
                if(!Tools.isDefineKey("@",paramEntry.getValue()))
                {
                    scriptBuf
                            .append("staticlinkparams=staticlinkparams+'&"+paramEntry.getKey()+"='+encodeURIComponent('"+paramEntry.getValue()+"');");
                }
            }
            ReportBean slaverbean=rbean.getPageBean().getSlaveReportBean(slaveid);
            if(slaverbean==null)
            {
                throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"生成刷新从报表数据的javascript函数失败，没有取到"+slaveid+"对应的从报表");
            }
            scriptBuf.append("refreshComponent(serverurl+'&WX_ISREFRESH_BY_MASTER=true&SLAVE_REPORTID="+slaveid+"'+staticlinkparams);");
        }
        return scriptBuf.toString();
    }

    private String hideAllSlaveReports(ReportBean rbean,Boolean parentDisplayWhenNoData)
    {
        StringBuffer scriptBuf=new StringBuffer();
        ReportBean rbeanTmp;
        String slaveidTmp;
        scriptBuf.append("serverurl='"+Config.showreport_onpage_url+"&PAGEID='+pageid;");
        for(Entry<String,Map<String,String>> reportEntries:rbean.getMDependChilds().entrySet())
        {
            slaveidTmp=reportEntries.getKey();
            rbeanTmp=rbean.getPageBean().getReportChild(slaveidTmp,true);
            boolean mydisplayWhenNoData=rbeanTmp.isDisplayOnParentNoData();
            if(parentDisplayWhenNoData!=null&&parentDisplayWhenNoData==false)
            {//如果当前从报表的主报表也是一个参与本次刷新的从报表，且本次是隐藏显示，则当前从报表无条件隐藏
                mydisplayWhenNoData=false;
            }
            if(!mydisplayWhenNoData)
            {
                scriptBuf.append("slaveReportSpanObjTmp=document.getElementById('WX_CONTENT_").append(rbeanTmp.getGuid()).append("');");
                scriptBuf.append("if(slaveReportSpanObjTmp!=null) slaveReportSpanObjTmp.innerHTML='&nbsp;';");
            }else
            {
                Map<String,String> mParamsTmp=reportEntries.getValue();
                scriptBuf.append("staticlinkparams='';");
                for(Entry<String,String> paramEntry:mParamsTmp.entrySet())
                {
                    if(!Tools.isDefineKey("@",paramEntry.getValue()))
                    {
                        scriptBuf
                                .append("staticlinkparams=staticlinkparams+'&"+paramEntry.getKey()+"='+encodeURIComponent('"+paramEntry.getValue()+"');");
                    }
                }
                scriptBuf.append("refreshComponent(serverurl+'&WX_ISREFRESH_BY_MASTER=true&"+slaveidTmp+"_PARENTREPORT_NODATA=true&SLAVE_REPORTID="+slaveidTmp+"'+staticlinkparams);");
            }
            if(rbeanTmp.getMDependChilds()!=null) scriptBuf.append(hideAllSlaveReports(rbeanTmp,mydisplayWhenNoData));
        }
        return scriptBuf.toString();
    }
    
    public void createComponentOnloadScript(IComponentConfigBean componentBean)
    {
        List<OnloadMethodBean> lstOnloadMethods=componentBean.getLstOnloadMethods();
        if(lstOnloadMethods==null||lstOnloadMethods.size()==0) return;
        Collections.sort(lstOnloadMethods);
        StringBuffer scriptBuf=new StringBuffer();
        for(OnloadMethodBean methodBeanTmp:lstOnloadMethods)
        {
            if(Consts_Private.ONLOAD_CONFIG.equalsIgnoreCase(methodBeanTmp.getType()))
            {
                scriptBuf.append(methodBeanTmp.getMethod()).append("('").append(componentBean.getPageBean().getId()).append("','");
                scriptBuf.append(componentBean.getId()).append("');");
            }else if(Consts_Private.ONlOAD_IMGSCROLL.equalsIgnoreCase(methodBeanTmp.getType())
                    ||Consts_Private.ONlOAD_CURVETITLE.equalsIgnoreCase(methodBeanTmp.getType()))
            {
                scriptBuf.append(methodBeanTmp.getMethod()).append(";");
            }else if(Consts_Private.ONLOAD_REFRESHSLAVE.equalsIgnoreCase(methodBeanTmp.getType()))
            {//是刷新从报表的js函数，则不传入pageid和reportid两个参数
                scriptBuf.append(methodBeanTmp.getMethod()).append("('").append(componentBean.getPageBean().getId()).append("','").append(
                        componentBean.getId()).append("',null,null);");
            }
        }
        if(scriptBuf.toString().trim().equals("")) return;
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+componentBean.getOnloadMethodName()+"(){");
        resultBuf.append(scriptBuf.toString());
        resultBuf.append("}");
        writeJsMethodToJsFiles(componentBean.getPageBean(),resultBuf.toString());
    }
    
    public void createSearchValidateEvent(ReportBean rbean)
    {
        if(rbean.getSbean()==null||!rbean.getSbean().isExistConditionWithInputbox(null)) return;
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+rbean.getGuid()+"_validateSearch(metadataObj){");
        resultBuf.append("var paramsObj=getObjectByJsonString(metadataObj.metaDataSpanObj.getAttribute('validateSearchMethodDynParams'));");
        resultBuf.append("var fontChilds=document.getElementsByName('font_"+rbean.getGuid()+"_conditions');");
        resultBuf.append("if(fontChilds==null||fontChilds.length==0) return true;");
        resultBuf.append("var boxObj;var boxValue;var value_name;");
        resultBuf.append("for(var i=0,len=fontChilds.length;i<len;i=i+1){if(fontChilds[i]==null) continue;");
        resultBuf.append("boxObj=fontChilds[i];");//在这种报表类型中，传入客户端校验函数的输入框对象不是真正的输入框对象，而是其所在的<font/>对象
        resultBuf.append("value_name=boxObj.getAttribute('value_name');if(value_name==null||value_name=='') continue;");
        resultBuf.append("boxValue=getColConditionValueByParentElementObj(boxObj);");//从<font/>中获取到此输入框的值
        resultBuf.append("if(boxValue==null) boxValue='';");
        List<SubmitFunctionParamBean> lstValidateSearchParams=new ArrayList<SubmitFunctionParamBean>();
        boolean hasValidateCondition=false;
        String conditionScriptTmp;
        for(ConditionBean cbeanTmp:rbean.getSbean().getLstConditions())
        {
            conditionScriptTmp=createConditionValidateScript(cbeanTmp,lstValidateSearchParams);
            if(conditionScriptTmp==null||conditionScriptTmp.trim().equals("")) continue;
            hasValidateCondition=true;
            resultBuf.append(" if(value_name=='"+cbeanTmp.getName()+"'){");
            resultBuf.append(conditionScriptTmp);
            resultBuf.append("}");
        }
        if(hasValidateCondition)
        {
            resultBuf.append("} return true;}");
            writeJsMethodToJsFiles(rbean.getPageBean(),resultBuf.toString());
            rbean.setValidateSearchMethod(rbean.getGuid()+"_validateSearch");
            if(lstValidateSearchParams.size()>0)
            {
                rbean.setLstSearchFunctionDynParams(lstValidateSearchParams);
            }else
            {
                rbean.setLstSearchFunctionDynParams(null);
            }
        }else
        {
            rbean.setValidateSearchMethod(null);
            rbean.setLstSearchFunctionDynParams(null);
        }
    }
    
    private String createConditionValidateScript(ConditionBean cbean,List<SubmitFunctionParamBean> lstValidateSearchParams)
    {
        if(!cbean.isConditionWithInputbox()) return "";
        AbsInputBox inputbox=cbean.getInputbox();
        if(inputbox.getJsvalidate()==null||inputbox.getJsvalidate().trim().equals("")) return "";
        List<String> lstJs=Tools.parseStringToList(inputbox.getJsvalidate().trim(),',','\"');
        StringBuffer labelScriptBuf=new StringBuffer();
        List<SubmitFunctionParamBean> lstLabelParams=new ArrayList<SubmitFunctionParamBean>();
        if(cbean.getLabel()!=null&&!cbean.getLabel().trim().equals("")&&cbean.getLabelstyle()==1)
        {
            String label=cbean.getLabel();
            if(Tools.isDefineKey("i18n",label))
            {
                String paramname="param_"+cbean.getName()+lstValidateSearchParams.size();
                SubmitFunctionParamBean sfpbean=new SubmitFunctionParamBean(paramname);
                sfpbean.setValue(label);
                lstLabelParams.add(sfpbean);
                label="paramsObj."+paramname;
            }else
            {
                label="'"+label+"'";
            }
            labelScriptBuf.append("  if(boxValue=="+label+") boxValue='';");
        }
        StringBuffer scriptBuf=new StringBuffer();
        scriptBuf.append(labelScriptBuf.toString());
        lstValidateSearchParams.addAll(lstLabelParams);
        scriptBuf.append(createInputBoxValidateMethodBody(cbean,lstValidateSearchParams,lstJs,false));
        createAndWriteInputBoxValidateMethodToJsFile(inputbox,labelScriptBuf,lstLabelParams);
        return scriptBuf.toString();
    }
    
    public void writeEditableReportColValidateJs(EditableReportColBean ercolbean,StringBuffer scriptBuffer,List<SubmitFunctionParamBean> lstParams)
    {
        if(ercolbean==null||ercolbean.getInputbox()==null||ercolbean.getInputbox().getJsvalidate()==null
                ||ercolbean.getInputbox().getJsvalidate().trim().equals("")) return;
        List<String> lstJs=Tools.parseStringToList(ercolbean.getInputbox().getJsvalidate().trim(),',','\"');
        if(lstJs==null||lstJs.size()==0) return;
        scriptBuffer.append(" if(value_name=='").append(EditableReportAssistant.getInstance().getColParamName((ColBean)ercolbean.getOwner())).append(
                "') {");
        scriptBuffer.append(createInputBoxValidateMethodBody(ercolbean,lstParams,lstJs,false));
        scriptBuffer.append(" }");
        createAndWriteInputBoxValidateMethodToJsFile(ercolbean.getInputbox(),new StringBuffer(),new ArrayList<SubmitFunctionParamBean>());
    }

    private void createAndWriteInputBoxValidateMethodToJsFile(AbsInputBox inputBoxObj,StringBuffer scriptBuf,
            List<SubmitFunctionParamBean> lstMyParams)
    {
        if(inputBoxObj.getOwner().getReportBean().getJsvalidatetype()==0) return;
        List<String> lstJs=Tools.parseStringToList(inputBoxObj.getJsvalidate().trim(),",");
        if(lstJs==null||lstJs.size()==0) return;
        scriptBuf.append(createInputBoxValidateMethodBody(inputBoxObj.getOwner(),lstMyParams,lstJs,true));//一定要加到scriptBuf中，因为对于查询条件输入框，scriptBuf可能已经有代码
        if(scriptBuf.toString().trim().equals("")) return;
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function validate_"+inputBoxObj.getOwner().getInputBoxId()+"(boxObj,params){");
        resultBuf.append("var boxValue=getInputBoxValue(boxObj.getAttribute('id'),boxObj.getAttribute('typename'));");
        if(lstMyParams.size()>0)
        {
            inputBoxObj.setLstSubmitFunctionParams(lstMyParams);
            resultBuf.append("var paramsObj=getObjectByJsonString(params);");
        }else
        {
            inputBoxObj.setLstSubmitFunctionParams(null);
        }
        resultBuf.append(scriptBuf.toString());
        resultBuf.append(" boxObj.errorPromptObj.hide();");
        resultBuf.append("return true;}");
        writeJsMethodToJsFiles(inputBoxObj.getOwner().getReportBean().getPageBean(),resultBuf.toString());
    }
    
    public String createInputBoxValidateMethodBody(IInputBoxOwnerBean ownerBean,List<SubmitFunctionParamBean> lstParamBeans,
            List<String> lstJsMethods,boolean onblur)
    {
        if(lstJsMethods==null||lstJsMethods.size()==0) return "";
        StringBuffer scriptBuffer=new StringBuffer();
        for(String jsTmp:lstJsMethods)
        {
            if(jsTmp==null||jsTmp.trim().equals("")) continue;
            jsTmp=jsTmp.trim();
            String methodname=jsTmp;
            String errormsg=null;
            SubmitFunctionParamBean sfpbean=null;
            int lidx=jsTmp.indexOf("(");
            int ridx=jsTmp.lastIndexOf(")");
            if(lidx>0&&lidx<ridx)
            {
                methodname=jsTmp.substring(0,lidx);
                errormsg=jsTmp.substring(lidx+1,ridx).trim();
                errormsg=Config.getInstance().getResourceString(null,ownerBean.getReportBean().getPageBean(),errormsg,true);
                if(Tools.isDefineKey("i18n",errormsg))
                {
                    String paramnameTmp=ownerBean.getReportBean().getId()+"_param"+lstParamBeans.size();
                    sfpbean=new SubmitFunctionParamBean(paramnameTmp);
                    sfpbean.setValue(errormsg);
                    List<String> lstParamValues=new ArrayList<String>();
                    if(methodname.trim().toLowerCase().equals("isnotempty"))
                    {
                        lstParamValues.add(ownerBean.getLabel());
                    }else
                    {
                        lstParamValues.add("%"+ownerBean.getInputBoxId()+"_value%");
                    }
                    sfpbean.setLstParamValues(lstParamValues);
                    lstParamBeans.add(sfpbean);
                    errormsg="paramsObj."+paramnameTmp;
                }else
                {
                    if(errormsg!=null&&!errormsg.trim().equals(""))
                    {
                        Object[] oParams=new Object[1];
                        if(methodname.trim().toLowerCase().equals("isnotempty"))
                        {
                            oParams[0]=ownerBean.getLabel();
                        }else
                        {
                            oParams[0]="'+boxValue+'";
                        }
                        MessageFormat messformat=new MessageFormat(errormsg);
                        errormsg=messformat.format(oParams);
                        errormsg="'"+errormsg+"'";
                    }
                }
            }
            if(methodname.equals("")) continue;
            scriptBuffer.append("  if(!"+methodname+"(boxValue,boxObj))"+"{");
            if(errormsg!=null&&!errormsg.trim().equals(""))
            {
                if(sfpbean!=null)
                {
                    scriptBuffer.append(errormsg+"=").append(errormsg).append(".replace(").append("/%").append(ownerBean.getInputBoxId()).append(
                            "_value%/g,").append("boxValue);");
                }
                if(onblur)
                {
                    scriptBuffer.append("boxObj.errorPromptObj.show("+errormsg+");");
                }else
                {
                    scriptBuffer.append("wx_warn("+errormsg+");");
                }
            }
            scriptBuffer.append("return false;"+"}");
        }
        return scriptBuffer.toString();
    }

    public String getRuntimeParamsValueJsonString(ReportRequest rrequest,List<SubmitFunctionParamBean> lstSubmitFunctionParams)
    {
        StringBuffer paramsBuf=new StringBuffer();
        if(lstSubmitFunctionParams!=null&&lstSubmitFunctionParams.size()>0)
        {
            paramsBuf.append("{");
            String paramvalue;
            for(SubmitFunctionParamBean sfpbeanTmp:lstSubmitFunctionParams)
            {
                if(sfpbeanTmp==null) continue;
                paramvalue=sfpbeanTmp.getValue();
                paramvalue=paramvalue==null?"":paramvalue.trim();
                if(Tools.isDefineKey("i18n",paramvalue))
                {
                    paramvalue=rrequest.getI18NStringValue(paramvalue);
                }
                if(paramvalue==null) paramvalue="";
                
                if(sfpbeanTmp.getLstParamValues()!=null&&sfpbeanTmp.getLstParamValues().size()>0)
                {
                    List<String> lstParamTemp=sfpbeanTmp.getLstParamValues();
                    Object[] oParams=new Object[lstParamTemp.size()];
                    for(int j=0;j<lstParamTemp.size();j++)
                    {
                        String temp=lstParamTemp.get(j);
                        if(Tools.isDefineKey("i18n",temp))
                        {
                            temp=rrequest.getI18NStringValue(temp);
                        }
                        oParams[j]=temp;
                    }
                    MessageFormat messformat=new MessageFormat(paramvalue);
                    paramvalue=messformat.format(oParams);
                }
                paramsBuf.append(sfpbeanTmp.getName()).append(":\"").append(paramvalue).append("\",");
            }
        }
        if(paramsBuf.length()==1)
        {
            paramsBuf.deleteCharAt(0);//如果只有{，则说明没有参数
        }else if(paramsBuf.length()>0)
        {
            if(paramsBuf.charAt(paramsBuf.length()-1)==',') paramsBuf.deleteCharAt(paramsBuf.length()-1);
            paramsBuf.append("}");
        }
        return Tools.jsParamEncode(paramsBuf.toString());
    }

    public void writeJsMethodToJsFiles(PageBean pagebean,String jsMethodContent)
    {
        if(jsMethodContent!=null&&!jsMethodContent.trim().equals(""))
        {
            writeJsMethodToJsFiles(pagebean.getJsFilePath(),jsMethodContent);
            pagebean.setShouldIncludeAutoCreatedJs(true);
        }
    }
    
    public void writeJsMethodToJsFiles(String jsPath,String jsMethodContent)
    {
        if(!Config.should_createjs) return;
        if(jsMethodContent==null||jsMethodContent.trim().equals("")) return;
        if(jsPath==null||jsPath.trim().equals("")) return;
        OutputStreamWriter fileWriter=null;
        try
        {
            File f=new File(jsPath);
            if(!f.exists()) f.createNewFile();
            fileWriter=new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f,true)),Config.encode);
            fileWriter.write(jsMethodContent);
        }catch(IOException ioe)
        {
            jsMethodContent=jsMethodContent.trim();
            if(jsMethodContent.length()>100) jsMethodContent=jsMethodContent.substring(0,100)+"...";
            throw new WabacusConfigLoadingException("将JS方法"+jsMethodContent+"写入文件"+jsPath+"失败",ioe);
        }finally
        {
            try
            {
                if(fileWriter!=null) fileWriter.close();
            }catch(IOException e)
            {
                jsMethodContent=jsMethodContent.trim();
                if(jsMethodContent.length()>100) jsMethodContent=jsMethodContent.substring(0,100)+"...";
                throw new WabacusConfigLoadingException("将JS方法"+jsMethodContent+"写入文件"+jsPath+"失败",e);
            }
        }
    }
}
