import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer extends Remote {
	public String sayHello() throws RemoteException;
	public int getVersion(String path) throws RemoteException;
	public boolean sendFile(String path) throws RemoteException;
	public boolean recvFile() throws RemoteException;
}
