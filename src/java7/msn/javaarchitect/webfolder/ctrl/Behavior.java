// Copyright 2012 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.time.Year;

import com.beegman.webbee.base.BaseBehavior;

public class Behavior extends BaseBehavior {
	public Behavior() {
		isPublic = true;
		useLabels = false;
		useBreadCrumbs = false;
		ignoreSession = true;
	}
	
	public static String year() {
		if (getVersion() < 8) 
			return "2024";
		return Year.now().toString();
	}
	
	private static int getVersion() {
	    String version = System.getProperty("java.version");
	    if(version.startsWith("1.")) {
	        version = version.substring(2, 3);
	    } else {
	        int dot = version.indexOf(".");
	        if(dot != -1) { version = version.substring(0, dot); }
	    } return Integer.parseInt(version);
	}
}
