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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.com.DDAddinDll;
import com.wilutions.com.reg.RegUtil;
import com.wilutions.fx.util.ManifestUtil;
import com.wilutions.fx.util.ProgramVersionInfo;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;
import com.wilutions.itol.db.LoggerConfig;
import com.wilutions.itol.db.Profile;
import com.wilutions.itol.db.ProgressCallbackImpl;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;

public class Globals {

	private static OutlookAddinEx addin;
	private static MailExport mailExport = new MailExport();
	private static ResourceBundleNoThrow resb;
	
	/**
	 * CF that is completed if all connections have been checked.
	 * true: if all connections are successful OR if a message box has already been shown about failed connections.
	 * false: if at least one connection has failed.
	 * exception: if none of the connections succeeded.
	 */
	private static final CompletableFuture<Boolean> allIssueServicesConnected = new CompletableFuture<>();
	private static Logger log = Logger.getLogger("Globals");

	private static AppInfo appInfo = new AppInfo();

	public static AppInfo getAppInfo() {
		return appInfo;
	}

	public static void setAppInfo(AppInfo config) {
		appInfo = config;
	}

	protected static void setThisAddin(OutlookAddinEx addin) {
		ProgramVersionInfo versionInfo = ManifestUtil.getProgramVersionInfo(addin.getClass());
		log.info("Addin=" + versionInfo.getName() + ", version=" + versionInfo.getVersion());
		Globals.addin = addin;
	}

	public static void initialize(boolean async) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(");

		try {
			if (async) {
				BackgTask.run(() -> {
					try {
						internalInitIssueServices();
					}
					catch (Throwable e) {

					}
				});
			}
			else {
				internalInitIssueServices();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Cannot initialize issue service", e);
			throw e;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")initIssueService");

	}
	
	public static IssueService createIssueService(Profile profile) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "createIssueService(" + profile);

		String serviceFactoryClass = profile.getServiceFactoryClass();
		List<String> serviceFactoryParams = profile.getServiceFactoryParams();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getService class=" + serviceFactoryClass);
		
