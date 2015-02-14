package com.wilutions.itol;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;

public class PropertyGridView {

	private final GridPane propGrid;
	private final IssueService srv = Globals.getIssueService();
	private Node firstControl;

	public PropertyGridView(GridPane propGrid) throws IOException {
		this.propGrid = propGrid;
		ColumnConstraints constr0 = propGrid.getColumnConstraints().get(0);
		constr0.setPercentWidth(38);
	}
	
	public void initProperties(Issue issue) throws IOException {
		propGrid.getChildren().clear();
		propGrid.getRowConstraints().clear();

		int rowIndex = 0;

		List<String> propOrder = srv.getPropertyDisplayOrder(issue);

		for (String propertyId : propOrder) {
			if (isPropertyForGrid(propertyId)) {
				PropertyClass propClass = srv.getPropertyClass(propertyId, issue);
				if (propClass != null) {
					addProperty(issue, propertyId, rowIndex++);
				}
			}
		}

	}

	private boolean isPropertyForGrid(String propertyId) {
		boolean ret = false;
		switch (propertyId) {
		case Property.ASSIGNEE:
		case Property.SUBJECT:
		case Property.CATEGORY:
		case Property.ATTACHMENTS:
		case Property.DESCRIPTION:
		case Property.ISSUE_TYPE:
		case Property.STATUS:
			ret = false;
			break;
		default:
			ret = true;
			break;
		}
		return ret;
	}

	private void addProperty(Issue issue, String propertyId, int rowIndex) throws IOException {
		Property prop = issue.getLastUpdate().getProperty(propertyId);
		if (prop == null)
			return;

		PropertyClass pclass = srv.getPropertyClass(propertyId, issue);
		if (pclass == null) {
			return;
		}

		Label label = new Label(pclass.getName());
		propGrid.add(label, 0, rowIndex);

		Node ctrl = null;
		List<IdName> selectList = pclass.getSelectList();

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
		default: {
			if (selectList != null && selectList.size() != 0) {
				ctrl = makeChoiceBoxForProperty(prop, selectList);
			} else {
				ctrl = makeTextFieldForProperty(prop);
			}
		}
		}

		propGrid.add(ctrl, 1, rowIndex);
		if (firstControl == null) {
			firstControl = ctrl;
		}
	}

	@SuppressWarnings("unchecked")
	private Node makeMultiListBoxForProperty(Property prop, List<IdName> selectList) {
		ListView<IdName> lb = new ListView<IdName>();
		lb.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		lb.setItems(FXCollections.observableArrayList(selectList));
		lb.setPrefHeight(100);
		Object propValue = prop.getValue();
		List<String> values = new ArrayList<String>(0);
		if (propValue instanceof List) {
			values = (List<String>) propValue;
		}
		for (IdName item : selectList) {
			if (values.indexOf(item.getId()) != -1) {
				lb.getSelectionModel().select(item);
			}
		}
		return lb;
	}

	private Node makeTextFieldForProperty(Property prop) {
		Node ctrl;
		TextField ed = new TextField();
		ed.setText((String) prop.getValue());
		ctrl = ed;
		return ctrl;
	}

	private Node makeChoiceBoxForProperty(Property prop, List<IdName> selectList) {
		Node ctrl;
		ChoiceBox<IdName> cb = new ChoiceBox<IdName>();
		cb.setItems(FXCollections.observableArrayList(selectList));
		Object propValue = prop.getValue();
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
		ctrl = cb;
		return ctrl;
	}

	private CheckBox makeCheckBoxForProperty(Property prop) {
		CheckBox cb = new CheckBox();
		Object propValue = prop.getValue();
		boolean bValue = false;
		if (propValue != null) {
			if (propValue instanceof Boolean) {
				bValue = (Boolean)propValue;
			}
			else {
				String str = propValue.toString().toLowerCase();
				bValue = str.equals("1") || str.equals("yes") || str.equals("true");
			}
		}
		cb.setSelected(bValue);
		return cb;
	}

	private Node makeDatePickerForProperty(Property prop) {
		Node ctrl;
		DatePicker dpick = new DatePicker();
		String iso = (String) prop.getValue();
		if (iso != null && iso.length() != 0) {
			if (iso.length() > 10) {
				iso = iso.substring(0, 10);
			}
			LocalDate ldate = LocalDate.parse(iso);
			dpick.setValue(ldate);
		}
		ctrl = dpick;
		return ctrl;
	}

	public Node getFirstControl() {
		return firstControl;
	}
}
