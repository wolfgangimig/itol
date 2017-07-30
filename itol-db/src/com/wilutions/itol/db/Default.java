package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public static <T> List<T> value(List<T> v) {
		return v != null ? v : new ArrayList<T>(0);
	}

	public static <T> Set<T> value(Set<T> v) {
		return v != null ? v : new HashSet<T>(0);
	}

	public static <T,R> Map<T,R> value(Map<T,R> v) {
		return v != null ? v : new HashMap<T,R>(0);
	}
}
