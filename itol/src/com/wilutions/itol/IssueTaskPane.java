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
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
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
import com.wilutions.itol.db.ProgressCallbackImpl;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.mslib.office.CustomTaskPane;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.office._CustomTaskPane;

public class IssueTaskPane extends TaskPaneFX implements Initializable {

	private volatile Issue issue = new Issue();
	private Issue issueCopy;
	private Logger log = Logger.getLogger("IssueTaskPane");

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
	private MenuItem bnShowIssueInBrowser;
	@FXML
	private ChoiceBox<IdName> cbTracker;
	@FXML
	private ChoiceBox<IdName> cbProject;
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
	@FXML
	private CheckMenuItem mnInjectIssueIdIntoSubject;
	@FXML
	private ProgressBar pgProgress;

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
	private volatile boolean firstModifiedCheck;
	private AttachmentHelper attachmentHelper = new AttachmentHelper();
	private MyWrapper inspectorOrExplorer;

	/**
	 * Owner window for child dialogs (message boxes)
	 */
	private Object windowOwner;

	public IssueTaskPane(MyWrapper inspectorOrExplorer) {
		this.inspectorOrExplorer = inspectorOrExplorer;
		this.mailItem = inspectorOrExplorer.getSelectedItem();

		this.resb = Globals.getResourceBundle();
		Globals.getRegistry().readFields(this);

		updateIssueFromMailItem(null);
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

				detectIssueModifiedStop();

				updateIssueFromMailItem((succ, ex) -> {
					if (succ) {
						Platform.runLater(() -> {
							try {
								initialUpdate();
							} catch (Exception e) {
								log.log(Level.SEVERE, "initialUpdate failed", e);
							}
						});
					}
				});

		});
	}

	private class MyFakeProgressCallback extends MyProgressCallback {

		final Timeline fakeProgressTimer;
		private boolean finished;

		MyFakeProgressCallback() {
			super.setTotal(1);

			fakeProgressTimer = new Timeline(new KeyFrame(Duration.seconds(0.1), new EventHandler<ActionEvent>() {
				double x = 0;

				@Override
				public void handle(ActionEvent event) {
					x += 1;
					syncSetProgress(1 * (-1 / x + 1));
				}
			}));
			fakeProgressTimer.setCycleCount(Timeline.INDEFINITE);
			fakeProgressTimer.play();
		}
		
		private synchronized void syncSetProgress(double q) {
			if (!finished) {
				internalSetProgress(q);
			}
		}

		@Override
		public synchronized void setFinished() {
			finished = true;
			fakeProgressTimer.stop();
			super.setFinished();
		}

		@Override
		public void setTotal(double total) {
		}

		@Override
		public void setProgress(double current) {
		}
	}

	private void updateIssueFromMailItem(AsyncResult<Boolean> asyncResult) {

		final ProgressCallback progressCallback = new MyFakeProgressCallback();

		BackgTask.run(() -> {

			IssueService srv = null;
			boolean succ = false;
			try {

				// Get issue ID from mailItem
				final String subject = mailItem.getSubject();
				final String description = mailItem.getBody();
				srv = Globals.getIssueService();
				String issueId = srv.extractIssueIdFromMailSubject(subject);

				// If issue ID found...
				if (issueId != null && issueId.length() != 0) {

					// read issue
					issue = srv.readIssue(issueId);
				} else {

					// ... no issue ID: create blank issue
					String defaultIssueAsString = (String) Globals.getRegistry().read(Globals.REG_defaultIssueAsString);
					if (defaultIssueAsString == null) {
						defaultIssueAsString = "";
					}
					issue = srv.createIssue(subject, description, defaultIssueAsString);

					Thread.sleep(300);
				}

				succ = true;

			} catch (Throwable e) {

				String text = e.toString();
				if (text.indexOf("404") >= 0) {
					text = resb.getString("Error.IssueNotFound");
					log.log(Level.INFO, text, e);
				} else {
					log.log(Level.SEVERE, text, e);
				}

				if (srv != null) {
					showMessageBoxError(text);
				}

			} finally {

				progressCallback.setFinished();

				if (asyncResult != null) {
					asyncResult.setAsyncResult(succ, null);
				}
			}
		});
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
		Globals.getRegistry().writeFields(this);
		for (Runnable run : resourcesToRelease) {
			try {
				run.run();
			} catch (Throwable ignored) {
			}
		}

		detectIssueModifiedStop();

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
						log.log(Level.WARNING, "", e);
						ex = e;
					}
				}

				if (asyncResult != null) {
					asyncResult.setAsyncResult(succ, ex);
				}
			});

		} catch (Throwable e) {
			log.log(Level.WARNING, "", e);
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
			cbProject.valueProperty().addListener(new ComboboxChangeListener(cbProject, Property.PROJECT));

			initialUpdate();

			detectIssueModifiedTimer = new Timeline(new KeyFrame(Duration.seconds(0.3),
					new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							try {
								updateData(true);
								if (firstModifiedCheck) {
									firstModifiedCheck = false;
									// Backup issue
									issueCopy = (Issue) issue.clone();
									// System.out.println("issueCopy=" +
									// issueCopy.getLastUpdate().getProperties());
								} else {
									boolean eq = issue.equals(issueCopy);
									setModified(!eq);
									// if (modified) {
									// System.out.println("modified issue=" +
									// issue.getLastUpdate().getProperties());
									// }
								}
							} catch (IOException e) {
								log.log(Level.WARNING, "", e);
							}
						}
					}));
			detectIssueModifiedTimer.setCycleCount(Timeline.INDEFINITE);
			detectIssueModifiedStart();
			
			// Press Assign button when called from inspector.
			if (inspectorOrExplorer instanceof InspectorWrapper) {
				bnAssignSelection.setSelected(true);
			}
			// Show defaults when called from explorer.
			else {
				internalSetMailItem(new IssueMailItemBlank());
			}
			
			
		} catch (Throwable e) {
			log.log(Level.WARNING, "", e);
			showMessageBoxError(e.toString());
		}
	}

	private void detectIssueModifiedStop() {
		if (detectIssueModifiedTimer != null) {
			detectIssueModifiedTimer.stop();
		}
	}

	private void detectIssueModifiedStart() {
		if (detectIssueModifiedTimer != null) {
			firstModifiedCheck = true;
			detectIssueModifiedTimer.play();
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

			saveChoiceBox(cbProject, Property.PROJECT);

			saveProperties();

			saveAttachments();

		} else {

			initIssueId();

			initModified();

			initSubject();

			initDescription();

			initNotes();

			initChoiceBox(cbTracker, Property.ISSUE_TYPE);

			initChoiceBox(cbProject, Property.PROJECT);

			initChoiceBox(cbPriority, Property.PRIORITY);

			initChoiceBox(cbStatus, Property.STATUS);

			initProperties();

			initAttachments();

			initHistory();

			bnUpdate.setDisable(issue.getSubject().isEmpty());
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
		bnUpdate.setDisable(!enabled || issue.getSubject().isEmpty());
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
			log.log(Level.WARNING, "", e);
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
			AttachmentTableViewHandler.apply(attachmentHelper, tabAttachments);

			tabAttachments.setOnMouseClicked((click) -> {
				if (click.getClickCount() == 2) {
					showSelectedIssueAttachment();
				}
			});

			// This selection listener disables the "Remove" button, if
			// an already uploaded attachment is selected. Attachments
			// cannot be deleted via Redmine API.
			tabAttachments.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Attachment>() {
				@Override
				public void onChanged(javafx.collections.ListChangeListener.Change<? extends Attachment> c) {
					boolean hasId = false;
					for (Attachment att : c.getList()) {
						hasId = att.getId().isEmpty();
						if (hasId) {
							break;
						}
					}
					bnRemoveAttachment.setDisable(!hasId);
				}

			});

			tabAttachmentsApplyHandler = false;
		}

		// Copy attachments to backing list.
		List<Attachment> atts = new ArrayList<Attachment>(issue.getAttachments().size());
		for (Attachment att : issue.getAttachments()) {
			if (!att.isDeleted()) { // obsolete: cannot delete attachments via
									// Redmine API
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
		if (issue != null) {
			if (webDescription != null) {
				descriptionHtmlEditor = Globals.getIssueService().getHtmlEditor(issue, Property.DESCRIPTION);
				webDescription.getEngine().loadContent(descriptionHtmlEditor.getHtmlContent());
			} else {
				edDescription.setHtmlText(issue.getDescription());
			}
		}
	}

	private void initNotes() throws IOException {
		if (issue != null) {
			if (webNotes != null) {
				notesHtmlEditor = Globals.getIssueService().getHtmlEditor(issue, Property.NOTES);
				webNotes.getEngine().loadContent(notesHtmlEditor.getHtmlContent());
			} else {
				edNotes.setHtmlText(issue.getPropertyString(Property.NOTES, ""));
			}
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

		Boolean injectId = (Boolean) Globals.getRegistry().read(Globals.REG_injectIssueIdIntoMailSubject);
		mnInjectIssueIdIntoSubject.setSelected(injectId == null || injectId);

		modified = false;

		updateData(false);

		detectIssueModifiedStart();
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
			log.log(Level.WARNING, "", e);
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
				// att.setDeleted(true);
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
		detectIssueModifiedStop();
		String title = resb.getString("MessageBox.title.error");
		String ok = resb.getString("Button.OK");
		Object owner = getDialogOwner();
		MessageBox.create(owner).title(title).text(text).button(1, ok).bdefault().show((btn, ex) -> {
			detectIssueModifiedStart();
		});
	}

	private void queryDiscardChangesAsync(AsyncResult<Boolean> asyncResult) {
		if (modified) {
			detectIssueModifiedStop();
			String title = resb.getString("MessageBox.title.confirm");
			String text = resb.getString("MessageBox.queryDiscardChanges.text");
			String yes = resb.getString("Button.Yes");
			String no = resb.getString("Button.No");
			Object owner = getDialogOwner();
			MessageBox.create(owner).title(title).text(text).button(1, yes).button(0, no).bdefault()
					.show((btn, ex) -> {
						Boolean succ = btn != null && btn != 0;
						asyncResult.setAsyncResult(succ, ex);
						detectIssueModifiedStart();
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
						IssueMailItem mailItem = inspectorOrExplorer.getSelectedItem();
						IssueTaskPane.this.setMailItem(mailItem);
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

				bnAssignSelection.setSelected(false);

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
			log.log(Level.WARNING, "", e);
		}
	}

	@FXML
	public void onNextPage() {

		// Select the next tab
		int idx = tabpIssue.getSelectionModel().getSelectedIndex();
		if (idx >= 0) {
			if (++idx >= tabpIssue.getTabs().size()) {
				idx = 0;
			}
		} else {
			idx = 0;
		}
		tabpIssue.getSelectionModel().select(idx);

		// Determine which control should receive the focus
		Node node = null;
		switch (idx) {
		case 0: // DESCRIPTION
			node = webDescription != null ? webDescription : edDescription;
			break;
		case 1: // PROPERTIES
			node = propGridView.getFirstControl();
			break;
		case 2: // ATTACHMENTS
			node = bnAddAttachment;
			break;
		case 3: // HISTORY
			// Focus should stay on the next button
			break;
		case 4: // NOTES
			node = webNotes != null ? webNotes : edNotes;
			break;
		}

		// Focus control
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
				log.log(Level.WARNING, "", e);
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
					log.log(Level.WARNING, "", e);
				}
			}
		}

	}

	private class MyProgressCallback extends ProgressCallbackImpl {

		private int lastPercent = 0;

		public MyProgressCallback() {
			super("");
		}

		public void setProgress(final double current) {
			internalSetProgress(current);
		}

		protected void internalSetProgress(final double current) {
			super.setProgress(current);
			double currentSum = childSum + current;
			final double quote = currentSum / total;
			int percent = (int) Math.ceil(100.0 * quote);
			if (percent > lastPercent) {
				lastPercent = percent;
			}
			Platform.runLater(() -> {
				if (pgProgress != null) {
					pgProgress.setProgress(quote);
//					System.out.println("progress " + quote);
				}
			});
		}

		@Override
		public void setParams(String... params) {
			super.setParams(params);
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public void setFinished() {
			Platform.runLater(() -> {
				if (pgProgress != null) {
					pgProgress.setProgress(0);
//					System.out.println("progress 0");
				}
			});
		}

	}

	@FXML
	public void onUpdate() {

		final ProgressCallback progressCallback = isNew() ? new MyProgressCallback() : new MyFakeProgressCallback();
		detectIssueModifiedStop();

		try {
			IssueService srv = Globals.getIssueService();

			// Save dialog elements into data members
			updateData(true);

			// Compute bytes to upload
			long totalBytes = 0;
			for (Attachment att : issue.getAttachments()) {
				// Only new attachments are uploaded
				if (att.getId().isEmpty()) {
					// getContentLength() might store the attachment in a
					// temporary directory.
					totalBytes += att.getContentLength();
				}
			}

			// Total bytes to upload: #attachment-bytes + some bytes for the
			// issue's JSON object
			progressCallback.setTotal(totalBytes + 20 * 1000);

			// Create issue in background.
			// Currently, we are in the UI thread. The progress dialog would not
			// be updated,
			// If we processed updating the issue here.
			BackgTask.run(() -> {
				try {

					progressCallback.setProgress(4 * 1000);

					boolean isNew = updateIssueChangedMembers(srv, progressCallback);

					// ... save the mail with different subject
					if (isNew) {
						saveMailWithIssueId(progressCallback);
					}

					Platform.runLater(() -> {
						try {
							initialUpdate();

							progressCallback.setFinished();

						} catch (Exception e) {
							log.log(Level.SEVERE, "Failed to read issue data into UI controls.", e);
						}
					});

				} catch (Throwable e) {

					if (!progressCallback.isCancelled()) {
						log.log(Level.SEVERE, "Failed to update issue", e);

						String text = resb.getString("Error.FailedToUpdateIssue");
						text += " " + e.toString();
						showMessageBoxError(text);

						progressCallback.setFinished();
						detectIssueModifiedStart();
					}
				}
			});

		} catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to update issue", e);

			String text = resb.getString("Error.FailedToUpdateIssue");
			text += " " + e.toString();
			showMessageBoxError(text);

			progressCallback.setFinished();
			detectIssueModifiedStart();
		}
	}

	private boolean updateIssueChangedMembers(IssueService srv, final ProgressCallback progressCallback)
			throws IOException {
		boolean isNew = issue.getId().length() == 0;

		List<String> modifiedProperties = new ArrayList<String>();
		issueCopy.findChangedMembers(issue, modifiedProperties);

		// Create issue
		issue = srv.updateIssue(issue, modifiedProperties, progressCallback);
		return isNew;
	}

	private void saveMailWithIssueId(final ProgressCallback progressCallback) throws IOException {
		Boolean injectId = (Boolean) Globals.getRegistry().read(Globals.REG_injectIssueIdIntoMailSubject);
		if (injectId == null || injectId) {
			IssueService srv = Globals.getIssueService();
			String mailSubjectPrev = mailItem.getSubject();
			String mailSubject = srv.injectIssueIdIntoMailSubject(mailSubjectPrev, issue);
			if (!mailSubjectPrev.equals(mailSubject)) {
				mailItem.setSubject(mailSubject);
				progressCallback.setParams("Save mail as \"" + mailSubject + "\n");
				mailItem.Save();
			}
		}
	}

	@Override
	public void onVisibleStateChange(_CustomTaskPane ctp) throws ComException {
		IRibbonUI ribbon = Globals.getThisAddin().getRibbon();
		if (ribbon != null) {
			ribbon.InvalidateControl("NewIssue");
		}
	}

	@FXML
	public void onSaveAsDefault() {
		try {
			String str = Globals.getIssueService().getDefaultIssueAsString(issue);
			Globals.getRegistry().write(Globals.REG_defaultIssueAsString, str);
		} catch (IOException e) {
			showMessageBoxError(e.toString());
		}
	}

	@FXML
	public void onInjectIssueIdIntoSubject() {
		try {
			Boolean succ = mnInjectIssueIdIntoSubject.isSelected();
			Globals.getRegistry().write(Globals.REG_injectIssueIdIntoMailSubject, succ);
			IssueService srv = Globals.getIssueService();
			String oldSubject = mailItem.getSubject();
			String newSubject = srv.injectIssueIdIntoMailSubject(oldSubject, succ ? issue : null);
			if (!oldSubject.equals(newSubject)) {
				mailItem.setSubject(newSubject);
				mailItem.Save();
			}
		} catch (Exception e) {
			showMessageBoxError(e.toString());
		}
	}

	@Override
	public void setVisible(final boolean v) throws ComException {
		super.setVisible(v);
		if (v) {
			if (bnAssignSelection != null && !bnAssignSelection.isSelected() && !modified) {
				internalSetMailItem(new IssueMailItemBlank());
			}
		}
	}

}