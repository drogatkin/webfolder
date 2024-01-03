// Copyright 2024 Dmitriy Rogatkin
package msn.javaarchitect.webfolder.ctrl;

import java.util.Calendar;

import com.beegman.webbee.base.BaseBehavior;

public class Behavior extends BaseBehavior {
	public Behavior() {
		isPublic = true;
		useLabels = false;
		useBreadCrumbs = false;
		ignoreSession = true;
	}
	
	public static String year() {
		return "" + Calendar.getInstance().get(Calendar.YEAR);
	}
}
