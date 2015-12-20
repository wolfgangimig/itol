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
import com.wilutions.itol.db.Property;
import com.wilutions.joa.DeclAddin;
import com.wilutions.joa.LoadBehavior;
import com.wilutions.joa.OfficeApplication;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.joa.ribbon.RibbonButton;
import com.wilutions.joa.ribbon.RibbonControls;
import com.wilutions.joa.ribbon.RibbonToggleButton;
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
	private ResourceBundle resb;

	RibbonButton bnNewIssue = new RibbonButton();
	RibbonButton bnConnect = new RibbonButton();
	RibbonToggleButton bnMsg = new RibbonToggleButton();
	RibbonToggleButton bnMhtml = new RibbonToggleButton();
	RibbonToggleButton bnRtf = new RibbonToggleButton();

	public ItolAddin() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "ItolAddin(");
		Globals.setThisAddin(this);
		resb = Globals.getResourceBundle();
		initRibbonControls();

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")ItolAddin");
	}

	private void initRibbonControls() {

		RibbonControls ribbonControls = getRibbonControls();
		String grpName = resb.getString("Ribbon.grpIssue");
		ribbonControls.group("grpIssue", grpName);

		bnNewIssue = getRibbonControls().button("bnNewIssue", resb.getString("Ribbon.NewIssue"));
		bnNewIssue.setImage("Alert-icon-32.png");
		bnNewIssue.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			newIssue(control, context, pressed);
		});

		///////////////////////
		// Configure

		getRibbonControls().group("grpConnect", resb.getString("Ribbon.Connect"));

		// Connect

		bnConnect = getRibbonControls().button("bnConnect", resb.getString("Ribbon.bnConnect"));
		bnConnect.setImage("Connect32.png");
		bnConnect.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			onConnect(control, context, null);
		});

		// Attach Mail as

		getRibbonControls().group("grpAttachMailAs", resb.getString("Ribbon.AttachMailAs"));

		Property msgFileType = Globals.getConfigProperty(Property.MSG_FILE_TYPE);
		if (msgFileType == null) {
			msgFileType = new Property(".msg", "Outlook (.msg)");
		}

		bnMsg = getRibbonControls().toggleButton("bnMsg", "Outlook (.msg)");
		bnMsg.setImage("File.msg");
		bnMsg.setPressed(msgFileType.getId().equals(".msg"));
		bnMsg.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			onAddMailFormatChanged(control, context, pressed);
		});

		bnMhtml = getRibbonControls().toggleButton("bnMhtml", "MIME HTML (.mhtml)");
		bnMhtml.setImage("File.mhtml");
		bnMhtml.setPressed(msgFileType.getId().equals(".mhtml"));
		bnMhtml.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			onAddMailFormatChanged(control, context, pressed);
		});

		bnRtf = getRibbonControls().toggleButton("bnRtf", "Rich Text Format (.rtf)");
		bnRtf.setImage("File.rtf");
		bnRtf.setPressed(msgFileType.getId().equals(".rtf"));
		bnRtf.setOnAction((IRibbonControl control, Wrapper context, Boolean pressed) -> {
			onAddMailFormatChanged(control, context, pressed);
		});
	}

	private void newIssue(IRibbonControl control, Wrapper context, Boolean pressed) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "newIssue(");

		if (Globals.isIssueServiceRunning()) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "setIssueTaskPaneVisible(" + pressed + ")");
			((MyWrapper) context).setIssueTaskPaneVisible(pressed);
		}
		else if (pressed) {
			onConnect(control, context, (succ, ex) -> {
				if (ex != null) {
					ResourceBundle resb = Globals.getResourceBundle();
					Object owner = context.getWrappedObject();
					String msg = resb.getString("Error.NotConnected");
					log.log(Level.SEVERE, msg, ex);
					msg += "\n" + ex.getMessage();
					MessageBox.error(owner, msg, null);
				}
				else if (succ != null && succ) {
					((MyWrapper) context).setIssueTaskPaneVisible(pressed);
				}
			});
		}
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")newIssue");
	}

	public void onConnect(IRibbonControl control, Wrapper context, AsyncResult<Boolean> asyncResult) {
		DlgConnect dlg = new DlgConnect();
		Object owner = context.getWrappedObject();
		dlg.showAsync(owner, asyncResult);
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

	private void onAddMailFormatChanged(IRibbonControl control, Wrapper context, Boolean pressed) {
		// if (pressed != null && pressed)
		{
			String controlId = control.getId();
			RibbonButton[] cbs = new RibbonButton[] { bnMsg, bnMhtml, bnRtf };
			for (int i = 0; i < cbs.length; i++) {
				boolean p = cbs[i].getId().equals(controlId);
				cbs[i].setPressed(p);
				if (!p) {
					getRibbon().InvalidateControl(cbs[i].getId());
				}
			}
		}
	}

}
