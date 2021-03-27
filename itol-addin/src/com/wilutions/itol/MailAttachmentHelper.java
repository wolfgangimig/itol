package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.BackgTask;
import com.wilutions.com.Dispatch;
import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.AttachmentBlacklistItem;
import com.wilutions.itol.db.Default;
import com.wilutions.itol.db.IdName;
import com.wilutions.itol.db.Issue;
import com.wilutions.itol.db.MsgFileFormat;
import com.wilutions.itol.db.Profile;
import com.wilutions.itol.db.ProgressCallback;
import com.wilutions.itol.db.ProgressCallbackFactory;
import com.wilutions.mslib.outlook.Application;
import com.wilutions.mslib.outlook.MailItem;
import com.wilutions.mslib.outlook.OlAttachmentType;
import com.wilutions.mslib.outlook.OlBodyFormat;
import com.wilutions.mslib.outlook.OlSaveAsType;
import com.wilutions.mslib.outlook.PropertyAccessor;

import javafx.scene.image.Image;

public class MailAttachmentHelper {

	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private File __tempDir;
	private final static Logger log = Logger.getLogger("MailAttachmentHelper");
	public final static String FILE_URL_PREFIX = "file:/";

	public MailAttachmentHelper() {
	}

	/**
	 * Convert email attachments to issue attachments.
	 * @param mailItem
	 * @param issue
	 * @throws Exception
	 */
	public void initialUpdate(IssueMailItem mailItem, Issue issue) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("initialUpdate(mailItem=" + mailItem + ", issue=" + issue);
		
		// Observed that Outlook/ITOL hangs while updating attachments.
		// The JavaFX thread hung in an OLE call somewhere at mailItem.get...
		// To avoid a deadlock, execute the function in background and wait up to 30s.
				
		long t1 = System.currentTimeMillis();
		Executor executor = BackgTask.getExecutor();
		
		CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
			try {
				initialUpdateBackg(mailItem, issue);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		}, executor);

		future.get(30, TimeUnit.SECONDS);
		long t2 = System.currentTimeMillis();
		log.info("[" + (t2-t1) + "] MailAttachmentHelper.initialUpdate");
		
		if (log.isLoggable(Level.FINE)) log.fine(")initialUpdate");
	}

	private void initialUpdateBackg(IssueMailItem mailItem, Issue issue) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("initialUpdate(mailItem=" + mailItem + ", issue=" + issue);
		releaseResources();

		if (issue != null) {
			boolean isNew = issue.isNew();
			boolean isNewComment = issue.isNewComment();
			if (isNew || isNewComment) {
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

	private void initialUpdateNewIssueAttachments(IssueMailItem mailItem, Issue issue) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("initialUpdateNewIssueAttachments(");
		
		String ext = getConfigMsgFileExt();
		if (log.isLoggable(Level.FINE)) log.fine("ext=" + ext);

		if (!ext.equals(MsgFileFormat.NOTHING.getId())) {
			
			List<Attachment> attachments = new ArrayList<Attachment>(issue.getAttachments());

			// The code below should add mail attachments as issue attachments.
			boolean addAttachments = false; 
			
			// The code below should add the mail as an issue attachment.
			boolean addMail = false;
			
			// Add attachments embedded in the mail body as issue attachments. 
			boolean addEmbeddedAttachments = false;
			
			// Shortcut for option "Only Attachments" 
			boolean addOnlyAttachments = ext.equals(MsgFileFormat.ONLY_ATTACHMENTS.getId());
			if (log.isLoggable(Level.FINE)) log.fine("addOnlyAttachments=" + addOnlyAttachments);

			if (addOnlyAttachments) {
				addAttachments = true;
				addEmbeddedAttachments = true;
				addMail = false;
			}
			else {
				
				// If the mail should be added as MSG (isContainerFormat), it already includes all mail attachments. 
				// Thus, the attachments must not be added to save space on the server.   
				// On the other hand, if the mail body should be converted to JIRA markup, embedded attachments 
				// must be added to the issue explicitly. Otherwise, thumbnails of embedded images are not available.
				
				OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);
				if (log.isLoggable(Level.FINE)) log.fine("saveAsType=" + saveAsType);
				addAttachments = !MsgFileTypes.isContainerFormat(saveAsType);
				addEmbeddedAttachments = true;
				addMail = true;
			}
			
			// The mail body is empty, if this function is called from menu items "New issue" or "New Subtask".
			if (log.isLoggable(Level.FINE)) log.fine("#mail.body=" + mailItem.getBody().length());
			if (mailItem.getBody().isEmpty()) {
				addMail = false;
			}
			
			// Maybe add mail as an issue attachment.
			if (log.isLoggable(Level.FINE)) log.fine("addMail=" + addMail);
			if (addMail) {
				MailAtt mailAtt = new MailAtt(mailItem, ext);
				attachments.add(mailAtt);
			}

			// Maybe add mail attachments as issue attachments
			if (log.isLoggable(Level.FINE)) log.fine("addAttachments=" + addAttachments);
			if (addAttachments || addEmbeddedAttachments) {

				// Embedded RTF attachments have a special format - not PNG, JPG, BMP.
				// To add this attachments to the issue, the code adds the entire mail as RTF file, if it has not been added above.   
				 
				boolean addBodyToIncludeEmbeddedAttachments = !addMail;
				boolean isRTFBody = mailItem.getBodyFormat() == OlBodyFormat.olFormatRichText;
				if (log.isLoggable(Level.FINE)) log.fine("addBodyToIncludeEmbeddedAttachments=" + addBodyToIncludeEmbeddedAttachments + ", isRTFBody=" + isRTFBody);
									
				IssueAttachments mailAtts = mailItem.getAttachments();
				int n = mailAtts.getCount();
				if (log.isLoggable(Level.FINE)) log.fine("add #attachments=" + n);
				
				for (int i = 1; i <= n; i++) {
					
					com.wilutions.mslib.outlook.Attachment matt = mailAtts.getItem(i);
					MailAttAtt attatt = new MailAttAtt(matt);
					if (log.isLoggable(Level.FINE)) log.fine("attachment[" + i + "]=" + attatt);

					// Skip attachments from blacklist.
					boolean isBlacklistAttachment = isBlacklistAttachment(attatt);
					if (log.isLoggable(Level.FINE)) log.fine("isBlacklistAttachment=" + isBlacklistAttachment);
					if (isBlacklistAttachment) continue;

					OlAttachmentType attachmentType = matt.getType();
					
					if (log.isLoggable(Level.FINE)) log.fine("attachmentType=" + attachmentType);

					if (isRTFBody && attachmentType == OlAttachmentType.olOLE) {
						if (addBodyToIncludeEmbeddedAttachments) {
							if (log.isLoggable(Level.FINE)) log.fine("add mail as attachment to include embedded attachments");
							addBodyToIncludeEmbeddedAttachments = false;
							MailAtt mailAtt = new MailAtt(mailItem, MsgFileFormat.RTF.getId());
							attachments.add(mailAtt);
						}
					}
					else {
						if (log.isLoggable(Level.FINE)) log.fine("add attachment");
						attatt.setLastModified(mailItem.getReceivedTime());
						attachments.add(attatt);
					}
				}
			}
			
			issue.setAttachments(attachments);
		}
	
		if (log.isLoggable(Level.FINE)) log.fine(")initialUpdateNewIssueAttachments");
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
						
			PropertyAccessor mattProps = matt.getPropertyAccessor();
			String contentType = (String)matt.getPropertyAccessor().GetProperty(Attachment.OUTLOOK_MAPI_PROPTAG_EMBEDDED_ATTCHMENT_MIME_TYPE);

			String fname = "";
			try {
				fname = matt.getFileName();
				
				// Get advanced attachment properties to find out, whether the attachment is embedded in the mail body.
				// https://social.msdn.microsoft.com/Forums/vstudio/en-US/d6d339d2-ebc3-4332-9801-15a53020df94/embedded-images-attachments-with-html-based-emails?forum=vsto
				String cid = (String)mattProps.GetProperty(Attachment.OUTLOOK_MAPI_PROPTAG_EMBEDDED_ATTCHMENT); 
				boolean isEmbedded = cid != null && !cid.equals("");
				
				// ITJ-100: attachment embedded, if file name is found in CID
				isEmbedded &= cid.contains(fname);
				
				if (isEmbedded) {
					
					// File names of embedded attachments are extracted from Outlook's content ID.
					// In com.wilutions.jiraaddin.markup.MarkupHelper.processIMG, the file name is found in the 
					// content ID before the last @. 
					// Usually, an @ is found in the content ID. But ITJ-60 shows a mail example, where
					// no file name is included. In this case, the entire contentID is used as file name.
					
					int p = cid.lastIndexOf('@');
					if (p < 0) p = cid.length();
					fname = cid.substring(0, p);
					
					// Get file extension from content type.
					if (!fname.contains(".")) {
						String ext = ContentTypes.getFileExt(contentType);
						fname += ext;
					}
				}
			}
			catch (Exception e) {
				// Attachments embedded in a RTF mail body throw an exception here.
				fname = String.valueOf(System.identityHashCode(matt)); 
			}
			
			super.setSubject(fname);
			super.setFileName(fname);
			super.setContentType(contentType);
			super.setContentLength(matt.getSize());
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
					super.setLocalFile(msgFile);
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Failed to save attachment=" + this + " to local file.", e);
				throw e;
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
				log.log(Level.SEVERE, "Failed to get stream for attachment=" + this, e);
			}
			return ret;
		}

	}

	private static String getConfigMsgFileExt() {
		IdName type = Globals.getAppInfo().getConfig().getCurrentProfile().getMsgFileFormat();
		return type.getId();
	}

	public void showAttachmentAsync(Attachment att, ProgressCallbackFactory cbFact, AsyncResult<Boolean> asyncResult) {
		if (log.isLoggable(Level.FINE)) log.fine("showAttachmentAsync(att=" + att);
		
		// Download the entire file into a temp dir. Opening the URL with Desktop.browse()
		// would start a browser first, which in turn downloads the file. 
		
		ProgressCallback cb = cbFact.createProgressCallback("Show selected attachment");
		BackgTask.run(() -> {
			try {
				URI url = downloadAttachment(att, cb); 
				IssueApplication.showDocument(url.toString());
				asyncResult.setAsyncResult(Boolean.TRUE, null);
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Failed to show attachment=" + att, e);
				asyncResult.setAsyncResult(Boolean.FALSE, e);
			}
			finally {
				cb.setFinished();
			}
		});

		if (log.isLoggable(Level.FINE)) log.fine(")showAttachmentAsync");
	}

	public URI downloadAttachment(Attachment att, ProgressCallback cb) throws Exception {
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
			else try {
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
			catch (Exception e) {
				log.log(Level.WARNING, "Failed to provide local file for attachment=" + att, e);
				throw e;
			}
		}
		if (cb != null) cb.setFinished();
		if (log.isLoggable(Level.FINE)) log.fine(")downloadAttachment=" + url);
		return new URI(url);
	}
	
	private boolean compareFiles(File lhs, File rhs, ProgressCallback cb) {
		return compareFilesContents(lhs, rhs, cb);
	}
	
	/**
	 * Compare up to 8000 bytes at the beginning and end of the given files.
	 * @param lhs File 1
	 * @param rhs File 2
	 * @param cb ProgressCallback
	 * @return true, if the first and last 8000 bytes are equal.
	 */
	private boolean compareFilesContents(File lhs, File rhs, ProgressCallback cb) {
		cb.setTotal(1.0);
		boolean ret = lhs.exists() && rhs.exists() && lhs.length() == rhs.length();
		if (ret) {
			final int maxBytes = 8000; // less than default buffer size of BufferedInputStream
			ByteBuffer bbufL = ByteBuffer.allocate(maxBytes);
			ByteBuffer bbufR = ByteBuffer.allocate(maxBytes);
			
			try (
				FileChannel fcL = FileChannel.open(lhs.toPath(), StandardOpenOption.READ); 
				FileChannel fcR = FileChannel.open(rhs.toPath(), StandardOpenOption.READ)
			) {
				// Read up to 8000 bytes from the beginning
				int len = 0;
				do { len = fcL.read(bbufL);	} 
				while (len >= 0 && bbufL.hasRemaining());
				do { len = fcR.read(bbufR);	} 
				while (len >= 0 && bbufR.hasRemaining());
				cb.incrProgress(0.5);
				
				// Compare 
				bbufL.flip(); bbufR.flip();
				ret = bbufL.compareTo(bbufR) == 0;
				if (ret && fcL.size() > maxBytes) {
					
					// Read up to last 8000 bytes.
					
					// Move position to the last 8000 bytes
					long pos = Math.max(maxBytes, fcL.size() - maxBytes);
					fcL.position(pos);
					fcR.position(pos);
					bbufL.clear();
					bbufR.clear();

					// Read last bytes
					do { len = fcL.read(bbufL);	} 
					while (len >= 0 && bbufL.hasRemaining());
					do { len = fcR.read(bbufR);	} 
					while (len >= 0 && bbufR.hasRemaining());
					cb.incrProgress(0.5);
					
					// Compare
					bbufL.flip(); bbufR.flip();
					ret = bbufL.compareTo(bbufR) == 0;
				}
				
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Failed to compare file content, file1=" + lhs + ", file2=" + rhs, e);
				// Assume files are equal. This avoids copying rhs on lhs in exportAttachment.
			}
		}
		cb.setFinished();
		return ret;
	}

