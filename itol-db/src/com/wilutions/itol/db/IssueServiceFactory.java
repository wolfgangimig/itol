package com.wilutions.itol.db;

import java.io.IOException;
import java.util.List;

public interface IssueServiceFactory {

	public IssueService getService(List<String> params) throws IOException;
	
}
