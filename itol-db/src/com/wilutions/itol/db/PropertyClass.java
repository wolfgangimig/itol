/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class PropertyClass {

	public final static int TYPE_STRING = 1;
	public final static int TYPE_BOOL = 2;
	public final static int TYPE_PASSWORD = 3;
	public final static int TYPE_ISO_DATE = 5;
	public final static int TYPE_ID_NAME = 6;
	
	/**
	 * Multiline text field.
	 */
	public final static int TYPE_TEXT= 7;
	
	/**
	 * Date and time.
	 */
	public static final int TYPE_ISO_DATE_TIME = 8;
	
//	public final static int TYPE_INTEGER = 6;
//	public final static int TYPE_FLOAT = 7;
	
	public final static int TYPE_ARRAY = 0x1000;
	
	private String id;
	private String name;
	private Object defaultValue;
	private int type;
	
	/**
	 * List of acceptable values.
	 */
	private List<IdName> selectList;
	
	/**
	 * Suggestion class.
	 */
	private Suggest<IdName> autoCompletionSuggest;

	@SuppressWarnings("unchecked")
	public PropertyClass(int type, String id, String name, Object defaultValue, Collection<? extends IdName> selectList) {
		super();
		this.type = type;
		this.id = id;
		this.name = name;
		this.defaultValue = defaultValue;
		if (selectList != null) {
			this.selectList = selectList instanceof List ? (List<IdName>)selectList : new ArrayList<IdName>(selectList);
			this.autoCompletionSuggest = new DefaultSuggest<>(this.selectList);
		}
	}

	@SuppressWarnings("unchecked")
	public PropertyClass(int type, String id, String name, Object defaultValue, Suggest<? extends IdName> suggest) {
		super();
		this.type = type;
		this.id = id;
		this.name = name;
		this.defaultValue = defaultValue;
		this.selectList = null;
		this.autoCompletionSuggest = suggest != null ? (Suggest<IdName>)suggest : new DefaultSuggest<IdName>(selectList);
	}

	public PropertyClass(int type, String id, String name, Object defaultValue) {
		super();
		this.type = type;
		this.id = id;
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public PropertyClass(int type, String id, String name) {
		this(type, id, name, null);
	}
	
	public PropertyClass(PropertyClass rhs) {
		this.type = rhs.type;
		this.id = rhs.id;
		this.name = rhs.name;
		this.defaultValue = rhs.defaultValue;
		this.selectList = rhs.selectList;
		this.autoCompletionSuggest = rhs.autoCompletionSuggest;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public int getType() {
		return type & ~TYPE_ARRAY;
	}
	
	public int getRawType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public boolean isArray() {
		return (type & TYPE_ARRAY) != 0;
	}
	
	public void setArray(boolean b) {
		if (b) {
			type |= TYPE_ARRAY;
		}
		else {
			type &= ~TYPE_ARRAY;
		}
	}

	public List<IdName> getSelectList() {
		return selectList;
	}

	public void setSelectList(List<IdName> selectList) {
		this.selectList = selectList;
	}

	@Override
	public String toString() {
		String stype = "UNKNOWN_TYPE";
		switch(getType()) {
		case TYPE_STRING: stype = "TYPE_STRING"; break;
		case TYPE_TEXT: stype = "TYPE_TEXT"; break;
		case TYPE_ISO_DATE: stype = "TYPE_ISO_DATE"; break;
		case TYPE_ISO_DATE_TIME: stype = "TYPE_ISO_DATE_TIME"; break;
		case TYPE_BOOL: stype = "TYPE_BOOL"; break;
		case TYPE_ID_NAME: stype = "TYPE_ID_NAME"; break;
//		case TYPE_INTEGER: stype = "TYPE_INTEGER"; break;
		}
		if (isArray()) {
			stype += "[]";
		}
		return "[type=" + stype + ", id=" + id + ", name=" + name + "]";
	}

	public Suggest<IdName> getAutoCompletionSuggest() {
		return autoCompletionSuggest;
	}

	public void setAutoCompletionSuggest(Suggest<IdName> autoCompletionSuggest) {
		this.autoCompletionSuggest = autoCompletionSuggest;
	}

	
}
