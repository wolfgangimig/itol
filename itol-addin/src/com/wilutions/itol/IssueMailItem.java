package com.wilutions.itol;

import java.util.Date;

import com.wilutions.com.ComException;
import com.wilutions.mslib.outlook.OlBodyFormat;

public interface IssueMailItem {

	public String getSubject() throws ComException;

	public void setSubject(String mailSubject) throws ComException;

	public String getBody() ;
	public String getHTMLBody() ;
	public OlBodyFormat getBodyFormat();
	
	public String getFrom();
	public String getFromAddress();
	public String getTo();

	public void Save() throws ComException;

	public void SaveAs(final String Path, final Object Type) throws ComException;
	
	public IssueAttachments getAttachments() throws ComException;

	public Date getReceivedTime();
	
	public boolean isNew();

}
