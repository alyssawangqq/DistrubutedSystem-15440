#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main() {
	char buf[1000] = "hhhhhahsdasd";
	int fd1 = open("Pony", O_RDONLY);
	//printf("fd2 = %d\n", fd2);
	perror("");
	int ret = (int)write(fd1, buf, 6);
	perror("");
	//printf("write ret = %d\n", ret);
	//printf("close fd1 = %d\n", close(fd1));
	int fd2 = open("foo", O_WRONLY);
	perror("");
	//printf("fd2 = %d\n", fd2);
	//printf("close fd2 = %d\n", close(fd2));
	int fd3 = open("Pony", O_CREAT | O_EXCL, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
	perror("");
	//printf("close fd3 = %d\n", close(fd3));
	//printf("fd2 = %d\n", fd2);
	return 0;
}
