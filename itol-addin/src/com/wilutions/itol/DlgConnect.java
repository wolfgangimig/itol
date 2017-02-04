package com.wilutions.itol;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import com.wilutions.com.BackgTask;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.HttpClient;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class DlgConnect extends ModalDialogFX<Boolean> implements Initializable {

	private Config config;
	private ResourceBundle resb;
	private Scene scene;
	private SimpleBooleanProperty connectionInProcess = new SimpleBooleanProperty();
	private AtomicLong connectionProcessId = new AtomicLong();

	@FXML
	Button bnOK;
	@FXML
	Button bnCancel;
	@FXML
	TextField edUrl;
	@FXML
	TextField edUserName;
	@FXML
	TextField edPassword;
	@FXML
	ProgressBar pgProgress;
	
	@FXML
	Label lbProxyServer;
	@FXML
	TextField edProxyServer;
	@FXML
	Label lbProxyPort;
	@FXML
	TextField edProxyPort;
	@FXML
	Label lbProxyUserName;
	@FXML
	TextField edProxyUserName;
	@FXML
	Label lbProxyPassword;
	@FXML
	TextField edProxyPassword;
	@FXML
	CheckBox ckProxyEnabled;
	
	public DlgConnect() {
		this.resb = Globals.getResourceBundle();
		this.config = (Config)Globals.getAppInfo().getConfig().clone();
		setTitle(resb.getString("DlgConnect.Caption"));
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgConnect.fxml");
			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			});
			Parent p = loader.load();

			scene = new Scene(p);
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

		connectionProcessId.set(System.currentTimeMillis());

		pgProgress.setProgress(0);
		connectionInProcess.setValue(true);
		
		BackgTask.run(() -> {
			long id = connectionProcessId.get();
			try {
				final double n = HttpClient.CONNECT_TIMEOUT_SECONDS + 1;

				for (double i = 1; i < n; i++) {

					if (!connectionInProcess.getValue()) break;
					if (id != connectionProcessId.get()) break;

					final double p = ((double) i) / n;
					Platform.runLater(() -> pgProgress.setProgress(p));

					Thread.sleep(1000);
					if (!connectionInProcess.getValue()) break;
					if (id != connectionProcessId.get()) break;

				}

				Platform.runLater(() -> pgProgress.setProgress(1));
			}
			catch (InterruptedException e) {
			}
		});

		BackgTask.run(() -> {
			if (connect(this, config)) {
				Platform.runLater(() -> {
					setResult(true);
					close();
				});
			}
		});
	}

	protected boolean connect(Object ownerWindow, Config config) {
		long id = connectionProcessId.get();
		boolean succ = false;
		try {
			Globals.getAppInfo().setConfig(config);
			Globals.initialize(false);
			succ = true;
		}
		catch (Throwable e) {
			if (e instanceof InterruptedIOException) {
			}
			else if (connectionInProcess.getValue() && id == connectionProcessId.get()) {
				String msg = e.getMessage();
				String textf = resb.getString("msg.connection.error");
				String text = MessageFormat.format(textf, msg);
				MessageBox.error(ownerWindow, text, (ignored, ex) -> {

				});
			}
		}
		finally {
			if (id == connectionProcessId.get()) {
				connectionInProcess.setValue(false);
			}
		}
		return succ;
	}

	@FXML
	public void onCancel() {
		connectionProcessId.set(0);
		if (connectionInProcess.getValue()) {
			connectionInProcess.setValue(false);
		}
		else {
			close();
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resb) {

		bnOK.visibleProperty().bind(Bindings.not(connectionInProcess));
		bnOK.managedProperty().bind(Bindings.not(connectionInProcess));
		pgProgress.visibleProperty().bind(connectionInProcess);

		updateData(false);
	}

	private void updateData(boolean save) {
		if (save) {
			config.setServiceUrl(edUrl.getText());
			config.setUserName("");
			config.setEncryptedPassword("");
			config.setCredentials("");
			String pwd = edPassword.getText();
			if (pwd.isEmpty()) {
				config.setCredentials(edUserName.getText());
			}
			else {
				config.setUserName(edUserName.getText());
				config.setEncryptedPassword(PasswordEncryption.encrypt(edPassword.getText()));
			}
			config.setProxyServerEnabled(ckProxyEnabled.isSelected());
			config.setProxyServer(edProxyServer.getText());
			config.setProxyServerPort(Integer.parseInt(edProxyPort.getText()));
			config.setProxyServerUserName(edProxyUserName.getText());
			config.setProxyServerEncryptedUserPassword(PasswordEncryption.encrypt(edProxyUserName.getText()));
		}
		else {
			String url = config.getServiceUrl();
			if (url.isEmpty()) url = "http://server:port";
			edUrl.setText(url);

			String apiKey = config.getCredentials();
			// 1b5d44de5539ef39c6b3ef0befc2e71234af3d81
			if (apiKey.isEmpty()) {
				edUserName.setText(config.getUserName());
				edPassword.setText(PasswordEncryption.decrypt(config.getEncryptedPassword()));
			}
			else {
				edUserName.setText(apiKey);
			}

			edProxyServer.setText(config.getProxyServer());
			int proxyPort = config.getProxyServerPort();
			edProxyPort.setText(Integer.toString(proxyPort));
			edProxyUserName.setText(config.getProxyServerUserName());
			String proxyPassword = config.getProxyServerEncryptedUserPassword();
			edProxyPassword.setText(PasswordEncryption.decrypt(proxyPassword));
			ckProxyEnabled.setSelected(config.isProxyServerEnabled());
			
			enableProxySettings();
		}
	}
	
	private void enableProxySettings() {
		boolean disable = !ckProxyEnabled.isSelected();
		edProxyServer.setDisable(disable);
		edProxyPort.setDisable(disable);
		edProxyUserName.setDisable(disable);
		edProxyPassword.setDisable(disable);
	}
	
	@FXML
	private void onCheckProxyEnabled(ActionEvent event) {
		enableProxySettings();
	}

}
