import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.AlreadyBoundException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.rmi.Naming;
import java.rmi.Remote;

public class Server extends UnicastRemoteObject implements IServer{
    public static boolean isFrontTier = false;
    public static boolean isMaster = false;
    //public static UnicastRemoteObject masterBase;
    public static String [] rmiRegistryList; // 100 front tier VM most // TODO hashtable to map id and name?
    public static int [] midTierList; // 100 mid tier VM most
    public static LinkedList<Cloud.FrontEndOps.Request> requestQueue;
    public static int frontTierCnt = 0;
    public static int midTierCnt = 0;
    public static enum tier { front, mid };
    public Server() throws RemoteException {}

    // java RMI
    public static IServer getMasterInstance(String ip, int port) throws RemoteException {
        String url = String.format("//%s:%d/Master", ip, port);
        try{
            return (IServer) Naming.lookup (url);
	} catch (Exception e) {
	    System.err.println(e);
	    return null;
	}
    }

    public static boolean registeMaster(String ip, int port) throws RemoteException {
	Server server = null;
	//isFrontTier = true; // Master is also front tier ?
	isMaster = true;
	requestQueue = new LinkedList<Cloud.FrontEndOps.Request>();
	rmiRegistryList = new String[100];
	midTierList = new int[100];
	try{
	    server = new Server();
	}
	catch(RemoteException e) {
	    System.err.println("Failed to create server " + e);
	    System.exit(1);
	}

	try {
	    Naming.bind(String.format("//%s:%d/Master", ip, port), server);
	    System.out.println("I m master node");
	    return true;
	}
	catch (AlreadyBoundException e) {
	    System.err.println(e);
	    return false;
	}
	catch (RemoteException e) { 
	    System.err.println(e);
	    return false;
	}
	catch (MalformedURLException e) { 
	    System.err.println(e);
	    return false;
	}
    }
    
    // operation
    public synchronized Cloud.FrontEndOps.Request pollRequest()
      throws RemoteException{
	  return requestQueue.poll();
      }

    public synchronized void addRequest(Cloud.FrontEndOps.Request r) 
      throws RemoteException{
	  requestQueue.add(r);
      }

    public Cloud.FrontEndOps.Request peekRequest() 
      throws RemoteException{
	return requestQueue.peek();
      }

    public int getRequestLength()
      throws RemoteException{
	  return requestQueue.size();
      }

    private boolean flag = true;

    public boolean assignTier() {
	flag = !flag;
	return flag;
    }

    public synchronized void addVMCount(tier t) {
	if(t == t.front) { frontTierCnt++; }
	if(t == t.mid) { midTierCnt++; }
    }

    public static int getFrontVMNumb() {
	return frontTierCnt;
    }

    public static int getMidVMNumb() {
	return midTierCnt;
    }

    public static String[] getRmiList(String ip, int port) throws RemoteException {
	try {
	    return Naming.list("//" + ip + ":" + port);
	}catch (Exception e){
	    System.err.println(e);
	    return null;
	}
    }

    public static void main ( String args[] ) throws Exception {
	if (args.length != 2) throw new Exception("Need 2 args: <cloud_ip> <cloud_port>");

	ServerLib SL = new ServerLib( args[0], Integer.parseInt(args[1]) );
	int port = Integer.parseInt(args[1]);
	String ip = args[0];
	IServer master = null;

	if(!registeMaster(ip, port)) {
	    master = getMasterInstance(ip, port);
	    isFrontTier = master.assignTier();
	    if(!isFrontTier) {
		// handle request
	    }else {
		// register
		SL.register_frontend();
	    }
	}

	// main loop
	while (true) {
	    if(SL.getStatusVM(5) == Cloud.CloudOps.VMStatus.NonExistent) {
		SL.startVM();
	    } else {
		if(isFrontTier) {
		    Cloud.FrontEndOps.Request r = SL.getNextRequest();
		    if(master != null) master.addRequest(r);
		}else{
		    if (master != null && master.getRequestLength() != 0)
		      SL.processRequest(master.pollRequest());
		}
	    }
	}
    }
}

