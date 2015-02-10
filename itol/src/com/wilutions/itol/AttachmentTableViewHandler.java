package com.wilutions.itol;

import java.io.File;
import java.util.Comparator;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

import com.wilutions.itol.db.Attachment;

public class AttachmentTableViewHandler {

	public static void apply(TableView<Attachment> table) {
		
		TableColumn<Attachment, String> iconColumn = new TableColumn<>("Icon");
		iconColumn.setCellValueFactory(new PropertyValueFactory<Attachment, String>("fileName"));
		iconColumn.setCellFactory(new Callback<TableColumn<Attachment, String>, TableCell<Attachment, String>>() {

			@Override
			public TableCell<Attachment, String> call(TableColumn<Attachment, String> item) {
				TableCell<Attachment, String> cell = new TableCell<Attachment, String>() {
					@Override
					protected void updateItem(String fileName, boolean empty) {
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
				return getFileExt(o1).compareTo(getFileExt(o2));
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
						if (fileName != null) {
							String str = getFileName(fileName);
							setText(str);
						}
					}
				};
				return cell;
			}
			
		});
		
		TableColumn<Attachment, Long> contentLengthColumn = new TableColumn<>("Size");
		contentLengthColumn.setCellValueFactory(new PropertyValueFactory<Attachment, Long>("contentLength"));
		contentLengthColumn.setCellFactory(new Callback<TableColumn<Attachment, Long>, TableCell<Attachment, Long>>() {

			@Override
			public TableCell<Attachment, Long> call(TableColumn<Attachment, Long> item) {
				TableCell<Attachment, Long> cell = new TableCell<Attachment, Long>() {
					@Override
					protected void updateItem(Long contentLength, boolean empty) {
						if (contentLength != null) {
							String str = makeAttachmentSizeString(contentLength);
							setText(str);
						}
					}
				};
				cell.setStyle("-fx-alignment: CENTER-RIGHT;");
				return cell;
			}
			
		});
				
		table.getColumns().clear();
		table.getColumns().add(iconColumn);
		table.getColumns().add(fileNameColumn);
		table.getColumns().add(contentLengthColumn);
		
	}
	
	
	private static String getFileName(String path) {
		String fname = path;
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf('.');
			if (p >= 0) {
				fname = path.substring(0, p).toLowerCase();
			}
			p = path.lastIndexOf(File.separatorChar);
			fname = path.substring(p+1);
		}
		return fname;
	}
	
	private static String getFileExt(String path) {
		String ext = "";
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf('.');
			if (p >= 0) {
				ext = path.substring(p+1).toLowerCase();
			}
		}
		return ext;
	}
	
	private static String makeAttachmentSizeString(long contentLength) {
		String[] dims = new String[] {"Bytes", "KB", "MB", "GB", "TB"};
		int dimIdx = 0;
		long c = contentLength, nb = 0;
		for (int i = 0; i < dims.length; i++) {
			nb = c;
			c = (long)Math.floor(c / 1000);
			if (c == 0) {
				dimIdx = i;
				break;
			}
		}
		return nb + " " + dims[dimIdx];
	}
}
