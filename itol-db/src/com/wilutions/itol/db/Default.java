package com.wilutions.itol.db;

public class Default {

	public static String value(String s) {
		return s != null ? s : "";
	}

	public static boolean isEmpty(String s) {
		return value(s).isEmpty();
	}

	public static boolean value(Boolean v) {
		return v != null && v.booleanValue();
	}
}
