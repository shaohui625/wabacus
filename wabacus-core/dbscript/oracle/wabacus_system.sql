/* 
 * Copyright (C) 2010-2012 ����<349446658@qq.com>
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

 
create or replace package PKG_WABACUS is
   TYPE CURSOR_WABACUS IS REF CURSOR;
   PROCEDURE SP_WABACUS_EXECUTE(i_sql varchar2, i_orderby varchar2,i_systeminfo varchar2,o_systeminfo out varchar2,o_cursor out CURSOR_WABACUS); 
   FUNCTION FUN_WABACUS_GETPARAMVALUE(paramname varchar2,sourcestring varchar2) return varchar2;
end PKG_WABACUS;

create or replace package body PKG_WABACUS is
   procedure SP_WABACUS_EXECUTE
   (
       i_sql IN varchar2,
       i_orderby IN varchar2,
       i_systeminfo in varchar2,
       o_systeminfo out varchar2,
       o_cursor out CURSOR_WABACUS
   ) is
      startnum varchar2(20);
      endnum varchar2(20);
      pagesize integer;
      filtercolumn varchar2(100);
      filtercondition varchar2(300);
      refreshNavigateType varchar2(10);
      sqlCount varchar2(3000);
      realorderby varchar2(100);
      dynorderby varchar2(50);
      i_orderby_tmp varchar2(300);
      rowgroupCols varchar2(100);
      rowgroupColTmp varchar2(50);
      str1 varchar2(300);
      str2 varchar2(300);
      ordertypeTmp varchar2(10);
      idx2 integer;
      exec_sql varchar2(3000);
      idx integer;
      result_sql varchar2(3000);
   begin
      filtercondition:=FUN_WABACUS_GETPARAMVALUE('filter_condition',i_systeminfo);
      filtercolumn:=FUN_WABACUS_GETPARAMVALUE('filter_column',i_systeminfo);
      if filtercolumn<>' ' then /*��ȡ�й���ѡ������*/
         result_sql:='select  distinct '||filtercolumn||' from ('||i_sql||') wx_tbltemp';
         if filtercondition<>' ' then result_sql:=result_sql||' '||filtercondition; end if;/*�����ǰ�ڲ�ѯѡ�е��й���ѡ������*/
         /*�����ǰ���ڲ�ѯ�����й���ѡ�����ݣ��򲻼����Ѿ�ѡ�е��й���ѡ����Ϊ����*/
         result_sql:=result_sql||' order by '||filtercolumn;
      else /*��ȡ��������*/
         result_sql:=i_sql;
         if filtercondition<>' ' then result_sql:='select * from ('||result_sql||') wx_tbltemp1 '||filtercondition; end if;/*�й�������*/
         exec_sql:=FUN_WABACUS_GETPARAMVALUE('exec_sql',i_systeminfo);
         if exec_sql<>' ' then /*�ͻ��˴�����Ҫִ�е�SQL��䣨�����ѯͳ�����ݣ�*/
            result_sql:=replace(exec_sql,'%ORIGINAL_SQL%',result_sql);/*��Ҫִ�е�SQL����е�ҵ��SQL����滻�����������Ĳ�ѯҵ�����ݵ�SQL���*/
         else
            pagesize:=to_number(FUN_WABACUS_GETPARAMVALUE('pagesize',i_systeminfo));/*ÿҳ��¼��*/
	          refreshNavigateType:=FUN_WABACUS_GETPARAMVALUE('refreshNavigateType',i_systeminfo);
  	        if pagesize>0 and refreshNavigateType='true' then /*��ҳ��ʾ�����ұ���Ҫͳ�Ƽ�¼��*/
               sqlCount:='select count(*) from ('|| result_sql||') wx_tbltempcount';
               execute immediate sqlCount into idx;-- using paramno;
               o_systeminfo:=to_char(idx);
  	        end if;
            realorderby:=i_orderby;
            dynorderby:=FUN_WABACUS_GETPARAMVALUE('dynamic_orderby',i_systeminfo); /*��̬�����ֶ�*/
  	        if dynorderby<>' ' then realorderby:=dynorderby; end if;
  	        rowgroupCols:=FUN_WABACUS_GETPARAMVALUE('rowgroup_cols',i_systeminfo);
            if rowgroupCols<>' ' then /*��ǰ���з�������η�����ʾ�ı���*/
		           /*���潫���е������ֶ�,�����ҿո�ȥ�������ڵ�һ��λ�ú����һ��λ�ü���,�ţ��Ա�����жϣ����,col1 desc,col2,col3 asc,��ʽ����ǰ����һ��,��*/
		           i_orderby_tmp:='';
		           realorderby:=ltrim(rtrim(realorderby));
		           idx:=instr(realorderby,',');
		           while idx>0 loop
		              i_orderby_tmp:=i_orderby_tmp||ltrim(rtrim(substr(realorderby,1,idx-1)))||',';
		              realorderby:=ltrim(rtrim(substr(realorderby,idx+1)));
		              idx:=instr(realorderby,',');
		           end loop;
		           if realorderby<>' ' then i_orderby_tmp:=i_orderby_tmp||realorderby||','; end if;
		           if substr(i_orderby_tmp,1,1)<>',' then i_orderby_tmp:=','||i_orderby_tmp; end if;/*����ʼλ�ü���,��*/
		           /*���з����з��������ֶε���λ*/
		           realorderby:=' ';
               while rowgroupCols<>' ' and rowgroupCols<> ',' loop
		              idx:=instr(rowgroupCols,',');
		              if idx>0 then
		                 rowgroupColTmp:=ltrim(rtrim(substr(rowgroupCols,1,idx-1)));/*�õ������з�����У�������ǰ��Ķ���*/
			               rowgroupCols:=ltrim(rtrim(substr(rowgroupCols,idx+1)));
		              else /*����û�ж����ˣ�˵���Ѿ������һ���������ֶ���*/
			               rowgroupColTmp:=rowgroupCols;
			               rowgroupCols:='';
		              end if;
		              if rowgroupColTmp<>' ' then 
			               ordertypeTmp:='asc';/*��ǰ�����е�����˳��*/
			               idx2:=instr(lower(i_orderby_tmp),','||lower(rowgroupColTmp)||' '); /*,col1 asc/desc��ʽ*/
			               if idx2<=0 then idx2:=instr(lower(i_orderby_tmp),','||lower(rowgroupColTmp)||','); end if;/*,col1,...��ʽ*/
			               if idx2>0 then /*��@i_orderby���Ѿ����������������Ϊ�����ֶ�*/
			                  str1:=substr(i_orderby_tmp,1,idx2-1);/*ȡ���������ֶ�ǰ�沿�֣�����������*/
			                  i_orderby_tmp:=substr(i_orderby_tmp,idx2+1);
			                  idx2:=instr(i_orderby_tmp,',');
			                  if idx2>0 then /*��@i_orderby�д������к��滹��������*/
				                   str2:=substr(i_orderby_tmp,1,idx2-1);
				                   i_orderby_tmp:=substr(i_orderby_tmp,idx2);/*���ﱣ����ǰ��Ķ���*/
			                  else /*�Ѿ������һ����������*/
				                   str2:=i_orderby_tmp;
				                   i_orderby_tmp:='';
			                  end if;
			                  i_orderby_tmp:=str1||i_orderby_tmp;/*��@i_orderby��ɾ���˷����У���ΪҪ�����ᵽǰ���������*/
			                  idx2:=instr(str2,' '); /*@str2�д�ŵ��ǵ�ǰ�����ֶμ�����˳��asc/desc*/
			                  if idx2>0 then /*���ֶκ���ָ��������˳��*/
				                   ordertypeTmp:=ltrim(rtrim(substr(str2,idx2+1)));
				                   if ordertypeTmp=' ' then ordertypeTmp:='asc'; end if;
			                  end if;
			               end if;
			               realorderby:=realorderby||rowgroupColTmp||' '||ordertypeTmp||',';
		              end if;
		           end loop;
               if i_orderby_tmp<>' ' then /*��@i_orderby��ʣ�µĲ��ַ���@orderbyNew�ĺ���*/
		              if substr(i_orderby_tmp,1,1)=',' then i_orderby_tmp:=substr(i_orderby_tmp,2); end if;
		              realorderby:=realorderby||i_orderby_tmp;
		           end if;
		           if substr(realorderby,length(realorderby))=',' then realorderby:=substr(realorderby,1,length(realorderby)-1); end if;/*ɾ����������,��*/
            end if;
            if realorderby<>' ' then result_sql:='select * from ('||result_sql||') wx_tbltmp2 order by '||realorderby; end if;
	          if pagesize>0 then /*�����ǻ�ȡĳҳ�ļ�¼*/
		           startnum:=FUN_WABACUS_GETPARAMVALUE('startrownum',i_systeminfo);/*��ʼ��¼��*/
		           endnum:=FUN_WABACUS_GETPARAMVALUE('endrownum',i_systeminfo);/*������¼��*/
               result_sql:='SELECT * FROM(SELECT wx_temp_tbl1.*, ROWNUM row_num FROM ('||result_sql||')  wx_temp_tbl1 WHERE ROWNUM<='||endnum||')  wx_temp_tbl2 WHERE row_num>'||startnum;
            end if;
         end if;
      end if;
      --insert into tbl_test(message) values(result_sql);
      OPEN o_cursor FOR result_sql;-- using i_maxrownum,i_minrownum;
   end SP_WABACUS_EXECUTE;
   
   
   FUNCTION FUN_WABACUS_GETPARAMVALUE
   (
       paramname varchar2,
       sourcestring varchar2
   ) return varchar2 
   is
       paramvalue varchar2(2000);
       sourcestring_local varchar2(2000);
       paramname_local varchar2(100);
       idx integer;
   begin  
       paramvalue:=' ';
       sourcestring_local:=sourcestring;
       paramname_local:=paramname;
       if paramname_local is null or sourcestring_local is null then return paramvalue; end if;
       sourcestring_local:=LTRIM(RTRIM(sourcestring_local));
       paramname_local:=LTRIM(RTRIM(paramname_local));
       if sourcestring_local=' ' and paramname_local=' '  then return paramvalue; end if;
       idx:=instr(sourcestring_local,'{[(<'|| paramname_local||':');
       if idx<=0 then return paramvalue; end if;
       paramvalue:=substr(sourcestring_local,idx+length(paramname_local)+5);
       idx:=instr(paramvalue,'>)]}');
       if idx<=0 then return paramvalue; end if;
       paramvalue:=substr(paramvalue,1,idx-1);
      return paramvalue;
   end FUN_WABACUS_GETPARAMVALUE;
end PKG_WABACUS;
