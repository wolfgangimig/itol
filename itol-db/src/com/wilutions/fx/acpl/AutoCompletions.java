package com.wilutions.fx.acpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class AutoCompletions {

	/**
	 * Display at most 10 suggestions.
	 */
	public final static int NB_OF_SUGGESTIONS = 10;

	/**
	 * Display at most 3 recent items.
	 */
	public static final int NB_OF_RECENT_ITEMS = 3;

	/**
	 * Prepare the given ComboBox for auto completion. This function uses the
	 * {@link DefaultSuggest}.
	 * 
	 * @param cbox
	 *            ComboBox
	 * @param allItems
	 *            All items.
	 * 
	 * @return AutoCompletionBinding
	 */
	public static <T> AutoCompletionBinding<T> bindAutoCompletion(ExtractImage<T> extractImage, ComboBox<T> cbox,
			String recentCaption, String suggestionsCaption, ArrayList<T> recentItems, Collection<T> allItems) {
		return bindAutoCompletion(extractImage, cbox, recentCaption, suggestionsCaption, recentItems,
				new DefaultSuggest<T>(allItems));
	}

	public static <T> AutoCompletionBinding<T> bindAutoCompletion(ExtractImage<T> extractImage, ComboBox<T> cbox,
			String recentCaption, String suggestionsCaption, final ArrayList<T> recentItems, final Suggest<T> suggest) {

		AutoCompletionBinding<T> binding = new AutoCompletionBinding<T>();
		binding.setComboBox(cbox);
		binding.setRecentCaption(recentCaption);
		binding.setSuggestionsCaption(suggestionsCaption);
		binding.setRecentItems(recentItems);
		binding.setSuggest(suggest);
		binding.setExtractImage(extractImage);

		ContextMenu popup = new ContextMenu();

		popup.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				KeyCode kc = event.getCode();
				if (kc == KeyCode.TAB) {

					event.consume();

					if (event.getEventType() == KeyEvent.KEY_PRESSED) {
						cbox.fireEvent(new KeyEvent(event.getSource(), event.getTarget(), KeyEvent.KEY_PRESSED, "\r",
								"", KeyCode.ENTER, false, false, false, false));

						// Parent parent = cbox.getParent();
						Platform.runLater(() -> {
							cbox.fireEvent(new KeyEvent(event.getSource(), event.getTarget(), KeyEvent.KEY_PRESSED,
									"\r", "", KeyCode.TAB, event.isShiftDown(), event.isControlDown(),
									event.isAltDown(), event.isMetaDown()));
							cbox.fireEvent(new KeyEvent(event.getSource(), event.getTarget(), KeyEvent.KEY_RELEASED,
									"\r", "", KeyCode.TAB, event.isShiftDown(), event.isControlDown(),
									event.isAltDown(), event.isMetaDown()));
						});
					}

					// System.out.println("TAB consumed");
				}
				if (kc == KeyCode.ENTER) {
					// System.out.println("ENTER " + event.getEventType());
				}
			}
		});

		cbox.setEditable(true);

		// Set String converter.
		// Without a string converter, I receive a ClassCastException
		// inside ComboBox code when it internally tries to set the selection
		// from the edit field text.
		// This happens after pressing return in the selection list
		// (MenuItem.onAction...). ComboBoxPopupControl catches ENTER
		// and tries to set the selection from the edit text.
		// It is sufficient to return an Exception in fromString.
		cbox.setConverter(new StringConverter<T>() {

			@Override
			public String toString(T object) {
				return object != null ? object.toString() : null;
			}

			@Override
			public T fromString(String string) {
				throw new UnsupportedOperationException();
			}
		});

		// Show suggestion list, if combobox button is pressed
		cbox.setOnShowing(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				Platform.runLater(() -> {
					cbox.hide();
					showList(popup, binding, SHOW_ALWAYS_IGNORE_EDIT_TEXT);
				});

			}

		});

		// Show suggestion list, if ALT+DOWN is pressed
		cbox.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				KeyCode kc = event.getCode();
				if (kc == KeyCode.TAB) {
					System.out.println("TAB in CBox");
				}
			}
		});

		// Show suggestion list, if ALT+DOWN is pressed
		cbox.getEditor().setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				KeyCode kc = event.getCode();
				if (kc == KeyCode.DOWN && event.isAltDown()) {
					Platform.runLater(() -> {
						showList(popup, binding, SHOW_ALWAYS_IGNORE_EDIT_TEXT);
					});
				}
			}
		});

		// Show suggestion list if edit text changes.
		cbox.getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!binding.isLockChangeEvent()) {
					Platform.runLater(() -> {
						showList(popup, binding, SHOW_IF_EDIT_TEXT_DOES_NOT_MATCH | DISABLE_EDIT_ON_SELECT);
					});
				}
			}
		});

		// Select all if the editor receives input focus.
		// if focus is lost and no item is selected, take the first suggestion.
		cbox.getEditor().focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue != null) {
					// Received focus?
					if (newValue) {
						// Select edit text, show list
						Platform.runLater(() -> {
							cbox.getEditor().selectAll();
							T selectedItem = cbox.getSelectionModel().getSelectedItem();
							System.out.println("selectedItem=" + selectedItem);
							if (selectedItem == null) {
								showList(popup, binding, SHOW_IF_EDIT_TEXT_DOES_NOT_MATCH);
							}
						});
					}
					else {
						// Lost focus: check for selected item.
						T item = cbox.getSelectionModel().getSelectedItem();
						String editText = cbox.getEditor().getText();

						// Is an item selected?
						if (item != null) {
							// Does edit box display selected item?
							if (item.toString().equals(editText)) {
								// OK, the user sees the correct selection
							}
							else {
								// User sees something different.
								// Replace the selection in the following
								// statements
								item = null;
							}
						}

						// No selection or wrong selection?
						if (item == null) {

							// Try to select the first suggested item
							if (editText.length() != 0) {
								Collection<T> suggestedItems = suggest.find(cbox.getEditor().getText(),
										NB_OF_SUGGESTIONS);
								if (suggestedItems.size() > 0) {
									item = suggestedItems.iterator().next();
								}
							}

							// No suggestion fits, take a recently used item.
							ArrayList<T> recentItems = binding.getRecentItems();
							if (item == null && recentItems != null && recentItems.size() != 0) {
								item = recentItems.get(0);
							}

						}

						// Select item
						if (item != null) {
							cbox.getSelectionModel().select(item);
							cbox.getEditor().setText(item.toString());
						}
						else {
							cbox.getSelectionModel().select(-1);
							cbox.getEditor().setText("");
						}

					}
				}
			}
		});

		return binding;
	}

	/**
	 * Show select list regardless of edit content.
	 */
	private final static int SHOW_ALWAYS_IGNORE_EDIT_TEXT = 1;

	/**
	 * Show select list only if the TextField does not match any item.
	 */
	private final static int SHOW_IF_EDIT_TEXT_DOES_NOT_MATCH = 2;

	/**
	 * Disable the TextField if it matches an item.
	 */
	private final static int DISABLE_EDIT_ON_SELECT = 4;

	private static <T> void showList(ContextMenu popup, AutoCompletionBinding<T> binding, int ctrl) {

		ExtractImage<T> extractImage = binding.getExtractImage();
		ComboBox<T> cbox = binding.getComboBox();
		String recentCaption = binding.getRecentCaption();
		String suggestionsCaption = binding.getSuggestionsCaption();
		ArrayList<T> recentItems = binding.getRecentItems();
		Suggest<T> suggest = binding.getSuggest();

		if ((ctrl & DISABLE_EDIT_ON_SELECT) == 0) {
			cbox.getEditor().setEditable(true);
		}

		boolean showAlways = (ctrl & SHOW_ALWAYS_IGNORE_EDIT_TEXT) != 0;
		String editText = showAlways ? "" : cbox.getEditor().getText();

		// find suggestions
		Collection<T> listItems = suggest.find(editText, NB_OF_SUGGESTIONS);

		// if only one suggestion is found...
		if (!showAlways && listItems.size() == 1) {

			T thisItem = listItems.iterator().next();
			selectItem(binding, thisItem, ctrl);

			popup.hide();
			return;
		}

		List<MenuItem> items = new ArrayList<MenuItem>();

		double menuWidth = cbox.getBoundsInParent().getWidth();

		boolean isRecentListAvailable = recentItems != null;
		if (isRecentListAvailable) {

			// Menu header "Recent"
			items.add(makeHeaderMenuItem(recentCaption, menuWidth));

			// Items from recent list
			for (T item : recentItems) {
				Image image = extractImage.getImage(item);
				MenuItem cmItem1 = new MenuItem(item.toString(), new ImageView(image));
				cmItem1.setOnAction(new EventHandler<ActionEvent>() {
					public void handle(ActionEvent e) {
						selectItem(binding, item, DISABLE_EDIT_ON_SELECT);
					}
				});
				items.add(cmItem1);
			}

			// Separator
			SeparatorMenuItem sep = new SeparatorMenuItem();
			items.add(sep);

			// Menu header "Suggestions"
			items.add(makeHeaderMenuItem(suggestionsCaption, menuWidth));
		}

		for (T item : listItems) {
			Image image = extractImage.getImage(item);
			MenuItem cmItem1 = new MenuItem(item.toString(), new ImageView(image));
			cmItem1.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent e) {
					selectItem(binding, item, DISABLE_EDIT_ON_SELECT);
				}
			});
			items.add(cmItem1);
		}

		popup.getItems().clear();
		popup.getItems().addAll(FXCollections.observableArrayList(items));
		// cm.setMaxHeight(200);

		if (!popup.isShowing()) {

			showPopupBelowNode(cbox, popup);

			// Eigentlich ist dafür diese Methode vorgesehen:
			// cm.show(cbox, javafx.geometry.Side.TOP,0,0);
			// Aber dann wird das Menü an der falschen Stelle angezeigt,
			// wenn es zum zweiten mal dargestellt wird.
		}

	}

	private static <T> void selectItem(AutoCompletionBinding<T> binding, T item, int ctrl) {

		binding.setLockChangeEvent(true);

		// Select in ComboBox.
		ComboBox<T> cbox = binding.getComboBox();
		cbox.getSelectionModel().select(item);

		// Set item text in editor.
		final TextField ed = cbox.getEditor();
		String itemText = item.toString();
		ed.setText(itemText);
		ed.selectAll();

		// Disable TextField for 2 seconds.
		if ((ctrl & DISABLE_EDIT_ON_SELECT) != 0) {

			ed.setEditable(false);

			Timeline timeline = new Timeline(new KeyFrame(Duration.millis(2000), ae -> ed.setEditable(true)));
			timeline.play();
		}

		// Add item to recent list
		ArrayList<T> recentItems = binding.getRecentItems();
		if (recentItems != null && !recentItems.contains(item)) {
			if (recentItems.size() >= NB_OF_RECENT_ITEMS) {
				recentItems.remove(recentItems.size() - 1);
			}
			recentItems.add(0, item);
		}

		binding.setLockChangeEvent(false);
	}

	private static MenuItem makeHeaderMenuItem(String text, double wd) {
		Label label = new Label(text);
		label.setStyle("-fx-font-weight: bold;");
		label.setPrefWidth(wd);
		label.setMinWidth(Label.USE_PREF_SIZE);
		// label.setMaxWidth(wd);
		CustomMenuItem mi = new CustomMenuItem(label);
		mi.setHideOnClick(false);
		return mi;
	}

	private static void showPopupBelowNode(final Node node, final ContextMenu popup) {
		final Window window = node.getScene().getWindow();
		double x = window.getX() + node.localToScene(0, 0).getX() + node.getScene().getX();
		double y = window.getY() + node.localToScene(0, 0).getY() + node.getScene().getY()
				+ node.getBoundsInLocal().getHeight();
		// + node.getBoundsInParent().getHeight();
		popup.show(window, x, y);
	}

}
