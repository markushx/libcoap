/* File: str.i */
%module str
%{
#include "str.h"
%}

%include <datatype.i>

str *coap_new_string(size_t size);
void coap_delete_string(str *);
