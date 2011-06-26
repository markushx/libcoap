/*
 * Client.java -- Sample JAVA/JNI/SWIG client for libcoap
 *
 * Copyright (C) 2011 Markus Becker <mab@comnets.uni-bremen.de>
 *
 */

package de.tzi.coap;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.SWIGTYPE_p_unsigned_char;
import de.tzi.coap.jni.sockaddr_in6;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import de.tzi.coap.jni.coap_listnode;

import java.util.Random;

public class Client extends CoapBase {

    public Client() {

    }

    public void run() {
	System.out.println("run()");
	Random generator = new Random();

	// create coap_context
	System.out.println("INF: create context");
	coap_context_t ctx;
	ctx = coap.coap_new_context(77777);
	if (ctx == null) {
	    System.out.println("Could not create context");
	    return;
	}

	// register ourselves for message handling
	System.out.println("INF: register message handler");
	coap.register_message_handler(ctx, this);

	// create header
	System.out.println("INF: create header");
	coap_hdr_t hdr = new coap_hdr_t();
	hdr.setOptcnt(0);
	hdr.setType(coapConstants.COAP_MESSAGE_CON);
	hdr.setVersion(coapConstants.COAP_DEFAULT_VERSION);
	hdr.setCode(coapConstants.COAP_REQUEST_GET);
	hdr.setId(generator.nextInt(0xFFFF));

	// create pdu
	System.out.println("INF: create pdu");
	coap_pdu_t pdu;
	pdu = coap.coap_new_pdu();
	pdu.setHdr(hdr);

	// add media type option
	System.out.println("INF: create option mediatype");
	String mt = "" + coap.COAP_MEDIATYPE_TEXT_PLAIN;
	coap.coap_add_option(pdu, (short)coap.COAP_OPTION_CONTENT_TYPE,
			     (long)1, mt);

	// add uri option
	System.out.println("INF: create option uri");
	String stUri = ".well-known/core";
	coap.coap_add_option(pdu, (short)coap.COAP_OPTION_URI_PATH,
			     (long)stUri.length(), stUri);


	// add data
	System.out.println("INF: create data");
	String stData = "tst";
	coap.coap_add_data(pdu, stData.length(), stData);

	// set destination
	sockaddr_in6 dst = coap.sockaddr_in6_create(coapConstants.AF_INET6,
						    coapConstants.COAP_DEFAULT_PORT,
						    "::1");

	// send pdu
	System.out.println("INF: coap_send()");
	coap.coap_send(ctx, dst, pdu);
	System.out.println("INF: ~coap_send()");

	// receive and dispatch -> will trigger messageHandler() callback
	System.out.println("INF: coap_read()");
	coap.coap_read(ctx);
	System.out.println("INF: ~coap_read()");
	System.out.println("INF: coap_dispatch()");
	coap.coap_dispatch(ctx);
	System.out.println("INF: ~coap_dispatch()");

	// free data
	coap.sockaddr_in6_free(dst);
	//TODO: more to free?

	System.out.println("~run()");
    }

    public void messageHandler(coap_context_t ctx,
			       coap_listnode node,
			       String data) {
	System.out.println("INF: messageHandler()");

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

	System.out.println("~INF: messageHandler()");
    }

    public static void main(String argv[]) throws Exception {
	System.out.println("INF: main()");
	Client c = new Client();
	c.run();
	System.out.println("INF: ~main()");
    }
}
