#include "../include/dirtree.h"

int main() {
  struct dirtreenode* dir = getdirtree("./");
  freedirtree(dir);

  return 0;
}
