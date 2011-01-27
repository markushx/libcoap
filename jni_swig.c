#include <jni.h>
#include <stdio.h>

#include "de_tzi_coap_Coap.h" 
#include "coap.h"

/* prototype */
void message_handler_proxy(JNIEnv *env, jobject obj, jobject ctx, jobject node, jstring data);


/* Global variables */
JavaVM *cached_jvm;
jmethodID MID;
jfieldID fid_ctx, fid_node, fid_data;	
jobject jctx, jnode;
jobject ctx_obj, node_obj;
jstring data;
coap_context_t *ctx;
jobject cached_obj;

/* JNI on Load, caching variables here */
JNIEXPORT jint JNICALL 
JNI_OnLoad( JavaVM *jvm, void *reserved ){
	JNIEnv *env;
	jclass cls;

	printf("1. Enter C, JNI_OnLoad called: caching MID, JVM,...\n" );
    cached_jvm = jvm;  /* cache the JavaVM pointer */
 	
 	/* caching JVM */
    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
		printf("JNI verson not suppported \n");
        return JNI_ERR; /* JNI version not supported */
    }

	/* caching class */
	cls = (*env)->FindClass(env,"de/tzi/coap/Coap");
	if (cls==NULL) {return JNI_ERR; }
	

	/* caching method ID of message_handler */
	MID = (*env)->GetStaticMethodID(env,cls, "message_handler",
	"(Lde/tzi/coap/jni/SWIGTYPE_p_coap_context_t;Lde/tzi/coap/jni/SWIGTYPE_p_coap_queue_t;Ljava/lang/String;)V");
	if (MID==NULL)	{ return JNI_ERR;}

	return JNI_VERSION_1_2;
}

/* JNI on Unload */
JNIEXPORT void JNICALL 
JNI_OnUnLoad( JavaVM *jvm, void *reserved ){
	return;	
}


/* register message handler */
JNIEXPORT void JNICALL 
Java_de_tzi_coap_jni_CoapSwigJNI_JNIRegisterMessageHandler (JNIEnv *env, jobject obj,jstring st) {
	jclass cls;
	jclass ctx_cls;
	jclass node_cls;
	jmethodID midCons1;
	jfieldID ctx_fid;
	jfieldID node_fid;
	jlong ctx_ptr;
	jlong node_ptr;
	jmethodID midCons2;
	const char *str;
	coap_message_handler_t handler;

	printf("2. Get Objects from Java \n");
	cached_obj = obj;
	/* Get Object from Java: ctx, node and data */
	/* context */
	ctx_cls = (*env)->FindClass(env,"Lde/tzi/coap/jni/SWIGTYPE_p_coap_context_t;");
	midCons1 = (*env)->GetMethodID(env, ctx_cls, "<init>", "()V");
	ctx_fid = (*env)->GetFieldID(env,ctx_cls,"swigCPtr","J");
	ctx_ptr =(*env)->GetLongField(env, obj, ctx_fid);
	/*
	if (ctx_cls!=NULL) printf("ctx_cls = %x \n", (int)ctx_cls);
	if (ctx_fid!=NULL) printf("ctx_fid = %x \n", (int)ctx_fid);
	printf("ctx_ptr = %x \n", (int)ctx_ptr);
	*/
	
	/* queue node*/
	node_cls = (*env)->FindClass(env,"Lde/tzi/coap/jni/SWIGTYPE_p_coap_queue_t;");
	midCons2 = (*env)->GetMethodID(env, node_cls, "<init>", "()V");
	node_fid = (*env)->GetFieldID(env,node_cls,"swigCPtr","J");
	node_ptr =(*env)->GetLongField(env, obj, node_fid);
	/*
	if (node_cls!=NULL) printf("node_cls = %x \n", (int)node_cls);
	if (node_fid!=NULL) printf("node_fid = %x \n", (int)node_fid);
	printf("node_ptr = %x \n", (int)node_ptr);
	*/
	
	/*data*/
	/*cls = (*env)->GetObjectClass(env, obj);
	cls = (*env)->FindClass(env,"de/tzi/coap/Coap");	
	if (cls!=NULL)		printf("CLASS FOUND \n");
	fid_data= (*env)->GetFieldID(env,cls,"sdata","Ljava/lang/String;");
	if (fid_data!=NULL) 	printf("DATA_ID FOUND \n");		;
	data =(*env)->GetObjectField(env, obj, fid_data);
	str = (*env)->GetStringUTFChars(env, data, NULL);
	if (str==NULL) return;
	printf("Data = %s \n", str);
	(*env)->ReleaseStringUTFChars(env, data, str);	
	*/
	
	/*Reconstruct Object*/
	printf("3. Reconstruct Objects \n");	
    ctx_obj = (*env)->NewObject(env, ctx_cls, midCons1, ctx_ptr, NULL);
	ctx = (coap_context_t *)ctx_obj; /*caching ctx for register_message_handler() */
    node_obj = (*env)->NewObject(env, node_cls, midCons2, node_ptr, NULL);
	data = st;
	if (ctx_obj!=NULL)		printf("CTX-OBJ = %x \n", ctx_obj);
	if (node_obj!=NULL)		printf("NODE-OBJ = %x \n", node_obj);
	str = (*env)->GetStringUTFChars(env, data, NULL);
	printf("DATA = %s \n", str);	
	
	/* access C implementation here*/
	printf("4. Get function pointer of message_handler_proxy() \n");	
	handler = (coap_message_handler_t )(&message_handler_proxy);
	/*printf("ctx = %x \n", ctx);*/
	printf("&message_handler_proxy = %x \n", (int)&message_handler_proxy);
	printf("handler = %x \n", (int)handler);
	
	/* call C functions */
	coap_register_message_handler(ctx, handler);
	message_handler_proxy(env, obj, ctx_obj, node_obj, data);	

	/*Free local refs*/
	(*env)->DeleteLocalRef(env,ctx_obj);
	(*env)->DeleteLocalRef(env,node_obj);	
	printf("Exit C \n");
}

/* Call Java message_handler callback */
void JNICALL message_handler_proxy(JNIEnv *env, jobject obj, jobject ctx, jobject node, jstring data) {	
	JNIEnv *env_temp;
	int ret;

	/* call Java callback with reconstructed Obj*/
	printf("5. In C message_handler_proxy(): call message_handler() Java callback \n");
	(*cached_jvm)->GetEnv(cached_jvm, (void **)&env_temp, JNI_VERSION_1_2);
	ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&env_temp, NULL);
	if (ret >= 0)
		 (*env_temp)->CallVoidMethod(env_temp, obj, MID, ctx, node, data);	
	else 
		printf("Java callback failed \n");
}

