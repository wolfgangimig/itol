/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

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
	 * Issue category. Value is of type String and holds the issue's category
	 * ID.
	 */
	public final static String CATEGORY = "IssueProperty.Category";

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
	 * Issue assignee. Value is of type String and holds the ID of the user to
	 * which the issue is assigned.
	 */
	public final static String ASSIGNEE = "IssueProperty.Assignee";

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

	private String id;

	private Object value;

	public Property() {
		id = "";
		value = "";
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
		return "[" + id + "=" + value + "]";
	}

	public Property deepCopy() {
		Property prop = new Property();
		prop.id = this.id;
		if (value instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) value;
			prop.value = new ArrayList<String>(list);
		} else {
			prop.value = value;
		}
		return prop;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(Property prop2) {
		int ret = id.compareTo(prop2.id);
		if (ret == 0) {
			if (value == null) {
				ret = prop2.value != null ? -1 : 0;
			}
			else if (prop2.value == null) {
				ret = 1;
			}
			else if (value instanceof List) {
				List<String> list = (List<String>) value;
				if (prop2.value instanceof List) {
					List<String> list2 = (List<String>) prop2.value;
					ret = list.size() - list2.size();
					for (int i = 0; i < list.size() && ret == 0; i++) {
						ret = list.get(i).compareTo(list2.get(i));
					}
				}
				else {
					ret = 1;
				}
			} else if (value instanceof String) {
				ret = ((String)value).compareTo((String)prop2.value);
			} else if (value instanceof Boolean) {
				int v = (Boolean)value ? 1 : 0;
				int v2 = (Boolean)prop2.value ? 1 : 0;
				ret = v - v2;
			}
			else {
				ret = 1;
			}
		}
		return ret;
	}
}