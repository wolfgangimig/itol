package com.wilutions.itol;

import java.util.Date;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.mslib.outlook.Attachment;
import com.wilutions.mslib.outlook.Attachments;
import com.wilutions.mslib.outlook.MailItem;

public class IssueMailItemImpl implements IssueMailItem {

	private final IDispatch mailItem;
	private String subject;
	private String body;
	private String entryId;
	private String from;
	private String to;
	private Date receivedTime;

	IssueMailItemImpl(MailItem mailItem) {
		this.mailItem = mailItem;
		// this.subject = (String)mailItem._get("Subject");
		// this.body = (String)mailItem._get("Body");
		// this.entryId = (String)mailItem._get("EntryID");
		// this.from = (String)mailItem._get("SenderName");
		// this.to = (String)mailItem._get("ReceivedByName");
		this.subject = mailItem.getSubject();
		this.body = mailItem.getBody();
		this.entryId = mailItem.getEntryID();
		this.from = mailItem.getSenderName();
		this.to = mailItem.getReceivedByName();
		this.receivedTime = mailItem.getReceivedTime();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String mailSubject) throws ComException {
		mailItem._put("Subject", mailSubject);
	}

	public String getBody() {
		return body;
	}

	public String getEntryId() {
		return entryId;
	}

	public void Save() {
		mailItem._call("Save");
	}

	@Override
	public void SaveAs(String Path, Object Type) throws ComException {
		mailItem._call("SaveAs", Path, Type);
	}

	@Override
	public IssueAttachments getAttachments() throws ComException {

		final IDispatch disp = (IDispatch) mailItem._get("Attachments");
		final Attachments atts = disp.as(Attachments.class);

		IssueAttachments ret = new IssueAttachments() {

			@Override
			public int getCount() throws ComException {
				return atts.getCount();
			}

			@Override
			public Attachment getItem(int i) throws ComException {
				return atts.Item(i);
			}

		};
		return ret;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Date getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(Date receivedTime) {
		this.receivedTime = receivedTime;
	}

}
