#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main() {
	char buf[1000] = "hhhhh";
	int fd = open("a", O_RDONLY);
	//int fd2 = open("b", O_CREAT | O_RDWR, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	int fd2 = open("b", O_CREAT | O_RDONLY, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	int ret, awrite;
	printf("fd = %d\n", fd);
	printf("fd2 = %d\n", fd2);
	//while((ret = read(fd, buf, sizeof(buf))) != 0) {
	//	while((awrite = write(fd2, buf, ret)) != 0) {
	//		ret -= awrite;
	//	}
	//}
	write(fd2, buf, 6);
	int _errno = errno;
	printf("errno = %d\n", _errno);
	perror(NULL);
	close(fd);
	close(fd2);
	return 0;
}
