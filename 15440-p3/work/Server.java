//Check Point 2 Final Version
import java.util.concurrent.locks.ReentrantLock;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.AlreadyBoundException;
import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Date;
import java.rmi.Naming;
import java.rmi.Remote;

class Properties {
    boolean isFrontTier = false;
    boolean isMaster = false;
    int myID = 0;
    Date date = new Date();
    //Timestamp timeStamp;
    long lastProcessTime;
}

class calculateRequestInterval implements Runnable{
    private int idx = 0;
    private int [] q;
    private int slots;
    private Server server;
    private int initBootTime = 5;

    calculateRequestInterval(int _slots, Server _s) {
	q = new int[10];
	this.server = _s;
	this.slots = _slots;
    }

    @Override
      public void run () {
	  while(true) {
	      if(idx > slots - 1) {
		  idx = 0; initBootTime -= 1; 
		  int rps = getRequestRate();
		  if(initBootTime > 0) { rps *= 4; }
		  System.err.println(rps);
		  int fn = server.getFrontEndNumb();
		  int mn = server.getMidNumb();
		  int midNeeded = 0; int frontNeeded = 0;
		  if(rps < 5) {
		      frontNeeded = 1;
		      midNeeded = 2;
		  }else if(rps >=5 && rps < 10) {
		      frontNeeded = 1;
		      midNeeded = 3;
		  }else if(rps >= 10 && rps < 20) {
		      frontNeeded = 2;
		      midNeeded = 5;
		  }else if(rps >= 20 && rps < 30) {
		      frontNeeded = 2;
		      midNeeded = 7;
		  } else if(rps > 30) {
		      frontNeeded = 2;
		      midNeeded = 9;
		  }

		  if(mn <= midNeeded || fn <= frontNeeded) {
		      for(int i = 0; i < midNeeded - mn; i++) {
			  server.pushToQueue(false);
		      }
		      for(int i = 0; i < frontNeeded - fn; i++) {
			  server.pushToQueue(true);
		      }

		      for(int i = 0; i < midNeeded-mn + frontNeeded-fn; i++) {
			  if(server.SL.getStatusVM(mn + fn + 1 + i) ==
			     Cloud.CloudOps.VMStatus.NonExistent){
			      server.SL.startVM();
			  }
		      }
		  }
	      }
	      q[idx] = server.getCurrent();
	      idx++;
	      server.setCurrent(0);
	      try { Thread.sleep(1000/slots);}
	      catch (Exception e) { e.printStackTrace(); }
	  }
      }

    public int getRequestRate() {
	int total = 0;
	for(int i = 0; i < slots; i++) {
	    total += q[i];
	}
	return total;
    }
}

public class Server extends UnicastRemoteObject implements IServer{
    public static String [] rmiRegistryList; // 100 total registry most
    public static LinkedList<Request> requestQueue;
    public static Hashtable<Integer, Boolean> id_roleTable = new Hashtable<Integer, Boolean>();
    public Server() throws RemoteException {}
    public static int currentRequestNumb = 0;
    public static Properties vmProp = new Properties();
    public static int frontNumb = 0;
    public static int midNumb = 0;
    public static LinkedList<Boolean> readyToBeOpenQueue;
    public static ServerLib SL;
    //public static Cache cache;

    private static boolean init = false;
    private static int initNumb = 2;
    private static int master_id_cnt = 0;
    private static long activeTh = 3333;
    private static int moreTh = 4;
    private static int request_cnt = 0;
    private static long [] requestIncomArray;
    private static long lastincoming = 0;
    private static long incomingRate_record = 0;
    private static long incomingRate = 0;
    private static Server server = null;
    private static long purchaseTh = 1500;
    private static long browseTh = 900;
    private static final ReentrantLock lock = new ReentrantLock();

    // java RMI
    @SuppressWarnings("unchecked")
      public static <T> T getInstance(String ip, int port, String name) throws RemoteException {
	  String url = String.format("//%s:%d/%s", ip, port, name);
	  try{
	      return (T) Naming.lookup (url);
	  } catch (Exception e) {
	      System.err.println(e);
	      return null;
	  }
      }

