package com.wilutions.itol;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;

public class IssueDescriptionParser {

	public static String stripOriginalMessageFromReply(String from, String to, String subject, String description) {
		String ret = "";
		OultookOriginalMessageDelimiter delim = new OultookOriginalMessageDelimiter();
		delim.replyFrom = from;
		delim.replyTo = to;
		delim.replySubject = subject;
		int outlookDelimPos = findReplyDelim(description, delim);
		if (outlookDelimPos >= 0) {
			ret = description.substring(0, outlookDelimPos).trim();
		}
		return ret;
	}

	/**
	 * Find beginning of original message.
	 * 
	 * @param description
	 * @return position or -1
	 */
	private static int findReplyDelim(String description, OultookOriginalMessageDelimiter delim) {
		int org = 0;
		while (org < description.length()) {

			org = findFirstTextAfterNextEmptyLine(description, org);
			if (org < 0) break;
			
			// find e.g.: On Wed, Nov 18, 2015 at 2:53 PM, WILUTIONS <wilutions@gmail.com <mailto:wilutions@gmail.com> > wrote:
			if (isMaybeSimpleHeaderLine(description, org, delim)) break;

			// find e.g.:
			// From: WILUTIONS [mailto:wilutions@googlemail.com]
			// Sent: Tuesday, November 24, 2015 4:02 PM
			// To: Ch Hough <cxxxx@axxx.com>
			// Subject: AW: license email
			int pos = org;
			pos = isMaybeOutlookHeaderLine(description, pos, delim);
			pos = isMaybeOutlookHeaderLine(description, pos, delim);
			pos = isMaybeOutlookHeaderLine(description, pos, delim);
			pos = isMaybeOutlookHeaderLine(description, pos, delim);

			// Break, if at least 2 lines found that could belong to delimiter
			// lines.
			if (delim.toValue() >= 2) break;
		}
		return org;
	}

	/**
	 * Find first text after empty line starting from pos.
	 * 
	 * @param description
	 * @param pos
	 * @return position of text after the empty line or -1
	 */
	private static int findFirstTextAfterNextEmptyLine(String description, int pos) {
		int r = pos >= 0 ? description.indexOf('\n', pos) : -1;
		while (r >= 0) {
			r++;
			int q = description.indexOf('\n', r);
			if (q >= 0) {
				String s = description.substring(r, q).trim();
				r = q;
				if (s.length() == 0) {
					
					// find first non-whitespace
					while (++r < description.length()) {
						char ch = description.charAt(r);
						if (!Character.isWhitespace(ch)) break;
					}
					
					if (r >= description.length()) r = -1;
					break;
				}
			}
			else {
				r = -1;
			}
		}
		return r;
	}

	private static class OultookOriginalMessageDelimiter {
		boolean fromFound;
		boolean sentFound;
		boolean toFound;
		boolean subjectFound;

		String replyTo;
		String replyFrom;
		String replySubject;

		int toValue() {
			int v = 0;
			if (fromFound) v++;
			if (toFound) v++;
			if (sentFound) v++;
			if (subjectFound) v++;
			return v;
		}
	}

	/**
	 * Check whether line starting from pos could be a Outlook header line. A
	 * header line starts with one word, maybe followed by blanks, then a colon,
	 * then maybe blanks, and then some characters. If description beginning at
	 * pos is a header line, the function returns the starting position of the
	 * next line. If description beginning at pos is not a header line, the
	 * function returns -1.
	 * 
	 * @param description
	 * @param pos
	 * @param delim
	 * @return next line start position or -1.
	 */
	private static int isMaybeOutlookHeaderLine(String description, int pos, OultookOriginalMessageDelimiter delim) {
		int n = pos >= 0 ? description.indexOf('\n', pos) : -1;
		if (n >= 0) {

			String s = description.substring(pos, n);
			int w = s.indexOf(':');
			if (w > 0 && w + pos < n - 3) {

				s = s.substring(w + 1);

				boolean next = true;
				if (next && !delim.fromFound) {
					delim.fromFound = isMaybeMailAddress(s, delim.replyTo);
					next = !delim.fromFound;
				}
				if (next && !delim.toFound) {
					delim.toFound = isMaybeMailAddress(s, delim.replyFrom);
					next = !delim.toFound;
				}
				if (next && !delim.subjectFound) {
					delim.subjectFound = isMaybeSubject(s, delim.replySubject);
					next = !delim.subjectFound;
				}
				if (next && !delim.sentFound) {
					delim.sentFound = isMaybeSentDate(s);
					next = !delim.sentFound;
				}

				n++;
			}
			else {
				n = -1;
			}
		}

		return n;
	}

	/**
	 * Check whether the line starting from pos contains a mail address and a date.
	 * @param description
	 * @param pos
	 * @param delim
	 * @return true, if both expected values found.
	 */
	private static boolean isMaybeSimpleHeaderLine(String description, int pos, OultookOriginalMessageDelimiter delim) {
		int n = pos >= 0 ? description.indexOf('\n', pos) : -1;
		if (n >= 0) {

			String s = description.substring(pos, n);

			delim.fromFound = isMaybeMailAddress(s, delim.replyTo);
			delim.sentFound = isMaybeSentDate(s);

			n++;
		}

		return delim.fromFound && delim.sentFound;
	}

