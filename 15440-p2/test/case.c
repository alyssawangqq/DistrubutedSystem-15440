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
	printf("close fd2 = %d\n", close(fd2));
	//perror(NULL);
	fd2 = open("foo", O_WRONLY, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	printf("fd2 = %d\n", fd2);
	printf("close fd2 = %d\n", close(fd2));
	fd2 = open("b", O_CREAT | O_EXCL, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	printf("close fd2 = %d\n", close(fd2));
	//perror(NULL);
	printf("fd2 = %d\n", fd2);
	return 0;
}
