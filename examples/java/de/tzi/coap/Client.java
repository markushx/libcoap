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
//import de.tzi.coap.jni.sockaddr_in6;
//import de.tzi.coap.jni.sockaddr_in6;
//import de.tzi.coap.jni.in6_addr;
//import de.tzi.coap.jni.in6_addr___in6_u;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import de.tzi.coap.jni.coap_queue_t;
//import de.tzi.coap.jni.addrinfo;
import de.tzi.coap.jni.coap_log_t;
//import de.tzi.coap.jni.coap_tid_t;
import de.tzi.coap.jni.__coap_address_t;
import de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6;

import java.util.Random;
import java.util.Vector;


public class Client extends CoapBase {

    int mainLoopSleepTimeMilli = 50;

    coap_context_t ctx;
    coap_queue_t nextpdu;

    static Random generator = new Random();

    public Client() {
    }

    int COAP_RESPONSE_CLASS(int C) {
	return ((C>>5) & 0xFF);
    }

    coap_pdu_t new_ack( coap_context_t ctx, coap_queue_t node ) {
	coap_pdu_t pdu = coap.coap_new_pdu();

	if (pdu != null) {
	    pdu.getHdr().setType(coapConstants.COAP_MESSAGE_ACK);
	    pdu.getHdr().setCode(0);
	    pdu.getHdr().setId( node.getPdu().getHdr().getId());
	}

	return pdu;
    }

