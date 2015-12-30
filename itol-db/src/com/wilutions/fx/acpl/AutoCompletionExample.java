package com.wilutions.fx.acpl;

import java.util.ArrayList;

import com.wilutions.itol.db.IdName;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AutoCompletionExample extends Application {
	
	AutoCompletionBinding<IdName> autoCompletionProject;

	@Override
	public void start(Stage stage) {
		VBox vbox = new VBox();

		Image image1 = new Image("file:///c:/Users/Wolfgang/Pictures/icon1.png");
		
		ComboBox<IdName> control1 = new ComboBox<IdName>();
		ArrayList<IdName> recentItems = new ArrayList<IdName>();
		ArrayList<IdName> allItems = new ArrayList<IdName>();
		autoCompletionProject = AutoCompletions.bindAutoCompletion(
				(idn) -> idn.getImage(), 
				control1, "recent items", "suggested items", recentItems, 
				allItems);
		for (String s : Names.LIST) {
			int id = s.hashCode();
			Image img = ((id & 1) != 0) ? image1 : null;
			allItems.add(new IdName(Integer.toString(id), s, img));
		}

		ComboBox<IdName> control2 = new ComboBox<IdName>();
		control2.getItems().addAll(new IdName(0, "ABC"), new IdName(1, "DEF"));

		ImageView iv1 = makeImageView(image1);
		iv1.setCursor(Cursor.DEFAULT);

		Button bnOK = new Button("OK");

		vbox.getChildren().addAll(control1, control2, bnOK);

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