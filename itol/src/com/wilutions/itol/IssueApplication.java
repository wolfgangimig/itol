package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.stage.Stage;

import com.wilutions.com.ComException;
import com.wilutions.joa.AddinApplication;

public class IssueApplication extends AddinApplication {

	public static File getAppDir() {
		File ret = new File(System.getProperty("user.dir"));
		String javaHome = System.getProperty("java.home");
		System.out.println("javaHome=" + javaHome);

		// Self-contained Java application?
		boolean isSelfContainedApp = javaHome.endsWith("runtime\\jre");
		System.out.println("isSelfContainedApp=" + isSelfContainedApp);

		if (isSelfContainedApp) {

			// Application is an EXE file found at "${java.home}/../../"
			File appDir = new File(javaHome).getParentFile().getParentFile();
			ret = new File(appDir, "app");
		}

		return ret;
	}

	public boolean parseCommandLine(String[] args) throws ComException, IOException {
		boolean finished = super.parseCommandLine(args);
		if (!finished) {
			initIssueService();
		}
		return finished;
	}

	private void initIssueService() {

		File appDir = getAppDir();

		InputStream istream = null;
		try {
			File logFile = new File("logging.properties");
			if (!logFile.exists()) {
				logFile = new File(appDir, "logging.properties");
			}
			istream = new FileInputStream(logFile);
			LogManager.getLogManager().readConfiguration(istream);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (istream != null) {
				try {
					istream.close();
				} catch (IOException ignored) {
				}
			}
		}

		try {
			Globals.initIssueService(appDir);
		} catch (IOException e) {
			e.printStackTrace();
			Logger log = Logger.getLogger(IssueApplication.class.getName());
			log.severe(e.toString());
		}
	}

	public static void main(String[] args) {

		main(IssueApplication.class, IssueApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		instance = this;
		super.start(primaryStage);
	}

	private static volatile IssueApplication instance;

	public static void showDocument(String url) {
		instance.getHostServices().showDocument(url);
	}
}
