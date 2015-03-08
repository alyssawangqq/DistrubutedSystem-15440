/* Sample skeleton for proxy */ 
import java.io.*;
import java.util.Hashtable;
import java.util.LinkedList;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

class FILES{
	String path;
	File file;
	boolean modified;
	RandomAccessFile raf;
	FILES() {
		path = null;
		modified = false;
		file = null;
		raf = null;
	}
}

class Node<T> {
	public Node<T> prev;
	public Node<T> next;
	public T data;

	public Node(T d, Node<T> p, Node<T> n) {
		data = d;
		prev = p;
		next = n;
	}

	public void remove() {
		Node<T> p = prev;
		Node<T> n = next;
		p.next = n;
		n.prev = p;
	}
}

class LList<T> {
	public LList() {
		head = new Node<T>(null, null, null);
		head.prev = head;
		head.next = head;
	}

	public boolean empty() {
		return head.next == head;
	}

	public Node<T> front() {
		return head.next;
	}

	public Node<T> back() {
		return head.prev;
	}

	public void append(T data) {
		Node<T> n = new Node<T>(data, back(), head);
		back().next = n;
		head.prev = n;
	}

	private Node<T> head;
}


class VersionList{
	int version;
	boolean inUse;
	Node<String> node;
	VersionList(int v, Node<String> i, boolean use) {
		version = v;
		node = i;
		inUse = use;
	}
}

class Proxy{
	public static int port;
	public static int cache_size;
	public static int remain_size;
	public static int used_slot = 0;
	public static String server_addr;
	public static String path;
	public static IServer server = null;
	public static Hashtable<String, VersionList> proxy_version = new Hashtable<String, VersionList>();
	public static LList<String> cache = new LList<String>();

	private static class FileHandler implements FileHandling {
		FILES[] fs = new FILES[1000];
		FILES[] fs_private = new FILES[1000];

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

		public boolean handle_getFile(String path, int len) {
			long start = 0;
			try {
				if(len < 0) return false; // file not exists
				//check if need create dir
				File tmp = new File(Proxy.path + path);
				if(!tmp.getParentFile().exists()) new File(tmp.getParent()).mkdirs();
				// if need, create dir
				BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(Proxy.path+path));
				while (len > 1000000) { // memory limit
					System.err.println("len is: "+len+" start is: "+start);
					byte data[] = server.downloadFile(path, start, 1000000);
					len -= data.length;
					output.write(data, 0, data.length);
					start += data.length;
				}
				while (len > 0) {
					System.err.println("len is: "+len+" start is: "+start);
					byte data[] = server.downloadFile(path, start, len);
					len-=data.length;
					output.write(data, 0, data.length);
					start += data.length;
				}
				output.flush();
				output.close();
				//init version
			}catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true; //succes
		}

		public boolean handle_uploadFile(int fd) {
			if(fd >= 2048) {
				fd -= 2048;
			}
			System.err.println("handle upload called for fd: " + fd);
			long start = 0;
			int len = (int)fs_private[fd].file.length();
			System.err.println("handle upload file len: " + len);
			byte buffer[] = new byte[1000000];
			try {
				while (len > 1000000) {
					//byte buffer[20000000];
					fs_private[fd].raf.seek(start);
					int ret = fs_private[fd].raf.read(buffer, 0, 1000000);
					if(ret < 0) {
						break;
					}
					server.uploadFile(fs[fd].path, buffer, start, ret);
					len -= ret;
					start += ret;
				}
				while (len > 0) {
					fs_private[fd].raf.seek(start);
					int ret = fs_private[fd].raf.read(buffer, 0, len);
					System.err.println("handle upload read ret: " + ret);
					if(ret < 0) {
						break;
					}
					server.uploadFile(fs[fd].path, buffer, start, ret);
					System.err.println(fs[fd].path);
					len -= ret;
					start += ret;
				}
			}catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true; //TODO false
		}

