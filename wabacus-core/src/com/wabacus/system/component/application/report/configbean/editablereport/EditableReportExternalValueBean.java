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

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class EditableReportExternalValueBean implements Cloneable
{
    private String name;//<value/>配置的name属性

    private String value;

    private IDataType typeObj;

    private List<EditableReportParamBean> lstParamsBean;

    private EditableReportUpdateDataBean owner;
    

    
    public EditableReportExternalValueBean(EditableReportUpdateDataBean owner)
    {
        this.owner=owner;
    }
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name=name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public IDataType getTypeObj()
    {
        return typeObj;
    }

    public void setTypeObj(IDataType typeObj)
    {
        this.typeObj=typeObj;
    }

    public List<EditableReportParamBean> getLstParamsBean()
    {
        return lstParamsBean;
    }

    public void setLstParamsBean(List<EditableReportParamBean> lstParamsBean)
    {
        this.lstParamsBean=lstParamsBean;
    }
    
    public EditableReportUpdateDataBean getOwner()
    {
        return owner;
    }

    public void parseValues(EditableReportUpdateDataBean updateBean,ReportBean rbean,String reportTypeKey)
    {
        /**if(Tools.isDefineKey("#",value))
        {
            parseRefOtherExternalValue(rbean);
        }else if(Tools.isDefineKey("@",value))
        {//当前变量是引用其它列的值（目前只允许使用被绑定保存的其它报表定义的列，不能引用本报表自己的列）
            parseRefColExternalValue(rbean);
        }else*/
        if(Tools.isDefineKey("url",value))
        {
            rbean.addParamNameFromURL(Tools.getRealKeyByDefine("url",value));
        }else if(Tools.isDefineKey("foreign",value))
        {
            parseSqlExternalValue(updateBean,rbean,reportTypeKey);
        }
    }

    private void parseRefOtherExternalValue(ReportBean rbean)
    {
        /**String valTmp=Tools.getRealKeyByDefine("#",value);
        List<String> lstTmp=Tools.parseStringToList(valTmp,".");
        if(lstTmp.size()!=3)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，引用其它报表变量值的格式不合法，必须为#{reportid.insert/update/delete.name}");
        }
        ReportBean rbTmp=rbean.getPageBean().getReportChild(lstTmp.get(0),true);
        if(rbTmp==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，引用其它报表变量值时，没找到被引用的报表："+lstTmp.get(0));
        }
        EditableReportSqlBean editsqlbean=(EditableReportSqlBean)rbTmp.getSbean()
                .getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(editsqlbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name+"的<value/>配置错误，引用其它报表变量值时，引用的报表："
                    +lstTmp.get(0)+"不是可编辑报表");
        }
        if("insert".equals(lstTmp.get(1)))
        {//是引用源报表中<insert/>中定义的某个变量值
            if(editsqlbean.getInsertbean()==null)
            {//被引用的源报表没有配置<insert/>标签
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在<external-values/>中name属性为"+name
                        +"的<value/>配置错误，引用其它报表变量值时，引用的报表："+lstTmp.get(0)+"没有配置<insert/>");
            }
            this.refObj=editsqlbean.getInsertbean().getValueBeanByName(lstTmp.get(2));
        }else if("update".equals(lstTmp.get(1)))
        {
            if(editsqlbean.getUpdatebean()==null)
            {//被引用的源报表没有配置<update/>标签
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在<external-values/>中name属性为"+name
                        +"的<value/>配置错误，引用其它报表变量值时，引用的报表："+lstTmp.get(0)+"没有配置<update/>");
            }
            this.refObj=editsqlbean.getUpdatebean().getValueBeanByName(lstTmp.get(2));
        }else if("delete".equals(lstTmp.get(1)))
        {
            if(editsqlbean.getDeletebean()==null)
            {//被引用的源报表没有配置<delete/>标签
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在<external-values/>中name属性为"+name
                        +"的<value/>配置错误，引用其它报表变量值时，引用的报表："+lstTmp.get(0)+"没有配置<delete/>");
            }
            this.refObj=editsqlbean.getDeletebean().getValueBeanByName(lstTmp.get(2));
        }else
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，必须配置为reportid.insert/update/delete.name格式");
        }
        if(this.refObj==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，引用其它报表变量值时，引用的报表："+lstTmp.get(0)
                    +"没有定义name为"+lstTmp.get(2)+"的变量");
        }
        valTmp=((EditableReportExternalValueBean)this.refObj).getValue();
        if(Tools.isDefineKey("@",valTmp)||Tools.isDefineKey("#",valTmp))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，它引用的变量又是引用其它报表变量或列数据的变量。");
        }
        if(this.typeObj==null)
        {//此变量没有配置自己的类型，则使用被引用的源变量的类型
            this.typeObj=((EditableReportExternalValueBean)this.refObj).getTypeObj();
        }*/
    }
    
    private void parseRefColExternalValue(ReportBean rbean)
    {
        String valTmp=Tools.getRealKeyByDefine("@",value);
        List<String> lstTmp=Tools.parseStringToList(valTmp,".");
        if(lstTmp.size()!=2&&lstTmp.size()!=3&&lstTmp.size()!=4)
        {
            throw new WabacusConfigLoadingException(
                    "加载报表"
                            +rbean.getPath()
                            +"失败，在<external-values/>中name属性为"
                            +name
                            +"的<value/>配置错误，引用其它报表某列值时格式不合法，必须为@{reportid.insert/update/delete.colname[.old]}或者@{reportid.colname}");
        }
        ReportBean rbTmp=rbean.getPageBean().getReportChild(lstTmp.get(0),true);
        if(rbTmp==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，引用其它报表某列数据时，没找到被引用的报表："+lstTmp.get(0));
        }
        EditableReportSqlBean editsqlbean=(EditableReportSqlBean)rbTmp.getSbean()
                .getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(editsqlbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name+"的<value/>配置错误，引用其它报表变量值时，引用的报表："
                    +lstTmp.get(0)+"不是可编辑报表");
        }
        ColBean cbTmp=null;
        String colnameTmp=null;
        if(lstTmp.size()==2)
        {
            colnameTmp=lstTmp.get(1);
        }else
        {
            String type=lstTmp.get(1);
            if("insert".equals(type))
            {
                if(editsqlbean.getInsertbean()==null)
                {//被引用的源报表没有配置<insert/>标签
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                            +"失败，在<external-values/>中name属性为"+name
                            +"的<value/>配置错误，引用其它报表某列时，引用的报表："+lstTmp.get(0)+"没有配置<insert/>");
                }
            }else if("update".equals(type))
            {
                if(editsqlbean.getUpdatebean()==null)
                {//被引用的源报表没有配置<update/>标签
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                            +"失败，在<external-values/>中name属性为"+name
                            +"的<value/>配置错误，引用其它报表某列值时，引用的报表："+lstTmp.get(0)+"没有配置<update/>");
                }
            }else if("delete".equals(type))
            {//是引用源报表中删除数据时某列的值
                if(editsqlbean.getDeletebean()==null)
                {//被引用的源报表没有配置<delete/>标签
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                            +"失败，在<external-values/>中name属性为"+name
                            +"的<value/>配置错误，引用其它报表某列值时，引用的报表："+lstTmp.get(0)+"没有配置<delete/>");
                }
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在<external-values/>中name属性为"+name
                        +"的<value/>配置错误，必须配置为reportid.insert/update/delete.colname格式");
            }
            colnameTmp=lstTmp.get(2);
            if(lstTmp.size()==4)
            {
                if(!"update".equals(lstTmp.get(1))||!lstTmp.get(3).equals("old"))
                {
                    throw new WabacusConfigLoadingException(
                            "加载报表"
                                    +rbean.getPath()
                                    +"失败，在<external-values/>中name属性为"
                                    +name
                                    +"的<value/>配置错误，引用其它报表某列值时格式不合法，必须为@{reportid.insert/update/delete.colname}或者@{reportid.colname}");
                }
            }
        }
        cbTmp=rbTmp.getDbean().getColBeanByColProperty(colnameTmp);
        if(cbTmp==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name
                    +"的<value/>配置错误，引用其它报表列时，引用的报表："+lstTmp.get(0)
                    +"没有定义column为"+colnameTmp+"的列");
        }
       /** this.refObj=cbTmp;*/
        if(this.typeObj==null)
        {
            this.typeObj=cbTmp.getDatatypeObj();
        }
    }

    private void parseSqlExternalValue(EditableReportUpdateDataBean updateBean,ReportBean rbean,
            String reportTypeKey)
    {
        String sql=Tools.getRealKeyByDefine("foreign",value.trim());
        if(sql.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                    +"失败，在<external-values/>中name属性为"+name+"的<value/>配置错误，查询数据的SQL语句不能为空");
        }
        lstParamsBean=new ArrayList<EditableReportParamBean>();
        int idxwhere=sql.toLowerCase().lastIndexOf(" where ");
        if(idxwhere>0)
        {
            List<EditableReportParamBean> lstDynParamsInWhereClause=new ArrayList<EditableReportParamBean>();
            String whereclause=updateBean.parseUpdateWhereClause(rbean,
                    reportTypeKey,lstDynParamsInWhereClause,sql.substring(idxwhere).trim());
            sql=sql.substring(0,idxwhere)+" "+whereclause;
            EditableReportColBean ercbeanTmp;
            ColBean cb;
            for(EditableReportParamBean paramBean:lstDynParamsInWhereClause)
            {
                if(paramBean.getOwner() instanceof ColBean)
                {
                    cb=(ColBean)paramBean.getOwner();
                    if(cb.getProperty()==null||cb.getProperty().trim().equals("")
                            ||cb.isNonValueCol()
                            ||cb.isSequenceCol()
                            ||cb.isControlCol())
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，列"
                                +cb.getColumn()+"不是从数据库获取数据的列，不能做为查询条件值");
                    }
                    ercbeanTmp=(EditableReportColBean)cb
                            .getExtendConfigDataForReportType(reportTypeKey);
                    ColBean cbSrc=cb;
                    if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cb.getDisplaytype()))
                    {
                        if(ercbeanTmp!=null&&ercbeanTmp.getUpdatedcol()!=null
                                &&!ercbeanTmp.getUpdatedcol().trim().equals(""))
                        {
                            cbSrc=cb.getReportBean().getDbean()
                                    .getColBeanByColProperty(ercbeanTmp.getUpdatedcol());//取到引用此<col/>的<col/>对象
                        }
                    }else
                    {
                        if(ercbeanTmp!=null&&ercbeanTmp.getUpdatecol()!=null
                                &&!ercbeanTmp.getUpdatecol().trim().equals(""))
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                                    +"失败，列"+cb.getColumn()+"为通过updatecol属性引用了其它列，此列本身不能做为条件值");
                        }
                    }
                    updateBean.addParamBeanInExternalValuesWhereClause(cbSrc,paramBean);
                }
                lstParamsBean.add(paramBean);
            }
        }
        value=sql;
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone 对象失败",e);
        }
        //之所以不对成员变量对象lstParamsBean进行单独clone，是因为此对象是在doPostLoad()方法中生成的，调用这里的clone时，它的值为null
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((name==null)?0:name.hashCode());
        result=prime*result+((owner==null)?0:owner.hashCode());
        result=prime*result+((value==null)?0:value.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final EditableReportExternalValueBean other=(EditableReportExternalValueBean)obj;
        if(name==null)
        {
            if(other.name!=null) return false;
        }else if(!name.equals(other.name)) return false;
        if(owner==null)
        {
            if(other.owner!=null) return false;
        }else if(!owner.equals(other.owner)) return false;
        if(value==null)
        {
            if(other.value!=null) return false;
        }else if(!value.equals(other.value)) return false;
        return true;
    }
    
    
}
