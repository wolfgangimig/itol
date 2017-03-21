package com.wilutions.itol.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class ISODate {

	public final static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	public static String toISO(LocalDateTime ldate) {
		String iso = null;
		if (ldate != null) {
			Date date = Date.from(ldate.atZone(ZoneId.systemDefault()).toInstant());
			iso = toISO(date);
		}
		return iso;
	}

	public static LocalDateTime toLocalDateTime(String iso) throws ParseException {
		LocalDateTime ldate = null;
		if (iso != null && iso.length() != 0) {
			try {
				Date date = dateTimeFormat.parse(iso);
				ldate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			} catch (ParseException e) {
				// log.log(Level.WARNING, "Failed to parse ISO date provided by
				// JIRA for property worklog.started=" + iso, e);
				throw e;
			}
		}
		return ldate;
	}

	public static String toISO(Date date) {
		String iso = null;
		if (date != null) {
			iso = dateTimeFormat.format(date);
		}
		return iso;
	}

}
