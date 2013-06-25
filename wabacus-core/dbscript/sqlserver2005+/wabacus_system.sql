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
SET QUOTED_IDENTIFIER ON 
GO
SET ANSI_NULLS ON 
GO







ALTER           proc [dbo].[SP_WABACUS_EXECUTE]
(
	@i_sql  varchar(3000),
	@i_orderby  varchar(200),
   @i_systeminfo varchar(3000),
	@o_systeminfo varchar(100) output
)
as
declare @startnum  int;--��ʼ��¼
declare @endnum  int;--��ֹ��¼
declare @pagesize int;--ҳ���С�������-1���򲻷�ҳ��ʾ���������Զ��б���
declare @dynorderby varchar(100);--���������е��ֶ���
declare @filtercolumn varchar(100);
declare @filtercondition varchar(300);
declare @refreshNavigateType varchar(10);
declare @rowgroupCols varchar(100);
declare @sqlCount nvarchar(3000);
declare @idx int;

set @filtercondition=dbo.FUN_WABACUS_GETPARAMVALUE('filter_condition',@i_systeminfo);
set @filtercolumn=dbo.FUN_WABACUS_GETPARAMVALUE('filter_column',@i_systeminfo);
if @filtercolumn<>''
begin--��ȡ�й���ѡ������
	set @i_sql='select  distinct '+@filtercolumn+' from ('+@i_sql +') wx_tbltemp';
  	if @filtercondition<>'' set @i_sql=@i_sql+' '+@filtercondition; --�����ǰ�ڲ�ѯѡ�е��й���ѡ������
	-- �����ǰ���ڲ�ѯ�����й���ѡ�����ݣ��򲻼����Ѿ�ѡ�е��й���ѡ����Ϊ����
  	set @i_sql=@i_sql+' order by '+@filtercolumn;
