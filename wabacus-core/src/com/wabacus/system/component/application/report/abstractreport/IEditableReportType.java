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
package com.wabacus.system.component.application.report.abstractreport;

import java.sql.Connection;
import java.sql.SQLException;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.buttons.AbsButtonType;

public interface IEditableReportType
{
    public boolean needCertainTypeButton(AbsButtonType buttonType);

    public String getDefaultAccessMode();

    public String getRealAccessMode();
    
    public String getColOriginalValue(Object object,ColBean cbean);

    public int[] doSaveAction(Connection conn) throws SQLException;
    
    public void setNewAccessMode(String newaccessmode);
}