//	private byte[] getFileHash(File file, ProgressCallback cb) {
//		byte[] digest = null;
//		cb.setTotal(file.length());
//		try {
//			MessageDigest md = MessageDigest.getInstance("MD5");
//			try (InputStream is = Files.newInputStream(file.toPath());
//			     DigestInputStream dis = new DigestInputStream(is, md)) 
//			{
//				byte[] buf = new byte[10*1000];
//				int len = 0;
//				while ((len = dis.read(buf)) > 0) {
//					cb.incrProgress(len);
//				}
//			}
//			digest = md.digest();
//		}
//		catch (Exception e) {
//			digest = new byte[16];
//		}
//		cb.setFinished();
//		return digest;
//	}

	private File exportAttachment(File dir, Application outlookApplication, Attachment att, ProgressCallback cb) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("exportAttachment(dir=" + dir + ", att=" + att);
		File ret = null;
		try {
			// Download into temp dir.
			ProgressCallback cbDownload = cb.createChild("Download " + att.getFileName(), 0.5);
			URI url = downloadAttachment(att, cbDownload);
			File tempFile = new File(url);
			ProgressCallback cbSave = cb.createChild("Save " + att.getFileName(), 0.5);

			// ITJ-83: Mail attachments nicht mehr in RTF konvertieren.
			
			// Make unique file name
			File destFile = makeUniqueExportFileName(dir, tempFile, cbSave.createChild(0.5));
			if (log.isLoggable(Level.INFO)) log.info("Export issue attachment to destFile=" + destFile);

			// Copy to dest dir.
			if (!destFile.exists()) {
				Files.copy(tempFile.toPath(), destFile.toPath());
				destFile.setLastModified(att.getLastModified().getTime());
			}
			cbSave.incrProgress(0.5);
			
			ret = destFile;
		}
		finally {
			cb.setFinished();
		}
		if (log.isLoggable(Level.FINE)) log.fine(")exportAttachment=" + ret);
		return ret;
	}
	
	/**
	 * Export attachments to export directory.
	 * @param issue Issue
	 * @param selectedItems Attachments to be exported
	 * @param cb ProgressCallback
	 */
	public void exportAttachments(Issue issue, List<Attachment> selectedItems, ProgressCallback cb) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("exportAttachments(" + issue + ", #selectedItems=" + selectedItems.size() );
		
		// Get destination directory
		File exportDirectory = null;
		{
			String exportDirectoryName = Globals.getAppInfo().getConfig().getCurrentProfile().getExportAttachmentsDirectory();
			
			// Build sub-directory: issue ID or NEW-<now>
			String subdir = issue.getId();
			if (subdir.isEmpty()) {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
				subdir = issue.getProject().getId() + "-NEW-" + dateFormat.format(new Date());
			}

			exportDirectory = new File(new File(exportDirectoryName), subdir);
			if (log.isLoggable(Level.FINE)) log.fine("export directory=" + exportDirectory + ", exists=" + exportDirectory.exists());
					
			if (exportDirectory.exists()) {
				if (!exportDirectory.isDirectory()) {
					throw new IllegalStateException("Export destination=" + exportDirectory + " is not a directory.");
				}
			}
			else {
				if (!exportDirectory.mkdirs()) {
					throw new IllegalStateException("Export destination=" + exportDirectory + " cannot be created.");
				}
			}
		}

		// Properties file of exported attachments.
		ExportedAttachmentsPropertiesFile exportedAttachments = new ExportedAttachmentsPropertiesFile(exportDirectory);

		// Prepare progress object: compute total number of bytes to export.
		long totalBytes = selectedItems.stream().collect(Collectors.summingLong((att) -> att.getContentLength())).longValue();
		
		if (log.isLoggable(Level.INFO)) log.info("Export issue=" + issue.getId() + " " + selectedItems.size() + " attachments of totalBytes=" + totalBytes + " to directory=" + exportDirectory);
		
		// Show that export process has started
		cb.incrProgress(0.1); 
		
		// Export
		ProgressCallback cbExportAll = cb.createChild(0.9);
		Application outlookApplication = Globals.getThisAddin().getApplication();
		for (Attachment att : selectedItems) {
			if (cb.isCancelled()) break;

			// Move progress by this fraction.
			double progressRatio = (double)att.getContentLength() / (double)totalBytes;

			// Attachment already exported? lookup file name in .contents file.
			File alreadyExportedFile = exportedAttachments.get(att);
			if (log.isLoggable(Level.FINE)) log.fine("att=" + att + " already exported to=" + alreadyExportedFile);

			if (exportedAttachments.get(att) == null) {
				
				// Export attachment
				try {
					ProgressCallback childProgress = cbExportAll.createChild("Export " + att.getFileName(), progressRatio);
					File exportedFile = exportAttachment(exportDirectory, outlookApplication, att, childProgress);
					exportedAttachments.add(att, exportedFile);
				} catch (Exception e) {
					log.log(Level.WARNING, "Attachment could not be exported.", e);
				}
			}
			else {
				cbExportAll.incrProgress(progressRatio);
			}
		}
		cb.setFinished();

		// Open export directory in Windows Explorer 
		if (!cb.isCancelled()) {
			openExportDirectory(issue, exportDirectory);
		}

		if (log.isLoggable(Level.FINE)) log.fine(")exportAttachments");
	}
	
	/**
	 * Open export directory by configured program.
	 * @param issue
	 * @param exportDirectory
	 * @throws Exception
	 */
	private void openExportDirectory(Issue issue, File exportDirectory) throws Exception {
		if (log.isLoggable(Level.FINE)) log.fine("openExportDirectory(issue=" + issue + ", exportDirectory=" + exportDirectory);
		String exportProgram = Globals.getAppInfo().getConfig().getCurrentProfile().getExportAttachmentsProgram();
		if (Default.value(exportProgram).isEmpty()) {
			String url = exportDirectory.toURI().toString();
			IssueApplication.showDocument(url);
		}
		else {
			String cmd = exportProgram
					.replace(Profile.PLACEHODER_EXPORT_DIRECTORY, exportDirectory.getAbsolutePath())
					.replace(Profile.PLACEHODER_ISSUE_ID, issue.getId())
					.replace(Profile.PLACEHODER_ISSUE_ID, issue.getProject().getId());
			try {
				log.info("Open export directory: " + cmd); 
				Runtime.getRuntime().exec(cmd);
			} catch (Exception e) {
				log.log(Level.WARNING, "Failed to open export directory.", e);
				throw e;
			}

		}
		if (log.isLoggable(Level.FINE)) log.fine(")openExportDirectory");
	}

	/**
	 * This class handles a properties file with exported attachments.
	 */
	private static class ExportedAttachmentsPropertiesFile {
		final static String FILENAME = ".exported-attachments";
		Properties props = new Properties();
		File exportDirectory;
		
		ExportedAttachmentsPropertiesFile(File exportDir) {
			this.exportDirectory = exportDir;
			load();
		}
		
		private synchronized void load() {
			try (InputStream fis = new FileInputStream(new File(exportDirectory, FILENAME))) {
				props.load(fis);
			}
			catch (Exception ignored) {}
		}

		private synchronized void store() {
			try (OutputStream fos = new FileOutputStream(new File(exportDirectory, FILENAME))) {
				props.store(fos, "Exported Issue Attachments");
			}
			catch (Exception ignored) {}
		}
		
		private String getAttachmentId(Attachment att) {
			String id = att.getId();
			if (id.isEmpty()) {
				id = att.getFileName(); // new attachment
			}
			return id;
		}
		
		synchronized void add(Attachment att, File exportFile) {
			if (!exportFile.getParentFile().equals(exportDirectory)) throw new IllegalArgumentException("Export file=" + exportFile + " must be stored in export directory=" + exportDirectory);
			String id = getAttachmentId(att);
			props.setProperty(id, exportFile.getName());
			store();
		}
		
		synchronized File get(Attachment att) {
			File ret = null;
			String id = getAttachmentId(att);
			String fname = props.getProperty(id);
			if (!Default.value(fname).isEmpty()) {
				ret = new File(exportDirectory, fname);
			}
			return ret;
		}
	}

	/**
	 * Load file into Outlook MailItem object.
	 * @param outlookApplication Outlook application object.
	 * @param tempFile MSG file
	 * @return MailItem object
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
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
				mailItem = new IssueMailItemImpl(mailItemDisp, null);
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
	
	private File makeUniqueExportFileName(File dir, File tempFile, ProgressCallback cb) {
		File destFile = tempFile;
		if (!tempFile.getParentFile().equals(dir)) { // should always un-equal: export directory should not be the same as the temp directory.
			for (int retries = 0; retries < 1000; retries++) {
				destFile = makeTempFile(dir, tempFile.getName(), retries);
				if (compareFiles(tempFile, destFile, cb)) {
					break;
				}
				if (!destFile.exists()) {
					break;
				}
			}
		}
		cb.setFinished();
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

	/**
	 * Return thumbnail image of attachment.
	 * Downloads the image if thumbnailUrl is not empty from the sever. 
	 * @param attachment
	 * @param cb
	 * @return Thumbnail image or null, if there is no thumbnail available.
	 */
	public static Image getThumbnailImage(Attachment attachment, ProgressCallback cb) {
		if (log.isLoggable(Level.FINE)) log.fine("getThumbnailImage(" + attachment);
		Image ret = attachment.getThumbnailImage();
		if (ret == null) {
			String thumbnailUrl = attachment.getThumbnailUrl();
			if (log.isLoggable(Level.FINE)) log.fine("thumbnailUrl=" + thumbnailUrl);
			try {
				if (Default.value(thumbnailUrl).isEmpty()) {
					if (attachment.getId().isEmpty()) {
						if (attachment.getLocalFile() != null) {
							cb.incrProgress(0.2);
							File thumbnailFile = ThumbnailHelper.makeThumbnail(attachment.getLocalFile());
							if (thumbnailFile != null) {
								attachment.setThumbnailUrl(thumbnailFile.toURI().toString());
								if (log.isLoggable(Level.FINE)) log.fine("thumbnailUrl=" + attachment.getThumbnailUrl());
								ret = new Image(attachment.getThumbnailUrl());
							}
							cb.setFinished();
						}
					}
				}
				else {
					// Use download function since Image constructor does not follow redirections.
					if (!thumbnailUrl.startsWith(MailAttachmentHelper.FILE_URL_PREFIX)) {
						String fpath = Globals.getIssueService().downloadAttachment(thumbnailUrl, cb);
						File thumbnailFile = new File(fpath);
						thumbnailUrl = thumbnailFile.toURI().toString();
						attachment.setThumbnailUrl(thumbnailUrl);
						if (log.isLoggable(Level.FINE)) log.fine("thumbnailUrl=" + attachment.getThumbnailUrl());
					}
					Image image = new Image(thumbnailUrl);
					attachment.setThumbnailImage(image);
				}
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Failed to download thumbnail=" + thumbnailUrl + " for attachment=" + attachment, e);
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(")getThumbnailImage=" + ret);
		return ret;
	}

	public static String getFileChecksum(File file) throws Exception {
		byte[] b = Files.readAllBytes(Paths.get(file.toURI()));
		byte[] hash = MessageDigest.getInstance("MD5").digest(b);
		String ret = DatatypeConverter.printHexBinary(hash);
		return ret;
	}

	private boolean isBlacklistAttachment(Attachment att) throws Exception {
		boolean ret = false;
		long size = att.getContentLength();
		for (AttachmentBlacklistItem blackItem : Globals.getAppInfo().getConfig().getCurrentProfile().getBlacklist()) {
			
			// For performance reasons, check the size first before saving the attachment and 
			// computing the MD5 hash. 
			// Since the file size returned from Outlook is a bit larger than the real file size,
			// we cannot check of equal size. We have to add a tolerance.
			
			if (Math.abs(blackItem.getSize() - size) < 10000) {
				
				att.getStream().close(); // save attachment to local file
				String hash = MailAttachmentHelper.getFileChecksum(att.getLocalFile());
				ret = hash.equals(blackItem.getHash());
				if (ret) break;
			}
		}
		return ret;
	}

	public static void addBlacklistItem(String name, File file) throws Exception {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "addBlacklistItem(" + file);
		String hash = MailAttachmentHelper.getFileChecksum(file);
		AttachmentBlacklistItem item = new AttachmentBlacklistItem(name, file.length(), hash);
		if (log.isLoggable(Level.INFO)) log.info("Add blacklist item=" + item);
		
		boolean found = false;
		for (AttachmentBlacklistItem blackItem : Globals.getAppInfo().getConfig().getCurrentProfile().getBlacklist()) {
			found = blackItem.getHash().equals(hash);
			if (found) break;
		}
		
		if (!found) {
			Globals.getAppInfo().getConfig().getCurrentProfile().getBlacklist().add(item);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")addBlacklistItem");
	}
}
