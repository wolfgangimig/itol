package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.ComException;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.DescriptionHtmlEditor;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueUpdate;
import com.wilutions.itol.db.Property;
import com.wilutions.joa.fx.MessageBox;
import com.wilutions.joa.fx.TaskPaneFX;
import com.wilutions.mslib.office.CustomTaskPane;
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

	private DescriptionHtmlEditor descriptionHtmlEditor;
	private WebView webView;
	private final MailInspector mailInspector;
	private final MailItem mailItem;

	private ResourceBundle resb;

	public IssueTaskPane(MailInspector mailInspector, MailItem mailItem) {
		this.mailInspector = mailInspector;
		this.mailItem = mailItem;
		Globals.getThisAddin().getRegistry().readFields(this);
	}

	@Override
	public void close() {
		super.close();
		Globals.getThisAddin().getRegistry().writeFields(this);
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

			rootGrid.add(webView, 0, 1, 3, 1);
		} else {
			edDescription.setHtmlText("<pre>" + descr + "</pre>");
		}
	}

	@Override
	// This method is called by the FXMLLoader when initialization is complete
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		try {
			//lvAttachments.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			
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
			String text = (String) webView.getEngine().executeScript(scriptToGetDescription);
			issue.setDescription(text);
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

	private void initAttachments() {
		
		FileListViewHandler.apply(lvAttachments);
		
		List<File> items = new ArrayList<File>();
		items.add(new File(mailItem.getSubject() + ".msg"));
		items.add(new File(mailItem.getSubject() + ".pdf"));
		items.add(new File(mailItem.getSubject() + ".txt"));
//		items.add(new File(mailItem.getSubject() + ".zip"));
//		items.add(new File(mailItem.getSubject() + ".cpp"));
//		items.add(new File(mailItem.getSubject() + ".jar"));
		
		ObservableList<File> lvitems = FXCollections.observableArrayList(items);
		lvAttachments.setItems(lvitems);
	}

	private Attachment createMailAttachment() throws IOException {
		IssueService srv = Globals.getIssueService();
		File tempFile = File.createTempFile("issue", ".msg");
		mailItem.SaveAs(tempFile.getAbsolutePath(), OlSaveAsType.olMSG);
		Attachment att = new Attachment();
		att.setSubject(mailItem.getSubject());
		att.setContentType(".msg");
		att.setStream(new FileInputStream(tempFile));
		att.setContentLength(tempFile.length());
		att = srv.writeAttachment(att);
		tempFile.delete();
		return att;
	}

	private void addAttachment(IssueUpdate isu, Attachment att) {
		Property propAttachments = isu.getProperty(Property.ATTACHMENTS);
		if (propAttachments.isNull()) {
			propAttachments = new Property(Property.ATTACHMENTS, new Attachment[] { att });
		} else {
			Attachment[] atts = (Attachment[]) propAttachments.getValue();
			Attachment[] atts2 = new Attachment[atts.length + 1];
			System.arraycopy(atts, 0, atts2, 0, atts.length);
			atts2[atts2.length - 1] = att;
			propAttachments = new Property(Property.ATTACHMENTS, atts2);
		}
		isu.setProperty(propAttachments);
	}

	@FXML
	private void onCreateIssue() throws IOException {
		onRemoveAttachment();
		
//		IssueService srv = Globals.getIssueService();
//		try {
//			updateData(true);
//
//			Attachment att = createMailAttachment();
//			addAttachment(issue.getLastUpdate(), att);
//
//			issue = srv.updateIssue(issue);
//
//			String mailSubject = srv.injectIssueIdIntoMailSubject(mailItem.getSubject(), issue);
//			mailItem.setSubject(mailSubject);
//			mailItem.Save();
//
//			MessageBox.show(this, "Created", "Issue " + issue.getId() + " has been created", (succ, ex) -> {
//				IssueTaskPane.this.mailInspector.setIssueTaskPaneVisible(false);
//				// IssueTaskPane.this.mailInspector.setHistoryTaskPaneVisible(true);
//				});
//
//		} catch (Throwable e) {
//			e.printStackTrace();
//			MessageBox.show(this, "Error", "Failed to create issue, " + e.toString(), null);
//		}
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
		System.out.println("onInsertAttachment");
		lvAttachments.getSelectionModel().getSelectedItems();
	}
	
	@FXML
	private void onRemoveAttachment() {
		System.out.println("onRemoveAttachment");
		ObservableList<Integer> selectedIndexes = lvAttachments.getSelectionModel().getSelectedIndices();
		for (Integer idx : selectedIndexes) {
			lvAttachments.getItems().remove(idx);
			break;
		}
	}
	
	@FXML
	private void onValidateRemoveAttachment() {
		System.out.println("onValidateRemoveAttachment");
	}
	
	
}
