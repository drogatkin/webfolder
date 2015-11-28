// Copyright 2009 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;

import org.aldan3.util.DataConv;

import com.beegman.webbee.block.Message;

public class Delete extends Message {
	static final String PATH_SEPARATOR = "\\?";
	@Override
	protected void doApprovedAction() {
		String[] selection = getParameterValue("payload", "", 0).split(
				PATH_SEPARATOR);
		File folder = new File(getConfigValue("TOPFOLDER", File.separator));
		if (selection[0].indexOf("..") >= 0)
			return;
		File root = new File(folder, selection[0]);
		selection[0] = root.getName();
		root = root.getParentFile();
		for (int i = 1; i < selection.length; i++) {
			if (selection[i].indexOf("..") >= 0)
				return;
			selection[i] = new File(folder, selection[i]).getName();
		}
		if (deleteFiles(root, selection) == false)
			log("Can't delete:" + root, null);

	}
	
	@Override
	protected String getConfigValue(String name, String defVal) {
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal));
	}

	static boolean deleteFiles(File folder, String[] files) {
		for (String fn : files) {
			File f = new File(folder, fn);
			if (f.isDirectory()) {
				boolean res = deleteFiles(f, f.list());
				// log("deleted "+f, null);
				if (res == false)
					return false;
				res = f.delete();
				if (res == false)
					return false;
			} else {
				boolean res = f.delete();
				// log("deleted "+f, null);
				if (res == false) {
					//log("Can not delete:" + f, null);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected String getTitle() {
		return "Deleting files";
	}

	@Override
	protected String getMessage() {
		return "<pre>Please confirm that the following files will be deleted:?"
				+ DataConv.arrayToString(req.getParameterValues("files"), '\n')
				+ "</pre>";
	}

	@Override
	protected String getPayload() {
		return DataConv.arrayToString(req.getParameterValues("files"),
				PATH_SEPARATOR.charAt(1));
	}

}
