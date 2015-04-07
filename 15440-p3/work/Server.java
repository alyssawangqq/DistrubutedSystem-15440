import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.AlreadyBoundException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.rmi.Naming;
import java.rmi.Remote;

class Properties {
    public static boolean isFrontTier = false;
    public static boolean isMaster = false;
    public static int myID = 0;
}

public class Server extends UnicastRemoteObject implements IServer{
    public static String [] rmiRegistryList; // 100 total registry most
    public static LinkedList<Cloud.FrontEndOps.Request> requestQueue;
    public static Hashtable<Integer, Boolean> id_roleTable = new Hashtable<Integer, Boolean>();
    public Server() throws RemoteException {}

    private static Properties vmProp = new Properties();
    private static ServerLib SL;
    private static int frontNumb = 0;
    private static int midNumb = 0;
    private static boolean lackFront = false;

    // java RMI
    public static IServer getInstance(String ip, int port, String name) throws RemoteException {
	String url = String.format("//%s:%d/%s", ip, port, name);
	try{
	    return (IServer) Naming.lookup (url);
	} catch (Exception e) {
	    System.err.println(e);
	    return null;
	}
    }

    public static boolean registMaster(String ip, int port) throws RemoteException {
	Server server = null;
	frontNumb += 1;
	requestQueue = new LinkedList<Cloud.FrontEndOps.Request>();
	rmiRegistryList = new String[100];
	try{
	    server = new Server();
	}
	catch(RemoteException e) {
	    System.err.println("Failed to create server " + e);
	    System.exit(1);
	}

	try {
	    Naming.bind(String.format("//%s:%d/%s", ip, port, "Master"), server);
	    System.err.println("I'm master node");
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

    public static boolean registFrontTier(String ip, int port, int id) throws RemoteException {
	Server server = null;
	vmProp.isFrontTier = true;
	try{
	    server = new Server();
	}
	catch(RemoteException e) {
	    System.err.println("Failed to create server " + e);
	    System.exit(1);
	}

	try {
	    Naming.bind(String.format("//%s:%d/%s", ip, port, "FrontTier" + id), server);
	    System.err.println("I'm FrontTier node " + id);
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

    public static boolean registMidTier(String ip, int port, int id) throws RemoteException {
	Server server = null;
	vmProp.isFrontTier = false;
	try{
	    server = new Server();
	}
	catch(RemoteException e) {
	    System.err.println("Failed to create server " + e);
	    System.exit(1);
	}

	try {
	    Naming.bind(String.format("//%s:%d/%s", ip, port, "MidTier" + id), server);
	    System.err.println("I'm MidTire node " + id);
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

    // master operation
    public synchronized Cloud.FrontEndOps.Request pollRequest()
      throws RemoteException{
	  return requestQueue.poll();
      }

    public synchronized void addRequest(Cloud.FrontEndOps.Request r) 
      throws RemoteException{
	  requestQueue.add(r);
      }

    public static synchronized void masterAddRequest(Cloud.FrontEndOps.Request r) {
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

    //private boolean flag = true;

    public boolean assignTier() {
	return lackFront;
	//flag = !flag;
	//if((midNumb / frontNumb) >= 2)
	//  return flag;
	//else 
	//  return !flag;
    }

    public synchronized int addVM(int id, boolean b) throws RemoteException{
	if(id_roleTable.get(id) == null) {
	    id_roleTable.put(id, b);
	    if(b) frontNumb += 1;
	    else midNumb += 1;
	    return getCnt(); 
	}else {
	    return -1; // Already exists
	}
    }

    public int getCnt() throws RemoteException{
	return id_roleTable.size();
    }

    public static void getRmiList(String ip, int port) throws RemoteException {
	try {
	    String [] tmp = Naming.list("//" + ip + ":" + port);
	    for(int i =0; i < tmp.length; i++)
	      System.err.println(tmp[i]);
	}catch (Exception e){
	    System.err.println(e);
	}
    }

    // tier operation
    public static void shutDown(int id, boolean isFront, String ip, int port)
      throws RemoteException {
	  IServer inst = null;
	  if(isFront) inst = getInstance(ip, port, "FrontTier" + id);
	  else inst = getInstance(ip, port, "MidTier" + id);

	  // do clean shutdown
	  ServerLib SLinst = inst.getSL();
	  SLinst.interruptGetNext();
	  SLinst.shutDown();
	  UnicastRemoteObject.unexportObject(inst, true);
      }

    public ServerLib getSL() {
	return SL;
    }

    public static void main ( String args[] ) throws Exception {
	if (args.length != 2) throw new Exception("Need 2 args: <cloud_ip> <cloud_port>");

	SL = new ServerLib( args[0], Integer.parseInt(args[1]) );
	int port = Integer.parseInt(args[1]);
	String ip = args[0];
	IServer master = null;

	if((vmProp.isMaster = registMaster(ip, port)) == false) {
	    master = getInstance(ip, port, "Master");
	    vmProp.isFrontTier = master.assignTier();
	    vmProp.myID = master.getCnt();
	    master.addVM(vmProp.myID, vmProp.isFrontTier);
	    if(vmProp.isFrontTier == false) {
		// handle request // mid tier
		registMidTier(ip, port, vmProp.myID);
	    }else {
		// register
		registFrontTier(ip, port, vmProp.myID);
		SL.register_frontend();
		//System.err.println(SL.getQueueLength());
	    }
	}else {
	    System.err.println("master register front");
	    SL.register_frontend(); // Regist Master
	    SL.startVM(); // create the first mid tier
	    midNumb += 1;
	}

	//getRmiList(ip, port);

	// main loop
	while (true) {
	    //queue len should < numb_fonrt and request queue shoud < numb_mid
	    if(vmProp.isMaster) {
		// init drop
		Cloud.FrontEndOps.Request r = SL.getNextRequest();
		if(SL.getStatusVM(2) == Cloud.CloudOps.VMStatus.Booting) {
		    SL.drop(r);
		}else {
		    masterAddRequest(r);
		}
		// measure current traffic
		int deltaFront = SL.getQueueLength() - frontNumb;
		int deltaMid = requestQueue.size() - midNumb;
		if(deltaFront >= 0 || deltaMid >= 0) {
		    lackFront = deltaFront > deltaMid ? true : false;
		    int tmp = deltaFront > deltaMid ? deltaFront : deltaMid;
		    for(int i = 0; i <= tmp; i++) {
			if(SL.getStatusVM(id_roleTable.size() + i + 2) == Cloud.CloudOps.VMStatus.NonExistent)
			  SL.startVM();
		    }
		}
	    }
	    if(vmProp.isFrontTier) {
		Cloud.FrontEndOps.Request r = SL.getNextRequest();
		if(master != null) master.addRequest(r);
	    }else{
		if (master != null && master.getRequestLength() != 0)
		  SL.processRequest(master.pollRequest());
	    }
	}
    }
}

