all: mylib.so server

mylib.o: mylib.c
#gcc -Wall -fPIC -DPIC -c mylib.c -L../lib -ldirtree
	gcc -Wall -fPIC -DPIC -c mylib.c 

mylib.so: mylib.o util.o
	ld -shared -o mylib.so mylib.o util.o -ldl

server: server.c util.o
	gcc -Wall -pthread -o server server.c util.o

util.o: util.c util.h
	gcc -Wall -fPIC -DPIC -c -o util.o util.c -L../lib -ldirtree

clean:
	rm -f *.o *.so server

.PHONY: all clean
