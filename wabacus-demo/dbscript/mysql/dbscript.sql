set character_set_client=GBK; 
set character_set_connection=GBK; 
set character_set_database=GBK; 
set character_set_results=GBK; 
set character_set_server=GBK;
create database wabacusdemodb character set GBK;
use wabacusdemodb;

create table tbl_dynamicolumns(
	layer1 varchar (50) ,
	layer2 varchar (50) ,
	layer3 varchar (50) ,
	col_column varchar (50) 
);
insert into tbl_dynamicolumns values('[��̬]��������','[��̬]����','[��̬]������','name');
insert into tbl_dynamicolumns values('[��̬]��������','[��̬]����','[��̬]Ӣ����','ename');
insert into tbl_dynamicolumns values('[��̬]��������',null,'[��̬]�Ա�','sex');
insert into tbl_dynamicolumns values('[��̬]��������',null,'[��̬]����','age');
insert into tbl_dynamicolumns values(null,null,'[��̬]��������','birthday');
create table tbl_area(
	province	varchar(20),
	city		varchar(20),
	county		varchar(20)
);
insert into tbl_area values('����','�˴�','�ϸ�');
insert into tbl_area values('����','�˴�','�߰�');
insert into tbl_area values('����','�˴�','����');
insert into tbl_area values('����','�˴�','����');
insert into tbl_area values('����','�ϲ�','����');
insert into tbl_area values('����','�ϲ�','�½�');
insert into tbl_area values('����','����','����');
insert into tbl_area values('����','����','�ٴ�');
insert into tbl_area values('�㶫','����','����');
insert into tbl_area values('�㶫','����','����');
insert into tbl_area values('�㶫','����','��ɽ');
insert into tbl_area values('�㶫','��ݸ','����');
insert into tbl_area values('�㶫','��ݸ','����');
insert into tbl_area values('����','Ȫ��','�ݰ�');
insert into tbl_area values('����','Ȫ��','��Ϫ');
insert into tbl_area values('����','Ȫ��','�ϰ�');
insert into tbl_area values('����','����','��̩');
insert into tbl_area values('����','����','����');

create table tbl_baseinfo(
	uuid		varchar(50),
	no		varchar(20),
	name		varchar(30),
	sex		int,
	age		int,
	birthday	date,
	deptno		varchar(20)
);

insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0001','10001','���Ļ�',1,25,'1978-01-03','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0002','10002','����',0,38,'1978-12-21','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0003','10003','�ܺ�',0,45,'1968-05-23','0003');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0004','10004','���ǲ�',1,35,'1963-08-12','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0005','10005','������',1,36,'1965-06-11','0002');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0006','10006','Ϳ��Ӣ',0,56,'1959-05-03','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0007','10007','�νڱ�',1,45,'1957-11-05','0002');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0008','10008','��Ӣ��',1,26,'1980-10-09','0003');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0009','10009','������',1,32,'1983-11-15','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0010','10010','���»�',1,36,'1983-08-17','0012');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0011','10011','�����',1,26,'1980-07-19','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0012','10012','��־��',1,43,'1978-08-13','0002');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0013','10013','������',0,28,'1977-03-03','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0014','10014','����ΰ',1,36,'1978-07-05','0005');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0015','10015','�ܽڻ�',1,46,'1976-05-17','0009');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0016','10016','��Ա��',1,35,'1978-07-16','0009');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0017','10017','������',0,56,'1980-06-15','0005');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0018','10018','����ö',0,28,'1980-08-21','0007');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0019','10019','��־��',1,37,'1968-07-27','0004');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0020','10020','��־��',1,38,'1987-08-29','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0021','10021','����ɺ',0,28,'1969-06-25','0009');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0022','10022','��־��',1,36,'1980-07-26','0012');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0023','10023','������',1,28,'1986-03-25','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0024','10024','������',1,37,'1976-03-21','0004');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0025','10025','������',1,46,'1978-04-23','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0026','10026','������',1,45,'1966-06-12','0006');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0027','10027','��־Ө',0,29,'1985-07-11','0010');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0028','10028','�ߺ鲨',1,34,'1975-03-10','0008');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0029','10029','������',1,38,'1973-07-15','0006');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0030','10030','����',0,36,'1976-11-03','0010');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0031','10031','�⽨��',1,39,'1988-12-15','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0032','10032','��־��',1,26,'1986-04-19','0007');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0033','10033','��ΰ��',1,28,'1963-10-26','0007');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0034','10034','��Բ��',0,29,'1968-08-27','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0035','10035','��֥��',0,23,'1965-01-21','0002');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0036','10036','���˹�',1,27,'1958-11-20','0011');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0037','10037','�����',0,45,'1980-10-23','0001');
insert into tbl_baseinfo values('8a9f8189297d71b001297d71b01a0038','10038','�ι���',0,36,'1982-06-13','0008');

create table tbl_department(
	guid			varchar(50),
	deptno			varchar(20),	
	deptname		varchar(30),
	manager			varchar(30),
	builtdate		date,
	performance		varchar(20),
	description		text
);

