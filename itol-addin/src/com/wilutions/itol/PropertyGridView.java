package com.wilutions.itol;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;

import com.sun.javafx.scene.control.skin.TextAreaSkin;
import com.wilutions.fx.acpl.AutoCompletionComboBox;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.fx.util.DateTimePicker;
import com.wilutions.fx.util.TextFieldWithSuggestions;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.IssuePropertyEditor;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class PropertyGridView {

	private final IssueTaskPane issueTaskPane;
	private final GridPane propGrid;
	private final Tab tabProperties;
	private final ResourceBundle resb = Globals.getResourceBundle();
	private Node firstControl;
	private Logger log = Logger.getLogger("PropertyGridView");

	private final List<PropertyNode> propNodes = new ArrayList<>();

	public PropertyGridView(IssueTaskPane issueTaskPane, Tab tabProperties, GridPane propGrid) throws IOException {
		this.issueTaskPane = issueTaskPane;
		this.propGrid = propGrid;
		this.tabProperties = tabProperties;
		ColumnConstraints constr0 = propGrid.getColumnConstraints().get(0);
		constr0.setPercentWidth(25);
	}
	
	public void pushPropertyGrid(Node replaceBy, String title) {
		Pane tabVBox = (Pane)tabProperties.getContent();
		
		HBox.setHgrow(replaceBy, Priority.ALWAYS);

		tabVBox.getChildren().add(replaceBy);
		
		Region scrollPane = (Region)tabVBox.getChildren().get(0);
		scrollPane.setVisible(false);
		scrollPane.setManaged(false);
	}
	
	public void popPropertyGrid() {
		Pane tabVBox = (Pane)tabProperties.getContent();
		Node scrollPane = tabVBox.getChildren().get(0);
		scrollPane.setVisible(true);
		scrollPane.setManaged(true);
		tabVBox.getChildren().remove(1);
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

	public void updateData(boolean save) {
		for (PropertyNode propNode : propNodes) {
			propNode.updateData(save);
		}
	}

	private boolean isPropertyForGrid(String propertyId) {
		boolean ret = true;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isPropertyForGrid=" + ret);
		return ret;
	}
	
	private void addProperty(Issue issue, PropertyClass pclass, int rowIndex) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "addProperty(" + pclass);

		Label label = new Label();
		String labelText = pclass.getName();
		if (pclass.isRequired()) {
			labelText += " *";
			//label.setStyle("-fx-font-weight: bold");
		}
		label.setText(labelText);
		propGrid.add(label, 0, rowIndex);
		GridPane.setValignment(label, VPos.TOP);


		PropertyNode propNode = makeIssuePropertyEditor(issue, pclass);
		if (propNode == null) {
			
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
				propNode = makeDatePickerForProperty(issue, pclass);
				break;
			case PropertyClass.TYPE_ISO_DATE_TIME:
				propNode = makeDateTimePickerForProperty(issue, pclass);
				break;
			case PropertyClass.TYPE_BOOL:
				propNode = makeCheckBoxForProperty(issue, pclass);
				break;
			case PropertyClass.TYPE_TEXT:
				propNode = makeTextAreaForProperty(issue, pclass);
				break;
			case PropertyClass.TYPE_FLOAT:
				propNode = makeNumericFieldForProperty(issue, pclass);
				break;
			// case PropertyClass.TYPE_INTEGER:
			// ctrl = makeIntFieldForProperty(prop);
			// break;
			case PropertyClass.TYPE_ID_NAME:
			{
				if (selectList != null) {
					if (pclass.isArray()) {
						propNode = makeChoiceBoxForPropertyArray(issue, pclass);
					}
					else {
						propNode = makeChoiceBoxForProperty(issue, pclass);
					}
				}
				else if (suggest != null) {
					if (pclass.isArray()) {
						propNode = makeAutoCompletionNodeArray(issue, pclass);
					}
					else {
						propNode = makeAutoCompletionNode(issue, pclass);
					}
				}
				else {
					// IdName field without selection list - unsupported
					log.warning("Unsupported property class " + pclass + ".");
				}
			}
			break;
			default:
			{
				propNode = (pclass.getAutoCompletionSuggest() != null) ?
						makeTextFieldWithSuggestions(issue, pclass):
						makeTextFieldForProperty(issue, pclass);
			}
			}
		}
		
		if (propNode != null) {
			Node ctrl = propNode.getNode();
			
			// Set node read-only
			ctrl.setDisable(pclass.isReadOnly());

			// Node shall occupy the entire width.
			if (ctrl instanceof Region) {
				Region region = (Region)ctrl;
				region.setMaxWidth(Double.MAX_VALUE);
				region.setPrefWidth(Double.MAX_VALUE);
			}
			
			propGrid.add(ctrl, 1, rowIndex);
	
			if (rowIndex == 0) {
				firstControl = ctrl;
			}
	
			propNodes.add(propNode);
		}
		
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
			
			Node ed = makeAutoCompletionNode(issue, pclass).getNode();
			if (ed instanceof Region) {
				((Region)ed).setMaxWidth(Double.MAX_VALUE);
			}
			
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

	private PropertyNode makeAutoCompletionNodeArray(Issue issue, PropertyClass pclass) {
		VBox vbox = new VBox();
		vbox.setSpacing(4);
		
		Property prop = issue.getCurrentUpdate().getProperty(pclass.getId());
		Button bnAdd = new AutoCompletionNodeArray_FirstButton(vbox, prop, issue, pclass);
		vbox.getChildren().add(bnAdd);
		
		PropertyNode propNode = new PropertyNode(issue, pclass, vbox) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					
				}
				else {
					
				}
			}
		};
		
		return propNode;
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

	@SuppressWarnings({ "rawtypes", "unused" })
	private Region makeMultiListBoxForProperty(Property prop, Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeMultiListBoxForProperty(");
		List<IdName> selectList = pclass.getSelectList();
		Object propValue = prop != null ? prop.getValue() : null;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "value=" + propValue);

		List<CheckedIdName> items = new ArrayList<CheckedIdName>(selectList.size());
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

	private PropertyNode makeTextFieldForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeTextFieldForProperty(");
		TextField ed = new TextField();

		PropertyNode propNode = new PropertyNode(issue, pclass, ed) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					String text = ed.getText();
					Object value = null;
					if (pclass.isArray()) {
						List<String> list = Arrays.asList(text.split(" "));
						value = list;
					}
					else {
						value = text;
					}
					issue.setPropertyValue(pclass.getId(), value);
				}
				else {
					Object value = issue.getPropertyValue(pclass.getId(), null);
					String text = "";
					if (value != null) {
						if (pclass.isArray()) {
							@SuppressWarnings("rawtypes")
							List list = (List)value;
							StringBuilder sbuf = new StringBuilder();
							for (Object elm : list) {
								if (sbuf.length() != 0) sbuf.append(" ");
								sbuf.append(elm);
							}
						}
						else {
							text = value.toString();
						}
					}
					ed.setText(text);
				}
			}
		};
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeTextFieldForProperty");
		return propNode;
	}

	private PropertyNode makeTextFieldWithSuggestions(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeTextFieldForProperty(");
		TextField ed =  new TextFieldWithSuggestions<IdName>(pclass.getAutoCompletionSuggest());

		PropertyNode propNode = new PropertyNode(issue, pclass, ed) {
			@SuppressWarnings("unchecked")
			@Override
			public void updateData(boolean save) {
				if (save) {
					String text = ed.getText();
					if (pclass.isArray()) {
						ArrayList<String> list = new ArrayList<String>();
						StringTokenizer stok = new StringTokenizer(text, TextFieldWithSuggestions.DELIMS);
						while (stok.hasMoreTokens()) {
							String tok = stok.nextToken();
							list.add(tok);
						}
						issue.setPropertyValue(pclass.getId(), list);
					}
					else {
						issue.setPropertyValue(pclass.getId(), text);
					}
				}
				else {
					String value = "";
					if (pclass.isArray()) {
						// Handling for JIRA property type "Label"
						StringBuilder sbuf = new StringBuilder();
						List<String> values = (List<String>)issue.getPropertyValue(pclass.getId(), new ArrayList<String>());
						for (int i = 0; i < values.size(); i++) {
							String value_i = values.get(i);
							if (i != 0) sbuf.append(" ");
							sbuf.append(value_i);
						}
						value = sbuf.toString();
					}
					else {
						value = issue.getPropertyIdName(pclass.getId(), IdName.NULL).toString();
					}
					ed.setText(value);
				}
			}
		};
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeTextFieldForProperty");
		return propNode;
	}

	private PropertyNode makeNumericFieldForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeNumericFieldForProperty(");
		TextField ed = new TextField();
		
		ed.addEventFilter(KeyEvent.KEY_TYPED, (keyEvent) -> {
	        if (!"0123456789.,-".contains(keyEvent.getCharacter())) {
	        	keyEvent.consume();
	        }
		});
		 
		PropertyNode propNode = new PropertyNode(issue, pclass, ed) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					
					// Hint: only 3 fraction digits are saved by JIRA 
					
					try {
						Number n = NumberFormat.getInstance().parse(ed.getText());
						issue.setPropertyValue(pclass.getId(), n);
					}
					catch (ParseException e) {
					}
				}
				else {
					Number n = (Number)issue.getPropertyValue(pclass.getId(), null);
					String value = "";
					if (n != null) {
						value = NumberFormat.getInstance().format(n);
					}
					ed.setText(value);
				}
			}
		};
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeNumericFieldForProperty");
		return propNode;
	}

	private PropertyNode makeTextAreaForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeTextAreaForProperty(");
		PropertyNode ret = null;
		try {
			final IssueService srv = Globals.getIssueService();
			final IssuePropertyEditor editor = srv.getPropertyEditor(issueTaskPane, issue, pclass.getId());
			
			// Create a standard TextArea if no special property editor is provided.
			final TextArea textArea = editor != null ? null : new TextArea();
			if (textArea != null) {
				// Forward to next control on TAB
				textArea.addEventFilter(KeyEvent.KEY_PRESSED, (e) -> {
					if (e.getCode().equals(KeyCode.TAB)) {
						TextAreaSkin skin = (TextAreaSkin)textArea.getSkin();
			            if (e.isShiftDown()) {
			                skin.getBehavior().traversePrevious();
			            }
			            else {
			                skin.getBehavior().traverseNext();
			            }
			            e.consume();
					}
				});
				textArea.setWrapText(true);
				textArea.setMinHeight(60);
				textArea.setMaxHeight(Double.MAX_VALUE);
				textArea.setPrefHeight(60);
			}
			
			VBox vboxEditor = null;
			if (editor != null) {
				final Control editorControl = (Control)editor.getNode();
				VBox.setVgrow(editorControl, Priority.ALWAYS);
				vboxEditor = new VBox();
				vboxEditor.getChildren().clear();
				vboxEditor.getChildren().add(editorControl);
				vboxEditor.setStyle("-fx-border-color: LIGHTGREY;-fx-border-width: 1px;");
				vboxEditor.setMinHeight(100);
				vboxEditor.setMaxHeight(Double.MAX_VALUE);
				vboxEditor.setPrefHeight(100);
			}
			
			Region nodeRegion = textArea != null ? textArea : vboxEditor;
			
			ret = new PropertyNode(issue, pclass, nodeRegion) {
				@Override
				public void updateData(boolean save) {
					if (editor != null) {
						editor.updateData(save);
					}
					else {
						if (save) {
							String text = textArea.getText();
							issue.setPropertyString(pclass.getId(), text);
						}
						else {
							String text = issue.getPropertyString(pclass.getId(), "");
							textArea.setText(text);
						}
					}
				}
			};
			
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to create text area for property=" + pclass);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeTextAreaForProperty");
		return ret;
	}

	private PropertyNode makeChoiceBoxForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		ComboBox<IdName> ctrl = new ComboBox<IdName>();
		PropertyNode propNode = new PropertyNode(issue, pclass, ctrl) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					IdName idn = ctrl.getSelectionModel().getSelectedItem();
					issue.setPropertyIdName(pclass.getId(), idn);
				}
				else {
					List<IdName> selectList = pclass.getSelectList();
					ctrl.setItems(FXCollections.observableArrayList(selectList));
					Object value = issue.getPropertyValue(pclass.getId(), pclass.getDefaultValue());
					for (IdName item : selectList) {
						if (item.equals(value) || item.getId().equals(value)) {
							ctrl.setValue(item);
							break;
						}
					}
				}
			}
		};
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeChoiceBoxForProperty");
		return propNode;
	}

	@SuppressWarnings("rawtypes")
	private PropertyNode makeChoiceBoxForPropertyArray(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeChoiceBoxForProperty(");
		List<IdName> selectList = pclass.getSelectList();
		CheckComboBox<IdName> ctrl = new CheckComboBox<IdName>(FXCollections.observableArrayList(selectList));
		PropertyNode propNode = new PropertyNode(issue, pclass, ctrl) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					List<IdName> idns = new ArrayList<IdName>(ctrl.getCheckModel().getCheckedItems());
					Object value = null;
					switch(pclass.getType()) {
					case PropertyClass.TYPE_STRING: 
						value = idns.stream().map((idn) -> idn.getId()).collect(Collectors.toList());
						break;
					case PropertyClass.TYPE_ID_NAME:
						value = idns;
						break;
					}
					if (!pclass.isArray()) {
						if (value != null) {
							List list = (List)value;
							if (!list.isEmpty()) {
								value = list.get(0);
							}
						}
					}
					issue.setPropertyValue(pclass.getId(), value);
				}
				else {
					Object propValue = issue.getPropertyValue(pclass.getId(), null);
					if (propValue != null) {
						for (IdName idn : selectList) {
							boolean checked = false;
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
							
							if (checked) {
								ctrl.getCheckModel().check(idn);
							}
						}
					}
				}
			}
		};
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeChoiceBoxForProperty");
		return propNode;
	}

	private PropertyNode makeAutoCompletionNode(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeAutoCompletionNode(" + pclass.getId());
		String recentCaption = resb.getString("autocomplete.recentCaption");
		String suggestionsCaption = resb.getString("autocomplete.suggestionsCaption");
		List<IdName> recentItems = pclass.getSelectList();
		if (recentItems == null) recentItems = new ArrayList<IdName>();
		ExtractImage<IdName> extractImage = (item) -> item.getImage();

		AutoCompletionComboBox<IdName> comboBox = AutoCompletions.createAutoCompletionNode(extractImage, recentCaption,
				suggestionsCaption, recentItems, pclass.getAutoCompletionSuggest());

		PropertyNode propNode = new PropertyNode(issue, pclass, comboBox) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					IdName idn = comboBox.getBinding().getSelectedItem();
					issue.setPropertyValue(pclass.getId(), idn);
				}
				else {
					IdName item = (IdName)issue.getPropertyValue(pclass.getId(), null);
					if (item != null) {
						comboBox.getBinding().select(item);
					}
				}
			}
		};

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeAutoCompletionNode");
		return propNode;
	}

	private PropertyNode makeCheckBoxForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeCheckBoxForProperty(");
		CheckBox ctrl = new CheckBox();
		PropertyNode propNode = new PropertyNode(issue, pclass, ctrl) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					boolean value = ctrl.isSelected();
					issue.setPropertyBoolean(pclass.getId(), value);
				}
				else {
					boolean value = issue.getPropertyBoolean(pclass.getId(), Boolean.FALSE);
					ctrl.setSelected(value);
				}
			}
		};
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeCheckBoxForProperty");
		return propNode;
	}

	private PropertyNode makeDatePickerForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeDatePickerForProperty(");
		DatePicker ctrl = new DatePicker();
		PropertyNode propNode = new PropertyNode(issue, pclass, ctrl) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					LocalDate ldate = ctrl.getValue();
					String iso = null;
					if (ldate != null) {
						iso = ldate.format(DateTimeFormatter.ISO_LOCAL_DATE);
					}
					issue.setPropertyString(pclass.getId(), iso);
				}
				else {
					String iso = issue.getPropertyString(pclass.getId(), "");
					if (iso != null && iso.length() != 0) {
						if (iso.length() > 10) {
							iso = iso.substring(0, 10);
						}
						LocalDate ldate = LocalDate.parse(iso);
						ctrl.setValue(ldate);
					}
				}
			}
		};
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeDatePickerForProperty");
		return propNode;
	}

	private PropertyNode makeDateTimePickerForProperty(Issue issue, PropertyClass pclass) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeDateTimePickerForProperty(");
		DateTimePicker ctrl = new DateTimePicker();
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		PropertyNode propNode = new PropertyNode(issue, pclass, ctrl) {
			@Override
			public void updateData(boolean save) {
				if (save) {
					LocalDateTime ldate = ctrl.getDateTimeValue();
					String iso = null;
					if (ldate != null) {
						Date date = Date.from(ldate.atZone(ZoneId.systemDefault()).toInstant());
						iso = dateTimeFormat.format(date);
					}
					issue.setPropertyString(pclass.getId(), iso);
				}
				else {
					String iso = issue.getPropertyString(pclass.getId(), "");
					if (iso != null && iso.length() != 0) {
						try {
							Date date = dateTimeFormat.parse(iso);
							LocalDateTime ldate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
							ctrl.setDateTimeValue(ldate);
						} catch (ParseException e) {
							log.log(Level.WARNING, "Failed to parse ISO date provided by JIRA for property " + pclass.getId() + "=" + iso, e);
						}
					}
				}
			}
		};
		
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeDateTimePickerForProperty");
		return propNode;
	}

	private PropertyNode makeIssuePropertyEditor(Issue issue, PropertyClass pclass) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "makeIssuePropertyEditor(");
		PropertyNode propNode = null;
		final IssuePropertyEditor propEdit = Globals.getIssueService().getPropertyEditor(issueTaskPane, issue, pclass.getId());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "propEdit=" + propEdit);
		if (propEdit != null) {

			Node control = propEdit.getNode();
			if (pclass.getType() == PropertyClass.TYPE_TEXT) {
				VBox.setVgrow(control, Priority.ALWAYS);
				VBox vbox = new VBox();
				vbox.setMinHeight(100);
				vbox.setMaxHeight(Double.MAX_VALUE);
				vbox.setPrefHeight(100);
				vbox.getChildren().clear();
				vbox.getChildren().add(control);
				vbox.setStyle("-fx-border-color: LIGHTGREY;-fx-border-width: 1px;");
				control = vbox;
			}
			
			propNode = new PropertyNode(issue, pclass, control) {
				public void updateData(boolean save) {
					propEdit.updateData(save);
				}
			};
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")makeIssuePropertyEditor");
		return propNode;
	}

	public Node getFirstControl() {
		return firstControl;
	}

}
