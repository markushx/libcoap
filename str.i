/* File: str.i */
%module str
%{
#include "str.h"
%}


str *coap_new_string(size_t size);
void coap_delete_string(str *);
