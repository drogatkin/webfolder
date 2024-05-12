// Copyright 2012 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.aldan3.annot.FormField;
import org.aldan3.annot.OptionMap;
import org.aldan3.annot.FormField.FieldType;
import org.aldan3.data.DODelegator;
import org.aldan3.data.util.FieldConverter;
import org.aldan3.data.util.FieldFiller;
import org.aldan3.util.Stream;
import org.aldan3.util.DataConv;

import com.beegman.webbee.block.Form;
import com.beegman.webbee.model.AppModel;
import com.beegman.webbee.util.GenericResourceOptions;
import com.beegman.webbee.util.Option;

import org.aldan3.model.Coordinator;

/**
 * Java 7 based Editor implementation
 * 
 * @author dmitriy
 *
 */
public class Editor extends Form<Editor.editing, AppModel> {
	public static final String AUTOSAVE = "AUTOSAVE";
	
	private String modified;

	@Override
	protected editing loadModel(editing model) {
		if (model.file != null) {
		    String file = model.file.replace(File.separatorChar, '/');
		    int lastSep = file.lastIndexOf('/');
		    
			String tpath = getConfigValue(Folder.TOPFOLDER, File.separator);
			try (Folder.RequestTransalated rt = Folder.translateReq(tpath, file);) {
			    model.referer = req.getContextPath() + req.getServletPath() + "/Folder"+URLEncoder.encode((lastSep > 0? file.substring(0, lastSep): file), "UTF-8");
				//System.out.printf("-->loadinging %s%n", model.file);
				Path filePath = rt.transPath;
				//log("ld editing %s as %s",null, model.file, filePath);
				int maxSize = 1024 * 1024;
				try {
					maxSize = Integer.parseInt(frontController.getProperty("MaxEditedSize", "" + (1024 * 1024)));
				} catch (Exception e) {

				}
				updateEditorType(model, rt.transPath);
				if (Files.isReadable(filePath)) {
					if (Files.isDirectory(filePath) == false) {
						//log("as  text %b%n", null, model.as_text);
						if (model.tail) {
							try (SeekableByteChannel sbc = Files.newByteChannel(filePath, StandardOpenOption.READ)) {
								long shift = sbc.size() - maxSize;
								ByteBuffer bb = null;
								if (shift > 0) {
									sbc.position(shift);
									model.partial = true;
									bb = ByteBuffer.allocate(maxSize);
								} else
									bb = ByteBuffer.allocate((int) sbc.size());

								sbc.read(bb);
								if (model.partial)
									model.content = "<Content of the file was truncated>\r\n";
								else
									model.content = "";
								model.content += new String(bb.array(), model.as_text ? "UTF-8" : "ISO-8859-1");
							}
						} else
							try (InputStream is = Files.newInputStream(filePath)) {
								long sz = Files.size(filePath);
								if (sz >= maxSize) {
									model.partial = true;
								}
								if (model.partial)
									model.content = "<Content of the file was truncated>\r\n";
								else
									model.content = "";
								model.content += Stream.streamToString(is, model.as_text ? "UTF-8" : "ISO-8859-1",
										maxSize);
								model.modified = Files.getLastModifiedTime(filePath).toMillis();
							} catch (IOException ioe) {
								log("", ioe);
								// MalformedInputException
								model.content = "" + ioe;
							}
					} else {
						model.content = "The file isn't readable";
					}
				} else {
					model.content = String.format("You are editing a new file %s which will be created upon saving",
							filePath);
				}
			} catch (Exception e) {
				log("", e);
				model.content = "" + e;
			}
		}
		model.do_autosave = getConfigValue(AUTOSAVE, "true");
		return model;
	}

	@Override
	protected editing getFormModel() {
		return new editing();
	}
	
	private final void updateEditorType(editing model, Path filePath) {
		String editExts = getConfigValue("ace_edit_exts", "");
		if (editExts.isEmpty() == false) {
			String n = filePath.toString().toLowerCase();
			int dp = n.lastIndexOf('.');
			if (dp > 0 && dp < n.length() - 1)
				model.editor = editExts.indexOf(n.substring(dp)) >= 0 ? "ace" : null;
		}
	}

