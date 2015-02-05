package com.wilutions.itol;

import com.wilutions.com.ComException;

public interface IssueMailItem {

	public String getSubject() throws ComException;

	public void setSubject(String mailSubject) throws ComException;

	public String getBody() throws ComException;

	public void Save() throws ComException;

	public void SaveAs(final String Path, final Object Type) throws ComException;
	
	public IssueAttachments getAttachments() throws ComException;

}
