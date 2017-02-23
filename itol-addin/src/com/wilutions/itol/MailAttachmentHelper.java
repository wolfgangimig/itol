package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.com.Dispatch;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.Default;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.MsgFileFormat;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.Property;
import com.wilutions.mslib.outlook.Application;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlSaveAsType;

public class MailAttachmentHelper {

	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private File __tempDir;
	private final static Logger log = Logger.getLogger("MailAttachmentHelper");
	public final static String FILE_URL_PREFIX = "file:/";

	public MailAttachmentHelper() {
	}

	public void initialUpdate(IssueMailItem mailItem, Issue issue) throws IOException {
		if (log.isLoggable(Level.FINE)) log.fine("initialUpdate(mailItem=" + mailItem + ", issue=" + issue);
		releaseResources();

		if (issue != null) {
			boolean isNew = issue.getId().isEmpty();
			String newNotes = issue.getPropertyString(Property.NOTES, "");
			if (isNew || newNotes.length() != 0) {
				initialUpdateNewIssueAttachments(mailItem, issue);
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(")initialUpdate");
	}

	private File getTempDir() {
		if (__tempDir == null) {
			__tempDir = new File(Globals.getTempDir(), Long.toString(System.currentTimeMillis()));
			__tempDir.mkdirs();
		}
		return __tempDir;
	}

	private void initialUpdateNewIssueAttachments(IssueMailItem mailItem, Issue issue) throws IOException {

		if (mailItem.getBody().length() != 0) {
			String ext = getConfigMsgFileExt();

			if (!ext.equals(MsgFileFormat.NOTHING.getId())) {
				
				List<Attachment> attachments = new ArrayList<Attachment>(issue.getAttachments());

				boolean addAttachments = false; 
				boolean addMail = false;
				if (ext.equals(MsgFileFormat.ONL_ATTACHMENTS.getId())) {
					addAttachments = true;
					addMail = false;
				}
				else {
					OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);
					addAttachments = !MsgFileTypes.isContainerFormat(saveAsType);
					addMail = true;
				}

				if (addMail) {
					MailAtt mailAtt = new MailAtt(mailItem, ext);
					attachments.add(mailAtt);
				}

				if (addAttachments) {
					IssueAttachments mailAtts = mailItem.getAttachments();
					int n = mailAtts.getCount();
					for (int i = 1; i <= n; i++) {
						com.wilutions.mslib.outlook.Attachment matt = mailAtts.getItem(i);
						MailAttAtt attatt = new MailAttAtt(matt);
						attatt.setLastModified(mailItem.getReceivedTime());
						attachments.add(attatt);
					}
				}
				
				issue.setAttachments(attachments);
			}
		}
	}

	public Attachment makeMailAttachment(IssueMailItem mailItem) throws IOException {
		String ext = getConfigMsgFileExt();
		MailAtt mailAtt = new MailAtt(mailItem, ext);
		return mailAtt;
	}

	public void releaseResources() {
		for (Runnable run : resourcesToRelease) {
			try {
				run.run();
			}
			catch (Throwable ignored) {
			}
		}
		if (__tempDir != null) {
			__tempDir.delete();
			__tempDir = null;
		}
	}

	public static Attachment createFromFile(File file) {
		return new FileAtt(file);
	}

	public static String getFileName(String path) {
		String fname = path;
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf(File.separatorChar);
			fname = path.substring(p + 1);
		}
		return fname;
	}

