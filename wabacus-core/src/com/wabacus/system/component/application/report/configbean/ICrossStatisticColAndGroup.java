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
package com.wabacus.system.component.application.report.configbean;

import java.util.List;

import com.wabacus.config.component.application.report.ConditionBean;

public interface ICrossStatisticColAndGroup
{
    public String getColumn();
    
    /****************************************
     * 下面几个方法只对顶层交叉统计列或分组有效
     ****************************************/
    
    public void setTablename(String tablename);
    
    public String getTablename();
    
    public void setLstTablenameConditions(List<ConditionBean> lstTablenameConditions);
    
    public List<ConditionBean> getLstTablenameConditions();
    
    public CrossStatisticListColBean getStatisColBean();
    
    public void initStatisDisplayBean(CrossStatisticListStatisBean statisBean,List<String> lstStatitems);
    
    public CrossStatisticListGroupBean getParentCrossStatiGroupBean();
    
    public void setParentCrossStatiGroupBean(CrossStatisticListGroupBean parentCrossStatiGroupBean);
}
