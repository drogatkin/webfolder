// Copyright 2009 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.aldan3.model.TemplateProcessor;
import org.aldan3.servlet.BasicAuthFilter;
import org.aldan3.servlet.FrontController;
import org.aldan3.util.DataConv;
import org.aldan3.util.Stream;
import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.block.Tabular;
import com.beegman.webbee.model.Appearance;
import com.beegman.webbee.util.PageRef;

public class Folder extends Tabular {
	// TODO use resources for labels
	public static final String FILE_LINKS = "Get file links";

	public static final String OPEN_URL = "open_url";

	public static final String CONFIG_ATTR_NAME = "webfolder.external.config";

	public static final String CLIPBOARD = "clipboard";

	static final String DEF_CHARSET = CharSet.UTF8;

	private boolean formList;
	
	private String editFile;

	private Boolean android;

	private String getBaseUrl() {

		if (req.getProtocol().startsWith("HTTP"))
			return "http"
					+ (req.isSecure() ? "s" : "")
					+ "://"
					+ req.getServerName()
					+ ((req.isSecure() == false && req.getServerPort() != 80)
							|| (req.isSecure() && req.getServerPort() != 443) ? ":" + req.getServerPort() : "")
					+ req.getContextPath() + req.getServletPath() + "/Opendirect";
		return req.getProtocol();
	}

	@Override
	protected Object getTabularData(long pos, int size) {
		File folder = new File(getConfigValue("TOPFOLDER", File.separator));
		if (req.getPathInfo() != null)
			folder = new File(folder, req.getPathInfo().replace('/', File.separatorChar));
		// log("Getting %s dir %b", null, folder, folder.isDirectory());
		if (folder.isDirectory()) {
			File[] childs = folder.listFiles();
			// log("Childs %s"+childs, null, childs);
			if (childs != null) {
				int n = size > 0 ? Math.min(size, childs.length) : childs.length;
				Webfile[] result = new Webfile[n];
				n += (int) pos;
				long total = 0;
				for (int i = 0 + (int) pos; i < n; i++) {
					result[i] = new Webfile(childs[i]);
					total += result[i].size;
				}
				String totals = DataConv.toStringInUnits(total);
				try {
					Class fscl = Class.forName("android.os.StatFs");
					Object fs = fscl.getConstructor(String.class).newInstance(folder.getPath());
					long bls = ((Integer)fscl.getMethod("getBlockSize").invoke(fs)).longValue();
					
					totals += "/"+DataConv.toStringInUnits(bls*(int)(Integer)fscl.getMethod("getBlockCount").invoke(fs));
					totals += "/"+DataConv.toStringInUnits(bls*(int)(Integer)fscl.getMethod("getAvailableBlocks").invoke(fs));
				} catch(Exception e) {
					// not android 
				}
				modelInsert("total", totals);
				return result;
			} else
				return new Webfile[0];
		}
		return new Webfile[0];
	}

	@Override
	protected String getCharSet() {
		return DEF_CHARSET;
	}

