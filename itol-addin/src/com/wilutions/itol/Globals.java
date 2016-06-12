/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.com.reg.Registry;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;
import com.wilutions.itol.db.Property;
import com.wilutions.joa.OfficeAddinUtil;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;

public class Globals {

	public final static String REG_CONFIG = "Config";
	public final static String REG_defaultIssueAsString = "defaultIssueAsString";

	private static OutlookAddinEx addin;
	private static MailExport mailExport = new MailExport();
	private static ResourceBundleNoThrow resb;
	private static volatile IssueService issueService;
	private static volatile boolean issueServiceRunning;
	private static Registry registry;
	private static File appDir;
	private static File __tempDir;
	private static Logger log = Logger.getLogger("Globals");

	private static AppInfo appInfo = new AppInfo();

	public static AppInfo getAppInfo() {
		return appInfo;
	}

	protected static void setThisAddin(OutlookAddinEx addin) {
		Globals.addin = addin;
	}

	public static Registry getRegistry() {
		if (registry == null) {
			registry = new Registry(appInfo.getManufacturerName(), appInfo.getAppName());
		}
		return registry;
	}

	public static void initIssueService(File appDir, boolean async) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(" + appDir);
		long t1, t2, t3;

		Globals.appDir = appDir;
		Globals.issueServiceRunning = false;

		t1 = System.currentTimeMillis();
		readData();
		t2 = System.currentTimeMillis();

		initLogging();
		t3 = System.currentTimeMillis();

		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "readData ms=" + (t2 - t1) + ", initLogging ms=" + (t3 - t2));

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(" + appDir);

		try {
			if (async) {
				BackgTask.run(() -> {
					try {
						internalInitIssueService();
					}
					catch (Throwable e) {

					}
				});
			}
			else {
				internalInitIssueService();
			}

			// if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Waiting for
			// initialized service...");
			// cdl.await(1, TimeUnit.SECONDS);
			// if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Service
			// initialized=" + issueServiceRunning);

		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Cannot initialize issue service", e);
			throw e;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")initIssueService");

	}

	private static void internalInitIssueService() throws Exception {
		try {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getService");
			Class<?> clazz = Class.forName(appInfo.getServiceFactoryClass());
			IssueServiceFactory fact = (IssueServiceFactory) clazz.newInstance();
			issueService = fact.getService(appDir, appInfo.getServiceFactoryParams());

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "issueService.setConfig");
			issueService.setConfig(appInfo.getConfigProps());

			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initializing...");
			issueService.initialize();
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initialized.");
			System.out.println("Issue service initialized.");

			issueServiceRunning = true;
		}
		catch (Exception e) {
			throw e;
		}
	}

	public static boolean isIssueServiceRunning() {
		return issueServiceRunning;
	}

	private static void readData() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "readData(");

		AppInfo newConfig = (AppInfo) getRegistry().read(REG_CONFIG);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newConfig=" + newConfig);

		if (newConfig != null) {

			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "newConfig.getServiceFactoryClass()=" + newConfig.getServiceFactoryClass());
			if (newConfig.getServiceFactoryClass() != null) {
				appInfo.setServiceFactoryClass(newConfig.getServiceFactoryClass());
			}

			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "newConfig.serviceFactoryParams=" + newConfig.getServiceFactoryParams());
			if (newConfig.getServiceFactoryParams() != null) {
				appInfo.setServiceFactoryParams(newConfig.getServiceFactoryParams());
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newConfig.configProps=" + newConfig.getConfigProps());
			if (newConfig.getConfigProps() != null) {
				appInfo.setConfigProps(newConfig.getConfigProps());
			}
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")readData");
	}

	public static void writeData() {
		getRegistry().write(REG_CONFIG, appInfo);
	}

	public static void setConfig(List<Property> configProps) throws Exception {
		appInfo.setConfigProps(configProps);
		initLogging();
		writeData();
		readData();
		initIssueService(appDir, false);
	}

	public static OutlookAddinEx getThisAddin() {
		return addin;
	}

	public static MailExport getMailExport() {
		return mailExport;
	}

	public static IssueService getIssueService() throws IOException {
		if (issueService == null) {
			throw new IOException("Issue service not initialized.");
		}
		return issueService;
	}

	public static ResourceBundleNoThrow getResourceBundle() {
		if (resb == null) {
			resb = new ResourceBundleNoThrow();
			resb.addBundle("com/wilutions/itol/res_en.properties");
		}
		return resb;
	}

	public static File getTempDir() {
		if (__tempDir == null) {
			try {
				__tempDir = File.createTempFile("itol", ".tmp");
				__tempDir.delete();
				__tempDir.mkdirs();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return __tempDir;
	}

	public static String getVersion() {
		String ret = "";
		try {
			byte[] buf = OfficeAddinUtil.getResourceAsBytes(BackstageConfig.class, "version.properties");
			Properties props = new Properties();
			props.load(new ByteArrayInputStream(buf));
			ret = props.getProperty("Version");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static void releaseResources() {
		if (__tempDir != null) {
			__tempDir.delete();
			__tempDir = null;
		}
	}

	public static void initLogging() {
		try {
			ClassLoader classLoader = Globals.class.getClassLoader();
			String logprops = OfficeAddinUtil.getResourceAsString(classLoader, "com/wilutions/itol/logging.properties");

			String logLevel = getAppInfo().getLogLevel();
			String logFile = getAppInfo().getLogFile();

			if (logLevel != null && !logLevel.isEmpty() && logFile != null && !logFile.isEmpty()) {
				logFile = logFile.replace('\\', '/');
				logprops = MessageFormat.format(logprops, logLevel, logFile);
				ByteArrayInputStream istream = new ByteArrayInputStream(logprops.getBytes());
				LogManager.getLogManager().readConfiguration(istream);
				Logger log = Logger.getLogger(Globals.class.getName());
				log.info("Logger initialized");
			}
			else {
				Logger.getLogger("").setLevel(Level.SEVERE);
			}

		}
		catch (Throwable e) {
			System.out.println("Logger configuration not found or inaccessible. " + e);
		}
		finally {
		}

	}

}
