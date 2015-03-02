/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import com.wilutions.joa.fx.ModalDialogFX;

public class DlgPassword extends ModalDialogFX<String> implements Initializable{
	
	ResourceBundle resb;
	String pwd;
	
	public DlgPassword(String pwd) {
		resb = Globals.getResourceBundle();
		this.pwd = pwd;
		setTitle(resb.getString("DlgPassword.title"));
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgPassword.fxml");

			FXMLLoader loader = new FXMLLoader(fxmlURL, new PropertyResourceBundle(
					new ByteArrayInputStream(new byte[0])), new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			});
			Parent p = loader.load();

			Scene scene = new Scene(p);
			return scene;

		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	@FXML
	public void onOK() {
		setResult(edPassword.getText());
		this.close();
	}

	@FXML
	public void onCancel() {
		this.close();
	}

	@FXML
	private Button bnOK;
	@FXML
	private Button bnCancel;
	@FXML
	private TextField edPassword;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (pwd != null) {
			edPassword.setText(pwd);
		}
	}
}
