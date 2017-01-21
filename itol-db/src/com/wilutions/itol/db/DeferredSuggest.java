package com.wilutions.itol.db;

import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Find suggestions for a given text after a given delay has passed.
 * 
 * @param <T>
 *            Item type
 */
public class DeferredSuggest<T> {
	
	private long deferMillis;
	private AtomicInteger suggestionCounter = new AtomicInteger();
	private Suggest<T> suggest;
	
	public DeferredSuggest(Suggest<T> suggest, long deferMillis) {
		this.deferMillis = deferMillis;
		this.suggest = suggest;
	}
	
	/**
	 * Find suggestions for given text.
	 * Starts a timer that waits deferMillis milliseconds before it calls the suggest interface. 
	 * Right before the suggest interface is invoked, it is checked, whether another thread has 
	 * already invoked this function. In this case, the returned Future object is cancelled. 
	 * Otherwise the suggest interface is invoked and the Future object is completed with the results.
	 * 
	 * @param text
	 *            Text
	 * @param max
	 *            Maximum number of items to return.
	 * @param ignoreHits 
	 * 			  Return only items that do not exist in this collection. Can be null.         
	 * @return Future object that is completed, when the results are available. If the search is cancelled, the Future object is completed exceptionally.
	 */
	public CompletableFuture<Collection<T>> find(String text, int max, Collection<T> ignoreHits) {
		CompletableFuture<Collection<T>> ret = new CompletableFuture<>();
		final int suggestionId = suggestionCounter.incrementAndGet();
		Timeline timer = new Timeline(new KeyFrame(Duration.millis(deferMillis), (event) -> {
			if (suggestionId == suggestionCounter.get()) {
				CompletableFuture.supplyAsync(() -> {
					Collection<T> items = suggest.find(text, max, ignoreHits);
					return items;
				}).thenApply((items) -> {
					ret.complete(items);
					return null;
				});
			}
			else {
				ret.completeExceptionally(new CancellationException());
			}
		}));
		timer.setCycleCount(1);
		timer.play();
		return ret;
	}
}
