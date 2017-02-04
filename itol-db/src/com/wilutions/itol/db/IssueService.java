/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.util.Collection;
import java.util.List;

public interface IssueService {
	
	Config getConfig();
	void setConfig(Config config);
	
	void initialize() throws Exception;
	
	PropertyClass getPropertyClass(String propertyId, Issue iss) throws Exception;
	
	List<String> getPropertyDisplayOrder(Issue issue);
	
	List<IdName> getPropertyAutoCompletion(String propertyId, Issue iss, String filter, int max) throws Exception;
	
//	List<IdName> getIssueTypes(Issue iss) throws Exception;
//
//	List<IdName> getPriorities(Issue iss) throws Exception;
//
//	List<IdName> getCategories(Issue iss) throws Exception;
//
//	List<IdName> getAssignees(Issue iss) throws Exception;
//
//	List<IdName> getIssueStates(Issue iss) throws Exception;
//
//	List<IdName> getMilestones(Issue issue) throws Exception;

	IssuePropertyEditor getPropertyEditor(Object parent, Issue issue, String propertyId) throws Exception;

	Issue createIssue(String subject, String description) throws Exception;

	/**
	 * Validate properties in given issue and set default values for invalid properties.
	 * This function is called when the project or issue type has been changed.
	 * @param iss
	 * @param modifiedProperties
	 * @return Issue
	 * @throws Exception
	 */
	void validateIssue(Issue iss) throws Exception;

	String extractIssueIdFromMailSubject(String subject) throws Exception;

	String injectIssueIdIntoMailSubject(String subject, Issue iss) throws Exception;

	IdName getCurrentUser() throws Exception;

	String getShowIssueUrl(String issueId) throws Exception;

	Issue updateIssue(Issue iss, List<String> modifiedProperties, ProgressCallback cb) throws Exception;

	Issue readIssue(String issueId, ProgressCallback cb) throws Exception;
	
	MailInfo replyToComment(Issue issue, String mailTo, String comment) throws Exception;
	
	Collection<Issue> findIssues(String query, int maxResults) throws Exception;
	
	void setDefaultIssue(Issue iss) throws Exception;
	
	// TODO: rename to getHistory
	String getIssueHistoryUrl(String issueId) throws Exception;
	
	String downloadAttachment(String url, ProgressCallback cb) throws Exception;
	
//
//	FindIssuesResult findFirstIssues(FindIssuesInfo findInfo, int idx, int max) throws Exception;
//
//	FindIssuesResult findNextIssues(String searchId, int idx, int max) throws Exception;
//
//	void findCloseIssues(String searchId) throws Exception;
//
//	Attachment readAttachment(String attachmentId) throws Exception;
//
//	void deleteAttachment(String attachmentId) throws Exception;

}
