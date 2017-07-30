package com.wilutions.itol;

import java.util.Date;

import com.wilutions.com.ComException;
import com.wilutions.mslib.outlook.Attachment;
import com.wilutions.mslib.outlook.OlBodyFormat;

public class IssueMailItemBlank implements IssueMailItem {

	@Override
	public String getSubject() throws ComException {
		return "";
	}

	@Override
	public void setSubject(String mailSubject) throws ComException {
	}

	@Override
	public String getBody() throws ComException {
		return "";
	}
	
	@Override
	public String getHTMLBody() throws ComException {
		return "";
	}
	
	@Override
	public OlBodyFormat getBodyFormat() {
		return OlBodyFormat.olFormatPlain;
	}
	
	@Override
	public String getFrom() {
		return "";
	}
	
	@Override
	public String getTo() {
		return "";
	}

	@Override
	public void Save() throws ComException {
	}

	@Override
	public void SaveAs(String Path, Object Type) throws ComException {
	}

	@Override
	public IssueAttachments getAttachments() throws ComException {
		IssueAttachments ret = new IssueAttachments() {
			
			@Override
			public int getCount() throws ComException {
				return 0;
			}

			@Override
			public Attachment getItem(int i) throws ComException {
				throw new ComException();
			}
			
		};
		return ret;
	}

	@Override
	public Date getReceivedTime() {
		return new Date(0);
	}

	@Override
	public boolean isNew() {
		return false;
	}

	@Override
	public String getFromAddress() {
		return "";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new IssueMailItemBlank();
	}
}
