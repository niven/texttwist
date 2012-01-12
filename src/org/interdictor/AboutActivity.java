package org.interdictor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AboutActivity extends Activity implements OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		Button btn = (Button) findViewById(R.id.btn_close);
		btn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		finish();
	}
}
