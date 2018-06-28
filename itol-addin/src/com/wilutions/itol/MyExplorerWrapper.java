package com.wilutions.itol;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.joa.ribbon.RibbonButton;
import com.wilutions.joa.ribbon.RibbonGroup;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.Selection;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public class MyExplorerWrapper extends ExplorerWrapper implements MyWrapper {

	private IssueTaskPane issuePane;
	Object lastEntryID = "";
	private long showAtMillis = Long.MAX_VALUE;
	private final static long SHOW_DELAY_MILLIS = 500;
	private final Timeline deferShowSelectedItem;
	private final static Logger log = Logger.getLogger("MyExplorerWrapper");
	private ResourceBundle resb = Globals.getResourceBundle();

	public MyExplorerWrapper(Explorer explorer) {
		super(explorer);

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "MyExplorerWrapper(");

		deferShowSelectedItem = new Timeline(new KeyFrame(Duration.seconds(0.3), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (isSelectionDelayOver()) {
					showSelectedItem();
				}
			}
		}));

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "deferShowSelectedItem.play");
		deferShowSelectedItem.setCycleCount(Timeline.INDEFINITE);
		deferShowSelectedItem.play();

		initRibbonControls();

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")MyExplorerWrapper");
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
		IssueMailItem ret = new IssueMailItemBlank();
		IDispatch disp = getSelectedExplorerItem();
		if (disp != null) {
			MailItem mailItem = disp.as(MailItem.class);
			ret = new IssueMailItemImpl(mailItem, this);
		}
		return ret;
	}

	public void addRibbonControlDispatchReference(IRibbonControl control) {
		ribbonControlsDispatchReferences.put(control.getId(), control);
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
			internalShowSelectedItem();
		}
	}

	@Override
	public void onClose() throws ComException {

		if (deferShowSelectedItem != null) {
			deferShowSelectedItem.stop();
		}

		if (issuePane != null) {
			issuePane.setVisible(false);
			issuePane.close();
		}

		ribbonControlsDispatchReferences.clear();
		super.onClose();
	}

	/**
	 * Return the first selected mail item.
	 * 
	 * @param explorer
	 *            Explorer object
	 * @return MailItem object or null.
	 */
	private IDispatch getSelectedExplorerItem() {
		IDispatch ret = null;
		try {
			Selection selection = explorer.getSelection();
			int nbOfSelectedItems = selection.getCount();
			if (nbOfSelectedItems != 0) {
				ret = selection.Item(1);
			}
		}
		catch (ComException ignored) {
			// explorer.getSelection() causes a HRESULT=0x80020009 when
			// Outlook starts.
		}
		return ret;
	}

	public void setIssueTaskPaneVisible(boolean visible) {
		if (issuePane == null) {
			issuePane = new IssueTaskPane(this);
		}
		issuePane.createOrShowAsync(explorer, visible, (succ, ex) -> {
			if (succ && visible) {
				internalShowSelectedItem();
			}
		});
	}

	public boolean isIssueTaskPaneVisible() {
		return issuePane != null && issuePane.hasWindow() && issuePane.isVisible();
	}

	private void internalShowSelectedItem() {
		startSelectionDelay();
	}

	public void showSelectedItem() {
		if (issuePane != null) {
			BackgTask.run(() -> {
				if (issuePane.hasWindow() && issuePane.isVisible()) {
					IDispatch mailItem = getSelectedExplorerItem();
					if (mailItem != null) {
						Object mailId = mailItem._get("EntryID");
						if (!mailId.equals(lastEntryID)) {
							IssueMailItemImpl mailItemImpl = new IssueMailItemImpl(mailItem.as(MailItem.class), this);
							lastEntryID = mailId;
							Platform.runLater(() -> {
								issuePane.setMailItem(mailItemImpl);
							});
						}
					}
				}
			});
		}
	}

}
