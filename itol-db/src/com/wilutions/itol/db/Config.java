package com.wilutions.itol.db;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wilutions.joa.TaskPanePosition;
import com.wilutions.joa.gson.GsonBuilderJoa;
import com.wilutions.mslib.office.MsoCTPDockPosition;

/**
 * An object of this class stores configuration options. Options are first read
 * from a file defined by system option "com.wilutions.itol.appConfig". This
 * file might contain the options that are the same for all users. In a second
 * step, options are read from
 * %APPDATA%/manufacturerName/appName/user-profile.json. This options shall be
 * user related and have precedence over the application options. When writing
 * options, only the user-profile.json is updated.
 */
public class Config implements Serializable, Cloneable {
	private static final long serialVersionUID = -5838975424557674776L;

	/**
	 * System option that specifies the applications configuration file. This
	 * system option is optional. If not set, only the user related options are
	 * read.
	 */
	public final static String SYSTEM_OPTION_CONFIG_FILE = "com.wilutions.itol.config";

	/**
	 * Application configuration file name. Options are read from this file
	 * before user related options are read. User related options have
	 * precedence. When configuration is written, this file keeps unchanged,
	 * file {@link #CONFIG_FILE_APPLICATION_TEMPLATE} is written instead.
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
	 * First version of configuration does not support profiles.
	 */
	@SuppressWarnings("unused")
	private final static int VERSION_FIRST = 1;

	/**
	 * Configuration contains profiles.
	 */
	private final static int VERSION_SUPPORT_PROFILES = 2;

	/**
	 * Configuration file version.
	 */
	private int version = VERSION_SUPPORT_PROFILES;

	// Application or user options.
	private List<Profile> profiles = new ArrayList<Profile>();
	private String licenseKey;
	private int currentProfileIndex;
	private ProxyServerConfig proxyServerConfig;
	private LoggerConfig loggerConfig;

	// Only user related options.
	private TaskPanePosition taskPanePosition;

	public Config() {
		taskPanePosition = new TaskPanePosition();
		taskPanePosition.setDockPosition(MsoCTPDockPosition.msoCTPDockPositionRight);
		taskPanePosition.setWidth(600);
		proxyServerConfig = new ProxyServerConfig();
		loggerConfig = new LoggerConfig();
	}

	public static Config read(String manufacturerName, String appName) throws Exception {
		Config config = null;

		// Read application configuration
		File applicationConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_APPLICATION_READ);
		try {
			String optFile = System.getProperty(SYSTEM_OPTION_CONFIG_FILE);
			if (!Default.value(optFile).isEmpty()) {
				applicationConfigFile = new File(optFile);
			}

			System.out.println("INFO: Application configuration file=" + applicationConfigFile + ", exists="
					+ applicationConfigFile.exists());
			if (applicationConfigFile.exists()) {
				config = read(applicationConfigFile);
			}
		} catch (Exception e) {
			System.out.println("WARN: Failed to read application configuration file=" + applicationConfigFile);
			e.printStackTrace();
		}

