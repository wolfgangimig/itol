/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.wilutions.com.IDispatch;
import com.wilutions.mslib.outlook.Attachment;
import com.wilutions.mslib.outlook.Attachments;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlSaveAsType;

public class MailExport {

	private final File tempDir;
	private final Random rand = new Random();

	public MailExport() {
		tempDir = new File(System.getProperty("java.io.tmpdir"), "itol");
		tempDir.mkdirs();
	}

	public String export(MailItem mailItem) throws Exception {

		long id = rand.nextLong();
		File itemDir = new File(tempDir, Long.toHexString(id));
		itemDir.mkdirs();
		
		// Save mail body (inclusive embedded images)
		File itemFile = new File(itemDir, "index.html");
		mailItem.SaveAs(itemFile.getAbsolutePath(), OlSaveAsType.olHTML);

		// Compute the checksums for the embedded files.
		// Images pasted into the mail body are also
		// included in the attachments collection.
		// This images are saved with the mail items and
		// should not be stored as attachments again.
		// This is why the checksums are computed for
		// referenced files. An attachment with a known
		// checksum will not be stored later.
		Set<String> refsChecksums = getRefsChecksums(itemDir);

		// Save attachments into directory "./attachments"
		File attDir = new File(itemDir, "attachments");
		attDir.mkdirs();
		Attachments atts = mailItem.getAttachments();
		int nbOfAtts = atts.getCount();
		for (int i = 1; i <= nbOfAtts; i++) {
			IDispatch dispAtt = atts.Item(i);
			Attachment att = dispAtt.as(Attachment.class);

			String fname = att.getFileName();
			File attFile = new File(attDir, fname);
			att.SaveAsFile(attFile.getAbsolutePath());

			// Is attachment embedded in mail body?
			String attChecksum = MailAttachmentHelper.getFileChecksum(attFile);
			if (refsChecksums.contains(attChecksum)) {
				attFile.delete();
			}
		}

		return itemDir.getAbsolutePath();
	}

	private Set<String> getRefsChecksums(File itemDir) throws Exception {
		File[] itemRefs = null;
		File[] itemFiles = itemDir.listFiles();
		File itemRefsDir = null;
		for (File f : itemFiles) {
			if (f.isDirectory()) {
				itemRefsDir = f;
				break;
			}
		}
		itemRefs = itemRefsDir != null ? itemRefsDir.listFiles() : new File[0];
		Set<String> ret = new HashSet<String>(itemRefs.length);
		for (File f : itemRefs) {
			String hash = MailAttachmentHelper.getFileChecksum(f);
			ret.add(hash);
		}
		return ret;
	}
}
