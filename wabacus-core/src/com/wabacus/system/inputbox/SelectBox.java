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
package com.wabacus.system.inputbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.EditableListFormReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.util.Tools;

public class SelectBox extends AbsInputBox implements Cloneable
{
    private Map<String,Boolean> mParentids;

    private Map<String,AbsInputBox> mParentInputboxes;
    
    private boolean isRegex=false;

    private boolean isMultiply;
    
    private String separator;//如果允许多选，则存放每个值之间的分隔符，默认为空格
    
    private List<OptionBean> lstOptions=null;

    public SelectBox(String typename)
    {
        super(typename);
    }

    public Map<String,Boolean> getMParentids()
    {
        return mParentids;
    }

    public String getAllParentIdsAsString()
    {
        if(mParentids==null||mParentids.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        boolean isConditionBox=this.owner instanceof ConditionBean;
        ConditionBean cbTmp;
        for(String parentidTmp:this.mParentids.keySet())
        {
            if(isConditionBox)
            {
                cbTmp=this.owner.getReportBean().getSbean().getConditionBeanByName(parentidTmp);
                if(cbTmp==null||cbTmp.isHidden()||cbTmp.isConstant()||!cbTmp.isConditionValueFromUrl()||cbTmp.getInputbox()==null) continue;
            }
            resultBuf.append(parentidTmp).append(";");
        }
        if(resultBuf.charAt(resultBuf.length()-1)==';') resultBuf.deleteCharAt(resultBuf.length()-1);
        return resultBuf.toString();
    }
    
    public void setMParentids(Map<String,Boolean> parentids)
    {
        mParentids=parentids;
    }

    public boolean isRegex()
    {
        return isRegex;
    }

    public void setRegex(boolean isRegex)
    {
        this.isRegex=isRegex;
    }

    public void setLstOptions(List<OptionBean> lstOptions)
    {
        this.lstOptions=lstOptions;
    }

    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(isReadonly) style_property=addReadonlyToStyleProperty2(style_property);
        String realinputboxid=getInputBoxId(rrequest);
        resultBuf.append("<select typename='"+typename+"' name='"+realinputboxid+"' id='"+realinputboxid+"'");
        if(style_property!=null) resultBuf.append(" ").append(style_property);
        resultBuf.append(">");
        value=getInputBoxValue(rrequest,value);
        List<String[]> lstOptionsResult=getLstOptionsFromCache(rrequest,realinputboxid);
        if(lstOptionsResult!=null)
        {
            String name_temp,value_temp,selected;
            for(String[] items:lstOptionsResult)
            {
                name_temp=items[0];
                value_temp=items[1];
                value_temp=value_temp==null?"":value_temp.trim();
                if(!this.isMultiply)
                {
                    selected=value_temp.equals(value)?"selected":"";
                }else
                {//是多选下拉框
                    selected=SelectedBoxAssistant.getInstance().isSelectedValueOfMultiSelectBox(value,value_temp,this.separator)?"selected":"";
                }
                resultBuf.append("<option value='"+value_temp+"' "+selected+">"+name_temp+"</option>");
            }
        }
        resultBuf.append("</select>");
        resultBuf.append(this.getDescription(rrequest));
        return resultBuf.toString();
    }

    public List<String[]> getLstOptionsFromCache(ReportRequest rrequest,String realinputboxid)
    {
        ReportBean rbean=this.owner.getReportBean();
        List<String[]> lstOptionsResult=null;
        if(this.mParentids==null||this.mParentids.size()==0)
        {
            lstOptionsResult=(List<String[]>)rrequest.getAttribute("LISTOPTIONS_"+rbean.getId()+owner.getInputBoxId());
        }else
        {
            lstOptionsResult=(List<String[]>)rrequest.getAttribute("LISTOPTIONS_"+rbean.getId()+realinputboxid);
        }
        if(lstOptionsResult!=null) return lstOptionsResult;//从缓存中取到了
        if(this.mParentids==null||this.mParentids.size()==0)
        {
            lstOptionsResult=getOptionsList(rrequest,null);
            if(lstOptionsResult!=null)
            {
                rrequest.setAttribute("LISTOPTIONS_"+rbean.getId()+owner.getInputBoxId(),lstOptionsResult);
            }
            return lstOptionsResult;
        }
        
        lstOptionsResult=getOptionsList(rrequest,getAllParentValues(rrequest,realinputboxid));
        if(this.getLstChildids()!=null&&this.getLstChildids().size()>0)
        {
            rrequest.setAttribute("LISTOPTIONS_"+rbean.getId()+realinputboxid,lstOptionsResult);
        }
        return lstOptionsResult;
    }
    
