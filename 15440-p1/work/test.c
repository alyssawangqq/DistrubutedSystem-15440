int main() {
  char buff[4096];
  int fd = open("localFile", 0, 0);
  read(1000, buff, 1024);
  fd = open("my_favorite_songs", 0, 0);
  read(fd, buff, 1024);
  close(fd);
  write(-1, buff, 1024);
  close(4321);
  lseek(15440, 100, 0);
  fd = open("foo", 0, 0);
  lseek(fd, 10000, -1);
  close(fd);
  __xstat(1, "localFile", (struct stat*)buff);
  unlink("localFile");
  unsigned long i = 0;
  getdirentries(-10, buff, 1024, &i);
}
