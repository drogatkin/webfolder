package msn.javaarchitect.webfolder.ctrl;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import org.aldan3.util.inet.Base64Codecs;
import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.base.BaseBlock;
import com.beegman.webbee.model.AppModel;
import com.beegman.webbee.util.PageRef;

public class Console extends BaseBlock<AppModel> {
    
	
	public static String USER;
	public static String PASSWORD;
	
	@Override
	protected Object doControl() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object getModel() {
		String auth = req.getHeader("Authorization"); // Ok if exception		
		auth = Base64Codecs.base64Decode(
				auth.substring(auth.indexOf(' ') + 1),
				Base64Codecs.UTF_8);
		int i = auth.indexOf(':');
		USER = auth.substring(0, i);
		PASSWORD = auth.substring(i + 1);
		
//		readExtConfig(req.getServletContext());
        Folder.getConfigValue(frontController, Folder.TOPFOLDER, FileSystems.getDefault().getSeparator(), req);
		HashMap<String, Object> pageModel = new HashMap<String, Object>(10);
		pageModel.put("user", System.getProperty("user.name"));
		
		String webPath = req.getPathInfo();
		if (webPath == null)
			webPath = "/";
		FileSystem defFS = FileSystems.getDefault();
		Path path = defFS.getPath(webPath.replace('/', defFS.getSeparator().charAt(0)));
		PageRef[] toplinks = new PageRef[] {
				PageRef.create(req, "Folder", "Folder"+HttpUtils.urlEncode(webPath)),
				PageRef.create(req, "Info", "../sysinfo.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI())),
				PageRef.create(req, "Admin", "../admin.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI()))
				 };
		pageModel.put(TOPLINKS, toplinks);
		pageModel.put("path", webPath);

		return pageModel;
	}
	
	@Override
	protected String getTitle() {
		return "Terminal - WebFolder";
	}

	@Override
	protected String getSubmitPage() {
		throw new UnsupportedOperationException();
	}

}
