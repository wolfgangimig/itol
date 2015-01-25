package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.BackgTask;
import com.wilutions.com.ComException;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.DescriptionHtmlEditor;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.Property;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.mslib.office.CustomTaskPane;
import com.wilutions.mslib.outlook.Attachments;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlSaveAsType;

public class IssueTaskPane extends TaskPaneFX implements Initializable {

	private Issue issue;

	@FXML
	private GridPane rootGrid;
	@FXML
	private HBox hboxAssignee;
	@FXML
	private HBox hboxCategory;
	@FXML
	private TextField edSubject;
	@FXML
	private HTMLEditor edDescription;
	@FXML
	private ChoiceBox<IdName> cbTracker;
	@FXML
	private ChoiceBox<IdName> cbPriority;
	@FXML
	private ChoiceBox<IdName> cbAssignee;
	@FXML
	private ChoiceBox<IdName> cbStatus;
	@FXML
	private TextField edAssignee;
	@FXML
	private Button bnAssignee;
	@FXML
	private ChoiceBox<IdName> cbCategory;
	@FXML
	private TextField edCategory;
	@FXML
	private Button bnCategory;
	@FXML
	private MenuButton cbMilestone;
	@FXML
	private TextField edMilestone;
	@FXML
	private Button bnMilestone;
	@FXML
	private ListView<File> lvAttachments;
	@FXML
	private CheckBox ckSendConfirmation;
	@FXML
	private Button bnDetails;
	@FXML
	private Button bnCreate;
	@FXML
	private MenuItem mnInsertAttachment;
	@FXML
	private MenuItem mnRemoveAttachment;
	@FXML
	private MenuItem mnOpenAttachment;

	private DescriptionHtmlEditor descriptionHtmlEditor;
	private WebView webView;
	private final MailInspector mailInspector;
	private final MailItem mailItem;
	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private File tempDir;
	private ResourceBundle resb;

	public IssueTaskPane(MailInspector mailInspector, MailItem mailItem) {
		this.mailInspector = mailInspector;
		this.mailItem = mailItem;
		Globals.getThisAddin().getRegistry().readFields(this);
		tempDir = new File(Globals.getTempDir(), MsgFileTypes.makeValidFileName(mailItem.getSubject()));
		tempDir.mkdirs();
	}

	public String getIssueId() {
		String issueId = "";
		try {
			String subject = mailItem.getSubject();
			issueId = Globals.getIssueService().extractIssueIdFromMailSubject(subject);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return issueId;
	}

	@Override
	public void close() {
		super.close();
		Globals.getThisAddin().getRegistry().writeFields(this);
		for (Runnable run : resourcesToRelease) {
			try {
				run.run();
			} catch (Throwable ignored) {
			}
		}
		tempDir.delete();
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL fxmlURL = classLoader.getResource("com/wilutions/itol/NewIssue.fxml");

			resb = Globals.getResourceBundle();

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
	public void showAsync(final CustomTaskPane taskPane, AsyncResult<Boolean> asyncResult) throws ComException {
		try {
			issue = Globals.getIssueService().createIssue();

			super.showAsync(taskPane, (succ, ex) -> {
				if (succ) {
					String subject = mailItem.getSubject();
					String descr = mailItem.getBody();
					edSubject.setText(subject);
					try {
						initDescription(descr);
					} catch (Throwable e) {
						e.printStackTrace();
						ex = e;
					}
				}

				if (asyncResult != null) {
					asyncResult.setAsyncResult(succ, ex);
				}
			});

		} catch (Throwable e) {
			e.printStackTrace();
			if (asyncResult != null) {
				asyncResult.setAsyncResult(Boolean.FALSE, e);
			}
		}
	}

	private void initDescription(String descr) throws IOException {

		issue.setDescription(descr);

		descriptionHtmlEditor = Globals.getIssueService().getDescriptionHtmlEditor(issue);
		if (descriptionHtmlEditor != null) {
			edDescription.setVisible(false);

			webView = new WebView();
			webView.getEngine().loadContent(descriptionHtmlEditor.getHtmlContent());

			HBox hbox = new HBox();
			hbox.setStyle("-fx-border-color: LIGHTGREY;-fx-border-width: 1px;");
			hbox.getChildren().add(webView);

			rootGrid.add(hbox, 0, 1, 3, 1);
		} else {
			edDescription.setHtmlText("<pre>" + descr + "</pre>");
		}
	}

	@Override
	// This method is called by the FXMLLoader when initialization is complete
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		try {
			lvAttachments.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			lvAttachments.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent click) {
					if (click.getClickCount() == 2) {
						showSelectedIssueAttachment();
					}
				}
			});

