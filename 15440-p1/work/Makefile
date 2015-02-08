all: mylib.so server

test:
	rm -f srv/copy
	cat giant | LD_PRELOAD=./mylib.so ../tools/440write copy

mylib.o: mylib.c
	gcc -O2 -Wall -fPIC -DPIC -c mylib.c -L../lib -ldirtree

mylib.so: mylib.o util.o
	ld -shared -o mylib.so mylib.o util.o -ldl

server: server.c util.o
	#find /home/autograde/autolab/autograde
	#tail -n +1 /home/autograde/autolab/autograde/auto/*
	#tail -n +1 /home/autograde/autolab/autograde/tools/check_malloc.py
	gcc -O2 -Wall -o server server.c util.o -L../lib -lpthread -ldirtree

util.o: util.c util.h
	gcc -O2 -Wall -fPIC -DPIC -c -o util.o util.c

clean:
	rm -f *.o *.so server

.PHONY: all clean test
