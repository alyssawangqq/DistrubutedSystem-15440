#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h> 
#include <err.h>
#include <stdint.h>

#define MAXMSGLEN 100

enum system_call {
	OPEN,
	READ,
	WRITE,
	CLOSE
};

int read_int32(int sessfd) {
	int rv;
	char buf[MAXMSGLEN+1];
	if((rv = recv(sessfd, buf, MAXMSGLEN, 0)) > 0) {
		buf[rv] = 0;
		printf("%d", buf[0] - '0');
		//return (int)(buf - '0');
	}
		return buf[0] - '0';
}

const char* read_string(int sessfd) {
	int rv, length;
	char* buf = malloc(MAXMSGLEN+1);
	//char* str = malloc();
	if((rv = recv(sessfd, buf, MAXMSGLEN, 0)) > 0) {
		buf[rv] = 0;
		//length = buf[0]; //length of path
	}
	return buf;
}

int main(int argc, char**argv) {
	//char buf[MAXMSGLEN+1];
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
  while (1) {
		
		// wait for next client, get session socket
		sa_size = sizeof(struct sockaddr_in);
		sessfd = accept(sockfd, (struct sockaddr *)&cli, &sa_size);
		if (sessfd<0) err(1,0);
		
//		// get messages and send replies to this client, until it goes away
//		while ( (rv=recv(sessfd, buf, MAXMSGLEN, 0)) > 0) {
//			buf[rv]=0;		// null terminate string to print
//			if(strcmp(buf , "open")) {
//				printf("is open!!");
//			}
//		}
//=======
		int fid = read_int32(sessfd);
		if(fid == OPEN) {
			const char* path = read_string(sessfd);
			int flag = read_int32(sessfd);
			open(path, flag);
	}

	// either client closed connection, or error

	//if (rv==0) err(1,0);
	//close(sessfd);

	if (rv<0) err(1,0);
	close(sessfd);
	}

	// close socket
	printf("server closed");
	close(sockfd);

	return 0;
}

