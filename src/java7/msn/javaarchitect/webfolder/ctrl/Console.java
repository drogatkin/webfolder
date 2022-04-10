package msn.javaarchitect.webfolder.ctrl;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import org.aldan3.util.inet.HttpUtils;

import com.beegman.webbee.base.BaseBlock;
import com.beegman.webbee.model.AppModel;
import com.beegman.webbee.util.PageRef;

public class Console extends BaseBlock<AppModel> {
    
	public static String TOP_DIRECTORY = null;
	
	@Override
	protected Object doControl() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object getModel() {
		TOP_DIRECTORY = getConfigValue(Folder.TOPFOLDER, FileSystems.getDefault().getSeparator());
		HashMap<String, Object> pageModel = new HashMap<String, Object>(10);
		pageModel.put("user", System.getProperty("user.name"));
		
		//pageModel.put("test","this test");
		String webPath = req.getPathInfo();
		if (webPath == null)
			webPath = "/";
		FileSystem defFS = FileSystems.getDefault();
		Path path = defFS.getPath(webPath.replace('/', defFS.getSeparator().charAt(0)));
		PageRef[] toplinks = new PageRef[] {
				PageRef.create(req, "Folder", "Folder"+webPath),
				PageRef.create(req, "Info", "../sysinfo.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI())),
				PageRef.create(req, "Admin", "../admin.jsp?going_back=" + HttpUtils.urlEncode(req.getRequestURI()))
				 };
		pageModel.put(TOPLINKS, toplinks);
		pageModel.put("path", webPath);
		//pageModel.put("title", "WebFolder: terminal");
		return pageModel;
	}
	
	@Override
	protected String getTitle() {
		return "WebFolder: terminal";
	}

	@Override
	protected String getSubmitPage() {
		throw new UnsupportedOperationException();
	}

}
