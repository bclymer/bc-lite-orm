package com.bclymer.bcliteorm;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

public class BcOrm {

	static SQLiteDatabase db;
	static List<BcOrmChangeListener> listeners = new ArrayList<BcOrmChangeListener>(1);
	
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
	
	public static void addEventListener(BcOrmChangeListener listener) {
		listeners.add(listener);
	}
	
	public static void removeEventListener(BcOrmChangeListener listener) {
		listeners.remove(listener);
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
