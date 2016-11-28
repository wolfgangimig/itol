package com.wilutions.itol.db;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

public class IssueHtmlEditorTextArea implements IssueHtmlEditor {
	
	private TextArea textArea = new TextArea();
	
	public IssueHtmlEditorTextArea() {
		textArea.setMaxHeight(Double.MAX_VALUE);
	}

	@Override
	public String getText() {
		return textArea.getText();
	}

	@Override
	public void setText(String text) {
		textArea.setText(text);
	}

	@Override
	public Node getNode() {
		return textArea;
	}

	@Override
	public void setFocus() {
		textArea.requestFocus();
	}

	@Override
	public void updateData(boolean save) {
		
	}
}
