package com.wilutions.itol.db;

import java.util.Collection;
import java.util.HashMap;

public class PropertyClasses {

	private final HashMap<String, PropertyClass> map = new HashMap<String, PropertyClass>();

	private final static PropertyClasses instance = new PropertyClasses().init();

	public static PropertyClasses getDefault() {
		return instance;
	}

	public PropertyClasses() {
	}

	public void add(PropertyClass propClass) {
		map.put(propClass.getId(), propClass);
	}

	public PropertyClass add(int type, String id, String name) {
		PropertyClass propClass = new PropertyClass(type, id, name);
		add(propClass);
		return propClass;
	}
	
	public PropertyClass get(String propClassId) {
		return map.get(propClassId);
	}

	public Collection<PropertyClass> values() {
		return map.values();
	}

	private PropertyClasses init() {
		add(PropertyClass.TYPE_STRING, Property.ISSUE_TYPE, "Type");
		add(PropertyClass.TYPE_STRING, Property.CATEGORY, "Category");
		add(PropertyClass.TYPE_STRING, Property.SUBJECT, "Subject");
		add(PropertyClass.TYPE_STRING, Property.DESCRIPTION, "Description");
		add(PropertyClass.TYPE_STRING, Property.ASSIGNEE, "Assigned to");
		add(PropertyClass.TYPE_ARRAY_STRING, Property.ATTACHMENTS, "Attachments");
		add(PropertyClass.TYPE_STRING, Property.PRIORITY, "Priority");
		add(PropertyClass.TYPE_STRING, Property.STATE, "Status");
		add(PropertyClass.TYPE_ARRAY_STRING, Property.MILESTONES, "Milestones");
		return this;
	}
	
}
