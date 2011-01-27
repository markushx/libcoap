package de.tzi.coap;

import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.encode;
import de.tzi.coap.jni.net;
//import de.tzi.coap.jni.pdu;


public class CoapTest {
    static {
	System.loadLibrary("coap");			// coap lib
    }

	public static int port 		= COAP_DEFAULT_PORT;
	public static int localport = COAP_DEFAULT_PORT; 
	public static de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx;
	public static de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t 	node;
	public String data = "TEST_STRING_IN_JAVA";

	// Natives
	private native void JNIRegisterMessageHandler();
	
	/*
	public static void RegisterMessageHandler() {
    	CoapTest c = new CoapTest();
    	c.JNIRegisterMessageHandler();		
	}
    */
    public static void message_handler(	de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx_obj,
				       					de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t node_obj,
				       					String data  ) {
		System.out.println("Enter Java message_handler()");
		System.out.println("CTX  = " + ctx_obj);
		System.out.println("NODE = " + node_obj);
		System.out.println("DATA = " + data);
		System.out.println("Exit Java message_handler()");
    }


	public static void main(String argv[]) throws Exception {
		ctx = net.coap_new_context(port);
		node = net.coap_new_node();
		//net.coap_register_message_handler(ctx, message_handler);
		
		//JNI access
    	CoapTest c = new CoapTest();
    	c.JNIRegisterMessageHandler();
	
		System.out.println("CTX in Java  = "+ctx);
		System.out.println("NODE in Java = "+node);
		System.out.println("Exit Java");
    }
    
}	

