package com.wilutions.itol;

import com.wilutions.com.DDAddinDll;
import com.wilutions.com.reg.RegUtil;

/**
 * Install and uninstall license key into registry.
 * Installs the key for DDAddin too.
 */
public class License {
	
	private final static String MANUFACTURER = "WILUTIONS";
	private final static String PRODUCT = "ITOL";

	public static boolean install(String licenseKey, boolean userNotMachine) {
		boolean succ = DDAddinDll.install(licenseKey, userNotMachine);
		if (succ) {
			String key = getProductRegKey(userNotMachine);
			RegUtil.setRegistryValue(key, "", licenseKey);
		}
		return succ;
	}
	
	public static void uninstall(boolean userNotMachine) {
		DDAddinDll.uninstall(userNotMachine);
		
		String key = getProductRegKey(userNotMachine);
		RegUtil.deleteRegistryKey(key);
	}
	
	private static String getProductRegKey(boolean userNotMachine) {
		String key = userNotMachine ? "HKCU" : "HKLM";
		key += "\\SOFTWARE\\" + MANUFACTURER + "\\" + PRODUCT + "\\License";
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
}
