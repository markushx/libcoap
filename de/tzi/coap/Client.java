package de.tzi.coap;

//import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.net;
import de.tzi.coap.jni.pdu;
import de.tzi.coap.jni.constant;
import de.tzi.coap.jni.socket;
import java.util.Random;


public class Client {
    static {
	System.loadLibrary("coap");			// coap lib
    }

	public static de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx;
	public static de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6 dst;
	public static int version, type, option_cnt, hdr_code, id; 
	public static int socket_family, socket_port;
	public static String socket_addr;

	public static void main(String argv[]) throws Exception {    	
		int i;
		String stData;		
  		Random generator = new Random();

		socket_family = 10;
		socket_port = 616161;
		socket_addr = "::1";
		dst = socket.socket6_create(socket_family, socket_port, socket_addr);

		ctx = net.coap_new_context(constant.COAP_DEFAULT_PORT);
		
		for (i=0;i<5;i++) {
			version		= constant.COAP_DEFAULT_VERSION;
			type 		= constant.COAP_MESSAGE_CON;
			option_cnt 	= 0;
			hdr_code 	= constant.COAP_REQUEST_POST;
			id 			= generator.nextInt(0xFFFF);
			stData 		= "COMNETS";
			socket.socket6_send(ctx, dst, version, type, 0, hdr_code ,id, stData);
			socket.socket6_receive(ctx);
			Thread.sleep(1000);
		}
		socket.socket6_free(dst);
		System.out.println("EXIT in JAVA");
     }   
}
