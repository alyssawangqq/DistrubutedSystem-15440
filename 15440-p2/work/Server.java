import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;

//You should investigate when to use UnicastRemoteObject vs Serializable. This is really important!
public class Server extends UnicastRemoteObject implements IServer {

	public Server() throws RemoteException {}

	public String sayHello() throws RemoteException{
		return "Hello :)";
	}

	public static void main(String [] args) {
		if(args.length < 2) return;
		int port = Integer.parseInt(args[0]);
		String path = args[1];

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
