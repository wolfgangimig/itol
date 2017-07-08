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
	 * References to attachments are replaced by this marker plus the attachment ID.
	 */
	public final static String ATTACHMENT_MARKER_BEGIN = "8e942af8-9e06-4093-92b4-7c5efa212f8d-";
	public final static String ATTACHMENT_MARKER_END = "-8e942af8-9e06-4093-92b4-7c5efa212f8d";
	
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
	
	/**
	 * Last comment or worklog entry.
	 * This text is used in the reply mail that can be automatically send to the reporter.
	 * @see Config#getAutoReplyField().
	 */
	private String lastComment;
	
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

	public String getLastComment() {
		if (lastComment == null) lastComment = "";
		return lastComment;
	}

	public void setLastComment(String s) {
		this.lastComment = s;
	}

}
