/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.MailItem;

public class MailInspector extends InspectorWrapper {

	private final IssueTaskPane issuePane;
	private final IssueHistoryTaskPane_off historyPane;

	public MailInspector(Inspector inspector, IDispatch currentItem) throws ComException, IOException {
		super(inspector, currentItem);

		IssueMailItem mailItem = new IssueMailItemImpl(currentItem.as(MailItem.class));

		issuePane = new IssueTaskPane(this, mailItem);
		historyPane = new IssueHistoryTaskPane_off(this, mailItem);

	}
	
	public String getIssueId() throws ComException, IOException {
		IssueMailItem mailItem = issuePane.getMailItem();
		return Globals.getIssueService().extractIssueIdFromMailSubject(mailItem.getSubject());
	}

	public void setIssueTaskPaneVisible(boolean visible) {
		if (!issuePane.hasWindow() && visible) {
			Globals.getThisAddin().createTaskPaneWindowAsync(issuePane, "Issue", inspector, (succ, ex) -> {
				if (ex != null) {
					MessageBox.show(inspector, "Error", ex.getMessage(), null);
				}
			});
		}
		issuePane.setVisible(visible);
	}

	public void setHistoryTaskPaneVisible(boolean visible) {
		if (!historyPane.hasWindow() && visible) {
			Globals.getThisAddin().createTaskPaneWindowAsync(historyPane, "Issue History", inspector, null);
		}
		historyPane.setVisible(visible);
	}

	@Override
	public void onClose() throws ComException {
		if (issuePane != null) {
			issuePane.close();
		}
		if (historyPane != null) {
			historyPane.close();
		}
		super.onClose();
	}

}
