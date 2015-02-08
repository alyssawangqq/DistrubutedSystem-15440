#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <string.h>
#include <unistd.h>
#include <stdbool.h>
#include <assert.h>
#include <errno.h>
#include <netinet/tcp.h>

int sockfd;

int dial() {
  static char *serverip;
  static char *serverport;
  static unsigned short port;
  static int rv;
  static struct sockaddr_in srv;

  serverip = getenv("server15440");
  if (!serverip) {
    serverip = "127.0.0.1";
  }

  serverport = getenv("serverport15440");
  if (!serverport) {
    serverport = "15440";
  }
  port = (unsigned short)atoi(serverport);

  int sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd<0) {
    return -1;
  }

  int flag = 1;
  int result = setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY, (char *) &flag, sizeof(int));
  if (result < 0) {
    return -1;
  }

  memset(&srv, 0, sizeof(srv));
  srv.sin_family = AF_INET;
  srv.sin_addr.s_addr = inet_addr(serverip);
  srv.sin_port = htons(port);

  rv = connect(sockfd, (struct sockaddr*)&srv, sizeof(struct sockaddr));
  if (rv < 0) {
    return -1;
  }
  return sockfd;
}

#include <dlfcn.h>
#include <stdio.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

#include "../include/dirtree.h"
#include "util.h"

int (*orig_open)(const char *pathname, int flags, ...);
int (*orig_close)(int fildes);
ssize_t (*orig_read)(int fildes, void *buf, size_t nbyte);
ssize_t (*orig_write)(int fildes, const void *buf, size_t nbyte);
off_t (*orig_lseek)(int fildes, off_t offset, int whence);
int (*orig_stat)(const char *path, struct stat *buf);
int (*orig_xstat)(int ver, const char *path, struct stat *buf);
int (*orig_unlink)(const char *path);
ssize_t (*orig_getdirentries)(int fd, char *buf, size_t nbytes , off_t *basep);

int open(const char *pathname, int flags, ...) {
  mode_t m = 0;
  if (flags & O_CREAT) {
    va_list a;
    va_start(a, flags);
    m = va_arg(a, mode_t);
    va_end(a);
  }

  debug("open(%s, %d, %d)", pathname, flags, m);
  int32_t ret;
  if (send_int(sockfd, OPEN) &&
      send_string(sockfd, pathname) &&
      send_int(sockfd, flags) &&
      send_int(sockfd, m) &&
      recv_int(sockfd, &ret) &&
      recv_int(sockfd, &errno)) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("open");
    } else {
      ret += 2048;
    }
    return ret;
  }
  assert(false);
}

int close(int fildes) {
  if (fildes < 2048) {
    return orig_close(fildes);
  } else {
    fildes -= 2048;
  }
  debug("close(%d)", fildes);
  int32_t ret;
  if (send_int(sockfd, CLOSE) &&
      send_int(sockfd, fildes) &&
      recv_int(sockfd, &ret) &&
      recv_int(sockfd, &errno)) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("close");
    }
    return ret;
  }
  assert(false);
}

ssize_t read(int fildes, void *buf, size_t nbyte) {
  if (fildes < 2048) {
    return orig_read(fildes, buf, nbyte);
  } else {
    fildes -= 2048;
  }
  debug("read(%d, ..., %ld)", fildes, nbyte);
  int32_t ret;
  if (send_int(sockfd, READ) &&
      send_int(sockfd, fildes) &&
      send_int(sockfd, nbyte) &&
      recv_int(sockfd, &ret) &&
      recv_int(sockfd, &errno) &&
      (ret == -1 || recv_exact(sockfd, buf, ret))) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("read");
    }
    return ret;
  }
  assert(false);
}

ssize_t write(int fildes, const void *buf, size_t nbyte) {
  if (fildes < 2048) {
    return orig_write(fildes, buf, nbyte);
  } else {
    fildes -= 2048;
  }
  debug("write(%d, ..., %lu)", fildes, nbyte);
  int32_t ret;
  if (send_int(sockfd, WRITE) &&
      send_int(sockfd, fildes) &&
      send_int(sockfd, nbyte) &&
      send_exact(sockfd, buf, nbyte) &&
      recv_int(sockfd, &ret) &&
      recv_int(sockfd, &errno)) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("write");
    }
    return ret;
  }
  assert(false);
}

