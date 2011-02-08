package de.tzi.coap;

class Constants
{
    // Prevents instanciation 
    private Constants() {}
	public final static int COAP_DEFAULT_RESPONSE_TIMEOUT = 1; /* response timeout in seconds */
	public final static int COAP_DEFAULT_MAX_RETRANSMIT   = 5; /* max number of retransmissions */
	public final static int COAP_DEFAULT_PORT           = 61616; /* CoAP default UDP port */
	public final static int COAP_DEFAULT_MAX_AGE        =  60; /* default maximum object lifetime in seconds */
	public final static int COAP_MAX_PDU_SIZE           = 700; /* maximum size of a CoAP PDU */
	
	public final static int COAP_DEFAULT_VERSION       =    1; /* version of CoAP supported */
	public final static String COAP_DEFAULT_SCHEME        = "coap"; /* the default scheme for CoAP URIs */
	public final static String COAP_DEFAULT_URI_WELLKNOWN = ".well-known/core"; /* well-known resources URI */
	
	/* CoAP message types */
	
	public final static int COAP_MESSAGE_CON             =  0; /* confirmable message (requires ACK/RST) */
	public final static int COAP_MESSAGE_NON             =  1; /* non-confirmable message (one-shot message) */
	public final static int COAP_MESSAGE_ACK             =  2; /* used to acknowledge confirmable messages */
	public final static int COAP_MESSAGE_RST             =  3; /* indicates error in received messages */
	
	/* CoAP request methods */
	
	public final static int COAP_REQUEST_GET     =  1;
	public final static int COAP_REQUEST_POST    =  2;
	public final static int COAP_REQUEST_PUT     =  3;
	public final static int COAP_REQUEST_DELETE  =  4;
	
	/* CoAP option types (be sure to update check_critical when adding options */
	
	public final static int COAP_OPTION_CONTENT_TYPE = 1; /* C, 8-bit uint, 1 B, 0 (text/plain) */
	public final static int COAP_OPTION_MAXAGE       = 2; /* E, variable length, 1--4 B, 60 Seconds */
	public final static int COAP_OPTION_URI_SCHEME   = 3; /* C, String, 1-270 B, "coap" */
	public final static int COAP_OPTION_ETAG         = 4; /* E, sequence of bytes, 1-4 B, - */
	public final static int COAP_OPTION_URI_AUTHORITY= 5; /* C, String, 1-270 B, "" */
	public final static int COAP_OPTION_LOCATION     = 6; /* E, String, 1-270 B, - */
	public final static int COAP_OPTION_URI_PATH     =  9; /* C, String, 1-270 B, "" */
	public final static int COAP_OPTION_TOKEN        = 11; /* C, Sequence of Bytes, 1-2 B, - */
	public final static int COAP_OPTION_URI_QUERY    = 15; /* C, String, 1-270 B, "" */
	
	/* option types from draft-hartke-coap-observe-01 */
	
	public final static int COAP_OPTION_SUBSCRIPTION = 10; /* E, Duration, 1 B, 0 */
	
	/* selected option types from draft-bormann-coap-misc-04 */
	
	public final static int COAP_OPTION_ACCEPT    =    8; /* E  Sequence of Bytes, 1-n B, - */
	public final static int COAP_OPTION_BLOCK     =   13; /* C, unsigned integer, 1--3 B, 0 */
	public final static int COAP_OPTION_NOOP      =   14; /* no-op for fenceposting */
	
	/* CoAP result codes (HTTP-Code / 100 * 40 + HTTP-Code % 100) */
	
	public final static int COAP_RESPONSE_100    =   40;   /* 100 Continue */
	public final static int COAP_RESPONSE_200    =   80;   /* 200 OK */
	public final static int COAP_RESPONSE_201    =   81;   /* 201 Created */
	public final static int COAP_RESPONSE_304    =  124;   /* 304 Not Modified */
	public final static int COAP_RESPONSE_400    =  160;   /* 400 Bad Request */
	public final static int COAP_RESPONSE_404    =  164;   /* 404 Not Found */
	public final static int COAP_RESPONSE_405    =  165;   /* 405 Method Not Allowed */
	public final static int COAP_RESPONSE_415    =  175;   /* 415 Unsupported Media Type */
	public final static int COAP_RESPONSE_500    =  200;   /* 500 Internal Server Error */
	public final static int COAP_RESPONSE_503    =  203;   /* 503 Service Unavailable */
	public final static int COAP_RESPONSE_504    =  204;   /* 504 Gateway Timeout */
	public final static int COAP_RESPONSE_X_240  =  240;   /* Token Option required by server */
	public final static int COAP_RESPONSE_X_241  =  241;   /* Uri-Authority Option required by server */
	public final static int COAP_RESPONSE_X_242  =  242;   /* Critical Option not supported */
    
}
