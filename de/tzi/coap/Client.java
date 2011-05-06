package de.tzi.coap;

//import static de.tzi.coap.Constants.*;
import de.tzi.coap.jni.net;
import de.tzi.coap.jni.pdu;
import de.tzi.coap.jni.constant;
import de.tzi.coap.jni.socket;
import java.util.Random;

public class Client {
    static {
	System.loadLibrary("coap"); // coap lib
    }

    public static de.tzi.coap.jni.SWIGTYPE_p_coap_context_t ctx;
    public static de.tzi.coap.jni.SWIGTYPE_p_coap_pdu_t pdu;
    public static de.tzi.coap.jni.SWIGTYPE_p_sockaddr_in6 dst;
    public static int version, type, option_cnt, hdr_code, id, opt_key, opt_length;
    public static int socket_family;
    public static double socket_port;
    public static String socket_addr, opt_data;

    public static void main(String argv[]) throws Exception {
	int i;
	String stData;
	Random generator = new Random();

	//FIXME: use constant for socket_family
	socket_family = 10;
	socket_port = 616161;
	socket_addr = "::1";
	dst = socket.socket6_create(socket_family, socket_port, socket_addr);

	ctx = net.coap_new_context(constant.COAP_DEFAULT_PORT);
	System.out.println("CTX in JAVA = "+ctx);

	if (ctx == null) {
	    System.out.println("Could not create context");
	}

	//FIXME: no data for CON+GET
	stData = "35DEGREE";

	for (i=0;i<5;i++) {
	    //create PDU
	    version	= constant.COAP_DEFAULT_VERSION;
	    type        = constant.COAP_MESSAGE_CON;
	    option_cnt  = 0;
	    hdr_code    = constant.COAP_REQUEST_PUT;
	    id          = generator.nextInt(0xFFFF);
	    pdu = socket.socket6_create_pdu( version, type, 0, hdr_code ,id);

	    //create option
	    //FIXME: use constants from lib for option key
	    opt_key = 1;
	    opt_length = 6;
	    //FIXME: use senseful option data
	    opt_data = "OPTION";
	    socket.socket6_add_option(pdu, opt_key, opt_length, opt_data);

	    //FIXME: use constants from lib for option key
	    opt_key = 2;
	    opt_length = 4;
	    //FIXME: use senseful option data
	    opt_data = "TIME";
	    socket.socket6_add_option(pdu, opt_key, opt_length, opt_data);

	    //send pdu
	    System.out.println("CTX in JAVA before socket6_send " + ctx);
	    socket.socket6_send(ctx, dst, pdu, stData);
	    System.out.println("CTX in JAVA after socket6_send " + ctx);

	    //receive
	    System.out.println("CTX in JAVA before socket6_receive " + ctx);
	    socket.socket6_receive(ctx);
	    System.out.println("CTX in JAVA after socket6_receive " + ctx);
	    Thread.sleep(1000);
	}

	socket.socket6_free(dst);
	System.out.println("EXIT in JAVA");
    }
}
