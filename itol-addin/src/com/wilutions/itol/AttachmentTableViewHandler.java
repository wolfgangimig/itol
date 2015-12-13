package com.wilutions.itol;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

import com.wilutions.com.IDispatch;
import com.wilutions.itol.db.Attachment;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.Selection;
import com.wilutions.mslib.outlook._Explorer;

public class AttachmentTableViewHandler {

	public static void apply(AttachmentHelper attachmentHelper, TableView<Attachment> table) {

		ResourceBundle resb = Globals.getResourceBundle();

		TableColumn<Attachment, String> iconColumn = new TableColumn<>("");
		final int iconColumnWidth = 24;
		iconColumn.setPrefWidth(iconColumnWidth);
		// iconColumn.setMaxWidth(iconColumnWidth);
		// iconColumn.setMinWidth(iconColumnWidth);
		iconColumn.setCellValueFactory(new PropertyValueFactory<Attachment, String>("fileName"));
		iconColumn.setCellFactory(new Callback<TableColumn<Attachment, String>, TableCell<Attachment, String>>() {

			@Override
			public TableCell<Attachment, String> call(TableColumn<Attachment, String> item) {
				TableCell<Attachment, String> cell = new TableCell<Attachment, String>() {
					@Override
					protected void updateItem(String fileName, boolean empty) {
						super.updateItem(fileName, empty);
						if (fileName != null) {
							Image fxImage = FileIconCache.getFileIcon(new File(fileName));
							if (fxImage != null) {
								ImageView imageView = new ImageView(fxImage);
								setGraphic(imageView);
							}
						}
					}
				};
				return cell;
			}

		});
		iconColumn.setComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return AttachmentHelper.getFileExt(o1).compareToIgnoreCase(AttachmentHelper.getFileExt(o2));
			}
		});

		TableColumn<Attachment, String> fileNameColumn = new TableColumn<>("Name");
		fileNameColumn.setCellValueFactory(new PropertyValueFactory<Attachment, String>("fileName"));
		fileNameColumn.setCellFactory(new Callback<TableColumn<Attachment, String>, TableCell<Attachment, String>>() {

			@Override
			public TableCell<Attachment, String> call(TableColumn<Attachment, String> item) {
				TableCell<Attachment, String> cell = new TableCell<Attachment, String>() {
					@Override
					protected void updateItem(String fileName, boolean empty) {
						super.updateItem(fileName, empty);
						if (fileName != null) {
							String str = AttachmentHelper.getFileName(fileName);
							setText(str);
						}
					}
				};
				return cell;
			}

		});
		fileNameColumn.setComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return AttachmentHelper.getFileName(o1).compareToIgnoreCase(AttachmentHelper.getFileName(o2));
			}
		});

		// final int fileNameColumnWidth = 150;
		// fileNameColumn.setPrefWidth(fileNameColumnWidth);
		// fileNameColumn.setMinWidth(fileNameColumnWidth);

		TableColumn<Attachment, Long> contentLengthColumn = new TableColumn<>("Size");
		contentLengthColumn.setCellValueFactory(new PropertyValueFactory<Attachment, Long>("contentLength"));
		contentLengthColumn.setCellFactory(new Callback<TableColumn<Attachment, Long>, TableCell<Attachment, Long>>() {

			@Override
			public TableCell<Attachment, Long> call(TableColumn<Attachment, Long> item) {
				TableCell<Attachment, Long> cell = new TableCell<Attachment, Long>() {
					@Override
					protected void updateItem(Long contentLength, boolean empty) {
						super.updateItem(contentLength, empty);
						if (contentLength != null) {
							String str = AttachmentHelper.makeAttachmentSizeString(contentLength);
							setText(str);
						}
					}
				};
				cell.setStyle("-fx-alignment: CENTER-RIGHT;");
				return cell;
			}

		});
		final int contentLengthColumnWidth = 100;
		contentLengthColumn.setPrefWidth(contentLengthColumnWidth);
		// contentLengthColumn.setMaxWidth(contentLengthColumnWidth);
		// contentLengthColumn.setMinWidth(contentLengthColumnWidth);

		fileNameColumn.prefWidthProperty().bind(
				table.widthProperty().subtract(iconColumnWidth + contentLengthColumnWidth));

		table.getColumns().clear();
		table.getColumns().add(iconColumn);
		table.getColumns().add(fileNameColumn);
		table.getColumns().add(contentLengthColumn);

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setPlaceholder(new Label(resb.getString("tabAttachments.emptyMessage")));

		// //////////////////
		// Drag&Drop

		// Java does not support D&D from Outlook.
		// Outlook sends a special data format that Java does not
		// understand.

		// If an item is dragged from Outlook, Java 8 runs in an
		// "java.io.IOException: no native data was transfered".
		// This exception occurs deep inside the toolkit and cannot
		// be caught.

		table.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					// D&D from Outlook also enteres this block,
					// although Java does not understand the format.
					// In contrast to this, db.hasFiles() is false
					// for Outlook D&D inside the drop handler.
					event.acceptTransferModes(TransferMode.COPY);
				}
				else {
					event.consume();
				}
			}
		});

		// Dropping over surface
		table.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					List<File> files = db.getFiles();
					for (File file : files) {
						Attachment att = AttachmentHelper.createFromFile(file);
						table.getItems().add(att);
					}
				}
				else {
					
					// Because db.hasFiles() was true in onDragOver
					// and it is false here, we assume that Outlook
					// data is dropped.
					// Here, all selected items in the explorer are
					// dropped.
					
					// Mail attachments are sent as ordinary file drops. 

					try {
						_Explorer explorer = Globals.getThisAddin().getApplication().ActiveExplorer();
						Selection selection = explorer.getSelection();
						int count = selection.getCount();
						for (int i = 1; i <= count; i++) {
							IDispatch item = selection.Item(i);
							if (item.is(MailItem.class)) {
								MailItem mailItem = item.as(MailItem.class);
								Attachment att = attachmentHelper.makeMailAttachment(new IssueMailItemImpl(mailItem));
								table.getItems().add(att);
							}
						}
					}
					catch (Throwable e) {
						e.printStackTrace();
					}
				}
				event.setDropCompleted(success);
				event.consume();
			}

		});

	}

}