insert into tbl_department values('3a8f8189297d71b001297d71b01a0001','0001','����������','�⻪��','2001-01-03','����','���ݿͻ�Ҫ��Ĳ�Ʒ�����ڣ��������������Ų��ԣ����Ű�װ����ʱ������ɲ�Ʒ�����з���Ʒ������������֧�֣������������䣬5S�������������������ϡ����Ʒ�ͳ�Ʒ����������������������Ա����������������ѵ���ƶ����������ֵ��ƶȺ����̣������۵Ĳ�Ʒ�������и��ٺ͹�������ͻ���ƷͶ�ߣ������Ʒ���������������֯����߲�Ʒ��֤��3C��CCEE��CCIB��CE����������֤��ISO9000-ISO90004����������֤��ISO14000�����ƶ�������ȫ����������TQM��');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0002','0002','������','��ΰ��','2001-01-03','�ܲ�','�����۲���õĿͻ�����¼�뵽ERPϵͳ����������Ӧ�ļӹ������Գ�Ʒ���տͻ�Ҫ������ںͷ����ص���ɳ�Ʒ���ˣ�������˾�Կͻ���ʱ�����ʣ���������Э����Ӧ�̹������ϣ��߽����ϣ������Ͻ���������飬���Թ�Ӧ�����Ͻ��и��ܺ�����FIFO�����յ���Ʊ����֯���������Ӧ�̶Թ�˾�ļ�ʱ�����ʣ��Լ���˰���Ϻͱ���ʵ�������ƿ�棬�ƶ������ƶȺ���Ӧ����');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0003','0003','�ɹ���','���һ�','2001-11-13','����','ǰ�ڹ�Ӧ�̵���Ѱ���Թ�Ӧ�̵���֤�Ϳ��ˣ�̸�����ϼ۸��ƶ������ɹ�Э�飬�Թ�Ӧ�̵Ĳ�Ʒ���ͻ������������ܣ������������ϵĲɹ����ƶ����вɹ����ϵ����̺��ƶȣ��Թ�ҵ��Ŀ�ɹ���֧��');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0004','0004','����','������','2003-06-09','����','�Թ�˾�ľ�Ӫ״�����з������ṩ���߲�������֧�֣�����ƾ֤¼��ϵͳ���Թ�Ӧ�̿��ߵķ�Ʊ�������Ʊ���ͻ���������Ӧ�տ�Ĺ������Ը���ƾ֤���ܲ������ˣ����߲��񱨱��ʲ���ծ��������ֽ�������������˾�����ֽ𣬹���֧Ʊ����Ʊ����Ʊ���վݣ�����������˾�̶��ʲ�');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0005','0005','�з�������','������','2003-12-03','����','��������в�Ʒ�������£������Ż�����Ʒ�Ľ����²�Ʒ�з�����Ʒ��������֧�֣������з��豸���з�ʵ����');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0006','0006','����������','���»�','2003-12-09','����','��չ��˾��ҵ�Ļ����ƶ���˾�����ƶȣ�����Ա����ְ��ְ���Ͷ���ͬ����Ա����Ϣ�����ƶ�Ա�������ͽ����ƶȣ��ƶ���˾��ѵ���ߺ��ƶȣ����ų�����Ա��ʳ�ޣ�����˾�೵������˾ʳ�ã�����˾�칫�豸');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0007','0007','��Ϣ����','��С��','2004-03-12','����','����˾����Ϣ�豸����֤�豸����Ϣ�İ�ȫ���ṩ�ȶ�����ȫ����Ч��Ӧ�÷�����Ϲ�˾ս�Թ滮���ƶ���Ϣ����չ�ƻ��������֧��ҵ���ţ��ṩ���ʵĲ�Ʒ�ͷ���');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0008','0008','�����г���','������','2004-03-12','�Ϻ�','Ϊ��˾��Ʒ�߻�Ӫ�����������˾Ʒ��Ӱ�����������Ʒ�г�ռ�ж��������̡����������磬�ƶ���˾������ۼƻ��������ۻ���мල�͹�����������ҵ����Ϊ�ͻ��ṩ���ʲ�Ʒ�ͷ���');
insert into tbl_department values('3a8f8189297d71b001297d71b01a0009','0009','�ۺ����','���Ʒ�','2004-03-12','�Ϻ�','�ṩ��Ʒ�ͻ����񣬲�Ʒ��װ����Ʒ���ԣ���Ʒά�ޣ���Ʒ���������Ʒ�ۺ��������ṩ���ʵĲ�Ʒ�ۺ����');
insert into tbl_department values('3a8f8189297d71b001297d71b01a00010','0010','��ҵ���̲�','�����','2005-01-06','�Ϻ�','�滮�����ߣ��ṩ�����������������̣��ƶ���Ʒ�������չ��򣬲�Ʒ���������ֲᣬ�������䲼�ֹ����Ż��������չ����ṩ����Ч�ʣ��滮��ȫ������������Ʒ������������㲿���汾�����������̱�����������з��Ĳ�Ʒ��ҵ�����²�Ʒ�����߹滮��������WI��ѵ');
insert into tbl_department values('3a8f8189297d71b001297d71b01a00011','0011','�豸ά�޲�','��ƽ��','2005-01-06','����','����˾���й����豸��ά�������');
insert into tbl_department values('3a8f8189297d71b001297d71b01a00012','0012','������','������','2005-01-08','����','ά����˾��Ա����ȫ�����Ϲ�˾�Ʋ������ƻ�');

create table tbl_dept_pingjia(
	guid			varchar(50),
	pingjia			varchar(30)
);

insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0001','�ܺ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0002','�ܲ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0003','�Ϻ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0004','�ϲ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0005','�ܺ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0006','�Ϻ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0007','�Ϻ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0008','�ܺ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a0009','�Ϻ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a00010','�Ϻ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a00011','�ܲ�');
insert into tbl_dept_pingjia values('3a8f8189297d71b001297d71b01a00012','�ܺ�');

create table tbl_detailinfo(
	no		varchar(20),
	ename		varchar(30),
	salary		float,
	joinindate	date,
	province	varchar(30),
	city		varchar(30),
	county		varchar(30),
	marriage	int,
	interest	varchar(100),
	photo		varchar(100),
	description	text,
	jl		varchar(100),
	orderline	int
);

