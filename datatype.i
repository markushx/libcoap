/* File: datatype.i */
%module datatype
%{
#include "coap.h"
%}

typedef struct coap_linkedlistnode{
  struct coap_linkedlistnode *next;
  void *data;
/*  void (*delete)(void *);*/
} coap_list_t;


typedef struct coap_listnode {
  struct coap_listnode *next;
  time_t t;			/* when to send PDU for the next time */
  unsigned char retransmit_cnt;	/* retransmission counter, will be removed when zero */
  struct sockaddr_in6 remote;	/* remote address */
  coap_pdu_t *pdu;		/* the CoAP PDU to send */
} coap_queue_t;

typedef struct {
  coap_list_t *resources, *subscriptions; /* FIXME: make these hash tables */
  coap_queue_t *sendqueue, *recvqueue; /* FIXME make these coap_list_t */
  int sockfd;			/* send/receive socket */
/*  void ( *msg_handler )( void *, coap_queue_t *, void *);*/
/*   coap_queue_t *asynresqueue;*/
} coap_context_t;

typedef struct {
  unsigned int version;
  unsigned int type;
  unsigned int optcnt;
  unsigned int code;
  unsigned short id;
}coap_hdr_t;

typedef struct {
  coap_hdr_t *hdr;
  unsigned short length;
  coap_list_t *options;
  char *data;		
} coap_pdu_t;


typedef union {
  struct {		        
    unsigned int delta:4;      
    unsigned int length:4;	
  } sval;
  struct {			
    unsigned int delta:4;      
    unsigned int flag:4;	
    unsigned int length:8;
  } lval;
} coap_opt_t;

typedef int coap_tid_t;
