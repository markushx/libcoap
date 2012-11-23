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

%apply int { socklen_t };
%apply int { in_port_t };
%apply int { coap_tid_t };
%apply long { time_t };
%ignore coap_get_data;
%ignore coap_print_addr;
%ignore COAP_DEFAULT_WKC_HASHKEY;

%{
  // libcoap
#include "coap.h"

  // /usr/include
//#include "linux/in6.h"
//#include <sys/select.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
  //#include <stdio.h>
#ifdef ANDROID
#include <android/log.h>
#endif

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
#include "coap.h"

%ignore coap_opt_t;

%define __signed__
signed
%enddef

// /usr/include
//#include "netdb.h"
//#include "linux/in6.h"
//#include <netinet/in.h>
//#include <sys/socket.h>
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
    coap_log(LOG_DEBUG, "JNI_OnLoad\n");

    /* caching JVM */
    cached_jvm = jvm;

    /* get environment */
    if ((*jvm)->GetEnv(jvm, (void **)&jenv, JNI_VERSION_1_4)) {
      coap_log(LOG_CRIT, "JNI verson not supported \n");
      //fflush(stdout);
      return JNI_ERR;
    }

    //fflush(stdout);
    return JNI_VERSION_1_4;
  }

  JNIEXPORT void JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
    coap_log(LOG_DEBUG, "JNI_OnUnLoad\n" );
    //fflush(stdout);
    return;
  }

  //--------------
  //client:
  //--------------
  JNIEXPORT void JNICALL response_handler_proxy(struct coap_context_t  *ctx,
						const coap_address_t *remote,
						coap_pdu_t *sent,
						coap_pdu_t *received,
						const coap_tid_t tid) {
    coap_log(LOG_DEBUG, "response_handler_proxy()\n");

    //printf("--------------------------\n");
    //coap_show_pdu( node->pdu );
    //printf("--------------------------\n");

    jlong     jctx;
    jclass    ctx_cls;
    jmethodID ctx_con;
    jfieldID  ctx_fid;
    jobject   ctx_obj;

    jlong     jremote;
    jclass    remote_cls;
    jmethodID remote_con;
    jfieldID  remote_fid;
    jobject   remote_obj;

    jlong     jpdusent;
    jlong     jpdureceived;
    jclass    pdu_cls;
    jmethodID pdu_con;
    jfieldID  pdu_fid;
    jobject   pdusent_obj;
    jobject   pdureceived_obj;

    /*jlong     jid;
    jclass    tid_cls;
    jmethodID tid_con;
    jfieldID  tid_fid;
    jobject   id_obj;*/

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {

      //TODO: handle null pointers.
      coap_log(LOG_DEBUG, "ctx\n");
      ctx_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_context_t");
      ctx_con = (*jenv)->GetMethodID(jenv, ctx_cls, "<init>", "(JZ)V");
      ctx_fid = (*jenv)->GetFieldID(jenv, ctx_cls, "swigCPtr", "J");
      *((coap_context_t **)&jctx) = (coap_context_t *) ctx;
      ctx_obj = (*jenv)->NewObject(jenv, ctx_cls, ctx_con, jctx, NULL);

      coap_log(LOG_DEBUG, "remote\n");
      remote_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/__coap_address_t");
      remote_con = (*jenv)->GetMethodID(jenv, remote_cls, "<init>", "(JZ)V");
      remote_fid = (*jenv)->GetFieldID(jenv, remote_cls, "swigCPtr", "J");
      *((coap_address_t **)&jremote) = (coap_address_t *) remote;
      remote_obj = (*jenv)->NewObject(jenv, remote_cls, remote_con, jremote, NULL);

      coap_log(LOG_DEBUG, "pdu\n");
      pdu_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_pdu_t");
      pdu_con = (*jenv)->GetMethodID(jenv, pdu_cls, "<init>", "(JZ)V");
      pdu_fid = (*jenv)->GetFieldID(jenv, pdu_cls, "swigCPtr", "J");
      *((coap_pdu_t **)&jpdusent) = (coap_pdu_t *) sent;
      pdusent_obj = (*jenv)->NewObject(jenv, pdu_cls, pdu_con, jpdusent, NULL);
      *((coap_pdu_t **)&jpdureceived) = (coap_pdu_t *) received;
      pdureceived_obj = (*jenv)->NewObject(jenv, pdu_cls, pdu_con, jpdureceived, NULL);

      //printf("INF: tid\n");
      /*
      tid_cls = (*jenv)->FindClass(jenv, "I");
      //tid_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_tid_t");
      printf("INF: tid2\n");
      tid_con = (*jenv)->GetMethodID(jenv, tid_cls, "<init>", "(JZ)V");
      printf("INF: tid3\n");
      tid_fid = (*jenv)->GetFieldID(jenv, tid_cls, "swigCPtr", "J");
      *((coap_address_t **)&jid) = (coap_address_t *) id;
      id_obj = (*jenv)->NewObject(jenv, tid_cls, tid_con, jid, NULL);
      */
      jint jid = 0;
      int id;
      id = (int)tid;
      jid = (jint)id;

      /*      jint jid = id;
      */
      //printf("INF: done\n");

      /* find Java Client/Server class */
      cls = (*jenv)->GetObjectClass(jenv, cached_client_server);
      if (cls == NULL) {
	coap_log(LOG_CRIT, "Client/Server class not found.\n");
      }

      methodid = (*jenv)->GetMethodID(jenv, cls, "responseHandler",
				      "("
				      "Lde/tzi/coap/jni/coap_context_t;"
				      "Lde/tzi/coap/jni/__coap_address_t;"
				      "Lde/tzi/coap/jni/coap_pdu_t;"
				      "Lde/tzi/coap/jni/coap_pdu_t;"
				      "I)"
				      "V");

      if (methodid == NULL) {
	coap_log(LOG_CRIT, "messageHandler not found.\n");
	return;
      } else {
	//printf("INF: messageHandler found.\n");
      }

      //TODO: handle data properly (needed at all?)
      //jstring data_obj = (*jenv)->NewStringUTF(jenv, "");

      //printf("INF: callback into Java %p\n", methodid);
      /*(*jenv)->CallNonvirtualVoidMethod(jenv, cached_client_server, cls, methodid,
	ctx_obj, node_obj, data_obj);*/
      (*jenv)->CallNonvirtualVoidMethod(jenv,
					cached_client_server, cls, methodid,
					ctx_obj, remote_obj,
					pdusent_obj, pdureceived_obj, jid);

      if ((*jenv)->ExceptionOccurred(jenv)) {
	coap_log(LOG_CRIT, "Exception occurred.\n");
	SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Exception in callback.");
	return;
      }
      //printf("INF: ~callback\n");

    } else {
      coap_log(LOG_CRIT, "could not attach to thread.\n");
    }
    //printf("INF: ~response_handler_proxy()\n");
    //fflush(stdout);
  }

  /* register response handler */
  void register_response_handler(coap_context_t *ctx, jobject client_server) {
    coap_log(LOG_DEBUG, "register_response_handler(%p, %p)\n", ctx, client_server);

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {
      cached_client_server = (*jenv)->NewGlobalRef(jenv, client_server);
      coap_register_response_handler(ctx, response_handler_proxy);
    } else {
      //printf("ERR: could not attach to thread.\n");
    }
    //printf ("INF: ~register_response_handler()\n");
    //fflush(stdout);
  }

  void deregister_response_handler(coap_context_t *ctx, jobject client_server) {
    coap_log(LOG_DEBUG, "deregister_response_handler(%p, %p)\n", ctx, client_server);
    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {
      (*jenv)->DeleteGlobalRef(jenv, client_server);
    } else {
      coap_log(LOG_CRIT, "could not attach to thread.\n");
    }
    //printf ("INF: ~deregister_response_handler()\n");
    //fflush(stdout);
  }

  //--------------
  //server:
  //--------------
  JNIEXPORT void JNICALL handler_proxy(struct coap_context_t  *ctx,
				       struct coap_resource_t *resource,
				       const coap_address_t *peer,
				       coap_pdu_t *request,
				       str* token,
				       coap_pdu_t *response) {
    coap_log(LOG_DEBUG, "handler_proxy()\n");
    coap_log(LOG_DEBUG, "ctx %p\n", ctx);

    //printf("--------------------------\n");
    //coap_show_pdu( node->pdu );
    //printf("--------------------------\n");

    jlong     jctx;
    jclass    ctx_cls;
    jmethodID ctx_con;
    jfieldID  ctx_fid;
    jobject   ctx_obj;

    jlong     jresource;
    jclass    resource_cls;
    jmethodID resource_con;
    jfieldID  resource_fid;
    jobject   resource_obj;

    jlong     jpeer;
    jclass    peer_cls;
    jmethodID peer_con;
    jfieldID  peer_fid;
    jobject   peer_obj;

    jlong     jrequest;
    jlong     jresponse;
    jclass    pdu_cls;
    jmethodID pdu_con;
    jfieldID  pdu_fid;
    jobject   request_obj;
    jobject   response_obj;

    jlong     jtoken;
    jclass    token_cls;
    jmethodID token_con;
    jfieldID  token_fid;
    jobject   token_obj;

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {

      //printf("INF: figuring out java objects\n");
      //TODO: handle null pointers.
      coap_log(LOG_DEBUG, "context\n");
      ctx_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_context_t");
      ctx_con = (*jenv)->GetMethodID(jenv, ctx_cls, "<init>", "(JZ)V");
      ctx_fid = (*jenv)->GetFieldID(jenv, ctx_cls, "swigCPtr", "J");
      *((coap_context_t **)&jctx) = (coap_context_t *) ctx;
      ctx_obj = (*jenv)->NewObject(jenv, ctx_cls, ctx_con, jctx, NULL);

      coap_log(LOG_DEBUG, "resource\n");
      resource_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_resource_t");
      resource_con = (*jenv)->GetMethodID(jenv, ctx_cls, "<init>", "(JZ)V");
      resource_fid = (*jenv)->GetFieldID(jenv, ctx_cls, "swigCPtr", "J");
      *((coap_resource_t **)&jresource) = (coap_resource_t *) resource;
      resource_obj = (*jenv)->NewObject(jenv, resource_cls, resource_con, jresource, NULL);

      coap_log(LOG_DEBUG, "peer\n");
      peer_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/__coap_address_t");
      peer_con = (*jenv)->GetMethodID(jenv, peer_cls, "<init>", "(JZ)V");
      peer_fid = (*jenv)->GetFieldID(jenv, peer_cls, "swigCPtr", "J");
      *((coap_address_t **)&jpeer) = (coap_address_t *) peer;
      peer_obj = (*jenv)->NewObject(jenv, peer_cls, peer_con, jpeer, NULL);

      coap_log(LOG_DEBUG, "pdus\n");
      pdu_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_pdu_t");
      pdu_con = (*jenv)->GetMethodID(jenv, pdu_cls, "<init>", "(JZ)V");
      pdu_fid = (*jenv)->GetFieldID(jenv, pdu_cls, "swigCPtr", "J");
      *((coap_pdu_t **)&jrequest) = (coap_pdu_t *) request;
      request_obj = (*jenv)->NewObject(jenv, pdu_cls, pdu_con, jrequest, NULL);
      *((coap_pdu_t **)&jresponse) = (coap_pdu_t *) response;
      response_obj = (*jenv)->NewObject(jenv, pdu_cls, pdu_con, jresponse, NULL);

      coap_log(LOG_DEBUG, "token\n");
      token_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/str");
      token_con = (*jenv)->GetMethodID(jenv, token_cls, "<init>", "(JZ)V");
      token_fid = (*jenv)->GetFieldID(jenv, token_cls, "swigCPtr", "J");
      *((str **)&token) = (str *) token;
      token_obj = (*jenv)->NewObject(jenv, token_cls, token_con, jtoken, NULL);

      coap_log(LOG_DEBUG, "figured out java objects\n");

      /* find Java Client/Server class */
      cls = (*jenv)->GetObjectClass(jenv, cached_client_server);
      if (cls == NULL) {
	coap_log(LOG_CRIT, "Client/Server class not found.\n");
      }

      methodid = (*jenv)->GetMethodID(jenv, cls, "handler",
				      "("
				      "Lde/tzi/coap/jni/coap_context_t;"
				      "Lde/tzi/coap/jni/coap_resource_t;"
				      "Lde/tzi/coap/jni/__coap_address_t;"
				      "Lde/tzi/coap/jni/coap_pdu_t;"
				      "Lde/tzi/coap/jni/str;"
				      "Lde/tzi/coap/jni/coap_pdu_t;"
				      ")V");

      if (methodid == NULL) {
	coap_log(LOG_CRIT, "Java handler not found.\n");
	return;
      } else {
	coap_log(LOG_DEBUG, "messageHandler found.\n");
      }

      //printf("INF: callback into Java %p\n", methodid);
      (*jenv)->CallNonvirtualVoidMethod(jenv,
					cached_client_server, cls, methodid,
					ctx_obj, resource_obj, peer_obj,
					request_obj, token_obj, response_obj);

      if ((*jenv)->ExceptionOccurred(jenv)) {
	coap_log(LOG_CRIT, "Exception occurred.\n");
	SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Exception in callback.");
	return;
      }
      //printf("INF: ~callback\n");

    } else {
      coap_log(LOG_CRIT, "could not attach to thread.\n");
    }
    //printf("INF: ~response_handler_proxy()\n");
    //fflush(stdout);
  }

  /* register handler */
  void register_handler(coap_resource_t *r, unsigned char method, jobject client_server) {
    coap_log(LOG_DEBUG, "register_handler(%p, %p)\n", r, client_server);

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {
      cached_client_server = (*jenv)->NewGlobalRef(jenv, client_server);
      coap_register_handler(r, method, (coap_method_handler_t)handler_proxy);
    } else {
      coap_log(LOG_CRIT, "could not attach to thread.\n");
    }
    //printf ("INF: ~register_response_handler()\n");
    //fflush(stdout);
  }

  /*
  void deregister_handler(coap_resource_t *r, jobject client_server) {
    //printf ("INF: deregister_response_handler(%p, %p)\n", ctx, client_server);
    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);

    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
    if (ret >= 0) {
      (*jenv)->DeleteGlobalRef(jenv, client_server);
    } else {
      //TODO: handle error
    }
    //printf ("INF: ~deregister_response_handler()\n");
    //fflush(stdout);
  }
  */

  /* JNIEXPORT void JNICALL coap_send_impl(coap_context_t *ctx, */
  /* 					const struct sockaddr_in6 *dst, */
  /* 					coap_pdu_t *pdu, */
  /* 					int free_pdu) { */

  /*   jlong     jctx; */
  /*   jclass    ctx_cls; */
  /*   jmethodID ctx_con; */
  /*   jfieldID  ctx_fid; */
  /*   jobject   ctx_obj; */

  /*   jlong     jpdu; */
  /*   jclass    pdu_cls; */
  /*   jmethodID pdu_con; */
  /*   jfieldID  pdu_fid; */
  /*   jobject   pdu_obj; */

  /*   jlong     jdst; */
  /*   jclass    dst_cls; */
  /*   jmethodID dst_con; */
  /*   jfieldID  dst_fid; */
  /*   jobject   dst_obj; */

  /*   (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4); */

  /*   int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL); */
  /*   if (ret >= 0) { */

  /*     //TODO: handle null pointers. */
  /*     ctx_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_context_t"); */
  /*     ctx_con = (*jenv)->GetMethodID(jenv, ctx_cls, "<init>", "(JZ)V"); */
  /*     ctx_fid = (*jenv)->GetFieldID(jenv, ctx_cls, "swigCPtr", "J"); */
  /*     *((coap_context_t **)&jctx) = (coap_context_t *) ctx; */
  /*     ctx_obj = (*jenv)->NewObject(jenv, ctx_cls, ctx_con, jctx, NULL); */

  /*     pdu_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/coap_pdu_t"); */
  /*     pdu_con = (*jenv)->GetMethodID(jenv, pdu_cls, "<init>", "(JZ)V"); */
  /*     pdu_fid = (*jenv)->GetFieldID(jenv, pdu_cls, "swigCPtr", "J"); */
  /*     *((coap_pdu_t **)&jpdu) = (coap_pdu_t *) pdu; */
  /*     pdu_obj = (*jenv)->NewObject(jenv, pdu_cls, pdu_con, jpdu, NULL); */

  /*     //dst_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/sockaddr_in6"); */
  /*     dst_cls = (*jenv)->FindClass(jenv, "de/tzi/coap/jni/SWIGTYPE_p_sockaddr_in6"); */
  /*     dst_con = (*jenv)->GetMethodID(jenv, dst_cls, "<init>", "(JZ)V"); */
  /*     dst_fid = (*jenv)->GetFieldID(jenv, dst_cls, "swigCPtr", "J"); */
  /*     *((struct sockaddr_in6 **)&jdst) = (struct sockaddr_in6 *) dst; */
  /*     dst_obj = (*jenv)->NewObject(jenv, dst_cls, dst_con, jdst, NULL); */

  /*     //FIXME!!!!!!!!! */
  /*     /\* find Java Client/Server class *\/ */
  /*     cls = (*jenv)->GetObjectClass(jenv, cached_client_server); */
  /*     if (cls == NULL) { */
  /* 	//printf("ERR: Client/Server class not found.\n"); */
  /*     } */

  /*     /\*methodid = (*jenv)->GetMethodID(jenv, cls, "coap_send_impl", */
  /* 	"(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/sockaddr_in6;Lde/tzi/coap/jni/coap_pdu_t;I)V");*\/ */
  /*     methodid = (*jenv)->GetMethodID(jenv, cls, "coap_send_impl", */
  /* 				      "(Lde/tzi/coap/jni/coap_context_t;Lde/tzi/coap/jni/SWIGTYPE_p_sockaddr_in6;Lde/tzi/coap/jni/coap_pdu_t;I)V"); */

  /*     if (methodid == NULL) { */
  /* 	//printf("ERR: messageHandler not found.\n"); */
  /* 	return; */
  /*     } else { */
  /* 	//printf("INF: messageHandler found.\n"); */
  /*     } */

  /*     jint free = free_pdu; */

  /*     //printf("INF: callback into Java %p\n", methodid); */
  /*     (*jenv)->CallNonvirtualVoidMethod(jenv, cached_client_server, cls, methodid, */
  /* 					ctx_obj, dst_obj, pdu_obj, free); */

  /*     if ((*jenv)->ExceptionOccurred(jenv)) { */
  /* 	//printf("ERR: Exception occurred.\n"); */
  /* 	SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, "Exception in callback."); */
  /* 	return; */
  /*     } */
  /*     //printf("INF: ~callback\n"); */

  /*   } else { */
  /*     //printf("ERR: could not attach to thread.\n"); */
  /*   } */
  /*   //printf("INF: ~response_handler_proxy()\n"); */
  /*   //fflush(stdout); */
  /* } */

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

