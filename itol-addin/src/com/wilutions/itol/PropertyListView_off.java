package com.wilutions.itol;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;

public class PropertyListView_off {
	
	private final ListView<Property> listView;
	private final HashMap<String, PropertyClass> propertyClasses = new HashMap<String, PropertyClass>();
	private final ResourceBundle resb = Globals.getResourceBundle();
	private final IssueService srv = Globals.getIssueService();

	public PropertyListView_off(ListView<Property> listView) throws IOException {
		this.listView = listView;
		init();
	}
	
	public void initProperties(Issue issue) throws IOException {
		listView.getItems().clear();
		
		int rowIndex = 0;
		addProperty(issue, Property.PRIORITY, rowIndex++);
		
		// Add custom properties
	}
	
	private void addProperty(Issue issue, String propertyId, int rowIndex) throws IOException {
		Property prop = issue.getLastUpdate().getProperty(propertyId);
		if (prop == null) return;
		
		PropertyClass pclass = new PropertyClass(PropertyClass.TYPE_STRING_LIST, 
				prop.getId(),
				resb.getString(prop.getId()), 
				prop.getValue());
		
//		if (prop.getId().equals(Property.PRIORITY)) {
//			pclass.setSelectList(srv.getPriorities(issue));
//		}
//		else if (prop.getId().equals(Property.MILESTONES)) {
//			pclass.setSelectList(srv.getMilestones(issue));
//		}
		
		propertyClasses.put(prop.getId(), pclass);
		
		listView.getItems().add(prop);
	}
	
	private void init() {
		
		listView.setCellFactory(new Callback<ListView<Property>, ListCell<Property>>() {
			@Override
			public ListCell<Property> call(ListView<Property> list) {
				return new PropertyListCell();
			}
		});

	}
	
	private class PropertyListCell extends ListCell<Property> {
		@Override
		public void updateItem(Property prop, boolean empty) {
			super.updateItem(prop, empty);
			if (prop != null) {
				HBox hbox = new HBox();
				hbox.setPrefWidth(Double.MAX_VALUE);
				hbox.setMaxWidth(Double.MAX_VALUE);
				
				PropertyClass pclass = propertyClasses.get(prop.getId());
				Label label = new Label(pclass.getName());
				hbox.getChildren().add(label);
				
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
					hbox.getChildren().add(cb);
				}
				else {
					TextField ed = new TextField();
					ed.setText((String)prop.getValue());
					hbox.getChildren().add(ed);
				}
				
				setGraphic(hbox);
			}
		}
	}

	private String getPropertyDisplayValue(Property prop) {
		String str = "";
		Object value = prop.getValue();
		PropertyClass propClass = propertyClasses.get(prop.getId());
		if (propClass != null) {
			List<IdName> idns = propClass.getSelectList();
			if (idns != null) {
				for (IdName idn : idns) {
					if (idn.getId().equals(value)) {
						str = idn.getName();
						break;
					}
				}
			}
		}
		return str;
	}

}
