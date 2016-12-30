package com.wilutions.fx.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

import com.wilutions.itol.db.Suggest;

import javafx.application.Platform;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class TextFieldWithSuggestions<T> extends TextField {

	private PopupForSuggestions<T> popup;
	private boolean suggestionsLocked = false;
	public final static String DELIMS = " \t\n\r\f";

	public TextFieldWithSuggestions(Suggest<T> suggest) {
		popup = new PopupForSuggestions<T>(this, 
				new SuggestIfNotExist<T>(suggest), 
				(item) -> onSuggestionSelected(item));
		
		this.textProperty().addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				updateSuggestions();
			});
		});
		
		this.setOnKeyPressed((keyEvent) -> {
			KeyCode code = keyEvent.getCode();
			switch (code) {
			case DOWN:
			case UP:
				if (popup.isShowing()) {
					popup.fireEvent(keyEvent);
				}
				else {
					// show popup
					popup.updateSuggestions("");
				}
				keyEvent.consume();
				break;
			case TAB:
				if (popup.isShowing()) {
					popup.fireEvent(keyEvent);
					keyEvent.consume();
				}
			default:
			}
		});

	}

	private void updateSuggestions() {
		String newValue = getText();
		String filter = "";
		if (!suggestionsLocked && !newValue.isEmpty()) {
			int startIndex = getStartTokenIndex(newValue);
			int endIndex = getEndTokenIndex(newValue, startIndex);
			filter = newValue.substring(startIndex, endIndex);
		}
		popup.updateSuggestions(filter);
	}
	
	protected int getStartTokenIndex(String text) {
		int index = 0;
		int caretPos = this.getCaretPosition()-1;
		for (int i = caretPos; i >= 0; i--) {
			if (i < text.length() && DELIMS.indexOf(text.charAt(i)) >= 0) {
				index = i+1;
				break;
			}
		}
		return index;
	}
	
	protected int getEndTokenIndex(String text, int startIndex) {
		int index = text.length();
		for (int i = startIndex; i < text.length(); i++) {
			if (DELIMS.indexOf(text.charAt(i)) >= 0) {
				index = i;
				break;
			}
		}
		return index;

	}
	
	protected void onSuggestionSelected(T item) {
		suggestionsLocked = true;
		try {
			String text = getText();
			int startIndex = getStartTokenIndex(text);
			int endIndex = getEndTokenIndex(text, startIndex);
			String addText = item.toString();
			if (endIndex >= text.length() || DELIMS.indexOf(text.charAt(endIndex)) < 0) {
				addText += " ";
			}
			replaceText(startIndex, endIndex, addText);
			
		}
		finally {
			suggestionsLocked = false;
		}
	}
	
	protected class SuggestIfNotExist<S> implements Suggest<S> {
		
		Suggest<S> innerSuggest;
		
		SuggestIfNotExist(Suggest<S> innerSuggest) {
			this.innerSuggest = innerSuggest;
		}

		@Override
		public Collection<S> find(String text, int max) {
			HashSet<String> terms = new HashSet<>();
			StringTokenizer stok = new StringTokenizer(getText(), DELIMS);
			while (stok.hasMoreTokens()) terms.add(stok.nextToken());
			Collection<S> innerItems = innerSuggest.find(text, max + terms.size());
			ArrayList<S> outerItems = new ArrayList<>(max);
			for (S item : innerItems) {
				if (terms.contains(item.toString())) continue;
				outerItems.add(item);
				if (outerItems.size() >= max) break;
			}
			return outerItems;
		}
		
	}
}
