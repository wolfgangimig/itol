package com.wilutions.itol.db.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;

public class IssueServiceFactoryImpl implements IssueServiceFactory {

	@Override
	public IssueService getService(File instDir, List<String> params) throws IOException {
		return new IssueServiceImpl();
	}

}
