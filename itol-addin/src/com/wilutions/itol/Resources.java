package com.wilutions.itol;

import com.wilutions.fx.util.ResourceLoader;

import javafx.scene.image.Image;

public class Resources {
	
	public static Resources getInstance() {
		return instance;
	}

	protected Resources() {
	}

	private static Resources instance = new Resources();
	private ResourceLoader rloader = new ResourceLoader();
	
	public Image getDeleteImage() {
		return rloader.getImage(Resources.class, "delete.png");
	}

	public Image getDeleteDisabledImage() {
		return rloader.getImage(Resources.class, "delete-disabled.png");
	}

	public Image getAttachmentImage() {
		return rloader.getImage(Resources.class, "Attachment32.png");
	}
	

}
