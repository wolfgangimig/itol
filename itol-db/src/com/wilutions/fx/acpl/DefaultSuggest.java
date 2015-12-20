package com.wilutions.fx.acpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Default implementation for interface Suggest.
 * 
 * @param <T>
 *            Item type
 */
public class DefaultSuggest<T> implements Suggest<T> {

	/**
	 * All items. Passed in the constructor.
	 */
	protected Collection<T> allItems;

	/**
	 * Constructor.
	 * 
	 * @param allItems
	 *            Collection of all items.
	 */
	public DefaultSuggest(Collection<T> allItems) {
		this.allItems = allItems;
	}

	/**
	 * Constructor.
	 */
	public DefaultSuggest() {
	}

	/**
	 * Find suggestions for given text. All items are returned that contain the
	 * given text in their return value of toString. Those items that start with
	 * the given text are ordered at the beginning of the returned collection.
	 * 
	 * @param text
	 *            Text
	 * @param max
	 *            Maximum number of items to return.
	 * @return Collection of items.
	 */
	public Collection<T> find(String text, int max) {
		ArrayList<T> matches = new ArrayList<T>(allItems);
		String textLC = text.toLowerCase();

		Collections.sort(matches, new Comparator<T>() {
			public int compare(T o1, T o2) {
				String s1 = o1.toString().toLowerCase();
				String s2 = o2.toString().toLowerCase();
				int cmp = 0;
				if (!textLC.isEmpty()) {
					int p1 = s1.indexOf(textLC);
					int p2 = s2.indexOf(textLC);
					p1 = makeCompareFromPosition(p1);
					p2 = makeCompareFromPosition(p2);
					cmp = p1 - p2;
				}
				if (cmp == 0) {
					cmp = s1.compareTo(s2);
				}
				return cmp;
			}
		});

		// Return only items that contain the text.
		// Therefore, find the first item that does not contain the text.
		int endIdx = 0;
		for (; endIdx < matches.size(); endIdx++) {
			T item = matches.get(endIdx);
			if (!item.toString().toLowerCase().startsWith(textLC)) {
				break;
			}
		}

		endIdx = Math.min(endIdx, max);

		// Cut the list at the item that does not contain the text.
		Collection<T> ret = matches.subList(0, endIdx);

		return ret;
	}

	private int makeCompareFromPosition(int p) {
		if (p == 0) {
			// item starts with the given text.
			// This item should be positioned at the beginning of the list.
		}
		else if (p > 0) {
			// The item contains the text but does not start with it.
			// Set p=1 since it does not matter where the text is found.
			p = 1;
		}
		else if (p < 0) {
			// The item does not contain the text.
			// Move this item at the end of the list.
			p = Integer.MAX_VALUE;
		}
		return p;
	}

}
