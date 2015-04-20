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
		//int midNeeded = (int)(rps/35);
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
		    frontNeeded = 3;
		    midNeeded = 8;
		} else if(rps > 30) {
		    frontNeeded = 3;
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
    //private static boolean lackFront = false;
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
	//System.err.println("id table: " + id_roleTable.size());
	//requestQueueWithTime = new Hashtable<Cloud.FrontEndOps.Request, Long>();
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
	    //System.err.println("get id: " +getID());
	    //System.err.println("total numb" + (frontNumb + midNumb));
	    //return getID(); 
	    return 1;
	}else {
	    return -1; // Already exists
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

	//getRmiList(ip, port);
	Cloud.DatabaseOps cache = Server.<Cloud.DatabaseOps>getInstance(ip, port, "Cache");

	// time stamp
	vmProp.date = new Date();
	vmProp.lastProcessTime = vmProp.date.getTime();
	incomingRate_record = vmProp.lastProcessTime;
	// main loop
	while (true) {
	    //queue len should < numb_fonrt and request queue shoud < numb_mid
	    if(vmProp.isMaster) {
		// init drop
		//long request_rate = updateIncomTime();
		//if(SL.getQueueLength() > 0) {
		//    Date time = new Date();
		//    System.err.println("interval: " + time.getTime());

		//}
		//long startTime = 100000000;
		//if(incomingRate > startTime) continue;
		//System.err.println("interval: " + incomingRate);
		//init new 
		//System.err.println(cri.getRequestRate());
		//int rps = cri.getRequestRate();
		//System.err.println(currentRequestNumb);
		//int deltaFront = SL.getQueueLength() - frontNumb;
		//int deltaMid = requestQueue.size() -  midNumb;

		//System.err.println(request_rate);

		//if(request_rate > 0 ) {
		    //int midNeeded = (int)(1200/request_rate);
		//    int midNeeded = (int)(rps/3);
		////    System.err.println("mid needed" + midNeeded + ",mid numb" +
		////		       midNumb);
		//    if(midNumb < midNeeded) {
		//	for(int i = 0; i < midNeeded - midNumb; i++) {
		//	    readyToBeOpenQueue.push(false);
		//	    if(SL.getStatusVM(midNeeded + 1) == 
		//	       Cloud.CloudOps.VMStatus.NonExistent){
		//		SL.startVM();
		//	    }
		//	}
		//    }

		//    //int frontNeeded = (int)(450/request_rate);
		//    int frontNeeded = (int)(rps/6);
		////    System.err.println("front needed" + frontNeeded + ", front numb" +
		////		       frontNumb);
		//    if(frontNumb < frontNeeded) {
		//	for(int i = 0; i < frontNeeded - frontNumb; i++) {
		//	    readyToBeOpenQueue.push(true);
		//	    if(SL.getStatusVM(frontNeeded + 1) == 
		//	       Cloud.CloudOps.VMStatus.NonExistent){
		//		SL.startVM();
		//	    }
		//	}
		//    }
		////}

		//if(incomingRate < 100 && !init) { // TODO: why this will cause problem ? lack front
		//    System.err.println("master init start");
		//    for(int i = 0; i < 3; i++) {
		//	lackFront = true;
		//	if(SL.getStatusVM(id_roleTable.size() + i + 2) == 
		//	   Cloud.CloudOps.VMStatus.NonExistent)
		//	   { SL.startVM(); }
		//    }
		//    for(int i = 0; i < 3; i++) {
		//	lackFront = false;
		//	if(SL.getStatusVM(id_roleTable.size() + i + 2) == 
		//	   Cloud.CloudOps.VMStatus.NonExistent)
		//	   { SL.startVM(); }
		//    }
		//    initNumb += 6;
		//    init = false;
		//    System.err.println("table size" + id_roleTable.size());
		//}
		// TODO: open up 10 VM if incoming Rate < 200
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
		//// measure current traffic
		//// use new method
		//int deltaFront = SL.getQueueLength() - frontNumb;
		//int deltaMid = requestQueue.size() -  midNumb;
		//if(deltaFront > 0 || deltaMid > 0) {
		//    //int frontNeeded = 200/incomingRate;
		//    //int midNeeded = 300/incomingRate;
		//    for(int i = 0; i < deltaMid; i++) {
		//        lackFront = false;
		//        if(SL.getStatusVM(id_roleTable.size() + i + 2) == 
		//           Cloud.CloudOps.VMStatus.NonExistent){
		//            SL.startVM();
		//        }
		//    }

		//    for(int i = 0; i < deltaFront; i++) {
		//        lackFront = true;
		//        if(SL.getStatusVM(id_roleTable.size() + i + 2) == 
		//           Cloud.CloudOps.VMStatus.NonExistent){
		//            SL.startVM();
		//        }
		//    }
		//}

	    }else if(vmProp.isFrontTier) { //TODO drop when cannot handle // consider work more here
		//System.err.println("r len : " + master.getRequestLength());
		//System.err.println("queue len : " + SL.getQueueLength());
		//System.err.println("m len : " + master.getVMNumber(false));
		while(master.getRequestLength() - master.getVMNumber(false) > moreTh
		      //&& SL.getStatusVM(master.getVMNumber(false) +
		      //  		master.getVMNumber(true)) ==
		      //(Cloud.CloudOps.VMStatus.Booting)
		  )
		    { 
		      //System.err.println("head drop");
		      SL.dropHead(); 
		      master.tellMasterNewReq(1);
		    }
		vmProp.date = new Date();
		if(vmProp.date.getTime() - vmProp.lastProcessTime < activeTh) {
		    // detect time interval
		    //long test_record = vmProp.date.getTime();
		    //int length = SL.getQueueLength();
		    //if(length > 0) master.tellMasterNewReq(1);
		    Cloud.FrontEndOps.Request r = SL.getNextRequest();
		    int length = SL.getQueueLength();
		    if(length >= 0)  master.tellMasterNewReq(1);
		    Request req = new Request(r, vmProp.date.getTime());
		    master.addRequest(req);
		    vmProp.date = new Date();
		    vmProp.lastProcessTime = vmProp.date.getTime();
		    //System.err.println("tier add time" + (vmProp.lastProcessTime - test_record));
		}else {
		    if(master.getVMNumb() > 2) {
			SL.unregister_frontend();
			master.removeVM(true);
			shutDownVM(vmProp.myID, true, ip, port); // Numb and Number T T
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
		//if(vmProp.date.getTime() - vmProp.lastProcessTime > activeTh)
		//  System.err.println(vmProp.date.getTime() - vmProp.lastProcessTime);
		if (vmProp.date.getTime() - vmProp.lastProcessTime < activeTh) {
		    int length;
		    if((length = master.getRequestLength()) > 0) {
			//long test_record = vmProp.date.getTime();
			//while((length - master.getVMNumber(false) > moreTh) &&
			//   SL.getStatusVM(master.getID() + 2) ==
			//   Cloud.CloudOps.VMStatus.Booting) {
			//    Request r = master.pollRequest();
			//    System.err.println("mid drop");
			//    //if(r == null) break;
			//    //SL.drop(r._r);
			//}
			    Request r = master.pollRequest();
			    if(r == null) continue; // TODO: why r will be null
			//}else {
			    if(r._r.isPurchase && ((vmProp.date.getTime() - r.timeArrived) > purchaseTh) ||
				vmProp.date.getTime() - r.timeArrived > browseTh){
				//System.err.println("no way to handle purchase, drop");
				SL.drop(r._r);
			    }else {
				SL.processRequest(r._r, cache);
			    }
			//}
			vmProp.date = new Date();
			vmProp.lastProcessTime = vmProp.date.getTime();
			//System.err.println("tier process time" + (vmProp.lastProcessTime - test_record));
		    }
		}else {
		    if(master.getVMNumb() > 2) {
			shutDownVM(vmProp.myID, false, ip, port);
			master.removeVM(false);
		    }else {
			    Request r = master.pollRequest();
			    if(r == null) continue; // TODO: why r will be null
			    SL.processRequest(r._r, cache);
			    vmProp.date = new Date();
			    vmProp.lastProcessTime = vmProp.date.getTime();
		    }
		}
	    }
	}
    }
}

// test log : 20s - 100 , total time: 5292792 VM 50 Bad:140
// test log : 20s - 100 , total time: 596309 VM 15 Bad:160
// test log : 20s - 100 , total time: 647491
// test log : 20s - 100 , total time: 1540670 VM 19
// test log : 40s - 100 , total time: 1396999