	@Override
	protected Object storeModel(editing model) {
		if (model.file != null) {
			if (model.partial)
				return "Can't store a part without destroying rest of the file";
			String topFolder = getConfigValue(Folder.TOPFOLDER, FileSystems.getDefault().getSeparator());
			try (Folder.RequestTransalated rt = Folder.translateReq(topFolder,
					model.file.replace(File.separatorChar, '/'));) {
				Path filePath = rt.transPath;
				updateEditorType(model, filePath);
				model.referer = getStringParameterValue("referer", ".", 0);
				//log("editing %s in %s as %b top %s", null, filePath, rt.reqPath, model.as_text, topFolder);
				if (Files.isDirectory(filePath) || Files.isWritable(filePath) == false && Files.exists(filePath))
					return "The file isn't editable";
				//if (model.content.length() > 10)
					//return "Too big";
				if (Files.exists(filePath) && model.modified < Files.getLastModifiedTime(filePath).toMillis())
						return "File's already modified, <a href=\""+req.getContextPath()+req.getServletPath()+"/Editor?file="+URLEncoder.encode(model.file, "UTF-8") + "&as+text=" + (model.as_text?"true":"false") +"\">reread</a>";
				if (model.as_text && !model.eol_type.equals("N")) {
					
				}
				try (BufferedWriter osw = Files.newBufferedWriter(filePath, model.as_text ? Charset.forName("UTF-8")
						: Charset.forName("ISO-8859-1"));) {
					if (model.as_text) {
						String sep;
						switch(model.eol_type) {
						case "W":
							sep = "\r\n";
							break;
						case "L":
							sep = "\n";
							break;
						case "M":
							sep =  "\r";
							break;
					    default:
					    	sep = System.getProperty("line.separator");
						}
						model.content = model.content.replaceAll("\\R", sep);
					}
					osw.write(model.content);
					osw.flush();
					Path rp = FileSystems.getDefault().getPath(topFolder);
					//log("tf/tffs: %s::%s rt: %s p: %s", null, topFolder, rp, rt.reqPath, filePath);
					if (rt.reqPath.isEmpty())
						navigation = "Folder/"
								+ URLEncoder.encode(rp.relativize(filePath.getParent()).toString().replace('\\', '/'),
										"UTF-8");
					else
						navigation = "Folder/"
								+ URLEncoder
										.encode(rp
												.relativize(
														rp.getFileSystem().getPath(rt.reqPath,
																filePath.getParent().toString())).toString(), "UTF-8");
                    model.modified = Files.getLastModifiedTime(filePath).toMillis();
					modified = "OK "+model.modified;
					return "";
				} catch (IOException e) {
					log("Exception at saving file", e);
					return "Exception at saving a file: " + e;
				}
			} catch (Exception e) {
				log("", e);
				return "Exception at saving a file: " + e;
			}
		}

		return "Can't save the file, a wrong location";
	}
	
	@Override
	protected void addEnv(Object model, boolean ajaxView) {
		if (ajaxView)
			if (model instanceof Map && !((Map)model).containsKey(Variable.ERROR))
				((Map)model).put(Variable.ERROR, modified);
		super.addEnv(model, ajaxView);
	}	

	@Override
	protected String getTitle() {
		return "(" + getParameterValue("file", "", 0) + ")";
	}

	@Override
	protected String getConfigValue(String name, String defVal) {
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal), req);
	}

	public static final class editing {
		@FormField(presentSize = 120, presentRows = 42, presentStyle = "style=\"width:96%;margin-left:auto;margin-right:auto;height:100%;\"")
		public String content;
		@FormField
		public String file;
		//@FormField
		//public String root; // root directory
		@FormField(formFieldName = "as text")
		public boolean as_text;
		public String editor;
		public boolean partial;
		@FormField
		public boolean tail;
		@FormField(presentFiller=EolFiller.class)
		public String eol_type;
		@FormField
		public String do_autosave;
		@FormField(presentType=FieldType.Hidden)
		public long modified;
		@FormField(presentType=FieldType.Hidden)
		public String referer;
	}
	
	@OptionMap(valueMap = "id")
	public static final class EolFiller implements FieldFiller<DODelegator[], Object> {
		public DODelegator[] fill(Object modelObject, String filter) {
			return new DODelegator[] { new DODelegator(new Option<String, String>("N","As OS")),
					new DODelegator(new Option<String, String>("W","Windows")),
							new DODelegator(new Option<String, String>("L","Linux")),
									new DODelegator(new Option<String, String>("M", "Old Mac"))};
		}
	}

}
