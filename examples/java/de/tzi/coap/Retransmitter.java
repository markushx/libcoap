package de.tzi.coap08;

import java.io.IOException;

import android.os.Message;
import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_listnode;

public class Retransmitter extends Thread {
	boolean doStop = false;
	coap_context_t ctx;

	public Retransmitter(coap_context_t ctx) {
		Log.i(CoAPClient.LOG_TAG, "INF: init Retransmitter() @ "+System.currentTimeMillis());
		this.ctx = ctx;
	}

	public void run() {
		Log.i(CoAPClient.LOG_TAG, "INF: run() @ "+System.currentTimeMillis());
		try {
			retransmitLoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void requestStop() {
		Log.i(CoAPClient.LOG_TAG, "INF: requestStop()");
		doStop = true;
	}

	private void retransmitLoop() throws IOException {
		coap_listnode nextpdu;

		//initial waiting time for first retransmit
		try {
			Thread.sleep(coapConstants.COAP_DEFAULT_RESPONSE_TIMEOUT*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//returns after coapConstants.COAP_DEFAULT_MAX_RETRANSMIT + 1 or when stopped manually
		while (!doStop || (coap.coap_can_exit(ctx) == 0)) {
			nextpdu = coap.coap_peek_next(ctx);

			while ( (nextpdu != null) && (nextpdu.getT() <= System.currentTimeMillis()/1000) ) {
				coap.coap_retransmit( ctx, coap.coap_pop_next( ctx ) );
				Log.i(CoAPClient.LOG_TAG, "INF: coap_retransmit()");
				nextpdu = coap.coap_peek_next( ctx );
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (coap.coap_can_exit(ctx) == 1) {
				doStop = true;
			} else {

				Message msg = CoAPClient.messageUIHandlerRetransmission.obtainMessage();
				msg.arg1 = nextpdu.getRetransmit_cnt();
				CoAPClient.messageUIHandlerRetransmission.sendMessage(msg);

			}
			
		}

		Log.i(CoAPClient.LOG_TAG, "INF: retransmitLoop finshed.");
	}
}