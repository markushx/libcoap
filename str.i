/* File: str.i */
%module str
%{
#include "str.h"
%}

%{
typedef str swig_str;
%}

str *coap_new_string(size_t size);
void coap_delete_string(str *);
