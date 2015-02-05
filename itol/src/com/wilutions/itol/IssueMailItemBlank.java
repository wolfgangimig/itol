package com.wilutions.itol;

import com.wilutions.com.ComException;
import com.wilutions.mslib.outlook.Attachment;

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

}
