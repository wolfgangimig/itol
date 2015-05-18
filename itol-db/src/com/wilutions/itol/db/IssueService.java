/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.IOException;
import java.util.List;

public interface IssueService {
	
	List<Property> getConfig();
	void setConfig(List<Property> configProps);
	
	void initialize();
	
	PropertyClasses getPropertyClasses();
	
	PropertyClass getPropertyClass(String propertyId, Issue iss) throws IOException;
	
	List<String> getPropertyDisplayOrder(Issue issue);
	
	List<IdName> getPropertyAutoCompletion(String propertyId, Issue iss, String filter) throws IOException;
	
//	List<IdName> getIssueTypes(Issue iss) throws IOException;
//
//	List<IdName> getPriorities(Issue iss) throws IOException;
//
//	List<IdName> getCategories(Issue iss) throws IOException;
//
//	List<IdName> getAssignees(Issue iss) throws IOException;
//
//	List<IdName> getIssueStates(Issue iss) throws IOException;
//
//	List<IdName> getMilestones(Issue issue) throws IOException;

	IssueHtmlEditor getHtmlEditor(Issue issue, String propertyId) throws IOException;

	Issue createIssue(String subject, String description, String defaultIssueAsString) throws IOException;

	Issue validateIssue(Issue iss) throws IOException;

	String extractIssueIdFromMailSubject(String subject) throws IOException;

	String injectIssueIdIntoMailSubject(String subject, Issue iss) throws IOException;

	IdName getCurrentUser() throws IOException;

	String getShowIssueUrl(String issueId) throws IOException;

	String getMsgFileType() throws IOException;

	Issue updateIssue(Issue iss, List<String> modifiedProperties, ProgressCallback cb) throws IOException;

	Issue readIssue(String issueId) throws IOException;
	
	String getDefaultIssueAsString(Issue iss) throws IOException;
	
	String getIssueHistoryUrl(String issueId) throws IOException;
	
//
//	FindIssuesResult findFirstIssues(FindIssuesInfo findInfo, int idx, int max) throws IOException;
//
//	FindIssuesResult findNextIssues(String searchId, int idx, int max) throws IOException;
//
//	void findCloseIssues(String searchId) throws IOException;
//
//	Attachment readAttachment(String attachmentId) throws IOException;
//
//	void deleteAttachment(String attachmentId) throws IOException;

}
