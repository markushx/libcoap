package de.tzi.coap;

import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.encode;
import de.tzi.coap.jni.net;

// SWIG wrapper
import de.tzi.coap.jni.CoapSwig;

public class Coap {
    static {
	System.loadLibrary("coap");			// coap lib
    }

	public static int port 		= COAP_DEFAULT_PORT;
	public static int localport = COAP_DEFAULT_PORT; 
	public static de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx;
	public static de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t 	node;
	public static String sdata = "TEST_STRING_IN_JAVA";

	// Natives
	//public native void JNIRegisterMessageHandler();
	    
    public static void message_handler(	de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx_obj,
				       					de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t node_obj,
				       					String stData  ) {
		System.out.println("Enter Java message_handler()");
		System.out.println("CTX  = " + ctx_obj);
		System.out.println("NODE = " + node_obj);
		System.out.println("DATA = " + stData);
		System.out.println("Exit Java message_handler()");
    }

	public static void main(String argv[]) throws Exception {
    	
    	//SWIG access
		ctx  = net.coap_new_context(port);
		node = net.coap_new_node();
    	CoapSwig.JNIRegisterMessageHandler(sdata);		

		//JNI access
    	//Coap c = new Coap();
    	//c.JNIRegisterMessageHandler();
	
		System.out.println("CTX in Java  = "+ctx);
		System.out.println("NODE in Java = "+node);
		System.out.println("Exit Java");
    }   
}