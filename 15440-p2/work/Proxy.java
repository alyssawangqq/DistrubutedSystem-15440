/* Sample skeleton for proxy */ 
import java.io.*;
import java.util.Hashtable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

class FILES{
	File file;
	RandomAccessFile raf;
	FILES() {
		file = null;
		raf = null;
	}
}

class Proxy{
	public static int port;
	public static int cache_size;
	public static String server_addr;
	public static String path;

	private static class FileHandler implements FileHandling {
		FILES[] fs = new FILES[1000];
		IServer server = null;
		Hashtable<String, Integer> proxy_version = new Hashtable<String, Integer>();

		public static IServer getServerInstance(String ip, int port){
			String url = String.format("//%s:%d/ServerService", ip, port);
			try {
				return (IServer) Naming.lookup (url);
			} catch (MalformedURLException e) {
				//you probably want to do logging more properly
				System.err.println("Bad URL" + e);
			} catch (RemoteException e) {
				System.err.println("Remote connection refused to url "+ url + " " + e);
			} catch (NotBoundException e) {
				System.err.println("Not bound " + e);
			}
			return null;
		}

		public int compareVersion(String path) {
			//get server & check version
			try{
				server = getServerInstance(server_addr, port);
			}
			catch(Exception e) {
				e.printStackTrace(); //you should actually handle exceptions properly
			}
			if(server == null) System.exit(1); //You should handle errors properly.
			try {
				String hello = server.sayHello();
				System.err.println("Server said " + hello);
				int server_version = server.getVersion(path);
				int local_version = proxy_version.get(path);
				if(!proxy_version.containsKey(path) || server_version > local_version) {
					//get whole file
					System.err.println("need to get whole file");
					int len = server.getFileLen(path);
					long start = 0;
					try {
						BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(path));
						while (len > 0) {
							byte data[] = server.downloadFile(path, start, 20000);
							len -= 20000;
							start += 20000;
							output.write(data, 0, data.length);
						}
						output.flush();
						output.close();
					}catch (Exception e) {
						e.printStackTrace();
					}
					return 0;
				} 
				if(server_version == -1) return -1; // file not found on server
				//if(server_version > local_version) {System.err.println("server have new version");} //TODO get file
				if(server_version == local_version) {System.err.println("same version"); return 1;} //TODO just read local file
				if(server_version < local_version) {System.err.println("client have new version"); return 2;} //TODO maybe push to server
			}
			catch(RemoteException e) {
				System.err.println(e); //probably want to do some better logging here.
			}
			return 0;
		}

		public synchronized int process (String path) {
			//get fd
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
			//switch(compareVersion(path)) {
			//	case -1:
			//		//not on server
			//		break;
			//	case 0:
			//		//get file
			//		break;
			//	case 1:
			//		//same file
			//		break;
			//	case 2:
			//		//client newer
			//		break;
			//}
			if(fs[fd].file.isDirectory()) return fd + 2048;
			try{
				switch(o) {
					case CREATE:
						System.err.println("CREATE");
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case CREATE_NEW:
						System.err.println("CREATE_NEW");
						//if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						if(fs[fd].file.exists()) return Errors.EEXIST; 
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
					case READ:
						System.err.println("READ");
						//if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						if(!fs[fd].file.exists()) return Errors.ENOENT;
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "r");
						break;
					case WRITE:
						System.err.println("WRITE");
						//if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						if(!fs[fd].file.exists()) return Errors.ENOENT;
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						break;
				}			
			}catch (FileNotFoundException e) {
				e.printStackTrace();
				return Errors.ENOENT;
				//return -1;
			}
			return fd + 2048;
		}

		public int close_all() {
			int fd = 0;
			System.err.println("close all fd");
			while(fd < 1000) {
				if(fs[fd] != null) close(fd);
				fd++;
			}
			return 0;
		}

		public synchronized int close( int fd ) {
			System.err.println("close called for fd" + fd);
			if(fd >= 2048) {
				fd -= 2048;
			}
			//if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) {System.err.println("close err"); return Errors.EBADF;}
			if(fs[fd] == null) System.err.println("null fs");
			if(fs[fd].raf == null) System.err.println("null raf");
			if(fs[fd].file == null) System.err.println("file null");
			try{
				if(fs[fd].raf != null) fs[fd].raf.close();
				fs[fd].raf = null;
				fs[fd].file = null;
				fs[fd] = null;
			}catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			return 0;
		}

		public synchronized long write( int fd, byte[] buf ) {
			System.err.println("write called for fd" + fd);
			if(fd >= 2048) {
				fd -= 2048;
			}
			//if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) {System.err.println("write err"); return Errors.EBADF;}
			if(fs[fd].file.isDirectory()) return Errors.EISDIR;
			try {
				fs[fd].raf.write(buf);
			}catch (IOException e) {
				e.printStackTrace();
				System.err.println("write exception");
				return Errors.EBADF;
			}
			return buf.length;
		}

		public synchronized long read( int fd, byte[] buf ) {
			System.err.println("read called for fd" + fd);
			if(fd >= 2048) {
				fd -= 2048;
			}
			//if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) {System.err.println("read err"); return Errors.EBADF;}
			if(fs[fd].file.isDirectory()) return Errors.EISDIR;
			try {
				int ret = fs[fd].raf.read(buf);
				if(ret == -1) {
					return 0; // EOF
				}
				return ret;
			}catch (IOException e){
				e.printStackTrace();
				System.err.println("read exception");
				return Errors.EBADF;
			}
			//return 0;
		}

		public synchronized long lseek( int fd, long pos, LseekOption o ) {
			System.err.println("lseek called for fd" + fd);
			if(fd >= 2048) {
				fd -= 2048;
			}
			//if(fd < 0 || fs[fd] == null || fs[fd].raf == null || fs[fd].file == null) return Errors.EBADF;
			if(fs[fd].file.isDirectory()) return Errors.EISDIR;
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
				System.err.println("lseek exception");
				return -1;
			}
			return 0;
		}

		public synchronized int unlink( String path ) {
			System.err.println("unlink called for path" + path);
			try {
				File file = new File(path);
				if(!file.exists())  return Errors.ENOENT;
				if(file.isDirectory()) return Errors.EISDIR;
				if(!file.delete()) {
					return -1;
				}
			}catch(Exception e) {
				System.err.println("unlink exception");
				e.printStackTrace();
				return -1;
			}
			return 0;
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
		if(args.length < 4) return;
		Proxy.server_addr = args[0];
		Proxy.port = Integer.parseInt(args[1]);
		Proxy.path = args[2];
		Proxy.cache_size = Integer.parseInt(args[3]);
		//(new Thread(new RPCreceiver(new FileHandlingFactory()))).start();
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
