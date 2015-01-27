package de.elo.itol;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.stage.Stage;

import com.wilutions.itol.Globals;
import com.wilutions.joa.AddinApplication;

public class ELOIssueApplication extends AddinApplication {
	
	public static void main(String[] args) {
		
		if (args.length == 0) {
			try {
				String logFile = "D:\\git\\itol\\itol\\logging.properties";
				LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
			} catch (Throwable e) {
				e.printStackTrace();
			}
	
			try {
				Globals.initIssueService();
			} catch (IOException e) {
				e.printStackTrace();
				Logger log = Logger.getLogger(ELOIssueApplication.class.getName());
				log.severe(e.toString());
			}
			
		}
		
		main(ELOIssueApplication.class, ELOIssueApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		instance = this;
		super.start(primaryStage);
	}
	
	private static volatile ELOIssueApplication instance;
	
	public static void showDocument(String url) {
		instance.getHostServices().showDocument(url);
	}
}
