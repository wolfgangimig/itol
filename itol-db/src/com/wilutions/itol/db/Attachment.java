/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Attachment {

	private String id;
	private String subject;
	private String contentType;
	private String fileName;
	private long contentLength;
	private InputStream stream;
	private String url;
	private boolean deleted;

	public Attachment() {
		id = "";
		subject = "";
		contentType = "";
		fileName = "";
		stream = new ByteArrayInputStream(new byte[0]);
		url = "";
	}

	public Attachment(String id, String subject, String contentType, String fileName, InputStream stream, String url) {
		super();
		this.id = id;
		this.subject = subject;
		this.contentType = contentType;
		this.fileName = fileName;
		this.stream = stream;
		this.url = url;
	}
	
	@Override
	protected Object clone() {
		Attachment copy = new Attachment();
		copy.id = id;
		copy.subject = subject;
		copy.contentType = contentType;
		copy.fileName = fileName;
		copy.stream = null;
		copy.url = url;
		copy.deleted = deleted;
		return copy;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof Attachment) {
			Attachment rhs = (Attachment)obj;
			ret = id.equals(rhs.id);
			if (ret) {
				ret = subject.equals(rhs.subject);
				if (ret) {
					ret = contentType.equals(rhs.contentType);
					if (ret) {
						ret = fileName.equals(rhs.fileName);
						if (ret) {
							ret = url.equals(rhs.url);
							if (ret) {
								ret = deleted == rhs.deleted;
							}
						}
					}
				}
			}
		}
		return ret;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public InputStream getStream() {
		return stream;
	}

	public void setStream(InputStream stream) {
		this.stream = stream;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String toString() {
		return "["  + fileName + ", length=" + contentLength + ", deleted=" + deleted + "]"; 
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}
