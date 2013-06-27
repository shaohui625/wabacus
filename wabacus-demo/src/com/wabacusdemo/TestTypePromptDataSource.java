/* 
 * Copyright (C) 2010 星星<349446658@qq.com>
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
package com.wabacusdemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.typeprompt.AbsTypePromptDataSource;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.system.ReportRequest;

public class TestTypePromptDataSource extends AbsTypePromptDataSource
{
    private final static String[][] datacache= {//测试JAVA对象中的数据源，在真正使用时，可能这里的数据是从缓存或从数据库中获取的
    { "10001", "宋文华" }, { "10002", "周燕" }, { "10003", "周红" }, { "10004", "胡智波" },
            { "10005", "胡秀青" }, { "10006", "涂琦英" }, { "10007", "宋节斌" }, { "10008", "周英龙" },
            { "10009", "吴树青" }, { "10010", "范新华" }, { "10011", "吴国发" }, { "10012", "吴志枫" },
            { "10013", "范坚琴" }, { "10014", "周勇伟" }, { "10015", "周节华" }, { "10016", "范员波" },
            { "10017", "胡冬琴" }, { "10018", "周红" }, { "10019", "吴志清" }, { "10020", "吴志国" },
            { "10021", "吴清珊" }, { "10022", "王志尖" }, { "10023", "吴良光" }, { "10024", "胡玟乐" },
            { "10025", "洪亮亮" }, { "10026", "付瑞明" }, { "10027", "胡志莹" }, { "10028", "高洪波" },
            { "10029", "刘铭署" }, { "10030", "胡婷" }, { "10031", "吴建中" }, { "10032", "吴志诚" },
            { "10033", "苏伟官" }, { "10034", "吴圆蛾" }, { "10035", "吴芝红" }, { "10036", "万兴国" },
            { "10037", "范贵红" }, { "10038", "宋国珍" }
    };

    public List<Map<String,String>> getResultDataList(ReportRequest rrequest,ReportBean rbean,
            String typedata)
    {
        List<Map<String,String>> lstResult=new ArrayList<Map<String,String>>();
        if(typedata==null||typedata.trim().equals("")) return lstResult;
        List<TypePromptColBean> lstPColsBean=promptConfigBean.getLstPColBeans();//取出关于本输入联想的配置信息
        if(lstPColsBean==null||lstPColsBean.size()==0) return lstResult;
        int matchmode_no=lstPColsBean.get(0).getMatchmode();//取出“工号”列的匹配模式
        int matchmode_name=lstPColsBean.get(1).getMatchmode();//取出“中文名”列的匹配模式
        for(int i=0;i<datacache.length;i++)
        {
            Map<String,String> mresult=new HashMap<String,String>();
            if((matchmode_no==1&&datacache[i][0].indexOf(typedata)==0)
                    ||(matchmode_no==2&&datacache[i][0].indexOf(typedata)>=0))
            {//如果工号字段匹配
                mresult.put("no",datacache[i][0]);
                mresult.put("name",datacache[i][1]);
                lstResult.add(mresult);
            }else if((matchmode_name==1&&datacache[i][1].indexOf(typedata)==0)
                    ||(matchmode_name==2&&datacache[i][1].indexOf(typedata)>=0))
            {//如果中文名字段匹配
                mresult.put("no",datacache[i][0]);
                mresult.put("name",datacache[i][1]);
                lstResult.add(mresult);
            }
        }
        return lstResult;
    }

}
