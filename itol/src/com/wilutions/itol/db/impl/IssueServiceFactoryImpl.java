package com.wilutions.itol.db.impl;

import java.io.IOException;

import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;

public class IssueServiceFactoryImpl implements IssueServiceFactory {

	@Override
	public IssueService getService() throws IOException {
		return new IssueServiceImpl();
	}

}
