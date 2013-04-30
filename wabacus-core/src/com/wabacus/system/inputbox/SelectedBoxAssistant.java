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
package com.wabacus.system.inputbox;

import java.util.List;

public class SelectedBoxAssistant
{
    private final static SelectedBoxAssistant instance=new SelectedBoxAssistant();

    private SelectedBoxAssistant()
    {}

    public static SelectedBoxAssistant getInstance()
    {
        return instance;
    }
    
    boolean isSelectedValueOfMultiSelectBox(String selectedvalues,String optionvalue,String separator)
    {
        if(selectedvalues==null||optionvalue==null) return false;
        if(separator==null||separator.equals("")) separator=" ";
        selectedvalues=selectedvalues.trim();
        optionvalue=optionvalue.trim();
        while(selectedvalues.startsWith(separator))
        {
            selectedvalues=selectedvalues.substring(separator.length());
        }
        while(selectedvalues.endsWith(separator))
        {
            selectedvalues=selectedvalues.substring(0,selectedvalues.length()-separator.length());
        }
        if(selectedvalues.equals(optionvalue)) return true;
        String[] tmpArr=selectedvalues.split(separator);
        for(int i=0;i<tmpArr.length;i++)
        {
            if(optionvalue.equals(tmpArr[i].trim())) return true;
        }
        return false;
    }
    
    String getSelectedLabelByValuesOfSingleSelectedBox(List<String[]> lstOptionsResult,String selectedvalue)
    {
        if(lstOptionsResult==null||lstOptionsResult.size()==0) return "";
        for(String[] items:lstOptionsResult)
        {
            if(items[1]!=null&&items[1].equals(selectedvalue))
            {
                return items[0];
            }
        }
        return "";
    }
    
    String getSelectedLabelByValuesOfMultiSelectedBox(List<String[]> lstOptionsResult,String selectedvalue,String separator)
    {
        if(lstOptionsResult==null||lstOptionsResult.size()==0) return "";
        StringBuffer labelBuf=new StringBuffer();
        for(String[] items:lstOptionsResult)
        {
            if(items[1]!=null&&isSelectedValueOfMultiSelectBox(selectedvalue,items[1],separator))
            {
                labelBuf.append(items[0]).append(separator);
            }
        }
        String labels=labelBuf.toString();
        if(labels.endsWith(separator)) labels=labels.substring(0,labels.length()-separator.length());
        return labels;
    }
}
