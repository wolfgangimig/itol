package com.wilutions.fx.acpl;

import org.controlsfx.control.textfield.CustomTextField;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class AutoCompletionField<T> extends CustomTextField {

    protected StackPane arrowButton;
    protected Region arrow;
    protected ExtractImage<T> extractImage;

	protected SimpleObjectProperty<T> selectedItem = new SimpleObjectProperty<T>();
	protected EventHandler<ActionEvent> onShowSuggestions;
	
	public SimpleObjectProperty<T> selectedItemProperty() {
		return selectedItem;
	}

	public T getSelectedItem() {
		return selectedItem.get();
	}
	
	public void setSelectedItem(T item) {
		selectedItem.set(item);
	}
	
	public void setOnShowSuggestions(EventHandler<ActionEvent> handler) {
		onShowSuggestions = handler;
	}
	
	public AutoCompletionField() {
		
		getStyleClass().add("auto-completion-field"); 
		
        Region arrowRegion = new Region();
        arrowRegion.getStyleClass().addAll("graphic"); 
        StackPane arrowPane = new StackPane(arrowRegion);
        arrowPane.getStyleClass().addAll("arrow"); 
        arrowPane.setOpacity(1.0);
        arrowPane.setCursor(Cursor.DEFAULT);
        
        arrowPane.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
				if (onShowSuggestions != null) {
					onShowSuggestions.handle(new ActionEvent());
				}
			}
        });
	
        super.rightProperty().set(arrowPane);
        
        ImageView imageView = makeImageView(null);
		imageView.setFitWidth(16);
		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);
		imageView.setCache(true);
        super.leftProperty().set(imageView);
        
        selectedItem.addListener(new ChangeListener<T>() {
			public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
				if (newValue != null && extractImage != null) {
					Image image = extractImage.getImage(newValue);
					if (image != null) {
						imageView.setImage(image);
					}
				}
			}
        });
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

	public ExtractImage<T> getExtractImage() {
		return extractImage;
	}

	public void setExtractImage(ExtractImage<T> extractImage) {
		this.extractImage = extractImage;
	}

}
