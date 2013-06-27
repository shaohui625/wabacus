<%@ page language="java" contentType="text/html;charset=UTF-8"%>
<%@ page import="com.wabacus.system.*,com.wabacus.system.component.application.report.abstractreport.AbsReportType,java.util.*,com.wabacus.system.assistant.*"%>
<%@ taglib uri="wabacus" prefix="wx"%>

<hr>
<%
	ReportRequest rrequest=(ReportRequest)request.getAttribute("WX_REPORTREQUEST");
	AbsReportType reportTypeObj=(AbsReportType)request.getAttribute("WX_COMPONENT_OBJ");//取到报表对象
	/*List lstData=reportTypeObj.getLstReportData();
	for(int i=0;i<lstData.size();i++)
	{
		System.out.println(ReportDataAssistant.getInstance().getColValue(rrequest,"report1","name",lstData.get(i)));
	}*/
%>
<b>报表<%=reportTypeObj.getReportBean().getId() %>起始内容</b>
<hr>
	<wx:component/>
<hr>
  <b>报表<%=reportTypeObj.getReportBean().getId() %>结束内容</b>
<hr>



