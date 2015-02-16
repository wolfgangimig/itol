/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.BackgTask;
import com.wilutions.com.ComException;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueHtmlEditor;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.mslib.office.CustomTaskPane;
import com.wilutions.mslib.office._CustomTaskPane;
import com.wilutions.mslib.outlook.Explorer;

public class IssueTaskPane extends TaskPaneFX implements Initializable {

	private Issue issue;
	private Issue issueCopy;

	@FXML
	private VBox vboxRoot;

	@FXML
	private GridPane rootGrid;
	@FXML
	private TextField edSubject;
	@FXML
	private HTMLEditor edDescription;
	@FXML
	private HBox hboxDescription;
	@FXML
	private ToggleButton bnAssignSelection;
	@FXML
	private Button bnClear;
	@FXML
	private Button bnShowIssueInBrowser;

	@FXML
	private ChoiceBox<IdName> cbTracker;
	@FXML
	private ChoiceBox<IdName> cbCategory;
	@FXML
	private ChoiceBox<IdName> cbPriority;
	@FXML
	private Tab tpDescription;
	@FXML
	private Tab tpHistory;
	@FXML
	private Tab tpNotes;
	@FXML
	private HTMLEditor edNotes;
	@FXML
	private HBox hboxNotes;
	@FXML
	private WebView webHistory;
	@FXML
	private Tab tpProperties;
	@FXML
	private TableView<Attachment> tabAttachments;
	@FXML
	private TabPane tabpIssue;
	@FXML
	private ChoiceBox<IdName> cbStatus;
	@FXML
	private Button bnUpdate;
	@FXML
	private Button bnShowAttachment;
	@FXML
	private Button bnAddAttachment;
	@FXML
	private Button bnRemoveAttachment;
	@FXML
	private HBox hboxAttachments;
	@FXML
	private GridPane propGrid;
	@FXML
	private TextField edIssueId;

	private boolean tabAttachmentsApplyHandler = true;

	private PropertyGridView propGridView;

	private IssueHtmlEditor descriptionHtmlEditor;
	private IssueHtmlEditor notesHtmlEditor;

	private WebView webDescription;
	private WebView webNotes;
	private IssueMailItem mailItem;
	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private ResourceBundle resb;
	private Timeline detectIssueModifiedTimer;
	private boolean modified;
	private boolean firstModifiedCheck;
	private AttachmentHelper attachmentHelper = new AttachmentHelper();

	/**
	 * Owner window for child dialogs (message boxes)
	 */
	private Object windowOwner;

