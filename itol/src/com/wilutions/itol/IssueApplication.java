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
import com.wilutions.com.JoaDll;
import com.wilutions.com.reg.RegUtil;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.AddinApplication;

public class IssueApplication extends AddinApplication {

	private static Logger log = Logger.getLogger(IssueApplication.class.getName());

	static {
		Config config = Globals.getConfig();
		// config.appName = "Issue Tracker for Microsoft Outlook " +
		// System.getProperty("sun.arch.data.model") + "bit";
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
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "parseCommandLine(");
		boolean finished = super.parseCommandLine(args);
		if (!finished) {
			initIssueService();
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")parseCommandLine=" + finished);
		return finished;
	}

	private void initIssueService() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(");
		File appDir = getAppDir();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "appDir=" + appDir);
		try {
			Globals.initIssueService(appDir);
		}
		catch (Throwable e) {
			e.printStackTrace();
			log.severe(e.toString());
		}
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
		instance = this;
		super.start(primaryStage);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")start");
	}

	@Override
	protected void register(boolean userNotMachine, String execPath) {
		try {
			instance = this;
			super.register(userNotMachine, execPath);
			
			String linkName = registerAutostart(true);
			
			if (linkName != null && linkName.length() != 0) {
				showDocument(linkName);
			}

			// showDocument("http://www.wilutions.com/joa/itol/installed.html");
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to register Addin", e);
		}
	}

	@Override
	protected void unregister(boolean userNotMachine) {
		try {
			instance = this;
			
			registerAutostart(false);

			super.unregister(userNotMachine);
			// showDocument("http://www.wilutions.com/joa/itol/uninstalled.html");
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to register Addin", e);
		}
	}

	private static volatile IssueApplication instance;

	public static void showDocument(String url) {
		instance.getHostServices().showDocument(url);
	}

	private String registerAutostart(boolean registerNotUnregister) {
		String exe = RegUtil.getExecPath(IssueApplication.class);
		if (exe.startsWith("\"")) exe = exe.substring(1);
		if (exe.endsWith("\"")) exe = exe.substring(0, exe.length() - 1);
		String linkName = makeValidPath(AUTOSTART_FOLDER + "\\" + (new File(exe)).getName() + ".lnk");
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "linkName=" + linkName);

		// Im Autostart-Ordner ablegen
		if (registerNotUnregister) {
			try {
				String targetName = exe;
				String description = "";
				if (log.isLoggable(Level.INFO)) log
						.log(Level.INFO, "Create shortcut=" + linkName + " to " + targetName);
				
				// MISSING
				JoaDll.nativeCreateShortcut(linkName, targetName, description);
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, "Failed to create shortcut=" + linkName, e);
			}
		}

		// Im Autostart-Ordner löschen.
		else {
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Delete lnk=" + linkName);
			new File(linkName).delete();
		}

		// pushd "%APPDATA%\..\Local\Issue Tracker for Microsoft Outlook 32bit"
		// REG add
		// "HKCU\Software\Microsoft\Office\Outlook\Addins\ItolAddin.Class" /f /v
		// "LoadBehavior" /t REG_DWORD /d 3
		// START "" "Issue Tracker for Microsoft Outlook 32bit.exe"
		// popd

		return exe.toLowerCase().endsWith("exe") ? linkName : null;
	}

	/**
	 * Make valid path.
	 * 
	 * @param path
	 *            File system path that might contain environment variables.
	 * @return File system path with replaced variables.
	 */
	public static String makeValidPath(String path) {

		int p = path.indexOf('%');
		while (p >= 0) {

			int e = path.indexOf('%', p + 1);
			String variableName = path.substring(p + 1, e);
			String variableValue = System.getenv(variableName);
			if (variableValue == null) variableValue = "";
			path = path.replace(path.substring(p, e + 1), variableValue);

			p = path.indexOf('%');
		}

		return path;
	}
	
	private final static String AUTOSTART_FOLDER = "%APPDATA%\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
}
