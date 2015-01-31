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

	public Attachment() {
		id = "";
		subject = "";
		contentType = "";
		fileName = "";
		stream = new ByteArrayInputStream(new byte[0]);
	}

	public Attachment(String id, String subject, String contentType, String fileName, InputStream stream) {
		super();
		this.id = id;
		this.subject = subject;
		this.contentType = contentType;
		this.fileName = fileName;
		this.stream = stream;
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

}
