package de.tzi.coap;

import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.net;
import de.tzi.coap.jni.pdu;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;

// SWIG wrapper
import de.tzi.coap.jni.CoapSwig;

public class Client {
    static {
	System.loadLibrary("coap");			// coap lib
    }

	//private static int port 		= COAP_DEFAULT_PORT;
	public static de.tzi.coap.jni.coap_context_t ctx;
	public static de.tzi.coap.jni.coap_queue_t 	node;
	public static de.tzi.coap.jni.coap_pdu_t 	PDU;
	public static de.tzi.coap.jni.coap_hdr_t 	HDR;
	public static int		id;
	public static String data;

	public static void main(String argv[]) throws Exception {    	
		int port = COAP_DEFAULT_PORT;
		int len;
    	//SWIG access
		ctx  = net.coap_new_context(port);
		node = net.coap_new_node();

		HDR = new coap_hdr_t();
 	 	HDR.setType(COAP_MESSAGE_CON);
  		HDR.setCode(COAP_REQUEST_POST);
  		HDR.setId(11);
		//System.out.println("HDR.Type in Java = "+HDR.getType());		
		//System.out.println("HDR.Code in Java = "+HDR.getCode());		
		//System.out.println("HDR.Id in Java = "+HDR.getId());		

		PDU  = pdu.coap_new_pdu();
		PDU.setHdr(HDR);
		data = "SONVOQUE";
		len = data.length();
		pdu.coap_add_data(PDU,len, data);
		port = 616161;
		//System.out.println("PORT  = "+port);
    	//CoapSwig.JNIRegisterMessageHandler(data);		
		//pdu.coap_send( CTX, &dst, PDU);
    	CoapSwig.JNIPDUCoapSend(ctx,port,PDU);		
		
		System.out.println("CTX in Java  = "+ctx);
		System.out.println("NODE in Java = "+node);		
		System.out.println("HDR in Java = "+HDR);		
		System.out.println("PDU in Java = "+ PDU);		
    }   
}