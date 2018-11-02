package com.wilutions.fx.acpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.wilutions.itol.db.DefaultSuggest;
import com.wilutions.itol.db.Suggest;

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
import javafx.scene.control.Skin;
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
			String recentCaption, String suggestionsCaption, List<T> recentItems, Collection<T> allItems) {
		return bindAutoCompletion(extractImage, cbox, recentCaption, suggestionsCaption, recentItems,
				new DefaultSuggest<T>(allItems));
	}

	public static <T> AutoCompletionBinding<T> bindAutoCompletion(ExtractImage<T> extractImage, ComboBox<T> cbox,
			String recentCaption, String suggestionsCaption, final List<T> recentItems, final Suggest<T> suggest) {

		AutoCompletionControl<T> control = createAutoCompletionControl(cbox, extractImage);
		AutoCompletionBinding<T> binding = createAutoCompletionBinding(extractImage, recentCaption, suggestionsCaption,
				recentItems, suggest, control);

		ContextMenu popup = createPopup(cbox);

		bindComboBox(cbox, binding, popup);

		internalBindTextField(cbox.getEditor(), binding, popup);

		return binding;
	}
	
	public static <T> AutoCompletionComboBox<T> createAutoCompletionNode(
			ExtractImage<T> extractImage, 
			String recentCaption, 
			String suggestionsCaption, 
			List<T> recentItems, 
			 Suggest<T> suggest) {
		AutoCompletionComboBox<T> cbox = new AutoCompletionComboBox<T>();
		AutoCompletionBinding<T> binding = bindAutoCompletion(extractImage, cbox, recentCaption, suggestionsCaption, recentItems, suggest);
		cbox.setBinding(binding);
		return cbox;
	}
	
	private static <T> void bindComboBox(ComboBox<T> cbox, AutoCompletionBinding<T> binding, ContextMenu popup) {

		cbox.setEditable(true);

		TextField ed = cbox.getEditor();
		
		// 
		if (binding.getExtractImage() != null) {
			ed.setSkin(new TextFieldSkinWithImage(ed));
		}

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
	}

	private static <T> AutoCompletionBinding<T> createAutoCompletionBinding(ExtractImage<T> extractImage,
			String recentCaption, String suggestionsCaption, final List<T> recentItems, final Suggest<T> suggest,
			AutoCompletionControl<T> control) {
		AutoCompletionBinding<T> binding = new AutoCompletionBinding<T>();
		binding.setControl(control);
		binding.setRecentCaption(recentCaption);
		binding.setSuggestionsCaption(suggestionsCaption);
		binding.setRecentItems(recentItems);
		binding.setSuggest(suggest);
		binding.setExtractImage(extractImage);
		return binding;
	}

	private static <T> ContextMenu createPopup(Node node) {
		ContextMenu popup = new ContextMenu();

		popup.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				KeyCode kc = event.getCode();
				if (kc == KeyCode.TAB) {

					event.consume();

					if (event.getEventType() == KeyEvent.KEY_PRESSED) {
						node.fireEvent(new KeyEvent(event.getSource(), event.getTarget(), KeyEvent.KEY_PRESSED, "\r",
								"", KeyCode.ENTER, false, false, false, false));

						// Wenn ein Men�-Eintrag ausgew�hlt ist, dann soll bei
						// TAB
						// der Auswahl im Auto-Editfeld eingetragen werden und
						// anschlie�end
						// der Fokus zum n�chsten Control weitergegeben werden.
						// Ist aber kein Men�-Eintrag ausgew�hlt, dann soll der
						// Fokus nicht
						// weiter wandern. Ich kann hier aber nicht feststellen,
						// ob ein
						// Men�eintrag gew�hlt ist. Das f�hrt dazu, dass das
						// Men� nicht
						// geschlossen wird und die Tab-Key-Ereignisse gepuffert
						// werden.
						// Erfolgt dann eine Selektion z. B. mit Cursortasten,
						// geht das
						// Men� unerwartet zu und der Fokus zum n�chsten Control
						// Deshal ist der Block hier auskommentiert.
						// Platform.runLater(() -> {
						// node.fireEvent(new KeyEvent(event.getSource(),
						// event.getTarget(), KeyEvent.KEY_PRESSED,
						// "\t", "", KeyCode.TAB, event.isShiftDown(),
						// event.isControlDown(),
						// event.isAltDown(), event.isMetaDown()));
						// node.fireEvent(new KeyEvent(event.getSource(),
						// event.getTarget(), KeyEvent.KEY_RELEASED,
						// "\t", "", KeyCode.TAB, event.isShiftDown(),
						// event.isControlDown(),
						// event.isAltDown(), event.isMetaDown()));
						// });
					}

					// System.out.println("TAB consumed");
				}
				if (kc == KeyCode.ENTER) {
					// System.out.println("ENTER " + event.getEventType());
				}
			}
		});
		return popup;
	}

	private static <T> void internalBindTextField(TextField textField, AutoCompletionBinding<T> binding,
			ContextMenu popup) {

		final Suggest<T> suggest = binding.getSuggest();

		// Show suggestion list, if ALT+DOWN is pressed
		textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
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
		textField.textProperty().addListener(new ChangeListener<String>() {
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
		textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue != null) {
					// Received focus?
					if (newValue) {
						// Select edit text, show list
						Platform.runLater(() -> {
							textField.selectAll();
							T selectedItem = binding.getControl().getSelectedItem();
							if (selectedItem == null) {
								showList(popup, binding, SHOW_IF_EDIT_TEXT_DOES_NOT_MATCH);
							}
						});
					}
					else {
						// Lost focus: check for selected item.
						T item = binding.getControl().getSelectedItem();
						String editText = textField.getText();

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
								Collection<T> suggestedItems = suggest.find(textField.getText(), NB_OF_SUGGESTIONS, null);
								if (suggestedItems.size() > 0) {
									item = suggestedItems.iterator().next();
								}
							}

							// No suggestion fits, take a recently used item.
							List<T> recentItems = binding.getRecentItems();
							if (item == null && recentItems != null && recentItems.size() != 0) {
								item = recentItems.get(0);
							}

						}

						// Select item
						binding.getControl().select(item);
					}
				}
			}
		});
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

		AutoCompletionControl<T> control = binding.getControl();
		String recentCaption = binding.getRecentCaption();
		String suggestionsCaption = binding.getSuggestionsCaption();
		List<T> recentItems = binding.getRecentItems();
		Suggest<T> suggest = binding.getSuggest();

		if ((ctrl & DISABLE_EDIT_ON_SELECT) == 0) {
			control.setEditable(true);
		}

		boolean showAlways = (ctrl & SHOW_ALWAYS_IGNORE_EDIT_TEXT) != 0;
		String editText = showAlways ? "" : control.getEditText();

		// find suggestions
		Collection<T> suggestedItems = suggest.find(editText, NB_OF_SUGGESTIONS, null);

		// if only one suggestion is found...
		if (!showAlways && suggestedItems.size() == 1) {

			T thisItem = suggestedItems.iterator().next();
			selectItem(binding, thisItem, ctrl);

			popup.hide();
			return;
		}

		List<MenuItem> items = new ArrayList<MenuItem>();

		double menuWidth = control.getNode().getBoundsInParent().getWidth();

		boolean showRecentList = recentItems != null && editText.isEmpty();
		if (showRecentList) {

			// Menu header "Recent"
			items.add(makeHeaderMenuItem(recentCaption, menuWidth));

			// Items from recent list
			for (T item : recentItems) {
				addMenuItem(items, binding, item);
			}

			// Separator
			SeparatorMenuItem sep = new SeparatorMenuItem();
			items.add(sep);

			// Menu header "Suggestions"
			items.add(makeHeaderMenuItem(suggestionsCaption, menuWidth));
		}

		for (T item : suggestedItems) {
			addMenuItem(items, binding, item);
		}

		popup.getItems().clear();
		popup.getItems().addAll(FXCollections.observableArrayList(items));
		// cm.setMaxHeight(200);

		if (!popup.isShowing()) {

			showPopupBelowNode(control.getNode(), popup);

			// Eigentlich ist daf�r diese Methode vorgesehen:
			// cm.show(cbox, javafx.geometry.Side.TOP,0,0);
			// Aber dann wird das Men� an der falschen Stelle angezeigt,
			// wenn es zum zweiten mal dargestellt wird.
		}

	}

	private static <T> void addMenuItem(List<MenuItem> items, AutoCompletionBinding<T> binding, T item) {
		ExtractImage<T> extractImage = binding.getExtractImage();
		ImageView imageView = null;
		if (extractImage != null) {
			Image image = extractImage.getImage(item);
			imageView = makeImageView(image);
		}
		MenuItem cmItem1 = new MenuItem(item.toString(), imageView);
		cmItem1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				selectItem(binding, item, DISABLE_EDIT_ON_SELECT);
			}
		});
		items.add(cmItem1);
	}

	private static ImageView makeImageView(Image image) {
		if (image == null) return null;
		ImageView imageView = new ImageView();
		imageView.setImage(image);
		imageView.setFitWidth(16);
		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);
		imageView.setCache(true);
		return imageView;
	}

	private static <T> void selectItem(AutoCompletionBinding<T> binding, T item, int ctrl) {

		binding.setLockChangeEvent(true);

		// Select item in control
		AutoCompletionControl<T> control = binding.getControl();
		control.select(item);

		// Disable TextField for 1 second
		if ((ctrl & DISABLE_EDIT_ON_SELECT) != 0) {

			control.setEditable(false);

			Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000), ae -> control.setEditable(true)));
			timeline.play();
		}

		// Add item to recent list
		List<T> recentItems = binding.getRecentItems();
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
		popup.show(window, x, y);
	}

	private static <T> AutoCompletionControl<T> createAutoCompletionControl(final ComboBox<T> cbox,
			final ExtractImage<T> extractImage) {
		AutoCompletionControl<T> control = new AutoCompletionControl<T>() {
			public void select(T item) {
				final TextField ed = cbox.getEditor();
				if (item != null) {
					cbox.getSelectionModel().select(item);
					if (extractImage != null) {
						Image image = extractImage.getImage(item);
						ImageView imageView = makeImageView(image);
						((TextFieldSkinWithImage) ed.getSkin()).setImageView(imageView);
					}
					ed.setText(item.toString());
					ed.selectAll();
				}
				else {
					cbox.getSelectionModel().select(-1);
					Skin<?> tfs = ed.getSkin();
					if (tfs != null) {
						if (tfs instanceof TextFieldSkinWithImage) {
							TextFieldSkinWithImage tfswi = ((TextFieldSkinWithImage) ed.getSkin());
							tfswi.setImageView(null);
						}
					}
					ed.setText("");
				}
			}

			public T getSelectedItem() {
				return cbox.getSelectionModel().getSelectedItem();
			}

			public Node getNode() {
				return cbox;
			}

			public void setEditable(boolean en) {
				cbox.getEditor().setEditable(en);
			}

			public boolean isEditable() {
				return cbox.getEditor().isEditable();
			}

			public String getEditText() {
				return cbox.getEditor().getText();
			}
		};
		return control;
	}

}
