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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.ProgressCallbackFactory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.Duration;

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

		////////////////////
		// Drag&Drop

		table.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
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
				event.setDropCompleted(success);
				event.consume();
			}

		});

		/////////////////////////////////////////////////////
		// Render preview in tooltip for image attachments.
		
		TooltipRefCount activeTooltip = new TooltipRefCount();
		activeTooltip.owner = table;
		
		table.setRowFactory(tableView -> {
		    final TableRow<Attachment> row = new TableRow<>();
	        final Popup tooltip = new Popup();

		    row.setOnMouseEntered((event) -> {
		    	
		    	Attachment attachment = row.getItem();
				double x = event.getScreenX(), y = event.getScreenY();

		    	if (tooltip.getContent().isEmpty()) {
					initThumbnailTooltip(attachment, tooltip, progressCallbackFactory, () -> {
						
						// Add mouse handlers to keep showing tooltip if mouse is over the image.
						ImageView imageView = (ImageView)tooltip.getContent().get(0);
						imageView.setOnMouseEntered((_ignore) -> {
							imageView.getScene().setCursor(Cursor.HAND);
							activeTooltip.show(tooltip, x, y);
						});
						imageView.setOnMouseExited((_ignore) -> {
							activeTooltip.hide(tooltip);
							imageView.getScene().setCursor(Cursor.DEFAULT);
						});
						
						// If image is clicked, show full attachment and hide tooltip.
						imageView.setOnMouseClicked((_ignore) -> {
							BackgTask.run(() -> {
								try {
									attachmentHelper.showAttachment(attachment, progressCallbackFactory.createProgressCallback("Show attachment"));
								} catch (Exception e) {
									log.log(Level.WARNING, "Failed to show attachment=" + attachment, e);
								}
							});
						});
						
						activeTooltip.show(tooltip, x, y);
					});
				}
				else {
					activeTooltip.show(tooltip, x, y);
				}
		    });
		    
		    row.setOnMouseExited((event) -> {
		    	activeTooltip.hide(tooltip);
		    });


		    return row;
		});
		
		
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] apply(observableAttachments=" + observableAttachments + ")");
	}

	/**
	 * This class holds tooltip window and a reference count for the number of calls to show().
	 * The tooltip is being hidden, when the reference count reaches 0.
	 */
	private static class TooltipRefCount {
		
		final static Popup NO_TOOLTIP = new Popup(); 
		Node owner;
		Popup tooltip = NO_TOOLTIP;
		int refCount;
		
		public void show(Popup newTooltip, double x, double y) { 
			Popup oldTooltip = tooltip;
			tooltip = newTooltip;
			
			if (oldTooltip != newTooltip) {
				oldTooltip.hide();
				refCount = 0;
			}

			refCount++;

			if (!tooltip.isShowing()) {
				tooltip.show(owner, x, y);
			}
		}
		
		public void hide(Popup newTooltip) {
			Popup oldTooltip = tooltip;
			if (oldTooltip == newTooltip) {
				refCount--;
				if (refCount == 0) {
			    	Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.0), new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							if (oldTooltip == tooltip && refCount == 0) {
								tooltip.hide();
							}
						}
			    	}));
			    	timeline.setCycleCount(1);
			    	timeline.play();
				}
			}
			else {
				refCount = 0;
				oldTooltip.hide();
			}
		}
	}
	
	/**
	 * Add a ImageView node to the given tooltip for showing the thumbnail image of the attachment.
	 * @param attachment
	 * @param tooltip
	 * @param progressCallbackFactory
	 * @param thenRunInFxThread
	 */
	private static void initThumbnailTooltip(Attachment attachment, Popup tooltip, ProgressCallbackFactory progressCallbackFactory, Runnable thenRunInFxThread) {
        if (attachment != null) {
        	BackgTask.run(() -> {
        		if (attachment.getThumbnailImage() == null) {
			        MailAttachmentHelper.getThumbnailImage(attachment, progressCallbackFactory.createProgressCallback("Download thumbnail"));
        		}
        		Image image = attachment.getThumbnailImage();
        		if (image != null) {
        			Platform.runLater(() -> {
        				ImageView imageView = new ImageView(image);
    					tooltip.getContent().add(imageView);
    	    			thenRunInFxThread.run();
        			});
        		}
        	});
        }
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
