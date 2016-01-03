/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.DescriptionTextEditor;
import com.wilutions.itol.db.FindIssuesInfo;
import com.wilutions.itol.db.FindIssuesResult;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueHtmlEditor;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueUpdate;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.PropertyClasses;

public class IssueServiceImpl implements IssueService {

	private HashMap<String, Issue> issues = new HashMap<String, Issue>();
	private File tempDir;

	public final static int TYPE_BUG = 1;
	public final static int TYPE_FEATURE_REQUEST = 2;
	public final static int TYPE_SUPPORT = 3;
	public final static int TYPE_DOCUMENTATION = 4;

	public IssueServiceImpl() {
		tempDir = new File(new File(System.getProperty("java.io.tmpdir"), "itol"), "issuetracker");
		tempDir.mkdirs();
	}

//	@Override
//	public List<IdName> getIssueTypes(Issue iss) {
//		return Arrays.asList(new IdName(TYPE_BUG, "Bug"), new IdName(TYPE_FEATURE_REQUEST, "Feature Request"),
//				new IdName(TYPE_SUPPORT, "Support"), new IdName(TYPE_DOCUMENTATION, "Documentation"));
//	}
//
//	@Override
//	public List<IdName> getPriorities(Issue iss) {
//		return Arrays.asList(new IdName(1, "Immediately"), new IdName(2, "High priority"), new IdName(3,
//				"Medium priority"), new IdName(4, "Low priority"));
//	}
//
//	@Override
//	public List<IdName> getCategories(Issue iss) {
//		return Arrays.asList(new IdName(1, "Project A"), new IdName(88, "Project B"), new IdName(4, "Project C"));
//	}
//
//	@Override
//	public List<IdName> getMilestones(Issue iss) {
//		return Arrays.asList(new IdName(1, "9.00.014"), new IdName(2, "8.00.050"), new IdName(3, "10.000.001"));
//	}

	private final static List<IdName> assignees;
	private final static Set<Integer> ASSIGNEES_FOR_DOCUMENTATION = new HashSet<Integer>(Arrays.asList(2, 3, 4, 5, 6));

	static {
		int i = 0;
		assignees = Arrays.asList(new IdName(++i, "Anne Napper"), new IdName(++i, "Bert Ebbing"), new IdName(++i,
				"Charlotte Hall"), new IdName(++i, "Delmon Esser"), new IdName(++i, "Fritz Recker"), new IdName(++i,
				"Gertrude Elsewhere"), new IdName(++i, "Hardy Amster"), new IdName(++i, "Iris Rolo"), new IdName(++i,
				"Jeff Elba"), new IdName(++i, "Kathy Adorno"), new IdName(++i, "Lionel Itaka"), new IdName(++i,
				"Monica Oakley"), new IdName(++i, "Neels Eddy"), new IdName(++i, "Olivia Laine"), new IdName(++i,
				"Paul Adams"), new IdName(++i, "Quoba Ustick"), new IdName(++i, "Rafi Althof"), new IdName(++i,
				"Sabrina Althof"), new IdName(++i, "Timothy Ingham"), new IdName(++i, "Ulla Laaten"), new IdName(++i,
				"Victor Irmscher"), new IdName(++i, "Wilhelma Irvine"), new IdName(++i, "Xen Eggert"), new IdName(++i,
				"Yoko Ogren"), new IdName(++i, "Zak Arnold"));
	}

//	@Override
//	public List<IdName> getAssignees(Issue iss) {
//		List<IdName> ret = null;
//		if (iss != null && iss.getType().equals(String.valueOf(TYPE_DOCUMENTATION))) {
//			ret = assignees.stream().filter(idn -> ASSIGNEES_FOR_DOCUMENTATION.contains(Integer.parseInt(idn.getId())))
//					.collect(Collectors.toList());
//		} else {
//			ret = assignees;
//		}
//		return ret;
//	}

	@Override
	public IdName getCurrentUser() {
		return new IdName(0, ""); //getAssignees(null).get(3);
	}

//	@Override
//	public List<IdName> getIssueStates(Issue iss) {
//		return Arrays.asList(new IdName(1, "New issue"), new IdName(2, "In Progress"), new IdName(3,
//				"Waiting for feedback"), new IdName(4, "Solved"), new IdName(5, "Closed"));
//	}

