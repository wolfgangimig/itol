package com.wilutions.itol.db;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wilutions.joa.TaskPanePosition;
import com.wilutions.joa.gson.GsonBuilderJoa;

/**
 * An object of this class stores configuration options.
 * Options are first read from a file defined by system option "com.wilutions.itol.appConfig".
 * This file might contain the options that are the same for all users. In a second step,
 * options are read from %APPDATA%/manufacturerName/appName/user-profile.json. This options
 * shall be user related and have precedence over the application options.
 * When writing options, only the user-profile.json is updated.
 */
public class Config implements Serializable, Cloneable {
	private static final long serialVersionUID = -5838975424557674776L;

	/**
	 * System option that specifies the applications configuration file.
	 * This system option is optional. If not set, only the user related options are read.
	 */
	public final static String SYSTEM_OPTION_CONFIG_FILE = "com.wilutions.itol.config";
	
	/**
	 * Application configuration file name.
	 * Options are read from this file before user related options are read. 
	 * User related options have precedence.
	 * When configuration is written, this file keeps unchanged, file {@link #CONFIG_FILE_APPLICATION_TEMPLATE}
	 * is written instead.
	 */
	public final static String CONFIG_FILE_APPLICATION_READ = "application.json";

	/**
	 * Write application related values into this file.
	 */
	public final static String CONFIG_FILE_APPLICATION_WRITE = "application.json.templ";

	/**
	 * User configuration file name.
	 */
	public final static String CONFIG_FILE_USER = "user.json";

	private transient String manufacturerName;
	private transient String appName;
	
	/**
	 * Configuration file version.
	 */
	private int version = 1;

	// Application or user options.
	private String serviceUrl = "";
	private String issueIdMailSubjectFormat = Property.ISSUE_ID_MAIL_SUBJECT_FORMAT_DEFAULT;
	private Boolean injectIssueIdIntoMailSubject = Boolean.FALSE;
	private IdName msgFileFormat = MsgFileFormat.DEFAULT;
	private String logLevel = "INFO";
	private int nbOfSuggestions = 20;
	private String proxyServer = "";
	private boolean proxyServerEnabled;
	private int proxyServerPort;
	private TaskPanePosition taskPanePosition;
	
	/**
	 * Custom field for mail address.
	 */
	private String autoReplyField = "";

	// Only user related options.
	private String userName = "";
	private String encryptedPassword = "";
	private String credentials = "";
	private String logFile = "";
	private String exportAttachmentsDirectory = "";
	private String proxyServerUserName = "";
	private String proxyServerEncryptedUserPassword = "";
	
	public Config() {
		logFile = new File(System.getProperty("java.io.tmpdir"), "itol.log").getAbsolutePath();
		exportAttachmentsDirectory = System.getProperty("java.io.tmpdir");
		userName = proxyServerUserName = System.getProperty("user.name");
		taskPanePosition = new TaskPanePosition();
	}
	
	@Override
	public Object clone() {
		Config config = new Config();
		config.copyFrom(this);
		return config;
	}
	
	protected void copyFrom(Config rhs) {
		this.manufacturerName = rhs.manufacturerName;
		this.appName = rhs.appName;
		this.version = rhs.version;
		this.serviceUrl = rhs.serviceUrl;
		this.issueIdMailSubjectFormat = rhs.issueIdMailSubjectFormat;
		this.injectIssueIdIntoMailSubject = rhs.injectIssueIdIntoMailSubject;
		this.userName = rhs.userName;
		this.encryptedPassword = rhs.encryptedPassword;
		this.msgFileFormat = rhs.msgFileFormat;
		this.logLevel = rhs.logLevel;
		this.logFile = rhs.logFile;
		this.nbOfSuggestions = rhs.nbOfSuggestions;
		this.exportAttachmentsDirectory = rhs.exportAttachmentsDirectory;
		this.credentials = rhs.credentials;
		this.proxyServerUserName = rhs.proxyServerUserName;
		this.proxyServerEncryptedUserPassword = rhs.proxyServerEncryptedUserPassword;
		this.proxyServer = rhs.proxyServer;
		this.proxyServerEnabled = rhs.proxyServerEnabled;
		this.proxyServerPort = rhs.proxyServerPort;
		this.taskPanePosition = rhs.taskPanePosition;
		this.autoReplyField = rhs.autoReplyField;
	}

