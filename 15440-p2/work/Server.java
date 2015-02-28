import java.net.MalformedURLException;
import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
//import java.io.Serializable;
import java.util.Hashtable;

//You should investigate when to use UnicastRemoteObject vs Serializable. This is really important!
public class Server extends UnicastRemoteObject implements IServer {
	Hashtable<String, Integer> server_version = new Hashtable<String, Integer>();
	//Hashtable<String, BufferedOutputStream> bos = new Hashtable<String, BufferedOutputStream>();
	public static String root_path;

	public Server() throws RemoteException {}

	public String sayHello() throws RemoteException{
		return "Hello :)";
	}

	public int getVersion(String path) throws RemoteException {
		File f = new File(root_path+path);
		if(!f.exists()) return -1; //-1 returned by server means file not exist on server
		if(!server_version.containsKey(root_path+path)) {server_version.put(root_path+path, 0); return 0;} //0 means init version
		return server_version.get(root_path+path);
	}

	public int getFileLen(String path) throws RemoteException{
		File file = new File(root_path+path); // TODO can be non exits?
		System.err.println(root_path+path);
		return (int)file.length();
		//try {
		//	BufferedInputStream fis = new BufferedInputStream(new FileInputStream(path));
		//}catch (Exception e) {
		//	e.printStackTrace();
		//}
		//return fis.available();
	}

	public byte[] downloadFile(String path, long n, int len) {
		//int len = getFileLen(path);
		byte buffer[] = new byte[len];
		int off = 0;
		try {
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(root_path+path));
			if (input.skip(n) < 0){ 
				// skip fail
				System.err.println("skip err");
				return null;
			}
			while(len > 0) {
				System.err.println("off is: "+off+"len is: "+len);
				int ret = input.read(buffer, off, len);
				System.err.println("ret is: "+ret);
				off += ret;
				len -= ret;
			}
			input.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return(buffer);
	}

	//public boolean openStream(String path) { // opened when trying to write TODO should check if using //TODO maybe use later
	//	try{
	//		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(root_path+path));
	//		bos.put(path, output);
	//	}catch(Exception e) {
	//		e.printStackTrace();
	//		return false;
	//	}
	//	return true;
	//}

	//public boolean closeStream(String path) {
	//	try{
	//		bos.get(path).flush();
	//		bos.get(path).close();
	//		bos.remove(path);
	//	}catch(Exception e) {
	//		e.printStackTrace();
	//		return false;
	//	}
	//	return true;
	//}

	public boolean uploadFile(String path, byte[] buffer, long pos, int len) {
		//BufferedOutputStream output = bos.get(path);
		try {
			String abs_path = root_path + path;
			System.err.println("uploadFile Path: " + abs_path);
			System.err.println("uploadFile pos: " + pos);
			System.err.println("uploadFile len: " + len);
			File file = new File(abs_path);
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(pos);
			raf.write(buffer, 0, len);
			raf.close();
			//if(!output.write(buffer, 0, buffer.length)) return false;
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		//bos.put(path,bos.get(path)+1);
		return true;
	}

	public boolean sendFile(String path) throws RemoteException {
		return false;
	}

	public boolean recvFile() throws RemoteException {
		return false;
	}

	public static void main(String [] args) {
		if(args.length < 2) return;
		int port = Integer.parseInt(args[0]);
		root_path = args[1];

		try {
			//create the RMI registry if it doesn't exist.
			LocateRegistry.createRegistry(port);
		}
		catch(RemoteException e) {
			System.err.println("Failed to create the RMI registry " + e);
		}

		Server server = null;
		try{
			server = new Server(); 
		}
		catch(RemoteException e) {
			//You should handle errors properly.
			System.err.println("Failed to create server " + e);
			System.exit(1);
		}
		try {
			Naming.rebind(String.format("//127.0.0.1:%d/ServerService", port), server);
		} catch (RemoteException e) {
			System.err.println(e); //you probably want to do some decent logging here
		} catch (MalformedURLException e) {
			System.err.println(e); //same here
		}
	}
}