insert into tbl_detailinfo values('10001','songwenhua',1320.2,'2008-11-03','����','�ϲ�','����',1,'����,����','/WabacusDemo/wabacusdemo/uploadfile/2_chiay.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',101538111);
insert into tbl_detailinfo values('10002','zhouyuan',2345.32,'2003-02-21','�㶫','����','����',0,'����,����,�ܲ�','/WabacusDemo/wabacusdemo/uploadfile/2_claudxyz.jpg','�ó����������ֺ��У����ݶ���','',111538112);
insert into tbl_detailinfo values('10003','zhouhong',4323.23,'2005-03-23','����','Ȫ��','�ݰ�',0,'����,��ɽ,��Ӿ','/WabacusDemo/wabacusdemo/uploadfile/2_cuidenghong123.jpg','��͵��Լ�������Ͻ�','',121538113);
insert into tbl_detailinfo values('10004','huzhibo',6352,'2006-08-22','����','�˴�','�߰�',1,'����,����','/WabacusDemo/wabacusdemo/uploadfile/2_cxz003.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',131538114);
insert into tbl_detailinfo values('10005','huxiuqing',4367.2,'2005-06-01','����','Ȫ��','��Ϫ',0,'����,����,�ܲ�,����','/WabacusDemo/wabacusdemo/uploadfile/2_e_mi_tuo_fo.jpg','��͵��Լ�������Ͻ�','',141538115);
insert into tbl_detailinfo values('10006','tuqiying',5355.2,'2006-05-23','�㶫','��ݸ','����',0,'�ܲ�,����','/WabacusDemo/wabacusdemo/uploadfile/2_emon123.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',151538116);
insert into tbl_detailinfo values('10007','songjiebin',7434.34,'2002-11-03','����','�ϲ�','����',0,'�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_fanyuanwaifdl.jpg','��͵��Լ�������Ͻ�','',161538117);
insert into tbl_detailinfo values('10008','zhouyinglong',7436.5,'2000-10-19','����','Ȫ��','�ݰ�',0,'��Ӿ,����,����,','/WabacusDemo/wabacusdemo/uploadfile/2_fellowcheng.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',171538118);
insert into tbl_detailinfo values('10009','wushuqing',3455.3,'2009-11-15','����','����','�ٴ�',1,'����','/WabacusDemo/wabacusdemo/uploadfile/2_gaohenggaoheng.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',181538119);
insert into tbl_detailinfo values('10010','fanxinhua',3466.3,'2007-08-20','����','�ϲ�','����',0,'��Ӿ,����','/WabacusDemo/wabacusdemo/uploadfile/2_haiyong_sea.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',191538120);
insert into tbl_detailinfo values('10011','woguofa',3677.4,'2000-07-29','����','����','�ٴ�',0,'�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_huahua320.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',201538121);
insert into tbl_detailinfo values('10012','wuzhifeng',7637.3,'2002-08-23','�㶫','����','����',0,'��Ӿ,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_huiyaxiong.jpg','��͵��Լ�������Ͻ�','',211538122);
insert into tbl_detailinfo values('10013','fanjianqin',6346.3,'2006-03-23','����','�˴�','�߰�',0,'��ɽ,����','/WabacusDemo/wabacusdemo/uploadfile/2_hyblusea.jpg','�ó����������ֺ��У����ݶ���','',221538123);
insert into tbl_detailinfo values('10014','zhouyongwei',7598,'2004-07-15','����','Ȫ��','�ݰ�',0,'����,�ܲ�,����','/WabacusDemo/wabacusdemo/uploadfile/2_ivfangwang_long.jpg','�ó����������ֺ��У����ݶ���','',231538124);
insert into tbl_detailinfo values('10015','zhoujiehua',9346.3,'2000-05-07','�㶫','����','����',1,'����,�ܲ�,����','/WabacusDemo/wabacusdemo/uploadfile/2_jaffy.jpg','��͵��Լ�������Ͻ�','',241538125);
insert into tbl_detailinfo values('10016','fanyuanbo',7463.3,'2003-07-06','����','����','�ٴ�',0,'�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_javaalpha.jpg','��͵��Լ�������Ͻ�','',51538126);
insert into tbl_detailinfo values('10017','hudongqin',8998.2,'2008-06-05','����','�˴�','�ϸ�',0,'����,��ɽ,����','/WabacusDemo/wabacusdemo/uploadfile/2_jennyvenus.jpg','�ó����������ֺ��У����ݶ���','',281538127);
insert into tbl_detailinfo values('10018','zhouhong',7654.7,'2007-08-11','����','����','��̩',0,'����,��ɽ,����','/WabacusDemo/wabacusdemo/uploadfile/2_jjkodada.jpg','��͵��Լ�������Ͻ�','',271538128);
insert into tbl_detailinfo values('10019','wuzhiqing',6745.3,'2006-07-17','�㶫','����','��ɽ',1,'����,����','/WabacusDemo/wabacusdemo/uploadfile/2_kaiyekai.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',281538129);
insert into tbl_detailinfo values('10020','wuzhiguo',7655.4,'2005-08-20','�㶫','����','����',0,'����,��ɽ','/WabacusDemo/wabacusdemo/uploadfile/2_kakajay008.jpg','��͵��Լ�������Ͻ�','',291538130);
insert into tbl_detailinfo values('10021','wuqingshan',10080.3,'2005-06-15','����','����','����',0,'����,�ܲ�','/WabacusDemo/wabacusdemo/uploadfile/2_lbh119.jpg','��͵��Լ�������Ͻ�','',301538131);
insert into tbl_detailinfo values('10022','wangzhijian',2980.9,'2006-07-20','����','�˴�','�߰�',1,'����,����','/WabacusDemo/wabacusdemo/uploadfile/2_liufang12381.jpg','�ó����������ֺ��У����ݶ���','',311538132);
insert into tbl_detailinfo values('10023','wuliangguang',3800,'2007-03-20','�㶫','����','����',0,'����,����','/WabacusDemo/wabacusdemo/uploadfile/2_liuxk99.jpg','��͵��Լ�������Ͻ�','',321538134);
insert into tbl_detailinfo values('10024','huwenle',9890.3,'2006-03-23','�㶫','����','����',1,'����,��ɽ,��Ӿ,����','/WabacusDemo/wabacusdemo/uploadfile/2_luojianfeng.jpg','��͵��Լ�������Ͻ�','',331538135);
insert into tbl_detailinfo values('10025','hongliangliang',6551,'2008-04-21','����','Ȫ��','�ݰ�',1,'����,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_luxiaoshuai.jpg','�ó����������ֺ��У����ݶ���','',341538136);
insert into tbl_detailinfo values('10026','fuluiming',3455.3,'2003-06-22','����','����','����',1,'�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_lzj_freedom.jpg','��͵��Լ�������Ͻ�','',351538137);
insert into tbl_detailinfo values('10027','huzhiying',5764.3,'2009-07-12','����','�˴�','�ϸ�',0,'����,�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_minvt.jpg','�ó����������ֺ��У����ݶ���','',361538138);
insert into tbl_detailinfo values('10028','gaohongbo',9832.3,'2007-03-13','����','����','����',0,'����,����,�ܲ�','/WabacusDemo/wabacusdemo/uploadfile/2_pepeet.jpg','��͵��Լ�������Ͻ�','',371538139);
insert into tbl_detailinfo values('10029','liumingshu',4645,'2006-07-16','����','�ϲ�','�½�',0,'�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_pxh360395296.jpg','��͵��Լ�������Ͻ�','',381538140);
insert into tbl_detailinfo values('10030','huting',5789,'2004-11-02','�㶫','����','��ɽ',0,'��ɽ,��Ӿ,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_pzy0428.jpg','�ó����������ֺ��У����ݶ���','',391538141);
insert into tbl_detailinfo values('10031','wujianzhong',3456,'2007-12-25','����','����','��̩',0,'����,��ɽ,��Ӿ','/WabacusDemo/wabacusdemo/uploadfile/2_q107770540.jpg','��͵��Լ�������Ͻ�','',401538142);
insert into tbl_detailinfo values('10032','wuzhicheng',9253,'2003-04-10','����','�ϲ�','����',1,'����,��ɽ,����','/WabacusDemo/wabacusdemo/uploadfile/2_qiubojuncode.jpg','��͵��Լ�������Ͻ�','',411538143);
insert into tbl_detailinfo values('10033','shuweiguan',8992.5,'2000-10-16','�㶫','��ݸ','����',0,'����,����','/WabacusDemo/wabacusdemo/uploadfile/2_shineboyadh.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',421538144);
insert into tbl_detailinfo values('10034','wuyuane',7687,'2002-08-23','����','����','��̩',1,'����,��ɽ,����','/WabacusDemo/wabacusdemo/uploadfile/2_songpengasp.jpg','�ó����������ֺ��У����ݶ���','',431538145);
insert into tbl_detailinfo values('10035','wuzhihong',2336,'2006-01-22','����','�˴�','�߰�',0,'����,����,�ܲ�','/WabacusDemo/wabacusdemo/uploadfile/2_steptodream.jpg','��͵��Լ�������Ͻ�','',441538146);
insert into tbl_detailinfo values('10036','wanxingguo',8566.3,'2005-11-23','����','Ȫ��','��Ϫ',0,'��Ӿ,����','/WabacusDemo/wabacusdemo/uploadfile/2_stonekind.jpg','��͵��Լ�������Ͻ�','',451538147);
insert into tbl_detailinfo values('10037','fanguihong',3246,'2001-10-25','����','�ϲ�','�½�',1,'�ܲ�,����,����','/WabacusDemo/wabacusdemo/uploadfile/2_tashiwoweiyi.jpg','�ó����������ֺ��У����ݶ���','',461538148);
insert into tbl_detailinfo values('10038','songguozhen',9675,'2003-06-18','�㶫','��ݸ','����',1,'����,��ɽ,����','/WabacusDemo/wabacusdemo/uploadfile/2_teng_s2000.jpg','Ŭ���ڷܣ��Ͻ���ѧ','',471538149);


