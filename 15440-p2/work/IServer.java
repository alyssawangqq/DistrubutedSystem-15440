import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer extends Remote {
	public String sayHello() throws RemoteException;
	public int getVersion(String path) throws RemoteException;
	public boolean sendFile(String path) throws RemoteException;
	public boolean recvFile() throws RemoteException;
	//public boolean openStream(String path)throws RemoteException;
	//public boolean closeStream(String path) throws RemoteException;
	public byte[] downloadFile(String path, long n, int len) throws RemoteException;
	public boolean uploadFile(String path, byte[] buffer, long pos, int len) throws RemoteException;
	public int getFileLen(String path) throws RemoteException;
}
