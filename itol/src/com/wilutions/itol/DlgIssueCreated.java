package com.wilutions.itol;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.Label;

public class DlgIssueCreated {

	@FXML
	private Label lbCreatedMessage;

	public Parent load() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgIssueCreated.fxml");

		FXMLLoader loader = new FXMLLoader(fxmlURL, Globals.getResourceBundle(), new JavaFXBuilderFactory(),
				(clazz) -> {
					return this;
				});
		Parent p = loader.load();
		return p;
	}

	@FXML
	protected void onGotoIssue() {
	}

	@FXML
	protected void onContinue() {
	}

	@FXML
	protected void onClosePanel() {
	}

}
