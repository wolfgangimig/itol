package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * ResourceBundle implementation.
 * This class tries to find files resourceId_language_country.properties in the current directory,
 * e.g. com.wilutions.com.jiraddin.res_de.properties.
 * If this fails, the files tried to be found as resources in the classpath.
 * File content has to be UTF-8 encoded.
 */
public class ResourceBundleNoThrow extends ResourceBundle {

	private final ArrayList<ResourceBundle> innerBundles = new ArrayList<ResourceBundle>();

	public ResourceBundleNoThrow() {
	}

	/**
	 * Adds given bundle at the head of the list.
	 * Resource IDs are tried to find in this bundle first.
	 * @param b ResourceBundle
	 */
	public void addBundle(ResourceBundle b) {
		this.innerBundles.add(0, b);
	}

	/**
	 * Add resource file identified by resourceFileId at the heade of the list.
	 * @param resourceFileId Resource ID, e.g. com.wilutions.jiraddin.res
	 * @param classLoader
	 */
	public void addBundle(String resourceFileId, ClassLoader classLoader) {

		Locale locale = Locale.getDefault();
		ResourceBundle resb = null;
		InputStream rstream = null;

		try {

			// Try to find resource file in current directory. 
			File resourceFile = new File(".", resourceFileId + "_" + locale.getLanguage() + "_" + locale.getCountry() + ".properties");
			if (!resourceFile.exists()) {
				resourceFile = new File(".", resourceFileId + "_" + locale.getLanguage() + ".properties");
				if (!resourceFile.exists()) {
					resourceFile = new File(".", resourceFileId + "_en.properties");
				}
			}

			if (resourceFile.exists()) {
				try {
					rstream = new FileInputStream(resourceFile);
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}

			// Try to find resource file in JAR
			if (rstream == null) {
				resourceFileId = resourceFileId.replace(".", "/");
				rstream = classLoader.getResourceAsStream(resourceFileId + "_" + locale.getLanguage() + "_" + locale.getCountry() + ".properties");
				if (rstream == null) {
					rstream = classLoader.getResourceAsStream(resourceFileId + "_" + locale.getLanguage() + ".properties");
					if (rstream == null) {
						rstream = classLoader.getResourceAsStream(resourceFileId + "_en.properties");
					}
				}
			}
			
			if (rstream != null) {
				try (Reader rd = new InputStreamReader(rstream, "UTF-8")) {
					resb = new PropertyResourceBundle(rd);
					rstream = null;
				}
			}

			if (resb != null) {
				addBundle(resb);
			}

		}
		catch (Exception e) {
			e.printStackTrace(); 
		} 
		finally {
			if (rstream != null) {
				try {
					rstream.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Override
	protected Object handleGetObject(String key) {
		Object obj = null;
		for (ResourceBundle inner : innerBundles) {
			try {
				obj = inner.getObject(key);
				break;
			} catch (MissingResourceException ignored) {
			}
		}
		if (obj == null) {
			obj = key;
		}
		return obj;
	}

	@Override
	public Enumeration<String> getKeys() {
		HashSet<String> ret = new HashSet<String>();
		for (ResourceBundle inner : innerBundles) {
			for (Enumeration<String> en = inner.getKeys(); en.hasMoreElements();) {
				ret.add(en.nextElement());
			}
		}
		return Collections.enumeration(ret);
	}

}
