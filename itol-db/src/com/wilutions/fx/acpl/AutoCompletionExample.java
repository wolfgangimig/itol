package com.wilutions.fx.acpl;

import org.controlsfx.control.textfield.CustomTextField;

import com.wilutions.itol.db.IdName;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AutoCompletionExample extends Application {

	@Override
	public void start(Stage stage) {
		VBox vbox = new VBox();

		AutoCompletionField<IdName> control1 = new AutoCompletionField<IdName>();

		AutoCompletionField<IdName> control2 = new AutoCompletionField<IdName>();

		Image image1 = new Image("file:///c:/Users/Wolfgang/Pictures/icon1.png");
		ImageView iv1 = makeImageView(image1);
		iv1.setCursor(Cursor.DEFAULT);

		CustomTextField customTextField1 = new CustomTextField();
		customTextField1.setRight(iv1);

		Button bnOK = new Button("OK");

		vbox.getChildren().addAll(control1, control2, customTextField1, bnOK);

		Scene scene = new Scene(vbox, 300, 150);
		scene.getStylesheets().add(getClass().getResource("AutoCompletionExample.css").toExternalForm());

		stage.setScene(scene);
		stage.setTitle("Sample");

		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

	private static ImageView makeImageView(Image image) {
		ImageView imageView = new ImageView();
		imageView.setImage(image);
		imageView.setFitWidth(16);
		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);
		imageView.setCache(true);
		return imageView;
	}

}