<%@ page import="org.aldan3.servlet.BasicAuthFilter"  %>
<%@ page import="java.util.Properties"  %>
<%@ page import="java.util.Date"  %>
<%@ page import="java.io.File"  %>
<%@ page import="java.io.FileInputStream"  %>
<%@ page import="java.io.FileOutputStream"  %>
<%@ page import="java.util.Set, java.util.HashSet, java.util.List, java.lang.reflect.Method"  %>

<%@ page import="android.content.Context, android.os.Build, android.provider.MediaStore, android.os.storage.StorageManager, android.os.storage.StorageVolume"  %>
<%!public Properties getConfigProperties() {
		Properties result = new Properties();
		FileInputStream fis = null;
		try {
			result.load(new FileInputStream(BasicAuthFilter
					.getAuthPropertiesFile(getServletContext().getContextPath())));
		} catch (Exception e) {
			log("No props: " + e);
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
	String bookmarks = "";
	boolean Java7 = true;
	pass = request.getParameter("pass");
	String going_back = request.getParameter("going_back");
	if (going_back == null)
		going_back = request.getHeader("referer");
	Properties configProps = getConfigProperties();
	if (pass == null) {
		pass = unchangedPass;
		try {
			Class.forName("java.nio.file.FileSystems");
		} catch(ClassNotFoundException cnf) {
			Java7 = false;
		}
		top_folder = configProps.getProperty("TOPFOLDER", Java7?java.nio.file.FileSystems.getDefault()
				.getSeparator():"/sdcard");
		use_watch = "true".equalsIgnoreCase(configProps.getProperty("WATCHSERVICE", "false"))?"checked":"";
		sess_clpbrd = "true".equalsIgnoreCase(configProps.getProperty("SESSIONCLIPBOARD", "false"))?"checked":"";
		bookmarks = "true".equalsIgnoreCase(configProps.getProperty("BOOKMARKS", "true"))?"checked":"";
	} else {
		configProps.setProperty("TOPFOLDER",
				request.getParameter("top_folder"));
		// TODO ! check if config file location is inside the top folder and refuse
		if (pass.equals(unchangedPass) == false
				&& pass.equals(request.getParameter("pass2"))) {
			configProps.setProperty("PASSWORD", pass);
		}
		configProps.setProperty("WATCHSERVICE", request.getParameter("watchservice") == null?"false":"true");
		configProps.setProperty("SESSIONCLIPBOARD", request.getParameter("sessionclipbrd") == null?"false":"true");
		configProps.setProperty("BOOKMARKS", request.getParameter("bookmarks") == null?"false":"true");
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
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>WebFolder: Admin</title>
<style>
     <%@include file="css/style.css"%>
</style>
</head>
<script>
   function selVal(aval) {
	   const volume = document.querySelector('[name="top_folder"]')
	   if (volume)
		   volume.value = aval
   }
</script>
<body>
<h2>Admin</h2>
<div><%=error%></div>
<form name="pass_sel" method="post"  action="admin.jsp">
<table>
  <tr>
    <td>Admin password</td>
    <td><input name="pass" size="12" type="password" value="<%=pass%>"/></td><td>confirm &nbsp; <input type="password" name="pass2" size="12" value="<%=pass%>"/></td>
  </tr>
  <tr>
     <td>Top folder</td><td colspan="2" ><input type="text" name="top_folder" value="<%=top_folder%>"/></td>
     </tr>
  <% if (Build.VERSION.SDK_INT >= 24) {
       HashSet<File> topDirs = new HashSet<File>();
       Context context = (Context)application.getAttribute("##RuntimeEnv");
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			Set<String> volumeNames = MediaStore.getExternalVolumeNames(context);
			for (String name: volumeNames) {
				File vf = new File("/storage/"+name);
				if (vf.exists())
					topDirs.add(vf);
			}
       }
       if (topDirs.size() == 0 && Build.VERSION.SDK_INT >= 24) {
			try {
				// https://www.programcreek.com/java-api-examples/index.php?api=android.os.storage.StorageManager
				StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
				List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
				
				Method getPath = null;
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)	
					getPath = StorageVolume.class.getDeclaredMethod("getPath");
				if (storageVolumes != null && storageVolumes.size() > 0) {
					for (StorageVolume volume : storageVolumes) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
							topDirs.add(volume.getDirectory());
						} else {
							topDirs.add(new File((String) getPath.invoke(volume)));
						}
					}
				}
			} catch (Exception e) {
				
			}
       }
		if (topDirs.size() > 0) {
       %>   
  <tr>
     <td>Suggestions</td><td colspan="2"><select name="volumes" id="volume-select" onchange="selVal(this.value)">
        <% for(File f: topDirs) {%>
            <option value="<%=f.getAbsolutePath()%>"><%=f.getAbsolutePath()%></option>
         <% } %>
         </select></td>
  </tr>
  <% } } %>
     <!-- add a combobox for Android volumes based on https://developer.android.com/training/data-storage/shared/media -->
     <tr><td>Maintain a clipboard<br /> in a session</td><td colspan="2"><input type="checkbox" name="sessionclipbrd" value="true" <%=sess_clpbrd %>></input></td></tr> 
     <tr><td>Use Watch service</td><td colspan="2"><input type="checkbox" name="watchservice" value="true" <%=use_watch %>></input></td></tr>
     <tr><td>Use directory bookmarks</td><td colspan="2"><input type="checkbox" name="bookmarks" value="true" <%=bookmarks %>></input></td></tr>
</table>
<input type="hidden" name="going_back" value="<%=going_back%>"/>
<input type="submit" value="Apply"/>
</form>

<div style="padding-top:0.5em"><a href="<%=going_back%>">Go back</a></div>
<jsp:include page="/WEB-INF/view/insert/copyright.htmt"/>
</body>
</html>