    public static boolean registMaster(String ip, int port) throws RemoteException {
	frontNumb += 1;
	init = true;
	requestIncomArray = new long[5];
	readyToBeOpenQueue = new LinkedList<Boolean>();
	requestQueue = new LinkedList<Request>();
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
    public synchronized Request pollRequest()
      throws RemoteException{
	  lock.lock();
	  Request req = null;
	  try { req = requestQueue.pollFirst(); }
	  catch(Exception e) { e.printStackTrace(); }
	  finally {
	      lock.unlock();
	      return req;
	  }
      }

    public synchronized long getRequestStartTime(Request req) 
      throws RemoteException{
	  return req.timeArrived;
      }

    public synchronized void addRequest(Request req)
      throws RemoteException{
	  lock.lock();
	  requestQueue.add(req);
	  lock.unlock();
      }

    public static long updateIncomTime() {
	if(request_cnt > 4) {
	    int sum = 0;
	    for(int i = 0; i < 4; i++) {
		sum += requestIncomArray[i];
	    }
	    incomingRate = sum/4;
	    incomingRate_record = incomingRate;
	    request_cnt = 0;
	    return incomingRate;
	}else {
	    vmProp.date = new Date();
	    long tmp = vmProp.date.getTime(); // get incoming rate
	    if((incomingRate = tmp - lastincoming) < 0) return 0;
	    requestIncomArray[request_cnt] = incomingRate;
	    lastincoming = tmp;
	    request_cnt++;
	    return incomingRate_record;
	}
    }

    public static synchronized void masterAddRequest(Request req) {
	requestQueue.add(req);
    }

    public synchronized void tellMasterNewReq(int n) throws RemoteException {
	currentRequestNumb += 1;
    }

    public Request peekRequest() 
      throws RemoteException{
	  return requestQueue.peek();
      }

    public synchronized int getRequestLength()
      throws RemoteException{
	  lock.lock();
	  int size = 0;
	  try{ size = requestQueue.size(); }
	  finally { lock.unlock(); }
	  return size;
      }

    public synchronized boolean assignTier() {
	return readyToBeOpenQueue.poll();
    }

    public int getVMNumber(boolean b) {
	if(b) return frontNumb;
	else return midNumb;
    }

    public synchronized int addVM(int id, boolean b) throws RemoteException{
	if(id_roleTable.get(id) == null) {
	    id_roleTable.put(id, b);
	    if(b) frontNumb += 1;
	    else midNumb += 1;
	    return 1;
	}else {
	    return -1;
	}
    }

    // tier operation/inspection
    public int getVMNumb() throws RemoteException {
	return frontNumb + midNumb;
    }

    public long getIncomingRate() throws RemoteException {
	return incomingRate;
    }

    public synchronized int getID() throws RemoteException {
	return master_id_cnt++;
    }

    public int getRequestQueueLength() throws RemoteException {
	return requestQueue.size();
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

    public static void shutDownVM(int id, boolean isFront, String ip, int port)
      throws RemoteException {
	  System.err.println("shutDown!");
	  IServer inst = null;
	  try {
	      if(isFront) {
		  inst = Server.<IServer>getInstance(ip, port, "FrontTier" + id);
	      }
	      else {
		  inst = Server.<IServer>getInstance(ip, port, "MidTier" + id);
	      }
	      SL.interruptGetNext();
	      SL.shutDown();
	      UnicastRemoteObject.unexportObject(inst, true);
	      inst = null;
	  } catch (NoSuchObjectException e) {
	      e.printStackTrace();
	  }
	  System.exit(0);
      }

    public ServerLib getSL() {
	return SL;
    }

    public Cloud.DatabaseOps getRealDB() {
	return SL.getDB();
    }

    public synchronized void setCurrent(int n) {
	currentRequestNumb = 0;
    }

    public synchronized int getCurrent() {
	return currentRequestNumb;
    }

    public static synchronized int getFrontEndNumb() {
	return frontNumb;
    }

    public static synchronized int getMidNumb() {
	return midNumb;
    }

    public synchronized void pushToQueue(boolean b) {
	readyToBeOpenQueue.push(b);
    }

    public synchronized void removeVM(boolean b) throws RemoteException{
	if(b) frontNumb--;
	else midNumb--;
    }

    public static void main ( String args[] ) throws Exception {
	if (args.length != 2) throw new Exception("Need 2 args: <cloud_ip> <cloud_port>");

	SL = new ServerLib( args[0], Integer.parseInt(args[1]) );
	int port = Integer.parseInt(args[1]);
	String ip = args[0];
	IServer master = null;
	calculateRequestInterval cri = null;

	if((vmProp.isMaster = registMaster(ip, port)) == false) {
	    master = Server.<IServer>getInstance(ip, port, "Master");
	    vmProp.isFrontTier = master.assignTier();
	    vmProp.myID = master.getID();
	    master.addVM(vmProp.myID, vmProp.isFrontTier);
	    if(vmProp.isFrontTier == false) {
		// handle request // mid tier
		if( registMidTier(ip, port, vmProp.myID) == false) return;
	    }else {
		// register
		if( registFrontTier(ip, port, vmProp.myID) == false)  return;
		SL.register_frontend();
	    }
	}else {
	    SL.register_frontend();
	    cri = new calculateRequestInterval(10, server);
	    new Thread(cri).start();
	    //Master to Mid Tier
	    SL.startVM(); // create the first mid tier
	    readyToBeOpenQueue.push(false);
	    if(Cache.main(args)){
		System.err.println("regist Cach success");
	    }else {
		System.err.println("regist Cach fail");
	    }	      // register Cache
	}

	Cloud.DatabaseOps cache = Server.<Cloud.DatabaseOps>getInstance(ip, port, "Cache");

	// time stamp
	vmProp.date = new Date();
	vmProp.lastProcessTime = vmProp.date.getTime();
	incomingRate_record = vmProp.lastProcessTime;
	// main loop
	while (true) {
	    if(vmProp.isMaster) {
		Cloud.FrontEndOps.Request r = SL.getNextRequest();
		int length = SL.getQueueLength();
		if(length >= 0) currentRequestNumb += 1;
		if(SL.getStatusVM(initNumb) == Cloud.CloudOps.VMStatus.Booting)
		  { SL.drop(r); }
		else {
		    vmProp.date = new Date();
		    Request req = new Request(r, vmProp.date.getTime());
		    masterAddRequest(req);
		    init = false;
		}
	    }else if(vmProp.isFrontTier) {
		while(master.getRequestLength() - master.getVMNumber(false) > moreTh)
		  { 
		    SL.dropHead(); 
		    master.tellMasterNewReq(1);
		  }
		vmProp.date = new Date();
		if(vmProp.date.getTime() - vmProp.lastProcessTime < activeTh) {
		    Cloud.FrontEndOps.Request r = SL.getNextRequest();
		    int length = SL.getQueueLength();
		    if(length >= 0)  master.tellMasterNewReq(1);
		    Request req = new Request(r, vmProp.date.getTime());
		    master.addRequest(req);
		    vmProp.date = new Date();
		    vmProp.lastProcessTime = vmProp.date.getTime();
		}else {
		    if(master.getVMNumb() > 2) {
			SL.unregister_frontend();
			master.removeVM(true);
			shutDownVM(vmProp.myID, true, ip, port);
		    }else {
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			int length = SL.getQueueLength();
			if(length >= 0)  master.tellMasterNewReq(1);
			Request req = new Request(r, vmProp.date.getTime());
			master.addRequest(req);
			vmProp.date = new Date();
			vmProp.lastProcessTime = vmProp.date.getTime();
		    }
		}
	    }else if(!vmProp.isFrontTier) {
		vmProp.date = new Date();
		if (vmProp.date.getTime() - vmProp.lastProcessTime < activeTh) {
		    int length;
		    if((length = master.getRequestLength()) > 0) {
			Request r = master.pollRequest();
			if(r == null) continue;
			if(r._r.isPurchase && ((vmProp.date.getTime() - r.timeArrived) > purchaseTh) ||
			   vmProp.date.getTime() - r.timeArrived > browseTh){
			    SL.drop(r._r);
			}else {
			    SL.processRequest(r._r, cache);
			}
			vmProp.date = new Date();
			vmProp.lastProcessTime = vmProp.date.getTime();
		    }
		}else {
		    if(master.getVMNumb() > 2) {
			shutDownVM(vmProp.myID, false, ip, port);
			master.removeVM(false);
		    }else {
			Request r = master.pollRequest();
			if(r == null) continue;
			SL.processRequest(r._r, cache);
			vmProp.date = new Date();
			vmProp.lastProcessTime = vmProp.date.getTime();
		    }
		}
	    }
	}
    }
}
