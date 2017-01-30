package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.List;

/**
 * This class describes a mail.
 * Used as return value of {@link IssueService#replyToComment(Issue, ProgressCallback)}.
 */
public class MailInfo {
	
	private String TO;
	private String CC;
	private String BCC;
	private String subject;
	private String textBody;
	private String htmlBody;
	private List<Attachment> attachments;
	
	public MailInfo() {
	}
	
	public String getTO() {
		return Default.value(TO);
	}
	public void setTO(String tO) {
		TO = tO;
	}
	public String getCC() {
		return Default.value(CC);
	}
	public void setCC(String cC) {
		CC = cC;
	}
	public String getBCC() {
		return Default.value(BCC);
	}
	public void setBCC(String bCC) {
		BCC = bCC;
	}
	public String getSubject() {
		return Default.value(subject);
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getTextBody() {
		return Default.value(textBody);
	}
	public void setTextBody(String textBody) {
		this.textBody = textBody;
	}
	public String getHtmlBody() {
		return Default.value(htmlBody);
	}
	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}
	public List<Attachment> getAttachments() {
		if (attachments == null) attachments = new ArrayList<>(0);
		return attachments;
	}
	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		return "[TO=" + TO + ", CC=" + CC + ", BCC=" + BCC + ", subject=" + subject + ", textBody=" + textBody
				+ ", htmlBody=" + htmlBody + ", attachments=" + attachments + "]";
	}
	
	
}
