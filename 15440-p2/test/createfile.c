#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main(int argc, char* argv[]) {
	char buf[11] = "unlinktest\n";
	sleep(1);
	int fd = open(argv[1], O_CREAT | O_EXCL);
	sleep(3);
	write(fd, buf, 11);
	close(fd);
	return 0;
}
