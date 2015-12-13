package com.wilutions.itol;

import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.mslib.outlook.MailItem;

public interface MyWrapper extends Wrapper {
	public boolean isIssueTaskPaneVisible();
	public void setIssueTaskPaneVisible(boolean v);
	public IssueMailItem getSelectedItem();
}
