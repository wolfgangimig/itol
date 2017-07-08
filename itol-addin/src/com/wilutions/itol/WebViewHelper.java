package com.wilutions.itol;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebViewHelper {

	private final static Logger log = Logger.getLogger("WebViewHelper");

	public static void addClickHandlerToWebView(WebView webView, ShowAttachmentHelper showAttachmentHelper) {
		
		WebEngine webEngine = webView.getEngine();
		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

			@Override
			public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
		        if (newValue == Worker.State.SUCCEEDED) {

		        	EventListener listener = new EventListener() {
						@Override
						public void handleEvent(org.w3c.dom.events.Event ev) {
							EventTarget target = ev.getCurrentTarget();
							String type = ev.getType();
							if (type.equals("click")) {
								ev.preventDefault();
								
								Platform.runLater(() -> {
									String href = ((Element)target).getAttribute("href");
									if (href != null && !href.isEmpty()) {
										showAttachmentHelper.showAttachment(href);
									}
								});
							}
						}
		            };
	
			        // Bind click-event listeners only once.
		            bindClickListenersToAnchors(listener);
		        }
				
			}

			private void bindClickListenersToAnchors(EventListener listener) {
				Document doc = webEngine.getDocument();
				NodeList anchors = doc.getElementsByTagName("a");
				for (int i=0; i<anchors.getLength(); i++) {
					EventTarget elm = ((EventTarget)anchors.item(i));
				    elm.addEventListener("click", listener, false);
				}
			}
		});

	}
	

}
