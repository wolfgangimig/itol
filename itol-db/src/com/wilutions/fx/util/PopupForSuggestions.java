package com.wilutions.fx.util;

import java.util.Collection;

import com.wilutions.itol.db.Suggest;

import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;

public class PopupForSuggestions<T> extends Popup {

	private ListView<T> lvSuggestions = new ListView<T>();
	private Suggest<T> suggest;
	private ItemSelected<T> itemSelected;
	private Node owner;
	
	public interface ItemSelected<T> {
		public void OnItemSelected(T item);
	}

	public PopupForSuggestions(Node owner, Suggest<T> suggest, ItemSelected<T> itemSelected) {
		this.owner = owner;
		this.suggest = suggest;
		this.itemSelected = itemSelected;
		
		setAutoHide( true );
        setHideOnEscape( true );
        setAutoFix( true );
        getContent().add(lvSuggestions);

		lvSuggestions.setOnKeyPressed((keyEvent) -> {
			KeyCode code = keyEvent.getCode();
			switch (code) {
			case TAB:
			case ENTER: 
				processSelectedItem();
				keyEvent.consume();
				break;
			case ESCAPE:
				hide();
				keyEvent.consume();
				break;
			default:
			}
		});
		
		lvSuggestions.setOnMouseClicked((event) -> {
			if (event.getClickCount() == 2) {
				processSelectedItem();
			}
		});
	}
	
	public void processSelectedItem() {
		T item = lvSuggestions.getSelectionModel().getSelectedItem();
		if (item != null) {
			this.itemSelected.OnItemSelected(item);
			hide();
		}
	}

	public void updateSuggestions(String filter) {
		Collection<T> suggestions = suggest.find(filter, 10);
		lvSuggestions.setItems(FXCollections.observableArrayList(suggestions));
		if (suggestions.isEmpty()) {
			if (isShowing()) hide();
		}
		else {
			Bounds bounds = owner.getBoundsInLocal();
			Bounds screenBounds = owner.localToScreen(bounds);
			double anchorX = screenBounds.getMinX();
			double anchorY = screenBounds.getMaxY();
			double width = bounds.getWidth()-4;
			lvSuggestions.setMinWidth(width);
			lvSuggestions.setMaxWidth(width);
			lvSuggestions.setPrefWidth(width);
			int n = suggestions.size();
			double height = 24 * n - (n-2);
			//lvSuggestions.setMinHeight(height);
			//lvSuggestions.setMaxHeight(height);
			lvSuggestions.setPrefHeight(height);
			lvSuggestions.requestLayout();
			lvSuggestions.getSelectionModel().select(0);
			if (!isShowing()) {
				show(owner, anchorX, anchorY);
			}
		}
	}


}