create table tbl_zhaoping(
	id		int auto_increment NOT NULL,
	zhiwei		varchar(30),
	salary		float,
	count		int,
	xueli		varchar(30),
	primary key (id)
);
insert into tbl_zhaoping values(null,'���繤��ʦ',5030,1,'��������');
insert into tbl_zhaoping values(null,'�������ʦ',6800,3,'��������');
insert into tbl_zhaoping values(null,'�߼����ʦ',8000,1,'˶ʿ����');
insert into tbl_zhaoping values(null,'���������ά������ʦ',4500,2,'ר������');
insert into tbl_zhaoping values(null,'�߻�����',8200,1,'��������');
insert into tbl_zhaoping values(null,'����������ʦ',5500,5,'��������');
insert into tbl_zhaoping values(null,'�չ�',1200,20,'��');
insert into tbl_zhaoping values(null,'��ʦ',3000,2,'��');
insert into tbl_zhaoping values(null,'��Ŀ����',8500,2,'��������');


create table tbl_zhaopingdesc(
	id		int  NOT NULL,
	yaoqiu		varchar(3000),
	description	varchar(3000)
);
insert into tbl_zhaopingdesc values(1,'����������ع������飬�����̿࣬�ܳ��ܹ���ѹ�����ܳ���','����˾����ܹ���ά���Լ�������ز�Ʒ�Ĳɹ�');
insert into tbl_zhaopingdesc values(2,'����������ع������飬ѧϰ����ǿ���ܳ��ܹ���ѹ�����ܳ���','����˾��������Ŀ�����ά��������ѵ��˾Ա��ʹ��');
insert into tbl_zhaopingdesc values(3,'����������ع������飬˼ά��Ծ�����渺��','����˾����');
insert into tbl_zhaopingdesc values(4,'�����̿࣬�ܳ��ܹ���ѹ�����ܳ���','����˾���õ��Խ��а�װ��ά��');
insert into tbl_zhaopingdesc values(5,'����������ع������飬˼ά��Ծ�����渺��������ǿ���׺���ǿ','����˾�²�Ʒ�Ĳ߻��Լ���˾���������֯�Ͳ߻�');
insert into tbl_zhaopingdesc values(6,'�����渺�𣬹����̿࣬�ܳ��ܹ���ѹ�����ܳ���','����˾��Ʒ�Ĳ���');
insert into tbl_zhaopingdesc values(7,'�����̿࣬�����ϸɣ��ܳ��ܹ���ѹ�������Ӱ���','����˾��Ʒ������');
insert into tbl_zhaopingdesc values(8,'����������ع������飬�����̿࣬������','����˾ʳ�ó�����');
insert into tbl_zhaopingdesc values(9,'����������ع������飬�������ǿ���׺���ǿ','�����쵼��˾��ƷС��Ŀ��������');

create table tbl_zhaoping2(
	id		int  NOT NULL,
	zhiwei		varchar(30),
	salary		float,
	count		int,
	xueli		varchar(30),
	primary key (id)
);
insert into tbl_zhaoping2 values(1,'���繤��ʦ2',5030,1,'��������');
insert into tbl_zhaoping2 values(2,'�������ʦ2',6800,3,'��������');
insert into tbl_zhaoping2 values(3,'�߼����ʦ2',8000,1,'˶ʿ����');
insert into tbl_zhaoping2 values(4,'���������ά������ʦ2',4500,2,'ר������');
insert into tbl_zhaoping2 values(5,'�߻�����2',8200,1,'��������');
insert into tbl_zhaoping2 values(6,'����������ʦ2',5500,5,'��������');
insert into tbl_zhaoping2 values(7,'�չ�2',1200,20,'��');
insert into tbl_zhaoping2 values(8,'��ʦ2',3000,2,'��');
insert into tbl_zhaoping2 values(9,'��Ŀ����2',8500,2,'��������');


create table tbl_zhaopingdesc2(
	id		int  NOT NULL,
	yaoqiu		varchar(3000),
	description	varchar(3000)
);
insert into tbl_zhaopingdesc2 values(1,'����������ع������飬�����̿࣬�ܳ��ܹ���ѹ�����ܳ���','����˾����ܹ���ά���Լ�������ز�Ʒ�Ĳɹ�');
insert into tbl_zhaopingdesc2 values(2,'����������ع������飬ѧϰ����ǿ���ܳ��ܹ���ѹ�����ܳ���','����˾��������Ŀ�����ά��������ѵ��˾Ա��ʹ��');
insert into tbl_zhaopingdesc2 values(3,'����������ع������飬˼ά��Ծ�����渺��','����˾����');
insert into tbl_zhaopingdesc2 values(4,'�����̿࣬�ܳ��ܹ���ѹ�����ܳ���','����˾���õ��Խ��а�װ��ά��');
insert into tbl_zhaopingdesc2 values(5,'����������ع������飬˼ά��Ծ�����渺��������ǿ���׺���ǿ','����˾�²�Ʒ�Ĳ߻��Լ���˾���������֯�Ͳ߻�');
insert into tbl_zhaopingdesc2 values(6,'�����渺�𣬹����̿࣬�ܳ��ܹ���ѹ�����ܳ���','����˾��Ʒ�Ĳ���');
insert into tbl_zhaopingdesc2 values(7,'�����̿࣬�����ϸɣ��ܳ��ܹ���ѹ�������Ӱ���','����˾��Ʒ������');
insert into tbl_zhaopingdesc2 values(8,'����������ع������飬�����̿࣬������','����˾ʳ�ó�����');
insert into tbl_zhaopingdesc2 values(9,'����������ع������飬�������ǿ���׺���ǿ','�����쵼��˾��ƷС��Ŀ��������');

