/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

public class PasswordEncryption {
	
	private final static String IV = "happynewyearh201";
	private final static String encryptionKey = "18afc39c755144a9";
	
	public enum EAction {
		ENCRYPT, DECRYPT;
	}

	public static String encrypt(String plainText) throws IOException {
		try {
			byte[] data = plainText.getBytes("UTF-8");
			if (data.length % 16 != 0) {
				byte[] ndata = new byte[data.length + 16 - (data.length & 0x0F)];
				System.arraycopy(data, 0, ndata, 0, data.length);
				data = ndata;
			}
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
			SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
			return bytesToString(cipher.doFinal(data));
		} catch (Throwable e) {
			throw new IOException(e);
		}
	}
	
	public static String encrypt(String plainText, EAction action) throws IOException {
		String ret = plainText;
		switch (action) {
		case ENCRYPT: 
			ret = encrypt(plainText);
			break;
		case DECRYPT: 
			ret = decrypt(plainText);
			break;
		}
		return ret;
	}

	public static String decrypt(String cipherText) throws IOException {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
			SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
			return new String(cipher.doFinal(stringToBytes(cipherText)), "UTF-8").trim();
		} catch (Throwable e) {
			throw new IOException(e);
		}
	}

	private static byte[] stringToBytes(String s) throws Base64DecodingException, UnsupportedEncodingException {
		return Base64.decode(s.getBytes("UTF-8"));
	}

	private static String bytesToString(byte[] b) {
		return Base64.encode(b);
	}


}
