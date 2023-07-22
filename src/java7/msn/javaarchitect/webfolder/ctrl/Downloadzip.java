// Copyright 2009 Dmitriy Roatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.aldan3.util.Stream;
import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.model.Appearance;

/** Java 7 code for file downloading
 * 
 * @author dmitriy
 *
 */
public class Downloadzip extends com.beegman.webbee.block.Stream {
	@Override
	protected void fillStream(OutputStream os) throws IOException {
		String range = req.getHeader("Range");
		if (range != null)
			throw new IOException("Range isn't supported");
		String[] selection = req.getParameterValues("files");
		String topf = getConfigValue(Folder.TOPFOLDER, File.separator);
		if (selection == null || selection.length == 0) {
			resp.setContentType("text/plain; charset=UTF-8");
			os.write("Nothing is selected to download, hit back and check off some files.".getBytes(Folder.DEF_CHARSET));
			return;
		}
		// for(String el:selection)
		// log("downloading.."+el, null);
		try (Folder.RequestTransalated rt = Folder.translateReq(topf, selection);) {
			Path p = rt.transPath;

			if (selection.length == 1) {
				if (rt.reqPath.isEmpty() == false && rt.transPaths[0].getParent() == null) {
					p = FileSystems.getDefault().getPath(rt.reqPath);
					rt.close();
				}
				if (Files.isRegularFile(p)) {
					String name = p.getFileName().toString();
					if (Files.isReadable(p) == false) {
						message(os, "Can't read %s, hit back.", name);
						return;
					}
					resp.setHeader("Content-Length", "" + Files.size(p));
					if (appearance != Appearance.mobile || userAgent.indexOf("iPad") > 0) {
						resp.setContentType("application/octet-stream"); // since going to download
						if (userAgent.indexOf("MSIE") > 0)
							name = URLEncoder.encode(name, CharSet.UTF8).replace('+', ' ');
						//if (name.length() > 77)
							//name = HttpUtils.quoted_printableEncode(name, Folder.DEF_CHARSET);
						resp.setHeader("Content-disposition", "attachment; filename=\"" + name + "\"; filename*=UTF-8'"+URLEncoder.encode(name, "UTF-8"));
					} else {
						// figure first if type is supported
						String ct = Files.probeContentType(p);
						if (ct == null)
							ct = "text/plain";
						if (ct.startsWith("text/plain") || ct.startsWith("text/html") || ct.startsWith("image/")
								|| ct.startsWith("application/xml") || ct.startsWith("text/xml"))
							resp.setContentType(ct);
						else if (ct.startsWith("application/vnd.android.package-archive")) {
							resp.setContentType(ct);
							resp.setHeader("Content-disposition", "attachment; filename=\"" + name + '"');
						} else {
							message(os, "This file type %s is not supported by the mobile platfrom.", ct);
							return;
						}
					}
					// TODO add range operation
					Files.copy(p, os);
					os.flush();
					return;
				}
			}
			// multiple selection
			if (appearance != Appearance.mobile || userAgent.indexOf("iPad") > 0) {
				resp.setContentType("application/octet-stream");
				// resp.setContentType("application/x-zip-compressed");
				String zipFaliName = String
						.format("zipped-%d-on-%tF(%2$tR).zip", selection.length, System.currentTimeMillis());
				// TODO do entire value in one format
				resp.setHeader("Content-disposition", "attachment; filename=\"" + zipFaliName + "\"");
			} else {
				message(os, "You can't download multiple files in archive format to mobile device");
				return;
			}
			try (ZipOutputStream zs = new ZipOutputStream(os);) {
				zs.setLevel(ZipOutputStream.STORED);
				zs.setComment(String.format(
						"Generated by WebFolde (java 7)r %s, Copyright (c) 2009, 2013 by Dmitriy Rogatkin",
						getConfigValue("version", "")));
				zipFiles(zs, rt.transPaths);
			} catch (IOException ioe) {
				throw ioe;
			}

		} catch (Exception e) {
			log("", e);
			message(os, "%s", e);
			return;
		}
	}

	@Override
	protected String getConfigValue(String name, String defVal) {
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal));
	}

	protected void message(OutputStream os, String m, Object...params) throws IOException {
		resp.setContentType("text/plain; charset=UTF-8");
		byte[] content = String.format(m, params).getBytes(Folder.DEF_CHARSET);
		resp.setContentLength(content.length);
		os.write(content);
	}
	
	void zipFiles(final ZipOutputStream zs, Path... selection) throws IOException {
		for (final Path p : selection) {
			if (Files.isRegularFile(p) && Files.isReadable(p)) {
				addZipEntry(p, p.getParent(), zs);
			} else if (Files.isDirectory(p)) {
				//log("gong in:%s", null, p);
				Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						addZipEntry(file, p.getParent(), zs);
						return FileVisitResult.CONTINUE;
					}
				});
			}
		}
	}
	
	void addZipEntry(Path p, Path rp,  ZipOutputStream zs) throws IOException {
		ZipEntry e = new ZipEntry(rp.relativize(p).toString().replace('\\', '/'));
		//log("!!!!!!!!!!!!!processing file %s at %s %s", null, p, rp, e);
		e.setTime(Files.getLastModifiedTime(p).toMillis());
		zs.putNextEntry(e);
		Files.copy(p, zs);
		zs.closeEntry();		
	}
}
