package com.wilutions.fx.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.image.Image;

public class ResourceLoader {
	
	private static Logger log = Logger.getLogger("ResourceLoader");
	private Map<String, Image> images = new HashMap<>();
	
	public ResourceLoader() {
	}

	public InputStream getResourceAsStream(Class<?> clazz, String resId) {
		InputStream ret = null;
		try {
			ret = clazz.getResourceAsStream(resId);
		}
		catch (Exception e) {
			log.log(Level.WARNING, "Failed to load resource from class=" + clazz + ", resId=" + resId, e);
		}
		if (ret == null) {
			log.log(Level.WARNING, "Missing resource from class=" + clazz + ", resId=" + resId);
			ret = new ByteArrayInputStream(new byte[0]);
		}
		return ret;
	}
	
	/**
	 * Get an image from the resources.
	 * This function can only be called in the JavaFX UI thread.
	 * @param clazz Class object used to load the resource.
	 * @param resId Resource ID.
	 * @return Image
	 */
	public Image getImage(Class<?> clazz, String resId) {
		// Allow access to UI thread only. This allows to access the map un-synchronized.
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("Wrong thread.");
		}
		Image ret = images.get(resId);
		if (ret == null) {
			ret = new Image(getResourceAsStream(clazz, resId));
			images.put(resId, ret);
		}
		return ret;
	}
}
