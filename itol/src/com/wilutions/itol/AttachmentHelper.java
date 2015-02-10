package com.wilutions.itol;

import java.io.File;

import com.wilutions.itol.db.Attachment;

public class AttachmentHelper {

	public static Attachment createFromFile(File file) {
		Attachment att = new Attachment();
		att.setFileName(getFileName(file.getAbsolutePath()));
		att.setContentLength(file.length());
		String url = file.getAbsolutePath();
		url = url.replace("\\", "/");
		att.setUrl("file:///" + url);
		return att;
	}

	public static String getFileName(String path) {
		String fname = path;
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf('.');
			if (p >= 0) {
				fname = path.substring(0, p).toLowerCase();
			}
			p = path.lastIndexOf(File.separatorChar);
			fname = path.substring(p+1);
		}
		return fname;
	}
	
	public static String getFileExt(String path) {
		String ext = "";
		if (path != null && path.length() != 0) {
			int p = path.lastIndexOf('.');
			if (p >= 0) {
				ext = path.substring(p+1).toLowerCase();
			}
		}
		return ext;
	}
	
	public static String makeAttachmentSizeString(long contentLength) {
		String[] dims = new String[] {"Bytes", "KB", "MB", "GB", "TB"};
		int dimIdx = 0;
		long c = contentLength, nb = 0;
		for (int i = 0; i < dims.length; i++) {
			nb = c;
			c = (long)Math.floor(c / 1000);
			if (c == 0) {
				dimIdx = i;
				break;
			}
		}
		return nb + " " + dims[dimIdx];
	}

}
