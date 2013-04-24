package com.bclymer.bcliteorm;

import android.database.sqlite.SQLiteDatabase;

public class BcOrm {

	static SQLiteDatabase db;
	
	public static void beginTransaction() {
		db = BcSQLiteOpenHelper.getMyWritableDatabase();
		db.beginTransaction();
	}
	
	public static void setTransactionSuccessful() {
		db.setTransactionSuccessful();
	}
	
	public static void endTransaction() {
		db.endTransaction();
		db.close();
		db = null;
	}
	
	public static void close(SQLiteDatabase dbToClose) {
		if (dbToClose != db) {
			dbToClose.close();
		}
	}
	
}