	public static String getFileNameWithoutExt(String path) {
		String fname = path;
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf(File.separatorChar);
			fname = path.substring(p + 1);
			p = fname.lastIndexOf('.');
			if (p >= 0) {
				fname = fname.substring(0, p);
			}
		}
		return fname;
	}

	public static String getFileExt(String path) {
		String ext = "";
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf('.');
			if (p >= 0) {
				ext = path.substring(p + 1).toLowerCase();
			}
		}
		return ext;
	}

	public static String getFileContentType(File file) {
		String contentType = "";
		try {
			contentType = Files.probeContentType(file.toPath());
		} catch (IOException ignore) {}
			
		if (contentType == null || contentType.isEmpty()) {
			String fname = file.getName();
			String ext = ".";
			int p = fname.lastIndexOf('.');
			if (p >= 0) {
				ext = fname.substring(p);
			}
			contentType = ContentTypes.getContentType(ext.toLowerCase());
		}
		return contentType;
	}

	public static String makeAttachmentSizeString(long contentLength) {
		String ret = "";
		if (contentLength >= 0) {
			final String[] dims = new String[] { "Bytes", "KB", "MB", "GB", "TB" };
			int dimIdx = 0;
			long c = contentLength, nb = 0;
			for (int i = 0; i < dims.length; i++) {
				nb = c;
				c = (long) Math.floor(c / 1000);
				if (c == 0) {
					dimIdx = i;
					break;
				}
			}
			ret = nb + " " + dims[dimIdx];
		}
		return ret;
	}

	/**
	 * Issue attachment for mail attachment.
	 */
	public class MailAttAtt extends Attachment {

		private final com.wilutions.mslib.outlook.Attachment matt;
		private final File dir = getTempDir();

		private MailAttAtt(com.wilutions.mslib.outlook.Attachment matt) {
			this.matt = matt;
			super.setSubject(matt.getFileName());
			super.setContentType(getFileContentType(new File(getTempDir(), matt.getFileName())));
			super.setContentLength(matt.getSize());
			super.setFileName(matt.getFileName());
			super.setLastModified(new Date());
		}

		@Override
		public InputStream getStream() {
			File f = save();
			try {
				return new FileInputStream(f);
			}
			catch (FileNotFoundException e) {
				return null;
			}
		}

		@Override
		public String getUrl() {
			save();
			return super.getUrl();
		}

		private File save() {
			if (getLocalFile() == null) {
				final File mattFile = new File(dir, getFileName());
				super.setLocalFile(mattFile);
				resourcesToRelease.add(() -> mattFile.delete());
				log.info("Save attachment to " + mattFile);
				matt.SaveAsFile(mattFile.getAbsolutePath());
				super.setContentLength(mattFile.length());
				super.setUrl(mattFile.toURI().toString());
			}
			return getLocalFile();
		}
		
		public String getThumbnailUrl() {
			String thurl = super.getThumbnailUrl();
			if (Default.isEmpty(thurl)) {
				if (!ThumbnailHelper.getImageFileType(new File(getFileName())).isEmpty()) {
					save();
					File thumbnailFile = ThumbnailHelper.makeThumbnail(getLocalFile());
					thurl = thumbnailFile != null ? thumbnailFile.toURI().toString() : "";
					setThumbnailUrl(thurl);
				}
			}
			return thurl;
		}
	}

	/**
	 * Issue attachment for mail. Must be public, otherwise it cannot be viewed
	 * in the table.
	 */
	public class MailAtt extends Attachment {

		private final IssueMailItem mailItem;
		private final String ext;
		private final File dir = getTempDir();

		private MailAtt(IssueMailItem mailItem, String ext) {
			this.mailItem = mailItem;
			this.ext = ext;

			setSubject(mailItem.getSubject());
			setContentLength(-1);
			setLastModified(mailItem.getReceivedTime());
		}

		@Override
		public void setSubject(String subject) {

			OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);
			String msgFileName = MsgFileTypes.makeMsgFileName(subject, saveAsType);

			super.setSubject(subject);
			super.setContentType(getFileContentType(new File(dir, msgFileName)));
			super.setContentLength(-1);
			super.setFileName(msgFileName);

		}

		@Override
		public InputStream getStream() {
			File f = save();
			try {
				return new FileInputStream(f);
			}
			catch (FileNotFoundException e) {
				return null;
			}
		}

		@Override
		public String getUrl() {
			save();
			return super.getUrl();
		}

		@Override
		public long getContentLength() {
			return super.getContentLength();
		}

		private File save() {

			final File msgFile = new File(dir, getFileName());
			try {

				if (getContentLength() < 0) {

					OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);

					resourcesToRelease.add(() -> msgFile.delete());

					long t1 = System.currentTimeMillis();
					mailItem.SaveAs(msgFile.getAbsolutePath(), saveAsType);
					long t2 = System.currentTimeMillis();
					log.info("[" + (t2-t1) + "] Save mail to " + msgFile);

					super.setContentLength(msgFile.length());
					super.setUrl(msgFile.toURI().toString());
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			return msgFile;
		}
	}

	/**
	 * Issue attachment for file from filesystem.
	 */
	public static class FileAtt extends Attachment {

		private FileAtt(File file) {
			super.setSubject(file.getName());
			super.setFileName(file.getName());
			super.setUrl(file.toURI().toString());
			super.setLocalFile(file);
			super.setContentLength(file.length());
			super.setContentType(getFileContentType(file));
			File thumbnailFile = ThumbnailHelper.makeThumbnail(file);
			String thurl = thumbnailFile != null ? thumbnailFile.toURI().toString() : "";
			super.setThumbnailUrl(thurl);
			super.setLastModified(new Date(file.lastModified()));
		}

		@Override
		public InputStream getStream() {
			InputStream ret = null;
			try {
				ret = new FileInputStream(getLocalFile());
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return ret;
		}

	}

	private static String getConfigMsgFileExt() {
		IdName type = Globals.getAppInfo().getConfig().getMsgFileFormat();
		return type.getId();
	}

	public void showAttachment(Attachment att, ProgressCallback cb) throws Exception {
		// Download the entire file into a temp dir. Opening the URL with Desktop.browse()
		// would start a browser first, which in turn downloads the file. 
		String url = downloadAttachment(att, cb); 
		IssueApplication.showDocument(url);
	}

	public String downloadAttachment(Attachment att, ProgressCallback cb) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("downloadAttachment(att=" + att);
		String url = att.getUrl();
		// Local file added from file system (new file, not uploaded)
		if (url.startsWith(FILE_URL_PREFIX)) {
			//;
		}
		else {
			// Already downloaded attachment?
			if (att.getLocalFile() != null && att.getLocalFile().exists()) {
				if (log.isLoggable(Level.FINE)) log.fine("already downloaded file=" + att.getLocalFile());
				url = att.getLocalFile().toURI().toString();
			}
			else {
				if (log.isLoggable(Level.FINE)) log.fine("download from url=" + url);
				String fileName = Globals.getIssueService().downloadAttachment(url, cb);
				File srcFile = new File(fileName);
				if (log.isLoggable(Level.FINE)) log.fine("received file=" + srcFile);
				if (srcFile.exists()) {
					for (int retries = 0; retries < 100; retries++) {
						File destFile = makeTempFile(getTempDir(), att.getFileName(), retries);
						destFile.delete();
						boolean succ = srcFile.renameTo(destFile);
						if (log.isLoggable(Level.FINE)) log.fine("move to=" + destFile + ", succ=" + succ);
						if (succ) {
							att.setLocalFile(destFile);
							fileName = destFile.getAbsolutePath();
							url = destFile.toURI().toString();
							break;
						}
					}
				}
			}
		}
		if (cb != null) cb.setFinished();
		if (log.isLoggable(Level.FINE)) log.fine(")downloadAttachment=" + url);
		return url;
	}
	
	private boolean compareFiles(File lhs, File rhs) {
		if (!lhs.exists()) return false;
		if (!rhs.exists()) return false;
		if (lhs.length() != rhs.length()) return false;
		byte[] lhsHash = getFileHash(lhs);
		byte[] rhsHash = getFileHash(rhs);
		return Arrays.equals(lhsHash, rhsHash);
	}
	
	private byte[] getFileHash(File file) {
		byte[] digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			try (InputStream is = Files.newInputStream(file.toPath());
			     DigestInputStream dis = new DigestInputStream(is, md)) 
			{
				byte[] buf = new byte[10*1000];
				while (dis.read(buf) > 0) {}
			}
			digest = md.digest();
		}
		catch (Exception e) {
			digest = new byte[16];
		}
		return digest;
	}

	public String exportAttachment(File dir, Application outlookApplication, Attachment att, ProgressCallback cb) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("exportAttachment(dir=" + dir + ", att=" + att);
		String url = "";
		try {
			// Download into temp dir.
			url = downloadAttachment(att, cb);
			File tempFile = new File(new URI(url));

			// Is the issue attachment a mail?
			boolean attachmentIsMail = tempFile.getName().toLowerCase().endsWith(MsgFileFormat.MSG.getId()); 
			if (log.isLoggable(Level.FINE)) log.fine("attachmentIsMail=" + attachmentIsMail);
			if (attachmentIsMail) {
				try {
					// Load mail into Outlook.MailItem object
					IssueMailItem mailItem = loadMailItem(outlookApplication, tempFile);
					
					// Export mail as RTF 
					{
						MailAtt matt = new MailAtt(mailItem, MsgFileFormat.RTF.getId());
						File destFile = makeUniqueExportFileName(dir, new File(dir, matt.getFileName()));
						if (log.isLoggable(Level.INFO)) log.info("Export mail to destFile=" + destFile);
						mailItem.SaveAs(destFile.getAbsolutePath(), OlSaveAsType.olRTF);
						destFile.setLastModified(mailItem.getReceivedTime().getTime());
					}
					
					// Export mail attachments
					IssueAttachments mailAtts = mailItem.getAttachments();
					int n = mailAtts.getCount();
					for (int i = 1; i <= n; i++) {
						com.wilutions.mslib.outlook.Attachment matt = mailAtts.getItem(i);
						File destFile = makeUniqueExportFileName(dir, new File(dir, matt.getFileName()));
						if (log.isLoggable(Level.INFO)) log.info("Export mail attachment to destFile=" + destFile);
						matt.SaveAsFile(destFile.getAbsolutePath());
						destFile.setLastModified(mailItem.getReceivedTime().getTime());
					}
					
				}
				catch (Exception e) {
					log.log(Level.WARNING, "Failed to export mail " + tempFile, e);
				}
			}
			else {
				// Make unique file name
				File destFile = makeUniqueExportFileName(dir, tempFile);
				if (log.isLoggable(Level.INFO)) log.info("Export issue attachment to destFile=" + destFile);

				// Copy to dest dir.
				if (!destFile.exists()) {
					Files.copy(tempFile.toPath(), destFile.toPath());
					destFile.setLastModified(att.getLastModified().getTime());
				}
			}
		}
		finally {
			if (cb != null) cb.setFinished();
		}
		if (log.isLoggable(Level.FINE)) log.fine(")exportAttachment=" + url);
		return url;
	}

	/**
	 * Load file into Outlook MailItem object.
	 * @param outlookApplication Outlook application object.
	 * @param tempFile MSG file
	 * @return MailItem object
	 * @throws Exception
	 */
	private IssueMailItem loadMailItem(Application outlookApplication, File tempFile) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("loadMailItem(" + tempFile);
		IssueMailItem mailItem = null;
		MailItem mailItemDisp = null;
		File mailFile = tempFile;
		int maxRetries = 100;
		
		// We need a retry loop, because the MSG file could already be opened. 
		for (int retries = 0; retries < maxRetries; retries++) {
			try {
				// Create MailItem object from file.
				if (log.isLoggable(Level.FINE)) log.fine("OpenSharedItem(" + tempFile + ")");
				mailItemDisp = Dispatch.as(outlookApplication.getSession().OpenSharedItem(mailFile.getAbsolutePath()), MailItem.class);
				mailItem = new IssueMailItemImpl(mailItemDisp);
				break;
			}
			catch (Exception e) {
				if (log.isLoggable(Level.FINE)) log.fine("failed: " + e);
				if (retries == maxRetries-1) throw e;
				
				// Copy MSG file to an unique file.
				for (; retries < maxRetries && mailFile.exists(); retries++) {
					mailFile = makeTempFile(tempFile.getParentFile(), tempFile.getName(), retries);
				}
				final File fmailFile = mailFile;
				Files.copy(tempFile.toPath(), fmailFile.toPath());
				resourcesToRelease.add(() -> fmailFile.delete());
				
				// for-loop: try to open the copied MSG file. 
			}
		}
		
		if (log.isLoggable(Level.FINE)) log.fine(")loadMailItem=" + mailItem);
		return mailItem;
	}
	
	private File makeUniqueExportFileName(File dir, File tempFile) {
		File destFile = new File(dir, tempFile.getName());
		if (destFile.exists()) {
			for (int retries = 0; retries < 100 && destFile.exists() && !compareFiles(tempFile, destFile); retries++) {
				destFile = makeTempFile(dir, tempFile.getName(), retries);
			}
		}
		return destFile;
	}

	private File makeTempFile(File tempDir, String fname, int retries) {
		if (retries != 0) {
			String unique = Integer.toString(retries);
			int p = fname.lastIndexOf('.');
			if (p >= 0) {
				fname = fname.substring(0, p) + "_" + unique + fname.substring(p);
			}
		}
		File destFile = new File(tempDir, fname);
		return destFile;
	}
	

}
