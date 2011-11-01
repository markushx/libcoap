package de.tzi.coap;

import de.tzi.coap.jni.*;
import android.app.Activity;
import android.os.Bundle;

public class CoAPClient extends Activity {

	static {
		try{
			System.loadLibrary("coap");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		System.out.println("INF: create context");
		coap_context_t ctx = coap.coap_new_context(77777);
        if (ctx == null) {
            System.err.println("Could not create context");
            return;
        }
        System.out.println("INF: create context success");

	}
}