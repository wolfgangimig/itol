package com.wilutions.itol;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.Profile;
import com.wilutions.joa.fx.ModalDialogFX;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;

public class DlgProfiles extends ModalDialogFX<Config> implements Initializable {

	private static Logger log = Logger.getLogger("DlgProfiles");
	private Config config;
	private ObservableList<Profile> profiles;
	private ResourceBundle resb;
	private Scene scene;

	@FXML
	Button bnNew;
	@FXML
	Button bnEdit;
	@FXML
	Button bnConfigure;
	@FXML
	Button bnDelete;
	@FXML
	Button bnCancel;
	@FXML
	TableView<Profile> tviewProfiles;
	@FXML
	TableColumn<Profile, String> tcolType;
	@FXML
	TableColumn<Profile, String> tcolName;
	@FXML
	TableColumn<Profile, String> tcolUserName;
	@FXML
	TableColumn<Profile, Boolean> tcolStatus;

	public DlgProfiles() {
		this.resb = Globals.getResourceBundle();
		this.config = (Config) Globals.getAppInfo().getConfig();
		this.profiles = FXCollections.observableArrayList(config.getProfiles());
		setTitle(resb.getString("DlgProfiles.Caption"));
	}

	@Override
	public Scene createScene() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			URL fxmlURL = classLoader.getResource("com/wilutions/itol/DlgProfiles.fxml");
			FXMLLoader loader = new FXMLLoader(fxmlURL, resb, new JavaFXBuilderFactory(), (clazz) -> {
				return this;
			});
			Parent p = loader.load();

			tviewProfiles.setOnKeyReleased((e) -> {
				if (e.getCode() == KeyCode.ESCAPE) {
					DlgProfiles.this.close();
				}
			});
			
			Hyperlink emptyTableNode = new Hyperlink(resb.getString("DlgProfiles.emptyTable"));
			emptyTableNode.setOnAction((e) -> onNew());
			tviewProfiles.setPlaceholder(emptyTableNode);
			
			bnEdit.disableProperty().bind(Bindings.isNull(tviewProfiles.getSelectionModel().selectedItemProperty()));
			bnDelete.disableProperty().bind(Bindings.isNull(tviewProfiles.getSelectionModel().selectedItemProperty()));
			bnConfigure.disableProperty().bind(Bindings.isNull(tviewProfiles.getSelectionModel().selectedItemProperty()));
			

			scene = new Scene(p);
			return scene;
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	@FXML
	public void onOK() {
		updateData(true);
		config.setProfiles(profiles);
		setResult(config);
	}

	@FXML
	public void onNew() {
		Profile profile = new Profile();
		DlgConnect dlg = new DlgConnect(profile);
		dlg.showAsync(this, (acceptedProfile, ex) -> {
			if (ex != null) {
				ResourceBundle resb = Globals.getResourceBundle();
				String msg = resb.getString("Error.NotConnected");
				log.log(Level.SEVERE, msg, ex);
				msg += "\n" + ex.getMessage();
				showMessageBoxError(msg);
			} else if (acceptedProfile != null) {
				
				// Add profile and save config.
				profiles.add(acceptedProfile);
				updateData(true);
				
				// Refresh table view.
				tviewProfiles.refresh();
				tviewProfiles.getSelectionModel().select(profiles.size() - 1);
			}
		});
	}

	@FXML
	public void onEdit() {
		int selectedIndex = tviewProfiles.getSelectionModel().getSelectedIndex();
		if (selectedIndex < 0)
			return;

		Profile profile = profiles.get(selectedIndex);
		DlgConnect dlg = new DlgConnect(profile);
		dlg.showAsync(this, (acceptedProfile, ex) -> {
			if (ex != null) {
				ResourceBundle resb = Globals.getResourceBundle();
				String msg = resb.getString("Error.NotConnected");
				log.log(Level.SEVERE, msg, ex);
				msg += "\n" + ex.getMessage();
				showMessageBoxError(msg);
			} else if (acceptedProfile != null) {
				
				// Replace new profile in list and save config.
				profiles.set(selectedIndex, acceptedProfile);
				updateData(true);
				
				// Update table view.
				tviewProfiles.refresh();
				tviewProfiles.getSelectionModel().select(selectedIndex);
			}
		});
	}

