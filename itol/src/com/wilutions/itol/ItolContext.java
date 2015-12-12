package com.wilutions.itol;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import com.wilutions.com.reg.Registry;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;

public interface ItolContext {
	public IssueService getIssueService();
	public ResourceBundle getResourceBundle();
	public Registry getRegistry();
	public File getTempDir();
	public ItolAddin getThisAddin();
	public String getVersion();
	public Property getConfigProperty(String s);
	public void setConfig(List<Property> configProps);
}
