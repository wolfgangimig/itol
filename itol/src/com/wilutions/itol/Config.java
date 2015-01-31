/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.util.List;

import com.wilutions.itol.db.Property;

public class Config {
	
	public String serviceFactoryClass;
	
	public List<String> serviceFactoryParams;
	
	public List<Property> configProps;

	public String appName;

	public String manufacturerName;

}
