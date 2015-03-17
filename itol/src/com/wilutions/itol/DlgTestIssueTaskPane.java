package com.wilutions.itol;

import com.wilutions.com.IDispatch;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.outlook.MailItem;

import javafx.stage.Stage;

public class DlgTestIssueTaskPane {
	
	
	
	private static class MyIssueTaskPane extends IssueTaskPane {
		Stage dlg;
		
		public MyIssueTaskPane(Stage dlg, MyWrapper wrapper) {
			super(wrapper);
			this.dlg = dlg;
		}
		
		protected Object getDialogOwner() {
			return dlg;
		}

	}

	public static void showAndWait() {
		Stage dlg = new Stage();
		
		MyWrapper myWrapper = new MyWrapper() {

			public IDispatch getWrappedObject() {
				return null;
			}

			@Override
			public void addRibbonControl(IRibbonControl control) {
			}

			@Override
			public boolean isIssueTaskPaneVisible() {
				return false;
			}

			@Override
			public void setIssueTaskPaneVisible(boolean v) {
			}

			@Override
			public IssueMailItem getSelectedItem() {
				return new IssueMailItemBlank();
			}
			
		};
		dlg.setScene(new MyIssueTaskPane(dlg, myWrapper).createScene());
		dlg.showAndWait();
	}

}
