package com.wilutions.itol.db;

import java.util.Collection;

/**
 * Callback interface to find suggestions for a given text.
 * 
 * @param <T>
 *            Item type
 */
public interface Suggest<T> {
	/**
	 * Find suggestions for given text.
	 * 
	 * @param text
	 *            Text
	 * @param max
	 *            Maximum number of items to return.
	 * @param ignoreHits 
	 * 			  Return only items that do not exist in this collection. Can be null.         
	 * @return Collection of suggestions to be displayed in the list view.
	 */
	public Collection<T> find(String text, int max, Collection<T> ignoreHits);
}
