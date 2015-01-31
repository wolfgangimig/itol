/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpContext;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueUpdate;
import com.wilutions.itol.db.Property;


public class IssueToHtml_off implements Closeable {

	private final ResourceBundle resb = Globals.getResourceBundle();

	private final static AtomicInteger contextCounter = new AtomicInteger(0);
	private final static DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private final Issue issue;
	private final TrafosIdToName trafos;
	private final List<HttpContext> contextesToBeRemoved = new ArrayList<HttpContext>();
	private final File tempDir;
	private final List<File> tempFiles = new ArrayList<File>();

	public IssueToHtml_off(Issue issue) throws IOException {
		this.issue = issue;
		trafos = new TrafosIdToName();
		tempDir = new File(new File(System.getProperty("java.io.tmpdir"), "itol"), "issuetohtml");
		tempDir.mkdirs();

	}

	public String getInitialUpdateAsHtml() {
		StringBuilder sbuf = new StringBuilder();
		String css = getCSS();
		sbuf.append("<html>\n");
		sbuf.append("<style type=\"text/css\">").append(css).append("</style>\n");
		sbuf.append("\n<body>");

		formatIssueUpdate(issue.getInitialUpdate(), sbuf);

		sbuf.append("\n</body>\n</html>");
		return sbuf.toString();
	}

	private void formatIssueUpdate(IssueUpdate isu, StringBuilder sbuf) {
		sbuf.append("\n<table class=\"propertyTable\" >");

		String createdByUserName = getUserNameById(isu.getCreatedBy());

		sbuf.append("<tr>\n");
		formatStringProperty(isu, Property.SUBJECT, 2, sbuf);

		String createdAtBy = MessageFormat.format(resb.getString("IssueToHtml.createdAtBy"),
				isoFormat.format(isu.getCreateDate()), createdByUserName);
		sbuf.append("\n<td class=\"propertyValue20\">\n").append(createdAtBy).append("\n</td>\n");

		sbuf.append("\n</tr><tr>\n");
		formatStringProperty(isu, Property.DESCRIPTION, 3, sbuf);
		sbuf.append("\n</tr><tr>\n");
		formatStringProperty(isu, Property.ISSUE_TYPE, 1, sbuf);
		formatStringProperty(isu, Property.CATEGORY, 1, sbuf);
		formatStringProperty(isu, Property.MILESTONES, 1, sbuf);
		sbuf.append("\n</tr><tr>\n");
		formatStringProperty(isu, Property.ASSIGNEE, 1, sbuf);
		formatStringProperty(isu, Property.PRIORITY, 1, sbuf);
		formatStringProperty(isu, Property.STATE, 1, sbuf);
		sbuf.append("\n</tr><tr>\n");
		formatStringProperty(isu, Property.ATTACHMENTS, 3, sbuf);

		sbuf.append("\n</table>");
	}

	private void formatStringProperty(IssueUpdate isu, String propertyId, int colspan, StringBuilder sbuf) {
		Property prop = isu.getProperty(propertyId);
		formatStringProperty(prop, colspan, sbuf);
	}

	private void formatStringProperty(Property property, int colspan, StringBuilder sbuf) {

		Object propertyObject = property != null && !property.isNull() ? property.getValue() : null;
		String stringValue = "";
		String[] arrayValue = new String[0];
		Attachment[] attachmentsValue = new Attachment[0];

		String cellStyleFormat = "\n<td class=\"{0}\" colspan=\"{1}\">\n";
		String cellStyle = MessageFormat.format(cellStyleFormat,
				property.getId().equals(Property.DESCRIPTION) ? "propertyValueMaxHeight" : "propertyValue20",
				Integer.toString(colspan));
		sbuf.append(cellStyle);

		if (propertyObject != null) {
			if (propertyObject instanceof String) {
				stringValue = (String) propertyObject;
			} else if (propertyObject instanceof String[]) {
				arrayValue = (String[]) propertyObject;
			} else if (propertyObject instanceof Attachment[]) {
				attachmentsValue = (Attachment[]) propertyObject;
			}
		}

		if (property.getId().equals(Property.DESCRIPTION)) {
			formatDescriptionProperty(sbuf, stringValue);
		} else if (property.getId().equals(Property.SUBJECT)) {
			String s = escapeHTML(stringValue);
			sbuf.append("<b>").append(s).append("</b>");
		} else if (property.getId().equals(Property.ATTACHMENTS)) {
			formatAttachmentsProperty(attachmentsValue, sbuf);
		} else {
			Map<String, String> valueIdToName = trafos.getTrafoValueIdToName(property.getId());
			if (arrayValue.length != 0) {
				append(arrayValue, valueIdToName, sbuf);
			} else {
				append(stringValue, valueIdToName, sbuf);
			}
		}

		sbuf.append("\n</td>\n");
	}

	protected void formatAttachmentsProperty(Attachment[] atts, StringBuilder sbuf) {
		if (atts != null && atts.length != 0) {
			for (Attachment att : atts) {
				formatAttachment(att, sbuf);
			}
		} else {
			String s = escapeHTML(resb.getString("IssueToHtml.noAttachments"));
			sbuf.append(s);
		}
	}

