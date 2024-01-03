<%@ page import="java.util.Properties"  %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
 <title>WebFolder: System properties info</title>
 <style>
     <jsp:include page="css/style.css" />
</style>
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
<%--
<%@ include file="copyright.jsp" %>

--%>

<div style="padding-top:1em;font-size: small;"><%=((Properties)application.getAttribute(org.aldan3.servlet.Constant.ALDAN3_CONFIG)).getProperty("app_name")%> 
   <%=((Properties)application.getAttribute(org.aldan3.servlet.Constant.ALDAN3_CONFIG)).getProperty("version")%> &copy; <%=msn.javaarchitect.webfolder.ctrl.Behavior.year()%> Dmitriy Rogatkin</div>
</body>
</html>