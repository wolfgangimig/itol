/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.wilutions.mslib.outlook.OlSaveAsType;

public class MsgFileTypes {

	/**
	 * File extensions in relation to OlSaveAsType
	 */
	public final static List<String> exts = Arrays.asList(".txt", ".rtf", ".tmp", ".msg", ".doc", ".html", ".vcard",
			".vcal", ".ical", ".msg", ".mhtml");

	/**
	 * Replace characters unsupported by file systems with underscore.
	 * 
	 * @param subject
	 * @return string
	 */
	public static String makeValidFileName(String subject) {
		String ret = subject.trim();
		ret = ret.replaceAll("[^a-zA-Z0-9.-]", "_");
		ret = ret.replace("  ", " ");
		ret = ret.replace(" \\.", ".");
		if (ret.length() > 200) {
			ret = ret.substring(0, 200);
		}
		if (ret.isEmpty()) {
		    DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		    ret = df.format(new Date());
		}
		return ret;
	}

	/**
	 * Make file name for mail with given subject.
	 * 
	 * @param subject
	 *            Mail subject
	 * @param saveAsType
	 *            File type
	 * @return
	 */
	public static String makeMsgFileName(String subject, OlSaveAsType saveAsType) {
		String ext = makeMsgFileExt(saveAsType);
		return makeValidFileName(subject) + ext;
	}

	/**
	 * Get extension for file type.
	 * 
	 * @param saveAsType
	 *            File type
	 * @return File extension starting with "."
	 */
	public static String makeMsgFileExt(OlSaveAsType saveAsType) {
		int idx = saveAsType.value;
		String ext = idx < exts.size() ? exts.get(idx) : ".msg";
		return ext;
	}

	/**
	 * Get file type from extension.
	 * 
	 * @param ext
	 *            File extension
	 * @return File type
	 */
	public static OlSaveAsType getMsgFileType(String ext) {
		OlSaveAsType ret = OlSaveAsType.olMSG;
		if (ext != null && ext.length() != 0) {
			ext = ext.toLowerCase();
			if (!ext.startsWith(".")) {
				ext = "." + ext;
			}
			int idx = exts.indexOf(ext);
			ret = OlSaveAsType.valueOf(idx);
		}
		return ret;
	}

	/**
	 * Return true, if the given format can hold attachments as well.
	 * @param saveAsType File type
	 * @return true for container format (msg, mhtml)
	 */
	public static boolean isContainerFormat(OlSaveAsType saveAsType) {
		boolean ret = false;
		switch (saveAsType.value) {
		case OlSaveAsType._olMSG:
		case OlSaveAsType._olMSGUnicode:
			ret = true;
			break;
		}
		return ret;
	}
}
