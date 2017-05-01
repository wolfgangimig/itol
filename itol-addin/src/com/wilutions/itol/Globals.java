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
import com.wilutions.com.DDAddinDll;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.itol.db.ProgressCallbackImpl;
import com.wilutions.joa.OfficeAddinUtil;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;

public class Globals {

	private static OutlookAddinEx addin;
	private static MailExport mailExport = new MailExport();
	private static ResourceBundleNoThrow resb;
	private static volatile IssueService issueService;
	private static volatile boolean issueServiceRunning;
	private static Logger log = Logger.getLogger("Globals");
	private static String PRODUCT_NAME = "ITOL";

	private static AppInfo appInfo = new AppInfo();

	public static AppInfo getAppInfo() {
		return appInfo;
	}

	public static void setAppInfo(AppInfo config) {
		appInfo = config;
		
		// Set DDAddin product name. It finds the license key under 
		// HKCU/Software/WILUTIONS/productName/License
		DDAddinDll.setProductName("DDAddin-" + config.getAppName());
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
			issueService.initialize(new ProgressCallbackImpl());
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
		
	
/*
 * http://www.pcwelt.de/ratgeber/Mit-Squid-als-Proxy-Server-schneller-im-Netzwerk-surfen-5896242.html
 * https://blog.mafr.de/2013/06/16/setting-up-a-web-proxy-with-squid/
 * 
 	
 	1. Installieren:
 	
 	1.1. Squid installieren
 	
 	sudo apt install squid
 	
 	1.2. Apache Hilfsmittel installieren, damit Kennwort-Datenbank erstellt werden kann
 	
 	sudo apt-get install apache2-utils 
 	
 	
 	2. Konfigurieren
 	
 	2.1. Original-Konfigurationsdatei sichern
 	
 	sudo cp /etc/squid/squid.conf /etc/squid/squid.conf.original
 	
 	2.2. Zugriffsrechte auf Konfigdatei für alle
 	
 	sudo chmod ugo+rwx ./squid.conf
 	
 	2.3. Zugriffsrechte auf Logdateien
 	
 	sudo chmod ugo+rwx /var/log/squid/access.log
 	sudo chmod ugo+rwx /var/log/squid/cache.log
 	
 	2.4. User für Basic-Authentication erstellen

 	Mit Basic-Authentication kann man sich über Java nicht mit dem Proxy verbinden. 
 	Deshalb besser 2.5.

 	sudo htpasswd -c /etc/squid/passwords squiduser
 	> squidpwd
 	
 	2.5. Digest Authentication
 	
 	cd /etc/squid
 	sudo touch passwd
 	sudo chown proxy.proxy passwd
 	sudo chmod 640 passwd
 	sudo htdigest -c /etc/squid/passwd SquidRealm squiduser
 	> squidpwd
 	
 	Prüfen:
 	sudo /usr/lib/squid/digest_file_auth /etc/squid/passwd
 	>"squiduser":"SquidRealm"
 	OK...
 	CTRL-D
 	
 	
 	
 	3.5. Konfigurationsdatei 

 	auth_param digest program /usr/lib/squid/digest_file_auth -c /etc/squid/passwd
 	auth_param digest realm SquidRealm
 	auth_param digest children 2

	acl auth_users proxy_auth REQUIRED
	
	http_access allow auth_users
	http_access deny all

 	http_port 3128
 	http_port 3129 intercept
 	
 	
 	
 	3. Squid neu starten
 	
 	sudo /etc/init.d/squid restart
 	
 	
 	4. Verwendung mit Chrome
 	
 	Einstellungen - Suche nach "proxy" - Windows-Einstellungen öffnen - LAN-Einstellungen
 	Proxyserver anhaken, Adresse und Port eingeben
 	
 	// -Djava.net.useSystemProxies=true
 		
 */
		
		Config config = appInfo.getConfig();
		String proxyHost = config.getProxyServer();
		boolean proxyServerEnabled = config.isProxyServerEnabled(); 
		int proxyPort = config.getProxyServerPort();
		final String proxyUserName = config.getProxyServerUserName(); 
		final String proxyPassword = PasswordEncryption.decrypt(config.getProxyServerEncryptedUserPassword());

		if (proxyServerEnabled) {
			log.info("Initialize proxy: proxyHost=" + proxyHost + ", proxyPort=" + proxyPort + ", proxyUser=" + proxyUserName);
				
			Authenticator.setDefault(new Authenticator() {
			    @Override
			    protected PasswordAuthentication getPasswordAuthentication() {
			    	PasswordAuthentication ret = null;
			        if (getRequestorType() == RequestorType.PROXY) {
			        	
			        	String requestingHost = getRequestingHost();
			        	int requestingPort = getRequestingPort();
			            String prot = getRequestingProtocol().toLowerCase();
			            
			            log.info("Proxy authentication: requestingHost=" + requestingHost + ", requestingProtocol=" + prot + ", requestingPort=" + requestingPort 
			            		+ ", proxyHost=" + proxyHost + ", proxyPort=" + proxyPort + ", proxyUser=" + proxyUserName);
						
			            if (proxyHost.isEmpty()) {
		                    ret = new PasswordAuthentication(proxyUserName, proxyPassword.toCharArray());
			            }
			            else {
			            	if (requestingHost.equalsIgnoreCase(proxyHost)) {
				                if (proxyPort == requestingPort) {
				                    ret = new PasswordAuthentication(proxyUserName, proxyPassword.toCharArray());
				                }
				            }
			            }
			        }
			        return ret;
			    }
			});

			if (proxyHost.isEmpty()) {
				// Use system proxies.
				// Check whether the program was started with this option.
				// It can only be set on the command line.
				String useSystemProxiesStr = System.getProperty("java.net.useSystemProxies");
				if (useSystemProxiesStr == null || useSystemProxiesStr.isEmpty()) {
					log.warning("Command line option -Djava.net.useSystemProxies=true has to be passed in order to use system proxies.");
				}
			}
			else {
				System.setProperty("http.proxyHost", proxyHost); 
				System.setProperty("http.proxyPort", Integer.toString(proxyPort));
				System.setProperty("https.proxyHost", proxyHost); 
				System.setProperty("https.proxyPort", Integer.toString(proxyPort));
			}
			
		}
		else {
			log.info("Proxy not used.");
		}
		
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
		
		// Purge temporary directory
		File tempDir = getAppInfo().getConfig().getTempDir();
		if (tempDir.getAbsolutePath().length() > 10) { // make sure the we do not delete c:\\
			purgeDirectory(tempDir);
		}
		
		DDAddinDll.closeLogFile();
	}
	
	private static void purgeDirectory(File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				purgeDirectory(f);
			}
			f.delete();
		}
		dir.delete();
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

			
			// Initialize DDAddin Logfile
			{
				File ddaddinLogFile = new File(new File(logFile).getParent(), "itol-ddaddin.log");
				DDAddinDll.openLogFile(ddaddinLogFile.getAbsolutePath(), logLevel, true);
			}

		}
		catch (Throwable e) {
			System.out.println("Logger configuration not found or inaccessible. " + e);
		}
		finally {
		}

	}

	public static File getTempDir() {
		return Globals.getAppInfo().getConfig().getTempDir();
	}

	public static String getProductName() {
		return PRODUCT_NAME;
	}
}
