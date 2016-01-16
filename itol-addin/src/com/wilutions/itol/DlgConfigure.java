package com.wilutions.itol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import com.wilutions.fx.acpl.AutoCompletionBinding;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Property;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

public class DlgConfigure extends ModalDialogFX<Boolean> implements Initializable {

	private List<Property> configProps;
	private ResourceBundle resb;
	private Scene scene;

	@FXML
	Button bnOK;
	@FXML
	Button bnCancel;
	@FXML
	ComboBox<IdName> cbAttachMailAs;
	@FXML
	TextField edLogFile;
	@FXML
	ChoiceBox<IdName> cbLogLevel;
	@FXML
	CheckBox ckInsertIssueId;

	List<IdName> msgFileTypes = Arrays.asList(MsgFileTypes.NOTHING, MsgFileTypes.TEXT, MsgFileTypes.MSG,
			MsgFileTypes.MHTML, MsgFileTypes.RTF);
	private AutoCompletionBinding<IdName> autoCompletionAttachMailAs;

	public DlgConfigure() {
		this.resb = Globals.getResourceBundle();
		this.configProps = Globals.getAppInfo().getConfigProps();
		setTitle(resb.getString("DlgConfigure.Caption"));
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgConfigure.fxml");
			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			});
			Parent p = loader.load();

			scene = new Scene(p);
			return scene;
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	@FXML
	public void onOK() {
		updateData(true);
		Globals.writeData();
		Globals.initLogging();
		close();
	}

	@FXML
	public void onCancel() {
		close();
	}

	@Override
	public void initialize(URL location, ResourceBundle resb) {

		initAutoCompletionAttachMailAs(resb);

		cbLogLevel.getItems().add(new IdName("INFO", resb.getString("DlgConnect.LogLevel.Info")));
		cbLogLevel.getItems().add(new IdName("FINE", resb.getString("DlgConnect.LogLevel.Debug")));
		cbLogLevel.getSelectionModel().select(0);

		updateData(false);
	}

	private void initAutoCompletionAttachMailAs(ResourceBundle resb) {
		MsgFileTypes.NOTHING.setName(resb.getString("DlgConfigure.AttachMailAs.nothing"));
		String recentCaption = resb.getString("autocomplete.recentCaption");
		String suggestionsCaption = resb.getString("autocomplete.suggestionsCaption");
		ExtractImage<IdName> extractImage = new ExtractImage<IdName>() {
			public Image getImage(IdName item) {
				Image ret = null;
				if (item.getId().length() > 1) {
					ret = FileIconCache.getFileIcon(item.getId());
				}
				return ret;
			}
		};
		autoCompletionAttachMailAs = AutoCompletions.bindAutoCompletion(extractImage, cbAttachMailAs, recentCaption,
				suggestionsCaption, null, msgFileTypes);
	}

	private void updateData(boolean save) {
		if (save) {
			setConfigProperty(Property.LOG_FILE, edLogFile.getText());
			setConfigProperty(Property.LOG_LEVEL, cbLogLevel.getSelectionModel().getSelectedItem().getId());

			IdName fileType = autoCompletionAttachMailAs.getSelectedItem();
			String fileTypeId = fileType.getId();
			setConfigProperty(Property.MSG_FILE_TYPE, fileTypeId);
			
			boolean injectIssueId = ckInsertIssueId.isSelected();
			setConfigProperty(Property.INJECT_ISSUE_ID_INTO_MAIL_SUBJECT, Boolean.toString(injectIssueId));
		}
		else {
			String logFile = getConfigProperty(Property.LOG_FILE);
			if (logFile.isEmpty())
				logFile = (new File(System.getProperty("java.io.tmpdir"), "itol.log")).getAbsolutePath();
			edLogFile.setText(logFile);

			cbLogLevel.getSelectionModel().select(new IdName(getConfigProperty(Property.LOG_LEVEL), ""));

			String fileTypeId = getConfigProperty(Property.MSG_FILE_TYPE);
			for (IdName item : msgFileTypes) {
				if (item.getId().equals(fileTypeId)) {
					autoCompletionAttachMailAs.select(item);
					break;
				}
			}
			
			String insertIssueIdStr = getConfigProperty(Property.INJECT_ISSUE_ID_INTO_MAIL_SUBJECT);
			Boolean insertIssueId = (insertIssueIdStr.isEmpty()) ? Boolean.FALSE : Boolean.valueOf(insertIssueIdStr);
			ckInsertIssueId.setSelected(insertIssueId);
		}
	}

	private String getConfigProperty(String propId) {
		Property ret = null;
		for (Property prop : configProps) {
			if (prop.getId().equals(propId)) {
				ret = prop;
				break;
			}
		}
		return ret != null ? (String) ret.getValue() : "";
	}

	private void setConfigProperty(String propId, String propValue) {
		boolean found = false;
		for (Iterator<Property> it = configProps.iterator(); it.hasNext();) {
			Property prop = it.next();
			if (prop.getId().equals(propId)) {
				prop.setValue(propValue);
				found = true;
				break;
			}
		}
		if (!found) {
			configProps.add(new Property(propId, propValue));
		}
	}

}
