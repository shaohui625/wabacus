/* 
 * Copyright (C) 2010-2012 吴卫华(wuweihua)<349446658@qq.com>
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
package com.wabacus.system.permission;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.permission.permissiontype.AbsPermissionType;

/**
 * 用户权限解析处理器:用于解决用户指指定组件的权限
 * @author qxo(qxodream@gmail.com)
 *
 */
public interface UserPermissionHandler
{

    UserPermissionHandler NULL=new UserPermissionHandler()
    {

        /* (non-Javadoc)
         * @see com.wabacus.system.permission.UserPermissionHandler#getUserPermission(com.wabacus.config.component.IComponentConfigBean, java.lang.String)
         */
        public String getUserPermission(ReportRequest rrequest,String cmpId,String permissionType)
        {
            return null;
        }

    };

    /**
     * 获取指定组件指定类型的权限值[可以在此处理进行组件运行时权限初始化]
     * @param configBean - 组件对象
     * @param permissionType - 权限类型
     * @return 返回null表示设置权限,否则返回当前用户对指定组件指定类型权限的权限值
     * @see AbsPermissionType
     */
    String getUserPermission(ReportRequest rrequest,String cmpId ,String permissionType);
}