	public IssueTaskPane(MailInspector mailInspectorOrNull, IssueMailItem mailItem) {
		this.mailItem = mailItem;

		this.resb = Globals.getResourceBundle();
		// Globals.getThisAddin().getRegistry().readFields(this);

		try {
			updateIssueFromMailItem();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setMailItem(IssueMailItem mailItem) {
		// Task pane initialized?
		if (bnAssignSelection != null) {
			if (bnAssignSelection.isSelected()) {
				internalSetMailItem(mailItem);
			}
		}
	}

	private void internalSetMailItem(IssueMailItem mailItem) {
		this.mailItem = mailItem;

		Platform.runLater(() -> {
			try {
				// Task pane initialized?
				if (detectIssueModifiedTimer != null) {
					detectIssueModifiedTimer.stop();
					if (updateIssueFromMailItem()) {
						initialUpdate();
					}
					detectIssueModifiedTimer.play();
				}
			} catch (Throwable e) {
				e.printStackTrace();
				showMessageBoxError(e.toString());
			}
		});
	}

	private boolean updateIssueFromMailItem() throws IOException {
		boolean succ = false;
		try {
			final String subject = mailItem.getSubject();
			final String description = mailItem.getBody();
			IssueService srv = Globals.getIssueService();
			String issueId = srv.extractIssueIdFromMailSubject(subject);
			if (issueId != null && issueId.length() != 0) {
				issue = srv.readIssue(issueId);
			} else {
				issue = srv.createIssue(subject, description);
			}
			succ = true;
		} catch (Throwable e) {
			e.printStackTrace();
			String text = e.toString();
			if (text.indexOf("404") >= 0) {
				text = resb.getString("Error.IssueNotFound");
			}
			showMessageBoxError(text);
		}
		return succ;
	}

	public IssueMailItem getMailItem() {
		return mailItem;
	}

	public Object getWindowOwner() {
		return windowOwner;
	}

	public void setWindowOwner(Object w) {
		windowOwner = w;
	}

	public boolean isNew() {
		String issueId = issue != null ? issue.getId() : "";
		return issueId == null || issueId.length() == 0;
	}

	@Override
	public void close() {
		super.close();
		// Globals.getThisAddin().getRegistry().writeFields(this);
		for (Runnable run : resourcesToRelease) {
			try {
				run.run();
			} catch (Throwable ignored) {
			}
		}

		if (detectIssueModifiedTimer != null) {
			detectIssueModifiedTimer.stop();
		}

		attachmentHelper.releaseResources();
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL fxmlURL = classLoader.getResource("com/wilutions/itol/NewIssue9.fxml");

			resb = Globals.getResourceBundle();

			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			} // Do not create a new IssueTaskPane object.
			);
			Parent p = loader.load();

			Scene scene = new Scene(p);
			scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

			// ScenicView.show(scene);

			return scene;
		} catch (Throwable e) {
			throw new IllegalStateException("Cannot create scene.", e);
		}
	}

	@Override
	public void showAsync(final CustomTaskPane taskPane, AsyncResult<Boolean> asyncResult) throws ComException {
		try {

			super.showAsync(taskPane, (succ, ex) -> {
				if (succ) {
					try {
						initDescription();
						initNotes();
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

	@Override
	// This method is called by the FXMLLoader when initialization is complete
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		try {

			cbTracker.valueProperty().addListener(new ComboboxChangeListener(cbTracker, Property.ISSUE_TYPE));
			cbStatus.valueProperty().addListener(new ComboboxChangeListener(cbStatus, Property.STATUS));
			cbPriority.valueProperty().addListener(new ComboboxChangeListener(cbPriority, Property.PRIORITY));
			cbCategory.valueProperty().addListener(new ComboboxChangeListener(cbCategory, Property.CATEGORY));

			initialUpdate();

			detectIssueModifiedTimer = new Timeline(new KeyFrame(Duration.seconds(0.5),
					new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							try {
								updateData(true);
								if (firstModifiedCheck) {
									firstModifiedCheck = false;
									// Backup issue
									issueCopy = (Issue) issue.clone();
//									System.out.println("issueCopy=" + issueCopy.getLastUpdate().getProperties());
								} else {
									boolean eq = issue.equals(issueCopy);
									setModified(!eq);
//									if (modified) {
//										System.out.println("modified issue=" + issue.getLastUpdate().getProperties());
//									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}));
			detectIssueModifiedTimer.setCycleCount(Timeline.INDEFINITE);
			detectIssueModifiedTimer.play();

		} catch (Throwable e) {
			e.printStackTrace();
			showMessageBoxError(e.toString());
		}
	}

	private void setModified(boolean modified) {
		this.modified = modified;
		initModified();
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
		if (saveAndValidate) {

			saveSubject();

			saveDescription();

			saveNotes();

			saveChoiceBox(cbTracker, Property.ISSUE_TYPE);

			saveChoiceBox(cbStatus, Property.STATUS);

			saveChoiceBox(cbPriority, Property.PRIORITY);

			saveChoiceBox(cbCategory, Property.CATEGORY);

			saveProperties();

			saveAttachments();

		} else {

			initIssueId();

			initModified();

			initSubject();

			initDescription();

			initNotes();

			initChoiceBox(cbTracker, Property.ISSUE_TYPE);

			initChoiceBox(cbCategory, Property.CATEGORY);

			initChoiceBox(cbPriority, Property.PRIORITY);

			initChoiceBox(cbStatus, Property.STATUS);

			initProperties();

			initAttachments();

			initHistory();

		}
	}

	private void saveAttachments() {
		ArrayList<Attachment> atts = new ArrayList<Attachment>(tabAttachments.getItems());
		issue.setAttachments(atts);
	}

	private void initModified() {
		if (modified) {
			bnAssignSelection.setSelected(false);
		}
		boolean enabled = modified || isNew();
		bnUpdate.setDisable(!enabled);
	}

	private void initIssueId() {
		edIssueId.setText(issue.getId());
	}

	private void saveProperties() {
		propGridView.saveProperties(issue);
	}

	private void saveChoiceBox(ChoiceBox<IdName> cb, String propertyId) {
		IdName idn = cb.getValue();
		if (idn != null && !idn.equals(IdName.NULL)) {
			Property prop = new Property(propertyId, idn.getId());
			issue.getLastUpdate().setProperty(prop);
		} else {
			issue.getLastUpdate().removeProperty(propertyId);
		}
	}

	private void saveDescription() {
		if (descriptionHtmlEditor != null) {
			saveTextFromHtmlEditor(descriptionHtmlEditor, webDescription, Property.DESCRIPTION);
		} else {
			String text = edDescription.getHtmlText().trim();
			issue.setDescription(text);
		}
	}

	private void saveNotes() {
		if (notesHtmlEditor != null) {
			saveTextFromHtmlEditor(notesHtmlEditor, webNotes, Property.NOTES);
		} else {
			String text = edNotes.getHtmlText().trim();
			issue.setPropertyString(Property.NOTES, text);
		}
	}

	private void saveTextFromHtmlEditor(IssueHtmlEditor ed, WebView web, String propertyId) {
		String elementId = ed.getElementId();
		String scriptToGetDescription = "document.getElementById('" + elementId + "')";
		try {
			JSObject elm = (JSObject) web.getEngine().executeScript(scriptToGetDescription);
			if (elm != null) {
				String text = (String) elm.getMember("value");
				issue.setPropertyString(propertyId, text);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void saveSubject() {
		issue.setSubject(edSubject.getText().trim());
	}

	private void initHistory() throws IOException {
		if (issue.getId() != null && issue.getId().length() != 0) {
			IssueService srv = Globals.getIssueService();
			String url = srv.getIssueHistoryUrl(issue.getId());
			webHistory.getEngine().load(url);
		}
	}

	private void initAttachments() {
		if (tabAttachmentsApplyHandler) {
			AttachmentTableViewHandler.apply(tabAttachments);

			tabAttachments.setOnMouseClicked((click) -> {
				if (click.getClickCount() == 2) {
					showSelectedIssueAttachment();
				}
			});

			tabAttachmentsApplyHandler = false;
		}

		// Copy only non-deleted attachments to backing list.
		List<Attachment> atts = new ArrayList<Attachment>(issue.getAttachments().size());
		for (Attachment att : issue.getAttachments()) {
			if (!att.isDeleted()) {
				atts.add(att);
			}
		}

		ObservableList<Attachment> obs = FXCollections.observableList(atts);
		tabAttachments.setItems(obs);
	}

	private void initProperties() throws IOException {
		if (propGridView == null) {
			propGridView = new PropertyGridView(propGrid);
		}
		propGridView.initProperties(issue);
	}

	private void initSubject() {
		edSubject.setText(issue.getSubject());
	}

	private void initDescription() throws IOException {
		if (webDescription != null) {
			descriptionHtmlEditor = Globals.getIssueService().getHtmlEditor(issue, Property.DESCRIPTION);
			webDescription.getEngine().loadContent(descriptionHtmlEditor.getHtmlContent());
		} else {
			edDescription.setHtmlText(issue.getDescription());
		}
	}

	private void initNotes() throws IOException {
		if (webNotes != null) {
			notesHtmlEditor = Globals.getIssueService().getHtmlEditor(issue, Property.NOTES);
			webNotes.getEngine().loadContent(notesHtmlEditor.getHtmlContent());
		} else {
			edNotes.setHtmlText(issue.getPropertyString(Property.NOTES, ""));
		}
	}

	private void initChoiceBox(ChoiceBox<IdName> cb, String propertyId) throws IOException {
		IssueService srv = Globals.getIssueService();
		Property prop = issue.getLastUpdate().getProperty(propertyId);
		PropertyClass pclass = srv.getPropertyClass(propertyId, issue);
		if (pclass != null) {
			List<IdName> items = pclass.getSelectList();
			cb.setItems(FXCollections.observableArrayList(items));
			String value = "";
			if (prop != null) {
				Object propValue = prop.getValue();
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
		}
	}

	private void initialUpdate() throws IOException {

		attachmentHelper.initialUpdate(mailItem, issue);

		initalUpdateAttachmentView();

		// Show/Hide History and Notes
		addOrRemoveTab(tpHistory, !isNew());
		addOrRemoveTab(tpNotes, !isNew());
		tabpIssue.getSelectionModel().select(tpDescription);

		bnShowIssueInBrowser.setDisable(isNew());
		bnUpdate.setText(resb.getString(isNew() ? "bnUpdate.text.create" : "bnUpdate.text.update"));

		descriptionHtmlEditor = Globals.getIssueService().getHtmlEditor(issue, Property.DESCRIPTION);
		if (descriptionHtmlEditor != null) {
			edDescription.setVisible(false);
			webDescription = new WebView();
			hboxDescription.getChildren().clear();
			hboxDescription.getChildren().add(webDescription);
			hboxDescription.setStyle("-fx-border-color: LIGHTGREY;-fx-border-width: 1px;");
		}

		notesHtmlEditor = Globals.getIssueService().getHtmlEditor(issue, Property.NOTES);
		if (notesHtmlEditor != null) {
			edNotes.setVisible(false);
			webNotes = new WebView();
			hboxNotes.getChildren().clear();
			hboxNotes.getChildren().add(webNotes);
			hboxNotes.setStyle("-fx-border-color: LIGHTGREY;-fx-border-width: 1px;");
		}

		modified = false;
		firstModifiedCheck = true;

		updateData(false);
	}

	private void addOrRemoveTab(Tab t, boolean add) {
		if (add) {
			for (Tab p : tabpIssue.getTabs()) {
				if (p == t)
					return;
			}
			tabpIssue.getTabs().add(t);
		} else {
			tabpIssue.getTabs().remove(t);
		}
	}

	private void showSelectedIssueAttachment() {
		Attachment att = tabAttachments.getSelectionModel().getSelectedItem();
		if (att != null) {
			String url = att.getUrl();
			IssueApplication.showDocument(url);
		}
	}

	@FXML
	public void onShowAttachment() {
		showSelectedIssueAttachment();
	}

	@FXML
	public void onAddAttachment() {
		try {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Resource File");
			List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
			if (selectedFiles != null) {
				for (File file : selectedFiles) {
					Attachment att = AttachmentHelper.createFromFile(file);
					tabAttachments.getItems().add(att);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void onRemoveAttachment() {
		List<Attachment> selectedItems = tabAttachments.getSelectionModel().getSelectedItems();

		for (int i = 0; i < selectedItems.size(); i++) {
			Attachment att = selectedItems.get(i);
			if (att.getId() != null && att.getId().length() != 0) {
				// Mark existing attachment deleted.
				// The attachment is deleted in IssueService.updateIssue().
				// --- THIS IS CURRENTLY NOT SUPPORTED OVER REDMINE API ---
				att.setDeleted(true);
			} else {
				issue.getAttachments().remove(att);
			}
		}

		initalUpdateAttachmentView();
	}

	private void initalUpdateAttachmentView() {
		// The TableView does not refresh it's items on getItems().remove(...)
		// Other devs had this problem with JavaFX 2.1
		// http://stackoverflow.com/questions/11065140/javafx-2-1-tableview-refresh-items
		// But the suggested workarounds do not help.

		hboxAttachments.getChildren().remove(tabAttachments);

		tabAttachments = new TableView<Attachment>();
		tabAttachmentsApplyHandler = true;
		initAttachments();
		hboxAttachments.getChildren().add(0, tabAttachments);
		HBox.setHgrow(tabAttachments, Priority.ALWAYS);
	}

	/**
	 * Return owner for sub-dialogs. This function is overridden in
	 * {@link DlgTestIssueTaskPane}.
	 * 
	 * @return owner object
	 */
	protected Object getDialogOwner() {
		return this;
	}

	private void showMessageBoxError(String text) {
		detectIssueModifiedTimer.stop();
		String title = resb.getString("MessageBox.title.error");
		String ok = resb.getString("Button.OK");
		Object owner = getDialogOwner();
		MessageBox.create(owner).title(title).text(text).button(1, ok).bdefault().show((btn, ex) -> {
			detectIssueModifiedTimer.play();
		});
	}

	private void queryDiscardChangesAsync(AsyncResult<Boolean> asyncResult) {
		if (modified) {
			detectIssueModifiedTimer.stop();
			String title = resb.getString("MessageBox.title.confirm");
			String text = resb.getString("MessageBox.queryDiscardChanges.text");
			String yes = resb.getString("Button.Yes");
			String no = resb.getString("Button.No");
			Object owner = getDialogOwner();
			MessageBox.create(owner).title(title).text(text).button(1, yes).button(0, no).bdefault()
					.show((btn, ex) -> {
						Boolean succ = btn != null && btn != 0;
						asyncResult.setAsyncResult(succ, ex);
						detectIssueModifiedTimer.play();
					});
		} else {
			asyncResult.setAsyncResult(true, null);
		}
	}

	@FXML
	public void onClear() {
		queryDiscardChangesAsync((succ, ex) -> {
			if (ex == null && succ) {
				bnAssignSelection.setSelected(false);
				internalSetMailItem(new IssueMailItemBlank());
			}
		});
	}

	@FXML
	public void onAssignSelection() {
		boolean sel = bnAssignSelection.isSelected();
		if (sel) {
			queryDiscardChangesAsync((succ, ex) -> {
				if (ex == null && succ) {
					try {
						Explorer explorer = Globals.getThisAddin().getApplication().ActiveExplorer().as(Explorer.class);
						MyExplorerWrapper myExplorer = Globals.getThisAddin().getMyExplorerWrapper(explorer);
						myExplorer.showSelectedMailItem();
					} catch (Throwable e) {

					}
				} else {
					bnAssignSelection.setSelected(false);
				}
			});
		}
	}

	@FXML
	public void onShowExistingIssue() {
		queryDiscardChangesAsync((succ, ex) -> {
			if (ex == null && succ) {
				final String issueId = edIssueId.getText();
				IssueMailItem mitem = new IssueMailItemBlank() {
					public String getSubject() {
						return "[R-" + issueId + "]";
					}
				};
				internalSetMailItem(mitem);
			}
		});
	}

	@FXML
	public void onShowIssueInBrowser() {
		try {
			IssueService srv = Globals.getIssueService();
			String url = srv.getShowIssueUrl(issue.getId());
			IssueApplication.showDocument(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void onNextPage() {
		int idx = tabpIssue.getSelectionModel().getSelectedIndex();
		if (idx >= 0) {
			if (++idx >= tabpIssue.getTabs().size()) {
				idx = 0;
			}
		} else {
			idx = 0;
		}
		tabpIssue.getSelectionModel().select(idx);

		Node node = null;
		switch (idx) {
		case 0: // DESCRIPTION
			node = webDescription != null ? webDescription : edDescription;
			break;
		case 1: // PROPERTIES
			node = propGridView.getFirstControl();
			break;
		case 2: // ATTACHMENTS
			node = tabAttachments.getItems().size() != 0 ? tabAttachments : bnAddAttachment;
			break;
		case 3: // HISTORY
			node = webHistory;
			break;
		case 4: // NOTES
			node = webNotes != null ? webNotes : edNotes;
			break;
		}
		if (node != null) {
			final Node nodeF = node;
			BackgTask.run(() -> {
				try {
					Thread.sleep(200);
					setNodeFocusLater(nodeF);
				} catch (Throwable e) {
				}
			});
		}
	}

	private void setNodeFocusLater(Node node) {
		Platform.runLater(() -> {
			if (node == webDescription || node == webNotes) {
				BackgTask.run(() -> {
					try {
						Thread.sleep(200);
						focuseWebViewHtmlEditor((WebView) node, descriptionHtmlEditor);
					} catch (Exception e) {
					}
				});
			}
			node.requestFocus();
			System.out.println("focus " + node);
		});
	}

	private void focuseWebViewHtmlEditor(WebView webView, IssueHtmlEditor descriptionHtmlEditor) {
		Platform.runLater(() -> {
			String elementId = descriptionHtmlEditor.getElementId();
			String scriptToFocusControl = "document.getElementById('" + elementId + "').focus()";
			try {
				webView.getEngine().executeScript(scriptToFocusControl);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	private class ComboboxChangeListener implements ChangeListener<IdName> {

		@SuppressWarnings("unused")
		final ChoiceBox<IdName> cb;
		@SuppressWarnings("unused")
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

				try {
					updateData(true);

					IssueService srv = Globals.getIssueService();
					issue = srv.validateIssue(issue);

					updateData(false);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class ProgressPane extends DlgProgress {

		public ProgressPane() {
			super("");
		}

		@Override
		public void close() {
			super.close();
			Platform.runLater(() -> {
				
				// Replace progress dialog with issue view.
				vboxRoot.getChildren().clear();
				vboxRoot.getChildren().add(rootGrid);
				
				// Update issue view
				try {
					initialUpdate();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	@FXML
	public void onUpdate() {

		final DlgProgress dlgProgress = new ProgressPane();
		detectIssueModifiedTimer.stop();

		try {
			IssueService srv = Globals.getIssueService();
		
			// Replace issue view with progress dialog
			{
				Parent p = dlgProgress.load();
				vboxRoot.getChildren().clear();
				vboxRoot.getChildren().add(p);
			}
	
			// Save dialog elements into data members
			updateData(true);

			// Compute bytes to upload
			long totalBytes = 0;
			for (Attachment att : issue.getAttachments()) {
				// Only new attachments are uploaded
				if (att.getId().isEmpty()) {
					// getContentLength() might store the attachment in a temporary directory.
					totalBytes += att.getContentLength();
				}
			}

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
					boolean isNew = issue.getId().length() == 0;

					// Create issue
					issue = srv.updateIssue(issue, progressCallback);

					// If process was not cancelled...
					if (dlgProgress.getResult()) {

						// ... save the mail with different subject
						if (isNew) {
							saveMailWithIssueId(progressCallback);
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
						
						String text = resb.getString("Error.FailedToUpdateIssue");
						text += " " + e.toString();
						showMessageBoxError(text);
						
						if (dlgProgress != null) {
							dlgProgress.finish(false);
						}
					}
				}
			});

		} catch (Throwable e) {
			e.printStackTrace();
			
			String text = resb.getString("Error.FailedToUpdateIssue");
			text += " " + e.toString();
			showMessageBoxError(text);
			
			if (dlgProgress != null) {
				dlgProgress.finish(false);
			}
		} finally {
		}
	}

	private void saveMailWithIssueId(final ProgressCallback progressCallback) throws IOException {
		IssueService srv = Globals.getIssueService();
		String mailSubjectPrev = mailItem.getSubject();
		String mailSubject = srv.injectIssueIdIntoMailSubject(mailSubjectPrev, issue);
		if (!mailSubjectPrev.equals(mailSubject)) {
			mailItem.setSubject(mailSubject);
			progressCallback.setParams("Save mail as \"" + mailSubject + "\n");
			mailItem.Save();
		}
	}

	@Override
	public void onVisibleStateChange(_CustomTaskPane ctp) throws ComException {
		Globals.getThisAddin().getRibbon().InvalidateControl("NewIssue");
	}
}
