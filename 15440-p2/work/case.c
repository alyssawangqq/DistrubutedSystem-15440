#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main() {
	char buf[1000] = "hhhhhahsdasd";
	int fd2 = open("b", O_RDONLY, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	printf("fd2 = %d\n", fd2);
	int ret = (int)write(fd2, buf, 6);
	printf("write ret = %d\n", ret);
	int _errno = ret;
	//printf("errno = %d\n", _errno);
	perror(NULL);
	close(fd2);
	return 0;
}
