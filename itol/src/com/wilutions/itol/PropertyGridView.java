package com.wilutions.itol;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.PropertyClasses;

public class PropertyGridView {

	private final GridPane propGrid;
	private final PropertyClasses propertyClasses;
	private final ResourceBundle resb = Globals.getResourceBundle();
	private final IssueService srv = Globals.getIssueService();

	public PropertyGridView(GridPane propGrid) throws IOException {
		this.propGrid = propGrid;
		this.propertyClasses = srv.getPropertyClasses();
		ColumnConstraints constr0 = propGrid.getColumnConstraints().get(0);
		constr0.setPercentWidth(38);
	}

	public void initProperties(Issue issue) throws IOException {
		propGrid.getChildren().clear();

		int rowIndex = 0;
		addProperty(issue, Property.PRIORITY, rowIndex++);
		// for (int i = 0; i < 100; i++)
//		addProperty(issue, Property.MILESTONES, rowIndex++);

		// Add custom properties
	}

	private void addProperty(Issue issue, String propertyId, int rowIndex) throws IOException {
		Property prop = issue.getLastUpdate().getProperty(propertyId);
		if (prop == null)
			return;

		PropertyClass pclass = srv.getPropertyClass(propertyId, issue);
		if (pclass == null) {
			pclass = new PropertyClass(PropertyClass.TYPE_ARRAY_STRING, prop.getId(), resb.getString(prop.getId()),
					prop.getValue());
			propertyClasses.add(pclass);
		}

		Label label = new Label(pclass.getName());
		propGrid.add(label, 0, rowIndex);

		List<IdName> selectList = pclass.getSelectList();
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
			propGrid.add(cb, 1, rowIndex);
		} else {
			TextField ed = new TextField();
			ed.setText((String) prop.getValue());
			propGrid.add(ed, 1, rowIndex);
		}

	}

}
