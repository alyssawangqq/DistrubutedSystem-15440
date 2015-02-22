import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class ClientRMIExample {

	public static IServer getServerInstance(String ip, int port) {
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

	public static void main(String [] args) {
		String address = args[0];
		int port = Integer.parseInt(args[1]); //you should get port from args
		IServer server = null;
		try{
			server = getServerInstance(address, port);
		}
		catch(Exception e) {
			e.printStackTrace(); //you should actually handle exceptions properly
		}

		if(server == null) System.exit(1); //You should handle errors properly.

		try {
			String hello = server.sayHello();
			System.out.println("Server said " + hello);
		}
		catch(RemoteException e) {
			System.err.println(e); //probably want to do some better logging here.
		}
	}
}
