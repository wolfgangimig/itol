/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.MsgFileFormat;
import com.wilutions.joa.ribbon.RibbonListItem;
import com.wilutions.mslib.outlook.OlSaveAsType;

public class MsgFileTypes {
	
	/**
	 * Replace characters unsupported by file systems with underscore.
	 * 
	 * @param subject
	 * @return string
	 */
	public static String makeValidFileName(String subject) {
		String ret = subject.trim();
		//ret = ret.replaceAll("[^a-zA-Z0-9.-]", "_");
		ret = ret.replaceAll("[\\<\\>\\:\\\"/\\\\|?*]", "_");
		ret = ret.replace("  ", " ");
		ret = ret.replace(" .", ".");
		ret = ret.replace("\t", " ");
		ret = ret.replace("\r", " ");
		ret = ret.replace("\n", " ");
		if (ret.length() > 200) {
			ret = ret.substring(0, 200);
		}
		while (!ret.isEmpty() && (ret.endsWith(".") || Character.isWhitespace(ret.charAt(ret.length()-1)))) {
			ret = ret.substring(0, ret.length()-1);
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
		String ext = idx < MsgFileFormat.exts.size() ? MsgFileFormat.exts.get(idx) : ".msg";
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
			int idx = MsgFileFormat.exts.indexOf(ext);
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
	
	@SuppressWarnings("unused")
	private static RibbonListItem makeRibbonListItem(IdName idn, String fileName) {
		return new RibbonListItem(idn.getId(), idn.getName(), fileName);
	}


//	public final static void main(String[] args) {
//		System.out.println(makeValidFileName("< > x : \" x / \\ x | ? x * ."));
//		System.out.println(makeValidFileName("abcdefg.hij"));
//		System.out.println(makeValidFileName("<.x"));
//		System.out.println(makeValidFileName(">.>"));
//		System.out.println(makeValidFileName("\\./"));
//	}

}
