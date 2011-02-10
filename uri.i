/* File: uri.i */
%module uri
%{
#include "uri.h"
%}

%{
typedef coap_uri_t swig_coap_uri_t;
%}

int coap_split_uri(unsigned char *str, coap_uri_t *uri);
coap_uri_t *coap_new_uri(const unsigned char *uri, unsigned int length);
coap_uri_t *coap_clone_uri(const coap_uri_t *uri);
