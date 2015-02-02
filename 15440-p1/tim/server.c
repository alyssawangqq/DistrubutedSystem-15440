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

#include "util.h"

#define BUF_LEN 4096
#define DUMP fprintf(stderr, "%s:%d\n", __FILE__, __LINE__)

bool write_exact(int fd, const void* buf, int size) {
  while (size > 0) {
    int ret = write(fd, buf, size);
    if (ret <= 0) {
      perror("");
      return false;
    }
    buf = (char*)buf + ret;
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

bool handle(int clientfd) {
  fprintf(stderr, "\n");
  char buf[BUF_LEN];
  int func;
  if (!recv_int(clientfd, &func)) {
    return false;
  }
  if (func == OPEN) {
    fprintf(stderr, "open\n");
    int flags;
    int mode;
    int ret;
    if (recv_string(clientfd, buf) &&
        recv_int(clientfd, &flags) &&
        (!(flags & O_CREAT) || recv_int(clientfd, &mode))) {
      if (flags & O_CREAT) {
        ret = open(buf, flags, mode);
      } else {
        ret = open(buf, flags);
      }
    } else {
      return false;
    }
    return send_int(clientfd, ret);
  }
  if (func == CLOSE) {
    fprintf(stderr, "close\n");
    int fd;
    int ret;
    if (recv_int(clientfd, &fd)) {
      ret = close(fd);
    } else {
      return false;
    }
    return send_int(clientfd, ret);
  }
  if (func == WRITE) {
    fprintf(stderr, "write\n");
    int len;
    int fd;
    //int ret = -1;
    if (recv_int(clientfd, &fd) &&
        recv_int(clientfd, &len)) {
      //ret = len;
      while (len > 0) {
        int recv_len = min(len, BUF_LEN);
        DUMP;
        if (!recv_exact(clientfd, buf, recv_len)) {
          return false;
        }
        if (!write_exact(fd, buf, recv_len)) {
          DUMP;
          //ret = -1;
          return false;
        }
        len -= recv_len;
      }
    } else {
          DUMP;
      return false;
    }
          DUMP;
  }
  return true;
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
    fprintf(stderr, "accpeting\n");
    DUMP;
    sessfd = accept(sockfd, (struct sockaddr *)&cli, &sa_size);
    DUMP;
    if (sessfd<0) {
    DUMP;
      continue;
    }
    DUMP;

    while (handle(sessfd));

    close(sessfd);
  }

  return 0;
}
