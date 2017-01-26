/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wilutions.itol.db.Default;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Property;

public class AppInfo {
	
	private String serviceFactoryClass;
	
	private List<String> serviceFactoryParams = new ArrayList<String>();
	
	private List<Property> configProps = new ArrayList<Property>();

	private String appName;

	private String manufacturerName;
	
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

	public List<Property> getConfigProps() {
		return configProps;
	}

	public void setConfigProps(List<Property> configProps) {
		this.configProps = configProps;
		initIssueIdMailSubjectFormat();
		initExportAttachmentsDirectory();
	}
	
	public Property getConfigProperty(String propId) {
		Property ret = null;
		for (Property prop : getConfigProps()) {
			if (prop.getId().equals(propId)) {
				ret = prop;
				break;
			}
		}
		return ret;
	}

	public String getConfigPropertyString(String propId, String defaultValue) {
		Property prop = getConfigProperty(propId);
		String ret = defaultValue;
		if (prop != null) {
			Object value = prop.getValue();
			if (value != null) {
				ret = (String)value;
			}
		}
		return ret;
	}

	public void setConfigPropertyString(String propId, String value) {
		Property prop = getConfigProperty(propId);
		if (prop == null) {
			prop = new Property(propId, value);
			getConfigProps().add(prop);
		}
		else {
			prop.setValue(value);
		}
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getManufacturerName() {
		return manufacturerName;
	}

	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	public IdName getMsgFileType() {
		IdName ret = MsgFileTypes.NOTHING;
		String s = getConfigPropertyString(Property.MSG_FILE_TYPE, MsgFileTypes.NOTHING.getId());
		for (IdName t : MsgFileTypes.TYPES) {
			if (t.getId().equals(s)) {
				ret = t;
				break;
			}
		}
		return ret;
	}
	
	public String getLogLevel() {
		return getConfigPropertyString(Property.LOG_LEVEL, "INFO");
	}
	
	public String getLogFile() {
		String defaultValue  = new File(System.getProperty("java.io.tmpdir"), "itol.log").getAbsolutePath();
		return getConfigPropertyString(Property.LOG_FILE, defaultValue);
	}
	
	public String getIssueIdMailSubjectFormat() {
		String ret = getConfigPropertyString(Property.ISSUE_ID_MAIL_SUBJECT_FORMAT, "");
		return ret;
	}
	
	/**
	 * Remove old option INJECT_ISSUE_ID_INTO_MAIL_SUBJECT.
	 * If option is set, add the default ISSUE_ID_MAIL_SUBJECT_FORMAT.
	 */
	private void initIssueIdMailSubjectFormat() {
		
		boolean injectIssueId = false;
		for (Iterator<Property> it = getConfigProps().iterator(); it.hasNext(); ) {
			Property p = it.next();
			if (p.getId().equals(Property.INJECT_ISSUE_ID_INTO_MAIL_SUBJECT)) {
				injectIssueId = p.getValue().equals("true");
				it.remove();
				break;
			}
		}

		String ret = getConfigPropertyString(Property.ISSUE_ID_MAIL_SUBJECT_FORMAT, "");
		if (ret.isEmpty() && injectIssueId) {
			getConfigProps().add(new Property(Property.ISSUE_ID_MAIL_SUBJECT_FORMAT, Property.ISSUE_ID_MAIL_SUBJECT_FORMAT_DEFAULT));
		}
	}
	
	private void initExportAttachmentsDirectory() {
		String tempDir = System.getProperty("java.io.tmpdir");
		File dir = new File(new File(tempDir), "issues");
		getConfigProps().add(new Property(Property.EXPORT_ATTACHMENTS_DIRECTORY, dir.getAbsolutePath()));
	}
	
	public void setNbOfSuggestions(int nb) {
		setConfigPropertyString(Property.NB_OF_SUGGESTIONS, Integer.toString(nb));
	}
	
	public int getNbOfSuggestions() {
		return Integer.parseInt(getConfigPropertyString(Property.NB_OF_SUGGESTIONS, "20"));
	}

	public AppInfo readFromAppData() {
		AppInfo ret = null;
		try {
			File configFile = getConfigFile(manufacturerName, appName);
			System.out.println("Load config from " + configFile);
			ret = read(configFile);
		}
		catch(Exception e) {
			System.out.println("File not found.");
			ret = new AppInfo();
		}
		ret.setManufacturerName(manufacturerName);
		ret.setAppName(appName);
		ret.setServiceFactoryClass(serviceFactoryClass);
		return ret;
	}
	
	public void writeIntoAppData() throws Exception {
		File configFile = getConfigFile(manufacturerName, appName);
		System.out.println("Write config into " + configFile);
		write(configFile);
	}
	
	private static File getConfigFile(String manufacturerName, String appName) {
		String appData = System.getenv("APPDATA");
		if (Default.value(appData).isEmpty()) {
			appData = ".";
		}
		File dataDir = new File(new File(new File(appData), manufacturerName), appName).getAbsoluteFile();
		dataDir.mkdirs();
		File configFile = new File(dataDir, "application.json");
		return configFile;
	}
	
	private static AppInfo read(File configFile) throws Exception {
		String json = new String(Files.readAllBytes(configFile.toPath()), "UTF-8");
		GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(json, AppInfo.class);
	}
	
//	private void fromJSON(JSONObject jsAppInfo) {
//		this.serviceFactoryClass = jsAppInfo.getString("serviceFactoryClass");
//		this.appName = jsAppInfo.getString("appName");
//		this.manufacturerName = jsAppInfo.getString("manufacturerName");
//		
//		GsonBuilder builder = new GsonBuilder();
//        Gson gson = builder.create();
//        
//		
//		this.serviceFactoryParams = new ArrayList<String>();
//		{
//			JSONArray jsServiceFactoryParams = (JSONArray)jsAppInfo.get("serviceFactoryParams");
//			int n = jsServiceFactoryParams.length();
//			for (int i = 0; i < n; i++) {
//				String s = jsServiceFactoryParams.getString(i);
//				this.serviceFactoryParams.add(s);
//			}
//		}		
//		
//		this.configProps = new ArrayList<Property>();
//		{
//			JSONArray jsConfigProps = (JSONArray)jsAppInfo.get("configProps");
//			int n = jsConfigProps.length();
//			for (int i = 0; i < n; i++) {
//				JSONObject jsProperty = jsConfigProps.getJSONObject(i);
//				Property prop = null;
//				String propId = jsProperty.getString("id");
//				Object propValue = jsProperty.get("value");
//				if (propId.isEmpty() || propValue == null) {
//					prop = Property.NULL;
//				}
//				else if (propValue instanceof JSONArray) {
//					List<Object> values = new ArrayList<>();
//					JSONArray jsValues = (JSONArray)propValue;
//					int nv = jsValues.length();
//					for (int iv = 0; iv < nv; iv++) {
//						Object elm = jsValues.get(iv);
//						values.add(elm);
//					}
//					propValue = values;
//					prop = new Property(propId, propValue);
//				}
//				else {
//					prop = new Property(propId, propValue);
//				}
//				this.configProps.add(prop);
//			}
//			
//		}
//	}
	
	private void write(File configFile) throws Exception {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
        Gson gson = builder.create();
        String json = gson.toJson(this);
        Files.write(configFile.toPath(), json.getBytes("UTF-8"), StandardOpenOption.CREATE);
	}
	
	
}
