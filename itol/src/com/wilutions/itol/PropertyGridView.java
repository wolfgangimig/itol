package com.wilutions.itol;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
	private final ResourceBundle resb = Globals.getResourceBundle();
	private final IssueService srv = Globals.getIssueService();

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
		case PropertyClass.TYPE_ISO_DATE: {
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
			break;
		}
		case PropertyClass.TYPE_BOOL: {
			ctrl = new CheckBox();
			break;
		}
		default: {
			if (selectList != null && selectList.size() != 0) {

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
				
			} else {
				TextField ed = new TextField();
				ed.setText((String) prop.getValue());
				ctrl = ed;
			}
		}
		}

		propGrid.add(ctrl, 1, rowIndex);

	}
}
