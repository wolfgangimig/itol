package com.wilutions.itol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.LoggerConfig;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class DlgLogging extends ModalDialogFX<Boolean> implements Initializable {

	private Config config;
	private ResourceBundle resb;
	private Scene scene;

	@FXML
	Button bnOK;
	@FXML
	Button bnCancel;
	@FXML
	TextField edLogFile;
	@FXML
	ChoiceBox<IdName> cbLogLevel;
	@FXML
	CheckBox ckLogAppend;

	public DlgLogging() {
		this.resb = Globals.getResourceBundle();
		this.config = (Config)Globals.getAppInfo().getConfig().clone();
		setTitle(resb.getString("DlgLogging.Caption"));
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgLogging.fxml");
			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			});
			Parent p = loader.load();
			
			scene = new Scene(p);
			//scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

			return scene;
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	@FXML
	public void onOK() {
		updateData(true);
		
		LoggerConfig loggerConfig = config.getLoggerConfig();
		try {
			File logFile = new File(loggerConfig.getFile());
			logFile.getParentFile().mkdirs();
			logFile.createNewFile();
			if (!logFile.exists() || !logFile.isFile()) {
				String textf = resb.getString("msg.config.invalidExportDir");
				String text = MessageFormat.format(textf, logFile.getAbsolutePath());
				throw new FileNotFoundException(text);
			}
			
			Globals.getAppInfo().setConfig(config);
			Globals.initLogging();
			
			config.write();
			
			finish(true);
			
		} catch (Exception e) {
			
			String msg = e.getMessage();
			String textf = resb.getString("msg.config.error");
			String text = MessageFormat.format(textf, msg);
			MessageBox.error(this, text, (ignored, ex) -> {
			});
		}
	}

	@FXML
	public void onCancel() {
		close();
	}

	@Override
	public void initialize(URL location, ResourceBundle resb) {

		cbLogLevel.getItems().add(new IdName("INFO", resb.getString("DlgLogging.LogLevel.Info")));
		cbLogLevel.getItems().add(new IdName("DEBUG", resb.getString("DlgLogging.LogLevel.Debug")));
		cbLogLevel.getSelectionModel().select(0);
		
		updateData(false);
	}

	private void updateData(boolean save) {
		LoggerConfig loggerConfig = config.getLoggerConfig();
		if (save) {
			loggerConfig.setFile(edLogFile.getText());
			loggerConfig.setLevel(cbLogLevel.getSelectionModel().getSelectedItem().getId());
			loggerConfig.setAppend(ckLogAppend.isSelected());
		}
		else {
			edLogFile.setText(loggerConfig.getFile());
			cbLogLevel.getSelectionModel().select(new IdName(loggerConfig.getLevel(), ""));
			ckLogAppend.setSelected(loggerConfig.isAppend());
		}
	}

}
