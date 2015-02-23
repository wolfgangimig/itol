package com.wilutions.itol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.wilutions.itol.db.Attachment;
import com.wilutions.itol.db.Issue;
import com.wilutions.mslib.outlook.OlSaveAsType;

public class AttachmentHelper {

	private IssueMailItem mailItem;
	private List<Runnable> resourcesToRelease = new ArrayList<Runnable>();
	private File __tempDir;

	public AttachmentHelper() {
	}

	public void initialUpdate(IssueMailItem mailItem, Issue issue) throws IOException {
		releaseResources();

		this.mailItem = mailItem;

		if (issue != null && (issue.getId() == null || issue.getId().length() == 0)) {
			initialUpdateNewIssue(issue);
		}
	}

	private File getTempDir() {
		if (__tempDir == null) {
			__tempDir = new File(Globals.getTempDir(), Long.toString(System.currentTimeMillis()));
			__tempDir.mkdirs();
		}
		return __tempDir;
	}

	private void initialUpdateNewIssue(Issue issue) throws IOException {

		if (mailItem.getBody().length() != 0) {

			MailAtt mailAtt = new MailAtt(mailItem);
			issue.getAttachments().add(mailAtt);

			String ext = Globals.getIssueService().getMsgFileType();
			OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);

			if (!MsgFileTypes.isContainerFormat(saveAsType)) {
				IssueAttachments mailAtts = mailItem.getAttachments();
				int n = mailAtts.getCount();
				for (int i = 1; i <= n; i++) {
					com.wilutions.mslib.outlook.Attachment matt = mailAtts.getItem(i);
					MailAttAtt attatt = new MailAttAtt(matt);
					issue.getAttachments().add(attatt);
				}
			}
		}
	}

	public void releaseResources() {
		for (Runnable run : resourcesToRelease) {
			try {
				run.run();
			} catch (Throwable ignored) {
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

	public static String getFileUrl(File file) {
		String url = file.getAbsolutePath();
		url = url.replace("\\", "/");
		return "file:///" + url;
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

	public static String getFileContentType(String fname) {
		String ext = ".";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			ext = fname.substring(p);
		}
		return ContentTypes.getContentType(ext.toLowerCase());
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

		private MailAttAtt(com.wilutions.mslib.outlook.Attachment matt) {
			this.matt = matt;
			File mattFile = new File(getTempDir(), matt.getFileName());
			super.setSubject(matt.getFileName());
			super.setContentType(getFileContentType(mattFile.getName()));
			super.setContentLength(matt.getSize());
			super.setFileName(mattFile.getAbsolutePath());
		}

		@Override
		public InputStream getStream() {
			File f = save();
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		@Override
		public String getUrl() {
			save();
			return super.getUrl();
		}

		private File save() {
			final File mattFile = new File(getFileName());
			if (!mattFile.exists()) {
				resourcesToRelease.add(() -> mattFile.delete());
				System.out.println("save attachment to " + mattFile);
				matt.SaveAsFile(mattFile.getAbsolutePath());
				super.setContentLength(mattFile.length());
				super.setUrl(getFileUrl(mattFile));
			}
			return mattFile;
		}
	}

	/**
	 * Issue attachment for mail.
	 */
	public class MailAtt extends Attachment {

		private final IssueMailItem mailItem;

		private MailAtt(IssueMailItem mailItem) throws IOException {
			this.mailItem = mailItem;

			String ext = Globals.getIssueService().getMsgFileType();
			OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);
			String msgFileName = MsgFileTypes.makeMsgFileName(mailItem.getSubject(), saveAsType);
			File msgFile = new File(getTempDir(), msgFileName);

			super.setSubject(mailItem.getSubject());
			super.setContentType(getFileContentType(msgFileName));
			super.setContentLength(-1);
			super.setFileName(msgFile.getAbsolutePath());
		}

		@Override
		public InputStream getStream() {
			File f = save();
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
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

			final File msgFile = new File(getFileName());
			try {

				if (getContentLength() < 0) {

					String ext = Globals.getIssueService().getMsgFileType();
					OlSaveAsType saveAsType = MsgFileTypes.getMsgFileType(ext);

					resourcesToRelease.add(() -> msgFile.delete());

					System.out.println("save mail to " + msgFile);
					mailItem.SaveAs(msgFile.getAbsolutePath(), saveAsType);

					super.setContentLength(msgFile.length());
					super.setUrl(getFileUrl(msgFile));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			return msgFile;
		}
	}

	/**
	 * Issue attachment for arbitrary file.
	 */
	public static class FileAtt extends Attachment {
		private File file;

		private FileAtt(File file) {
			this.file = file;
			super.setFileName(AttachmentHelper.getFileName(file.getAbsolutePath()));
			super.setContentLength(file.length());
			String url = getFileUrl(file);
			super.setUrl(url);
		}

		@Override
		public InputStream getStream() {
			InputStream ret = null;
			try {
				ret = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return ret;
		}

	}
}