	private static boolean isMaybeMailAddress(String s, String replyAddr) {
		boolean ret = false;
		String s1 = trimMailAddress(s);
		String k1 = trimMailAddress(replyAddr);
		HashSet<String> sparts = new HashSet<String>(Arrays.asList(s1.split(" ")));
		HashSet<String> kparts = new HashSet<String>(Arrays.asList(k1.split(" ")));
		for (String k : kparts) {
			if (sparts.contains(k)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	/**
	 * Eliminate mail server address and special chars from given string. Should
	 * return only first name and family name.
	 * 
	 * @param s
	 * @return name parts
	 */
	private static String trimMailAddress(String s) {
		StringBuilder r = new StringBuilder();
		boolean skippingServerAddress = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (skippingServerAddress) {
				skippingServerAddress = Character.isLetterOrDigit(c);
			}
			else {
				skippingServerAddress = c == '@';
			}

			if (!skippingServerAddress) {
				if (Character.isLetterOrDigit(c)) {
					r.append(c);
				}
				else if (r.length() == 0 || r.charAt(r.length() - 1) != ' ') {
					r.append(' ');
				}
			}
		}
		return r.toString().trim().toLowerCase();
	}

	private static boolean isMaybeSentDate(String s) {
		s = s.replace('-', ' ');
		s = s.replace('.', ' ');
		s = s.replace('/', ' ');
		String[] parts = s.split(" ");

		boolean yearFound = false;
		boolean hourFound = false;
		boolean minuteFound = false;

		for (int i = 0; i < parts.length && !(yearFound && hourFound && minuteFound); i++) {
			String part = parts[i].trim();
			if (!yearFound && part.length() >= 4) {
				try {
					int year = Integer.parseInt(part);
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTimeInMillis(System.currentTimeMillis());
					int thisYear = cal.get(Calendar.YEAR);
					if (year >= 1970 && year <= thisYear) {
						yearFound = true;
					}
				}
				catch (Exception ignored) {
				}
			}
			else if (!(yearFound && minuteFound) && part.length() >= 4) {
				String[] hm = part.split(":");
				if (hm.length == 2) {
					try {
						int hour = Integer.parseInt(hm[0]);
						int minute = Integer.parseInt(hm[1]);
						hourFound = hour >= 0 && hour < 24;
						minuteFound = minute >= 0 && minute < 60;
					}
					catch (Exception ignored) {
					}
				}
			}
		}
		return yearFound && hourFound && minuteFound;
	}

	private static boolean isMaybeSubject(String s, String knownSubject) {
		boolean ret = false;
		s = s.trim();
		if (s.length() > 5) {
			int n = Math.min(s.length(), knownSubject.length());
			int nbOfMatchingChars = 0;
			for (; nbOfMatchingChars < n; nbOfMatchingChars++) {
				char cs = s.charAt(s.length() - nbOfMatchingChars - 1);
				char ck = knownSubject.charAt(knownSubject.length() - nbOfMatchingChars - 1);
				if (cs != ck) break;
			}
			int matchingPercent = (nbOfMatchingChars * 100) / knownSubject.length();
			ret = matchingPercent > 50;
		}
		return ret;
	}
	
	// das kann ich später mal versuchen.
	// greetings sind bereits in res/greetings.properties zu finden
	@SuppressWarnings("unused")
	private static int findGreeting(String description, int pos) {
		int ret = -1;
		
		return ret;
	}
	
	/*
	 * Outlook Antworten: Leerzeile, dann 4 aufeinanderfolgende Zeilen, die mit
	 * einem Wort + Doppelpunkt beginnen Eine Zeile kann eine Mailadresse
	 * enthalten: [*]@[*].[*] Eine Zeile kann ein Datum enthalten: Zahl zw 1970
	 * und heutiger Jahreszahl, Uhrzeit [1-2 stellige zahl]:[1-2 stellige zahl]
	 * Eine Zeile enthält nach dem ersten Doppelpunkt den Betreff
	 * 
	 */

	// De : WILUTIONS [mailto:wilutions@googlemail.com]
	//
	// Envoyé : dimanche 29 novembre 2015 14:54
	//
	// À : Malvina Camus
	//
	// Objet : AW: QUOTE REQUEST
	//
	//
	// Da: Wolfgang Imig [mailto:wilutions@googlemail.com]
	//
	// Inviato: giovedì 16 luglio 2015 11.43
	//
	// A: Claudio Gamberini
	//
	// Oggetto: Re: Info about DDAddin
	//
	//
	// From: WILUTIONS [mailto:wilutions@googlemail.com]
	//
	// Sent: Tuesday, November 24, 2015 4:02 PM
	//
	// To: Chuck Houghton <choughton@alro.com>
	//
	// Subject: AW: license email
	//
	//
	// Von: Rupert Stuffer
	//
	// Gesendet: Dienstag, 22. September 2015 08:57
	//
	// An: 'Wolfgang Imig'
	//
	// Betreff: AW: Anfrage bzgl. Ihres Outlook Add Ins
	//
	//
	// Van: WILUTIONS [mailto:wilutions@gmail.com]
	//
	// Verzonden: dinsdag 17 november 2015 23:09
	//
	// Aan: Jaap van der Reijden <jaap@toplogistics.nl>
	//
	// Onderwerp: AW: Request Add Inn Offer
	//
	//
	// -----Original Message-----
	//
	// From: WILUTIONS Support [mailto:mailer@fastspring.com]
	//
	// Sent: Friday, October 09, 2015 3:26 PM
	//
	// To: George Evans
	//
	// Subject: COMMERCIAL: Your Drag and Drop to HTML5 Addin for Microsoft
	// Outlook, NAMED USER License Delivery Information
	//
	//
	// From: Or Fogel
	//
	// Sent: Thursday, September 03, 2015 6:10 PM
	//
	// To: 'WILUTIONS'
	//
	// Subject: RE: Drag Drop solution - Outlook to HTML5

}
