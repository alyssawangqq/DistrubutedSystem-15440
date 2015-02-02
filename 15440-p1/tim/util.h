#ifndef __UTIL__
#define __UTIL__

#include <stdbool.h>
#include <stdio.h>

#define MAX_STRING_LEN 256

enum SystemCall {
  OPEN,
  WRITE,
  CLOSE,
};

extern ssize_t (*util_read)(int fildes, void *buf, size_t nbyte);
extern ssize_t (*util_write)(int fildes, const void *buf, size_t nbyte);

bool send_exact(int fd, const void* buf, int size);

bool recv_exact(int fd, void* buf, int size);

bool send_int(int fd, int i);

bool send_string(int fd, const char* str);

bool recv_int(int fd, int* i);

bool recv_string(int fd, char* str);

#endif
