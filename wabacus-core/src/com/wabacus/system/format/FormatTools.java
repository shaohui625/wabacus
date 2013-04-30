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
package com.wabacus.system.format;

import java.text.DecimalFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FormatTools
{
    private static Log log=LogFactory.getLog(FormatTools.class);

    public static String substring(String str,Integer start,Integer length)
    {
        return substring(str,start.intValue(),length.intValue());
    }

    public static String substring(String str,int start,int length)
    {
        if(str!=null)
        {
            str=str.trim();
            if(length>0)
            {
                if(str.length()>start+length)
                {
                    str=str.substring(start,length);
                }
            }else
            {
                if(str.length()>start)
                {
                    str=str.substring(start);
                }
            }
        }
        return str;
    }

    public static String formatDouble(String srcString,String pattern)
    {
        try
        {
            if(srcString==null||srcString.trim().equals(""))
            {
                return "";
            }
            DecimalFormat df=new DecimalFormat(pattern);
            srcString=df.format(Double.parseDouble(srcString));
            return srcString;
        }catch(Exception e)
        {
            log.error("以"+pattern+"格式格式化"+srcString+"时，发生了异常：",e);
            return srcString;
        }
    }

    public static String formatLong(String srcString,String pattern)
    {
        try
        {
            if(srcString==null||srcString.trim().equals(""))
            {
                return "";
            }
            DecimalFormat df=new DecimalFormat(pattern);
            srcString=df.format(Long.parseLong(srcString));
            return srcString;
        }catch(Exception e)
        {
            log.error("以"+pattern+"格式格式化"+srcString+"时，发生了异常：",e);
            return srcString;
        }
    }

    //    /**
    
    
    
    
    //     * @param pattern
    
    
    //     */
    
    
    //        try
    
    
    
    
    //            }
    
    
    
    
    //        }catch(Exception e)
    
    
    
    
    //    }
    public static String htmlEncode(String src)
    {
        if(src==null||src.trim().equals(""))
        {
            return src;
        }
        StringBuffer result=new StringBuffer();
        char character;
        for(int i=0;i<src.length();i++)
        {
            character=src.charAt(i);
            if(character=='<')
            {
                result.append("&lt;");
            }else if(character=='>')
            {
                result.append("&gt;");
            }else if(character=='\"')
            {
                result.append("&quot;");
            }else if(character=='\'')
            {
                result.append("&#039;");
            }else if(character=='\\')
            {
                result.append("&#092;");
            }else
            {
                result.append(character);
            }
        }
        return result.toString();
    }
}
