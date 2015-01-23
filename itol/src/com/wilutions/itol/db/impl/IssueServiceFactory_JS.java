package com.wilutions.itol.db.impl;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.wilutions.itol.db.IssueService;
import com.wilutions.itol.db.IssueServiceFactory;

public class IssueServiceFactory_JS implements IssueServiceFactory {

	private File tempDir;

	public final static int TYPE_BUG = 1;
	public final static int TYPE_FEATURE_REQUEST = 2;
	public final static int TYPE_SUPPORT = 3;
	public final static int TYPE_DOCUMENTATION = 4;

	public IssueServiceFactory_JS() {
		tempDir = new File(new File(System.getProperty("java.io.tmpdir"), "itol"), "issuetracker");
		tempDir.mkdirs();
	}

	public IssueService getService() throws IOException {
		IssueService srv = null;
		Reader rd = null;
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
			
			engine.eval("load(\"" + "IssueServiceImpl.js" + "\");");
			
			srv = ((Invocable)engine).getInterface(IssueService.class);
		}
//		catch (IOException e) {
//			throw e;
//		}
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