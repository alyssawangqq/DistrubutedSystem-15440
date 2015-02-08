#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <string.h>
#include <unistd.h>
#include <err.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <pthread.h>
#include <errno.h>
#include <assert.h>
#include <dirent.h>
#include <netinet/tcp.h>

#include "util.h"
#include "../include/dirtree.h"

#define BUF_LEN 1000001

typedef enum Status {
  OK,
  SRC_ERROR,
  DST_ERROR,
} Status;

bool read_exact(int fd, void* buff, int size) {
  while (size > 0) {
    int ret = read(fd, buff, size);
    if (ret <= 0) {
      return false;
    }
    buff = (char*)buff + ret;
    size -= ret;
  }
  return true;
}

bool write_exact(int fd, const void* buff, int size) {
  while (size > 0) {
    int ret = write(fd, buff, size);
    if (ret <= 0) {
      return false;
    }
    buff = (char*)buff + ret;
    size -= ret;
  }
  return true;
}

int min(int a, int b) {
  if (a < b) {
    return a;
  }
  return b;
}

Status pipeline_exact(int dstfd, int srcfd, int len) {
  char buff[BUF_LEN];
  while (len > 0) {
    int actual = min(len, BUF_LEN);
    if (!read_exact(srcfd, buff, actual)) {
      return SRC_ERROR;
    }
    if (!write_exact(dstfd, buff, actual)) {
      return DST_ERROR;
    }
    len -= actual;
  }
  return OK;
}

bool handle_open(int sockfd, fd_set* opened_files) {
  char buff[BUF_LEN];
  int32_t flags;
  int32_t mode;
  int32_t ret;
  return recv_string(sockfd, buff) &&
    recv_int(sockfd, &flags) &&
    recv_int(sockfd, &mode) &&
    (ret = open(buff, flags, mode), true) &&
    debug("open(%s, %d, %d) = %d\n", buff, flags, mode, ret) &&
    (ret == -1 || (FD_SET(ret, opened_files), true)) &&
    send_int(sockfd, ret) &&
    send_int(sockfd, errno);
}

bool handle_close(int sockfd, fd_set* opened_files) {
  int32_t fd;
  int32_t ret;
  return recv_int(sockfd, &fd) &&
    (FD_ISSET(fd, opened_files) ? (FD_CLR(fd, opened_files), true) : (fd = -18, true)) &&
    (ret = close(fd), true) &&
    debug("close(%d) = %d\n", fd, ret) &&
    send_int(sockfd, ret) &&
    send_int(sockfd, errno);
}

bool handle_read(int sockfd, const fd_set* opened_files) {
  char buff[BUF_LEN];
  int32_t fd, ret, size;
  return recv_int(sockfd, &fd) &&
    (FD_ISSET(fd, opened_files) || (fd = -18, true)) &&
    recv_int(sockfd, &size) &&
    (ret = read(fd, buff, min(size, BUF_LEN)), true) &&
    debug("read(%d, ..., %d) = %d\n", fd, size, ret) &&
    send_int(sockfd, ret) &&
    send_int(sockfd, errno) &&
    (ret == -1 || send_exact(sockfd, buff, ret));
}

bool handle_write(int sockfd, const fd_set* opened_files) {
  int32_t len, fd;
  Status status;
  return recv_int(sockfd, &fd) &&
    (FD_ISSET(fd, opened_files) || (fd = -18, true)) &&
    recv_int(sockfd, &len) &&
    (status = pipeline_exact(fd, sockfd, len), status != SRC_ERROR) &&
    debug("pipeline_exact(%d, %d, %d) = %d\n", fd, sockfd, len, status) &&
    send_int(sockfd, status == OK ? len : -1) &&
    send_int(sockfd, errno);
}

bool handle_lseek(int sockfd, const fd_set* opened_files) {
  int32_t fd, offset, whence, ret;
  return recv_int(sockfd, &fd) &&
    (FD_ISSET(fd, opened_files) || (fd = -18, true)) &&
    recv_int(sockfd, &offset) &&
    recv_int(sockfd, &whence) &&
    (ret = lseek(fd, offset, whence), true) &&
    debug("lseek(%d, %d, %d) = %d\n", fd, offset, whence, ret) &&
    send_int(sockfd, ret) &&
    send_int(sockfd, errno);
}

bool handle_stat(int sockfd) {
  char buff[BUF_LEN];
  int32_t ret;
  struct stat st;
  return recv_string(sockfd, buff) &&
    (ret = stat(buff, &st), true) &&
    debug("stat(%s, ...) = %d\n", buff, ret) &&
    send_int(sockfd, ret == -1 ? -1 : sizeof(st)) &&
    send_int(sockfd, errno) &&
    (ret == -1 || send_exact(sockfd, &st, sizeof(st)));
}

