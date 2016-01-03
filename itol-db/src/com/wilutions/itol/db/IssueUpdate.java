/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueUpdate implements Serializable {

	private static final long serialVersionUID = -814436458752378253L;
	
	private String id;

	private Date createDate;

	private String createdBy;

	private Map<String, Property> properties;

	public IssueUpdate() {
		createDate = new Date(System.currentTimeMillis());
		properties = new HashMap<String, Property>();
		createdBy = "";
	}

	public IssueUpdate(Date createDate, String createdBy, Map<String, Property> props) {
		this.createDate = createDate != null ? createDate : new Date(System.currentTimeMillis());
		this.createdBy = createdBy;
		this.properties = props;
	}

	public Map<String, Property> getProperties() {
		return properties;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public Property getProperty(String propertyId) {
		return properties.getOrDefault(propertyId, new Property(propertyId, null));
	}

	public void setProperty(Property prop) {
		if (prop.isNull()) {
			properties.remove(prop.getId());
		} else {
			properties.put(prop.getId(), prop);
		}
	}

	public Property removeProperty(String propertyId) {
		return properties.remove(propertyId);
	}

	public String getCreatedBy() {
		return createdBy;
	}

	@Override
	public Object clone() {
		IssueUpdate ret = new IssueUpdate();
		ret.createDate = this.createDate;
		ret.createdBy = this.createdBy;

		for (Map.Entry<String, Property> e : this.properties.entrySet()) {
			Property prop = e.getValue();
			Property propCopy = (Property) prop.clone();
			ret.properties.put(e.getKey(), propCopy);
		}

		return ret;
	}

	@Override
	public boolean equals(Object rhs) {
		boolean ret = false;
		if (rhs != null && rhs instanceof IssueUpdate) {
			IssueUpdate isu = (IssueUpdate) rhs;
			ret = createDate.equals(isu.createDate);
			if (ret) {
				ret = createdBy.equals(isu.createdBy);
				if (ret) {
					ret = properties.equals(isu.properties);
				}
			}
		}
		return ret;
	}

	public void findChangedMembers(IssueUpdate rhs, List<String> propIds) {
		for (Map.Entry<String, Property> e : properties.entrySet()) {
			String propId = e.getKey();
			Property propL = e.getValue();
			Property propR = rhs.properties.get(propId);
			if (propR == null || !propL.equals(propR)) {
				propIds.add(propId);
			}
		}
		for (String propId : rhs.properties.keySet()) {
			if (!properties.containsKey(propId)) {
				propIds.add(propId);
			}
		}
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setCreateDateIso(String createDate) {
		this.createDate = Date.from(ZonedDateTime.parse(createDate).toInstant());
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public void setProperties(Map<String, Property> properties) {
		this.properties = properties;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
