all: mylib.so server

mylib.o: mylib.c
	gcc -Wall -fPIC -DPIC -c mylib.c -L../lib -ldirtree

mylib.so: mylib.o util_nodebug.o
	ld -shared -o mylib.so mylib.o util_nodebug.o -ldl

server: server.c util.o
	gcc -Wall -o server server.c util.o

util_nodebug.o: util.c util.h
	#gcc -DNO_DEBUG_OUTPUT -Wall -fPIC -DPIC -c -o util_nodebug.o util.c
	gcc -Wall -fPIC -DPIC -c -o util_nodebug.o util.c

util.o: util.c util.h
	gcc -Wall -fPIC -DPIC -c -o util.o util.c

clean:
	rm -f *.o *.so server

.PHONY: all clean
