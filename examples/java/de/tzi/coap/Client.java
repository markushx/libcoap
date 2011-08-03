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
import java.util.Vector;


public class Client extends CoapBase {

    static Random generator = new Random();

    public Client() {
    }

    coap_pdu_t new_ack( coap_context_t  ctx, coap_listnode node ) {
	coap_pdu_t pdu = coap.coap_new_pdu();

	if (pdu != null) {
	    pdu.getHdr().setType(coapConstants.COAP_MESSAGE_ACK);
	    pdu.getHdr().setCode(0);
	    pdu.getHdr().setId( node.getPdu().getHdr().getId());
	}

	return pdu;
    }

    coap_pdu_t new_response( coap_context_t  ctx, coap_listnode node, int code ) {
	coap_pdu_t pdu = new_ack(ctx, node);

	if (pdu != null)
	    pdu.getHdr().setCode(code);

	return pdu;
    }

    coap_pdu_t coap_new_request(int methodid, Vector<CoapJavaOption> optlist, String payload) {

	coap_pdu_t pdu = coap.coap_new_pdu();
	if (pdu == null) {
	    System.out.println("INF: could not create pdu");
	    return pdu;
	}

	System.out.println("INF: set header values");
	pdu.getHdr().setVersion(coapConstants.COAP_DEFAULT_VERSION);
	pdu.getHdr().setType(coapConstants.COAP_MESSAGE_CON);
	pdu.getHdr().setCode(methodid);
	pdu.getHdr().setId(generator.nextInt(0xFFFF));

	for (int i=0; i<optlist.size(); i++) {
	    coap.coap_add_option(pdu, optlist.get(i).getType(), optlist.get(i).getLength(), optlist.get(i).getValue());
	}

	if (payload != null) {
	    coap.coap_add_data(pdu, payload.length(), payload);
	}

	System.out.println("INF: created pdu");
	return pdu;
    }


    public void run(String destination, int port, int method, Vector<CoapJavaOption> optlist, String payload) {
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

	coap_pdu_t pdu = coap_new_request(method,
					  optlist,
					  payload);

	if (pdu == null) {
	    System.err.println("Could not create pdu");
	    return;
	}

	// set destination
	sockaddr_in6 dst;
	dst = coap.sockaddr_in6_create(coapConstants.AF_INET6,
				       port,
				       destination);

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
	coap.coap_send_confirmed(ctx, dst, pdu);
	// handle retransmission, receive and dispatch
	// -> will trigger messageHandler() callback
	coap.check_receive_client(ctx);

	// free data
	coap.sockaddr_in6_free(dst);
	coap.coap_free_context(ctx);

	System.out.println("INF: ~run()");
    }

    public void messageHandler(coap_context_t ctx,
			       coap_listnode node,
			       String data) {
	coap_pdu_t pdu = null;
	System.out.println("INF: Java Client messageHandler()");

	System.out.println("****** pdu (" + node.getPdu().getLength() +  " bytes)"
			   + " v:"  + node.getPdu().getHdr().getVersion()
			   + " t:"  + node.getPdu().getHdr().getType()
			   + " oc:" + node.getPdu().getHdr().getOptcnt()
			   + " c:"  + node.getPdu().getHdr().getCode()
			   + " id:" + node.getPdu().getHdr().getId());

	if ( node.getPdu().getHdr().getVersion() != coapConstants.COAP_DEFAULT_VERSION ) {
	    System.out.printf("dropped packet with unknown version %u\n", node.getPdu().getHdr().getVersion());
	    return;
	}

	/* send 500 response */
	if ( node.getPdu().getHdr().getCode() < coapConstants.COAP_RESPONSE_100 && node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON ) {
	    pdu = new_response( ctx, node, coapConstants.COAP_RESPONSE_500 );
	    finish(ctx, node, pdu);
	    return;
	}

	if (node.getPdu().getHdr().getCode() == coapConstants.COAP_RESPONSE_200) {
	    String pdudata = node.getPdu().getData();
	    System.out.println("****** data:'" + pdudata + "'");
	}

	/* acknowledge if requested */
	if ( pdu != null && node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON ) {
	    pdu = new_ack( ctx, node );
	}

	finish(ctx, node, pdu);
	System.out.println("INF: ~Java messageHandler()");
    }

    public void finish(coap_context_t ctx, coap_listnode node, coap_pdu_t pdu){
	if ( (pdu != null) && (coap.coap_send( ctx, node.getRemote(), pdu ) == coapConstants.COAP_INVALID_TID )) {
	    System.out.println("message_handler: error sending reponse");
	    coap.coap_delete_pdu(pdu);
	}
    }

    public static void main(String argv[]) throws Exception {
	System.out.println("INF: main()");

	//set argruments
	String destination = "::1";
	int port = coapConstants.COAP_DEFAULT_PORT;
	int method = coapConstants.COAP_REQUEST_GET;
	int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
	String uri = ".well-known/core";
	String token = "3a";
	String payload = "Hello CoAP";

	Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();
	CoapJavaOption contentTypeOption = new CoapJavaOption(coapConstants.COAP_OPTION_CONTENT_TYPE, ""+(char)content_type, 1);
	optionList.add(contentTypeOption);
	CoapJavaOption uriOption = new CoapJavaOption(coap.COAP_OPTION_URI_PATH, uri, uri.length());
	optionList.add(uriOption);
	CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN, token, token.length());
	optionList.add(tokenOption);
	if ( method == coapConstants.COAP_REQUEST_GET || method == coapConstants.COAP_REQUEST_DELETE) {
	    payload = null;
	}

	Client c = new Client();
	c.run(destination, port, method, optionList, payload);
	System.out.println("INF: ~main()");
    }
}
