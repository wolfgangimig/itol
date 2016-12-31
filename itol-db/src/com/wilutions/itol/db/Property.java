/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Property {

	/**
	 * Empty property. Value is null.
	 */
	public final static Property NULL = new Property();

	/**
	 * Issue type. Value is of type String and holds the issue's type ID.
	 */
	public final static String ISSUE_TYPE = "IssueProperty.Type";

	/**
	 * Issue project. Value is of type String and holds the issue's project
	 * ID.
	 */
	public final static String PROJECT = "IssueProperty.Project";

	/**
	 * Issue subject. Value is of type String and holds the issue's subject.
	 */
	public final static String SUBJECT = "IssueProperty.Subject";

	/**
	 * Issue description. Value is of type String and holds the issue's
	 * description as HTML text.
	 */
	public final static String DESCRIPTION = "IssueProperty.Description";

	/**
	 * Array of attachments. Value is of type List<Attachment>.
	 */
	public final static String ATTACHMENTS = "IssueProperty.Attachments";

	/**
	 * Issue priority. Value is of type String and holds the issue's priority
	 * ID.
	 */
	public static final String PRIORITY = "IssueProperty.Priority";

	/**
	 * Issue state. Value is of type String and holds the issue's state ID.
	 */
	public static final String STATUS = "IssueProperty.State";

	/**
	 * Issue update notes. Value is of type String. It contains the notes
	 * entered for an updated issue.
	 */
	public static final String NOTES = "IssueProperty.Notes";
	
	/**
	 * Jira: visibility of comment.
	 * Select list contains project roles.
	 */
	public static final String NOTES_VISIBILITY = "IssueProperty.NotesVisibility";
	
	
	public static final String URL = "url";
	public static final String USER_NAME = "userName";
	public static final String PASSWORD = "userPwd";
	public static final String API_KEY = "apiKey";
	
	public static final String PROXY_SERVER_ENABLED = "proxyServerEnabled";
	public static final String PROXY_SERVER = "proxyServer";
	public static final String PROXY_PORT = "proxyPort";
	public static final String PROXY_USER_NAME = "proxyUserName";
	public static final String PROXY_PASSWORD = "proxyUserPwd";
	
	/**
	 * JIRA rest API returns web links to user images.
	 * User images are requested from- to www.gravatar.com.
	 * If this option is true, user images will not be queried from gravatar.
	 */
	public static final String DISALLOW_IMAGES_FROM_GRAVATAR = "disallowImagesFromGravatar";

	
	public static final String INJECT_ISSUE_ID_INTO_MAIL_SUBJECT = "injectIssueIdIntoMailSubject";
	public static final String ISSUE_ID_MAIL_SUBJECT_FORMAT = "issueIdMailSubjectFormat";
	public static final String ISSUE_ID_MAIL_SUBJECT_FORMAT_DEFAULT = "[#%1]";
	
	public static final String RECENTLY_USED_PROJECTS = "recentlyUsedProjects";
	
	public static final String MSG_FILE_TYPE = "msgFileType";

	public static final String LOG_FILE = "Log.file";
	
	public static final String LOG_LEVEL = "Log.level";

	/**
	 * Directory to save attachments when button "Export" is clicked.
	 */
	public static final String EXPORT_ATTACHMENTS_DIRECTORY = "exportAttachmentsDirectory";

	private String id;

	private Object value;

	public Property() {
		id = "";
		value = null;
	}

	public Property(String id, Object value) {
		this.id = id != null ? id : "";
		this.value = value;
	}

	public Property(Property prop) {
		this.id = prop.id;
		this.value = prop.value;
	}

	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object v) {
		this.value = v;
	}

	public boolean isNull() {
		return id.isEmpty() || value == null;
	}

	public String toString() {
		String v = value != null ? value.toString() : "";
		if (v.length() > 10) {
			v = v.substring(0, 10) + "...";
		}
		return "[" + id + "=" + v + "]";
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object clone() {
		Property prop = new Property();
		prop.id = this.id;
		if (value != null) {
			if (value instanceof List) {
				List list = (List)value;
				List listCopy = new ArrayList(list.size());
				for (Object elm : list) {
					Object elmCopy = elm;
					if (elm instanceof Cloneable) {
						try {
							Method cloneMethod = elm.getClass().getMethod("clone");
							elmCopy = cloneMethod.invoke(elm);
						}
						catch (Exception e) {}
					}
					listCopy.add(elmCopy);
				}
				prop.value = listCopy;
			}
			else {
				// String or Boolean
				prop.value = value;
			}
		}
		return prop;
	}

	@Override
	public boolean equals(Object rhs) {
		boolean ret = false;
		if (rhs != null && rhs instanceof Property) {
			Property prop2 = (Property)rhs;
			ret = id.equals(prop2.id);
			if (ret) {
				if (value == null) {
					ret = prop2.value == null;
				} else if (prop2.value == null) {
					ret = false;
				} else {
					ret = value.equals(prop2.value);
				}
			}
		}
		return ret;
	}
}