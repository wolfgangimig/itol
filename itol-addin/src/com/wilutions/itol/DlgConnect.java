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
import com.wilutions.itol.db.Profile;
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
import javafx.scene.control.ComboBox;
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
	ComboBox<String> cbProxyServer;
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
			
			config.write();
			
			succ = true;
		}
		catch (Throwable e) {
			if (e instanceof InterruptedIOException) {
			}
			else if (connectionInProcess.getValue() && id == connectionProcessId.get()) {
				String msg = e.getMessage();
				if (msg.contains("401") || msg.contains("403")) {
					msg = resb.getString("msg.connection.authentication.failed");
				}
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
		
		cbProxyServer.getItems().add(resb.getString("DlgConnect.Proxy.server.default"));
		cbProxyServer.getSelectionModel().select(0);

		updateData(false);
	}

	private void updateData(boolean save) {
		Profile profile = config.getCurrentProfile();
		if (save) {
			profile.setServiceUrl(edUrl.getText());
			profile.setUserName("");
			profile.setEncryptedPassword("");
			profile.setCredentials("");
			String pwd = edPassword.getText();
			if (pwd.isEmpty()) {
				profile.setCredentials(edUserName.getText());
			}
			else {
				profile.setUserName(edUserName.getText());
				profile.setEncryptedPassword(PasswordEncryption.encrypt(edPassword.getText()));
			}
			profile.setProxyServerEnabled(ckProxyEnabled.isSelected());
			
			{
				String proxyServerAndPort = cbProxyServer.getEditor().getText();
				int p = proxyServerAndPort.indexOf(':');
				if (p >= 0) {
					String proxyServer = proxyServerAndPort.substring(0, p);
					String proxyPortStr = proxyServerAndPort.substring(p+1);
					int proxyPort = Integer.parseInt(proxyPortStr);
					profile.setProxyServer(proxyServer);
					profile.setProxyServerPort(proxyPort);
				}
				else {
					// Assume DlgConnect.Proxy.server.default is selected, use default settings.
					profile.setProxyServer("");
					profile.setProxyServerPort(0);
				}
			}
			
			profile.setProxyServerUserName(edProxyUserName.getText());
			profile.setProxyServerEncryptedUserPassword(PasswordEncryption.encrypt(edProxyPassword.getText()));
		}
		else {
			String url = profile.getServiceUrl();
			if (url.isEmpty()) url = "http://server:port";
			edUrl.setText(url);

			String apiKey = profile.getCredentials();
			// 1b5d44de5539ef39c6b3ef0befc2e71234af3d81
			if (apiKey.isEmpty()) {
				edUserName.setText(profile.getUserName());
				edPassword.setText(PasswordEncryption.decrypt(profile.getEncryptedPassword()));
			}
			else {
				edUserName.setText(apiKey);
			}

			{
				String proxyServerAndPort = profile.getProxyServer() + ":" + profile.getProxyServerPort();
				boolean useSystemSettings = proxyServerAndPort.equals(":0"); 
				if (useSystemSettings) { 
					cbProxyServer.getSelectionModel().select(0);
				}
				else {
					cbProxyServer.getSelectionModel().select(-1);
					cbProxyServer.getEditor().setText(proxyServerAndPort);
				}
			}
			edProxyUserName.setText(profile.getProxyServerUserName());
			String proxyPassword = profile.getProxyServerEncryptedUserPassword();
			edProxyPassword.setText(PasswordEncryption.decrypt(proxyPassword));
			ckProxyEnabled.setSelected(profile.isProxyServerEnabled());
			
			enableProxySettings();
		}
	}
	
	private void enableProxySettings() {
		boolean disable = !ckProxyEnabled.isSelected();
		cbProxyServer.setDisable(disable);
		edProxyUserName.setDisable(disable);
		edProxyPassword.setDisable(disable);
	}
	
	@FXML
	private void onCheckProxyEnabled(ActionEvent event) {
		enableProxySettings();
	}

}