	@Override
	protected Object doControl() {
		navigation = null;
		// log("Top folder: "+getConfigValue("TOPFOLDER", File.separator),
		// null);
		String path = new File(getConfigValue("TOPFOLDER", File.separator), getStringParameterValue("path", "", 0))
				.getPath();
		String operation = getParameterValue(Form.SUBMIT, FILE_LINKS, 0);
		// log("operation:"+operation, null);
		if ("Download".equals(operation)) {
			navigation = "Downloadzip";
			return null;
		} else if ("Delete".equals(operation)) {
			String[] selection = req.getParameterValues("files");
			if (selection != null && selection.length > 0) {
				navigation = "Delete";
				return null;
			}
			return getResult("No delete entries were selected");
		} else if ("Upload".equals(operation)) {
			String error = null;
			String name = getParameterValue("file+filename", null, 0);
			for (int i=0; name != null; name=getParameterValue("file+filename", null, ++i)) {
				log("Uploading:"+name, null);
				if (name.length() > 0) {
					// make any separators matching current file system
					name = sanitizeName(name);
					Object attachment = getObjectParameterValue("file", null, i, false);
					File target = new File(path, name);
					try {
						// log("name:"+getObjectParameterValue("file", null, 0,
						// false)+" to "+path, null);

						if (target.exists())
							throw new IOException("File with name " + name + " already exists");
						if (attachment instanceof byte[]) {
							FileOutputStream fos = new FileOutputStream(target);
							try {
								fos.write((byte[]) attachment);
								fos.flush();
							} finally {
								fos.close();
							}
						} else if (attachment instanceof File) {
							Stream.copyFile((File) attachment, target);
							((File) attachment).delete();
						} else if (attachment instanceof String) {
							// TODO get attachment charset from content type
							FileWriter fw = new FileWriter(target);
							try {
								fw.write((String) attachment);
							} finally {
								fw.close();
							}
						} else
							log("Unsupported attachment type " + attachment.getClass().getName()+" skipped", null);
					} catch (IOException e) {
						String problem = "Problem in uploading " + name + " to " + path;
						log(problem, e);
						if (error == null)
							error = problem;
						else
							error += ", "+problem;						
					}
				}
			}
			return getResult(error);
		} else if ("New folder".equals(operation)) {
			String folder = getStringParameterValue("folder", "", 0);
			if (folder.length() > 0 && folder.indexOf("..") < 0)
				folder = sanitizeName(folder); // no need check for .. ?
				if (new File(path, folder).mkdir() == false) {
					log("Can't create folder " + folder + " in " + path, null);
					return getResult("Folder " + folder + " has not been created");
				} else
					log("Folder " + folder + " in " + path + " was created", null);
			return getResult(null);
		} else if ("Edit".equals(operation)) {
			String[] selection = req.getParameterValues("files");
			if (selection != null && selection.length == 1) {
				editFile = sanitizeName(selection[0]);				
			} else {
				editFile = sanitizeName(getStringParameterValue("folder", "", 0));
				if (editFile.trim().length() == 0)
					editFile = null;
			}
			if (editFile != null && new File(path, editFile).isDirectory() == false) {
				String relPath = getStringParameterValue("path", "/", 0);
				if (relPath.endsWith("/") == false && relPath.endsWith("\\") == false) // TODO make a method for that
					relPath += File.separator;
				try {
					navigation = "Editor?file="	+ URLEncoder.encode(relPath + editFile, "UTF-8");
					//log("navigation:"+navigation, null);
					return null;
				} catch (Exception ie) {

				}
			}
			return getResult("No valid file name and type is specified");
			
		} else if ("Paste".equals(operation)) {
			String[] selection = (String[]) frontController.getAttribute(CLIPBOARD);
			String top = getConfigValue("TOPFOLDER", File.separator);
			if (selection != null) {
				try {
					for (String from : selection) {
						// if (from.indexOf("..") < 0) sanity already done when
						// placed in clipboard
						copy(URLDecoder.decode(from, "UTF-8"), path, top); // TODO find out while clipboard is encoded
					}
				} catch (UnsupportedEncodingException uee) {

				}
			}
			return getResult(null);
		}
		String[] selection = req.getParameterValues("files");
		if (selection != null && selection.length > 0) {
			if (FILE_LINKS.equals(operation)) {
				formList = true;
				HashMap list_res = new HashMap(4);
				list_res.put("list", selection);
				req.setAttribute(OPEN_URL, getBaseUrl());
				return list_res;
			} else if ("Copy".equals(operation)) {
				frontController.getServletConfig().getServletContext().setAttribute(CLIPBOARD, selection);
				return getResult(null);
			}
		} else if ("Copy".equals(operation)) {
			frontController.getServletConfig().getServletContext().removeAttribute(CLIPBOARD);
		}
		// log("Navigation:%s", null, navigation);
		return super.doControl();
	}

	private String copy(String from, String path, String top) {
		// TODO consider throwing exception instead of silent logging errors
		File ffrom = new File(top, from);
		File fto = new File(path, ffrom.getName());
		if (ffrom.isDirectory()) {
			if (fto.exists() == false)
				if (fto.mkdir() == false)
					return "Can't create target directory";
			File [] content = ffrom.listFiles();
			String result = null;
			if (content != null) {				
				for (File cf:content) {
					String ec = copy (cf.getName(), fto.getPath(), cf.getParent());
					if (ec != null)
						if (result == null)
							result = ec;
						else
							result += "; "+ec;
				}
			}
			return result;
		} else {
			if (fto.exists() == false)
				try {
					long flen = Stream.copyFile(ffrom, fto);
					if (flen < ffrom.length())
						throw new IOException("Copied only " + flen + " bytes of " + ffrom.length());
				} catch (IOException e) {
					log("Problem in copy:" + from + " to " + path, e);
					return "Exception at copy ("+e +")";
				}
			else {
				log("Can't copy, %s already exists", null, fto);
				return "File exists";
			}
		}
		return null;
	}

