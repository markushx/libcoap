#include <jni.h>
#include <stdio.h>

#include "../../../de_tzi_coap_CoapTest.h" 
#include "../../../coap.h"

// prototype
void message_handler_proxy(JNIEnv *env, jobject obj);

// Global variables
JavaVM *cached_jvm;
jmethodID MID;
jfieldID fid_ctx, fid_node, fid_data;	
jobject jctx, jnode;
jobject ctx_obj, node_obj;
jstring data;
coap_context_t *ctx;

// JNI on Load, caching variables here
JNIEXPORT jint JNICALL 
JNI_OnLoad( JavaVM *jvm, void *reserved ){
	printf("1. Enter C, JNI_OnLoad called: caching MID, JVM,...\n" );
	JNIEnv *env;
	jclass cls;
    cached_jvm = jvm;  /* cache the JavaVM pointer */
 	
 	// caching JVM
    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
		printf("JNI verson not suppported \n");
        return JNI_ERR; /* JNI version not supported */
    }

	// caching class
	cls = (*env)->FindClass(env,"de/tzi/coap/CoapTest");
	if (cls==NULL) {return JNI_ERR; }

	// caching method ID of message_handler
	MID = (*env)->GetStaticMethodID(env,cls, "message_handler",
	"(Lde/tzi/coap/jni/SWIGTYPE_p_coap_context_t;Lde/tzi/coap/jni/SWIGTYPE_p_coap_queue_t;Ljava/lang/String;)V");
	if (MID==NULL)	{ return JNI_ERR;}

	return JNI_VERSION_1_2;
}

// JNI on Unload
JNIEXPORT void JNICALL 
JNI_OnUnLoad( JavaVM *jvm, void *reserved ){
	return;	
}

// register message handler
JNIEXPORT void JNICALL 
Java_de_tzi_coap_CoapTest_JNIRegisterMessageHandler (JNIEnv *env, jobject obj) {
	jfieldID fid_ctx, fid_node;	
	jobject jctx, jnode;
	jclass cls;
	const char *str;

	//Get Object from Java: ctx, node and data
	printf("2. Get Objects from Java \n");
	//context
	jclass ctx_cls = (*env)->FindClass(env,"Lde/tzi/coap/jni/SWIGTYPE_p_coap_context_t;");
	//if (ctx_cls!=NULL)		printf("CTX_CLS in C = %d \n", ctx_cls);
	jmethodID midCons1 = (*env)->GetMethodID(env, ctx_cls, "<init>", "()V");
	jfieldID ctx_fid = (*env)->GetFieldID(env,ctx_cls,"swigCPtr","J");
	//if (ctx_fid!=NULL) 		printf("CTX_FID in C = %d \n", ctx_fid);		
	jlong ctx_ptr =(*env)->GetLongField(env, obj, ctx_fid);
	//if (ctx_ptr!=NULL) 		printf("CTX_PTR in C = %ld \n", ctx_ptr);		
	
	// queue node
	jclass node_cls = (*env)->FindClass(env,"Lde/tzi/coap/jni/SWIGTYPE_p_coap_queue_t;");
	//if (node_cls!=NULL)		printf("NODE_CLS in C = %d \n", node_cls);
	jmethodID midCons2 = (*env)->GetMethodID(env, node_cls, "<init>", "()V");
	jfieldID node_fid = (*env)->GetFieldID(env,node_cls,"swigCPtr","J");
	//if (node_fid!=NULL) 		printf("NODE_FID in C = %d \n", node_fid);		
	jlong node_ptr =(*env)->GetLongField(env, obj, node_fid);
	//if (node_ptr!=NULL) 		printf("NODE_PTR in C = %d \n", node_ptr);		

	//data
	cls = (*env)->GetObjectClass(env, obj);	
	fid_data= (*env)->GetFieldID(env,cls,"data","Ljava/lang/String;");
	if (fid_data==NULL) 		return;
	data =(*env)->GetObjectField(env, obj, fid_data);
	str = (*env)->GetStringUTFChars(env, data, NULL);
	if (str==NULL) return;
	printf("Data = %s \n", str);
	(*env)->ReleaseStringUTFChars(env, data, str);	
	
	//Reconstruct Object
	printf("3. Reconstruct Objects \n");	
    ctx_obj = (*env)->NewObject(env, ctx_cls, midCons1, ctx_ptr, NULL);
	//if (ctx_obj!=NULL) 		printf("CTX_OBJ in C = %d \n", ctx_obj);		
	// caching ctx
	ctx = (coap_context_t *)ctx_obj; // caching ctx
    
    node_obj = (*env)->NewObject(env, node_cls, midCons2, node_ptr, NULL);
	//if (node_obj!=NULL) 		printf("NODE_OBJ in C = %d \n", node_obj);		
	//str = "TEST_STRING_IN_C"; // change this data and pass to Java callback
	//data = (*env)->NewStringUTF(env,str);
	
	// access C implementation here
	printf("4. Get function pointer of message_handler_proxy() \n");	
	coap_message_handler_t handler = (coap_message_handler_t )(&message_handler_proxy);
	//printf("ctx = %x \n", ctx);
	printf("&message_handler_proxy = %x \n", &message_handler_proxy);
	printf("Handler = %x \n", handler);
	coap_register_message_handler(ctx, handler);

	message_handler_proxy(env, obj);	

	//Free local refs
	(*env)->DeleteLocalRef(env,ctx_obj);
	(*env)->DeleteLocalRef(env,node_obj);	
	printf("Exit C \n");
}

// Call Java message_handler callback 
void JNICALL message_handler_proxy(JNIEnv *env, jobject obj) {	
	JNIEnv *env_temp;
		
	// call Java callback with reconstructed Obj
	printf("3. In C message_handler_proxy(): call message_handler() Java callback \n");
	(*cached_jvm)->GetEnv(cached_jvm, (void **)&env_temp, JNI_VERSION_1_2);
	int ret = (*cached_jvm)->AttachCurrentThread(cached_jvm, (void **)&env_temp, NULL);
	if (ret >= 0)
		 (*env_temp)->CallVoidMethod(env_temp, obj, MID, ctx_obj, node_obj, data);	
	else 
		printf("Java callback failed \n");
}