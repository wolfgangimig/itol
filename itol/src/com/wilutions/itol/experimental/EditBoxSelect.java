package com.wilutions.itol.experimental;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class EditBoxSelect extends Application {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("AutoFillTextBox without FilterMode");

		// SAMPLE DATA
		ObservableList<String> data = FXCollections.observableArrayList();
		String[] s = new String[] { "apple", "ball", "cat", "doll", "elephant", "fight", "georgeous", "height", "ice",
				"jug", "aplogize", "bank", "call", "done", "ego", "finger", "giant", "hollow", "internet", "jumbo",
				"kilo", "lion", "for", "length", "primary", "stage", "scene", "zoo", "jumble", "auto", "text", "root",
				"box", "items", "hip-hop", "himalaya", "nepal", "kathmandu", "kirtipur", "everest", "buddha", "epic",
				"hotel" };

		for (int j = 0; j < s.length; j++) {
			data.add(s[j]);
		}

		// Layout
		VBox vbox = new VBox();
		vbox.setSpacing(10);
		
		// CustomControl
//		final ComboBox<String> box = new ComboBox<String>(data);
//		TextBoxFilterList.autoCompleteComboBox(box, TextBoxFilterList.AutoCompleteMode.CONTAINING);
//
//		vbox.getChildren().addAll(box);

		TextField textField = new TextField();
		final ComboBox<String> textField1 = new ComboBox<String>();
		TextFieldAutoComplete tfa = new TextFieldAutoComplete();
		tfa.init(textField);
		
		vbox.getChildren().addAll(textField, new ComboBox<String>());
		Scene scene = new Scene(vbox, 300, 200);

		primaryStage.setScene(scene);
		primaryStage.show();

	}
}
