package com.bclymer.bcliteorm;

import android.util.Log;

public class BcLog {

	private static final String TAG = "BcLog";
	private static boolean loggingEnabled = false;

	public static void enableLogging(boolean enableLogging) {
		loggingEnabled = enableLogging;
	}

	public static void v(String msg) {
		if (loggingEnabled)
			Log.v(TAG, msg);
	}

	public static void d(String msg) {
		if (loggingEnabled)
			Log.d(TAG, msg);
	}

	public static void i(String msg) {
		if (loggingEnabled)
			Log.i(TAG, msg);
	}

	public static void w(String msg) {
		if (loggingEnabled)
			Log.w(TAG, msg);
	}

	public static void e(String msg) {
		if (loggingEnabled)
			Log.e(TAG, msg);
	}

}
