/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javafx.stage.Stage;

import com.wilutions.com.ComException;
import com.wilutions.com.JoaDll;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.AddinApplication;


public class IssueApplication extends AddinApplication {

	static {
		Config config = Globals.getConfig();
		config.appName = "Issue Tracker for Microsoft Outlook and Redmine";
		config.manufacturerName = "WILUTIONS";
		config.serviceFactoryClass = IssueServiceFactory_JS.class.getName();
		config.serviceFactoryParams = Arrays.asList(IssueServiceFactory_JS.DEFAULT_SCIRPT);
		config.configProps = new ArrayList<Property>(0);
	}

	public static File getAppDir() {
		File ret = new File(System.getProperty("user.dir"));
		String javaHome = System.getProperty("java.home");
		System.out.println("javaHome=" + javaHome);

		// Self-contained Java application?
		boolean isSelfContainedApp = javaHome.endsWith("runtime\\jre");
		System.out.println("isSelfContainedApp=" + isSelfContainedApp);

		if (isSelfContainedApp) {

			// Application is an EXE file found at "${java.home}/../../"
			File appDir = new File(javaHome).getParentFile().getParentFile();
			ret = new File(appDir, "app");
		}

		return ret;
	}

	public boolean parseCommandLine(String[] args) throws ComException, IOException {
		boolean finished = super.parseCommandLine(args);
		if (!finished) {
			initIssueService();
		}
		return finished;
	}

	private void initIssueService() {

		File appDir = getAppDir();
		try {
			Globals.initIssueService(appDir);
		}
		catch (IOException e) {
			e.printStackTrace();
			Logger log = Logger.getLogger(IssueApplication.class.getName());
			log.severe(e.toString());
		}
	}

	public static void main(String[] args) {

		main(IssueApplication.class, IssueApplication.class, args);

		Globals.releaseResources();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		instance = this;
		super.start(primaryStage);
	}

	private static volatile IssueApplication instance;

	public static void showDocument(String url) {
		instance.getHostServices().showDocument(url);
	}
}
