/* File: coap_swig.i */
%module CoapSwig
%{
#include "de_tzi_coap_Coap.h"
%}

%include <datatype.i>

%native (JNIRegisterMessageHandler) void JNIRegisterMessageHandler(char *);
%{
JNIEXPORT void JNICALL Java_de_tzi_coap_jni_CoapSwigJNI_JNIRegisterMessageHandler(JNIEnv *, jobject, jstring);
%}

%native (JNIPDUCoapSend) void JNIPDUCoapSend(int hdr_type, int hdr_code, int hdr_id,  int port, char *);
%{
JNIEXPORT void JNICALL Java_de_tzi_coap_jni_CoapSwigJNI_JNIPDUCoapSend(JNIEnv *, jobject, jint, int, int, jint, jstring);
%}
