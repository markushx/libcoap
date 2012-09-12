/*
 * Server.java -- Sample JAVA/JNI/SWIG server for libcoap
 *
 * Copyright (C) 2011 Markus Becker <mab@comnets.uni-bremen.de>
 *
 */

package de.tzi.coap;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
//import de.tzi.coap.jni.sockaddr_in6;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import de.tzi.coap.jni.coap_resource_t;
import de.tzi.coap.jni.__coap_address_t;
import de.tzi.coap.jni.coap_queue_t;
import de.tzi.coap.jni.str;
import de.tzi.coap.jni.coap_log_t;

import java.util.Random;

public class Server extends CoapBase {

    int mainLoopSleepTimeMilli = 50;

    coap_context_t ctx;
    coap_queue_t nextpdu;

    String INDEX = "Hello, CoAP.";

    public Server() {

    }

    public void run() {
	boolean quit = false;

	System.out.println("INF: run()");

	coap.coap_set_log_level(coap_log_t.LOG_DEBUG);

	// create coap_context
	System.out.println("INF: create context on port " +
			   coapConstants.COAP_DEFAULT_PORT);
	ctx = coap.get_context("::", ""+coapConstants.COAP_DEFAULT_PORT);
	if (ctx == null) {
	    System.out.println("Could not create context");
	    return;
	}

	initResources(ctx);

	while (!quit) {
	    //System.out.print(".");

	    checkReceiveTraffic();
	    checkRetransmit();

	    try {
		Thread.sleep(mainLoopSleepTimeMilli);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	System.out.println("INF: ~run()");
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

    public void initResources(coap_context_t ctx) {
	coap_resource_t r;

	r = coap.coap_resource_init("", 0, 0);
	//r = coap.coap_resource_init("time", 4, 0);

	// we can only register one function as callback, otherwise
	// coap.i would need to be changed erverytime for new
	// resources
	coap.register_handler(r, (short)coapConstants.COAP_REQUEST_GET, this);

	coap.coap_add_attr(r, "ct", 2, "0", 1, 0);
	coap.coap_add_attr(r, "title", 5, "\"General Info\"", 14, 0);
	coap.coap_add_resource(ctx, r);
    }

    public void handler(coap_context_t  ctx, coap_resource_t resource,
			__coap_address_t peer, coap_pdu_t request, str token,
			coap_pdu_t response) {
	System.out.println("INF: Java handler");

	System.out.println("INF: URI '" + resource.getUri().getS()
			   + "' | " + resource.getUri().getLength());
	if ("".equals(resource.getUri().getS())) {
	    if (request.getHdr().getCode() == coapConstants.COAP_REQUEST_GET) {
		System.out.println("INF: GET /");
		response.getHdr().setCode(coapConstants.COAP_RESPONSE_205);
		System.out.println("INF: setCode Done");

		String buf = "";
		coap.coap_add_option(response, coapConstants.COAP_OPTION_CONTENT_TYPE,
				     coap.coap_encode_var_bytes(buf,
								coapConstants.COAP_MEDIATYPE_TEXT_PLAIN),
				     buf);

		coap.coap_add_option(response, coapConstants.COAP_OPTION_MAXAGE,
				     coap.coap_encode_var_bytes(buf, 0x2ffff),
				     buf);

		/*
		  accessing token creates a core dump
		System.out.println("INF: token " + token);
		if ((token != null) && (token.getLength() != 0))
		  coap.coap_add_option(response, coapConstants.COAP_OPTION_TOKEN,
		  (int)token.getLength(), token.getS());
		*/
		coap.coap_add_data(response, INDEX.length(), INDEX);
		System.out.println("INF: addData Done");
	    } else { // not GET
		response.getHdr().setCode(coapConstants.COAP_RESPONSE_405);
	    }
	}
    }

    /*
    public void messageHandler(coap_context_t ctx,
			       coap_address_t remote,
			       coap_pdu_t sent,
			       coap_pdu_t received,
			       coap_tid_t id) {

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

	System.out.println("from:" + coap.get_addr(node.getRemote()));

	String pdudata = node.getPdu().getData();
	System.out.println(pdudata);

	//TODO: do more...

	System.out.println("INF: ~messageHandler()");
    }
    */

    public static void main(String argv[]) throws Exception {
	System.out.println("INF: main()");
	Server s = new Server();
	s.run();
	System.out.println("INF: ~main()");
    }
}
