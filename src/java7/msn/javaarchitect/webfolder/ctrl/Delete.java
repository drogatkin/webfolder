// Copyright 2009 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.aldan3.util.DataConv;
import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.block.Message;

/** Java 7 delete files version
 * 
 * @author Dmitriy
 *
 */
public class Delete extends Message {

	static final String PATH_SEPARATOR = "\t";
	@Override
	protected void doApprovedAction() {
	    //log("Payload :%s", null, getParameterValue("payload", "", 0));
		String[] selection = getParameterValue("payload", "", 0).split(
				PATH_SEPARATOR);
		if (selection.length == 0)
			return;
		String tps = getConfigValue(Folder.TOPFOLDER, File.separator);
		//log("Selection :%s", null, Arrays.toString(selection));
		if (selection.length == 1) {
			Path p = FileSystems.getDefault().getPath(tps, selection[0]);
			if (Files.isRegularFile(p)) {
				try {
					Files.delete(p);
				}catch(IOException e) {
					log("", e);
				}
				return;
			}
		}
		try (Folder.RequestTransalated rt = Folder.translateReq(tps, selection);) {
			/*if (selection.length == 1 && rt.reqPath.isEmpty() == false && rt.transPaths[0].getParent() == null) {
				rt.close();
				Files.delete(FileSystems.getDefault().getPath(rt.reqPath));
				return;
			}*/
			for (Path p : rt.transPaths) {
				if (Files.isDirectory(p))
					deleteDir(p);
				else
					Files.delete(p);
			}
		} catch (Exception e) {
			log("", e);
		}
	}
	
	void deleteDir(Path d) throws IOException {
		Files.walkFileTree(d, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
				if (e == null) {
					// log("delete %s", null, dir);
					if (dir.getParent() != null) {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					} else
						return FileVisitResult.TERMINATE;

				} else {
					// directory iteration failed
					throw e;
				}
			}
		});
	}
	
	@Override
	protected String getConfigValue(String name, String defVal) {
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal), req);
	}

	@Override
	protected String getTitle() {
		return "Deleting files";
	}

	@Override
	protected String getMessage() {
		return "<pre>Please confirm that the following files will be deleted:?"
				+ HttpUtils.htmlEncode(DataConv.arrayToString(req.getParameterValues("files"), '\n'))
				+ "</pre>";
	}

	@Override
	protected String getPayload() {
		return DataConv.arrayToString(req.getParameterValues("files"),
				PATH_SEPARATOR.charAt(0));
	}
}
