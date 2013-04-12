package de.tzi.coap13;

import de.tzi.coap13.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends Activity {
	SharedPreferences settings;
	EditText resourcesText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		Button saveButton = (Button) findViewById(R.id.btn_save);
		Button cancelButton = (Button) findViewById(R.id.btn_cancel);  
		resourcesText = (EditText)findViewById(R.id.editTextResources);
		settings = getSharedPreferences(CoAPClient.PREFS_NAME, 0);
		resourcesText.setText(settings.getString("uris", ""));

		saveButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				//	save stuff
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("uris", resourcesText.getText().toString());
				editor.commit();
				finish();
			}

		});

		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}

		});

	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