		// Read user related configuration
		File userConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_USER);
		try {
			System.out
					.println("INFO: User configuration file=" + userConfigFile + ", exists=" + userConfigFile.exists());
			if (userConfigFile.exists()) {
				Config userConfig = read(userConfigFile);
				if (config != null) {
					config.copyNotEmptyFields(userConfig);
				} else {
					config = userConfig;
				}
			}
		} catch (Exception e) {
			System.out.println("WARN: Failed to read user configuration file=" + userConfigFile);
			e.printStackTrace();
		}

		if (config == null) {
			System.out.println("INFO: Missing configuration files, use default configuration.");
			config = Config.class.newInstance();
		}

		config.setManufacturerName(manufacturerName);
		config.setAppName(appName);
		return config;
	}

	private void copyNotEmptyFields(Config userConfig) throws Exception {

		// Copy license key
		if (!Default.value(userConfig.licenseKey).isEmpty()) {
			this.licenseKey = userConfig.licenseKey;
		}

		// Copy profile values or add profile
		HashMap<String, Profile> profileMap = new HashMap<String, Profile>();
		this.profiles.stream().forEach((p) -> profileMap.put(p.getProfileName(), p));

		userConfig.profiles.stream().forEach((p) -> {
			Profile appProfile = profileMap.get(p.getProfileName());
			if (appProfile != null) {
				appProfile.copyNotEmptyFields(p, p.getClass());
			} else {
				profileMap.put(p.getProfileName(), p);
			}
		});

		this.profiles = profileMap.values().stream().collect(Collectors.toList());
	}

	public void write() {

		// Write application related configuration.
		File applicationConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_APPLICATION_WRITE);
		try {
			Config config = (Config) this.clone();
			config.unsetUserRelatedValues();
			config.write(applicationConfigFile);
		} catch (Exception e) {
			System.out.println("ERROR: Failed to write application configuration file=" + applicationConfigFile);
			e.printStackTrace();
		}

		// Write user related configuration
		File userConfigFile = getConfigFile(manufacturerName, appName, CONFIG_FILE_USER);
		try {
			write(userConfigFile);
		} catch (Exception e) {
			System.out.println("WARN: Failed to write user configuration file=" + userConfigFile);
			e.printStackTrace();
		}

	}

	private static class DetectConfigVersion {
		int version = 0;
		String licenseKey = "";
	}

	private static Config read(File configFile) throws Exception {
		byte[] bytes = Files.readAllBytes(configFile.toPath());
		String jsonText = new String(bytes, "UTF-8");

		Config config = null;

		GsonBuilder builder = GsonBuilderJoa.create();
		Gson gson = builder.create();

		// Detect config file version.
		// The first config file version does not contain profiles.
		DetectConfigVersion configVersion = gson.fromJson(jsonText, DetectConfigVersion.class);
		if (configVersion.version < VERSION_SUPPORT_PROFILES) {

			// Read values that are common to all profiles.
			config = gson.fromJson(jsonText, Config.class);
			config.setLicenseKey(configVersion.licenseKey);

			// Read config file content as it was a Profile.
			// The first supported server was JIRA. Hence, instantiate
			// JiraProfile object.
			Profile profile = (Profile) gson.fromJson(jsonText, Class.forName("com.wilutions.jiraddin.JiraProfile"));
			profile.setServiceFactoryClass(Profile.JIRA_SERVICE_CLASS);
			profile.initProfileNameFromServiceUrl();

			config.setCurrentProfile(profile);

			config.version = VERSION_SUPPORT_PROFILES;

		} else {
			registerGsonTypeAdapters(builder);
			gson = builder.create();
			config = gson.fromJson(new String(bytes, "UTF-8"), Config.class);
		}

		return config;
	}
	
	private static void registerGsonTypeAdapters(GsonBuilder builder) {
		builder.registerTypeAdapter(Profile.class, new GsonInterfaceAdapter<Profile>());
	}

	private void write(File configFile) throws Exception {
		GsonBuilder builder = GsonBuilderJoa.create();
		builder.setPrettyPrinting();
		registerGsonTypeAdapters(builder);
		Gson gson = builder.create();
		String json = gson.toJson(this);
		json = json.replaceAll("\n", "\r\n");
		byte[] bytes = json.getBytes("UTF-8");
		configFile.delete();
		Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE_NEW);
	}

	public Config clone() {
		Config config = new Config();
		config.copyFrom(this);
		return config;
	}

	protected void copyFrom(Config rhs) {
		this.manufacturerName = rhs.manufacturerName;
		this.appName = rhs.appName;
		this.version = rhs.version;
		this.currentProfileIndex = rhs.currentProfileIndex;
		this.profiles = new ArrayList<Profile>();
		for (Profile profile : rhs.profiles) {
			this.profiles.add((Profile) profile.clone());
		}
		this.taskPanePosition = rhs.taskPanePosition;
		this.proxyServerConfig.copyFrom(rhs.proxyServerConfig);
		this.loggerConfig.copyFrom(rhs.loggerConfig);
		
		// ITJ-64: License key was not stored into application.json.templ
		this.licenseKey = rhs.licenseKey;
	}

	protected void unsetUserRelatedValues() {
		for (Profile profile : profiles) {
			profile.unsetUserRelatedValues();
		}
		proxyServerConfig.unsetUserRelatedValues();
		loggerConfig.unsetUserRelatedValues();
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

	public String getLicenseKey() {
		return Default.value(licenseKey);
	}

	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}

	/**
	 * Get current profile. Return an empty Profile object, if there are no
	 * profiles defined.
	 * 
	 * @return Profile object.
	 */
	public Profile getCurrentProfile() {
		Profile ret = new Profile();
		if (currentProfileIndex >= 0 && currentProfileIndex < profiles.size()) {
			ret = profiles.get(currentProfileIndex);
		}
		return ret;
	}

	/**
	 * Set the current profile. Add the given profile to the list of profiles,
	 * if it does not exist.
	 * 
	 * @param profile
	 *            Profile
	 */
	public void setCurrentProfile(Profile profile) {
		if (profile != null) {
			currentProfileIndex = profiles.indexOf(profile);
			if (currentProfileIndex < 0) {
				profiles.add(profile);
				currentProfileIndex = profiles.size()-1;
			}
		}
	}

	public TaskPanePosition getTaskPanePosition() {
		return taskPanePosition;
	}

	public void setTaskPanePosition(TaskPanePosition taskPanePosition) {
		this.taskPanePosition = taskPanePosition;
	}

	public File getTempDir() {
		return new File(Profile.DEFAULT_TEMP_DIR);
	}

	public List<Profile> getProfiles() {
		return Default.value(profiles);
	}
	
	public void setProfiles(List<Profile> profiles) {
		this.profiles = profiles;
	}

	public ProxyServerConfig getProxyServer() {
		return proxyServerConfig;
	}

	public void setProxyServer(ProxyServerConfig proxyServer) {
		this.proxyServerConfig = proxyServer;
	}

	public LoggerConfig getLoggerConfig() {
		return loggerConfig;
	}

	public void setLoggerConfig(LoggerConfig loggerConfig) {
		this.loggerConfig = loggerConfig;
	}

}
