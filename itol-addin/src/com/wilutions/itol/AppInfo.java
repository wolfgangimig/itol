/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.wilutions.itol.db.Config;

public class AppInfo {
	
	private String serviceFactoryClass;
	
	private List<String> serviceFactoryParams = new ArrayList<String>();
	
	private Config config = new Config();

	private String appName;

	private String manufacturerName;
	
	private File appDir;
	
	public String getServiceFactoryClass() {
		return serviceFactoryClass;
	}

	public void setServiceFactoryClass(String serviceFactoryClass) {
		this.serviceFactoryClass = serviceFactoryClass;
	}

	public List<String> getServiceFactoryParams() {
		return serviceFactoryParams;
	}

	public void setServiceFactoryParams(List<String> serviceFactoryParams) {
		this.serviceFactoryParams = serviceFactoryParams;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getManufacturerName() {
		return manufacturerName;
	}

	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	public File getAppDir() {
		return appDir;
	}

	public void setAppDir(File appDir) {
		this.appDir = appDir;
	}

	
}