		public boolean handle_rmFile(String path) {
			try {
				if(!server.rmFile(path)) return false;
			}catch(Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		public int compareVersion(String path) {
			//get server & check version
			int len = 0;
			try{
				len = server.getFileLen(path);
			}
			catch(Exception e) {
				e.printStackTrace(); //you should actually handle exceptions properly
			}
			if(len > cache_size) return -3;
			if(server == null) System.exit(1); //You should handle errors properly.
			try {
				int server_version = server.getVersion(path);
				if(server_version == -1) return -1; // file not found on server
				System.err.println("Server version " + server_version);
				if(!proxy_version.containsKey(path)) {
					//get whole file
					System.err.println("Clietn version null ");
					System.err.println("need to get whole file");
					if(remain_size > len) {
						if(!handle_getFile(path, len)) return -2; // fail to get file
						remain_size -= len;
						//proxy_version.put(path, server_version); // update version // change to class
						cache.append(path);
						VersionList node = new VersionList(server_version, cache.back(), true);
						proxy_version.put(path, node);
						return 3;
					}else {
						System.err.println("do LRU"); // TODO cannot close file that being opened
						while(remain_size < len && !cache.empty()) {
							Node ptr = cache.front();
							while(proxy_version.get(ptr.data.toString()).inUse && ptr != null) ptr = ptr.next; // find the first not in use
							File tmp = new File(Proxy.path + ptr.data.toString());
							remain_size += tmp.length();
							System.err.println("remain_size: " + remain_size);
							System.err.println("cache front: " + ptr.data.toString());
							System.err.println("cache back: " + ptr.data.toString());
							cache_unlink(ptr.data.toString());
							ptr.remove();
						}
						if(remain_size < len) {
							//TODO file too big
							System.err.println("FILE TOO BIG");
							return -3;
						}else {
							if(!handle_getFile(path, len)) return -1;
							cache.append(path);
							VersionList node = new VersionList(server_version, cache.back(), true);
							proxy_version.put(path, node);
							remain_size -= len;
						}
					}
				} 

				int local_version = proxy_version.get(path).version;
				System.err.println("Client version " + local_version);
				if(server_version > local_version) {
					File orig = new File(Proxy.path + path);
					//proxy_version.get(path).version = server_version;
					remain_size += orig.length();
					proxy_version.get(path).node.remove();
					//cache_unlink(path); //TODO  may useful
					//remove origin file first
					System.err.println("server have new version"); 
					while(remain_size < len && !cache.empty()) {
							Node ptr = cache.front();
							while(proxy_version.get(ptr.data).inUse && ptr!=null) ptr = ptr.next; // find the first not in use
							File tmp = new File(Proxy.path + ptr.data);
							remain_size += tmp.length();
							System.err.println("remain_size: " + remain_size);
							System.err.println("cache front: " + ptr.data);
							System.err.println("cache back: " + ptr.data);
							cache_unlink(ptr.data.toString());
							ptr.remove();
					}
					if(remain_size < len) {
						//TODO file too big
						System.err.println("FILE TOO BIG");
					}else {
						if(!handle_getFile(path, len)) return -1;
						cache.append(path);
						VersionList node = new VersionList(server_version, cache.back(), true);
						proxy_version.put(path, node);
						//proxy_version.get(path).node = cache.back();
						remain_size -= len;
					}
					return 0;
				} //get file
				if(server_version == local_version) {
					System.err.println("same version");
					proxy_version.get(path).node.remove();
					cache.append(path);
					proxy_version.get(path).node = cache.back();
					return 1;
				} //TODO just read local file, call hit()
				if(server_version < local_version) {System.err.println("client have new version"); 
					return 2;
				} //TODO maybe push to server
				return 3;
			}
			catch(RemoteException e) {
				System.err.println(e); //probably want to do some better logging here.
			}
			return 3;
		}

		public synchronized int process (String path) {
			//get fd
			int fd = 0;
			if(fd >= 1000) return Errors.EMFILE;
			while(fs[fd] !=null) {
				fd++;
			}
			//public
			fs[fd] = new FILES();
			fs[fd].path = path;
			fs[fd].file = new File(Proxy.path+fs[fd].path);
			//private
			fs_private[fd] = new FILES();
			fs_private[fd].path = path + fd;
			fs_private[fd].file = new File(Proxy.path+fs_private[fd].path);
			return fd;
		}

		public static String simplifyPath(String path) {
			LinkedList<String> stack = new LinkedList<String>();
			String[] pathsplit = path.split("/");
			for(String p : pathsplit) {
				if( p.equals("..") && !stack.isEmpty() ) {  // stack pop
					stack.removeLast();
				} else if(p.length()!=0 && !p.equals(".") && !p.equals("..")) {
					stack.addLast(p);   // stack push
				}   // other cases: do nothing
			}
			StringBuilder sb = new StringBuilder();
			if(stack.isEmpty()) return "/"; //!!! corner case
			for(String p : stack) { // build output
				sb.append("/");
				sb.append(p);
			}
			return sb.toString();
		}

		public synchronized int open( String path, OpenOption o ) {
			System.err.println("open called for path: " + Proxy.path + path);
			try{
				server = getServerInstance(server_addr, port);
				//change path
				//permission check
				String c_path = new File(Proxy.path + path).getCanonicalPath();
				String dir_path = new File(Proxy.path).getCanonicalPath();
				if(!c_path.contains(dir_path)) return Errors.EPERM;
				//simpe path
				path = simplifyPath(path);
				System.err.println("path is: " + path);
			}catch(Exception e) {
				e.printStackTrace();
			}
			int fd = process(path);
			int compareVRet = compareVersion(path); // if miss && full, cached file should be delete before get file, remain_size should be add TODO
			switch(compareVRet) { // for errno
				case -3:
					return Errors.ENOMEM;
				case -2:
					//get file fail
					break;
				case -1:
					//not on server
					System.err.println("not on server");
					//return Errors.ENOENT;
				case 0:
					//if(!handle_getFile(path)) System.err.println("get file fail");
					//get file
					break;
				case 1:
					//same file
					break;
				case 2:
					//if(!handle_uploadFile(path)) System.err.println("upload file fail");
					//should not be happen
					//client newer
					break;
				case 3:
					//success
					break;
			}
			if(fs[fd].file.isDirectory()) return fd + 2048;
			try{
				switch(o) {
					case CREATE:
						System.err.println("CREATE");
						if(compareVRet == -1){ 
							cache.append(fs[fd].path);
							if(!fs[fd].file.exists()) fs[fd].modified = true;
							proxy_version.put(path, new VersionList(0, cache.back(), true)); //not on server and create
						}
						//fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						fs_private[fd].raf = new RandomAccessFile(fs_private[fd].file, "rw");
						break;
					case CREATE_NEW:
						System.err.println("CREATE_NEW");
						//TODO create file on server
						if(fs[fd].file.exists() || compareVRet != -1) return Errors.EEXIST; 
						//fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
						fs[fd].modified = true;
						fs_private[fd].raf = new RandomAccessFile(fs_private[fd].file, "rw");
						if(compareVRet == -1){ 
							cache.append(fs[fd].path);
							proxy_version.put(path, new VersionList(0, cache.back(), true)); //not on server and create
						}
						break;
					case READ:
						System.err.println("READ");
						//if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						if(!fs[fd].file.exists() || compareVRet == -1) return Errors.ENOENT;
						fs[fd].raf = new RandomAccessFile(fs[fd].file, "r");
						break;
					case WRITE:
						System.err.println("WRITE");
						//if(fs[fd].file.isDirectory()) return Errors.EISDIR;
						if(!fs[fd].file.exists() || compareVRet == -1) return Errors.ENOENT;
						fs_private[fd].raf = new RandomAccessFile(fs_private[fd].file, "rw");
						//fs[fd].raf = new RandomAccessFile(fs[fd].file, "rw");
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
			if(fs[fd].modified) {
				if(!handle_uploadFile(fd)) return -1;
				fs[fd].modified = false;
				if(fs[fd].file.exists()) fs[fd].file.delete();
				fs_private[fd].file.renameTo(fs[fd].file);
				proxy_version.get(fs[fd].path).version += 1;
			}
			if(fs[fd] == null) System.err.println("null fs");
			if(fs[fd].raf == null) System.err.println("null raf");
			if(fs[fd].file == null) System.err.println("file null");
			try{
				if(fs[fd].raf != null) fs[fd].raf.close();
				//clean inUse, and set it to top of the queue
				proxy_version.get(fs[fd].path).inUse = false; //its a must
				proxy_version.get(fs[fd].path).node.remove();
				cache.append(fs[fd].path);
				proxy_version.get(fs[fd].path).node = cache.back();
				//clean public
				fs[fd].raf = null;
				fs[fd].file = null;
				fs[fd].path = null;
				fs[fd] = null;
				//clean private
				fs_private[fd].raf = null;
				fs_private[fd].file = null;
				fs_private[fd].path = null;
				fs_private[fd] = null;
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
			if(fs[fd].file.isDirectory()) return Errors.EISDIR;
			try {
				fs_private[fd].raf.write(buf);
			}catch (IOException e) {
				e.printStackTrace();
				System.err.println("write exception");
				return Errors.EBADF;
			}
			fs[fd].modified = true; // modified
			return buf.length;
		}

		public synchronized long read( int fd, byte[] buf ) {
			System.err.println("read called for fd" + fd);
			if(fd >= 2048) {
				fd -= 2048;
			}
			if(fs[fd].file.isDirectory()) return Errors.EISDIR;
			try {
				int ret = fs[fd].raf.read(buf);
				System.err.println("read ret: "+ret);
				if(ret == -1) {
					return 0; // EOF
				}
				return ret;
			}catch (IOException e){
				e.printStackTrace();
				System.err.println("read exception");
				return Errors.EBADF;
			}
		}

		public synchronized long lseek( int fd, long pos, LseekOption o ) { //TODO lseek write, maybe read
			System.err.println("lseek called for fd" + fd);
			if(fd >= 2048) {
				fd -= 2048;
			}
			if(fs[fd].file.isDirectory()) return Errors.EISDIR;
			try{
				switch(o) {
					case FROM_CURRENT:
						System.err.println("From current +: " + pos);
						if(fs[fd].raf != null) fs[fd].raf.seek(fs[fd].raf.getFilePointer() + pos);
						else 
							System.err.println("fs raf is null");
						if(fs_private[fd].raf != null) fs_private[fd].raf.seek(fs_private[fd].raf.getFilePointer() + pos);
						else 
							System.err.println("fs_private raf is null");
						break;
					case FROM_END:
						System.err.println("length is: " +fs[fd].raf.length() + "From end +: " + pos);
						if(fs[fd].raf != null) fs[fd].raf.seek(fs[fd].raf.length() - pos);
						else 
							System.err.println("fs raf is null");
						if(fs_private[fd].raf != null) fs_private[fd].raf.seek(fs_private[fd].raf.length() - pos);
						else 
							System.err.println("fs_private raf is null");
						break;
					case FROM_START:
						System.err.println("From start +: " + pos);
						if(fs[fd].raf != null) fs[fd].raf.seek(pos);
						else 
							System.err.println("fs raf is null");
						if(fs_private[fd].raf != null) fs_private[fd].raf.seek(pos);
						else 
							System.err.println("fs_private raf is null");
						break;
				}
			}catch(IOException e) {
				e.printStackTrace();
				System.err.println("lseek exception");
				return -1;
			}
			return 0;
		}

		public synchronized int cache_unlink( String path ) {
			System.err.println("cache unlink called for path" + path);
			try {
				File file = new File(Proxy.path+path);
				if(!file.exists())  return Errors.ENOENT;
				if(file.isDirectory()) return Errors.EISDIR;
				if(!file.delete()) { System.err.println("unlink fail locally"); return -1;}
				proxy_version.remove(path);
			}catch(Exception e) {
				System.err.println("unlink exception");
				e.printStackTrace();
				return -1;
			}
			return 0;
		}

		public synchronized int unlink( String path ) {
			System.err.println("unlink called for path" + path);
			try {
				File file = new File(Proxy.path+path);
				if(!file.exists())  return Errors.ENOENT;
				if(file.isDirectory()) return Errors.EISDIR;
				if(!file.delete()) { System.err.println("unlink fail locally"); return -1;}
				proxy_version.remove(path);
				if(!handle_rmFile(path)) {System.err.println("unlink fail remotely"); return -1;}
			}catch(Exception e) {
				System.err.println("unlink exception");
				e.printStackTrace();
				return -1;
			}
			return 0;
		}

		public void clientdone() {
			System.err.println("client done called");
			//close_all();
			return;
		}
	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		if(args.length < 4) return;
		Proxy.server_addr = args[0];
		Proxy.port = Integer.parseInt(args[1]);
		Proxy.path = args[2] + File.separator;
		Proxy.cache_size = Integer.parseInt(args[3]);
		Proxy.remain_size = cache_size;
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
