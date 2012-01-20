package org.interdictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.interdictor.util.Log;
import org.interdictor.util.Settings;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * 
 * @author chris
 * 
 */
public class TextTwistActivity extends Activity implements OnClickListener {

	Button[] letters = new Button[6];
	Button pause, undo, shuffle, check;
	TextView[] guesses = new TextView[6];
	private List<String> words = new ArrayList<String>();
	private TextView scoreText, timeText, notification, roundText; // where we
																	// show
																	// score,
																	// countdown,
																	// feedback
																	// notifications
																	// and round
																	// respectively
	Roundstate state;
	private Locale locale = Locale.ENGLISH;
	private Map<String, LinearLayout> guessedWordsDisplay = new HashMap<String, LinearLayout>();

	private TextTwistApplication tta;

	private Thread pump; // pump ticks to our main loop
	private AlphaAnimation alphaAnim;
	static boolean stop = false; // clock updater

	static final int SHOW_ROUNDCOMPLETED_ACTIVITY = 1;

	// static initializer for the fade out animation we use for notifications
	{
		alphaAnim = new AlphaAnimation(1.0f, 0);
		alphaAnim.setDuration(2 * 1000);
		alphaAnim.setFillAfter(true);
		alphaAnim.setFillEnabled(true);
	}
	
	
	Handler mainLoop = new Handler() {

		public void handleMessage(Message msg) {
			// we don't want to be showing Dialogs when we're finishing since that causes
			// "android.view.WindowManager$BadTokenException: Unable to add window Ñ token
			// android.os.BinderProxy@447a6748
			// is not valid; is your activity running?"
			if (isFinishing()) {
				return;
			}

			switch (msg.what) {
			case SHOW_ROUNDCOMPLETED_ACTIVITY:
				Intent intent = new Intent(TextTwistActivity.this, RoundCompletedActivity.class);
				startActivity(intent);
				break;
			default:
				handleTick(msg.arg1, msg.arg2);
			}

		}

		/**
		 * Countdown and trigger gameover when it runs out
		 * 
		 * @param min
		 * @param sec
		 */
		private void handleTick(int min, int sec) {
			if (min > 0 || sec > 0) {
				// update the clock
				timeText.setText(getResources().getString(R.string.time_left, min, sec));
				// update scores
				if(state.isUpdatingScore) {
					int scoreDiff = state.targetScore - state.score;
					state.score += scoreDiff > 10 ? 10 : scoreDiff;
					scoreText.setText(getResources().getString(R.string.score, state.score));
					if(state.score ==state.targetScore) { // stop the updating
						state.isUpdatingScore = false;
						scoreText.setTextColor(getResources().getColor(R.color.normal_text));
					}
				}
			} else { // round OVER!
				showFeedback(getResources().getString(R.string.time_up), R.color.notification_red);
				TextTwistActivity.stop = true;
				timeText.setText(getResources().getString(R.string.time_left, 0, 0));
				Log.print("ROUND OVER");
				// show all words
				for (String word : state.validWords) {
					updateGuessedWordDisplay(word);
				}
				if (!state.sixLetterWordGuessed) {
					tta.getGame().setGameIsOver(true);
				}
				tta.getGame().saveRound(state);
				// wait a little bit so the user can still see the feedback
				mainLoop.sendMessageDelayed(mainLoop.obtainMessage(SHOW_ROUNDCOMPLETED_ACTIVITY), 1200);

			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// ensure the screen doesn't dim or go out while you're thinking
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.game);

		tta = (TextTwistApplication) getApplication();

		scoreText = (TextView) findViewById(R.id.score);
		timeText = (TextView) findViewById(R.id.time);
		roundText = (TextView) findViewById(R.id.round);
		notification = (TextView) findViewById(R.id.notify);
		
		pause = (Button) findViewById(R.id.btn_pause);
		
		check = (Button) findViewById(R.id.check_word);
		check.setOnClickListener(this);

		shuffle = (Button) findViewById(R.id.shuffle);
		shuffle.setOnClickListener(this);

		undo = (Button) findViewById(R.id.undo);
		undo.setOnClickListener(this);

		Intent intent = getIntent();
		locale = new Locale(intent.getStringExtra("locale_string"));

		// TODO: do this in a thread and display a spinner
		int fileId = R.raw.twister_words_en;
		if (locale.equals(new Locale("nl"))) {
			fileId = R.raw.twister_words_nl;
		} else {
			fileId = R.raw.twister_words_en;
		}
		BufferedReader data = new BufferedReader(new InputStreamReader(getResources().openRawResource(fileId)));
		String line;
		try {
			while ((line = data.readLine()) != null) {
				words.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				data.close();
			} catch (IOException e) {
				// don't care
			}
		}

		// setup the letter and guess-letter areas
		for (int i = 0; i < 6; i++) {
			Button l = (Button) findViewById(getResources().getIdentifier("letter" + i, "id", getPackageName()));
			l.setOnClickListener(this);
			l.setVisibility(View.INVISIBLE);
			letters[i] = l;
			TextView g = (TextView) findViewById(getResources().getIdentifier("guess" + i, "id", getPackageName()));
			guesses[i] = g;
			findViewById(R.id.check_word).setVisibility(View.INVISIBLE);
		}

		Log.print("DONE WITN ONCREATE");

	}

	@Override
	protected void onPause() {
		super.onPause();
		stop = true; // make the pump thread stop
	}

	@Override
	protected void onResume() {
		super.onResume();

		/**
		 * Seems like a good startpoint for the game: this happens after we create the first time, then again after we
		 * show the RoundCompletedActivity (so we go to next round or finish)
		 */

		if (tta.getGame().over()) {
			// enable us playing another game
			tta.newGame();
			finish();
			return;
		}

		Log.print("Starting round: " + tta.getGame().getCurrentRound());
		scoreText.setTextColor(getResources().getColor(R.color.normal_text)); // make sure the score text doesn't remain green
		startGame();
	}

	// holy shit is this slow
	private void createGuessedWordsGrid() {
		TableLayout tl = (TableLayout) findViewById(R.id.grid);
		tl.removeAllViews();
		List<String> copy = new ArrayList<String>(state.validWords);
		// get a higher than wide grid
		int rows = 3;
		for (int i = 0; i < rows; i++) {
			TableRow tr = new TableRow(getApplicationContext());
			for (int j = 0; j < Math.ceil((double) state.validWords.size() / 3.0); j++) {
				if (copy.isEmpty()) {
					break;
				}
				LinearLayout ll = new LinearLayout(getApplicationContext());
				String word = copy.remove(0);
				for (@SuppressWarnings("unused")
				char c : word.toCharArray()) { // 1 box for every letter
					TextView t = new TextView(getApplicationContext());
					t.setTextAppearance(getApplicationContext(), R.style.ButtonText_Found);
					t.setBackgroundDrawable(getResources().getDrawable(R.drawable.letter_small_unguessed));
					t.setText(" "); // space so our grid looks nice
					ll.addView(t);
				}
				tr.addView(ll);
				guessedWordsDisplay.put(word, ll);
			}
			tl.addView(tr);
		}
	}

	@Override
	public void onClick(View v) {
		if(state.paused) { // just don't react to letter buttons when pausing
			return;
		}
		Object tag = v.getTag();
		if (tag != null) { // its a button
			processButton(Integer.parseInt((String) tag));
			return;
		}
		switch (v.getId()) {
		case R.id.check_word:

			guessWord();
			break;
		case R.id.undo:
			undo();
			break;
		case R.id.shuffle:
			shuffle();
			break;
		}
	}

	/**
	 * Show some text in the player notification area (middle top of game screen).
	 * This text fades out
	 * @param text
	 * @param colorId
	 */
	protected void showFeedback(String text, int colorId) {
		notification.setText(text);
		notification.setTextColor(getResources().getColor(colorId));
		notification.startAnimation(alphaAnim);
	}

	/**
	 * Shuffle the list of input letters (and clear the current guess)
	 */
	private void shuffle() {

		state.shuffled = true; // no bonus :)
		state.guess = ""; // reset guess
		state.letterSequence.clear(); // clear buttons pressed

		// shuffle the buttons around
		final Random rnd = new Random();
		Integer[] order = new Integer[] { 0, 1, 2, 3, 4, 5 };
		Arrays.sort(order, new Comparator<Integer>() {

			@Override
			public int compare(Integer object1, Integer object2) {
				return rnd.nextInt();
			}
		});
		Log.print("New order: " + Arrays.asList(order).toString());
		// reset the letters display
		for (int i = 0; i < 6; i++) {
			letters[i].setText("" + state.selectedWord.charAt(order[i]));
			letters[i].setVisibility(View.VISIBLE);
			guesses[i].setVisibility(View.INVISIBLE);
		}

	}

	/**
	 * Undo last letter pressed
	 */
	private void undo() {
		if (state.letterSequence.size() > 0) { // only if we pressed any
												// something
			Integer letterIdx = state.letterSequence.remove(0);
			Log.print("Undo: " + state.guess + " Undoing: " + letterIdx);
			int guessLength = state.guess.length();
			// reduce the guess by 1 character
			state.guess = state.guess.substring(0, guessLength - 1);
			guesses[guessLength - 1].setVisibility(View.INVISIBLE); // hide a
																	// guessed
																	// letter
			letters[letterIdx].setVisibility(View.VISIBLE); // show the last
															// letter pressed
		}
	}

	private void guessWord() {
		Log.print("guessWord START");

		if (state.validWords.contains(state.guess) && !state.alreadyGuessed.contains(state.guess)) {
			int points = Settings.POINTS_PER_WORD + (Settings.POINTS_PER_LETTER * state.guess.length());
			state.targetScore = state.score + points;
			state.isUpdatingScore = true;
			Log.print("CORRECT: " + state.guess);
			state.alreadyGuessed.add(state.guess);
			if (state.guess.length() == 6) {
				state.sixLetterWordGuessed = true;
			}
			scoreText.setTextColor(getResources().getColor(R.color.notification_green));
			showFeedback(getResources().getString(R.string.points_plus, points), R.color.notification_green);
			updateGuessedWordDisplay(state.guess);

			// go to next round if the player guessed all the words
			if (state.alreadyGuessed.size() == state.validWords.size() || Settings.DEBUG) {
				stop = true; // so we never also get the timeup trigger
				state.score = state.targetScore; // make sure we don't lose points
				tta.getGame().saveRound(state);
				// wait a little bit so the user can still see his word was ok
				mainLoop.sendMessageDelayed(mainLoop.obtainMessage(SHOW_ROUNDCOMPLETED_ACTIVITY), 1200);
				// TODO: make buttons unclickable or something
			}
		} else if (state.alreadyGuessed.contains(state.guess)) { // already
																	// guessed
																	// word
			showFeedback(getResources().getString(R.string.word_already_guessed), R.color.notification_orange);
		} else { // incorrect word
			showFeedback(getResources().getString(R.string.word_incorrect), R.color.notification_red);
		}

		state.guess = "";
		// reset the letters display
		for (int i = 0; i < 6; i++) {
			guesses[i].setVisibility(View.INVISIBLE);
			letters[i].setVisibility(View.VISIBLE);
		}
		// reset the letterSequence pressed
		state.letterSequence.clear();
		Log.print("Done with guessWord");
	}

	/**
	 * update the guessed words display
	 * 
	 * @param word
	 */
	private void updateGuessedWordDisplay(String word) {
		Log.print("Showing :" + word);
		LinearLayout ll = guessedWordsDisplay.get(word);
		char[] letters = word.toCharArray();
		for (int i = 0; i < letters.length; i++) {
			TextView t = (TextView) ll.getChildAt(i);
			t.setText("" + letters[i]);
			if (state.alreadyGuessed.contains(word)) {
				t.setBackgroundDrawable(getResources().getDrawable(R.drawable.letter_small));
			} else { // not used unless game is over
				t.setBackgroundDrawable(getResources().getDrawable(R.drawable.letter_small_unguessed));
			}
		}
	}
	
	public void pauseGame(View v) {
		Log.print("Pause Game");
		Resources resources = getResources();
		if(state.paused) {
			state.timeOfLastTick = System.currentTimeMillis(); // resume counting from now
			state.paused = false;
			setEnabled(true, letters);
			setEnabled(true, guesses);
			setEnabled(true, check, undo, shuffle);
			pause.setText(resources.getString(R.string.btn_pause));
		} else {			
			state.paused = true;
			setEnabled(false, letters);
			setEnabled(false, guesses);
			setEnabled(false, check, undo, shuffle);
			pause.setText(resources.getString(R.string.btn_resume));
			showFeedback(resources.getString(R.string.btn_pause), R.color.notification_green);
		}
	}

	/**
	 * Disable a bunch of things (could be buttons) by blurring etc
	 * @param textViews
	 */
	protected void setEnabled(boolean enable, TextView... textViews) {
		Resources resources = getResources();
		int normalTextColor = resources.getColor( R.color.normal_text);
		int transparantTextColor = resources.getColor( R.color.transparant);
		for(TextView tv : textViews) {
			tv.setEnabled(enable); // don't do the press-animation
			if(enable) {
				tv.setTextColor(normalTextColor);
				tv.setShadowLayer(0f, 0f, 0f, normalTextColor);
			} else {
				tv.setTextColor(transparantTextColor);
				tv.setShadowLayer(10f, 0f, 0f, normalTextColor);
			}
		}
	}
	
	protected void startGame() {
		// initialize our score from the previous round and make rounds shorter
		Game game = tta.getGame();
		int currentRound = game.getCurrentRound();
		long roundLength = Settings.ROUND_LENGTH - ((currentRound < 12 ? currentRound : 12) * Settings.ROUND_LENGTH_PENALTY);
		state = new Roundstate(currentRound == 0 ? 0 : game.getLastRound().score, roundLength);

		roundText.setText(getResources().getString(R.string.round, currentRound + 1)); // for display
		scoreText.setText(getResources().getString(R.string.score, state.score));

		Random rnd = new Random();
		String target = words.get(rnd.nextInt(words.size()));
		target = target.toUpperCase(); // for display
		String[] parts = target.split("/");
		state.selectedWord = parts[0];
		Log.print("Selected word: " + parts[0] + " w: " + parts[1]);
		state.validWords.addAll(Arrays.asList(parts[1].split(",")));
		// sort by size
		Collections.sort(state.validWords, new Comparator<String>() {

			@Override
			public int compare(String s1, String s2) {
				return s1.length() - s2.length();
			}
		});
		findViewById(R.id.check_word).setVisibility(View.VISIBLE);
		for (int i = 0; i < 6; i++) {
			letters[i].setText("" + parts[0].charAt(i));
			letters[i].setVisibility(View.VISIBLE);
			guesses[i].setVisibility(View.INVISIBLE);
		}

		guessedWordsDisplay.clear();
		scoreText.setText(getResources().getString(R.string.score, state.score));
		createGuessedWordsGrid();

		state.timeOfLastTick = System.currentTimeMillis();
		// start timer
		stop = false;
		pump = new Thread() {

			@Override
			public void run() {
				while (!stop) {
					long elapsed = System.currentTimeMillis() - state.timeOfLastTick; // since last time we were running: from game start or since last pause
					state.timeOfLastTick = System.currentTimeMillis(); // technically we're missing a few fractions of seconds here and there
					if(!state.paused) { // this is so elegant: if the game is paused, time does not elapse ;)
						state.elapsed += elapsed;
					}
					long min = (state.roundLength - state.elapsed) / (60 * 1000);
					long sec = ((state.roundLength - (min * 60 * 1000)) - (state.elapsed)) / (1000);
					Message msg = mainLoop.obtainMessage();
					msg.arg1 = (int) min;
					msg.arg2 = (int) sec;
					mainLoop.sendMessage(msg);
					try {
						Thread.sleep(Settings.TICK);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		pump.start();
	}

	/**
	 * When you press a button with a letter on it we - hide that one - append that letter to the current guess - set
	 * the current leftmost invisible guess-letter to that letter - set that guess-letter to visible
	 * 
	 * @param letterNo
	 */
	private void processButton(int letterNo) {
		state.letterSequence.add(0, letterNo);
		int guessPos = state.guess.length();
		state.guess += letters[letterNo].getText();
		guesses[guessPos].setText(letters[letterNo].getText());
		guesses[guessPos].setVisibility(View.VISIBLE);
		letters[letterNo].setVisibility(View.INVISIBLE);
	}
}