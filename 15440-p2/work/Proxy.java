/* Sample skeleton for proxy */

import java.io.*;

class FILES{
	boolean used;
	File file;
	RandomAccessFile raf;
	FILES() {
		used = false;
		file = null;
		raf = null;
	}
}

class Proxy {
	private static class FileHandler implements FileHandling {
		FILES[] fs = new FILES[1000];

		public int process (String path) {
			int fd = 0;
			while(fs[fd] !=null) {
				if(fs[fd].used !=false) {
					break;
				}
				fd++;
			}
			if(fd >= 1000) return Errors.EMFILE;
			fs[fd] = new FILES();
			fs[fd].file = new File(path);
			return fd;
		}

		public int open( String path, OpenOption o ) {
			System.err.println("open called");
			int fd = process(path);
			try{
				switch(o) {
					case CREATE:
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case CREATE_NEW:
						if(fs[fd].file.exists()) return Errors.EEXIST;
						if(!fs[fd].file.isDirectory()) return Errors.EISDIR;
						else
							fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case READ:
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "r");
						break;
					case WRITE:
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
				}			
			}catch (FileNotFoundException e) {
				e.printStackTrace();
				return Errors.ENOENT;
			}
			return fd;
		}

		public int close( int fd ) {
			System.err.println("close called");
			if(fd < 0 || fs[fd] == null) return Errors.EBADF;
			try{
				fs[fd].raf.close();
				fs[fd].used = false;
				fs[fd].raf = null;
				fs[fd].file = null;
			}catch (IOException e) {
				e.printStackTrace();
			}
			return 1;
		}

		public long write( int fd, byte[] buf ) {
			System.err.println("write called");
			if(fd < 0 || fs[fd] == null) return Errors.EBADF;
			try {
				fs[fd].raf.write(buf);
			}catch (IOException e) {
				e.printStackTrace();
			}
			return buf.length;
		}

		public long read( int fd, byte[] buf ) {
			System.err.println("read called");
			if(fd < 0 || fs[fd] == null) return Errors.EBADF;
			try {
				int ret = fs[fd].raf.read(buf);
				if(ret == -1) {
					return 0; // EOF
				}
				return ret;
			}catch (IOException e){
				e.printStackTrace();
			}
			return 0;
		}

		public long lseek( int fd, long pos, LseekOption o ) {
			System.err.println("lseek called");
			if(fd < 0 || fs[fd] == null) return Errors.EBADF;
			try{
				switch(o) {
					case FROM_CURRENT:
						fs[fd].raf.seek(pos);
						break;
					case FROM_END:
						fs[fd].raf.seek(fs[fd].raf.length() - pos);
						break;
					case FROM_START:
						fs[fd].raf.seek(0);
						fs[fd].raf.seek(pos);
						break;
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
			return 1;
		}

		public int unlink( String path ) {
			System.err.println("unlink called");
			try {
				File file = new File(path);
				if(file.delete()) {
					return 1;
				}else {
					if(!file.exists())  return Errors.ENOENT;
					if(file.isDirectory() && file.list().length > 0) return Errors.ENOTEMPTY;
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			return Errors.EPERM;
		}

		public void clientdone() {
			System.err.println("client done called");
			return;
		}
	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		System.err.println("Hello World");
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
