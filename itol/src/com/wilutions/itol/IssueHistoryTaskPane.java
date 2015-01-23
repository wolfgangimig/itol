package com.wilutions.itol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import javax.activation.DataHandler;

import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.mslib.outlook.MailItem;

public class IssueHistoryTaskPane extends TaskPaneFX implements Initializable {

	private ResourceBundle resb;
	private final MailInspector mailInspector;
	private final MailItem mailItem;
	private Issue issue;
	private IssueToHtml issueToHtml;

	@FXML
	WebView wviewInitial;

	@FXML
	WebView wviewIssueUpdates;

	public IssueHistoryTaskPane(MailInspector inspector, MailItem mailItem) {
		this.mailInspector = inspector;
		this.mailItem = mailItem;
		Globals.getThisAddin().getRegistry().readFields(this);
	}

	private final Issue readIssue() {
		Issue issue = null;
		try {
			IssueService srv = Globals.getIssueService();
			String subject = mailItem.getSubject();
			String issueId = srv.extractIssueIdFromMailSubject(subject);
			issue = srv.readIssue(issueId);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return issue;
	}

	@Override
	public void close() {
		super.close();
		try {
			if (issueToHtml != null) {
				issueToHtml.close();
			}
		} catch (IOException e) {
		}
		Globals.getThisAddin().getRegistry().writeFields(this);
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/IssueHistory.fxml");
			InputStream inputStream = classLoader.getResource("com/wilutions/itol/res_en.properties").openStream();
			resb = new PropertyResourceBundle(inputStream);

			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			} // Do not create a new IssueTaskPane object.
			);
			Parent p = loader.load();

			Scene scene = new Scene(p);
			scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

			return scene;
		} catch (Throwable e) {
			throw new IllegalStateException("Cannot create scene.", e);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		issue = readIssue();
		try {
			issueToHtml = new IssueToHtml(issue);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		wviewInitial.getEngine().locationProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String oldLoc, String newLoc) {
				// check if the newLoc corresponds to a file you want to be
				// downloadable
				// and if so trigger some code and dialogs to handle the
				// download.
				String remoteUrl = newLoc;
				if (remoteUrl != null && !remoteUrl.isEmpty()) {
					
					FileChooser chooser = new FileChooser();
					try {
						HttpURLConnection conn = (HttpURLConnection)(new URL(remoteUrl).openConnection());
						conn.setRequestMethod("HEAD");
						conn.connect();
						String contentDisposition = conn.getHeaderField("Content-Disposition");
						if (contentDisposition != null) {
							int p = contentDisposition.indexOf("filename=");
							if (p >= 0) {
								String fileName = contentDisposition.substring(p + "filename=".length()).trim();
								if (fileName.startsWith("\"")) {
									fileName = fileName.substring(1);
								}
								if (fileName.endsWith("\"")) {
									fileName = fileName.substring(0, fileName.length()-1);
								}
								chooser.setInitialFileName(fileName);
							}
						}
						conn.disconnect();
						
						File file = chooser.showSaveDialog(null);
						if (file != null)
						{
							FileOutputStream fos = null;
							DataHandler dh = new DataHandler(new URL(remoteUrl));
							try {
								fos = new FileOutputStream(file);
								dh.writeTo(fos);
							}
							finally {
								if (fos != null) {
									fos.close();
								}
							}
						}
					}
					catch (IOException e) {
						MessageBox.show(this, "Download failed", "Failed to download file: " + e.toString(), null);
						e.printStackTrace();
					}
				}
			}
		});

		updateData(false);
	}

	private void updateData(boolean saveAndValidate) {
		updateDataInitial(saveAndValidate);
		updateDataHistory(saveAndValidate);
	}

	private void updateDataInitial(boolean saveAndValidate) {
		if (saveAndValidate) {
		} else {
			String html = getInitialUpdateAsHtml();
			wviewInitial.getEngine().loadContent(html);
		}
	}

	private String getInitialUpdateAsHtml() {
		String html = issueToHtml.getInitialUpdateAsHtml();
		return html;
	}

	private void updateDataHistory(boolean saveAndValidate) {
		if (saveAndValidate) {
		} else {
		}
	}
}
