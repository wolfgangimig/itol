/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import javafx.scene.Node;

public interface IssuePropertyEditor {

	Node getNode();
	
	void setFocus();
	
	void updateData(boolean save);
	
	void setIssue(Issue issue);
}