CREATE TABLE tbl_salary2 (
	no varchar(50),
	year int null,
	jan float null,
	feb float NULL,
	mar float NULL,
	apr float NULL,
	may float NULL,
	june float NULL,
	july float NULL,
	aug float NULL,
	sep float NULL,
	oct float NULL,
	nov float NULL,
	dece float NULL
);

insert into tbl_salary2 values('10001',2008,7575.3,6757,2426.2,2964.3,2422.5,4644.5,5234.2,7675.2,7680,8676,4744,6575.3);
insert into tbl_salary2 values('10002',2008,3543.3,6535,3433.2,4235.3,2454.5,5775.5,2335.2,6444.2,5467,5675,7568,5646.3);
insert into tbl_salary2 values('10003',2008,7867.3,5475,5463.2,1124.3,3532.5,4470.5,5346.2,6489.2,4469,5678,5458,5464.3);
insert into tbl_salary2 values('10004',2008,5346.3,6887,3535.2,1421.3,2453.5,10755.5,2422.2,3565.2,3567,5647,6857,3533.3);
insert into tbl_salary2 values('10005',2008,7689.3,3456,5357.2,3414.3,4663.5,6355.5,5435.2,5635.2,4653,6587,4364,3535.3);
insert into tbl_salary2 values('10006',2008,4355.3,3444,4645.2,4212.3,4278.5,2378.5,5343.2,5689.2,5754,6758,5675,4522.3);
insert into tbl_salary2 values('10007',2008,7870.3,7565,3535.2,5353.3,3453.5,12786.5,3536.2,6675.2,2367,5867,8966,3246.3);
insert into tbl_salary2 values('10008',2008,8967.3,4555,5354.2,3808.3,4257.5,2757.5,6444.2,6578.2,3424,7865,4564,3566.3);
insert into tbl_salary2 values('10009',2008,8656.3,7699,4356.2,7865.3,3535.5,8653.5,3535.2,7568.2,1954,5758,4654,5424.3);
insert into tbl_salary2 values('10010',2008,2243.3,5466,3464.2,6448.3,5362.5,3787.5,2345.2,7647.2,7576,7478,5657,4263.3);
insert into tbl_salary2 values('10011',2008,5798.3,3453,6543.2,5757.3,2346.5,5376.5,3424.2,6478.2,3556,4767,6347,3456.3);
insert into tbl_salary2 values('10012',2008,7867.3,6768,3536.2,9766.3,5423.5,8675.5,4647.2,5474.2,7555,5689,4643,5365.3);

insert into tbl_salary2 values('10001',2009,8980.3,6789,7546.2,5464.3,2235.5,23453.5,6789.2,7866.2,9566,8676,6545,5646.3);
insert into tbl_salary2 values('10002',2009,8690.3,8866,3564.2,3432.3,2543.5,1276.5,4326.2,6458.2,7568,6746,7655,5464.3);
insert into tbl_salary2 values('10003',2009,5460.3,6475,3567.2,5353.3,2144.5,1256.5,8553.2,5690.2,5674,5867,4568,7866.3);
insert into tbl_salary2 values('10004',2009,2425.3,5768,8955.2,3357.3,2131.5,4786.5,5236.2,5757.2,4687,8696,7968,2355.3);
insert into tbl_salary2 values('10005',2009,3543.3,5457,8865.2,5333.3,2467.5,1576.5,1435.2,4633.2,6548,5675,2545,5422.3);
insert into tbl_salary2 values('10006',2009,7690.3,8708,5346.2,3454.3,2425.5,5456.5,5322.2,7988.2,4467,8796,6799,2422.3);
insert into tbl_salary2 values('10007',2009,5375.3,7797,7586.2,5464.3,5472.5,2435.5,2489.2,6759.2,6778,6765,69679,4224.3);
insert into tbl_salary2 values('10008',2009,6775.3,5688,8686.2,5467.3,2437.5,4669.5,3649.2,6570.2,6347,4765,5796,3535.3);
insert into tbl_salary2 values('10009',2009,2454.3,9977,4644.2,5353.3,3533.5,4568.5,4646.2,6757.2,5686,5889,5758,3542.3);
insert into tbl_salary2 values('10010',2009,5646.3,4575,4678.2,7567.3,2342.5,9564.5,5648.2,3654.2,5465,5856,5745,2425.3);
insert into tbl_salary2 values('10011',2009,3586.3,6842,7885.2,6545.3,2425.5,4366.5,7853.2,8066.2,7686,5787,5678,2635.3);
insert into tbl_salary2 values('10012',2009,6678.3,6478,5478.2,7956.3,5332.5,4697.5,3256.2,4589.2,6890,6467,7689,5648.3);



CREATE TABLE tbl_salary (
	no varchar(50),
	year int null,
	month int null,
	salary float NULL 
);

insert into tbl_salary values('10001',2008,1,3543.3);
insert into tbl_salary values('10001',2008,2,3643.3);
insert into tbl_salary values('10001',2008,3,3573.2);
insert into tbl_salary values('10001',2008,4,4543.3);
insert into tbl_salary values('10001',2008,5,3943.8);
insert into tbl_salary values('10001',2008,6,3547.3);
insert into tbl_salary values('10001',2008,7,3549.3);
insert into tbl_salary values('10001',2008,8,3543.0);
insert into tbl_salary values('10001',2008,9,3544.3);
insert into tbl_salary values('10001',2008,10,3343.6);
insert into tbl_salary values('10001',2008,11,3143.4);
insert into tbl_salary values('10001',2008,12,3943.3);

insert into tbl_salary values('10002',2008,1,6343.3);
insert into tbl_salary values('10002',2008,2,6743.3);
insert into tbl_salary values('10002',2008,3,6843.3);
insert into tbl_salary values('10002',2008,4,6543.45);
insert into tbl_salary values('10002',2008,5,6342.0);
insert into tbl_salary values('10002',2008,6,6443.3);
insert into tbl_salary values('10002',2008,7,6541.3);
insert into tbl_salary values('10002',2008,8,6643.5);
insert into tbl_salary values('10002',2008,9,6844.3);
insert into tbl_salary values('10002',2008,10,7573.3);
insert into tbl_salary values('10002',2008,11,4593.7);
insert into tbl_salary values('10002',2008,12,6545.13);

