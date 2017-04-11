package com.wilutions.fx.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
	
	/**
	 * Copy resource into temporary file.
	 * @param clazz
	 * @param resId
	 * @return
	 */
	public File getFile(Class<?> clazz, String resId) {
		int p = resId.lastIndexOf('.');
		String fname = p != -1 ? resId.substring(0, p) : resId;
		String ext = p != -1 ? resId.substring(p) : ".tmp";
		File file = null;
		try { 
			file = File.createTempFile(fname, ext);
			try (InputStream istream = getResourceAsStream(clazz, resId); 
				 OutputStream ostream = new FileOutputStream(file)) {
				byte[] buf = new byte[10*1000];
				int len = -1;
				while ((len = istream.read(buf)) != -1) {
					ostream.write(buf, 0, len);
				}
			}
			file.deleteOnExit();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return file;
	}
}
