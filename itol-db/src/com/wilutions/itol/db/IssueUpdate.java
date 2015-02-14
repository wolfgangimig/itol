/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class IssueUpdate implements Serializable {

	private static final long serialVersionUID = -814436458752378253L;

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

	public IssueUpdate deepCopy() {
		IssueUpdate ret = new IssueUpdate();
		ret.createDate = this.createDate;
		ret.createdBy = this.createdBy;

		for (Map.Entry<String, Property> e : this.properties.entrySet()) {
			Property prop = e.getValue();
			ret.properties.put(e.getKey(), prop);
		}

		return ret;
	}

	public int compareTo(IssueUpdate isu) {
		int ret = createDate.compareTo(isu.createDate);
		if (ret == 0) {
			ret = createdBy.compareTo(isu.createdBy);
			if (ret == 0) {
				ret = properties.size() - isu.properties.size();
				if (ret == 0) {
					for (Map.Entry<String, Property> e : properties.entrySet()) {
						Property prop = e.getValue();
						Property prop2 = isu.properties.get(prop.getId());
						if (prop2 == null) {
							ret = 1;
						} else {
							ret = prop.compareTo(prop2);
						}
						if (ret != 0) {
							break;
						}
					}
					if (ret == 0) {

					}
				}
			}
		}
		return ret;
	}
}
