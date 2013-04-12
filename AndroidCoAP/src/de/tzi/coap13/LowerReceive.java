package de.tzi.coap13;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;

public class LowerReceive extends Thread {
	boolean doStop = false;
	coap_context_t ctx;
	String RCI = "[CoAP] LowerReceive: ";
	DatagramSocket socket;
	Object syncObject; 
	
	public LowerReceive(coap_context_t ctx, DatagramSocket socket, Object syncObject) {
		this.ctx = ctx;
		this.socket = socket;
		this.syncObject = syncObject;
	}

	public void run() {
		System.out.println(RCI+"INF: run()");
		try {
			receiveLoop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void requestStop() {
		System.out.println(RCI+"INF: requestStop()");
		doStop = true;
	}

	private void receiveLoop() throws IOException {
		SWIGTYPE_p_sockaddr_in6 src;
		byte[] receiveData = new byte[coapConstants.COAP_MAX_PDU_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		while (!doStop) {
			System.out.println(RCI+"INF: waiting for incoming messages...");
			socket.receive(receivePacket);

			src = coap.sockaddr_in6_create(coapConstants.AF_INET6, receivePacket.getPort(),
					receivePacket.getAddress().getHostAddress());

			synchronized(syncObject) {
				//System.out.println(RCI+"INF: ENTER SYNC");
				coap.coap_read(ctx, src, receivePacket.getData(), receivePacket.getLength());
				coap.coap_dispatch(ctx);
				//System.out.println(RCI+"INF: LEAVE SYNC");
			}
			
			coap.sockaddr_in6_free(src);
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println(RCI+"INF: receiveLoop finshed.");
	}
}
