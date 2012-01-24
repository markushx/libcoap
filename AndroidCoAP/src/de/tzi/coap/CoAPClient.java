package de.tzi.coap;

import java.io.IOException;
import java.net.*;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.SyncStateContract.Constants;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_listnode;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;

/**
 * Sample implementation of a CoAP client for Android using SWIGified libcoap.
 * 
 * @author Markus Becker <mab@comnets.uni-bremen.de>
 * @author Thomas Poetsch <thp@comnets.uni-bremen.de>
 */

public class CoAPClient extends Activity {

	public static final String LOG_TAG = "CoAP";
	public Boolean isTablet = false;

	HashMap<Integer, String> uriHM = new HashMap<Integer, String>(); 

	public static final String PREFS_NAME = "MyCoapPrefs";
	static final int ABOUT_DIALOG = 0;
	static final int IPV6_DIALOG  = 1;
	static final int MODE_DIALOG  = 2;

	static final int MODE_IP  = 0;
	static final int MODE_SMS = 1;
	
	ArrayAdapter<String> adapter;

	// UI elements	
	LinearLayout coap_ip_layout;
	EditText ipText;
	EditText portText;
	Spinner uriSpinner;

	LinearLayout coap_sms_layout;
	EditText noText;
	Spinner uriSpinnerSMS;

	RadioGroup rgMethod;
	RadioButton rb_get;
	RadioButton rb_put;
	EditText payloadText;

	Button btn_send;

	CheckBox continuous;
	EditText secondsText;

	static TextView statusText;

	ScrollView sv;
	TextView responseTextView;

	WebView wv;
	//~ UI elements

	// storage for data received
	JSONArray dataTemp;
	JSONArray dataHum;
	JSONArray dataVolt;

	JSONObject temp;
	JSONObject hum;
	JSONObject volt;
	// ~storage for data received

	PowerManager.WakeLock wl;

	FlotGraphHandler mGraphHandler;

	String uris [];	
	private Date startDate;

	// CoAP specifics
	static Random generateRand = new Random();
	coap_context_t ctx;

	DatagramSocket clientSocket;
	LowerReceive lr = null;
	Retransmitter rt = null;
	RequesterThread reqthr = null;

