import java.rmi.Remote;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public interface IServer extends Remote {
    public boolean assignTier() throws RemoteException;
    public Cloud.FrontEndOps.Request pollRequest() throws RemoteException;
    public Cloud.FrontEndOps.Request peekRequest() throws RemoteException;
    public int getRequestLength() throws RemoteException;
    public void addRequest(Cloud.FrontEndOps.Request r) throws RemoteException;
}
