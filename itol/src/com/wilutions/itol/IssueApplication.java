package com.wilutions.itol;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.stage.Stage;

import com.wilutions.joa.AddinApplication;

public class IssueApplication extends AddinApplication {

	public static void main(String[] args) {
		
		try {
			String logFile = "D:\\java\\workspace_itol\\itol\\logging.properties";
			LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
		} catch (Throwable e) {
		}

		try {
			Globals.initIssueService();
		} catch (IOException e) {
			e.printStackTrace();
			Logger log = Logger.getLogger(IssueApplication.class.getName());
			log.severe(e.toString());
		}
		
		main(IssueApplication.class, IssueApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		super.start(primaryStage);
	}
}
