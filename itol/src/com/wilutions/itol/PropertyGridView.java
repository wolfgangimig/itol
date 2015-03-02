package com.wilutions.itol;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;

public class PropertyGridView {

	private final GridPane propGrid;
	private final IssueService srv = Globals.getIssueService();
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
	}

	public void initProperties(Issue issue) throws IOException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "PropertyGridView(");
		propGrid.getChildren().clear();
		propGrid.getRowConstraints().clear();
		propNodes.clear();

		int rowIndex = 0;

		List<String> propOrder = srv.getPropertyDisplayOrder(issue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "propOrder=" + propOrder);

		for (String propertyId : propOrder) {
			if (isPropertyForGrid(propertyId)) {
				PropertyClass propClass = srv.getPropertyClass(propertyId, issue);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "propClass=" + propClass);
				if (propClass != null) {
					addProperty(issue, propertyId, rowIndex++);
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
			} else if (node instanceof DatePicker) {
				LocalDate ldate = ((DatePicker) node).getValue();
				String iso = "";
				if (ldate != null) {
					iso = ldate.format(DateTimeFormatter.ISO_LOCAL_DATE);
				}
				issue.setPropertyString(propertyId, iso);
			} else if (node instanceof CheckBox) {
				boolean value = ((CheckBox) node).isSelected();
				issue.setPropertyBoolean(propertyId, value);
			} else if (node instanceof ChoiceBox) {
				@SuppressWarnings("unchecked")
				ChoiceBox<IdName> cb = (ChoiceBox<IdName>) node;
				IdName idn = cb.getSelectionModel().getSelectedItem();
				if (idn != null) {
					issue.setPropertyString(propertyId, idn.getId());
				}
			} else if (node instanceof ListView) {
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
		// switch (propertyId) {
		// case Property.PRIORITY:
		// case Property.SUBJECT:
		// case Property.CATEGORY:
		// case Property.ATTACHMENTS:
		// case Property.DESCRIPTION:
		// case Property.ISSUE_TYPE:
		// case Property.STATUS:
		// ret = false;
		// break;
		// default:
		// ret = true;
		// break;
		// }
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isPropertyForGrid=" + ret);
		return ret;
	}

	private void addProperty(Issue issue, String propertyId, int rowIndex) throws IOException {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "addProperty(" + propertyId);

		PropertyClass pclass = srv.getPropertyClass(propertyId, issue);
		if (pclass == null) {
			return;
		}

		Label label = new Label(pclass.getName());
		propGrid.add(label, 0, rowIndex);

		Node ctrl = null;
		List<IdName> selectList = pclass.getSelectList();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "selectList=" + selectList);

		Property prop = issue.getLastUpdate().getProperty(propertyId);
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
		case PropertyClass.TYPE_STRING_LIST:
			ctrl = makeMultiListBoxForProperty(prop, selectList);
			break;
//		case PropertyClass.TYPE_INTEGER:
//			ctrl = makeIntFieldForProperty(prop);
//			break;
//		case PropertyClass.TYPE_FLOAT:
//			ctrl = makeFloatFieldForProperty(prop);
//			break;
		default: {
			if (selectList != null && selectList.size() != 0) {
				ctrl = makeChoiceBoxForProperty(prop, selectList);
			} else {
				ctrl = makeTextFieldForProperty(prop);
			}
		}
		}

		propGrid.add(ctrl, 1, rowIndex);

		if (rowIndex == 0) {
			firstControl = ctrl;
		}

		// Save propertId with node to simplify access in saveProperties()
		PropertyNode propNode = new PropertyNode();
		propNode.propertyId = propertyId;
		propNode.node = ctrl;
		propNodes.add(propNode);
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")addProperty");
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
	private Node makeMultiListBoxForProperty(Property prop, List<IdName> selectList) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		Object propValue = prop != null ? prop.getValue() : null;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + propValue);

		List<CheckedIdName> items = new ArrayList<CheckedIdName>(selectList.size());
		for (IdName idn : selectList) {
			boolean checked = false;
			if (propValue != null) {
				if (propValue instanceof List) {
					checked = ((List<String>) propValue).contains(idn.getId());
				} else if (propValue instanceof String) {
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
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeChoiceBoxForProperty");
		return lb;
	}

	private Node makeTextFieldForProperty(Property prop) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		TextField ed = new TextField();
		if (prop != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + prop.getValue());
			ed.setText((String) prop.getValue());
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeChoiceBoxForProperty");
		return ed;
	}

	private Node makeChoiceBoxForProperty(Property prop, List<IdName> selectList) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		ChoiceBox<IdName> cb = new ChoiceBox<IdName>();
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
	
	private Node makeTextFieldWithSuggestions(Property prop, Callback<String, List<String>> findSuggestions) {
		Node node = null;
		
		return node;
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
				} else {
					String str = propValue.toString().toLowerCase();
					bValue = str.equals("1") || str.equals("yes") || str.equals("true");
				}
			}
		}
		cb.setSelected(bValue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeCheckBoxForProperty");
		return cb;
	}

	private Node makeDatePickerForProperty(Property prop) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeDatePickerForProperty(");
		Node ctrl;
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
