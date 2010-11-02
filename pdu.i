/* File: pdu.i */
%module pdu
%{
#include "pdu.h"
%}

coap_pdu_t *coap_new_pdu();
void coap_delete_pdu(coap_pdu_t *);
int coap_add_option(coap_pdu_t *pdu, unsigned char type, unsigned int len, const unsigned char *data);
coap_opt_t *coap_check_option(coap_pdu_t *pdu, unsigned char type);
int coap_check_critical(coap_pdu_t *pdu, coap_opt_t **option);
int coap_add_data(coap_pdu_t *pdu, unsigned int len, const unsigned char *data);
int coap_get_data(coap_pdu_t *pdu, unsigned int *len, unsigned char **data);
int coap_get_request_uri(coap_pdu_t *pdu, coap_uri_t *result);
