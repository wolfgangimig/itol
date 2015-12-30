package com.wilutions.redmineaddin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.reg.RegUtil;
import com.wilutions.itol.AppInfo;
import com.wilutions.itol.Globals;

public class IssueApplication extends com.wilutions.itol.IssueApplication {

	private static Logger log = Logger.getLogger(IssueApplication.class.getName());

	public IssueApplication() {
	}

	public static void main(String[] args) {

		// try {
		//
		// main2(args);
		//
		// AppInfo config = Globals.getAppInfo();
		// if (config != null) return;
		// }
		// catch (Exception e) {
		// return;
		// }

		String javaHome = System.getProperty("java.home");
		log.info("java.home=" + javaHome);
		for (Object key : System.getProperties().keySet()) {
			log.info(key + "=" + System.getProperty((String) key));
		}

		AppInfo config = Globals.getAppInfo();
		config.setAppName("Issue Tracker for Microsoft Outlook and Redmine");
		config.setManufacturerName("WILUTIONS");
		config.setServiceFactoryClass(IssueServiceFactory_JS.class.getName());

		Globals.getResourceBundle().addBundle("com/wilutions/redmineaddin/res_en.properties");

		// Initialized in IssueApplication.parseCommandLine 
//		try {
//			File workDir = new File(".");
//			Globals.initIssueService(workDir, true);
//		}
//		catch (Exception e1) {
//		}
//
		log.info("app.dir=" + RegUtil.getAppPathIfSelfContained());

		try {
			main(IssueApplication.class, IssueApplication.class, args);
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to excecute main.", e);
		}

		log.info("main finished");

		Globals.releaseResources();
	}
}
