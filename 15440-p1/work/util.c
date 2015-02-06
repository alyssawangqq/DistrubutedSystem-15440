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

//ssize_t (*util_read)(int fildes, void *buf, size_t nbyte) = &read;
//ssize_t (*util_write)(int fildes, const void *buf, size_t nbyte) = &write;

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

bool send_exact(int fd, const void* buf, int size, int flags) {
	debug("send_exact %d: ", size);
	while (size > 0) {
		int ret = send(fd, buf, size, flags);
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

bool recv_exact(int fd, void* buf, int size, int flags) {
	debug("recv_exact %d: ", size);
	while (size > 0) {
		int ret = recv(fd, buf, size, flags);
		perror(NULL);
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
	bool ret = send_exact(fd, &i, 4, 0);
	debug("send_int: %d, %d\n", i, ret);
	return ret;
}

bool send_int64(int fd, int i) {
  bool ret = send_exact(fd, &i, 8, 0);
  debug("send_int64: %d, %d\n", i, ret);
  return ret;
}

bool send_string(int fd, const char* str) {
	size_t len = strlen(str);
	bool ret;
	ret = send_int(fd, len);
	if (len >= MAX_STRING_LEN) {
		ret = false;
	}
	ret = ret && send_exact(fd, str, len, 0);
	debug("send_string: %s, %d\n", str, ret);
	return ret;
}

bool recv_int(int fd, int* i) {
	bool ret = recv_exact(fd, i, 4, 0);
	debug("recv_int: %d, %d\n", *i, ret);
	return ret;
}

bool recv_int64(int fd, int* i) {
	bool ret = recv_exact(fd, i, 8, 0);
	debug("recv_int64: %d, %d\n", *i, ret);
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
		ret = ret && recv_exact(fd, str, len, 0);
		str[len] = '\0';
	}
	debug("recv_string: %s, %d\n", str, ret);
	return ret;
}

bool send_dirtreenode(int fd, struct dirtreenode* node) {
	int i;
	if (!send_string(fd, node->name) ||
			!send_int(fd, node->num_subdirs)) {
		return false;
	}
	for (i = 0; i < node->num_subdirs; i++) {
		if (!send_dirtreenode(fd, node->subdirs[i])) {
			return false;
		}
	}
	return true;
}

bool recv_dirtreenode(int fd, struct dirtreenode** node) {
	int i;
	struct dirtreenode* new_node = malloc(sizeof(struct dirtreenode));
	new_node->name = malloc(sizeof(char) * (MAX_STRING_LEN + 1));
	if(!recv_string(fd, new_node->name) ||
			!recv_int(fd, &new_node->num_subdirs)) {
				fprintf(stderr, "recv name and numb of sub dirtree fail \n");
		return false;
	}else {
		new_node->subdirs = malloc(sizeof(struct dirtreenode*) * new_node->num_subdirs);
		for (i = 0; i < new_node->num_subdirs; i++) {
			if(!recv_dirtreenode(fd, &new_node->subdirs[i])) {
				fprintf(stderr, "recv sub dirtree fail \n");
				return false;
			}
		}
		*node = new_node;
	}
	return true;
}

//bool init_ditreenode(struct dirtreenode* node, int numb_subdir) { //out put node
//	for (int i = 0; i < node->num_subdirs; i++) {
//		dirtreenode* new_node = malloc(sizeof(struct dirtreenode));
//		node->subdirs[i] = new_node;
//		init_ditreenode(node->subdirs[i]);
//	}
//	return true;
//}
