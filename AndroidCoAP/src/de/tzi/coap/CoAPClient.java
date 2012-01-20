package de.tzi.coap;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_listnode;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;
import de.tzi.coap.jni.coap_uri_t;

/*
 * @author Markus Becker <mab@comnets.uni-bremen.de>
 * @author Thomas Poetsch <thp@comnets.uni-bremen.de>
 */

public class CoAPClient extends Activity {

	public static final String LOG_TAG = "CoAP";

	HashMap<Integer, String> uriHM = new HashMap(); 

	public static final String PREFS_NAME = "MyCoapPrefs";
	static final int ABOUT_DIALOG = 0;
	static final int IPV6_DIALOG = 1;

	SharedPreferences settings;
	ArrayAdapter<String> adapter;

	EditText ipText;
	EditText portText;
	Button sendButton;
	TextView responseTextView;
	Spinner spinner;
	RadioGroup rgMethod;
	EditText payloadText;

	String uris [];	
	public static TextView statusText;

	// CoAP specifics
	static Random generateRand = new Random();
	coap_context_t ctx;

	DatagramSocket clientSocket;
	LowerReceive lr = null;
	Retransmitter rt = null;

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

	public void setup_coap() {
		// create coap_context
		Log.i(LOG_TAG, "INF: create context");
		ctx = coap.coap_new_context(77777);
		if (ctx == null) {
			Log.e(LOG_TAG, "ERR: Could not create context");
			return;
		} else {
			Log.i(LOG_TAG, "INF: created context");
		}

		// register ourselves for message handling
		Log.i(LOG_TAG, "INF: register message handler");
		coap.register_message_handler(ctx, this);
		Log.i(LOG_TAG, "INF: registered message handler");

		try {
			clientSocket = new DatagramSocket();
			Log.i(LOG_TAG, "INF: open new socket on port " + clientSocket.getLocalPort());
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//		rt = new Retransmitter(ctx);
		//		rt.start();
	}

	// manages messages for current Thread (main)
	// received from our Thread
	public Handler mainHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				// updates the TextView with the received text
				setStatus(msg.getData().getString("text"));
			}
		};
	};

	void setStatus(CharSequence cs) {
		statusText.setText(cs);
	}
	
	//TODO: do in onDestroy()?
	protected void finalize() throws Throwable {
		try {
			// free context
			Log.i(LOG_TAG, "INF: finalize");
			coap.coap_free_context(ctx);
		} finally {
			super.finalize();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_client);

		setup_coap();

		ipText = (EditText)findViewById(R.id.editTextIP);		
		portText = (EditText)findViewById(R.id.editTextPort);

		responseTextView = (TextView)findViewById(R.id.errOutputClient);

		rgMethod = (RadioGroup)findViewById(R.id.radioGroup1);
		payloadText = (EditText)findViewById(R.id.payloadText);

		OnClickListener get_put_listener = new OnClickListener() {
			public void onClick(View v) {
				// Perform action on clicks
				RadioButton rb = (RadioButton) v;

				if (rb.getId() == R.id.rbPut) {
					payloadText.setVisibility(View.VISIBLE);
				} else {
					payloadText.setVisibility(View.GONE);
				}
			}
		};

		final RadioButton rbGet = (RadioButton) findViewById(R.id.rbGet);
		final RadioButton rbPut = (RadioButton) findViewById(R.id.rbPut);
		rbGet.setOnClickListener(get_put_listener);
		rbPut.setOnClickListener(get_put_listener);

		sendButton = (Button)findViewById(R.id.button);
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//thp: execute: send request
				if (ipText.getText().length() != 0) {
					sendRequest(ipText.getText().toString());
					lr = new LowerReceive(ctx, clientSocket);
					lr.start();
					//						lr.execute("");
					//						new runClient().execute("");
					//						sendRequest(ipText.getText().toString());
				} else {
					Toast toast = Toast.makeText(getApplicationContext(), "Enter IPv6 address!!", 
							Toast.LENGTH_LONG);
					toast.show();
				}

			} 
		});

		statusText = (TextView) findViewById(R.id.textStatus); 

		settings = getSharedPreferences(PREFS_NAME, 0);
		ipText.setText(settings.getString("ip", ""));
		portText.setText(settings.getString("port", ""));
		spinner = (Spinner) findViewById(R.id.spinner1);		
	}

	@Override
	protected void onStart() {
		super.onRestart();

		uris = stringToArray(settings.getString("uris", initResourcePref()));
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, uris);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(settings.getInt("resource", settings.getInt("selUri", 0)));	
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("ip", ipText.getText().toString());
		editor.putString("port", portText.getText().toString());
		editor.putInt("resource", spinner.getSelectedItemPosition());
		editor.putInt("selUri", spinner.getSelectedItemPosition());

		StringBuilder spinnerEntries = new StringBuilder();
		for (int i = 0; i < spinner.getCount(); i++) {
			spinnerEntries.append(spinner.getItemAtPosition(i)+",");		
		}
		spinnerEntries.deleteCharAt(spinnerEntries.length()-1);
		editor.putString("uris", spinnerEntries.toString());

		//		Log.d("CoAP", "result: "+spinnerEntries.toString());

		// Commit the edits!
		editor.commit();
	}

	@Override
	public void onDestroy() {
		super.onDestroy(); 

		// free context
		Log.i(LOG_TAG, "INF: free context");
		coap.coap_free_context(ctx);
		Log.i(LOG_TAG, "INF: free context~");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_item_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		case R.id.menu_item_about:
			showDialog(ABOUT_DIALOG);
			return true;

		case R.id.menu_item_ip:
			showDialog(IPV6_DIALOG);
			return true;

		}
		return false;    
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case ABOUT_DIALOG:
			AlertDialog.Builder builderAbout;
			Dialog dialog;

			LayoutInflater inflaterAbout = (LayoutInflater) this
					.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflaterAbout.inflate(R.layout.about,
					(ViewGroup) findViewById(R.id.layout_about));

			builderAbout = new AlertDialog.Builder(this);
			builderAbout.setView(layout);
			builderAbout.setMessage("").setPositiveButton(
					this.getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			dialog = builderAbout.create();

			return dialog;
		case IPV6_DIALOG:
			AlertDialog.Builder builderIp;
			Dialog dialog1;

			LayoutInflater inflaterIp = (LayoutInflater) this
					.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout2 = inflaterIp.inflate(R.layout.ip,
					(ViewGroup) findViewById(R.id.layout_ip));

			builderIp = new AlertDialog.Builder(this);
			builderIp.setView(layout2);
			builderIp.setMessage("").setPositiveButton(
					this.getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			dialog1 = builderIp.create();
			IPv6AddressesHelper ipv6Address = new IPv6AddressesHelper();
			ipv6Address.execute((TextView) layout2.findViewById(R.id.ip_content));

			return dialog1;
		default:
			return null;
		}
	}

	public String initResourcePref() {
		String entries [] = getResources().getStringArray(R.array.URIs);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < entries.length; i++) {
			result.append(entries[i]+",");			
		}
		result.deleteCharAt(result.length()-1);
		return result.toString();
	}

	public String [] stringToArray(String str) {
		String strArray [];
		strArray = str.split(",");

		return strArray;
	}

	private void sendRequest(String destination) {
		Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();

		int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
		CoapJavaOption contentTypeOption = new CoapJavaOption(
				coapConstants.COAP_OPTION_CONTENT_TYPE, ""
						+ (char) content_type, 1);

		String uri = "l";
		uri = spinner.getSelectedItem().toString();
		//		Log.i(LOG_TAG, "INF: URI "+uri);
		//String uri = "lipsum";

		optionList.add(contentTypeOption);
		CoapJavaOption uriOption = new CoapJavaOption(
				coap.COAP_OPTION_URI_PATH, uri, uri.length());
		optionList.add(uriOption);

		String token = Integer.toHexString(generateRand.nextInt(0xFF)); 

		CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN,
				token, token.length());
		optionList.add(tokenOption);

		int method = coapConstants.COAP_REQUEST_GET;
		String payload = null;
		coap_pdu_t pdu = coap_new_request(method, optionList, payload);

		if (pdu == null) {
			Log.e(LOG_TAG, "ERR: Could not create pdu");
			return;
		}

		uriHM.put(pdu.getHdr().getId(), uri);

		// set destination
		SWIGTYPE_p_sockaddr_in6 dst = null;
		dst = coap.sockaddr_in6_create(coapConstants.AF_INET6, coapConstants.COAP_DEFAULT_PORT,
				destination);

		// send pdu	
		Log.i(LOG_TAG, "INF: send_confirmed");
		coap.coap_send_confirmed(ctx, dst, pdu);
		// will trigger messageHandler() callback
		Log.i(LOG_TAG, "INF: send_confirmed~");
		// free destination
		coap.sockaddr_in6_free(dst);
		
		setStatus("CoAP request sent.");
	}

	//JNI callback to replace C socket with Java DatagramSocket
	public void coap_send_impl(coap_context_t ctx, SWIGTYPE_p_sockaddr_in6 dst, 
			coap_pdu_t pdu,
			int free_pdu) {
		// if you change the signature (name or parameters) of this 
		// function, the swig interface needs to be changed as well

		Log.i(LOG_TAG, "INF: callback coap_send_impl @ "+System.currentTimeMillis());
		try {
			InetAddress IPAddress = InetAddress.getByName(coap.get_addr(dst));
			lowerSend(coap.get_bytes(pdu), IPAddress);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.w(LOG_TAG, "WARN: No network connectivity...\n");
			//e.printStackTrace();
		}

		return;
	}

	public void lowerSend(byte[] sendData, InetAddress IPAddress) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, coapConstants.COAP_DEFAULT_PORT);
		Log.i(LOG_TAG, "INF: send message to " + IPAddress.getHostAddress());
		clientSocket.send(sendPacket);		
		return;
	}

	//message handler to update UI thread
	private Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {

			short[] pdudata = (short[])msg.obj;

			if (!uriHM.isEmpty()) {
				//TODO: read content-type and decide with switch/case
				if (uriHM.get(msg.arg1).equals("l")) {
					responseTextView.append(""+(int)pdudata[0] + "\n");
				} 
				if (uriHM.get(msg.arg1).equals("rt")) {
					responseTextView.append(shortArray2String(pdudata)+"\n");
				}
			} else {
				responseTextView.append("URI not found");
			}

			uriHM.clear();
		}
	};

	String shortArray2String(short[] arr) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			byte[] b = new byte[1];
			b[0] = (byte)arr[i];
			sb.append(new String(b));
		}

		return sb.toString();
	}

	public void messageHandler(coap_context_t ctx, coap_listnode node,
			String data) {

		Message msg = messageHandler.obtainMessage();

		lr.requestStop();
		coap_pdu_t pdu = null;
		Log.i(LOG_TAG, "INF: Java Client messageHandler()");

		//		System.out.println(LI+"INF: ****** pdu (" + node.getPdu().getLength()
		//				+ " bytes)" + " v:" + node.getPdu().getHdr().getVersion()
		//				+ " t:" + node.getPdu().getHdr().getType() + " oc:"
		//				+ node.getPdu().getHdr().getOptcnt() + " c:"
		//				+ node.getPdu().getHdr().getCode() + " id:"
		//				+ node.getPdu().getHdr().getId());

		if (node.getPdu().getHdr().getVersion() != coapConstants.COAP_DEFAULT_VERSION) {
			Log.w(LOG_TAG, "WARN: dropped packet with unknown version "+
					node.getPdu().getHdr().getVersion()+"\n");
			return;
		}

		/* send 500 response */
		if (node.getPdu().getHdr().getCode() < coapConstants.COAP_RESPONSE_100
				&& node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
			pdu = new_response(ctx, node, coapConstants.COAP_RESPONSE_500);
			finish(ctx, node, pdu);
			return;
		}		

		//block not supported yet		

		/* just print payload */
		if (node.getPdu().getHdr().getCode() == coapConstants.COAP_RESPONSE_200) {

			short[] pdudata = new short[node.getPdu().getLength()];

			int len = coap.coap_get_data_java(node.getPdu(), pdudata);

			Log.i(LOG_TAG, "INF: ****** data:'" + pdudata + "'" +len);
			Log.i(LOG_TAG, "INF: ****** data:' URI" + node.getPdu().getHdr().getId());

			msg.arg1 = node.getPdu().getHdr().getId();
			msg.obj = pdudata;
			messageHandler.sendMessage(msg);

			// responseTextView.setText(node.getPdu().getData());
			// System.out.println(LI+"INF: "+coap.get_addr(node.getRemote()));
		}

		/* acknowledge if requested */
		if (node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
			pdu = new_ack(ctx, node);
			Log.i(LOG_TAG, "INF: Acknowledge CON message");
		}

		finish(ctx, node, pdu);
		//System.out.println("INF: ~Java messageHandler()");
	}

	public void finish(coap_context_t ctx, coap_listnode node, coap_pdu_t pdu) {
		Log.i(LOG_TAG, "INF: finish");
		//		toggleButton.setChecked(false);
		if ((pdu != null)
				&& (coap.coap_send(ctx, node.getRemote(), pdu) == coapConstants.COAP_INVALID_TID)) {
			Log.e(LOG_TAG, "ERR: message_handler: error sending reponse");
			coap.coap_delete_pdu(pdu);
		}
		//		System.out.println(LI+"INF: doStop = true");
		//		doStop = true;
	}

	// CoAP:
	coap_pdu_t new_ack(coap_context_t ctx, coap_listnode node) {
		coap_pdu_t pdu = coap.coap_new_pdu();

		if (pdu != null) {
			pdu.getHdr().setType(coapConstants.COAP_MESSAGE_ACK);
			pdu.getHdr().setCode(0);
			pdu.getHdr().setId(node.getPdu().getHdr().getId());
		}

		return pdu;
	}

	coap_pdu_t new_response(coap_context_t ctx, coap_listnode node, int code) {
		coap_pdu_t pdu = new_ack(ctx, node);

		if (pdu != null)
			pdu.getHdr().setCode(code);

		return pdu;
	}

	coap_pdu_t coap_new_request(int methodid, Vector<CoapJavaOption> optlist, String payload) {

		coap_pdu_t pdu = coap.coap_new_pdu();
		if (pdu == null) {
			Log.e(LOG_TAG, "INF: could not create pdu");
			return pdu;
		}

		Log.i(LOG_TAG, "INF: set header values");
		pdu.getHdr().setVersion(coapConstants.COAP_DEFAULT_VERSION);
		pdu.getHdr().setType(coapConstants.COAP_MESSAGE_CON);
		pdu.getHdr().setCode(methodid);
		pdu.getHdr().setId(generateRand.nextInt(0xFFFF));

		for (int i=0; i<optlist.size(); i++) {
			coap.coap_add_option(pdu, optlist.get(i).getType(), optlist.get(i).getLength(), optlist.get(i).getValue());
		}

		if (payload != null) {
			coap.coap_add_data(pdu, payload.length(), payload);
		}

		Log.i(LOG_TAG, "INF: created pdu");
		return pdu;
	}
}