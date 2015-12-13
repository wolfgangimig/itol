package com.wilutions.itol;

import com.wilutions.com.ComException;
import com.wilutions.mslib.outlook.Attachment;

public interface IssueAttachments {
	
	public int getCount() throws ComException;
	
	Attachment getItem(int i) throws ComException;
	
}
