package com.wilutions.itol;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;

public class PropertyTableViewHandler_off {
	
	private final TableView<Property> table;
	private final HashMap<String, PropertyClass> propertyClasses = new HashMap<String, PropertyClass>();
	private final ResourceBundle resb = Globals.getResourceBundle();
	private final IssueService srv = Globals.getIssueService();

	public PropertyTableViewHandler_off(TableView<Property> table) throws IOException {
		this.table = table;
		init();
	}
	
	public void initProperties(Issue issue) throws IOException {
		table.getItems().clear();
		
		addProperty(issue, Property.PRIORITY);
		
		// Add custom properties
	}
	
	private void addProperty(Issue issue, String propertyId) throws IOException {
		Property prop = issue.getCurrentUpdate().getProperty(propertyId);
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
		
		table.getItems().add(prop);
	}

	private void init() {
		
		TableColumn<Property, String> nameColumn = new TableColumn<>(resb.getString("IssuePropertyName"));
		final int iconColumnWidth = 200;
		nameColumn.setPrefWidth(iconColumnWidth);
//		iconColumn.setMaxWidth(iconColumnWidth);
//		iconColumn.setMinWidth(iconColumnWidth);
		nameColumn.setCellValueFactory(new PropertyValueFactory<Property, String>("id"));
		nameColumn.setCellFactory(new Callback<TableColumn<Property, String>, TableCell<Property, String>>() {

			@Override
			public TableCell<Property, String> call(TableColumn<Property, String> item) {
				TableCell<Property, String> cell = new TableCell<Property, String>() {
					@Override
					protected void updateItem(String propertyId, boolean empty) {
						super.updateItem(propertyId, empty);
						if (propertyId != null) {
							PropertyClass pclass = PropertyTableViewHandler_off.this.propertyClasses.get(propertyId);
							String text = pclass != null ? pclass.getName() : "";
							setText(text);
						}
					}
				};
				return cell;
			}
			
		});

		List<String> levelChoice = FXCollections.observableArrayList (
			    new String("Bla"),
			    new String("Blo")
			);
		
		TableColumn<Property, List<String>> valueColumn = new TableColumn<>(resb.getString("IssuePropertyValue"));
		valueColumn.setCellFactory(ComboBoxTableCell.forTableColumn(levelChoice));
		valueColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Property,List<String>>>() {

			@Override
			public void handle(CellEditEvent<Property, List<String>> t) {
				((Property) t.getTableView().getItems().get(t.getTablePosition().getRow())).setValue(t.getNewValue());
			}
			
		});
		
//		TableColumn<Property, Property> valueColumn = new TableColumn<>(resb.getString("IssuePropertyValue"));
//		valueColumn.setCellValueFactory(new PropertyValueFactory<Property, Property>("value") {
//			@Override
//			public ObservableValue<Property> call(CellDataFeatures<Property, Property> rowData) {
//				return new SimpleObjectProperty<Property>(rowData.getValue());
//			}
//		});
//		
//		valueColumn.setCellFactory(new Callback<TableColumn<Property, Property>, TableCell<Property, Property>>() {
//
//			@Override
//			public TableCell<Property, Property> call(TableColumn<Property, Property> item) {
//				TableCell<Property, Property> cell = new TableCell<Property, Property>() {
//					@Override
//					protected void updateItem(Property prop, boolean empty) {
//						super.updateItem(prop, empty);
//						if (prop != null) {
//							String str = getPropertyDisplayValue(prop);
//							setText(str);
//						}
//					}
//
//				};
//				return cell;
//			}
//			
//		});
		
		table.getColumns().clear();
		table.getColumns().add(nameColumn);
		table.getColumns().add(valueColumn);

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
