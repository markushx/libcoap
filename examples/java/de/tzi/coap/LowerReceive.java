package de.tzi.coap08;

import java.io.IOException;

import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_queue_t;

public class CheckThread extends Thread {
	boolean doStop = false;
	coap_context_t ctx;
    coap_queue_t nextpdu;
	
    int mainLoopSleepTimeMilli = 50;

	public CheckThread(coap_context_t ctx) {
		this.ctx = ctx;
	}

	public void run() {
		Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest run()");
		try {
			loop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void requestStop() {
		Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest requestStop");
		doStop = true;
	}

	private void checkRetransmit() {
		//System.out.print("r");
		nextpdu = coap.coap_peek_next(ctx);

		if (nextpdu != null) {
			//System.out.println("R cond " + nextpdu.getT() + "," + System.currentTimeMillis()/1000 );
			if (nextpdu.getT() <= System.currentTimeMillis()/1000) {
				System.out.println("INF: CoAP retransmission");
				coap.coap_retransmit( ctx, coap.coap_pop_next( ctx ) );
			}
		}
	}

	private void checkReceiveTraffic() {
		coap.coap_read(ctx);
		coap.coap_dispatch(ctx);
	}

	private void loop() throws IOException {
		while (!doStop) {

			Log.i(CoAPClient.LOG_TAG, ".");
			checkReceiveTraffic();
			Log.i(CoAPClient.LOG_TAG, "#");
			checkRetransmit();
			Log.i(CoAPClient.LOG_TAG, "*");
			
			try {
				Thread.sleep(mainLoopSleepTimeMilli);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest: receiveLoop finished");
	}
}
