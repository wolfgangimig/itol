package com.wilutions.itol;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.Duration;

import com.wilutions.itol.db.IdName;

public class AutoCompleteControl {

	private final static long SHOW_DELAY_MILLIS = 200;

	public static void autoCompleteComboBox(final ComboBox<IdName> comboBox,
			final Callback<String, List<IdName>> findSuggestions) {
		comboBox.setEditable(true);

		final TextField ed = comboBox.getEditor();

		// comboBox.setOnAction((event) -> {
		// IdName idn = comboBox.getSelectionModel().getSelectedItem();
		// System.out.println("selected " + idn);
		// });

		comboBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == false) {
				String text = ed.getText();
				List<IdName> items = comboBox.getItems();
				IdName idn = items.size() != 0 ? items.get(0) : null;
				if (idn != null && text.equals(idn.getName())) {
					comboBox.getSelectionModel().select(0);
				}
				else {
					comboBox.getSelectionModel().clearSelection();
				}
			}
		});

		comboBox.addEventHandler(KeyEvent.KEY_PRESSED, (e) -> comboBox.hide());
		comboBox.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			@SuppressWarnings("unused")
			private Timeline deferShowSelectedItem = initTimeline();
			private long showAutoCompleteAtMillis = Long.MAX_VALUE;
			private AtomicInteger callId = new AtomicInteger(0);

			private Timeline initTimeline() {

				Timeline tl = new Timeline(new KeyFrame(Duration.seconds(0.1), new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						if (isAutoCompleteDelayOver() && comboBox.getEditor().isFocused()) {
							final int currentCallId = callId.incrementAndGet();
							new Thread(() -> {
								if (currentCallId == callId.get()) {
									showAutoComplete();
								}
							}).start();
						}
					}

					private void showAutoComplete() {
						String filter = ed.getText();
						if (filter == null) filter = "";
						final List<IdName> list = findSuggestions.call(filter);

						Platform.runLater(() -> {
							ObservableList<IdName> olist = FXCollections.observableArrayList(list);
							comboBox.setItems(olist);

							int n = olist.size();
							switch (n) {
							case 1:
								if (ed.getText().equals(olist.get(0).getName())) {
									comboBox.getSelectionModel().select(0);
								}
								else {
									comboBox.show();
								}
								break;
							case 0:
								olist.add(new IdName(-1, "No matches found"));
							default:
								comboBox.getSelectionModel().clearSelection();
								comboBox.show();
							}
						});
					}

					private boolean isAutoCompleteDelayOver() {
						boolean ret = showAutoCompleteAtMillis <= System.currentTimeMillis();
						if (ret) {
							showAutoCompleteAtMillis = Long.MAX_VALUE;
						}
						return ret;
					}

				}));

				tl.setCycleCount(Timeline.INDEFINITE);
				tl.play();
				return tl;
			}

			@Override
			public void handle(KeyEvent event) {
				boolean showAutoComplete = false;
				switch (event.getCode()) {
				case BACK_SPACE:
				case DELETE:
					showAutoComplete = true;
					break;
				case HOME:
					ed.positionCaret(0);
					event.consume();
					break;
				case END:
					ed.positionCaret(ed.getText().length());
					event.consume();
					break;
				default:
					if (!event.getText().isEmpty()) {
						showAutoComplete = true;
					}
					break;
				}

				if (showAutoComplete) {
					showAutoCompleteAtMillis = System.currentTimeMillis() + SHOW_DELAY_MILLIS;
				}
			}
		});
	}

	public static IdName getComboBoxValue(ComboBox<IdName> comboBox) {
		if (comboBox.getSelectionModel().getSelectedIndex() < 0) {
			return null;
		}
		else {
			return comboBox.getItems().get(comboBox.getSelectionModel().getSelectedIndex());
		}
	}
}