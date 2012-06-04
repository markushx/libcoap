%module coap

 //#ifdef SWIGJAVA
 //TODO: wrap Java specific stuff, to enable other languages as well
 //#endif

%include "typemaps.i"

 // handle unsigned char* the same way as char* -> String
%typemap(jni) unsigned char *, unsigned char *&, unsigned char[ANY], unsigned char[]               "jstring"
%typemap(jtype) unsigned char *, unsigned char *&, unsigned char[ANY], unsigned char[]               "String"
%typemap(jstype) unsigned char *, unsigned char *&, unsigned char[ANY], unsigned char[]               "String"

 /* unsigned char * - treat as String */
%typemap(in, noblock=1) unsigned char * {
  $1 = 0;
  if ($input) {
    $1 = ($1_ltype)JCALL2(GetStringUTFChars, jenv, $input, 0);
    if (!$1) return $null;
  }
 }

%typemap(freearg, noblock=1) unsigned char * { if ($1) JCALL2(ReleaseStringUTFChars, jenv, $input, (const unsigned char *)$1); }
%typemap(out, noblock=1) unsigned char * { if ($1) $result = JCALL1(NewStringUTF, jenv, (const unsigned char *)$1); }

/* unsigned char *& - treat as String */
%typemap(in, noblock=1) unsigned char *& ($*1_ltype temp = 0) {
  $1 = 0;
  if ($input) {
    temp = ($*1_ltype)JCALL2(GetStringUTFChars, jenv, $input, 0);
    if (!temp) return $null;
  }
  $1 = &temp;
 }
%typemap(freearg, noblock=1) unsigned char *& { if ($1 && *$1) JCALL2(ReleaseStringUTFChars, jenv, $input, (const char *)*$1); }
%typemap(out, noblock=1) unsigned char *& { if (*$1) $result = JCALL1(NewStringUTF, jenv, (const char *)*$1); }

%typecheck(SWIG_TYPECHECK_STRING) /* Java String */
jstring,
   unsigned char *,
   unsigned char *&,
   unsigned char[ANY],
   unsigned char []
   ""

   %typemap(throws) unsigned char *
   %{ SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, $1);
   return $null; %}

%typemap(javain) unsigned char *, unsigned char *&, unsigned char[ANY], unsigned char[] "$javainput"

   %typemap(javaout) unsigned char *, unsigned char *&, unsigned char[ANY], unsignedchar[] {
  return $jnicall;
 }

/* String & length */
%typemap(jni)     (unsigned char *STRING, size_t LENGTH) "jbyteArray"
%typemap(jtype)   (unsigned char *STRING, size_t LENGTH) "byte[]"
%typemap(jstype)  (unsigned char *STRING, size_t LENGTH) "byte[]"
%typemap(javain)  (unsigned char *STRING, size_t LENGTH) "$javainput"
%typemap(freearg) (unsigned char *STRING, size_t LENGTH) ""
%typemap(in)      (unsigned char *STRING, size_t LENGTH) {
  $1 = (unsigned char *) JCALL2(GetByteArrayElements, jenv, $input, 0);
  $2 = (size_t) JCALL1(GetArrayLength,       jenv, $input);
 }
%typemap(argout)  (unsigned char *STRING, size_t LENGTH) {
  JCALL3(ReleaseByteArrayElements, jenv, $input, (jbyte *)$1, 0);
 }
%apply (unsigned char *STRING, size_t LENGTH) { (unsigned char *STRING, int LENGTH) }

// override protected to public:
%typemap(javabody) SWIGTYPE %{
  private long swigCPtr;
		       protected boolean swigCMemOwn;

		       public $javaclassname(long cPtr, boolean cMemoryOwn) {
			 swigCMemOwn = cMemoryOwn;
			 swigCPtr = cPtr;
		       }

  public static long getCPtr($javaclassname obj) {
    //System.out.println(">> DBG: swigCPtr " + obj.swigCPtr + "\n");
    return (obj == null) ? 0 : obj.swigCPtr;
  }
  %}

%typemap(in) jobject {
  $1 = (*jenv)->NewGlobalRef(jenv, $input);
 }

%apply int { in_port_t };
%apply long { time_t };
%ignore coap_get_data;