insert into tbl_salary values('10003',2008,1,5463.13);
insert into tbl_salary values('10003',2008,2,5142.2);
insert into tbl_salary values('10003',2008,3,5343.3);
insert into tbl_salary values('10003',2008,4,5543.7);
insert into tbl_salary values('10003',2008,5,5596.3);
insert into tbl_salary values('10003',2008,6,5543.3);
insert into tbl_salary values('10003',2008,7,5643.3);
insert into tbl_salary values('10003',2008,8,5579.9);
insert into tbl_salary values('10003',2008,9,5543.3);
insert into tbl_salary values('10003',2008,10,5540.3);
insert into tbl_salary values('10003',2008,11,5583.3);
insert into tbl_salary values('10003',2008,12,5841.0);

insert into tbl_salary values('10004',2008,1,1583.6);
insert into tbl_salary values('10004',2008,2,1842.3);
insert into tbl_salary values('10004',2008,3,1563.8);
insert into tbl_salary values('10004',2008,4,1443.3);
insert into tbl_salary values('10004',2008,5,1508.3);
insert into tbl_salary values('10004',2008,6,1543.3);
insert into tbl_salary values('10004',2008,7,1643.9);
insert into tbl_salary values('10004',2008,8,1239.3);
insert into tbl_salary values('10004',2008,9,1743.3);
insert into tbl_salary values('10004',2008,10,1553.3);
insert into tbl_salary values('10004',2008,11,1943.5);
insert into tbl_salary values('10004',2008,12,1812.3);

insert into tbl_salary values('10005',2008,1,3542.2);
insert into tbl_salary values('10005',2008,2,3713.3);
insert into tbl_salary values('10005',2008,3,3541.5);
insert into tbl_salary values('10005',2008,4,3843.3);
insert into tbl_salary values('10005',2008,5,3513.3);
insert into tbl_salary values('10005',2008,6,3040.7);
insert into tbl_salary values('10005',2008,7,3543.3);
insert into tbl_salary values('10005',2008,8,3523.2);
insert into tbl_salary values('10005',2008,9,3243.3);
insert into tbl_salary values('10005',2008,10,3527.3);
insert into tbl_salary values('10005',2008,11,3143.2);
insert into tbl_salary values('10005',2008,12,3549.3);

insert into tbl_salary values('10006',2008,1,7543.3);
insert into tbl_salary values('10006',2008,2,7570.2);
insert into tbl_salary values('10006',2008,3,7543.3);
insert into tbl_salary values('10006',2008,4,7518.6);
insert into tbl_salary values('10006',2008,5,7543.3);
insert into tbl_salary values('10006',2008,6,7543.2);
insert into tbl_salary values('10006',2008,7,7593.3);
insert into tbl_salary values('10006',2008,8,7543.3);
insert into tbl_salary values('10006',2008,9,7549.3);
insert into tbl_salary values('10006',2008,10,7563.6);
insert into tbl_salary values('10006',2008,11,7540.3);
insert into tbl_salary values('10006',2008,12,7510.1);

insert into tbl_salary values('10007',2008,1,6519.1);
insert into tbl_salary values('10007',2008,2,6343.3);
insert into tbl_salary values('10007',2008,3,6547.5);
insert into tbl_salary values('10007',2008,4,6753.3);
insert into tbl_salary values('10007',2008,5,6743.3);
insert into tbl_salary values('10007',2008,6,6541.7);
insert into tbl_salary values('10007',2008,7,6993.3);
insert into tbl_salary values('10007',2008,8,6507.3);
insert into tbl_salary values('10007',2008,9,6043.8);
insert into tbl_salary values('10007',2008,10,6943.3);
insert into tbl_salary values('10007',2008,11,6903.8);
insert into tbl_salary values('10007',2008,12,6213.5);


insert into tbl_salary values('10008',2008,1,3569.23);
insert into tbl_salary values('10008',2008,2,3643.3);
insert into tbl_salary values('10008',2008,3,3529.8);
insert into tbl_salary values('10008',2008,4,3523.3);
insert into tbl_salary values('10008',2008,5,3923.8);
insert into tbl_salary values('10008',2008,6,3544.3);
insert into tbl_salary values('10008',2008,7,3543.7);
insert into tbl_salary values('10008',2008,8,3013.3);
insert into tbl_salary values('10008',2008,9,3519.9);
insert into tbl_salary values('10008',2008,10,3313.0);
insert into tbl_salary values('10008',2008,11,3153.0);
insert into tbl_salary values('10008',2008,12,3189.8);

insert into tbl_salary values('10009',2008,1,2829.2);
insert into tbl_salary values('10009',2008,2,2543.3);
insert into tbl_salary values('10009',2008,3,2610.3);
insert into tbl_salary values('10009',2008,4,2542.5);
insert into tbl_salary values('10009',2008,5,2433.3);
insert into tbl_salary values('10009',2008,6,2543.3);
insert into tbl_salary values('10009',2008,7,2538.3);
insert into tbl_salary values('10009',2008,8,2835.6);
insert into tbl_salary values('10009',2008,9,2518.3);
insert into tbl_salary values('10009',2008,10,2557.36);
insert into tbl_salary values('10009',2008,11,2913.3);
insert into tbl_salary values('10009',2008,12,2915.35);

insert into tbl_salary values('10010',2008,1,10546.2);
insert into tbl_salary values('10010',2008,2,10513.3);
insert into tbl_salary values('10010',2008,3,10240.3);
insert into tbl_salary values('10010',2008,4,10543.4);
insert into tbl_salary values('10010',2008,5,10243.3);
insert into tbl_salary values('10010',2008,6,10592.3);
insert into tbl_salary values('10010',2008,7,10640.2);
insert into tbl_salary values('10010',2008,8,10540.3);
insert into tbl_salary values('10010',2008,9,10833.3);
insert into tbl_salary values('10010',2008,10,10543.3);
insert into tbl_salary values('10010',2008,11,10040.3);
insert into tbl_salary values('10010',2008,12,10801.6);

insert into tbl_salary values('10011',2008,1,2830.0);
insert into tbl_salary values('10011',2008,2,2543.3);
insert into tbl_salary values('10011',2008,3,2122.5);
insert into tbl_salary values('10011',2008,4,2543.3);
insert into tbl_salary values('10011',2008,5,1912.6);
insert into tbl_salary values('10011',2008,6,2143.3);
insert into tbl_salary values('10011',2008,7,2508.8);
insert into tbl_salary values('10011',2008,8,2943.3);
insert into tbl_salary values('10011',2008,9,2505.8);
insert into tbl_salary values('10011',2008,10,2703.7);
insert into tbl_salary values('10011',2008,11,2216.6);
insert into tbl_salary values('10011',2008,12,2731.5);

