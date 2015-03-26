#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main (int argc, char* argv[]) {
	//char buf[100] = "caonimade test";
	char buf[5];
	int fd = open(argv[1], O_RDONLY); 
	lseek(fd, 5, SEEK_END);
	read(fd, buf, 5);
	printf("%s \n",buf);
	close(fd);
}
