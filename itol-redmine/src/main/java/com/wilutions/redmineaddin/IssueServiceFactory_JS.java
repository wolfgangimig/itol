/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.redmineaddin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;

public class IssueServiceFactory_JS implements IssueServiceFactory {

	public static final String DEFAULT_SCIRPT = "IssueServiceImpl.js";

	public IssueServiceFactory_JS() {
	}

	public IssueService getService(File instDir, List<String> params) throws IOException {
		IssueService srv = null;
		Reader rd = null;
		
		if (params == null || params.size() == 0) {
			params.add(DEFAULT_SCIRPT);
		}
		
		try {
//			ClassLoader classLoader = this.getClass().getClassLoader();
//			InputStream istream = classLoader.getResourceAsStream("com/wilutions/itol/db/impl/IssueServiceImpl.js");
//			rd = new InputStreamReader(istream, "UTF-8");
//			engine.eval(rd);
			
			// If the current thread is not created by Java but by Windows COM, 
			// it does not have a context class loader attached. 
			// Since the Nashorn engine uses the thread's context class loader to find
			// Java classes, we make sure, that it is not null.
			// See: http://comments.gmane.org/gmane.comp.java.openjdk.nashorn.devel/2366
			ClassLoader cll = Thread.currentThread().getContextClassLoader();
			if (cll == null) {
				Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			}
			
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			
			File jsFile= new File(instDir, params.get(0));
			String jsName = jsFile.getAbsolutePath().replace("\\", "/");
			engine.eval("load(\"" + jsName + "\");");
			
			srv = ((Invocable)engine).getInterface(IssueService.class);
			if (srv == null) {
				throw new IllegalStateException(jsName + " does not implement all functions of the service interface");
			}
		}
		catch (Throwable e) {
			throw new IOException(e);
		}
		finally {
			if (rd != null) {
				rd.close();
			}
		}
		return srv;
	}
}