package com.bclymer.bcliteorm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bclymer.bcliteorm.BcCache.CachedClass;
import com.bclymer.bcliteorm.annotations.PersistantField;

public class BcDao<T, KD> {

	public T queryById(KD key, Class<T> cls) throws InstantiationException, IllegalAccessException {
		T instance = (T) cls.newInstance();
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyReadableDatabase();
		CachedClass cachedClass = BcCache.cacheClass(cls);
		Cursor c = db.query(cachedClass.tableName(), cachedClass.columns, cachedClass.idColName + " = ?", new String[] { key.toString() }, null, null, null);
		c.moveToFirst();
		matchCursorToObject(c, instance, cachedClass);
		c.close();
		db.close();
		return instance;
	}
	
	public List<T> queryForAll(Class<T> cls) throws InstantiationException, IllegalAccessException {
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyReadableDatabase();
		CachedClass cachedClass = BcCache.cacheClass(cls);
		Cursor c = db.query(cachedClass.tableName(), cachedClass.columns, null, null, null, null, null);
		List<T> results = new ArrayList<T>(c.getCount());
		c.moveToFirst();
		while (!c.isAfterLast()) {
			T instance = (T) cls.newInstance();
			matchCursorToObject(c, instance, cachedClass);
			results.add(instance);
			c.moveToNext();
		}
		c.close();
		db.close();
		return results;
	}
	
	private void matchCursorToObject(Cursor c, T instance, CachedClass cachedClass) throws IllegalArgumentException, IllegalAccessException {
		for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			switch (TableUtil.typeToSQLString(entry.getKey().getType())) {
			case BLOB:
				entry.getKey().set(instance, c.getBlob(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
				break;
			case INTEGER:
				entry.getKey().set(instance, c.getInt(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
				break;
			case REAL:
				entry.getKey().set(instance, c.getDouble(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
				break;
			case TEXT:
				entry.getKey().set(instance, c.getString(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
				break;
			}
		}
	}
	
	public void insertMany(List<T> objects) throws IllegalArgumentException, IllegalAccessException {
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyWritableDatabase();
		db.beginTransaction();
		for (T obj : objects) {
			insertObject(obj, db);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}
	
	public void insert(T object) throws IllegalArgumentException, IllegalAccessException {
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyWritableDatabase();
		insertObject(object, db);
		db.close();
	}
	
	private void insertObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		CachedClass cachedClass = BcCache.cacheClass(object.getClass());
		StringBuilder s = new StringBuilder("INSERT INTO ");
		s.append("`" + cachedClass.tableName() + "` (");
		int i = 0;
		for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			s.append("`" + cachedClass.columnName(entry.getKey()) + "`");
			if (++i != cachedClass.fieldAnnotations.entrySet().size()) {
				s.append(", ");
			}
		}
		i = 0;
		s.append(") VALUES (");
		for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			s.append("'" + entry.getKey().get(object) + "'");
			if (++i != cachedClass.fieldAnnotations.entrySet().size()) {
				s.append(", ");
			}
		}
		s.append(")");
		String query = s.toString();
		BcLog.i(query);
		db.execSQL(query);
	}
}
