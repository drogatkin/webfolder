<%@ page import="org.aldan3.servlet.BasicAuthFilter"  %>
<%@ page import="java.util.Properties"  %>
<%@ page import="java.util.Date"  %>
<%@ page import="java.io.File"  %>
<%@ page import="java.io.FileInputStream"  %>
<%@ page import="java.io.FileOutputStream"  %>
<%!public Properties getConfigProperties() {
		Properties result = new Properties();
		FileInputStream fis = null;
		try {
			result.load(new FileInputStream(BasicAuthFilter
					.getAuthPropertiesFile(getServletContext().getContextPath())));
		} catch (Exception e) {
			log("No props", e);
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (Exception e) {
				}
		}
		return result;
	}%>

<%
	String unchangedPass = "*******";
	String pass, top_folder = "";
	String error = "";
	String use_watch = "";
	String sess_clpbrd = "";
	pass = request.getParameter("pass");
	String going_back = request.getParameter("going_back");
	if (going_back == null)
		going_back = request.getHeader("referer");
	Properties configProps = getConfigProperties();
	if (pass == null) {
		pass = unchangedPass;
		top_folder = configProps.getProperty("TOPFOLDER", "");
		use_watch = "true".equalsIgnoreCase(configProps.getProperty("WATCHSERVICE", "false"))?"checked":"";
		sess_clpbrd = "true".equalsIgnoreCase(configProps.getProperty("SESSIONCLIPBOARD", "false"))?"checked":"";
	} else {
		configProps.setProperty("TOPFOLDER",
				request.getParameter("top_folder"));
		if (pass.equals(unchangedPass) == false
				&& pass.equals(request.getParameter("pass2"))) {
			configProps.setProperty("PASSWORD", pass);
		}
		configProps.setProperty("WATCHSERVICE", request.getParameter("watchservice") == null?"false":"true");
		configProps.setProperty("SESSIONCLIPBOARD", request.getParameter("sessionclipbrd") == null?"false":"true");
		try {
			FileOutputStream fos;

			configProps.store(
					fos = new FileOutputStream((BasicAuthFilter
							.getAuthPropertiesFile(application
									.getContextPath()))),
					"Updated from  IP "
							+ request.getRemoteAddr()+" / "+request.getRemoteHost());
			fos.close();
			getServletContext().setAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME, Boolean.TRUE);
			//log("Properties updated");
		} catch (Exception e) {
			log("Propblem to save", e);
		}
		//log("Redirecting:"+going_back);
		response.sendRedirect(going_back);
	}
	// check if submitted
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>WebFolder: Admin</title>
</head>
<body>
<h2>Admin</h2>
<div><%=error%></div>
<form name="pass_sel" method="post"  action="admin.jsp">
<table>
  <tr>
    <td>Admin password</td>
    <td><input name="pass" type="password" value="<%=pass%>"/></td><td>confirm &nbsp; <input type="password" name="pass2" value="<%=pass%>"/></td>
  </tr>
  <tr>
     <td>Top folder</td><td colspan="2" ><input type="text" name="top_folder" value="<%=top_folder%>"/></td>
     </tr>
     <tr><td>Maintain clipboard<br /> in session</td><td colspan="2"><input type="checkbox" name="sessionclipbrd" value="true" <%=sess_clpbrd %>></input></td></tr> 
     <tr><td>Use Watch service</td><td colspan="2"><input type="checkbox" name="watchservice" value="true" <%=use_watch %>></input></td></tr>
</table>
<input type="hidden" name="going_back" value="<%=going_back%>"/>
<input type="submit" value="Apply"/>
</form>

<div><a href="<%=going_back%>">Go back</a></div>
<jsp:include page="/WEB-INF/view/insert/copyright.htmt"/>
</body>
</html>