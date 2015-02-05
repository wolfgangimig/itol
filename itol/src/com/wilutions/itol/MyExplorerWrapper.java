package com.wilutions.itol;

import java.io.IOException;

import com.wilutions.com.ComException;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;

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
	}
	
	public void setIssueTaskPaneVisible(boolean visible) {
		if (!issuePane.hasWindow() && visible) {
			Globals.getThisAddin().createTaskPaneWindowAsync(issuePane, "Issue", explorer, (succ, ex) -> {
				if (ex != null) {
					MessageBox.show(explorer, "Error", ex.getMessage(), null);
				}
			});
		}
		issuePane.setVisible(visible);
	}


}
