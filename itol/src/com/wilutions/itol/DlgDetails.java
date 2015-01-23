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
import javafx.scene.layout.GridPane;

import com.wilutions.joa.fx.ModalDialogFX;

public class DlgDetails extends ModalDialogFX<Boolean> implements Initializable {
	
	@FXML
	GridPane rootGrid;
	@FXML
	Button bnOK;
	@FXML
	Button bnCancel;

	public DlgDetails(String url) {
		super.setTitle("Details");
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgDetails.fxml");

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
		
	}
	
	@FXML
	public void onCancel() {
		
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
	}
}