		Class<?> clazz = Class.forName(serviceFactoryClass);
		IssueServiceFactory fact = (IssueServiceFactory) clazz.newInstance();
		IssueService issueService = fact.getService(appInfo.getAppDir(), serviceFactoryParams);
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "issueService.setConfig");
		issueService.setProfile(profile);

		if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initializing...");
		issueService.initialize(new ProgressCallbackImpl());
		if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initialized.");
		
		System.out.println("Initialized issue service for profile=" + profile);
		issueService.getProfile().setIssueService(issueService);
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")createIssueService");
		return issueService;
	}

	/**
	 * Connects to all profiles.
	 * @throws Exception If connection to any profile failed.
	 */
	private static void internalInitIssueServices() throws Exception {
		List<Profile> profiles = getAppInfo().getConfig().getProfiles();
		List<CompletableFuture<Profile>> profilesCompletedList = new ArrayList<>();
		
		// Connect to profiles asynchronously.
		for (int i = 0; i < profiles.size(); i++) {
			final int profileIndex = i;
			CompletableFuture<Profile> profileCompleted = CompletableFuture.supplyAsync(() -> {
				Profile profile = profiles.get(profileIndex);
				try {
					// Connect.
					IssueService issueService = createIssueService(profile);
					
					// Replace profile object in array
					Profile acceptedProfile = issueService.getProfile();
					synchronized(profiles) {
						profiles.set(profileIndex, acceptedProfile);
					}
					
					return acceptedProfile;
				}
				catch (RuntimeException e) {
					log.log(Level.SEVERE, "Failed to initialize issue service for profile=" + profile, e);
					throw e;
				}
				catch (Exception e) {
					log.log(Level.SEVERE, "Failed to initialize issue service for profile=" + profile, e);
					throw new RuntimeException(e);
				}
			});
			profilesCompletedList.add(profileCompleted);
		}
		
		// Wait for all profiles connected or failed.
		if (profilesCompletedList.isEmpty()) {
			allIssueServicesConnected.complete(true);
		}
		else {
			CompletableFuture<Void> allProfilesCompleted = CompletableFuture.allOf(profilesCompletedList.toArray(new CompletableFuture[0]));
			allProfilesCompleted.handle((_void, ex) -> {
				
				// If all connections failed, complete the member allIssueServicesConnected exceptionally.
				// If at least one connection is established, complete either with true or false.
				// If all profiles are connected, complete with true.
				// If one profile failed, complete with false.
				boolean succ = ex == null;
				Throwable ex2 = null;
				if (!succ) {
					Optional<Profile> anyConnectedProfileOpt = profiles.stream().filter((p) -> p.isConnected()).findAny();
					ex2 = anyConnectedProfileOpt.isPresent() ? null : ex;
				}
				if (ex2 != null) {
					allIssueServicesConnected.completeExceptionally(ex);
				}
				else {
					allIssueServicesConnected.complete(succ);
				}
				return 0;
			});
		}
	}

	public static OutlookAddinEx getThisAddin() {
		return addin;
	}

	public static MailExport getMailExport() {
		return mailExport;
	}

	public static IssueService getIssueService() throws Exception {
		IssueService issueService = null;
		Profile profile = getAppInfo().getConfig().getCurrentProfile();
		issueService = profile.getIssueService();
		if (issueService == null) {
			throw new IOException("No issue service connected.");
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

	public static void releaseResources() {
		
		// Purge temporary directory
		File tempDir = getAppInfo().getConfig().getTempDir();
		if (tempDir.getAbsolutePath().length() > 10) { // make sure the we do not delete c:\\
			purgeDirectory(tempDir);
		}
		
		DDAddinDll.closeLogFile();
	}
	
	private static void purgeDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					purgeDirectory(f);
				}
				f.delete();
			}
			dir.delete();
		}
	}
	
	public static void initProxy() {
		getAppInfo().getConfig().getProxyServer().init();
	}

	public static void initLogging() {
		LoggerConfig loggerConfig = getAppInfo().getConfig().getLoggerConfig();
		loggerConfig.init();
		
		if (addin != null) {
			ProgramVersionInfo versionInfo = ManifestUtil.getProgramVersionInfo(addin.getClass());
			log.info("Addin=" + versionInfo.getName() + ", version=" + versionInfo.getVersion());
		}
		
		String logFileNameWithoutExt = new File(loggerConfig.getFile()).getName();
		{
			int p = logFileNameWithoutExt.lastIndexOf('.');
			if (p >= 0) logFileNameWithoutExt = logFileNameWithoutExt.substring(0, p);
		}
		
		// Initialize logging for DDAddin and JOA running in Outlook.exe.
		// Has only an effect after Outlook is re-started.
		String file = loggerConfig.getFile();
		String level = loggerConfig.getLevel();
		setLogFileForHelperAddin("DnD to HTML5 Addin for Microsoft Outlook", file, level, logFileNameWithoutExt + "-outlook-ddaddin.log", loggerConfig.isAppend());
		setLogFileForHelperAddin("JOA\\JoaUtilAddin", file, level, logFileNameWithoutExt + "-outlook-joa.log", loggerConfig.isAppend());
	}

	private static void setLogFileForHelperAddin(String name, String logFile, String logLevel, String addinLog, boolean append) {
		String regKey = "HKCU\\Software\\" + getAppInfo().getManufacturerName() + "\\" + name;
		String joaLogFile = new File(new File(logFile).getParent(), addinLog).getAbsolutePath();
		RegUtil.setRegistryValue(regKey, "LogFile", joaLogFile);
		String logLevelKey = logLevel.equals("FINE") ? "DEBUG" : "INFO";
		RegUtil.setRegistryValue(regKey, "LogLevel", logLevelKey);
		RegUtil.setRegistryValue(regKey, "LogAppend", Boolean.toString(append));
	}


	public static File getTempDir() {
		return Globals.getAppInfo().getConfig().getCurrentProfile().getTempDir();
	}
	
	public static CompletableFuture<Boolean> getAllIssueServicesConnected() {
		return allIssueServicesConnected;
	}

}
