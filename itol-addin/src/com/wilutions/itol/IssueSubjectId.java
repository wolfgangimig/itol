package com.wilutions.itol;

import com.wilutions.itol.db.Issue;

// http://stackoverflow.com/questions/22946871/smtp-reply-method

public class IssueSubjectId {

	private static final String ISSUE_ID_PREFIX = "[R-";

	public static String extractIssueIdFromMailSubject(String subject) {
		String issueId = "";
		String startTag = ISSUE_ID_PREFIX;
		int p = subject.indexOf(startTag);
		if (p >= 0) {
			int q = subject.indexOf("]", p);
			if (q >= 0) {
				issueId = subject.substring(p + startTag.length(), q);
			}
		}
		else {
			p = subject.indexOf(" #");
			if (p >= 0) {
				p += 2;
				int q = p;
				for (; q < subject.length(); q++) {
					char c = subject.charAt(q);
					if (!Character.isDigit(c)) break;
				}
				if (q > p) {
					issueId = subject.substring(p, q);
				}
			}
		}
		return issueId;
	}

	public static String stripOneIssueIdFromMailSubject(String subject) {
		String ret = subject;

		String startTag = ISSUE_ID_PREFIX;
		int p = subject.indexOf(startTag);
		if (p >= 0) {
			int q = subject.indexOf("]", p);
			if (q >= 0) {
				ret = subject.substring(q + 1).trim();
			}
		}
		ret = ret.trim();
		return ret;
	}

	public static String stripIssueIdFromMailSubject(String subject) {
		String ret = stripOneIssueIdFromMailSubject(subject);
		while (ret != subject) {
			subject = ret;
			ret = stripOneIssueIdFromMailSubject(subject);
		}
		ret = ret.trim();
		return ret;
	}

	public static String stripReFwdFromSubject(String subject) {
		String p = "";
		while (p != subject) {
			p = subject;
			subject = stripFirstReFwdFromSubject(p);
		}
		return subject;
	}

	private static String stripFirstReFwdFromSubject( String subject) {
		subject = subject.trim();
		String s = subject.toLowerCase();
		int p = s.indexOf(":");
		if (p < 4) {
			subject = subject.substring(p + 1);
		}
		return subject;
	}

	public static String injectIssueIdIntoMailSubject(String subject, Issue iss) {
		String ret = stripIssueIdFromMailSubject("" + subject);

		if (iss != null && iss.getId() != null && iss.getId().length() != 0) {
			ret = ISSUE_ID_PREFIX + iss.getId() + "] ";
			ret += subject;
		}
		else {
			int p = subject.indexOf(ISSUE_ID_PREFIX);
			if (p >= 0) {
				int q = subject.indexOf("]", p + 3);
				if (q >= 0) {
					ret = subject.substring(0, p);
					ret += subject.substring(q + 1);
				}
			}
		}

		ret = ret.trim();
		return ret;
	};


}
