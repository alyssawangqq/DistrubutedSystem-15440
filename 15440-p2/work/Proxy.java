/* Sample skeleton for proxy */

import java.io.*;

class Proxy {

	private static class FileHandler implements FileHandling {

		public int open( String path, OpenOption o ) {
			System.out.println("open called");
			return Errors.ENOSYS;
		}

		public int close( int fd ) {
			System.out.println("close called");
			return Errors.ENOSYS;
		}

		public long write( int fd, byte[] buf ) {
			System.out.println("write called");
			return Errors.ENOSYS;
		}

		public long read( int fd, byte[] buf ) {
			System.out.println("read called");
			return Errors.ENOSYS;
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