end else --��ȡ��������
begin
  	if @filtercondition<>'' set @i_sql='select * from ('+@i_sql+') wx_tbltemp1 '+@filtercondition; --�й�������
	declare @exec_sql varchar(3000);
	set @exec_sql=dbo.FUN_WABACUS_GETPARAMVALUE('exec_sql',@i_systeminfo);
	if @exec_sql<>''
	begin --�ͻ��˴�����Ҫִ�е�SQL��䣨�����ѯͳ�����ݣ�
		set @i_sql=replace(@exec_sql,'%ORIGINAL_SQL%',@i_sql);--��Ҫִ�е�SQL����е�ҵ��SQL����滻�����������Ĳ�ѯҵ�����ݵ�SQL���
	end else
	begin
  		set @pagesize=convert(int,dbo.FUN_WABACUS_GETPARAMVALUE('pagesize',@i_systeminfo));--ÿҳ��¼��
	  	set @refreshNavigateType=dbo.FUN_WABACUS_GETPARAMVALUE('refreshNavigateType',@i_systeminfo);
  		if @pagesize>0 and @refreshNavigateType='true'
  		begin --��ҳ��ʾ�����ұ���Ҫͳ�Ƽ�¼��
    		declare @temp varchar(200);
    		declare @recordcount int;
    		set @sqlCount='select  @temp=count(*) from ('+@i_sql+')  wx_tbltempcount';
	    	exec sp_executesql @sqlCount,N'@temp int output',@recordcount output; 
   	 	set @o_systeminfo=convert(varchar(100),@recordcount);--����¼������
			--print @o_systeminfo;
  		end
  		set @dynorderby=dbo.FUN_WABACUS_GETPARAMVALUE('dynamic_orderby',@i_systeminfo);--��̬�����ֶ�
  		if @dynorderby<>'' set @i_orderby=@dynorderby;
  		set @rowgroupCols=dbo.FUN_WABACUS_GETPARAMVALUE('rowgroup_cols',@i_systeminfo);
  		if @rowgroupCols<>''
  		begin --��ǰ���з�������η�����ʾ�ı���
			--���潫���е������ֶ�,�����ҿո�ȥ�������ڵ�һ��λ�ú����һ��λ�ü���,�ţ��Ա�����жϣ����,col1 desc,col2,col3 asc,��ʽ����ǰ����һ��,��
			declare @i_orderby_tmp varchar(200);
			set @i_orderby_tmp='';
			set @i_orderby=ltrim(rtrim(@i_orderby));
			set @idx=charindex(',',@i_orderby);
			while @idx>0		
			begin
				set @i_orderby_tmp=@i_orderby_tmp+ltrim(rtrim(substring(@i_orderby,1,@idx-1)))+',';
				set @i_orderby=ltrim(rtrim(substring(@i_orderby,@idx+1,datalength(@i_orderby))));
	  			set @idx=charindex(',',@i_orderby);
			end
			if @i_orderby<>'' set @i_orderby_tmp=@i_orderby_tmp+@i_orderby+',';
			if substring(@i_orderby_tmp,1,1)<>',' set @i_orderby_tmp=','+@i_orderby_tmp;--����ʼλ�ü���,��
			--���з����з��������ֶε���λ
			declare @orderbyNew varchar(200);
			declare @rowgroupColTmp varchar(30);--���ȡ����ÿ���������ֶ�
			declare @idx2 int;
			declare @ordertypeTmp varchar(10);
			declare @str1 varchar(100);
			declare @str2 varchar(100);
			set @orderbyNew='';
			while @rowgroupCols<>'' and @rowgroupCols<>','
			begin
				set @idx=charindex(',',@rowgroupCols);
				if @idx>0
				begin
					set @rowgroupColTmp=ltrim(rtrim(substring(@rowgroupCols,1,@idx-1)));--�õ������з�����У�������ǰ��Ķ���
					set @rowgroupCols=ltrim(rtrim(substring(@rowgroupCols,@idx+1,datalength(@rowgroupCols))));
				end else
				begin --����û�ж����ˣ�˵���Ѿ������һ���������ֶ���
					set @rowgroupColTmp=@rowgroupCols;
					set @rowgroupCols='';
				end
				if @rowgroupColTmp='' continue;
				set @ordertypeTmp='asc';--��ǰ�����е�����˳��
				set @idx2=charindex(','+lower(@rowgroupColTmp)+' ',lower(@i_orderby_tmp)); --,col1 asc/desc��ʽ
				if @idx2<=0 set @idx2=charindex(','+lower(@rowgroupColTmp)+',',lower(@i_orderby_tmp)); --,col1,...��ʽ
				if @idx2>0
    			begin --��@i_orderby���Ѿ����������������Ϊ�����ֶ�
					set @str1=substring(@i_orderby_tmp,1,@idx2-1);--ȡ���������ֶ�ǰ�沿�֣�����������
					set @i_orderby_tmp=substring(@i_orderby_tmp,@idx2+1,datalength(@i_orderby_tmp));
					set @idx2=charindex(',',@i_orderby_tmp);
					if @idx2>0
					begin --��@i_orderby�д������к��滹��������
						set @str2=substring(@i_orderby_tmp,1,@idx2-1);
						set @i_orderby_tmp=substring(@i_orderby_tmp,@idx2,datalength(@i_orderby_tmp));--���ﱣ����ǰ��Ķ���
					end else
					begin --�Ѿ������һ����������
						set @str2=substring(@i_orderby_tmp,1,datalength(@i_orderby_tmp));
						set @i_orderby_tmp='';
					end
					set @i_orderby_tmp=@str1+@i_orderby_tmp;--��@i_orderby��ɾ���˷����У���ΪҪ�����ᵽǰ���������
					set @idx2=charindex(' ',@str2); --@str2�д�ŵ��ǵ�ǰ�����ֶμ�����˳��
					if @idx2>0
					begin --���ֶκ���ָ��������˳��
						set @ordertypeTmp=ltrim(rtrim(substring(@str2,@idx2+1,datalength(@str2))));
						if @ordertypeTmp='' set @ordertypeTmp='asc';
					end
				end
				set @orderbyNew=@orderbyNew+@rowgroupColTmp+' '+@ordertypeTmp+',';
			end
			if @i_orderby_tmp<>''
			begin --��@i_orderby��ʣ�µĲ��ַ���@orderbyNew�ĺ���
				if substring(@i_orderby_tmp,1,1)=',' set @i_orderby_tmp=substring(@i_orderby_tmp,2,datalength(@i_orderby_tmp));
				set @orderbyNew=@orderbyNew+@i_orderby_tmp;
			end
			if substring(@orderbyNew,datalength(@orderbyNew),datalength(@orderbyNew))=',' set @orderbyNew=substring(@orderbyNew,1,datalength(@orderbyNew)-1); --ɾ����������,��
			set @i_orderby=@orderbyNew;
  		end
  		if @pagesize<=0
  		begin --����ҳ��ʾ�ı���
  			if @i_orderby<>'' set @i_sql='select * from ('+@i_sql+') order by '+@i_orderby;
  		end else
  		begin --��ҳ��ʾ�ı���
  			set @startnum=convert(int,dbo.FUN_WABACUS_GETPARAMVALUE('startrownum',@i_systeminfo));--��ʼ��¼��
			set @endnum=convert(int,dbo.FUN_WABACUS_GETPARAMVALUE('endrownum',@i_systeminfo));--������¼��
			if @i_orderby='' raiserror ('���ô洢����SP_WABACUS_EXECUTE��ѯ��ҳ����ʱ�����ڵڶ����������������ֶ�',16,1);
			set @i_sql='select * from (select   ROW_NUMBER() OVER( order by '+@i_orderby+') AS ROWID,* from ('+@i_sql+') as wx_tbltmp2) AS wx_tblTmp3 WHERE ROWID >'+ CAST(@startnum AS CHAR(10))+' AND ROWID<='+CAST(@endnum AS CHAR(10));
  		end
	end
end
--print @i_sql;
exec(@i_sql);









GO
SET QUOTED_IDENTIFIER OFF 
GO
SET ANSI_NULLS ON 
GO

 
SET QUOTED_IDENTIFIER OFF 
GO
SET ANSI_NULLS ON 
GO





ALTER      function [dbo].[FUN_WABACUS_GETPARAMVALUE]  
(
	@paramname varchar(100),
   @sourcestring varchar(3000)
)
RETURNS varchar(2000)
as
begin
	declare @val varchar(2000);
	declare @idx int;
	if @paramname is null or @sourcestring is null return '';
	set @sourcestring=LTRIM(RTRIM(@sourcestring));
	set @paramname=LTRIM(RTRIM(@paramname));
	if @sourcestring='' or @paramname=''  return '';
	set @idx=charIndex('{[(<'+ @paramname+':',@sourcestring);
	if @idx<=0 return '';
	set @val=substring(@sourcestring,@idx+datalength(@paramname)+5,datalength(@sourcestring));
	set @idx=charindex('>)]}',@val);
	if @idx<=0 return @val;
	return substring(@val,1,@idx-1);
end
 