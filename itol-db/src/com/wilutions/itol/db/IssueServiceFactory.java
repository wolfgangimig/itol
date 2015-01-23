package com.wilutions.itol.db;

import java.io.IOException;

public interface IssueServiceFactory {

	public IssueService getService() throws IOException;
	
}
