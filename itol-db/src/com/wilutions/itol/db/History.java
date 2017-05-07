package com.wilutions.itol.db;

/**
 * HTML views of comment and worklog history.
 *
 */
public class History {
	
	/**
	 * Display comments when initially loaded.
	 */
	public final static int FLAG_COMMENTS_NEWER = 1;
	/**
	 * Display worklogs when initially loaded.
	 */
	public final static int FLAG_WORKLOG_NEWER = 2;
	
	/**
	 * Combination of FLAG_ values.
	 */
	private int flags;
	
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

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	
}
