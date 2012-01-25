package de.tzi.coap.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;
import de.tzi.coap.CoAPClient;
import de.tzi.coap.CoAPServer;

public class CoAPSMSReceiver extends BroadcastReceiver {

	CoAPClient cc = null;
	CoAPServer cs = null;
	
	public CoAPSMSReceiver(CoAPClient cc) {
		super();
		this.cc = cc;
	}

	public CoAPSMSReceiver(CoAPServer cs) {
		super();
		this.cs = cs;
	}
	
	public static final String LOG_TAG = "CoAPSMS";
	
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
				if (cc != null)
					cc.smsReceived(b, msgs[0].getOriginatingAddress());
				if (cs != null)
					cs.smsReceived(b, msgs[0].getOriginatingAddress());
				
			} catch (Exception e) {
				Log.e(LOG_TAG, "[SMSApp] exception", e);
			}
		}
	}	
}
