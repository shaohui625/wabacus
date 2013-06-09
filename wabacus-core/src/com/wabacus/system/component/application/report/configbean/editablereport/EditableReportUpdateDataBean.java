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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class EditableReportUpdateDataBean implements Cloneable
{
    public final static int EDITTYPE_INSERT=Consts.UPDATETYPE_INSERT;//当前的对象是对应<insert/>标签

    public final static int EDITTYPE_UPDATE=Consts.UPDATETYPE_UPDATE;//当前的对象是对应<update/>标签

    public final static int EDITTYPE_DELETE=Consts.UPDATETYPE_DELETE;//当前的对象是对应<delete/>标签

    private int edittype;

    protected String sqls;

    protected List<AbsEditSqlActionBean> lstSqlActionBeans;

    protected Map<ColBean,Set<EditableReportParamBean>> mParamsBeanInWhereClause;//出现在where子句中的<col/>

    protected Map<ColBean,Set<EditableReportParamBean>> mParamsBeanInUpdateClause;//出现在更新子句(包括insert和update两种子句)中的<col/>

    protected Map<ColBean,Set<EditableReportParamBean>> mParamsBeanInExternalValuesWhereClause;//出现在<external-values/>的select语句的where子句中

    protected List<EditableReportExternalValueBean> lstExternalValues;//通过<external-values/>配置的外部值

    private Map<String,String> mUpdateConditions;

    private String deleteConfirmMessage=null;
    
    private EditableReportSqlBean owner;

    public EditableReportUpdateDataBean(EditableReportSqlBean owner,int _edittype)
    {
        if(_edittype!=EDITTYPE_INSERT&&_edittype!=EDITTYPE_UPDATE&&_edittype!=EDITTYPE_DELETE)
        {
            throw new WabacusConfigLoadingException("传入的编辑类型不合法，只能取值为1、2、3之一");
        }
        this.owner=owner;
        this.edittype=_edittype;
    }

    public int getEdittype()
    {
        return edittype;
    }

    public void setSqls(String sqls)
    {
        this.sqls=sqls;
    }

    public List<AbsEditSqlActionBean> getLstSqlActionBeans()
    {
        return lstSqlActionBeans;
    }

    public void setLstSqlActionBeans(List<AbsEditSqlActionBean> lstSqlActionBeans)
    {
        this.lstSqlActionBeans=lstSqlActionBeans;
    }

    public Map<String,String> getMUpdateConditions()
    {
        return mUpdateConditions;
    }

    public void setMUpdateConditions(Map<String,String> mUpdateConditions)
    {
        this.mUpdateConditions=mUpdateConditions;
    }

    public String getDeleteConfirmMessage()
    {
        return deleteConfirmMessage;
    }

    public void setDeleteConfirmMessage(String deleteConfirmMessage)
    {
        this.deleteConfirmMessage=deleteConfirmMessage;
    }

    public EditableReportExternalValueBean getValueBeanByName(String name)
    {
        if(lstExternalValues==null) return null;
        for(int i=0;i<lstExternalValues.size();i++)
        {
            if(lstExternalValues.get(i).getName().equals(name))
            {
                return lstExternalValues.get(i);
            }
        }
        return null;
    }

    public List<EditableReportExternalValueBean> getLstExternalValues()
    {
        return lstExternalValues;
    }

    public void setLstExternalValues(List<EditableReportExternalValueBean> lstExternalValues)
    {
        this.lstExternalValues=lstExternalValues;
    }

    public EditableReportSqlBean getOwner()
    {
        return owner;
    }

    public void setOwner(EditableReportSqlBean owner)
    {
        this.owner=owner;
    }

    public void addParamBeanInWhereClause(ColBean cbean,EditableReportParamBean paramBean)
    {
        if(mParamsBeanInWhereClause==null) mParamsBeanInWhereClause=new HashMap<ColBean,Set<EditableReportParamBean>>();
        Set<EditableReportParamBean> sParamBeans=mParamsBeanInWhereClause.get(cbean);
        if(sParamBeans==null)
        {
            sParamBeans=new HashSet<EditableReportParamBean>();
            mParamsBeanInWhereClause.put(cbean,sParamBeans);
        }
        sParamBeans.add(paramBean);
    }

    public boolean containsParamBeanInWhereClause(ColBean cbean)
    {
        if(mParamsBeanInWhereClause==null) return false;
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return false;
        if(cbean.isNonValueCol()||cbean.isSequenceCol()
                ||cbean.isControlCol()) return false;
        return mParamsBeanInWhereClause.get(cbean)!=null&&mParamsBeanInWhereClause.get(cbean).size()>0;
    }

    public void addParamBeanInExternalValuesWhereClause(ColBean cbean,EditableReportParamBean paramBean)
    {
        if(mParamsBeanInExternalValuesWhereClause==null) mParamsBeanInExternalValuesWhereClause=new HashMap<ColBean,Set<EditableReportParamBean>>();
        Set<EditableReportParamBean> sParamBeans=mParamsBeanInExternalValuesWhereClause.get(cbean);
        if(sParamBeans==null)
        {
            sParamBeans=new HashSet<EditableReportParamBean>();
            mParamsBeanInExternalValuesWhereClause.put(cbean,sParamBeans);
        }
        sParamBeans.add(paramBean);
    }

    public boolean containsParamBeanInExternalValuesWhereClause(ColBean cbean)
    {
        if(mParamsBeanInExternalValuesWhereClause==null) return false;
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return false;
        if(cbean.isNonValueCol()||cbean.isSequenceCol()
                ||cbean.isControlCol()) return false;
        return mParamsBeanInExternalValuesWhereClause.get(cbean)!=null&&mParamsBeanInExternalValuesWhereClause.get(cbean).size()>0;
    }

    public void addParamBeanInUpdateClause(ColBean cbean,EditableReportParamBean paramBean)
    {
        if(mParamsBeanInUpdateClause==null) mParamsBeanInUpdateClause=new HashMap<ColBean,Set<EditableReportParamBean>>();
        Set<EditableReportParamBean> sParamBeans=mParamsBeanInUpdateClause.get(cbean);
        if(sParamBeans==null)
        {
            sParamBeans=new HashSet<EditableReportParamBean>();
            mParamsBeanInUpdateClause.put(cbean,sParamBeans);
        }
        sParamBeans.add(paramBean);
    }

    public boolean containsParamBeanInUpdateClause(ColBean cbean)
    {
        if(mParamsBeanInUpdateClause==null) return false;
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return false;
        if(cbean.isNonValueCol()||cbean.isSequenceCol()
                ||cbean.isControlCol()) return false;
        return mParamsBeanInUpdateClause.get(cbean)!=null&&mParamsBeanInUpdateClause.get(cbean).size()>0;
    }

    public int parseSqls(SqlBean sqlbean,String reportTypeKey)
    {
        if(lstExternalValues!=null&&lstExternalValues.size()>0)
        {
            for(EditableReportExternalValueBean evbeanTmp:lstExternalValues)
            {
                evbeanTmp.parseValues(this,sqlbean.getReportBean(),reportTypeKey);
            }
        }
        if(sqls==null||sqls.trim().equals(""))
        {
            return 0;
        }
        lstSqlActionBeans=new ArrayList<AbsEditSqlActionBean>();
        sqlbean.getDbType().parseUpdateAction(sqlbean,reportTypeKey,sqls,this);
        return 1;
    }


    public void addInsertSqlActionBean(String sql,List<EditableReportParamBean> lstParamBeans,String returnValueParamName)
    {
        InsertSqlActionBean insertActionBean=new InsertSqlActionBean(this);
        insertActionBean.setSql(sql);
        insertActionBean.setLstParamBeans(lstParamBeans);
        if(this.lstSqlActionBeans==null) this.lstSqlActionBeans=new ArrayList<AbsEditSqlActionBean>();
        lstSqlActionBeans.add(insertActionBean);
        insertActionBean.setReturnValueParamname(returnValueParamName);
    }

    public String parseUpdateWhereClause(ReportBean rbean,String reportKey,List<EditableReportParamBean> lstDynParams,String whereclause)
    {
        StringBuffer whereBuffer=new StringBuffer();
        whereclause=whereclause.trim();
        if(whereclause.toLowerCase().indexOf("where ")!=0)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新语句中条件子句不对，没有where关键字");
        }
        whereBuffer.append(" where ");
        whereclause=whereclause.substring("where ".length());
        whereclause=Tools.replaceCharacterInQuote(whereclause,'{',"$_LEFTBRACKET_$",true);
        whereclause=Tools.replaceCharacterInQuote(whereclause,'}',"$_RIGHTBRACKET_$",true);
        
        boolean isParam=false;
        int paramtype=-1;
        StringBuffer propBuffer=new StringBuffer();
        ColBean cb;
        for(int i=0;i<whereclause.length();i++)
        {
            if(isParam)
            {//说明当前是在"@{"内，后面的字符串都是动态参数对应的<col/>的property，直到"}"为止。
                if(whereclause.charAt(i)=='}')
                {
                    String property=propBuffer.toString().trim();
                    if(property==null||property.trim().equals("")) continue;
                    EditableReportParamBean paramBean=new EditableReportParamBean();
                    if(property.startsWith("%"))
                    {
                        paramBean.setHasLeftPercent(true);
                        property=property.substring(1);
                    }
                    if(i<whereclause.length()-1&&whereclause.charAt(i+1)=='%')
                    {
                        paramBean.setHasRightPercent(true);
                        i++;
                    }
                    if(paramtype==1)
                    {
                        //                        cb=rbean.getDbean().getColBeanByColProperty(
                        
                        cb=rbean.getDbean().getColBeanByColProperty(property);
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+property+"不合法，没有取到其值对应的<col/>");
                        }
                        EditableReportParamBean paramBeanTmp=createParamBeanByColbean(cb,reportKey,true,true);
                        paramBeanTmp.setHasLeftPercent(paramBean.isHasLeftPercent());
                        paramBeanTmp.setHasRightPercent(paramBean.isHasRightPercent());
                        paramBean=paramBeanTmp;
                    }else if(paramtype==2)
                    {
                        if(getValueBeanByName(property)==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有定义#{"+property+"}对应的值");
                        }
                        paramBean.setOwner(getValueBeanByName(property));
                        paramBean.setParamname(property);
                    }else if(paramtype==3)
                    {
                        paramBean.setParamname("!{"+property+"}");
                    }
                    lstDynParams.add(paramBean);
                    isParam=false;
                    paramtype=-1;
                    propBuffer=new StringBuffer();
                }else
                {
                    propBuffer.append(whereclause.charAt(i));
                }
            }else
            {
                if(whereclause.charAt(i)=='@')
                {
                    int k=parseDynParamsInUpdateClause(whereclause,i,propBuffer,whereBuffer);
                    if(k>i)
                    {
                        isParam=true;
                        paramtype=1;
                        i=k;
                    }
                }else if(whereclause.charAt(i)=='#')
                {
                    int k=parseDynParamsInUpdateClause(whereclause,i,propBuffer,whereBuffer);
                    if(k>i)
                    {
                        isParam=true;
                        paramtype=2;
                        i=k;
                    }
                }else if(whereclause.charAt(i)=='!')
                {
                    int k=parseDynParamsInUpdateClause(whereclause,i,propBuffer,whereBuffer);
                    if(k>i)
                    {
                        isParam=true;
                        paramtype=3;
                        i=k;
                    }
                }else
                {
                    whereBuffer.append(whereclause.charAt(i));
                }
            }
        }
        if(!propBuffer.toString().equals(""))
        {
            throw new WabacusConfigLoadingException("条件子句中不能出现“@{”、“!{”、“#{”字符组合");
        }
        whereclause=whereBuffer.toString();
      //将它们替换回来
        whereclause=Tools.replaceAll(whereclause,"$_LEFTBRACKET_$","{");
        whereclause=Tools.replaceAll(whereclause,"$_RIGHTBRACKET_$","}");
        return whereclause;
    }

    private int parseDynParamsInUpdateClause(String whereclause,int idx,StringBuffer propBuffer,StringBuffer whereBuffer)
    {
        int k=idx+1;
        for(;k<whereclause.length();k++)
        {
            if(whereclause.charAt(k)==' ') continue;
            if(whereclause.charAt(k)=='{')
            {
                if(whereclause.charAt(idx-1)=='%')
                {
                    propBuffer.append("%");
                    whereBuffer.deleteCharAt(whereBuffer.length()-1);
                }
                whereBuffer.append("?");
                idx=k;
            }else
            {//在@之后碰到的是其它字符，则不是参数，此时的@、#、! 就是一个普通字符。
                whereBuffer.append(whereclause.charAt(idx));
            }
            break;
        }
        if(k==whereclause.length())
        {
            whereBuffer.append(whereclause.charAt(idx));
        }
        return idx;
    }

    public EditableReportParamBean createParamBeanByColbean(ColBean cbean,String reportKey,boolean enableHiddenCol,boolean isMust)
    {
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")||cbean.isNonValueCol()
                ||cbean.isSequenceCol()||cbean.isControlCol())
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"失败，列"+cbean.getColumn()+"不是从数据库获取数据的列，不能做为更新列");
        }
        if(!isMust&&cbean.isNonFromDbCol()) return null;
        EditableReportParamBean paramBean=null;
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(reportKey);
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype()))
        {
            if(ercbean==null||ercbean.getUpdatedcol()==null||ercbean.getUpdatedcol().trim().equals(""))
            {//没有被别的<col/>通过updatecol属性引用到
                if(!enableHiddenCol)
                {
                    if(!isMust) return null;
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"失败，列"+cbean.getColumn()+"为隐藏列，在这种情况不允许做为更新数据的字段");
                }
            }
        }else if(ercbean!=null&&ercbean.getUpdatecol()!=null&&!ercbean.getUpdatecol().trim().equals(""))
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"失败，列"+cbean.getColumn()
                    +"为通过updatecol属性引用了其它列，此列本身不能做为更新列");
        }
        paramBean=new EditableReportParamBean();
        if(ercbean!=null) paramBean.setDefaultvalue(ercbean.getDefaultvalue());
        paramBean.setOwner(cbean);
        return paramBean;
    }

    public void doPostLoad()
    {
        processReturnValueOfAllSqlActionBeans();//处理所有sql中的返回值
        processParamnameOfAllParamBeans();
    }

    private void processReturnValueOfAllSqlActionBeans()
    {
        if(this.lstSqlActionBeans!=null)
        {
            String rtnValParaNameTmp;
            for(AbsEditSqlActionBean sqlBeanTmp:this.lstSqlActionBeans)
            {
                rtnValParaNameTmp=sqlBeanTmp.getReturnValueParamname();
                if(rtnValParaNameTmp==null||rtnValParaNameTmp.trim().equals("")) continue;
                if(Tools.isDefineKey("#",rtnValParaNameTmp))
                {//返回值存放在<external-values/>定义的变量中
                    rtnValParaNameTmp=Tools.getRealKeyByDefine("#",rtnValParaNameTmp);
                    if(rtnValParaNameTmp==null||rtnValParaNameTmp.trim().equals(""))
                    {
                        sqlBeanTmp.setReturnValueParamname(null);
                    }else if(getValueBeanByName(rtnValParaNameTmp)==null)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.owner.getOwner().getReportBean()+"的更新SQL语句失败，返回值：#{"+rtnValParaNameTmp
                                +"}引用的变量没有在<external-values/>中定义");
                    }
                }else if(Tools.isDefineKey("rrequest",rtnValParaNameTmp))
                {
                    rtnValParaNameTmp=Tools.getRealKeyByDefine("rrequest",rtnValParaNameTmp);
                    if(rtnValParaNameTmp==null||rtnValParaNameTmp.trim().equals(""))
                    {
                        sqlBeanTmp.setReturnValueParamname(null);
                    }
                }else
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.owner.getOwner().getReportBean()+"的更新SQL语句失败，返回值："+rtnValParaNameTmp
                            +"不合法，必须是#{paramname}或rrequset{key}之一的格式");
                }
            }
        }
    }

    private void processParamnameOfAllParamBeans()
    {
        if(mParamsBeanInUpdateClause!=null&&mParamsBeanInUpdateClause.size()>0)
        {
            Set<EditableReportParamBean> sParamBeansTmp;
            ColBean cbOwner;
            ColBean cbSrc;
            EditableReportColBean ercbeanTmp;
            for(Entry<ColBean,Set<EditableReportParamBean>> entry:mParamsBeanInUpdateClause.entrySet())
            {
                cbSrc=entry.getKey();
                sParamBeansTmp=entry.getValue();
                for(EditableReportParamBean paramBeanTmp:sParamBeansTmp)
                {
                    cbOwner=paramBeanTmp.getColbeanOwner();
                    if(cbOwner==null) continue;
                    if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbOwner.getDisplaytype()))
                    {
                        if(cbOwner.getProperty().equals(cbSrc.getProperty()))
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbOwner));
                            continue;
                        }
                    }
                    //显示字段或被隐藏字段通过updatecol引用的显示字段
                    if(edittype==EDITTYPE_DELETE)
                    {//是<delete/>中的更新语句
                        ercbeanTmp=(EditableReportColBean)cbSrc.getExtendConfigDataForReportType(EditableReportColBean.class);
                        if(ercbeanTmp==null||!ercbeanTmp.isEditableForUpdate())
                        {//没有配置<update/>，或修改时不允许编辑当前列
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc));
                        }else
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc)+"_old");
                        }
                    }else
                    {
                        paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc));
                    }
                }
            }
        }
        if(mParamsBeanInWhereClause!=null&&mParamsBeanInWhereClause.size()>0)
        {
            Set<EditableReportParamBean> sParamBeansTmp;
            ColBean cbOwner;
            ColBean cbSrc;
            EditableReportColBean ercbeanTmp;
            for(Entry<ColBean,Set<EditableReportParamBean>> entry:mParamsBeanInWhereClause.entrySet())
            {
                cbSrc=entry.getKey();
                sParamBeansTmp=entry.getValue();
                for(EditableReportParamBean paramBeanTmp:sParamBeansTmp)
                {
                    cbOwner=paramBeanTmp.getColbeanOwner();
                    if(cbOwner==null) continue;
                    if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbOwner.getDisplaytype()))
                    {
                        if(cbOwner.getProperty().equals(cbSrc.getProperty()))
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbOwner));
                            continue;
                        }
                    }
                    
                    if(edittype==EDITTYPE_INSERT)
                    {//是<insert/>中的条件语句
                        paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc));
                    }else
                    {
                        ercbeanTmp=(EditableReportColBean)cbSrc.getExtendConfigDataForReportType(EditableReportColBean.class);
                        if(ercbeanTmp==null||!ercbeanTmp.isEditableForUpdate())
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc));
                        }else
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc)+"_old");
                        }
                    }
                }
            }
        }
        if(mParamsBeanInExternalValuesWhereClause!=null&&mParamsBeanInExternalValuesWhereClause.size()>0)
        {
            Set<EditableReportParamBean> sParamBeansTmp;
            ColBean cbOwner;
            ColBean cbSrc;
            EditableReportColBean ercbeanTmp;
            for(Entry<ColBean,Set<EditableReportParamBean>> entry:mParamsBeanInExternalValuesWhereClause.entrySet())
            {
                cbSrc=entry.getKey();
                sParamBeansTmp=entry.getValue();
                for(EditableReportParamBean paramBeanTmp:sParamBeansTmp)
                {
                    cbOwner=paramBeanTmp.getColbeanOwner();
                    if(cbOwner==null) continue;
                    if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbOwner.getDisplaytype()))
                    {//隐藏字段
                        if(cbOwner.getProperty().equals(cbSrc.getProperty()))
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbOwner));
                            continue;
                        }
                    }
                    
                    if(edittype==EDITTYPE_DELETE)
                    {//是<delete/>中定义的foreign变量中引用的列数据
                        ercbeanTmp=(EditableReportColBean)cbSrc.getExtendConfigDataForReportType(EditableReportColBean.class);
                        if(ercbeanTmp==null||!ercbeanTmp.isEditableForUpdate())
                        {//没有配置<update/>，或修改时不允许编辑当前列
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc));
                        }else
                        {
                            paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc)+"_old");
                        }
                    }else
                    {//出现在<update/>或<insert/>中<external-values/>的foreign中的动态变量均取可编辑列的最新值，而不是取旧值
                        paramBeanTmp.setParamname(EditableReportAssistant.getInstance().getColParamName(cbSrc));
                    }
                }
            }
        }
    }

    public Object clone(EditableReportSqlBean newowner)
    {
        try
        {
            EditableReportUpdateDataBean newbean=(EditableReportUpdateDataBean)super.clone();
            newbean.setOwner(newowner);
            if(lstSqlActionBeans!=null&&lstSqlActionBeans.size()>0)
            {
                
                
                //                for(AbsEditSqlActionBean beanTmp:lstSqlActionBeans)
                
                
                
                throw new WabacusConfigLoadingException("当已经解析了更新数据的SQL语句后，不允许进行clone操作");
            }
            /*if(mParamsBeanInWhereClause!=null)
            {
                newbean.mParamsBeanInWhereClause=(Map)((HashMap)mParamsBeanInWhereClause).clone();
            }
            if(mParamsBeanInUpdateClause!=null)
            {
                newbean.mParamsBeanInUpdateClause=(Map)((HashMap)mParamsBeanInUpdateClause).clone();
            }
            if(mParamsBeanInExternalValuesWhereClause!=null)
            {
                newbean.mParamsBeanInExternalValuesWhereClause=(Map)((HashMap)mParamsBeanInExternalValuesWhereClause)
                        .clone();
            }*/

            if(lstExternalValues!=null)
            {
                List<EditableReportExternalValueBean> lstExternalValuesNew=new ArrayList<EditableReportExternalValueBean>();
                for(EditableReportExternalValueBean valueBeanTmp:lstExternalValues)
                {
                    lstExternalValuesNew.add((EditableReportExternalValueBean)valueBeanTmp.clone());
                }
                newbean.setLstExternalValues(lstExternalValuesNew);
            }
            if(mUpdateConditions!=null)
            {
                newbean.setMUpdateConditions((Map)((HashMap)mUpdateConditions).clone());
            }
            return newbean;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+edittype;
        result=prime*result+((owner==null)?0:owner.hashCode());
        result=prime*result+((sqls==null)?0:sqls.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final EditableReportUpdateDataBean other=(EditableReportUpdateDataBean)obj;
        if(edittype!=other.edittype) return false;
        if(owner==null)
        {
            if(other.owner!=null) return false;
        }else if(!owner.equals(other.owner)) return false;
        if(sqls==null)
        {
            if(other.sqls!=null) return false;
        }else if(!sqls.equals(other.sqls)) return false;
        return true;
    }
}
