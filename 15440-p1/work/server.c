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
#include<pthread.h>

#include "util.h"

#define BUF_LEN 4096
#define DUMP fprintf(stderr, "%s:%d\n", __FILE__, __LINE__)

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
	//fprintf(stderr, "client fd is :%d\n", clientfd);
	if (!recv_int(clientfd, &func)) {
		fprintf(stderr, "fail recv func\n");
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
				if (!recv_exact(clientfd, buf, recv_len, 0)) {
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
	if(func == READ) {
		int len;
		int fd;
		int retlen;
		fprintf(stderr, "read\n");
		if(recv_int(clientfd, &fd) && 
				recv_int(clientfd, &len)) {
			retlen = read(fd, buf, len);
			send_int(clientfd, retlen);
			send_exact(clientfd, buf, retlen, 0);
		}else {
			return false;
		}
	}
	if(func == LSEEK) {
		int fd, whence, offset;
		if(recv_int(clientfd, &fd)&&
				(recv_int(clientfd, &offset))&&
				(recv_int(clientfd, &whence))) {
			if(!send_int(clientfd, lseek(fd, (off_t)offset, whence))) {
				return false;	
			}
		}else {
			return false;
		}
	}
	if(func == UNLINK) {
		int ret;
		if(recv_string(clientfd, buf)) {
			if((ret = unlink(buf)) != 0) {
				fprintf(stderr, "unlink err.\n");
				return false;
			}else {
				send_int(clientfd, ret);
			}
		}
	}
	if(func == __XSTAT) {
		struct stat st;
		int ver;
		if(recv_int(clientfd, &ver) &&
				recv_string(clientfd, buf) &&
				recv_int64(clientfd, (int*)&st.st_dev) &&
				recv_int64(clientfd, (int*)&st.st_ino) &&
				recv_int64(clientfd, (int*)&st.st_mode) &&
				recv_int64(clientfd, (int*)&st.st_nlink) &&
				recv_int64(clientfd, (int*)&st.st_uid) &&
				recv_int64(clientfd, (int*)&st.st_gid) &&
				recv_int64(clientfd, (int*)&st.st_rdev) &&
				recv_int64(clientfd, (int*)&st.st_size) &&
				recv_int64(clientfd, (int*)&st.st_blksize) &&
				recv_int64(clientfd, (int*)&st.st_blocks) &&
				recv_int64(clientfd, (int*)&st.st_atime) &&
				recv_int64(clientfd, (int*)&st.st_ctime)) {
			//if((ret = __xstat(ver, buf, &st)) == -1) {
			//	send_int(clientfd, ret);
			//}
			if(!send_int(clientfd, __xstat(ver, buf, &st))) {
				return false;
			}
		}else {
			return false;
		}
	}
	if(func == GETDIRENTRIES) {
		int fd;
		int len, retlen;
		off_t *bsp = NULL;
		if(recv_int(clientfd, &fd) &&
				recv_int(clientfd, &len) &&
				recv_int64(clientfd, (int*)bsp)) {
			retlen = getdirentries(fd, buf, len, bsp);
			if(!send_int(clientfd, retlen) || 
					!send_exact(clientfd, buf, retlen, 0)) {
				return false;
			}
		}
	}
	if(func == GETDIRTREE) {
		//struct dirtreenode* ret_node;
			fprintf(stderr, "enter getdir\n");
		if(!recv_string(clientfd, buf) ||
				!send_dirtreenode(clientfd, getdirtree(buf))) {
			fprintf(stderr, "fail send or recv buf\n");
			return false;
		}
	}
	if(func == FREEDIRTREE) {
		fprintf(stderr, "enter free dir \n");
		struct dirtreenode* dir;
		if(recv_dirtreenode(clientfd, &dir)) {
			freedirtree(dir);
		}else {
			return false;
		}
	}
	return true;
}

void* client_handler(void* clientfd_desc) {
	int clientfd = (intptr_t)clientfd_desc;
	fprintf(stderr, "fd is : %d\n", clientfd);
	DUMP;
	while(handle(clientfd));
	DUMP;
	close(clientfd);
	return 0;
}

#include <stdio.h>

int main(int argc, char**argv) {
	if (BUF_LEN < MAX_STRING_LEN+1) {
		return 1;
	}

	char *serverport;
	unsigned short port;
	int sockfd,rv, sessfd;
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
		pthread_t thread_id;
		if(pthread_create(&thread_id, NULL, client_handler, (void*)(intptr_t)sessfd) < 0) {
			fprintf(stderr, "fail to create thread\n");
			return 1;
		}
		fprintf(stderr, "handle client\n");
		//while(handle(sessfd));
	}

	return 0;
}
