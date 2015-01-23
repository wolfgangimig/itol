package com.wilutions.itol.db.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.DescriptionHtmlEditor;
import com.wilutions.itol.db.FindIssuesInfo;
import com.wilutions.itol.db.FindIssuesResult;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueUpdate;
import com.wilutions.itol.db.Property;
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

	@Override
	public List<IdName> getIssueTypes(Issue iss) {
		return Arrays.asList(new IdName(TYPE_BUG, "Bug"), new IdName(TYPE_FEATURE_REQUEST, "Feature Request"),
				new IdName(TYPE_SUPPORT, "Support"), new IdName(TYPE_DOCUMENTATION, "Documentation"));
	}

	@Override
	public List<IdName> getPriorities(Issue iss) {
		return Arrays.asList(new IdName(1, "Immediately"), new IdName(2, "High priority"), new IdName(3,
				"Medium priority"), new IdName(4, "Low priority"));
	}

	@Override
	public List<IdName> getCategories(Issue iss) {
		return Arrays.asList(new IdName(1, "Project A"), new IdName(88, "Project B"), new IdName(4, "Project C"));
	}

	@Override
	public List<IdName> getMilestones(Issue iss) {
		return Arrays.asList(new IdName(1, "9.00.014"), new IdName(2, "8.00.050"), new IdName(3, "10.000.001"));
	}

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

	@Override
	public List<IdName> getAssignees(Issue iss) {
		List<IdName> ret = null;
		if (iss != null && iss.getType().equals(String.valueOf(TYPE_DOCUMENTATION))) {
			ret = assignees.stream().filter(idn -> ASSIGNEES_FOR_DOCUMENTATION.contains(Integer.parseInt(idn.getId())))
					.collect(Collectors.toList());
		} else {
			ret = assignees;
		}
		return ret;
	}

	@Override
	public IdName getCurrentUser() {
		return getAssignees(null).get(3);
	}

	@Override
	public List<IdName> getIssueStates(Issue iss) {
		return Arrays.asList(new IdName(1, "New issue"), new IdName(2, "In Progress"), new IdName(3,
				"Waiting for feedback"), new IdName(4, "Solved"), new IdName(5, "Closed"));
	}

	@Override
	public Issue createIssue() {
		IssueUpdate issi = new IssueUpdate();
		issi.setProperty(new Property(Property.ISSUE_TYPE, getIssueTypes(Issue.NULL).get(0).getId()));
		issi.setProperty(new Property(Property.ASSIGNEE, getAssignees(Issue.NULL).get(0).getId()));
		issi.setProperty(new Property(Property.CATEGORY, getCategories(Issue.NULL).get(0).getId()));
		issi.setProperty(new Property(Property.PRIORITY, getPriorities(Issue.NULL).get(2).getId()));
		issi.setProperty(new Property(Property.STATE, getIssueStates(Issue.NULL).get(0).getId()));
		Issue iss = new Issue("0", issi);
		return iss;
	}

	@Override
	public Issue updateIssue(Issue iss) {
		String id = String.valueOf(issues.size() + 1);
		Issue copy = new Issue(id, iss);
		issues.put(id, copy);
		return copy;
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

	@Override
	public FindIssuesResult findFirstIssues(FindIssuesInfo findInfo, int idx, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FindIssuesResult findNextIssues(String searchId, int idx, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void findCloseIssues(String searchId) {
		// TODO Auto-generated method stub
	}

	@Override
	public String extractIssueIdFromMailSubject(String subject) {
		assert subject != null;
		String issueId = "";
		int p = subject.indexOf("[");
		if (p >= 0) {
			int q = subject.indexOf("]", p);
			if (q >= 0) {
				int m = subject.indexOf("-", p);
				if (m >= 0 && m < q) {
					issueId = subject.substring(m + 1, q);
				}
			}
		}
		return issueId;
	}

	@Override
	public String injectIssueIdIntoMailSubject(String subject, Issue iss) {
		assert subject != null;
		assert iss != null;
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("[");
		switch (Integer.parseInt(iss.getType())) {
		case TYPE_BUG:
			sbuf.append("F");
			break;
		case TYPE_DOCUMENTATION:
			sbuf.append("D");
			break;
		case TYPE_FEATURE_REQUEST:
			sbuf.append("R");
			break;
		case TYPE_SUPPORT:
			sbuf.append("S");
			break;
		default:
			throw new IllegalStateException("Unknown issue type=" + iss.getType());
		}
		sbuf.append("-").append(iss.getId()).append("] ");
		sbuf.append(subject);
		return sbuf.toString();
	}

	@Override
	public Issue readIssue(String issueId) {
		return issues.get(issueId);
	}

	private File getFileForAttachmentId(String attachmentId) {
		return new File(tempDir, attachmentId);
	}

	private String getAttachmentIdForFile(File file) {
		return file.getName();
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
			att.setFileName(props.getProperty("fileName"));

			att.setContentLength(contentFile.length());
			att.setStream(new FileInputStream(contentFile));

		} finally {
			if (istream != null) {
				istream.close();
			}
		}
		return att;
	}

	@Override
	public Attachment writeAttachment(Attachment att) throws IOException {

		boolean isFileExt = att.getContentType().indexOf('.') == 0;
		String fileExt = isFileExt ? att.getContentType() : ".data";

		File contentFile = File.createTempFile("issue", fileExt, tempDir);

		// File name is attachment ID
		att.setId(getAttachmentIdForFile(contentFile));

		writeAttachmentContent(att, contentFile);

		File propertiesFile = new File(contentFile.getAbsolutePath() + ".properties");
		wirteAttachmentProperties(att, propertiesFile, contentFile);

		return att;
	}

	private void wirteAttachmentProperties(Attachment att, File propertiesFile, File contentFile) throws IOException {

		Properties props = new Properties();
		props.put("id", att.getId());
		props.put("contentType", att.getContentType());
		props.put("subject", att.getSubject());
		props.put("contentLength", "" + contentFile.length());
		props.put("fileName", att.getFileName());

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(propertiesFile);
			props.store(fos, "Attachment properties");
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	protected void writeAttachmentContent(Attachment att, File file) throws FileNotFoundException, IOException {
		FileOutputStream fos = null;
		InputStream istream = null;
		try {
			istream = att.getStream();
			fos = new FileOutputStream(file);
			byte[] buf = new byte[10 * 1000];
			int len = 0;
			while ((len = istream.read(buf)) != -1) {
				fos.write(buf, 0, len);
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
			if (istream != null) {
				istream.close();
			}
		}
	}

	@Override
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
	public List<Property> getDetails(Issue issue) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DescriptionHtmlEditor getDescriptionHtmlEditor(Issue issue) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


}