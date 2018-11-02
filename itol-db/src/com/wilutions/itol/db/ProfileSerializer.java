package com.wilutions.itol.db;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serialize class Profile and its derived classes.
 * https://stackoverflow.com/questions/16872492/gson-and-abstract-superclasses-deserialization-issue
 */
public class ProfileSerializer implements JsonSerializer<SerializableProfile>, JsonDeserializer<SerializableProfile> {
	
	Map<Integer, SerializableProfile> profiles = new HashMap<Integer, SerializableProfile>();

	@Override
	public JsonElement serialize(SerializableProfile src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.addProperty("type", src.getClass().getName());
		int __ref = System.identityHashCode(src);
		result.addProperty("__ref", __ref);
		if (!profiles.containsKey(__ref)) {
			profiles.put(__ref, src);
			result.add("properties", context.serialize(src, src.getClass()));
		}
		return result;
	}

	@Override
	public SerializableProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		String type = jsonObject.get("type").getAsString();
		JsonElement elmRef = jsonObject.get("__ref");
		int __ref = elmRef != null ? elmRef.getAsInt() : 0;
		SerializableProfile ret = profiles.get(__ref);
		if (ret == null) {
			JsonElement element = jsonObject.get("properties");
			try {
				ret = context.deserialize(element, Class.forName(type));
			} catch (ClassNotFoundException cnfe) {
				throw new JsonParseException("Unknown element type: " + type, cnfe);
			}
		}
		return ret;
	}

}
