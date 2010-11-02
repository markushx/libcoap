/* File: encode.i */
%module encode
%{
#include "encode.h"
%}

unsigned int coap_decode_var_bytes(unsigned char *buf,unsigned int len);
unsigned int coap_encode_var_bytes(unsigned char *buf, unsigned int val);
