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

import org.apache.commons.lang.StringUtils;

import com.wabacus.config.Config;
import com.wabacus.config.ResourceUtils;

/**
 * 
 * @author qxo(qxodream@gmail.com)
 *
 */
public class UserPermissionHandlerFactory
{

    private static UserPermissionHandler pmh;

    /**
     * 可通过全局配置可
     * @return 通过此工具方法获取当前配置的UserPermissionHandler实例,如果没配置则采用空实现UserPermissionHandler.NULL
     */
    public static UserPermissionHandler getUserPermissionHandler()
    {
        if(null==pmh)
        {
            final String strCls=Config.getInstance().getSystemConfigValue("UserPermissionHandlerClass",null);
            if(StringUtils.isNotBlank(strCls))  {
                try
                {
                    pmh=(UserPermissionHandler)ResourceUtils.loadClass(strCls.trim()).newInstance();
                }catch(InstantiationException e)
                {
                    throw new IllegalArgumentException(e);
                }catch(IllegalAccessException e)
                {
                    throw new IllegalArgumentException(e);
                }catch(ClassNotFoundException e)
                {
                    throw new IllegalArgumentException(e);
                }
            }
            if(null==pmh)
            {
                pmh=UserPermissionHandler.NULL;
            }

        }

        return pmh;
    }
}
