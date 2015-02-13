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
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
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

import com.wilutions.com.AsyncResult;
import com.wilutions.com.ComException;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.DescriptionHtmlEditor;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.mslib.office.CustomTaskPane;

public class IssueTaskPane extends TaskPaneFX implements Initializable {

	private Issue issue;

	@FXML
	private VBox vboxRoot;

	@FXML
	private GridPane rootGrid;
	@FXML
	private TextField edSubject;
	@FXML
	private HTMLEditor edDescription;
	@FXML
	private ToggleButton bnPin;
	@FXML
	private ToggleButton bnSelection;
	@FXML
	private ToggleButton bnBlank;
	@FXML
	private ChoiceBox<IdName> cbTracker;
	@FXML
	private ChoiceBox<IdName> cbCategory;
	@FXML
	private ChoiceBox<IdName> cbAssignee;
	@FXML
	private Tab tpDescription;
	@FXML
	private Tab tpHistory;
	@FXML
	private Tab tpNotes;
	@FXML
	private HTMLEditor edNotes;
	@FXML
	private WebView webHistory;
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
	
	private boolean tabAttachmentsApplyHandler = true;

	private PropertyGridView propGridView;

	private DescriptionHtmlEditor descriptionHtmlEditor;
	private WebView webView;
	private final MailInspector mailInspectorOrNull;
	private IssueMailItem mailItem;
	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private File tempDir;
	private ResourceBundle resb;

	/**
	 * Owner window for child dialogs (message boxes)
	 */
	private Object windowOwner;

	public IssueTaskPane(MailInspector mailInspectorOrNull, IssueMailItem mailItem) {
		this.mailInspectorOrNull = mailInspectorOrNull;
		this.mailItem = mailItem;

		this.resb = Globals.getResourceBundle();

		final String subject = mailItem.getSubject();
		final String description = mailItem.getBody();
		try {
			IssueService srv = Globals.getIssueService();
			issue = srv.createIssue(subject, description);

			String issueId = srv.extractIssueIdFromMailSubject(subject);
			issue.setId(issueId);

		} catch (Throwable e) {
			e.printStackTrace();
		}

		// Globals.getThisAddin().getRegistry().readFields(this);
	}

	public void setMailItem(IssueMailItem mailItem) {
		this.mailItem = mailItem;
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
		String issueId = issue.getId();
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
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			URL fxmlURL = classLoader.getResource("com/wilutions/itol/NewIssue6.fxml");

			resb = Globals.getResourceBundle();

			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			} // Do not create a new IssueTaskPane object.
			);
			Parent p = loader.load();

			Scene scene = new Scene(p);
			scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

//			ScenicView.show(scene);
			
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
			initialUpdate();

		} catch (Throwable e) {
			e.printStackTrace();
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
		if (saveAndValidate) {

		} else {
			initSubject();
			
			initChoiceBox(cbTracker, Property.ISSUE_TYPE);

			initChoiceBox(cbCategory, Property.CATEGORY);

			initChoiceBox(cbAssignee, Property.ASSIGNEE);

			initChoiceBox(cbStatus, Property.STATUS);

			initTabView();

			initDescription();

			initProperties();

			initAttachments();
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

		List<Attachment> atts = new ArrayList<Attachment>();
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

		// descriptionHtmlEditor =
		// Globals.getIssueService().getDescriptionHtmlEditor(issue);
		if (descriptionHtmlEditor != null) {

			if (webView == null) {
				edDescription.setVisible(false);

				webView = new WebView();

				HBox hbox = new HBox();
				hbox.setStyle("-fx-border-color: LIGHTGREY;-fx-border-width: 1px;");
				hbox.getChildren().add(webView);

				rootGrid.add(hbox, 0, 1, 3, 1);
			}

			webView.getEngine().loadContent(descriptionHtmlEditor.getHtmlContent());

		} else if (edDescription != null) {
			edDescription.setHtmlText(issue.getDescription());
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
		tabpIssue.getSelectionModel().select(tpDescription);

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

	private void initTabView() {
		addOrRemoveTab(tpHistory, !isNew());
		addOrRemoveTab(tpNotes, !isNew());

	}

	private void showSelectedIssueAttachment() {
		Attachment att = tabAttachments.getSelectionModel().getSelectedItem();
		if (att != null) {
			String url = att.getUrl();
			if (url.startsWith("mail:///")) {
				// File selectedFile = new
				// File(selectedAttachment.getFileName());
				// if (!selectedFile.exists()) {
				// MailSaveHandler saveHandler =
				// mapFileToSaveHandler.get(selectedFile);
				// try {
				// saveHandler.save();
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
				// }
			}
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
		List<Attachment> oldItems = tabAttachments.getItems();
		List<Attachment> newItems = new ArrayList<Attachment>(oldItems.size());

		HashSet<Integer> selectedIndices = new HashSet<Integer>(tabAttachments.getSelectionModel().getSelectedIndices());
		tabAttachments.getSelectionModel().clearSelection();

		for (int i = 0; i < oldItems.size(); i++) {
			if (!selectedIndices.contains(i)) {
				newItems.add(oldItems.get(i));
			}
		}

		// The TableView does not refresh it's items on getItems().remove(...)
		// Other devs had this problem with JavaFX 2.1
		// http://stackoverflow.com/questions/11065140/javafx-2-1-tableview-refresh-items
		// But the suggested workarounds do not help.

		hboxAttachments.getChildren().remove(tabAttachments);

		tabAttachments = new TableView<Attachment>();
		tabAttachmentsApplyHandler = true;
		initAttachments();
		tabAttachments.getItems().addAll(newItems);
		hboxAttachments.getChildren().add(0, tabAttachments);
		HBox.setHgrow(tabAttachments, Priority.ALWAYS);

	}

	@FXML
	public void onEditPropertyStart() {

	}

	@FXML
	public void onEditPropertyCommit() {

	}

	@FXML
	public void onEditPropertyCancel() {

	}
}
