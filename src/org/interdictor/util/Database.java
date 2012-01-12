package org.interdictor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.interdictor.Score;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Database {

	private static String DB_NAME = "ttdb";
	private static String SCORES = "scores";
	private static final String TABLE_CREATE_SCORES = "CREATE TABLE IF NOT EXISTS " + SCORES + " (timestamp INTEGER, score INTEGER, round INTEGER, language TEXT);";

	private static Database instance;

	Context context;
	private SQLiteDatabase db;

	private Database(Context context) {
		this.context = context;

		// debug add random score
		// Random rnd = new Random();
		// Score s = new Score((int) (System.currentTimeMillis()/1000), rnd.nextInt(10000), rnd.nextInt(10));
		// saveScore(s);
	}

	public static Database getInstance(Context context) {
		if (instance == null) {
			instance = new Database(context);
		}
		return instance;
	}

	private void open() {

		db = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
//		db.execSQL("DROP TABLE " + SCORES);


		db.execSQL(TABLE_CREATE_SCORES);
	}

	public void saveScore(Score s) {
		open();
		ContentValues data = new ContentValues();
		data.put("timestamp", s.timestamp);
		data.put("score", s.score);
		data.put("round", s.round);
		data.put("language", s.locale.getLanguage());
		db.insert(SCORES, null, data);
		close();
	}

	private void close() {
		db.close();
	}

	public List<Score> getHighScores(Locale locale) {
		Log.print("Getting highscores for " + locale.getLanguage());
		open();
		Cursor scores = db.query(SCORES, // scores
				null, // all columns
				"language = ?", // WHERE clause (my include ? placeholders)
				new String[]{locale.getLanguage()}, // values for where clause placeholders
				null, // group by nothing
				null, // having nothing
				"score DESC", // order by
				"10" // limit
		);
		List<Score> out = new ArrayList<Score>();
		if (scores.moveToFirst()) {
			do {
				Score s = new Score(scores.getInt(0), scores.getInt(1), scores.getInt(2), new Locale(scores.getString(3)));
				Log.print("Score: " + s);
				out.add(s);
			} while (scores.moveToNext());
		}
		close();

		Log.print("# Highscores: " + out.size());

		return out;
	}

}
