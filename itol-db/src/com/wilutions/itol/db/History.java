package com.wilutions.itol.db;

/**
 * HTML views of comment and worklog history.
 *
 */
public class History {
	
	/**
	 * HTML view of comment history.
	 */
	private String commentsHtml;
	
	/**
	 * HTML view of worklogHistory.
	 */
	private String worklogsHtml;
	
	public String getCommentsHtml() {
		if (commentsHtml == null) commentsHtml = "";
		return commentsHtml;
	}

	public void setCommentsHtml(String commentsHtml) {
		this.commentsHtml = commentsHtml;
	}

	public String getWorklogsHtml() {
		if (worklogsHtml == null) worklogsHtml = "";
		return worklogsHtml;
	}

	public void setWorklogsHtml(String worklogsHtml) {
		this.worklogsHtml = worklogsHtml;
	}

	
}
