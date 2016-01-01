/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.joa.ribbon.RibbonButton;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.MailItem;

public class MailInspector extends InspectorWrapper implements MyWrapper {

	private final IssueTaskPane issuePane;
	private ResourceBundle resb = Globals.getResourceBundle();
	
	public MailInspector(Inspector inspector, IDispatch currentItem) throws ComException, IOException {
		super(inspector, currentItem);

		issuePane = new IssueTaskPane(this);
		
		initRibbonControls();
	}
	
	private void initRibbonControls() {

		RibbonButton bnNewIssue = new RibbonButton();
		bnNewIssue = getRibbonControls().button("bnNewIssue", resb.getString("Ribbon.NewIssue"));
		bnNewIssue.setImage("Alert-icon-32.png");
		bnNewIssue.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			ItolAddin addin = (ItolAddin)Globals.getThisAddin();
			addin.showIssuePane(control, context, pressed);
		});
	}
	
	@Override
	public IssueMailItem getSelectedItem() {
		return new IssueMailItemImpl(currentItem.as(MailItem.class));
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
		super.onClose();
	}

}
