/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

public interface ProgressCallback {
	public void setParams(String ... params);
	public void incrProgress(double amout);
	public double getProgress();
	public void setTotal(double total);
	public double getTotal();
	public void setFakeProgress(boolean v);
	public void setFinished();
	public boolean isCancelled();
	public ProgressCallback createChild(String name, double ratio);
	public ProgressCallback createChild(String name, double childTotal, double parentTotal);
	public ProgressCallback createChild(double ratio);
	public ProgressCallback createChild(double childTotal, double parentTotal);
	public void cancel();
}
