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
#include <sys/stat.h>

int sockfd;

bool init_socket() {
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

	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if (sockfd<0) {
		return false;
	}

	memset(&srv, 0, sizeof(srv));
	srv.sin_family = AF_INET;
	srv.sin_addr.s_addr = inet_addr(serverip);
	srv.sin_port = htons(port);

	rv = connect(sockfd, (struct sockaddr*)&srv, sizeof(struct sockaddr));
	return rv >= 0;
}

int (*orig_open)(const char *pathname, int flags, ...);  // mode_t mode is needed when flags includes O_CREAT
int (*orig_close)(int fildes);
ssize_t (*orig_read)(int fildes, void *buf, size_t nbyte);
ssize_t (*orig_write)(int fildes, const void *buf, size_t nbyte);
off_t (*orig_lseek)(int fildes, off_t offset, int whence);
int (*orig_xstat)(int ver, const char * path, struct stat * stat_buf);
int (*orig_unlink)(const char *path);
ssize_t (*orig_getdirentries)(int fd, char *buf, size_t nbytes , off_t *basep);
struct dirtreenode* (*orig_getdirtree)(const char *path);
void (*orig_freedirtree)(struct dirtreenode* dt);

void reconnect() {
	//orig_close(sockfd);
	close(sockfd);
	if (!init_socket()) {
		exit(1);
	}
	fprintf(stderr, "reconnected\n");
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

struct OpenedFile {
	bool exist;
	int flags;
} opened_fd[4096];

int open(const char *pathname, int flags, ...) {
	fprintf(stderr, "try open\n");
	mode_t m = 0;
	if (flags & O_CREAT) {
		va_list a;
		va_start(a, flags);
		m = va_arg(a, mode_t);
		va_end(a);
	}

	int ret;
	if (send_int(sockfd, OPEN) &&
			send_string(sockfd, pathname) &&
			send_int(sockfd, flags) &&
			(!(flags & O_CREAT) || send_int(sockfd, m)) &&
			recv_int(sockfd, &ret)) {
		fprintf(stderr, "open: %d\n", ret);
		if (ret >= 0) {
			opened_fd[ret].exist = true;
			opened_fd[ret].flags = flags;
		}
		return ret;
	} else {
		reconnect();
	}
	fprintf(stderr, "open: -1\n");
	return -1;
}

int close(int fildes) {
	fprintf(stderr, "try close: %d\n", fildes);
	int ret;
	if (send_int(sockfd, CLOSE) &&
			send_int(sockfd, fildes) &&
			recv_int(sockfd, &ret)) {
		fprintf(stderr, "close %d\n", ret);
		if (ret == 0) {
			opened_fd[fildes].exist = false;
			return ret;
		}
	} else {
		//reconnect();
	}
	fprintf(stderr, "close -1\n");
	return -1;
}

ssize_t read(int fildes, void *buf, size_t nbyte) {
	//return orig_read(fildes, buf, nbyte);
	int ret;
	fprintf(stderr, "try read\n");
	if(send_int(sockfd, READ) && 
			send_int(sockfd, fildes) &&
			send_int(sockfd, nbyte) &&
			recv_int(sockfd, &ret)){
		fprintf(stderr, "send recv complete\n");
		if(ret > 0) {
			recv_exact(sockfd, buf, ret, 0);
			return ret;
		}else {
			return 0;
		}
	}else {
		fprintf(stderr, "reconnect");
		reconnect();
	}
	return -1;
}

ssize_t write(int fildes, const void *buf, size_t nbyte) {
	fprintf(stderr, "try write\n");

	if (!opened_fd[fildes].exist) {
		return orig_write(-1, buf, nbyte);
	}
	int flags = opened_fd[fildes].flags;
	if (!((flags & O_WRONLY) || (flags & O_RDWR))) {
		return orig_write(-1, buf, nbyte);
	}
	if (send_int(sockfd, WRITE) &&
			send_int(sockfd, fildes) &&
			send_int(sockfd, nbyte) &&
			send_exact(sockfd, buf, nbyte, 0)) {
		fprintf(stderr, "write %lu\n", nbyte);
		return nbyte;
	} else {
		reconnect();
	}
	fprintf(stderr, "write 0\n");
	return 0;
}

off_t lseek(int fildes, off_t offset, int whence) {
	fprintf(stderr, "mylib: lseek called for fd %d\n", fildes);
	int ret;
	if (!opened_fd[fildes].exist) {
		return orig_lseek(-1, offset, whence);
	}
	if(send_int(sockfd, LSEEK)&&
			send_int(sockfd, fildes)&&
			send_int(sockfd, offset)&&
			send_int(sockfd, whence)&&
			recv_int(sockfd, &ret)) {
		return (off_t)ret;
	}
	return 0;
	//return orig_lseek(fildes, offset, whence);
}

//int stat(const char *path, struct stat *buf) {
//	fprintf(stderr, "mylib: stat called for path %s", path);
//	return orig_stat(path, buf);
//}

int __xstat(int ver, const char * path, struct stat * stat_buf) {
	fprintf(stderr, "mylib: __xstat called for path %s", path);
	if(send_int(sockfd, ver) &&
			send_string(sockfd, path) 
			) {
			//TODO
	}
	//send_to_server("__xstat\n");
	//return orig_xstat(ver, path, stat_buf);
	return 0;
}

int unlink(const char *path) {
	fprintf(stderr, "mylib: unlink called for path %s\n", path);
	int ret;
	if(send_int(sockfd, UNLINK)&&
			send_string(sockfd, path)&&
			//send_exact(sockfd, path, strlen(path), 0)&&
			recv_int(sockfd, &ret)) {
		return ret;
	}
	return 0;
	//return orig_unlink(path);
}

ssize_t getdirentries(int fd, char *buf, size_t nbytes , off_t *basep) {
	fprintf(stderr, "mylib: getdirentries called for fd %d\n", fd);
	return orig_getdirentries(fd, buf, nbytes, basep);
}

struct dirtreenode* getdirtree(const char *path) {
	fprintf(stderr, "mylib: getdirtree called for path %s\n", path);
	return orig_getdirtree(path);
}

void freedirtree(struct dirtreenode* dt) {
	fprintf(stderr, "mylib: freedirtree called for path %s\n", dt->name);
	orig_freedirtree(dt);
}

void _init(void) {
	orig_open = dlsym(RTLD_NEXT, "open");
	orig_close = dlsym(RTLD_NEXT, "close");
	orig_read = dlsym(RTLD_NEXT, "read");
	orig_write = dlsym(RTLD_NEXT, "write");
	orig_lseek = dlsym(RTLD_NEXT, "lseek");
	orig_xstat = dlsym(RTLD_NEXT, "__xstat");
	orig_unlink = dlsym(RTLD_NEXT, "unlink");
	orig_getdirentries = dlsym(RTLD_NEXT, "getdirentries");
	orig_getdirtree = dlsym(RTLD_NEXT, "getdirtree");
	orig_freedirtree = dlsym(RTLD_NEXT, "freedirtree");

	if (!init_socket()) {
		exit(1);
	}
}
