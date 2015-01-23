package com.wilutions.itol.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

	public static HttpResponse send(String url, String method, String[] headers, Object content) {
		log.info("send(url=" + url);

		if (log.isLoggable(Level.FINE)) {
			log.fine("method=" + method);
			log.fine("headers=" + Arrays.toString(headers));
			log.fine("content=" + content);
		}

		HttpURLConnection conn = null;
		HttpResponse ret = new HttpResponse();

		try {
			conn = (HttpURLConnection) (new URL(url).openConnection());
			conn.setRequestMethod(method);
			conn.setDoOutput(content != null);

			for (String header : headers) {
				int p = header.indexOf(":");
				String key = header.trim();
				String value = "";
				if (p >= 0) {
					key = header.substring(0, p).trim();
					value = header.substring(p + 1).trim();
				}
				conn.setRequestProperty(key, value);
			}

			if (content != null) {
				if (content instanceof String) {
					writeStringIntoStream(conn.getOutputStream(), ((String) content));
				} else if (content instanceof File) {
					writeFileIntoStream(conn.getOutputStream(), ((File) content));
				}
			}

			if (log.isLoggable(Level.FINE))
				log.fine("getResponseCode...");
			ret.setStatus(conn.getResponseCode());
			if (log.isLoggable(Level.FINE))
				log.fine("getResponseCode=" + ret.getStatus());

			ArrayList<String> responseHeaders = new ArrayList<String>();
			for (String headerName : conn.getHeaderFields().keySet()) {
				List<String> headerValues = conn.getHeaderFields().get(headerName);
				if (log.isLoggable(Level.FINE))
					log.fine("response header=" + headerName + ", values=" + headerValues);

				String headerValue = "";
				if (headerValues.size() != 0) {
					headerValue = headerValues.get(0);
				}
				String header = headerName + ": " + headerValue;
				responseHeaders.add(header);
			}
			ret.setHeaders(responseHeaders.toArray(new String[responseHeaders.size()]));

			try {
				if (log.isLoggable(Level.FINE))
					log.fine("read from input...");
				ret.setContent(readStringFromStream(conn.getInputStream()));
			} catch (IOException e) {
				log.info("send failed, exception=" + e);
				ret.setErrorMessage(e.getMessage());
				if (log.isLoggable(Level.FINE))
					log.fine("read from error...");
				ret.setContent(readStringFromStream(conn.getErrorStream()));
			}

		} catch (IOException e) {
			String msg = "HTTP request to URL=" + url + " failed: " + e.getMessage();
			ret.setErrorMessage(msg);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		if (log.isLoggable(Level.FINE))
			log.fine("ret=" + ret);
		log.info(")send=" + ret.getStatus());
		return ret;
	}

	private static void writeStringIntoStream(OutputStream os, String s) throws IOException {
		OutputStreamWriter wr = new OutputStreamWriter(os, "UTF-8");
		try {
			wr.write(s);
		} finally {
			wr.close();
		}
	}

	private static void writeFileIntoStream(OutputStream os, File file) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] buf = new byte[10000];
			int len = 0;
			while ((len = fis.read(buf)) != -1) {
				os.write(buf, 0, len);
			}
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	private static String readStringFromStream(InputStream is) {
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

	public static HttpResponse post(String url, String[] headers, String content) {
		return send(url, "POST", headers, content);
	}

	public static HttpResponse get(String url, String[] headers) {
		return send(url, "GET", headers, null);
	}

	public static HttpResponse upload(String url, String[] headers, File file) {
		return send(url, "POST", headers, file);
	}

	public static String makeBasicAuthenticationHeader(String userName, String userPwd)
			throws UnsupportedEncodingException {
		String up = userName + ":" + userPwd;
		String b64 = Base64.getEncoder().encodeToString(up.getBytes("UTF-8"));
		return b64;
	}
}
