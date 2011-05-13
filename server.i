%module server

%{
#include "net.h"
#include "pdu.h"
#include "encode.h"
#include "subscribe.h"
#include "coap.h"

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <limits.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/stat.h>
#include <dirent.h>
#include <errno.h>
#include <signal.h>

  JavaVM *cached_jvm, *cached_s_jvm, *cached_c_jvm;
  JNIEnv *env, *env_s, *env_c;
  jmethodID MID;
  jstring stData;

  coap_context_t  *ctx; /*, *result_ctx;*/
  coap_pdu_t *pdu;
  coap_queue_t *nextpdu, *result_node;

  fd_set readfds;
  struct timeval tv, *timeout;
  int result;
  time_t now;
  int opt;
  char *group = NULL;

  struct sockaddr_in6 *p;

  int socket_port = COAP_DEFAULT_PORT;
  static int quit = 0;
  static char resource_buf[20000];


#define COAP_RESOURCE_CHECK_TIME 2
#define options_start(p) ((coap_opt_t *) ( (unsigned char *)p->hdr + sizeof ( coap_hdr_t ) ))
#define options_end(p, opt) {			\
    unsigned char opt_code = 0, cnt;		       \
    *opt = options_start( result_node->pdu );	       \
    for ( cnt = (p)->hdr->optcnt; cnt; --cnt ) {	\
      opt_code += COAP_OPT_DELTA(**opt);				\
      *opt = (coap_opt_t *)( (unsigned char *)(*opt) + COAP_OPT_SIZE(**opt)); \
    }									\
  }

  JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *jvm, void *reserved ){
    jclass cls;

    printf("JNI_OnLoad called: caching JVM, ENV \n" );
    cached_jvm = jvm;
    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
      return JNI_ERR;
    }
    else

      cls = (*env)->FindClass(env,"de/tzi/coap/Server");
    if (cls!=NULL) {
      MID = (*env)->GetStaticMethodID(env,cls, "MessageHandler","()V");
      cached_s_jvm = jvm;
      (*cached_s_jvm)->GetEnv(cached_s_jvm, (void **)&env_s, JNI_VERSION_1_2);
    }

    cls = (*env)->FindClass(env,"de/tzi/coap/Client");
    if (cls!=NULL) {
      cached_c_jvm = jvm;
      (*cached_c_jvm)->GetEnv(cached_c_jvm, (void **)&env_c, JNI_VERSION_1_2);
    }

    return JNI_VERSION_1_2;
  }

  JNIEXPORT void JNICALL JNI_OnUnLoad( JavaVM *jvm, void *reserved ){
    return;
  }

  struct sockaddr_in6 *s_socket6_create(int family, int port, jstring addr) {
    char *stAddr;

    stAddr = (char *)(*env)->GetStringUTFChars(env, addr, NULL);
    if (stAddr==NULL) return;

    struct sockaddr_in6 *p = (struct sockaddr_in6 *) malloc(sizeof(struct sockaddr_in6));

    p->sin6_family = family;
    p->sin6_port = port;
    inet_pton(AF_INET6, stAddr, &(p->sin6_addr) );


    /*result_ctx = coap_new_context(COAP_DEFAULT_PORT);*/
    result_node = coap_new_node();
    result_node->pdu = coap_new_pdu();

    (*env)->ReleaseStringUTFChars(env, addr, stAddr);
    return p;
  }

  void s_socket6_free(struct sockaddr_in6 *p) {
    free(p);
    /*free(result_ctx);*/
    free(result_node);
  }

  void JNICALL message_handler(JNIEnv *env, jobject obj) {
    JNIEnv *env_temp;

    printf("Enter msg handler \n");
    /*printf("Code = %d \n", result_node->pdu->hdr->code);*/
    (*cached_s_jvm)->GetEnv(cached_s_jvm, (void **)&env_temp, JNI_VERSION_1_2);
    int ret = (*cached_s_jvm)->AttachCurrentThread(cached_s_jvm, (void **)&env_temp, NULL);
    if (ret >= 0) {
      (*env_temp)->CallStaticVoidMethod(env_temp, obj, MID);
    }
  }

  void server_read(coap_context_t *ctx) {
    static char buf[COAP_MAX_PDU_SIZE];
    ssize_t bytes_read;
    static struct sockaddr_in6 src;
    socklen_t addrsize = sizeof src;
    coap_queue_t *node;
    coap_opt_t *opt;

#ifndef NDEBUG
    static char addr[INET6_ADDRSTRLEN];
#endif

    bytes_read = recvfrom( ctx->sockfd, buf, COAP_MAX_PDU_SIZE, 0, (struct sockaddr *)&src, &addrsize );

    node = coap_new_node();
    if ( !node )     return;

    node->pdu = coap_new_pdu();
    if ( !node->pdu ) {
      coap_delete_node( node );
      return;
    }

    time( &node->t );
    memcpy( &node->remote, &src, sizeof( src ) );

    memcpy( node->pdu->hdr, buf, bytes_read );
    node->pdu->length = bytes_read;

    /* finally calculate beginning of data block */
    options_end( node->pdu, &opt );

    if ( (unsigned char *)node->pdu->hdr + node->pdu->length < (unsigned char *)opt )
      node->pdu->data = (unsigned char *)node->pdu->hdr + node->pdu->length;
    else
      node->pdu->data = (unsigned char *)opt;

    result_node->pdu = node->pdu;
    result_node->pdu->data = node->pdu->data;
    result_node->remote = node->remote;

    /* and add new node to receive queue */
    coap_insert_node( &ctx->recvqueue, node, order_transaction_id );
#ifndef NDEBUG
    if ( inet_ntop(src.sin6_family, &src.sin6_addr, addr, INET6_ADDRSTRLEN) == 0 ) {
      perror("coap_read: inet_ntop");
    } else {
      debug("** received from [%s]:%d:\n  ",addr,ntohs(src.sin6_port));
    }
    coap_show_pdu( result_node->pdu );
    /*show_data(result_node->pdu);*/
#endif
  }

  int get_hdr_version () {	return result_node->pdu->hdr->version;}
  int get_hdr_code () {	return result_node->pdu->hdr->code;}
  int get_hdr_type () {return result_node->pdu->hdr->type;}
  int get_hdr_optcnt () {	return result_node->pdu->hdr->optcnt;}
  int get_hdr_id () {	return ntohs(result_node->pdu->hdr->id);}

  jstring get_opt_data(int index) {
    unsigned char cnt;
    coap_opt_t *opt;
    unsigned char opt_code = 0;
    coap_pdu_t *pdu;
    static unsigned char buf[COAP_MAX_PDU_SIZE];

    pdu = coap_new_pdu();
    if (! pdu )    return;
    pdu = result_node->pdu;

    opt = options_start( pdu );
    for ( cnt = pdu->hdr->optcnt; cnt; --cnt ) {
      opt_code += COAP_OPT_DELTA(*opt);
      if (cnt==index) {
	print_readable( COAP_OPT_VALUE(*opt), COAP_OPT_LENGTH(*opt), buf, COAP_MAX_PDU_SIZE, 0 );
      }
      opt = (coap_opt_t *)( (unsigned char *)opt + COAP_OPT_SIZE(*opt) );
    }
    jstring str = (*env)->NewStringUTF(env, buf);
    return str;
  }

  jstring get_pdu_data() {
    coap_pdu_t *pdu;
    static unsigned char buf[COAP_MAX_PDU_SIZE];

    pdu = coap_new_pdu();
    if (! pdu )    return;
    pdu = result_node->pdu;
    unsigned int len = (int)( (unsigned char *)pdu->hdr + pdu->length - pdu->data );
    print_readable( pdu->data, len, buf, COAP_MAX_PDU_SIZE, 0 );
    /*printf("GET Data = len = %d, '%s' \n",len, buf);*/

    jstring str = (*env)->NewStringUTF(env, buf);
    return str;
  }

  void send_response(coap_context_t *context, coap_pdu_t *pdu_ptr) {
    /*ctx = context;*/
    pdu = pdu_ptr;
    coap_send(context, &result_node->remote, pdu);
  }

  void
    handle_sigint(int signum) {
    quit = 1;
  }

  /* Check if provided path name is a valid CoAP URI path. */
  int
    is_valid(char *prefix, unsigned char *path, unsigned int length) {
    enum { START, PATH, DOT, DOTDOT } state;

    if (!path || length < strlen(prefix) ||
	strncmp((char *)path, prefix, strlen(prefix)) != 0)
      return 0;

    path += strlen(prefix);
    length -= strlen(prefix);
    if ( length && *path == '/' ) {
      state = START;
      ++path;
      --length;
    } else
      state = PATH;

    while (length) {
      switch (state) {
      case START:
	switch (path[0]) {
	case '.': state = DOT; break;
	case '/': return 0;
	default: state = PATH;
	}
	break;
      case PATH:
	if (path[0] == '/')
	  state = START;
	break;
      case DOT:
	switch (path[0]) {
	case '.': state = DOTDOT; break;
	case '/': return 0;
	default: state = PATH;
	}
	break;
      case DOTDOT:
	if (path[0] == '/')
	  return 0;
	state = PATH;
	break;
      }
      ++path;
      --length;
    }

    return state != DOT && state != DOTDOT;
  }


  int
    resource_time(coap_uri_t *uri,
		  unsigned char *mediatype, unsigned int offset,
		  unsigned char *buf, unsigned int *buflen,
		  int *finished) {
    static unsigned char b[400];
    size_t maxlen;
    time_t now;
    struct tm *tlocal;

    time(&now);
    tlocal = localtime(&now);

    *finished = 1;

    if ( !tlocal ) {
      *buflen = 0;
      return COAP_RESPONSE_500;
    }

    switch (*mediatype) {
    case COAP_MEDIATYPE_ANY :
    case COAP_MEDIATYPE_TEXT_PLAIN :
      *mediatype = COAP_MEDIATYPE_TEXT_PLAIN;
      maxlen = strftime(resource_buf, sizeof(b), "%b %d %H:%M:%S", tlocal);
      break;
    case COAP_MEDIATYPE_TEXT_XML :
    case COAP_MEDIATYPE_APPLICATION_XML :
      maxlen = strftime(resource_buf, sizeof(b), "<datetime>\n  <date>%Y-%m-%d</date>\n  <time>%H:%M:%S</time>\n  </tz>%S</tz>\n</datetime>", tlocal);
      break;
    default :
      *buflen = 0;
      return COAP_RESPONSE_415;
    }

    if ( offset > maxlen ) {
      *buflen = 0;
      return COAP_RESPONSE_400;
    } else if ( offset + *buflen > maxlen )
      *buflen = maxlen - offset;

    memcpy(buf, resource_buf + offset, *buflen);

    *finished =offset + *buflen == maxlen;
    return COAP_RESPONSE_200;
  }

  int
    resource_lipsum(coap_uri_t *uri,
		    unsigned char *mediatype, unsigned int offset,
		    unsigned char *buf, unsigned int *buflen,
		    int *finished) {
    static unsigned char verylargebuf[] = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris fermentum, lacus elementum venenatis aliquet, tortor risus laoreet sapien, a vulputate libero dolor ut odio. Vivamus congue elementum fringilla. Suspendisse porttitor, lectus sed gravida volutpat, dolor magna gravida massa, id fermentum lectus mi quis erat. Suspendisse lacinia, libero in euismod bibendum, magna nisi tempus lacus, eu suscipit augue nisi vel nulla. Praesent gravida lacus nec elit vestibulum sit amet rhoncus dui fringilla. Quisque diam lacus, ullamcorper non consectetur vitae, pellentesque eget lectus. Vestibulum velit nulla, venenatis vel mattis at, scelerisque nec mauris. Nulla facilisi. Mauris vel erat mi. Morbi et nulla nibh, vitae cursus eros. In convallis, magna egestas dictum porttitor, diam magna sagittis nisi, rhoncus tincidunt ligula felis sed mauris. Pellentesque pulvinar ante id velit convallis in porttitor justo imperdiet. Curabitur viverra placerat tincidunt. Vestibulum justo lacus, sollicitudin in facilisis vel, tempus nec erat. Duis varius viverra aliquet. In tempor varius elit vel pharetra. Sed mattis, quam in pulvinar ullamcorper, est ipsum tempor dui, at fringilla magna sem in sapien. Phasellus sollicitudin ornare sem, nec porta libero tempus vitae. Maecenas posuere pulvinar dictum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Cras eros mauris, pulvinar tempor facilisis ut, condimentum in magna. Nullam eget ipsum sit amet lacus massa nunc.<EOT>";
    unsigned int maxlen = sizeof(verylargebuf) - 1;

    switch (*mediatype) {
    case COAP_MEDIATYPE_ANY :
    case COAP_MEDIATYPE_TEXT_PLAIN :
      *mediatype = COAP_MEDIATYPE_TEXT_PLAIN;
      break;
    default :
      *buflen = 0;
      *finished = 1;
      return COAP_RESPONSE_415;
    }

    if ( offset > maxlen ) {
      *buflen = 0;
      return COAP_RESPONSE_400;
    } else if ( offset + *buflen > maxlen )
      *buflen = maxlen - offset;

    memcpy(buf, verylargebuf + offset, *buflen);

    *finished = offset + *buflen == maxlen;
    return COAP_RESPONSE_200;
  }

  int
    _resource_from_dir(char *filename,
		       unsigned char *mediatype, unsigned int offset,
		       unsigned char *buf, unsigned int *buflen,
		       int *finished) {
    DIR *dir;
    struct dirent *dirent;
    size_t namelen, overhead;
    int pos = 0;

    *finished = 1;

    switch (*mediatype) {
    case COAP_MEDIATYPE_ANY :
    case COAP_MEDIATYPE_APPLICATION_LINK_FORMAT:
      *mediatype = COAP_MEDIATYPE_APPLICATION_LINK_FORMAT;
      break;
      /* case COAP_MEDIATYPE_TEXT_PLAIN: */
      /*   break; */
    default:
      *buflen = 0;
      return COAP_RESPONSE_415;
    }

    if ( (dir = opendir(filename)) == NULL ) {
      perror("_resource_from_dir: opendir");
      *buflen = 0;
      return COAP_RESPONSE_404;
    }

    overhead = strlen(filename) + 10;

    errno = 0;
    while ( (dirent = readdir(dir)) ) {
      namelen = strlen(dirent->d_name);

      /* skip '.' and '..' as they are not allowed in CoAP URIs */
      if ( dirent->d_name[0] == '.' ) {
	if ( namelen == 1 || (namelen == 2 && dirent->d_name[1] == '.') )
	  continue;
      }

      if (pos + overhead + namelen * 2 > sizeof(resource_buf) - 1)
	break;			/* broken */

      if ( pos + overhead + namelen * 2 < offset) {
	offset -= overhead + namelen * 2;
      } else {
	pos += sprintf(resource_buf + pos, "</%s/%s>;n=\"%s\",",
		       filename, dirent->d_name, dirent->d_name);
      }

      if ( pos > offset + *buflen )
	break;
    }

    if (errno != 0)
      goto error;

    closedir(dir);

    if ( pos <= offset ) {
      *buflen = 0;
      return COAP_RESPONSE_400;
    }

    if ( (offset < pos) && (pos <= offset + *buflen) ) {
      *buflen = pos - offset - 1;
      *finished = 1;
    } else
      *finished = 0;

    memcpy(buf, resource_buf + offset, *buflen);

    return COAP_RESPONSE_200;

  error:
    perror("_resource_from_dir: readdir");
    closedir(dir);

    return COAP_RESPONSE_500;
  }

  int
    resource_from_file(coap_uri_t *uri,
		       unsigned char *mediatype, unsigned int offset,
		       unsigned char *buf, unsigned int *buflen,
		       int *finished) {
    static char filename[FILENAME_MAX+1];
    struct stat statbuf;
    FILE *file;
    int code = COAP_RESPONSE_500;	/* result code */

    if (uri) {
      memcpy(filename, uri->path.s, uri->path.length);
      filename[uri->path.length] = '\0';

      if (!is_valid("", (unsigned char *)filename, uri->path.length) ) {
	fprintf(stderr, "dropped invalid URI '%s'\n", filename);
	code = COAP_RESPONSE_404;
	goto error;
      }
    } else {
      fprintf(stderr, "dropped NULL URI\n");
      code = COAP_RESPONSE_404;
      goto error;
    }

    if (stat(filename, &statbuf) < 0) {
      perror("resource_from_file: stat");
      code = COAP_RESPONSE_404;
      goto error;
    }

    if ( S_ISDIR(statbuf.st_mode) ) {
      /* handle directory if mediatype allows */
      return _resource_from_dir(filename, mediatype, offset, buf, buflen, finished);
    }

    if ( !S_ISREG(statbuf.st_mode) ) {
      fprintf(stderr,"%s not a regular file, skipped\n", filename);
      code = COAP_RESPONSE_404;
      goto error;
    }

    if ( offset > statbuf.st_size ) {
      code = COAP_RESPONSE_400;
      goto error;
    } else if ( offset + *buflen > statbuf.st_size )
      *buflen = statbuf.st_size - offset;

    file = fopen(filename, "r");
    if ( !file ) {
      perror("resource_from_file: fopen");
      code = COAP_RESPONSE_500;
      goto error;
    }

    if ( fseek(file, offset, SEEK_SET) < 0 ) {
      perror("resource_from_file: fseek");
      code = COAP_RESPONSE_500;
      goto error;
    }

    *buflen = fread(buf, 1, *buflen, file);
    fclose(file);

    *finished = offset + *buflen >= statbuf.st_size;

    return COAP_RESPONSE_200;

  error:
    *buflen = 0;
    *finished = 1;
    return code;
  }

