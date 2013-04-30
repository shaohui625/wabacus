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
package com.wabacus.system.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupTitlePositionBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;

public class UltraListReportAssistant
{
    private static Log log=LogFactory.getLog(UltraListReportAssistant.class);
    
    private final static UltraListReportAssistant instance=new UltraListReportAssistant();

    protected UltraListReportAssistant()
    {};

    public static UltraListReportAssistant getInstance()
    {
        return instance;
    }
    
    public List sortChildrenByDynColOrders(List lstChildren,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(lstChildren.size()==1||lstDynColids==null||lstDynColids.size()==0)
        {
            return lstChildren;
        }
        Object[] objs=lstChildren.toArray();
        Object objTmp;
        int tmp;
        int n=objs.length-1;
        while(n>0)
        {
            int index=n;
            n=0;
            for(int i=0;i<index;i++)
            {
                tmp=comparePosition(objs[i],objs[i+1],lstDynColids,mColAndGroupTitlePostions);
                if(tmp>0)
                {
                    objTmp=objs[i];
                    objs[i]=objs[i+1];
                    objs[i+1]=objTmp;
                    n=i;
                }else if(tmp==-2)
                {
                    continue;
                }else if(tmp==-3)
                {
                    int j=i+2;
                    for(;j<=index;j++)
                    {
                        tmp=comparePosition(objs[i],objs[j],lstDynColids,mColAndGroupTitlePostions);
                        if(tmp==-3)
                        {//objs[j]还不参与本次显示
                            continue;
                        }
                        if(tmp>0)
                        {
                            objTmp=objs[i];
                            objs[i]=objs[j];
                            objs[j]=objTmp;
                            n=i;
                        }
                        break;
                    }
                    if(j>index) break;
                }
            }
        }
        List lstChildrenNew=new ArrayList();
        for(int i=0;i<objs.length;i++)
        {
            lstChildrenNew.add(objs[i]);
        }
        return lstChildrenNew;
    }

    private int comparePosition(Object obj1,Object obj2,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        String colid1=null;
        String colid2=null;
        
        
        if(obj1 instanceof ColBean)
        {
            colid1=((ColBean)obj1).getColid();
            if(mColAndGroupTitlePostions.get(colid1).getDisplaymode()<=0) return -2;
                //            label1=((ColBean)obj1).getLabel();
        }else
        {
            if(mColAndGroupTitlePostions.get(((UltraListReportGroupBean)obj1).getGroupid()).getDisplaymode()<=0) return -2;
            colid1=((UltraListReportGroupBean)obj1).getFirstColId(lstDynColids);
            
        }
        if(obj2 instanceof ColBean)
        {
            colid2=((ColBean)obj2).getColid();
            if(mColAndGroupTitlePostions.get(colid2).getDisplaymode()<=0) return -3;//被比较的当前普通列不显示，则不比较
                
        }else
        {
            if(mColAndGroupTitlePostions.get(((UltraListReportGroupBean)obj2).getGroupid())
                    .getDisplaymode()<=0) return -3;
            colid2=((UltraListReportGroupBean)obj2).getFirstColId(lstDynColids);
            //            label2=((UltraReportListGroupBean)obj2).getLabel();
        }
        int idx1=-1;
        int idx2=-1;
        for(int i=0;i<lstDynColids.size();i++)
        {
            if(lstDynColids.get(i).equals(colid1))
            {
                idx1=i;
            }else if(lstDynColids.get(i).equals(colid2))
            {
                idx2=i;
            }
        }
        if(idx1==-1)
        {
            log.warn("没有找到colid为"+colid1+"的<col/>，拖动列失败");
            return 0;
        }
        if(idx2==-1)
        {
            log.warn("没有找到colid为"+colid2+"的<col/>，拖动列失败");
            return 0;
        }
        
        if(idx1>idx2) return 1;
        if(idx1<idx2) return -1;
        return 0;
    }
}
