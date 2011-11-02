package de.tzi.coap;

import de.tzi.coap.jni.*;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

public class CoAPClient extends Activity {

	private static final String LOG_TAG = "CoAP";

	static {
		try{
			Log.i(LOG_TAG, "static load library");
			System.loadLibrary("coap");
			Log.i(LOG_TAG, "static load library success");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOG_TAG, "static load library fail");
		}
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Log.i(LOG_TAG, "create context");
		coap_context_t ctx = coap.coap_new_context(77777);
        if (ctx == null) {
            Log.e(LOG_TAG, "Could not create context");
            Toast.makeText(getBaseContext(), 
                    "ERR: create context fail", 
                    Toast.LENGTH_LONG).show();
            return;
        }
        Log.i(LOG_TAG, "create context success");
        Toast.makeText(getBaseContext(), 
                "INF: create context success", 
                Toast.LENGTH_LONG).show();
	}
}