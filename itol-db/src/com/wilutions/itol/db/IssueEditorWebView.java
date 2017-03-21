package com.wilutions.itol.db;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class IssueEditorWebView implements IssuePropertyEditor {
	
	private Logger log = Logger.getLogger("IssueHtmlEditorWebView");
	
	private String htmlContent;
	
	private String elementId;
	
	private WebView webView = new WebView();

	public String getHtmlContent() {
		return htmlContent;
	}

	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}

	public String getElementId() {
		return elementId;
	}

	public void setElementId(String elementId) {
		this.elementId = elementId;
	}
	
	@SuppressWarnings("unused")
	private JSObject getTextarea() {
		JSObject elm = null;
		String elementId = getElementId();
		String scriptToGetDescription = "document.getElementById('" + elementId + "')";
		try {
			elm = (JSObject) webView.getEngine().executeScript(scriptToGetDescription);
		}
		catch (Throwable e) {
			log.log(Level.WARNING, "", e);
		}
		return elm;
	}

	@Override
	public Node getNode() {
		return webView;
	}

	@Override
	public void setFocus() {
		webView.requestFocus();
		try {
			Platform.runLater(() -> {
				String elementId = getElementId();
				String scriptToFocusControl = "document.getElementById('" + elementId + "').focus()";
				try {
					webView.getEngine().executeScript(scriptToFocusControl);
				}
				catch (Throwable e) {
					log.log(Level.WARNING, "", e);
				}
			});
		}
		catch (Exception e) {
		}
	}

	@Override
	public void updateData(boolean save) {
		
	}

	@Override
	public void setIssue(Issue issue) {
	}

}
