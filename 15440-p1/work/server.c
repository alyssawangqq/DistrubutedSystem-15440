#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h> 
#include <util.h>
#include <err.h>
#include <stdint.h>

#define MAXMSGLEN 1000

int main(int argc, char**argv) {
	char *serverport;
	unsigned short port;
	int sockfd, sessfd, rv;
	struct sockaddr_in srv, cli;
	socklen_t sa_size;

	// Get environment variable indicating the port of the server
	serverport = getenv("serverport15440");
	if (serverport) port = (unsigned short)atoi(serverport);
	else port=15440;

	// Create socket
	sockfd = socket(AF_INET, SOCK_STREAM, 0);	// TCP/IP socket
	if (sockfd<0) err(1, 0);			// in case of error

	// setup address structure to indicate server port
	memset(&srv, 0, sizeof(srv));			// clear it first
	srv.sin_family = AF_INET;			// IP family
	srv.sin_addr.s_addr = htonl(INADDR_ANY);	// don't care IP address
	srv.sin_port = htons(port);			// server port

	// bind to our port
	rv = bind(sockfd, (struct sockaddr*)&srv, sizeof(struct sockaddr));
	if (rv<0) err(1,0);

	// start listening for connections
	rv = listen(sockfd, 5);
	if (rv<0) err(1,0);

	// main server loop, handle clients one at a time, quit after 10 clients
	//char buf[MAXMSGLEN+1];
	while (1) {

		// wait for next client, get session socket
		sa_size = sizeof(struct sockaddr_in);
		sessfd = accept(sockfd, (struct sockaddr *)&cli, &sa_size);
		if (sessfd<0) err(1,0);

		// get messages and send replies to this client, until it goes away
		//while ( (rv=recv(sessfd, buf, MAXMSGLEN, 0)) > 0) {
		//	buf[rv]=0;		// null terminate string to print
		//	int32_t fd = *(int32_t*)buf;
		//	fprintf(stderr, "%d \n", fd);
		//	//fprintf(stderr, "%s", buf);
		//}

		int fid = -1;
		while((fid = read_int32(sessfd)) >= 0) {
			if(fid == OPEN) {
				fprintf(stderr, "enter open");
				const char* path = read_string(sessfd, MAXMSGLEN);
				fprintf(stderr, "path is :%s \n", path);
				int flag = read_int32(sessfd);
				fprintf(stderr, "flag is :%i \n", flag);
				int mode = read_int32(sessfd);
				fprintf(stderr, "mode is :%i \n", mode);
					int32_t fd = open(path, flag, mode);
				if(flag & O_CREAT) {
					fd = open(path, flag, mode);
				}else {
					fd = open(path, flag);
				}
				fprintf(stderr, "fd is :%i \n", fd);
				send_int_to_client(&fd, sessfd);
			}

			if(fid == CLOSE) {
				fprintf(stderr, "enter close");
				int fd = read_int32(sessfd);
				int32_t return_val = close(fd);
				send_int_to_client(&return_val, sessfd);
			}

			if(fid == WRITE) {
				fprintf(stderr, "enter write");
				int fd = read_int32(sessfd);
				fprintf(stderr, "fd is :%i \n", fd);
				int32_t nbyte = read_int32(sessfd);
				fprintf(stderr, "nbyte is :%i \n", nbyte);
				//char buf[nbyte];
				char* buf = read_string(sessfd, nbyte);
				fprintf(stderr, "buf is :%s \n", buf);
				int32_t return_val = write(fd, buf, nbyte);
				//print_byte(buf);
				fprintf(stderr, "return_val is :%i \n", return_val);
				send_int_to_client(&return_val, sessfd);
				//send_byte_to_client(buf, return_val, sessfd);
			}

			if(fid == READ) {
				fprintf(stderr, "enter read");
				int fd = read_int32(sessfd);
				fprintf(stderr, "fd is :%i \n", fd);
				int32_t nbyte = read_int32(sessfd);
				fprintf(stderr, "nbyte is :%i \n", nbyte);
				char buf[nbyte];
				int32_t return_val = read(fd, buf, nbyte);
				print_byte(buf);
				fprintf(stderr, "return_val is :%i \n", return_val);
				send_int_to_client(&return_val, sessfd);
				send_byte_to_client(buf, return_val, sessfd);
				fprintf(stderr, "sent\n");
			}
		}

		// either client closed connection, or error
		if (rv<0) err(1,0);
		close(sessfd);
	}

	// close socket
	printf("server closed");
	close(sockfd);

	return 0;
}
