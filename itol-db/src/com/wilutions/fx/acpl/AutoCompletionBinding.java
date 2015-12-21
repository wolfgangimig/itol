package com.wilutions.fx.acpl;

import java.util.ArrayList;

import javafx.scene.control.ComboBox;

public class AutoCompletionBinding<T> {
	private AutoCompletionControl<T> control;
	private String recentCaption;
	private String suggestionsCaption;
	private ArrayList<T> recentItems;
	private Suggest<T> suggest;
	private ExtractImage<T> extractImage;
	private boolean lockChangeEvent;
	
	public String getRecentCaption() {
		return recentCaption;
	}
	public void setRecentCaption(String recentCaption) {
		this.recentCaption = recentCaption;
	}
	public String getSuggestionsCaption() {
		return suggestionsCaption;
	}
	public void setSuggestionsCaption(String suggestionsCaption) {
		this.suggestionsCaption = suggestionsCaption;
	}
	public ArrayList<T> getRecentItems() {
		return recentItems;
	}
	public void setRecentItems(ArrayList<T> recentItems) {
		this.recentItems = recentItems;
	}
	public Suggest<T> getSuggest() {
		return suggest;
	}
	public void setSuggest(Suggest<T> suggest) {
		this.suggest = suggest;
	}
	public ExtractImage<T> getExtractImage() {
		return extractImage;
	}
	public void setExtractImage(ExtractImage<T> extractImage) {
		this.extractImage = extractImage;
	}
	public boolean isLockChangeEvent() {
		return lockChangeEvent;
	}
	public void setLockChangeEvent(boolean lockChangeEvent) {
		this.lockChangeEvent = lockChangeEvent;
	}
	public AutoCompletionControl<T> getControl() {
		return control;
	}
	public void setControl(AutoCompletionControl<T> control) {
		this.control = control;
	}
}

