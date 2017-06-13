package com.wilutions.itol;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.reg.RegUtil;
import com.wilutions.itol.db.Config;
import com.wilutions.itol.db.Default;

import de.wim.liccheck.License;
import de.wim.liccheck.LicenseCheck;

/**
 * Install and uninstall license key into registry.
 * Installs the key for DDAddin too.
 */
public class LicenseInstall {

	public final static String DEMO_KEY = "DEMO";
	private final static String MANUFACTURER = "WILUTIONS";
	private static Logger log = Logger.getLogger("License");
	public String product;
	public Config config;
	
//	public LicenseInstall(String product) {
//		this.product = product;
//	}
	
	public LicenseInstall(Config config) {
		this.product = config.getAppName();
		this.config = config;
	}

	public boolean install(String licenseKey, boolean userNotMachine) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("install(" + licenseKey + ", userNotMachine=" + userNotMachine);
		if (log.isLoggable(Level.INFO)) log.info("Install license=" + licenseKey);
		License lic = check(licenseKey, LicenseCheck.Mode.Install);
		if (log.isLoggable(Level.FINE)) log.fine("isValid=" + lic.isValid() + ", isDemo=" + lic.isDemo());
		if (lic.isValid() && !lic.isDemo()) {
			if (config != null) {
				config.setLicenseKey(licenseKey);
				config.write();
			}
			else {
				if (log.isLoggable(Level.FINE)) log.fine("lic.options=" + lic.getOptions());
				String registryKey = getProductRegKey(userNotMachine);
				RegUtil.setRegistryValue(registryKey, "", licenseKey);
				if (log.isLoggable(Level.INFO)) log.info("Saved license into registry-key=" + registryKey);
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(")install");
		return lic.isValid();
	}
	
	public void uninstall(boolean userNotMachine) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("uninstall(userNotMachine=" + userNotMachine);
		if (config != null) {
			config.setLicenseKey("");
			config.write();
		}
		else {
			String registryKey = getProductRegKey(userNotMachine);
			if (log.isLoggable(Level.FINE)) log.fine("regkey=" + registryKey);
			String licenseKey = (String)RegUtil.getRegistryValue(registryKey, "", "");
			if (log.isLoggable(Level.FINE)) log.fine("licenseKey=" + licenseKey);
			if (!licenseKey.isEmpty()) {
				if (log.isLoggable(Level.INFO)) log.info("Uninstall license=" + licenseKey + " found at registry-key=" + registryKey);
				check(licenseKey, LicenseCheck.Mode.Uninstall);
				RegUtil.deleteRegistryKey(registryKey);
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(")uninstall");
	}
	
	private License check(String licenseKey, LicenseCheck.Mode mode) throws Exception {
		LicenseCheck licenseCheck = new LicenseCheck(product);
		License license = licenseCheck.check(licenseKey, mode);
		if (license.isValid() && !license.isDemo()) {
			boolean thisProgram = license.getOptions() == License.OPTION_ITOL;
			if (!thisProgram) {
				log.log(Level.SEVERE, "Given license key belongs to other product.");
				license.setValid(false);
			}
		}
		return license;
	}
	
	private static String getProductRegKey(boolean userNotMachine) {
		String product = Globals.getAppInfo().getAppName();
		String key = userNotMachine ? "HKCU" : "HKLM";
		key += "\\SOFTWARE\\" + MANUFACTURER + "\\" + product + "\\License";
		return key;
	}

	public License getInstalledLicense() {
		if (log.isLoggable(Level.FINE)) log.fine("getInstalledLicense(");
		String licenseKey = "";
		if (config != null) {
			licenseKey = config.getLicenseKey();
		}
		else {
			String registryKey = getProductRegKey(false);
			licenseKey = (String)RegUtil.getRegistryValue(registryKey, "", "");
			if (log.isLoggable(Level.FINE)) log.fine("Read license from machine settings, registry-key=" + registryKey + ", license=" + licenseKey);
			if (licenseKey.isEmpty()) {
				registryKey = getProductRegKey(true);
				licenseKey = (String)RegUtil.getRegistryValue(registryKey, "", "");
				if (log.isLoggable(Level.FINE)) log.fine("Read license from user settings, registry-key=" + registryKey + ", license=" + licenseKey);
			}
		}
		License lic = null;
		try {
			lic = check(licenseKey, LicenseCheck.Mode.Check);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot get installed license.", e);
			lic = new License(); // invalid
		}
		if (log.isLoggable(Level.FINE)) log.fine(")getInstalledLicense=" + lic);
		return lic;
	}
}
