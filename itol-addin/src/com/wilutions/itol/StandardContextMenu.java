package com.wilutions.itol;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * Create a context menu with Cut, Copy, Paste. 
 *
 */
public class StandardContextMenu extends ContextMenu {
	
	private MenuItem menuCut;
	private MenuItem menuCopy;
	private MenuItem menuPaste;
	private ResourceBundle resb;
	private Node node;
	private DataFlavor[] acceptedClipboardDataFlavors = {DataFlavor.javaFileListFlavor};

	public StandardContextMenu() {
		resb = Globals.getResourceBundle();
		menuCut = new MenuItem(resb.getString("menuCut"));
		menuCopy = new MenuItem(resb.getString("menuCopy"));
		menuPaste = new MenuItem(resb.getString("menuPaste"));
		
		menuCut.setVisible(false);
		this.getItems().addAll(menuCut, menuCopy, menuPaste);
	}
	
	public StandardContextMenu acceptedClipboardDataFlavors(DataFlavor ... flavors) {
		acceptedClipboardDataFlavors = flavors;
		return this;
	}
	
	public StandardContextMenu showCut(boolean en) {
		menuCut.setVisible(en);
		return this;
	}
	
	public StandardContextMenu showCopy(boolean en) {
		menuCopy.setVisible(en);
		return this;
	}
	
	public StandardContextMenu showPaste(boolean en) {
		menuPaste.setVisible(en);
		return this;
	}
	
	public StandardContextMenu onCopy(EventHandler<ActionEvent> handler) {
		menuCopy.setOnAction(handler);
		return this;
	}

	public StandardContextMenu onPaste(EventHandler<ActionEvent> handler) {
		menuPaste.setOnAction(handler);
		return this;
	}

	@Override
	public void show(Node anchor, double screenX, double screenY) {
		this.node = anchor;
		
		if (screenX < 0 || screenY < 0) {
	        Bounds bounds = node.getBoundsInLocal();
	        Bounds screenBounds = node.localToScreen(bounds);
	        int x = (int) screenBounds.getMinX();
	        int y = (int) screenBounds.getMinY();
	        int width = (int) screenBounds.getWidth();
	        int height = (int) screenBounds.getHeight();
	        screenX = x + width/2;
	        screenY = y + height/2;
		}

		menuPaste.setDisable(true);
		for (DataFlavor flavor : acceptedClipboardDataFlavors) {
			Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (transferable.isDataFlavorSupported(flavor)) {
				menuPaste.setDisable(false);
				break;
			}
		}

		super.show(anchor, screenX, screenY);
	}
	
	public void show(Node anchor) {
		show(anchor, -1, -1);
	}

}
