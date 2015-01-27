package com.wilutions.itol.db;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface IssueServiceFactory {

	public IssueService getService(File instDir, List<String> params) throws IOException;
	
}
