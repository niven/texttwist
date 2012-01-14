package org.interdictor;

import org.interdictor.util.Database;
import org.interdictor.util.Log;
import org.interdictor.util.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class RoundCompletedActivity extends Activity {

	private TextTwistApplication tta;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.round_over);

		tta = (TextTwistApplication) getApplication();

		Resources resources = getResources();

		TextView roundN = (TextView) findViewById(R.id.round_completed);

		TextView done = (TextView) findViewById(R.id.btn_rnd_complete_done);
		done.setText(resources.getString(tta.getGame().over() ? R.string.main_menu : R.string.next_round));

		TextView bonusAll = (TextView) findViewById(R.id.bonus_allwords);
		TextView bonusShuffle = (TextView) findViewById(R.id.bonus_noshuffle);
		TextView bonusTime = (TextView) findViewById(R.id.bonus_timeleft);

		TextView totalScore = (TextView) findViewById(R.id.total_score);

		// extra points for getting all words in a round
		Roundstate lastRound = tta.getGame().getLastRound();
		boolean guessedAllWords = lastRound.alreadyGuessed.size() == lastRound.validWords.size();
		if (guessedAllWords) {
			lastRound.score += Settings.POINTS_ALL_WORDS;
		}
		bonusAll.setText(resources.getString(R.string.bonus_allwords, guessedAllWords ? Settings.POINTS_ALL_WORDS : 0));

		if (!lastRound.shuffled) {
			lastRound.score += Settings.POINT_NO_SHUFFLE;
		}
		bonusShuffle.setText(resources.getString(R.string.bonus_noshuffle, lastRound.shuffled ? 0 : Settings.POINT_NO_SHUFFLE));

		long timeLeft = lastRound.roundLength - lastRound.elapsed;
		int pointsForTime = 0;
		if (timeLeft > 0) {
			pointsForTime = (int) (Settings.POINTS_PER_SECOND_LEFT * (timeLeft / 1000));
			lastRound.score += pointsForTime;
		}
		bonusTime.setText(resources.getString(R.string.bonus_timeleft, pointsForTime));

		totalScore.setText(resources.getString(R.string.total_score, lastRound.score));

		if (tta.getGame().over()) {
			bonusAll.setVisibility(View.GONE);
			bonusTime.setVisibility(View.GONE);
			roundN.setText(resources.getString(R.string.game_over));
			// save score
			Score score = new Score((int) (System.currentTimeMillis() / 1000), lastRound.score, tta.getGame().getCurrentRound(), tta.getLocale());
			Log.print("Saving score: " + score.toString());
			Database.getInstance(this).saveScore(score);

		} else {
			roundN.setText(String.format(resources.getString(R.string.round_n_over, tta.getGame().getCurrentRound())));
		}
		final ListView lookup = (ListView) findViewById(R.id.lookup_list);
		// lookup.setAdapter( new ArrayAdapter<String>(this, R.layout.lookup_item, R.id.label, lastRound.validWords));
		lookup.setAdapter(new WordlistAdapter(R.layout.lookup_item, lastRound));
		lookup.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String item = (String) lookup.getAdapter().getItem(position);
				String target = String.format(Settings.lookupURLs.get(tta.getLocale().getLanguage()), item);
				Log.print("Clicked on " + item + "/ " + target);
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
				startActivity(browserIntent);

			}
		});
	}

	/**
	 * Clicked on Done/Next Round button at the bottom of the screen. This closes our activity since we now the game is
	 * over/ready for next round everywhere.
	 * 
	 * @param v
	 */
	public void bottomButton(View v) {

		finish();
	}

	public class WordlistAdapter extends BaseAdapter implements ListAdapter {

		private Roundstate lastRound;
		private int wordRow;

		public WordlistAdapter(int lookupItem, Roundstate lastRound) {
			this.lastRound = lastRound;
			this.wordRow = lookupItem;
		}

		@Override
		public int getCount() {
			return lastRound.validWords.size();
		}

		@Override
		public Object getItem(int position) {
			return lastRound.validWords.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;

			if (row == null) {
				row = getLayoutInflater().inflate(wordRow, null, false);
			}
			TextView label = (TextView) row.findViewById(R.id.label);
			String word = lastRound.validWords.get(position);
			label.setText(word);
			if (lastRound.alreadyGuessed.contains(word)) {
				label.setTextColor(getResources().getColor(R.color.word_guessed));
			} else {
				label.setTextColor(getResources().getColor(R.color.word_unguessed));
			}

			return row;
		}

	}

}
