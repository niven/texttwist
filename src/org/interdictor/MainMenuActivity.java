package org.interdictor;

import java.util.Locale;

import org.interdictor.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class MainMenuActivity extends Activity implements OnClickListener, OnItemSelectedListener {

	// supported
	Locale[] locales = new Locale[] { Locale.ENGLISH, new Locale("nl") };
	private TextTwistApplication tta;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_menu);

		// this is dumb
		Button btn = (Button) findViewById(R.id.btn_start);
		btn.setOnClickListener(this);
		btn = (Button) findViewById(R.id.btn_about);
		btn.setOnClickListener(this);		
		btn = (Button) findViewById(R.id.btn_highscores);
		btn.setOnClickListener(this);
		btn = (Button) findViewById(R.id.btn_send_feedback);
		btn.setOnClickListener(this);
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.language_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);
		
		tta = (TextTwistApplication) getApplication();
		tta.setLocale(locales[0]);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.btn_start:
			Intent intent = new Intent(this, TextTwistActivity.class);
			intent.putExtra("locale_string", tta.getLocale().getLanguage());
			startActivity(intent);
			break;
		case R.id.btn_about:
			intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			break;
		case R.id.btn_highscores:
			Log.print("Clicked HS");
			intent = new Intent(this, HighscoreActivity.class);
			startActivity(intent);
			break;
		case R.id.btn_send_feedback:
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("text/plain");
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "TextTwist feedback");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"texttwist@interdictor.org"});
			startActivity(Intent.createChooser(emailIntent, "Email:"));
		}

	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		Log.print("Lang: " + parent.getItemAtPosition(pos).toString());
		if (tta.getLocale() != locales[pos]) { // change lang
			tta.setLocale(locales[pos]);
			TextTwistApplication tta = (TextTwistApplication) getApplication();
			tta.getGame().setLocale(locales[pos]);
			Locale.setDefault(locales[pos]);
			Configuration config2 = new Configuration();
			config2.locale = locales[pos];

			getBaseContext().getResources().updateConfiguration(config2, getBaseContext().getResources().getDisplayMetrics());

		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

}
