package com.wilutions.itol;

import javafx.stage.Stage;

public class DlgTestIssueTaskPane {
	
	
	
	private static class MyIssueTaskPane extends IssueTaskPane {
		Stage dlg;
		
		public MyIssueTaskPane(Stage dlg, MailInspector mailInspectorOrNull, IssueMailItem mailItem) {
			super(mailInspectorOrNull, mailItem);
			this.dlg = dlg;
		}
		
		protected Object getDialogOwner() {
			return dlg;
		}

	}

	public static void showAndWait() {
		Stage dlg = new Stage();
		IssueMailItem mitem = new IssueMailItemBlank();
		dlg.setScene(new MyIssueTaskPane(dlg, null, mitem).createScene());
		dlg.showAndWait();
	}

}
