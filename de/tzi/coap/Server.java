package de.tzi.coap;

//import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.net;
import de.tzi.coap.jni.pdu;
import de.tzi.coap.jni.constant;
import de.tzi.coap.jni.socket;
import de.tzi.coap.jni.server;
import java.util.Random;


public class Server {
    static {
	System.loadLibrary("coap");			// coap lib
    }

	public static de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx;
	public static de.tzi.coap.jni.SWIGTYPE_p_coap_queue_t node;
	public static de.tzi.coap.jni.SWIGTYPE_p_coap_pdu_t pdu;
	
	public static de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6 dst;
	public static int version, type, option_cnt, code, id, opt_key, opt_length; 
	public static int socket_family, socket_port;
	public static String socket_addr, opt_data, data;
	
	public static void HandlePutProc() {
		de.tzi.coap.jni.SWIGTYPE_p_coap_pdu_t pdu;
		//create PDU
		
		version		= constant.COAP_DEFAULT_VERSION;
		type 		= constant.COAP_MESSAGE_ACK;
		option_cnt 	= 0;
		code 	= constant.COAP_RESPONSE_400;
		pdu = socket.socket6_create_pdu( version, type, option_cnt, code ,id);
		System.out.println("JAVA server send response to PUT req");		
		server.send_response(ctx, pdu);
	}

	public static void MessageHandler() {		
		int i;
		System.out.println("Message Handler in Server JAVA");

		type 		= server.get_hdr_type();
		code 		= server.get_hdr_code();
		option_cnt 	= server.get_hdr_optcnt();
		id 			= server.get_hdr_id();
		data		= server.get_pdu_data();
		
		System.out.println("Receive PDU");		
		System.out.println("- id		= "+id);		
		System.out.println("- type		= "+type);		
		System.out.println("- optcnt	= "+option_cnt);		
		for (i=1;i<=option_cnt;i++) {
			opt_data	= server.get_opt_data(i);
			System.out.println("- opt["+i+"]"+" 	= "+opt_data);				
		}	
		System.out.println("- data 		= "+data);		
		switch (code) {
			case constant.COAP_REQUEST_POST:
				System.out.println("- POST request: code = "+code);		
				break;
			case constant.COAP_REQUEST_PUT:
				System.out.println("- PUT request: code = "+code);	
				HandlePutProc();	
				break;
			case constant.COAP_REQUEST_GET:
				System.out.println("- GET request: code = "+code);		
				break;
		}						
		
	}	

	public static void main(String argv[]) throws Exception {    	
		int i;
		String stData;		
  		Random generator = new Random();

		ctx = net.coap_new_context(constant.COAP_DEFAULT_PORT);
		node = net.coap_new_node();
		
		socket_family = 10;
		socket_port = 616161;
		socket_addr = "::1";
		dst = server.s_socket6_create(socket_family, socket_port, socket_addr);
					
		i = server.start(dst);
		server.s_socket6_free(dst);

		System.out.println("EXIT in JAVA");
     }   
}
