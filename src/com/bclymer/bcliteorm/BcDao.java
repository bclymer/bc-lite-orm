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
	
	public QueryBuilder queryBuilder() {
		return new QueryBuilder();
	}
	
	public List<T> query(Query query) {
		return queryMany(query);
	}

	public T queryById(KD key) {
		return queryOne(new Query(cachedClass.idColName + " = ?", new String[] { key.toString() }, null, null, null, null));
	}

	public List<T> queryForEq(String column, String value) {
		return queryMany(new Query(column + " = ?", new String[] { value }, null, null, null, null));
	}

	public List<T> queryForAll() {
		return queryMany(new Query(null, null, null, null, null, null));
	}
	
	public void refresh(T object) {
		try {
			String id = cachedClass.idEntry.getKey().get(object).toString();
			T newObject = queryOne(new Query(cachedClass.idColName + " = ?", new String[] { id }, null, null, null, null));
			for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
				entry.getKey().set(object, entry.getKey().get(newObject));
			}
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<T> fetchAllChildrenOfObject(Object o) {
		CachedClass foreignCachedClass = BcCache.cacheClass(o.getClass());
		try {
			String colunmName = null;
			for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
				if (entry.getValue().foreign() && entry.getKey().getType().equals(o.getClass())) {
					colunmName = cachedClass.columnName(entry.getKey());
				}
			}
			if (colunmName == null) {
				throw new RuntimeException("Class " + cls + " does not have a foreign object of class " + o.getClass());
			}
			String id = foreignCachedClass.idEntry.getKey().get(o).toString();
			return queryMany(new Query(colunmName + " = ?", new String[] { id }, null, null, null, null));
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private T queryOne(Query query) {
		BcLog.i(query.toString());
		SQLiteDatabase db = null;
		Cursor c = null;
		T instance = null;
		try {
			instance = (T) cls.newInstance();
			db = BcSQLiteOpenHelper.getMyReadableDatabase();
			c = db.query(cachedClass.tableName(), cachedClass.columns, query.selection, query.selectionArgs, query.groupBy, query.having, query.orderBy, query.limit);
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
			if (db != null) BcOrm.close(db);
		}
		return instance;
	}

	private List<T> queryMany(Query query) {
		BcLog.i(query.toString());
		SQLiteDatabase db = null;
		Cursor c = null;
		List<T> results = null;
		try {
			db = BcSQLiteOpenHelper.getMyReadableDatabase();
			c = db.query(cachedClass.tableName(), cachedClass.columns, query.selection, query.selectionArgs, query.groupBy, query.having, query.orderBy, query.limit);
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
			if (db != null) BcOrm.close(db);
		}
		return results;
	}

	private void matchCursorToObject(Cursor c, T instance) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			switch (TableUtil.typeToSQLString(entry)) {
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
			case FOREIGN:
				CachedClass foreignCachedClass = BcCache.cacheClass(entry.getKey().getType());
				Object o = entry.getKey().getType().newInstance();
				entry.getKey().set(instance, o);
				switch (TableUtil.typeToSQLString(foreignCachedClass.idEntry)) {
				case BLOB:
					foreignCachedClass.idEntry.getKey().set(o, c.getBlob(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
					break;
				case INTEGER:
					foreignCachedClass.idEntry.getKey().set(o, c.getInt(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
					break;
				case REAL:
					foreignCachedClass.idEntry.getKey().set(o, c.getDouble(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
					break;
				case TEXT:
					foreignCachedClass.idEntry.getKey().set(o, c.getString(c.getColumnIndex(cachedClass.columnName(entry.getKey()))));
					break;
				case FOREIGN:
					throw new IllegalArgumentException("ID field can not be a foreign key");
				}
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
			if (db != null) BcOrm.close(db);
		}
		return id;
	}

	public void insertMany(List<T> objects) {
		SQLiteDatabase db = null;
		try {
			db = getTransactionReadyDb();
			for (T obj : objects) {
				insertObject(obj, db);
			}
			endTransactionReadyDb(db);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) BcOrm.close(db);
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
			if (db != null) BcOrm.close(db);
		}
		return id;
	}

	public void updateMany(List<T> objects) {
		SQLiteDatabase db = null;
		try {
			db = getTransactionReadyDb();
			for (T obj : objects) {
				updateObject(obj, db);
			}
			endTransactionReadyDb(db);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) BcOrm.close(db);
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
			if (db != null) BcOrm.close(db);
		}
		return id;
	}

	public void deleteMany(List<T> objects) {
		SQLiteDatabase db = null;
		try {
			db = getTransactionReadyDb();
			for (T obj : objects) {
				deleteObject(obj, db);
			}
			endTransactionReadyDb(db);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			if (db != null) BcOrm.close(db);
		}
	}
	
	public void clearTable() {
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyWritableDatabase();
		db.delete(cachedClass.tableName(), null, null);
		BcOrm.close(db);
	}

	private int updateObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		ContentValues values = new ContentValues();
		String idValue = cachedClass.idEntry.getKey().get(object).toString();
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			if (!cachedClass.columnName(entry.getKey()).equals(cachedClass.idColName)) {
				values.put(cachedClass.columnName(entry.getKey()), entry.getKey().get(object).toString());
			}
		}
		return db.update(cachedClass.tableName(), values, cachedClass.idColName + " = ?", new String[] { idValue });
	}

	private long insertObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		ContentValues values = new ContentValues();
		for (Entry<Field, PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			if (!entry.getValue().foreign()) {
				values.put(cachedClass.columnName(entry.getKey()), entry.getKey().get(object).toString());
			} else {
				CachedClass foreignCachedClass = BcCache.cacheClass(entry.getKey().getType());
				Object o = entry.getKey().get(object);
				if (o != null) {
					values.put(cachedClass.columnName(entry.getKey()), foreignCachedClass.idEntry.getKey().get(o).toString());
				}
			}
		}
		return db.insert(cachedClass.tableName(), null, values);
	}

	private int deleteObject(T object, SQLiteDatabase db) throws IllegalArgumentException, IllegalAccessException {
		String idValue = cachedClass.idEntry.getKey().get(object).toString();
		return db.delete(cachedClass.tableName(), cachedClass.idColName + " = ?", new String[] { idValue });
	}
	
	private SQLiteDatabase getTransactionReadyDb() {
		SQLiteDatabase db = BcSQLiteOpenHelper.getMyWritableDatabase();
		if (db != BcOrm.db) {
			db.beginTransaction();
		}
		return db;
	}
	
	private void endTransactionReadyDb(SQLiteDatabase db) {
		if (db != BcOrm.db) {
			db.setTransactionSuccessful();
			db.endTransaction();
		}
	}

	public class Query {
		String selection;
		String[] selectionArgs;
		String groupBy;
		String having;
		String orderBy;
		String limit;

		public Query(String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
			this.selection = selection;
			this.selectionArgs = selectionArgs;
			this.groupBy = groupBy;
			this.having = having;
			this.orderBy = orderBy;
			this.limit = limit;
		}
	}
	
	public class QueryBuilder {
		
		private Query query;
		private List<String> selection;
		private List<String> selectionArgs;
		
		QueryBuilder() {
			query = new Query(null, null, null, null, null, null);
			selection = new ArrayList<String>();
			selectionArgs = new ArrayList<String>();
		}
		
		public QueryBuilder eq(String column, String value) {
			selection.add(BcOrm.tickAndCombine(column, " = ?"));
			selectionArgs.add(value);
			return this;
		}
		
		public QueryBuilder gt(String column, String value) {
			selection.add(BcOrm.tickAndCombine(column, " > ?"));
			selectionArgs.add(value);
			return this;
		}
		
		public QueryBuilder lt(String column, String value) {
			selection.add(BcOrm.tickAndCombine(column, " < ?"));
			selectionArgs.add(value);
			return this;
		}
		
		public QueryBuilder gte(String column, String value) {
			selection.add(BcOrm.tickAndCombine(column, " >= ?"));
			selectionArgs.add(value);
			return this;
		}
		
		public QueryBuilder lte(String column, String value) {
			selection.add(BcOrm.tickAndCombine(column, " <= ?"));
			selectionArgs.add(value);
			return this;
		}
		
		public QueryBuilder having(String having) {
			query.having = BcOrm.tickAndCombine(having);
			return this;
		}
		
		public QueryBuilder orderBy(String orderBy, boolean ASC) {
			query.orderBy = BcOrm.tickAndCombine(orderBy, (ASC ? " ASC" : " DESC"));
			return this;
		}
		
		public QueryBuilder groupBy(String groupBy) {
			query.groupBy = BcOrm.tickAndCombine(groupBy);
			return this;
		}
		
		public QueryBuilder limit(long limit) {
			query.limit = Long.toString(limit);
			return this;
		}
		
		public List<T> execute() {
			return query(prepare());
		}
		
		public Query prepare() {
			query.selectionArgs = new String[selectionArgs.size()];
			selectionArgs.toArray(query.selectionArgs);
			int i = 0;
			StringBuilder s = new StringBuilder();
			for (String sel : selection) {
				s.append(sel);
				if (++i != selection.size()) {
					s.append(" AND ");
				}
			}
			query.selection = s.toString();
			return query;
		}
		
	}
}
