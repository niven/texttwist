package org.interdictor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * game data + stores all rounds
 * 
 * 
 */
public class Game {

	private boolean gameOver = false;
	private List<Roundstate> rounds;
	private Locale locale;

	public Game() {
		rounds = new ArrayList<Roundstate>();
	}

	public void setGameIsOver(boolean state) {
		gameOver = state;
	}

	public int getCurrentRound() {
		return rounds.size();
	}

	public boolean over() {

		return gameOver;
	}

	public void saveRound(Roundstate state) {
		rounds.add(state);

	}

	public Roundstate getLastRound() {
		return rounds.isEmpty() ? null : rounds.get(rounds.size() - 1);
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		
	}

	public Locale getLocale() {
		return locale;
	}

}
