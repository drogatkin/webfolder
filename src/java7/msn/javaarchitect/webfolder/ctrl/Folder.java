// Copyright 2009 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.aldan3.model.TemplateProcessor;
import org.aldan3.servlet.BasicAuthFilter;
import org.aldan3.servlet.FrontController;
import org.aldan3.util.DataConv;
import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.block.Tabular;
import com.beegman.webbee.model.AppModel;
import com.beegman.webbee.model.Appearance;
import com.beegman.webbee.util.PageRef;
//import java.io.FileWriter;

/**
 * Java 7+ only version of folder viewer
 * 
 * @author dmitriy
 * 
 */
public class Folder extends Tabular <Collection<Folder.Webfile>, AppModel> {
	// TODO use resources for labels
	public static final String FILE_LINKS = "Get file links";

	public static final String OPEN_URL = "open_url";

	public static final String CONFIG_ATTR_NAME = "webfolder.external.config";

	public static final String CLIPBOARD = "clipboard";

	public static final String TOPFOLDER = "TOPFOLDER";
	
	public static final String BOOKMARKS = "BOOKMARKS";
	
	public static final String CLIPBOARD_TOPFOLDER = CLIPBOARD+TOPFOLDER;
	
	static final String DEF_CHARSET = CharSet.UTF8;
	
	static final String ZIP_EXT[] = {".zip", ".war", ".jar", ".apk", ".ipa", ".odt", ".ods", ".odp" };

	private boolean formList;