coap_context_t *
  get_context(const char *node, const char *port) {
    coap_context_t *ctx = NULL;
    int s;
    struct addrinfo hints;
    struct addrinfo *result, *rp;

    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_UNSPEC;    /* Allow IPv4 or IPv6 */
    hints.ai_socktype = SOCK_DGRAM; /* Coap uses UDP */
    hints.ai_flags = AI_PASSIVE | AI_NUMERICHOST;

    s = getaddrinfo(node, port, &hints, &result);
    if ( s != 0 ) {
      //fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(s));
      return NULL;
    }

    /* iterate through results until success */
    for (rp = result; rp != NULL; rp = rp->ai_next) {
      coap_address_t addr;

      if (rp->ai_addrlen <= sizeof(addr.addr)) {
	coap_address_init(&addr);
	addr.size = rp->ai_addrlen;
	memcpy(&addr.addr, rp->ai_addr, rp->ai_addrlen);

	ctx = coap_new_context(&addr);
	if (ctx) {
	  /* TODO: output address:port for successful binding */
	  goto finish;
	}
      }
    }

    //fprintf(stderr, "no context available for interface '%s'\n", node);

  finish:
    //fprintf(stdout, "get_context~\n");
    freeaddrinfo(result);
    return ctx;
  }

int
resolve_address(const str *server, struct sockaddr *dst) {

  struct addrinfo *res, *ainfo;
  struct addrinfo hints;
  static char addrstr[256];
  int error, len=-1;

  memset(addrstr, 0, sizeof(addrstr));
  if (server->length)
    memcpy(addrstr, server->s, server->length);
  else
    memcpy(addrstr, "localhost", 9);

  //fprintf(stderr, "getaddrinfo: %p '%s'\n", server->s, server->s);
  //fprintf(stderr, "getaddrinfo: len %u\n", server->length);

  memset ((char *)&hints, 0, sizeof(hints));
  hints.ai_socktype = SOCK_DGRAM;
  hints.ai_family = AF_UNSPEC;

  //fprintf(stderr, "res_add1\n");

  error = getaddrinfo(addrstr, "", &hints, &res);

  //fprintf(stderr, "res_add2\n");

  if (error != 0) {
    //fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(error));
    return error;
  }

  //fprintf(stderr, "res_add3\n");

  for (ainfo = res; ainfo != NULL; ainfo = ainfo->ai_next) {
    switch (ainfo->ai_family) {
    case AF_INET6:
    case AF_INET:
      len = ainfo->ai_addrlen;
      memcpy(dst, ainfo->ai_addr, len);
      goto finish;
    default:
      ;
    }
  }

  //fprintf(stderr, "res_add4\n");

 finish:
  //fprintf(stderr, "res_add5\n");

  freeaddrinfo(res);
  //fprintf(stderr, "res_add6, %u\n", len);

  return len;
}


  /* int coap_read(coap_context_t *ctx, struct sockaddr_in6 src, */
  /* 		 jbyteArray receivedData, int bytes_read) { */

  /*   static char buf[COAP_MAX_PDU_SIZE]; */
  /*   coap_hdr_t *pdu = (coap_hdr_t *)buf; */

  /*   coap_address_t /\*src, *\/dst; // TODO: src is not sockaddr_in6 anymore */
  /*   coap_queue_t *node; */

  /*   //coap_address_init(&src); */

  /*   jbyte *bytes = (*jenv)->GetByteArrayElements(jenv, receivedData, NULL); */
  /*   memcpy( buf, bytes, bytes_read ); //TODO: check: sizeof(buf) or bytes_read? */

  /*   if ( bytes_read < 0 ) { */
  /*     warn("coap_read: recvfrom"); */
  /*     return -1; */
  /*   } */

  /*   if ( (size_t)bytes_read < sizeof(coap_hdr_t)) { */
  /*     debug("coap_read: discarded invalid frame (too small)\n" ); */
  /*     return -1; */
  /*   } */

  /*   if ( pdu->version != COAP_DEFAULT_VERSION ) { */
  /*     debug("coap_read: unknown protocol version\n" ); */
  /*     return -1; */
  /*   } */

  /*   node = coap_new_node(); */
  /*   if ( !node ) { */
  /*     return -1; */
  /*   } */

  /*   node->pdu = coap_pdu_init(0, 0, 0, bytes_read); */
  /*   if ( !node->pdu ) */
  /*     goto error; */

  /*   coap_ticks( &node->t ); */
  /*   memcpy(&node->local, &dst, sizeof(coap_address_t)); */
  /*   memcpy(&node->remote, &src, sizeof(coap_address_t)); */

  /*   /\* "parse" received PDU by filling pdu structure *\/ */
  /*   memcpy( node->pdu->hdr, buf, bytes_read ); */
  /*   node->pdu->length = bytes_read; */

  /*   /\* finally calculate beginning of data block *\/ */
  /*   { */
  /*     coap_opt_t *opt = options_start(node->pdu); */
  /*     unsigned char cnt = node->pdu->hdr->optcnt; */

  /*     /\* Note that we cannot use the official options iterator here as */
  /*      * it eats up the fence posts. *\/ */
  /*     while (cnt && opt) { */
  /* 	if ((unsigned char *)node->pdu->hdr + node->pdu->max_size <= opt) { */
  /* 	  if (node->pdu->hdr->type == COAP_MESSAGE_CON || */
  /* 	      node->pdu->hdr->type == COAP_MESSAGE_NON) { */
  /* 	    coap_send_message_type(ctx, &node->remote, node->pdu, */
  /* 				   COAP_MESSAGE_RST); */
  /* 	    debug("sent RST on malformed message\n"); */
  /* 	  } else { */
  /* 	    debug("dropped malformed message\n"); */
  /* 	} */
  /* 	  goto error; */
  /* 	} */

  /* 	if (node->pdu->hdr->optcnt == COAP_OPT_LONG) { */
  /* 	  if (*opt == COAP_OPT_END) { */
  /* 	    ++opt; */
  /* 	    break; */
  /* 	  } */
  /* 	} else { */
  /* 	  --cnt; */
  /* 	} */
  /* 	opt = options_next(opt); */
  /*     } */

  /*     node->pdu->data = (unsigned char *)opt; */
  /*   } */

  /*   /\* and add new node to receive queue *\/ */
  /*   coap_transaction_id(&node->remote, node->pdu, &node->id); */
  /*   coap_insert_node(&ctx->recvqueue, node, _order_timestamp); */

  /*   (*jenv)->ReleaseByteArrayElements(jenv, receivedData, bytes, JNI_ABORT); */

  /*   /\* */
  /*   static char addr[INET6_ADDRSTRLEN]; */
  /*   if ( inet_ntop(src.sin6_family, &src.sin6_addr, addr, INET6_ADDRSTRLEN) == 0 ) { */
  /*     //printf("coap_read: inet_ntop failed"); */
  /*   } else { */
  /*     //printf("** received from [%s]:%d:\n  ",addr,ntohs(src.sin6_port)); */
  /*   } */
  /*   *\/ */
  /*   coap_show_pdu( node->pdu ); */

  /*   return 0; */
  /* error: */
  /*   coap_delete_node(node); */
  /*   return -1; */
  /* } */

 str set_str_s(str strobj, jstring jstr, jint len) {
   //char *coap_char = coap_malloc(strobj.length+1);

   (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);
#ifdef ANDROID
   int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
   int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
   if (ret >= 0) {
     //printf("INF: str1\n");
     memcpy(strobj.s, (char *)(*jenv)->GetStringUTFChars(jenv, jstr, NULL), len);
     //printf("INF: str2\n");
     memset(strobj.s + len, 0, 1);
     //printf("INF: str3\n");
     strobj.length = len;
     //printf("INF: str '%s'\n", strobj.s);
     return strobj;
   } else {
     //printf("ERR: could not attach to thread.\n");
     return ;
     //Exception?
   }
 }

  str* create_str(jstring jstr, int jstrlen) {
    char *coap_char;
    str* coap_str = coap_malloc(sizeof(str));//{ 0, NULL };

    //fprintf(stderr, "create_str");

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);
#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {
      //fprintf(stderr, "create_str1\n");
      coap_char = (char *)(*jenv)->GetStringUTFChars(jenv, jstr, NULL);

      //fprintf(stderr, "create_str2\n");
      coap_str->length = jstrlen;
      //fprintf(stderr, "create_str3\n");
      coap_str->s = (unsigned char *)coap_malloc(coap_str->length + 1);
      //fprintf(stderr, "create_str4\n");

      //memcpy(coap_str->s, jstr, coap_str->length + 1);
      memcpy(coap_str->s, coap_char, coap_str->length);
      //fprintf(stderr, "create_str5\n");
      memset(coap_str->s + coap_str->length, 0, 1);
      //fprintf(stderr, "create_str6\n");

      return coap_str;
    } else {
      //printf("ERR: could not attach to thread.\n");
      return NULL;
    }
  }

  /*
  int htons(int port) {
    int nsport = htons(port);
    return nsport;
  }
  */

  struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr) {
    char *stAddr;

    //printf("INF: sockaddr_in6_create()\n");

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);
#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {
      stAddr = (char *)(*jenv)->GetStringUTFChars(jenv, addr, NULL);
      if (stAddr == NULL) {
	//printf("ERR: stAddr == NULL\n");
	//fflush(stdout);
	SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Not able to allocate String.");
	return NULL;
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
    } else {
      //printf("ERR: could not attach to thread.\n");
      return NULL;
    }
  }

  jstring get_addr(struct sockaddr_in6 src) {
    static char cAddr[INET6_ADDRSTRLEN];
    //printf("INF: get_addr()\n");

    (*cached_jvm)->GetEnv(cached_jvm, (void **)&jenv, JNI_VERSION_1_4);
#ifdef ANDROID
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, &jenv, NULL);
#else
    int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&jenv, NULL);
