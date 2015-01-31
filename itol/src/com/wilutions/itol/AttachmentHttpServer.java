/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP server to deliver the issue descriptions and attachments.
 */
public class AttachmentHttpServer {

	private HttpServer httpServer;

	public void start() {
		HttpServer server = null;
		IOException lastException = null;
		for (int port = 10101; port < 11000; port++) {
			try {
				server = HttpServer.create(new InetSocketAddress(port), 0);
				server.start();
				lastException = null;
				break;
			} catch (IOException e) {
				lastException = e;
			}
		}
		if (lastException != null) {
			lastException.printStackTrace();
		}
		httpServer = server;
	}

	public void done() {
		httpServer.stop(0);
	}

	public void createContext(String uri, HttpHandler object) {
		httpServer.createContext(uri, object);
	}

	public void removeContext(HttpContext httpContext) {
		httpServer.removeContext(httpContext);
	}

	public String getUrl() {
		return "http://localhost:" + httpServer.getAddress().getPort();
	}

}
