<%@ page import="java.util.Properties" %>
<div style="padding-top:1em;font-size: small;margin: 0 auto;text-align:center;">
   <%=((Properties)application.getAttribute(org.aldan3.servlet.Constant.ALDAN3_CONFIG)).getProperty("app_name")%> 
   <%=((Properties)application.getAttribute(org.aldan3.servlet.Constant.ALDAN3_CONFIG)).getProperty("version")%>
   &copy; <%=msn.javaarchitect.webfolder.ctrl.Behavior.year()%> Dmitriy Rogatkin</div>