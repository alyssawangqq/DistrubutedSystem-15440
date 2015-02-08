#ifndef __UTIL__
#define __UTIL__

#include <stdbool.h>
#include <stdio.h>
#include <stdint.h>

#define MAX_STRING_LEN 256
#define debug(...) fprintf(stderr, __VA_ARGS__)
#define DUMP debug("%s:%d\n", __FILE__, __LINE__)

struct dirtreenode;

enum SystemCall {
  OPEN,
  CLOSE,
  READ,
  WRITE,
  LSEEK,
  STAT,
  XSTAT,
  UNLINK,
  GET_DIR_ENTRIES,
  GET_DIR_TREE,
};

bool send_exact(int fd, const void* buf, int size);

bool recv_exact(int fd, void* buf, int size);

bool send_int(int fd, int32_t i);

bool send_int64(int fd, int64_t i);

bool send_string(int fd, const char* str);

bool recv_int(int fd, int32_t* i);

bool recv_int64(int fd, int64_t* i);

bool recv_string(int fd, char* str);

bool send_dirtree(int fd, const char* path);

bool recv_dirtree(int fd, struct dirtreenode** dptr);

#endif
