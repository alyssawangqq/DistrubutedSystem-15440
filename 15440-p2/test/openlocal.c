#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>
#include <string.h>

int main() {
	errno = 0;
	char buf[1000];
	memset(buf, 0, 1000);
	int fd1 = open("localfile", O_RDONLY);
	perror("");
	int fd2 = open(".", O_RDONLY);
	read(fd2, buf, 10);
	perror("");
	close(fd2);
	int fd3 = open("Alice", O_RDONLY);
	close(fd3);
	unlink("localfile");
	perror("");
	return 0;
}
