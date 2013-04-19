package com.bclymer.bcliteorm;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map.Entry;

import android.database.sqlite.SQLiteDatabase;

import com.bclymer.bcliteorm.BcCache.CachedClass;
import com.bclymer.bcliteorm.annotations.PersistantField;

public class TableUtil {

	public static void createTable(SQLiteDatabase db, Class<?> cls) {
		CachedClass cachedClass = BcCache.cacheClass(cls);
		
		StringBuilder s = new StringBuilder("CREATE TABLE ");
		String className = cachedClass.persistantObject.tableName() == "" ? cls.getSimpleName() : cachedClass.persistantObject.tableName();
		s.append('`' + className + '`');
		s.append(" (");
		
		int i = 0;
		for (Entry<Field,PersistantField> fieldAnnotation : cachedClass.fieldAnnotations.entrySet()) {
			s.append('`' + cachedClass.columnName(fieldAnnotation.getKey()) + '`');
			s.append(" ");
			s.append(typeToSQLString(fieldAnnotation.getKey().getType()).name());
			if (fieldAnnotation.getValue().id()) {
				s.append(" PRIMARY KEY");
			} else if (fieldAnnotation.getValue().generatedId()) {
				s.append(" PRIMARY KEY AUTOINCREMENT");
			}
			if (++i == cachedClass.fieldAnnotations.size()) {
				s.append(");");
			} else {
				s.append(",");
			}
		}
		String query = s.toString();
		BcLog.i(query);
		db.execSQL(query);
	}
	
	public static void dropTable(SQLiteDatabase db, Class<?> cls) {
		CachedClass cachedClass = BcCache.cacheClass(cls);
		StringBuilder s = new StringBuilder("DROP TABLE IF EXISTS ");
		s.append('`' + cachedClass.tableName() + '`');
		String query = s.toString();
		BcLog.i(query);
		db.execSQL(query);
	}
	
	public static SqlType typeToSQLString(Class<?> cls) {
		if (cls.equals(Integer.TYPE) || cls.equals(Integer.class)) {
			return SqlType.INTEGER;
		} else if (cls.equals(String.class)) {
			return SqlType.TEXT;
		} else if (cls.equals(Double.TYPE) || cls.equals(Double.class) || cls.equals(Long.TYPE) || cls.equals(Long.class)) {
			return SqlType.REAL;
		} else if (cls instanceof Serializable) {
			return SqlType.BLOB;
		}
		throw new IllegalArgumentException("Type " + cls.getSimpleName() + " is not supported");
	}
	
	public enum SqlType {
		INTEGER,
		TEXT,
		REAL,
		BLOB;
	}
	
}
