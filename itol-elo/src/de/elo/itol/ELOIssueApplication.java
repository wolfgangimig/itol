package de.elo.itol;

import javafx.stage.Stage;

import com.wilutions.itol.IssueApplication;

public class ELOIssueApplication extends IssueApplication {
	
	public static void main(String[] args) {
		
		IssueApplication.main(ELOIssueApplication.class, ELOIssueApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		super.start(primaryStage);
	}
	
}
