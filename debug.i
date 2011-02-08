/* File: debug.i */
%module debug
%{
#include "debug.h"
%}

%include <datatype.i>

extern void coap_show_pdu(coap_pdu_t *);
