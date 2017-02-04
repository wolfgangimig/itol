package com.wilutions.itol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import com.wilutions.fx.acpl.AutoCompletionBinding;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.MsgFileFormat;
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
import javafx.stage.DirectoryChooser;

public class DlgConfigure extends ModalDialogFX<Boolean> implements Initializable {

	private Config config;
	private ResourceBundle resb;
	private Scene scene;

	@FXML
	Button bnOK;
	@FXML
	Button bnCancel;
	@FXML
	ComboBox<IdName> cbAttachMailAs;
	@FXML
	TextField edExportAttachmentsDirectory;
	@FXML
	TextField edLogFile;
	@FXML
	ChoiceBox<IdName> cbLogLevel;
	@FXML
	CheckBox ckInsertIssueId;

	List<IdName> msgFileFormats = Arrays.asList(MsgFileFormat.NOTHING, MsgFileFormat.TEXT, MsgFileFormat.MSG,
			MsgFileFormat.MHTML, MsgFileFormat.RTF);
	private AutoCompletionBinding<IdName> autoCompletionAttachMailAs;

	public DlgConfigure() {
		this.resb = Globals.getResourceBundle();
		this.config = (Config)Globals.getAppInfo().getConfig().clone();
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
		try {
			Globals.getAppInfo().setConfig(config);
		} catch (Exception e) {
			String msg = e.getMessage();
			String textf = resb.getString("msg.connection.error");
			String text = MessageFormat.format(textf, msg);
			MessageBox.error(this, text, (ignored, ex) -> {
			});

			// Something wrong in the configuration. Maybe the  
			// connection options do not fit anymore.    
			// I close the dialog to give the user the chance to 
			// adopt them.
		}
		finally {
			close();
		}
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
		MsgFileFormat.NOTHING.setName(resb.getString("DlgConfigure.AttachMailAs.nothing"));
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
				suggestionsCaption, null, msgFileFormats);
	}

	private void updateData(boolean save) {
		if (save) {
			config.setLogFile(edLogFile.getText());
			config.setLogLevel(cbLogLevel.getSelectionModel().getSelectedItem().getId());
			config.setMsgFileFormat(autoCompletionAttachMailAs.getSelectedItem());
			config.setInjectIssueIdIntoMailSubject(ckInsertIssueId.isSelected());
			
			File dir = new File(edExportAttachmentsDirectory.getText());
			dir.mkdirs();
			if (dir.exists() && dir.isDirectory()) {
				config.setExportAttachmentsDirectory(dir.getAbsolutePath());
			}
			else {
				// Causes Outlook to crash:
				// MessageBox.error(this, "Invalid directory " + dir, (succ, ex) -> {});
				// This is most likely a JOA bug.
			}
		}
		else {
			edLogFile.setText(config.getLogFile());
			cbLogLevel.getSelectionModel().select(new IdName(config.getLogLevel(), ""));

			String fileTypeId = config.getMsgFileFormat().getId();	
			if (fileTypeId == null || fileTypeId.isEmpty()) fileTypeId = MsgFileFormat.DEFAULT.getId();
			for (IdName item : msgFileFormats) {
				if (item.getId().equals(fileTypeId)) {
					autoCompletionAttachMailAs.select(item);
					break;
				}
			}
			
			ckInsertIssueId.setSelected(config.getInjectIssueIdIntoMailSubject());
			edExportAttachmentsDirectory.setText(config.getExportAttachmentsDirectory());
		}
	}

	@FXML
	public void onChooseExportAttachmentsDirectory() {
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resb.getString("bnAddAttachment.menu.fileChooser"));
		String dir = edExportAttachmentsDirectory.getText();
		if (!dir.isEmpty()) {
			File fdir = new File(dir);
			while (!fdir.exists()) fdir = fdir.getParentFile();
			directoryChooser.setInitialDirectory(fdir);
		}
        final File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            edExportAttachmentsDirectory.setText(selectedDirectory.getAbsolutePath());
        }
	}
}
