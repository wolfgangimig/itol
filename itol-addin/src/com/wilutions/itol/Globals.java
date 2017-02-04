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
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.joa.OfficeAddinUtil;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;

public class Globals {

	private static OutlookAddinEx addin;
	private static MailExport mailExport = new MailExport();
	private static ResourceBundleNoThrow resb;
	private static volatile IssueService issueService;
	private static volatile boolean issueServiceRunning;
	private static File __tempDir;
	private static Logger log = Logger.getLogger("Globals");

	private static AppInfo appInfo = new AppInfo();

	public static AppInfo getAppInfo() {
		return appInfo;
	}

	public static void setAppInfo(AppInfo config) {
		appInfo = config;
	}

	protected static void setThisAddin(OutlookAddinEx addin) {
		Globals.addin = addin;
	}

	private static void initIssueService(boolean async) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(");

		Globals.issueServiceRunning = false;

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(" );

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
			issueService = fact.getService(appInfo.getAppDir(), appInfo.getServiceFactoryParams());
			
			initProxy();

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "issueService.setConfig");
			issueService.setConfig(appInfo.getConfig());

			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initializing...");
			issueService.initialize();
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initialized.");
			System.out.println("Issue service initialized.");

			issueServiceRunning = true;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to initialize issue service", e);
			throw e;
		}
	}

	private static void initProxy() {
		Config config = appInfo.getConfig();
		String redmineUrl = config.getServiceUrl().toLowerCase();
		String httpProtocol = redmineUrl.indexOf("https") == 0 ? "https" : "http";
		String proxyHost = config.getProxyServer();
		boolean proxyServerEnabled = config.isProxyServerEnabled(); 
		int proxyPort = config.getProxyServerPort();
		String proxyUserName = config.getProxyServerUserName(); 
		String proxyPassword = PasswordEncryption.decrypt(config.getProxyServerEncryptedUserPassword());

		if (!proxyServerEnabled) {
			proxyHost = "";
			proxyPort = 0;
			proxyUserName = "";
			proxyPassword = "";
		}
		else {
			
			Authenticator.setDefault(new Authenticator() {
			    @Override
			    protected PasswordAuthentication getPasswordAuthentication() {
			        if (getRequestorType() == RequestorType.PROXY) {
			            String prot = getRequestingProtocol().toLowerCase();
			            String host = System.getProperty(prot + ".proxyHost", "");
			            String port = System.getProperty(prot + ".proxyPort", "80");
			            String user = System.getProperty(prot + ".proxyUser", "");
			            String password = System.getProperty(prot + ".proxyPassword", "");
			            if (getRequestingHost().equalsIgnoreCase(host)) {
			                if (Integer.parseInt(port) == getRequestingPort()) {
			                    return new PasswordAuthentication(user, password.toCharArray());
			                }
			            }
			        }
			        return null;
			    }
			});
		}
		
		System.setProperty(httpProtocol + ".proxySet", Boolean.toString(proxyServerEnabled)); 
		System.setProperty(httpProtocol + ".proxyHost", proxyHost); 
		System.setProperty(httpProtocol + ".proxyPort", Integer.toString(proxyPort));
		System.setProperty(httpProtocol + ".proxyUser", proxyUserName);
		System.setProperty(httpProtocol + ".proxyPassword", proxyPassword);
		

	}

	public static boolean isIssueServiceRunning() {
		return issueServiceRunning;
	}
	
	public static void initialize(boolean async) throws Exception {
		initLogging();
		initIssueService(async);
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
			resb.addBundle("com.wilutions.itol.res", Globals.class.getClassLoader());
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
			byte[] buf = OfficeAddinUtil.getResourceAsBytes(IssueTaskPane.class, "version.properties");
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

			String logLevel = getAppInfo().getConfig().getLogLevel();
			String logFile = getAppInfo().getConfig().getLogFile();

			if (logLevel != null && !logLevel.isEmpty() && logFile != null && !logFile.isEmpty()) {
				logFile = logFile.replace('\\', '/');
				logprops = MessageFormat.format(logprops, logLevel, logFile);
				ByteArrayInputStream istream = new ByteArrayInputStream(logprops.getBytes());
				LogManager.getLogManager().readConfiguration(istream);

				for (Handler handler : Logger.getLogger("").getHandlers()) {
					handler.setFormatter(new LogFormatter());
				}

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
