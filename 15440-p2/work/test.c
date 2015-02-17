#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main() {
	char buf[1000];
	int fd = open("a", O_RDONLY);
	int fd2 = open("b", O_CREAT | O_RDWR, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	int _errno = errno;
	int ret, awrite;
	printf("errno = %d\n", _errno);
	printf("fd = %d\n", fd);
	printf("fd2 = %d\n", fd2);
	while((ret = read(fd, buf, sizeof(buf))) != 0) {
		while((awrite = write(fd2, buf, ret)) != 0) {
			ret -= awrite;
		}
	}
	close(fd);
	close(fd2);
	return 0;
}
