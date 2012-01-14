package org.interdictor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.interdictor.util.Settings;

class Roundstate {

	public Roundstate(int initialScore, long roundLength) {
		score = initialScore; //carry on where we left off
		this.roundLength = roundLength;
	}
	public String selectedWord;

	// keep track of letters we pressed for easy undo
	// (I'm sure there is a more elegant way)
	public List<Integer> letterSequence = new ArrayList<Integer>();

	public Thread clock;
	long roundLength = Settings.ROUND_LENGTH;
	long start;
	int score = 0;
	String guess = "";
	boolean sixLetterWordGuessed = false;
	List<String> validWords = new ArrayList<String>();
	Set<String> alreadyGuessed = new HashSet<String>();

	public boolean shuffled;

	protected long elapsed;

	public int targetScore; // this is where we are going from score so we can animate counting up points

	public boolean isUpdatingScore = false;
}