/* File: list.i */
%module list
%{
#include "list.h"
%}

int coap_insert(coap_list_t **queue, coap_list_t *node,
		int (*order)(void *, void *) );
int coap_delete(coap_list_t *node);
void coap_delete_list(coap_list_t *queue);
coap_list_t *coap_new_listnode(void *data, void (*delete)(void *) );

