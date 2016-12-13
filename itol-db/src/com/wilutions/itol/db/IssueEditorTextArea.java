package com.wilutions.itol.db;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

public class IssueEditorTextArea implements IssuePropertyEditor {
	
	private TextArea textArea = new TextArea();
	
	public IssueEditorTextArea() {
		textArea.setMaxHeight(Double.MAX_VALUE);
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

	@Override
	public void setIssue(Issue issue) {
	}
}
