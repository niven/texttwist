package org.interdictor;

import java.util.Locale;

import org.interdictor.util.Log;

import android.app.Application;

public class TextTwistApplication extends Application {

	private Game game;
	private Locale locale = Locale.ENGLISH;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.print("Init Global State");
	}
	
	public Game getGame() {
		if(game == null) {
			newGame();
		}
		
		return game;
	}
	
	public void newGame() {
		game = new Game();
	}

	public void setLocale(Locale locale) {
		this.locale=locale;
		Log.print("Language set to " + this.locale.getDisplayLanguage());
	}

	public Locale getLocale() {
		return locale;
	}

}
