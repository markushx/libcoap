package de.tzi.coap;

import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.encode;
import de.tzi.coap.jni.net;
//import de.tzi.coap.jni.pdu;

public class CoapTest {
    static {
	System.loadLibrary("coap");
    }

    public static void message_handler(de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx,
				       de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t node,
				       String data  ) {
    }

    public static void main(String argv[]) {
	de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx;
	int port = COAP_DEFAULT_PORT, localport = COAP_DEFAULT_PORT; 
      
	de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t ct;
    
	// de.tzi.coap.jni.SWIGTYPE_p_coap_message_handler_t handler;
    
	// int port = COAP_DEFAULT_PORT;
	// TODO: handle argv
      
	ctx = net.coap_new_context(port);
	//net.coap_register_message_handler(ctx, message_handler );

	System.out.println(ctx);
	System.out.println("Hallo Welt");
    }
}
