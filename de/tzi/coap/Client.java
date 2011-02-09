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
	public static int hdr_ver, hdr_type,hdr_opt_cnt, hdr_code, hdr_id;
	public static String data;
	public static de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6 dst;
	
	public static void Return_PDU(int ver, int type, int opt_cnt, int code, int id, int data_len) {
		System.out.println("RETURN IN JAVA {");						
		System.out.println("	HDR.VERION = "+ver);				
		System.out.println("	HDR.TYPE   = "+type);				
		System.out.println("	HDR.OPTCNT = "+opt_cnt);		
		if ((code/100*40+code%100) == COAP_RESPONSE_200)
			System.out.println("	HDR.CODE   = "+code+" --> ACK");				
		else
			System.out.println("	HDR.CODE   = "+code+" --> UNKNOWN");				
		System.out.println("	HDR.ID     = "+id);				
		System.out.println("	PDU.LENGTH = "+data_len + " (byte)");				
		System.out.println("}");						
	}	

	public static void main(String argv[]) throws Exception {    	
		//int port = COAP_DEFAULT_PORT;
		int len;
		
		ctx  = net.coap_new_context(port);
		node = net.coap_new_node();
  		Random generator = new Random();

		for (int i=0; i<10;) {
			HDR = new coap_hdr_t();
			PDU  = pdu.coap_new_pdu();

    		//SWIG access
 	 		HDR.setType(COAP_MESSAGE_CON);
  			HDR.setCode(COAP_REQUEST_POST);
  			HDR.setId(generator.nextInt(0xFFFF));
			PDU.setHdr(HDR);
			data = "DATA_STRING";
			len = PDU.getLength();
			pdu.coap_add_data(PDU,len, data);
			len = PDU.getLength();
		
			//send PDU using licoap-SWIG wrapper
			net.coap_send(ctx, dst, PDU);// how can access dst structure????, nothing happens when executing this line
    	
    		//CoapSwig.JNIRegisterMessageHandler(data);	
    	
    		//this implementation seems better	
    		hdr_ver = COAP_DEFAULT_VERSION;
    		hdr_type = COAP_MESSAGE_CON;
    		hdr_opt_cnt = (int)HDR.getOptcnt();
    		hdr_code = (int)HDR.getCode();
    		hdr_id = (int)HDR.getId();
			System.out.println("IN JAVA SEND DATA PACKET "+i+ " {");						
			System.out.println("	HDR.Type = "+hdr_type);		
			System.out.println("	HDR.Code = "+hdr_code);		
			System.out.println("	HDR.Id   = "+hdr_id);		
			System.out.println("	PDU.Data = "+data);		
			System.out.println("	PDU.Len = "+len);		
			//send PDU using JNI access with libcoap
			System.out.println("}");						
    		CoapSwig.JNIPDUCoapSend(hdr_ver, hdr_type, hdr_opt_cnt, hdr_code, hdr_id ,port,data);		
			Thread.sleep(1000);		
		}	
    }   
}