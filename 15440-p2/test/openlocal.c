#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main() {
	errno = 0;
	int fd2 = open("localfile", O_RDONLY);
	perror("");
	printf("fd2 = %d\n", fd2);
	return 0;
}
