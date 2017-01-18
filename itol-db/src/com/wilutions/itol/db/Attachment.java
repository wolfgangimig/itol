/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Date;

public class Attachment implements Cloneable {

	private String id;
	private String subject;
	private String contentType;
	private String fileName;
	private long contentLength;
	private InputStream stream;
	private String url;
	private String thumbnailUrl;
	private boolean deleted;
	private Date lastModified;
	
	/**
	 * This member is set if the attachment was downloaded into a local file.
	 */
	private File localFile;
	
	public Attachment() {
		id = "";
		subject = "";
		contentType = "";
		fileName = "";
		stream = new ByteArrayInputStream(new byte[0]);
		url = "";
		thumbnailUrl = "";
	}

	public Attachment(String id, String subject, String contentType, String fileName, InputStream stream, String url) {
		super();
		this.id = id;
		this.subject = subject;
		this.contentType = contentType;
		this.fileName = fileName;
		this.stream = stream;
		this.url = url;
		this.thumbnailUrl = "";
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
		copy.thumbnailUrl = thumbnailUrl;
		copy.localFile = localFile;
		return copy;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof Attachment) {
			Attachment rhs = (Attachment)obj;
			ret = getId().equals(rhs.getId());
			if (ret) {
				ret = getSubject().equals(rhs.getSubject());
				if (ret) {
					ret = getFileName().equals(rhs.getFileName());
					if (ret) {
						ret = deleted == rhs.deleted;
					}
				}
			}
		}
		return ret;
	}

	public String getId() {
		if (id == null) id = "";
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubject() {
		if (subject == null) subject = "";
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContentType() {
		if (contentType == null) contentType = "";
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
		if (fileName == null) fileName = "";
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getUrl() {
		if (url == null) url = "";
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

	public String getThumbnailUrl() {
		if (thumbnailUrl == null) thumbnailUrl = "";
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public File getLocalFile() {
		return localFile;
	}

	public void setLocalFile(File localFile) {
		this.localFile = localFile;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	
}
