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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

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
	private TitledPane tpDescription;
	@FXML
	private TitledPane tpHistory;
	@FXML
	private TitledPane tpNotes;
	@FXML
	private HTMLEditor edNotes;
	@FXML
	private WebView webHistory;
	@FXML
	private GridPane gridProps;
	@FXML
	private TableView<Attachment> tabAttachments;
	@FXML
	private Accordion accIssue;

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
	
	private PropertyClass pclassPriority;
	private PropertyClass pclassMilestones;

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
			
			pclassPriority = new PropertyClass(PropertyClass.TYPE_ARRAY_STRING, 
					Property.PRIORITY,
					resb.getString(Property.PRIORITY), 
					issue.getPriority());
			pclassPriority.setSelectList(srv.getPriorities(issue));
			
			pclassMilestones = new PropertyClass(PropertyClass.TYPE_ARRAY_STRING, 
					Property.MILESTONES,
					resb.getString(Property.MILESTONES), 
					"");
			pclassMilestones.setSelectList(srv.getMilestones(issue));

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
			URL fxmlURL = classLoader.getResource("com/wilutions/itol/NewIssue5.fxml");

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
		IssueService srv = Globals.getIssueService();
		if (saveAndValidate) {

		} else {
			initSubject();
			initChoiceBox(cbTracker, srv.getIssueTypes(issue), issue.getLastUpdate().getProperty(Property.ISSUE_TYPE));

			initChoiceBox(cbCategory, srv.getCategories(issue), issue.getLastUpdate().getProperty(Property.CATEGORY));

			initChoiceBox(cbAssignee, srv.getAssignees(issue), issue.getLastUpdate().getProperty(Property.ASSIGNEE));

			//initChoiceBox(cbStatus, srv.getIssueStates(issue), issue.getLastUpdate().getProperty(Property.STATE));
			
			initAccordion();

			initDescription();

			initProperties();
			
			initAttachments();
		}
	}

	private void initAttachments() {
		if (tabAttachments.getColumns().size() != 3) {
			AttachmentTableViewHandler.apply(tabAttachments);
		}
		List<Attachment> atts = issue.getAttachments();
		{
			Attachment att = new Attachment();
			att.setFileName("abc.msg");
			att.setContentLength(12345);
			atts.add(att);
		}
		{
			Attachment att = new Attachment();
			att.setFileName("def.txt");
			att.setContentLength(123456789);
			atts.add(att);
		}
		ObservableList<Attachment> obs = FXCollections.observableList(atts);
		tabAttachments.setItems(obs);
	}

	private void addGridProperty(PropertyClass propClass, int rowIdx) throws IOException {
		Property prop = issue.getLastUpdate().getProperty(propClass.getId());
		Label label = new Label(propClass.getName());
		Node editNode = null;
		switch (propClass.getType()) {
		case PropertyClass.TYPE_ARRAY_STRING: {
			ChoiceBox<IdName> cb = new ChoiceBox<IdName>();
			initChoiceBox(cb, propClass.getSelectList(), prop);
			editNode = cb;
		}
			break;
		}
		gridProps.add(label, 0, rowIdx);
		gridProps.add(editNode, 1, rowIdx);
	}

	private void initProperties() throws IOException {
		addGridProperty(pclassPriority, 0);
		addGridProperty(pclassMilestones, 1);
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

	private void initialUpdate() throws IOException {
		accIssue.setExpandedPane(tpDescription);

		updateData(false);
	}

	private void addOrRemoveAccordionPane(TitledPane t, boolean add) {
		if (add) {
			for (TitledPane p : accIssue.getPanes()) {
				if (p == t)
					return;
			}
			accIssue.getPanes().add(t);
		} else {
			accIssue.getPanes().remove(t);
		}
	}

	private void initAccordion() {
		addOrRemoveAccordionPane(tpHistory, !isNew());
		addOrRemoveAccordionPane(tpNotes, !isNew());

	}
}
