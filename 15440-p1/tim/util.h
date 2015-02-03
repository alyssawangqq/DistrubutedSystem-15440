#ifndef __UTIL__
#define __UTIL__

#include <stdbool.h>
#include <stdio.h>

#define MAX_STRING_LEN 256

enum SystemCall {
	OPEN,
	WRITE,
	CLOSE,
	READ,
};

bool send_exact(int fd, const void* buf, int size, int flags);

bool write_exact(int fd, const void* buf, int size);

bool read_exact(int fd, void* buf, int size);

bool recv_exact(int fd, void* buf, int size, int flags);

bool send_int(int fd, int i);

bool send_string(int fd, const char* str);

bool recv_int(int fd, int* i);

bool recv_string(int fd, char* str);

#endif
