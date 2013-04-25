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
	
	static void close(SQLiteDatabase dbToClose) {
		if (dbToClose != db) {
			dbToClose.close();
		}
	}
	
	static String tickAndCombine(String param, String... append) {
		StringBuilder str = new StringBuilder("`").append(param).append("`");
		for (String string : append) {
			str.append(string);
		}
		return str.toString();
	}
	
}
