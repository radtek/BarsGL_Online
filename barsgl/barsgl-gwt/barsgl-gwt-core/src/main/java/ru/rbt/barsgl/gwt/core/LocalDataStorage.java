package ru.rbt.barsgl.gwt.core;

import java.util.HashMap;
import java.util.Map;

public final class LocalDataStorage {
	private static final Map<String, Object> storage = new HashMap<String, Object>();
	
	public static void putParam(String name, Object param) {
		storage.put(name, param);
	}
	
	public static Object getParam(String name) {
		return storage.get(name);
	}
	
	public static boolean isParamExists(String name) {
		return storage.containsKey(name);
	}
	
	public static void removeParam(String name) {
		storage.remove(name);
	}

	public static void clear(){
		storage.clear();
	}
	
	private LocalDataStorage() {}
}
