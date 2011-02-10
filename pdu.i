/* File: pdu.i */
%module pdu
%{
#include "pdu.h"
%}

/*%include "datatype.i"*/

%{
typedef coap_hdr_t 	swig_coap_hdr;
typedef coap_pdu_t 	swig_coap_pdu;
typedef coap_opt_t 	swig_coap_opt;
typedef coap_option swig_coap_option;
%}

coap_pdu_t *coap_new_pdu();
void coap_delete_pdu(coap_pdu_t *);
int coap_add_option(coap_pdu_t *pdu, char type, unsigned int len, char *data);
/*int coap_add_option(coap_pdu_t *pdu, char type, int len, char *data);*/
coap_opt_t *coap_check_option(coap_pdu_t *pdu, char type);
int coap_check_critical(coap_pdu_t *pdu, coap_opt_t **option);

int coap_add_data(coap_pdu_t *pdu, unsigned int len, const unsigned char *data);
/*int coap_add_data(coap_pdu_t *pdu, int len, char *data);*/

int coap_get_data(coap_pdu_t *pdu, unsigned int *len, unsigned char **data);
/*int coap_get_data(coap_pdu_t *pdu,  int *len,  char **data);*/
/*int coap_get_data(coap_pdu_t *pdu,  int len, char *data);*/

int coap_get_request_uri(coap_pdu_t *pdu, coap_uri_t *result);