insert into tbl_salary values('10012',2008,1,12768.6);
insert into tbl_salary values('10012',2008,2,12643.2);
insert into tbl_salary values('10012',2008,3,12471.3);
insert into tbl_salary values('10012',2008,4,12143.0);
insert into tbl_salary values('10012',2008,5,12222.3);
insert into tbl_salary values('10012',2008,6,12243.0);
insert into tbl_salary values('10012',2008,7,12312.3);
insert into tbl_salary values('10012',2008,8,12043.0);
insert into tbl_salary values('10012',2008,9,12843.3);
insert into tbl_salary values('10012',2008,10,12510.2);
insert into tbl_salary values('10012',2008,11,12643.1);
insert into tbl_salary values('10012',2008,12,12560.8);

insert into tbl_salary values('10001',2009,1,5546.3);
insert into tbl_salary values('10001',2009,2,5523.3);
insert into tbl_salary values('10001',2009,3,5543.3);
insert into tbl_salary values('10001',2009,4,5518.1);
insert into tbl_salary values('10001',2009,5,5643.1);
insert into tbl_salary values('10001',2009,6,5543.3);
insert into tbl_salary values('10001',2009,7,5943.2);
insert into tbl_salary values('10001',2009,8,5549.3);
insert into tbl_salary values('10001',2009,9,5443.3);
insert into tbl_salary values('10001',2009,10,5523.8);
insert into tbl_salary values('10001',2009,11,6543.3);
insert into tbl_salary values('10001',2009,12,5518.9);
                                          
insert into tbl_salary values('10002',2009,1,8233.3);
insert into tbl_salary values('10002',2009,2,8343.13);
insert into tbl_salary values('10002',2009,3,8633.23);
insert into tbl_salary values('10002',2009,4,8723.3);
insert into tbl_salary values('10002',2009,5,8413.34);
insert into tbl_salary values('10002',2009,6,8533.62);
insert into tbl_salary values('10002',2009,7,8353.3);
insert into tbl_salary values('10002',2009,8,8283.3);
insert into tbl_salary values('10002',2009,9,8143.3);
insert into tbl_salary values('10002',2009,10,8673.8);
insert into tbl_salary values('10002',2009,11,8723.9);
insert into tbl_salary values('10002',2009,12,8833.31);
                                          
insert into tbl_salary values('10003',2009,1,6703.3);
insert into tbl_salary values('10003',2009,2,6542.5);
insert into tbl_salary values('10003',2009,3,6593.3);
insert into tbl_salary values('10003',2009,4,6843.3);
insert into tbl_salary values('10003',2009,5,6541.13);
insert into tbl_salary values('10003',2009,6,6543.3);
insert into tbl_salary values('10003',2009,7,6533.0);
insert into tbl_salary values('10003',2009,8,6571.3);
insert into tbl_salary values('10003',2009,9,6243.3);
insert into tbl_salary values('10003',2009,10,6241.8);
insert into tbl_salary values('10003',2009,11,6573.3);
insert into tbl_salary values('10003',2009,12,6241.9);
                                          
insert into tbl_salary values('10004',2009,1,2591.2);
insert into tbl_salary values('10004',2009,2,2643.3);
insert into tbl_salary values('10004',2009,3,2986.2);
insert into tbl_salary values('10004',2009,4,2543.3);
insert into tbl_salary values('10004',2009,5,2848.3);
insert into tbl_salary values('10004',2009,6,2543.7);
insert into tbl_salary values('10004',2009,7,26473.3);
insert into tbl_salary values('10004',2009,8,2549.3);
insert into tbl_salary values('10004',2009,9,2353.2);
insert into tbl_salary values('10004',2009,10,2543.3);
insert into tbl_salary values('10004',2009,11,2213.3);
insert into tbl_salary values('10004',2009,12,2142.1);
                                         
insert into tbl_salary values('10005',2009,1,3811.2);
insert into tbl_salary values('10005',2009,2,3841.3);
insert into tbl_salary values('10005',2009,3,3842.3);
insert into tbl_salary values('10005',2009,4,3553.3);
insert into tbl_salary values('10005',2009,5,3847.3);
insert into tbl_salary values('10005',2009,6,3543.7);
insert into tbl_salary values('10005',2009,7,3542.3);
insert into tbl_salary values('10005',2009,8,3863.3);
insert into tbl_salary values('10005',2009,9,3541.2);
insert into tbl_salary values('10005',2009,10,3543.3);
insert into tbl_salary values('10005',2009,11,3743.3);
insert into tbl_salary values('10005',2009,12,3961.1);
                                          
insert into tbl_salary values('10006',2009,1,7805.2);
insert into tbl_salary values('10006',2009,2,7803.3);
insert into tbl_salary values('10006',2009,3,7873.3);
insert into tbl_salary values('10006',2009,4,7883.8);
insert into tbl_salary values('10006',2009,5,7865.3);
insert into tbl_salary values('10006',2009,6,7843.3);
insert into tbl_salary values('10006',2009,7,782.4);
insert into tbl_salary values('10006',2009,8,7841.3);
insert into tbl_salary values('10006',2009,9,7843.1);
insert into tbl_salary values('10006',2009,10,7823.3);
insert into tbl_salary values('10006',2009,11,7843.1);
insert into tbl_salary values('10006',2009,12,7810.3);
                                          
insert into tbl_salary values('10007',2009,1,7519.1);
insert into tbl_salary values('10007',2009,2,7348.3);
insert into tbl_salary values('10007',2009,3,7547.5);
insert into tbl_salary values('10007',2009,4,7558.4);
insert into tbl_salary values('10007',2009,5,7443.8);
insert into tbl_salary values('10007',2009,6,7541.7);
insert into tbl_salary values('10007',2009,7,7995.3);
insert into tbl_salary values('10007',2009,8,7507.3);
insert into tbl_salary values('10007',2009,9,7043.2);
insert into tbl_salary values('10007',2009,10,7943.2);
insert into tbl_salary values('10007',2009,11,7903.8);
insert into tbl_salary values('10007',2009,12,7215.1);
                                   
insert into tbl_salary values('10008',2009,1,4169.23);
insert into tbl_salary values('10008',2009,2,4243.3);
insert into tbl_salary values('10008',2009,3,4529.8);
insert into tbl_salary values('10008',2009,4,3927.3);
insert into tbl_salary values('10008',2009,5,4783.8);
insert into tbl_salary values('10008',2009,6,3844.3);
insert into tbl_salary values('10008',2009,7,4543.7);
insert into tbl_salary values('10008',2009,8,4313.3);
insert into tbl_salary values('10008',2009,9,4519.9);
insert into tbl_salary values('10008',2009,10,3813.2);
insert into tbl_salary values('10008',2009,11,4453.2);
insert into tbl_salary values('10008',2009,12,4189.6);
                                          
