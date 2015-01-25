package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.istack.internal.logging.Logger;

public class ProgressCallbackImpl implements ProgressCallback {
	
	private final Logger log;
	private final ProgressCallback parent;
	protected final String name;
	protected volatile String[] params;
	protected volatile double total;
	protected volatile double childSum;
	
	public ProgressCallbackImpl(String name) {
		this(null, name);
	}
	
	public ProgressCallbackImpl(ProgressCallback parent, String name) {
		this.parent = parent;
		this.name = name;
		this.log = Logger.getLogger(ProgressCallbackImpl.class);
		if (parent != null) {
			parent.setParams(new String[0]);
		}
	}

	@Override
	public void setParams(String... params) {
		this.params = params;
		if (parent != null) {
			List<String> paramList = new ArrayList<String>(Arrays.asList(params));
			if (name != null && name.length() != 0) {
				paramList.add(0, name);
			}
			String[] parentParams = paramList.toArray(new String[paramList.size()]);
			parent.setParams(parentParams);
		}
	}

	@Override
	public void setProgress(double current) {
		StringBuilder line = new StringBuilder();
		line.append(name);
		if (params != null && params.length != 0) {
			line.append(" ");
			line.append(Arrays.toString(params));
		}
		
		if (total > 0) {
			line.append(" ").append(current).append("/").append(total);
		}
		else {
			line.append(" ").append((long)current);
		}
		log.info(line.toString());
		
		if (parent != null) {
			parent.setProgress(childSum + current);
		}
	}
	
	@Override
	public void setTotal(double total) {
		this.total = total;
	}

	@Override
	public boolean isCancelled() {
		boolean ret = false;
		if (parent != null) {
			ret = parent.isCancelled();
		}
		return ret;
	}

	@Override
	public ProgressCallback createChild(String name) {
		return new ProgressCallbackImpl(this, name);
	}

	@Override
	public void setFinished() {
		setProgress(total);
		if (parent != null) {
			parent.childFinished(total);
		}
	}

	@Override
	public void childFinished(double total) {
		childSum += total;
	}

}
