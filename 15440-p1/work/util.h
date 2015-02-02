#ifndef __UTIL__
#define __UTIL__

#include <stdio.h>

enum system_call {
	OPEN,
	READ,
	WRITE,
	CLOSE
};

int32_t read_int32(int sessfd);
char* read_string(int sessfd, int32_t size);
void send_int_to_client(int32_t* fd, int sessfd);
void send_byte_to_client(char* file, int32_t size, int sessfd);
#endif
