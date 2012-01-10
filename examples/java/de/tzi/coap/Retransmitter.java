package de.tzi.coap;

import java.io.IOException;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_listnode;

public class Retransmitter extends Thread {
	boolean doStop = false;
	coap_context_t ctx;
	String RCI = "Retransmitter: ";

	public Retransmitter(coap_context_t ctx) {
		System.out.println(RCI+"INF: init Retransmitter() @ "+System.currentTimeMillis());
		this.ctx = ctx;
	}

	public void run() {
		System.out.println(RCI+"INF: run() @ "+System.currentTimeMillis());
		try {
			retransmitLoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void requestStop() {
		System.out.println(RCI+"INF: requestStop()");
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
				System.out.println(RCI+"INF: coap_retransmit()");
				nextpdu = coap.coap_peek_next( ctx );
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println(RCI+"INF: retransmitLoop finshed.");
	}
}