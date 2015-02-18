/* Sample skeleton for proxy */

import java.io.*;

class FILES{
	//boolean used;
	File file;
	RandomAccessFile raf;
	FILES() {
		//used = false;
		file = null;
		raf = null;
	}
}

class Proxy {
	private static class FileHandler implements FileHandling {
		FILES[] fs = new FILES[1000];

		public synchronized int process (String path) {
			int fd = 0;
			if(fd >= 1000) return Errors.EMFILE;
			while(fs[fd] !=null) {
				fd++;
			}
			fs[fd] = new FILES();
			fs[fd].file = new File(path);
			return fd;
		}

		public synchronized int open( String path, OpenOption o ) {
			System.err.println("open called for path" + path);
			int fd = process(path);
			try{
				switch(o) {
					case CREATE:
						System.err.println("CREATE");
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case CREATE_NEW:
						System.err.println("CREATE_NEW");
						if(fs[fd].file.exists()) return Errors.EEXIST;
						if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case READ:
						System.err.println("READ");
						if(!fs[fd].file.exists()) return Errors.ENOENT;
						if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "r");
						break;
					case WRITE:
						System.err.println("WRITE");
						if(!fs[fd].file.exists()) return Errors.ENOENT;
						if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
				}			
			}catch (FileNotFoundException e) {
				e.printStackTrace();
				return Errors.ENOENT;
			}
			return fd;
		}

		public int close_all() {
			int fd = 0;
			System.err.println("close all fd");
			while(fd++ < 1000) {
				if(fs[fd] != null) close(fd);
			}
			return 1;
		}

		public synchronized int close( int fd ) {
			System.err.println("close called for fd" + fd);
			if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) return Errors.EBADF;
			try{
				fs[fd].raf.close();
				fs[fd].raf = null;
				fs[fd].file = null;
				fs[fd] = null;
			}catch (IOException e) {
				e.printStackTrace();
			}
			return 1;
		}

		public synchronized long write( int fd, byte[] buf ) {
			System.err.println("write called for fd" + fd);
			if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) return Errors.EBADF;
			try {
				fs[fd].raf.write(buf);
			}catch (IOException e) {
				e.printStackTrace();
				return Errors.EBADF;
			}
			return buf.length;
		}

		public synchronized long read( int fd, byte[] buf ) {
			System.err.println("read called for fd" + fd);
			if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) return Errors.EBADF;
			try {
				int ret = fs[fd].raf.read(buf);
				if(ret == -1) {
					return 0; // EOF
				}
				return ret;
			}catch (IOException e){
				e.printStackTrace();
				return Errors.EBADF;
			}
			//return 0;
		}

		public synchronized long lseek( int fd, long pos, LseekOption o ) {
			System.err.println("lseek called for fd" + fd);
			if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) return Errors.EBADF;
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

		public synchronized int unlink( String path ) {
			System.err.println("unlink called for path" + path);
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
			close_all();
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
		//(new Thread(new RPCreceiver(new FileHandlingFactory()))).start();
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
