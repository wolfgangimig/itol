/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.BackgTask;
import com.wilutions.com.ComException;
import com.wilutions.com.Dispatch;
import com.wilutions.fx.acpl.AutoCompletionBinding;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.Default;
import com.wilutions.itol.db.DefaultSuggest;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssuePropertyEditor;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.MailBodyConversion;
import com.wilutions.itol.db.MailInfo;
import com.wilutions.itol.db.MsgFileFormat;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.ProgressCallbackFactory;
import com.wilutions.itol.db.ProgressCallbackImpl;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.Suggest;
import com.wilutions.joa.TaskPanePosition;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.joa.outlook.ex.InspectorWrapper;
import com.wilutions.mslib.office.CustomTaskPane;
import com.wilutions.mslib.office.IRibbonUI;
import com.wilutions.mslib.office._CustomTaskPane;
import com.wilutions.mslib.outlook.Application;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlAttachmentType;
import com.wilutions.mslib.outlook.OlItemType;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import javafx.util.Duration;

public class IssueTaskPane extends TaskPaneFX implements Initializable, ProgressCallbackFactory {

	private volatile Issue issue = new Issue();

	@FXML
	private VBox vboxRoot;

	@FXML
	private GridPane rootGrid;
	@FXML
	private TextField edSubject;
	@FXML
	private HTMLEditor edDescription;
	@FXML
	private VBox boxDescription;
	@FXML
	private SplitMenuButton bnAssignSelection;
	@FXML
	private MenuItem bnClear;
	@FXML
	private CheckBox ckAssigned;
	@FXML
	private Button bnShow;
	@FXML
	private Button bnShowIssueInBrowser;
	@FXML
	private ComboBox<IdName> cbTracker;
	@FXML
	private ComboBox<IdName> cbProject;
	@FXML
	private ComboBox<IdName> cbPriority;
	@FXML
	private Tab tpDescription;
	@FXML
	private Tab tpHistory;
	@FXML
	private Tab tpNotes;
	@FXML
	private Tab tpAttachments;
	@FXML
	private VBox boxNotes;
	@FXML
	private Button bnCopyMailBody;
	@FXML
	private WebView webHistory;
	@FXML
	private Tab tpProperties;
	@FXML
	private TableView<Attachment> tabAttachments;
	@FXML
	private TabPane tabpIssue;
	@FXML
	private ComboBox<IdName> cbStatus;
	@FXML
	private Button bnUpdate;
	@FXML
	private Button bnShowAttachment;
	@FXML
	private MenuButton bnAddAttachment;
	@FXML
	private Button bnRemoveAttachment;
	@FXML
	private Button bnExportAttachments;
	@FXML
	private HBox hboxAttachments;
	@FXML
	private GridPane propGrid;
	@FXML
	private TextField edIssueId;
	@FXML
	private ProgressBar pgProgress;

	private AutoCompletionBinding<IdName> autoCompletionProject;
	private AutoCompletionBinding<IdName> autoCompletionTracker;
	private AutoCompletionBinding<IdName> autoCompletionPriority;
	private AutoCompletionBinding<IdName> autoCompletionStatus;

	private boolean tabAttachmentsApplyHandler = true;
	private SimpleIntegerProperty updateBindingToAttachmentList = new SimpleIntegerProperty();
	private Attachments observableAttachments = new Attachments();
	private AddAttachmentMenu addAttachmentMenu;

	private IssuePropertyEditor descriptionEditor;
	private IssuePropertyEditor notesEditor;
	private PropertyGridView propGridView;

	private IssueMailItem mailItem;
	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private ResourceBundle resb;
	private Timeline detectIssueModifiedTimer;
	private boolean modified;
	private MailAttachmentHelper attachmentHelper = new MailAttachmentHelper();
	private MyWrapper inspectorOrExplorer;
	private StandardContextMenu standardContextMenu = new StandardContextMenu();
	private static Logger log = Logger.getLogger("IssueTaskPane");

	/**
	 * Backup issue copy used to check for modifications. The
	 * detectIssueModifiedTimer frequently compares the {@link #issue} with this
	 * member to find out, if the issue has been modified.
	 */
	private Issue issueCopy;

	/**
	 * Is first check for modification after a new issue has been assigned. If
	 * this member is true, the detectIssueModifiedModifiedTimer creates a
	 * backup copy of the issue instead of checking for modifications. Creating
	 * the backup copy has to be delayed after the issue has been read, because
	 * custom properties with default values might be added to the issue in the
	 * first call to updateData(true).
	 */
	private volatile boolean firstModifiedCheck;

	/**
	 * Run this objects after backup copy of issue has been created. This is
	 * currently used to set the NEW NOTES after an issue has been read.
	 * 
	 * @see #updateIssueFromMailItem(AsyncResult)
	 */
	private List<Runnable> autoIssueModifications = new ArrayList<Runnable>();

	/**
	 * Owner window for child dialogs (message boxes)
	 */
	private Object windowOwner;
	
	/**
	 * This property is true, if the NOTES were extracted from the mail body.
	 */
	private volatile boolean tookNotesFromMail;

	/**
	 * True, if license is valid.
	 */
	private boolean licenseValid;

