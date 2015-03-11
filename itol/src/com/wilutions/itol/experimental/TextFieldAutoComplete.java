package com.wilutions.itol.experimental;

import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Window;

public class TextFieldAutoComplete {

	final Popup popup = new Popup();
	final ListView<String> listView = new ListView<String>();

	public void init(TextField textField) {

		// popup.getContent().addAll(new Circle(25, 25, 50, Color.AQUAMARINE));
		
		textField.setEditable(true);

		ArrayList<String> items = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			items.add("nb-" + i);
		}
		listView.setItems(FXCollections.observableList(items));
		popup.getContent().add(listView);
		// popup.getScene().setRoot(listView);
		
		

		textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue == false) {
					popup.hide();
				}
			}
		});

		textField.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				// switch(event.getCode()) {
				// case TAB:
				// System.out.println("tab");
				// popup.hide();
				// break;
				// default:
				// }
			}

		});

		textField.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				boolean showAutoComplete = false;
				switch (event.getCode()) {
				case BACK_SPACE:
				case DELETE:
					showAutoComplete = true;
					break;
				case HOME:
					break;
				case END:
					break;
				case ESCAPE:
				case TAB:
					System.out.println("e.source=" + event.getSource() + ", e.target=" + event.getTarget());
					String s = listView.getSelectionModel().getSelectedItem();
					textField.setText(s);
					textField.positionCaret(0);
					textField.selectAll();
					popup.hide();
					break;
				default:
					if (!event.getText().isEmpty()) {
						showAutoComplete = true;
					}
					break;
				}
				if (showAutoComplete) {
					if (!popup.isShowing()) {
						Point2D p = textField.localToScene(0, 0);
						double x = p.getX() + textField.getScene().getX() + textField.getScene().getWindow().getX();
						double y = p.getY() + textField.getScene().getY() + textField.getScene().getWindow().getY();
						popup.setX(x);
						popup.setY(y + textField.getHeight());

						textField.widthProperty().addListener(new ChangeListener<Number>() {
							@Override
							public void changed(ObservableValue<? extends Number> observable, Number oldValue,
									Number newValue) {
								listView.setPrefWidth(newValue.doubleValue());
							}
						});

						listView.setPrefWidth(textField.getWidth());
						listView.setPrefHeight(textField.getHeight() * 10);

						// popup.setHeight(50);

						Window owner = textField.getScene().getWindow();
						popup.show(owner);
					}
				}
			}
		});

	}

}
