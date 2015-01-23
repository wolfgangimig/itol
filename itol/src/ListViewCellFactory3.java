import java.awt.image.BufferedImage;
import java.io.File;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

public class ListViewCellFactory3 extends Application {

	ListView<String> list = new ListView<String>();
	ObservableList<String> data = FXCollections.observableArrayList("a.msg", "b.txt", "c.pdf");

	private static Image getFileIcon(String fname) {
		String ext = "";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			ext = fname.substring(p);
		}

		final BufferedImage[] refbufferedImage = new BufferedImage[1];
		final String ext2 = ext;

		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					File file = File.createTempFile("icon", ext2);
					FileSystemView view = FileSystemView.getFileSystemView();
					javax.swing.Icon icon = view.getSystemIcon(file);
					file.delete();

					BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
							BufferedImage.TYPE_INT_ARGB);
					icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);

					refbufferedImage[0] = bufferedImage;
				} catch (Throwable e) {
					e.printStackTrace();
				}
			});
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Image fxImage = SwingFXUtils.toFXImage(refbufferedImage[0], null);
		return fxImage;
	}

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
		HBox hbox = new HBox();
		Label label = new Label("(empty)");
		Pane pane = new Pane();
		Button button = new Button("(>)");
		String lastItem;
		ImageView imageView;

		public AttachmentListCell() {
			//hbox.getChildren().addAll(label, pane, imageView);
			//HBox.setHgrow(pane, Priority.ALWAYS);

		}

		public void updateItem2(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);  // No text in label of super class
            if (empty) {
                lastItem = null;
                setGraphic(null);
            } else {
                lastItem = item;
                label.setText(item!=null ? item : "<null>");
                setGraphic(hbox);
            }
		}
		public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
            } else {
    			Image icon = getFileIcon(item);
    			imageView = new ImageView(icon);

                setGraphic(imageView);
                setText(item);
            }
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}