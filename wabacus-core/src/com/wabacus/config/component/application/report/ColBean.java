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
import java.util.List;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class ColBean extends AbsConfigBean
{
    private String colid;
    
    private String property;

    private String column;

    private String datasetid;
    
    private String label=null;

    private String displaytype=Consts.COL_DISPLAYTYPE_INITIAL;
    
    private String tagcontent;
    
    private boolean isI18n;

    private IDataType datatypeObj;

    private String labelstyleproperty="";//如果当前label列要以某种样式显示，则在这个属性中配置，生成报表时，会将这里配置的内容原封不动的拼凑到当前label的<td...>里面。

    private String valuestyleproperty="";
 
    private String labelalign;
    
    private String valuealign;
    
    private float plainexcelwidth;
    
    private float pdfwidth;
    
    private String printwidth;
    
    private String printlabelstyleproperty;
    
    private String printvaluestyleproperty;
    
    private Method setMethod=null;

    private Method getMethod=null;


//


//    private Map<String,String> mFormatParamsColProperties;//存放当前列的所有格式化方法参数中用到的其它<col/>的定义property(即@{}格式)和真正property
    
    public ColBean(AbsConfigBean parent)
    {
        super(parent);
        this.colid=String.valueOf(((DisplayBean)parent).generate_childid());
    }

    public ColBean(AbsConfigBean parent,int colid)
    {
        super(parent);
        this.colid=String.valueOf(colid);
    }
    
    public String getColid()
    {
        return colid;
    }

    public void setColid(String colid)
    {
        this.colid=colid;
    }

    public String getDatasetid()
    {
        return datasetid;
    }

    public void setDatasetid(String datasetid)
    {
        this.datasetid=datasetid;
    }

    public float getPlainexcelwidth()
    {
        return plainexcelwidth;
    }

    public void setPlainexcelwidth(float plainexcelwidth)
    {
        this.plainexcelwidth=plainexcelwidth;
    }

    public float getPdfwidth()
    {
        return pdfwidth;
    }

    public void setPdfwidth(float pdfwidth)
    {
        this.pdfwidth=pdfwidth;
    }

    public Method getGetMethod()
    {
        return getMethod;
    }

    public void setGetMethod(Method getMethod)
    {
        this.getMethod=getMethod;
    }

    public Method getSetMethod()
    {
        return setMethod;
    }

    public void setSetMethod(Method setMethod)
    {
        this.setMethod=setMethod;
    }

    public boolean isI18n()
    {
        return isI18n;
    }

    public void setI18n(boolean isI18n)
    {
        this.isI18n=isI18n;
    }

    public String getPrintwidth()
    {
        return printwidth;
    }

    public void setPrintwidth(String printwidth)
    {
        this.printwidth=printwidth;
    }

    public String getPrintlabelstyleproperty()
    {
        return printlabelstyleproperty;
    }

    public void setPrintlabelstyleproperty(String printlabelstyleproperty)
    {
        this.printlabelstyleproperty=printlabelstyleproperty;
    }

    public String getPrintvaluestyleproperty()
    {
        return printvaluestyleproperty;
    }

    public void setPrintvaluestyleproperty(String printvaluestyleproperty)
    {
        this.printvaluestyleproperty=printvaluestyleproperty;
    }

    public void setProperty(String property)
    {
        this.property=property;
    }

    public void setColumn(String column)
    {
        if(Tools.isDefineKey("i18n",column))
        {
            String columnTemp=Tools.getRealKeyByDefine("i18n",column);
            if(columnTemp.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"配置的列"
                        +column+"不合法");
            }
            setI18n(true);
            this.column=columnTemp;
        }else
        {
            setI18n(false);
            this.column=column;
        }
    }

    public void setLabel(String label)
    {
        this.label=label;
    }

    public String getDisplaytype()
    {
        return displaytype;
    }

    public void setDisplaytype(String displaytype)
    {
        displaytype=displaytype==null?"":displaytype.toLowerCase().trim();
        if(displaytype.equals(""))
        {
            this.displaytype=Consts.COL_DISPLAYTYPE_INITIAL;
        }else
        {
            if(!displaytype.equals(Consts.COL_DISPLAYTYPE_ALWAYS)&&!displaytype.equals(Consts.COL_DISPLAYTYPE_INITIAL)
                    &&!displaytype.equals(Consts.COL_DISPLAYTYPE_HIDDEN)&&!displaytype.equals(Consts.COL_DISPLAYTYPE_OPTIONAL))
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的列"+this.column+"失败，配置的displaytype属性"+displaytype
                        +"不支持");
            }
            this.displaytype=displaytype;
        }
    }

    public String getProperty()
    {
        return this.property;
    }

    public String getColumn()
    {
        return this.column;
    }

    public String getLabel()
    {
        return this.label;
    }

    public IDataType getDatatypeObj()
    {
        return datatypeObj;
    }

    public void setDatatypeObj(IDataType datatypeObj)
    {
        this.datatypeObj=datatypeObj;
    }

    public String getLabelstyleproperty()
    {
        return labelstyleproperty;
    }

    private String dataexportlabelstyleproperty=null;//导出到word/richexcel中的标题列样式
    
    public String getLabelstyleproperty(ReportRequest rrequest)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return this.printlabelstyleproperty;
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.getReportBean().isListReportType())
        {
            if(dataexportlabelstyleproperty==null)
            {
                dataexportlabelstyleproperty=this.labelstyleproperty==null?"":this.labelstyleproperty.trim();
                String stylevalue=Tools.getPropertyValueByName("style",dataexportlabelstyleproperty,false);
                if(stylevalue==null) stylevalue="";
                if(!stylevalue.trim().equals("")&&!stylevalue.endsWith(";")) stylevalue=stylevalue+";";
                if(stylevalue.toLowerCase().indexOf("text-align")<0)
                {
                    stylevalue=stylevalue+"text-align:center;";
                }
                if(stylevalue.toLowerCase().indexOf("vertical-align")<0)
                {
                    stylevalue=stylevalue+"vertical-align:middle;";
                }
                dataexportlabelstyleproperty=Tools.removePropertyValueByName("style",dataexportlabelstyleproperty);
                dataexportlabelstyleproperty=dataexportlabelstyleproperty+" style=\""+stylevalue+"\"";
            }
            return dataexportlabelstyleproperty;
        }
        return labelstyleproperty;
    }
    
    public void setLabelstyleproperty(String labelstyleproperty)
    {
        this.labelstyleproperty=labelstyleproperty;
    }

    public String getValuestyleproperty()
    {
        return valuestyleproperty;
    }
    
    public String getValuestyleproperty(ReportRequest rrequest)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return this.printvaluestyleproperty;
        return valuestyleproperty;
    }

    public void setValuestyleproperty(String valuestyleproperty)
    {
        this.valuestyleproperty=valuestyleproperty;
    }

    public String getTagcontent()
    {
        return tagcontent;
    }

    public void setTagcontent(String tagcontent)
    {
        this.tagcontent=tagcontent;
    }

    public String getLabelalign()
    {
        return labelalign;
    }

    public void setLabelalign(String labelalign)
    {
        this.labelalign=labelalign;
    }

    public String getValuealign()
    {
        return valuealign;
    }

    public void setValuealign(String valuealign)
    {
        this.valuealign=valuealign;
    }

    public boolean isMatchDataSet(ReportDataSetBean svbean)
    {
        if(this.isControlCol()||this.isSequenceCol()||this.isNonFromDbCol()||this.isNonValueCol()) return false;
        if((this.datasetid==null||this.datasetid.trim().equals(""))&&svbean.isIndependentDataSet()) return true;
        return this.datasetid.equals(svbean.getId());
    }
    
    public boolean checkDisplayPermission(ReportRequest rrequest)
    {
        if(!rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.column,Consts.PERMISSION_TYPE_DISPLAY)) return false;
        if(this.property!=null&&!this.property.trim().equals("")&&!this.property.equals(this.column))
        {
            if(!rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.property,Consts.PERMISSION_TYPE_DISPLAY)) return false;
        }
        return true;
    }
    
    public boolean checkReadonlyPermission(ReportRequest rrequest)
    {
        if(rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.column,Consts.PERMISSION_TYPE_READONLY)) return true;
        if(this.property!=null&&!this.property.trim().equals("")&&!this.property.equals(this.column))
        {
            if(rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.property,Consts.PERMISSION_TYPE_READONLY)) return true;
        }
        return false;
    }
    
    public int getDisplaymode(ReportRequest rrequest,List<String> lstDisplayColIds)
    {
        if(rrequest!=null)
        {
            if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.isControlCol()) return 0;
            if(!checkDisplayPermission(rrequest)) return -1;
        }
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(displaytype)) return 0;
        if(Consts.COL_DISPLAYTYPE_ALWAYS.equals(displaytype)) return 2;
        DisplayBean dbean=(DisplayBean)this.getParent();
        if(!dbean.isColselect()) return 1;
        if(lstDisplayColIds==null||lstDisplayColIds.size()==0)
        {
            if(Consts.COL_DISPLAYTYPE_INITIAL.equals(displaytype)) return 1;
        }else if(lstDisplayColIds.contains(colid))
        {
            return 1;
        }
        return 0;
    }
    
    public boolean isNonValueCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.NON_VALUE);
    }
    
    public boolean isSequenceCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.indexOf("{sequence")==0&&column.indexOf("}")==column.length()-1;
    }
    
    public boolean isNonFromDbCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.NON_FROMDB);
    }
    
    public boolean isRowSelectCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWSELECT);
    }
    
    public boolean isRoworderArrowCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWORDER_ARROW);
    }
    
    public boolean isRoworderInputboxCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWORDER_INPUTBOX);
    }
    
    public boolean isRoworderTopCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWORDER_TOP);
    }
    
    public boolean isRoworderCol(String rowordertypeColumn)
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(rowordertypeColumn);
    }
    
    public boolean isRoworderCol()
    {
        return isRoworderArrowCol()||isRoworderInputboxCol()||isRoworderTopCol();
    }
    
    public boolean isEditableListEditCol()
    {
        if(this.column==null||this.column.trim().equals("")) return false;
        return this.column.equalsIgnoreCase(Consts_Private.COL_EDITABLELIST_EDIT);
    }
    
    public boolean isControlCol()
    {
        if(isRowSelectCol()||isRoworderCol()||isEditableListEditCol())
        {//如果是行选中的列，或行排序列
            return true;
        }
        return false;
    }
    
    public String getBorderStylePropertyOnColBean()
    {
        ReportBean rb=this.getReportBean();
        String border=rb.getBorder();
        String borderstyle="";
        if(Consts_Private.REPORT_BORDER_NONE0.equals(border)||Consts_Private.REPORT_BORDER_NONE1.equals(border))
        {
            borderstyle="border:none;";
        }else
        {
            String bordercolor=rb.getBordercolor();
            if(bordercolor!=null&&!bordercolor.trim().equals(""))
            {
                borderstyle="border-color:"+bordercolor+";";
            }
           if(Consts_Private.REPORT_BORDER_HORIZONTAL0.equals(border)||Consts_Private.REPORT_BORDER_HORIZONTAL1.equals(border))
           {
               borderstyle=borderstyle+"border-left:none;border-right:none;";
           }else if(Consts_Private.REPORT_BORDER_VERTICAL.equals(border))
           {
               borderstyle=borderstyle+"border-top:none;border-bottom:none;";
           }
        }
        return borderstyle;
    }
    
    public String getDisplayValue(Object dataObj,ReportRequest rrequest)
    {
        try
        {
            Object objValue=getRealTypeValue(dataObj,rrequest);
            if(objValue==null) return "";

//            {//没有配置格式化方法
                return this.datatypeObj.value2label(objValue);

           /* Map mParamValues=new HashMap();
            if(mFormatParamsColProperties!=null&&mFormatParamsColProperties.size()>0)
            {
                Iterator<Entry<String,String>> itColids=this.mFormatParamsColProperties.entrySet()
                        .iterator();
                Entry<String,String> entry;
                while(itColids.hasNext())
                {
                    entry=itColids.next();
                    if(entry==null) continue;
                    mParamValues.put(entry.getValue(),WabacusUtil.getPropertyValue(dataObj,entry
                            .getValue()));
                }
            }
            String realvalue=this.formatProperties.get(0).format(objValue,mParamValues,rrequest);
            for(int i=1;i<this.formatProperties.size();i++)
            {
                realvalue=this.formatProperties.get(i).format(realvalue,mParamValues,rrequest);
            }
            return realvalue;*/
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("格式化报表"+this.getReportBean().getPath()+"的字段"
                    +this.column+"时失败",e);
        }
    }

    public Object getRealTypeValue(Object dataObj,ReportRequest rrequest)
    {
        if(this.property==null||this.property.trim().equals("")) return null;
        if(this.isNonValueCol()||this.isSequenceCol()||this.isControlCol()) return null;
        if(dataObj==null) return null;
        Object objValue=null;
        if(!"[DYN_COL_DATA]".equals(this.getProperty()))
        {
            if(this.getMethod==null) return null;
            try
            {
                objValue=this.getMethod.invoke(dataObj,new Object[] {});
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("获取报表"+this.getReportBean().getPath()+"的字段"
                        +this.column+"时失败",e);
            }
        }else
        {
            objValue=ReportAssistant.getInstance().getCrossDynamicColDataFromPOJO(this.getReportBean(),dataObj,this.column);
        }
        return objValue;
    }
    
    public ColBean getUpdateColBeanDest(boolean isMust)
    {
        EditableReportColBean ercbean=(EditableReportColBean)this.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ercbean==null||ercbean.getUpdatecolDest()==null||ercbean.getUpdatecolDest().trim().equals(""))
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>没有配置updatecol更新其它列");
        }
        ColBean cbTemp=this.getReportBean().getDbean().getColBeanByColProperty(ercbean.getUpdatecolDest());
        if(cbTemp==null)
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecolDest()+"引用的列不存在");
        }
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTemp.getDisplaytype()))
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecolDest()+"引用的列不是displaytype为hidden的列");
        }
        if(cbTemp.getProperty()==null||cbTemp.getProperty().trim().equals("")||cbTemp.isNonValueCol()||cbTemp.isSequenceCol()||cbTemp.isControlCol())
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecolDest()+"引用的列不是从数据库中获取数据，不能被引用");
        }
        return cbTemp;
    }
    
    public ColBean getUpdateColBeanSrc(boolean isMust)
    {
        EditableReportColBean ercbean=(EditableReportColBean)this.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ercbean==null||ercbean.getUpdatecolSrc()==null||ercbean.getUpdatecolSrc().trim().equals(""))
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>没有被其它列通过updatecol属性引用");
        }
        ColBean cbTemp=this.getReportBean().getDbean().getColBeanByColProperty(ercbean.getUpdatecolSrc());
        if(cbTemp==null)
        {
            throw new WabacusConfigLoadingException("在报表"+this.getReportBean().getPath()+"中没有取到property为"+ercbean.getUpdatecolSrc()+"的列");
        }
        return cbTemp;
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        ColBean cbNew=(ColBean)super.clone(parent);
        cloneExtendConfig(cbNew);
        return cbNew;
    }

    public void doPostLoad()
    {
        /*if(formatproperty!=null&&!formatproperty.trim().equals(""))
        {
            this.formatProperties=FormatPropertyBean.convertFormatStringToFormatBean(
                    formatproperty,this);
            this.formatproperty=null;
            mFormatParamsColProperties=new HashMap<String,String>();
            if(this.formatProperties!=null&&this.formatProperties.size()>0)
            {
                for(int i=0;i<this.formatProperties.size();i++)
                {
                    FormatPropertyBean fpropbean=this.formatProperties.get(i);
                    if(fpropbean==null) continue;
                    fpropbean.doPostLoad(mFormatParamsColProperties);
                }
            }
            if(mFormatParamsColProperties.size()==0) mFormatParamsColProperties=null;
        }*/
        EditableReportColBean ecolbean=(EditableReportColBean)this.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ecolbean!=null&&ecolbean.getInputbox()!=null)
        {
            ecolbean.getInputbox().doPostLoad(ecolbean);
        }
    }
    
    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((colid==null)?0:colid.hashCode());
        result=prime*result+((column==null)?0:column.hashCode());
        result=prime*result+((property==null)?0:property.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final ColBean other=(ColBean)obj;
        if(colid==null)
        {
            if(other.colid!=null) return false;
        }else if(!colid.equals(other.colid)) return false;
        if(column==null)
        {
            if(other.column!=null) return false;
        }else if(!column.equals(other.column)) return false;
        if(property==null)
        {
            if(other.property!=null) return false;
        }else if(!property.equals(other.property)) return false;
        if(this.getReportBean()==null)
        {
            if(other.getReportBean()!=null) return false;
        }else if(!this.getReportBean().equals(other.getReportBean()))
        {
            return false;
        }
        return true;
    }
}
