package com.wilutions.itol;

import java.io.IOException;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.Selection;

public class MyExplorerWrapper extends ExplorerWrapper {

	final IssueTaskPane issuePane;

	public MyExplorerWrapper(Explorer explorer) {
		super(explorer);
		issuePane = new IssueTaskPane(null, new IssueMailItemBlank());
	}

	@Override
	public void onSelectionChange() throws ComException {
		// Update visibility of ShowIssue button
		//Globals.getThisAddin().getRibbon().InvalidateControl("ShowIssue");
		IRibbonUI ribbon = Globals.getThisAddin().getRibbon(); 
		ribbon.InvalidateControl("ShowIssue");
		
		showSelectedMailItem();
	}
	
	@Override
	public void onClose() throws ComException {
		if (issuePane != null) {
			issuePane.close();
		}
		super.onClose();
	}

	/**
	 * Return the first selected mail item.
	 * @param explorer Explorer object
	 * @return MailItem object or null.
	 */
	public MailItem getSelectedMail() {
		MailItem mailItem = null;
		try {
			Selection selection = explorer.getSelection();
			int nbOfSelectedItems = selection.getCount();
			if (nbOfSelectedItems != 0) {
				IDispatch selectedItem = selection.Item(1);
				if (selectedItem.is(MailItem.class)) {
					mailItem = selectedItem.as(MailItem.class);
				}
			}
		}
		catch (ComException ignored) {
			// explorer.getSelection() causes a HRESULT=0x80020009 when 
			// Outlook starts.
		}
		return mailItem;
	}
	
	/**
	 * Return the issue ID of the selected mail.
	 * @return issue ID or empty string.
	 */
	public String getIssueIdOfSelectedMail() {
		String issueId = "";
		MailItem mailItem = getSelectedMail();
		if (mailItem != null) {
			String subject = mailItem.getSubject();
			try {
				issueId = Globals.getIssueService().extractIssueIdFromMailSubject(subject);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return issueId;
	}


	public void setIssueTaskPaneVisible(boolean visible) {
		
		if (!issuePane.hasWindow() && visible) {
			Globals.getThisAddin().createTaskPaneWindowAsync(issuePane, "Issue", explorer, (succ, ex) -> {
				if (ex != null) {
					MessageBox.show(explorer, "Error", ex.getMessage(), null);
				}
				else {
					showSelectedMailItem();
				}
			});
		}
		
		issuePane.setVisible(visible, (succ, ex) -> {
			if (succ && visible) {
				showSelectedMailItem();
			}
		});
	}
	
	public boolean isIssueTaskPaneVisible() {
		return issuePane.hasWindow() && issuePane.isVisible();
	}

	public void showSelectedMailItem() {
		if (issuePane.hasWindow() && issuePane.isVisible()) {
			MailItem mailItem = getSelectedMail();
			if (mailItem != null) {
				issuePane.setMailItem(new IssueMailItemImpl(mailItem));
			}
		}
	}


}
