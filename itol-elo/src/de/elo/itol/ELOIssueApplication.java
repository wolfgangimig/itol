package de.elo.itol;

import java.util.ArrayList;
import java.util.Arrays;

import javafx.stage.Stage;

import com.wilutions.itol.Config;
import com.wilutions.itol.Globals;
import com.wilutions.itol.IssueApplication;
import com.wilutions.itol.db.Property;
import com.wilutions.itol.db.impl.IssueServiceFactory_JS;

public class ELOIssueApplication extends IssueApplication {
	
	public static void main(String[] args) {
		
		Config config = Globals.getConfig();
		config.appName = "ELO Issue Tracker";
		config.manufacturerName = "ELO Digital";
		config.serviceFactoryClass = IssueServiceFactory_JS.class.getName();
		config.serviceFactoryParams = Arrays.asList("IssueServiceImpl-ELO.js");
		config.configProps = new ArrayList<Property>(0);
		
		IssueApplication.main(ELOIssueApplication.class, ELOIssueApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		super.start(primaryStage);
	}
	
}
