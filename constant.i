/* File : constant.i */
%module constant

/* Force the generated Java code to use the C constant values rather than making a JNI call */
%javaconst(1);


#define COAP_DEFAULT_RESPONSE_TIMEOUT  1
#define COAP_DEFAULT_MAX_RETRANSMIT    5 /* max number of retransmissions */
#define COAP_DEFAULT_PORT          61616 /* CoAP default UDP port */
#define COAP_DEFAULT_MAX_AGE          60 /* default maximum object lifetime in seconds */
#define COAP_MAX_PDU_SIZE           700 /* maximum size of a CoAP PDU */

#define COAP_DEFAULT_VERSION           1 /* version of CoAP supported */
#define COAP_DEFAULT_SCHEME        "coap" /* the default scheme for CoAP URIs */
#define COAP_DEFAULT_URI_WELLKNOWN ".well-known/core" /* well-known resources URI */

/* CoAP message types */

#define COAP_MESSAGE_CON               0 /* confirmable message (requires ACK/RST) */
#define COAP_MESSAGE_NON               1 /* non-confirmable message (one-shot message) */
#define COAP_MESSAGE_ACK               2 /* used to acknowledge confirmable messages */
#define COAP_MESSAGE_RST               3 /* indicates error in received messages */

/* CoAP request methods */

#define COAP_REQUEST_GET       1
#define COAP_REQUEST_POST      2
#define COAP_REQUEST_PUT       3
#define COAP_REQUEST_DELETE    4

/* CoAP option types (be sure to update check_critical when adding options */

#define COAP_OPTION_CONTENT_TYPE  1 /* C, 8-bit uint, 1 B, 0 (text/plain) */
#define COAP_OPTION_MAXAGE        2 /* E, variable length, 1--4 B, 60 Seconds */
#define COAP_OPTION_URI_SCHEME    3 /* C, String, 1-270 B, "coap" */
#define COAP_OPTION_ETAG          4 /* E, sequence of bytes, 1-4 B, - */
#define COAP_OPTION_URI_AUTHORITY 5 /* C, String, 1-270 B, "" */
#define COAP_OPTION_LOCATION      6 /* E, String, 1-270 B, - */
#define COAP_OPTION_URI_PATH      9 /* C, String, 1-270 B, "" */
#define COAP_OPTION_TOKEN        11 /* C, Sequence of Bytes, 1-2 B, - */
#define COAP_OPTION_URI_QUERY    15 /* C, String, 1-270 B, "" */

/* option types from draft-hartke-coap-observe-01 */

#define COAP_OPTION_SUBSCRIPTION 10 /* E, Duration, 1 B, 0 */

/* selected option types from draft-bormann-coap-misc-04 */

#define COAP_OPTION_ACCEPT        8 /* E  Sequence of Bytes, 1-n B, - */
#define COAP_OPTION_BLOCK        13 /* C, unsigned integer, 1--3 B, 0 */
#define COAP_OPTION_NOOP         14 /* no-op for fenceposting */

/* CoAP result codes (HTTP-Code / 100 * 40 + HTTP-Code % 100) */

#define COAP_RESPONSE_100       40   /* 100 Continue */
#define COAP_RESPONSE_200       80   /* 200 OK */
#define COAP_RESPONSE_201       81   /* 201 Created */
#define COAP_RESPONSE_304      124   /* 304 Not Modified */
#define COAP_RESPONSE_400      160   /* 400 Bad Request */
#define COAP_RESPONSE_404      164   /* 404 Not Found */
#define COAP_RESPONSE_405      165   /* 405 Method Not Allowed */
#define COAP_RESPONSE_415      175   /* 415 Unsupported Media Type */
#define COAP_RESPONSE_500      200   /* 500 Internal Server Error */
#define COAP_RESPONSE_503      203   /* 503 Service Unavailable */
#define COAP_RESPONSE_504      204   /* 504 Gateway Timeout */
#define COAP_RESPONSE_X_240    240   /* Token Option required by server */
#define COAP_RESPONSE_X_241    241   /* Uri-Authority Option required by server */
#define COAP_RESPONSE_X_242    242   /* Critical Option not supported */

/* CoAP media type encoding */

#define COAP_MEDIATYPE_TEXT_PLAIN                     0 /* text/plain (UTF-8) */
#define COAP_MEDIATYPE_TEXT_XML                       1 /* text/xml (UTF-8) */
#define COAP_MEDIATYPE_TEXT_CSV                       2 /* text/csv (UTF-8) */
#define COAP_MEDIATYPE_TEXT_HTML                      3 /* text/html (UTF-8) */
#define COAP_MEDIATYPE_IMAGE_GIF                     21 /* image/gif */
#define COAP_MEDIATYPE_IMAGE_JPEG                    22 /* image/jpeg */
#define COAP_MEDIATYPE_IMAGE_PNG                     23 /* image/png */
#define COAP_MEDIATYPE_IMAGE_TIFF                    24 /* image/tiff */
#define COAP_MEDIATYPE_AUDIO_RAW                     25 /* audio/raw */
#define COAP_MEDIATYPE_VIDEO_RAW                     26 /* video/raw */
#define COAP_MEDIATYPE_APPLICATION_LINK_FORMAT       40 /* application/link-format */
#define COAP_MEDIATYPE_APPLICATION_XML               41 /* application/xml */
#define COAP_MEDIATYPE_APPLICATION_OCTET_STREAM      42 /* application/octet-stream */
#define COAP_MEDIATYPE_APPLICATION_RDF_XML           43 /* application/rdf+xml */
#define COAP_MEDIATYPE_APPLICATION_SOAP_XML          44 /* application/soap+xml  */
#define COAP_MEDIATYPE_APPLICATION_ATOM_XML          45 /* application/atom+xml  */
#define COAP_MEDIATYPE_APPLICATION_XMPP_XML          46 /* application/xmpp+xml  */
#define COAP_MEDIATYPE_APPLICATION_EXI               47 /* application/exi  */
#define COAP_MEDIATYPE_APPLICATION_X_BXML            48 /* application/x-bxml  */
#define COAP_MEDIATYPE_APPLICATION_FASTINFOSET       49 /* application/fastinfoset  */
#define COAP_MEDIATYPE_APPLICATION_SOAP_FASTINFOSET  50 /* application/soap+fastinfoset  */
#define COAP_MEDIATYPE_APPLICATION_JSON              51 /* application/json  */

#define COAP_MEDIATYPE_ANY                         0xff /* any media type */