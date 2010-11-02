/* File: debug.i */
%module debug
%{
#include "debug.h"
%}

extern void coap_show_pdu(coap_pdu_t *);
