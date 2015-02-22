/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.PropertyClasses;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.mslib.office.IRibbonControl;
import com.wilutions.mslib.office.IRibbonUI;

public class BackstageConfig {

	public final static String CONTROL_ID_PREFIX = "BackstageConfig_";
	private List<Property> configProps;
	private IRibbonUI ribbon;

	public BackstageConfig() {
	}

	public String getCustomUI(String customUITemplate) throws IOException {

		init();

		StringBuilder sbuf = new StringBuilder();

		sbuf.append("<layoutContainer id=\"").append(CONTROL_ID_PREFIX).append("vert1\" layoutChildren=\"vertical\" >");

		for (Property configProp : configProps) {
			appendField(sbuf, configProp);
		}

		sbuf.append("</layoutContainer>");

		// Save and Cancel buttons
		sbuf.append("<layoutContainer id=\"").append(CONTROL_ID_PREFIX)
				.append("horz1\" layoutChildren=\"horizontal\" >");
		sbuf.append("<button id=\"").append(CONTROL_ID_PREFIX)
				.append("bnSave\" label=\"Save\" onAction=\"Button_onAction\" />");
		sbuf.append("<button id=\"").append(CONTROL_ID_PREFIX)
				.append("bnCancel\" label=\"Cancel\" onAction=\"Button_onAction\" />");
		sbuf.append("</layoutContainer>");
		
		String version = Globals.getVersion();
		String ui = MessageFormat.format(customUITemplate, sbuf.toString(), version);

		return ui;
	}

	/**
	 * Deep copy property list.
	 * 
	 * @throws IOException
	 */
	private void init() throws IOException {
		List<Property> props = Globals.getIssueService().getConfig();
		this.configProps = new ArrayList<Property>();
		for (Property prop : props) {
			this.configProps.add(new Property(prop));
		}
		
		// Set default logging options if nessesary
		Property propLogFile = Globals.getConfigProperty(Property.LOG_FILE);
		this.configProps.add(propLogFile);
		Property propLogLevel = Globals.getConfigProperty(Property.LOG_LEVEL);
		this.configProps.add(propLogLevel);
	}

	private void appendField(StringBuilder sbuf, Property configProp) throws IOException {

		IssueService srv = Globals.getIssueService();
		PropertyClasses propertyClasses = srv.getPropertyClasses();
		PropertyClass propClass = propertyClasses.get(configProp.getId());
		if (propClass == null)
			throw new IllegalStateException("Undefined property class=" + configProp.getId());
		List<IdName> selectList = propClass.getSelectList();

		String elm = "editBox";
		switch (propClass.getType()) {
		case PropertyClass.TYPE_BOOL:
			elm = "checkBox";
			break;
		case PropertyClass.TYPE_PASSWORD:
			elm = "button";
			break;
		default:
			if (selectList != null && selectList.size() != 0) {
				elm = "comboBox";
			}
		}

		sbuf.append("<").append(elm).append(" ");
		sbuf.append("id=\"").append(CONTROL_ID_PREFIX).append(configProp.getId()).append("\" ");
		sbuf.append("label=\"").append(propClass.getName()).append("\" ");

		switch (propClass.getType()) {
		case PropertyClass.TYPE_BOOL:
			break;
		case PropertyClass.TYPE_PASSWORD:
			sbuf.append("imageMso=\"SignatureShow\" ");
			sbuf.append("onAction=\"Button_onAction\" ");
			break;
		case PropertyClass.TYPE_STRING:
//		case PropertyClass.TYPE_INTEGER:
			if (selectList != null && selectList.size() != 0) {
				sbuf.append("getItemCount=\"ComboBox_getItemCount\" ");
				sbuf.append("getItemLabel=\"ComboBox_getItemLabel\" ");
				sbuf.append("onChange=\"ComboBox_onChange\" ");
				sbuf.append("getText=\"ComboBox_getText\" ");
			} else {
				sbuf.append("sizeString=\"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\" ");
				sbuf.append("onChange=\"EditBox_onChange\" ");
				sbuf.append("getText=\"EditBox_getText\" ");
			}
			break;
		}

		sbuf.append("/>");
	}

	public int ComboBox_getItemCount(IRibbonControl control) {
		List<IdName> selectList = getPropertySelectList(control);
		int ret = selectList != null ? selectList.size() : 0;
		return ret;
	}