	public IssueTaskPane(MyWrapper inspectorOrExplorer) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "IssueTaskPane(");
		this.inspectorOrExplorer = inspectorOrExplorer;
		
		super.setDefaultWidth(550);

		this.mailItem = inspectorOrExplorer.getSelectedItem();

		this.resb = Globals.getResourceBundle();

		this.setPosition(Globals.getAppInfo().getConfig().getTaskPanePosition());
		
		this.licenseValid = License.isValid();

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")IssueTaskPane");
	}

	public void setMailItem(IssueMailItem mailItem) {
		// Task pane initialized?
		if (bnAssignSelection != null) {
			if (bnAssignSelection_isSelected()) {
				internalSetMailItem(mailItem, createProgressCallback("Set mail item"), (succ, ex) -> {});
			}
		}
	}

	private void internalSetMailItem(IssueMailItem mailItem, ProgressCallback cb, AsyncResult<Boolean> asyncResult) {
		this.mailItem = mailItem;
		this.tookNotesFromMail = false;

		Platform.runLater(() -> {

			detectIssueModifiedStop();

			updateIssueFromMailItem(cb.createChild(0.9), (succ, ex) -> {
				if (succ) {
					Platform.runLater(() -> {
						try {
							initialUpdate(cb.createChild(0.1));
						}
						catch (Exception e) {
							log.log(Level.WARNING, "initialUpdate failed", e);
						}
						finally {
							cb.setFinished();
							asyncResult.setAsyncResult(succ, ex);
						}
					});
				}
				else {
					cb.setFinished();
					asyncResult.setAsyncResult(succ, ex);
				}
			});

		});
	}

	private void updateIssueFromMailItem(ProgressCallback cb, AsyncResult<Boolean> asyncResult) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "updateIssueFromMailItem(");

		BackgTask.run(() -> {

			IssueService srv = null;
			boolean succ = false;
			try {
				srv = Globals.getIssueService();

				// Get issue ID from mailItem
				String subject = mailItem.getSubject();
				String issueId = srv.extractIssueIdFromMailSubject(subject);
				
				// Issue description from mail body.
				String textBody = mailItem.getBody().replace("\r\n", "\n");
				String description = textBody;
				if (Globals.getAppInfo().getConfig().getMailBodyConversion().equals(MailBodyConversion.MARKUP)) {
					String htmlBody = mailItem.getHTMLBody();
					String markup = Globals.getIssueService().convertHtmlBodyToMarkup(htmlBody);
					if (markup.length() > textBody.length()/2) {
						description = markup;
					}
				}

				issue = null;

				// If issue ID found...
				if (issueId != null && issueId.length() != 0) {

					// read issue
					issue = tryReadIssue(srv, subject, description, issueId, cb);
				}

				if (issue == null) {

					// ... no issue ID: create blank issue
					issue = srv.createIssue(subject, description, null, null, cb);
					
					assignMailAdressToAutoReplyField();

					Thread.sleep(300);
				}

				succ = true;

			}
			catch (Throwable e) {

				String text = e.toString();
				log.log(Level.SEVERE, text, e);

				if (srv != null) {
					showMessageBoxError(text);
				}

			}
			finally {

				cb.setFinished();

				if (asyncResult != null) {
					asyncResult.setAsyncResult(succ, null);
				}
			}
		});

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")updateIssueFromMailItem");
	}

	private void assignMailAdressToAutoReplyField() {
		String autoReplyField = Globals.getAppInfo().getConfig().getAutoReplyField();
		if (!Default.value(autoReplyField).isEmpty()) {
			String from = mailItem.getFromAddress();
			issue.setPropertyString(autoReplyField, from);
		}
	}

	private Issue tryReadIssue(IssueService srv, String subject, String description, String issueId, ProgressCallback cb)
			throws IOException {

		Issue ret = null;

		try {
			final Issue issue = srv.readIssue(issueId, cb);

			Date lastModified = issue.getLastModified();

			boolean newMail = mailItem.isNew();
			Date receivedTime = mailItem.getReceivedTime();

			// Set reply description (without original message) as issue
			// notes, if the mail is newer than the last update.
			if (newMail || lastModified.before(receivedTime)) {
				String replyDescription = IssueDescriptionParser.stripOriginalMessageFromReply(mailItem.getFrom(),
						mailItem.getTo(), subject, description);

				// Set NEW NOTES property after the backup copy
				// has been created. Otherwise the issue would not
				// bee seen as modified.
				autoIssueModifications.add(() -> {

					issue.setPropertyString(Property.NOTES, replyDescription);
					
					// ITJ-18: Send auto reply.
					// Mark that notes were copied from mail body.
					// This information is used to prevent an auto reply mail 
					// to be sent.  
					tookNotesFromMail = true;

					Platform.runLater(() -> {

						tabpIssue.getSelectionModel().select(tpNotes);

						try {
							attachmentHelper.initialUpdate(mailItem, issue);
							initalUpdateAttachmentView();
						}
						catch (Exception e) {
							log.log(Level.SEVERE, "Failed to update mail attachments.", e);
						}
					});

				});
			}

			ret = issue;
		}
		catch (Throwable e) {

			String text = e.toString();
			log.log(Level.SEVERE, text, e);
		}

		return ret;
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
		
		TaskPanePosition tpp = this.getPosition();
		Globals.getAppInfo().getConfig().setTaskPanePosition(tpp);
		Globals.getAppInfo().getConfig().write();

		for (Runnable run : resourcesToRelease) {
			try {
				run.run();
			}
			catch (Throwable ignored) {
			}
		}

		detectIssueModifiedStop();

		attachmentHelper.releaseResources();
		
		inspectorOrExplorer = null;
		mailItem = null;
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
		}
		catch (Throwable e) {
			throw new IllegalStateException("Cannot create scene.", e);
		}
	}

	@Override
	public void showAsync(final CustomTaskPane taskPane, AsyncResult<Boolean> asyncResult) throws ComException {
		super.showAsync(taskPane, asyncResult);
	}

	@Override
	// This method is called by the FXMLLoader when initialization is complete
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		try {
			edIssueId.setOnKeyPressed(new EventHandler<KeyEvent>() {
				public void handle(KeyEvent event) {
					if (event.getCode() == KeyCode.ENTER) {
						if (!edIssueId.getText().isEmpty()) {
							onShowExistingIssue();
							tabpIssue.requestFocus();
						}
					}
				}
			});
			
			bnAssignSelection.setTooltip(new Tooltip(resb.getString("nbAssignSelection.tooltip")));

			bnShow.disableProperty().bind(Bindings.isEmpty(edIssueId.textProperty()));
			bnShowIssueInBrowser.disableProperty().bind(Bindings.isEmpty(edIssueId.textProperty()));

			edSubject.requestFocus();

			IssueService srv = Globals.getIssueService();
			autoCompletionProject = initAutoComplete(srv, cbProject, Property.PROJECT, true);
			autoCompletionTracker = initAutoComplete(srv, cbTracker, Property.ISSUE_TYPE, true);
			autoCompletionPriority = initAutoComplete(srv, cbPriority, Property.PRIORITY, false);
			autoCompletionStatus = initAutoComplete(srv, cbStatus, Property.STATUS, false);

			WebViewHelper.addClickHandlerToWebView(webHistory);

			initDetectIssueModified();


			// Press Assign button when called from inspector.
			if (inspectorOrExplorer instanceof InspectorWrapper) {
				bnAssignSelection_select(true);
				internalSetMailItem(mailItem, createProgressCallback("Initialize"), (succ,ex) -> {});
			}
			// Show defaults when called from explorer.
			else {
				internalSetMailItem(new IssueMailItemBlank(), createProgressCallback("Initialize"), (succ,ex) -> {});
			}

			// Update menu items for "Add Attachment" button 
			bnAddAttachment.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
	            if (isNowShowing) {
	            	updateBnAddAttachmentMenuItems();
	            }
	        });

		}
		catch (Throwable e) {
			log.log(Level.WARNING, "", e);
			showMessageBoxError(e.toString());
		}
	}

	private void initDetectIssueModified() {
		detectIssueModifiedTimer = new Timeline(new KeyFrame(Duration.seconds(0.1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					updateData(true);

					if (firstModifiedCheck) {

						firstModifiedCheck = false;

						// Backup issue
						issueCopy = (Issue) issue.clone();

						// Run the deferred issue modifications.
						{
							for (Runnable run : autoIssueModifications) {
								run.run();
							}
							if (!autoIssueModifications.isEmpty()) {
								updateData(false);
							}
							autoIssueModifications.clear();
						}
					}
					else {
						boolean eq = issue.equals(issueCopy);
						setModified(!eq);
					}
				}
				catch (Exception e) {
					log.log(Level.WARNING, "", e);
				}
			}
		}));
		detectIssueModifiedTimer.setCycleCount(Timeline.INDEFINITE);
		detectIssueModifiedStart();
	}

	private AutoCompletionBinding<IdName> initAutoComplete(IssueService srv, ComboBox<IdName> cb, String propertyId, boolean validateIssueOnChange)
			throws Exception {
		List<IdName> allItems = new ArrayList<IdName>();

		String recentCaption = resb.getString("autocomplete.recentCaption");
		String suggestionsCaption = resb.getString("autocomplete.suggestionsCaption");
		ExtractImage<IdName> extractImage = (item) -> item.getImage();
		List<IdName> recentItems = new ArrayList<IdName>();

		AutoCompletionBinding<IdName> ret = AutoCompletions.bindAutoCompletion(extractImage, cb, recentCaption, suggestionsCaption, recentItems, allItems);

		if (validateIssueOnChange) {
			cb.valueProperty().addListener(new ComboboxChangeListener(propertyId));
		}

		return ret;
	}

	private void detectIssueModifiedStop() {
		if (detectIssueModifiedTimer != null) {
			detectIssueModifiedTimer.stop();
		}
	}

	private void detectIssueModifiedStart() {
		firstModifiedCheck = true;
		detectIssueModifiedContinue();
	}

	private void detectIssueModifiedContinue() {
		if (detectIssueModifiedTimer != null) {
			detectIssueModifiedTimer.play();
		}
	}

	private void setModified(boolean modified) {
		this.modified = modified;
		initModified();
	}

	private boolean lockChangeListener;

	private void updateData(boolean saveAndValidate) throws Exception {
		if (lockChangeListener) return;
		try {
			lockChangeListener = true;
			internalUpdateData(saveAndValidate);
		}
		finally {
			lockChangeListener = false;
		}
	}

	private void internalUpdateData(boolean saveAndValidate) throws Exception {
		if (saveAndValidate) {

			saveSubject();

			saveDescription();

			saveNotes();

			saveComboBox(cbProject, Property.PROJECT);

			saveComboBox(cbTracker, Property.ISSUE_TYPE);

			saveComboBox(cbStatus, Property.STATUS);

			saveComboBox(cbPriority, Property.PRIORITY);

			saveProperties();

			saveAttachments();

		}
		else {
			long t1 = System.currentTimeMillis();
			
			initMenuItemsOfButtonAssign();

			initIssueId();

			initModified();

			initSubject();

			initDescription();

			initNotes();

			initComboBox(autoCompletionProject, cbProject, Property.PROJECT);

			initComboBox(autoCompletionTracker, cbTracker, Property.ISSUE_TYPE);

			initComboBox(autoCompletionPriority, cbPriority, Property.PRIORITY);

			initComboBox(autoCompletionStatus, cbStatus, Property.STATUS);

			initProperties();

			initAttachments();

			initHistory();

			long t2 = System.currentTimeMillis();
			log.info("[" + (t2-t1) + "] internalUpdateData(saveAndValidate=" + saveAndValidate + ")");
		}
	}

	private void setBnUpdateEnabled(boolean enable) {
		bnUpdate.setDisable(!enable);
	}

	private void saveAttachments() {
		issue.setAttachments(observableAttachments.getObservableList());
	}

	private void initModified() {
		if (modified) {
			bnAssignSelection_select(false);
		}
		boolean enabled = modified || isNew();
		boolean hasSubject = !issue.getSubject().isEmpty();
		setBnUpdateEnabled(enabled && hasSubject);
	}

	private void initIssueId() {
		edIssueId.setText(issue.getId());
	}

	private void saveProperties() {
		if (propGridView != null) {
			propGridView.updateData(true);
		}
	}

	private void saveComboBox(ComboBox<IdName> cb, String propertyId) {
		IdName idn = cb.getValue();
		if (idn != null && !idn.equals(IdName.NULL)) {
			Property prop = new Property(propertyId, idn);
			issue.getCurrentUpdate().setProperty(prop);
		}
		else {
			issue.getCurrentUpdate().removeProperty(propertyId);
		}
	}

	private void saveDescription() throws Exception {
		if (descriptionEditor != null) descriptionEditor.updateData(true);
	}

	private void saveNotes() throws Exception {
		if (notesEditor != null) notesEditor.updateData(true);
	}

	private void saveSubject() {
		String text = edSubject.getText().trim();
		if (issue != null) {
			issue.setSubject(text);
		}
	}

	private void initHistory() throws Exception {
		if (issue.getId() != null && issue.getId().length() != 0) {
			IssueService srv = Globals.getIssueService();
			WebEngine webEngine = webHistory.getEngine();
			String url = srv.getIssueHistoryUrl(issue.getId());
			if (url.toLowerCase().startsWith("http")) {
				webEngine.load(url);
			}
			else {
				webEngine.loadContent(url);
			}
		}
	}

	private void initAttachments() {
		long t1 = System.currentTimeMillis();
		if (tabAttachmentsApplyHandler) {

			// Copy attachments to backing list.
			List<Attachment> atts = new ArrayList<Attachment>(issue.getAttachments().size());
			for (Attachment att : issue.getAttachments()) {
				if (!att.isDeleted()) { // obsolete: cannot delete attachments via
										// Redmine API
					atts.add(att);
				}
			}
			observableAttachments = new Attachments(FXCollections.observableList(atts));

			AttachmentTableViewHandler.apply(attachmentHelper, tabAttachments, observableAttachments, this);

			tabAttachments.setOnMouseClicked((click) -> {
				if (click.getClickCount() == 2) {
					showSelectedIssueAttachment();
				}
			});
			tabAttachments.setOnKeyPressed((keyEvent) -> {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					showSelectedIssueAttachment();
				}
			});
			
			tabAttachments.setOnMouseClicked((click) -> {
				if (click.getClickCount() == 2) {
					showSelectedIssueAttachment();
				}
				else if (click.getButton() == MouseButton.SECONDARY) {
					showTabAttachmentsContextMenu(click.getScreenX(), click.getScreenY());
				}
			});
			
			tabAttachments.setOnKeyPressed((keyEvent) -> {
				switch (keyEvent.getCode()) {
				case ENTER:
					showSelectedIssueAttachment();
					break;
				case CONTEXT_MENU:
					showTabAttachmentsContextMenu(-1, -1);
					break;
				case V:
					if (keyEvent.isControlDown()) {
						AttachmentTableViewHandler.paste(observableAttachments);
					}
					break;
				case C:
					if (keyEvent.isControlDown()) {
						copySelectedAttachmentsToClipboard();
					}
					break;
				default:
				}
			});


			bnRemoveAttachment.setDisable(true);

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

			addAttachmentMenu = new AddAttachmentMenu(this.getWindow(), observableAttachments);

			// Bind button "Open Attachment" table selection
			bnShowAttachment.disableProperty().unbind();
			bnShowAttachment.disableProperty().bind(Bindings.size(tabAttachments.getSelectionModel().getSelectedIndices()).isEqualTo(0));
			
			// Bind button "Export" to table content 
			bnExportAttachments.disableProperty().unbind();
			bnExportAttachments.disableProperty().bind(Bindings.size(tabAttachments.getItems()).isEqualTo(0));
			
			tabAttachmentsApplyHandler = false;
		}

		fireUpdateBindingToAttachmentList();
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] initAttachments()");
	}
	
	private void copySelectedAttachmentsToClipboard() {
		try {
			AttachmentTableViewHandler.copy(tabAttachments, attachmentHelper, createProgressCallback("Copy attachmnts to clipboard"));
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to copy attachments.", e);
			showMessageBoxError("Failed to copy attachments. " + e);
		}
	}
	
	private void showTabAttachmentsContextMenu(double screenX, double screenY) {
		standardContextMenu
		.acceptedClipboardDataFlavors(DataFlavor.imageFlavor, DataFlavor.javaFileListFlavor)
		.showCut(false).showCopy(!observableAttachments.isEmpty())
		.onCopy((event) -> copySelectedAttachmentsToClipboard())
		.onPaste((event) -> AttachmentTableViewHandler.paste(observableAttachments))
		.show(tabAttachments, screenX, screenY);
	}
	
	private void fireUpdateBindingToAttachmentList() {
		int v = updateBindingToAttachmentList.getValue();
		updateBindingToAttachmentList.setValue(v+1);
	}

	private void initProperties() throws Exception {
		if (propGridView == null) {
			propGridView = new PropertyGridView(this, tabpIssue, tpProperties, propGrid);
		}
		propGridView.initProperties(issue);
		propGridView.updateData(false);
	}

	public PropertyGridView getPropGridView() {
		return propGridView;
	}

	private void initSubject() {
		String text = issue.getSubject();
		edSubject.setText(text);
	}

	private void initDescription() throws Exception {
		long t1 = System.currentTimeMillis();
		if (issue != null) {
			descriptionEditor.updateData(false);
		}
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] initDescription()");
	}

	private void initNotes() throws Exception {
		long t1 = System.currentTimeMillis();
		if (issue != null) {
			notesEditor.updateData(false);
		}
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] initNotes()");
	}

	private void initComboBox(AutoCompletionBinding<IdName> autoCompletionBinding, ComboBox<IdName> cb, String propertyId)
			throws Exception {
		long t1 = System.currentTimeMillis();
		IssueService srv = Globals.getIssueService();
		PropertyClass pclass = srv.getPropertyClass(propertyId, issue);
		List<IdName> items = pclass != null ? pclass.getSelectList() : new ArrayList<IdName>(0);

		if (pclass.isReadOnly()) {
			autoCompletionBinding.disableRecentItems();
		}
		else {
			List<IdName> recentItems = pclass.getRecentItems();
			autoCompletionBinding.setRecentItems(recentItems);
		}

		cb.setVisible(!items.isEmpty());
		if (!items.isEmpty()) {
			IdName idn = issue.getPropertyIdName(propertyId, IdName.NULL);
			autoCompletionBinding.select(idn);
			
			if (pclass.isReadOnly()) {
				items = Arrays.asList(idn);
			}
			
			Suggest<IdName> suggest = new DefaultSuggest<IdName>(items);
			autoCompletionBinding.setSuggest(suggest);
		}
		
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] initComboBox(propertyId=" + propertyId + ")");
	}

	private void initialUpdate(ProgressCallback cb) throws Exception {
		long t1 = System.currentTimeMillis();
		
		// Create a new binding for attachment modifications.
		// This avoids too many listeners for attachment modifications, since
		// listeners were never removed.
		updateBindingToAttachmentList = new SimpleIntegerProperty();
		
		tpNotes.setStyle("-fx-font-weight:normal;");
		tpAttachments.setStyle("-fx-font-weight:normal;");

		attachmentHelper.initialUpdate(mailItem, issue);

		initalUpdateAttachmentView();

		// Show/Hide History and Notes
		addOrRemoveTab(tpNotes, !isNew(), 0);
		addOrRemoveTab(tpHistory, !isNew(), 0);
		
		boolean hasHistory = !Globals.getIssueService().getIssueHistoryUrl(issue.getId()).isEmpty();
		tabpIssue.getSelectionModel().select(hasHistory ? tpHistory : tpDescription);

		initBnUpdateText();

		descriptionEditor = Globals.getIssueService().getPropertyEditor(this, issue, Property.DESCRIPTION);
		VBox.setVgrow(descriptionEditor.getNode(), Priority.ALWAYS);
		boxDescription.getChildren().clear();
		boxDescription.getChildren().add(descriptionEditor.getNode());

		notesEditor = Globals.getIssueService().getPropertyEditor(this, issue, Property.NOTES);
		VBox.setVgrow(notesEditor.getNode(), Priority.ALWAYS);
		boxNotes.getChildren().clear();
		boxNotes.getChildren().add(notesEditor.getNode());

		modified = false;

		updateData(false);

		detectIssueModifiedStart();
		
		cb.setFinished();
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] initialUpdate()");
	}

	private void initBnUpdateText() {
		if (isLicenseValid()) {
			bnUpdate.setText(resb.getString(isNew() ? "bnUpdate.text.create" : "bnUpdate.text.update"));
		}
		else {
			bnUpdate.setText(resb.getString("mnLicense"));
		}
	}

	private boolean isInjectIssueId() {
		boolean ret = Globals.getAppInfo().getConfig().isInjectIssueIdIntoMailSubject();
		return ret;
	}

	private void addOrRemoveTab(Tab t, boolean add, int pos) {
		if (add) {
			for (Tab p : tabpIssue.getTabs()) {
				if (p == t) return;
			}
			pos = Math.min(pos, tabpIssue.getTabs().size());
			tabpIssue.getTabs().add(pos, t);
		}
		else {
			tabpIssue.getTabs().remove(t);
		}
	}

	private void showSelectedIssueAttachment() {
		Attachment att = tabAttachments.getSelectionModel().getSelectedItem();
		if (att != null) {
			ProgressCallback cb = createProgressCallback("Show selected attachment");
			BackgTask.run(() -> {
				try {
					attachmentHelper.showAttachment(att, cb);
				}
				catch (Exception e) {
					showMessageBoxError(e.toString());
				}
				finally {
					cb.setFinished();
				}
			});
		}
	}

	@FXML
	public void onShowAttachment() {
		showSelectedIssueAttachment();
	}

	@FXML
	public void onAddAttachment() {
	}
	
	@FXML
	public void onExportAttachments() {

		ProgressCallback cb = createProgressCallback("Export attachments");

		// Selected items or all items
		List<Attachment> selectedItems = new ArrayList<Attachment>();
		selectedItems.addAll(tabAttachments.getSelectionModel().getSelectedItems());
		if (selectedItems.isEmpty()) {
			selectedItems.addAll(tabAttachments.getItems());
		}
		
		BackgTask.run(() -> {
			try {
				attachmentHelper.exportAttachments(issue, selectedItems, cb);
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Failed to export attachments of issue=" + issue, e);
			}
		});
	}
	
	@FXML
	public void onRemoveAttachment() {
		List<Attachment> selectedItems = tabAttachments.getSelectionModel().getSelectedItems();

		// ITJ-1: Copy issue attachment list since the original list is an observable list
		// That is also attached to the table view.
		List<Attachment> issueAttachments = new ArrayList<>(issue.getAttachments());
		
		for (int i = 0; i < selectedItems.size(); i++) {
			Attachment att = selectedItems.get(i);
			if (att.getId() != null && att.getId().length() != 0) {
				// Mark existing attachment deleted.
				// The attachment is deleted in IssueService.updateIssue().
				// --- THIS IS CURRENTLY NOT SUPPORTED OVER REDMINE API ---
				// att.setDeleted(true);
			}
			else {
				for (Iterator<Attachment> it = issueAttachments.iterator(); it.hasNext(); ) {
					Attachment issueAttachment = it.next();
					if (issueAttachment == att) {
						it.remove();
						break;
					}
				}
			}
		}
		
		issue.setAttachments(issueAttachments);

		initalUpdateAttachmentView();
	}

	private void initalUpdateAttachmentView() {
		long t1 = System.currentTimeMillis();
		
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
		
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] initialUpdateAttachmentView()");
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

	/**
	 * Display a message box with an error message.
	 * @param text
	 */
	public void showMessageBoxError(String text) {
		detectIssueModifiedStop();
		String title = resb.getString("MessageBox.title.error");
		String ok = resb.getString("Button.OK");
		Window owner = this.getWindow();
		com.wilutions.joa.fx.MessageBox.create(owner).title(title).text(text).button(1, ok).bdefault()
				.show((btn, ex) -> {
					detectIssueModifiedContinue();
				});
	}

	private void queryDiscardChangesAsync(AsyncResult<Boolean> asyncResult) {
		if (modified) {
			detectIssueModifiedStop();
			String title = resb.getString("MessageBox.title.confirm");
			String text = resb.getString("MessageBox.queryDiscardChanges.text");
			String yes = resb.getString("Button.Yes");
			String no = resb.getString("Button.No");
			Object owner = this.getDialogOwner();
			com.wilutions.joa.fx.MessageBox.create(owner).title(title).text(text).button(1, yes).button(0, no)
					.bdefault().show((btn, ex) -> {
						Boolean succ = btn != null && btn != 0;
						asyncResult.setAsyncResult(succ, ex);
						detectIssueModifiedContinue();
					});
		}
		else {
			asyncResult.setAsyncResult(true, null);
		}
	}

	@FXML
	public void onClear() {
		queryDiscardChangesAsync((succ, ex) -> {
			if (ex == null && succ) {
				bnAssignSelection_select(false);
				internalSetMailItem(new IssueMailItemBlank(), createProgressCallback("Clear"), (succ1,ex1)->{});
			}
		});
	}

	@FXML
	public void onAssignSelection() {
		boolean sel = bnAssignSelection_isSelected();
		if (sel) {
			queryDiscardChangesAsync((succ, ex) -> {
				if (ex == null && succ) {
					try {
						IssueMailItem mailItem = inspectorOrExplorer.getSelectedItem();
						IssueTaskPane.this.setMailItem(mailItem);
					}
					catch (Throwable e) {

					}
				}
				else {
					bnAssignSelection_select(false);
				}
			});
		}
	}
	
	private void internalShowIssue(String issueId, ProgressCallback cb, AsyncResult<Boolean> asyncResult) throws Exception {
		IssueService srv = Globals.getIssueService();
		Issue issue = srv.readIssue(issueId, cb.createChild(0.5));
		String subject = srv.injectIssueIdIntoMailSubject("", issue);
		
		IssueMailItem mitem = new IssueMailItemBlank() {
			public String getSubject() {
				return subject;
			}
		};
		internalSetMailItem(mitem, cb.createChild(0.5), asyncResult);
	}

	@FXML
	public void onShowExistingIssue() {

		queryDiscardChangesAsync((succ, ex) -> {
			if (ex == null && succ) {

				ProgressCallback cb = createProgressCallback("Show issue");
				
				// Show that reading issue has started
				cb.incrProgress(0.1);
				
				bnAssignSelection_select(false);
				
				try {
					String issueId = edIssueId.getText();
					internalShowIssue(issueId, cb.createChild(0.9), (succ1, ex1) -> {
						cb.setFinished();	
					});
				}
				catch (Exception e) {
					showMessageBoxError(e.toString());
					cb.setFinished();
				}
			}
		});
	}

	@FXML
	public void onShowIssueInBrowser() {
		try {
			IssueService srv = Globals.getIssueService();
			String url = srv.getShowIssueUrl(issue.getId());
			IssueApplication.showDocument(url);
		}
		catch (Exception e) {
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
		}
		else {
			idx = 0;
		}
		tabpIssue.getSelectionModel().select(idx);

		// Determine which control should receive the focus
		try {
			Node node = null;
			switch (idx) {
			case 0: // DESCRIPTION
			{
				IssuePropertyEditor editor = Globals.getIssueService().getPropertyEditor(this, issue, Property.DESCRIPTION);
				node = editor.getNode();
				break;
			}
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
			{
				IssuePropertyEditor editor = Globals.getIssueService().getPropertyEditor(this, issue, Property.NOTES);
				node = editor.getNode();
				break;
			}
			}
		
			// Focus control
			if (node != null) {
				final Object nodeF = node;
				BackgTask.run(() -> {
					try {
						Thread.sleep(200);
						Platform.runLater(() -> {
							if (nodeF instanceof IssuePropertyEditor) {
								((IssuePropertyEditor)nodeF).setFocus();
							}
							else {
								((Node)nodeF).requestFocus();
							}
						});
					}
					catch (InterruptedException e) {
					}
				});
			}
			
		}
		catch (Exception e) {
			log.log(Level.WARNING, "Failed to set focus", e);
		}
	}

	private class ComboboxChangeListener implements ChangeListener<IdName> {

		@SuppressWarnings("unused")
		final String propertyId;

		ComboboxChangeListener(String propertyId) {
			this.propertyId = propertyId;
		}

		@Override
		public void changed(ObservableValue<? extends IdName> observable, IdName oldValue, IdName newValue) {
			if (lockChangeListener) {
				return;
			}

			if (oldValue == null) oldValue = IdName.NULL;
			if (newValue == null) newValue = IdName.NULL;

			if (!oldValue.equals(newValue)) {

				try {
					updateData(true);

					IssueService srv = Globals.getIssueService();
					srv.validateIssue(issue);
					
					assignMailAdressToAutoReplyField();

					updateData(false);

				}
				catch (Throwable e) {
					log.log(Level.WARNING, "", e);
				}
			}
		}

	}

	private class MyProgressCallback extends ProgressCallbackImpl {

		private int lastPercent = 0;

		public MyProgressCallback(String name) {
			super("");
//			System.out.println("start progress ---- ");
//			log.info("start progress");
		}
		
		@Override
		public void setTotal(double total) {
//			System.out.println("total=" + total);
			super.setTotal(total);
		}
		
		@Override
		protected void internalIncrProgress(double amount) {
			super.internalIncrProgress(amount);
			final double quote = current / total;
			int percent = (int) Math.ceil(100.0 * quote);
			if (percent > lastPercent) {
				lastPercent = percent;
			}
			if (quote > 1.01) {
//				System.err.println("progress " + quote);
			}
			else {
				Platform.runLater(() -> {
					if (pgProgress != null) {
						pgProgress.setProgress(quote);
					}
				});
			}
//			System.out.println("progress " + quote);
//			log.info("progress=" + quote);
		}

		@Override
		public void setFinished() {
			super.setFinished();
			Platform.runLater(() -> {
				if (pgProgress != null) {
					pgProgress.setProgress(0);
					// System.out.println("progress 0");
				}
			});
//			System.out.println("progress finished");
//			log.info("progress finished");
		}

	}

	@FXML
	public void onUpdate() {
		
		// If the license is not valid, show the license dialog instead of storing any issue changes.
		if (!isLicenseValid()) {
			DlgLicense.show(this);
			return;
		}

		final ProgressCallback progressCallback = createProgressCallback("Update issue");
		detectIssueModifiedStop();

		try {
			IssueService srv = Globals.getIssueService();

			// Save dialog elements into data members
			updateData(true);

			// Create issue in background.
			// Currently, we are in the UI thread. The progress dialog would not
			// be updated,
			// If we processed updating the issue here.
			BackgTask.run(() -> {
				try {

					// Show that progress has started.
					progressCallback.incrProgress(0.1);

					// Update issue.
					// this.issue is replaced by a new object.
					Issue prevIssue = (Issue)issue.clone();
					updateIssueChangedMembers(srv, progressCallback.createChild(0.8));

					Platform.runLater(() -> {
						try {
							initialUpdate(progressCallback.createChild(0.1));
							maybeSendReplyWithNotes(prevIssue);
						}
						catch (Exception e) {
							log.log(Level.SEVERE, "Failed to read issue data into UI controls.", e);
						}
						finally {
							progressCallback.setFinished();
						}
					});

				}
				catch (Throwable e) {

					if (!progressCallback.isCancelled()) {
						log.log(Level.SEVERE, "Failed to update issue", e);

						String text = resb.getString("Error.FailedToUpdateIssue");
						String msg = e.getMessage();
						if (msg == null || msg.isEmpty()) msg = e.toString();
						text += " " + msg;
						showMessageBoxError(text);

						detectIssueModifiedContinue();
					}

					progressCallback.setFinished();
				}
			});

		}
		catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to update issue", e);

			String text = resb.getString("Error.FailedToUpdateIssue");
			String msg = e.getMessage();
			if (msg.isEmpty()) msg = e.toString();
			text += " " + msg;
			showMessageBoxError(text);

			progressCallback.setFinished();
			detectIssueModifiedContinue();
		}
	}

	/**
	 * Create or update an issue.
	 * 
	 * @param srv
	 *            Service
	 * @param progressCallback
	 *            Callback object
	 * @throws IOException
	 */
	private void updateIssueChangedMembers(IssueService srv, final ProgressCallback progressCallback)
			throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "updateIssueChangedMembers(");

		// If the issue ID is empty, a new issue has to be created.
		boolean isNew = issue.getId().length() == 0;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isNew=" + isNew);

		// Collect modified properties.
		List<String> modifiedProperties = new ArrayList<String>();
		issueCopy.findChangedMembers(issue, modifiedProperties);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "modifiedProperties=" + modifiedProperties);

		// Enhancement #40: issue ID should be inserted into the attached mail
		// too.
		// Therefore, we have to create the issue before uploading attachments.
		List<Attachment> deferredAttachments = null;
		if (isNew && isInjectIssueId()) {
			IdName type = Globals.getAppInfo().getConfig().getMsgFileFormat();
			if (type == MsgFileFormat.MSG) {
				modifiedProperties.remove(Property.ATTACHMENTS);
				deferredAttachments = issue.getAttachments();
				issue.setAttachments(new ArrayList<Attachment>(0));
			}
		}

		// Create issue
		issue = srv.updateIssue(issue, modifiedProperties, progressCallback);

		// Inject the issue ID into Outlook's mail object
		if (isNew && isInjectIssueId()) {
			saveMailWithIssueId(progressCallback);
		}

		// Upload deferred attachments.
		if (deferredAttachments != null && deferredAttachments.size() != 0) {
			modifiedProperties.clear();
			modifiedProperties.add(Property.ATTACHMENTS);

			// Inject the issue ID into the mail attachment that is uploaded.
			for (Attachment att : deferredAttachments) {
				if (att instanceof MailAttachmentHelper.MailAtt) {
					String subject = att.getSubject();
					subject = injectIssueIdIntoMailSubject(subject, issue);
					att.setSubject(subject);
					break;
				}
			}

			// Upload
			issue.setAttachments(deferredAttachments);
			issue = srv.updateIssue(issue, modifiedProperties, progressCallback);
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")updateIssueChangedMembers");
	}

	private void saveMailWithIssueId(final ProgressCallback progressCallback) throws Exception {
		String mailSubjectPrev = mailItem.getSubject();
		String mailSubject = injectIssueIdIntoMailSubject(mailSubjectPrev, issue);
		if (!mailSubjectPrev.equals(mailSubject)) {
			mailItem.setSubject(mailSubject);
			progressCallback.setParams("Save mail as \"" + mailSubject + "\n");
			mailItem.Save();
		}
	}

	private String injectIssueIdIntoMailSubject(String subject, Issue issue) throws Exception {
		return Globals.getIssueService().injectIssueIdIntoMailSubject(subject, issue);
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
			Globals.getIssueService().setDefaultIssue(issue);
		}
		catch (Exception e) {
			showMessageBoxError(e.toString());
		}
	}

	@Override
	public void setVisible(final boolean v) throws ComException {
		super.setVisible(v);
		if (v) {
			if (bnAssignSelection != null && !bnAssignSelection_isSelected() && !modified) {
				// internalSetMailItem(new IssueMailItemBlank());
			}
		}
	}

	@FXML
	public void onConnect() {
		ItolAddin addin = (ItolAddin) Globals.getThisAddin();
		Window owner = this.getWindow();
		addin.internalConnect(owner, null);
	}

	@FXML
	public void onConfigure() {
		ItolAddin addin = (ItolAddin) Globals.getThisAddin();
		Window owner = this.getWindow();
		addin.internalConfigure(owner, (succ, ex) -> {
			if (succ) {
				ProgressCallback cb = createProgressCallback("Re-connect");
				try {
					cb.incrProgress(0.5);
					Globals.initialize(false); // Re-connect in background.
					cb.incrProgress(0.5);
				}
				catch (Exception e) {
					log.log(Level.WARNING, "Re-connect to server failed.", e);
					String msg = e.getMessage();
					String textf = resb.getString("msg.connection.error");
					String text = MessageFormat.format(textf, msg);
					showMessageBoxError(text);
				}
				finally {
					cb.setFinished();
				}
			}
		});
	}

	/**
	 * Comment editor of JIRA addin binds to this value.
	 * If the value changes, the editor will call {@link #getObservableAttachments()}
	 * to bind to the new list.
	 * @return
	 */
	public SimpleIntegerProperty getUpdateBindingToAttachmentList() {
		return updateBindingToAttachmentList;
	}

	/**
	 * Comment editor of JIRA addin binds to the list of attachments.
	 * @return
	 */
	public Attachments getObservableAttachments() {
		return observableAttachments; 
	}

	private void updateBnAddAttachmentMenuItems() {
		// http://stackoverflow.com/questions/26895534/javafx-split-menu-button-arrow-trigger-event
		bnAddAttachment.getItems().clear();
		bnAddAttachment.getItems().addAll(addAttachmentMenu.create());
	}

	/**
	 * Create a callback object that displays the state in the progress bar.
	 * @return ProgressCallback object
	 */
	@Override
	public ProgressCallback createProgressCallback(String name) {
		return new MyProgressCallback(name);
	}
	

	/**
	 * ITJ-18: Send reply with notes.
	 * The recipient(s) of the reply mail is found in a custom property, see Config.getAutoReplyField().
	 * 
	 * @param prevIssue Issue before it has been saved.
	 */
	private void maybeSendReplyWithNotes(Issue prevIssue) {
		if (log.isLoggable(Level.FINE)) log.fine("maybeSendReplyWithNotes(tookNotesFromMail=" + tookNotesFromMail);
		
		if (!prevIssue.isNew()) {
			if (!tookNotesFromMail) {
				String mailToPropId = Globals.getAppInfo().getConfig().getAutoReplyField();  
				if (log.isLoggable(Level.FINE)) log.fine("mailToPropId=" + mailToPropId);
				if (!mailToPropId.isEmpty()) {
					String mailTo = issue.getPropertyString(mailToPropId, "");
					if (log.isLoggable(Level.FINE)) log.fine("mailTo=" + mailTo);
					if (!mailTo.isEmpty()) {
						String lastComment = prevIssue.getPropertyString(Property.NOTES, "");
						if (log.isLoggable(Level.FINE)) log.fine("lastComment=" + lastComment);
						if (!lastComment.isEmpty()) {
							if (log.isLoggable(Level.FINE)) log.fine("start sendReplyWithNotes in background");
							BackgTask.run(() -> {
								sendReplyWithNotes(issue, mailTo, lastComment, createProgressCallback("Prepare reply"));
							});
						}
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(")maybeSendReplyWithNotes");
	}

	private void sendReplyWithNotes(Issue issue, String mailTo, String lastComment, ProgressCallback cb) {
		if (log.isLoggable(Level.FINE)) log.fine("sendReplyWithNotes(issue=" + issue + ", mailTo=" + mailTo + ", lastComment=" + lastComment);
		try {
			IssueService srv = Globals.getIssueService();
			MailInfo mailInfo = srv.replyToComment(issue, mailTo, lastComment, cb);

			// Create MailItem object with subject, body, TO,...
			Application application = Globals.getThisAddin().getApplication();
			MailItem mailItem = Dispatch.as(application.CreateItem(OlItemType.olMailItem), MailItem.class);

			mailItem.setTo(mailInfo.getTO());
			mailItem.setCC(mailInfo.getCC());
			mailItem.setBCC(mailInfo.getBCC());
			mailItem.setSubject(mailInfo.getSubject());
			mailItem.setHTMLBody(mailInfo.getHtmlBody());
			
			// Attachments
			List<Attachment> attachments = mailInfo.getAttachments();
			addAttachmentsToReply(mailItem, attachments, cb);

			// Show Inspector window
			if (log.isLoggable(Level.FINE)) log.fine("mailItem.Display");
			mailItem.Display(false); 
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to build reply mail.", e);
			this.showMessageBoxError("Failed to build reply mail.\n" + e);
		}
		finally {
			cb.setFinished();
		}
		if (log.isLoggable(Level.FINE)) log.fine(")sendReplyWithNotes");
	}
	
	private void addAttachmentsToReply(MailItem mailItem, List<Attachment> attachments, ProgressCallback cb) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("addAttachmentsToReply(" + attachments);
		
		// Compute total size of attachments to be added.
		double totalProgress = attachments.stream().collect(Collectors.summarizingLong((att) -> att.getContentLength())).getSum();
		cb.setTotal(totalProgress);

		com.wilutions.mslib.outlook.Attachments mailAttachments = mailItem.getAttachments().as(com.wilutions.mslib.outlook.Attachments.class);
		int index = 0;
		for (Attachment attachment : attachments) {
			if (log.isLoggable(Level.FINE)) log.fine("download " + attachment);
			String uri = attachmentHelper.downloadAttachment(attachment, cb.createChild("Download " + attachment.getFileName(), attachment.getContentLength(), cb.getTotal()));
			File file = new File(new URI(uri));
			if (log.isLoggable(Level.FINE)) log.fine("mailAttachment.Add " + file.getAbsolutePath());
			mailAttachments.Add(file.getAbsolutePath(), OlAttachmentType.olByValue, ++index, attachment.getFileName());
		}
		if (log.isLoggable(Level.FINE)) log.fine(")addAttachmentsToReply");
	}
	
	private void bnAssignSelection_select(boolean v) {
		ckAssigned.setSelected(v);
	}
	
	private boolean bnAssignSelection_isSelected() {
		return ckAssigned.isSelected();
	}

	/**
	 * Update menu items of button "Assign".
	 */
	private void initMenuItemsOfButtonAssign() {
		try {
			ObservableList<MenuItem> items = bnAssignSelection.getItems();
			items.remove(1, items.size());
			if (!issue.isNew()) {
				
				IssueService srv = Globals.getIssueService();
				final String resourceText = resb.getString("bnCreateSubtask.text");
				
				boolean hasSeparator = false;
				for (IdName subtaskType : srv.getSubtaskTypes(issue)) {
					if (!hasSeparator) {
						items.add(new SeparatorMenuItem());
						hasSeparator = true;
					}
					
					MenuItem mnCreateSubtask = new MenuItem();
					mnCreateSubtask.setText(MessageFormat.format(resourceText, subtaskType.getName()));
					mnCreateSubtask.setGraphic(new ImageView(subtaskType.getImage()));
					mnCreateSubtask.setOnAction((e) -> {
						onCreateSubtask(subtaskType);
					});
					items.add(mnCreateSubtask);
				}
			}
			
		} catch (Exception e) {
		}
	}

	public void onCreateSubtask(IdName subtaskType) {
		
		ProgressCallback cb = createProgressCallback("Create subtask");
		bnAssignSelection_select(false);

		BackgTask.run(() -> {

			IssueService srv = null;
			try {

				srv = Globals.getIssueService();
				
				String subject = "";
				String description = "";
				
				issue = srv.createIssue(subject, description, issue, subtaskType, cb.createChild(0.9));

				Platform.runLater(() -> {
					try {
						initialUpdate(cb.createChild(0.1));
					} catch (Exception e) {
						String text = e.toString();
						log.log(Level.SEVERE, text, e);
						showMessageBoxError(text);
					}
					cb.setFinished();
				});

			}
			catch (Throwable e) {
				String text = e.toString();
				log.log(Level.SEVERE, text, e);
				showMessageBoxError(text);
				cb.setFinished();
			}
		});

	}
	
	@FXML
	public void onLicense() {
		DlgLicense.show(this);
	}
	
	private boolean isLicenseValid() {
		return licenseValid;
	}
	
	public void setLicenseValid(boolean v) {
		this.licenseValid = v;
		initBnUpdateText();
	}
	
	@FXML
	public void onAbout() {
		DlgAbout.show(this.getWindow());
	}
}