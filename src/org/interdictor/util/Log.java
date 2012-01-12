package org.interdictor.util;

public class Log {

	static final String TAG = "tt";
	public static void print(String text) {
		android.util.Log.d(TAG, text);	
	}
}
