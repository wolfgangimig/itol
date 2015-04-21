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
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private final List<Property> configProps = new ArrayList<Property>();
	private IRibbonUI ribbon;
	private Logger log = Logger.getLogger("BackstageConfig");

	public BackstageConfig() {
	}

	public String getCustomUI(String customUITemplate) throws IOException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getCustomUI(" + customUITemplate);
		String version = Globals.getVersion();
		StringBuilder propsUI = new StringBuilder();
		ResourceBundle resb = Globals.getResourceBundle();

		if (init()) {

			propsUI.append("<layoutContainer id=\"").append(CONTROL_ID_PREFIX)
					.append("vert1\" layoutChildren=\"vertical\" >");

			for (Property configProp : configProps) {
				appendField(propsUI, configProp);
			}

			propsUI.append("</layoutContainer>");

			final String saveText = resb.getString("bnSave.text");
			final String cancelText = resb.getString("bnCancel.text");

			// Save and Cancel buttons
			propsUI.append("<layoutContainer id=\"").append(CONTROL_ID_PREFIX)
					.append("horz1\" layoutChildren=\"horizontal\" >");
			propsUI.append("<button id=\"").append(CONTROL_ID_PREFIX).append("bnSave\" label=\"").append(saveText)
					.append("\" onAction=\"Button_onAction\" />");
			propsUI.append("<button id=\"").append(CONTROL_ID_PREFIX).append("bnCancel\" label=\"").append(cancelText)
					.append("\" onAction=\"Button_onAction\" />");
			propsUI.append("</layoutContainer>");

		}

		String ui = MessageFormat.format(customUITemplate, propsUI.toString(), version);

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")getCustomUI");
		return ui;
	}

	/**
	 * Deep copy property list.
	 * 
	 * @throws IOException
	 */
	private boolean init() {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "init(");
		boolean ret = false;
		this.configProps.clear();

		try {
			List<Property> props = Globals.getIssueService().getConfig();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "props=" + props);

			for (Property prop : props) {
				this.configProps.add(new Property(prop));
			}

			// Add properties for logging options
			Property propLogFile = Globals.getConfigProperty(Property.LOG_FILE);
			this.configProps.add(propLogFile);
			Property propLogLevel = Globals.getConfigProperty(Property.LOG_LEVEL);
			this.configProps.add(propLogLevel);

			ret = true;
		}
		catch (IOException e) {
			// IssueService might not be ready at the moment.
			log.log(Level.INFO, "Cannot get configuration from issue service.", e);
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")init=" + ret);
		return ret;
	}

	private void appendField(StringBuilder sbuf, Property configProp) throws IOException {

		IssueService srv = Globals.getIssueService();
		PropertyClasses propertyClasses = srv.getPropertyClasses();
		PropertyClass propClass = propertyClasses.get(configProp.getId());
		if (propClass == null) throw new IllegalStateException("Undefined property class=" + configProp.getId());
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
			// case PropertyClass.TYPE_INTEGER:
			if (selectList != null && selectList.size() != 0) {
				sbuf.append("getItemCount=\"ComboBox_getItemCount\" ");
				sbuf.append("getItemLabel=\"ComboBox_getItemLabel\" ");
				sbuf.append("onChange=\"ComboBox_onChange\" ");
				sbuf.append("getText=\"ComboBox_getText\" ");
			}
			else {
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
		}
		catch (IOException e) {
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
			case "bnReload":
				onReload();
				break;
			default:
				onPropertyControlAction(control);
				break;
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (msg == null || msg.length() == 0) {
				msg = e.toString();
			}
			MessageBox.show(getOwnerWindow(), "Error", msg, null);
		}
	}

	private void onReload() {
		init();
		this.ribbon.Invalidate();
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
				}
				else {
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
