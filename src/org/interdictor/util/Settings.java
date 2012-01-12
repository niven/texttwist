package org.interdictor.util;

import java.util.HashMap;
import java.util.Map;

public class Settings {

	public static final long ROUND_LENGTH = 2 * 60 * 1000; // 2 minute round
	public static final int ROUND_LENGTH_PENALTY = 5 * 1000; // 5 sec shorter rounds after the first

	public static final int POINTS_PER_WORD = 100;
	public static final int POINTS_ALL_WORDS = 1000;
	public static final int POINTS_PER_LETTER = 10;
	public static final int POINT_NO_SHUFFLE = 250;
	public static final int POINTS_PER_SECOND_LEFT = 5;
	public static final Map<String, String> lookupURLs = new HashMap<String, String>();
	
	static {
		lookupURLs.put("en", "http://m.dictionary.com/d/?q=%s");
		lookupURLs.put("nl", "http://www.woorden.org/index.php?woord=%s");
	}
	
}
