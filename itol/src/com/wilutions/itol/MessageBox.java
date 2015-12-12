package com.wilutions.itol;

import java.util.ResourceBundle;

import com.wilutions.com.AsyncResult;

public class MessageBox {
	
	public static void error(Object owner, String text, AsyncResult<Integer> asyncResult) {
		ResourceBundle resb = Globals.getResourceBundle();
		String title = resb.getString("MessageBox.title.error");
		com.wilutions.joa.fx.MessageBox.show(owner, title, text, asyncResult);
	}
	
	public static void info(Object owner, String text, AsyncResult<Integer> asyncResult) {
		ResourceBundle resb = Globals.getResourceBundle();
		String title = resb.getString("MessageBox.title.info");
		com.wilutions.joa.fx.MessageBox.show(owner, title, text, asyncResult);
	}
}
