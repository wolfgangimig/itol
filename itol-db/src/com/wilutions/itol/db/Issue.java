/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Issue implements Serializable {

	private static final long serialVersionUID = 7470768205802774676L;
	
	public final static Issue NULL = new Issue();

	private String id;
	
	private String parentIssueId;
	
	private List<String> subIssueIds;
	
	private List<String> relatedIssueIds;
	
	/**
	 * Current issue data.
	 */
	private IssueUpdate currentUpdate;
	
	/**
	 * Issue items in reverse order.
	 * The oldest item is at index 0.
	 */
	private List<IssueUpdate> updates;
	
	/**
	 * Initialize.
	 */
	public Issue() {
		this.id = "";
		this.currentUpdate = new IssueUpdate();
		this.updates = new ArrayList<IssueUpdate>();
	}
	
	public Issue(String id, Issue rhs) {
		assert id != null && id.length() != 0;
		assert rhs != null;
		this.id = id;
		this.currentUpdate = rhs.getCurrentUpdate();
		this.updates = rhs.getUpdates();
	}
	
	@Override
	public Object clone() {
		Issue ret = new Issue();
		ret.id = this.id;
		ret.currentUpdate = (IssueUpdate)this.currentUpdate.clone();
		ret.updates = new ArrayList<IssueUpdate>(this.updates.size());
		for (IssueUpdate upd : this.updates) {
			IssueUpdate updCopy = (IssueUpdate)upd.clone();
			ret.updates.add(updCopy);
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object rhs) {
		boolean ret = false;
		if (rhs != null && rhs instanceof Issue) {
			Issue issue = (Issue)rhs;
			ret = this.id.equals(issue.id);
			if (ret) {
				ret = currentUpdate.equals(issue.currentUpdate);
			}
			if (ret) {
				ret = updates.equals(issue.updates);
			}
		}
		return ret;
	}
	
	public void findChangedMembers(Issue rhs, List<String> propIds) {
		if (rhs != null) {
			currentUpdate.findChangedMembers(rhs.currentUpdate, propIds);
		}
	}
	
	public boolean isNew() {
		return this.id.isEmpty();
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		if (id == null) id = "";
		return id;
	}

	public String getParentIssueId() {
		if (parentIssueId == null) parentIssueId = "";
		return parentIssueId;
	}

	public void setParentIssueId(String parentIssueId) {
		this.parentIssueId = parentIssueId;
	}

	public List<String> getSubIssueIds() {
		if (subIssueIds == null) subIssueIds = new ArrayList<String>();
		return subIssueIds;
	}

	public List<String> getRelatedIssueIds() {
		if (relatedIssueIds == null) relatedIssueIds = new ArrayList<String>();
		return relatedIssueIds;
	}

	public List<IssueUpdate> getUpdates() {
		return updates;
	}

	public IssueUpdate getCurrentUpdate() {
		return currentUpdate;
	}
	
	public IssueUpdate getLastUpdate() {
		IssueUpdate ret = currentUpdate;
		if (updates.size() != 0) {
			ret = updates.get(updates.size()-1);
		}
		return ret;
	}
	
	public Object getPropertyValue(String propertyId, Object defaultValue) {
		IssueUpdate update = getCurrentUpdate();
		Property property = update.getProperty(propertyId);
		Object ret = property.getValue();
		if (ret == null) {
			ret = defaultValue;
			setPropertyValue(propertyId, ret);
		}
		return ret;
	}
	
	public void setPropertyValue(String propertyId, Object value) {
		if (value != null) {
			Property prop = new Property(propertyId, value);
			getCurrentUpdate().setProperty(prop);
		}
		else {
			getCurrentUpdate().removeProperty(propertyId);
		}
	}
	
	public String getPropertyString(String propertyId, String defaultValue) {
		return (String)getPropertyValue(propertyId, defaultValue);
	}
	
	public void setPropertyString(String propertyId, String value) {
		setPropertyValue(propertyId, value);
	}
	
	public IdName getPropertyIdName(String propertyId, IdName defaultValue) {
		Object value = getPropertyValue(propertyId, defaultValue);
		return (IdName)value;
	}
	
	public void setPropertyIdName(String propertyId, IdName value) {
		setPropertyValue(propertyId, value);
	}
	

	public Boolean getPropertyBoolean(String propertyId, Boolean defaultValue) {
		return (Boolean)getPropertyValue(propertyId, defaultValue);
	}
	
	public void setPropertyBoolean(String propertyId, Boolean value) {
		setPropertyValue(propertyId, value);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getPropertyStringList(String propertyId, List<String> defaultValue) {
		return (List<String>)getPropertyValue(propertyId, defaultValue);
	}
	
	public void setPropertyStringList(String propertyId, List<String> value) {
		setPropertyValue(propertyId, value);
	}
	
	public IdName getType() {
		return (IdName)getPropertyValue(Property.ISSUE_TYPE, IdName.NULL);
	}
	
	public void setType(IdName value) {
		setPropertyValue(Property.ISSUE_TYPE, value);
	}
	
	public IdName getStatus() {
		return (IdName)getPropertyValue(Property.STATUS, IdName.NULL);
	}
	
	public void setStatus(IdName value) {
		setPropertyValue(Property.STATUS, value);
	}
	
	public IdName getProject() {
		return (IdName)getPropertyValue(Property.PROJECT, IdName.NULL);
	}
	
	public void setProject(IdName value) {
		setPropertyValue(Property.PROJECT, value);
	}
	
	public IdName getPriority() {
		return (IdName)getPropertyValue(Property.PRIORITY, IdName.NULL);
	}
	
	public void setPriority(IdName value) {
		setPropertyValue(Property.PRIORITY, value);
	}

	public String getSubject() {
		return (String)getPropertyValue(Property.SUBJECT, "");
	}

	public void setSubject(String value) {
		setPropertyValue(Property.SUBJECT, value);
	}
	
	public String getDescription() {
		return (String)getPropertyValue(Property.DESCRIPTION, "");
	}

	public void setDescription(String value) {
		setPropertyValue(Property.DESCRIPTION, value);
	}
	
	@SuppressWarnings("unchecked")
	public List<Attachment> getAttachments() {
		List<Attachment> ret = (List<Attachment>)getPropertyValue(Property.ATTACHMENTS, null);
		if (ret == null) {
			ret = new ArrayList<Attachment>(0);
			setPropertyValue(Property.ATTACHMENTS, ret);
		}
		return ret;
	}
	
	public void setAttachments(List<Attachment> atts) {
		setPropertyValue(Property.ATTACHMENTS, atts);
	}
	
	public Date getLastModified() {
		Date ret = getLastUpdate().getCreateDate();
		return ret;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", subject=" + getSubject() + "]";
	}

	
}
