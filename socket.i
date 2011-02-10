%module socket

%{
#include "net.h"
#include "pdu.h"
#include "encode.h"

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <limits.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

typedef struct socket6 {
	int           	sin6_len;      
	int           	sin6_family;   
	int       		sin6_port;     
	unsigned int    sin6_flowinfo; 
	struct in6_addr  sin6_addr;     
} Socket6;


JavaVM *cached_jvm;
JNIEnv *env;
int cached_port;
coap_context_t  *ctx;

JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *jvm, void *reserved ){
	printf("JNI_OnLoad called: caching JVM, ENV \n" );
    cached_jvm = jvm;
    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
		printf("JNI verson not suppported \n");
        return JNI_ERR; /* JNI version not supported */
    }
	return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnLoad( JavaVM *jvm, void *reserved ){
	return;	
}

Socket6 *socket6_create(int family, int port, jstring addr) {
	char *stAddr;

	stAddr = (*env)->GetStringUTFChars(env, addr, NULL);
	if (stAddr==NULL) return;

  	Socket6 *p = (Socket6 *) malloc(sizeof(Socket6));
  	p->sin6_family = family;
  	p->sin6_port = port;		
	inet_pton(AF_INET6, stAddr, &(p->sin6_addr) );  	
  	printf("sin6_family = %d, sin6_port = %d, sin6_addr = %s \n", p->sin6_family, p->sin6_port, stAddr);
	
	//caching
	cached_port = port;
		
	(*env)->ReleaseStringUTFChars(env, addr, stAddr);	
	return p;
}

coap_pdu_t * 
make_pdu( unsigned int value, int ver, int type, int opt_cnt, int code, int id, char* data ) {
  coap_pdu_t *pdu;
  unsigned char enc;
  static unsigned char buf[20];
  int len, ls;

  if ( ! ( pdu = coap_new_pdu() ) )
    return NULL;

  	pdu->hdr->version = ver;
	pdu->hdr->type = type;
   	pdu->hdr->optcnt = opt_cnt; 
 	pdu->hdr->code = code;
  	pdu->hdr->id = htons(id);

	strcpy(buf, data);
	printf("Make PDU: add to PDU , Data = %s \n", buf);
	
  	enc = COAP_PSEUDOFP_ENCODE_8_4_DOWN(value,ls);
  	coap_add_data( pdu, 1, &enc);
	
 	len = sprintf((char *)buf, "%u", COAP_PSEUDOFP_DECODE_8_4(enc));
  	if ( len > 0 ) {
    	coap_add_data( pdu, len, buf );
  	}
  	
  	return pdu;
}


void socket6_send(coap_context_t  *context, Socket6 *p, jint jhdr_ver, jint jhdr_type, jint jhdr_opt_cnt, jint jhdr_code, jint jhdr_id, jstring jdata) {
  	struct sockaddr_in6 dst;
  	coap_pdu_t  *pdu;
	int ver, type, opt_cnt, code, id, hops=16;
	char *data;
	
  	memset(&dst, 0, sizeof(struct sockaddr_in6 ));
  	dst.sin6_family = p->sin6_family;
  	dst.sin6_addr 	= p->sin6_addr;
  	dst.sin6_port 	= htons(p->sin6_port);
	
	data = (*env)->GetStringUTFChars(env, jdata, NULL);
	if (data==NULL) return;
	
	/*ctx = coap_new_context(0);*/
	ctx = context;
	
	ver 	= (int)jhdr_ver;
  	type 	= (int)jhdr_type;
	opt_cnt = (int)jhdr_opt_cnt;
  	code 	= (int)jhdr_code;
  	id 		= (int)jhdr_id;
	pdu 	= make_pdu( rand() & 0xfff, ver, type, opt_cnt, code, id, data );
	
	coap_send(ctx, &dst, pdu);
    /*coap_read(ctx);*/
	(*env)->ReleaseStringUTFChars(env, jdata, data);
}	

void socket6_receive(coap_context_t  *context) {
    coap_read(context);
}    


void socket6_free(Socket6 *p) {
	free(p);
}	
%}

Socket6 *socket6_create(int family, int port, jstring addr);
void socket6_send(coap_context_t  *context, Socket6 *p,jint jhdr_ver, jint jhdr_type, jint jhdr_opt_cnt, jint jhdr_code, jint jhdr_id, jstring jdata);
void socket6_receive(coap_context_t  *context);
void socket6_free(Socket6 *p);


