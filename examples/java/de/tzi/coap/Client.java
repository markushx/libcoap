/*
 * Client.java -- Sample JAVA/JNI/SWIG client for libcoap
 *
 * Copyright (C) 2011 Markus Becker <mab@comnets.uni-bremen.de>
 *
 */

package de.tzi.coap;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coapJNI;
import de.tzi.coap.jni.sockaddr_in6;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import de.tzi.coap.jni.coap_listnode;
import de.tzi.coap.jni.in6_addr;
import de.tzi.coap.jni.in6_addr___in6_u;
import de.tzi.coap.jni.sockaddr_in6;
import de.tzi.coap.jni.addrinfo;

import java.util.Random;

public class Client extends CoapBase {

    static Random generator = new Random();

    public Client() {
    }

    //TODO: options as parameters
    coap_pdu_t coap_new_request(int methodid, String payload) {
	System.out.println("INF: create pdu");
	coap_pdu_t pdu = coap.coap_new_pdu();
	if (pdu == null) {
	    return pdu;
	}

	System.out.println("INF: set header values");
	pdu.getHdr().setVersion(coapConstants.COAP_DEFAULT_VERSION);
	pdu.getHdr().setType(coapConstants.COAP_MESSAGE_CON);
	pdu.getHdr().setOptcnt(0);
	pdu.getHdr().setCode(methodid);
	pdu.getHdr().setId(generator.nextInt(0xFFFF));

	// add media type option
	System.out.println("INF: create option mediatype");
	String mt = "" + coap.COAP_MEDIATYPE_TEXT_PLAIN;
	coap.coap_add_option(pdu, (short)coap.COAP_OPTION_CONTENT_TYPE,
			     1, mt);

	// add uri option
	System.out.println("INF: create option uri");
	String stUri = ".well-known/core";
	coap.coap_add_option(pdu, (short)coap.COAP_OPTION_URI_PATH,
			     stUri.length(), stUri);


	System.out.println("INF: add data");
	coap.coap_add_data(pdu, payload.length(), payload);

	System.out.println("INF: created pdu");
	return pdu;
    }


    public void run() {
	System.out.println("INF: run()");

	// create coap_context
	System.out.println("INF: create context");
	coap_context_t ctx = coap.coap_new_context(77777);
	if (ctx == null) {
	    System.err.println("Could not create context");
	    return;
	}

	// register ourselves for message handling
	System.out.println("INF: register message handler");
	coap.register_message_handler(ctx, this);

	coap_pdu_t pdu = coap_new_request(coapConstants.COAP_REQUEST_GET,
					  "tst");
	if (pdu == null) {
	    System.err.println("Could not create pdu");
	    return;
	}

	// set destination
	sockaddr_in6 dst;
	dst = coap.sockaddr_in6_create(coapConstants.AF_INET6,
				       coapConstants.COAP_DEFAULT_PORT,
				       "::1");

	// TODO: JAVAfy sockaddr_in6 creation?
	// sockaddr_in6 is already wrapped
	// still problems with sin6_addr
	/*
	dst = new sockaddr_in6();
	if (dst == null) {
	    System.out.println("Could not create dst");
	    return;
	}
	dst.setSin6_family(coapConstants.AF_INET6);
	dst.setSin6_port(coap.htons(coapConstants.COAP_DEFAULT_PORT));
	//dst.setSin6_addr(addr);
	*/

	// send pdu
	//System.out.println("INF: coap_send_confirmed()");
	coap.coap_send_confirmed(ctx, dst, pdu);
	//System.out.println("INF: ~coap_send_confirmed()");

	// receive and dispatch -> will trigger messageHandler() callback
	//System.out.println("INF: coap_read()");
	coap.coap_read(ctx);
	//System.out.println("INF: ~coap_read()");
	//System.out.println("INF: coap_dispatch()");
	coap.coap_dispatch(ctx);
	//System.out.println("INF: ~coap_dispatch()");

	// free data
	coap.sockaddr_in6_free(dst);
	//TODO: more to free?

	System.out.println("INF: ~run()");
    }

    public void messageHandler(coap_context_t ctx,
			       coap_listnode node,
			       String data) {
	System.out.println("INF: Java messageHandler()");

	//System.out.println("****** ctx " + ctx);
	//System.out.println("****** node " + node);
	//System.out.println("****** data " + data);
	//System.out.println("****** node.getPdu() " + node.getPdu());
	//System.out.println("****** node.getPdu().getHdr() " + node.getPdu().getHdr());
	System.out.println("****** pdu (" + node.getPdu().getLength() +  " bytes)"
			   + " v:"  + node.getPdu().getHdr().getVersion()
			   + " t:"  + node.getPdu().getHdr().getType()
			   + " oc:" + node.getPdu().getHdr().getOptcnt()
			   + " c:"  + node.getPdu().getHdr().getCode()
			   + " id:" + node.getPdu().getHdr().getId());

	//System.out.println("****** node.getPdu().getOptions() " + node.getPdu().getOptions());

	String pdudata = node.getPdu().getData();
	System.out.println("****** data:'" + pdudata + "'");

	//TODO: do more...

	System.out.println("INF: ~Java messageHandler()");
    }

    public static void main(String argv[]) throws Exception {
	System.out.println("INF: main()");
	Client c = new Client();
	c.run();
	System.out.println("INF: ~main()");
    }
}
