package com.wilutions.itol.db;

import java.util.Arrays;
import java.util.List;

public class MsgFileFormat {

	/**
	 * Do not attach mail.
	 */
	public final static IdName NOTHING = new IdName("", "");
	/**
	 * Attach mail as MSG.
	 */
	public final static IdName MSG = new IdName(".msg", "Outlook (.msg)");
	/**
	 * Attach mail as MHTML.
	 */
	public final static IdName MHTML = new IdName(".mhtml", "MIME HTML (.mhtml)");
	/**
	 * Attach mail as RTF.
	 */
	public final static IdName RTF = new IdName(".rtf", "Rich Text Format (.rtf)");
	/**
	 * Attach mail as plain text.
	 */
	public final static IdName TEXT = new IdName(".txt", "Plain Text (.txt)");
	/**
	 * Default format.
	 */
	public final static IdName DEFAULT = new IdName(MSG);
	
	/**
	 * List of all types.
	 */
	public final static IdName[] TYPES = new IdName[] {
			NOTHING, DEFAULT, MSG, MHTML, RTF, TEXT
	};

	/**
	 * File extensions in relation to OlSaveAsType
	 */
	public final static List<String> exts = Arrays.asList(".txt", ".rtf", ".tmp", ".msg", ".doc", ".html", ".vcard",
			".vcal", ".ical", ".msg", ".mhtml");

}
