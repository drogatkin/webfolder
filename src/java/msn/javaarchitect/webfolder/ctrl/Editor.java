// Copyright 2012 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.aldan3.annot.FormField;
import org.aldan3.annot.FormField.FieldType;
import org.aldan3.data.util.FieldConverter;
import org.aldan3.servlet.Constant.Variable;
import org.aldan3.util.DataConv;
import org.aldan3.util.Stream;

import com.beegman.webbee.block.Form;
import com.beegman.webbee.model.AppModel;

public class Editor extends Form<Editor.editing, AppModel> {
	
	private String modified;

	@Override
	protected editing loadModel(editing model) {
		if (model.file != null) {
			String path = new File(getConfigValue("TOPFOLDER", File.separator)).getPath();
			String filePath = model.file.getPath();
			if (filePath != null && filePath.length() > 0 && filePath.indexOf("..") < 0) {
				int maxSize = 1024 * 1024;
				try {
					maxSize = Integer.parseInt(frontController.getProperty("MaxEditedSize", "" + (1024 * 1024)));
				} catch (Exception e) {

				}
				File editFile = new File(path, filePath);
				if (editFile.exists()) {
					if (editFile.isDirectory() == false) {
						if (editFile.canRead()) {
							FileInputStream fis = null;
							try {
								model.content = Stream.streamToString(fis = new FileInputStream(editFile), "UTF-8",
										maxSize);
								if (editFile.length() > maxSize) {
									model.content += "\r\n<Content of the file was truncated>";
									model.partial = true;
								}
								model.modified = editFile.lastModified();
							} catch (IOException ioe) {
								model.content = "" + ioe;
							} finally {
								if (fis != null)
									try {
										fis.close();
									} catch (Exception e) {

									}
							}
						} else {
							model.content = "The file isn't readable";
						}
					} else
						model.content = "The file is a directory";
				} else {
					model.content = String.format("You are editing new file %s which will be created upon saving",
							filePath);
				}
			}
		}
		return model;
	}

	@Override
	protected editing getFormModel() {
		return new editing();
	}

	@Override
	protected Object storeModel(editing model) {
		if (model.file != null) {
			String path = new File(getConfigValue("TOPFOLDER", File.separator)).getPath();
			String filePath = model.file.getPath();
			// log("File as param:"+model.file+", and as path:"+filePath, null);
			if (filePath != null && filePath.length() > 0 && filePath.indexOf("..") < 0) {
				File editFile = new File(path, filePath);
				if (editFile.isDirectory() || (editFile.exists() && editFile.canWrite() == false))
					return "The file isn't editable";
				if (model.modified < editFile.lastModified())
					return "File's already modified, reread";
				OutputStreamWriter osw = null;
				try {
					navigation = "Folder/"
							+ URLEncoder.encode(DataConv.ifNull(new File(filePath.substring(1)).getParent(), ""),
									"UTF-8");
					osw = new OutputStreamWriter(new FileOutputStream(editFile), model.as_text?"UTF-8":"ISO-8859-1");
					if (model.as_text)
						model.content = model.content.replaceAll("(\\r)?\\n", System.getProperty("line.separator"));
					osw.write(model.content);
					osw.flush();
					model.modified = editFile.lastModified();
					modified = "OK "+model.modified;
					return "";
				} catch (Exception e) {
					log("", e);
					return "Exception at saving file: " + e;
				} finally {
					try {
						osw.close();
					} catch (Exception e) {

					}
				}
			}
		}
		return "Can't save file, wrong location";
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
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal));
	}

	public static final class editing {
		@FormField(presentSize = 120, presentRows = 34)
		public String content;
		@FormField(/* presentType = FieldType.Hidden, */converter = File2Str.class)
		public File file;
		@FormField(formFieldName="as text")
		public boolean as_text;
		public boolean partial;
		@FormField(presentType=FieldType.Hidden)
		public long modified;
	}

	public static class File2Str implements FieldConverter<File> {

		@Override
		public File convert(String arg0, TimeZone arg1, Locale arg2) {
			return new File(arg0);
		}

		@Override
		public String deConvert(File arg0, TimeZone arg1, Locale arg2) {
			return arg0.getPath();
		}

	}
}
