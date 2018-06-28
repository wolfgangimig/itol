/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.util.ResourceBundle;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.joa.ribbon.RibbonButton;
import com.wilutions.joa.ribbon.RibbonGroup;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.MailItem;

public class MailInspector extends InspectorWrapper implements MyWrapper {

	private IssueTaskPane issuePane;
	private ResourceBundle resb = Globals.getResourceBundle();

	public MailInspector(Inspector inspector, IDispatch currentItem) throws ComException, IOException {
		super(inspector, currentItem);

		initRibbonControls();
	}

	private void initRibbonControls() {
		
		@SuppressWarnings("unused")
		RibbonGroup grpIssue = getRibbonControls().group("grpIssue", resb.getString("Ribbon.grpIssue"));

		RibbonButton bnNewIssue = getRibbonControls().button("bnNewIssue", resb.getString("Ribbon.NewIssue"));
		bnNewIssue.setImage("IssueTaskPaneIcon-1a.png");
		bnNewIssue.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			ItolAddin addin = (ItolAddin) Globals.getThisAddin();
			addin.showIssuePane(control, context, pressed);
		});
	}

	@Override
	public IssueMailItem getSelectedItem() {
		return new IssueMailItemImpl(currentItem.as(MailItem.class), this);
	}

	public void setIssueTaskPaneVisible(boolean visible) {
		if (issuePane == null) {
			issuePane = new IssueTaskPane(this);
		}
		issuePane.createOrShowAsync(inspector, visible, (succ, ex) -> {
			if (succ && visible) {
				
			}
		});
	}

	public boolean isIssueTaskPaneVisible() {
		return issuePane != null && issuePane.hasWindow() && issuePane.isVisible();
	}

	@Override
	public void onClose() throws ComException {
		if (issuePane != null) {
			
			// Always save visible state as false. 
			// Feels to be better UX not to show the task pane when a mail inspector is displayed.
			issuePane.setVisible(false); 
			
			issuePane.close();
		}
		super.onClose();
	}

}
