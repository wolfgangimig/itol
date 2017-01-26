/*
    Copyright (c) 2014 Wolfgang Imig
    
    This file is part of the library "Java Add-in for Microsoft Office".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wilutions.com.ComEnum;
import com.wilutions.com.ComException;
import com.wilutions.com.reg.DeclRegistryValue;
import com.wilutions.itol.db.Default;

/**
 * This class helps to store and load the state of UI controls.
 */
public class UserProfile {

	private static final int OPT_ONLY_ANNOTATED_FIELDS = 1;
	private static final int OPT_ALL_FIELDS = 0;

	private String appName;
	private String manufacturerName;
	private JSONObject root;
	
	/**
	 * Initialize the registry destination path.
	 * 
	 * @param manufacturerName
	 *            Manufacturer name
	 * @param appName
	 *            Application name
	 */
	private UserProfile(String manufacturerName, String appName) {
		this.appName = appName;
		this.manufacturerName = manufacturerName;
	}
	
	public static UserProfile readFromAppData(String manufacturerName, String appName) {
		UserProfile ret = null;
		try {
			File configFile = getConfigFile(manufacturerName, appName);
			System.out.println("Load config from " + configFile);
			ret = read(configFile);
		}
		catch(Exception e) {
			System.out.println("File not found.");
			ret = new UserProfile(manufacturerName, appName);
		}
		return ret;
	}
	
	private void flush() {
		try {
			writeIntoAppData();
		} catch (Exception e) {
		}
	}

	private void writeIntoAppData() throws Exception {
		File configFile = getConfigFile(manufacturerName, appName);
		System.out.println("Write config into " + configFile);
		write(configFile);
	}

	private static UserProfile read(File configFile) throws Exception {
		String json = new String(Files.readAllBytes(configFile.toPath()), "UTF-8");
		GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(json, UserProfile.class);
	}
	
	private void write(File configFile) throws Exception {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
        Gson gson = builder.create();
        String json = gson.toJson(this);
        Files.write(configFile.toPath(), json.getBytes("UTF-8"), StandardOpenOption.CREATE);
	}

	/**
	 * Store all fields annotated by {@link DeclRegistryValue}.
	 * 
	 * @param obj
	 *            Store fields of this object.
	 */
	public void writeFields(Object obj) {
		if (obj == null) throw new IllegalArgumentException("obj must not be null");
		String subKey = obj.getClass().getName();
		writeObject(subKey, obj, OPT_ONLY_ANNOTATED_FIELDS);
		flush();
	}

	/**
	 * Read all fields annotated by {@link DeclRegistryValue}.
	 * 
	 * @param obj
	 *            Read annotated fields of this object.
	 */
	public void readFields(Object obj) {
		String subKey = obj.getClass().getName();
		readFields(subKey, obj, OPT_ONLY_ANNOTATED_FIELDS);
	}

	/**
	 * Read an object or primitive value at the given sub-key. This function
	 * reads all fields - not only annotated fields.
	 * 
	 * @param subKey
	 *            Sub-key
	 * @return Object or primitive value
	 */
	public Object read(String subKey) {
		return readObject(subKey);
	}

	/**
	 * Write object or primitive value at the given sub-key. This function
	 * writes all fields - not only annotated fields.
	 * 
	 * @param subKey
	 *            Sub-key
	 * @param obj
	 *            Object to be written.
	 */
	public void write(String subKey, Object obj) {
		String destKey = subKey;
		if (obj != null) {
			writeObject(destKey, obj, OPT_ALL_FIELDS);
		}
		else {
			deleteObject(destKey);
		}
		flush();
	}

