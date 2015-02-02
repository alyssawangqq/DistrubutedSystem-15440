#include "util.h"

#include <sys/socket.h>
#include <string.h>
#include <unistd.h>

#ifdef NO_DEBUG_OUTPUT
#define debug(...)
#else
#define debug(...) fprintf(stderr, __VA_ARGS__)
#endif

#define DUMP debug("%s:%d\n", __FILE__, __LINE__)

ssize_t (*util_read)(int fildes, void *buf, size_t nbyte) = &read;
ssize_t (*util_write)(int fildes, const void *buf, size_t nbyte) = &write;

bool send_exact(int fd, const void* buf, int size) {
  debug("send_exact %d: ", size);
  while (size > 0) {
    int ret = util_write(fd, buf, size);
    if (ret <= 0) {
      debug("fail\n");
      return false;
    }
    buf = (char*)buf + ret;
    size -= ret;
  }
  debug("succ\n");
  return true;
}

bool recv_exact(int fd, void* buf, int size) {
  debug("recv_exact %d: ", size);
  while (size > 0) {
    int ret = util_read(fd, buf, size);
    if (ret <= 0) {
      debug("fail\n");
      return false;
    }
    buf = (char*)buf + ret;
    size -= ret;
  }
  debug("succ\n");
  return true;
}

bool send_int(int fd, int i) {
  bool ret = send_exact(fd, &i, 4);
  debug("send_int: %d, %d\n", i, ret);
  return ret;
}

bool send_string(int fd, const char* str) {
  size_t len = strlen(str);
  bool ret;
  ret = send_int(fd, len);
  if (len >= MAX_STRING_LEN) {
    ret = false;
  }
  ret = ret && send_exact(fd, str, len);
  debug("send_string: %s, %d\n", str, ret);
  return ret;
}

bool recv_int(int fd, int* i) {
  bool ret = recv_exact(fd, i, 4);
  debug("recv_int: %d, %d\n", *i, ret);
  return ret;
}

bool recv_string(int fd, char* str) {
  bool ret;
  int len;
  ret = recv_int(fd, &len);
  if (len > MAX_STRING_LEN) {
    ret = false;
  }
  if (ret) {
    ret = ret && recv_exact(fd, str, len);
    str[len] = '\0';
  }
  debug("recv_string: %s, %d\n", str, ret);
  return ret;
}