    public Map<String,String> getAllParentValues(ReportRequest rrequest,String realinputboxid)
    {
        boolean isConditionBox=this.owner instanceof ConditionBean;
        if(this.mParentInputboxes==null)
        {
            if(isConditionBox)
            {//查询条件
                this.mParentInputboxes=new HashMap<String,AbsInputBox>();
                ConditionBean cbTmp;
                for(String connameTmp:this.mParentids.keySet())
                {
                    cbTmp=this.owner.getReportBean().getSbean().getConditionBeanByName(connameTmp);
                    if(cbTmp==null||cbTmp.isHidden()||cbTmp.isConstant()||!cbTmp.isConditionValueFromUrl()||cbTmp.getInputbox()==null) continue;
                    this.mParentInputboxes.put(connameTmp,cbTmp.getInputbox());
                }
            }else
            {
                this.mParentInputboxes=new HashMap<String,AbsInputBox>();
                ColBean cbTmp;
                EditableReportColBean ercbTmp;
                for(String colpropertyTmp:this.mParentids.keySet())
                {
                    cbTmp=this.owner.getReportBean().getDbean().getColBeanByColProperty(colpropertyTmp);
                    if(cbTmp==null) continue;
                    ercbTmp=(EditableReportColBean)cbTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
                    if(ercbTmp==null||ercbTmp.getInputbox()==null) continue;
                    this.mParentInputboxes.put(colpropertyTmp,ercbTmp.getInputbox());
                }
            }
        }
        int rowidx=-1;
        int idx=realinputboxid.lastIndexOf("__");
        if(idx>0)
        {
            try
            {
                rowidx=Integer.parseInt(realinputboxid.substring(idx+2).trim());
            }catch(NumberFormatException e)
            {
                rowidx=-1;
            }
        }
        Map<String,String> mResults=new HashMap<String,String>();
        AbsInputBox parentBoxTmp;
        String parentValue;
        for(String parentidTmp:this.mParentids.keySet())
        {
            parentBoxTmp=this.mParentInputboxes.get(parentidTmp);
            if(parentBoxTmp!=null)
            {
                parentValue=parentBoxTmp.getOwner().getInputBoxValue(rrequest,rowidx);
                parentValue=parentValue==null?"":parentValue.trim();
                parentValue=parentBoxTmp.getInputBoxValue(rrequest,parentValue);//父输入框可能配置了defaultvalue，所以在这里再让父下拉框根据parentValue决定一下自己的值
                if((parentValue==null||parentValue.trim().equals(""))&&parentBoxTmp instanceof SelectBox)
                {
                    String parentRealInputboxId=parentBoxTmp.getOwner().getInputBoxId();
                    if(idx>0)
                    {
                        parentRealInputboxId=parentRealInputboxId+realinputboxid.substring(idx);
                    }
                    List<String[]> lstParentOptions=((SelectBox)parentBoxTmp).getLstOptionsFromCache(rrequest,parentRealInputboxId);
                    if(lstParentOptions!=null&&lstParentOptions.size()>0&&!hasBlankValueOption(lstParentOptions))
                    {
                        parentValue= lstParentOptions.get(0)[1];
                    }
                }
            }else
            {
                if(isConditionBox)
                {
                    ConditionBean cbTmp=this.getOwner().getReportBean().getSbean().getConditionBeanByName(parentidTmp);
                    parentValue=cbTmp.getConditionValue(rrequest,-1);
                }else
                {
                    ColBean cbTmp=this.getOwner().getReportBean().getDbean().getColBeanByColProperty(parentidTmp);
                    AbsReportType reportTypeObj=rrequest.getDisplayReportTypeObj(this.owner.getReportBean().getId());
                    if(reportTypeObj.getLstReportData()==null||reportTypeObj.getLstReportData().size()==0||rowidx>=reportTypeObj.getLstReportData().size())
                    {
                        parentValue=""; 
                    }else
                    {
                        parentValue=cbTmp.getDisplayValue(reportTypeObj.getLstReportData().get(rowidx),rrequest);
                    }
                }
            }
            mResults.put(parentidTmp,parentValue);
        }
        return mResults;
    }
    
