/*
 * Server.java -- Sample JAVA/JNI/SWIG server for libcoap
 *
 * Copyright (C) 2011 Markus Becker <mab@comnets.uni-bremen.de>
 *
 */

package de.tzi.coap;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.sockaddr_in6;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import de.tzi.coap.jni.coap_listnode;

import java.util.Random;

public class Server extends CoapBase {

    public Server() {

    }

    public void run() {
	boolean quit = false;

	// create coap_context
	System.out.println("INF: create context");
	coap_context_t ctx;
	ctx = coap.coap_new_context(coapConstants.COAP_DEFAULT_PORT);
	if (ctx == null) {
	    System.out.println("Could not create context");
	    return;
	}

	// register ourselves for message handling
	System.out.println("INF: register message handler");
	coap.register_message_handler(ctx, this);

	while (!quit) {
	    //System.out.println("INF: check_receive()");
	    coap.check_receive(ctx);
	    //System.out.println("INF: ~check_receive()");
	}

	System.out.println("INF: ~run()");
    }

    public void messageHandler(coap_context_t ctx,
			       coap_listnode node,
			       String data) {
	System.out.println("INF: Java Server messageHandler()");

	System.out.println("****** ctx " + ctx);
	System.out.println("****** node " + node);
	// System.out.println("****** data " + data);
	System.out.println("****** node.getPdu() " + node.getPdu());
	System.out.println("****** node.getPdu().getHdr() " + node.getPdu().getHdr());
	System.out.println("****** node.getPdu().getHdr().getOptcnt() " + node.getPdu().getHdr().getOptcnt());
	System.out.println("****** node.getPdu().getHdr().getType() " + node.getPdu().getHdr().getType());
	System.out.println("****** node.getPdu().getHdr().getVersion() " + node.getPdu().getHdr().getVersion());
	System.out.println("****** node.getPdu().getHdr().getCode() " + node.getPdu().getHdr().getCode());
	System.out.println("****** node.getPdu().getHdr().getId() " + node.getPdu().getHdr().getId());
	System.out.println("****** node.getPdu().getData() " + node.getPdu().getData());

	System.out.println("****** node.getPdu().getLength() " + node.getPdu().getLength());
	System.out.println("****** node.getPdu().getOptions() " + node.getPdu().getOptions());

	String pdudata = node.getPdu().getData();
	System.out.println(pdudata);

	//TODO: do more...

	System.out.println("INF: ~messageHandler()");
    }

    public static void main(String argv[]) throws Exception {
	System.out.println("INF: main()");
	Server s = new Server();
	s.run();
	System.out.println("INF: ~main()");
    }

}
