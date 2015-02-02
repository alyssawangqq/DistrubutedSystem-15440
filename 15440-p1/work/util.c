#include "util.c"
#include <sys/socket.h>
#include <string.h>
#include <unistd.h>

int32_t read_int32(int sessfd) {
	int rv;
	int32_t* buf = malloc(sizeof(int));
	if((rv = recv(sessfd, buf, sizeof(int32_t), 0)) > 0) {
		return *buf;
	}
	return -1;
}

char* read_string(int sessfd, int32_t size) {
	int rv;
	char buf[size];
	if((rv = recv(sessfd, buf, size, 0)) > 0) {
		char* recv_str = malloc(rv);
		memcpy(recv_str, buf, rv);
		return recv_str;
	}
	return NULL;
}

void send_int_to_client(int32_t* fd, int sessfd) {
	send(sessfd, fd, sizeof(int32_t), 0);
}

void send_byte_to_client(char* file, int32_t size, int sessfd) {
	send(sessfd, file, size, 0);
}

void send_to_server(const char* msg) {
	  send(sockfd, msg, strlen(msg), 0);
}

void send_int_to_server(int32_t* msg, const int32_t size) {
	  send(sockfd, msg, size * sizeof(int32_t), 0);
}