	private String editFile;

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
	protected Collection<Webfile> getTabularData(final long pos, final int size) {
		final ArrayList<Webfile> result = new ArrayList<>(size <= 0 ? 100 : size);
		try (RequestTransalated rt = translateReq(getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator()), req.getPathInfo());
				DirectoryStream<Path> stream = Files.newDirectoryStream(rt.transPath);) {
			//log("display: %s for %s", null, rt.transPath.getFileSystem(), rt);
			//System.err.printf("display: %s %n", rt);
			long total = 0;
			Webfile wf;
			for (Path entry : stream) {
				result.add(wf = new Webfile(entry, rt.reqPath));
				total += wf.size;
			}			
			try {
				String totals = DataConv.toStringInUnits(total);
				Class fscl = Class.forName("android.os.StatFs");
				Object fs = fscl.getConstructor(String.class).newInstance(rt.transPath.toString());
				long bls = ((Integer)fscl.getMethod("getBlockSize").invoke(fs)).longValue();
				
				totals += "/"+DataConv.toStringInUnits(bls*(int)(Integer)fscl.getMethod("getBlockCount").invoke(fs));
				totals += "/"+DataConv.toStringInUnits(bls*(int)(Integer)fscl.getMethod("getAvailableBlocks").invoke(fs));
				modelInsert("total", totals);
			} catch(Exception e) {
				//e.printStackTrace();
				FileStore fst = Files.getFileStore(rt.transPath);
				modelInsert("total", DataConv.toStringInUnits(fst.getTotalSpace())+" / "+DataConv.toStringInUnits(fst.getUnallocatedSpace())
						+" / "+DataConv.toStringInUnits(total)); 
			}
		} catch (Exception ioe) {
			//log("", ioe);
			//getResult("" + ioe);
			modelInsert(Variable.ERROR, ioe);
			/*if (req.getPathInfo().endsWith("/favicon.ico")) {
				try {
					req.getRequestDispatcher("/favicon.ico").forward(req, resp);
					return null;
				} catch (Exception se) {
					log("", se);
				}
			}*/
		}
		return result;
	}

	@Override
	protected String getCharSet() {
		return DEF_CHARSET;
	}

	@Override
	protected Object doControl() {
		navigation = null;
		String stayResult = null;
		String operation = getParameterValue(Form.SUBMIT, FILE_LINKS, 0);
		
		try (RequestTransalated tr = translateReq(getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator()),
				getStringParameterValue("path", "", 0));) {
			Path path = tr.transPath;

			//log("operation: %s on %s", null, operation, path.getFileSystem());
			String[] selection;
			switch (operation) {
			case "Download":
				navigation = "Downloadzip";
				return null;
			case "Delete":
				selection = req.getParameterValues("files");
				if (selection != null && selection.length > 0) {
					navigation = "Delete";
					return null;
				}
				stayResult = "No delete entries were selected";
				break;
			case "Upload":
				//String error = null;
				String name = getParameterValue("file+filename", null, 0);
				for (int i = 0; name != null; name = getParameterValue("file+filename", null, ++i)) {
					if (name.length() > 0) {
						name = adjustSeparators(name);
						Object attachment = getObjectParameterValue("file", null, i, false);
						Path target = path.resolve(path.getFileSystem().getPath(name).getFileName());
						//log("Uploading:%s", null, target);
						try {
							// log("name:"+getObjectParameterValue("file", null, 0,
							// false)+" to "+path, null);
							//if (true) throw new IOException("problem");
							if (Files.exists(target))
								throw new IOException("File with name " + name + " already exists");
							if (attachment instanceof byte[]) {
								try (OutputStream fos = Files.newOutputStream(target);) {
									fos.write((byte[]) attachment);
									fos.flush();
								}
							} else if (attachment instanceof File) {
								try (FileInputStream fis = new FileInputStream((File) attachment);) {
									Files.copy(fis, target);
								}
								((File) attachment).delete();
							} else if (attachment instanceof String) {
								// TODO get attachment charset from content type
								try (BufferedWriter bw = Files.newBufferedWriter(target, Charset.forName("UTF-8"));) {
									bw.write((String) attachment);
								}
							} else
								log("Unsupported attachment type %s skipped", null, attachment.getClass().getName());
						} catch (IOException e) {
							String problem = "Problem in uploading " + name + " to " + path;
							log(problem, e);
							if (stayResult == null)
								stayResult = problem;
							else
								stayResult += ", " + problem;
							if (1 == getParameterValue("background", 0, 0))
								resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR/*, problem*/);
						}
					}
				}
				//tr.close();
				//return getResult(error);
				break;
			case "New folder":
				String folder = getStringParameterValue("folder", "", 0);
				Path target = path.resolve(folder);
				try {
					if (target.startsWith(".."))
						throw new IllegalArgumentException("Requested name is out of reach");
					Files.createDirectory(target);
					//log("Folder %s has been created", null, target);
				} catch (Exception e) {
					stayResult = String.format("Can't create folder %s in path %s", folder, path);
					log(stayResult, e);
					//return getResult("Folder " + folder + " has not been created");
				}
				//return getResult(null);
				break;
			case "Edit":
				selection = req.getParameterValues("files");
				if (selection != null && selection.length == 1) {
					editFile = path.getFileSystem().getPath(selection[0]).getFileName().toString();
				} else {
					editFile = getStringParameterValue("folder", "", 0);
					if (editFile.trim().length() == 0)
						editFile = null;
				}
				//log("edit file:%s",null, editFile);
				if (editFile != null) {
					Path editPath = path.resolve(editFile);
					if (Files.isDirectory(editPath) == false) {
						String p = getStringParameterValue("path", "", 0);
						if (p.endsWith("/") == false)
							p += "/";
						try {
							navigation = "Editor?file=" + URLEncoder.encode(p+editPath.getFileName().toString(), "UTF-8");
							//log("navigation: %s path %s", null, navigation, editPath);
							return null;
						} catch (Exception ie) {
							log("", ie);
						}
					}
				}
				//return getResult("No valid file name and type is specified");
				stayResult = "No valid file name and type is specified";
				break;
			case "Paste":
				String clipTopFolder = null;
				if ("true".equalsIgnoreCase(getConfigValue("SESSIONCLIPBOARD", "false"))) {
					HttpSession sess = req.getSession(false);
					if (sess != null)
						synchronized (sess) {
							selection = (String[]) sess.getAttribute(CLIPBOARD);
							clipTopFolder = (String) sess.getAttribute(CLIPBOARD_TOPFOLDER);
						}
					else
						selection = null;
				} else
					synchronized (frontController) {
						selection = (String[]) frontController.getAttribute(CLIPBOARD);
						clipTopFolder = (String) frontController.getAttribute(CLIPBOARD_TOPFOLDER);
					}
				if (selection != null) {					
					if (clipTopFolder == null)
						clipTopFolder = getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator());
					for (String from : selection) {
						try {
							// if (from.indexOf("..") < 0) sanity already done when
							// placed in clipboard
							copy(clipTopFolder, from, path); // TODO check if decode required
						} catch (IOException ioe) {
							if (stayResult == null)
								stayResult = ioe.toString();
							else
								stayResult += ioe.toString();
						}
					}
				}
				//return getResult(errors);
				break;
				default:
					selection = req.getParameterValues("files");
					if (selection != null && selection.length > 0) {
						if (FILE_LINKS.equals(operation)) {
							formList = true;
							HashMap list_res = new HashMap(4);
							list_res.put("list", selection);
							req.setAttribute(OPEN_URL, getBaseUrl());
							return list_res;
					} else if ("Copy".equals(operation)) {
						if ("true".equalsIgnoreCase(getConfigValue("SESSIONCLIPBOARD", "false"))) {
							HttpSession sess = req.getSession();
							synchronized (sess) {
								sess.setAttribute(CLIPBOARD, selection);
								sess.setAttribute(CLIPBOARD_TOPFOLDER,
										getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator()));

							}
						} else
							synchronized (frontController) {
								frontController.getServletConfig().getServletContext()
										.setAttribute(CLIPBOARD, selection);
								frontController
										.getServletConfig()
										.getServletContext()
										.setAttribute(CLIPBOARD_TOPFOLDER,
												getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator()));
							}
						// return getResult(null);
					}
				} else // {
				if ("Copy".equals(operation)) {
					if ("true".equalsIgnoreCase(getConfigValue("SESSIONCLIPBOARD", "false"))) {
						HttpSession sess = req.getSession(false);
						if (sess != null)
							synchronized (sess) {
								sess.removeAttribute(CLIPBOARD_TOPFOLDER);
								sess.removeAttribute(CLIPBOARD);
							}
					} else
						synchronized (frontController) {
							frontController.getServletConfig().getServletContext().removeAttribute(CLIPBOARD_TOPFOLDER);
							frontController.getServletConfig().getServletContext().removeAttribute(CLIPBOARD);
						}
				}						
					//}
			}
		} catch (Exception e) {
			log("", e);
		}
		return getResult(stayResult);
	}

	private void copy(String topPathFr, String from, Path path) throws IOException {
		try (Folder.RequestTransalated rtf = translateReq(topPathFr, from);) {
			final Path ffrom ;
			if (rtf.reqPath.isEmpty() == false && rtf.transPath.getFileName() == null)
				ffrom = FileSystems.getDefault().getPath(rtf.reqPath);
			else
				ffrom = rtf.transPath;
			final Path fto = path.resolve(ffrom.getFileName().toString());
			//log("paste %s to %s fs: %s (trans:%s)",null, ffrom, fto, path.getFileSystem(), rtf.transPath);
			Files.walkFileTree(ffrom, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
								throws IOException {
							Path targetdir = fto.resolve(fto.getFileSystem().getPath(ffrom.relativize(dir).toString()));							
							try {
								Files.copy(dir, targetdir, StandardCopyOption.COPY_ATTRIBUTES);
							} catch (FileAlreadyExistsException e) {
								if (!Files.isDirectory(targetdir))
									throw e;
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Path targetfile = fto.resolve(fto.getFileSystem()
									.getPath(ffrom.relativize(file).toString()));
							// check if target already exists
							if (Files.exists(targetfile))
								targetfile = targetfile.resolveSibling("Copy of "+file.getFileName());
							Files.copy(file, targetfile, StandardCopyOption.COPY_ATTRIBUTES);
							return FileVisitResult.CONTINUE;
						}
					});
		} catch (Exception e) {
			if (e instanceof IOException)
				throw (IOException) e;
			log("", e);
		}
	}

	private HashMap getResult(String error) {
		HashMap<String, Object> result = (HashMap<String, Object>) getModel();
		result.put(Variable.ERROR, error);
		return result;
	}
	
	static String adjustSeparators(String pathName) {
		char separatorChar = FileSystems.getDefault().getSeparator().charAt(0);
		if (separatorChar != '\\')
			pathName = pathName.replace('\\', separatorChar);
		else
			pathName = pathName.replace('/', separatorChar);
		return pathName;
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
		editFile = null;
		HashMap<String, Object> pageModel = (HashMap) super.getModel();
		String webPath = req.getPathInfo();
		if (webPath == null)
			webPath = "/";
		FileSystem defFS = FileSystems.getDefault();
		Path path = defFS.getPath(webPath.replace('/', defFS.getSeparator().charAt(0)));

		pageModel.put("file", path.getFileName());
		path = path.getParent();
		pageModel.put("parent_label", path);
		pageModel.put("path", webPath);
		
		pageModel.put("parents", splitPath(path == null ? "" : path.toString()));
		pageModel.put(OPEN_URL, getBaseUrl());
		
		PageRef[] toplinks =  new PageRef[] {
				PageRef.create(req, "Console", "Console"+HttpUtils.urlEncode(webPath)),
				PageRef.create(req, "Info", "../sysinfo.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI())),
				PageRef.create(req, "Admin", "../admin.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI())),
				PageRef.create(req, "Headers",
						"../threadump.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI())) } ;
		pageModel.put(TOPLINKS, toplinks);
		String[] selection = null;
		if ("true".equalsIgnoreCase(getConfigValue("SESSIONCLIPBOARD", "false"))) {
			HttpSession sess = req.getSession(false);
			if (sess != null)
				selection =	(String[]) sess.getAttribute(CLIPBOARD);
		} else
			selection =	(String[]) frontController.getAttribute(CLIPBOARD);
		pageModel.put(CLIPBOARD, selection == null ? 0 : selection.length);
		if (pageModel.get(MODEL) == null)
			pageModel.put(Variable.ERROR, "Can't read content of the directory");
		if ("true".equalsIgnoreCase(getConfigValue("WATCHSERVICE", "false"))) {
			pageModel.put("watchservice", "");
			WatchUpdater wu = assureWatchUpdater();
			pageModel.put("page-mark", wu.newMark());
		}
		if (userAgent != null && userAgent.toLowerCase().indexOf("midori")>= 0)
			pageModel.put("midori", true);
		if ("true".equalsIgnoreCase(getConfigValue(BOOKMARKS, "true"))) {
			//log("adding bookmarks", null);
			pageModel.put("bookmarks_section", true);
			String[] bookmarks = readBookmarks(frontController);
			pageModel.put("bookmarks", bookmarks);
		} else
			log("no bookmarks", null);
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

	@Override
	protected void start() {
		formList = false;
		super.start();
	}
	
	public String processRenameCall() {
		try (RequestTransalated trans = translateReq(getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator()), getParameterValue("path", "", 0));){
			Path path = trans.transPath;

			Path sfrom = path.resolve(getParameterValue("from", "", 0));
			Path sto = path.resolve(getParameterValue("to", "", 0));
			//System.out.printf("ren %s to %s as %s in %s%n",sfrom, sto, trans.transPath, trans.reqPath);
			sto = Files.move(sfrom, sto, StandardCopyOption.ATOMIC_MOVE); // REPLACE_EXISTING
			//log("anala:"+tar, null);
			if (Files.isRegularFile(sto) && trans.reqPath.isEmpty())
				if (isZip(sto.getFileName().toString()))
					return "oka";
				else
					return "okn";
			//log("return as reg",null);
			return "ok";
		} catch (Exception e) {
			log("", e);
		}
		return "error";
	}
	
	public String processCheckForModificationsCall() {
		try (RequestTransalated rt = translateReq(getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator()), req.getPathInfo());) {
			//log("%s: %s: %s", null, req.getPathInfo(), rt.reqPath, rt.transPath);
			if (rt.reqPath == null || rt.reqPath.toString().isEmpty()) {
				WatchUpdater wu = assureWatchUpdater();
				wu.addToUpdate(rt.transPath, req.startAsync());
			} else
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Watch service not found");
		} catch (Exception ioe) {
			log("", ioe);
		}

		return null;
	}
	
	/** add, replace or remove a bookmark
	 * 
	 * @return
	 */
	public String processBookmarkCall() {
		String repBM = getParameterValue("old", "", 0);
		String BM = getParameterValue("bookmark", "", 0);

		if (BM .isEmpty()) {
			if (repBM.isEmpty())
				return "no bookmark";
		}
		Set<String> bookmarkSet = new HashSet<>(Arrays.asList(readBookmarks(frontController)));
		if (!repBM.isEmpty()) {
			bookmarkSet.remove(repBM);
		}
		if (!BM .isEmpty() && !bookmarkSet.add(BM)) 
			return "already exists";
		try {
			writeBookmark(bookmarkSet.toArray(new String[0]), frontController);
			return "ok";
		} catch (Exception e) {
			
		}
		return "error";
	}
	
	public String processReleaseWatchRequestCall() {
		WatchUpdater wu = assureWatchUpdater();
		wu.releaseMarked(getParameterValue("page-mark", "", 0));
		return "ok";
	}

	public String processChmodCall() {
		try {
			Path path = translateReq(
					getConfigValue(TOPFOLDER, FileSystems.getDefault()
							.getSeparator()), getParameterValue("path", "", 0)).transPath;
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString(getParameterValue("attr", "", 0));
			//log("setting attr: %s for %s and owner %s", null, perms, path, getParameterValue("owner", "", 0));
			Files.setPosixFilePermissions(path, perms);
			perms = Files.getPosixFilePermissions(path);

			UserPrincipal owner = FileSystems.getDefault()
					.getUserPrincipalLookupService()
					.lookupPrincipalByName(getParameterValue("owner", "", 0));

			if (owner.equals(Files.getOwner(path)) == false) {
				//log("updateing owner", null);
				Files.setOwner(path, owner);
			}
			return "?"+PosixFilePermissions.toString(perms)+"?";
		} catch (Exception e) {
			log("", e);
		}
		return "error";
	}
	
	private static final String UPDATER_HOLDER_NAME = "##watcher";
	
	private WatchUpdater assureWatchUpdater() {
		WatchUpdater wu = (WatchUpdater) frontController.getAttribute(UPDATER_HOLDER_NAME);
		if (wu == null) {
			synchronized (frontController) {
				wu = (WatchUpdater) frontController.getAttribute(UPDATER_HOLDER_NAME);
				if (wu == null) {
					try {
						wu = new WatchUpdater();
						frontController.registerResource(wu);
						frontController.getServletConfig().getServletContext().setAttribute(UPDATER_HOLDER_NAME, wu);
					} catch (IOException io) {
						log("Underline file system doesn't support watch service", io);
					}
				}
			}
		}
		return wu;
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
		return getConfigValue(frontController, name, super.getConfigValue(name, defVal), req);
	}

	static String getConfigValue(FrontController frontController, String name, String defVal, HttpServletRequest _req) {
		Properties configProps = (Properties) frontController.getAttribute(CONFIG_ATTR_NAME);
		if (configProps == null || frontController.getAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME) != null ||
				(_req != null && _req.getSession(false) != null &&
					_req.getSession(false).getAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME) != null)) {
			configProps = new Properties();
			FileInputStream fis;
			try {
				configProps.load(fis = new FileInputStream(BasicAuthFilter.getAuthPropertiesFile(frontController
						.getServletContext().getContextPath())));
				fis.close();
			} catch (Exception e) {

			} finally {
				//frontController.log("store prop "+CONFIG_ATTR_NAME+" in " + configProps);
				synchronized (frontController) {
					frontController.getServletContext().setAttribute(CONFIG_ATTR_NAME, configProps);
					frontController.getServletContext().removeAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME);
					if (_req != null && _req.getSession(false) != null) 
							_req.getSession(false).removeAttribute(BasicAuthFilter.REQUPDATE_ATTR_NAME);
				}
			}
		}
		String result = configProps.getProperty(name);
		if (result == null || result.isEmpty())
			return defVal;
		return result; //configProps.getProperty(name, configProps.getProperty(name);
	}
	
	static String[] readBookmarks(FrontController frontController) {
		String bookmarkscfg = getConfigValue(frontController, BOOKMARKS, "true", null);
		if ("true".equals(bookmarkscfg)) {
			Properties props = new Properties();
			try (FileInputStream is = new FileInputStream(getBookmarksFile(frontController))) {
				props.loadFromXML(is);
				ArrayList<String> names = new ArrayList<>();
				for(String name:props.stringPropertyNames()) {
					names.add(props.getProperty(name));
				}
				return names.toArray(new String[0]);
			} catch (Exception e) {
				frontController.log("exception:", e);
			}
			return new String[0];
		}
		//frontController.log("bookmaks not allowed", null);
		return null;
	}
	
	static void writeBookmark(String[] bookmarks, FrontController frontController) throws IOException {
		if (bookmarks.length > DataConv.toIntWithDefault(frontController.getProperty("max_bookmarks", "8"), 8))
			throw new IOException("too many elements");
		Properties props = new Properties();
		for(int ind=0; ind < bookmarks.length; ind++) {
			props.setProperty("bm"+(ind+1), bookmarks[ind]);
		}
		props.storeToXML(new FileOutputStream(getBookmarksFile(frontController)), "Bookmars "+bookmarks.length, "UTF-8");
	}
	
	static File getBookmarksFile(FrontController frontController) {
		String homeDir = System.getProperty("user.home");
		return new File(homeDir, ".bookmarks_" + frontController
				.getServletContext().getContextPath().substring(1) // context "" (ROOT) is not allowed
				+ ".properties");
	}
	
	public static class Webelement {
		public String name;

		public String path;
	}
	

	public class Webfile extends Webelement {
		
		public Webfile(Path p, String bp) throws IOException {
			name = p.getFileName().toString();
			path = bp + p.toString();
			String sep = FileSystems.getDefault().getSeparator();
			String tp = getConfigValue(TOPFOLDER, sep);
			int a = 0;
			if (tp.endsWith(sep))
				a = -1;
			// TODO figure out if file system is case sensitive
			if (path.startsWith(tp))
				path = path.substring(tp.length() + a);
			else
				path = tp;
			archive = Files.isRegularFile(p) && isZip(name) && p.getFileSystem().equals(FileSystems.getDefault());
			try {
				//Files.readAttributes(p, DosFileAttributes.class);
				Map<String, Object> fa = Files.readAttributes(p,
						"lastModifiedTime,isDirectory,isSymbolicLink,isRegularFile,size");
				// log("%s attrs %s",null, p, fa);
				folder = (Boolean) fa.get("isDirectory") || archive;
				size = (Long) fa.get("size");
				last_mod = new Date(((FileTime) fa.get("lastModifiedTime")).toMillis());
				// bug 7168172 on Windows 7 System.getProperty("os.name").equals("Windows 7")
				unaccessible = Files.isReadable(p) == false;
				//log("access %b for %s", null, unaccessible, p);
				try {
					owner = Files.getOwner(p).toString();
					permissions = PosixFilePermissions.toString(Files.getPosixFilePermissions(p));
				} catch (UnsupportedOperationException | FileSystemException uoe) {
					char[] perm_array = new char[3];
					if (Files.isReadable(p))
						perm_array[0] = 'r';
					else
						perm_array[0] = '-';
					if (Files.isWritable(p))
						perm_array[1] = 'w';
					else
						perm_array[1] = '-';
					if (Files.isExecutable(p))
						perm_array[2] = 'x';
					else
						perm_array[2] = '-';
					permissions = new String(perm_array);
				}
				if (Files.isHidden(p))
					permissions += '.';
				else
					permissions += '-';
				if (archive)
					permissions = 'a'+permissions;
				else if (folder)
					permissions = 'd' + permissions;
				else
					permissions = '-' + permissions;
				
			} catch (FileSystemException ade) {
				// TODO rethrow as IO?
			}
		}

		public boolean folder;

		public long size;

		public Date last_mod;

		public String permissions;
		
		public boolean archive;
		
		public boolean unaccessible;
		
		public String owner;
	}
	
	protected static class WatchUpdater extends Thread implements Closeable {
		WatchService watchService;
		HashMap<Watchable,AsyncContext> updateQ;
		long markGen;
		//HashMap<String, Watchable>
		public WatchUpdater() throws IOException {
			setDaemon(true);
			setName("WatchUpdater");
			watchService = FileSystems.getDefault().newWatchService();
			updateQ = new HashMap<>();
			start();
		}
		
		@Override
		public void run() {
			for (;;) {
				try {
					WatchKey watchKey = watchService.take(); // poll(10, );
					processWatchKey(watchKey);
				} catch (InterruptedException ie) {
					break;
				}
			}
		}
		
		@Override
		public void close() throws IOException  {
			for(AsyncContext ac:updateQ.values()) {
				try {
					ac.getResponse().getOutputStream().close();
				} catch (IOException e1) {
	
				}
				ac.complete();
			}
			updateQ.clear();
			watchService.close();
		}
		
		String newMark() {
			return String.valueOf(++markGen);
		}
		
		synchronized void releaseMarked(String pageMark) {
			Iterator< Entry<Watchable, AsyncContext>> i = updateQ.entrySet().iterator();
			while(i.hasNext()) {
				Entry<Watchable, AsyncContext> e = i.next();
				AsyncContext ac = e.getValue();
				if (pageMark.equals(ac.getRequest().getParameter("page-mark"))) {
					//System.err.printf("Removed entry marked %s%n", pageMark);
					try {
						ac.getResponse().getOutputStream().close();
					} catch (IOException e1) {
		
					}
					ac.complete(); 
					i.remove();
				}
			}
		}

		synchronized void addToUpdate(Watchable p, AsyncContext ac) throws IOException {
			AsyncContext oac = updateQ.remove(p);
			if (oac != null) {
				try {
					oac.getResponse().getOutputStream().close();
				} catch (IOException e1) {
	
				}
				oac.complete();
			} else {
				/*final WatchKey watchKey1 =*/
				p.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
				/*ac.addListener(new AsyncListener() {
					WatchKey watchKey = watchKey1;
					@Override
					public void onComplete(AsyncEvent ae) throws IOException {
						watchKey.cancel();
					}

					@Override
					public void onError(AsyncEvent ae) throws IOException {
						onComplete(ae);
					}

					@Override
					public void onStartAsync(AsyncEvent ae) throws IOException {
					}

					@Override
					public void onTimeout(AsyncEvent ae) throws IOException {
						onComplete(ae);
					}
					// TODO cancel key on timeout
				});*/
			}
			ac.setTimeout(5*60*1000);
//			if (updateQ.containsKey(p))
	//			throw new RuntimeException("Already there"+p);
			updateQ.put(p, ac);
		}

		private AsyncContext pickAsyncContext(Watchable path) {
			//System.err.printf(">>>Picking ctx for %s%n", path);
			return updateQ.remove(path);
		}
		
		private void processWatchKey(WatchKey watchKey) {
			//System.err.printf(">>>Processing key %s%n", watchKey);
			if (watchKey == null)
				return;
			AsyncContext ac = pickAsyncContext(watchKey.watchable());
			//System.err.printf(">>>CTX %s%n", ac);
			if (ac == null) {
				watchKey.cancel();
				return;
			}
			StringBuffer sb = new StringBuffer("[{\"eventHandler\":\"notifyModifications\",\"parameters\":[");
			StringBuffer evb = new StringBuffer();
			for (WatchEvent<?> event : watchKey.pollEvents()) {
				evb.append("{\"").append(event.kind()).append("\":\"");
				evb.append(HttpUtils.toJavaString("" + event.context())).append("\"},");
			}
			//System.err.printf("Events: %s%n", evb);
			if (evb.length() > 0) {
				sb.append(evb.substring(0, evb.length() - 1));
				watchKey.cancel();
			} else if (!watchKey.reset()) {
			}
			sb.append("]}]");
			try {
				PrintWriter pw = ac.getResponse().getWriter();
				pw.write(sb.toString());
				pw.close();
				ac.complete();
			} catch (IOException e) {

			}
		}
	}

	public Webelement[] splitPath(String path) {
		if (path == null)
			return new Webelement[0]; // null can be good as well
		String[] paths = path.split(File.separatorChar == '\\' ? "\\\\" : "" + File.separatorChar);
		Webelement[] result = new Webelement[paths.length == 0 ? 1 : paths.length];
		result[0] = new Webelement();
		result[0].name = getConfigValue(TOPFOLDER, FileSystems.getDefault().getSeparator());
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
	
	/** used as result for processing file operations
	 * 
	 * @author dmitiry
	 *
	 */
	public static class RequestTransalated implements AutoCloseable {
		String reqPath; // path as it comes from request to file system
		Path transPath; // path in target file system
		Path[] transPaths;
		@Override
		public void close() throws Exception {
			if (transPath != null)
				try {
					//System.err.printf("<<<FS %s closed%n", transPath.getFileSystem());
					transPath.getFileSystem().close();
				} catch (UnsupportedOperationException uoe) {
				}
			transPath = null;
		}
		
		@Override
		public String toString() {
			return "RequestTransalated [reqPath=" + reqPath + ", transPath=" + transPath + ", transPaths="
					+ Arrays.toString(transPaths) + "]";
		}
		
	}
	
	/** processes web arguments and translate them in local file system
	 * Path values
	 * @param topPath root directory of exposed file system, can be any level to restrict access
	 * @param webPath file path from web more likely with / and relative to top path
	 * @param webPaths multiple web paths can be specified for bulk processing
	 * @return RequestTransalated containing web path converted to underneath file system which can be
	 * not default one, for example zip. The best guess is performed for bulk operations to find out if
	 * all path are part of same file system. reqPath keeps path to new file system as a single file
	 * @throws IOException
	 */
	public static RequestTransalated translateReq(String topPath, String ...webPaths) throws IOException {
		if (webPaths == null || webPaths.length == 0 )
			throw new IllegalArgumentException("Inconsistency in using arguments, no web paths specified");
		//System.out.printf("translated called top %s, paths %s%n", topPath, Arrays.toString(webPaths));
		RequestTransalated result = new RequestTransalated();
		FileSystem fs = FileSystems.getDefault();
		char psc = fs.getSeparator().charAt(0);
		result.reqPath = "";
		String sp = getLongestBegining(webPaths);
		boolean partsOfDir = false;
		if (sp.isEmpty() == false) {
			Path p = fs.getPath(topPath, sp);
			//System.out.printf("path %s for %s / %s%n", p, topPath, sp);
			if (Files.isRegularFile(p)) {
				if (!isZip(p.getFileName().toString()))
					partsOfDir = true;
			} else if (Files.isDirectory(p))
				partsOfDir = true;
		} else
			partsOfDir = true;
		//System.out.printf("common %s --- %s parts %b%n", sp, result.reqPath, partsOfDir);
		if (!partsOfDir) {
			String[] begParts = sp.split("/");
			sanitize(begParts);
			String[] zipParts = new String[0];			// find first zip in the path
			do {
				try {
					Path p = Paths.get(topPath, begParts);
					//System.out.printf("Parts from %s are %s%n", p, Arrays.toString(begParts));
					if (Files.exists(p)) {
						if (Files.isRegularFile(p)) {
							// check if it's zip
							if (isZip(p.getFileName().toString())) {
								result.transPath = p;
								try {
									fs = FileSystems.newFileSystem(result.transPath, null);
									result.reqPath = result.transPath.toString();
								} catch(ProviderNotFoundException pnfe) {
									log("File system not found for " + result.transPath, pnfe);
									throw new IOException("File system not found for " + result.transPath, pnfe);
								}
								
								break;
							} 
						} else { // it exists but not zip
							result.transPath = fs.getPath(topPath, sp); // consider creation not in zip
							//System.out.printf("trans path %s from %s%n", result.transPath, sp);
							result.transPaths = new Path[1]; // ignore others because not in zip
							result.transPaths[0] = result.transPath;
							return result;
						}
					} else {
						if (begParts.length <= 2)
							throw new IOException("File not found for " + result.transPath);
					}
					zipParts = insertFirst(zipParts, begParts[begParts.length - 1]);
					//System.out.printf("Zip parts are %s after inserting %s%n", Arrays.toString(zipParts), begParts[begParts.length - 1]);
					begParts = removeLast(begParts);
				} catch(InvalidPathException ipe) {
					//zipParts = insertFirst(zipParts, begParts[begParts.length - 1]);
					// reduce number of parts
					//begParts = removeLast(begParts);
				}
			} while (begParts.length > 1);
			
			// based that real path parts and inside zip parts transPaths
			// result.transPath = fs.getPath(topPath, begParts);
			result.transPaths = new Path[webPaths.length];
			//result.transPaths [0] = fs.getPath("/", zipParts);
			for(int i=0; i<webPaths.length;i++) {
				String parts[] = webPaths[i].split("/");
				//System.out.printf("Parts %d of %s%n", parts.length, Arrays.toString(parts));
				parts = Arrays.copyOfRange(parts, begParts.length, parts.length); // can be an exception
				//System.out.printf("Zip parts are %s after inserting %s%n",
				sanitize(parts);
				result.transPaths [i] = fs.getPath("/", parts);
				if (i == 0)
					result.transPath = result.transPaths [i];
			}
		} else {
			result.transPath = fs.getPath(topPath, sp);
			result.transPaths = new Path[webPaths.length];
			//System.out.printf("ordinary %s --- %s%n", sp, result.transPath);
			for(int i=0; i<webPaths.length;i++) {
				sp = DataConv.ifNull(webPaths[i], "");
				result.transPaths [i] = fs.getPath(topPath, sp.replace('/', psc));
			}
		}
		//System.out.printf("!!!Trans returned %s%n", result);
		return result;
	}
	
	public static String toNumInUnits(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	static String getLongestBegining(String...s) {
		if (s == null || s.length == 0 || s[0] == null)
			return "";
		String result = s[0];
		int m = result.length();
		for (int i=1; i<s.length; i++) {
			int l = 0, ls = 0;
			
			while(l < result.length() && l < s[i].length() && result.charAt(l) == s[i].charAt(l)) {
				if (result.charAt(l) == '\\' || result.charAt(l) == '/')
					ls = l;
				l++;
			}
			result = s[i];
			if (ls < m)
				m = ls;
		}
		if (result.length() == m)
			return result;
		return result.substring(0,m);
	}
	
	public static boolean isZip(String name) {
		name = name.toLowerCase();
		for (String ext : ZIP_EXT)
			if (name.endsWith(ext))
					return true;
		return false;
	}
	
	public static void sanitize(String[] parts) throws IOException {
		for (String part: parts)
			if (part.equals(".") || part.equals(".."))
				throw new IOException("Invalid path element with dots");
	}
	
	// these methods have to be parts of util
	
	public static String[] removeLast(String[] original) {
		return removeElement(original, original.length-1);
	}
	
	public static String[] insertFirst(String[] original, String element) {
		return insertElement(original, element, 0);
	}
	
	public static String[] removeElement(String[] original, int element){
		if (original.length == 0)
			throw new IllegalArgumentException("The array is alredy empty");
	    String[] result = new String[original.length - 1];
	    System.arraycopy(original, 0, result, 0, element );
	    if (element < original.length-1)
	    System.arraycopy(original, element+1, result, element, original.length - element-1);
	    return result;
	}
	
	public static String[] insertElement(String[] original, String element, int index){
		if (index > original.length || index < 0)
			throw new IllegalArgumentException("Invalid index");
	    String[] result = new String[original.length + 1];
	    System.arraycopy(original, 0, result, index+1, index );
	    result[index] = element;
	    //System.out.printf("Setting %s at %d%n", element, index);
	    if (index < original.length)
	    	System.arraycopy(original, index, result, index + 1, original.length - index);
	    return result;
	}
	
	public static void main(String...args) throws IOException {
		System.out.printf("%s,  %s, %s, %s%n", DataConv.toStringInUnits(387l), 
				 DataConv.toStringInUnits(387*1000l), DataConv.toStringInUnits(387*1000*1000l),
				 DataConv.toStringInUnits(387*1000*1000*1000l));
		System.out.printf("Enter blank separated web paths%n");
		Scanner sc= new Scanner(System.in); 
		String str= sc.nextLine();   
		String webparts[] = str.split(";") ;
		RequestTransalated rt = translateReq("/", webparts);
		System.out.printf("Processed %s%n", rt);
	}
}
