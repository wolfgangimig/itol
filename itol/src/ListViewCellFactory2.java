import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.filechooser.FileSystemView;

public class ListViewCellFactory2 extends Application {

	ListView<String> list = new ListView<String>();
	ObservableList<String> data = FXCollections.observableArrayList("a.msg", "a1.msg", "b.txt", "c.pdf", "d.html",
			"e.png", "f.zip", "g.docx", "h.xlsx", "i.pptx");

	@Override
	public void start(Stage stage) {
		VBox box = new VBox();
		Scene scene = new Scene(box, 200, 200);
		stage.setScene(scene);
		stage.setTitle("ListViewSample");
		box.getChildren().addAll(list);
		VBox.setVgrow(list, Priority.ALWAYS);

		list.setItems(data);

		list.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> list) {
				return new AttachmentListCell();
			}
		});

		stage.show();
	}

	private static class AttachmentListCell extends ListCell<String> {
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
			} else {
				
				Image fxImage = getFileIcon(item);
				if (fxImage != null) {
					ImageView imageView = new ImageView(fxImage);
					setGraphic(imageView);
				}
				
				setText(item);
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<String, Image>();

	private static String getFileExt(String fname) {
		String ext = ".";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			ext = fname.substring(p);
		}
		return ext.toLowerCase();
	}

	private static javax.swing.Icon getJSwingIconFromFileSystem(File file) {

		// Windows {
		FileSystemView view = FileSystemView.getFileSystemView();
		javax.swing.Icon icon = view.getSystemIcon(file);
		// }

		// OS X {
		// final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		// javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
		// }

		return icon;
	}

	private static Image getFileIcon(String fname) {
		final String ext = getFileExt(fname);

		Image fileIcon = mapOfFileExtToSmallIcon.get(ext);
		if (fileIcon == null) {

			javax.swing.Icon jswingIcon = null;

			File file = new File(fname);
			if (file.exists()) {
				jswingIcon = getJSwingIconFromFileSystem(file);
			} else {
				File tempFile = null;
				try {
					tempFile = File.createTempFile("icon", ext);
					jswingIcon = getJSwingIconFromFileSystem(tempFile);
				} catch (IOException ignored) {
					// Cannot create temporary file.
				} finally {
					if (tempFile != null) {
						tempFile.delete();
					}
				}
			}

			if (jswingIcon != null) {
				fileIcon = jswingIconToImage(jswingIcon);
				mapOfFileExtToSmallIcon.put(ext, fileIcon);
			}
		}

		return fileIcon;
	}

	private static Image jswingIconToImage(javax.swing.Icon jswingIcon) {
		BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
		jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
		return SwingFXUtils.toFXImage(bufferedImage, null);
	}

}