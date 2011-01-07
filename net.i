/* File: net.i */
%module net
%{
#include "net.h"
%}

%typemap(jni) in_port_t "jint"
%typemap(jtype) in_port_t "int"
%typemap(jstype) in_port_t "int"

%typemap(out) in_port_t %{ $result = (jint)$1; %}
%typemap(in) in_port_t "(in_port_t)$1"

int coap_insert_node(coap_queue_t **queue, coap_queue_t *node,
		     int (*order)(coap_queue_t *, coap_queue_t *node) );
int coap_delete_node(coap_queue_t *node);
void coap_delete_all(coap_queue_t *queue);
coap_queue_t *coap_new_node();

void coap_register_message_handler( coap_context_t *context, coap_message_handler_t handler);
//void coap_register_error_handler( coap_context_t *context, coap_message_handler_t handler);

coap_queue_t *coap_peek_next( coap_context_t *context );
coap_queue_t *coap_pop_next( coap_context_t *context );

coap_context_t *coap_new_context(int port);
void coap_free_context( coap_context_t *context );

coap_tid_t coap_send_confirmed( coap_context_t *context, const struct sockaddr_in6 *dst, coap_pdu_t *pdu );
coap_tid_t coap_send( coap_context_t *context, const struct sockaddr_in6 *dst, coap_pdu_t *pdu );
coap_tid_t coap_retransmit( coap_context_t *context, coap_queue_t *node );

int coap_read( coap_context_t *context );

int coap_remove_transaction( coap_queue_t **queue, coap_tid_t id );
coap_queue_t *coap_find_transaction(coap_queue_t *queue, coap_tid_t id);

void coap_dispatch( coap_context_t *context );

int coap_can_exit( coap_context_t *context );
