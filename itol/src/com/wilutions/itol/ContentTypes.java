package com.wilutions.itol;

import java.util.HashMap;

import com.wilutions.com.reg.RegUtil;

public class ContentTypes {

	private static final String CONTENT_TYPE_MSG = "application/vnd.ms-outlook";
	private static final String CONTENT_TYPE_TXT = "text/plain";
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

	private static final HashMap<String, String> contentTypeToFileExt = new HashMap<String, String>();
	private static final HashMap<String, String> fileExtToContentType = new HashMap<String, String>();

	static {
		add(CONTENT_TYPE_MSG, ".msg");
		add(CONTENT_TYPE_TXT, ".txt");
	}

	public static String getContentType(String fileExt) {
		String contentType = DEFAULT_CONTENT_TYPE;

		if (fileExt != null && fileExt.length() != 0) {

			fileExt = fileExt.toLowerCase();
			if (!fileExt.startsWith(".")) {
				fileExt = "." + fileExt;
			}

			contentType = fileExtToContentType.get(fileExt);
			
			if (contentType == null) {
				contentType = (String) RegUtil.getRegistryValue("HKCR\\" + fileExt, "Content Type",
						DEFAULT_CONTENT_TYPE);
			}

		}

		return contentType;
	}

	public static String getFileExt(String contentType) {
		String fileExt = ".bin";
		String regkey = "HKEY_CLASSES_ROOT\\MIME\\Database\\Content Type\\" + contentType;
		if (contentType != null && contentType.length() != 0) {
			
			contentType = contentType.toLowerCase();
			
			fileExt = contentTypeToFileExt.get(contentType);
			if (fileExt == null) {
				fileExt = (String) RegUtil.getRegistryValue(regkey, "Extension", ".bin");
			}
		}
		return fileExt;
	}

	public static void main(String[] args) {
		System.out.println("msg=" + getContentType(".msg"));
		System.out.println("txt=" + getContentType(".txt"));
		System.out.println(CONTENT_TYPE_MSG + "=" + getFileExt(CONTENT_TYPE_MSG));
		System.out.println("text=" + getFileExt("text/plain"));
	}

	private static void add(String contentType, String fileExt) {
		contentTypeToFileExt.put(contentType, fileExt);
		fileExtToContentType.put(fileExt, contentType);
	}
}
