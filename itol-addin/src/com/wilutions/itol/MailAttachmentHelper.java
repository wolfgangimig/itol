package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.Property;
import com.wilutions.mslib.outlook.OlSaveAsType;

public class MailAttachmentHelper {

	private IssueMailItem mailItem;
	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private File __tempDir;
	private final static Logger log = Logger.getLogger("MailAttachmentHelper");
	public final static String FILE_URL_PREFIX = "file:/";

	public MailAttachmentHelper() {
	}

	public void initialUpdate(IssueMailItem mailItem, Issue issue) throws IOException {
		if (log.isLoggable(Level.FINE)) log.fine("initialUpdate(mailItem=" + mailItem + ", issue=" + issue);
		releaseResources();

		this.mailItem = mailItem;

		if (issue != null) {
			boolean isNew = issue.getId().isEmpty();
			String newNotes = issue.getPropertyString(Property.NOTES, "");
			if (isNew || newNotes.length() != 0) {
				initialUpdateNewIssueAttachments(issue);
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

	private void initialUpdateNewIssueAttachments(Issue issue) throws IOException {

		if (mailItem.getBody().length() != 0) {
			String ext = getConfigMsgFileExt();

			if (!ext.equals(MsgFileTypes.NOTHING.getId())) {
				
				List<Attachment> attachments = new ArrayList<Attachment>();

				MailAtt mailAtt = new MailAtt(mailItem, ext);
				attachments.add(mailAtt);

				OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);

				if (!MsgFileTypes.isContainerFormat(saveAsType)) {
					IssueAttachments mailAtts = mailItem.getAttachments();
					int n = mailAtts.getCount();
					for (int i = 1; i <= n; i++) {
						com.wilutions.mslib.outlook.Attachment matt = mailAtts.getItem(i);
						MailAttAtt attatt = new MailAttAtt(matt);
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
			final File mattFile = new File(dir, getFileName());
			if (!mattFile.exists()) {
				resourcesToRelease.add(() -> mattFile.delete());
				log.info("Save attachment to " + mattFile);
				matt.SaveAsFile(mattFile.getAbsolutePath());
				super.setContentLength(mattFile.length());
				super.setUrl(mattFile.toURI().toString());
			}
			return mattFile;
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
	 * Issue attachment for arbitrary file.
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
		IdName type = Globals.getAppInfo().getMsgFileType();
		return type.getId();
	}

	public void showAttachment(Attachment att, ProgressCallback cb) throws Exception {
		String url = downloadAttachment(att, cb);
		IssueApplication.showDocument(url);
	}

	public String downloadAttachment(Attachment att, ProgressCallback cb) throws Exception {
		String url = att.getUrl();
		// Local file added from file system (new file, not uploaded)
		if (url.startsWith(FILE_URL_PREFIX)) {
			//;
		}
		else {
			// Already downloaded attachment?
			if (att.getLocalFile() != null && att.getLocalFile().exists()) {
				url = att.getLocalFile().toURI().toString();
			}
			else {
				String fileName = Globals.getIssueService().downloadAttachment(url, cb);
				File srcFile = new File(fileName);
				if (srcFile.exists()) {
					for (int retries = 0; retries < 100; retries++) {
						File destFile = makeTempFile(getTempDir(), att.getFileName(), retries);
						destFile.delete();
						if (srcFile.renameTo(destFile)) {
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
		return url;
	}

	public String exportAttachment(File dir, Attachment att, ProgressCallback cb) throws Exception {
		String url = "";
		try {
			url = downloadAttachment(att, cb);
			File attFile = new File(new URI(url));
			File destFile = new File(dir, attFile.getName());
			if (!destFile.exists()) {
				Files.copy(attFile.toPath(), destFile.toPath());
			}
		}
		finally {
			if (cb != null) cb.setFinished();
		}
		return url;
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