	public String ComboBox_getItemLabel(IRibbonControl control, int idx) {
		List<IdName> selectList = getPropertySelectList(control);
		String ret = selectList != null && idx < selectList.size() ? selectList.get(idx).getName() : "";
		return ret;
	}

	public void ComboBox_onChange(IRibbonControl control, String text) {
		Property configProp = getPropertyForControl(control);
		if (configProp != null) {
			List<IdName> selectList = getPropertySelectList(control);
			if (selectList != null) {
				for (IdName idn : selectList) {
					if (idn.getName().equals(text)) {
						configProp.setValue(idn.getId());
						break;
					}
				}
			}
		}
	}

	public String ComboBox_getText(IRibbonControl control) {
		String ret = "";
		String propId = getPropertyIdFromControl(control);
		Property configProp = getPropertyById(propId);
		if (configProp != null) {
			List<IdName> selectList = getPropertySelectList(control);
			if (selectList != null) {
				for (IdName idn : selectList) {
					if (configProp.getValue().equals(idn.getId())) {
						ret = idn.getName();
						break;
					}
				}
			}
		}
		return ret;
	}

	private List<IdName> getPropertySelectList(IRibbonControl control) {
		List<IdName> selectList = null;
		try {
			Property configProp = getPropertyForControl(control);
			if (configProp != null) {
				IssueService srv = Globals.getIssueService();
				PropertyClasses propertyClasses = srv.getPropertyClasses();
				PropertyClass propClass = propertyClasses.get(configProp.getId());
				selectList = propClass.getSelectList();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return selectList;
	}

	public void EditBox_onChange(IRibbonControl control, String text) {
		Property configProp = getPropertyForControl(control);
		if (configProp != null) {
			configProp.setValue(text);
		}
	}

	public String EditBox_getText(IRibbonControl control) {
		String ret = "";
		String propId = getPropertyIdFromControl(control);
		Property configProp = getPropertyById(propId);
		if (configProp != null) {
			ret = (String) configProp.getValue();
		}
		return ret;
	}

	public void Button_onAction(IRibbonControl control) {
		try {
			String propId = getPropertyIdFromControl(control);
			switch (propId) {
			case "bnSave":
				onSave();
				break;
			case "bnCancel":
				onCancel();
				break;
			default:
				onPropertyControlAction(control);
				break;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (msg == null || msg.length() == 0) {
				msg = e.toString();
			}
			MessageBox.show(getOwnerWindow(), "Error", msg, null);
		}
	}

	private void onCancel() throws IOException {
		init();
		this.ribbon.Invalidate();
	}

	private void onSave() throws IOException {
		Globals.setConfig(configProps);
		IdName currentUser = Globals.getIssueService().getCurrentUser();
		MessageBox.show(getOwnerWindow(), "OK", "Configuration saved and connection re-initialized. "
				+ "You are logged in as " + currentUser.getName() + ".", null);
	}

	private void onPropertyControlAction(IRibbonControl control) throws IOException {
		final Property configProp = getPropertyForControl(control);
		PropertyClass propClass = Globals.getIssueService().getPropertyClasses().get(configProp.getId());

		if (propClass.getType() == PropertyClass.TYPE_PASSWORD) {
			DlgPassword dlg = new DlgPassword();
			dlg.showAsync(getOwnerWindow(), (succ, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				} else {
					configProp.setValue(succ);
				}
			});
		}
	}

	private Property getPropertyForControl(IRibbonControl control) {
		String propId = getPropertyIdFromControl(control);
		Property configProp = getPropertyById(propId);
		return configProp;
	}

	private String getPropertyIdFromControl(IRibbonControl control) {
		String propId = control.getId();
		if (propId.startsWith(CONTROL_ID_PREFIX)) {
			propId = propId.substring(CONTROL_ID_PREFIX.length());
		}
		return propId;
	}

	private Object getOwnerWindow() {
		return Globals.getThisAddin().getApplication().ActiveExplorer();
	}

	private Property getPropertyById(String propId) {
		for (Property configProp : configProps) {
			if (configProp.getId().equals(propId)) {
				return configProp;
			}
		}
		return null;
	}

	public void onLoadRibbon(IRibbonUI ribbon) {
		this.ribbon = ribbon;
	}
}
