/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import com.wilutions.com.ComException;
import com.wilutions.com.reg.RegUtil;
import com.wilutions.fx.util.ManifestUtil;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.Profile;
import com.wilutions.joa.AddinApplication;

import javafx.application.Application;
import javafx.stage.Stage;

public class IssueApplication extends AddinApplication {

	private static Logger log = Logger.getLogger(IssueApplication.class.getName());

	static {
		
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
		
		SimpleFormatter fmt = new SimpleFormatter();

		 StreamHandler sh = new StreamHandler(System.out, fmt);
		 log.setUseParentHandlers(false);
		 log.addHandler(sh);
		
	}

	public boolean parseCommandLine(String[] args) throws ComException, IOException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "parseCommandLine(");
		boolean finished = super.parseCommandLine(args);
		if (!finished) {
			try {
				initIssueServices();
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

	private void initIssueServices() throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(");
		Globals.initialize(true);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")initIssueService");
	}
	
	public static void main(String[] args) {
		main(IssueApplication.class, IssueApplication.class, args);
	}

	public static void main(Class<? extends AddinApplication> mainClass, Class<? extends Application> fxappClass, String[] args) {
	
		log.info("Application args=" + Arrays.toString(args));
				
		String javaHome = System.getProperty("java.home");
		log.info("java.home=" + javaHome);
		for (Object key : System.getProperties().keySet()) {
			log.info(key + "=" + System.getProperty((String) key));
		}

		try {
			AppInfo appInfo = new AppInfo();
			
			appInfo.setManufacturerName("WILUTIONS");
			appInfo.setAppName(ManifestUtil.getProgramName(mainClass));
			appInfo.setAppDir(RegUtil.getAppPathIfSelfContained());
			
			log.info("appInfo.appName=" + appInfo.getAppName());
			log.info("appInfo.appDir=" + appInfo.getAppDir());
			
			// Read configuration
			Config config = Config.read(appInfo.getManufacturerName(), appInfo.getAppName());
			appInfo.setConfig(config);

			// Maybe initialize a new profile if the configuration is empty.
//			Profile profile = config.getCurrentProfile();
//			if (profile.isNew()) {
//				profile.setServiceFactoryClass(Profile.JIRA_SERVICE_CLASS);
//				profile.setProfileName("JIRA");
//				config.setCurrentProfile(profile);
//			}
			
//			try {
//				config.write();
//			}
//			catch (Throwable e) {
//				e.printStackTrace();
//			}

			
			Globals.setAppInfo(appInfo);
			Globals.initProxy();
			Globals.initLogging();

			AddinApplication.main(mainClass, fxappClass, args);
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to excecute main.", e);
		}

		log.info("main finished");

		Globals.releaseResources();
	}

	@SuppressWarnings("unused")
	private static void redirectStdStreamsToFile() {
		File stdoutFile = new File(Profile.DEFAULT_TEMP_DIR, "itol-stdout.txt");
		File stderrFile = new File(Profile.DEFAULT_TEMP_DIR, "itol-stderr.txt");
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(stdoutFile))));
			System.out.println("STDOUT started.");
			System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(stderrFile))));
			System.out.println("STDERR started.");
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
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
			log.fine("autostart link=" + linkName);

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
			log.fine("autostart link=" + linkName);

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
				if (file.isFile() && isPotentiallyDangerousFile(file)) {
					ProcessBuilder pb = new ProcessBuilder("notepad.exe", file.getAbsolutePath());
					pb.start();
				}
				else {
					// AWT opens the file more reliably than JavaFX
					Desktop.getDesktop().open(file);
				}
			}
			else {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to opent document=" + url, e);
		}
	}

	private static boolean isPotentiallyDangerousFile(File file) {
		String ext_1 = MailAttachmentHelper.getFileExt(file.getName()).toLowerCase() + ".";
		return Globals.getAppInfo().getConfig().getCurrentProfile().getExtensionsAlwaysOpenAsText().contains(ext_1);
	}

}
