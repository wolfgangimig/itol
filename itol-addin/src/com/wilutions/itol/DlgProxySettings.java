package com.wilutions.itol;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.itol.db.ProxyServerConfig;
import com.wilutions.joa.fx.ModalDialogFX;

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

public class DlgProxySettings extends ModalDialogFX<Boolean> implements Initializable {

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
	
	public DlgProxySettings() {
		this.resb = Globals.getResourceBundle();
		this.config = Globals.getAppInfo().getConfig();
		setTitle(resb.getString("DlgProxySettings.caption"));
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgProxySettings.fxml");
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
		Globals.getAppInfo().setConfig(config);
		Globals.initProxy();
		config.write();
		setResult(Boolean.TRUE);
		close();
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
		
		cbProxyServer.getItems().add(resb.getString("DlgProxySettings.server.default"));
		cbProxyServer.getSelectionModel().select(0);

		updateData(false);
	}

	private void updateData(boolean save) {
		
		ProxyServerConfig proxy = config.getProxyServer();
		if (save) {
			
			proxy.setEnabled(ckProxyEnabled.isSelected());
			
			{
				String proxyServerAndPort = cbProxyServer.getEditor().getText();
				int p = proxyServerAndPort.indexOf(':');
				if (p >= 0) {
					String proxyServer = proxyServerAndPort.substring(0, p);
					String proxyPortStr = proxyServerAndPort.substring(p+1);
					int proxyPort = Integer.parseInt(proxyPortStr);
					proxy.setHost(proxyServer);
					proxy.setPort(proxyPort);
				}
				else {
					// Assume DlgConnect.Proxy.server.default is selected, use default settings.
					proxy.setHost("");
					proxy.setPort(0);
				}
			}
			
			proxy.setUserName(edProxyUserName.getText());
			proxy.setEncryptedPassword(PasswordEncryption.encrypt(edProxyPassword.getText()));
		}
		else {

			{
				String proxyServerAndPort = proxy.getHost() + ":" + proxy.getPort();
				boolean useSystemSettings = proxyServerAndPort.equals(":0"); 
				if (useSystemSettings) { 
					cbProxyServer.getSelectionModel().select(0);
				}
				else {
					cbProxyServer.getSelectionModel().select(-1);
					cbProxyServer.getEditor().setText(proxyServerAndPort);
				}
			}
			edProxyUserName.setText(proxy.getUserName());
			String proxyPassword = proxy.getEncryptedPassword();
			edProxyPassword.setText(PasswordEncryption.decrypt(proxyPassword));
			ckProxyEnabled.setSelected(proxy.isEnabled());
			
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
