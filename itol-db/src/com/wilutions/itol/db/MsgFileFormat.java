package com.wilutions.itol.db;

import java.util.Arrays;
import java.util.Collections;
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
	 * Nobody would use this proprietary Microsoft format:
	 */
	//public final static IdName MHTML = new IdName(".mhtml", "MIME HTML (.mhtml)");
	/**
	 * Attach mail as RTF.
	 */
	public final static IdName RTF = new IdName(".rtf", "Rich Text Format (.rtf)");
	/**
	 * Attach mail as plain text.
	 */
	public final static IdName TEXT = new IdName(".txt", "Plain Text (.txt)");
	/**
	 * Attach only mail attachments, not the mail itself.
	 */
	public final static IdName ONLY_ATTACHMENTS = new IdName(".attachments_09efeb7a-9eb6-4631-a25f-57a45de7f0da", "Only attachments");
	/**
	 * Default format.
	 */
	public final static IdName DEFAULT = new IdName(MSG);
	
	/**
	 * List of all types.
	 */
	public final static List<IdName> FORMATS = Collections.unmodifiableList(Arrays.asList(new IdName[] { NOTHING, MSG, RTF, TEXT, ONLY_ATTACHMENTS }));

	/**
	 * File extensions in relation to OlSaveAsType
	 */
	public final static List<String> exts = Collections.unmodifiableList(Arrays.asList(".txt", ".rtf", ".tmp", ".msg", ".doc", ".html", ".vcard",
			".vcal", ".ical", ".msg", ".mhtml"));

}
