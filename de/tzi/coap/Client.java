package de.tzi.coap;

import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.net;
import de.tzi.coap.jni.pdu;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.coap_hdr_t;
import de.tzi.coap.jni.sockaddr6_t;
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
	public static int hdr_ver, hdr_type,hdr_opt_cnt, hdr_code, hdr_id;
	public static String data;
	public static de.tzi.coap.jni.sockaddr6_t dst;
	
	public static void Return_PDU(int ver, int type, int opt_cnt, int code, int id, int data_len) {
		System.out.println("RETURN IN JAVA {");						
	}	


	public static void main(String argv[]) throws Exception {    	
		//int port = COAP_DEFAULT_PORT;
		int len;
		String st="NULL";
		
		ctx  = net.coap_new_context(port);
		node = net.coap_new_node();
		dst = new sockaddr6_t();

  		Random generator = new Random();

		for (int i=0; i<5;i++) {
			HDR = new coap_hdr_t();
			PDU  = pdu.coap_new_pdu();
			
    		//SWIG access
 	 		HDR.setVersion(COAP_DEFAULT_VERSION);
 	 		HDR.setType(COAP_MESSAGE_CON);
  			HDR.setCode(COAP_REQUEST_POST);
  			HDR.setId(generator.nextInt(0xFFFF));
			PDU.setHdr(HDR);
			data = "ComNets";  /*max=12-4*/
			len = data.length();
			pdu.coap_add_data(PDU, len, data);
			len = PDU.getLength();
		
			//send PDU using licoap-SWIG wrapper
			dst.setSin6_family(10);//AF_INET6
			dst.setSin6_port(616161);//port
			net.coap_send_u(ctx, dst, PDU);
    	    	
			Thread.sleep(1000);		
		}	
    }   
}