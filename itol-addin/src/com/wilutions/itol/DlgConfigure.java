package com.wilutions.itol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.wilutions.fx.acpl.AutoCompletionBinding;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.MailBodyConversion;
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
	TextField edAutoReplyField;
	@FXML
	TextField edExportAttachmentsDirectory;
	@FXML
	TextField edLogFile;
	@FXML
	ChoiceBox<IdName> cbLogLevel;
	@FXML
	CheckBox ckInsertIssueId;
	@FXML
	ChoiceBox<IdName> cbMailBody;

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
			scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

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
			File dir = new File(config.getExportAttachmentsDirectory());
			dir.mkdirs();
			if (!dir.exists() || !dir.isDirectory()) {
				String textf = resb.getString("msg.config.invalidExportDir");
				String text = MessageFormat.format(textf, dir.getAbsolutePath());
				throw new FileNotFoundException(text);
			}
			
			Globals.getAppInfo().setConfig(config);
			
			finish(true);
			
		} catch (Exception e) {
			
			String msg = e.getMessage();
			String textf = resb.getString("msg.config.error");
			String text = MessageFormat.format(textf, msg);
			MessageBox.error(this, text, (ignored, ex) -> {
			});
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
		
		cbMailBody.getItems().add(new IdName(MailBodyConversion.MARKUP.toString(), resb.getString("DlgConfigure.MailBody.markup")));
		cbMailBody.getItems().add(new IdName(MailBodyConversion.TEXT.toString(), resb.getString("DlgConfigure.MailBody.text")));
		cbMailBody.getSelectionModel().select(0);

		updateData(false);
	}

	private void initAutoCompletionAttachMailAs(ResourceBundle resb) {
		MsgFileFormat.NOTHING.setName(resb.getString("DlgConfigure.AttachMailAs.nothing"));
		MsgFileFormat.ONLY_ATTACHMENTS.setName(resb.getString("DlgConfigure.AttachMailAs.onlyAttachments"));
		MsgFileFormat.ONLY_ATTACHMENTS.setImage(Resources.getInstance().getAttachmentImage());
		String recentCaption = resb.getString("autocomplete.recentCaption");
		String suggestionsCaption = resb.getString("autocomplete.suggestionsCaption");
		ExtractImage<IdName> extractImage = new ExtractImage<IdName>() {
			public Image getImage(IdName item) {
				Image ret = item.getImage();
				if (ret == null && !item.getId().isEmpty()) {
					ret = FileIconCache.getFileIcon(item.getId());
				}
				return ret;
			}
		};
		autoCompletionAttachMailAs = AutoCompletions.bindAutoCompletion(extractImage, cbAttachMailAs, recentCaption,
				suggestionsCaption, null, MsgFileFormat.FORMATS);
	}

	private void updateData(boolean save) {
		if (save) {
			config.setLogFile(edLogFile.getText());
			config.setLogLevel(cbLogLevel.getSelectionModel().getSelectedItem().getId());
			config.setMsgFileFormat(autoCompletionAttachMailAs.getSelectedItem());
			config.setInjectIssueIdIntoMailSubject(ckInsertIssueId.isSelected());
			config.setExportAttachmentsDirectory(edExportAttachmentsDirectory.getText());
			config.setAutoReplyField(edAutoReplyField.getText());
			
			String mailBodyConversionId = cbMailBody.getSelectionModel().getSelectedItem().getId();
			config.setMailBodyConversion(MailBodyConversion.valueOf(mailBodyConversionId));
		}
		else {
			edLogFile.setText(config.getLogFile());
			cbLogLevel.getSelectionModel().select(new IdName(config.getLogLevel(), ""));

			String fileTypeId = config.getMsgFileFormat().getId();	
			for (IdName item : MsgFileFormat.FORMATS) {
				if (item.getId().equals(fileTypeId)) {
					autoCompletionAttachMailAs.select(item);
					break;
				}
			}

			IdName mailBodyConversionItem = new IdName(config.getMailBodyConversion().toString(), "");
			cbMailBody.getSelectionModel().select(mailBodyConversionItem);
			
			ckInsertIssueId.setSelected(config.getInjectIssueIdIntoMailSubject());
			edExportAttachmentsDirectory.setText(config.getExportAttachmentsDirectory());
			edAutoReplyField.setText(config.getAutoReplyField());
		}
	}

	@FXML
	public void onChooseExportAttachmentsDirectory() {
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resb.getString("bnAddAttachment.menu.fileChooser"));
		String dir = edExportAttachmentsDirectory.getText();
		if (!dir.isEmpty()) {
			try {
				File fdir = new File(dir);
				while (fdir != null && !fdir.exists()) fdir = fdir.getParentFile();
				if (fdir != null) {
					directoryChooser.setInitialDirectory(fdir);
				}
			}
			catch (Exception ignored) {
				// invalid directory
			}
		}
        final File selectedDirectory = directoryChooser.showDialog(scene.getWindow());
        if (selectedDirectory != null) {
            edExportAttachmentsDirectory.setText(selectedDirectory.getAbsolutePath());
        }
	}
}
