package com.wilutions.itol;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.BackgTask;
import com.wilutions.itol.db.Default;
import com.wilutions.itol.db.ProgressCallback;

import de.wim.liccheck.License;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class DlgLicense {

	private static Logger log = Logger.getLogger("DlgLicense");

	public static void show(IssueTaskPane taskPane) {
		if (log.isLoggable(Level.FINE)) log.fine("show(");
		
		Stage owner = (Stage)taskPane.getWindow();
		ResourceBundle resb = Globals.getResourceBundle();
		LicenseInstall licenseInstall = new LicenseInstall(Globals.getAppInfo().getConfig());
		License license = licenseInstall.getInstalledLicense();
		String expiresAtIso = license.getExpiresAt();
		String licenseKey = license.toString();
		if (Default.value(licenseKey).isEmpty()) licenseKey = License.CUSTOMER_DEMO;
		if (log.isLoggable(Level.INFO)) log.info("Current license=" + licenseKey + ", expiresAtIso=" + expiresAtIso);

		// Determine which text to be displayed that describes
		// the licensing state: DEMO, INVALID, valid
		String headerText = makeLicenseText(resb, expiresAtIso, licenseKey);
		
		TextInputDialog dialog = new TextInputDialog(licenseKey);
		dialog.initOwner(owner);
		dialog.setTitle(resb.getString("DlgLicense.titel"));
		dialog.setHeaderText(headerText + "                                                                          ");

		// Show dialog
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			
			// Set new license key?
			String licenseKeyNew = result.get().trim();
			if (!licenseKeyNew.equals(licenseKey)) {

				if (log.isLoggable(Level.INFO)) log.info("Install new license key=" + licenseKeyNew);

				ProgressCallback cb = taskPane.createProgressCallback("Install license");
				cb.setFakeProgress(true);
				BackgTask.run(() -> {

					AtomicBoolean succ = new AtomicBoolean();
					AtomicReference<Exception> ex = new AtomicReference<Exception>();
					
					try {
						// Uninstall current license
						licenseInstall.uninstall(true);
						
						// Install new license
						boolean v = licenseInstall.install(licenseKeyNew, true);
						succ.set(v);
						if (v) {
							log.info("Successuflly installed license key=" + licenseKeyNew);
						}
						else {
							log.warning("Wrong license key=" + licenseKeyNew);
						}
					}
					catch (Exception e) {
						log.log(Level.SEVERE, "Failed to install license.", e);
						ex.set(e);
					}
					finally {
						cb.setFinished();
					}

					Platform.runLater(() -> {
						
						// Show information or error dialog.
						if (succ.get()) {
							Alert alert = new Alert(AlertType.INFORMATION);
							alert.setTitle(dialog.getTitle());
							
							License licenseNew = licenseInstall.getInstalledLicense();
							String expiresAtIso3 = licenseNew.getExpiresAt();
							String headerText3 = expiresAtIso3.isEmpty() ? resb.getString("DlgLicense.headerText.valid") : makeTimeLimitText(resb, expiresAtIso3);

							alert.setContentText(headerText3);
							alert.initOwner(owner);
							alert.showAndWait();
						}
						else {
							Alert alert = new Alert(AlertType.ERROR);
							alert.setTitle(dialog.getTitle());
							alert.setContentText(resb.getString("DlgLicense.headerText.wrong"));
							alert.initOwner(owner);
							alert.showAndWait();
						}

						// Set the license in the IssueTaskPane valid/invalid.
						taskPane.setLicenseValid(succ.get());
					});
				});
			}
		}
		
		if (log.isLoggable(Level.FINE)) log.fine(")show");
	}
	
	private static String makeTimeLimitText(ResourceBundle resb, String expiresAtIso) {
		expiresAtIso = expiresAtIso.replace("-", "").replace(" ", "").replaceAll(":", "");
		int year = Integer.parseInt(expiresAtIso.substring(0, 4));
		int month = Integer.parseInt(expiresAtIso.substring(4, 6));
		int day = Integer.parseInt(expiresAtIso.substring(6, 8));
		LocalDate date = LocalDate.of(year, month, day);
		String expiresAtIsoFormatted = date.format(DateTimeFormatter.ISO_DATE);
		String ret = MessageFormat.format(resb.getString("DlgLicense.headerText.demo"), expiresAtIsoFormatted);
		return ret;
	}

	private static String makeLicenseText(ResourceBundle resb, String expiresAtIso, String licenseKey) {
		String headerText = "";
		switch (licenseKey) {
		case "DEMO":
			if (expiresAtIso.length() >= 8) {
				headerText = resb.getString("DlgLicense.headerText") + "\n" + makeTimeLimitText(resb, expiresAtIso);
				break;
			}
			else {
				// fall through
				// INVALID
			}
			
		case "INVALID":
			headerText = resb.getString("DlgLicense.headerText") + "\n" + resb.getString("DlgLicense.headerText.invalid");
			break;
			
		default: // VALID license
			headerText = resb.getString("DlgLicense.headerText.valid");
			break;
		}
		return headerText;
	}
}
