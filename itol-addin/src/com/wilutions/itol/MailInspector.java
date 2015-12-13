/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.MailItem;

public class MailInspector extends InspectorWrapper implements MyWrapper {

	private final IssueTaskPane issuePane;
	private Map<String, IRibbonControl> ribbonControls = new HashMap<String, IRibbonControl>();

	public MailInspector(Inspector inspector, IDispatch currentItem) throws ComException, IOException {
		super(inspector, currentItem);

		issuePane = new IssueTaskPane(this);
	}
	
	@Override
	public IssueMailItem getSelectedItem() {
		return new IssueMailItemImpl(currentItem.as(MailItem.class));
	}
	
	public void addRibbonControl(IRibbonControl control) {
		ribbonControls.put(control.getId(), control);
	}
	
	public String getIssueId() throws ComException, IOException {
		IssueMailItem mailItem = issuePane.getMailItem();
		return Globals.getIssueService().extractIssueIdFromMailSubject(mailItem.getSubject());
	}

	public void setIssueTaskPaneVisible(boolean visible) {
		if (!issuePane.hasWindow() && visible) {
			String title = Globals.getResourceBundle().getString("IssueTaskPane.title");
			Globals.getThisAddin().createTaskPaneWindowAsync(issuePane, title, inspector, (succ, ex) -> {
				if (ex != null) {
					MessageBox.show(inspector, "Error", ex.getMessage(), null);
				}
			});
		}
		issuePane.setVisible(visible);
	}

	public boolean isIssueTaskPaneVisible() {
		return issuePane.hasWindow() && issuePane.isVisible();
	}

	@Override
	public void onClose() throws ComException {
		if (issuePane != null) {
			issuePane.close();
		}
		ribbonControls.clear();
		super.onClose();
	}

}
