/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

public class Property {
	
	/**
	 * Empty property.
	 * Value is null.
	 */
	public final static Property NULL = new Property();

	/**
	 * Issue type.
	 * Value is of type String and holds the issue's type ID.
	 */
	public final static String ISSUE_TYPE = "IssueProperty.Type";

	/**
	 * Issue category.
	 * Value is of type String and holds the issue's category ID.
	 */
	public final static String CATEGORY = "IssueProperty.Category";

	/**
	 * Issue subject.
	 * Value is of type String and holds the issue's subject.
	 */
	public final static String SUBJECT = "IssueProperty.Subject";

	/**
	 * Issue description.
	 * Value is of type String and holds the issue's description as HTML text.
	 */
	public final static String DESCRIPTION = "IssueProperty.Description";

	/**
	 * Issue assignee.
	 * Value is of type String and holds the ID of the user to which the issue is assigned.
	 */
	public final static String ASSIGNEE = "IssueProperty.Assignee";

	/**
	 * Array of attachments.
	 * Value is of type List<Attachment>.
	 */
	public final static String ATTACHMENTS = "IssueProperty.Attachments";

	/**
	 * Issue priority.
	 * Value is of type String and holds the issue's priority ID.
	 */
	public static final String PRIORITY = "IssueProperty.Priority";

	/**
	 * Issue state.
	 * Value is of type String and holds the issue's state ID.
	 */
	public static final String STATE = "IssueProperty.State";

	/**
	 * Issue milestones.
	 * Value is of type String[] and holds the issue's milestone IDs.
	 */
	public static final String MILESTONES = "IssueProperty.Milestones";

	private String id;
	
	private Object value;
	
	public Property() {
		id = "";
		value = "";
	}

	public Property (String id, Object value) {
		this.id = id != null ? id : "";
		this.value = value;
	}
	
	public Property (Property prop) {
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
}