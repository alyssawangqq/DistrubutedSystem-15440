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
		public int open( String path, OpenOption o ) {
			int fd = 0;
			System.out.println("open called");
			//while(fs[fd] != null && fs[fd].used != false) {
			//	fd++;
			//}
			while(fs[fd] !=null) {
				if(fs[fd].used !=false) {
					break;
				}
				fd++;
			}
			fs[fd] = new FILES();
			fs[fd].file = new File(path);
			try{
				switch(o) {
					case CREATE:
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case CREATE_NEW:
						if(fs[fd].file.exists() && !fs[fd].file.isDirectory()) return -1;
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
			}catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			return fd;
			//return Errors.ENOSYS;
		}

		public int close( int fd ) {
			System.out.println("close called");
			try{
				fs[fd].raf.close();
				fs[fd].used = false;
				fs[fd].raf = null;
				fs[fd].file = null;
			}catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			//return Errors.ENOSYS;
			return 1;
		}

		public long write( int fd, byte[] buf ) {
			System.out.println("write called");
			return Errors.ENOSYS;
		}

		public long read( int fd, byte[] buf ) {
			System.out.println("read called");
			try {
				fs[fd].raf.read(buf);
			}catch (IOException e){
				e.printStackTrace();
				return -1;
			}
			return buf.length;
			//return Errors.ENOSYS;
		}

		public long lseek( int fd, long pos, LseekOption o ) {
			System.out.println("lseek called");
			return Errors.ENOSYS;
		}

		public int unlink( String path ) {
			System.out.println("unlink called");
			return Errors.ENOSYS;
		}

		public void clientdone() {
			System.out.println("client done called");
			return;
		}

	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Hello World");
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