bool handle_xstat(int sockfd) {
  char buff[BUF_LEN];
  int32_t ver;
  int32_t ret;
  struct stat st;
  return recv_int(sockfd, &ver) &&
    recv_string(sockfd, buff) &&
    (ret = __xstat(ver, buff, &st), true) &&
    debug("xstat(%d, %s, ...) = %d\n", ver, buff, ret) &&
    send_int(sockfd, ret == -1 ? -1 : sizeof(st)) &&
    send_int(sockfd, errno) &&
    (ret == -1 || send_exact(sockfd, &st, sizeof(st)));
}

bool handle_unlink(int sockfd) {
  char buff[BUF_LEN];
  int32_t ret;
  return recv_string(sockfd, buff) &&
    (ret = unlink(buff), true) &&
    debug("unlink(%s) = %d\n", buff, ret) &&
    send_int(sockfd, ret) &&
    send_int(sockfd, errno);
}

bool handle_getdirentries(int sockfd, const fd_set* opened_files) {
  char buff[BUF_LEN];
  int32_t ret, fd, size;
  int64_t base;
  return recv_int(sockfd, &fd) &&
    (FD_ISSET(fd, opened_files) || (fd = -18, true)) &&
    recv_int(sockfd, &size) &&
    recv_int64(sockfd, &base) &&
    debug("getdirentries(%d, ..., %d, %ld) = ", fd, size, base) &&
    (ret = getdirentries(fd, buff, min(size, BUF_LEN), &base), true) &&
    debug("%d, %ld\n", ret, base) &&
    send_int(sockfd, ret) &&
    (ret == -1 || send_exact(sockfd, buff, ret)) &&
    (ret == -1 || send_int64(sockfd, base)) &&
    send_int(sockfd, errno);
}

bool handle_getdirtree(int sockfd) {
  char buff[MAX_STRING_LEN+1];
  return recv_string(sockfd, buff) &&
    debug("getdirtree(%s)\n", buff) &&
    send_dirtree(sockfd, buff);
}

bool dispatch(int sockfd, fd_set* opened_files) {
  int func;
  if (!recv_int(sockfd, &func)) {
    return false;
  }
  enum SystemCall ftype = func;
  switch (ftype) {
  case OPEN:
    return handle_open(sockfd, opened_files);
  case CLOSE:
    return handle_close(sockfd, opened_files);
  case READ:
    return handle_read(sockfd, opened_files);
  case WRITE:
    return handle_write(sockfd, opened_files);
  case LSEEK:
    return handle_lseek(sockfd, opened_files);
  case STAT:
    return handle_stat(sockfd);
  case XSTAT:
    return handle_xstat(sockfd);
  case UNLINK:
    return handle_unlink(sockfd);
  case GET_DIR_ENTRIES:
    return handle_getdirentries(sockfd, opened_files);
  case GET_DIR_TREE:
    return handle_getdirtree(sockfd);
  };
  return false;
}

void* handle(void* data) {
  int sockfd = (intptr_t)data;
  fd_set* opened_files = malloc(sizeof(fd_set));
  FD_ZERO(opened_files);
  while ((debug("client=%d, ", sockfd), dispatch(sockfd, opened_files)));
  close(sockfd);
  free(opened_files);
  return NULL;
}

#include <stdio.h>

int main(int argc, char**argv) {
  if (BUF_LEN < MAX_STRING_LEN+1) {
    return 1;
  }

  char *serverport;
  unsigned short port;
  int sockfd, sessfd, rv;
  struct sockaddr_in srv, cli;
  socklen_t sa_size;

  serverport = getenv("serverport15440");
  if (serverport) port = (unsigned short)atoi(serverport);
  else port=15440;

  sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd<0) err(1, 0);

  int flag = 1;
  int result = setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY, (char *) &flag, sizeof(int));
  if (result < 0) {
    return -1;
  }

  memset(&srv, 0, sizeof(srv));
  srv.sin_family = AF_INET;
  srv.sin_addr.s_addr = htonl(INADDR_ANY);
  srv.sin_port = htons(port);

  rv = bind(sockfd, (struct sockaddr*)&srv, sizeof(struct sockaddr));
  if (rv<0) err(1,0);

  rv = listen(sockfd, 5);
  if (rv<0) err(1,0);
  while (1) {
    sa_size = sizeof(struct sockaddr_in);
    debug("accpeting\n");
    sessfd = accept(sockfd, (struct sockaddr *)&cli, &sa_size);
    if (sessfd<0) {
      continue;
    }
    pthread_t th;
    if (pthread_create(&th, NULL, handle, (void*)(intptr_t)sessfd) != 0 || pthread_detach(th) != 0) {
      close(sessfd);
    }
  }

  return 0;
}