#define RESOURCE_SET_URI(r,st)						\
  (r)->uri = coap_new_uri((const unsigned char *)(st), strlen(st));

#define RESOURCE_SET_DESC(r,st)			 \
  (r)->name = coap_new_string(strlen(st));	 \
  if ((r)->name) {				 \
    (r)->name->length = strlen(st);		   \
    memcpy((r)->name->s, (st), (r)->name->length); \
  }

  int
    write_file(char *filename, unsigned char *text, int length) {
    FILE *file;
    ssize_t written;

    file = fopen(filename, "w");
    if ( !file ) {
      perror("write_file: fopen");
      return 0;
    }

    written = fwrite(text, 1, length, file);
    fclose(file);

    return written;
  }

  void
    init_resources(coap_context_t *ctx) {
    static const char *u_lipsum = "/lipsum";
    static const char *d_lipsum = "some large text to test buffer sizes (<EOT> marks its end)";
    static const char *u_time = "/time";
    static const char *d_time = "server's local time and date";
    static const char *u_file = "/filestorage";
    static const char *d_file = "a single file, you can PUT things here";
    static const char *u_data = "/data-sink";
    static const char *d_data = "POSTed data is stored here";
    coap_resource_t *r;

    if ( !(r = coap_malloc( sizeof(coap_resource_t) )))
      return;

    memset(r, 0, sizeof(coap_resource_t));
    r->uri = coap_new_uri((const unsigned char *)"/" COAP_DEFAULT_URI_WELLKNOWN,
			  sizeof(COAP_DEFAULT_URI_WELLKNOWN));
    r->mediatype = COAP_MEDIATYPE_APPLICATION_LINK_FORMAT;
    r->dirty = 0;
    r->writable = 0;
    coap_add_resource( ctx, r );

    if ( !(r = coap_malloc( sizeof(coap_resource_t) )))
      return;

    memset(r, 0, sizeof(coap_resource_t));
    RESOURCE_SET_URI(r,u_lipsum);
    RESOURCE_SET_DESC(r,d_lipsum);
    r->mediatype = COAP_MEDIATYPE_TEXT_PLAIN;
    r->dirty = 1;
    r->writable = 0;
    r->data = resource_lipsum;
    r->maxage = 1209600;		/* two weeks */
    coap_add_resource( ctx, r );

    if ( !(r = coap_malloc( sizeof(coap_resource_t) )))
      return;

    memset(r, 0, sizeof(coap_resource_t));
    RESOURCE_SET_URI(r,u_time);
    RESOURCE_SET_DESC(r,d_time);
    r->mediatype = COAP_MEDIATYPE_ANY;
    r->dirty = 0;
    r->writable = 0;
    r->data = resource_time;
    r->maxage = 1;
    coap_add_resource( ctx, r );

    if ( !(r = coap_malloc( sizeof(coap_resource_t) )))
      return;

    memset(r, 0, sizeof(coap_resource_t));
    RESOURCE_SET_URI(r,u_file);
    RESOURCE_SET_DESC(r,d_file);
    r->mediatype = COAP_MEDIATYPE_ANY;
    r->dirty = 0;
    r->writable = 1;
    r->data = resource_from_file;
    write_file("filestorage",(unsigned char *)"initial text", 12);
    coap_add_resource( ctx, r );

    if ( !(r = coap_malloc( sizeof(coap_resource_t) )))
      return;

    memset(r, 0, sizeof(coap_resource_t));
    RESOURCE_SET_URI(r,u_data);
    RESOURCE_SET_DESC(r,d_data);
    r->mediatype = COAP_MEDIATYPE_APPLICATION_LINK_FORMAT;
    r->dirty = 0;
    r->writable = 0;
    r->data = resource_from_file;
    r->maxage = 10;
    coap_add_resource(ctx, r);
  }

  int
    start(struct sockaddr_in6 *p) {
    /*coap_context_t  *ctx;*/
    int port;

    printf ("Enter START\n");
    port = p->sin6_port;
    ctx = coap_new_context(port);
    if ( !ctx )    return -1;

    printf ("1. Register msg_handler \n");
    coap_register_message_handler(ctx, (void *)message_handler );

#warning "FIXME: init_resources should be done from Java"
    printf ("2. Init resource \n");
    init_resources(ctx);
    signal(SIGINT, handle_sigint);

    printf ("3. Enter loop\n");
    while ( !quit ) {
      FD_ZERO(&readfds);
      FD_SET( ctx->sockfd, &readfds );

      nextpdu = coap_peek_next( ctx );

      time(&now);
      while ( nextpdu && nextpdu->t <= now ) {
	coap_retransmit( ctx, coap_pop_next( ctx ) );
	nextpdu = coap_peek_next( ctx );
      }

      if ( nextpdu && nextpdu->t <= now + COAP_RESOURCE_CHECK_TIME ) {
	tv.tv_usec = 0;
	tv.tv_sec = nextpdu->t - now;
	timeout = &tv;
      } else {
	tv.tv_usec = 0;
	tv.tv_sec = COAP_RESOURCE_CHECK_TIME;
	timeout = &tv;
      }
      result = select( FD_SETSIZE, &readfds, 0, 0, timeout );

      if ( result < 0 ) {
	if (errno != EINTR)
	  perror("select");
      } else if ( result > 0 ) {
	if ( FD_ISSET( ctx->sockfd, &readfds ) ) {
	  server_read(ctx);
	  /*coap_read( ctx );*/
	  coap_dispatch( ctx );
	}
      } else {
	coap_check_resource_list( ctx );
	coap_check_subscriptions( ctx );
      }
    }

    coap_free_context( ctx );
    return 0;
  }
  %}

struct sockaddr_in6 *s_socket6_create(int family, int port, jstring addr);
void s_socket6_free(struct sockaddr_in6 *p);
int start(struct sockaddr_in6 *p);
int get_hdr_version ();
int get_hdr_code ();
int get_hdr_type ();
int get_hdr_optcnt ();
int get_hdr_id ();
jstring get_opt_data(int index);
jstring get_pdu_data();
void send_response(coap_context_t *context, coap_pdu_t *pdu_ptr);
