import java.rmi.Remote;
import java.rmi.RemoteException;

public class Protocol {

	public interface IServer extends Remote {
		public String sayHello() throws RemoteException;
	}

	class interface IProxy {
		public static IServer getServerInstance(String ip, int port);
	}
}
