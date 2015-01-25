import javafx.application.Application;
import javafx.beans.value.*;
import javafx.event.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
 
public class JavaFXListViewManipulation extends Application {
  @Override public void start(final Stage stage) {
    final Label status       = new Label();
    final Label changeReport = new Label();
    
    final ListView<String> listView = new ListView<>();
    initListView(listView);

    listView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
      @Override public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        changeReport.setText("Selection changed from '" + oldValue + "' to '" + newValue + "'");
      }
    });

    final Button removeButton = new Button("Remove Selected");
    removeButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent event) {
        final int selectedIdx = listView.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
          String itemToRemove = listView.getSelectionModel().getSelectedItem();
 
          final int newSelectedIdx =
            (selectedIdx == listView.getItems().size() - 1)
               ? selectedIdx - 1
               : selectedIdx;
 
          listView.getItems().remove(selectedIdx);
          status.setText("Removed " + itemToRemove);
          listView.getSelectionModel().select(newSelectedIdx);
        }
      }
    });
    final Button removeAllButton = new Button("Remove all selected");
    removeAllButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent event) {
        for (int selectedIdx : listView.getSelectionModel().getSelectedIndices()) {
          String itemToRemove = listView.getItems().get(selectedIdx);
          listView.getItems().remove(selectedIdx);
          status.setText("Removed " + itemToRemove);
        }
      }
    });
    final Button resetButton = new Button("Reset List");
    resetButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent event) {
        initListView(listView);
        status.setText("List Reset");
      }
    });
    final HBox controls = new HBox(10);
    controls.setAlignment(Pos.CENTER);
    controls.getChildren().addAll(removeButton, removeAllButton, resetButton);
 
    final VBox layout = new VBox(10);
    layout.setAlignment(Pos.CENTER);
    layout.setStyle("-fx-padding: 10; -fx-background-color: cornsilk;");
    layout.getChildren().setAll(
      listView, 
      controls,
      status,
      changeReport
    );
    layout.setPrefWidth(320);
    
    stage.setScene(new Scene(layout));
    stage.show();
  }
 
  private void initListView(ListView<String> listView) {
    listView.getItems().setAll("apples", "oranges", "peaches", "pears");
  }
  
  public static void main(String[] args) { launch(args); }
}