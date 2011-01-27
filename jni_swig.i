/* File: coap_swig.i */
%module CoapSwig
%{
#include "de_tzi_coap_Coap.h"
#include "coap.h"
%}

%native (JNIRegisterMessageHandler) void JNIRegisterMessageHandler(char *);
%{
JNIEXPORT void JNICALL Java_de_tzi_coap_jni_CoapSwigJNI_JNIRegisterMessageHandler(JNIEnv *, jobject, jstring);
%}
