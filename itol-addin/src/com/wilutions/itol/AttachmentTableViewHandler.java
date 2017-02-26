package com.wilutions.itol;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.com.IDispatch;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.Default;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.ProgressCallbackFactory;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.Selection;
import com.wilutions.mslib.outlook._Explorer;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

public class AttachmentTableViewHandler {
	
	private final static Logger log = Logger.getLogger("AttachmentTableViewHandler");
	
	@SuppressWarnings("unchecked")
	public static void apply(MailAttachmentHelper attachmentHelper, TableView<Attachment> table, Attachments observableAttachments, ProgressCallbackFactory progressCallbackFactory) {
		long t1 = System.currentTimeMillis();
		table.setItems(observableAttachments.getObservableList());

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
				return MailAttachmentHelper.getFileExt(o1).compareToIgnoreCase(MailAttachmentHelper.getFileExt(o2));
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
							Attachment att = (Attachment) getTableRow().getItem();
							if (att != null) { // att is null when table.getItems() is modified in IssueHtmlEditor - why?
								String style = att.getId().isEmpty() ? "-fx-font-weight: bold;" : "fx-font-weight: normal;";
								setStyle(style);
								String str = MailAttachmentHelper.getFileName(fileName);
								setText(str);
							}
						}
					}
				};
				return cell;
			}

		});
		fileNameColumn.setComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return MailAttachmentHelper.getFileName(o1).compareToIgnoreCase(MailAttachmentHelper.getFileName(o2));
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
							String str = MailAttachmentHelper.makeAttachmentSizeString(contentLength);
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


		TableColumn<Attachment, Date> lastModifiedColumn = new TableColumn<>("Date");
		lastModifiedColumn.setCellValueFactory(new PropertyValueFactory<Attachment, Date>("lastModified"));
		lastModifiedColumn.setCellFactory(new Callback<TableColumn<Attachment, Date>, TableCell<Attachment, Date>>() {

			@Override
			public TableCell<Attachment, Date> call(TableColumn<Attachment, Date> item) {
				TableCell<Attachment, Date> cell = new TableCell<Attachment, Date>() {
					@Override
					protected void updateItem(Date lastModified, boolean empty) {
						super.updateItem(lastModified, empty);
						if (lastModified != null) {
							String str = DateFormat.getDateTimeInstance().format(lastModified);
							setText(str);
						}
					}
				};
				cell.setStyle("-fx-alignment: CENTER-RIGHT;");
				return cell;
			}

		});
		final int lastModifiedColumnWidth = 150;
		lastModifiedColumn.setPrefWidth(lastModifiedColumnWidth);
		
		fileNameColumn.prefWidthProperty()
				.bind(table.widthProperty().subtract(iconColumnWidth + contentLengthColumnWidth + lastModifiedColumnWidth + 2));
		
		table.getColumns().clear();
		table.getColumns().addAll(iconColumn, fileNameColumn, lastModifiedColumn, contentLengthColumn);

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setPlaceholder(new Label(resb.getString("tabAttachments.emptyMessage")));
		
		lastModifiedColumn.setSortType(TableColumn.SortType.DESCENDING);
		table.getSortOrder().add(lastModifiedColumn);

		// table.setOnKeyPressed(new EventHandler<KeyEvent>() {
		// @Override
		// public void handle(KeyEvent event) {
		// if (event.getCode() == KeyCode.V && event.isControlDown()) {
		// Clipboard cb = Clipboard.getSystemClipboard();
		// System.out.println("clipboard: image=" + cb.hasImage() +
		// ", files=" + cb.hasFiles() +
		// ", html=" + cb.hasHtml() +
		// ", rtf=" + cb.hasRtf() +
		// ", string=" + cb.hasString());
		// if (cb.hasImage()) {
		// bild wird falsch dargestellt.
		// Image image = cb.getImage();
		// ImageView iv = new ImageView();
		// iv.setImage(image);
		//
		// VBox vbox = new VBox();
		// vbox.getChildren().add(iv);
		// Scene scene = new Scene(vbox);
		// Stage stage = new Stage();
		// stage.setScene(scene);
		// stage.show();
		//
		// Attachment att = attachmentHelper.createFromImage(image, "");
		// table.getItems().add(att);
		// }
		//
		// }
		// }
		// });

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
//				System.out.println("clipboard: image=" + db.hasImage() + ", files=" + db.hasFiles() + ", html="
//						+ db.hasHtml() + ", rtf=" + db.hasRtf() + ", string=" + db.hasString());
				if (db.hasImage()) {

				}
				else if (db.hasFiles()) {
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
						Attachment att = MailAttachmentHelper.createFromFile(file);
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
		
		// Render preview in tooltip for image attachments.
		table.setRowFactory(tableView -> {
		    final TableRow<Attachment> row = new TableRow<>();
	        Tooltip tooltip = new Tooltip();
	        AtomicBoolean mouseEntered = new AtomicBoolean();
	        AtomicReference<Image> refImage = new AtomicReference<>();

		    row.setOnMouseEntered((event) -> {
		    	mouseEntered.set(true);
		    	BackgTask.run(() -> {
			        Attachment attachment = row.getItem();
			        if (attachment != null) {
			        	String thumbnailUrl = attachment.getThumbnailUrl();
			        	if (!Default.value(thumbnailUrl).isEmpty()) {
			        		if (refImage.get() == null) {
			        			try {
			        				// Use download function since Image constructor does not follow redirections.
			        				if (!thumbnailUrl.startsWith(MailAttachmentHelper.FILE_URL_PREFIX)) {
										String fpath = Globals.getIssueService().downloadAttachment(thumbnailUrl, progressCallbackFactory.createProgressCallback("Download thumbnail"));
										File thumbnailFile = new File(fpath);
										thumbnailUrl = thumbnailFile.toURI().toString();
										attachment.setThumbnailUrl(thumbnailUrl);
			        				}
				        			Image image = new Image(thumbnailUrl);
				        			refImage.set(image);
								} catch (Exception e) {
									log.log(Level.WARNING, "Failed to load thumbnail for attachment=" + attachment.getFileName(), e);
								}
			        		}
			        		
			        		if (refImage.get() != null) {
			        			Platform.runLater(() -> {
				        			if (mouseEntered.get()) {
				        				if (tooltip.getGraphic() == null) {
				        					tooltip.setGraphic(new ImageView(refImage.get()));
				        				}
				        				tooltip.show(table, event.getScreenX() + 50, event.getScreenY());
				        			}
			        			});
			        		}
			        	}
			        }
		    	});
		    });
		    row.setOnMouseExited((event) -> {
		    	mouseEntered.set(false);
		    	tooltip.hide();
		    });

		    return row;
		});
		
		
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] apply(observableAttachments=" + observableAttachments + ")");
	}

	public static List<Attachment> paste(Attachments attachments) {
		if (log.isLoggable(Level.FINE)) log.fine("paste(");
		List<Attachment> ret = new ArrayList<Attachment>(0);
		try {
			ret = attachments.addAttachmentsFromClipboard().get();
		}
		catch (Exception ex) {
			log.log(Level.WARNING, "Add attachments from clipboard failed.", ex);
		}
		if (log.isLoggable(Level.FINE)) log.fine(")paste");
		return ret;
	}

	public static void copy(TableView<Attachment> table, MailAttachmentHelper attachmentHelper, ProgressCallback cb) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("copy(");
		// http://stackoverflow.com/questions/31798646/can-java-system-clipboard-copy-a-file
		final String FILE_URL_PREFIX = MailAttachmentHelper.FILE_URL_PREFIX;
		List<File> files = new ArrayList<File>();
		for (Attachment att : table.getSelectionModel().getSelectedItems()) {
			String fileName = attachmentHelper.downloadAttachment(att, cb);
			if (fileName.isEmpty()) continue;
			if (fileName.startsWith(FILE_URL_PREFIX)) fileName = fileName.substring(FILE_URL_PREFIX.length());
			files.add(new File(fileName));
		}
		FileTransferable ft = new FileTransferable(files);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ft, null);
		if (log.isLoggable(Level.FINE)) log.fine(")copy");
	}
	
	private static class FileTransferable implements Transferable {

        private List<File> listOfFiles;

        public FileTransferable(List<File> listOfFiles) {
            this.listOfFiles = listOfFiles;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return listOfFiles;
        }
    }


}
