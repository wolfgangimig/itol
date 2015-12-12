package com.wilutions.fx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

public class AutoCompletions {

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

			if (endIdx > NB_OF_SUGGESTIONS) endIdx = NB_OF_SUGGESTIONS;

			// Cut the list at the item that does not contain the text.
			Collection<T> ret = matches.subList(0, endIdx);

			return ret;
		}

		private int makeCompareFromPosition(int p) {
			if (p == 0) {
				// item starts with the given text.
				// This item should be positioned at the beginning of the list.
			}
			else if (p > 0) {
				// The item contains the text but does not start with it.
				// Set p=1 since it does not matter where the text is found.
				p = 1;
			}
			else if (p < 0) {
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
	 * @param cbox
	 *            ComboBox
	 * @param allItems
	 *            All items.
	 */
	public static <T> void bindAutoCompletion(ComboBox<T> cbox, String recentCaption, String suggestionsCaption,
			ArrayList<T> recentItems, Collection<T> allItems) {
		bindAutoCompletion(cbox, recentCaption, suggestionsCaption, recentItems, new DefaultSuggest<T>(allItems));
	}

	public static <T> void bindAutoCompletion(ComboBox<T> cbox, String recentCaption, String suggestionsCaption,
			final ArrayList<T> recentItems, final Suggest<T> suggest) {

		ContextMenu popup = new ContextMenu();

		cbox.setEditable(true);

		// Show suggestion list, if combobox button is pressed
		cbox.setOnShowing(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				Platform.runLater(() -> {
					cbox.hide();
					showList(cbox, popup, recentCaption, suggestionsCaption, recentItems, suggest,
							SHOW_ALWAYS_IGNORE_EDIT_TEXT);
				});

			}

		});

		// Show suggestion list, if ALT+DOWN is pressed
		cbox.getEditor().setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				KeyCode kc = event.getCode();
				if (kc == KeyCode.DOWN && event.isAltDown()) {
					Platform.runLater(() -> {
						showList(cbox, popup, recentCaption, suggestionsCaption, recentItems, suggest,
								SHOW_ALWAYS_IGNORE_EDIT_TEXT);

					});
				}
			}
		});

		// Show suggestion list if edit text changes.
		cbox.getEditor().textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				Platform.runLater(() -> {
					showList(cbox, popup, recentCaption, suggestionsCaption, recentItems, suggest,
							SHOW_EDIT_TEXT_DOES_NOT_EXACTLY_MATCH);
				});
			}
		});
		
		// Select all 
		cbox.getEditor().focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue != null && newValue) {
					Platform.runLater(() -> {
						cbox.getEditor().selectAll();
					});
				}
			}
		});
	}

	private final static boolean SHOW_ALWAYS_IGNORE_EDIT_TEXT = true;
	private final static boolean SHOW_EDIT_TEXT_DOES_NOT_EXACTLY_MATCH = false;
	private static final int NB_OF_RECENT_ITEMS = 3;

	private static <T> void showList(ComboBox<T> cbox, ContextMenu cm, String recentCaption, String suggestionsCaption,
			ArrayList<T> recentItems, Suggest<T> suggest, boolean showAlways) {

		String editText = showAlways ? "" : cbox.getEditor().getText();

		// find suggestions
		Collection<T> listItems = suggest.find(editText);

		// if only one suggestion is found...
		if (!showAlways && listItems.size() == 1) {

			// if the suggestion is exactly the edit text...
			T thisItem = listItems.iterator().next();
			String itemStr = thisItem.toString();
			if (itemStr.equals(editText)) {

				// if the suggestion is not already the selected item,
				// the user has entered its exact name.
				if (cbox.getSelectionModel().getSelectedItem() != thisItem) {
					selectItem(cbox, thisItem, recentItems);
				}
				else {
					// The function is called from the changed-handler of the
					// editor, which in turn has been fired because a suggestion
					// was selected from the list.
				}

				cm.hide();
				return;
			}
		}

		List<MenuItem> items = new ArrayList<MenuItem>();

		double menuWidth = cbox.getBoundsInParent().getWidth();

		items.add(makeHeaderMenuItem(recentCaption, menuWidth));

		for (T item : recentItems) {
			MenuItem cmItem1 = new MenuItem(item.toString());
			cmItem1.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent e) {
					selectItem(cbox, item, recentItems);
				}
			});
			items.add(cmItem1);
		}

		SeparatorMenuItem sep = new SeparatorMenuItem();
		items.add(sep);

		items.add(makeHeaderMenuItem(suggestionsCaption, menuWidth));

		for (T item : listItems) {
			MenuItem cmItem1 = new MenuItem(item.toString());
			cmItem1.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent e) {
					selectItem(cbox, item, recentItems);
				}
			});
			items.add(cmItem1);
		}

		cm.getItems().clear();
		cm.getItems().addAll(FXCollections.observableArrayList(items));
		// cm.setMaxHeight(200);

		if (!cm.isShowing()) {

			showPopupBelowNode(cbox, cm);

			// Eigentlich ist dafür diese Methode vorgesehen:
			// cm.show(cbox, javafx.geometry.Side.TOP,0,0);
			// Aber dann wird das Menü an der falschen Stelle angezeigt,
			// wenn es zum zweiten mal dargestellt wird.
		}

	}

	private static <T> void selectItem(ComboBox<T> cbox, T item, ArrayList<T> recentItems) {
		cbox.getSelectionModel().select(item);
		cbox.getEditor().selectAll();
		if (!recentItems.contains(item)) {
			if (recentItems.size() >= NB_OF_RECENT_ITEMS) {
				recentItems.remove(recentItems.size()-1);
			}
			recentItems.add(0, item);
		}
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