    public boolean hasBlankValueOption(List<String[]> lstParentOptions)
    {
        if(lstParentOptions==null||lstParentOptions.size()==0) return false;
        for(String[] optionArrTmp:lstParentOptions)
        {
            if(optionArrTmp[1]==null||optionArrTmp[1].trim().equals("")) return true;
        }
        return false;
    }
    
    
    public List<String[]> getOptionsList(ReportRequest rrequest,Map<String,String> mParentValues)
    {
        List<String[]> lstResults=new ArrayList<String[]>();
        ReportBean rbean=this.owner.getReportBean();
        String name_temp,value_temp;
        String[] optionItemsTmp;
        for(OptionBean obean:lstOptions)
        {
            if(obean.getSourceType()==1&&obean.getSql()!=null)
            {
                String sql=obean.getSql();
                if(mParentValues!=null&&mParentValues.size()>0)
                {//说明数据依赖了其它下拉框
                    String parentNameTmp, parentValTmp;
                    for(Entry<String,String> entryValueTmp:mParentValues.entrySet())
                    {
                        parentNameTmp=entryValueTmp.getKey();
                        parentValTmp=entryValueTmp.getValue();
                        parentValTmp=Tools.removeSQLKeyword(parentValTmp);
                        if(parentValTmp.equals(""))
                        {
                            sql=Tools.replaceAll(sql,"#["+parentNameTmp+"]#","");
                            sql=Tools.replaceAll(sql,"#"+parentNameTmp+"#","#_#-#_@-@_@-!_!-!");
                        }else if(parentValTmp.equals("[%ALL%]"))
                        {
                            sql=Tools.replaceAll(sql,"#["+parentNameTmp+"]#","");
                            sql=Tools.replaceAll(sql,"#"+parentNameTmp+"#","");
                        }else
                        {
                            sql=Tools.replaceAll(sql,"#["+parentNameTmp+"]#",parentValTmp);
                            sql=Tools.replaceAll(sql,"#"+parentNameTmp+"#",parentValTmp);
                        }
                    }
                }
                lstResults.addAll(ReportAssistant.getInstance().getOptionListFromDB(rrequest,rbean,sql,obean));
            }else
            {
                name_temp=rrequest.getI18NStringValue(obean.getLabel());
                value_temp=obean.getValue();
                if(mParentValues!=null&&mParentValues.size()>0&&obean.getType()!=null&&obean.getType().length>0)
                {
                    if(obean.getType().length==1&&(obean.getType()[0].equals("%false-false%")||obean.getType()[0].equals("%true-true%")))
                    {//当前下拉选项是在下拉框没有选项数据或有选项数据时才显示出来（这两种是特殊的选项，不在这里判断，而是在下面判断）
                        continue;
                    }
                    if(mParentValues.size()>1)
                    {
                        throw new WabacusRuntimeException("显示报表"+this.getOwner().getReportBean().getPath()+"的下拉框"+this.getOwner().getInputBoxId()
                                +"失败，此下拉框不是从数据库中获取选项数据，不能同时依赖多个父下拉框");
                    }
                    String parentVal=mParentValues.entrySet().iterator().next().getValue();
                    if(!obean.isMatch(parentVal,this.isRegex)&&!parentVal.equals("[%ALL%]")) continue;
                }
                optionItemsTmp=new String[2];
                optionItemsTmp[0]=name_temp;
                optionItemsTmp[1]=value_temp;
                lstResults.add(optionItemsTmp);
            }
        }
        if(lstResults.size()==0)
        {
            for(OptionBean obean:lstOptions)
            {
                if(obean.getSourceType()==1||obean.getType()==null) continue;
                if(obean.getType().length==1&&obean.getType()[0].equals("%false-false%"))
                {
                    optionItemsTmp=new String[2];
                    optionItemsTmp[0]=rrequest.getI18NStringValue(obean.getLabel());
                    optionItemsTmp[1]=obean.getValue();
                    lstResults.add(optionItemsTmp);
                }
            }
        }else
        {
            for(OptionBean obean:lstOptions)
            {
                if(obean.getSourceType()==1) continue;
                if(obean.getType()!=null&&obean.getType().length==1&&obean.getType()[0].equals("%true-true%"))
                {//当前下拉选项是在下拉框有选项数据时才显示出来
                    optionItemsTmp=new String[2];
                    optionItemsTmp[0]=rrequest.getI18NStringValue(obean.getLabel());
                    optionItemsTmp[1]=obean.getValue();
                    lstResults.add(0,optionItemsTmp);
                }
            }
        }
        return lstResults;
    }
    
