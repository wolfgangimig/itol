package com.wilutions.itol;

import java.io.IOException;

import com.wilutions.com.CoClass;
import com.wilutions.com.ComException;
import com.wilutions.com.IDispatch;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.DeclAddin;
import com.wilutions.joa.LoadBehavior;
import com.wilutions.joa.OfficeApplication;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.joa.outlook.ex.OutlookAddinEx;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.outlook.Inspector;
import com.wilutions.mslib.outlook.OlObjectClass;

@CoClass(progId = "ItolAddin.Class", guid = "{6da82554-8fea-4395-bd73-33f823a6dc24}")
@DeclAddin(application = OfficeApplication.Outlook, loadBehavior = LoadBehavior.LoadOnStart, friendlyName = "JOA Issue Tracking", description = "Outlook Addin for Issual Tracking")
public class ItolAddin extends OutlookAddinEx {

	private AttachmentHttpServer httpServer = new AttachmentHttpServer();
	private BackstageConfig backstageConfig = new BackstageConfig();
	private IRibbonUI ribbon;

	public ItolAddin() {
		Globals.setThisAddin(this);
		httpServer.start();
	}

	public void onLoadRibbon(IRibbonUI ribbon) {
		this.ribbon = ribbon;
		this.backstageConfig.onLoadRibbon(ribbon);
	}
	
	public String EditBox_getText(IRibbonControl control) {
		String ret = "";
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			ret = backstageConfig.EditBox_getText(control);
		}
		System.out.println("EditBox_getText id=" + control.getId() + ", text=" + ret);
		return ret;
	}

	public void EditBox_onChange(IRibbonControl control, String text) {
		System.out.println("EditBox_onChange id=" + control.getId() + ", text=" + text);
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			backstageConfig.EditBox_onChange(control, text);
		}
	}

	public void Button_onAction(IRibbonControl control) {
		String controlId = control.getId();
		if (controlId.startsWith(BackstageConfig.CONTROL_ID_PREFIX)) {
			backstageConfig.Button_onAction(control);
		}
	}
	
	@Override
	public String GetCustomUI(String ribbonId) {
		String ui = super.GetCustomUI(ribbonId);
		if (!ribbonId.equals("Microsoft.Outlook.Explorer")) {
			return ui;
		}
		try {
			ui = backstageConfig.getCustomUI(ui);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ui;
	}
	
	public void NewIssue_onAction(IRibbonControl control) {
		IDispatch dispContext = control.getContext();
		Inspector inspector = dispContext.as(Inspector.class);
		MailInspector mailInspector = (MailInspector) getInspectorWrapper(inspector);
		mailInspector.setIssueTaskPaneVisible(true);
	}

	public void IssueHistory_onAction(IRibbonControl control) {
		IDispatch dispContext = control.getContext();
		Inspector inspector = dispContext.as(Inspector.class);
		MailInspector mailInspector = (MailInspector) getInspectorWrapper(inspector);
		mailInspector.setHistoryTaskPaneVisible(true);
	}

	protected InspectorWrapper createInspectorWrapper(Inspector inspector, OlObjectClass olclass) {
		switch (olclass.value) {
		case OlObjectClass._olMail:
			return new MailInspector(inspector, inspector.getCurrentItem());
		default:
			return super.createInspectorWrapper(inspector, olclass);
		}
	}

	@Override
	public void onQuit() throws ComException {
		super.onQuit();
		httpServer.done();
	}

	public AttachmentHttpServer getHttpServer() {
		return httpServer;
	}
	
	public String test() {
		return "test";
	}
}
