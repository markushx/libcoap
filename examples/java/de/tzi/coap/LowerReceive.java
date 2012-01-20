package de.tzi.coap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;

public class LowerReceive extends Thread {
	boolean doStop = false;
	coap_context_t ctx;
	String RCI = "LowerReceive: ";
	DatagramSocket clientSocket;

	public LowerReceive(coap_context_t ctx, DatagramSocket clientSocket) {
		this.ctx = ctx;
		this.clientSocket = clientSocket;
	}

	public void run() {
		Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest run()");
		try {
			receiveLoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void requestStop() {
		Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest requestStop");
		doStop = true;
	}

	private void receiveLoop() throws IOException {
		SWIGTYPE_p_sockaddr_in6 src;
		byte[] receiveData = new byte[coapConstants.COAP_MAX_PDU_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		while (!doStop) {
			Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest: waiting for incoming message...");
			clientSocket.receive(receivePacket);

			src = coap.sockaddr_in6_create(coapConstants.AF_INET6, receivePacket.getPort(),
					receivePacket.getAddress().getHostAddress());

			coap.coap_read(ctx, src, receivePacket.getData(), receivePacket.getLength());
			coap.coap_dispatch(ctx);

			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		Log.i(CoAPClient.LOG_TAG, "INF: LowerRequest: receiveLoop finished");
//		System.out.println(RCI+"INF: receiveLoop finshed.");
	}
}