/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
}
