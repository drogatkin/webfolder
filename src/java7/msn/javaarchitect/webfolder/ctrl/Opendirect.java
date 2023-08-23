// Copyright 2012 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.io.File;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;

import javax.servlet.http.HttpServletResponse;

import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.block.Stream;

/** Java 7 version to directly download files
 * 
 * */
public class Opendirect extends Stream {

	@Override
	protected void fillStream(OutputStream os) throws IOException {
		try (Folder.RequestTransalated rt = Folder.translateReq(getConfigValue(Folder.TOPFOLDER, File.separator), req.getPathInfo());) {
			Path fp = rt.transPath;
			if(rt.reqPath.isEmpty() == false && rt.transPath.getParent() == null) {
				fp = FileSystems.getDefault().getPath(rt.reqPath);
				rt.close();
			}
			if (Files.isRegularFile(fp) == false || Files.isReadable(fp) == false) {
				resp.setContentType("text/plain; charset=UTF-8");
				os.write(("The "+fp+" can't be read").getBytes(Folder.DEF_CHARSET));
				return;
			}
			//log("Loading " + fp, null);
			String contentType = Files.probeContentType(fp);
			if (contentType != null)
				contentType = frontController.getServletContext().getMimeType(fp.getFileName().toString());
			if (contentType != null)
				resp.setContentType(contentType);
			long flen = Files.size(fp);	
			resp.setDateHeader("Last-modified", Files.getLastModifiedTime(fp).toMillis());
			long[] rr = HttpUtils.parseRangeHeader(req.getHeader("Range"), flen);
			if (rr != null) {
				if (rr[1] > 0) {
					if (rr[0] > rr[1] || rr[1] >= flen) {
						// TODO If-Range presence can change behavior
						resp.setHeader("Content-Range", HttpUtils.BYTES_UNIT + " */" + flen);
						resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return;
					}
					try (InputStream is = Files.newInputStream(fp);)  {
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
					} 
					return;
				}
			}
			resp.setHeader("Accept-Ranges", HttpUtils.BYTES_UNIT);
			resp.setHeader("content-length", String.valueOf(flen));
			Files.copy(fp, os);
		} catch(Exception e) {
			if (e instanceof IOException)
				throw (IOException)e;
			log("", e);
		}
	}

	@Override
	protected String getConfigValue(String name, String defVal) {
		return Folder.getConfigValue(frontController, name, super.getConfigValue(name, defVal), req);
	}
}
