package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

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
	 * Function used to build the string representation of T.
	 */
	protected Function<T, String> toStringFunction;

	/**
	 * Limit select list to keep auto completion menu small.
	 * Menu does not handle smoothly if it cannot be displayed below the edit box. 
	 */
	private static final int MAX_SUGGESTIONS = 5;
	
	/**
	 * Constructor.
	 * 
	 * @param allItems
	 *            Collection of all items.
	 */
	public DefaultSuggest(Collection<T> allItems) {
		this(allItems, (item) -> item.toString());
	}

	/**
	 * Constructor.
	 * 
	 * @param allItems
	 *            Collection of all items.
	 * @param toStringFunction
	 * 			  Function used to build the string representation of T
	 */
	public DefaultSuggest(Collection<T> allItems, Function<T, String> toStringFunction) {
		this.allItems = allItems != null ? allItems : new ArrayList<T>(0);
		this.toStringFunction = toStringFunction;
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
	public Collection<T> find(String text, int max, Collection<T> ignoreHits) {
		Collection<T> ret = allItems;
		String textLC = text.toLowerCase();
		
		if (textLC.isEmpty()) {
			ret = new ArrayList<T>();
			for (T item : allItems) {
				if (ret.size() == max) break;
				ret.add(item);
			}
		}
		else {
			ArrayList<T> matches = new ArrayList<T>();
			if (ignoreHits != null) {
				for (T t : allItems) {
					if (ignoreHits.contains(t)) continue;
					matches.add(t);
				}
			}
			else {
				matches.addAll(allItems);
			}
			
			Collections.sort(matches, new Comparator<T>() {
				public int compare(T o1, T o2) {
					String s1 = toStringFunction.apply(o1).toLowerCase();
					String s2 = toStringFunction.apply(o2).toLowerCase();
					int p1 = s1.indexOf(textLC);
					int p2 = s2.indexOf(textLC);
					p1 = makeCompareFromPosition(p1);
					p2 = makeCompareFromPosition(p2);
					int cmp = p1 - p2;
					return cmp;
				}
			});
			
			// Return only items that contain the text.
			// Therefore, find the first item that does not contain the text.
			int endIdx = 0;
			for (; endIdx < matches.size(); endIdx++) {
				T item = matches.get(endIdx);
				if (toStringFunction.apply(item).toLowerCase().indexOf(textLC) < 0) {
					break;
				}
			}

			endIdx = Math.min(endIdx, max);

			// Cut the list at the item that does not contain the text.
			ret = matches.subList(0, endIdx);
		}
		
		if (ret.size() > MAX_SUGGESTIONS) {
			ret = new ArrayList<T>(ret).subList(0, MAX_SUGGESTIONS);
		}

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