	@FXML
	public void onConfigure() {
		Profile selectedProfile = tviewProfiles.getSelectionModel().getSelectedItem();
		DlgConfigure dlg = new DlgConfigure(selectedProfile);
		dlg.showAsync(this, (updatedProfile, ex) -> {
			if (updatedProfile != null) {
				
				// Hand over updated profile to issue service.
				if (updatedProfile.getIssueService() != null) {
					updatedProfile.getIssueService().setProfile(updatedProfile);
				}
			}
		});
	}

	private void showMessageBoxError(String text) {
		String title = resb.getString("MessageBox.title.error");
		String ok = resb.getString("Button.OK");
		com.wilutions.joa.fx.MessageBox.create(this).title(title).text(text).button(1, ok).bdefault()
				.show((btn, ex) -> {
				});
	}

	@FXML
	public void onDelete() {
		int index = tviewProfiles.getSelectionModel().getSelectedIndex();
		if (index < 0) return;
		
		Profile profile = profiles.get(index);
		String title = resb.getString("MessageBox.title.confirm");
		String text = MessageFormat.format(resb.getString("DlgProfiles.queryDelete.text"), profile.getProfileName());
		String yes = resb.getString("Button.Yes");
		String no = resb.getString("Button.No");
		Object owner = this;
		com.wilutions.joa.fx.MessageBox.create(owner).title(title).text(text).button(1, yes).button(0, no).bdefault()
				.show((btn, ex) -> {
					boolean succ = btn != null && btn != 0;
					if (succ) {
						profiles.remove(index);
						updateData(true);
						if (index < profiles.size()) {
							tviewProfiles.refresh();
							tviewProfiles.getSelectionModel().select(index);
						}
					}
				});
	}

	@FXML
	public void onCancel() {
		this.close();
	}

	@Override
	public void initialize(URL location, ResourceBundle resb) {

		tcolType.setCellValueFactory(new PropertyValueFactory<Profile, String>("serviceFactoryClass"));
		tcolType.setCellFactory(new Callback<TableColumn<Profile, String>, TableCell<Profile, String>>() {

			@Override
			public TableCell<Profile, String> call(TableColumn<Profile, String> item) {

				TableCell<Profile, String> cell = new TableCell<Profile, String>() {
					@Override
					protected void updateItem(String serviceFactoryClass, boolean empty) {
						super.updateItem(serviceFactoryClass, empty);
						if (serviceFactoryClass != null) {
							if (serviceFactoryClass.equals(Profile.JIRA_SERVICE_CLASS)) {
								setText("JIRA");
							} else {
								setText("Unknown Service Class");
							}
						}
					}
				};

				return cell;
			}

		});

		tcolStatus.setCellValueFactory(new PropertyValueFactory<Profile, Boolean>("Connected"));
		tcolStatus.setCellFactory(new Callback<TableColumn<Profile, Boolean>, TableCell<Profile, Boolean>>() {

			@Override
			public TableCell<Profile, Boolean> call(TableColumn<Profile, Boolean> item) {

				TableCell<Profile, Boolean> cell = new TableCell<Profile, Boolean>() {
					@Override
					protected void updateItem(Boolean connected, boolean empty) {
						super.updateItem(connected, empty);
						if (connected != null) {
							String text = resb.getString(connected ? "Button.Yes" : "Button.No");
							setText(text);
						}
					}
				};

				return cell;
			}

		});
		
		tcolName.setCellValueFactory(new PropertyValueFactory<Profile, String>("ProfileName"));
		tcolUserName.setCellValueFactory(new PropertyValueFactory<Profile, String>("UserName"));

		tviewProfiles.setItems(profiles);

		updateData(false);
	}

	private void updateData(boolean save) {
		if (save) {
			config.setProfiles(new ArrayList<Profile>(profiles));
			config.write();
		} else {
			Profile profile = config.getCurrentProfile();
			if (!profile.isNew()) {
				tviewProfiles.getSelectionModel().select(profile);
			}
		}
	}

}
