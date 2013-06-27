<%@ page language="java" contentType="text/html;charset=UTF-8"%>
<%@ page import="com.wabacus.system.component.application.report.abstractreport.AbsReportType"%>
<%@ taglib uri="wabacus" prefix="wx"%>

<hr>
<%
	AbsReportType reportTypeObj=(AbsReportType)request.getAttribute("WX_COMPONENT_OBJ");//取到报表对象
%>
<b>报表<%=reportTypeObj.getReportBean().getId() %>起始内容</b>
<hr>
	<wx:component/>
<hr>
  <b>报表<%=reportTypeObj.getReportBean().getId() %>结束内容</b>
<hr>



