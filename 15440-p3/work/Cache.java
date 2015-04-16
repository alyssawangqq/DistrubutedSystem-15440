import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.AlreadyBoundException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.Hashtable;
import java.rmi.Naming;

public class Cache extends UnicastRemoteObject implements Cloud.DatabaseOps {
    public static Hashtable<String, String> dbRecords = new Hashtable<String, String>();
    // ctor
    public Cache() throws RemoteException { }

    //private static ServerLib Cache_SL;
    private static Cloud.DatabaseOps realDB;
    private static IServer master;

    // custom function
    public static boolean registerCache(String ip, int port) {
	Cache cache = null;
	try {
	    cache = new Cache();
	}
	catch(RemoteException e) {
	    System.err.println("Failed to create server " + e);
	    System.exit(1);
	}

	try {
	    Naming.bind(String.format("//%s:%d/%s", ip, port, "Cache"), cache);
	    System.err.println("I'm cache tier");
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

    // Interface
    public String get(String key) throws RemoteException{
	if(dbRecords.get(key) == null) { // miss
	    String records = realDB.get(key);
	    dbRecords.put(key, records);
	    return records;
	}else {
	    return dbRecords.get(key);
	}
    }
    public boolean set(String key, String val, String auth) throws RemoteException{
	return realDB.set(key, val, auth);
    }
    public boolean transaction(String item, float price, int qty) throws RemoteException{
	return realDB.transaction(item, price, qty);
    }

    public static boolean main ( String args[] ) throws Exception {
	if (args.length != 2) throw new Exception("Need 2 args: <cloud_ip> <cloud_port>");
	dbRecords.put("hi", "hello");
	master = Server.<IServer>getInstance(args[0], Integer.parseInt(args[1]), "Master");
	realDB = master.getRealDB();
	return registerCache(args[0], Integer.parseInt(args[1]));
    }
}
