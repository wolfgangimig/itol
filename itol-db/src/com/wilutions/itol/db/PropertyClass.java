/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.util.List;

public class PropertyClass {

	public final static int TYPE_STRING = 1;
	public final static int TYPE_BOOL = 2;
	public final static int TYPE_PASSWORD = 3;
	public final static int TYPE_STRING_LIST = 4;
	public final static int TYPE_ISO_DATE = 5;
	
	private String id;
	private String name;
	private Object defaultValue;
	private int type;
	private List<IdName> selectList;

	public PropertyClass(int type, String id, String name, Object defaultValue, List<IdName> selectList) {
		super();
		this.type = type;
		this.id = id;
		this.name = name;
		this.defaultValue = defaultValue;
		this.selectList = selectList;
	}

	public PropertyClass(int type, String id, String name, Object defaultValue) {
		this(type, id, name, defaultValue, null);
	}

	public PropertyClass(int type, String id, String name) {
		this(type, id, name, null, null);
	}
	
	public PropertyClass(PropertyClass rhs) {
		this.type = rhs.type;
		this.id = rhs.id;
		this.name = rhs.name;
		this.defaultValue = rhs.defaultValue;
		this.selectList = rhs.selectList;
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
		return type;
	}

	public void setType(int type) {
		this.type = type;
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
		switch(type) {
		case TYPE_STRING: stype = "TYPE_STRING"; break;
		case TYPE_STRING_LIST: stype = "TYPE_STRING_LIST"; break;
		case TYPE_ISO_DATE: stype = "TYPE_ISO_DATE"; break;
		case TYPE_BOOL: stype = "TYPE_BOOL"; break;
		}
		return "PropertyClass [type=" + stype + ", id=" + id + ", name=" + name + "]";
	}

	
}
