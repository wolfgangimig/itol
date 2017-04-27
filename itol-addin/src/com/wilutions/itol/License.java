package com.wilutions.itol;

import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import com.wilutions.com.DDAddinDll;
import com.wilutions.com.reg.RegUtil;

import byps.BTransportFactory;
import byps.http.HTransportFactoryClient;
import byps.http.HWireClient;
import de.wim.lic.BApiDescriptor_LicApi;
import de.wim.lic.BClient_LicApi;
import de.wim.lic.LicenseInfo;
import de.wim.lic.LicenseResult;

/**
 * Install and uninstall license key into registry.
 * Installs the key for DDAddin too.
 */
public class License {
	
	private final static String MANUFACTURER = "WILUTIONS";
	private static Logger log = Logger.getLogger("License");

	public static boolean install(String licenseKey, boolean userNotMachine) {
		if (log.isLoggable(Level.FINE)) log.fine("install(" + licenseKey + ", userNotMachine=" + userNotMachine);
		boolean succ = DDAddinDll.install(licenseKey, userNotMachine);
		if (succ) {
			String key = getProductRegKey(userNotMachine);
			RegUtil.setRegistryValue(key, "", licenseKey);
		}
		if (log.isLoggable(Level.FINE)) log.fine(")install");
		return succ;
	}
	
	public static void uninstall(boolean userNotMachine) {
		DDAddinDll.uninstall(userNotMachine);
		
		String key = getProductRegKey(userNotMachine);
		RegUtil.deleteRegistryKey(key);
	}
	
	private static String getProductRegKey(boolean userNotMachine) {
		String product = Globals.getAppInfo().getAppName();
		String key = userNotMachine ? "HKCU" : "HKLM";
		key += "\\SOFTWARE\\" + MANUFACTURER + "\\" + product + "\\License";
		return key;
	}
	
	public static String getLicenseTimeLimit() {
		return DDAddinDll.getLicenseTimeLimit();
	}

	public static String getLicenseKey() {
		return DDAddinDll.getLicenseKey();
	}

	public static int getLicenseCount() {
		return DDAddinDll.getLicenseCount();
	}
	
	public static boolean isValid() {
		boolean ret = true;
		String licenseKey = getLicenseKey();
		switch (licenseKey) {
		case "DEMO":
			String expiresAtIso = getLicenseTimeLimit();
			ret = expiresAtIso.length() >= 8;
			break;
		case "INVALID":
			ret = false;
			break;
		default: // VALID license
			ret = true;
			break;
		}
		return ret;
	}
}
