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
package com.wabacus.system.intercept;
public class RowDataByInterceptor
{
    private String insertDisplayRowHtml;
    
    private String dynTrStyleproperty;//显示当前行时的<tr/>样式字符串，比如设置为 bgcolor='red' height='30px'。这样显示当前行的<tr/>时就会有<tr bgcolor='red' height='30px'.../>
    
    private boolean shouldDisplayThisRow=true;

    public String getInsertDisplayRowHtml()
    {
        return insertDisplayRowHtml;
    }

    public void setInsertDisplayRowHtml(String insertDisplayRowHtml)
    {
        this.insertDisplayRowHtml=insertDisplayRowHtml;
    }

    public String getDynTrStyleproperty()
    {
        return dynTrStyleproperty;
    }

    public void setDynTrStyleproperty(String dynTrStyleproperty)
    {
        this.dynTrStyleproperty=dynTrStyleproperty;
    }

    public boolean isShouldDisplayThisRow()
    {
        return shouldDisplayThisRow;
    }

    public void setShouldDisplayThisRow(boolean shouldDisplayThisRow)
    {
        this.shouldDisplayThisRow=shouldDisplayThisRow;
    }    
}

