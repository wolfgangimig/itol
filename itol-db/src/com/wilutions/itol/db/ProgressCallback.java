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
