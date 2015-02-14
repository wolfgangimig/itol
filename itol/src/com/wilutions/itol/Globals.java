/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.stage.Stage;

import com.wilutions.com.BackgTask;
import com.wilutions.com.reg.Registry;
import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;
import com.wilutions.itol.db.PasswordEncryption;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.PropertyClass;
import com.wilutions.itol.db.PropertyClasses;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;
import com.wilutions.joa.OfficeAddinUtil;


public class Globals {

	private static ItolAddin addin;
	private static MailExport mailExport = new MailExport();
	private static ResourceBundle resb;
	private static IssueService issueService;
	private static Registry registry;
	private static File appDir;

	private static Config config = new Config();

	public static Config getConfig() {
		return config;
	}

	protected static void setThisAddin(ItolAddin addin) {
		Globals.addin = addin;
	}

	public static Registry getRegistry() {
		if (registry == null) {
			registry = new Registry(config.manufacturerName, config.appName);
		}
		return registry;
	}

	public static void initIssueService(File appDir) throws IOException {

		Globals.appDir = appDir;

		// Required for PasswordEncryption.decrypt
		com.sun.org.apache.xml.internal.security.Init.init();

		readData();

		try {
			Class<?> clazz = Class.forName(config.serviceFactoryClass);
			IssueServiceFactory fact = (IssueServiceFactory) clazz.newInstance();
			issueService = fact.getService(appDir, config.serviceFactoryParams);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new IOException(e);
		}

		BackgTask.run(() -> {
			try {
				issueService.setConfig(config.configProps);
				System.out.println("Issue service initialized.");
				
				Platform.runLater(() -> {
					Stage dlg = new Stage();
					IssueMailItem mitem = new IssueMailItemBlank() {
						public String getSubject() {
							return "[R-206] test";
						}
					};
					dlg.setScene(new IssueTaskPane(null, mitem).createScene());
					dlg.showAndWait();
					System.exit(0);
				});
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private static void readData() {
		Config newConfig = (Config) getRegistry().read("Config");

		if (newConfig != null) {
			if (newConfig.serviceFactoryClass != null) {
				config.serviceFactoryClass = newConfig.serviceFactoryClass;
			}

			if (newConfig.serviceFactoryParams != null) {
				config.serviceFactoryParams = newConfig.serviceFactoryParams;
			}

			if (newConfig.configProps != null) {
				config.configProps = newConfig.configProps;
			}
		}

		for (Property configProp : config.configProps) {
			PropertyClass propClass = PropertyClasses.getDefault().get(configProp.getId());
			if (propClass != null && propClass.getType() == PropertyClass.TYPE_PASSWORD) {
				try {
					configProp.setValue(PasswordEncryption.decrypt((String) configProp.getValue()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void writeData() {
		for (Property configProp : config.configProps) {
			PropertyClass propClass = PropertyClasses.getDefault().get(configProp.getId());
			if (propClass != null && propClass.getType() == PropertyClass.TYPE_PASSWORD) {
				try {
					configProp.setValue(PasswordEncryption.encrypt((String) configProp.getValue()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		getRegistry().write("Config", config);
	}

	public static void setConfig(List<Property> configProps) throws IOException {
		config.configProps = configProps;
		writeData();
		readData();
		issueService = new IssueServiceFactory_JS().getService(appDir, config.serviceFactoryParams);
		issueService.setConfig(configProps);
	}

	public static ItolAddin getThisAddin() {
		return addin;
	}

	public static MailExport getMailExport() {
		return mailExport;
	}

	public static IssueService getIssueService() throws IOException {
		if (issueService == null) {
			throw new IOException("Issue service not initialized.");
		}
		return issueService;
	}

	public static ResourceBundle getResourceBundle() {
		if (resb == null) {
			try {
				ClassLoader classLoader = Globals.class.getClassLoader();
				InputStream inputStream = classLoader.getResourceAsStream("com/wilutions/itol/res_en.properties");
				resb = new PropertyResourceBundle(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
				try {
					resb = new PropertyResourceBundle(new ByteArrayInputStream(new byte[0]));
				} catch (IOException never) {
				}
			}
		}
		return resb;
	}

	public static File getTempDir() {
		File dir = new File(".");
		try {
			dir = File.createTempFile("itol", ".tmp");
			dir.delete();
			dir.mkdirs();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dir;
	}

	public static String getVersion() {
		String ret = "";
		try {
			byte[] buf = OfficeAddinUtil.getResourceAsBytes(BackstageConfig.class,
					"version.properties");
			Properties props = new Properties();
			props.load(new ByteArrayInputStream(buf));
			ret = props.getProperty("Version");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
}
