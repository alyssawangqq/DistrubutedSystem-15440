all: mylib.so server dirtree_test

mylib.o: mylib.c
		gcc -Wall -fPIC -DPIC -c mylib.c -L../lib

mylib.so: mylib.o
		ld -shared -o mylib.so mylib.o -ldl

server: server.c
	  gcc -Wall -o server server.c

dirtree_test: dirtree_test.c
	  gcc -L../lib -ldirtree -o dirtree_test dirtree_test.c 

clean:
		rm -f *.o *.so server dirtree_test

.PHONY: all clean
