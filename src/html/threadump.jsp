<%@ page language="java" import="java.util.*,java.lang.Thread.*" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="CACHE-CONTROL" content="NO-CACHE">
<title>WebFolder: HTTP headers and Thread Dump</title>
</head>
<body>
<h2> HTTP headers</h2>
<table  width="100%" border="1">
<%
	Enumeration rhe = request.getHeaderNames();
	while (rhe.hasMoreElements()) {
		String headername = (String) rhe.nextElement();
		out.print("<tr><td>");
		out.print(headername);
		out.print("</td><td>");
		Enumeration hve = request.getHeaders(headername);
		while (hve.hasMoreElements()) {
			String headerValue = (String) hve.nextElement();
			out.print(headerValue);
			out.print("&nbsp;");
		}
		out.print("</td></tr>");
	}
%>
 </table>
<table width="100%" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
  <tr>
     <td bgcolor="#E7E7EF" bordercolor="#000000" align="center" nowrap>
	      <font face="Verdana" size="+1">Thread Dump&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
	</td>
  </tr>
  <tr>
	<td bgcolor="#E7E7EF" bordercolor="#000000">
	
---------------------- Java Thread Dump----------------------------------<br>
Generating Thread-Dump At:&nbsp;<%=new java.util.Date()%><BR>
-------------------------------------------------------------------------<br>
<%
	Map map = Thread.getAllStackTraces();

	Iterator itr = map.keySet().iterator();
	while (itr.hasNext()) {
		Thread t = (Thread) itr.next();
		StackTraceElement[] elem = (StackTraceElement[]) map.get(t);

		out.print("\"" + t.getName() + "\"");
		out.print(" Priority=" + t.getPriority());
		out.print(" Thread Id=" + t.getId());
		State s = t.getState();
		String state = null;
		String color = "000000";
		String GREEN = "00FF00";
		String RED = "FF0000";
		String ORANGE = "FCA742";
		switch (s) {
		case NEW:
			state = "NEW";
			color = GREEN;
			break;
		case BLOCKED:
			state = "BLOCKED";
			color = RED;
			break;
		case RUNNABLE:
			state = "RUNNABLE";
			color = GREEN;
			break;
		case TERMINATED:
			state = "TERMINATED";
			break;
		case TIMED_WAITING:
			state = "TIME WAITING";
			color = ORANGE;
			break;
		case WAITING:
			state = "WAITING";
			color = RED;
			break;
		}
		out.print(" In State :");
		out.print(" <font color=\"");
		out.print(color);
		out.print("\">");
		out.print(state);
		out.print("</font><BR>");
		for (int i = 0; i < elem.length; i++) {
			out.println("  at ");
			out.print(elem[i].toString());
			out.println("<BR>");
		}
		out.println("--------------------------------------------------------------------------<br>");
	}
	String going_back = request.getParameter("going_back");
	if (going_back == null)
		going_back = request.getHeader("referer");
%>
-----------------------End Java Thread Dump--------------------------------<br>
   </td>
  </tr>
 </table>
   <div><a href="<%=going_back%>">Go back</a></div>
   <jsp:include page="/WEB-INF/view/insert/copyright.htmt"/>
</body>
</html>
