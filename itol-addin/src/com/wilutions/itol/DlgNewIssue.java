package com.wilutions.itol;

import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.wilutions.joa.fx.ModalDialogFX;
import com.wilutions.mslib.outlook.Explorer;


public class DlgNewIssue extends ModalDialogFX<Boolean> {
	
	private MailInspector mailInspector;
	private IssueMailItem mailItem;
	private boolean modal;
	
	public DlgNewIssue(MailInspector mailInspector, IssueMailItem mailItem) {
		this.mailInspector = mailInspector;
		this.mailItem = mailItem;
		setTitle("New Issue");
		this.modal = false;
	}

	public void show(Explorer explorer) throws IOException {
		
		if (modal) {
			showAsync(explorer, null);
		}
		else {
			final double explX = explorer.getLeft();
			final double explY = explorer.getTop();
			final double explWd = explorer.getWidth();
			final double explHt = explorer.getHeight();

			Platform.runLater(() -> {
				
				Stage stage = new Stage();
		        stage.setTitle(getTitle());
		
		        IssueTaskPane taskPane = new IssueTaskPane(mailInspector);
		        
		        Scene scene = taskPane.createScene();

				scene.impl_preferredSize();
				double sceneWidth = scene.getWidth();
				double sceneHeight = scene.getHeight();
				
				double dlgX = explX;
				if (explWd > sceneWidth) {
					dlgX += (explWd - sceneWidth) / 2;
				}
				
				double dlgY = explY;
				if (explHt > sceneWidth) {
					dlgY += (explHt - sceneHeight) / 2;
				}
				
				stage.setX(dlgX);
				stage.setY(dlgY);
		        stage.setScene(scene);
		        stage.show();
		        
		        stage.toFront();
			});
		}
	}

	@Override
	public Scene createScene() {
        IssueTaskPane taskPane = new IssueTaskPane(mailInspector);
        taskPane.setWindowOwner(this);
        Scene scene = taskPane.createScene();
        return scene;
	}

	
}
