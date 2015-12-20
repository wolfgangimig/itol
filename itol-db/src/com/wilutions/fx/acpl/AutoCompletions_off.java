package com.wilutions.fx.acpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Creates an auto completion field. An auto completion field is based on a
 * ComboBox. A special dummy item "theSearchItem" in the ComboBox shows a popup
 * window that contains a TextField and a ListView. If a key is pressed in the
 * TextField, suggestions are tried to be found via a callback interface. The
 * returned suggestions are listed in the ListView. A suggestion is sellected by
 * pressing ENTER or double-clicking the item in the ListView.
 */
public class AutoCompletions_off {

	/**
	 * Display at most 10 suggestions.
	 */
	public final static int NB_OF_SUGGESTIONS = 10;

	/**
	 * Callback interface to find suggestions for a given text.
	 * 
	 * @param <T>
	 *            Item type
	 */
	public static interface Suggest<T> {
		/**
		 * Find suggestions for given text.
		 * 
		 * @param text
		 *            Text
		 * @return Collection of suggestions to be displayed in the list view.
		 */
		public Collection<T> find(String text);
	}

	/**
	 * Default implementation for interface Suggest.
	 * 
	 * @param <T>
	 *            Item type
	 */
	public static class DefaultSuggest<T> implements Suggest<T> {

		/**
		 * All items. Passed in the constructor.
		 */
		protected Collection<T> allItems;

		/**
		 * Constructor.
		 * 
		 * @param allItems
		 *            Collection of all items.
		 */
		public DefaultSuggest(Collection<T> allItems) {
			this.allItems = allItems;
		}

		/**
		 * Constructor.
		 */
		public DefaultSuggest() {
		}

		/**
		 * Find suggestions for given text. All items are returned that contain
		 * the given text in their return value of toString. Those items that
		 * start with the given text are ordered at the beginning of the
		 * returned collection.
		 * 
		 * @param text
		 *            Text
		 * @return Collection of items.
		 */
		public Collection<T> find(String text) {
			ArrayList<T> matches = new ArrayList<T>(allItems);
			String textLC = text.toLowerCase();

			Collections.sort(matches, new Comparator<T>() {
				public int compare(T o1, T o2) {
					String s1 = o1.toString().toLowerCase();
					String s2 = o2.toString().toLowerCase();
					int cmp = 0;
					if (!textLC.isEmpty()) {
						int p1 = s1.indexOf(textLC);
						int p2 = s2.indexOf(textLC);
						p1 = makeCompareFromPosition(p1);
						p2 = makeCompareFromPosition(p2);
						cmp = p1 - p2;
					}
					if (cmp == 0) {
						cmp = s1.compareTo(s2);
					}
					return cmp;
				}
			});

			// Return only items that contain the text.
			// Therefore, find the first item that does not contain the text.
			int endIdx = 0;
			for (; endIdx < matches.size(); endIdx++) {
				T item = matches.get(endIdx);
				if (!item.toString().toLowerCase().contains(textLC)) {
					break;
				}
			}

			// Cut the list at the item that does not contain the text.
			Collection<T> ret = matches.subList(0, endIdx);

			return ret;
		}

		private int makeCompareFromPosition(int p) {
			if (p == 0) {
				// item starts with the given text.
				// This item should be positioned at the beginning of the list.
			} else if (p > 0) {
				// The item contains the text but does not start with it.
				// Set p=1 since it does not matter where the text is found.
				p = 1;
			} else if (p < 0) {
				// The item does not contain the text.
				// Move this item at the end of the list.
				p = Integer.MAX_VALUE;
			}
			return p;
		}

	}

	/**
	 * Prepare the given ComboBox for auto completion. This function uses the
	 * {@link DefaultSuggest}.
	 * 
	 * @param cbbox
	 *            ComboBox
	 * @param theSearchItem
	 *            The dummy item that is to be selected to start the auto
	 *            completion popup.
	 * @param allItems
	 *            All items.
	 */
	public static <T> void bindAutoCompletion(ComboBox<T> cbbox, final T theSearchItem, Collection<T> allItems) {
		bindAutoCompletion(cbbox, theSearchItem, new DefaultSuggest<T>(allItems));
	}