insert into tbl_salary values('10009',2009,1,3821.2);
insert into tbl_salary values('10009',2009,2,3543.0);
insert into tbl_salary values('10009',2009,3,3115.3);
insert into tbl_salary values('10009',2009,4,3542.5);
insert into tbl_salary values('10009',2009,5,3233.8);
insert into tbl_salary values('10009',2009,6,3545.3);
insert into tbl_salary values('10009',2009,7,2538.8);
insert into tbl_salary values('10009',2009,8,2935.6);
insert into tbl_salary values('10009',2009,9,3513.1);
insert into tbl_salary values('10009',2009,10,2557.9);
insert into tbl_salary values('10009',2009,11,3013.3);
insert into tbl_salary values('10009',2009,12,3016.8);
                                          
insert into tbl_salary values('10010',2009,1,11546.2);
insert into tbl_salary values('10010',2009,2,11513.1);
insert into tbl_salary values('10010',2009,3,11240.0);
insert into tbl_salary values('10010',2009,4,10943.4);
insert into tbl_salary values('10010',2009,5,10843.7);
insert into tbl_salary values('10010',2009,6,11592.3);
insert into tbl_salary values('10010',2009,7,10646.2);
insert into tbl_salary values('10010',2009,8,10930.1);
insert into tbl_salary values('10010',2009,9,10938.2);
insert into tbl_salary values('10010',2009,10,11523.6);
insert into tbl_salary values('10010',2009,11,10940.3);
insert into tbl_salary values('10010',2009,12,11209.8);
                                          
insert into tbl_salary values('10011',2009,1,3138.0);
insert into tbl_salary values('10011',2009,2,3043.3);
insert into tbl_salary values('10011',2009,3,3362.5);
insert into tbl_salary values('10011',2009,4,3245.6);
insert into tbl_salary values('10011',2009,5,3312.7);
insert into tbl_salary values('10011',2009,6,3547.2);
insert into tbl_salary values('10011',2009,7,3608.4);
insert into tbl_salary values('10011',2009,8,3042.1);
insert into tbl_salary values('10011',2009,9,3105.2);
insert into tbl_salary values('10011',2009,10,2902.0);
insert into tbl_salary values('10011',2009,11,3216.3);
insert into tbl_salary values('10011',2009,12,3035.6);
                                          
insert into tbl_salary values('10012',2009,1,13162.3);
insert into tbl_salary values('10012',2009,2,13243.5);
insert into tbl_salary values('10012',2009,3,13178.5);
insert into tbl_salary values('10012',2009,4,13343.5);
insert into tbl_salary values('10012',2009,5,13128.8);
insert into tbl_salary values('10012',2009,6,13043.8);
insert into tbl_salary values('10012',2009,7,13612.3);
insert into tbl_salary values('10012',2009,8,12546.3);
insert into tbl_salary values('10012',2009,9,13345.2);
insert into tbl_salary values('10012',2009,10,13119.1);
insert into tbl_salary values('10012',2009,11,13049.8);
insert into tbl_salary values('10012',2009,12,13566.3);

create table tbl_log(
	uuid		varchar(50),
	createdate	datetime,
	content		varchar(200),
	updatetype	varchar(20)
);

create table tbl_testimport1(
	no		varchar(20),	
	name		varchar(30),
	age		int,
	birthday	date,
	salary		float
);

create table tbl_testimport2(
	no		varchar(20),	
	description	text
);
commit;

DELIMITER $$ ;

DROP PROCEDURE IF EXISTS `WabacusDemoDB`.`sp_testInsertData`$$

CREATE  PROCEDURE `sp_testInsertData`(
    in_uuid varchar(50),
    in_deptno varchar(20),
    in_deptname varchar(30),
    in_manager varchar(30),
    in_builtdate datetime,
    in_performance varchar(20),
    in_description varchar(3000),
    out out_rtnValue varchar(500)
)
BEGIN
    insert into tbl_department(guid,deptno,deptname,manager,builtdate,performance,description) values (in_uuid,in_deptno,in_deptname,in_manager,in_builtdate,in_performance,in_description);
    set out_rtnValue=concat('��ӹ��ţ�',in_deptno,'��������',in_deptname,'�ļ�¼�ɹ�');
END$$


DROP PROCEDURE IF EXISTS `WabacusDemoDB`.`sp_testUpdateData`$$

CREATE PROCEDURE `sp_testUpdateData`(
    in_deptno varchar(20),
    in_deptno_old varchar(20),
    in_manager varchar(30),
    in_builtdate datetime,
    in_performance varchar(20),
    in_description varchar(3000),
    out out_rtnValue varchar(50)
)
BEGIN
    update tbl_department set deptno=in_deptno,manager=in_manager,builtdate=in_builtdate,performance=in_performance,description=in_description where deptno=in_deptno_old;
    set out_rtnValue=concat('�޸Ĺ��ţ�',in_deptno_old,'�ļ�¼�ɹ�');
END$$

DROP PROCEDURE IF EXISTS `WabacusDemoDB`.`sp_testDeleteData`$$

CREATE  PROCEDURE `sp_testDeleteData`(
    in_deptno varchar(20)
)
BEGIN
     delete from tbl_department where deptno=in_deptno;
END$$

DROP PROCEDURE IF EXISTS `WabacusDemoDB`.`sp_testInvokeServerSP`$$

CREATE PROCEDURE `sp_testInvokeServerSP`(
    in_uuid varchar(50),
    in_now datetime,
    in_param_no varchar(50),
    in_content varchar(300)
)
BEGIN
    update tbl_detailinfo set marriage=0 where no=in_param_no;
    insert into tbl_log(uuid,createdate,content,updatetype)values(in_uuid,in_now,in_content,'����Ϊδ��״̬');
END$$

DROP PROCEDURE IF EXISTS sp_selectEmployeeData$$

CREATE  PROCEDURE sp_selectEmployeeData(
    txtno varchar(20),
    txtprovince varchar(30),
    txtage varchar(30),
    i_systeminfo varchar(3000)
)
BEGIN
    declare var_sql varchar(1000);    
    set var_sql='SELECT A.no,name,ename,sex,age,salary,marriage,birthday,joinindate,province,city,county,description FROM tbl_baseinfo A left join tbl_detailinfo B on A.no=B.no where  1=1';
    if txtno<>'' then set var_sql=concat(var_sql,' and A.no like  \'%',txtno,'%\''); end if;
    if txtprovince<>'' then set var_sql=concat(var_sql,' and province like  \'%',txtprovince,'%\''); end if;
    if txtage<>'' then set var_sql=concat(var_sql,' and A.age >=',txtage); end if;
    call SP_WABACUS_EXECUTE (var_sql,'no,name desc',i_systeminfo);
END$$