package com.wilutions.itol;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import com.wilutions.com.BackgTask;
import com.wilutions.itol.db.HttpClient;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.itol.db.Profile;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class DlgConnect extends ModalDialogFX<Profile> implements Initializable {

	private Profile profile;
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
	
	public DlgConnect(Profile profile) {
		this.resb = Globals.getResourceBundle();
		this.profile = (Profile)profile.clone();
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

		// Connect to service in background thread.
		BackgTask.run(() -> {
			
			// Detect service type if dialog was opened for a new connection. 
			// Therefore, loop over supported service factory classes.
			
			// Collect supported service factory classes for new connection or choose
			// the service factory class for an existing profile.
			List<String> serviceFactoryClasses = profile.getServiceFactoryClass().isEmpty() ? 
					Profile.SERVICE_FACTORY_CLASSES : Arrays.asList(profile.getServiceFactoryClass());
			
			for (int i = 0; i < serviceFactoryClasses.size(); i++) {
				
				// Try with service factory class.
				String serviceFactoryClass = serviceFactoryClasses.get(i);
				profile.setServiceFactoryClass(serviceFactoryClass);
				
				// Connect to service.
				// On success, the service might have created its extended profile object.
				boolean lastTry = i == serviceFactoryClasses.size()-1;
				Profile acceptedProfile = connect(profile, lastTry);
				if (acceptedProfile != null) {
					
					// Success: close dialog.
					Platform.runLater(() -> {
						finish(acceptedProfile);
					});
					
					break;
				}
			}
		});
	}

	/**
	 * Connect to profile.
	 * @param profile Profile object.
	 * @param lastTry false, if errors should be ignored. If true, a message box is displayed on error.
	 * @return Accepted profile object (maybe replaced by an extended object created by the service).
	 */
	protected Profile connect(Profile profile, boolean lastTry) {
		long id = connectionProcessId.get();
		Profile acceptedProfile = null;
		try {
			
			// Create and connect to issue service.
			IssueService issueService = Globals.createIssueService(profile);
			
			// Issue service might have created an extended profile (e.g. JiraProfile).
			// Return this updated profile.
			acceptedProfile = issueService.getProfile();
		}
		catch (Throwable e) {
			if (!lastTry) {
				// Ignore error, if not last try
			}
			else if (e instanceof InterruptedIOException) {
			}
			else if (connectionInProcess.getValue() && id == connectionProcessId.get()) {
				String msg = e.getMessage();
				
				// Authentication failed?
				boolean authFailed = msg.contains("401") || msg.contains("403");
				if (authFailed) {
					msg = resb.getString("msg.connection.authentication.failed");
				}
				
				// Show error
				String textf = resb.getString("msg.connection.error");
				String text = MessageFormat.format(textf, msg);
				MessageBox.error(this, text, (ignored, ex) -> {
				});
			}
		}
		finally {
			if (id == connectionProcessId.get()) {
				connectionInProcess.setValue(false);
			}
		}
		return acceptedProfile;
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
			if (profile.isNew()) {
				profile.initProfileNameFromServiceUrl();
			}
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

		}
	}
	

}
