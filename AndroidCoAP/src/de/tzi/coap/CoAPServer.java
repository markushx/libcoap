package de.tzi.coap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_listnode;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;
import de.tzi.coap.jni.coap_resource_t;
import de.tzi.coap.jni.coap_uri_t;

/**
 * Sample implementation of a CoAP server for Android using SWIGified libcoap.
 * 
 * @author Markus Becker <mab@comnets.uni-bremen.de>
 * @author Thomas Poetsch <thp@comnets.uni-bremen.de>
 */

public class CoAPServer extends Activity {

	public static final String LOG_TAG = "CoAP";

	HashMap<String, CoapJavaResource> resourceMap = new HashMap<String, CoapJavaResource>(); 
	
	public static final String PREFS_NAME = "MyCoapServerPrefs";
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

	//LinearLayout coap_sms_layout;
	//EditText noText;
	//Spinner uriSpinnerSMS;

	ToggleButton btn_start;

	static TextView statusText;

	ScrollView sv;
	TextView responseTextView;
	//~ UI elements

	PowerManager.WakeLock wl;

	// CoAP specifics
	static Random generateRand = new Random();
	coap_context_t ctx;

	// IP
	DatagramSocket serverSocket;
	LowerReceive lr = null;
	Retransmitter rt = null;
	//RequesterThread reqthr = null;

	// SMS
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

	static void setStatus(CharSequence cs) {
		statusText.setText(cs);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_server);

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

