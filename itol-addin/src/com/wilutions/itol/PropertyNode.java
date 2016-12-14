package com.wilutions.itol;

import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.PropertyClass;

import javafx.scene.Node;

/**
 * Item in PropertyGridView
 *
 */
public abstract class PropertyNode {
	
	private Issue issue;
	private PropertyClass pclass;
	private Node node;

	public PropertyNode(Issue issue, PropertyClass pclass, Node node) {
		this.issue = issue;
		this.pclass = pclass;
		this.node = node;
	}

	public abstract void updateData(boolean save);
	
	public Issue getIssue() {
		return issue;
	}

	public PropertyClass getPclass() {
		return pclass;
	}

	public Node getNode() {
		return node;
	}

	public String getPropertyId() {
		return pclass.getId();
	}


}
