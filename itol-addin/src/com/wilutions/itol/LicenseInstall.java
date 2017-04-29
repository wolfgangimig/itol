package com.wilutions.itol;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.reg.RegUtil;

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
	
	public LicenseInstall(String product) {
		this.product = product;
	}

	public boolean install(String licenseKey, boolean userNotMachine) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("install(" + licenseKey + ", userNotMachine=" + userNotMachine);
		LicenseCheck licenseCheck = new LicenseCheck(product);
		if (log.isLoggable(Level.INFO)) log.info("Install license=" + licenseKey);
		License lic = licenseCheck.check(licenseKey, LicenseCheck.Mode.Install);
		if (log.isLoggable(Level.FINE)) log.fine("isValid=" + lic.isValid() + ", isDemo=" + lic.isDemo());
		if (lic.isValid() && !lic.isDemo()) {
			String registryKey = getProductRegKey(userNotMachine);
			RegUtil.setRegistryValue(registryKey, "", licenseKey);
			if (log.isLoggable(Level.INFO)) log.info("Saved license into registry-key=" + registryKey);
		}
		if (log.isLoggable(Level.FINE)) log.fine(")install");
		return lic.isValid();
	}
	
	public void uninstall(boolean userNotMachine) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("uninstall(userNotMachine=" + userNotMachine);
		LicenseCheck licenseCheck = new LicenseCheck(product);
		String registryKey = getProductRegKey(userNotMachine);
		if (log.isLoggable(Level.FINE)) log.fine("regkey=" + registryKey);
		String licenseKey = (String)RegUtil.getRegistryValue(registryKey, "", "");
		if (log.isLoggable(Level.FINE)) log.fine("licenseKey=" + licenseKey);
		if (!licenseKey.isEmpty()) {
			if (log.isLoggable(Level.INFO)) log.info("Uninstall license=" + licenseKey + " found at registry-key=" + registryKey);
			licenseCheck.check(licenseKey, LicenseCheck.Mode.Uninstall);
			RegUtil.deleteRegistryKey(registryKey);
		}
		if (log.isLoggable(Level.FINE)) log.fine(")uninstall");
	}
	
	private static String getProductRegKey(boolean userNotMachine) {
		String product = Globals.getAppInfo().getAppName();
		String key = userNotMachine ? "HKCU" : "HKLM";
		key += "\\SOFTWARE\\" + MANUFACTURER + "\\" + product + "\\License";
		return key;
	}

	public License getInstalledLicense() {
		if (log.isLoggable(Level.FINE)) log.fine("getInstalledLicense(");
		String registryKey = getProductRegKey(false);
		String licenseKey = (String)RegUtil.getRegistryValue(registryKey, "", "");
		if (log.isLoggable(Level.FINE)) log.fine("Read license from machine settings, registry-key=" + registryKey + ", license=" + licenseKey);
		if (licenseKey.isEmpty()) {
			registryKey = getProductRegKey(true);
			licenseKey = (String)RegUtil.getRegistryValue(registryKey, "", "");
			if (log.isLoggable(Level.FINE)) log.fine("Read license from user settings, registry-key=" + registryKey + ", license=" + licenseKey);
		}
		LicenseCheck licenseCheck = new LicenseCheck(product);
		License lic = null;
		try {
			lic = licenseCheck.check(licenseKey, LicenseCheck.Mode.Check);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot get installed license.", e);
			lic = new License(); // invalid
		}
		if (log.isLoggable(Level.FINE)) log.fine(")getInstalledLicense=" + lic);
		return lic;
	}
}
