/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.ProgressCallbackImpl;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class DlgProgress extends ModalDialogFX<Boolean> implements Initializable {

	//private static Logger log = Logger.getLogger("DlgProgress");
	private volatile ProgressCallback progressCallback;
	private volatile boolean cancelled = false;

	private class MyProgressCallback extends ProgressCallbackImpl {

		private int lastPercent = 0;

		public MyProgressCallback(String name) {
			super(name);
		}

		public void setProgress(final double current) {
			super.setProgress(current);
			double currentSum = childSum + current;
			final double quote = currentSum / total;
			int percent = (int) Math.ceil(100.0 * quote);
			if (percent > lastPercent) {
				lastPercent = percent;
			}

			Platform.runLater(() -> {
				pgProgressBar.setProgress(quote);
			});
		}

		@Override
		public void setParams(String... params) {
			super.setParams(params);
			Platform.runLater(() -> {
				if (params != null && params.length != 0) {
					StringBuilder sbuf = new StringBuilder();
					for (int i = 0; i < params.length; i++) {
						if (i != 0) {
							sbuf.append(", ");
						}
						sbuf.append(params[i]);
					}
					lbProgress.setText(sbuf.toString());
				}
			});
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}
		
	}

	public DlgProgress(String title) {
		setTitle(title);
		setResult(Boolean.TRUE);
	}

	@Override
	public Scene createScene() {
		try {
			Parent p = load();
			Scene scene = new Scene(p);
			return scene;

		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	public Parent load() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgProgress.fxml");

		FXMLLoader loader = new FXMLLoader(fxmlURL, Globals.getResourceBundle(), new JavaFXBuilderFactory(),
				(clazz) -> {
					return this;
				});
		Parent p = loader.load();
		return p;
	}

	public void setButtonOK() {
		Platform.runLater(() -> {
			bnOK.setVisible(true);
			bnOK.setManaged(true);
			bnCancel.setVisible(false);
			bnCancel.setManaged(false);
		});
	}
	
	@Override
	public void finish(Boolean result) {
		if (progressCallback != null) {
			progressCallback.setFinished();
		}
		super.finish(result);
	}

	@FXML
	public void onCancel() {
		cancelled = true;
		finish(false);
	}

	@FXML
	public void onOK() {
		finish(true);
	}

	public ProgressCallback getProgressCallback() {
		return progressCallback;
	}

	public ProgressCallback startProgress(String name, long total) {
		this.progressCallback = new MyProgressCallback(name);
		this.progressCallback.setTotal(total);
		return progressCallback;
	}

	@FXML
	private Button bnCancel;
	@FXML
	private Button bnOK;
	@FXML
	private Label lbProgress;
	@FXML
	private ProgressBar pgProgressBar;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		bnOK.setVisible(false);
		bnOK.setManaged(false);
	}

}
