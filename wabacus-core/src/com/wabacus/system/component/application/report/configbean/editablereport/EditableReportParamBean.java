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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.text.MessageFormat;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;

public class EditableReportParamBean
{
    private String paramname;

    private String defaultvalue;

    private boolean hasLeftPercent;

    private boolean hasRightPercent;

    private Object owner;

    private String[][] servervalidate=null;

    public String getParamname()
    {
        return paramname;
    }

    public void setParamname(String paramname)
    {
        this.paramname=paramname;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public boolean isHasLeftPercent()
    {
        return hasLeftPercent;
    }

    public void setHasLeftPercent(boolean hasLeftPercent)
    {
        this.hasLeftPercent=hasLeftPercent;
    }

    public boolean isHasRightPercent()
    {
        return hasRightPercent;
    }

    public void setHasRightPercent(boolean hasRightPercent)
    {
        this.hasRightPercent=hasRightPercent;
    }

    public void setServervalidate(String[][] servervalidate)
    {
        this.servervalidate=servervalidate;
    }

    public Object getOwner()
    {
        return owner;
    }

    public void setOwner(Object owner)
    {
        this.owner=owner;
    }

    public ColBean getColbeanOwner()
    {
        if(!(this.owner instanceof ColBean))
        {
            return null;
        }
        return (ColBean)this.owner;
    }

    public IDataType getDataTypeObj()
    {
        IDataType typeObj=null;
        if(this.owner instanceof EditableReportExternalValueBean)
        {
            typeObj=((EditableReportExternalValueBean)this.owner).getTypeObj();
        }else if(this.owner instanceof ColBean)
        {
            typeObj=((ColBean)this.owner).getDatatypeObj();
        }
        if(typeObj==null)
        {
            typeObj=new VarcharType();
        }
        return typeObj;
    }

    public String getParamValue(String value,ReportRequest rrequest,ReportBean rbean)
    {
        if(value==null||value.trim().equals(""))
        {
            value=ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
        }
        if(this.servervalidate!=null&&this.servervalidate.length>0)
        {
            if(value==null||value.trim().equals(""))
            {
                int len=servervalidate.length;
                for(int j=0;j<len;j++)
                {
                    if(servervalidate[j][0]==null)
                    {
                        continue;
                    }
                    if(servervalidate[j][0].trim().toLowerCase().equals("isnotempty"))
                    {
                        String label="";
                        if(owner instanceof ColBean)
                        {
                            label=((ColBean)owner).getLabel();
                            label=label==null?"":label.trim();
                        }
                        Object[] oParams= { label };
                        if(servervalidate[j][1]==null||servervalidate[j][1].trim().equals(""))
                        {
                            servervalidate[j][1]="{0} can't be empty";
                        }
                        String errorprompt=servervalidate[j][1];
                        errorprompt=rrequest.getI18NStringValue(errorprompt);
                        MessageFormat messformat=new MessageFormat(errorprompt);
                        errorprompt=messformat.format(oParams);
                        rrequest.getWResponse().getMessageCollector().warn(errorprompt,false,Consts.STATECODE_FAILED);
                        break;
                    }
                }
            }else
            {
                int erroridx=ReportAssistant.getInstance().doServerValidate(value,servervalidate);
                if(erroridx>=0)
                {
                    String errorprompt=servervalidate[erroridx][1];
                    if(errorprompt!=null&&!errorprompt.trim().equals(""))
                    {
                        errorprompt=rrequest.getI18NStringValue(errorprompt);
                        Object[] oParams= { value };
                        MessageFormat messformat=new MessageFormat(errorprompt);
                        errorprompt=messformat.format(oParams);
                    }
                    rrequest.getWResponse().getMessageCollector().warn(errorprompt,false,Consts.STATECODE_FAILED);
                }
            }
        }
        if(getDataTypeObj() instanceof VarcharType)
        {
            if(this.hasLeftPercent)
            {
                if(value==null) value="";
                value="%"+value;
            }
            if(this.hasRightPercent)
            {
                if(value==null) value="";
                value=value+"%";
            }
        }
        if(rrequest.getWResponse().getStatecode()==Consts.STATECODE_FAILED)
        {
            rrequest.getWResponse().terminateResponse(Consts.STATECODE_FAILED);
        }
        return value;
    }
    //    public int hashCode()
    
    
    
    
    
    
    
    
    
    //        {
    
    
    
    
    
    //        {//如果引用到同一个<col/>
    
    
    
    //            {//说明是出现在同一个位置（比如）
    
    //            }
    
    
    
}