	private HashMap getResult(String error) {
		HashMap<String, Object> result = (HashMap<String, Object>) getModel();
		result.put(Variable.ERROR, error);
		return result;
	}
	
	static String sanitizeName(String pathName) {
		if (File.separatorChar != '\\')
			pathName = pathName.replace('\\', File.separatorChar);
		else
			pathName = pathName.replace('/', File.separatorChar);
		return new File(pathName).getName();
	}

	@Override
	protected String getSubmitPage() {
		return req.getServletPath() + "/" + (navigation == null ? "Folder"+DataConv.ifNull(req.getPathInfo(), "") : navigation);
	}

	@Override
	protected String getCanvasView() {
		if (formList)
			return null;
		return super.getCanvasView();
	}

	@Override
	protected boolean canCache() {
		return formList;
	}

	@Override
	protected Object getModel() {
		formList = false;
		editFile = null;
		HashMap<String, Object> pageModel = (HashMap) super.getModel();
		File path = req.getPathInfo() == null ? new File("/") : new File(req.getPathInfo().replace('/',
				File.separatorChar));

		pageModel.put("file", path.getName());
		pageModel.put("parent_label", path.getParent());
		pageModel.put("path", path.getPath());
		path = path.getParentFile();
		pageModel.put("parents", splitPath(path == null ? "" : path.getPath()));
		pageModel.put(OPEN_URL, getBaseUrl());
		PageRef[] toplinks = new PageRef[3];
		toplinks[0] = PageRef.create(req, "Info",
				"../sysinfo.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI()));
		toplinks[1] = PageRef.create(req, "Admin",
				"../admin.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI()));
		toplinks[2] = PageRef.create(req, "Headers",
				"../threadump.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI()));
		
		pageModel.put(TOPLINKS, toplinks);
		String[] selection = (String[]) frontController.getAttribute(CLIPBOARD);
		pageModel.put(CLIPBOARD, selection == null ? 0 : selection.length);
		if (pageModel.get(MODEL) == null)
			pageModel.put(Variable.ERROR, "Can't read content of the directory");
		return pageModel;
	}

	@Override
	protected String getViewName() {
		if (formList) {
			return "true".equals(getParameterValue("as text", "false", 0)) ? "filelist.txt" : "filelist.htmt";
		}
		return super.getViewName();
	}


	@Override
	protected String getTitle() {
		return "(" + (req.getPathInfo() == null ? "" : req.getPathInfo()) + ")";
	}

	public String processRenameCall() {
		String path = getParameterValue("path", "", 0);
		if (path.endsWith("/") == false && path.endsWith("\\") == false)
			path += File.separator;
		String sfrom = getParameterValue("from", "", 0);
		if (sfrom.length() == 0 || sfrom.indexOf("..") >= 0)
			return "error";
		String sto = getParameterValue("to", "", 0);
		if (sto.length() == 0 || sto.indexOf("..") >= 0)
			return "error";
		File ffrom = new File(getConfigValue("TOPFOLDER", File.separator), path + sfrom);
		File fto = new File(getConfigValue("TOPFOLDER", File.separator), path + sto);
		log("Renaming %s existing %s to %s", null, ffrom.exists() ? "" : "non", ffrom, fto);
		if (ffrom.renameTo(fto))
			return "ok";
		return "error";
	}

	@Override
	protected TemplateProcessor getTemplateProcessor(String viewName) {
		return super.getTemplateProcessor("dummy.htm");
	}

	@Override
	protected String getContentType(String viewName) {
		if (viewName.endsWith(".txt")) {
			if (appearance != Appearance.mobile && appearance != Appearance.tablet)
				resp.setHeader("Content-disposition", "attachment; filename=down-list.txt"); // see
			// http://www.ietf.org/rfc/rfc2183.txt
			return "text/plain; charset=" + DEF_CHARSET;
		} // else if (viewName.endsWith(".html"))
		return super.getContentType(viewName);
	}

	@Override
	protected Object applySideEffects(Object modelData) {
		modelData = super.applySideEffects(modelData);
		if (modelData instanceof Map && !((Map)modelData).containsKey(SEARCH_BLOCK)) {
			((Map)modelData).put(SEARCH_BLOCK, "insert/search.htmt");
		}
		return modelData;
	}
	
	@Override
	public boolean useForward() {
		return true;
	}

	@Override
	protected String getConfigValue(String name, String defVal) {
		return getConfigValue(frontController, name, super.getConfigValue(name, defVal));
	}

	static String getConfigValue(FrontController frontController, String name, String defVal) {
		Properties configProps = (Properties) frontController.getAttribute(CONFIG_ATTR_NAME);
		if (configProps == null || frontController.getAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME) != null) {
			configProps = new Properties();
			FileInputStream fis;
			try {
				configProps.load(fis = new FileInputStream(BasicAuthFilter.getAuthPropertiesFile(frontController
						.getServletContext().getContextPath())));
				fis.close();
			} catch (Exception e) {

			} finally {
				synchronized (frontController) {
					frontController.getServletContext().setAttribute(CONFIG_ATTR_NAME, configProps);
					frontController.getServletContext().removeAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME);
				}
			}
		}
		return configProps.getProperty(name, defVal);
	}

	public static class Webelement {
		public String name;

		public String path;
	}

	public class Webfile extends Webelement {
		public Webfile(File f) {
			name = f.getName();
			path = f.getPath();
			String tp = getConfigValue("TOPFOLDER", File.separator);
			int a = 0;
			if (tp.endsWith(File.separator))
				a = -1;
			if (path.startsWith(tp))
				path = path.substring(tp.length() + a);
			else
				path = tp;
			folder = f.isDirectory();
			size = f.length();
			last_mod = new Date(f.lastModified());
			if (android == null) {
				android = System.getProperty("java.vm.name", "").indexOf("Dalvik") >= 0;
			}
			char[] perm_array = new char[5];
			if (f.isDirectory())
				perm_array[0] = 'd';
			else
				perm_array[0] = '-';
			if (f.canRead())
				perm_array[1] = 'r';
			else
				perm_array[1] = '-';
			if (f.canWrite())
				perm_array[2] = 'w';
			else
				perm_array[2] = '-';
			if (android == false && f.canExecute())
				perm_array[3] = 'x';
			else
				perm_array[3] = '-';
			if (f.isHidden())
				perm_array[4] = '.';
			else
				perm_array[4] = '-';
			permissions = new String(perm_array);
		}

		public boolean folder;

		public long size;

		public Date last_mod;

		public String permissions;
 
		public boolean archive;
		
		public boolean unaccessible;
		
		public String owner;
	}

	public Webelement[] splitPath(String path) {
		if (path == null)
			return new Webelement[0]; // null can be good as well
		String[] paths = path.split(File.separatorChar == '\\' ? "\\\\" :  File.separator);
		Webelement[] result = new Webelement[paths.length == 0 ? 1 : paths.length];
		result[0] = new Webelement();
		result[0].name = getConfigValue("TOPFOLDER", File.separator);
		result[0].path = paths.length > 0 ? paths[0] : "";
		for (int ei = 1; ei < paths.length; ei++) {
			result[ei] = new Webelement();
			result[ei].name = paths[ei];
			try {
				result[ei].path = result[ei - 1].path + '/' + URLEncoder.encode(result[ei].name, DEF_CHARSET);
			} catch (UnsupportedEncodingException e) {
				result[ei].path = result[ei - 1].path + '/' + result[ei].name;
			}
		}
		return result;
	}

	public static String webPath(String path) {
		if (path == null)
			return null;
		try {
			String[] paths = path.split(File.separatorChar == '\\' ? "\\\\" : "" + File.separatorChar);
			path = "";
			for (String part : paths) {
				if (part.length() == 0)
					continue;
				path += '/';
				path += URLEncoder.encode(part, DEF_CHARSET);
			}
			return path;
		} catch (UnsupportedEncodingException e) {
			path = path.replace(File.separatorChar, '/');
		}
		return path;
	}
}
