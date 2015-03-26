#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main(int argc, char* argv[]) {
	int fd = open (argv[1], O_RDWR);
	char buf[9] = "Version1\n";
	lseek(fd,0, SEEK_SET);
	sleep(5);
	int ret1 = write(fd, buf, 5);
	sleep(5);
	int ret2 = write(fd, buf + ret1, 4);
	sleep(1);
	close(fd);
}
