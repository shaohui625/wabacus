set character_set_client=GBK; 
set character_set_connection=GBK; 
set character_set_database=GBK; 
set character_set_results=GBK; 
set character_set_server=GBK;
DELIMITER $$  ;
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
DROP PROCEDURE IF EXISTS SP_WABACUS_EXECUTE$$

CREATE PROCEDURE SP_WABACUS_EXECUTE(
    i_sql  varchar(3000),
    i_orderby  varchar(200),
    i_systeminfo varchar(3000),
    out o_systeminfo varchar(100)
)
BEGIN
    declare startnum varchar(20);
    declare endnum varchar(20);
    declare pagesize int;/*ҳ���С�������-1���򲻷�ҳ��ʾ���������Զ��б���*/
    declare filtercolumn varchar(100);
    declare filtercondition varchar(300);
    declare refreshNavigateType varchar(10);
    declare dynorderby varchar(50);
    declare i_orderby_tmp varchar(300);
    declare rowgroupCols varchar(100);
    declare rowgroupColTmp varchar(50);
    declare str1 varchar(300);
    declare str2 varchar(300);
    declare ordertypeTmp varchar(10);
    declare exec_sql varchar(3000);
    declare idx int;
    declare idx2 int;
    set @sql='';
    set filtercondition=FUN_WABACUS_GETPARAMVALUE('filter_condition',i_systeminfo);
    set filtercolumn=FUN_WABACUS_GETPARAMVALUE('filter_column',i_systeminfo);
    if filtercolumn<>'' then /*��ȡ�й���ѡ������*/
	set i_sql=concat('select  distinct ',filtercolumn,' from (',i_sql ,') wx_tbltemp');
  	if filtercondition<>'' then set i_sql=concat(i_sql,' ',filtercondition); end if;/*�����ǰ�ڲ�ѯѡ�е��й���ѡ������*/
	/*�����ǰ���ڲ�ѯ�����й���ѡ�����ݣ��򲻼����Ѿ�ѡ�е��й���ѡ����Ϊ����*/
  	set i_sql=concat(i_sql,' order by ',filtercolumn);
	set @sql=i_sql;
    else /*��ȡ��������*/
	if filtercondition<>'' then set i_sql=concat('select * from (',i_sql,') wx_tbltemp1 ',filtercondition); end if;/*�й�������*/
	set exec_sql=FUN_WABACUS_GETPARAMVALUE('exec_sql',i_systeminfo);
	if exec_sql<>'' then /*�ͻ��˴�����Ҫִ�е�SQL��䣨�����ѯͳ�����ݣ�*/
	    set i_sql=replace(exec_sql,'%ORIGINAL_SQL%',i_sql);/*��Ҫִ�е�SQL����е�ҵ��SQL����滻�����������Ĳ�ѯҵ�����ݵ�SQL���*/
	    set @sql=i_sql;
	else
	    set pagesize=cast(FUN_WABACUS_GETPARAMVALUE('pagesize',i_systeminfo) as signed);/*ÿҳ��¼��*/
	    set refreshNavigateType=FUN_WABACUS_GETPARAMVALUE('refreshNavigateType',i_systeminfo);
  	    if pagesize>0 and refreshNavigateType='true' then /*��ҳ��ʾ�����ұ���Ҫͳ�Ƽ�¼��*/
		set @tmp=0;
    	        set @sqlCount=concat('select  count(*) from (',i_sql,')  wx_tbltempcount into @tmp');
	        prepare stmt from @sqlCount;
		execute stmt;
		set o_systeminfo=cast(@tmp as char(20));
		deallocate prepare stmt;
  	    end if;
  	    set dynorderby=FUN_WABACUS_GETPARAMVALUE('dynamic_orderby',i_systeminfo); /*��̬�����ֶ�*/
  	    if dynorderby<>'' then set i_orderby=dynorderby; end if;
  	    set rowgroupCols=FUN_WABACUS_GETPARAMVALUE('rowgroup_cols',i_systeminfo);
	    if rowgroupCols<>'' then /*��ǰ���з�������η�����ʾ�ı���*/
		/*���潫���е������ֶ�,�����ҿո�ȥ�������ڵ�һ��λ�ú����һ��λ�ü���,�ţ��Ա�����жϣ����,col1 desc,col2,col3 asc,��ʽ����ǰ����һ��,��*/
		set i_orderby_tmp='';
		set i_orderby=ltrim(rtrim(i_orderby));
		set idx=instr(i_orderby,',');
		while idx>0 do
		    set i_orderby_tmp=concat(i_orderby_tmp,ltrim(rtrim(substring(i_orderby,1,idx-1))),',');
		    set i_orderby=ltrim(rtrim(substring(i_orderby,idx+1)));
		    set idx=instr(i_orderby,',');
		end while;
		if i_orderby<>'' then set i_orderby_tmp=concat(i_orderby_tmp,i_orderby,','); end if;
		if substring(i_orderby_tmp,1,1)<>',' then set i_orderby_tmp=concat(',',i_orderby_tmp); end if;/*����ʼλ�ü���,��*/
		/*���з����з��������ֶε���λ*/
		set i_orderby='';
		while rowgroupCols<>'' and rowgroupCols<>',' do
		    set idx=instr(rowgroupCols,',');
		    if idx>0 then
		        set rowgroupColTmp=ltrim(rtrim(substring(rowgroupCols,1,idx-1)));/*�õ������з�����У�������ǰ��Ķ���*/
			set rowgroupCols=ltrim(rtrim(substring(rowgroupCols,idx+1)));
		    else /*����û�ж����ˣ�˵���Ѿ������һ���������ֶ���*/
			set rowgroupColTmp=rowgroupCols;
			set rowgroupCols='';
		    end if;
		    if rowgroupColTmp<>'' then 
			set ordertypeTmp='asc';/*��ǰ�����е�����˳��*/
			set idx2=instr(lower(i_orderby_tmp),concat(',',lower(rowgroupColTmp),' ')); /*,col1 asc/desc��ʽ*/
			if idx2<=0 then set idx2=instr(lower(i_orderby_tmp),concat(',',lower(rowgroupColTmp),',')); end if;/*,col1,...��ʽ*/
			if idx2>0 then /*��@i_orderby���Ѿ����������������Ϊ�����ֶ�*/
			    set str1=substring(i_orderby_tmp,1,idx2-1);/*ȡ���������ֶ�ǰ�沿�֣�����������*/
			    set i_orderby_tmp=substring(i_orderby_tmp,idx2+1);
			    set idx2=instr(i_orderby_tmp,',');
			    if idx2>0 then /*��@i_orderby�д������к��滹��������*/
				set str2=substring(i_orderby_tmp,1,idx2-1);
				set i_orderby_tmp=substring(i_orderby_tmp,idx2);/*���ﱣ����ǰ��Ķ���*/
			    else /*�Ѿ������һ����������*/
				set str2=i_orderby_tmp;
				set i_orderby_tmp='';
			    end if;
			    set i_orderby_tmp=concat(str1,i_orderby_tmp);/*��@i_orderby��ɾ���˷����У���ΪҪ�����ᵽǰ���������*/
			    set idx2=instr(str2,' '); /*@str2�д�ŵ��ǵ�ǰ�����ֶμ�����˳��asc/desc*/
			    if idx2>0 then /*���ֶκ���ָ��������˳��*/
				set ordertypeTmp=ltrim(rtrim(substring(str2,idx2+1)));
				if ordertypeTmp='' then set ordertypeTmp='asc'; end if;
			    end if;
			end if;
			set i_orderby=concat(i_orderby,rowgroupColTmp,' ',ordertypeTmp,',');
		    end if;
		end while;
		if i_orderby_tmp<>'' then /*��@i_orderby��ʣ�µĲ��ַ���@orderbyNew�ĺ���*/
		    if substring(i_orderby_tmp,1,1)=',' then set i_orderby_tmp=substring(i_orderby_tmp,2); end if;
		    set i_orderby=concat(i_orderby,i_orderby_tmp);
		end if;
		if substring(i_orderby,length(i_orderby))=',' then set i_orderby=substring(i_orderby,1,length(i_orderby)-1); end if;/*ɾ����������,��*/
	    end if;
	
	    if i_orderby<>'' then set i_sql=concat('select * from (',i_sql,') wx_tbltmp2 order by ',i_orderby); end if;
	    if pagesize>0 then /*�����ǻ�ȡĳҳ�ļ�¼*/
		set startnum=FUN_WABACUS_GETPARAMVALUE('startrownum',i_systeminfo);/*��ʼ��¼��*/
		set endnum=FUN_WABACUS_GETPARAMVALUE('endrownum',i_systeminfo);/*������¼��*/
		set i_sql=concat(i_sql,' limit ',startnum,',',endnum);
  	    end if;
	    set @sql=i_sql;
        end if;
    end if;
   /*insert testable values(@sql);*/
   prepare stmt2 from @sql;
   execute stmt2;
   deallocate prepare stmt2;
END$$


DROP FUNCTION IF EXISTS FUN_WABACUS_GETPARAMVALUE$$

CREATE FUNCTION FUN_WABACUS_GETPARAMVALUE(
    paramname varchar(100),
    sourcestring varchar(3000)
) RETURNS varchar(2000)
BEGIN
    declare val varchar(2000);
    declare idx int;
    set val='';
    if paramname is not null and sourcestring is not null then
        set sourcestring=LTRIM(RTRIM(sourcestring));
        set paramname=LTRIM(RTRIM(paramname));
        if sourcestring<>'' and paramname<>'' then
	    set idx=instr(sourcestring,concat('{[(<',paramname,':'));
	    if idx>0 then 
		set val=substring(sourcestring,idx+length(paramname)+5);
		set idx=instr(val,'>)]}');
		if idx>0 then
		    set val=substring(val,1,idx-1);
		end if;
	    end if;
	end if;
    end if;
    return val;
END$$