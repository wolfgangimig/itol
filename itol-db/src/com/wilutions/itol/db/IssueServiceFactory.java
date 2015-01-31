/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface IssueServiceFactory {

	public IssueService getService(File instDir, List<String> params) throws IOException;
	
}