	public static <T extends Config> T read(String manufacturerName, String appName, Class<T> clazz) throws Exception {
		T config = null;

		// Read application configuration
		File applicationConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_APPLICATION_READ);
		try {
			String optFile = System.getProperty(SYSTEM_OPTION_CONFIG_FILE);
			if (!Default.value(optFile).isEmpty()) {
				applicationConfigFile = new File(optFile);
			}

			System.out.println("INFO: Application configuration file=" + applicationConfigFile + ", exists=" + applicationConfigFile.exists());
			if (applicationConfigFile.exists()) {
				config = read(applicationConfigFile, clazz);
			}
		}
		catch (Exception e) {
			System.out.println("WARN: Failed to read application configuration file=" + applicationConfigFile);
			e.printStackTrace();
		}

		// Read user related configuration
		File userConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_USER);
		try {
			System.out.println("INFO: User configuration file=" + userConfigFile + ", exists=" + userConfigFile.exists());
			if (userConfigFile.exists()) {
				T userConfig = read(userConfigFile, clazz);
				if (config != null) {
					config.copyNotEmptyFields(userConfig, clazz);
				}
				else {
					config = userConfig;
				}
			}
		}
		catch (Exception e) {
			System.out.println("WARN: Failed to read user configuration file=" + userConfigFile);
			e.printStackTrace();
		}
		
		if (config == null) {
			System.out.println("INFO: Missing configuration files, use default configuration.");
			config = clazz.newInstance();
		}

		config.setManufacturerName(manufacturerName);
		config.setAppName(appName);
		return config;
	}

	public void write() {
		
		// Write application related configuration.
		File applicationConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_APPLICATION_WRITE);
		try {
			System.out.println("INFO: write configuration file=" + applicationConfigFile);
			extractApplicationConfig().write(applicationConfigFile);
		}
		catch (Exception e) {
			System.out.println("ERROR: Failed to write application configuration file=" + applicationConfigFile);
			e.printStackTrace();
		}

		// Write user related configuration
		File userConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_USER);
		try {
			System.out.println("INFO: Write configuration file=" + userConfigFile);
			write(userConfigFile);
		}
		catch (Exception e) {
			System.out.println("WARN: Failed to write user configuration file=" + userConfigFile);
			e.printStackTrace();
		}
		
	}

	private static <T extends Config> T  read(File configFile, Class<T> clazz) throws Exception {
		GsonBuilder builder = GsonBuilderJoa.create();
		Gson gson = builder.create();
		byte[] bytes = Files.readAllBytes(configFile.toPath());
		return gson.fromJson(new String(bytes, "UTF-8"), clazz);
	}
	
	protected void copyNotEmptyFields(Object userConfig, Class<?> clazz) throws Exception {
		for (Field field : clazz.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			if (Modifier.isFinal(field.getModifiers())) continue;
			if (Modifier.isTransient(field.getModifiers())) continue;
			field.setAccessible(true);
			Object value = field.get(userConfig);
			if (value != null) {
				if (value instanceof String) {
					if (!((String)value).isEmpty()) {
						field.set(this, value);
					}
				}
				else if (value instanceof Collection) {
					if (!((Collection<?>)value).isEmpty()) {
						field.set(this, value);
					}
				}
				else {
					field.set(this, value);
				}
			}
		}
		if (clazz != Config.class) {
			copyNotEmptyFields(userConfig, clazz.getSuperclass());
		}
	}
	
	private void write(File configFile) throws Exception {
		GsonBuilder builder = GsonBuilderJoa.create();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		String json = gson.toJson(this);
		json = json.replaceAll("\n", "\r\n");
		byte[] bytes = json.getBytes("UTF-8");
		configFile.delete();
		Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE_NEW);
	}
	
	protected Config extractApplicationConfig() {
		Config appConfig = (Config)this.clone();
		appConfig.setUserName(null);
		appConfig.setEncryptedPassword(null);
		appConfig.setLogFile(null);
		appConfig.setExportAttachmentsDirectory(null);
		appConfig.setCredentials(null);
		appConfig.setProxyServerUserName(null);
		appConfig.setProxyServerEncryptedUserPassword(null);
		return appConfig;
	}
	
	private static File getConfigFile(String manufacturerName, String appName, String fileName) {
		String appData = System.getenv("APPDATA");
		if (Default.value(appData).isEmpty()) {
			appData = ".";
		}
		File dataDir = new File(new File(new File(appData), manufacturerName), appName).getAbsoluteFile();
		dataDir.mkdirs();
		File configFile = new File(dataDir, fileName);
		return configFile;
	}

	public String getManufacturerName() {
		return manufacturerName;
	}

	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getIssueIdMailSubjectFormat() {
		return issueIdMailSubjectFormat;
	}

	public void setIssueIdMailSubjectFormat(String issueIdMailSubjectFormat) {
		this.issueIdMailSubjectFormat = issueIdMailSubjectFormat;
	}

	public boolean isInjectIssueIdIntoMailSubject() {
		return injectIssueIdIntoMailSubject;
	}

	public void setInjectIssueIdIntoMailSubject(boolean injectIssueIdIntoMailSubject) {
		this.injectIssueIdIntoMailSubject = injectIssueIdIntoMailSubject;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}

	public IdName getMsgFileFormat() {
		return msgFileFormat;
	}

	public void setMsgFileFormat(IdName msgFileFormat) {
		this.msgFileFormat = msgFileFormat;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Boolean getInjectIssueIdIntoMailSubject() {
		return injectIssueIdIntoMailSubject;
	}

	public void setInjectIssueIdIntoMailSubject(Boolean injectIssueIdIntoMailSubject) {
		this.injectIssueIdIntoMailSubject = injectIssueIdIntoMailSubject;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public int getNbOfSuggestions() {
		return nbOfSuggestions;
	}

	public void setNbOfSuggestions(int nbOfSuggestions) {
		this.nbOfSuggestions = nbOfSuggestions;
	}

	public String getExportAttachmentsDirectory() {
		return exportAttachmentsDirectory;
	}

	public void setExportAttachmentsDirectory(String exportAttachmentsDirectory) {
		this.exportAttachmentsDirectory = exportAttachmentsDirectory;
	}

	public String getCredentials() {
		return credentials;
	}

	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	public String getProxyServer() {
		return proxyServer;
	}

	public void setProxyServer(String proxyServer) {
		this.proxyServer = proxyServer;
	}

	public boolean isProxyServerEnabled() {
		return proxyServerEnabled;
	}

	public void setProxyServerEnabled(boolean proxyServerEnabled) {
		this.proxyServerEnabled = proxyServerEnabled;
	}

	public int getProxyServerPort() {
		return proxyServerPort;
	}

	public void setProxyServerPort(int proxyServerPort) {
		this.proxyServerPort = proxyServerPort;
	}

	public String getProxyServerUserName() {
		return proxyServerUserName;
	}

	public void setProxyServerUserName(String proxyServerUserName) {
		this.proxyServerUserName = proxyServerUserName;
	}

	public String getProxyServerEncryptedUserPassword() {
		return proxyServerEncryptedUserPassword;
	}

	public void setProxyServerEncryptedUserPassword(String proxyServerEncryptedUserPassword) {
		this.proxyServerEncryptedUserPassword = proxyServerEncryptedUserPassword;
	}

	public TaskPanePosition getTaskPanePosition() {
		return taskPanePosition;
	}

	public void setTaskPanePosition(TaskPanePosition taskPanePosition) {
		this.taskPanePosition = taskPanePosition;
	}

	public String getAutoReplyField() {
		return autoReplyField;
	}

	public void setAutoReplyField(String autoReplyField) {
		this.autoReplyField = autoReplyField;
	}

	
}