    public String getDefaultlabel(ReportRequest rrequest)
    {
        if(this.defaultvalue==null) return null;
        List<String[]> lstOptionsResult=null;
        ReportBean rbean=owner.getReportBean();
        if(this.mParentids==null||this.mParentids.size()==0)
        {
            lstOptionsResult=getLstOptionsFromCache(rrequest,this.owner.getInputBoxId());
        }else
        {
            lstOptionsResult=(List<String[]>)rrequest.getAttribute("LISTOPTIONS_"+rbean.getId()+owner.getInputBoxId()+"_[ALL]");
            if(lstOptionsResult==null)
            {
                Map<String,String> mParentValues=new HashMap<String,String>();
                for(String parentidTmp:this.mParentids.keySet())
                {
                    mParentValues.put(parentidTmp,"[%ALL%]");
                }
                lstOptionsResult=getOptionsList(rrequest,mParentValues);
                if(lstOptionsResult!=null)
                {
                    rrequest.setAttribute("LISTOPTIONS_"+rbean.getId()+owner.getInputBoxId()+"_[ALL]",lstOptionsResult);
                }
            }
        }
        if(!this.isMultiply)
        {
            return SelectedBoxAssistant.getInstance().getSelectedLabelByValuesOfSingleSelectedBox(lstOptionsResult,this.getDefaultvalue(rrequest));
        }else
        {
            return SelectedBoxAssistant.getInstance().getSelectedLabelByValuesOfMultiSelectedBox(lstOptionsResult,this.getDefaultvalue(rrequest),
                    this.separator);
        }
    }
    
    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        if(!this.isMultiply) return super.initDisplaySpanStart(rrequest);
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        resultBuf.append(" separator=\"").append(this.separator).append("\"");
        return resultBuf.toString();
    }
    
    protected String initDisplaySpanContent(ReportRequest rrequest)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.initDisplaySpanContent(rrequest));
        List<String[]> lstOptionsResult=null;
        if(this.mParentids==null||this.mParentids.size()==0)
        {//如果当前下拉框不是依赖其它下拉框
            lstOptionsResult=getLstOptionsFromCache(rrequest,this.owner.getInputBoxId());
        }else
        {
            AbsReportType reportTypeObj=Config.getInstance().getReportType(this.owner.getReportBean().getType());
            if(reportTypeObj instanceof EditableListFormReportType)
            {
                lstOptionsResult=getLstOptionsFromCache(rrequest,this.owner.getInputBoxId()+"__"+Integer.MAX_VALUE);
            }
            //对于其它报表类型，比如editablelist2/editabledetail2等等在页面中准备下拉选项没有意义，因为它们是在点击后显示下拉框，并且此时会根据父下拉框的值从服务器端重新加载一次下拉选项列表
        }
        if(lstOptionsResult==null||lstOptionsResult.size()==0) return "";
        String name_temp,value_temp;
        for(String[] items:lstOptionsResult)
        {
            name_temp=items[0];
            value_temp=items[1];
            value_temp=value_temp==null?"":value_temp.trim();
            resultBuf.append("<span value=\""+value_temp+"\" label=\""+name_temp+"\"></span>");
        }
        return resultBuf.toString();
    }

    public String filledInContainer(String onblur)
    {
        StringBuffer resultBuf=new StringBuffer();

//        if(Config.showreport_url.indexOf("?")>0) token="&";




        resultBuf.append("var onchangeEvent='';if(inputboxSpanObj!=null){onchangeEvent=inputboxSpanObj.getAttribute('onchange_propertyvalue');}");
        resultBuf.append("var boxstr=\"<select \";").append(getInputBoxCommonFilledProperties());
        resultBuf.append("boxstr=boxstr+\" onblur=\\\"try{\"+onblurmethod+\"").append(onblur).append("}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";");
//        resultBuf.append("if(childid!=null&&childid!=''){");//被别的下拉框依赖
//        resultBuf.append("  var myonchange='';");
//        resultBuf.append("  if(fillmode==1){");//显示时填充

//        resultBuf.append("      if(name.lastIndexOf('__')>0){realchildid=realchildid+name.substring(name.lastIndexOf('__'));}");//是可编辑列表报表的下拉框
//        resultBuf.append("      myonchange=\"reloadSelectBoxData('").append(serverurl).append("','\"+realchildid+\"',this);\";");//对于显示单元格时就显示下拉框的方式，改变父下拉框选中数据时就刷新依赖它的子下拉框的选项数据
//        resultBuf.append("  }else{");//点击时填充
//        resultBuf.append("      myonchange=\"resetChildSelectBoxData(this);\";");//对于点击单元格时显示下拉框的方式，改变父下拉框选中数据后要更新其子下拉框所在<td/>的值



        resultBuf.append("if(onchangeEvent!=null&&onchangeEvent!=''){boxstr=boxstr+\" onchange=\\\"try{\"+onchangeEvent+\"}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";}");//加上onchange事件
        
//        resultBuf.append("if(parentid!=null&&parentid!=''){");//依赖别的下拉框
//        resultBuf.append("  if(fillmode==2){");//如果是点击时填充输入框，给它加上onfocus事件
//        resultBuf.append("      var realParentSelectBoxTdId=parentid+'__td';var originalChildid=name;");//realParentSelectBoxTdId存放的是父下拉框所在<td/>的真正id值，originalChildid是本下拉框的id，且不带rowindex
//        resultBuf.append("      if(name.lastIndexOf('__')>0){");//可编辑数据自动列表报表
//        resultBuf.append("          realParentSelectBoxTdId=realParentSelectBoxTdId+name.substring(name.lastIndexOf('__')+2);")//取到父下拉框所在<td/>的id，因为取父下拉框数据时，是从其所在<td/>中获取
//                .append("           originalChildid=name.substring(0,name.lastIndexOf('__'));");//这个要去掉行号

//        resultBuf.append("      var parentValue=''; var parentSelectBoxTdObj=document.getElementById(realParentSelectBoxTdId);");//取到父下拉框所属<td/>对象
//        resultBuf.append("      if(parentSelectBoxTdObj!=null) parentValue=getEditable2ColValueByParentTd(parentSelectBoxTdObj);if(parentValue==null) parentValue='';");//从父下拉框所属的<td/>中获取下拉框的值



//        resultBuf.append("      onfocusmethod=onfocusmethod+\"reloadSelectBoxData('"+serverurl+"','\"+name+\"',null);\";");// *对于点击单元格填充下拉框的方式，如果当前下拉框数据依赖别的下拉框，则是点击所在单元格显示当前下拉框时根据父下拉框的选中数据从服务器端获取。
//        resultBuf.append("  }");//end if fillmode==2
//        resultBuf.append("  boxstr=boxstr+\" SELECTEDVALUE=\\\"\"+boxValue+\"\\\"\";");//分配一个SELECTEDVALUE属性指向为当前下拉框选中值，以便如果此下拉框依赖别的下拉框数据时，能在刷新本下拉框数据时保持之前的选中值选中。
//        resultBuf.append("}");
        resultBuf.append("  if(onfocusmethod!=null&&onfocusmethod!='') boxstr=boxstr+\" onfocus=\\\"try{\"+onfocusmethod+\"}catch(e){logErrorsAsJsFileLoad(e);}\\\"\";");
        resultBuf.append("  boxstr=boxstr+\">\";");
        
        resultBuf.append("  if(inputboxSpanObj!=null){");
        resultBuf.append("      var separator=inputboxSpanObj.getAttribute('separator');if(separator==null) separator='';");
        resultBuf.append("      var optionSpans=inputboxSpanObj.getElementsByTagName(\"span\");");
        resultBuf.append("      if(optionSpans!=null&&optionSpans.length>0){ ");
//        resultBuf.append("          if(tdObj.getAttribute('value')==null){");//如果填充当前下拉框的单元格是新建的，还没有设置value属性，则先将下拉框第一个选项值设置给它，以免保存时，下拉框默认选中了第一个值，而从它的<td/>中取不到值。


        resultBuf.append("          var optionlabel=null;var optionvalue=null;");
        resultBuf.append("          for(var i=0,len=optionSpans.length;i<len;i++){");
        resultBuf.append("              optionlabel=optionSpans[i].getAttribute('label'); optionvalue=optionSpans[i].getAttribute('value');");
        resultBuf.append("              boxstr=boxstr+\"<option value='\"+optionvalue+\"'\";");
        resultBuf.append("              if((separator==''&&boxValue==optionvalue)||(separator!=''&&isSelectedValueForMultiSelectedBox(boxValue,optionvalue,separator))) boxstr=boxstr+\" selected\";");
        resultBuf.append("              boxstr=boxstr+\">\"+optionlabel+\"</option>\";");
        resultBuf.append("          }");
        resultBuf.append("      }");
        resultBuf.append("  }");
        resultBuf.append("boxstr=boxstr+\"</select>\";");
        return resultBuf.toString();
    }

//    public String getDefaultvalue(ReportRequest rrequest)


//        {//如果是点击时再填充


//        //下面处理显示时就直接填充的情况，此时如果没有配置默认值，则以第一个下拉选项做为默认值
//        


    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        List<String[]> lstOptionsResult=(List<String[]>)specificDataObj;
        StringBuffer resultBuf=new StringBuffer();
        dynstyleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,dynstyleproperty,1);
        if(isReadonly) dynstyleproperty=addReadonlyToStyleProperty1(dynstyleproperty);
        resultBuf.append("<select ").append(dynstyleproperty).append(">");
        if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
        {
            String name_temp,value_temp,selected;
            for(String[] items:lstOptionsResult)
            {
                name_temp=items[0];
                value_temp=items[1];
                value_temp=value_temp==null?"":value_temp.trim();
                selected=value_temp.equals(value)?"selected":"";
                resultBuf.append("<option value='"+value_temp+"' "+selected+">"+name_temp+"</option>");
            }
        }
        resultBuf.append("</select>");
        return resultBuf.toString();
    }
    
    public String createGetValueByInputBoxObjJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("if(boxObj.options.length==0){value='';label='';return;}");
        resultBuf.append("var separator=boxObj.getAttribute('separator');");
        resultBuf.append("if(separator==null||separator==''){");
        resultBuf.append("  value=boxObj.options[boxObj.options.selectedIndex].value;");
        resultBuf.append("  label=boxObj.options[boxObj.options.selectedIndex].text;");
        resultBuf.append("}else{");
        resultBuf.append("  value='';label='';");
        resultBuf.append("  for(var i=0,len=boxObj.options.length;i<len;i++){");
        resultBuf.append("      if(boxObj.options[i].selected){");
        resultBuf.append("          value=value+boxObj.options[i].value+separator;");
        resultBuf.append("          label=label+boxObj.options[i].text+separator;");
        resultBuf.append("      }");
        resultBuf.append("  }");
        resultBuf.append("  value=wx_rtrim(value,separator);label=wx_rtrim(label,separator);");
        resultBuf.append("}");
        return resultBuf.toString();
    }

    public String createGetValueByIdJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("var selectboxObj=document.getElementById(id);");
        resultBuf.append("if(selectboxObj==null) return null;");
        resultBuf.append("if(selectboxObj.options.length==0) return '';");
        resultBuf.append("var separator=selectboxObj.getAttribute('separator');");
        resultBuf.append("if(separator==null||separator=='') return selectboxObj.options[selectboxObj.options.selectedIndex].value;");//如果不是复选的下拉框
        resultBuf.append("var resultVal='';");
        resultBuf.append("for(var i=0,len=selectboxObj.options.length;i<len;i++){");
        resultBuf.append("  if(selectboxObj.options[i].selected){resultVal=resultVal+selectboxObj.options[i].value+separator;}");
        resultBuf.append("}");
        resultBuf.append("return wx_rtrim(resultVal,separator);");
        return resultBuf.toString();
    }

    public String createSetInputBoxValueByIdJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("var selectboxObj=document.getElementById(id);");
        resultBuf.append("if(selectboxObj==null||selectboxObj.options.length==0){return;}");
        resultBuf.append("var separator=selectboxObj.getAttribute('separator');");
        resultBuf.append("if(separator!=null&&separator!=''){");
        resultBuf.append("  for(var j=0,len=selectboxObj.options.length;j<len;j++){");
        resultBuf.append("      if(selectboxObj.options[j].selected&&selectboxObj.options[j].value==newvalue){return;}");
        resultBuf.append("  }");
        resultBuf.append("}else{");
        resultBuf.append("  var oldvalue=selectboxObj.options[selectboxObj.selectedIndex].value;");
        resultBuf.append("  if(oldvalue&&oldvalue==newvalue){return;}");//新旧值相等，则不用设置
        resultBuf.append("}");
        resultBuf.append("var i=0;");
        resultBuf.append("for(len=selectboxObj.options.length;i<len;i=i+1){");
        resultBuf.append("  if(selectboxObj.options[i].value==newvalue){selectboxObj.options[i].selected=true;break;}");
        resultBuf.append("}");
        resultBuf.append("if(i!=selectboxObj.options.length&&selectboxObj.onchange){selectboxObj.onchange();}");
        return resultBuf.toString();
    }
    
    public void loadInputBoxConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(ownerbean,eleInputboxBean);
        if(eleInputboxBean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+ownerbean.getReportBean().getPath()+"的下拉框类型输入框失败，没有配置下拉选项");
        }
        
        List<OptionBean> lstObs=new ArrayList<OptionBean>();
        List<XmlElementBean> lstOptionElements=eleInputboxBean.getLstChildElementsByName("option");
        if(lstOptionElements!=null&&lstOptionElements.size()>0)
        {
            lstObs=ComponentConfigLoadManager.loadOptionInfo(lstOptionElements,ownerbean);
        }
        if(lstObs==null||lstObs.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+ownerbean.getReportBean().getPath()+"配置的下拉框类型的输入框失败，没有配置下拉选项");
        }
        this.setLstOptions(lstObs);
        String multiply=eleInputboxBean.attributeValue("multiply");
        this.isMultiply=multiply!=null&&multiply.toLowerCase().trim().equals("true");
        if(this.isMultiply)
        {
            this.separator=eleInputboxBean.attributeValue("separator");
            if(this.separator==null||this.separator.equals("")) this.separator=" ";
        }

        
        String depends=eleInputboxBean.attributeValue("depends");
        if(depends!=null&&!depends.trim().equals(""))
        {
            String isregex=eleInputboxBean.attributeValue("isregrex");
            if(isregex!=null&&!isregex.trim().equals(""))
            {
                this.isRegex=Boolean.parseBoolean(isregex.trim());
            }
            List<String> lstParentidsTmp=Tools.parseStringToList(depends,";",false);
            this.mParentids=new HashMap<String,Boolean>();
            String dependtypeTmp=null;
            for(String parentidTmp:lstParentidsTmp)
            {
                if(parentidTmp==null||parentidTmp.trim().equals("")) continue;
                dependtypeTmp=null;
                int idx=parentidTmp.indexOf("=");
                if(idx>0)
                {
                    dependtypeTmp=parentidTmp.substring(idx+1).trim();
                    parentidTmp=parentidTmp.substring(0,idx).trim();
                }
                this.mParentids.put(parentidTmp,dependtypeTmp==null||!dependtypeTmp.trim().toLowerCase().equals("false"));
                ownerbean.getReportBean().addSelectBoxWithRelate(this);
            }
        }
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "onkeypress='return onKeyEvent(event);'  class='cls-inputbox2-selectbox'";
    }
    
    public void processRelateInputboxByReportBean()
    {
        if(this.mParentids==null||this.mParentids.size()==0) return;
        boolean isConditionBox=this.owner instanceof ConditionBean;
        String parentidTmp;
        AbsInputBox parentBoxTmp;
        for(Entry<String,Boolean> entryTmp:this.mParentids.entrySet())
        {
            if(entryTmp.getValue()==Boolean.FALSE) continue;//父列（条件）没有输入框，或者父输入框数据有变化时不需实时刷新此子下拉框的选项数据
            parentidTmp=entryTmp.getKey();
            if(isConditionBox)
            {//当前下拉框是查询条件输入框，则parentidTmp对应的是<conditon/>的name属性
                ConditionBean cbTmp=this.owner.getReportBean().getSbean().getConditionBeanByName(parentidTmp);
                if(cbTmp==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"的下拉框"+this.owner.getInputBoxId()+"失败，其依赖的"
                            +parentidTmp+"对应的父输入框不存在");
                }
                if(cbTmp.isHidden()||cbTmp.isConstant())
                {
                    this.mParentids.put(parentidTmp,Boolean.FALSE);
                    continue;
                }
                parentBoxTmp=cbTmp.getInputbox();
            }else
            {//是编辑列的输入框，则parentidTmp对应的是<col/>的property属性
                ColBean cbTmp=this.owner.getReportBean().getDbean().getColBeanByColProperty(parentidTmp);
                if(cbTmp==null||cbTmp.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"的下拉框"+this.owner.getInputBoxId()+"失败，其依赖的"
                            +parentidTmp+"对应的父输入框不存在");
                }
                EditableReportColBean ercbTmp=(EditableReportColBean)cbTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
                if(ercbTmp==null)
                {
                    this.mParentids.put(parentidTmp,Boolean.FALSE);
                    continue;
                }
                if(ercbTmp.getUpdatedcol()!=null&&!ercbTmp.getUpdatedcol().trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"的下拉框"+this.owner.getInputBoxId()+"失败，其依赖的"
                            +parentidTmp+"对应的列是被其它列通过updatecol属性进行更新，因此需要指定为更新此列的列");
                }
                parentBoxTmp=ercbTmp.getInputbox();
            }
            parentBoxTmp.addChildInputboxId(this.getOwner().getInputBoxId());
        }
    }
    
    public void doPostLoad(IInputBoxOwnerBean ownerbean)
    {
        super.doPostLoad(ownerbean);
        boolean isPreparedStmt=false;
        if(ownerbean.getReportBean().getSbean().getStatementType()==SqlBean.STMTYPE_PREPAREDSTATEMENT)
        {
            isPreparedStmt=true;
        }
        List<ConditionBean> lstCons;
        for(OptionBean obTmp:this.lstOptions)
        {
            if(obTmp.getSourceType()!=1) continue; 
            lstCons=obTmp.getLstConditions();
            if(lstCons==null||lstCons.size()==0) continue;
            for(ConditionBean cbTmp:lstCons)
            {
                if(isPreparedStmt) cbTmp.getConditionExpression().parseConditionExpression();
//                if(cbTmp.isConditionValueFromUrl())


//                    {//当前下拉框是依赖别的下拉框数据的从下拉框，如果要配置动态查询条件，只能配置为从session中取条件数据


//                    }


            }
        }
    }

    protected void processRelativeInputBoxes()
    {
        super.processRelativeInputBoxes();
        if(this.displaymode==2)
        {//editablelist2/editabledetail2报表类型的编辑框
            ReportBean rbean=this.owner.getReportBean();
            if(this.mParentids!=null&&this.mParentids.size()>0)
            {
                String event="onFocus=\"reloadSelectBoxDataByFocus('"+rbean.getPageBean().getId()+"','"+rbean.getId()+"',this,'"+this.getAllParentIdsAsString()
                        +"')\"";
                this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,event,1);
            }
        }
    }

    protected String getRefreshChildboxDataEventName()
    {
        return "onchange";
    }

    protected void processStylePropertyAfterMerged(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        super.processStylePropertyAfterMerged(reportTypeObj,ownerbean);
        if(this.isMultiply) 
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"separator=\""+this.separator+"\"",1);
            this.styleproperty=this.styleproperty+" multiple ";
        }
    }

    protected void processStylePropertyForFillInContainer()
    {
       super.processStylePropertyForFillInContainer(); 
       String onchange=Tools.getPropertyValueByName("onchange",this.styleproperty,false);
       if(onchange!=null&&!onchange.trim().equals(""))
       {
           this.mStyleProperties2.put("onchange",onchange);
       }
       this.styleproperty2=Tools.removePropertyValueByName("onchange",this.styleproperty2);
    }

    public Object clone(IInputBoxOwnerBean owner)
    {
        SelectBox boxObjNew=(SelectBox)super.clone(owner);
        if(lstOptions!=null)
        {
            List<OptionBean> lstOptionsNew=new ArrayList<OptionBean>();
            for(OptionBean obTmp:lstOptions)
            {
                lstOptionsNew.add((OptionBean)obTmp.clone());
            }
            boxObjNew.setLstOptions(lstOptionsNew);
        }
        if(mParentids!=null&&mParentids.size()>0)
        {
            boxObjNew.setMParentids((Map<String,Boolean>)((HashMap<String,Boolean>)mParentids).clone());
            if(owner!=null&&owner.getReportBean()!=null)
            {
                owner.getReportBean().addSelectBoxWithRelate(boxObjNew);
            }
        }
        return boxObjNew;
    }
}