			// Details currently not supported.
			bnDetails.setVisible(false);

			updateData(false);

			cbTracker.valueProperty().addListener(new ComboboxChangeListener(cbTracker, Property.ISSUE_TYPE));
			cbPriority.valueProperty().addListener(new ComboboxChangeListener(cbPriority, Property.PRIORITY));
			cbStatus.valueProperty().addListener(new ComboboxChangeListener(cbStatus, Property.STATE));
			cbAssignee.valueProperty().addListener(new ComboboxChangeListener(cbAssignee, Property.ASSIGNEE));
			cbCategory.valueProperty().addListener(new ComboboxChangeListener(cbCategory, Property.CATEGORY));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private class ComboboxChangeListener implements ChangeListener<IdName> {

		final ChoiceBox<IdName> cb;
		final String propertyId;

		ComboboxChangeListener(ChoiceBox<IdName> cb, String propertyId) {
			this.cb = cb;
			this.propertyId = propertyId;
		}

		@Override
		public void changed(ObservableValue<? extends IdName> observable, IdName oldValue, IdName newValue) {
			if (lockChangeListener) {
				return;
			}
			if (oldValue == null)
				oldValue = IdName.NULL;
			if (newValue == null)
				newValue = IdName.NULL;
			if (!oldValue.equals(newValue)) {

				saveChoiceBox(cb, propertyId);

				try {
					IssueService srv = Globals.getIssueService();
					issue = srv.validateIssue(issue);

					updateData(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private boolean lockChangeListener;

	private void updateData(boolean saveAndValidate) throws IOException {
		if (lockChangeListener)
			return;
		try {
			lockChangeListener = true;
			internalUpdateData(saveAndValidate);
		} finally {
			lockChangeListener = false;
		}
	}

	private void internalUpdateData(boolean saveAndValidate) throws IOException {
		IssueService srv = Globals.getIssueService();
		try {
			if (saveAndValidate) {
				saveSubject();
				saveDescription();
				saveChoiceBox(cbTracker, Property.ISSUE_TYPE);
				saveChoiceBox(cbPriority, Property.PRIORITY);
				saveChoiceBox(cbStatus, Property.STATE);
				saveEditOrChoiceBox(cbAssignee, edAssignee, Property.ASSIGNEE);
				saveEditOrChoiceBox(cbCategory, edCategory, Property.CATEGORY);
				saveMilestones();
			} else {
				initChoiceBox(cbTracker, srv.getIssueTypes(issue),
						issue.getLastUpdate().getProperty(Property.ISSUE_TYPE));
				initChoiceBox(cbPriority, srv.getPriorities(issue), issue.getLastUpdate()
						.getProperty(Property.PRIORITY));
				initChoiceBox(cbStatus, srv.getIssueStates(issue), issue.getLastUpdate().getProperty(Property.STATE));

				initAssignee(srv);

				initEditOrChoicebox(cbCategory, edCategory, bnCategory, srv.getCategories(issue), issue.getLastUpdate()
						.getProperty(Property.CATEGORY));

				initMilestones();

				initAttachments();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initAssignee(IssueService srv) throws IOException {
		initEditOrChoicebox(cbAssignee, edAssignee, bnAssignee, srv.getAssignees(issue), issue.getLastUpdate()
				.getProperty(Property.ASSIGNEE));
	}

	private void saveDescription() {
		if (descriptionHtmlEditor != null) {
			String elementId = descriptionHtmlEditor.getElementId();
			String scriptToGetDescription = "document.getElementById('" + elementId + "').value";
			try {
				String text = (String) webView.getEngine().executeScript(scriptToGetDescription);
				issue.setDescription(text);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} else {
			String text = edDescription.getHtmlText().trim();
			issue.setDescription(text);
		}
	}

	private void saveSubject() {
		issue.setSubject(edSubject.getText().trim());
	}

	private void initMilestones() throws IOException {
		final IssueService srv = Globals.getIssueService();
		HashSet<String> selectedIds = new HashSet<String>(Arrays.asList(issue.getMilestones()));
		cbMilestone.getItems().clear();
		StringBuilder sbufText = new StringBuilder();

		for (IdName idn : srv.getMilestones(issue)) {

			CheckMenuItem cmi = new CheckMenuItem(idn.getName());

			boolean selected = selectedIds.contains(idn.getId());
			cmi.setSelected(selected);
			if (selected) {
				if (sbufText.length() != 0) {
					sbufText.append(", ");
				}
				sbufText.append(idn.getName());
			}

			cmi.selectedProperty().addListener((ov, valueBefore, valueAfter) -> {
				try {
					saveMilestones();
					issue = srv.validateIssue(issue);
					updateData(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			cbMilestone.getItems().add(cmi);
		}

		if (sbufText.length() == 0) {
			String milestoneText = Globals.getIssueService().getPropertyClasses().get(Property.MILESTONES).getName();
			sbufText.append(milestoneText);
		}

		cbMilestone.setText(sbufText.toString());
	}

	private void saveMilestones() throws IOException {
		IssueService srv = Globals.getIssueService();
		ArrayList<IdName> milestones = new ArrayList<IdName>(srv.getMilestones(issue));
		ArrayList<String> selectedIds = new ArrayList<String>();
		int i = 0;
		for (MenuItem mi : cbMilestone.getItems()) {
			CheckMenuItem cmi = (CheckMenuItem) mi;
			if (cmi.isSelected()) {
				String milestoneId = milestones.get(i).getId();
				selectedIds.add(milestoneId);
			}
			i++;
		}
		issue.setMilestones(selectedIds.toArray(new String[selectedIds.size()]));
	}

	private void saveChoiceBox(ChoiceBox<IdName> cb, String propertyId) {
		IdName idn = cb.getValue();
		if (idn != null && !idn.equals(IdName.NULL)) {
			Property prop = new Property(propertyId, idn.getId());
			System.out.println("save property=" + prop);
			issue.getLastUpdate().setProperty(prop);
		} else {
			System.out.println("remove propertyId=" + propertyId);
			issue.getLastUpdate().removeProperty(propertyId);
		}
	}

	private void saveEditOrChoiceBox(ChoiceBox<IdName> cb, TextField ed, String propertyId) {
		IdName idn = cb.getValue();
		if (idn != null) {
			Property prop = new Property(propertyId, idn.getId());
			issue.getLastUpdate().setProperty(prop);
		}
	}

	private void initEditOrChoicebox(ChoiceBox<IdName> cb, TextField ed, Button bn, List<IdName> items, Property prop) {
		if (items.size() > 200) {
			cb.setVisible(false);
			cb.setManaged(false);
			for (IdName item : items) {
				if (item.getId().equals(prop.getValue())) {
					ed.setText(item.getName());
				}
			}
		} else {
			ed.setVisible(false);
			ed.setManaged(false);
			bn.setVisible(false);
			bn.setManaged(false);
			initChoiceBox(cb, items, prop);
		}
	}

	private void initChoiceBox(ChoiceBox<IdName> cb, List<IdName> items, Property prop) {
		cb.setItems(FXCollections.observableArrayList(items));
		Object propValue = prop.getValue();
		String value = "";
		if (propValue instanceof String) {
			value = (String) propValue;
		} else if (propValue instanceof String[]) {
			String[] values = (String[]) propValue;
			if (values.length != 0) {
				value = values[0];
			}
		}
		for (IdName item : items) {
			if (item.getId().equals(value)) {
				cb.setValue(item);
				break;
			}
		}
	}

	private interface MailSaveHandler {
		public void save() throws IOException;
	}

	private HashMap<File, MailSaveHandler> mapFileToSaveHandler = new HashMap<File, MailSaveHandler>();

	private class MailBodySaveHandler implements MailSaveHandler {

		private final File msgFile;

		public MailBodySaveHandler(File msgFile) {
			this.msgFile = msgFile;
		}

		public void save() throws IOException {

			// Already saved when double-clicked in listbox.
			if (!msgFile.exists()) {
				String ext = Globals.getIssueService().getMsgFileType();
				OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);

				resourcesToRelease.add(() -> msgFile.delete());
				System.out.println("save mail to " + msgFile);
				mailItem.SaveAs(msgFile.getAbsolutePath(), saveAsType);
			}

			Attachment att = new Attachment();
			att.setSubject(mailItem.getSubject());
			att.setContentType(getFileContentType(msgFile.getName()));
			att.setContentLength(msgFile.length());
			att.setFileName(msgFile.getName());
			att.setStream(new FileInputStream(msgFile));
			issue.getAttachments().add(att);
		}
	}

	private class MailAttachmentSaveHandler implements MailSaveHandler {

		private final com.wilutions.mslib.outlook.Attachment matt;
		private final File mattFile;

		MailAttachmentSaveHandler(com.wilutions.mslib.outlook.Attachment matt, File mattFile) {
			this.matt = matt;
			this.mattFile = mattFile;
		}

		public void save() throws IOException {

			// Already saved when double-clicked in listbox.
			if (!mattFile.exists()) {
				resourcesToRelease.add(() -> mattFile.delete());
				System.out.println("save attachment to " + mattFile);
				matt.SaveAsFile(mattFile.getAbsolutePath());
			}

			Attachment att = new Attachment();
			att.setSubject(matt.getFileName());
			att.setContentType(getFileContentType(mattFile.getName()));
			att.setContentLength(mattFile.length());
			att.setFileName(mattFile.getName());
			att.setStream(new FileInputStream(mattFile));
			issue.getAttachments().add(att);
		}
	}

	private void initAttachments() throws IOException {
		FileListViewHandler.apply(lvAttachments);

		String ext = Globals.getIssueService().getMsgFileType();
		OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);
		String msgFileName = MsgFileTypes.makeMsgFileName(mailItem.getSubject(), saveAsType);

		List<File> items = new ArrayList<File>();
		File msgFile = new File(tempDir, msgFileName);
		items.add(msgFile);

		mapFileToSaveHandler.put(msgFile, new MailBodySaveHandler(msgFile));

		if (!MsgFileTypes.isContainerFormat(saveAsType)) {
			Attachments mailAtts = mailItem.getAttachments();
			int n = mailAtts.getCount();
			for (int i = 1; i <= n; i++) {
				com.wilutions.mslib.outlook.Attachment matt = mailAtts.Item(i);
				File mattFile = new File(tempDir, matt.getFileName());
				items.add(mattFile);
				mapFileToSaveHandler.put(mattFile, new MailAttachmentSaveHandler(matt, mattFile));
			}
		}

		ObservableList<File> lvitems = FXCollections.observableArrayList(items);
		lvAttachments.setItems(lvitems);

	}

	private void showSelectedIssueAttachment() {
		File selectedFile = lvAttachments.getSelectionModel().getSelectedItem();
		if (selectedFile != null) {
			if (!selectedFile.exists()) {
				MailSaveHandler saveHandler = mapFileToSaveHandler.get(selectedFile);
				try {
					saveHandler.save();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			String url = selectedFile.getAbsolutePath();
			url = url.replace("\\", "/");
			IssueApplication.showDocument("file:///" + url);
		}
	}

	private static String getFileContentType(String fname) {
		String ext = ".";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			ext = fname.substring(p);
		}
		return ContentTypes.getContentType(ext.toLowerCase());
	}

	private long addIssueAttachments() throws IOException {
		long totalBytes = 0;
		for (File file : lvAttachments.getItems()) {
			MailSaveHandler saveHandler = mapFileToSaveHandler.get(file);
			if (saveHandler != null) {
				saveHandler.save();
			} else if (file.exists()) {
				Attachment att = new Attachment();
				att.setSubject(file.getName());
				att.setContentType(getFileContentType(file.getName()));
				att.setContentLength(file.length());
				att.setFileName(file.getName());
				att.setStream(new FileInputStream(file));
				issue.getAttachments().add(att);
			}
		}
		for (Attachment att : issue.getAttachments()) {
			totalBytes += att.getContentLength();
		}
		return totalBytes;
	}

	@FXML
	private void onCreateIssue() throws IOException {

		// Show progress dialog
		final DlgProgress dlgProgress = new DlgProgress("Create Issue");
		dlgProgress.showAsync(this, (succ, ex) -> {
			if (succ) {
				Globals.getThisAddin().onIssueCreated(mailInspector);
			}
		});

		IssueService srv = Globals.getIssueService();
		try {
			// Save dialog elements into data members
			updateData(true);

			// Create attachment objects, open attachment files,
			// compute number of bytes to upload.
			issue.getAttachments().clear();
			long totalBytes = addIssueAttachments();

			// Initialize DlgProgress.
			// Total bytes to upload: #attachment-bytes + some bytes for the
			// issue's JSON object
			final ProgressCallback progressCallback = dlgProgress.startProgress(totalBytes + 20 * 1000);

			// Create issue in background.
			// Currently, we are in the UI thread. The progress dialog would not
			// be updated,
			// If we processed updating the issue here.
			BackgTask.run(() -> {
				try {

					// Create issue
					issue = srv.updateIssue(issue, progressCallback);

					// If process was not cancelled...
					if (dlgProgress.getResult()) {

						// ... save the mail with different subject
						String mailSubjectPrev = mailItem.getSubject();
						String mailSubject = srv.injectIssueIdIntoMailSubject(mailSubjectPrev, issue);
						if (!mailSubjectPrev.equals(mailSubject)) {
							mailItem.setSubject(mailSubject);
							progressCallback.setParams("Save mail as \"" + mailSubject + "\n");
							mailItem.Save();
						}

						// // Progress dialog tells that the issue has been
						// // created.
						// progressCallback.setParams("Issue " + issue.getId() +
						// " has been created");
						// progressCallback.setFinished();
						// dlgProgress.setButtonOK();
						dlgProgress.finish(true);
					}

				} catch (Throwable e) {
					if (!progressCallback.isCancelled()) {
						e.printStackTrace();
						MessageBox.show(this, "Error", "Failed to create issue, " + e.toString(), null);
						if (dlgProgress != null) {
							dlgProgress.finish(false);
						}
					}
				}
			});

		} catch (Throwable e) {
			e.printStackTrace();
			MessageBox.show(this, "Error", "Failed to create issue, " + e.toString(), null);
			if (dlgProgress != null) {
				dlgProgress.finish(false);
			}
		} finally {
		}
	}

	@FXML
	private void onDetails() throws IOException {
		List<Property> configProps = Globals.getIssueService().getConfig();
		String url = "";
		for (Property prop : configProps) {
			if (prop.getId().equals("url")) {
				url = (String) prop.getValue();
				break;
			}
		}
		DlgDetails dlg = new DlgDetails(url);
		dlg.showAsync(this, null);
	}

	@FXML
	private void onInsertAttachment() {
		try {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Resource File");
			List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
			if (selectedFiles != null) {
				lvAttachments.getItems().addAll(selectedFiles);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onRemoveAttachment() {
		System.out.println("onRemoveAttachment");

		List<File> oldItems = lvAttachments.getItems();
		List<File> newItems = new ArrayList<File>(oldItems.size());

		HashSet<Integer> selectedIndices = new HashSet<Integer>(lvAttachments.getSelectionModel().getSelectedIndices());

		for (int i = 0; i < oldItems.size(); i++) {
			if (!selectedIndices.contains(i)) {
				newItems.add(oldItems.get(i));
			}
		}

		lvAttachments.setItems(FXCollections.observableArrayList(newItems));
	}

	@FXML
	private void onOpenAttachment() {
		showSelectedIssueAttachment();
	}

	@FXML
	private void onValidateRemoveAttachment() {
		System.out.println("onValidateRemoveAttachment");
	}

}
