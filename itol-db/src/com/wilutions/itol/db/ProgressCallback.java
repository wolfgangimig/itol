/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

public interface ProgressCallback {
	public void setParams(String ... params);
	public void setProgress(double current);
	public void setTotal(double total);
	public void setFinished();
	public boolean isCancelled();
	public ProgressCallback createChild(String name);
	public void childFinished(double total);
}
