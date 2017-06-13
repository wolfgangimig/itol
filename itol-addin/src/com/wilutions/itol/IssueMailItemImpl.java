package com.wilutions.itol;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.itol.db.Default;
import com.wilutions.mslib.outlook.AddressEntry;
import com.wilutions.mslib.outlook.Attachment;
import com.wilutions.mslib.outlook.Attachments;
import com.wilutions.mslib.outlook.ExchangeUser;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlAddressEntryUserType;
import com.wilutions.mslib.outlook.OlBodyFormat;

public class IssueMailItemImpl implements IssueMailItem {

	private final static Logger log = Logger.getLogger("IssueMailItemImpl");
	private final IDispatch mailItem;
	private String subject;
	private String body;
	private String htmlBody;
	private String entryId;
	private String from;
	private String fromAddress;
	private String to;
	private Date receivedTime;
	private OlBodyFormat bodyFormat;

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
		this.setFromAddress(getSenderEmailAddress(mailItem));
		// mailItem.getRecipients().Item(1).getAddressEntry().
		this.to = mailItem.getReceivedByName();
		this.receivedTime = mailItem.getReceivedTime();
		this.htmlBody = mailItem.getHTMLBody();
		this.bodyFormat = mailItem.getBodyFormat();
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

	@Override
	public String getHTMLBody() {
		return htmlBody;
	}

	public OlBodyFormat getBodyFormat() {
		return this.bodyFormat;
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

	@Override
	public boolean isNew() {
		return entryId.isEmpty();
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	private String getSenderEmailAddress(MailItem mail) {
		if (log.isLoggable(Level.FINE)) log.fine("getSenderEmailAddress(");
		String senderEmailAddress = "";

		String mailType = mail.getSenderEmailType();
		if (log.isLoggable(Level.FINE)) log.fine("mailType=" + mailType);
		
		if (Default.value(mailType).equals("EX")) {
			AddressEntry sender = mail.getSender();
			if (log.isLoggable(Level.FINE)) log.fine("AddressEntry sender=" + sender);
			if (sender != null) {

				OlAddressEntryUserType userType = sender.getAddressEntryUserType();
				if (log.isLoggable(Level.FINE)) log.fine("sender.addressEntryUserType=" + userType);
				
				if (userType == OlAddressEntryUserType.olExchangeUserAddressEntry
						|| userType == OlAddressEntryUserType.olExchangeRemoteUserAddressEntry) {
					ExchangeUser exchUser = sender.GetExchangeUser();
					if (log.isLoggable(Level.FINE)) log.fine("exchUser=" + exchUser);

					if (exchUser != null) {
						senderEmailAddress = exchUser.getPrimarySmtpAddress();
					}
				}
			}
		}

		if (Default.value(senderEmailAddress).isEmpty()) {
			senderEmailAddress = mail.getSenderEmailAddress();
		}

		if (log.isLoggable(Level.FINE)) log.fine(")getSenderEmailAddress=" + senderEmailAddress);
		return senderEmailAddress;
	}
}
