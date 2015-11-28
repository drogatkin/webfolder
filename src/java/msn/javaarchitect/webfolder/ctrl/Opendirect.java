// Copyright 2012 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.block.Stream;

public class Opendirect extends Stream {

	@Override
	protected void fillStream(OutputStream os) throws IOException {

		File folder = new File(getConfigValue("TOPFOLDER", File.separator));
		File openFile = new File(folder, req.getPathInfo().replace('/', File.separatorChar));
		if (openFile.getPath().startsWith(folder.getPath()) == false) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		try {
			openFile.getCanonicalPath();
		} catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden, exception:" + e);
			return;
		}
		// log("Loading " + openFile, null);
		String contentType = frontController.getServletContext().getMimeType(openFile.getName());
		if (contentType != null)
			resp.setContentType(contentType);
		long flen = openFile.length();
		resp.setDateHeader("Last-modified", openFile.lastModified());
		long[] rr = HttpUtils.parseRangeHeader(req.getHeader("Range"), flen);
		if (rr != null) {
			if (rr[1] > 0) {
				if (rr[0] > rr[1] || rr[1] >= flen) {
					// TODO If-Range presence can change behavior
					resp.setHeader("Content-Range", HttpUtils.BYTES_UNIT + " */" + flen);
					resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					return;
				}
				FileInputStream is = new FileInputStream(openFile);
				try {
					long sl = is.skip(rr[0]);
					if (rr[0] != sl) {
						resp.setHeader("Content-Range", HttpUtils.BYTES_UNIT + " */" + flen);
						resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return;
					}
					long clen = rr[1] - rr[0] + 1;
					resp.setHeader("content-length", String.valueOf(clen));
					resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
					resp.setHeader("Content-Range", HttpUtils.BYTES_UNIT + " " + rr[0] + '-' + rr[1] + '/' + flen);
					org.aldan3.util.Stream.copyStream(is, os, clen);
				} finally {
					is.close();
				}
				return;
			}
		}
		resp.setHeader("Accept-Ranges",HttpUtils.BYTES_UNIT);
		resp.setHeader("content-length", String.valueOf(flen));
		org.aldan3.util.Stream.copyFile(openFile, os);
	}

	@Override
	protected String getConfigValue(String name, String defVal) {
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal));
	}
}