#endif
    if (ret >= 0) {
      if ( inet_ntop(src.sin6_family, &src.sin6_addr, cAddr, INET6_ADDRSTRLEN) == 0 ) {
	//printf("INF: get_addr() inet_ntop fail\n");
	SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Not able to do address conversion.");
      } else {
	//printf("INF: get_addr() inet_ntop success\n");
      }

      jstring stAddr = (*jenv)->NewStringUTF(jenv, cAddr);

      return stAddr;
    } else {
      //printf("ERR: could not attach to thread.\n");
      return NULL;
    }
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


//server
void register_handler(coap_resource_t *r, unsigned char method , jobject client_server);

coap_context_t *get_context(const char *node, const char *port);

//client
void register_response_handler(coap_context_t *ctx, jobject client_server);
void deregister_response_handler(coap_context_t *ctx, jobject client_server);

int resolve_address(const str *server, struct sockaddr *dst);


str set_str_s(str strobj, jstring jstr, jint len);
//str* create_str(jstring jstr, int jstrlen);
//int htons(int port);

struct sockaddr_in6 *sockaddr_in6_create(int family, int port, jstring addr);
jstring get_addr(struct sockaddr_in6 src);
jint get_port(struct sockaddr_in6 src);
jbyteArray get_bytes(coap_pdu_t pdu);
//void coap_read(coap_context_t *ctx);
//void coap_read(coap_context_t *ctx, struct sockaddr_in6 src, jbyteArray receivedData, int length);
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
