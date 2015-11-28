<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
 <title>WebFolder: System properties info</title>
</head>
<body>
<h2>System properties</h2>
<%@ page import="java.util.Map.Entry"  %>
<table>
  <tr><th>Property</th><th>Value</th></tr>
<%

for(Entry<Object,Object> entry:System.getProperties().entrySet()) {
	out.print("<tr><td>");
	out.print(entry.getKey());
	out.print("</td><td>");
	out.print(entry.getValue());
	out.print("</td></tr>");
}

for (Entry<String,String> entry:System.getenv().entrySet()) {
	out.print("<tr><td>");
	out.print(entry.getKey());
	out.print("</td><td>");
	out.print(entry.getValue());
	out.print("</td></tr>");
}
String going_back = request.getParameter("going_back");
if (going_back == null)
	going_back = request.getHeader("referer");
%>
</table>
<hr />
<div><a href="<%=going_back %>">Go back</a></div>
<jsp:include page="/WEB-INF/view/insert/copyright.htmt"/>
</body>
</html>