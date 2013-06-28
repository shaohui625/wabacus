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

import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.system.ReportRequest;

public class DeleteSqlActionBean extends AbsEditSqlActionBean
{
    private static Log log=LogFactory.getLog(DeleteSqlActionBean.class);

    public DeleteSqlActionBean(EditActionGroupBean ownerGroupBean)
    {
        super(ownerGroupBean);
    }

    public void parseActionscript(String reportTypeKey,String actionscript)
    {
        //$ByQXO move to AbstractJdbcDatabaseType.parseActionscript
        final AbsDatabaseType dbtype=this.ownerGroupBean.getDbType();        
        sql = dbtype.parseDeleteSql(this,reportTypeKey,actionscript);
        ownerGroupBean.addActionBean(this);        
        //ByQXO$
    }

    public void updateData(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues)
            throws SQLException
    {
    
        AbsDatabaseType dbtype=rrequest.getDbType(this.ownerGroupBean.getDatasource());
        
        //$ByQXO
        dbtype.updateDataForDeleteSqlAction(mRowData,mParamValues,rbean,rrequest,this);
        //ByQXO$
    }
    
}
