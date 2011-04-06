/* File: subscribe.i */
%module subscribe
%{
#include "subscribe.h"
%}

%{
typedef coap_resource_t swig_coap_resource_t;
typedef coap_subscription_t swig_coap_subscription_t;
%}

void coap_check_resource_list(coap_context_t *context);
void coap_check_subscriptions(coap_context_t *context);
coap_key_t coap_add_resource(coap_context_t *context, coap_resource_t *);
int coap_delete_resource(coap_context_t *context, coap_key_t key);
coap_subscription_t *coap_new_subscription(coap_context_t *context,
					   const coap_uri_t *resource,
					   const struct sockaddr_in6 *subscriber,
					   time_t expiry);
coap_key_t coap_add_subscription(coap_context_t *context,
				 coap_subscription_t *subscription);
coap_subscription_t * coap_find_subscription(coap_context_t *context,
					     coap_key_t hashkey,
					     struct sockaddr_in6 *subscriber,
					     str *token);
int coap_delete_subscription(coap_context_t *context,
			     coap_key_t hashkey,
			     struct sockaddr_in6 *subscriber);
coap_key_t coap_uri_hash(const coap_uri_t *uri);
coap_key_t coap_subscription_hash(coap_subscription_t *subscription);
coap_resource_t *coap_get_resource_from_key(coap_context_t *ctx, coap_key_t key);
coap_resource_t *coap_get_resource(coap_context_t *ctx, coap_uri_t *uri);
