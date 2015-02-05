package com.wilutions.itol;

import com.wilutions.com.ComException;
import com.wilutions.mslib.outlook.Attachment;
import com.wilutions.mslib.outlook.Attachments;
import com.wilutions.mslib.outlook.MailItem;

public class IssueMailItemImpl implements IssueMailItem {
	
	private final MailItem mailItem;
	private String subject;
	private String body;
	
	IssueMailItemImpl(MailItem mailItem) {
		this.mailItem = mailItem;
		this.subject = mailItem.getSubject();
		this.body = mailItem.getBody();
	}
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String mailSubject) throws ComException {
		mailItem.setSubject(this.subject = mailSubject);
	}

	public String getBody() {
		return body;
	}
	
	public void Save() {
		mailItem.Save();
	}

	@Override
	public void SaveAs(String Path, Object Type) throws ComException {
		mailItem.SaveAs(Path, Type);
	}

	@Override
	public IssueAttachments getAttachments() throws ComException {
		
		final Attachments atts = mailItem.getAttachments();
		
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

}
