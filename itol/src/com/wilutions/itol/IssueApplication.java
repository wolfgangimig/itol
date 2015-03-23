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
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.stage.Stage;

import com.wilutions.com.ComException;
import com.wilutions.com.reg.RegUtil;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.AddinApplication;


public class IssueApplication extends AddinApplication {
	
	private static Logger log = Logger.getLogger(IssueApplication.class.getName());

	static {
		Config config = Globals.getConfig();
		config.appName = "Issue Tracker for Microsoft Outlook and Redmine";
		config.manufacturerName = "WILUTIONS";
		config.serviceFactoryClass = IssueServiceFactory_JS.class.getName();
		config.serviceFactoryParams = Arrays.asList(IssueServiceFactory_JS.DEFAULT_SCIRPT);
		config.configProps = new ArrayList<Property>(0);
	}

	public static File getAppDir() {
		File ret = RegUtil.getAppPathIfSelfContained();
		if (ret == null) {
			ret = new File(System.getProperty("user.dir"));
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
			log.severe(e.toString());
		}
	}

	public static void main(String[] args) {
		
		Globals.initLogging();
		String javaHome = System.getProperty("java.home");
		log.info("java.home=" + javaHome);
		for (Object key : System.getProperties().keySet()) {
			log.info(key + "=" + System.getProperty((String)key));
		}
		
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

	@Override
	public void start(Stage primaryStage) throws Exception {
		instance = this;
		super.start(primaryStage);
	}

	@Override
	protected void register(boolean userNotMachine, String execPath) {
		try {
			instance = this;
			super.register(userNotMachine, execPath);
			
//		      String exe = RegUtil.getExecPath(RemoteTestMain.class);
//		      if (exe.startsWith("\"")) exe = exe.substring(1);
//		      if (exe.endsWith("\"")) exe = exe.substring(0, exe.length()-1);
//		      String linkName = makeValidPath(AUTOSTART_FOLDER + "\\" + (new File(exe)).getName() + ".lnk");
//		      if (log.isDebugEnabled()) log.debug("linkName=" + linkName);
//		      
//		      // Im Autostart-Ordner ablegen
//		      if (arg.equals("/Register")) {
//		        try {
//		          String targetName = exe;
//		          String description = "";
//		          if (log.isInfoEnabled()) log.info("Create shortcut=" + linkName + " to " + targetName);
//		          JoaDll.nativeCreateShortcut(linkName, targetName, description);
//		        } catch (Exception e) {
//		          log.error("Failed to create shortcut=" + linkName, e);
//		        }
//		      }
//		      
//		      // Im Autostart-Ordner löschen.
//		      else if (arg.equals("/Unregister")) {
//		        log.info("Delete lnk=" + linkName);
//		        new File(linkName).delete();
//		      }
			
			showDocument("http://www.wilutions.com/joa/itol/installed.html");
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to register Addin", e);
		}
	}
	
	@Override
	protected void unregister(boolean userNotMachine) {
		try {
			instance = this;
			super.unregister(userNotMachine);
			showDocument("http://www.wilutions.com/joa/itol/uninstalled.html");
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to register Addin", e);
		}
	}

	private static volatile IssueApplication instance;

	public static void showDocument(String url) {
		instance.getHostServices().showDocument(url);
	}
}
