/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.ComException;
import com.wilutions.com.reg.RegUtil;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.AddinApplication;

import javafx.stage.Stage;

public class IssueApplication extends AddinApplication {

	private static Logger log = Logger.getLogger(IssueApplication.class.getName());

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
		
		AppInfo appConfig = Globals.getAppInfo();
		// config.appName = "Issue Tracker for Microsoft Outlook " +
		// System.getProperty("sun.arch.data.model") + "bit";
		appConfig.setAppName("Issue Tracker for Microsoft Outlook");
		appConfig.setManufacturerName("WILUTIONS");
		appConfig.setServiceFactoryClass(IssueServiceFactory_JS.class.getName());
		appConfig.setServiceFactoryParams(Arrays.asList(IssueServiceFactory_JS.DEFAULT_SCIRPT));
		appConfig.setAppDir(getAppDir());
	}

	private static File getAppDir() {
		File ret = RegUtil.getAppPathIfSelfContained();
		if (ret == null) {
			ret = new File(System.getProperty("user.dir"));
		}
		return ret;
	}

	public boolean parseCommandLine(String[] args) throws ComException, IOException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "parseCommandLine(");
		boolean finished = super.parseCommandLine(args);
		if (!finished) {
			try {
				initIssueService();
			}
			catch (IOException e) {
				throw e;
			}
			catch (Throwable e) {
				throw new IOException(e);
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")parseCommandLine=" + finished);
		return finished;
	}

	private void initIssueService() throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(");
		Globals.initialize(true);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")initIssueService");
	}

	public static void main(String[] args) {

		Globals.initLogging();
		String javaHome = System.getProperty("java.home");
		log.info("java.home=" + javaHome);
		for (Object key : System.getProperties().keySet()) {
			log.info(key + "=" + System.getProperty((String) key));
		}

		log.info("app.dir=" + RegUtil.getAppPathIfSelfContained());

		try {
			log.info("call main");
			AddinApplication.main(IssueApplication.class, IssueApplication.class, args);
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to excecute main.", e);
		}

		log.info("main finished");

		Globals.releaseResources();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "start(");
		super.start(primaryStage);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")start");
	}

	@Override
	protected void register(boolean userNotMachine, String execPath) {
		try {
			super.register(userNotMachine, execPath);

			String linkName = registerAutostart(true, execPath);

//			if (linkName != null && linkName.length() != 0) {
//				showDocument(linkName);
//				showDocument("http://www.wilutions.com/joa/itol/installed.html");
//			}

		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to register Addin", e);
		}
	}

	@Override
	protected void unregister(boolean userNotMachine, String exePath) {
		try {
			String linkName = registerAutostart(false, exePath);

//			if (linkName != null && linkName.length() != 0) {
//				showDocument("http://www.wilutions.com/joa/itol/uninstalled.html");
//			}

			super.unregister(userNotMachine, exePath);

		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to register Addin", e);
		}
	}

	public static void showDocument(String url) {
		try {
			if (url.startsWith("file:/")) {
				File file = new File(new URI(url));
				
				// Open potentially dangerous files with notepad. 
				String ext_1 = MailAttachmentHelper.getFileExt(file.getName()).toLowerCase() + ".";
				if (Globals.getAppInfo().getConfig().getBlackExtensions().contains(ext_1)) {
					File dest = new File(file.getParentFile(), file.getName() + ".txt");
					dest.delete();
					file.renameTo(dest);
					file = dest;
				}
				
				// AWT opens the file more reliably
				Desktop.getDesktop().open(file);
			}
			else {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to opent document=" + url, e);
		}
	}

}