	BroadcastReceiver sentReceiver;
	BroadcastReceiver replyReceiver;
	
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
			e.printStackTrace();
		}
	}

	static void setStatus(CharSequence cs) {
		statusText.setText(cs);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_client);

		// don't dim screen for demonstrations
		PowerManager pm  = (PowerManager) getSystemService(Context.POWER_SERVICE); 
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
		wl.acquire();

		// register us for the SMS sent notification
		sentReceiver = new BroadcastReceiver()	{
			public void onReceive(Context context, Intent intent)
			{
				String info = "Sent information: ";

				switch(getResultCode())
				{
				case Activity.RESULT_OK: info += "send successful"; break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE: info += "send failed, generic failure"; break;
				case SmsManager.RESULT_ERROR_NO_SERVICE: info += "send failed, no service"; break;
				case SmsManager.RESULT_ERROR_NULL_PDU: info += "send failed, null pdu"; break;
				case SmsManager.RESULT_ERROR_RADIO_OFF: info += "send failed, radio is off"; break;
				}

				Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();
			}
		};
		registerReceiver(sentReceiver, new IntentFilter("SMS_SENT"));

		replyReceiver = new SmsReceiver();
		registerReceiver(replyReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

		//setup orientation based on screensize
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();

		if (width > 700) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
			isTablet = true;
		} else {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		// setup UI elements
		coap_ip_layout = (LinearLayout)findViewById(R.id.coap_ip_layout);
		ipText = (EditText)findViewById(R.id.editTextIP);
		portText = (EditText)findViewById(R.id.editTextPort);
		uriSpinner = (Spinner) findViewById(R.id.uriSpinner);

		coap_sms_layout = (LinearLayout)findViewById(R.id.coap_sms_layout);
		noText = (EditText)findViewById(R.id.editTextTelephoneNumber);
		uriSpinnerSMS = (Spinner) findViewById(R.id.uriSpinnerSMS);

		rgMethod = (RadioGroup)findViewById(R.id.radioGroup1);
		rb_get = ((RadioButton) findViewById(R.id.rbGet));
		rb_put = ((RadioButton) findViewById(R.id.rbPut));
		payloadText = (EditText)findViewById(R.id.payloadText);

		btn_send = (Button)findViewById(R.id.btn_send);

		continuous = (CheckBox) findViewById(R.id.continuous);
		secondsText = (EditText)findViewById(R.id.seconds);

		statusText = (TextView) findViewById(R.id.textStatus);

		sv = (ScrollView)findViewById(R.id.scrollView1);
		responseTextView  = (TextView)findViewById(R.id.responseTextView);

		wv = (WebView)findViewById(R.id.wv1);
		//~ setup UI elements

		setup_coap();

		// GET/PUT -------
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
		rb_get.setOnClickListener(get_put_listener);
		rb_put.setOnClickListener(get_put_listener);

		// SEND ONCE -------
		btn_send.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				responseTextView.setText("");
				Log.d("CoAP", "start LowerReceive");
				lr = new LowerReceive(ctx, clientSocket);
				lr.start();
				Log.d("CoAP", "LowerReceive started");
				rt = new Retransmitter(ctx);
				rt.start();

				Log.d("CoAP", "sendRequest");

				// TODO: CHECK: also send SMS as if to an IPv6 address???
				if (ipText.getText().length() != 0) {
					int port = coapConstants.COAP_DEFAULT_PORT;

					try {
						port = new Integer(portText.getText().toString()).intValue();
					} catch (NumberFormatException e) {
						Toast toast = Toast.makeText(getApplicationContext(),
								"Using port "+coapConstants.COAP_DEFAULT_PORT+".", 
								Toast.LENGTH_LONG);
						toast.show();
						portText.setText(""+coapConstants.COAP_DEFAULT_PORT);
					}

					Log.d("CoAP", "setting destination");
					SWIGTYPE_p_sockaddr_in6 dst = null;
					dst = coap.sockaddr_in6_create(coapConstants.AF_INET6,
							port,
							ipText.getText().toString());

					Log.d("CoAP", "sending Request");
					sendRequest(uriSpinner.getSelectedItem().toString(),
							(rb_get.isChecked() ?
									coapConstants.COAP_REQUEST_GET :
										coapConstants.COAP_REQUEST_PUT),
										dst);

					Log.d("CoAP", "free destination");
					// free destination
					coap.sockaddr_in6_free(dst);
				} else {
					Toast toast = Toast.makeText(getApplicationContext(),
							"Enter IPv6 address!",
							Toast.LENGTH_LONG);
					toast.show();
				}
				Log.d("CoAP", "sendRequest~");
			}
		});

		// CONTINUOUS -------		
		OnCheckedChangeListener cont_listener = new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {

					// storage for data received
					dataTemp = new JSONArray();
					dataHum = new JSONArray();
					dataVolt = new JSONArray();

					temp = new JSONObject();
					hum = new JSONObject();
					volt = new JSONObject();

					try {
						temp.put("color", "#d1002c");
						temp.put("label", "Temperature (degC)");
						hum.put("color", "#0066ff");
						hum.put("label", "Humidity (%)");
						volt.put("color", "#08ff00");
						volt.put("label", "Voltage (V)");
					} catch (JSONException e) {
						Toast toast = Toast.makeText(getApplicationContext(),
								"JSON parsing error.", 
								Toast.LENGTH_LONG);
						toast.show();
						e.printStackTrace();
					}
					// ~storage for data received

					Log.d("CoAP", "-> continuous mode");
					if (!isTablet) {
						sv.setVisibility(View.GONE);
					}

					btn_send.setEnabled(false);

					wv.setVisibility(View.VISIBLE);
					wv.getSettings().setJavaScriptEnabled(true);
					wv.loadUrl("file:///android_asset/flot/stats_graph.html");

					startDate  = new Date();

					int port = coapConstants.COAP_DEFAULT_PORT;
					try {
						port = new Integer(portText.getText().toString()).intValue();
					} catch (NumberFormatException e) {
						port = coapConstants.COAP_DEFAULT_PORT;
						portText.setText(""+port);

						Toast toast = Toast.makeText(getApplicationContext(),
								"Number error. Setted port to "+port+".", 
								Toast.LENGTH_LONG);
						toast.show();
					}

					int seconds = 2;
					try{
						seconds = new Integer(secondsText.getText().toString()).intValue();
					} catch (NumberFormatException e) {
						seconds = 2;
						secondsText.setText(""+seconds);

						Toast toast = Toast.makeText(getApplicationContext(),
								"Number error. Setted request time to "+seconds+"s.", 
								Toast.LENGTH_LONG);
						toast.show();
					}
					reqthr = new RequesterThread(getApplicationContext(),
							ipText.getText().toString(),
							port,
							uriSpinner.getSelectedItem().toString(),
							rb_get.isChecked() ?
									coapConstants.COAP_REQUEST_GET :
										coapConstants.COAP_REQUEST_PUT,
										seconds
							);
					reqthr.start();

					secondsText.setEnabled(false);
				} else {
					Log.d("CoAP", "-> single shot mode");
					sv.setVisibility(View.VISIBLE);
					wv.setVisibility(View.GONE);
					btn_send.setEnabled(true);
					if (reqthr != null) {
						reqthr.requestStop();
					}
					secondsText.setEnabled(true);
				}
			}
		};
		continuous.setOnCheckedChangeListener(cont_listener);

		// --------
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		//ipText.setText(settings.getString("ip", ""));
//		ipText.setText("2001:0638:0708:1003:0226:37ff:fe9a:5d08"); //thp: remove
		ipText.setText("2001:0638:0708:1003:9221:55ff:fee4:ec58"); // mab: remove
		portText.setText(settings.getString("port", ""));

		//setup UI for proper mode: SMS resp. IP 
		//TDOD: thp: why not calling setOperationMode???
		if (settings.getInt("mode", MODE_IP) == MODE_IP) {
			coap_sms_layout.setVisibility(View.GONE);
			coap_ip_layout.setVisibility(View.VISIBLE);
		} else {
			coap_sms_layout.setVisibility(View.VISIBLE);
			coap_ip_layout.setVisibility(View.GONE);			
		}
	}

	@Override
	protected void onStart() {
		super.onRestart();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		uris = stringToArray(settings.getString("uris", initResourcePref()));
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, uris);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		uriSpinner.setAdapter(adapter);
		uriSpinner.setSelection(settings.getInt("resource", settings.getInt("selUri", 0)));	
		uriSpinnerSMS.setAdapter(adapter);
		uriSpinnerSMS.setSelection(settings.getInt("resource", settings.getInt("selUri", 0)));	
		adapter.notifyDataSetChanged();		
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("ip", ipText.getText().toString());
		editor.putString("port", portText.getText().toString());
		editor.putInt("resource", uriSpinner.getSelectedItemPosition());
		editor.putInt("selUri", uriSpinner.getSelectedItemPosition());
		editor.putInt("resourceSMS", uriSpinnerSMS.getSelectedItemPosition());
		editor.putInt("selUriSMS", uriSpinnerSMS.getSelectedItemPosition());

		StringBuilder spinnerEntries = new StringBuilder();
		for (int i = 0; i < uriSpinner.getCount(); i++) {
			spinnerEntries.append(uriSpinner.getItemAtPosition(i)+",");		
		}
		spinnerEntries.deleteCharAt(spinnerEntries.length()-1);
		editor.putString("uris", spinnerEntries.toString());

		// Log.d("CoAP", "result: "+spinnerEntries.toString());

		editor.commit();
	}

	@Override
	public void onDestroy() {
		super.onDestroy(); 

		// free context
		Log.i(LOG_TAG, "INF: free context");
		coap.coap_free_context(ctx);
		Log.i(LOG_TAG, "INF: free context~");

		Log.i(LOG_TAG, "INF: deregister message handler");
		coap.deregister_message_handler(ctx, this);
		Log.i(LOG_TAG, "INF: deregistered message handler");

		//stop all threads, just in case 
		try {
			lr.requestStop();
			rt.requestStop();
			reqthr.requestStop();
		} catch (NullPointerException e) {
			// do nothing, just exit
		}

		//release wake-lock
		wl.release();
		
		// unregister BroadcastReceiver for sent SMS
		unregisterReceiver(sentReceiver);
		unregisterReceiver(replyReceiver);
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

		case R.id.menu_item_mode:
			showDialog(MODE_DIALOG);
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

			LayoutInflater inflaterAbout =
					(LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
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

		case MODE_DIALOG:
			final CharSequence[] items = {"IP", "SMS"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose the client's transport mode");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Toast.makeText(getApplicationContext(),
			        		items[item],
			        		Toast.LENGTH_SHORT).show();
			        
			        setOperationMode(item);
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
			
		default:
			return null;
		}
	}

	private void setOperationMode(int item) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();	
		editor.putInt("mode", item);
		editor.commit();
		
		if (item == MODE_IP) {
			coap_sms_layout.setVisibility(View.GONE);
			coap_ip_layout.setVisibility(View.VISIBLE);
		} else {
			coap_sms_layout.setVisibility(View.VISIBLE);
			coap_ip_layout.setVisibility(View.GONE);			
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
		String strArray[];
		strArray = str.split(",");
		return strArray;
	}

	class RequesterThread extends Thread {
		boolean doStop = false;

		Context context;

		String ip;
		int port;
		String uri;
		int method;

		int seconds_to_sleep;

		public RequesterThread(Context context,
				String ip, int port,
				String uri, int method,
				int seconds_to_sleep) {
			this.context = context;

			this.ip = ip;
			this.port = port;
			this.uri = uri;
			this.method = method;

			this.seconds_to_sleep = seconds_to_sleep;
		}

		public void run() {
			Log.i(CoAPClient.LOG_TAG, "INF: RequesterThread run()");
			Looper.prepare();
			requestLoop();
		}

		public void requestStop() {
			Log.i(CoAPClient.LOG_TAG, "INF: RequesterThread requestStop");
			doStop = true;
			//lr.stop(); //CHECK: is the requestLoop killed, when we stop the receive part??? 
		}

		private void requestLoop() {
			while (!doStop) {
				Log.d("CoAP", "start LowerReceive");
				lr = new LowerReceive(ctx, clientSocket);
				lr.start();
				Log.d("CoAP", "LowerReceive started");

				Log.i(CoAPClient.LOG_TAG, "INF: RequesterThread: next request...");
				//sendRequest(this.ip, this.port, this.uri, this.method);
				
				SWIGTYPE_p_sockaddr_in6 dst = null;
				dst = coap.sockaddr_in6_create(
						coapConstants.AF_INET6,
						this.port,
						this.ip);

				Log.d("CoAP", "sending Request");
				sendRequest(this.uri, this.method, dst);

				// free destination
				coap.sockaddr_in6_free(dst);

				try {
					Thread.sleep(seconds_to_sleep*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void sendRequest(String uri, int method, SWIGTYPE_p_sockaddr_in6 dst) {
		Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();

		int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
		CoapJavaOption contentTypeOption = new CoapJavaOption(
				coapConstants.COAP_OPTION_CONTENT_TYPE, ""
						+ (char) content_type, 1);

		//Log.i(LOG_TAG, "INF: sendRequest: "+((method==coapConstants.COAP_REQUEST_GET)?"GET":"PUT")+" coap://["+destination+"]:"+port+"/"+uri);

		optionList.add(contentTypeOption);
		CoapJavaOption uriOption = new CoapJavaOption(
				coap.COAP_OPTION_URI_PATH, uri, uri.length());
		optionList.add(uriOption);

		//TODO: token is added, use this on receive path to differentiate?
		String token = Integer.toHexString(generateRand.nextInt(0xFF)); 
		CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN,
				token, token.length());
		optionList.add(tokenOption);

		String payload = null;
		coap_pdu_t pdu = coap_new_request(method, optionList, payload);

		if (pdu == null) {
			Log.e(LOG_TAG, "ERR: Could not create pdu");
			return;
		}

		//TODO: token is added, use this on receive path to differentiate?
		uriHM.put(pdu.getHdr().getId(), uri);
		
		// send pdu
		Log.i(LOG_TAG, "INF: send_confirmed: " + ctx + " " + dst + " " + pdu);
		coap.coap_send_confirmed(ctx, dst, pdu);
		// will trigger messageHandler() callback
		Log.i(LOG_TAG, "INF: send_confirmed~");

		// free destination
//		coap.sockaddr_in6_free(dst);

	}
	
	//JNI callback to replace C socket with Java DatagramSocket
	public void coap_send_impl(coap_context_t ctx, SWIGTYPE_p_sockaddr_in6 dst, 
			coap_pdu_t pdu,
			int free_pdu) {
		// if you change the signature (name or parameters) of this 
		// function, the swig interface needs to be changed as well

		Log.i(LOG_TAG, "INF: callback coap_send_impl @ "+System.currentTimeMillis());
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if (settings.getInt("mode", MODE_IP) == MODE_IP) {
			Log.i(LOG_TAG, "INF: lowerSendIP");
			try {
				InetAddress IPAddress = InetAddress.getByName(coap.get_addr(dst));
				lowerSendIP(coap.get_bytes(pdu), IPAddress);
			} catch (UnknownHostException e) {
				Log.w(LOG_TAG, "WARN: UnknownHostException\n");
				e.printStackTrace();
			} catch (IOException e) {
				Log.w(LOG_TAG, "WARN: No network connectivity...\n");
				//e.printStackTrace();
			}
		} else {
			Log.i(LOG_TAG, "INF: lowerSendSMS");
			lowerSendSMS(coap.get_bytes(pdu), noText.getText().toString());
		}

		Log.i(LOG_TAG, "INF: callback coap_send_impl return.");
		return;
	}

	public void lowerSendIP(byte[] sendData, InetAddress IPAddress) throws IOException {
		//TODO: BUG: don't use default port!!!
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, coapConstants.COAP_DEFAULT_PORT);
		Log.i(LOG_TAG, "INF: sending message to " + IPAddress.getHostAddress());
		clientSocket.send(sendPacket);
		Log.i(LOG_TAG, "INF: sent message.");
		return;
	}

	public void lowerSendSMS(byte[] sendData, String phoneNo) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, CoAPClient.class), 0);

    	SmsManager sms = SmsManager.getDefault();
    	
    	//Log.i(LOG_TAG, "[SMSApp] byte[]: "+message + "/" + message.length);
    	//String str = new String(message, "utf-8");

    	String str = Base64.encodeToString(sendData, Base64.DEFAULT);

    	Log.i(LOG_TAG, "[SMS] String: " + str + "/"+str.length());
    	sms.sendTextMessage(phoneNo, null, str, pi, null);
    	//sms.sendDataMessage(phoneNumber, null, (short)8091, message, pi, null);
    	Log.i(LOG_TAG, "[SMS] text sms sent");
	}

	public class SmsReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(LOG_TAG, "[SMS] onReceiveIntent");

			// ---get the SMS message passed in---
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;

			if (bundle != null) {
				Log.i(LOG_TAG, "[SMS] onReceiveIntent bundle != null");
				// ---retrieve the SMS message received---
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];
				Log.i(LOG_TAG, "[SMS] onReceiveIntent:" + pdus.length);
				for (int i = 0; i < msgs.length; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					Log.i(LOG_TAG, "[SMS] handling msg " + i);
				}

				try {
					Log.i(LOG_TAG, "[SMS] info " + msgs.length);
					Log.i(LOG_TAG, "[SMS] info " + msgs[0]);
					Log.i(LOG_TAG, "[SMS] messagebody " + msgs[0].getMessageBody());
					Log.i(LOG_TAG, "[SMS] length " + msgs[0].getMessageBody().length());
					Log.i(LOG_TAG, "[SMS] byte[] " + msgs[0].getMessageBody().getBytes("utf-8"));
					Log.i(LOG_TAG, "[SMS] length " + msgs[0].getMessageBody().getBytes("utf-8").length);

		            byte[] b = Base64.decode(msgs[0].getMessageBody(), Base64.DEFAULT);	          
		            Log.i(LOG_TAG, "[SMS] base64[] " + b);
					
		            //TODO: act only on white-listed telephone numbers !!!
		            // faking an IPv6 address to make libcoap happily match request and response
		            SWIGTYPE_p_sockaddr_in6 src;
		            
					int port = 0;
					try {
						port = new Integer(portText.getText().toString()).intValue();
					} catch (NumberFormatException e) {
						port = coapConstants.COAP_DEFAULT_PORT;
					}
					src = coap.sockaddr_in6_create(coapConstants.AF_INET6,
							port,
							ipText.getText().toString());

					coap.coap_read(ctx, src, b, b.length);
					coap.coap_dispatch(ctx);
		            
					/*Log.i(LOG_TAG,
							"[SMSApp] Coap Message reconstruction successful: " + msg.toString());
					Toast.makeText(context, "CoAP: " + msg.toString(),
							Toast.LENGTH_SHORT).show();*/
				} catch (Exception e) {
					Log.e(LOG_TAG, "[SMSApp] exception", e);
				}
				// ---display the new SMS message---
				// Toast.makeText(context, "Got an SMS. Auto-replying to it.",
				// Toast.LENGTH_SHORT).show();
				// sendSMS(msgs[0].getOriginatingAddress(), "Selber hallo.");
			}
		}
	
	}
	
	private void handleR(ResourceR val) {
		int temp_val = val.getTemp();
		int hum_val  = val.getHum();
		int volt_val = val.getVolt();

		if (continuous.isChecked()) {
			JSONArray result = new JSONArray();

			JSONArray entryTemp = new JSONArray();
			JSONArray entryHum = new JSONArray();
			JSONArray entryVolt = new JSONArray();
			
			float diff = (new Date()).getTime() - startDate.getTime();
			try {
				entryTemp.put(diff / 1000);
				entryHum.put(diff / 1000);
				entryVolt.put(diff / 1000);

				entryTemp.put((float) (temp_val / 100 - 273.15));
				dataTemp.put(entryTemp);
				//temp.putOpt("label", "&nbsp;Temperature <br> &nbsp;("+temp_val+" degC)");
				temp.put("data", dataTemp);

				entryHum.put((float) hum_val / 100);
				dataHum.put(entryHum);
				//hum.putOpt("label", "&nbsp;Humidity <br> &nbsp;("+hum_val+" %)");
				hum.put("data", dataHum);

				entryVolt.put((float) volt_val / 100);
				dataVolt.put(entryVolt);
				//volt.putOpt("label", "&nbsp;Voltage <br> &nbsp;("+volt_val+" V)");
				volt.put("data", dataVolt);

				result.put(temp);
				result.put(hum);
				result.put(volt);

				updateMeasurements(result);
			} catch (JSONException e) {
				Toast toast = Toast.makeText(getApplicationContext(),
						"JSON exception.", 
						Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}

	//message handler to update UI thread for received messages
	private Handler messageUIHandlerReceived = new Handler() {
		public void handleMessage(Message msg) {

			setStatus("CoAP Response (MID "+msg.arg1+") received.");

			short[] pdudata = (short[])msg.obj;

			if (!uriHM.isEmpty()) {
				//TODO: read content-type and decide with switch/case
				if (uriHM.get(msg.arg1).equals("l")) {
					responseTextView.append(""+(int)pdudata[0] + "\n");
				} else if (uriHM.get(msg.arg1).equals("rt")) {
					responseTextView.append(shortArray2String(pdudata)+"\n");
				} else if (uriHM.get(msg.arg1).equals("time")) {
					responseTextView.append(shortArray2String(pdudata)+"\n");
				} else if (uriHM.get(msg.arg1).equals("r")) {
					ResourceR val = new ResourceR(pdudata);
					val.show();
					handleR(val);
					responseTextView.append("r: T:"+ roundTwoDecimals((float)val.getTemp()/100 - 273.15)
							+ " H:" + roundTwoDecimals(((float)val.getHum()/100)) 
							+ " V:" + roundTwoDecimals((float)val.getVolt()/100) + "\n");
				} else {
					responseTextView.append("URI "+uriHM.get(msg.arg1)+" not found\n");
				}
				
			} else {
				responseTextView.append("URI not available\n");
			}

			//scroll down to bottom
			sv.fullScroll(ScrollView.FOCUS_DOWN);
			
			uriHM.clear();
		}
	};

	float roundTwoDecimals(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Float.valueOf(twoDForm.format(d));
	}

	//message handler to update UI thread for retransmissions
	public static Handler messageUIHandlerRetransmission = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.arg1 < coapConstants.COAP_DEFAULT_MAX_RETRANSMIT) {
				setStatus("Retransmission #"+msg.arg1);
			} else {
				setStatus("Request aborted... ");
			}
		}
	};

	protected void updateMeasurements(JSONArray... data) {
		if (data != null && data.length > 0) {
			mGraphHandler = new FlotGraphHandler(this, wv, data[0],
					DateFormat.format("hh:mm:ss", startDate).toString());

			wv.addJavascriptInterface(mGraphHandler, "testhandler");
			if (isTablet) {
				wv.loadUrl("file:///android_asset/flot/stats_graph_tablet.html");
			}else {
				wv.loadUrl("file:///android_asset/flot/stats_graph.html");
			}
			
			
		} else {
			Toast toast = Toast.makeText(getApplicationContext(),
					"JSON data has errors.", 
					Toast.LENGTH_LONG);
			toast.show();
		}
	}

	String shortArray2String(short[] arr) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			byte[] b = new byte[1];
			b[0] = (byte)arr[i];
			sb.append(new String(b));
		}

		return sb.toString();
	}

	byte[] shortArray2byteArray(short[] arr) {
		byte[] bArr = new byte[arr.length];

		for (int i = 0; i < arr.length; i++) {
			bArr[i] = (byte)(arr[i] & 0xFF);
		}

		return bArr;
	}

	public void messageHandler(coap_context_t ctx, coap_listnode node,
			String data) {

		Log.i(LOG_TAG, "INF: Java Client messageHandler()");

		lr.requestStop();

		//		System.out.println(LI+"INF: ****** pdu (" + node.getPdu().getLength()
		//				+ " bytes)" + " v:" + node.getPdu().getHdr().getVersion()
		//				+ " t:" + node.getPdu().getHdr().getType() + " oc:"
		//				+ node.getPdu().getHdr().getOptcnt() + " c:"
		//				+ node.getPdu().getHdr().getCode() + " id:"
		//				+ node.getPdu().getHdr().getId());

		if (node.getPdu().getHdr().getVersion() != coapConstants.COAP_DEFAULT_VERSION) {
			Log.w(LOG_TAG, "WARN: dropped packet with unknown version "+
					node.getPdu().getHdr().getVersion());
			return;
		}

		/* send 500 response */
		coap_pdu_t pdu = null;
		if (node.getPdu().getHdr().getCode() < coapConstants.COAP_RESPONSE_100
				&& node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
			Log.w(LOG_TAG, "WARN: received error code for CONfirmable message");
			pdu = new_response(ctx, node, coapConstants.COAP_RESPONSE_500);
			finish(ctx, node, pdu);
			return;
		}		

		//block not supported yet		

		if (node.getPdu().getHdr().getCode() == coapConstants.COAP_RESPONSE_200) {

			Log.i(LOG_TAG, "INF: 200 OK");

			short[] pdudata = new short[node.getPdu().getLength()];

			int len = coap.coap_get_data_java(node.getPdu(), pdudata);

			Log.i(LOG_TAG, "INF: ****** data:'" + pdudata + "'" +len);

			Message msg = messageUIHandlerReceived.obtainMessage();
			msg.arg1 = node.getPdu().getHdr().getId();
			msg.obj = pdudata;
			messageUIHandlerReceived.sendMessage(msg);

			// responseTextView.setText(node.getPdu().getData());
			// System.out.println(LI+"INF: "+coap.get_addr(node.getRemote()));
		} else {
			Log.w(LOG_TAG, "WARN: not 200 OK: "+node.getPdu().getHdr().getCode());
		}

		/* acknowledge if requested */
		if (node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
			pdu = new_ack(ctx, node);
			Log.i(LOG_TAG, "INF: Acknowledge CON message");
		}

		finish(ctx, node, pdu);
		Log.i(LOG_TAG, "INF: Java Client messageHandler()~");
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