%{
  // libcoap
#include "debug.h"
#include "encode.h"
#include "coap_list.h"
#include "net.h"
#include "pdu.h"
#include "subscribe.h"
#include "uri.h"

  // /usr/include
//#include "netdb.h"
//#include "linux/in6.h"
#include <netinet/in.h>
//#include <sys/select.h>

int coap_get_data_java(coap_pdu_t *pdu, unsigned char *data) {
  int len = 0;
  if ( !pdu )
    return -1;

  if ( pdu->data < (unsigned char *)pdu->hdr + pdu->length ) {
    /* pdu contains data */

    len = (unsigned char *)pdu->hdr + pdu->length - pdu->data;
    memcpy(data, pdu->data, len);
  } else {			/* no data, clear everything */
    len = 0;
    //*data = NULL;
  }

  return len;
}

  %}

%ignore __quad_t;
%ignore __u_quad_t;
%ignore recvmmsg;

%typemap(jni) (unsigned int) = int;
%typemap(jtype) (unsigned int) = int;
%typemap(jstype) (unsigned int) = int;
%typemap(javain) (unsigned int) = int;
%typemap(javaout) (unsigned int) = int;

// libcoap
#include "debug.h"
#include "encode.h"
#include "list.h"
#include "net.h"
#include "pdu.h"
%ignore coap_opt_t;

#include "subscribe.h"
#include "uri.h"

%define __signed__
signed
%enddef

