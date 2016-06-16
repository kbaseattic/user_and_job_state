package us.kbase.common.test;

import java.lang.reflect.Field;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

import us.kbase.common.test.TestException;

public class TestCommon {
	
	public static final String MONGOEXE = "test.mongo.exe";
	
	public static final String TEST_TEMP_DIR = "test.temp.dir";
	public static final String KEEP_TEMP_DIR = "test.temp.dir.keep";
	
	public static void printJava() {
		System.out.println("Java: " + System.getProperty("java.runtime.version"));
	}
	
	private static String getProp(String prop) {
		String p = System.getProperty(prop);
		if (p == null || p.isEmpty()) {
			throw new TestException("Property " + prop +
					" cannot be null or the empty string.");
		}
		return p;
	}
	
	public static String getTempDir() {
		return getProp(TEST_TEMP_DIR);
	}
	
	public static String getMongoExe() {
		return getProp(MONGOEXE);
	}
	
	public static boolean getDeleteTempFiles() {
		return !"true".equals(System.getProperty(KEEP_TEMP_DIR));
	}
	
	//http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html
	@SuppressWarnings("unchecked")
	public static Map<String, String> getenv() throws NoSuchFieldException,
			SecurityException, IllegalArgumentException,
			IllegalAccessException {
		Map<String, String> unmodifiable = System.getenv();
		Class<?> cu = unmodifiable.getClass();
		Field m = cu.getDeclaredField("m");
		m.setAccessible(true);
		return (Map<String, String>) m.get(unmodifiable);
	}
	
	public static void destroyDB(DB db) {
		for (String name: db.getCollectionNames()) {
			if (!name.startsWith("system.")) {
				// dropping collection also drops indexes
				db.getCollection(name).remove(new BasicDBObject());
			}
		}
	}
}
