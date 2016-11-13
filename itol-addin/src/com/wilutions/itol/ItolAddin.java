/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.CoClass;
import com.wilutions.com.ComException;
import com.wilutions.joa.DeclAddin;
import com.wilutions.joa.LoadBehavior;
import com.wilutions.joa.OfficeApplication;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.OlObjectClass;

@CoClass(progId = "ItolAddin.Class", guid = "{013ebe9e-fbb4-4ccf-857b-ab716f7273c1}")
@DeclAddin(application = OfficeApplication.Outlook, loadBehavior = LoadBehavior.LoadOnStart, friendlyName = "Issue Tracker Addin", description = "Issue Tracker Addin for Microsoft Outlook")
public class ItolAddin extends OutlookAddinEx {

	private AttachmentHttpServer httpServer = new AttachmentHttpServer();
	private Logger log = Logger.getLogger("ItolAddin");

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

		if (Globals.isIssueServiceRunning()) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "setIssueTaskPaneVisible(" + pressed + ")");
			((MyWrapper) context).setIssueTaskPaneVisible(pressed);
		}
		else if (pressed) {
			Object owner = context.getWrappedObject(); 
			internalConnect(owner, (succ, ex) -> {
				if (succ) {
					((MyWrapper) context).setIssueTaskPaneVisible(pressed);
				}
			});
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")showIssuePane");
	}

	
	protected void internalConnect(Object owner, AsyncResult<Boolean> asyncResult) {
		onConnect(owner, (succ, ex) -> {
			if (ex != null) {
				ResourceBundle resb = Globals.getResourceBundle();
				String msg = resb.getString("Error.NotConnected");
				log.log(Level.SEVERE, msg, ex);
				msg += "\n" + ex.getMessage();
				MessageBox.error(owner, msg, null);
			}
			if (asyncResult != null) {
				asyncResult.setAsyncResult(succ != null && succ && ex == null, null);
			}
		});
	}

	protected void onConnect(Object owner, AsyncResult<Boolean> asyncResult) {
		DlgConnect dlg = new DlgConnect();
		dlg.showAsync(owner, asyncResult);
	}

	protected void internalConfigure(Wrapper context, AsyncResult<Boolean> asyncResult) {
		onConfigure(context, (succ, ex) -> {
			if (ex != null) {
				Object owner = context.getWrappedObject();
				log.log(Level.SEVERE, "Configuration failed", ex);
				MessageBox.error(owner, ex.getMessage(), null);
			}
			if (asyncResult != null) {
				asyncResult.setAsyncResult(succ != null && succ && ex == null, null);
			}
		});
	}
	
	protected void onConfigure(Wrapper context, AsyncResult<Boolean> asyncResult) {
		DlgConfigure dlg = new DlgConfigure();
		dlg.showAsync(context.getWrappedObject(), asyncResult);

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
		httpServer.done();
		Globals.releaseResources();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")onQuit");
	}

	public AttachmentHttpServer getHttpServer() {
		return httpServer;
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
