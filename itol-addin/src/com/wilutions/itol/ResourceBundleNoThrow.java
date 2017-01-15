package com.wilutions.itol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleNoThrow extends ResourceBundle {

	private final ArrayList<ResourceBundle> innerBundles = new ArrayList<ResourceBundle>();

	public ResourceBundleNoThrow() {
	}

	public void addBundle(ResourceBundle b) {
		this.innerBundles.add(0,b);
	}
	
	public void addBundle(String resourceId, ClassLoader classLoader) {
		ResourceBundle resb = ResourceBundle.getBundle(resourceId, Locale.getDefault(), classLoader);
		addBundle(resb);
	}

	@Override
	protected Object handleGetObject(String key) {
		Object obj = null;
		for (ResourceBundle inner : innerBundles) {
			try {
				obj = inner.getObject(key);
				break;
			}
			catch (MissingResourceException ignored) {
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
			for (Enumeration<String> en = inner.getKeys(); en.hasMoreElements(); ) {
				ret.add(en.nextElement());
			}
		}
		return Collections.enumeration(ret);
	}

}
