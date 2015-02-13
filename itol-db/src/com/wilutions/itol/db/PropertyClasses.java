/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
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
	
	public PropertyClass get(String propertyId) {
		return map.get(propertyId);
	}

	public PropertyClass getCopy(String propertyId) {
		PropertyClass pclass =  map.get(propertyId);
		if (pclass != null) {
			pclass = new PropertyClass(pclass);
		}
		return pclass;
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
		add(PropertyClass.TYPE_STRING, Property.STATUS, "Status");
		return this;
	}
	
}
