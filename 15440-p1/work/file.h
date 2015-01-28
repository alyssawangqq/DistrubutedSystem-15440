#ifndef __FILE_H
#include <stdlib.h>

typedef struct Array {
	int* data;
	int size;
}Array;

typedef struct File {
	Array path;
	Array content;
}File;

Array Concat(Array a, Array b) {
	int new_size = a.size + b.size;
	Array* new_arr = malloc(new_size); // malloc
	for(int i = 0; i < a.size; i++) {
		new_arr->data[i] = a.data[i];
	}
	for(int i = a.size; i < new_size; i++) {
		new_arr->data[i] = a.data[i];
	}
	return *new_arr;
}

Array SerializeFile(File file) {
	return Concat(file.path, file.content);
}

File DeSerializeFile(Array a) {
	int incoming_size = a.size;
}

#endif
