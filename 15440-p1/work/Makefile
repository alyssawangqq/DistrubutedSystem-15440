all: mylib.so server

mylib.o: mylib.c
		gcc -Wall -fPIC -DPIC -c mylib.c -L../lib

mylib.so: mylib.o
		ld -shared -o mylib.so mylib.o -ldl
#gcc -Wall -o mylib.dylib -dynamiclib mylib.c

server: server.c
	  gcc -std=c99 -Wall -o server server.c

dirtree_test: dirtree_test.c
	  gcc -L../lib -ldirtree -o dirtree_test dirtree_test.c 

clean:
		rm -f *.o *.so *.dylib server dirtree_test

.PHONY: all clean
