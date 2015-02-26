/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.util.Callback;

import com.wilutions.com.CoClass;
import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.joa.DeclAddin;
import com.wilutions.joa.LoadBehavior;
import com.wilutions.joa.OfficeApplication;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.outlook.ex.ExplorerWrapper;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Explorer;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.OlObjectClass;

@CoClass(progId = "ItolAddin.Class", guid = "{013ebe9e-fbb4-4ccf-857b-ab716f7273c1}")
@DeclAddin(application = OfficeApplication.Outlook, loadBehavior = LoadBehavior.LoadOnStart, friendlyName = "Issue Tracker Addin", description = "Issue Tracker Addin for Microsoft Outlook")
public class ItolAddin extends OutlookAddinEx {

	private AttachmentHttpServer httpServer = new AttachmentHttpServer();
	private BackstageConfig backstageConfig = new BackstageConfig();
	private Logger log = Logger.getLogger("ItolAddin");

	public ItolAddin() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "ItolAddin(");
		Globals.setThisAddin(this);
		// httpServer.start();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")ItolAddin");
	}

	public void onLoadRibbon(IRibbonUI ribbon) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "onLoadRibbon(");
		super.onLoadRibbon(ribbon);
		this.backstageConfig.onLoadRibbon(ribbon);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")onLoadRibbon");
	}

	public String EditBox_getText(IRibbonControl control) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "EditBox_getText(");
		String ret = "";
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			ret = backstageConfig.EditBox_getText(control);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")EditBox_getText=" + ret);
		return ret;
	}

	public void EditBox_onChange(IRibbonControl control, String text) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "EditBox_onChange(" + text);
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			backstageConfig.EditBox_onChange(control, text);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")ditBox_onChange");
	}

	public void Button_onAction(IRibbonControl control, Boolean pressed) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Button_onAction(" + pressed);
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			backstageConfig.Button_onAction(control);
		}
		else if (controlId.equals("NewIssue")) {
			newIssue(control, pressed);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")Button_onAction");
	}

	private void newIssue(IRibbonControl control, Boolean pressed) {
		forContextWrapper(control, (context) -> {
			if (Globals.isIssueServiceRunning()) {
				context.setIssueTaskPaneVisible(pressed);
			}
			else if (pressed) {
				ResourceBundle resb = Globals.getResourceBundle();
				Object owner = context.getWrappedObject();
				MessageBox.show(owner, resb.getString("MessageBox.title.error"), resb.getString("Error.NotConnected"), null);
			}
			return true;
		});
	}

	public boolean Button_getEnabled(IRibbonControl control) {
		forContextWrapper(control, null);
		return true;
	}

	public boolean Button_getVisible(IRibbonControl control) {
		forContextWrapper(control, null);
		return true;
	}

	public boolean Button_getPressed(IRibbonControl control) {
		Boolean ret = forContextWrapper(control, (wrapper) -> {
			return wrapper.isIssueTaskPaneVisible();
		});
		return ret != null ? ret : false;
	}

	public String Button_getLabel(IRibbonControl control) {
		String text = forContextWrapper(control, (context) -> {
			String localResId = "";
			String controlId = control.getId();
			switch (controlId) {
			case "NewIssue":
				localResId = "Ribbon.NewIssue";
				break;
			default:
			}
			String str = "";
			if (!localResId.isEmpty()) {
				str = Globals.getResourceBundle().getString(localResId);
			}
			return str;
		});
		return text;
	}

	public String ComboBox_getText(IRibbonControl control) {
		String ret = "";
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			ret = backstageConfig.ComboBox_getText(control);
		}
		System.out.println("ComboBox_getText id=" + control.getId() + ", text=" + ret);
		return ret;
	}

	public int ComboBox_getItemCount(IRibbonControl control) {
		int ret = 0;
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			ret = backstageConfig.ComboBox_getItemCount(control);
		}
		System.out.println("ComboBox_getItemCount id=" + control.getId() + ", ret=" + ret);
		return ret;
	}

	public String ComboBox_getItemLabel(IRibbonControl control, Integer idx) {
		String ret = "";
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			ret = backstageConfig.ComboBox_getItemLabel(control, idx);
		}
		System.out.println("ComboBox_getItemLabel id=" + control.getId() + ", idx=" + idx + ", ret=" + ret);
		return ret;
	}

	public void ComboBox_onChange(IRibbonControl control, String text) {
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			backstageConfig.ComboBox_onChange(control, text);
		}
		System.out.println("ComboBox_onChange id=" + control.getId() + ", text=" + text);
	}

	@Override
	public String GetCustomUI(String ribbonId) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "GetCustomUI(" + ribbonId);
		String ui = super.GetCustomUI(ribbonId);
		if (!ribbonId.equals("Microsoft.Outlook.Explorer")) {
			return ui;
		}
		try {
			ui = backstageConfig.getCustomUI(ui);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")GetCustomUI=" + ui);
		return ui;
	}

	@Override
	protected InspectorWrapper createInspectorWrapper(Inspector inspector, OlObjectClass olclass) {
		switch (olclass.value) {
		case OlObjectClass._olMail:
			try {
				IRibbonUI ribbon = getRibbon();
				if (ribbon != null) ribbon.Invalidate();
				return new MailInspector(inspector, inspector.getCurrentItem());
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
		default:
			return super.createInspectorWrapper(inspector, olclass);
		}
	}

	@Override
	protected ExplorerWrapper createExplorerWrapper(Explorer explorer) {
		return new MyExplorerWrapper(explorer);
	}

	@Override
	public void onQuit() throws ComException {
		super.onQuit();
		httpServer.done();
		Globals.releaseResources();
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
		MyExplorerWrapper explorerWrapper = (MyExplorerWrapper) super.getExplorerWrapper(explorer);
		if (explorerWrapper == null) { // Might be null, if IssueServiceImpl was
										// not available on startup.
			onNewExplorer(explorer);
			explorerWrapper = (MyExplorerWrapper) super.getExplorerWrapper(explorer);
		}
		return explorerWrapper;
	}

	private <T> T forContextWrapper(IRibbonControl control, Callback<MyWrapper, T> call) {
		T ret = null;
		MyWrapper wrapper = null;

		IDispatch dispContext = control.getContext();
		if (dispContext.is(Inspector.class)) {
			Inspector inspector = dispContext.as(Inspector.class);
			wrapper = (MyWrapper) getInspectorWrapper(inspector);
		}
		else if (dispContext.is(Explorer.class)) {
			Explorer explorer = dispContext.as(Explorer.class);
			wrapper = (MyWrapper) getMyExplorerWrapper(explorer);
		}

		if (wrapper != null) {
			wrapper.addRibbonControl(control);
			if (call != null) {
				ret = call.call(wrapper);
			}
		}

		return ret;
	}

}
