package de.tzi.coap;

import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.net;
import de.tzi.coap.jni.pdu;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import java.util.Random;

// SWIG wrapper
import de.tzi.coap.jni.CoapSwig;

public class Client {
    static {
	System.loadLibrary("coap");			// coap lib
    }

	private static int port 		= COAP_DEFAULT_PORT;
	public static de.tzi.coap.jni.coap_context_t ctx;
	public static de.tzi.coap.jni.coap_queue_t 	node;
	public static de.tzi.coap.jni.coap_pdu_t 	PDU;
	public static de.tzi.coap.jni.coap_hdr_t 	HDR;
	public static int hdr_type,hdr_code, hdr_id;
	public static String data;
	public static de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6 dst;

	public static void main(String argv[]) throws Exception {    	
		//int port = COAP_DEFAULT_PORT;
		int len;
		
		ctx  = net.coap_new_context(port);
		node = net.coap_new_node();
		HDR = new coap_hdr_t();
		PDU  = pdu.coap_new_pdu();
  		Random generator = new Random();

		for (int i=0; i<=10; i++) {
    		//SWIG access
 	 		HDR.setType(COAP_MESSAGE_CON);
  			HDR.setCode(COAP_REQUEST_POST);
  			HDR.setId(generator.nextInt(0xFFFF));
			PDU.setHdr(HDR);
			data = "SONVQ";
			len = data.length();
			pdu.coap_add_data(PDU,len, data);
		
			//send PDU using licoap SWIG wrapper
			net.coap_send(ctx, dst, PDU);// how can access dst structure????, nothing happens when executing this line
    	
    		//CoapSwig.JNIRegisterMessageHandler(data);	
    	
    		//this implementation seems better	
    		hdr_type = (int)HDR.getType();
    		hdr_code = (int)HDR.getCode();
    		hdr_id = (int)HDR.getId();
			//System.out.println("HDR.Type in Java = "+hdr_type);		
			//System.out.println("HDR.Code in Java = "+hdr_code);		
			//System.out.println("HDR.Id in Java = "+hdr_id);		
			//send PDU using JNI access with libcoap
    		CoapSwig.JNIPDUCoapSend(hdr_type, hdr_code, hdr_id ,port,data);		
		
			//System.out.println("CTX in Java  = "+ctx);
			//System.out.println("NODE in Java = "+node);		
			//System.out.println("HDR in Java = "+HDR);		
			//System.out.println("PDU in Java = "+ PDU);	
			Thread.sleep(1000);
		}	
    }   
}