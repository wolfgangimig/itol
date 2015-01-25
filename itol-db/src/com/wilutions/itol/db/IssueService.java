package com.wilutions.itol.db;

import java.io.IOException;
import java.util.List;

public interface IssueService {
	
	List<Property> getConfig();
	void setConfig(List<Property> configProps);
	
	PropertyClasses getPropertyClasses();

	List<IdName> getIssueTypes(Issue iss) throws IOException;

	List<IdName> getPriorities(Issue iss) throws IOException;

	List<IdName> getCategories(Issue iss) throws IOException;

	List<IdName> getAssignees(Issue iss) throws IOException;

	List<IdName> getIssueStates(Issue iss) throws IOException;

	List<IdName> getMilestones(Issue issue) throws IOException;

	List<Property> getDetails(Issue issue) throws IOException;
	
	DescriptionHtmlEditor getDescriptionHtmlEditor(Issue issue) throws IOException;

	Issue createIssue() throws IOException;

	Issue validateIssue(Issue iss) throws IOException;

	Issue updateIssue(Issue iss, ProgressCallback cb) throws IOException;

	Issue readIssue(String issueId) throws IOException;

	String extractIssueIdFromMailSubject(String subject) throws IOException;

	String injectIssueIdIntoMailSubject(String subject, Issue iss) throws IOException;

	FindIssuesResult findFirstIssues(FindIssuesInfo findInfo, int idx, int max) throws IOException;

	FindIssuesResult findNextIssues(String searchId, int idx, int max) throws IOException;

	void findCloseIssues(String searchId) throws IOException;

	Attachment readAttachment(String attachmentId) throws IOException;

	void deleteAttachment(String attachmentId) throws IOException;

	IdName getCurrentUser() throws IOException;

	String getShowIssueUrl(String issueId) throws IOException;

	String getMsgFileType() throws IOException;
}
