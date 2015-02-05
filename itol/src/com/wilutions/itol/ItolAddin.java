/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;

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
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlObjectClass;
import com.wilutions.mslib.outlook.Selection;

@CoClass(progId = "ItolAddin.Class", guid = "{013ebe9e-fbb4-4ccf-857b-ab716f7273c1}")
@DeclAddin(application = OfficeApplication.Outlook, loadBehavior = LoadBehavior.LoadOnStart, friendlyName = "Issue Tracker Addin", description = "Issue Tracker Addin for Microsoft Outlook")
public class ItolAddin extends OutlookAddinEx {

	private AttachmentHttpServer httpServer = new AttachmentHttpServer();
	private BackstageConfig backstageConfig = new BackstageConfig();

	public ItolAddin() {
		Globals.setThisAddin(this);
		// httpServer.start();
	}

	public void onLoadRibbon(IRibbonUI ribbon) {
		super.onLoadRibbon(ribbon);
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
		} else if (controlId.equals("NewIssue")) {
			newIssue(control);
		} else if (controlId.equals("ShowIssue")) {
			showIssue(control);
		}

	}

	private void newIssue(IRibbonControl control) {
		IDispatch dispContext = control.getContext();
		try {
			if (dispContext.is(Inspector.class)) {
				Inspector inspector = dispContext.as(Inspector.class);
				MailInspector mailInspector = (MailInspector) getInspectorWrapper(inspector);
				mailInspector.setIssueTaskPaneVisible(true);
			}
			else if (dispContext.is(Explorer.class)) {
				Explorer explorer = dispContext.as(Explorer.class);
//				MyExplorerWrapper explorerWrapper = (MyExplorerWrapper)getExplorerWrapper(explorer);
//				if (explorerWrapper == null) {  // Might be null, if IssueServiceImpl was not available on startup.
//					onNewExplorer(explorer);
//					explorerWrapper = (MyExplorerWrapper)getExplorerWrapper(explorer);
//				}
//				explorerWrapper.setIssueTaskPaneVisible(true);
				
				DlgNewIssue dlg = new DlgNewIssue(null, new IssueMailItemBlank());
				dlg.show(explorer);
			}
			
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void showIssue(IRibbonControl control) {
		IDispatch dispContext = control.getContext();
		Inspector inspector = null;
		Explorer explorer = null;
		String issueId = "";
		try {
			if (dispContext.is(Inspector.class)) {
				inspector = dispContext.as(Inspector.class);
				MailInspector mailInspector = (MailInspector) getInspectorWrapper(inspector);
				issueId = mailInspector.getIssueId();
			}
			else if (dispContext.is(Explorer.class)) {
				explorer = dispContext.as(Explorer.class);
				issueId = getIssueIdOfSelectedMail(explorer);
			}

			System.out.println("issueId=" + issueId);
			if (issueId != null && issueId.length() != 0) {
				String issueUrl = Globals.getIssueService().getShowIssueUrl(issueId);
				if (issueUrl != null && issueUrl.length() != 0) {
					IssueApplication.showDocument(issueUrl);
				} else {
					throw new IllegalStateException("Implementation provided no URL for issue ID=" + issueId);
				}
			}
			
		} catch (Throwable e) {
			MessageBox.show(inspector, "Error", "Cannot show issue " + e, null);
		}
	}

	public boolean Button_getEnabled(IRibbonControl control) {
		return true;
	}

	public boolean Button_getVisible(IRibbonControl control) {
		String controlId = control.getId();
		boolean ret = true;
		try {

			if (controlId.equals("NewIssue") || controlId.equals("ShowIssue") || controlId.equals("grpIssue")) {
				IDispatch dispContext = control.getContext();
				
				// Is button placed in the ribbon of mail inspector? 
				if (dispContext.is(Inspector.class)) {
					Inspector inspector = dispContext.as(Inspector.class);
					MailInspector mailInspector = (MailInspector) getInspectorWrapper(inspector);
					if (mailInspector != null) {
						
						// Enable ShowIssue button, if mail subject contains an issue ID.
						if (controlId.equals("ShowIssue")) {
							String issueId = mailInspector.getIssueId();
							ret = issueId != null && issueId.length() != 0;
						}
					}
				}
				// Is button placed in the ribbon of the explorer window?
				else if (dispContext.is(Explorer.class)) {
	
					Explorer explorer = dispContext.as(Explorer.class);
	
					// Enable ShowIssue button, if mail subject contains an issue ID.
					if (controlId.equals("ShowIssue")) {
						String issueId = getIssueIdOfSelectedMail(explorer);
						ret = issueId != null && issueId.length() != 0;
					}
				}
			}
		
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		return ret;
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

	public String Button_getLabel(IRibbonControl control) {
		String resId = "";
		String controlId = control.getId();
		switch (controlId) {
		case "NewIssue":
			resId = "Ribbon.NewIssue";
			break;
		case "ShowIssue":
			resId = "Ribbon.ShowIssue";
			break;
		default:
		}
		return Globals.getResourceBundle().getString(resId);
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

	@Override
	protected InspectorWrapper createInspectorWrapper(Inspector inspector, OlObjectClass olclass) {
		switch (olclass.value) {
		case OlObjectClass._olMail:
			try {
				getRibbon().Invalidate();
				return new MailInspector(inspector, inspector.getCurrentItem());
			} catch (Throwable e) {
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
	}

	public AttachmentHttpServer getHttpServer() {
		return httpServer;
	}

	public String test() {
		return "test";
	}

	public void onIssueCreated(MailInspector mailInspector) {
		mailInspector.setIssueTaskPaneVisible(false);
		getRibbon().InvalidateControl("NewIssue");
		getRibbon().InvalidateControl("ShowIssue");
		// ribbon.InvalidateControl("grpIssue");
	}

	/**
	 * Return the issue ID of the selected mail.
	 * @param explorer Explorer object
	 * @return issue ID or empty string.
	 */
	private String getIssueIdOfSelectedMail(Explorer explorer) {
		String issueId = "";
		MailItem mailItem = getSelectedMail(explorer);
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

	/**
	 * Return the first selected mail item.
	 * @param explorer Explorer object
	 * @return MailItem object or null.
	 */
	private MailItem getSelectedMail(Explorer explorer) {
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
		}
		catch (ComException ignored) {
			// explorer.getSelection() causes a HRESULT=0x80020009 when 
			// Outlook starts.
		}
		return mailItem;
	}
}
