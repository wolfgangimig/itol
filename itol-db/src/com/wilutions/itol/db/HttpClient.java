/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class HttpClient {

	private final static Logger log = Logger.getLogger(HttpClient.class.getName());

	private static HashMap<String, String> redirections = new HashMap<>();
	
	public final static int CONNECT_TIMEOUT_SECONDS = 10;

	static {

//		 see #12 "handshake alert: unrecognized_name"
//		 http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
//			UUUHHH this cannot be a solution. If this option is false, images from https://avatar-cdn.atlassian.com cannot be downloaded.
//			(This image URLs are provided by https://ecosystem.atlassian.net)
//		 System.setProperty("jsse.enableSNIExtension", "false");
	}
	
	public static CompletableFuture<HttpResponse> sendAsync(String surl, String method, String[] headers, Object content, ProgressCallback cb, Executor executor) {
		return CompletableFuture.supplyAsync(() -> send(surl, method, headers, content, cb), executor);
	}

	public static HttpResponse send(String surl, String method, String[] headers, Object content, ProgressCallback cb) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("send(" + method + ", surl=" + surl);
			log.fine("headers=" + Arrays.toString(headers));
			log.fine("content=" + content);
		}

		if (cb == null) {
			cb = new ProgressCallbackImpl("HttpClient.send");
		}
		cb.setTotal(1.0);

		long startTime = System.currentTimeMillis();
		HttpURLConnection conn = null;
		HttpResponse ret = new HttpResponse();

		for (String key : redirections.keySet()) {
			int p = surl.indexOf(key);
			if (p >= 0) {
				String srepl = redirections.get(key);
				log.fine("redirect, replace part=" + key + " with " + srepl);

				String nurl = surl.substring(0, p) + srepl + surl.substring(p + key.length());
				log.info("redirect, new-url=" + nurl);
				surl = nurl;
			}
		}

		try {
			
			//doTrustToCertificates();  
			
			URL url = new URL(surl);
			conn = (HttpURLConnection) (url.openConnection());

			conn.setConnectTimeout(CONNECT_TIMEOUT_SECONDS * 1000);

			conn.setRequestMethod(method);
			conn.setDoOutput(content != null);
			// conn.setInstanceFollowRedirects(false);

			long contentLength = -1;
			String contentDisposition = "";
			
			// Wrap String bytes into InputStream 
			if (content instanceof String) {
				byte[] buf = ((String)content).getBytes("UTF-8");
				content = new ByteArrayInputStream(buf);
				contentLength = buf.length;
				conn.setRequestProperty("Content-Length", Long.toString(contentLength));
			}

			conn.addRequestProperty("Accept-Encoding", "gzip");
			
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
				else if (!(content instanceof String)) {
					conn.setChunkedStreamingMode(9000);
				}

				ProgressCallback subcb = cb.createChild("Upload", 0.5);
				if (content instanceof File) {
					writeFileIntoStream(conn.getOutputStream(), ((File) content), subcb);
				}
				else if (content instanceof InputStream) {
					writeFileIntoStream(conn.getOutputStream(), ((InputStream) content), contentLength, subcb);
				}
				subcb.setFinished();
			}

			ProgressCallback subcbRecv = cb.createChild("Receive", content != null ? 0.5 : 1.0);
			if (log.isLoggable(Level.FINE)) log.fine("getResponseCode...");
			ret.setStatus(conn.getResponseCode());
			if (log.isLoggable(Level.FINE)) log.fine("status=" + ret.getStatus());

			contentLength = -1;
			contentDisposition = "";

			ArrayList<String> responseHeaders = new ArrayList<String>();
			for (String headerName : conn.getHeaderFields().keySet()) {
				List<String> headerValues = conn.getHeaderFields().get(headerName);
				if (log.isLoggable(Level.FINE)) log.fine("response header=" + headerName + ", values=" + headerValues);

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

			if (!url.equals(conn.getURL())) {
				String ol = surl;
				String nl = conn.getURL().toString();
				log.info("was redirected, old-url=" + ol + ", new-url=" + nl);
				for (int i = 0; i < ol.length(); i++) {
					String sub = ol.substring(i);
					int p = nl.indexOf(sub);
					if (p >= 0) {
						String sfind = ol.substring(0, i);
						String srepl = nl.substring(0, p);
						log.fine("add redirection replacement " + sfind + " -> " + srepl);
						redirections.put(sfind, srepl);
						break;
					}
				}
			}

			if (contentDisposition != null && contentDisposition.length() != 0) {
				subcbRecv.setParams(contentDisposition);
			}

			String contentType = Default.value(conn.getHeaderField("Content-Type")).toLowerCase();
			boolean isStringContent = contentType.contains("json") || contentType.contains("text/html");
			
			String contentEncoding = conn.getHeaderField("Content-Encoding");
			boolean isGZIP = Default.value(contentEncoding).toLowerCase().contains("gzip");

			try {
				InputStream istream = conn.getInputStream();
				if (isGZIP) {
					istream = new GZIPInputStream(istream, 10 * 1000); 
				}
				
				if (log.isLoggable(Level.FINE)) log.fine("read from input...");
				long responseContentLength = 0;
				if (isStringContent) {
					ret.setContent(readStringFromStream(istream, contentLength, subcbRecv));
					responseContentLength = ret.getContent().length();
				}
				else {
					ret.setFile(readFileFromStream(istream, contentLength, subcbRecv));
					responseContentLength = ret.getFile().length();
				}
				
				long endTime = System.currentTimeMillis();
				log.info("[" + (endTime-startTime) + "] " + ret.getStatus() + " #" + responseContentLength);

			}
			catch (IOException e) {
				log.info("send failed, exception=" + e);
				ret.setErrorMessage(e.getMessage());
				if (log.isLoggable(Level.FINE)) log.fine("read from error...");
				InputStream istream = conn.getErrorStream();
				if (isGZIP) {
					istream = new GZIPInputStream(istream, 10 * 1000); 
				}
				ret.setContent(readStringFromStream(istream, contentLength, subcbRecv));
			}
			finally {
				subcbRecv.setFinished();
			}

		}
		catch (Exception e) {
			String msg = "HTTP request to URL=" + surl + " failed. ";
			log.log(Level.WARNING, msg, e);
			ret.setErrorMessage(msg + e.toString());
		}
		finally {
			// if (conn != null) {
			// conn.disconnect();
			// }
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine(")send=" + ret.getStatus() + ", ret=" + ret);
		}
		return ret;
	}

	private static File readFileFromStream(InputStream is, long contentLength, ProgressCallback cb) throws IOException {
		cb.setTotal(contentLength >= 0 ? contentLength : 1);
		cb.setFakeProgress(contentLength < 0);
		File ret = null;
		if (is != null) {
			FileOutputStream fos = null;
			try {
				ret = File.createTempFile("itol", ".tmp");
				fos = new FileOutputStream(ret);
				byte[] buf = new byte[10000];
				int len = 0;
				while ((len = is.read(buf)) != -1) {

					if (cb.isCancelled()) {
						throw new InterruptedIOException();
					}

					fos.write(buf, 0, len);
					
					cb.incrProgress(len);
				}
			}
			finally {
				if (fos != null) {
					try {
						fos.close();
					}
					catch (IOException e) {
					}
				}
				if (is != null) {
					try {
						is.close();
					}
					catch (IOException e) {
					}
				}
			}
		}
		return ret;
	}

	private static void writeFileIntoStream(OutputStream os, File file, ProgressCallback cb) throws IOException {
		writeFileIntoStream(os, new FileInputStream(file), file.length(), cb);
	}

	private static void writeFileIntoStream(OutputStream os, InputStream stream, long contentLength,
			ProgressCallback cb) throws IOException {
		if (log.isLoggable(Level.FINE)) log.fine("writeFileIntoStream(contentLength=" + contentLength);
		cb.setTotal(contentLength >= 0 ? contentLength : 1);
		cb.setFakeProgress(contentLength < 0);
		try {
			byte[] buf = new byte[10000];
			int len = 0;
			double sum = 0;
			while ((len = stream.read(buf)) != -1) {
				os.write(buf, 0, len);

				if (cb.isCancelled()) {
					throw new InterruptedIOException();
				}

				sum += (double) len;
				cb.incrProgress(len);
			}
			if (log.isLoggable(Level.FINE)) log.fine("#written=" + sum);
		}
		finally {
			if (stream != null) {
				stream.close();
			}
			cb.setFinished();
		}
		if (log.isLoggable(Level.FINE)) log.fine(")writeFileIntoStream");
	}

	private static String readStringFromStream(InputStream is, long contentLength, ProgressCallback cb) throws IOException {
		cb.setTotal(contentLength >= 0 ? contentLength : 1);
		cb.setFakeProgress(contentLength < 0);
		String ret = null;
		if (is != null) {
			Reader rd = null;
			try {
				rd = new InputStreamReader(is, "UTF-8");
				StringBuilder sbuf = new StringBuilder();
				char[] buf = new char[10000];
				int len = 0;
				while ((len = rd.read(buf)) != -1) {
					sbuf.append(buf, 0, len);

					if (cb.isCancelled()) {
						throw new InterruptedIOException();
					}

					cb.incrProgress(len);
				}
				ret = sbuf.toString();
			}
			finally {
				if (rd != null) {
					try {
						rd.close();
					}
					catch (IOException e) {
					}
				}
			}
		}
		return ret;
	}

	public static CompletableFuture<HttpResponse> postAsync(String url, String[] headers, String content, ProgressCallback cb, Executor executor) {
		return sendAsync(url, "POST", headers, content, cb, executor);
	}

	public static HttpResponse post(String url, String[] headers, String content, ProgressCallback cb) {
		return send(url, "POST", headers, content, cb);
	}

	public static CompletableFuture<HttpResponse> getAsync(String url, String[] headers, ProgressCallback cb, Executor executor) {
		return sendAsync(url, "GET", headers, null, cb, executor);
	}

	public static HttpResponse get(String url, String[] headers, ProgressCallback cb) {
		return send(url, "GET", headers, null, cb);
	}

	public static CompletableFuture<HttpResponse> uploadAsync(String url, String[] headers, File file, ProgressCallback cb, Executor executor) {
		return sendAsync(url, "POST", headers, file, cb, executor);
	}

	public static HttpResponse upload(String url, String[] headers, File file, ProgressCallback cb) {
		return send(url, "POST", headers, file, cb);
	}

	public static String makeBasicAuthenticationHeader(String userName, String userPwd)
			throws UnsupportedEncodingException {
		String plainPwd = PasswordEncryption.decrypt(userPwd);
		String up = userName + ":" + plainPwd;
		String b64 = Base64.getEncoder().encodeToString(up.getBytes("UTF-8"));
		return b64;
	}
	
	
	// Try to fix LazyHttpInputStream:70 Download https://avatar-cdn.atlassian.com/227743a57756a82078cffda8cf49a7dc?s=16&d=https%3A%2F%2Fsecure.gravatar.com%2Favatar%2F227743a57756a82078cffda8cf49a7dc%3Fd%3Dmm%26s%3D16%26noRedirect%3Dtrue failed. 
	//	 javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure

	// http://stackoverflow.com/questions/6659360/how-to-solve-javax-net-ssl-sslhandshakeexception-error
	// trusting all certificate
	@SuppressWarnings("unused")
	private static void doTrustToCertificates() throws Exception {
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
				return;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
				return;
			}
		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
					System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '"
							+ session.getPeerHost() + "'.");
				}
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
}
