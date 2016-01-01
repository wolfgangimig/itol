package com.wilutions.fx.acpl;

import javafx.scene.control.ComboBox;

public class AutoCompletionComboBox<T> extends ComboBox<T> {

	private AutoCompletionBinding<T> binding;

	public AutoCompletionComboBox() {
	}

	public AutoCompletionBinding<T> getBinding() {
		return binding;
	}

	public void setBinding(AutoCompletionBinding<T> binding) {
		this.binding = binding;
	}

}
