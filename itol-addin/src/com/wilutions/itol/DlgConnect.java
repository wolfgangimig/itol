package com.wilutions.itol;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import com.wilutions.com.BackgTask;
import com.wilutions.itol.db.HttpClient;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.itol.db.Property;
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

	private List<Property> configProps;
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
		this.configProps = Globals.getAppInfo().getConfigProps();
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
			if (connect(this, configProps)) {
				Platform.runLater(() -> {
					setResult(true);
					close();
				});
			}
		});
	}

	protected boolean connect(Object ownerWindow, List<Property> configProps) {
		long id = connectionProcessId.get();
		boolean succ = false;
		try {
			Globals.setConfig(configProps);
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
			setConfigProperty(Property.URL, edUrl.getText());
			setConfigProperty(Property.API_KEY, "");
			setConfigProperty(Property.USER_NAME, "");
			setConfigProperty(Property.PASSWORD, "");
			String pwd = edPassword.getText();
			if (pwd.isEmpty()) {
				setConfigProperty(Property.API_KEY, edUserName.getText());
			}
			else {
				setConfigProperty(Property.USER_NAME, edUserName.getText());
				setConfigProperty(Property.PASSWORD, PasswordEncryption.encrypt(edPassword.getText()));
			}
			
			setConfigProperty(Property.PROXY_SERVER_ENABLED, Boolean.toString(ckProxyEnabled.isSelected()));
			setConfigProperty(Property.PROXY_SERVER, edProxyServer.getText());
			setConfigProperty(Property.PROXY_PORT, edProxyPort.getText());
			setConfigProperty(Property.PROXY_USER_NAME, edProxyUserName.getText());
			setConfigProperty(Property.PROXY_PASSWORD, edProxyPassword.getText());
		}
		else {
			String url = getConfigProperty(Property.URL);
			if (url.isEmpty()) url = "http://server:port";
			edUrl.setText(url);

			String apiKey = getConfigProperty(Property.API_KEY);
			// 1b5d44de5539ef39c6b3ef0befc2e71234af3d81
			if (apiKey.isEmpty()) {
				String userName = getConfigProperty(Property.USER_NAME);
				if (userName.isEmpty()) userName = System.getProperty("user.name");
				edUserName.setText(userName);
			}
			else {
				edUserName.setText(apiKey);
			}
			edPassword.setText(PasswordEncryption.decrypt(getConfigProperty(Property.PASSWORD)));
			
			String proxyServer = getConfigProperty(Property.PROXY_SERVER);
			edProxyServer.setText(proxyServer);
			String proxyPort = getConfigProperty(Property.PROXY_PORT);
			edProxyPort.setText(proxyPort);
			String proxyKey = getConfigProperty(Property.PROXY_USER_NAME);
			edProxyUserName.setText(proxyKey);
			String proxyPassword = getConfigProperty(Property.PROXY_PASSWORD);
			edProxyPassword.setText(proxyPassword);

			String proxyEnabledStr = getConfigProperty(Property.PROXY_SERVER_ENABLED);
			ckProxyEnabled.setSelected(proxyEnabledStr.equals("true"));
			
			enableProxySettings();
		}
	}

	private String getConfigProperty(String propId) {
		Property ret = null;
		for (Property prop : configProps) {
			if (prop.getId().equals(propId)) {
				ret = prop;
				break;
			}
		}
		return ret != null ? (String) ret.getValue() : "";
	}

	private void setConfigProperty(String propId, String propValue) {
		boolean found = false;
		for (Iterator<Property> it = configProps.iterator(); it.hasNext();) {
			Property prop = it.next();
			if (prop.getId().equals(propId)) {
				prop.setValue(propValue);
				found = true;
				break;
			}
		}
		if (!found) {
			configProps.add(new Property(propId, propValue));
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
