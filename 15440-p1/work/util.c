#include "util.h"

#include <sys/socket.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <assert.h>

#include "../include/dirtree.h"

bool send_exact(int fd, const void* buf, int size) {
  //int dbg_size = size;
  while (size > 0) {
    int ret = send(fd, buf, size, 0);
    if (ret <= 0) {
      //debug("    send_exact f: fd=%d, size=%d\n", fd, dbg_size);
      return false;
    }
    buf = (char*)buf + ret;
    size -= ret;
  }
  //debug("    send_exact s: fd=%d, size=%d\n", fd, dbg_size);
  return true;
}

bool recv_exact(int fd, void* buf, int size) {
  //int dbg_size = size;
  while (size > 0) {
    int ret = recv(fd, buf, size, 0);
    if (ret <= 0) {
      //debug("    recv_exact f: fd=%d, size=%d\n", fd, dbg_size);
      return false;
    }
    buf = (char*)buf + ret;
    size -= ret;
  }
  //debug("    recv_exact s: fd=%d, size=%d\n", fd, dbg_size);
  return true;
}

bool send_int(int fd, int32_t i) {
  bool ret = send_exact(fd, &i, 4);
  //debug("  send_int %d: %d\n", ret, i);
  return ret;
}

bool send_int64(int fd, int64_t i) {
  bool ret = send_exact(fd, &i, 8);
  //debug("  send_int64 %d: %ld\n", ret, i);
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
  //debug("  send_string %d: %s\n", ret, str);
  return ret;
}

bool recv_int(int fd, int32_t* i) {
  bool ret = recv_exact(fd, i, 4);
  //debug("  recv_int %d: %d\n", ret, *i);
  return ret;
}

bool recv_int64(int fd, int64_t* i) {
  bool ret = recv_exact(fd, i, 8);
  //debug("  recv_int64 %d: %ld\n", ret, *i);
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
  //debug("  recv_string %d: %s\n", ret, str);
  return ret;
}

// TODO: prevent buffer overflow.
char* append_exact(char* buff, const void* str, int len) {
  memcpy(buff, str, len);
  return buff + len;
}

char* append_string(char* buff, const char* str) {
  int32_t len = strlen(str);
  return append_exact(append_exact(buff, &len, 4), str, len);
}

char* send_dirtree_impl(char* buff, struct dirtreenode* dptr) {
  if (dptr == NULL) {
    return buff;
  }
  int32_t num = dptr->num_subdirs;
  buff = append_exact(append_string(buff, dptr->name), &num, 4);
  int i;
  for (i = 0; i < num; i++) {
    buff = send_dirtree_impl(buff, dptr->subdirs[i]);
  }
  return buff;
}

bool send_dirtree(int fd, const char* path) {
  char buff[4096];
  struct dirtreenode* tree = getdirtree(path);
  int32_t len = send_dirtree_impl(buff, tree) - buff;
  freedirtree(tree);
  return send_int(fd, len) &&
    send_exact(fd, buff, len);
}

const char* recv_dirtree_impl(const char* buff, struct dirtreenode** dptr) {
  struct dirtreenode* ret = malloc(sizeof(struct dirtreenode));
  int32_t len = *(int32_t*)buff;
  buff += 4;
  ret->name = malloc(len + 1);
  memcpy(ret->name, buff, len);
  ret->name[len] = '\0';
  buff += len;
  ret->num_subdirs = *(int32_t*)buff;
  buff += 4;
  if (ret->num_subdirs != 0) {
    ret->subdirs = malloc(sizeof(struct dirtreenode) * ret->num_subdirs);
  } else {
    ret->subdirs = NULL;
  }
  int i;
  for (i = 0; i < ret->num_subdirs; i++) {
    buff = recv_dirtree_impl(buff, &ret->subdirs[i]);
  }
  *dptr = ret;
  return buff;
}

bool recv_dirtree(int fd, struct dirtreenode** dptr) {
  char buff[4096];
  int32_t len;
  if (!recv_int(fd, &len) ||
      !recv_exact(fd, buff, len)) {
    return false;
  }
    *dptr = NULL;
  if (len == 0) {
    return true;
  }
  assert(recv_dirtree_impl(buff, dptr) == buff + len);
  return true;
}
