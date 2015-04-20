import java.rmi.Remote;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public interface IServer extends Remote {
    // master operation
    public boolean assignTier() throws RemoteException;
    public Request pollRequest() throws RemoteException;
    public Request peekRequest() throws RemoteException;
    public int getRequestLength() throws RemoteException;
    public void addRequest(Request r) throws RemoteException;
    public long getRequestStartTime(Request r) throws RemoteException;
    public void removeVM(boolean b) throws RemoteException;

    // master inspect
    public int addVM(int i, boolean b) throws RemoteException;
    public int getID() throws RemoteException;
    public int getVMNumber(boolean b) throws RemoteException;
    public int getRequestQueueLength() throws RemoteException;
    public int getVMNumb() throws RemoteException;
    public long getIncomingRate() throws RemoteException;

    // other
    public ServerLib getSL() throws RemoteException;
    public Cloud.DatabaseOps getRealDB() throws RemoteException;
    public void tellMasterNewReq(int length) throws RemoteException;
}
