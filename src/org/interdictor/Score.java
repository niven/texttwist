package org.interdictor;

import java.util.Locale;

public class Score {

	public final int timestamp, score, round;
	public final Locale locale;

	public Score(int timestamp, int score, int round, Locale locale) {
		this.timestamp = timestamp;
		this.score = score;
		this.round = round;
		this.locale = locale;
	}

	@Override
	public String toString() {
		return "Score: " + score + " (" + round + " rounds, ts: " + timestamp + " lc: " + (locale == null ? " no locale" : locale.getLanguage()) + ")";
	}
}
