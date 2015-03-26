#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main(int argc, char* argv[]) {
	int fd = open (argv[1], O_RDONLY);
	char buf[100];
	int ret = read(fd, buf, 5);
	sleep(5);
	//printf("%s", buf);
	sleep(5);
	read(fd, buf + ret, 4);
	printf("%s \n", buf);
	close(fd);
}
