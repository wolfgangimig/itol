package com.wilutions.itol;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleNoThrow extends ResourceBundle {
	
	private final ResourceBundle inner;
	
	public ResourceBundleNoThrow(ResourceBundle inner) {
		this.inner = inner;
	}

	@Override
	protected Object handleGetObject(String key) {
		Object obj = null;
		try {
			obj = inner.getObject(key);
		}
		catch (MissingResourceException ignored) {
		}
		if (obj == null) {
			obj = key;
		}
		return obj;
	}

	@Override
	public Enumeration<String> getKeys() {
		return inner.getKeys();
	}

}