	/**
	 * Prepare the given ComboBox for auto completion.
	 * 
	 * @param cbbox
	 *            ComboBox
	 * @param theSearchItem
	 *            The dummy item that is to be selected to start the auto
	 *            completion popup.
	 * @param suggest
	 *            Callback interface to find suggestions.
	 */
	public static <T> void bindAutoCompletion(ComboBox<T> cbbox, final T theSearchItem, Suggest<T> suggest) {

		// Make sure the dummy item "theSearchItem" is contained in the ComboBox.
		if (!cbbox.getItems().contains(theSearchItem)) {
			cbbox.getItems().add(0, theSearchItem);
		}

		// Select the "theSearchItem" if the ComboBox does not have a selected
		// item.
		if (cbbox.getSelectionModel().getSelectedIndex() == -1) {
			cbbox.getSelectionModel().select(theSearchItem);
		}

		// Make sure the ComboBox has a converter interface.
		if (cbbox.getConverter() == null) {
			cbbox.setConverter(new StringConverter<T>() {
				@Override
				public String toString(T object) {
					return object != null ? object.toString() : null;
				}

				@Override
				public T fromString(String string) {
					throw new UnsupportedOperationException();
				}
			});
		}

		// Set listener for selection changed event.
		// If the selection changes to the "theSearchItem", the auto complete
		// popoup is shown.
		cbbox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<T>() {

			@Override
			public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
				if (newValue != null && newValue.equals(theSearchItem)) {
					Platform.runLater(() -> {
						showAutoCompletionPopup(cbbox, theSearchItem, suggest);
					});
				}
			}

		});

		// Key-pressed handler
		cbbox.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				switch (event.getCode()) {

				// Show auto complete popup, if SPACE or ENTER is pressed while
				// "theSearchItem" is selected.
				case SPACE:
				case ENTER:
					if (cbbox.getSelectionModel().getSelectedItem() == theSearchItem) {
						Platform.runLater(() -> showAutoCompletionPopup(cbbox, theSearchItem, suggest));
					}
					else {
						cbbox.show();
					}
					break;

				// Show auto complete popup if CTRL-F is pressed.
				case F:
					if (event.isControlDown()) {
						event.consume();
						Platform.runLater(() -> showAutoCompletionPopup(cbbox, theSearchItem, suggest));
					}
					break;

				// Show the list of the ComboBox if ALT-Down is pressed.
				case DOWN:
					if (event.isAltDown()) {
						event.consume();
						cbbox.show();
					}
					break;
				default:

				}
			}

		});

		// Mouse handler
		cbbox.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				// Show auto complete popup, if the mouse is clicked while
				// "theSearchItem" is selected.
				if (cbbox.getSelectionModel().getSelectedItem() == theSearchItem) {
					Platform.runLater(() -> showAutoCompletionPopup(cbbox, theSearchItem, suggest));
				}
			}

		});
		
		// Cell factory 
		cbbox.setCellFactory(
	            new Callback<ListView<T>, ListCell<T>>() {
	                @Override public ListCell<T> call(ListView<T> param) {
	                    final ListCell<T> cell = new ListCell<T>() {
	                        {
	                            super.setPrefWidth(100);
	                        }    
	                        @Override public void updateItem(T item, 
	                            boolean empty) {
	                                super.updateItem(item, empty);
	                                if (item != null) {
	                                    setText(item.toString());
	                                    if (item == theSearchItem) {
	                                    	setStyle("-fx-font-style: italic;");
	                                    }
	                                }
	                                else {
	                                    setText(null);
	                                }
	                            }
	                };
	                return cell;
	            }
	        });
	}

	/**
	 * Show auto completion popup.
	 * 
	 * @param cbbox
	 *            ComboBox
	 * @param theSearchItem
	 *            Dummy item
	 * @param suggest
	 *            Suggest callback
	 */
	private static <T> void showAutoCompletionPopup(ComboBox<T> cbbox, final T theSearchItem, Suggest<T> suggest) {

		// Edit field
		TextField textField = new TextField();

		// Transfer the selected item from the ComboBox into the TextField.
		// T item = cbbox.getSelectionModel().getSelectedItem();
		// if (item != null && item != theSearchItem) {
		// String s = cbbox.getConverter().toString(item);
		// textField.setText(s);
		// }

		// List box
		ListView<T> listView = new ListView<T>();

		// Fill ListView with suggestions.
		// Select the first item in the ListView.
		// This allows to press ENTER immediately after the popup has been
		// displayed.
		listView.setItems(FXCollections.observableArrayList(suggest.find(textField.getText())));
		listView.getSelectionModel().select(0);

		// Arrange EditField and ListBox vertically.
		VBox vbox = new VBox();
		vbox.getChildren().addAll(textField, listView);

		// Create a new window.
		Window parent = cbbox.getScene().getWindow();
		Stage dlg = new Stage();
		dlg.initOwner(parent);
		dlg.initStyle(StageStyle.UNDECORATED);

		// Create a scene
		double wd = cbbox.getBoundsInParent().getWidth(); // * 2;
		double ht = cbbox.getBoundsInParent().getHeight() * (NB_OF_SUGGESTIONS);
		Scene scene = new Scene(vbox, wd, ht);
		dlg.setScene(scene);

		// Move popup window over ComboBox.
		final Window window = cbbox.getScene().getWindow();
		double x = window.getX() + cbbox.localToScene(0, 0).getX() + cbbox.getScene().getX();
		double y = window.getY() + cbbox.localToScene(0, 0).getY() + cbbox.getScene().getY();
		dlg.setX(x);
		dlg.setY(y);

		// Key released handler for TextField
		textField.setOnKeyReleased(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				switch (event.getCode()) {

				// Move focus to ListView if arrow key UP or DOWN is pressed.
				case DOWN:
				case UP:
					listView.requestFocus();
					event.consume();
					break;

				// Do nothing with positioning keys.
				case LEFT:
				case RIGHT:
				case HOME:
				case END:
					event.consume();
					break;

				// Find suggestions.
				default:
					Platform.runLater(() -> {
						Collection<T> listItems = suggest.find(textField.getText());
						listView.setItems(FXCollections.observableArrayList(listItems));
						listView.getSelectionModel().select(0);
					});
					break;
				}
			}

		});

		// Key pressed handler for TextField and ListView
		EventHandler<KeyEvent> keyHandler = new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				switch (event.getCode()) {

				// Close popup on ESCAPE, do not change ComboBox selection.
				case ESCAPE:
					dlg.close();
					break;

				// On TAB or ENTER, select item in ComboBox and close popup.
				case TAB:
				case ENTER: {
					selectedListItemToComboBox(cbbox, listView);
					dlg.close();
					break;
				}
				default:
				}
			}
		};
		textField.setOnKeyPressed(keyHandler);
		listView.setOnKeyPressed(keyHandler);

		// Mouse handler for ListView
		listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent mouseEvent) {
				
				// On double-click, select item in ComboBox and close popup.
				if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
					if (mouseEvent.getClickCount() > 1) {
						selectedListItemToComboBox(cbbox, listView);
						dlg.close();
					}
				}
			}
		});
		
		// Focused handler for popup.
		// Close -popup if it looses the focus (e.g. mouse click somewhere else).
		dlg.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					dlg.close();
				}
			}
		});

		// Show auto completion popup
		dlg.show();
	}

	/**
	 * Select ComboBox item if ENTER is pressed.
	 * @param cbbox ComboBox
	 * @param listView ListView
	 */
	private static <T> void selectedListItemToComboBox(ComboBox<T> cbbox, ListView<T> listView) {
		T selectedItem = listView.getSelectionModel().getSelectedItem();
		if (selectedItem != null) {
			if (!cbbox.getItems().contains(selectedItem)) {
				cbbox.getItems().add(selectedItem);
			}
			cbbox.getSelectionModel().select(selectedItem);
		}
	}

}
