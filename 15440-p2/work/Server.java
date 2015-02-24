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
	public static String root_path;
	public Server() throws RemoteException {}
	public String sayHello() throws RemoteException{
		return "Hello :)";
	}

	public int getVersion(String path) throws RemoteException {
		File f = new File(root_path+"/"+path);
		if(!f.exists()) return -1; //-1 returned by server means file not exist on server
		if(!server_version.containsKey(root_path+"/"+path)) {server_version.put(root_path+"/"+path, 0); return 0;} //0 means init version
		return server_version.get(root_path+"/"+path);
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
