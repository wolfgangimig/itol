package com.wilutions.fx.acpl;

import javafx.scene.Node;

interface AutoCompletionControl<T> {
	public void select(T item);
	public T getSelectedItem();
	public Node getNode();
	public void setEditable(boolean en);
	public boolean isEditable();
	public String getEditText();
}