	protected void formatAttachment(final Attachment att, StringBuilder sbuf) {
		final AttachmentHttpServer httpServer = Globals.getThisAddin().getHttpServer();
		final int fileId = contextCounter.incrementAndGet();
		final String uri = "/" + fileId;
		sbuf.append("<a href=\"").append(httpServer.getUrl());
		sbuf.append(uri);
		sbuf.append("\">");
		sbuf.append(escapeHTML(att.getSubject()));
		sbuf.append("</a> ");
		httpServer.createContext(uri, (httpExchange) -> {
			
			httpExchange.getResponseHeaders().add("Content-Type", att.getContentType());
			httpExchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + att.getFileName() + "\"");
			OutputStream os = httpExchange.getResponseBody();

			try {
				if (httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
					
					httpExchange.sendResponseHeaders(200, att.getContentLength());
					
					IssueService srv = Globals.getIssueService();
					Attachment attWithStream = new Attachment(); //srv.readAttachment(att.getId());
					InputStream istream = attWithStream.getStream();
					
					try {
						byte[] buf = new byte[10 * 1000];
						int len = 0;
						while ((len = istream.read(buf)) != -1) {
							os.write(buf, 0, len);
						}
					} finally {
						if (istream != null) {
							istream.close();
						}
					}
				}
			}
			finally {
				if (os != null) {
					os.close();
				}
			}
		});
	}

	protected void formatDescriptionProperty(StringBuilder sbuf, String stringValue) {
		final AttachmentHttpServer httpServer = Globals.getThisAddin().getHttpServer();
		final String localValue = stringValue;
		final String uri = "/" + contextCounter.incrementAndGet();
		sbuf.append("<iframe src=\"").append(httpServer.getUrl());
		sbuf.append(uri);
		sbuf.append("\" width=\"100%\" height=\"100%\"></iframe>");
		httpServer.createContext(uri, (httpExchange) -> {
			httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
			byte[] buf = localValue.getBytes("UTF-8");
			httpExchange.sendResponseHeaders(200, buf.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(buf);
			os.close();
		});
	}

	private String getUserNameById(String id) {
		Map<String, String> trafo = trafos.getTrafoValueIdToName(Property.ASSIGNEE);
		return trafo.get(id);
	}

	private void append(String stringValue, Map<String, String> valueIdToName, StringBuilder sbuf) {
		String stringValueName = valueIdToName.get(stringValue);
		if (stringValueName == null) {
			stringValueName = stringValue;
		}
		String s = escapeHTML(stringValueName);
		sbuf.append(s);
	}

	private void append(String[] arrayValue, Map<String, String> valueIdToName, StringBuilder sbuf) {
		for (int i = 0; i < arrayValue.length; i++) {
			if (i != 0) {
				sbuf.append(", ");
			}
			append(arrayValue[i], valueIdToName, sbuf);
		}
	}

	private static String escapeHTML(String s) {
		s = s.trim();
		if (s.isEmpty())
			return "&nbsp;";

		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
				out.append("&#");
				out.append((int) c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private String getCSS() {
		String ret = "";
		InputStream is = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream("com/wilutions/itol/IssueToHtml.css");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int c = 0;
			while ((c = is.read()) != -1) {
				bos.write(c);
			}
			ret = new String(bos.toByteArray(), "UTF-8");
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Throwable e) {
				}
			}
		}
		return ret;
	}

	private static class TrafosIdToName {

		Map<String, String> assignees;
		Map<String, String> categories;
		Map<String, String> milestones;
		Map<String, String> priorities;
		Map<String, String> states;
		Map<String, String> types;

		TrafosIdToName() throws IOException {
			final IssueService srv = Globals.getIssueService();
			final Issue issue = Issue.NULL;
			try {
				this.assignees = toMap(srv.getAssignees(issue));
				this.categories = toMap(srv.getCategories(issue));
				this.milestones = toMap(srv.getMilestones(issue));
				this.priorities = toMap(srv.getPriorities(issue));
				this.states = toMap(srv.getIssueStates(issue));
				this.types = toMap(srv.getIssueTypes(issue));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private Map<String, String> toMap(List<IdName> list) {
			Map<String, String> ret = new HashMap<String, String>(list.size());
			for (IdName idn : list) {
				ret.put(idn.getId(), idn.getName());
			}
			return ret;
		}

		private Map<String, String> getTrafoValueIdToName(String propertyId) {
			Map<String, String> ret = null;
			if (propertyId.equals(Property.ASSIGNEE)) {
				ret = assignees;
			} else if (propertyId.equals(Property.CATEGORY)) {
				ret = categories;
			} else if (propertyId.equals(Property.MILESTONES)) {
				ret = milestones;
			} else if (propertyId.equals(Property.PRIORITY)) {
				ret = priorities;
			} else if (propertyId.equals(Property.STATE)) {
				ret = states;
			} else if (propertyId.equals(Property.ISSUE_TYPE)) {
				ret = types;
			} else {
				ret = new HashMap<String, String>(0);
			}

			return ret;
		}
	}

	@Override
	public void close() throws IOException {
		final AttachmentHttpServer httpServer = Globals.getThisAddin().getHttpServer();
		for (HttpContext httpContext : contextesToBeRemoved) {
			httpServer.removeContext(httpContext);
		}
		for (File file : tempFiles) {
			file.delete();
		}
	}
}
