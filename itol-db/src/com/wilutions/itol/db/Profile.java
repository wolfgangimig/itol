package com.wilutions.itol.db;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Profile {
	
	/**
	 * Default temp directory.
	 * Value: %TEMP%\ITOL
	 */
	public final static String DEFAULT_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"), "ITOL").getAbsolutePath();
	
	/**
	 * Profile file version.
	 */
	private int version = 1;
	
	// Application or user options.
	private String profileName = "";
	private String serviceUrl = "";
	private String issueIdMailSubjectFormat = Property.ISSUE_ID_MAIL_SUBJECT_FORMAT_DEFAULT;
	private Boolean injectIssueIdIntoMailSubject = Boolean.FALSE;
	private IdName msgFileFormat = MsgFileFormat.DEFAULT;
	private int nbOfSuggestions = 20;
	private String proxyServer = "";
	private boolean proxyServerEnabled;
	private int proxyServerPort;
	private int maxHistoryItems = 100;
	private String serviceFactoryClass = "";
	private List<String> serviceFactoryParams = new ArrayList<String>(0);
	
	/**
	 * Mail address of issue trackin sevice.
	 * The body of this mails is not used as comment when assigning to the ITOL dialog. 
	 */
	private String serviceNotifcationMailAddress;
	
	/**
	 * Convert HTML mail body to markup.
	 */
	private MailBodyConversion mailBodyConversion = MailBodyConversion.MARKUP;
	
	/**
	 * Timeout for converting mail body.
	 * E.g. mails from ABBYY require more than 20s to be loaded.
	 */
	private int mailBodyConversionTimeoutSeconds = 10;
	
	/**
	 * Custom field for mail address.
	 */
	private String autoReplyField = "";
	
	/**
	 * Start this program after attachments have been exported.
	 */
	private String exportAttachmentsProgram = "";
	
	/**
	 * Files with this extensions are always opened as text files. 
	 */
	private String extensionsAlwaysOpenAsText = DEFAULT_BLACK_EXTENSIONS;
	
	/**
	 * List of files that should not be added to an issue.
	 */
	private List<AttachmentBlacklistItem> blacklist = new ArrayList<>();

	// https://www.howtogeek.com/137270/50-file-extensions-that-are-potentially-dangerous-on-windows/
	private final static String DEFAULT_BLACK_EXTENSIONS =  
			".exe.pif.application.gadget.msi.msp.com.scr.hta.cpl.msc.jar.scf.lnk.inf" +
			".reg.pl.bat.cmd.vb.vbs.js.jse.ws.wsf.wsc.wsh.ps1.ps1xml.ps2.ps2xml.psc1.psc2.msh.msh1.msh2.mshxml.msh1xml.msh2xml";

	/**
	 * Placeholder for export directory.
	 * Pass this placehoder to {@link #exportAttachmentsProgram} to reference the export directory.
	 */
	public final static String PLACEHODER_EXPORT_DIRECTORY = "${export.dir}";

	/**
	 * Placeholder for project ID.
	 * Pass this placehoder to {@link #exportAttachmentsProgram} to reference the project ID.
	 */
	public final static String PLACEHODER_PROJECT_ID = "${project.id}";

	/**
	 * Placeholder for issue ID.
	 * Pass this placehoder to {@link #exportAttachmentsProgram} to reference the issue ID.
	 */
	public final static String PLACEHODER_ISSUE_ID = "${issue.id}";

	public final static String EXPORT_PROROGRAM_EXPLORER = "\"C:\\Windows\\explorer.exe\" \"" + PLACEHODER_EXPORT_DIRECTORY + "\"";
	public final static String EXPORT_PROGRAM_CMD = "C:\\Windows\\System32\\cmd.exe /C start \"" + PLACEHODER_ISSUE_ID + "\" /d \"" + PLACEHODER_EXPORT_DIRECTORY + "\"";
	public final static String EXPORT_PROGRAM_TOTALCMD = "\"C:\\Program Files\\totalcmd\\TOTALCMD64.exe\" /O /L=\"" + PLACEHODER_EXPORT_DIRECTORY + "\"";
	/**
	 * Default export program.
	 * This program is executed by default when attachments are exported.
	 */
	public final static String EXPORT_PROROGRAM_DEFAULT = EXPORT_PROROGRAM_EXPLORER;


	// Only user related options.
	private String userName = "";
	private String encryptedPassword = "";
	private String credentials = "";
	private String exportAttachmentsDirectory = "";
	private String proxyServerUserName = "";
	private String proxyServerEncryptedUserPassword = "";
	
	private transient File tempDirForSession;

	public Profile() {
		getTempDir();
		getExportAttachmentsDirectory();
		exportAttachmentsProgram = EXPORT_PROROGRAM_DEFAULT;
		userName = proxyServerUserName = System.getProperty("user.name");
	}
	
	@Override
	public Object clone() {
		Profile profile = new Profile();
		profile.copyFrom(this);
		return profile;
	}
	
	protected void copyFrom(Profile rhs) {
		this.version = rhs.version;
		this.serviceUrl = rhs.serviceUrl;
		this.issueIdMailSubjectFormat = rhs.issueIdMailSubjectFormat;
		this.injectIssueIdIntoMailSubject = rhs.injectIssueIdIntoMailSubject;
		this.userName = rhs.userName;
		this.encryptedPassword = rhs.encryptedPassword;
		this.msgFileFormat = rhs.msgFileFormat;
		this.nbOfSuggestions = rhs.nbOfSuggestions;
		this.exportAttachmentsDirectory = rhs.exportAttachmentsDirectory;
		this.credentials = rhs.credentials;
		this.proxyServerUserName = rhs.proxyServerUserName;
		this.proxyServerEncryptedUserPassword = rhs.proxyServerEncryptedUserPassword;
		this.proxyServer = rhs.proxyServer;
		this.proxyServerEnabled = rhs.proxyServerEnabled;
		this.proxyServerPort = rhs.proxyServerPort;
		this.autoReplyField = rhs.autoReplyField;
		this.extensionsAlwaysOpenAsText = rhs.extensionsAlwaysOpenAsText;
		this.maxHistoryItems = rhs.maxHistoryItems;
		this.mailBodyConversion = rhs.mailBodyConversion;
		this.blacklist = new ArrayList<AttachmentBlacklistItem>(rhs.blacklist);
		this.exportAttachmentsProgram = rhs.exportAttachmentsProgram;
		this.serviceNotifcationMailAddress = rhs.serviceNotifcationMailAddress;
	}

	void unsetUserRelatedValues() {
		setUserName(null);
		setEncryptedPassword(null);
		setExportAttachmentsDirectory(null);
		setCredentials(null);
		setProxyServerUserName(null);
		setProxyServerEncryptedUserPassword(null);
	}

	protected void copyNotEmptyFields(Object userConfig, Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			if (Modifier.isFinal(field.getModifiers())) continue;
			if (Modifier.isTransient(field.getModifiers())) continue;
			field.setAccessible(true);
			try {
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
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (clazz != Config.class) {
			copyNotEmptyFields(userConfig, clazz.getSuperclass());
		}
	}
	
	public String getServiceUrl() {
		return  Default.value(serviceUrl);
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getIssueIdMailSubjectFormat() {
		return  Default.value(issueIdMailSubjectFormat);
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
		return  Default.value(encryptedPassword);
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

	public int getNbOfSuggestions() {
		return nbOfSuggestions;
	}

	public void setNbOfSuggestions(int nbOfSuggestions) {
		this.nbOfSuggestions = nbOfSuggestions;
	}

	public String getExportAttachmentsDirectory() {
		if (Default.value(exportAttachmentsDirectory).isEmpty()) {
			exportAttachmentsDirectory = new File(DEFAULT_TEMP_DIR, "Export").getAbsolutePath();
		}
		return exportAttachmentsDirectory;
	}

	public void setExportAttachmentsDirectory(String exportAttachmentsDirectory) {
		this.exportAttachmentsDirectory = exportAttachmentsDirectory;
	}

	public String getCredentials() {
		return  Default.value(credentials);
	}

	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	public String getProxyServer() {
		return  Default.value(proxyServer);
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
		return  Default.value(proxyServerUserName);
	}

	public void setProxyServerUserName(String proxyServerUserName) {
		this.proxyServerUserName = proxyServerUserName;
	}

	public String getProxyServerEncryptedUserPassword() {
		return  Default.value(proxyServerEncryptedUserPassword);
	}

	public void setProxyServerEncryptedUserPassword(String proxyServerEncryptedUserPassword) {
		this.proxyServerEncryptedUserPassword = proxyServerEncryptedUserPassword;
	}

	public String getAutoReplyField() {
		return  Default.value(autoReplyField);
	}

	public void setAutoReplyField(String autoReplyField) {
		this.autoReplyField = autoReplyField;
	}
	
	public synchronized File getTempDir() {
		if (tempDirForSession == null) {
			SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			String dir =  DEFAULT_TEMP_DIR + File.separator + profileName + dateTimeFormat.format(new Date(System.currentTimeMillis()));
			tempDirForSession = new File(dir);
			tempDirForSession.mkdirs();
		}
		return tempDirForSession;
	}

	public int getMaxHistoryItems() {
		return maxHistoryItems;
	}
	public void setMaxHistoryItems(int v) {
		maxHistoryItems = v;
	}

	public MailBodyConversion getMailBodyConversion() {
		return mailBodyConversion;
	}

	public void setMailBodyConversion(MailBodyConversion mailBodyConversion) {
		this.mailBodyConversion = mailBodyConversion;
	}

	public List<AttachmentBlacklistItem> getBlacklist() {
		return blacklist;
	}

	public void setBlacklist(List<AttachmentBlacklistItem> blacklist) {
		this.blacklist = blacklist;
	}

	public String getExtensionsAlwaysOpenAsText() {
		return  Default.value(extensionsAlwaysOpenAsText);
	}

	public void setExtensionsAlwaysOpenAsText(String extensionsAlwaysOpenAsText) {
		this.extensionsAlwaysOpenAsText = extensionsAlwaysOpenAsText;
	}

	public String getExportAttachmentsProgram() {
		return  Default.value(exportAttachmentsProgram);
	}

	public void setExportAttachmentsProgram(String exportAttachmentsProgram) {
		this.exportAttachmentsProgram = exportAttachmentsProgram;
	}

	public int getMailBodyConversionTimeoutSeconds() {
		return mailBodyConversionTimeoutSeconds;
	}

	public void setMailBodyConversionTimeoutSeconds(int mailBodyConversionTimeoutSeconds) {
		this.mailBodyConversionTimeoutSeconds = mailBodyConversionTimeoutSeconds;
	}

	public String getServiceNotifcationMailAddress() {
		return  Default.value(serviceNotifcationMailAddress);
	}

	public void setServiceNotifcationMailAddress(String serviceNotifcationMailAddress) {
		this.serviceNotifcationMailAddress = serviceNotifcationMailAddress;
	}

	public String getProfileName() {
		return Default.value(profileName);
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}
	
	public boolean isNew() {
		return getProfileName().isEmpty();
	}

	public String getServiceFactoryClass() {
		return serviceFactoryClass;
	}

	public void setServiceFactoryClass(String serviceFactoryClass) {
		this.serviceFactoryClass = serviceFactoryClass;
	}

	public List<String> getServiceFactoryParams() {
		return serviceFactoryParams;
	}

	public void setServiceFactoryParams(List<String> serviceFactoryParams) {
		this.serviceFactoryParams = serviceFactoryParams;
	}

}
