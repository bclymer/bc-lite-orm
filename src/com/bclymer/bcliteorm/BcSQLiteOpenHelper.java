package com.bclymer.bcliteorm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class BcSQLiteOpenHelper extends SQLiteOpenHelper {

	private static BcSQLiteOpenHelper instance;
	
	public BcSQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		instance = this;
	}
	
	public static SQLiteDatabase getMyReadableDatabase() {
		if (BcOrm.db != null) {
			return BcOrm.db;
		}
		return instance.getReadableDatabase();
	}

	public static SQLiteDatabase getMyWritableDatabase() {
		if (BcOrm.db != null) {
			return BcOrm.db;
		}
		synchronized (instance) {
			return instance.getWritableDatabase();
		}
	}
}
