var tmp=0;
function testBeforeSaveCallBack(pageid,reportid,dataObjArr)
{
	alert('将保存页面ID:::'+pageid+';;;报表ID:::'+reportid);
	printSavingData(dataObjArr);
	tmp++;
	if(tmp%2===0)
	{
		alert('本次中断保存数据操作');
		return WX_SAVE_TERMINAT;
	}
	alert('本次执行保存数据操作');
	return WX_SAVE_CONTINUE;
}

function testAfterSaveCallback(paramsObj)
{
	alert('已保存的页面ID:::'+paramsObj.pageid+';;;报表ID:::'+paramsObj.reportid);
	var reportguid=getComponentGuidById(paramsObj.pageid,paramsObj.reportid);
	var dataObjArr=WX_ALL_SAVEING_DATA[reportguid];//得到已保存的数据
	printSavingData(dataObjArr);
}

function printSavingData(dataObjArr)
{
	var dataObjTmp;
	for(var i=0;i<dataObjArr.length;i++)
	{
		dataObjTmp=dataObjArr[i];
		alert('本条记录的操作类型：'+dataObjTmp['WX_TYPE']);
		var rowdata='';
		for(var key in dataObjTmp)
		{
			rowdata=rowdata+key+':::'+dataObjTmp[key]+';;;';
		}
		alert(rowdata);
		//dataObjTmp['age']='100';
	}
}

function testonload(pageid,reportid)
{
	alert('加载完报表页面ID：'+pageid+'；报表ID：'+reportid);
}

function validateRedundantboxpage1BirthdayInput(pageid,reportid,dataObjArr)
{
	for(var i=0;i<dataObjArr.length;i++)
	{
		dataObjTmp=dataObjArr[i];
		if(dataObjTmp['WX_TYPE']=='delete') continue;//当前是在做删除操作
		var birthday=dataObjTmp['birthday'];
		var birthday2=dataObjTmp['birthday2'];
		if(birthday!=birthday2)
		{//两次输入的出生日期不一致
			wx_alert('保存时两次输入的出生日期不一致');
			return WX_SAVE_TERMINAT;
		}
	}
	return WX_SAVE_CONTINUE;
}

function testBeforeSearch(pageid,reportid,searchurl)
{
	alert('执行查询数据的URL：'+searchurl);
	//开发人员可以修改这里的URL中的参数，然后返回修改后的URL。如果返回null，则中断本次查询操作
	return searchurl;
}