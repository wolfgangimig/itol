import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.swing.filechooser.FileSystemView;

public class FileIconViewer extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Runnable fetchIcon = () -> {
            File file = null;
            try {
                file = File.createTempFile("icon", ".txt");

                // commented code always returns the same icon on OS X...
                 FileSystemView view = FileSystemView.getFileSystemView();
                 javax.swing.Icon icon = view.getSystemIcon(file);

                // following code returns different icons for different types on OS X...
                //final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                //javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);

                BufferedImage bufferedImage = new BufferedImage(
                    icon.getIconWidth(), 
                    icon.getIconHeight(), 
                    BufferedImage.TYPE_INT_ARGB
                );
                icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);

                Platform.runLater(() -> {
                    Image fxImage = SwingFXUtils.toFXImage(
                        bufferedImage, null
                    );
                    ImageView imageView = new ImageView(fxImage);
                    stage.setScene(
                        new Scene(
                            new StackPane(imageView), 
                            200, 200
                        )
                    );
                    stage.show();
                });
            } catch (IOException e) {
                e.printStackTrace();
                Platform.exit();
            } finally {
                if (file != null) {
                    file.delete();
                }
            }
        };

        javax.swing.SwingUtilities.invokeLater(fetchIcon);
    }

    public static void main(String[] args) { launch(args); }
}