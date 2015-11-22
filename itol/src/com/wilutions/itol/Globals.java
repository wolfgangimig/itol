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
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.com.reg.Registry;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.PropertyClasses;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.OfficeAddinUtil;


public class Globals {
	
	public final static String REG_CONFIG = "Config";
	public final static String REG_defaultIssueAsString = "defaultIssueAsString";
	public final static String REG_injectIssueIdIntoMailSubject = "injectIssueIdIntoMailSubject";

	private static ItolAddin addin;
	private static MailExport mailExport = new MailExport();
	private static ResourceBundle resb;
	private static volatile IssueService issueService;
	private static volatile boolean issueServiceRunning;
	private static Registry registry;
	private static File appDir;
	private static File __tempDir;
	private static Logger log = Logger.getLogger("Globals");

	private static Config config = new Config();

	public static Config getConfig() {
		return config;
	}

	protected static void setThisAddin(ItolAddin addin) {
		Globals.addin = addin;
	}

	public static Registry getRegistry() {
		if (registry == null) {
			registry = new Registry(config.manufacturerName, config.appName);
		}
		return registry;
	}

	public static void initIssueService(File appDir) throws IOException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(" + appDir);
		long t1, t2, t3;
		
		Globals.appDir = appDir;
		Globals.issueServiceRunning = false;

		t1 = System.currentTimeMillis();
		readData();
		t2 = System.currentTimeMillis();
		
		initLogging();
		t3 = System.currentTimeMillis();
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "readData ms=" + (t2-t1) + ", initLogging ms=" + (t3-t2));
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initIssueService(" + appDir);

		try {
			// Initialize in background
			CountDownLatch cdl =  new CountDownLatch(1);
			BackgTask.run(() -> {
				try {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getService");
					Class<?> clazz = Class.forName(config.serviceFactoryClass);
					IssueServiceFactory fact = (IssueServiceFactory) clazz.newInstance();
					issueService = fact.getService(appDir, config.serviceFactoryParams);
				
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "issueService.setConfig");
					issueService.setConfig(config.configProps);
					
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initializing...");
					issueService.initialize();
					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Issue service initialized.");
					System.out.println("Issue service initialized.");
				
					issueServiceRunning = true;
				}
				catch (Exception e) {
					log.log(Level.SEVERE, "Cannot initialize issue service", e);
				}
				finally {
					cdl.countDown();
				}
			});
			
//			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Waiting for initialized service...");
//			cdl.await(1, TimeUnit.SECONDS);
//			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Service initialized=" + issueServiceRunning);
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot initialize issue service", e);
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")initIssueService");
	}

	public static boolean isIssueServiceRunning() {
		return issueServiceRunning;
	}

	private static void readData() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "readData(");
		
		Config newConfig = (Config) getRegistry().read(REG_CONFIG);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newConfig=" + newConfig);

		if (newConfig != null) {
			
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newConfig.serviceFactoryClass=" + newConfig.serviceFactoryClass);
			if (newConfig.serviceFactoryClass != null) {
				config.serviceFactoryClass = newConfig.serviceFactoryClass;
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newConfig.serviceFactoryParams=" + newConfig.serviceFactoryParams);
			if (newConfig.serviceFactoryParams != null) {
				config.serviceFactoryParams = newConfig.serviceFactoryParams;
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newConfig.configProps=" + newConfig.configProps);
			if (newConfig.configProps != null) {
				config.configProps = newConfig.configProps;
			}
		}

		// Decrypt passwords
		for (Property configProp : config.configProps) {
			PropertyClass propClass = PropertyClasses.getDefault().get(configProp.getId());
			if (propClass != null && propClass.getType() == PropertyClass.TYPE_PASSWORD) {
				try {
					configProp.setValue(PasswordEncryption.decrypt((String) configProp.getValue()));
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		
		// Set default logging options if nessesary
		String logLevel = getConfigPropertyString(Property.LOG_LEVEL);
		String logFile = getConfigPropertyString(Property.LOG_FILE);
		if (logLevel.isEmpty()) {
			Property propLogLevel = new Property(Property.LOG_LEVEL, "SEVERE");
			config.configProps.add(propLogLevel);
		}
		if (logFile.isEmpty()) {
			File flog = new File(System.getProperty("java.io.tmpdir"), "itol.log");
			Property propLogFile = new Property(Property.LOG_FILE, flog.getAbsolutePath());
			config.configProps.add(propLogFile);
		}
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")readData");
	}

	private static void writeData() {
		for (Property configProp : config.configProps) {
			PropertyClass propClass = PropertyClasses.getDefault().get(configProp.getId());
			if (propClass != null && propClass.getType() == PropertyClass.TYPE_PASSWORD) {
				try {
					configProp.setValue(PasswordEncryption.encrypt((String) configProp.getValue()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		getRegistry().write(REG_CONFIG, config);
	}

	public static void setConfig(List<Property> configProps) throws IOException {
		config.configProps = configProps;
		initLogging();
		writeData();
		readData();
		issueService = new IssueServiceFactory_JS().getService(appDir, config.serviceFactoryParams);
		issueService.setConfig(config.configProps);
		issueService.initialize();
		issueServiceRunning = true;
	}

	public static ItolAddin getThisAddin() {
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

	public static ResourceBundle getResourceBundle() {
		if (resb == null) {
			try {
				ClassLoader classLoader = Globals.class.getClassLoader();
				InputStream inputStream = classLoader.getResourceAsStream("com/wilutions/itol/res_en.properties");
				resb = new ResourceBundleNoThrow(new PropertyResourceBundle(inputStream));
			} catch (IOException e) {
				e.printStackTrace();
				try {
					resb = new PropertyResourceBundle(new ByteArrayInputStream(new byte[0]));
				} catch (IOException never) {
				}
			}
		}
		return resb;
	}

	public static File getTempDir() {
		if (__tempDir == null) {
			try {
				__tempDir = File.createTempFile("itol", ".tmp");
				__tempDir.delete();
				__tempDir.mkdirs();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return __tempDir;
	}

	public static String getVersion() {
		String ret = "";
		try {
			byte[] buf = OfficeAddinUtil.getResourceAsBytes(BackstageConfig.class,
					"version.properties");
			Properties props = new Properties();
			props.load(new ByteArrayInputStream(buf));
			ret = props.getProperty("Version");
		} catch (IOException e) {
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
	
	public static Property getConfigProperty(String propId) {
		Property ret = null;
		for (Property prop : config.configProps) {
			if (prop.getId().equals(propId)){
				ret = prop;
				break;
			}
		}
		return ret;
	}
	
	private static String getConfigPropertyString(String propId) {
		Property prop = getConfigProperty(propId);
		return prop != null ? (String)prop.getValue() : "";
	}
	
	public static void initLogging() {
		try {
			ClassLoader classLoader = Globals.class.getClassLoader();
			String logprops = OfficeAddinUtil.getResourceAsString(classLoader, "com/wilutions/itol/logging.properties");
			
			String logLevel = getConfigPropertyString(Property.LOG_LEVEL);
			String logFile = getConfigPropertyString(Property.LOG_FILE);
			
			if (logLevel == null || logLevel.isEmpty()) logLevel = "INFO";
			if (logFile == null || logFile.isEmpty()) {
				logFile = File.createTempFile("itol", ".log").getAbsolutePath();
				logLevel = "FINE";
			}
			
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
			
		} catch (Throwable e) {
			System.out.println("Logger configuration not found or inaccessible. " + e);
		} finally {
		}

	}
}
