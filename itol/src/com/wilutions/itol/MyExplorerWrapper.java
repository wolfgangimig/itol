package com.wilutions.itol;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.Selection;

public class MyExplorerWrapper extends ExplorerWrapper implements MyWrapper {

	final IssueTaskPane issuePane;
	String lastEntryID = "";
	private long showAtMillis = Long.MAX_VALUE;
	private final static long SHOW_DELAY_MILLIS = 500;
	private final Timeline deferShowSelectedItem;

	public MyExplorerWrapper(Explorer explorer) {
		super(explorer);
		issuePane = new IssueTaskPane(null, new IssueMailItemBlank());

		deferShowSelectedItem = new Timeline(new KeyFrame(Duration.seconds(0.1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (isSelectionDelayOver()) {
					showSelectedMailItem(false);
				}
			}
		}));

		deferShowSelectedItem.setCycleCount(Timeline.INDEFINITE);
		deferShowSelectedItem.play();
	}

	public void addRibbonControl(IRibbonControl control) {
		ribbonControls.put(control.getId(), control);
	}

	private synchronized boolean isSelectionDelayOver() {
		boolean ret = showAtMillis <= System.currentTimeMillis();
		if (ret) {
			showAtMillis = Long.MAX_VALUE;
		}
		return ret;
	}

	private synchronized void startSelectionDelay() {
		showAtMillis = System.currentTimeMillis() + SHOW_DELAY_MILLIS;
	}

	@Override
	public void onSelectionChange() throws ComException {
		// Update visibility of ShowIssue button
		// Globals.getThisAddin().getRibbon().InvalidateControl("ShowIssue");
		IRibbonUI ribbon = Globals.getThisAddin().getRibbon();
		if (ribbon != null) {
			internalShowSelectedMailItem();
		}
	}

	@Override
	public void onClose() throws ComException {

		if (deferShowSelectedItem != null) {
			deferShowSelectedItem.stop();
		}

		if (issuePane != null) {
			issuePane.close();
		}
		
		ribbonControls.clear();
		super.onClose();
	}

	/**
	 * Return the first selected mail item.
	 * 
	 * @param explorer
	 *            Explorer object
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
		} catch (ComException ignored) {
			// explorer.getSelection() causes a HRESULT=0x80020009 when
			// Outlook starts.
		}
		return mailItem;
	}

	/**
	 * Return the issue ID of the selected mail.
	 * 
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
		if (issuePane != null) {
			if (!issuePane.hasWindow() && visible) {
				String title = Globals.getResourceBundle().getString("IssueTaskpane.title");
				Globals.getThisAddin().createTaskPaneWindowAsync(issuePane, title, explorer, (succ, ex) -> {
					if (ex != null) {
						MessageBox.show(explorer, "Error", ex.getMessage(), null);
					} else {
						internalShowSelectedMailItem();
					}
				});
			}

			issuePane.setVisible(visible, (succ, ex) -> {
				if (succ && visible) {
					internalShowSelectedMailItem();
				}
			});
		}
	}

	public boolean isIssueTaskPaneVisible() {
		return issuePane != null && issuePane.hasWindow() && issuePane.isVisible();
	}

	private void internalShowSelectedMailItem() {
		startSelectionDelay();
	}

	public void showSelectedMailItem(boolean ignoreLastItem) {
		if (issuePane != null) {
			if (issuePane.hasWindow() && issuePane.isVisible()) {
				MailItem mailItem = getSelectedMail();
				if (mailItem != null) {
					String mailId = mailItem.getEntryID();
					if (ignoreLastItem || !mailId.equals(lastEntryID)) {
						issuePane.setMailItem(new IssueMailItemImpl(mailItem));
						lastEntryID = mailId;
					}
				}
			}
		}
	}

}
