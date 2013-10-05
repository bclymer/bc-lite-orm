package com.bclymer.bcliteorm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.database.Cursor;

import com.bclymer.bcliteorm.annotations.PersistantField;
import com.bclymer.bcliteorm.annotations.PersistantObject;

class BcCache {

	private static Map<Class<?>, CachedClass> mCache = new HashMap<Class<?>, CachedClass>();
	
	public static CachedClass cacheClass(Class<?> cls) {
		return cacheClass(cls, false);
	}
	
	public static CachedClass cacheClass(Class<?> cls, boolean creatingTable) {
		CachedClass cachedClass = mCache.get(cls);
		if (cachedClass != null) {
			return cachedClass;
		}
		cachedClass = new CachedClass();
		cachedClass.cls = cls;
		if (!cls.isAnnotationPresent(PersistantObject.class)) {
			throw new IllegalArgumentException("Class " + cls.getSimpleName() + " is not persistant");
		}
		cachedClass.persistantObject = cls.getAnnotation(PersistantObject.class);
		
		Field[] fields = cls.getDeclaredFields();
		boolean hasIdField = false;
		boolean hasPersistantField = false;
		cachedClass.fieldAnnotations = new HashMap<Field, PersistantField>();
		for (Field field : fields) {
			if (field.isAnnotationPresent(PersistantField.class)) {
				hasPersistantField = true;
				PersistantField persistantField = field.getAnnotation(PersistantField.class);
				cachedClass.fieldAnnotations.put(field, persistantField);
				if (persistantField.id()) {
					if (hasIdField) {
						throw new IllegalArgumentException("Class " + cls.getSimpleName() + " has more than 1 id fields");
					}
					hasIdField = true;
					cachedClass.idColName = cachedClass.columnName(field, true);
				} else if (persistantField.generatedId()) {
					if (hasIdField) {
						throw new IllegalArgumentException("Class " + cls.getSimpleName() + " has more than 1 id fields");
					}
					hasIdField = true;
					cachedClass.idColName = cachedClass.columnName(field, true);
					if (!creatingTable) {
						Cursor c = BcSQLiteOpenHelper.getMyReadableDatabase().query("sqlite_sequence", new String[] { "seq" }, "name = ?", new String[] { cachedClass.tableName(false) }, null, null, null);
						if (c.moveToFirst()) {
							cachedClass.primaryId = c.getInt(0);
						}
					}
				}
			}
		}
		cachedClass.columns = new String[cachedClass.fieldAnnotations.size()];
		int i = 0;
		for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			cachedClass.columns[i++] = cachedClass.columnName(entry.getKey(), true);
			if (entry.getValue().id() || entry.getValue().generatedId()) {
				cachedClass.idEntry = entry;
			}
		}
		if (!hasIdField) {
			throw new IllegalArgumentException("Class " + cls.getSimpleName() + " didn't have an id field");
		}
		if (!hasPersistantField) {
			throw new IllegalArgumentException("Class " + cls.getSimpleName() + " has no persistant field");
		}
		return cachedClass;
	}
	
	static class CachedClass {
		Map<Field, PersistantField> fieldAnnotations;
		Entry<Field, PersistantField> idEntry;
		PersistantObject persistantObject;
		Class<?> cls;
		String idColName;
		String[] columns;
		int primaryId = 0;
		
		String columnName(Field field, boolean tickSafe) {
			PersistantField persistantField = fieldAnnotations.get(field);
			String columnName = persistantField.columnName().equals("") ? field.getName() : persistantField.columnName();
			return tickSafe ? BcOrm.tickAndCombine(columnName) : columnName;
		}
		
		String tableName() {
			return tableName(true);
		}
		
		String tableName(boolean tickSafe) {
			String tableName = persistantObject.tableName().equals("") ? cls.getSimpleName() : persistantObject.tableName();
			return tickSafe ? BcOrm.tickAndCombine(tableName) : tableName;
		}
	}
	
}
