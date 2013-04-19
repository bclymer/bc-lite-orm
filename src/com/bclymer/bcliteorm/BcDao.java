package com.bclymer.bcliteorm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bclymer.bcliteorm.BcCache.CachedClass;
import com.bclymer.bcliteorm.annotations.PersistantField;

public class BcDao<T, KD> {

	private CachedClass cachedClass;
	private Class<T> cls;

	public BcDao(Class<T> cls) {
		cachedClass = BcCache.cacheClass(cls);
		this.cls = cls;
	}

	public T queryById(KD key) {
		return queryOne(new Query(cachedClass.idColName + " = ?", new String[] { key.toString() }, null, null, null));
	}

	public List<T> queryForEq(String column, String value) {
		return queryMany(new Query(column + " = ?", new String[] { value }, null, null, null));
	}

	public List<T> queryForAll() {
		return queryMany(new Query(null, null, null, null, null));
	}

	private T queryOne(Query query) {
		SQLiteDatabase db = null;
		Cursor c = null;
		T instance = null;
		try {
			instance = (T) cls.newInstance();
			db = BcSQLiteOpenHelper.getMyReadableDatabase();
			c = db.query(cachedClass.tableName(), cachedClass.columns, query.selection, query.selectionArgs, query.groupBy, query.having, query.orderBy);
			if (c.getCount() == 0) {
				return null;
			}
			c.moveToFirst();
			matchCursorToObject(c, instance);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (c != null) c.close();
			if (db != null) db.close();
		}
		return instance;
	}

	private List<T> queryMany(Query query) {
		SQLiteDatabase db = null;
		Cursor c = null;
		List<T> results = null;
		try {
			db = BcSQLiteOpenHelper.getMyReadableDatabase();
			c = db.query(cachedClass.tableName(), cachedClass.columns, query.selection, query.selectionArgs, query.groupBy, query.having, query.orderBy);
			results = new ArrayList<T>(c.getCount());
			c.moveToFirst();
			while (!c.isAfterLast()) {
				T instance = (T) cls.newInstance();
				matchCursorToObject(c, instance);
				results.add(instance);
				c.moveToNext();
			}
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (c != null) c.close();
			if (db != null) db.close();
		}
		return results;
	}

	private void matchCursorToObject(Cursor c, T instance) throws IllegalArgumentException, IllegalAccessException {
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
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

	public long insert(T object) {
		SQLiteDatabase db = null;
		long id = -1;
		try {
			db = BcSQLiteOpenHelper.getMyWritableDatabase();
			id = insertObject(object, db);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) db.close();
		}
		return id;
	}

	public void insertMany(List<T> objects) {
		SQLiteDatabase db = null;
		try {
			db = BcSQLiteOpenHelper.getMyWritableDatabase();
			db.beginTransaction();
			for (T obj : objects) {
				insertObject(obj, db);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) db.close();
		}
	}

	public int update(T object)  {
		SQLiteDatabase db = null;
		int id = -1;
		try {
			db = BcSQLiteOpenHelper.getMyWritableDatabase();
			id = updateObject(object, db);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) db.close();
		}
		return id;
	}

	public void updateMany(List<T> objects) {
		SQLiteDatabase db = null;
		try {
			db = BcSQLiteOpenHelper.getMyWritableDatabase();
			db.beginTransaction();
			for (T obj : objects) {
				updateObject(obj, db);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) db.close();
		}
	}

	public int delete(T object) {
		SQLiteDatabase db = null;
		int id = -1;
		try {
			db = BcSQLiteOpenHelper.getMyWritableDatabase();
			id = deleteObject(object, db);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) db.close();
		}
		return id;
	}

	public void deleteMany(List<T> objects) {
		SQLiteDatabase db = null;
		try {
			db = BcSQLiteOpenHelper.getMyWritableDatabase();
			db.beginTransaction();
			for (T obj : objects) {
				deleteObject(obj, db);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) db.close();
		}
	}
	
	public void clearTable() {
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyWritableDatabase();
		db.delete(cachedClass.tableName(), null, null);
		db.close();
	}

	private int updateObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		ContentValues values = new ContentValues();
		String idValue = null;
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			if (cachedClass.columnName(entry.getKey()).equals(cachedClass.idColName)) {
				idValue = entry.getKey().get(object).toString();
			} else {
				values.put(cachedClass.columnName(entry.getKey()), entry.getKey().get(object).toString());
			}
		}
		return db.update(cachedClass.tableName(), values, cachedClass.idColName + " = ?", new String[] { idValue });
	}

	private long insertObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		ContentValues values = new ContentValues();
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			values.put(cachedClass.columnName(entry.getKey()), entry.getKey().get(object).toString());
		}
		return db.insert(cachedClass.tableName(), null, values);
	}

	private int deleteObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		String idValue = null;
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			if (cachedClass.columnName(entry.getKey()).equals(cachedClass.idColName)) {
				idValue = entry.getKey().get(object).toString();
				break;
			}
		}
		return db.delete(cachedClass.tableName(), cachedClass.idColName + " = ?", new String[] { idValue });
	}

	private class Query {
		String selection;
		String[] selectionArgs;
		String groupBy;
		String having;
		String orderBy;

		public Query(String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
			this.selection = selection;
			this.selectionArgs = selectionArgs;
			this.groupBy = groupBy;
			this.having = having;
			this.orderBy = orderBy;
		}
	}
}
