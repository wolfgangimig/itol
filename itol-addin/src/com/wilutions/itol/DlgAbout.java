package com.wilutions.itol;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

public class DlgAbout implements Initializable {

	private static Logger log = Logger.getLogger("DlgAbout");
	
	@FXML
	Label lbProgramName;
	@FXML
	Label lbProgramTitle;
	@FXML
	Label lbProgramVersion;
	@FXML
	TableView<About3rdPartyLib> tv3rdPartyLibs;
		
	Stage stage;
	
	public static void show(Window owner) {
		new DlgAbout().internalShow(owner);
	}
	
	private void internalShow(Window owner) {
		try {
			ResourceBundle resb = Globals.getResourceBundle();
			
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgAbout.fxml");
			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			});
			Parent p = loader.load();

			Scene scene = new Scene(p);
			scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

			stage = new Stage();
			stage.setTitle("About");
			stage.initOwner(owner);
			stage.setScene(scene);
			stage.showAndWait();
		}
		catch (Exception ex) {
			log.log(Level.WARNING, "About dialog cannot be displayed.", ex);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		String programName = ((ItolAddin)Globals.getThisAddin()).getProgramName();
		lbProgramName.setText(programName);

		String programTitle = ((ItolAddin)Globals.getThisAddin()).getProgramTitle();
		lbProgramTitle.setText(programTitle);
		
		String programVersion = ((ItolAddin)Globals.getThisAddin()).getProgramVersion();
		lbProgramVersion.setText(programVersion);
		
		
		TableColumn<About3rdPartyLib, String> columnName = new TableColumn<>("Name");
		columnName.setCellValueFactory(new PropertyValueFactory<About3rdPartyLib, String>("name"));
		
		TableColumn<About3rdPartyLib, String> columnLicense = new TableColumn<>("License");
		columnLicense.setCellValueFactory(new PropertyValueFactory<About3rdPartyLib, String>("license"));
		columnLicense.setCellFactory(new Callback<TableColumn<About3rdPartyLib, String>, TableCell<About3rdPartyLib, String>>() {

			@Override
			public TableCell<About3rdPartyLib, String> call(TableColumn<About3rdPartyLib, String> item) {
				
				TableCell<About3rdPartyLib, String> cell = new TableCell<About3rdPartyLib, String>() {
					@Override
					protected void updateItem(String license, boolean empty) {
						super.updateItem(license, empty);
						TableRow<About3rdPartyLib> row = getTableRow();
						if (row != null) {
							About3rdPartyLib lib = row.getItem();
							Hyperlink hlink = new Hyperlink(license);
							hlink.setOnAction((action) -> {
								lib.showLicense();
							});
							setGraphic(hlink);
						}
					}
				};

				return cell;
			}

		});

		tv3rdPartyLibs.getColumns().clear();
		tv3rdPartyLibs.getColumns().add(columnName);
		tv3rdPartyLibs.getColumns().add(columnLicense);

		tv3rdPartyLibs.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		columnName.setMaxWidth( 1f * Integer.MAX_VALUE * 50 ); // 50% width
		columnLicense.setMaxWidth( 1f * Integer.MAX_VALUE * 50 ); // 50% width

		tv3rdPartyLibs.setItems(FXCollections.observableList(About3rdPartyLib.LIBS));
	}

	@FXML
	private void onHomepageClicked() {
		IssueApplication.showDocument("http://www.wilutions.com");
	}
	
	@FXML
	private void onOK() {
		this.stage.close();
	}

	@FXML
	private void onLogoEntered() {
		this.stage.getScene().setCursor(Cursor.HAND);
	}

	@FXML
	private void onLogoExited() {
		this.stage.getScene().setCursor(Cursor.DEFAULT);
	}
}
