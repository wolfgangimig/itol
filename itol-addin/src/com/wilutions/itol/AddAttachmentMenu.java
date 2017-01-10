package com.wilutions.itol;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.fx.util.WindowsRecentFolder;
import com.wilutions.itol.db.Attachment;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Creates the menu items for adding attachments. 
 *
 */
public class AddAttachmentMenu {
	
	private Window stage;
	private Attachments observableAttachments;
	private Function<Attachment, Void> onAddAttachment;
	private ResourceBundle resb = Globals.getResourceBundle();
	private static WindowsRecentFolder windowsRecentFolder = new WindowsRecentFolder();
	private static Logger log = Logger.getLogger("AddAttachmentMenu");

	public AddAttachmentMenu(Window stage, Attachments observableAttachments) {
		this(stage, observableAttachments, (attachtment) -> null);
	}
	
	public AddAttachmentMenu(Window stage, Attachments observableAttachments, Function<Attachment, Void> onAddAttachment) {
		this.stage = stage;
		this.observableAttachments = observableAttachments;
		this.onAddAttachment = onAddAttachment;
	}

	public List<MenuItem> create() {
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		makeMenuItemsForClipboardFiles(menuItems);
		makeMenuItemsForRecentFiles(menuItems);
		makeMenuItemsForFileChooser(menuItems);
		return menuItems;
	}

	public CustomMenuItem makeSeparator(String resId) {
		Text text = new Text(resb.getString(resId));
		text.setStyle("-fx-font-style: italic; -fx-font-weight: bold;");
		CustomMenuItem miSeparatorRecent = new CustomMenuItem(text);
		miSeparatorRecent.setHideOnClick(false);
		return miSeparatorRecent;
	}
	
	private void makeMenuItemsForFileChooser(List<MenuItem> menuItems) {
		menuItems.add(new SeparatorMenuItem()); 
		
		MenuItem menuItem = new MenuItem(resb.getString("bnAddAttachment.menu.fileChooser"));
		menuItem.setOnAction((e) -> {
			try {
				FileChooser fileChooser = new FileChooser();
				List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
				if (selectedFiles != null) {
					for (File file : selectedFiles) {
						Attachment att = MailAttachmentHelper.createFromFile(file);
						observableAttachments.add(att);
						onAddAttachment.apply(att);
					}
				}
			}
			catch (Throwable ex) {
				log.log(Level.WARNING, "File chooser failed.", ex);
			}
		});
		menuItems.add(menuItem);
	}

	@SuppressWarnings("unchecked")
	private void makeMenuItemsForClipboardFiles(List<MenuItem> menuItems) {
		try {
			Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (transferable != null) {
				
				java.awt.Image clipboardImage = null;
				if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
					clipboardImage = (java.awt.Image) transferable.getTransferData(DataFlavor.imageFlavor);
				}

				List<File> clipboardFiles = null;
				if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					clipboardFiles = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
				}
				
				if (clipboardImage != null || !clipboardFiles.isEmpty()) {
					
					CustomMenuItem miSeparatorCliboard = makeSeparator("bnAddAttachment.menu.clipboard");
					menuItems.add(miSeparatorCliboard);

					if (clipboardImage != null) {
						BufferedImage thumbnailImage = ThumbnailHelper.makeThumbnailImage(clipboardImage);
						Image image = SwingFXUtils.toFXImage((BufferedImage)thumbnailImage, null);
						CustomMenuItem miImage = new CustomMenuItem(new ImageView(image));
						miImage.setOnAction((e) -> {
							List<Attachment> attachments = AttachmentTableViewHandler.paste(observableAttachments);
							for (Attachment attachment : attachments) onAddAttachment.apply(attachment);
						});
						menuItems.add(miImage);
					}

					if (clipboardFiles != null) {
						int nbOfFiles = clipboardFiles.size();
						if (nbOfFiles == 1) {
							makeBnAddAttachmentMenuItemsFiles(menuItems, clipboardFiles);
						}
						else {
							MenuItem mi = new MenuItem();
							String miText;
							try {
								miText = MessageFormat.format(resb.getString("bnAddAttachment.menu.pasteNbOfFiles"), nbOfFiles);
							}
							catch (Exception e) {
								// Resource file corrupt
								miText = "Paste " + nbOfFiles + " files";
							}
							mi.setText(miText);
							mi.setOnAction((e) -> {
								List<Attachment> attachments = AttachmentTableViewHandler.paste(observableAttachments);
								for (Attachment attachment : attachments) onAddAttachment.apply(attachment);
							});
							menuItems.add(mi);
						}
					}
				}
				
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, "Failed to access clipboard.", e);
		}
	}

	private void makeMenuItemsForRecentFiles(List<MenuItem> menuItems) {
		CustomMenuItem miSeparatorRecent = makeSeparator("bnAddAttachment.menu.recentFiles");
		menuItems.add(miSeparatorRecent);
		
		List<File> recentFiles = windowsRecentFolder.getFiles(10, WindowsRecentFolder.FILES);
		makeBnAddAttachmentMenuItemsFiles(menuItems, recentFiles);
	}

	private void makeBnAddAttachmentMenuItemsFiles(List<MenuItem> menuItems, List<File> recentFiles) {
		for (File file : recentFiles) {
			MenuItem mi = makeBnAddAttachmentMenuItemFile(file);
			menuItems.add(mi);
		}
	}

	private MenuItem makeBnAddAttachmentMenuItemFile(File file) {
		Image fxImage = FileIconCache.getFileIcon(file);
		MenuItem mi = new MenuItem();
		mi.setGraphic(new ImageView(fxImage));
		mi.setText(file.getName());
		mi.setOnAction((e) -> {
			Attachment att = MailAttachmentHelper.createFromFile(file);
			observableAttachments.add(att);
			onAddAttachment.apply(att);
		});
		return mi;
	}


}
