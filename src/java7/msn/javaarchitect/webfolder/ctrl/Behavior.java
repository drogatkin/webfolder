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
		return Year.now().toString();
	}
}