	@Override
	public Issue createIssue(String subject, String description, String defaultIssueAsString) {
//		issi.setProperty(new Property(Property.ISSUE_TYPE, getIssueTypes(Issue.NULL).get(0).getId()));
//		issi.setProperty(new Property(Property.ASSIGNEE, getAssignees(Issue.NULL).get(0).getId()));
//		issi.setProperty(new Property(Property.CATEGORY, getCategories(Issue.NULL).get(0).getId()));
//		issi.setProperty(new Property(Property.PRIORITY, getPriorities(Issue.NULL).get(2).getId()));
//		issi.setProperty(new Property(Property.STATE, getIssueStates(Issue.NULL).get(0).getId()));
		Issue iss = new Issue();
		iss.setSubject(subject);
		iss.setDescription(description);
		return iss;
	}

	@Override
	public Issue validateIssue(Issue iss) {
		Issue ret = iss;
		if (iss.getType().equals(String.valueOf(TYPE_DOCUMENTATION))) {

			int userId = Integer.parseInt(iss.getAssignee());
			if (!ASSIGNEES_FOR_DOCUMENTATION.contains(userId)) {
				iss.setAssignee(null);
			}

		}
		return ret;
	}

	public FindIssuesResult findFirstIssues(FindIssuesInfo findInfo, int idx, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	public FindIssuesResult findNextIssues(String searchId, int idx, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	public void findCloseIssues(String searchId) {
		// TODO Auto-generated method stub
	}

	public Issue readIssue(String issueId) {
		return issues.get(issueId);
	}

	private File getFileForAttachmentId(String attachmentId) {
		return new File(tempDir, attachmentId);
	}

	public Attachment readAttachment(String attachmentId) throws IOException {
		Attachment att = readAttachmentProperties(attachmentId);
		att.setStream(readAttachmentContent(attachmentId));
		return att;
	}

	private InputStream readAttachmentContent(String attachmentId) throws IOException {
		return new FileInputStream(getFileForAttachmentId(attachmentId));
	}

	private Attachment readAttachmentProperties(String attachmentId) throws IOException {
		Attachment att = new Attachment();
		File contentFile = getFileForAttachmentId(attachmentId);
		InputStream istream = null;
		try {
			istream = new FileInputStream(new File(contentFile.getAbsolutePath() + ".properties"));
			Properties props = new Properties();
			props.load(istream);

			att.setId(props.getProperty("id"));
			att.setContentType(props.getProperty("contentType"));
			att.setSubject(props.getProperty("subject"));

			att.setContentLength(contentFile.length());
			att.setStream(new FileInputStream(contentFile));

		} finally {
			if (istream != null) {
				istream.close();
			}
		}
		return att;
	}

	public void deleteAttachment(String attachmentId) throws IOException {
		File file = new File(attachmentId);
		file.delete();
		File propertiesFile = new File(attachmentId + ".properties");
		propertiesFile.delete();
	}

	@Override
	public List<Property> getConfig() {
		return new ArrayList<Property>();
	}

	@Override
	public void setConfig(List<Property> configProps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PropertyClasses getPropertyClasses() {
		return PropertyClasses.getDefault();
	}

	@Override
	public IssueHtmlEditor getHtmlEditor(Issue issue, String propertyId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getShowIssueUrl(String issueId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public DescriptionTextEditor getDescriptionTextEditor(Issue issue)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PropertyClass getPropertyClass(String propertyId, Issue iss) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getPropertyDisplayOrder(Issue iss) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIssueHistoryUrl(String issueId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Issue updateIssue(Issue iss, List<String> modifiedProperties, ProgressCallback cb) throws IOException {
		String id = String.valueOf(issues.size() + 1);
		Issue copy = new Issue(id, iss);
		issues.put(id, copy);
		return copy;
	}

	@Override
	public String getDefaultIssueAsString(Issue iss) throws IOException {
		return "";
	}

	@Override
	public List<IdName> getPropertyAutoCompletion(String propertyId, Issue iss, String filter, int max) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}


}