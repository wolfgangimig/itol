/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpClient {

	private final static Logger log = Logger.getLogger(HttpClient.class.getName());
	
	static
	{
		// see #12 "handshake alert: unrecognized_name"
		// http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
		System.setProperty ("jsse.enableSNIExtension", "false");
	}
	
	public static HttpResponse send(String url, String method, String[] headers, Object content, ProgressCallback cb) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("send(" + method + ", url=" + url);
			log.fine("headers=" + Arrays.toString(headers));
			log.fine("content=" + content);
		}
		
		if (cb == null) {
			cb = new ProgressCallbackImpl("HttpClient.send");
		}

		HttpURLConnection conn = null;
		HttpResponse ret = new HttpResponse();

		try {
			conn = (HttpURLConnection) (new URL(url).openConnection());
			conn.setRequestMethod(method);
			conn.setDoOutput(content != null);
			conn.setInstanceFollowRedirects(false);
			
			long contentLength = -1;
			String contentDisposition = "";
			
			for (String header : headers) {
				int p = header.indexOf(":");
				String key = header.trim();
				String value = "";
				if (p >= 0) {
					key = header.substring(0, p).trim();
					value = header.substring(p + 1).trim();
				}
				conn.setRequestProperty(key, value);
				
				if (key.equalsIgnoreCase("Content-Length")) {
					try {
						contentLength = Long.parseLong(value);
					}
					catch (NumberFormatException ignored) {
					}
				}
			}

			log.info(method + " " + url + " #" + contentLength);

			if (content != null) {
				
				conn.setUseCaches(false);
				if (contentLength >= 0) {
					conn.setFixedLengthStreamingMode(contentLength);
				}
				else {
					conn.setChunkedStreamingMode(9000);
				}

				ProgressCallback subcb = cb.createChild("upload");
				if (content instanceof String) {
					writeStringIntoStream(conn.getOutputStream(), ((String) content), subcb);
				} else if (content instanceof File) {
					writeFileIntoStream(conn.getOutputStream(), ((File) content), subcb);
				} else if (content instanceof InputStream) {
					writeFileIntoStream(conn.getOutputStream(), ((InputStream) content), contentLength, subcb);
				}
				subcb.setFinished();
			}
			

			ProgressCallback subcbRecv = cb.createChild("receive");
			if (log.isLoggable(Level.FINE))
				log.fine("getResponseCode...");
			ret.setStatus(conn.getResponseCode());
			if (log.isLoggable(Level.FINE))
				log.fine("status=" + ret.getStatus());
			
			contentLength = -1;
			contentDisposition = "";
			
			ArrayList<String> responseHeaders = new ArrayList<String>();
			for (String headerName : conn.getHeaderFields().keySet()) {
				List<String> headerValues = conn.getHeaderFields().get(headerName);
				if (log.isLoggable(Level.FINE))
					log.fine("response header=" + headerName + ", values=" + headerValues);

				String headerValue = "";
				if (headerValues.size() != 0) {
					headerValue = headerValues.get(0);
				}
				
				String header = (headerName != null ? headerName : "") + ": " + headerValue;
				responseHeaders.add(header);
				
				if (headerName == null) {
					
				}
				else if (headerName.equalsIgnoreCase("Content-Length")) {
					try {
						contentLength = Long.parseLong(headerValue);
					}
					catch (NumberFormatException ignored) {
					}
				}
				else if (headerName.equalsIgnoreCase("Content-Disposition")) {
					contentDisposition = headerValue;
				}

			}
			ret.setHeaders(responseHeaders.toArray(new String[responseHeaders.size()]));
			subcbRecv.setFinished();
			
			ProgressCallback subcbDownload = cb.createChild("download");
			if (contentDisposition != null && contentDisposition.length() != 0) {
				subcbDownload.setParams(contentDisposition);
			}
			
			try {
				if (log.isLoggable(Level.FINE))
					log.fine("read from input...");
				ret.setContent(readStringFromStream(conn.getInputStream(), contentLength, subcbDownload));
				
				log.info(ret.getStatus() + " #" + ret.getContent().length());

			} catch (IOException e) {
				log.info("send failed, exception=" + e);
				ret.setErrorMessage(e.getMessage());
				if (log.isLoggable(Level.FINE))
					log.fine("read from error...");
				ret.setContent(readStringFromStream(conn.getErrorStream(), contentLength, subcbDownload));
			}
			finally {
				subcbDownload.setFinished();
			}

		} catch (IOException e) {
			String msg = "HTTP request to URL=" + url + " failed. ";
			log.log(Level.WARNING, msg, e);
			ret.setErrorMessage(msg + e.toString());
		} finally {
//			if (conn != null) {
//				conn.disconnect();
//			}
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine(")send=" + ret.getStatus() + ", ret=" + ret);
		}
		return ret;
	}

	private static void writeStringIntoStream(OutputStream os, String s, ProgressCallback cb) throws IOException {
		if (log.isLoggable(Level.FINE))log.fine("writeStringIntoStream(length=" + s.length()); 
		cb.setProgress(0);
		cb.setTotal(s.length());
		OutputStreamWriter wr = new OutputStreamWriter(os, "UTF-8");
		try {
			wr.write(s);
		} finally {
			wr.close();
		}
		cb.setProgress(s.length());
		if (log.isLoggable(Level.FINE))log.fine(")writeStringIntoStream");
	}

	private static void writeFileIntoStream(OutputStream os, File file, ProgressCallback cb) throws IOException {
		writeFileIntoStream(os, new FileInputStream(file), file.length(), cb);
	}

	private static void writeFileIntoStream(OutputStream os, InputStream stream, long contentLength, ProgressCallback cb) throws IOException {
		if (log.isLoggable(Level.FINE))log.fine("writeFileIntoStream(contentLength=" + contentLength); 
		cb.setTotal(contentLength);
		cb.setProgress(0);
		try {
			byte[] buf = new byte[10000];
			int len = 0;
			double sum = 0;
			while ((len = stream.read(buf)) != -1) {
				os.write(buf, 0, len);
				
				if (cb.isCancelled()) {
					throw new InterruptedIOException();
				}
				
				sum += (double)len;
				cb.setProgress(sum);
			}
			if (log.isLoggable(Level.FINE))log.fine("#written=" + sum);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
		if (log.isLoggable(Level.FINE))log.fine(")writeFileIntoStream");
	}

	private static String readStringFromStream(InputStream is, long contentLength, ProgressCallback cb) {
		cb.setTotal(contentLength);
		String ret = null;
		if (is != null) {
			Reader rd = null;
			try {
				rd = new InputStreamReader(is, "UTF-8");
				StringBuilder sbuf = new StringBuilder();
				char[] buf = new char[10000];
				int len = 0;
				double sum = 0;
				while ((len = rd.read(buf)) != -1) {
					sbuf.append(buf, 0, len);
					
					if (cb.isCancelled()) {
						throw new InterruptedIOException();
					}
					
					sum += len;
					cb.setProgress(sum);
				}
				ret = sbuf.toString();
			} catch (IOException e) {
			} finally {
				if (rd != null) {
					try {
						rd.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return ret;
	}

	public static HttpResponse post(String url, String[] headers, String content, ProgressCallback cb) {
		return send(url, "POST", headers, content, cb);
	}

	public static HttpResponse get(String url, String[] headers, ProgressCallback cb) {
		return send(url, "GET", headers, null, cb);
	}

	public static HttpResponse upload(String url, String[] headers, File file, ProgressCallback cb) {
		return send(url, "POST", headers, file, cb);
	}

	public static String makeBasicAuthenticationHeader(String userName, String userPwd)
			throws UnsupportedEncodingException {
		String up = userName + ":" + userPwd;
		String b64 = Base64.getEncoder().encodeToString(up.getBytes("UTF-8"));
		return b64;
	}
}