	private void readFields(String key, Object obj, int opts) {
		Class<?> clazz = obj.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				String fieldName = field.getName();
				int mods = field.getModifiers();

				if (Modifier.isStatic(mods)) continue;
				if (Modifier.isFinal(mods)) continue;
				if (Modifier.isTransient(mods)) continue;

				if ((opts & OPT_ONLY_ANNOTATED_FIELDS) != 0) {
					DeclRegistryValue regValueAnno = field.getAnnotation(DeclRegistryValue.class);
					if (regValueAnno == null) continue;
					String s = regValueAnno.value();
					if (s != null && s.length() != 0) {
						fieldName = s;
					}
				}

				if (!Modifier.isPublic(mods)) {
					field.setAccessible(true);
				}

				try {
					Object fieldValue = getFieldValue(key, fieldName, field.getType());
					if (fieldValue != null) {
						field.set(obj, fieldValue);
					}
				}
				catch (Throwable ignored) {
				}
			}

			clazz = clazz.getSuperclass();
		}
	}
	
	private Object readObject(String key) {
		String className = (String) RegUtil_getRegistryValue(key, "", "");
		if (className == null || className.length() == 0) return null;

		Object ret = null;

		try {
			Class<?> clazz = Class.forName(className);

			// Value written as object?
			if (clazz.isArray() || (clazz == String.class) || (clazz == Integer.class) || (clazz == Long.class)
					|| (clazz == Double.class) || (clazz == Float.class) || (clazz == Boolean.class) || (clazz.isEnum())
					|| (ComEnum.class.isAssignableFrom(clazz)) || (List.class.isAssignableFrom(clazz))
					|| (Map.class.isAssignableFrom(clazz))) {
				return getFieldValue(key, "value", clazz);
			}

			ret = clazz.newInstance();
			readFields(key, ret, OPT_ALL_FIELDS);

		}
		catch (Throwable ignored) {
		}

		return ret;
	}

	private Object getFieldValueArray(String key, String fieldName, Class<?> arrayType)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String arrayKey = key + "\\" + fieldName;
		Class<?> elementType = arrayType.getComponentType();
		int length = Integer.valueOf((String) RegUtil_getRegistryValue(arrayKey, "length", "0"));
		Object fieldValue = Array.newInstance(elementType, length);
		for (int i = 0; i < length; i++) {
			Object elementValue = getFieldValue(arrayKey, Integer.toString(i), elementType);
			Array.set(fieldValue, i, elementValue);
		}
		return fieldValue;
	}

	private void setFieldValueArray(String key, String fieldName, Object arrayValue) throws ComException {
		String arrayKey = key + "\\" + fieldName;
		int length = Array.getLength(arrayValue);
		RegUtil_setRegistryValue(arrayKey, "length", length);
		for (int i = 0; i < length; i++) {
			Object elementValue = Array.get(arrayValue, i);
			if (elementValue == null) continue;
			setFieldValue(arrayKey, Integer.toString(i), elementValue, elementValue.getClass());
		}
	}

	private Object getFieldValueList(String key, String fieldName, Class<?> listType)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String listKey = key + "\\" + fieldName;
		Integer length = (Integer) RegUtil_getRegistryValue(listKey, "length", 0);
		@SuppressWarnings("unchecked")
		List<Object> fieldValue = listType.equals(List.class) ? new ArrayList<Object>()
				: (List<Object>) listType.newInstance();
		if (length != null && length != 0) {
			String elementTypeName = (String) RegUtil_getRegistryValue(listKey, "elementClass", "");
			Class<?> elementType = Class.forName(elementTypeName);
			for (int i = 0; i < length; i++) {
				Object elementValue = getFieldValue(listKey, Integer.toString(i), elementType);
				if (elementValue != null) {
					fieldValue.add(elementValue);
				}
			}
		}
		return fieldValue;
	}

	@SuppressWarnings("rawtypes")
	private void setFieldValueList(String key, String fieldName, Object listValue) throws ComException {
		String listKey = key + "\\" + fieldName;
		List list = (List) listValue;
		int length = list.size();
		RegUtil_setRegistryValue(listKey, "length", length);
		int i = 0;
		boolean classWritten = false;
		for (Object elementValue : list) {
			if (!classWritten && elementValue != null) {
				RegUtil_setRegistryValue(listKey, "elementClass", elementValue.getClass().getName());
				classWritten = true;
			}
			setFieldValue(listKey, Integer.toString(i++), elementValue, elementValue.getClass());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getFieldValueMap(String key, String fieldName, Class<?> mapType)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String mapKey = key + "\\" + fieldName;
		Integer length = (Integer) RegUtil_getRegistryValue(mapKey, "length", 0);
		Map fieldValue = mapType.equals(Map.class) ? new HashMap<Object, Object>() : (Map) mapType.newInstance();
		if (length != null && length != 0) {
			String keyTypeName = (String) RegUtil_getRegistryValue(mapKey, "keyClass", "");
			String valueTypeName = (String) RegUtil_getRegistryValue(mapKey, "valueClass", "");
			Class<?> keyType = Class.forName(keyTypeName);
			Class<?> valueType = Class.forName(valueTypeName);
			for (int i = 0; i < length; i++) {
				String elementKey = mapKey + "\\" + i;
				Object keyValue = getFieldValue(elementKey, "key", keyType);
				if (keyValue != null) {
					Object valueValue = getFieldValue(elementKey, "value", valueType);
					if (valueValue != null) {
						fieldValue.put(keyValue, valueValue);
					}
				}
			}
		}
		return fieldValue;
	}

	@SuppressWarnings("rawtypes")
	private void setFieldValueMap(String key, String fieldName, Object mapValue) throws ComException {
		String mapKey = key + "\\" + fieldName;
		Map map = (Map) mapValue;
		int length = map.size();
		RegUtil_setRegistryValue(mapKey, "length", length);
		boolean keyClassWritten = false, valueClassWritten = false;
		int i = 0;
		for (Object e : map.entrySet()) {
			String elementKey = mapKey + "\\" + (i++);
			Map.Entry mapEntry = (Map.Entry) e;
			Object keyValue = mapEntry.getKey();
			Object valueValue = mapEntry.getValue();
			if (!keyClassWritten && keyValue != null) {
				RegUtil_setRegistryValue(mapKey, "keyClass", keyValue.getClass().getName());
			}
			if (!valueClassWritten && valueValue != null) {
				RegUtil_setRegistryValue(mapKey, "valueClass", valueValue.getClass().getName());
			}
			setFieldValue(elementKey, "key", keyValue, keyValue.getClass());
			setFieldValue(elementKey, "value", valueValue, valueValue.getClass());
		}
	}

	private Object getFieldValue(String key, String fieldName, Class<?> fieldType)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Object ret = null;

		if (fieldType.isArray()) {
			ret = getFieldValueArray(key, fieldName, fieldType);
		}
		else if (fieldType == String.class) {
			ret = RegUtil_getRegistryValue(key, fieldName, "");
		}
		else if (fieldType == Integer.class || fieldType == int.class) {
			Object regValue = RegUtil_getRegistryValue(key, fieldName, null);
			if (regValue != null) {
				try {
					ret = (Integer)regValue;
				}
				catch (Exception ignored) {}
			}
		}
		else if (fieldType == Long.class || fieldType == long.class) {
			Object regValue = RegUtil_getRegistryValue(key, fieldName, null);
			if (regValue != null) {
				try {
					ret = Long.valueOf((String) regValue);
				}
				catch (Exception ignored) {}
			}
		}
		else if (fieldType == Double.class || fieldType == double.class) {
			Object regValue = RegUtil_getRegistryValue(key, fieldName, null);
			if (regValue != null) {
				try {
					ret = Double.valueOf((String) regValue);
				}
				catch (Exception ignored) {}
			}
		}
		else if (fieldType == Float.class || fieldType == float.class) {
			Object regValue = RegUtil_getRegistryValue(key, fieldName, null);
			if (regValue != null) {
				try {
					ret = Float.valueOf((String) regValue);
				}
				catch (Exception ignored) {}
			}
		}
		else if (fieldType == Boolean.class || fieldType == boolean.class) {
			Object regValue = RegUtil_getRegistryValue(key, fieldName, Boolean.FALSE.toString());
			if (regValue != null) {
				try {
					ret = Boolean.valueOf((String) regValue);
				}
				catch (Exception ignored) {}
			}
		}
		else if (fieldType.isEnum()) {
			String enumValueStr = (String) RegUtil_getRegistryValue(key, fieldName, "");
			for (Object e : fieldType.getEnumConstants()) {
				if (e.toString().equals(enumValueStr)) {
					ret = e;
					break;
				}
			}
		}
		else if (ComEnum.class.isAssignableFrom(fieldType)) {
			String enumValueStr = (String) RegUtil_getRegistryValue(key, fieldName, "");
			if (!enumValueStr.isEmpty()) {
				try {
					int enumValue = Integer.parseInt(enumValueStr);
					Method m = fieldType.getDeclaredMethod("valueOf", int.class);
					ret = m.invoke(null, enumValue);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		else if (List.class.isAssignableFrom(fieldType)) {
			ret = getFieldValueList(key, fieldName, fieldType);
		}
		else if (Map.class.isAssignableFrom(fieldType)) {
			ret = getFieldValueMap(key, fieldName, fieldType);
		}
		else {
			ret = readObject(key + "\\" + fieldName);
		}
		return ret;
	}

	private void setFieldValue(String key, String fieldName, Object value, Class<?> fieldType)
			throws ComException {
		if (value == null) {
			RegUtil_deleteRegistryValue(key, fieldName);
		}
		else {
			if (fieldType.isArray()) {
				setFieldValueArray(key, fieldName, value);
			}
			else if (fieldType == String.class) {
				RegUtil_setRegistryValue(key, fieldName, (String) value);
			}
			else if (fieldType == Integer.class || fieldType == int.class) {
				RegUtil_setRegistryValue(key, fieldName, (Integer) value);
			}
			else if (fieldType == Long.class || fieldType == long.class) {
				RegUtil_setRegistryValue(key, fieldName, value.toString());
			}
			else if (fieldType == Double.class || fieldType == double.class) {
				RegUtil_setRegistryValue(key, fieldName, value.toString());
			}
			else if (fieldType == Float.class || fieldType == float.class) {
				RegUtil_setRegistryValue(key, fieldName, value.toString());
			}
			else if (fieldType == Boolean.class || fieldType == boolean.class) {
				RegUtil_setRegistryValue(key, fieldName, value.toString());
			}
			else if (fieldType.isEnum()) {
				RegUtil_setRegistryValue(key, fieldName, value.toString());
			}
			else if (ComEnum.class.isAssignableFrom(fieldType)) {
				try {
					Field f = fieldType.getDeclaredField("value");
					String enumValueStr = Integer.toString((int) f.get(value));
					RegUtil_setRegistryValue(key, fieldName, enumValueStr);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (List.class.isAssignableFrom(fieldType)) {
				setFieldValueList(key, fieldName, value);
			}
			else if (Map.class.isAssignableFrom(fieldType)) {
				setFieldValueMap(key, fieldName, value);
			}
			else {
				writeObject(key + "\\" + fieldName, value, OPT_ALL_FIELDS);
			}
		}
	}

	private void writeObject(String key, Object obj, int opts) {
		Class<?> clazz = obj != null ? obj.getClass() : null;
		String className = clazz != null ? clazz.getName() : "";

		try {
			RegUtil_setRegistryValue(key, "", className);
			if (className == null || className.length() == 0) return;

			if (clazz.isArray() || (clazz == String.class) || (clazz == Integer.class) || (clazz == Long.class)
					|| (clazz == Double.class) || (clazz == Float.class) || (clazz == Boolean.class) || (clazz.isEnum())
					|| (ComEnum.class.isAssignableFrom(clazz)) || (List.class.isAssignableFrom(clazz))
					|| (Map.class.isAssignableFrom(clazz))) {
				setFieldValue(key, "value", obj, clazz);
				return;
			}

			while (clazz != null && clazz != Object.class) {
				for (Field field : clazz.getDeclaredFields()) {
					String fieldName = field.getName();
					int mods = field.getModifiers();

					if (Modifier.isStatic(mods)) continue;
					if (Modifier.isFinal(mods)) continue;
					if (Modifier.isTransient(mods)) continue;

					if ((opts & OPT_ONLY_ANNOTATED_FIELDS) != 0) {
						DeclRegistryValue regValueAnno = field.getAnnotation(DeclRegistryValue.class);
						if (regValueAnno == null) continue;
						String s = regValueAnno.value();
						if (s != null && s.length() != 0) {
							fieldName = s;
						}
					}

					if (!Modifier.isPublic(mods)) {
						field.setAccessible(true);
					}

					try {
						Object fieldValue = field.get(obj);
						Class<?> fieldClass = field.getType();
						setFieldValue(key, fieldName, fieldValue, fieldClass);
					}
					catch (Throwable e) {
						e.printStackTrace();
					}
				}

				clazz = clazz.getSuperclass();
			}
		}
		catch (Throwable e) {
		}
	}

	private static class Property {
		@DeclRegistryValue
		String id;
		@DeclRegistryValue
		int type = 3;
		@DeclRegistryValue
		Object value;

		Property(String id, Object value) {
			this.id = id;
			this.value = value;
		};

		public String toString() {
			return "[" + id + "," + value + "," + type + "]";
		}
	}

	private static class Globals {
		@DeclRegistryValue
		List<Property> properties;

		public String toString() {
			return "[" + properties + "]";
		}
	}

	private static File getConfigFile(String manufacturerName, String appName) {
		String appData = System.getenv("APPDATA");
		if (Default.value(appData).isEmpty()) {
			appData = ".";
		}
		File dataDir = new File(new File(new File(appData), manufacturerName), appName).getAbsoluteFile();
		dataDir.mkdirs();
		File configFile = new File(dataDir, "user-profile.json");
		return configFile;
	}

	private void deleteObject(String key) {
		RegUtil_purgeRegistryKey(key);
	}

	private void RegUtil_deleteRegistryValue(String key, String valueName) {
		valueName = makeValueName(valueName);
		Map<String, Object> child = properties.get(key);
		if (child != null) {
			child.remove(valueName);
		}
	}

	private void RegUtil_purgeRegistryKey(String key) {
		properties.remove(key);
	}

	private Object RegUtil_getRegistryValue(String keyName, String valueName, Object defaultValue) {
		valueName = makeValueName(valueName);
		Object ret = defaultValue;
		Map<String, Object> child = properties.get(keyName);
		if (child != null) {
			Object value = child.get(valueName);
			if (value != null) {
				ret = value;
			}
		}
		return ret;
	}

	private void RegUtil_setRegistryValue(String keyName, String valueName, Object value) {
		valueName = makeValueName(valueName);
		Map<String,Object> child = properties.get(keyName);
		if (child == null) {
			child = new HashMap<String, Object>();
			properties.put(keyName, child);
		}
		child.put(valueName, value);
	}
	
	private String makeValueName(String valueName) {
		if (Default.value(valueName).isEmpty()) {
			valueName = "(default)"; 
		}
		return valueName;
	}

	public static void main(String[] args) {
		UserProfile registry = new UserProfile("WILUTIONS", "TEST-REGISTRY");

		Globals globals = new Globals();
		globals.properties = Arrays.asList(new Property("abc", "def"));

		System.out.println("globals=" + globals);

		registry.writeFields(globals);

		Globals rglobals = new Globals();
		registry.readFields(rglobals);

		System.out.println("rglobals=" + rglobals);
	}
}
