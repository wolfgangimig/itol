package com.wilutions.itol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.wilutions.fx.acpl.AutoCompletionBinding;
import com.wilutions.fx.acpl.AutoCompletions;
import com.wilutions.fx.acpl.ExtractImage;
import com.wilutions.itol.db.AttachmentBlacklistItem;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.MailBodyConversion;
import com.wilutions.itol.db.MsgFileFormat;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

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
	@FXML
	Button bnRemoveFromBlacklist;
	@FXML
	TableView<AttachmentBlacklistItem> tvBlacklist;
	@FXML
	TableColumn<AttachmentBlacklistItem, String> colBlacklistName;
	@FXML
	TableColumn<AttachmentBlacklistItem, Long> colBlacklistSize;
	@FXML
	TableColumn<AttachmentBlacklistItem, String> colBlacklistHash;
	@FXML
	ComboBox<String> cbExportAttachmentsProgram;
	@FXML
	TextField edServiceNotificationMailAddress;

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
			
			tvBlacklist.setPlaceholder(new Label(""));
			tvBlacklist.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			colBlacklistName.setCellValueFactory(new PropertyValueFactory<AttachmentBlacklistItem, String>("name"));

			colBlacklistHash.setCellValueFactory(new PropertyValueFactory<AttachmentBlacklistItem, String>("hash"));
			colBlacklistHash.setVisible(false);

			colBlacklistSize.setCellValueFactory(new PropertyValueFactory<AttachmentBlacklistItem, Long>("size"));
			colBlacklistSize.setCellFactory(new Callback<TableColumn<AttachmentBlacklistItem, Long>, TableCell<AttachmentBlacklistItem, Long>>() {

				@Override
				public TableCell<AttachmentBlacklistItem, Long> call(TableColumn<AttachmentBlacklistItem, Long> item) {
					TableCell<AttachmentBlacklistItem, Long> cell = new TableCell<AttachmentBlacklistItem, Long>() {
						@Override
						protected void updateItem(Long contentLength, boolean empty) {
							super.updateItem(contentLength, empty);
							if (contentLength != null) {
								String str = MailAttachmentHelper.makeAttachmentSizeString(contentLength);
								setText(str);
							}
						}
					};
					cell.setStyle("-fx-alignment: CENTER-RIGHT;");
					return cell;
				}

			});

			colBlacklistSize.setPrefWidth(100);
			colBlacklistName.prefWidthProperty().bind(tvBlacklist.widthProperty().subtract(100 + 20));

			scene = new Scene(p);
			//scene.getStylesheets().add(getClass().getResource("TaskPane.css").toExternalForm());

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

		cbExportAttachmentsProgram.getItems().addAll(Config.EXPORT_PROROGRAM_EXPLORER, Config.EXPORT_PROGRAM_CMD, Config.EXPORT_PROGRAM_TOTALCMD);
				
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
			config.setServiceNotifcationMailAddress(edServiceNotificationMailAddress.getText().trim());
			
			String mailBodyConversionId = cbMailBody.getSelectionModel().getSelectedItem().getId();
			config.setMailBodyConversion(MailBodyConversion.valueOf(mailBodyConversionId));

			ObservableList<AttachmentBlacklistItem> blacklistItems = tvBlacklist.getItems();
			config.setBlacklist(blacklistItems);
			
			config.setExportAttachmentsProgram(cbExportAttachmentsProgram.getEditor().getText());
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
			edServiceNotificationMailAddress.setText(config.getServiceNotifcationMailAddress());
			
			ObservableList<AttachmentBlacklistItem> blacklistItems = FXCollections.observableArrayList(config.getBlacklist());
			tvBlacklist.setItems(blacklistItems);
			
			cbExportAttachmentsProgram.getEditor().setText(config.getExportAttachmentsProgram());
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
	
	@FXML
	public void onRemoveFromBlacklist() {
		ArrayList<AttachmentBlacklistItem> allItems = new ArrayList<>(tvBlacklist.getItems());
		for (Integer index : tvBlacklist.getSelectionModel().getSelectedIndices()) {
			allItems.set(index, null);
		}
		ArrayList<AttachmentBlacklistItem> newItems = new ArrayList<>();
		for (int i = 0; i < allItems.size(); i++) {
			if (allItems.get(i) != null) newItems.add(allItems.get(i));
		}

		tvBlacklist.getItems().clear();
		tvBlacklist.getItems().addAll(newItems);	
		tvBlacklist.refresh();
	}
}
