package com.wilutions.itol;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.mslib.outlook.Attachment;
import com.wilutions.mslib.outlook.Attachments;

public class IssueMailItemImpl implements IssueMailItem {
	
	private final IDispatch mailItem;
	private String subject;
	private String body;
	
	IssueMailItemImpl(IDispatch mailItem) {
		this.mailItem = mailItem;
		this.subject = (String)mailItem._get("Subject");
		this.body = (String)mailItem._get("Body");
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
	
	public void Save() {
		mailItem._call("Save");
	}

	@Override
	public void SaveAs(String Path, Object Type) throws ComException {
		mailItem._call("SaveAs", Path, Type);
	}

	@Override
	public IssueAttachments getAttachments() throws ComException {
		
		final Attachments atts = (Attachments)mailItem._get("Attachments");
		
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