off_t lseek(int fildes, off_t offset, int whence) {
  if (fildes < 2048) {
    return orig_lseek(fildes, offset, whence);
  } else {
    fildes -= 2048;
  }
  debug("lseek(%d, %lu, %d)", fildes, offset, whence);
  int32_t ret;
  if (send_int(sockfd, LSEEK) &&
      send_int(sockfd, fildes) &&
      send_int(sockfd, offset) &&
      send_int(sockfd, whence) &&
      recv_int(sockfd, &ret) &&
      recv_int(sockfd, &errno)) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("lseek");
    }
    return ret;
  }
  assert(false);
}

int stat(const char *path, struct stat *buf) {
  debug("stat(%s, ...)", path);
  int32_t ret;
  if (send_int(sockfd, XSTAT) &&
      send_string(sockfd, path) &&
      recv_int(sockfd, &ret) &&
      (assert(ret == -1 || ret == sizeof(struct stat)), true) &&
      recv_int(sockfd, &errno) &&
      // Cheating.
      (ret == -1 || recv_exact(sockfd, buf, ret))) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("stat");
      return -1;
    } else {
      return 0;
    }
  }
  assert(false);
}

int __xstat(int ver, const char *path, struct stat *buf) {
  debug("__xstat(%d, %s, ...)", ver, path);
  int32_t ret;
  if (send_int(sockfd, XSTAT) &&
      send_int(sockfd, ver) &&
      send_string(sockfd, path) &&
      recv_int(sockfd, &ret) &&
      (assert(ret == -1 || ret == sizeof(struct stat)), true) &&
      recv_int(sockfd, &errno) &&
      // Cheating.
      (ret == -1 || recv_exact(sockfd, buf, ret))) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("stat");
      return -1;
    } else {
      return 0;
    }
  }
  assert(false);
}

int unlink(const char *path) {
  debug("unlink(%s)", path);

  int32_t ret;
  if (send_int(sockfd, UNLINK) &&
      send_string(sockfd, path) &&
      recv_int(sockfd, &ret) &&
      recv_int(sockfd, &errno)) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("unlink");
    }
    return ret;
  }
  assert(false);
}

ssize_t getdirentries(int fd, char *buf, size_t nbytes, off_t *basep) {
  if (fd < 2048) {
    return orig_getdirentries(fd, buf, nbytes, basep);
  } else {
    fd -= 2048;
  }
  debug("getdirentries(%d, ..., %ld, %lu)", fd, nbytes, *basep);
  int32_t ret;
  int64_t base;
  if (send_int(sockfd, GET_DIR_ENTRIES) &&
      send_int(sockfd, fd) &&
      send_int(sockfd, nbytes) &&
      send_int64(sockfd, *basep) &&
      recv_int(sockfd, &ret) &&
      (ret == -1 || recv_exact(sockfd, buf, ret)) &&
      (ret == -1 || recv_int64(sockfd, &base)) &&
      recv_int(sockfd, &errno)) {
    debug(" = %d\n", ret);
    if (ret == -1) {
      perror("getdirentries");
    }
    *basep = base;
    return ret;
  }
  assert(false);
}

struct dirtreenode* getdirtree(const char *path) {
  debug("getdirtree(%s)\n", path);
  struct dirtreenode* ret;
  if (send_int(sockfd, GET_DIR_TREE) &&
      send_string(sockfd, path) &&
      recv_dirtree(sockfd, &ret)) {
    return ret;
  }
  assert(false);
}

void _init(void) {
  orig_open = dlsym(RTLD_NEXT, "open");
  orig_close = dlsym(RTLD_NEXT, "close");
  orig_read = dlsym(RTLD_NEXT, "read");
  orig_write = dlsym(RTLD_NEXT, "write");
  orig_lseek = dlsym(RTLD_NEXT, "lseek");
  orig_stat = dlsym(RTLD_NEXT, "stat");
  orig_xstat = dlsym(RTLD_NEXT, "__xstat");
  orig_unlink = dlsym(RTLD_NEXT, "unlink");
  orig_getdirentries = dlsym(RTLD_NEXT, "getdirentries");

  if ((sockfd = dial()) == -1) {
    exit(1);
  }
}