    coap_pdu_t new_response( coap_context_t ctx, coap_queue_t node, int code ) {
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
	boolean quit = false;

	System.out.println("INF: run()");

	coap.coap_set_log_level(coap_log_t.LOG_DEBUG);

	// create coap_context
	System.err.println("INF: create context");
	ctx = coap.get_context("::", ""+0/*coapConstants.COAP_DEFAULT_PORT*/);
	if (ctx == null) {
	    System.out.println("Could not create context");
	    return;
	}

	// register ourselves for message handling
	System.out.println("INF: register message handler");
	coap.register_response_handler(ctx, this);

	coap_pdu_t pdu = coap_new_request(method,
					  optlist,
					  payload);

	if (pdu == null) {
	    System.err.println("Could not create pdu");
	    return;
	}

	// set destination
	__coap_address_t dst = new __coap_address_t();

	int res = coap.resolve_address(coap.create_str(destination,
						       destination.length()),
				       dst.getAddr().getSa());
	// int res = coap.resolve_address(coap.create_str(destination,
	// 					       0),
	// 			       dst.getAddr().getSa());
	if (res < 0) {
	    System.err.println("Could not resolve address.");
	    return;
	}
	dst.setSize(res);
	//dst.getAddr().getSin().setSin_port(coap.htons(port));
	//dst.getAddr().getSin().setSin_port(port); //FIXME: add htons() for port

	SWIGTYPE_p_sockaddr_in6 dstsin6;
	dstsin6 = coap.sockaddr_in6_create(coapConstants.AF_INET6,
					   port,
					   destination);

	dst.getAddr().setSin6(dstsin6);

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
	// coap.check_receive_client(ctx); //TODO: reenable

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

	// free data
	//coap.sockaddr_in6_free(dst);
	coap.coap_free_context(ctx);

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

    /*
    int	check_token(coap_pdu_t received) {
	coap_opt_iterator_t opt_iter;
	coap_list_t option;
	str token1 = { 0, NULL }, token2 = { 0, NULL };

	if (coap_check_option(received, COAP_OPTION_TOKEN, &opt_iter)) {
	    token1.s = COAP_OPT_VALUE(opt_iter.option);
	    token1.length = COAP_OPT_LENGTH(opt_iter.option);
	}

	for (option = optlist; option; option = option->next) {
	    if (COAP_OPTION_KEY(*(coap_option *)option->data) == COAP_OPTION_TOKEN) {
		token2.s = COAP_OPTION_DATA(*(coap_option *)option->data);
		token2.length = COAP_OPTION_LENGTH(*(coap_option *)option->data);
		break;
	    }
	}

	return token1.length == token2.length &&
	    memcmp(token1.s, token2.s, token1.length) == 0;
    }
    */

    public void responseHandler(coap_context_t ctx,
				__coap_address_t remote,
				coap_pdu_t sent,
				coap_pdu_t received,
				int id) {

	coap_pdu_t pdu = null;

	short[] databuf;

	System.out.println("INF: Java Client responseHandler()");

	System.out.println("****** pdu (" + received.getLength() +  " bytes)"
			   + " v:"  + received.getHdr().getVersion()
			   + " t:"  + received.getHdr().getType()
			   + " oc:" + received.getHdr().getOptcnt()
			   + " c:"  + received.getHdr().getCode()
			   + " id:" + received.getHdr().getId());

	System.out.println("from: " + coap.get_addr(remote.getAddr().getSin6()));

	//TODO: str handling for token
	// if (!coap.check_token(received)) {
	//     /* drop if this was just some message, or send RST in case of notification */
	//     if ((sent != null) && (received.getHdr().getType() == coapConstants.COAP_MESSAGE_CON ||
	// 		  received.getHdr().getType() == coapConstants.COAP_MESSAGE_NON))
	// 	coap.coap_send_rst(ctx, remote, received);
	//     return;
	// }

	if (received.getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
	    /* acknowledge received response if confirmable (TODO: check Token) */
	    coap.coap_send_ack(ctx, remote, received);
	} else if (received.getHdr().getType() == coapConstants.COAP_MESSAGE_RST) {
	    System.out.println("INF: got RST\n");
	    return;
	} else {
	}

	/* output the received data, if any */
	if (received.getHdr().getCode() == coapConstants.COAP_RESPONSE_205) {

	    //TODO: implement observe in Java Client
	    /* set obs timer if we have successfully subscribed a resource */
	    // if ((sent != null) &&
	    //  coap.coap_check_option(received,
	    //	       coapConstants.COAP_OPTION_SUBSCRIPTION,
	    //	       opt_iter)) {
	    //  System.out.println("DBG: observation relationship established, set timeout to " + obs_seconds);
	    //	set_timeout(&obs_wait, obs_seconds);
	    // }

	    /* Got some data, check if block option is set. Behavior is undefined if
	     * both, Block1 and Block2 are present. */
	    // block_opt = get_block(received, &opt_iter);
	    // if (!block_opt) {

	    /* There is no block option set, just read the data and we are done. */
	    databuf = new short[received.getLength()];
	    int len = coap.coap_get_data_java(received, databuf);
	    if (len > 0) {
		//append_to_output(databuf);
	    }
	    outputData(databuf);

	    //TODO: implement block in Java Client
	    // } else {
	    //   unsigned short blktype = opt_iter.type;

	    //   /* TODO: check if we are looking at the correct block number */
	    //   if (coap_get_data(received, &len, &databuf))
	    // 	append_to_output(databuf, len);

	    //   if (COAP_OPT_BLOCK_MORE(block_opt)) {
	    // 	/* more bit is set */
	    // 	debug("found the M bit, block size is %u, block nr. %u\n",
	    // 	      COAP_OPT_BLOCK_SZX(block_opt), COAP_OPT_BLOCK_NUM(block_opt));

	    // 	/* create pdu with request for next block */
	    // 	pdu = coap_new_request(ctx, method, NULL); /* first, create bare PDU w/o any option  */
	    // 	if ( pdu ) {
	    // 	  /* add URI components from optlist */
	    // 	  for (option = optlist; option; option = option->next ) {
	    // 	    switch (COAP_OPTION_KEY(*(coap_option *)option->data)) {
	    // 	    case COAP_OPTION_URI_HOST :
	    // 	    case COAP_OPTION_URI_PORT :
	    // 	    case COAP_OPTION_URI_PATH :
	    // 	    case COAP_OPTION_TOKEN :
	    // 	    case COAP_OPTION_URI_QUERY :
	    // 	      coap_add_option ( pdu, COAP_OPTION_KEY(*(coap_option *)option->data),
	    // 				COAP_OPTION_LENGTH(*(coap_option *)option->data),
	    // 				COAP_OPTION_DATA(*(coap_option *)option->data) );
	    // 	      break;
	    // 	    default:
	    // 	      ;			/* skip other options */
	    // 	    }
	    // 	  }

	    // 	  /* finally add updated block option from response, clear M bit */
	    // 	  /* blocknr = (blocknr & 0xfffffff7) + 0x10; */
	    // 	  debug("query block %d\n", (COAP_OPT_BLOCK_NUM(block_opt) + 1));
	    // 	  coap_add_option(pdu, blktype, coap_encode_var_bytes(buf,
	    // 	      ((COAP_OPT_BLOCK_NUM(block_opt) + 1) << 4) |
	    //           COAP_OPT_BLOCK_SZX(block_opt)), buf);

	    // 	  if (received->hdr->type == COAP_MESSAGE_CON)
	    // 	    tid = coap_send_confirmed(ctx, remote, pdu);
	    // 	  else
	    // 	    tid = coap_send(ctx, remote, pdu);

	    // 	  if (tid == COAP_INVALID_TID) {
	    // 	    debug("message_handler: error sending new request");
	    //         coap_delete_pdu(pdu);
	    // 	  } else {
	    // 	    set_timeout(&max_wait, wait_seconds);
	    //         if (received->hdr->type != COAP_MESSAGE_CON)
	    //           coap_delete_pdu(pdu);
	    //       }

	    // 	  return;
	    // 	}
	    //   }
	    // }
	} else {			/* no 2.05 */
	    /* check if an error was signaled and output payload if so */
	    if (COAP_RESPONSE_CLASS(received.getHdr().getCode()) >= 4) {
		System.err.println("INF: " + (received.getHdr().getCode() >> 5)
				   + "." + (received.getHdr().getCode() & 0x1F));

		databuf = new short[received.getLength()];
		int len = coap.coap_get_data_java(received, databuf);
		outputData(databuf);
	    }
	}

	/* finally send new request, if needed */
	if ((pdu != null) &&
	    coap.coap_send(ctx, remote, pdu) == coapConstants.COAP_INVALID_TID) {
	    System.out.println("DBG: response_handler: error sending response");
	}
	coap.coap_delete_pdu(pdu);

	/* our job is done, we can exit at any time */
	//ready = coap_check_option(received, COAP_OPTION_SUBSCRIPTION, &opt_iter) == NULL;

	System.out.println("INF: ~Java messageHandler()");
    }

    public void outputData(short[] databuf) {
	for (int i = 0; i < databuf.length; i++) {
	    System.out.print(""+(char)databuf[i]);
	}
	System.out.println("");
    }

    public static void main(String argv[]) throws Exception {
	System.out.println("INF: main()");

	//set arguments
	String destination = "::1";
	int port = coapConstants.COAP_DEFAULT_PORT;
	int method = coapConstants.COAP_REQUEST_GET;
	int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
	String uri = "";
	//String uri = ".well-known/core";
	String token = "3a";
	String payload = "Hello CoAP";

	Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();
	CoapJavaOption contentTypeOption = new CoapJavaOption(coapConstants.COAP_OPTION_CONTENT_TYPE,
							      ""+(char)content_type, 1);
	optionList.add(contentTypeOption);
	CoapJavaOption uriOption = new CoapJavaOption(coap.COAP_OPTION_URI_PATH,
						      uri, uri.length());
	optionList.add(uriOption);
	CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN,
							token, token.length());
	optionList.add(tokenOption);
	if ( method == coapConstants.COAP_REQUEST_GET ||
	     method == coapConstants.COAP_REQUEST_DELETE) {
	    payload = null;
	}

	Client c = new Client();
	c.run(destination, port, method, optionList, payload);
	System.out.println("INF: ~main()");
    }
}
