package com.wilutions.fx.util;

import java.io.FileInputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Manifest;

public class ManifestUtil {

	private Properties props = new Properties();
	
	public ManifestUtil(Class<?> clazz) {
		getManifest(clazz);
	}
	
	public String getProgramVersion() {
		return props.getProperty("programVersion");
	}

	public String getProgramName() {
		return props.getProperty("programName");
	}

	public String getProgramTitle() {
		return props.getProperty("programTitle");
	}

	public ProgramVersionInfo getProgramVersionInfo() {
		ProgramVersionInfo ret = new ProgramVersionInfo();
		ret.setName(getProgramName());
		ret.setVersion(getProgramVersion());
		ret.setTitle(getProgramTitle());
		return ret;
	}

	public static String getProgramVersion(Class<?> clazz) {
		return new ManifestUtil(clazz).getProgramVersion();
	}

	public static String getProgramName(Class<?> clazz) {
		return new ManifestUtil(clazz).getProgramName();
	}

	public static String getProgramTitle(Class<?> clazz) {
		return new ManifestUtil(clazz).getProgramTitle();
	}
	
	public static ProgramVersionInfo getProgramVersionInfo(Class<?> clazz) {
		return new ManifestUtil(clazz).getProgramVersionInfo();
	}

	private void getManifest(Class<?> clazz) {
		try {
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			if (!classPath.startsWith("jar")) {
			    loadFromGradleProperties();
			}
			else {
				URL url = new URL(classPath);
				JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
				Manifest manifest = jarConnection.getManifest();
				String version = manifest.getMainAttributes().getValue("Implementation-Version");
				props.put("programVersion", version);
				String programName = manifest.getMainAttributes().getValue("Implementation-Name");
				props.put("programName", programName);
				String programTitle = manifest.getMainAttributes().getValue("Implementation-Title");
				props.put("programTitle", programTitle);
			}
		}
		catch (Exception ex) {
			System.err.println("Cannot read gradle.properties.");
			ex.printStackTrace();
		}
	}
	
	private void loadFromGradleProperties() throws Exception {
		try (FileInputStream fis = new FileInputStream("./gradle.properties")) {
			props.load(fis);
		}
	}

}
