aclocal -I m4macros/
autoconf
./configure
make
make javaclient
make javaserver

./examples/coap-server -p 61616
java -Djava.library.path="./swig/java" -cp ./swig/java/:./examples/java/ -Xcheck:jni de.tzi.coap.Client


java -Djava.library.path="./swig/java" -cp ./swig/java/:./examples/java/ -Xcheck:jni de.tzi.coap.Server