// /usr/include
//#include "netdb.h"
//#include "linux/in6.h"
/*
%javaconst(1);
#include "bits/socket.h"
%javaconst(0);
*/
%{
  static JavaVM *cached_jvm;
  static JNIEnv *jenv;
  static jobject cached_client_server;
  static jclass cls;
  static jmethodID methodid;
  static coap_context_t *cached_ctx;

  /* cache jvm */
  JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    //printf("INF: JNI_OnLoad\n");

    /* caching JVM */
    cached_jvm = jvm;

    /* get environment */
    if ((*jvm)->GetEnv(jvm, (void **)&jenv, JNI_VERSION_1_4)) {
      //printf("ERR: JNI verson not supported \n");
      //fflush(stdout);
      return JNI_ERR;
    }

    //fflush(stdout);
    return JNI_VERSION_1_4;
  }

  JNIEXPORT void JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
    //printf("INF: JNI_OnUnLoad\n" );
    //fflush(stdout);
    return;
  }

  JNIEXPORT void JNICALL message_handler_proxy(coap_context_t *ctx,
					       coap_queue_t *node,
					       void *data) {
    //printf("INF: message_handler_proxy()\n");
    //printf("INF: ctx %p, node %p, data %p\n", ctx, node, data);

    //printf("--------------------------\n");
    //coap_show_pdu( node->pdu );
    //printf("--------------------------\n");

    jlong     jctx;
    jclass    ctx_cls;
    jmethodID ctx_con;
    jfieldID  ctx_fid;
    jobject   ctx_obj;

    jlong     jnode;
    jclass    node_cls;
    jmethodID node_con;
    jfieldID  node_fid;
    jobject   node_obj;

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
    if (ret >= 0) {

      //TODO: handle null pointers.
      ctx_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_context_t");
      ctx_con = (*jenv)->GetMethodID(jenv, ctx_cls, "<init>", "(JZ)V");
      ctx_fid = (*jenv)->GetFieldID(jenv, ctx_cls, "swigCPtr", "J");
      *((coap_context_t **)&jctx) = (coap_context_t *) ctx;
      ctx_obj = (*jenv)->NewObject(jenv, ctx_cls, ctx_con, jctx, NULL);

      node_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_listnode");
      node_con = (*jenv)->GetMethodID(jenv, node_cls, "<init>", "(JZ)V");
      node_fid = (*jenv)->GetFieldID(jenv, node_cls, "swigCPtr", "J");
      *((coap_queue_t **)&jnode) = (coap_queue_t *) node;
      node_obj = (*jenv)->NewObject(jenv, node_cls, node_con, jnode, NULL);

      /* find Java Client/Server class */
      cls = (*jenv)->GetObjectClass(jenv, cached_client_server);
      if (cls == NULL) {
	//printf("ERR: Client/Server class not found.\n");
      }

      methodid = (*jenv)->GetMethodID(jenv, cls, "messageHandler",
				      "(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/coap_listnode;Ljava/lang/String;)V");
      if (methodid == NULL) {
	//printf("ERR: messageHandler not found.\n");
	return;
      } else {
	//printf("INF: messageHandler found.\n");
      }

      //TODO: handle data properly (needed at all?)
      jstring data_obj = (*jenv)->NewStringUTF(jenv, "");

      //printf("INF: callback into Java %p\n", methodid);
      (*jenv)->CallNonvirtualVoidMethod(jenv, cached_client_server, cls, methodid,
					ctx_obj, node_obj, data_obj);

      if ((*jenv)->ExceptionOccurred(jenv)) {
	//printf("ERR: Exception occurred.\n");
	SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Exception in callback.");
	return;
      }
      //printf("INF: ~callback\n");

    } else {
      //printf("ERR: could not attach to thread.\n");
    }
    //printf("INF: ~message_handler_proxy()\n");
    //fflush(stdout);
  }

  /* register message handler */
  void register_message_handler(coap_context_t *ctx, jobject client_server) {
    //printf ("INF: register_message_handler(%p, %p)\n", ctx, client_server);

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
    if (ret >= 0) {
      cached_client_server = (*jenv)->NewGlobalRef(jenv, client_server);
      coap_register_message_handler(ctx, message_handler_proxy);
    } else {
      //TODO: handle error
    }
    //printf ("INF: ~register_message_handler()\n");
    //fflush(stdout);
  }

  void deregister_message_handler(coap_context_t *ctx, jobject client_server) {
    //printf ("INF: deregister_message_handler(%p, %p)\n", ctx, client_server);
    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
    if (ret >= 0) {
      (*jenv)->DeleteGlobalRef(jenv, client_server);
    } else {
      //TODO: handle error
    }
    //printf ("INF: ~deregister_message_handler()\n");
    //fflush(stdout);
  }

  JNIEXPORT void JNICALL coap_send_impl(coap_context_t *ctx,
					const struct sockaddr_in6 *dst,
					coap_pdu_t *pdu,
					int free_pdu) {

    jlong     jctx;
    jclass    ctx_cls;
    jmethodID ctx_con;
    jfieldID  ctx_fid;
    jobject   ctx_obj;

    jlong     jpdu;
    jclass    pdu_cls;
    jmethodID pdu_con;
    jfieldID  pdu_fid;
    jobject   pdu_obj;

    jlong     jdst;
    jclass    dst_cls;
    jmethodID dst_con;
    jfieldID  dst_fid;
    jobject   dst_obj;

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
    if (ret >= 0) {

      //TODO: handle null pointers.
      ctx_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_context_t");
      ctx_con = (*jenv)->GetMethodID(jenv, ctx_cls, "<init>", "(JZ)V");
      ctx_fid = (*jenv)->GetFieldID(jenv, ctx_cls, "swigCPtr", "J");
      *((coap_context_t **)&jctx) = (coap_context_t *) ctx;
      ctx_obj = (*jenv)->NewObject(jenv, ctx_cls, ctx_con, jctx, NULL);

      pdu_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_pdu_t");
      pdu_con = (*jenv)->GetMethodID(jenv, pdu_cls, "<init>", "(JZ)V");
      pdu_fid = (*jenv)->GetFieldID(jenv, pdu_cls, "swigCPtr", "J");
      *((coap_pdu_t **)&jpdu) = (coap_pdu_t *) pdu;
      pdu_obj = (*jenv)->NewObject(jenv, pdu_cls, pdu_con, jpdu, NULL);

      //dst_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/sockaddr_in6");
      dst_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/SWIGTYPE_p_sockaddr_in6");
      dst_con = (*jenv)->GetMethodID(jenv, dst_cls, "<init>", "(JZ)V");
      dst_fid = (*jenv)->GetFieldID(jenv, dst_cls, "swigCPtr", "J");
      *((struct sockaddr_in6 **)&jdst) = (struct sockaddr_in6 *) dst;
      dst_obj = (*jenv)->NewObject(jenv, dst_cls, dst_con, jdst, NULL);

      //FIXME!!!!!!!!!
      /* find Java Client/Server class */
      cls = (*jenv)->GetObjectClass(jenv, cached_client_server);
      if (cls == NULL) {
	//printf("ERR: Client/Server class not found.\n");
      }

      /*methodid = (*jenv)->GetMethodID(jenv, cls, "coap_send_impl",
	"(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/sockaddr_in6;Lde/tzi/coap/jni/coap_pdu_t;I)V");*/
      methodid = (*jenv)->GetMethodID(jenv, cls, "coap_send_impl",
				      "(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/SWIGTYPE_p_sockaddr_in6;Lde/tzi/coap/jni/coap_pdu_t;I)V");

      if (methodid == NULL) {
	//printf("ERR: messageHandler not found.\n");
	return;
      } else {
	//printf("INF: messageHandler found.\n");
      }

      jint free = free_pdu;

      //printf("INF: callback into Java %p\n", methodid);
      (*jenv)->CallNonvirtualVoidMethod(jenv, cached_client_server, cls, methodid,
					ctx_obj, dst_obj, pdu_obj, free);

      if ((*jenv)->ExceptionOccurred(jenv)) {
	//printf("ERR: Exception occurred.\n");
	SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Exception in callback.");
	return;
      }
      //printf("INF: ~callback\n");

    } else {
      //printf("ERR: could not attach to thread.\n");
    }
    //printf("INF: ~message_handler_proxy()\n");
    //fflush(stdout);
  }

  jbyteArray get_bytes(coap_pdu_t pdu) {

    //printf("INF: get_bytes()\n");
    jbyteArray b_array;

    b_array = (*jenv)->NewByteArray(jenv, pdu.length);
    (*jenv)->SetByteArrayRegion(jenv, b_array, 0, pdu.length, (jbyte *)pdu.hdr);

    return b_array;
  }

  /*
  #define options_start(p) ((coap_opt_t *) ( (unsigned char *)p->hdr + sizeof ( coap_hdr_t ) ))

#define options_end(p, opt) {			\
  unsigned char opt_code = 0, cnt;		\
  *opt = options_start( node->pdu );            \
  for ( cnt = (p)->hdr->optcnt; cnt; --cnt ) {  \
    opt_code += COAP_OPT_DELTA(**opt);			\
    *opt = (coap_opt_t *)( (unsigned char *)(*opt) + COAP_OPT_SIZE(**opt)); \
  } \
}
*/

  int coap_read(coap_context_t *ctx, struct sockaddr_in6 src,
		 jbyteArray receivedData, int bytes_read) {

    static char buf[COAP_MAX_PDU_SIZE];
    coap_hdr_t *pdu = (coap_hdr_t *)buf;

    coap_address_t /*src, */dst; // TODO: src is not sockaddr_in6 anymore
    coap_queue_t *node;

    //coap_address_init(&src);

    jbyte *bytes = (*jenv)->GetByteArrayElements(jenv, receivedData, NULL);
    memcpy( buf, bytes, bytes_read ); //TODO: check: sizeof(buf) or bytes_read?

    if ( bytes_read < 0 ) {
      warn("coap_read: recvfrom");
      return -1;
    }

    if ( (size_t)bytes_read < sizeof(coap_hdr_t)) {
      debug("coap_read: discarded invalid frame (too small)\n" );
      return -1;
    }

    if ( pdu->version != COAP_DEFAULT_VERSION ) {
      debug("coap_read: unknown protocol version\n" );
      return -1;
    }

    node = coap_new_node();
    if ( !node ) {
      return -1;
    }

    node->pdu = coap_pdu_init(0, 0, 0, bytes_read);
    if ( !node->pdu )
      goto error;

    coap_ticks( &node->t );
    memcpy(&node->local, &dst, sizeof(coap_address_t));
    memcpy(&node->remote, &src, sizeof(coap_address_t));

    /* "parse" received PDU by filling pdu structure */
    memcpy( node->pdu->hdr, buf, bytes_read );
    node->pdu->length = bytes_read;

    /* finally calculate beginning of data block */
    {
      coap_opt_t *opt = options_start(node->pdu);
      unsigned char cnt = node->pdu->hdr->optcnt;

      /* Note that we cannot use the official options iterator here as
       * it eats up the fence posts. */
      while (cnt && opt) {
	if ((unsigned char *)node->pdu->hdr + node->pdu->max_size <= opt) {
	  if (node->pdu->hdr->type == COAP_MESSAGE_CON ||
	      node->pdu->hdr->type == COAP_MESSAGE_NON) {
	    coap_send_message_type(ctx, &node->remote, node->pdu,
				   COAP_MESSAGE_RST);
	    debug("sent RST on malformed message\n");
	  } else {
	    debug("dropped malformed message\n");
	}
	  goto error;
	}

	if (node->pdu->hdr->optcnt == COAP_OPT_LONG) {
	  if (*opt == COAP_OPT_END) {
	    ++opt;
	    break;
	  }
	} else {
	  --cnt;
	}
	opt = options_next(opt);
      }

      node->pdu->data = (unsigned char *)opt;
    }

    /* and add new node to receive queue */
    coap_transaction_id(&node->remote, node->pdu, &node->id);
    coap_insert_node(&ctx->recvqueue, node, _order_timestamp);

    (*jenv)->ReleaseByteArrayElements(jenv, receivedData, bytes, JNI_ABORT);

    /*
    static char addr[INET6_ADDRSTRLEN];
    if ( inet_ntop(src.sin6_family, &src.sin6_addr, addr, INET6_ADDRSTRLEN) == 0 ) {
      //printf("coap_read: inet_ntop failed");
    } else {
      //printf("** received from [%s]:%d:\n  ",addr,ntohs(src.sin6_port));
    }
    */
    coap_show_pdu( node->pdu );

    return 0;
  error:
    coap_delete_node(node);
    return -1;
  }

  struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr) {
    char *stAddr;

    //printf("INF: sockaddr_in6_create()\n");

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    stAddr = (char *)(*jenv)->GetStringUTFChars(jenv, addr, NULL);
    if (stAddr == NULL) {
      //printf("ERR: stAddr == NULL\n");
      //fflush(stdout);
      SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Not able to allocate String.");
      return;
    }
    //printf("INF: family %i port %i addr %s\n", family, port, stAddr);

    struct sockaddr_in6 *p = (struct sockaddr_in6 *) malloc(sizeof(struct sockaddr_in6));
    p->sin6_family = family;
    p->sin6_port = htons(port);
    inet_pton(AF_INET6, stAddr, &(p->sin6_addr));

    (*jenv)->ReleaseStringUTFChars(jenv, addr, stAddr);
    //printf("INF: ~sockaddr_in6_create() %p\n", p);
    //fflush(stdout);
    return p;
  }

  jstring get_addr(struct sockaddr_in6 src) {
    static char cAddr[INET6_ADDRSTRLEN];
    //printf("INF: get_addr()\n");

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    if ( inet_ntop(src.sin6_family, &src.sin6_addr, cAddr, INET6_ADDRSTRLEN) == 0 ) {
      //printf("INF: get_addr() inet_ntop fail\n");
      SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Not able to do address conversion.");
    } else {
      //printf("INF: get_addr() inet_ntop success\n");
    }

    jstring stAddr = (*jenv)->NewStringUTF(jenv, cAddr);

    return stAddr;
  }

  jint get_port(struct sockaddr_in6 src) {

    jint iPort;

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    iPort = ntohs(src.sin6_port);

    return iPort;
  }

  void sockaddr_in6_free(struct sockaddr_in6 *p) {
    //printf("INF: sockaddr_in6_free()\n");
    free(p);
    //printf("INF: ~sockaddr_in6_free()\n");
    //fflush(stdout);
  }
  %}

void register_message_handler(coap_context_t *ctx, jobject client_server);
void deregister_message_handler(coap_context_t *ctx, jobject client_server);
struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr);
jstring get_addr(struct sockaddr_in6 src);
jint get_port(struct sockaddr_in6 src);
jbyteArray get_bytes(coap_pdu_t pdu);
void coap_read(coap_context_t *ctx, struct sockaddr_in6 src, jbyteArray receivedData, int length);
void sockaddr_in6_free(struct sockaddr_in6 *p);
void coap_send_impl(coap_context_t *ctx, const struct sockaddr_in6 *dst, coap_pdu_t *pdu, int free_pdu);

//void check_receive_client(coap_context_t *ctx);
//void check_receive_server(coap_context_t *ctx);

%javaconst(1);

#define      PF_INET         2       /* IP protocol family. */
#define      PF_INET6        10      /* IP version 6. */
#define      AF_INET         PF_INET
#define      AF_INET6        PF_INET6

%include "arrays_java.i"
%apply unsigned char[] {unsigned char *};

int coap_get_data_java(coap_pdu_t *pdu, unsigned char *data);
