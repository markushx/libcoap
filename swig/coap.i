%module coap

//#ifdef SWIGJAVA
//TODO: wrap Java specific stuff, to enable other languages as well
//#endif

%include "carrays.i"
%array_functions(unsigned char, unsignedCharArray);

%typemap(javabody) SWIGTYPE %{
  private long swigCPtr;
  protected boolean swigCMemOwn;

  public $javaclassname(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  public static long getCPtr($javaclassname obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }
%}

%typemap(in) jobject {
  $1 = (*jenv)->NewGlobalRef(jenv, $input);
}
typedef int in_port_t;

struct in6_addr
{
  union
  {
    signed char __u6_addr8[16];
  } __in6_u;
  //#define s6_addr                 __in6_u.__u6_addr8
};

struct sockaddr_in6
{
  in_port_t sin6_port;        /* Transport layer port # */
  uint32_t sin6_flowinfo;     /* IPv6 flow information */
  struct in6_addr sin6_addr;  /* IPv6 address */
  uint32_t sin6_scope_id;     /* IPv6 scope-id */
};

%{
#include "debug.h"
#include "encode.h"
#include "list.h"
#include "net.h"
#include <netinet/in.h>
#include "pdu.h"
#include "subscribe.h"
#include "uri.h"
%}

#include "debug.h"
#include "encode.h"
#include "list.h"
#include "net.h"
#include "pdu.h"
#include "subscribe.h"
#include "uri.h"

%{
static JavaVM *cached_jvm;
static JNIEnv *env;
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
  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_4)) {
    printf("ERR: JNI verson not supported \n");
    return JNI_ERR;
  }

  return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
  printf("INF: JNI_OnUnLoad\n" );
  return;
}

JNIEXPORT void JNICALL message_handler_proxy(coap_context_t *ctx,
					     coap_queue_t *node,
					     void *data) {
  printf("message_handler_proxy()\n");
  printf("ctx %p, node %p, data %p\n", ctx, node, data);

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

  (*cached_jvm)->GetEnv(cached_jvm, (void **)&env, JNI_VERSION_1_4);

  int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&env, NULL);
  if (ret >= 0) {

    //TODO: handle null pointers.
    ctx_cls = (*env)->FindClass(env, "de/tzi/coap/jni/coap_context_t");
    ctx_con = (*env)->GetMethodID(env, ctx_cls, "<init>", "(JZ)V");
    ctx_fid = (*env)->GetFieldID(env, ctx_cls, "swigCPtr", "J");
    *((coap_context_t **)&jctx) = (coap_context_t *) ctx;
    ctx_obj = (*env)->NewObject(env, ctx_cls, ctx_con, jctx, NULL);

    node_cls = (*env)->FindClass(env, "de/tzi/coap/jni/coap_listnode");
    node_con = (*env)->GetMethodID(env, node_cls, "<init>", "(JZ)V");
    node_fid = (*env)->GetFieldID(env, node_cls, "swigCPtr", "J");
    *((coap_queue_t **)&jnode) = (coap_queue_t *) node;
    node_obj = (*env)->NewObject(env, node_cls, node_con, jnode, NULL);

    //printf("ctx_obj %p, node_obj %p, data %p\n", ctx_obj, node_obj, data);
    //printf("ctx_ptr %p, node_ptr %p, data %p\n", ctx_ptr, node_ptr, data);
    //printf("ctx %p, node %p, data %p\n", ctx, node, data);

    /* find class */
    cls = (*env)->FindClass(env, "de/tzi/coap/Client");
    if (cls == NULL) {
      printf("INF: Not client.\n");
      cls = (*env)->FindClass(env, "de/tzi/coap/Server");
      if (cls == NULL) {
	printf("INF: Not server.\n");
	printf("ERR: Neither Server nor Client\n");
	return;
      } else {
	printf("INF: Server.\n");
      }
    } else {
      printf("INF: Client.\n");
    }

    methodid = (*env)->GetMethodID(env, cls, "messageHandler",
				   "(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/coap_listnode;Ljava/lang/String;)V");

    if (methodid == NULL) {
      printf("ERR: messageHandler not found.\n");
      return;
    } else {
      printf("INF: messageHandler found.\n");
    }

    //printf("cached_client_server %p\n", cached_client_server);

    printf("calling Mr. Raider %p\n", methodid);
    //TODO: handle data properly (needed at all?)
    jstring data_obj = (*env)->NewStringUTF(env, "");

    printf("node->pdu %p\n", node->pdu);
    printf("node->pdu->hdr %p\n", node->pdu->hdr);
    printf("node->pdu->hdr->version %u\n", node->pdu->hdr->version);
    printf("node->pdu->hdr->type %u\n", node->pdu->hdr->type);
    printf("node->pdu->hdr->optcnt %u\n", node->pdu->hdr->optcnt);
    printf("node->pdu->hdr->code %u\n", node->pdu->hdr->code);

    printf("node->pdu->length %u\n", node->pdu->length);

    (*env)->CallNonvirtualVoidMethod(env, cached_client_server, cls, methodid,
				     ctx_obj, node_obj, data_obj);

    printf("calling Mr. Vain\n");
  } else {
    printf("ERR: could not attach to thread.\n");
  }
  printf("~message_handler_proxy()\n");
}

/* register message handler */
void register_message_handler(coap_context_t *ctx, jobject client_server) {
  printf ("register_message_handler(%p, %p)\n", ctx, client_server);

  cached_client_server = (*env)->NewGlobalRef(env, client_server);

  coap_register_message_handler(ctx, message_handler_proxy);
  printf ("~register_message_handler()\n");
}

struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr) {
  char *stAddr;

  printf("sockaddr_in6_create()\n");

  stAddr = (char *)(*env)->GetStringUTFChars(env, addr, NULL);
  if (stAddr==NULL) return;

  struct sockaddr_in6 *p = (struct sockaddr_in6 *) malloc(sizeof(struct sockaddr_in6));

  printf("family %i port %i/%i addr %s\n", family, port, htons(port), stAddr);
  p->sin6_family = family;
  p->sin6_port = htons(port);
  inet_pton(AF_INET6, stAddr, &(p->sin6_addr));

  (*env)->ReleaseStringUTFChars(env, addr, stAddr);
  printf("~sockaddr_in6_create()\n");
  return p;
}

void sockaddr_in6_free(struct sockaddr_in6 *p) {
  printf("sockaddr_in6_free()\n");
  free(p);
  printf("~sockaddr_in6_free()\n");
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
  printf("nextpdu %p\n", nextpdu);

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
  printf("result %u\n", result);

  if ( result < 0 ) {		/* error */
    /*if (errno != EINTR)
      perror("select");*/
  } else if ( result > 0 ) {	/* read from socket */
    if ( FD_ISSET( ctx->sockfd, &readfds ) ) {
      printf("read\n");
      coap_read( ctx );	/* read received data */
      printf("~read\n");
      printf("dispatch\n");
      coap_dispatch( ctx );	/* and dispatch PDUs from receivequeue */
      printf("~dispatch\n");
    }
  } else {			/* timeout */
    printf("timeout\n");
    coap_check_resource_list( ctx );
    coap_check_subscriptions( ctx );
  }

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
