package com.wilutions.itol;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.control.CheckComboBox;

import com.wilutions.fx.acpl.AutoCompletionComboBox;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.Suggest;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class PropertyGridView {

	private final GridPane propGrid;
	private final ResourceBundle resb = Globals.getResourceBundle();
	private Node firstControl;
	private Logger log = Logger.getLogger("PropertyGridView");

	private static class PropertyNode {
		String propertyId;
		Node node;
	}

	private final List<PropertyNode> propNodes = new ArrayList<>();

	public PropertyGridView(GridPane propGrid) throws IOException {
		this.propGrid = propGrid;
		ColumnConstraints constr0 = propGrid.getColumnConstraints().get(0);
		constr0.setPercentWidth(38);
		propGrid.setVgap(8);
	}

	public void initProperties(Issue issue) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "PropertyGridView(");
		propGrid.getChildren().clear();
		propGrid.getRowConstraints().clear();
		propNodes.clear();

		int rowIndex = 0;
		IssueService srv = Globals.getIssueService();
		List<String> propOrder = srv.getPropertyDisplayOrder(issue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "propOrder=" + propOrder);

		for (String propertyId : propOrder) {
			if (isPropertyForGrid(propertyId)) {
				PropertyClass propClass = srv.getPropertyClass(propertyId, issue);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "propClass=" + propClass);
				if (propClass != null) {
					addProperty(issue, propClass, rowIndex++);
				}
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")PropertyGridView");
	}

	public void saveProperties(Issue issue) {
		for (PropertyNode propNode : propNodes) {
			String propertyId = propNode.propertyId;
			Node node = propNode.node;
			if (node instanceof TextField) {
				issue.setPropertyString(propertyId, ((TextField) node).getText());
			}
			else if (node instanceof DatePicker) {
				LocalDate ldate = ((DatePicker) node).getValue();
				String iso = "";
				if (ldate != null) {
					iso = ldate.format(DateTimeFormatter.ISO_LOCAL_DATE);
				}
				issue.setPropertyString(propertyId, iso);
			}
			else if (node instanceof CheckBox) {
				boolean value = ((CheckBox) node).isSelected();
				issue.setPropertyBoolean(propertyId, value);
			}
			else if (node instanceof ChoiceBox) {
				@SuppressWarnings("unchecked")
				ChoiceBox<IdName> cb = (ChoiceBox<IdName>) node;
				IdName idn = cb.getSelectionModel().getSelectedItem();
				issue.setPropertyIdName(propertyId, idn);
			}
			else if (node instanceof ComboBox) {
				@SuppressWarnings("unchecked")
				ComboBox<IdName> cb = (ComboBox<IdName>) node;
				IdName idn = cb.getSelectionModel().getSelectedItem();
				issue.setPropertyIdName(propertyId, idn);
			}
			else if (node instanceof ListView) {
				List<String> ids = new ArrayList<String>();
				@SuppressWarnings("unchecked")
				ListView<CheckedIdName> lb = (ListView<CheckedIdName>) node;
				for (CheckedIdName idn : lb.getItems()) {
					if (idn.checked.get()) {
						ids.add(idn.getId());
					}
				}
				issue.setPropertyStringList(propertyId, ids);
			}
		}
	}

	private boolean isPropertyForGrid(String propertyId) {
		boolean ret = true;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isPropertyForGrid=" + ret);
		return ret;
	}

	private void addProperty(Issue issue, PropertyClass pclass, int rowIndex) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "addProperty(" + pclass);

		Label label = new Label(pclass.getName());
		propGrid.add(label, 0, rowIndex);
		GridPane.setValignment(label, VPos.TOP);

		Region ctrl = null;
		List<IdName> selectList = pclass.getSelectList();
		Suggest<IdName> suggest = pclass.getAutoCompletionSuggest();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "selectList=" + selectList + ", suggest=" + suggest);

		Property prop = issue.getCurrentUpdate().getProperty(pclass.getId());
		if (prop != null && prop.getValue() == null) {
			Object defaultValue = pclass.getDefaultValue();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "defaultValue=" + defaultValue);
			prop.setValue(defaultValue);
		}

		switch (pclass.getType()) {
		case PropertyClass.TYPE_ISO_DATE:
			ctrl = makeDatePickerForProperty(prop);
			break;
		case PropertyClass.TYPE_BOOL:
			ctrl = makeCheckBoxForProperty(prop);
			break;
		// case PropertyClass.TYPE_INTEGER:
		// ctrl = makeIntFieldForProperty(prop);
		// break;
		// case PropertyClass.TYPE_FLOAT:
		// ctrl = makeFloatFieldForProperty(prop);
		// break;
		default: {
			if (selectList != null) {
				if (pclass.isArray()) {
					if (selectList.size() > 3) {
						ctrl = makeChoiceBoxForPropertyArray(prop, issue, pclass);
					}
					else {
						ctrl = makeMultiListBoxForProperty(prop, issue, pclass);
					}
				}
				else {
					ctrl = makeChoiceBoxForProperty(prop, issue, pclass);
				}
			}
			else if (suggest != null) {
				if (pclass.isArray()) {
					ctrl = makeAutoCompletionNodeArray(prop, issue, pclass);
				}
				else {
					ctrl = makeAutoCompletionNode(prop, issue, pclass);
				}
			}
			else {
				ctrl = makeTextFieldForProperty(prop);
			}
		}
		}

		ctrl.setMaxWidth(Double.MAX_VALUE);
		ctrl.setPrefWidth(Double.MAX_VALUE);
		propGrid.add(ctrl, 1, rowIndex);

		if (rowIndex == 0) {
			firstControl = ctrl;
		}

		// Save propertId with node to simplify access in saveProperties()
		PropertyNode propNode = new PropertyNode();
		propNode.propertyId = pclass.getId();
		propNode.node = ctrl;
		propNodes.add(propNode);

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")addProperty");
	}

	private class AutoCompletionNodeArray_FirstButton extends Button {
		
		AutoCompletionNodeArray_FirstButton(VBox vbox, Property prop, Issue issue, PropertyClass pclass) {
			super("+");
			this.setMaxWidth(25);
			this.setOnAction(new AutoCompletionNodeArray_onAdd(vbox, prop, issue, pclass) {
				@Override
				public void handle(ActionEvent event) {
					vbox.getChildren().remove(0);
					super.handle(event);
				}
			});

		}
	}
	
	private class AutoCompletionNodeArray_onAdd implements EventHandler<ActionEvent> {
		VBox vbox;
		Property prop;
		Issue issue;
		PropertyClass pclass;
		
		AutoCompletionNodeArray_onAdd(VBox vbox, Property prop, Issue issue, PropertyClass pclass) {
			this.vbox = vbox;
			this.prop = prop;
			this.issue = issue;
			this.pclass = pclass;
		}

		@Override
		public void handle(ActionEvent event) {
			final HBox thisHBox = new HBox();
			thisHBox.setSpacing(4);
			Button bnDelete = new Button("x");
			bnDelete.setMaxWidth(25);
			bnDelete.setPrefWidth(25);
//			bnDelete.setStyle("-fx-background-color: white");
//			Image imageDelete = new Image(getClass().getResourceAsStream("delete.png"));
//			bnDelete.setGraphic(new ImageView(imageDelete));
			bnDelete.setOnAction((_ignored) -> {
				List<Node> vchildren = vbox.getChildren();
				vchildren.remove(thisHBox);
				if (vchildren.isEmpty()) {
					vchildren.clear();
					Button firstButton = new AutoCompletionNodeArray_FirstButton(vbox, prop, issue, pclass);
					vchildren.add(firstButton);
					firstButton.requestFocus();
				}
				else {
					HBox lastRow = (HBox)vchildren.get(vchildren.size()-1);
					Button bnAdd_1 = (Button)lastRow.getChildren().get(1);
					bnAdd_1.setDisable(false);
				}
			});
			
			Control ed = makeAutoCompletionNode(prop, issue, pclass);
			ed.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(ed, Priority.ALWAYS);
			Button bnAdd2 = new Button("+");
			bnAdd2.setPrefWidth(25);
			bnAdd2.setMaxWidth(25);
//			bnAdd2.setStyle("-fx-background-color: white");
			bnAdd2.setOnAction(new AutoCompletionNodeArray_onAdd(vbox, prop, issue, pclass));
			
			List<Node> children = vbox.getChildren();
			if (!children.isEmpty()) {
				HBox hbox_1 = (HBox)children.get(children.size()-1);
				Button bnAdd_1 = (Button)hbox_1.getChildren().get(1);
				bnAdd_1.setDisable(true);
			}
			
			thisHBox.getChildren().addAll(ed, bnAdd2, bnDelete);
			children.add(thisHBox);
			
			ed.requestFocus();
		}
	};

	private VBox makeAutoCompletionNodeArray(Property prop, Issue issue, PropertyClass pclass) {
		VBox vbox = new VBox();
		vbox.setSpacing(4);
		
		Button bnAdd = new AutoCompletionNodeArray_FirstButton(vbox, prop, issue, pclass);
		vbox.getChildren().add(bnAdd);
		
		return vbox;
	}

	private static class CheckedIdName extends IdName {
		private static final long serialVersionUID = -5331330727211218988L;
		private final SimpleBooleanProperty checked = new SimpleBooleanProperty(false);

		public CheckedIdName(IdName rhs, boolean checked) {
			super(rhs);
			this.checked.set(checked);
		}

		public SimpleBooleanProperty checkedProperty() {
			return checked;
		}
	}

	@SuppressWarnings("unchecked")
	private Control makeMultiListBoxForProperty(Property prop, Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeMultiListBoxForProperty(");
		List<IdName> selectList = pclass.getSelectList();
		Object propValue = prop != null ? prop.getValue() : null;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + propValue);

		List<CheckedIdName> items = new ArrayList<CheckedIdName>(selectList.size());
		for (IdName idn : selectList) {
			boolean checked = false;
			if (propValue != null) {
				if (propValue instanceof List) {
					checked = ((List<String>) propValue).contains(idn.getId());
				}
				else if (propValue instanceof String) {
					checked = ((String) propValue).equals(idn.getId());
				}
			}
			items.add(new CheckedIdName(idn, checked));
		}

		ListView<CheckedIdName> lb = new ListView<CheckedIdName>();
		Callback<CheckedIdName, ObservableValue<Boolean>> getProperty = new Callback<CheckedIdName, ObservableValue<Boolean>>() {
			@Override
			public BooleanProperty call(CheckedIdName idn) {
				return idn.checkedProperty();
			}
		};
		Callback<ListView<CheckedIdName>, ListCell<CheckedIdName>> forListView = CheckBoxListCell
				.forListView(getProperty);
		lb.setCellFactory(forListView);

		lb.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		lb.setItems(FXCollections.observableArrayList(items));
		lb.setMinHeight(25);
		lb.setPrefHeight(25 * items.size());
		lb.setMaxHeight(25 * 6);

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeMultiListBoxForProperty");
		return lb;
	}

	private Control makeTextFieldForProperty(Property prop) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeTextFieldForProperty(");
		TextField ed = new TextField();
		if (prop != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + prop.getValue());
			ed.setText((String) prop.getValue());
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeTextFieldForProperty");
		return ed;
	}

	private Control makeChoiceBoxForProperty(Property prop, Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		ComboBox<IdName> cb = new ComboBox<IdName>();
		List<IdName> selectList = pclass.getSelectList();
		cb.setItems(FXCollections.observableArrayList(selectList));
		if (prop != null) {
			Object propValue = prop.getValue();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + propValue);
			String value = "";
			if (propValue instanceof String) {
				value = (String) propValue;
			}
			for (IdName item : selectList) {
				if (item.getId().equals(value)) {
					cb.setValue(item);
					break;
				}
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeChoiceBoxForProperty");
		return cb;
	}

	@SuppressWarnings("rawtypes")
	private Control makeChoiceBoxForPropertyArray(Property prop, Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		List<IdName> selectList = pclass.getSelectList();
		CheckComboBox<IdName> cb = new CheckComboBox<IdName>(FXCollections.observableArrayList(selectList));
		
		if (prop != null) {
			Object propValue = prop.getValue();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + propValue);
			
			for (IdName idn : selectList) {
				boolean checked = false;
				if (propValue != null) {
					if (propValue instanceof List) {
						switch(pclass.getType()) {
						case PropertyClass.TYPE_STRING:
							checked = ((List)propValue).contains(idn.getId());
							break;
						case PropertyClass.TYPE_ID_NAME:
							checked = ((List)propValue).contains(idn);
							break;
						}
					}
					else if (propValue instanceof String) {
						switch(pclass.getType()) {
						case PropertyClass.TYPE_STRING:
							checked = propValue.equals(idn.getId());
							break;
						case PropertyClass.TYPE_ID_NAME:
							checked = propValue.equals(idn);
							break;
						}
					}
				}
				
				if (checked) {
					cb.getCheckModel().getCheckedItems().add(idn);
				}
			}
			
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeChoiceBoxForProperty");
		return cb;
	}

	private Control makeAutoCompletionNode(final Property prop, final Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeAutoCompletionNode(" + prop.getId());
		String recentCaption = resb.getString("autocomplete.recentCaption");
		String suggestionsCaption = resb.getString("autocomplete.suggestionsCaption");
		List<IdName> recentItems = pclass.getSelectList();
		if (recentItems == null) recentItems = new ArrayList<IdName>();
		ExtractImage<IdName> extractImage = (item) -> item.getImage();

		AutoCompletionComboBox<IdName> comboBox = AutoCompletions.createAutoCompletionNode(extractImage, recentCaption,
				suggestionsCaption, recentItems, pclass.getAutoCompletionSuggest());

		IdName item = (IdName)prop.getValue();
		if (item != null) {
			comboBox.getBinding().select(item);
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeAutoCompletionNode");
		return comboBox;
	}

	private CheckBox makeCheckBoxForProperty(Property prop) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeCheckBoxForProperty(");
		CheckBox cb = new CheckBox();
		boolean bValue = false;
		if (prop != null) {
			Object propValue = prop.getValue();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + propValue);
			if (propValue != null) {
				if (propValue instanceof Boolean) {
					bValue = (Boolean) propValue;
				}
				else {
					String str = propValue.toString().toLowerCase();
					bValue = str.equals("1") || str.equals("yes") || str.equals("true");
				}
			}
		}
		cb.setSelected(bValue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeCheckBoxForProperty");
		return cb;
	}

	private Control makeDatePickerForProperty(Property prop) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeDatePickerForProperty(");
		Control ctrl;
		DatePicker dpick = new DatePicker();
		if (prop != null) {
			String iso = (String) prop.getValue();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + iso);
			if (iso != null && iso.length() != 0) {
				if (iso.length() > 10) {
					iso = iso.substring(0, 10);
				}
				LocalDate ldate = LocalDate.parse(iso);
				dpick.setValue(ldate);
			}
		}
		ctrl = dpick;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeDatePickerForProperty");
		return ctrl;
	}

	public Node getFirstControl() {
		return firstControl;
	}

}
