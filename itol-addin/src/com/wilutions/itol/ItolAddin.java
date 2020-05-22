/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.BackgTask;
import com.wilutions.com.CoClass;
import com.wilutions.com.ComException;
import com.wilutions.itol.db.Default;
import com.wilutions.joa.DeclAddin;
import com.wilutions.joa.LoadBehavior;
import com.wilutions.joa.OfficeApplication;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.joa.ribbon.RibbonButton;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.OlObjectClass;

@CoClass(progId = "ItolAddin.Class", guid = "{013ebe9e-fbb4-4ccf-857b-ab716f7273c1}")
@DeclAddin(application = OfficeApplication.Outlook, loadBehavior = LoadBehavior.LoadOnStart, friendlyName = "Issue Tracker Addin", description = "Issue Tracker Addin for Microsoft Outlook")
public class ItolAddin extends OutlookAddinEx {

	private Logger log = Logger.getLogger("ItolAddin");
	private ResourceBundle resb = Globals.getResourceBundle();

	public ItolAddin() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "ItolAddin(");
		Globals.setThisAddin(this);

		getIconManager().addPackageAsResourceDirectory(ItolAddin.class);
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")ItolAddin");
	}

	/**
	 * Show issue pane.
	 * This function is invoked from MyExplorerWrapper and MailInspector.
	 * @param control
	 * @param context
	 * @param pressed
	 */
	public void showIssuePane(IRibbonControl control, Wrapper context, Boolean pressed) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "showIssuePane(");
		
		// This CompletableFuture is completed, if the connections to all issue services are checked.   
		CompletableFuture<Boolean> allIssueServicesConnected = Globals.getAllIssueServicesConnected();
		Object msgboxOwner = context.getWrappedObject();
		
		// Task pane should be shown?
		boolean showTaskPane = Default.value(pressed);
		if (showTaskPane) {
			
			// Wrap the function that shows the task pane into an AsyncResult.
			// So I do not have to duplicate this lines.
			AsyncResult<Integer> showTaskPaneResult = (btn, ex) -> {
				BackgTask.run(() -> {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "setIssueTaskPaneVisible(" + pressed + ")");
					
					// Exporer or Inspector wrapper.
					MyWrapper wrapper = ((MyWrapper) context);
					
					// Success, if not "cancel" clicked. 
					boolean succ = Default.value(btn) != 0;

					// Show or hide task pane
					wrapper.setIssueTaskPaneVisible(succ);
					
					// Update ribbon button state.
					RibbonButton bnNewIssue = (RibbonButton)wrapper.getRibbonControls().get("bnNewIssue");
					bnNewIssue.setPressed(succ);
					getRibbon().InvalidateControl("bnNewIssue");
				});
			};
			
			// Already all connections checked?
			if (allIssueServicesConnected.isDone()) {
				com.wilutions.joa.fx.MessageBox.Builder msgb = null;
				
				try {
					// CF is true, if all connections succeeded.
					// CF is false, if at least one connection is failed.
					boolean oneFailed = !allIssueServicesConnected.get();
					if (oneFailed) {
						
						// Hack: to avoid that a message box is shown every time
						// the button is pressed, just set all completed.
						allIssueServicesConnected.complete(true);
						
						// Show message box: "... one connection has failed ..."
						String title = resb.getString("MessageBox.title.info");
						String ok = resb.getString("Button.OK");
						String text = resb.getString("msg.connection.oneFailed");
						msgb = com.wilutions.joa.fx.MessageBox.create(msgboxOwner).title(title).text(text).button(1, ok).bdefault();
					}
					
				}
				catch (Throwable ex) {
					
					log.log(Level.SEVERE, "Failed to show task pane.", ex);
					
					// CF completed exceptionally: none of the connections could be established.
					String title = resb.getString("MessageBox.title.error");
					String ok = resb.getString("Button.OK");
					String text = resb.getString("msg.connection.allFailed");
					msgb = com.wilutions.joa.fx.MessageBox.create(msgboxOwner).title(title).text(text).button(1, ok).bdefault();
				}
				
				// Show message box with info or error as defined above.
				// Or show task pane immediately.
				if (msgb != null) {
					msgb.show(showTaskPaneResult);
				}
				// Empty configuration?
				else if (Globals.getAppInfo().getConfig().getProfiles().isEmpty()) {
					
					// Show profiles dialog. 
					DlgProfiles dlg = new DlgProfiles();
					dlg.showAsync(msgboxOwner, (config, ex) -> {
						
						// Configuration still empty?
						boolean empty = Globals.getAppInfo().getConfig().getProfiles().isEmpty();
						showTaskPaneResult.setAsyncResult(empty ? 0 : 1, ex);
					});
				}
				else {
					showTaskPaneResult.setAsyncResult(1, null);
				}
			}
			else {
				
				// Not all connections have been checked at this time.
				// The user should wait.
				
				String title = resb.getString("MessageBox.title.confirm");
				String yes = resb.getString("Button.Yes");
				String no = resb.getString("Button.No");
				String text = resb.getString("msg.connection.stillConnecting");
				com.wilutions.joa.fx.MessageBox.create(msgboxOwner).title(title).text(text).button(1, yes).button(0, no)
					.bdefault().show((btn, ex) -> {
						boolean succ = btn != null && btn != 0;
						if (succ) {
							// User wants to check connection status again.
							// Recursive call.
							showIssuePane(control, context, pressed);
						}
						else {
							// User wants to continue -> show task pane. 
							showTaskPaneResult.setAsyncResult(1, null);
						}
				});
			}
		}
		else {
			// Hide task pane.
			((MyWrapper) context).setIssueTaskPaneVisible(false);
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")showIssuePane");
	}

	@Override
	public String GetCustomUI(String ribbonId) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "GetCustomUI(" + ribbonId);
		String ui = super.GetCustomUI(ribbonId);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")GetCustomUI=" + ui);
		return ui;
	}

	@Override
	protected InspectorWrapper createInspectorWrapper(Inspector inspector, OlObjectClass olclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "createInspectorWrapper(" + olclass);
		InspectorWrapper ret = null;
		switch (olclass.value) {
		case OlObjectClass._olMail:
		case OlObjectClass._olPost:
			try {
				IRibbonUI ribbon = getRibbon();
				if (ribbon != null) ribbon.Invalidate();
				ret = new MailInspector(inspector, inspector.getCurrentItem());
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, "Failed to create inspector wrapper object.", e);
			}
			break;
		default:
			ret = super.createInspectorWrapper(inspector, olclass);
			break;
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")createInspectorWrapper=" + ret);
		return ret;
	}

	@Override
	protected ExplorerWrapper createExplorerWrapper(Explorer explorer) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "createExplorerWrapper(");
		MyExplorerWrapper v = new MyExplorerWrapper(explorer);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")createExplorerWrapper=" + v);
		return v;
	}

	@Override
	public void onQuit() throws ComException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "onQuit(");
		super.onQuit();
		Globals.releaseResources();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")onQuit");
	}

	public String test() {
		return "test";
	}

	public void onIssueCreated(MailInspector mailInspector) {
	}

	public MyExplorerWrapper getMyExplorerWrapper(Explorer explorer) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getMyExplorerWrapper(");
		MyExplorerWrapper explorerWrapper = (MyExplorerWrapper) super.getExplorerWrapper(explorer);
		if (explorerWrapper == null) { // Might be null, if IssueServiceImpl was
										// not available on startup.
			onNewExplorer(explorer);
			explorerWrapper = (MyExplorerWrapper) super.getExplorerWrapper(explorer);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")getMyExplorerWrapper=" + explorerWrapper);
		return explorerWrapper;
	}

}
