package com.wilutions.itol;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.MailItem;

public class MailInspector extends InspectorWrapper {

	private final IssueTaskPane issuePane;
	private final IssueHistoryTaskPane historyPane;

	public MailInspector(Inspector inspector, IDispatch currentItem) throws ComException {
		super(inspector, currentItem);

		MailItem mailItem = currentItem.as(MailItem.class);

		issuePane = new IssueTaskPane(this, mailItem);
		historyPane = new IssueHistoryTaskPane(this, mailItem);

	}
	
	public String getIssueId() {
		return issuePane.getIssueId();
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
