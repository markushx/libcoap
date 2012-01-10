package de.tzi.coap;

import java.util.Random;
import java.util.Vector;

import de.tzi.coap.jni.*;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
//import de.tzi.coap.jni.sockaddr_in6;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;

public class CoAPClient extends Activity {

	private static final String LOG_TAG = "CoAP";
	static Random generator = new Random();

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


	private coap_pdu_t coap_new_request(int methodid, Vector<CoapJavaOption> optlist, String payload) {

		coap_pdu_t pdu = coap.coap_new_pdu();
		if (pdu == null) {
			System.out.println("INF: could not create pdu");
			return pdu;
		}

		System.out.println("INF: set header values");
		pdu.getHdr().setVersion(coapConstants.COAP_DEFAULT_VERSION);
		pdu.getHdr().setType(coapConstants.COAP_MESSAGE_CON);
		pdu.getHdr().setCode(methodid);
		pdu.getHdr().setId(generator.nextInt(0xFFFF));

		for (int i=0; i<optlist.size(); i++) {
			coap.coap_add_option(pdu, optlist.get(i).getType(), optlist.get(i).getLength(), optlist.get(i).getValue());
		}

		if (payload != null) {
			coap.coap_add_data(pdu, payload.length(), payload);
		}

		System.out.println("INF: created pdu");
		return pdu;
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_client);

		Button run = (Button)findViewById(R.id.button1);
		run.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {

				//				new runClient().execute(null);
				run();
			}
		});


		//		Log.i(LOG_TAG, "create context");    
		//		coap_context_t ctx = coap.coap_new_context(77777);
		//		if (ctx == null) {
		//			Log.e(LOG_TAG, "Could not create context");
		//			Toast.makeText(getBaseContext(), 
		//					"ERR: create context fail", 
		//					Toast.LENGTH_LONG).show();
		//			return;
		//		}
		//		Log.i(LOG_TAG, "create context success");
		//		Toast.makeText(getBaseContext(), 
		//				"INF: create context success", 
		//				Toast.LENGTH_LONG).show();
		//		coap.register_message_handler(ctx, this);
		//        coap_pdu_t pdu = coap_new_request(method, optlist, payload);

	}

	//	private class runClient extends AsyncTask<String, String, String> {
	//		protected String doInBackground(String... s) {
	//			
	//			
	//			return null;
	//		}
	//
	//		protected void onProgressUpdate(String... progress) {
	//			//				errTextView.setText(""+progress[0]);
	//		}
	//
	//		protected void onPostExecute(String result) {
	//			//				toggleButton.setChecked(false);
	//		}
	//	}

	public void run() {
		String destination = "::1";
		int port = coapConstants.COAP_DEFAULT_PORT;
		int method = coapConstants.COAP_REQUEST_GET;
		int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
		String uri = "/l";
		String token = "3a";
		String payload = "Hello CoAP";

		Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();
		CoapJavaOption contentTypeOption = new CoapJavaOption(coapConstants.COAP_OPTION_CONTENT_TYPE, ""+(char)content_type, 1);
		optionList.add(contentTypeOption);
		CoapJavaOption uriOption = new CoapJavaOption(coap.COAP_OPTION_URI_PATH, uri, uri.length());
		optionList.add(uriOption);
		CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN, token, token.length());
		optionList.add(tokenOption);
		if ( method == coapConstants.COAP_REQUEST_GET || method == coapConstants.COAP_REQUEST_DELETE) {
			payload = null;
		}

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
		coap.register_message_handler(ctx, this);

		coap_pdu_t pdu = coap_new_request(method, optionList, payload);

		if (pdu == null) {
			System.err.println("Could not create pdu");
			return;
		}

		// set destination
		//sockaddr_in6 dst;
		SWIGTYPE_p_sockaddr_in6 dst;
		dst = coap.sockaddr_in6_create(coapConstants.AF_INET6,
				port,
				destination);




	}
}