		// register us for incoming SMS
		replyReceiver = new SmsReceiver();
		registerReceiver(replyReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
		
		// setup UI elements
		coap_ip_layout = (LinearLayout)findViewById(R.id.coap_ip_layout);
		ipText = (EditText)findViewById(R.id.editTextIP);
		portText = (EditText)findViewById(R.id.editTextPort);
		uriSpinner = (Spinner) findViewById(R.id.uriSpinner);

		//coap_sms_layout = (LinearLayout)findViewById(R.id.coap_sms_layout);
		//noText = (EditText)findViewById(R.id.editTextTelephoneNumber);
		//uriSpinnerSMS = (Spinner) findViewById(R.id.uriSpinnerSMS);

		btn_start = (ToggleButton)findViewById(R.id.startButton);

		statusText = (TextView) findViewById(R.id.textStatus);

		sv = (ScrollView)findViewById(R.id.scrollView1);
		responseTextView  = (TextView)findViewById(R.id.responseTextView);
		//~ setup UI elements

		btn_start.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button, boolean checked) {
				if (checked == true) { 
					// start the server

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

					setup_coap(port);

					responseTextView.setText("");
					Log.d("CoAP", "start LowerReceive");
					lr = new LowerReceive(ctx, serverSocket);
					lr.start();
					Log.d("CoAP", "LowerReceive started");
					//rt = new Retransmitter(ctx);
					//rt.start();

				} else {
					// stop the server

					stopServer();
				}
			}
		});

		// --------
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		ipText.setText(settings.getString("ip", ""));
		portText.setText(settings.getString("port", ""));

		//setup UI for proper mode: SMS resp. IP 
		/*if (settings.getInt("mode", MODE_IP) == MODE_IP) {
			coap_sms_layout.setVisibility(View.GONE);
			coap_ip_layout.setVisibility(View.VISIBLE);
		} else {
			coap_sms_layout.setVisibility(View.VISIBLE);
			coap_ip_layout.setVisibility(View.GONE);	
		}*/
	}

	public void stopServer() {
		//TODO: implement
	}
	
	@Override
	protected void onStart() {
		super.onRestart();

		/*
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		uris = stringToArray(settings.getString("uris", initResourcePref()));
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, uris);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		uriSpinner.setAdapter(adapter);
		uriSpinner.setSelection(settings.getInt("resource", settings.getInt("selUri", 0)));	
		uriSpinnerSMS.setAdapter(adapter);
		uriSpinnerSMS.setSelection(settings.getInt("resource", settings.getInt("selUri", 0)));	
		adapter.notifyDataSetChanged();
		*/		
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("ip", ipText.getText().toString());
		editor.putString("port", portText.getText().toString());
		//editor.putInt("resource", uriSpinner.getSelectedItemPosition());
		//editor.putInt("selUri", uriSpinner.getSelectedItemPosition());
		//editor.putInt("resourceSMS", uriSpinnerSMS.getSelectedItemPosition());
		//editor.putInt("selUriSMS", uriSpinnerSMS.getSelectedItemPosition());

		/*
		StringBuilder spinnerEntries = new StringBuilder();
		for (int i = 0; i < uriSpinner.getCount(); i++) {
			spinnerEntries.append(uriSpinner.getItemAtPosition(i)+",");		
		}
		spinnerEntries.deleteCharAt(spinnerEntries.length()-1);
		editor.putString("uris", spinnerEntries.toString());
		 */
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
			//reqthr.requestStop();
		} catch (NullPointerException e) {
			// do nothing, just exit
		}

		// close server socket
		serverSocket.close();
		
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

	public void setup_coap(int port) {
		// create coap_context
		Log.i(LOG_TAG, "INF: create context");
		ctx = coap.coap_new_context(port);
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
		init_resources(ctx);
 /*
		try {
			ListNets ln = new ListNets();
			ln.init_ipv6();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/
		Log.e(LOG_TAG, "workaround: " + get_global_ipv6_address());
		
	 	String global_ipv6_localhost = get_global_ipv6_address();
		
		try {
			Log.i(LOG_TAG, "INF: opening new socket on port " + port);
			serverSocket = new DatagramSocket(null);
			serverSocket.setReuseAddress(true);
			try {
				Log.i(LOG_TAG, "INF: localAddr");
				SocketAddress localAddr = new InetSocketAddress((Inet6Address)InetAddress.getByName(global_ipv6_localhost), port);
				Log.i(LOG_TAG, "INF: binding socket");
				serverSocket.bind(localAddr);
				Log.i(LOG_TAG, "INF: bound socket");
			} catch (UnknownHostException e) {
				Log.e(LOG_TAG, "ERR: failed to bind socket");
				e.printStackTrace();
				serverSocket.close();
			}

			Log.i(LOG_TAG, "INF: opened new socket");
			//Log.i(LOG_TAG, "INF: opened new socket on port " + serverSocket.getLocalPort());
		} catch (BindException e) {
			Log.e(LOG_TAG, "ERR: creating server socket failed.");
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/*
	public class ListNets 
    {
		public ListNets() {
		}

		void init_ipv6() throws SocketException {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
                displayInterfaceInformation(netint);
        }

        void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
        	Log.e(LOG_TAG, "Display name: " + netint.getDisplayName());
        	Log.e(LOG_TAG, "Name: " + netint.getName());
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            	Log.e(LOG_TAG, "InetAddress: " + inetAddress.toString());
            }
        }
    }
	*/
	
	String get_global_ipv6_address() {
		Boolean ipv6Available = false;
		
		try {
			Process process = Runtime.getRuntime().exec(
					"cat /proc/net/if_inet6");

			// Reads stdout
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			StringBuilder log = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {

				String[] buffer;
				buffer = line.split(" ");
				if (!(line.startsWith("0000") || line.startsWith("fe"))) {
					StringBuffer output = new StringBuffer(buffer[0]);

					int i = 4;
					while (output.length() < 39) {
						output.insert(i, ":");
						i = i + 5;
					}
					log.append(output);
					ipv6Available = true;
					break;
				}

			}
			Log.d("IPHELPER", "" + log);

			reader.close();

			// Waits for the command to finish.
			process.waitFor();

			// return output.toString();
			if (ipv6Available)
				return log.toString();
			else
				return null;

		} catch (IOException e) {
			// do nothing
		} catch (InterruptedException e) {
			// do nothing
		} catch (RuntimeException re) {
			// do nothing
		}
		return null;
	}

/*	
	void init_ipv6() {
		Log.e(LOG_TAG, "IP addresses");
       
	    try {
			for(InetAddress addr : InetAddress.getAllByName("::1"))
			{
			    if (addr instanceof Inet6Address) {
			    	Log.e(LOG_TAG, "IPV6: " + ((Inet6Address)addr).toString());
			    } else {
			    	Log.e(LOG_TAG, "IP: " +addr.toString());
			    }
			}
		} catch (UnknownHostException e) {
			Log.e(LOG_TAG, "exception");
			e.printStackTrace();
		}
	    //Log.e(LOG_TAG, "No IPv6 address found for");
	    //throw new UnknownHostException("No IPv6 address found for");
	}
	*/
	void init_resources(coap_context_t ctx) {
		//FIXME!!!
		String u_time = "/time";
		//str d_time = "server's local time and date";
		
		coap_resource_t r = null; // TODO: really init??
		coap_uri_t uri = coap.coap_new_uri(u_time, u_time.length());
		
		//TODO: move most of the following into CoapJavaResource:
		
		//r.setUri(uri);
		//r.setName(d_time);
		
		//r.setMediatype((short)coapConstants.COAP_MEDIATYPE_TEXT_PLAIN);
		//r.setDirty(0);
		//r.setWritable(0);
		
		//SWIGTYPE_p_f_p_coap_uri_t_p_unsigned_char_unsigned_int_p_unsigned_char_p_unsigned_int_p_int__int arg0 = null; // TODO: really init
		//r.setData(arg0);
		//r.setMaxage(1);
		coap.coap_add_resource(ctx, r);
		
		resourceMap.put(u_time, new CoapJavaResourceTime((short)coapConstants.COAP_MEDIATYPE_TEXT_PLAIN));
	}

	private void setOperationMode(int item) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();	
		editor.putInt("mode", item);
		editor.commit();
		
		if (item == MODE_IP) {
			//coap_sms_layout.setVisibility(View.GONE);
			coap_ip_layout.setVisibility(View.VISIBLE);
		} else {
			//coap_sms_layout.setVisibility(View.VISIBLE);
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

	/*
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
*/
	/*
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

		setStatus("CoAP request (MID: "+pdu.getHdr().getId()+") sent.");
	}
	*/
	
	//JNI callback to replace C socket with Java DatagramSocket
	public void coap_send_impl(coap_context_t ctx, SWIGTYPE_p_sockaddr_in6 dst, 
			coap_pdu_t pdu,
			int free_pdu) {
		// if you change the signature (name or parameters) of this 
		// function, the swig interface needs to be changed as well
		
		Log.i(LOG_TAG, "INF: callback coap_send_impl @ "+System.currentTimeMillis());
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		//if (settings.getInt("mode", MODE_IP) == MODE_IP) {
			Log.i(LOG_TAG, "INF: lowerSendIP");
			try {
				InetAddress ipAddress = InetAddress.getByName(coap.get_addr(dst));
				lowerSendIP(coap.get_bytes(pdu), ipAddress, coap.get_port(dst));
			} catch (UnknownHostException e) {
				Log.w(LOG_TAG, "WARN: UnknownHostException\n");
				e.printStackTrace();
			} catch (IOException e) {
				Log.w(LOG_TAG, "WARN: No network connectivity...\n");
				//e.printStackTrace();
			}
		/*} else {
			Log.i(LOG_TAG, "INF: lowerSendSMS");
			lowerSendSMS(coap.get_bytes(pdu), noText.getText().toString());
		}*/

		Log.i(LOG_TAG, "INF: callback coap_send_impl return.");
		return;
	}

	public void lowerSendIP(byte[] sendData, InetAddress IPAddress, int port) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		Log.i(LOG_TAG, "INF: sending message to " + IPAddress.getHostAddress() +":"+port);
		serverSocket.send(sendPacket);
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

	//message handler to update UI thread for received messages
	/*
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
				} else if (uriHM.get(msg.arg1).equals("r")) {
					ResourceR val = new ResourceR(pdudata);
					val.show();
					responseTextView.append("r: T:"+ ((float)val.getTemp()/ 100 - 273.15)
							+ " H:" + ((float)val.getHum()/100) 
							+ " V:" + ((float)val.getVolt()/100) + "\n");
				} else {
					responseTextView.append("URI not found\n");
				}

			} else {
				responseTextView.append("URI not available\n");
			}

			uriHM.clear();
		}
	};*/

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
	
	coap_pdu_t handle_get(coap_context_t ctx, coap_listnode node) {
		coap_pdu_t pdu = null;
		coap_uri_t uri = coap.coap_new_uri(null, 0);
		int code;
		int blk = 0; // TODO
		int blklen = 0; // TODO
		boolean finished = true;
		int mediatype = coapConstants.COAP_MEDIATYPE_ANY;
		String buf = "";
		
		Log.i(LOG_TAG, "INF: handle_get");
		
		if ( coap.coap_get_request_uri(node.getPdu(), uri) == 0) {
			Log.i(LOG_TAG, "INF: get_req_uri == 0");
			return null;
		}

		// GET /
		if ( uri.getPath().getLength() == 0 ) {
			Log.i(LOG_TAG, "INF: GET /");
			pdu = new_response(ctx, node, coapConstants.COAP_RESPONSE_200);
			if ( pdu == null)
				return null;

			add_contents( pdu, coapConstants.COAP_MEDIATYPE_TEXT_PLAIN, "index".length() - 1, "index" );
			return pdu;
		}
		
		Log.i(LOG_TAG, "INF: GET /xxx");
		// GET other resources
		CoapJavaResource resource = get_resource(uri);
		if (resource == null) {
			Log.i(LOG_TAG, "INF: GET /unknown " + uri.getPath().getS());
			return new_response(ctx, node, coapConstants.COAP_RESPONSE_404);
		}

		/* check if requested mediatypes match */
		//TODO:
		/*
		if ( coap.coap_check_option(node.getPdu(), (short)coapConstants.COAP_OPTION_ACCEPT) 
				&& !mediatype_matches(node.getPdu(), r->getMediatype()) ) {
			debug("media type mismatch\n");
			return new_response(ctx, node, COAP_RESPONSE_415);
		}
		 */
		
		Log.i(LOG_TAG, "INF: GET getData() " + blklen + " " + finished);
		/* invoke callback function to get data representation of requested
	     resource */
		CoapReturnData crd = resource.getData(uri, mediatype, blk, buf, blklen, finished);
		Log.i(LOG_TAG, "INF: GET getData()~ " + crd.code + " "+ crd.buf + " " + crd.blklen + " " + crd.finished);
		
		//TODO:
//		if ( resource.getData() ) {
//			mediatype = resource->mediatype;
//
//			code = resource->data(&uri, &mediatype, 
//					(blk & ~0x0f) << (blk & 0x07), buf, &blklen, 
//					&finished);
//		} else {
//			/* check if the well-known URI was requested */
//			if (memcmp(uri.path.s, COAP_DEFAULT_URI_WELLKNOWN, 
//					MIN(uri.path.length, sizeof(COAP_DEFAULT_URI_WELLKNOWN) - 1))
//					== 0) {
//				mediatype = resource->mediatype;
//				code = resource_wellknown(ctx, resource, &mediatype,
//						(blk & ~0x0f) << (blk & 0x07), buf, &blklen, 
//						&finished);
//			} else {
//				/* no callback available, set code, blklen and finished manually
//		 (-> empty payload) */
//				code = COAP_RESPONSE_200;
//				blklen = 0;
//				finished = 1;
//			}
//		}

		pdu = new_response(ctx, node, crd.code);
		if ( pdu == null ) {
			Log.i(LOG_TAG, "INF: GET response pdu failed");
			return null;
		}
		
		if ( crd.blklen > 0 ) {
			Log.i(LOG_TAG, "INF: GET data present");
			
			/* add content-type */
			if ( mediatype != coapConstants.COAP_MEDIATYPE_ANY ) 
				coap.coap_add_option(pdu, (short)coapConstants.COAP_OPTION_CONTENT_TYPE, 1, Integer.toHexString(mediatype));

			/* set Max-age option unless resource->maxage is zero */
			//TODO

			/* set Etag option unless resource->etag is zero */
			//TODO
			
			/* handle subscription if requested */
			// TODO

			/* add a block option when it has been requested explicitly or
			 * there is more data available */
			// TODO

		    /* We will add contents only when it is not empty. This might lead
		     * to problems when this is the last block of a sequence of more
		     * than one block. For now, we ignore this problem as it can
		     * happen only when the block sizes have changed.
		     */
		    if (coap.coap_add_data(pdu, crd.blklen, crd.buf) == 0) {
		    	Log.w(LOG_TAG, "WARN: add_data failed");
		    	/* FIXME: handle this case -- must send 500 or something */
		    }
		} else {
			Log.i(LOG_TAG, "INF: GET no data");
		}

		Log.i(LOG_TAG, "INF: GET return pdu");
		return pdu;
	}

	CoapJavaResource get_resource(coap_uri_t uri) {
		Log.i(LOG_TAG, "INF: get_resource: " + uri.toString());
		Log.i(LOG_TAG, "INF: get_resource: " + uri.getPath().getS());
		Log.i(LOG_TAG, "INF: get_resource: " + uri.getPath().getLength());
		
		Log.i(LOG_TAG, "INF: get_resource: " + "/"+uri.getPath().getS().substring(0, (int)uri.getPath().getLength()));
						
		return resourceMap.get("/"+uri.getPath().getS().substring(0, (int)uri.getPath().getLength()));
	}

	class CoapReturnData {
		int code;
		String buf;
		int blk;
		int blklen;
		boolean finished;

		public CoapReturnData(int code, String buf, int blk, int blklen,
				boolean finished) {
			super();
			this.code = code;
			this.buf = buf;
			this.blk = blk;
			this.blklen = blklen;
			this.finished = finished;
		}		
	}
	
	class CoapJavaResource {
		int mediatype;
		
		CoapJavaResource(int mediatype) {
			this.mediatype = mediatype;
		}

		int getMediaType() {
			return this.mediatype;
		}
		
		// pure virtual?
		CoapReturnData getData(coap_uri_t uri, int mediatype, int offset, String buf, int buflen, boolean finished) {
			return null; // TODO: check correct code?
		}
	}
	
	class CoapJavaResourceTime extends CoapJavaResource {
		CoapJavaResourceTime(int mediatype) {
			super(mediatype);
		}

		CoapReturnData getData(coap_uri_t uri, int mediatype, int offset, String buf, int buflen, boolean finished) {
			Log.i(LOG_TAG, "INF: getData() ");
			
			offset = 0;
			buf = "it's teatime.";
			buflen = buf.length(); 
			Log.i(LOG_TAG, "INF: getData(): " + buf + " " + buflen);
			finished = true;
			
			CoapReturnData crd = new CoapReturnData(
					coapConstants.COAP_RESPONSE_200,
					"it's teatime.",
					0,
					"it's teatime.".length(),
					true);
			
			Log.i(LOG_TAG, "INF: getData()~ " + coapConstants.COAP_RESPONSE_200);
			return crd;
		}
	}
	
	public void messageHandler(coap_context_t ctx,
			coap_listnode node,
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
		Log.i(LOG_TAG, "INF: version OK()");
		
		coap_pdu_t pdu = null;
		if (node.getPdu().getHdr().getCode() == coapConstants.COAP_REQUEST_GET) {
			Log.i(LOG_TAG, "INF: GET");
			pdu = handle_get(ctx, node/*, data*/);
			Log.i(LOG_TAG, "INF: GET~");

			if ( pdu == null && node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON ) {
				Log.i(LOG_TAG, "INF: RST");
				pdu = new_rst( ctx, node, coapConstants.COAP_RESPONSE_500 );
			} else {
				Log.i(LOG_TAG, "INF: PDU OK or non-CON");
			}
			
		} else if (node.getPdu().getHdr().getCode() == coapConstants.COAP_REQUEST_PUT) {
			//FIXME: implement the other methods
			Log.i(LOG_TAG, "INF: PUT");
		} else if (node.getPdu().getHdr().getCode() == coapConstants.COAP_REQUEST_POST) {
			//FIXME: implement the other methods
			Log.i(LOG_TAG, "INF: POST");
		} else if (node.getPdu().getHdr().getCode() == coapConstants.COAP_REQUEST_DELETE) {
			//FIXME: implement the other methods
			Log.i(LOG_TAG, "INF: DELETE");
		} else {
			Log.w(LOG_TAG, "WARN: unimplemented method");	
			if (node.getPdu().getHdr().getCode() >= coapConstants.COAP_RESPONSE_100
					&& node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
				Log.w(LOG_TAG, "WARN: received error code for CONfirmable message");
				pdu = new_rst(ctx, node, coapConstants.COAP_RESPONSE_500);
			}
		}
		
		if (pdu != null) {
			Log.i(LOG_TAG, "INF: send reply");
			if (coap.coap_send(ctx, node.getRemote(), pdu) == coapConstants.COAP_INVALID_TID) {
				Log.i(LOG_TAG, "INF: send reply failed");
				coap.coap_delete_pdu(pdu);
			}
		} else {
			Log.i(LOG_TAG, "INF: no pdu");
		}
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
	
	coap_pdu_t new_rst(coap_context_t ctx, coap_listnode node, int code ) {
		coap_pdu_t pdu = coap.coap_new_pdu();
		if (pdu != null) {
			pdu.getHdr().setType(coapConstants.COAP_MESSAGE_RST);
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
	
	void add_contents( coap_pdu_t pdu, int mediatype, int len, String data) {
	  if (pdu == null)
	    return;
	  
	  /* add content-encoding */
	  coap.coap_add_option(pdu, (short)coapConstants.COAP_OPTION_CONTENT_TYPE,
			  1, Integer.toHexString(coapConstants.COAP_MEDIATYPE_APPLICATION_LINK_FORMAT));

	  /* TODO: handle fragmentation (check result code) */
	  coap.coap_add_data(pdu, len, data);
	}

}
