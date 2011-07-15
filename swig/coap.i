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


%{
// libcoap
#include "debug.h"
#include "encode.h"
#include "list.h"
#include "net.h"
#include <netinet/in.h>
#include "pdu.h"
#include "subscribe.h"
#include "uri.h"

// /usr/include
#include "netdb.h"
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

// /usr/include
#include "netdb.h"

%javaconst(1);
#include "bits/socket.h"
%javaconst(0);

%{
static JavaVM *cached_jvm;
static JNIEnv *jenv;
static jobject cached_client_server;
static jclass cls;
static jmethodID methodid;
static coap_context_t *cached_ctx;

/* cache jvm */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
  printf("INF: JNI_OnLoad\n");

  /* caching JVM */
  cached_jvm = jvm;

  /* get environment */
  if ((*jvm)->GetEnv(jvm, (void **)&jenv, JNI_VERSION_1_4)) {
    printf("ERR: JNI verson not supported \n");
    fflush(stdout);
    return JNI_ERR;
  }

  fflush(stdout);
  return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
  printf("INF: JNI_OnUnLoad\n" );
  fflush(stdout);
  return;
}

JNIEXPORT void JNICALL message_handler_proxy(coap_context_t *ctx,
					     coap_queue_t *node,
					     void *data) {
  printf("INF: message_handler_proxy()\n");
  printf("INF: ctx %p, node %p, data %p\n", ctx, node, data);

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
	printf("ERR: Client/Server class not found.\n");
    }

    methodid = (*jenv)->GetMethodID(jenv, cls, "messageHandler",
				   "(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/coap_listnode;Ljava/lang/String;)V");
    if (methodid == NULL) {
      printf("ERR: messageHandler not found.\n");
      return;
    } else {
      printf("INF: messageHandler found.\n");
    }

    //TODO: handle data properly (needed at all?)
    jstring data_obj = (*jenv)->NewStringUTF(jenv, "");

    printf("INF: callback into Java %p\n", methodid);
    (*jenv)->CallNonvirtualVoidMethod(jenv, cached_client_server, cls, methodid,
				      ctx_obj, node_obj, data_obj);

    if ((*jenv)->ExceptionOccurred(jenv)) {
      printf("ERR: Exception occurred.\n");
      SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Exception in callback.");
      return;
    }
    printf("INF: ~callback\n");

  } else {
    printf("ERR: could not attach to thread.\n");
  }
  printf("INF: ~message_handler_proxy()\n");
  fflush(stdout);
}

/* register message handler */
void register_message_handler(coap_context_t *ctx, jobject client_server) {
  printf ("INF: register_message_handler(%p, %p)\n", ctx, client_server);
  cached_client_server = (*jenv)->NewGlobalRef(jenv, client_server);
  coap_register_message_handler(ctx, message_handler_proxy);
  printf ("INF: ~register_message_handler()\n");
  fflush(stdout);
}

struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr) {
  char *stAddr;

  printf("INF: sockaddr_in6_create()\n");

  stAddr = (char *)(*jenv)->GetStringUTFChars(jenv, addr, NULL);
  if (stAddr == NULL) {
    printf("ERR: stAddr == NULL\n");
    fflush(stdout);
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Not able to allocate String.");
    return;
  }
  printf("INF: family %i port %i addr %s\n", family, port, stAddr);

  struct sockaddr_in6 *p = (struct sockaddr_in6 *) malloc(sizeof(struct sockaddr_in6));
  p->sin6_family = family;
  p->sin6_port = htons(port);
  inet_pton(AF_INET6, stAddr, &(p->sin6_addr));

  (*jenv)->ReleaseStringUTFChars(jenv, addr, stAddr);
  printf("INF: ~sockaddr_in6_create() %p\n", p);
  fflush(stdout);
  return p;
}

void sockaddr_in6_free(struct sockaddr_in6 *p) {
  printf("INF: sockaddr_in6_free()\n");
  free(p);
  printf("INF: ~sockaddr_in6_free()\n");
  fflush(stdout);
}

#define COAP_RESOURCE_CHECK_TIME 2
void check_receive(coap_context_t *ctx) {
  fd_set readfds;
  struct timeval tv, *timeout;
  int result;
  time_t now;
  coap_queue_t *nextpdu;

  //signal(SIGINT, handle_sigint);

  FD_ZERO(&readfds);
  FD_SET( ctx->sockfd, &readfds );

  nextpdu = coap_peek_next( ctx );
  printf("INF: nextpdu %p\n", nextpdu);

  time(&now);
  while ( nextpdu && nextpdu->t <= now ) {
    coap_retransmit( ctx, coap_pop_next( ctx ) );
    nextpdu = coap_peek_next( ctx );
  }

  if ( nextpdu && nextpdu->t <= now + COAP_RESOURCE_CHECK_TIME ) {
    /* set timeout if there is a pdu to send before our automatic timeout occurs */
    tv.tv_usec = 0;
    tv.tv_sec = nextpdu->t - now;
    timeout = &tv;
  } else {
    tv.tv_usec = 0;
    tv.tv_sec = COAP_RESOURCE_CHECK_TIME;
    timeout = &tv;
  }
  result = select( FD_SETSIZE, &readfds, 0, 0, timeout );
  printf("INF: result %u\n", result);

  if ( result < 0 ) {		/* error */
    /*if (errno != EINTR)
      perror("select");*/
  } else if ( result > 0 ) {	/* read from socket */
    if ( FD_ISSET( ctx->sockfd, &readfds ) ) {
      printf("INF: read\n");
      coap_read( ctx );	/* read received data */
      printf("INF: ~read\n");
      printf("INF: dispatch\n");
      coap_dispatch( ctx );	/* and dispatch PDUs from receivequeue */
      printf("INF: ~dispatch\n");
    }
  } else {			/* timeout */
    printf("INF: timeout\n");
    coap_check_resource_list( ctx );
    coap_check_subscriptions( ctx );
  }

  fflush(stdout);
  return;
}

%}

void register_message_handler(coap_context_t *ctx, jobject client_server);
struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr);
void sockaddr_in6_free(struct sockaddr_in6 *p);
void check_receive(coap_context_t *ctx);

%javaconst(1);

#define      PF_INET         2       /* IP protocol family.  */
#define      PF_INET6        10      /* IP version 6.  */
#define      AF_INET         PF_INET
#define      AF_INET6        PF_INET6

